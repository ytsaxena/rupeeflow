package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.BankStatementEntity
import com.example.data.model.StatementTransactionEntity
import com.example.data.model.TxnType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StatementDeleteTest {

    private fun buildDb(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `deleting a statement removes only its own transactions and updates totals`() = runTest {
        val db = buildDb()
        val dao = db.statementDao()

        val statementAId = dao.insertStatement(
            BankStatementEntity(
                fileName = "StatementA.pdf",
                uploadedAt = 1000L,
                periodStart = 100L,
                periodEnd = 200L,
                transactionCount = 2,
                totalCredit = 500.0,
                totalDebit = 300.0
            )
        )
        dao.insertTransactions(
            listOf(
                StatementTransactionEntity(statementId = statementAId, date = 100L, description = "A-debit", amount = 300.0, type = TxnType.DEBIT, balance = 700.0),
                StatementTransactionEntity(statementId = statementAId, date = 150L, description = "A-credit", amount = 500.0, type = TxnType.CREDIT, balance = 1200.0)
            )
        )

        val statementBId = dao.insertStatement(
            BankStatementEntity(
                fileName = "StatementB.pdf",
                uploadedAt = 2000L,
                periodStart = 300L,
                periodEnd = 400L,
                transactionCount = 1,
                totalCredit = 0.0,
                totalDebit = 50.0
            )
        )
        dao.insertTransactions(
            listOf(
                StatementTransactionEntity(statementId = statementBId, date = 350L, description = "B-debit", amount = 50.0, type = TxnType.DEBIT, balance = 1150.0)
            )
        )

        // Sanity check before delete: both statements and all 3 transactions exist.
        assertEquals(2, dao.getAllStatements().first().size)
        assertEquals(3, dao.getTransactionsInRange(0L, 10_000L).first().size)

        // Per-statement scoped query returns only that statement's rows, not the whole table.
        assertEquals(2, dao.getTransactionsForStatement(statementAId).first().size)
        assertEquals(1, dao.getTransactionsForStatement(statementBId).first().size)

        // Range query only returns rows inside the window, e.g. Statement A's dates (100-200) only.
        assertEquals(2, dao.getTransactionsInRange(100L, 200L).first().size)

        dao.deleteStatement(statementAId)

        val remainingStatements = dao.getAllStatements().first()
        val remainingTransactions = dao.getTransactionsInRange(0L, 10_000L).first()

        assertEquals("Statement A should be gone, Statement B should remain", 1, remainingStatements.size)
        assertEquals(statementBId, remainingStatements[0].id)

        assertEquals("Statement A's transactions must cascade-delete; Statement B's must survive", 1, remainingTransactions.size)
        assertEquals("B-debit", remainingTransactions[0].description)
        assertTrue(remainingTransactions.none { it.statementId == statementAId })

        db.close()
    }
}
