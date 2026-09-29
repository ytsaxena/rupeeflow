package com.example.data.repository

import com.example.data.db.StatementDao
import com.example.data.model.BankStatementEntity
import com.example.data.model.StatementTransactionEntity
import kotlinx.coroutines.flow.Flow

class StatementRepository(private val dao: StatementDao) {

    val statements: Flow<List<BankStatementEntity>> = dao.getAllStatements()

    fun transactionsInRange(startInclusive: Long, endInclusive: Long): Flow<List<StatementTransactionEntity>> =
        dao.getTransactionsInRange(startInclusive, endInclusive)

    fun transactionsForStatement(statementId: Long): Flow<List<StatementTransactionEntity>> =
        dao.getTransactionsForStatement(statementId)

    suspend fun saveStatement(
        statement: BankStatementEntity,
        transactions: List<StatementTransactionEntity>
    ): Long {
        val statementId = dao.insertStatement(statement)
        if (transactions.isNotEmpty()) {
            dao.insertTransactions(transactions.map { it.copy(statementId = statementId) })
        }
        return statementId
    }

    suspend fun deleteStatement(statementId: Long) = dao.deleteStatement(statementId)

    suspend fun deleteTransaction(transactionId: Long) = dao.deleteTransaction(transactionId)

    suspend fun updateTransactionCategory(transactionId: Long, category: String?) =
        dao.updateTransactionCategory(transactionId, category)

    val distinctCategories: Flow<List<String>> = dao.getDistinctCategories()

    suspend fun uncategorizedTransactions(): List<StatementTransactionEntity> = dao.getUncategorizedTransactions()
}
