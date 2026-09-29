package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StatementTransactionEntity
import com.example.data.model.TxnType
import com.example.parser.PdfStatementExporter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfStatementExporterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)
    }

    private fun sampleTransactions(count: Int): List<StatementTransactionEntity> {
        return (1..count).map { i ->
            StatementTransactionEntity(
                id = i.toLong(),
                statementId = 1L,
                date = 1_700_000_000_000L + i * 86_400_000L,
                description = "Transaction number $i for testing export pagination",
                amount = 100.0 + i,
                type = if (i % 3 == 0) TxnType.CREDIT else TxnType.DEBIT,
                balance = 50000.0 - i
            )
        }
    }

    @Test
    fun `exports a small transaction list onto a single valid PDF page`() {
        val outputFile = File.createTempFile("export_small", ".pdf")
        val uri = Uri.fromFile(outputFile)
        val transactions = sampleTransactions(5)

        val success = PdfStatementExporter.export(
            context = context,
            outputUri = uri,
            rangeLabel = "01 Aug 2026 - 05 Aug 2026",
            transactions = transactions,
            totalCredit = transactions.filter { it.type == TxnType.CREDIT }.sumOf { it.amount },
            totalDebit = transactions.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }
        )

        assertTrue("Export should report success", success)
        assertTrue("Output file should exist and be non-empty", outputFile.length() > 0)

        PDDocument.load(outputFile).use { document ->
            assertEquals(1, document.numberOfPages)
            val text = PDFTextStripper().getText(document)
            assertTrue(text.contains("RupeeFlow Statement"))
            assertTrue(text.contains("Transaction number 1"))
        }

        outputFile.delete()
    }

    @Test
    fun `paginates across multiple pages when there are many transactions`() {
        val outputFile = File.createTempFile("export_large", ".pdf")
        val uri = Uri.fromFile(outputFile)
        val transactions = sampleTransactions(120)

        val success = PdfStatementExporter.export(
            context = context,
            outputUri = uri,
            rangeLabel = "01 Jan 2026 - 31 Dec 2026",
            transactions = transactions,
            totalCredit = transactions.filter { it.type == TxnType.CREDIT }.sumOf { it.amount },
            totalDebit = transactions.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }
        )

        assertTrue(success)

        PDDocument.load(outputFile).use { document ->
            assertTrue("120 rows should overflow onto more than one page", document.numberOfPages > 1)
            val text = PDFTextStripper().getText(document)
            assertTrue(text.contains("Transaction number 1 "))
            assertTrue(text.contains("Transaction number 120"))
        }

        outputFile.delete()
    }
}
