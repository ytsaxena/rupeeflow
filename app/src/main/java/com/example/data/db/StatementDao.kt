package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.BankStatementEntity
import com.example.data.model.StatementTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatementDao {
    @Insert
    suspend fun insertStatement(statement: BankStatementEntity): Long

    @Insert
    suspend fun insertTransactions(transactions: List<StatementTransactionEntity>)

    @Query("SELECT * FROM bank_statements ORDER BY uploadedAt DESC")
    fun getAllStatements(): Flow<List<BankStatementEntity>>

    @Query("SELECT * FROM statement_transactions WHERE date BETWEEN :startInclusive AND :endInclusive ORDER BY date DESC")
    fun getTransactionsInRange(startInclusive: Long, endInclusive: Long): Flow<List<StatementTransactionEntity>>

    @Query("SELECT * FROM statement_transactions WHERE statementId = :statementId ORDER BY date DESC")
    fun getTransactionsForStatement(statementId: Long): Flow<List<StatementTransactionEntity>>

    @Query("DELETE FROM bank_statements WHERE id = :statementId")
    suspend fun deleteStatement(statementId: Long)

    @Query("DELETE FROM statement_transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

    @Query("UPDATE statement_transactions SET category = :category WHERE id = :transactionId")
    suspend fun updateTransactionCategory(transactionId: Long, category: String?)

    @Query("SELECT DISTINCT category FROM statement_transactions WHERE category IS NOT NULL ORDER BY category")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT * FROM statement_transactions WHERE category IS NULL")
    suspend fun getUncategorizedTransactions(): List<StatementTransactionEntity>
}
