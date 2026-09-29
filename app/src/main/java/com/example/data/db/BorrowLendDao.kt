package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.BorrowLendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowLendDao {
    @Insert
    suspend fun insert(entry: BorrowLendEntity): Long

    @Query("SELECT * FROM borrow_lend_entries ORDER BY date DESC")
    fun getAll(): Flow<List<BorrowLendEntity>>

    @Query("DELETE FROM borrow_lend_entries WHERE id = :entryId")
    suspend fun delete(entryId: Long)
}
