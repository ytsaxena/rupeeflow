package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BorrowLendType { BORROWED, LENT }

/**
 * Fully independent of the bank-statement tables — no foreign keys, no shared queries.
 * Borrow/Lend tracking is a separate ledger the user keeps deliberately apart from their
 * statement-derived credit/debit totals.
 */
@Entity(tableName = "borrow_lend_entries")
data class BorrowLendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personInfo: String,
    val amount: Double,
    val type: BorrowLendType,
    val date: Long
)
