package com.example.parser

import android.content.Context
import android.net.Uri
import com.example.data.model.StatementTransactionEntity
import com.example.data.model.TxnType
import com.example.ui.components.CurrencyFormatUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font

object PdfStatementExporter {

    private const val MARGIN = 40f
    private const val ROW_HEIGHT = 16f
    private const val BOTTOM_LIMIT = 60f
    private const val MAX_DESCRIPTION_CHARS = 38

    fun export(
        context: Context,
        outputUri: Uri,
        rangeLabel: String,
        transactions: List<StatementTransactionEntity>,
        totalCredit: Double,
        totalDebit: Double
    ): Boolean {
        return try {
            PDDocument().use { document ->
                val pageWidth = PDRectangle.A4.width
                val pageHeight = PDRectangle.A4.height
                val numX = MARGIN
                val dateX = MARGIN + 25f
                val descX = MARGIN + 95f
                val typeX = MARGIN + 335f
                val amountX = pageWidth - MARGIN - 115f

                var page = PDPage(PDRectangle.A4)
                document.addPage(page)
                var stream = PDPageContentStream(document, page)
                var y = pageHeight - MARGIN

                fun text(x: Float, yPos: Float, size: Float, bold: Boolean, value: String) {
                    stream.beginText()
                    stream.setFont(if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA, size)
                    stream.newLineAtOffset(x, yPos)
                    stream.showText(sanitizeForPdf(value))
                    stream.endText()
                }

                fun drawTableHeader() {
                    text(numX, y, 10f, true, "#")
                    text(dateX, y, 10f, true, "Date")
                    text(descX, y, 10f, true, "Description")
                    text(typeX, y, 10f, true, "Type")
                    text(amountX, y, 10f, true, "Amount")
                    y -= 6f
                    stream.setLineWidth(0.5f)
                    stream.moveTo(MARGIN, y)
                    stream.lineTo(pageWidth - MARGIN, y)
                    stream.stroke()
                    y -= ROW_HEIGHT
                }

                text(MARGIN, y, 18f, true, "RupeeFlow Statement")
                y -= 24f
                text(MARGIN, y, 11f, false, rangeLabel)
                y -= 20f
                text(
                    MARGIN, y, 11f, true,
                    "Credit: ${formatRs(totalCredit)}   Debit: ${formatRs(totalDebit)}   " +
                        "Net: ${formatRs(totalCredit - totalDebit)}   Transactions: ${transactions.size}"
                )
                y -= 26f
                drawTableHeader()

                transactions.forEachIndexed { index, txn ->
                    if (y < BOTTOM_LIMIT) {
                        stream.close()
                        page = PDPage(PDRectangle.A4)
                        document.addPage(page)
                        stream = PDPageContentStream(document, page)
                        y = pageHeight - MARGIN
                        drawTableHeader()
                    }

                    val description = if (txn.description.length > MAX_DESCRIPTION_CHARS) {
                        txn.description.take(MAX_DESCRIPTION_CHARS - 3) + "..."
                    } else {
                        txn.description
                    }
                    val sign = if (txn.type == TxnType.CREDIT) "+" else "-"

                    text(numX, y, 9f, false, "${index + 1}")
                    text(dateX, y, 9f, false, CurrencyFormatUtils.formatDateShort(txn.date))
                    text(descX, y, 9f, false, description)
                    text(typeX, y, 9f, false, if (txn.type == TxnType.CREDIT) "Credit" else "Debit")
                    text(amountX, y, 9f, false, "$sign${formatRs(txn.amount)}")

                    y -= ROW_HEIGHT
                }

                stream.close()

                val outputStream = context.contentResolver.openOutputStream(outputUri) ?: return false
                outputStream.use { document.save(it) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun formatRs(amount: Double): String {
        val formatted = CurrencyFormatUtils.formatInr(amount)
        return when {
            formatted.startsWith("-₹") -> "-Rs. " + formatted.removePrefix("-₹")
            else -> "Rs. " + formatted.removePrefix("₹")
        }
    }

    /** Standard PDF Type1 fonts only support Latin-1; swap anything outside that range so showText never throws. */
    private fun sanitizeForPdf(text: String): String =
        text.map { if (it.code in 32..255) it else '?' }.joinToString("")
}
