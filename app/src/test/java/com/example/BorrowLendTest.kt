package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.BankStatementEntity
import com.example.data.model.BorrowLendEntity
import com.example.data.model.BorrowLendType
import com.example.data.model.StatementTransactionEntity
import com.example.data.model.TxnType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BorrowLendTest {

    private fun buildDb(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `entries can be added, listed newest first, and deleted`() = runTest {
        val db = buildDb()
        val dao = db.borrowLendDao()

        dao.insert(BorrowLendEntity(personInfo = "Rahul", amount = 5000.0, type = BorrowLendType.LENT, date = 100L))
        val secondId = dao.insert(BorrowLendEntity(personInfo = "Office loan", amount = 20000.0, type = BorrowLendType.BORROWED, date = 200L))

        val entries = dao.getAll().first()
        assertEquals(2, entries.size)
        assertEquals("Newest date should come first", "Office loan", entries[0].personInfo)

        dao.delete(secondId)
        val afterDelete = dao.getAll().first()
        assertEquals(1, afterDelete.size)
        assertEquals("Rahul", afterDelete[0].personInfo)

        db.close()
    }

    @Test
    fun `borrow-lend entries are completely isolated from statement transactions`() = runTest {
        val db = buildDb()
        val statementDao = db.statementDao()
        val borrowLendDao = db.borrowLendDao()

        val statementId = statementDao.insertStatement(
            BankStatementEntity(
                fileName = "Statement.pdf",
                uploadedAt = 1000L,
                periodStart = 100L,
                periodEnd = 200L,
                transactionCount = 1,
                totalCredit = 500.0,
                totalDebit = 0.0
            )
        )
        statementDao.insertTransactions(
            listOf(StatementTransactionEntity(statementId = statementId, date = 100L, description = "Salary", amount = 500.0, type = TxnType.CREDIT, balance = 500.0))
        )
        borrowLendDao.insert(BorrowLendEntity(personInfo = "Friend", amount = 1000.0, type = BorrowLendType.LENT, date = 100L))

        // Deleting the entire statement (and its cascaded transactions) must not touch borrow/lend data.
        statementDao.deleteStatement(statementId)

        assertEquals(0, statementDao.getTransactionsInRange(0L, 10_000L).first().size)
        assertEquals("Borrow/Lend entries must survive statement deletion untouched", 1, borrowLendDao.getAll().first().size)

        // And the reverse: deleting a borrow/lend entry must not touch statement data at all
        // (there's nothing to even reference — proving they share no keys/relations).
        val statementsBefore = statementDao.getAllStatements().first().size
        val entryId = borrowLendDao.getAll().first().first().id
        borrowLendDao.delete(entryId)
        assertEquals(statementsBefore, statementDao.getAllStatements().first().size)

        db.close()
    }
}
