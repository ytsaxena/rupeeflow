package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.BorrowLendType
import com.example.data.model.TxnType

class Converters {
    @TypeConverter
    fun fromTxnType(type: TxnType): String = type.name

    @TypeConverter
    fun toTxnType(value: String): TxnType {
        return try {
            TxnType.valueOf(value)
        } catch (e: Exception) {
            TxnType.DEBIT
        }
    }

    @TypeConverter
    fun fromBorrowLendType(type: BorrowLendType): String = type.name

    @TypeConverter
    fun toBorrowLendType(value: String): BorrowLendType {
        return try {
            BorrowLendType.valueOf(value)
        } catch (e: Exception) {
            BorrowLendType.LENT
        }
    }
}
