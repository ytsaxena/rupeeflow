package com.example.data.repository

import com.example.data.db.BorrowLendDao
import com.example.data.model.BorrowLendEntity
import kotlinx.coroutines.flow.Flow

class BorrowLendRepository(private val dao: BorrowLendDao) {

    val entries: Flow<List<BorrowLendEntity>> = dao.getAll()

    suspend fun addEntry(entry: BorrowLendEntity): Long = dao.insert(entry)

    suspend fun deleteEntry(entryId: Long) = dao.delete(entryId)
}
