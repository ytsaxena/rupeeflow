package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxnType { CREDIT, DEBIT }

@Entity(tableName = "bank_statements")
data class BankStatementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val uploadedAt: Long,
    val periodStart: Long?,
    val periodEnd: Long?,
    val transactionCount: Int,
    val totalCredit: Double,
    val totalDebit: Double
)

@Entity(
    tableName = "statement_transactions",
    foreignKeys = [
        ForeignKey(
            entity = BankStatementEntity::class,
            parentColumns = ["id"],
            childColumns = ["statementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("statementId"), Index("date"), Index("category")]
)
data class StatementTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val statementId: Long,
    val date: Long,
    val description: String,
    val amount: Double,
    val type: TxnType,
    val balance: Double?,
    val category: String? = null
)
