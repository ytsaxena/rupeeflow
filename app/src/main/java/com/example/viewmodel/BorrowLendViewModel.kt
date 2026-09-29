package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BorrowLendEntity
import com.example.data.model.BorrowLendType
import com.example.data.repository.BorrowLendRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Deliberately separate from [StatementViewModel] — this ledger has no bearing on statement totals. */
class BorrowLendViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BorrowLendRepository(AppDatabase.getInstance(application).borrowLendDao())

    val entries: StateFlow<List<BorrowLendEntity>> = repository.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(personInfo: String, amount: Double, type: BorrowLendType, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addEntry(
                BorrowLendEntity(personInfo = personInfo, amount = amount, type = type, date = date)
            )
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(entryId)
        }
    }
}
