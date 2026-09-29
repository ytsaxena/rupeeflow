package com.example.parser

import android.content.Context
import android.net.Uri
import com.example.data.model.TxnType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

sealed interface PdfExtractionResult {
    data class Success(val text: String) : PdfExtractionResult
    data class PasswordRequired(val wrongAttempt: Boolean) : PdfExtractionResult
    data class Error(val message: String) : PdfExtractionResult
}

data class ParsedStatementTxn(
    val date: Long,
    val description: String,
    val amount: Double,
    val type: TxnType,
    val balance: Double?
)

object PdfStatementParser {

    private val DATE_PATTERNS = listOf(
        "dd/MM/yyyy" to Pattern.compile("""\b(\d{1,2}/\d{1,2}/\d{4})\b"""),
        "dd-MM-yyyy" to Pattern.compile("""\b(\d{1,2}-\d{1,2}-\d{4})\b"""),
        "dd.MM.yyyy" to Pattern.compile("""\b(\d{1,2}\.\d{1,2}\.\d{4})\b"""),
        "dd/MM/yy" to Pattern.compile("""\b(\d{1,2}/\d{1,2}/\d{2})\b"""),
        "dd-MM-yy" to Pattern.compile("""\b(\d{1,2}-\d{1,2}-\d{2})\b"""),
        "dd MMM yyyy" to Pattern.compile("""\b(\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4})\b"""),
        "dd-MMM-yyyy" to Pattern.compile("""\b(\d{1,2}-[A-Za-z]{3,9}-\d{4})\b"""),
        "dd-MMM-yy" to Pattern.compile("""\b(\d{1,2}-[A-Za-z]{3}-\d{2})\b"""),
        "MMM dd, yyyy" to Pattern.compile("""\b([A-Za-z]{3,9}\s+\d{1,2},\s*\d{4})\b"""),
        "MMM dd yyyy" to Pattern.compile("""\b([A-Za-z]{3,9}\s+\d{1,2}\s+\d{4})\b""")
    )

    private val AMOUNT_PATTERN = Pattern.compile("""[\d,]+\.\d{2}""")

    // Strong, format-level signals banks actually print (e.g. "UPI/DR/...", "WDL TFR", "DEBIT ACHDr").
    // Checked before the generic keyword fallback because narration text ("Payment for PF NAC")
    // can contain debit-sounding words even on a credit row.
    private val STRONG_DEBIT_REGEX = Regex("""(?i)(/dr\b|\bdr/|\bwdl\b|\bwithdrawal\b|\bdebit\b)""")
    private val STRONG_CREDIT_REGEX = Regex("""(?i)(/cr\b|\bcr/|\bdep\b|\bdeposit\b|\bcredit\b)""")

    private val DEBIT_KEYWORDS = listOf(
        "withdrawal", "purchase", "paid", "payment", "spent", "sent", "atm", "pos"
    )
    private val CREDIT_KEYWORDS = listOf(
        "received", "salary", "refund", "cashback", "interest"
    )

    fun extractText(context: Context, uri: Uri, password: String? = null, hadPreviousAttempt: Boolean = false): PdfExtractionResult {
        return try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) return PdfExtractionResult.Error("Could not open the selected file.")
                PDDocument.load(input, password ?: "").use { document ->
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    val text = stripper.getText(document)
                    PdfExtractionResult.Success(text)
                }
            }
        } catch (e: InvalidPasswordException) {
            PdfExtractionResult.PasswordRequired(wrongAttempt = hadPreviousAttempt)
        } catch (e: Exception) {
            PdfExtractionResult.Error(e.message ?: "Failed to read the PDF.")
        }
    }

    /**
     * Rather than requiring a date and its amounts to sit on the same extracted line (fragile —
     * wrapped narration text and column reflow break that easily), each date occurrence marks the
     * start of a transaction "block" that runs up to the next date occurrence anywhere in the text.
     * This tolerates multi-line wrapped descriptions and duplicate Value-Date/Post-Date columns.
     */
    fun parseTransactions(rawText: String): List<ParsedStatementTxn> {
        val dateMatches = findAllDates(rawText)
        if (dateMatches.isEmpty()) return emptyList()

        val results = mutableListOf<ParsedStatementTxn>()

        for (i in dateMatches.indices) {
            val current = dateMatches[i]
            val blockEnd = if (i + 1 < dateMatches.size) dateMatches[i + 1].start else rawText.length
            if (blockEnd <= current.end) continue
            val block = rawText.substring(current.end, blockEnd)

            val amountMatcher = AMOUNT_PATTERN.matcher(block)
            val amounts = mutableListOf<Pair<Double, Int>>()
            while (amountMatcher.find()) {
                val value = amountMatcher.group().replace(",", "").toDoubleOrNull()
                if (value != null) amounts.add(value to amountMatcher.start())
            }
            if (amounts.isEmpty()) continue

            val description = block.substring(0, amounts.first().second)
                .replace(Regex("\\s+"), " ")
                .trim()
            if (description.isBlank()) continue

            val (amount, balance, type) = when (amounts.size) {
                1 -> Triple(amounts[0].first, null, inferType(block))
                2 -> Triple(amounts[0].first, amounts[1].first, inferType(block))
                else -> {
                    val debitVal = amounts[0].first
                    val creditVal = amounts[1].first
                    val balanceVal = amounts[2].first
                    when {
                        debitVal > 0.0 -> Triple(debitVal, balanceVal, TxnType.DEBIT)
                        creditVal > 0.0 -> Triple(creditVal, balanceVal, TxnType.CREDIT)
                        else -> Triple(balanceVal, null, inferType(block))
                    }
                }
            }

            if (amount <= 0.0) continue

            results.add(
                ParsedStatementTxn(
                    date = current.millis,
                    description = description,
                    amount = amount,
                    type = type,
                    balance = balance
                )
            )
        }

        return results
    }

    private fun inferType(text: String): TxnType {
        val strongDebit = STRONG_DEBIT_REGEX.containsMatchIn(text)
        val strongCredit = STRONG_CREDIT_REGEX.containsMatchIn(text)
        if (strongDebit && !strongCredit) return TxnType.DEBIT
        if (strongCredit && !strongDebit) return TxnType.CREDIT

        val lower = text.lowercase(Locale.ROOT)
        val weakDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val weakCredit = CREDIT_KEYWORDS.any { lower.contains(it) }
        return if (weakCredit && !weakDebit) TxnType.CREDIT else TxnType.DEBIT
    }

    private data class DateMatch(val start: Int, val end: Int, val millis: Long)

    /**
     * Only accepts a date as a transaction-boundary if it starts a physical line (after leading
     * whitespace). Scanning the whole text for a date pattern anywhere would also match dates
     * embedded inside a UPI/NEFT reference or narration string (e.g. "...RefNo dated 15/08/2026"),
     * which would wrongly be read as the start of a new transaction and corrupt the real one.
     * Restricting to line starts avoids that while still letting a transaction's block span
     * multiple wrapped lines (only the boundary itself needs to be at a line start).
     */
    private fun findAllDates(text: String): List<DateMatch> {
        val matches = mutableListOf<DateMatch>()
        var lineStart = 0
        val length = text.length

        while (lineStart < length) {
            val newlineIndex = text.indexOf('\n', lineStart)
            val lineEnd = if (newlineIndex == -1) length else newlineIndex
            val line = text.substring(lineStart, lineEnd)
            val trimmed = line.trimStart()
            val offset = lineStart + (line.length - trimmed.length)

            for ((format, pattern) in DATE_PATTERNS) {
                val matcher = pattern.matcher(trimmed)
                if (matcher.lookingAt()) {
                    val matchedText = matcher.group(1)
                    val millis = matchedText?.let { tryParseDate(it, format) }
                    if (millis != null) {
                        matches.add(DateMatch(offset, offset + matcher.end(1), millis))
                        break
                    }
                }
            }

            lineStart = lineEnd + 1
        }

        return matches
    }

    private fun tryParseDate(text: String, format: String): Long? {
        return try {
            val sdf = SimpleDateFormat(format, Locale.ENGLISH)
            sdf.isLenient = false
            sdf.parse(text)?.time
        } catch (e: Exception) {
            null
        }
    }
}
