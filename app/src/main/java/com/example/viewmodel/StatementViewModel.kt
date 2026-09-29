package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BankStatementEntity
import com.example.data.model.StatementTransactionEntity
import com.example.data.model.TxnType
import com.example.data.repository.StatementRepository
import com.example.parser.PdfExtractionResult
import com.example.parser.PdfStatementParser
import com.example.ui.components.CurrencyFormatUtils
import com.example.ui.components.DEFAULT_CATEGORIES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class PeriodType { WEEK, MONTH, YEAR }

data class ChartEntry(val label: String, val credit: Double, val debit: Double)

data class PeriodSummary(
    val label: String,
    val totalCredit: Double,
    val totalDebit: Double,
    val transactions: List<StatementTransactionEntity>,
    val chartEntries: List<ChartEntry>
)

sealed interface ImportState {
    data object Idle : ImportState
    data object Loading : ImportState
    data class NeedsPassword(val wrongAttempt: Boolean) : ImportState
    data class Success(val count: Int) : ImportState
    data class Error(val message: String) : ImportState
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Loading : ExportState
    data object Success : ExportState
    data class Error(val message: String) : ExportState
}

class StatementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatementRepository(AppDatabase.getInstance(application).statementDao())

    val statements: StateFlow<List<BankStatementEntity>> = repository.statements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun transactionsForStatement(statementId: Long): Flow<List<StatementTransactionEntity>> =
        repository.transactionsForStatement(statementId)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.statements.first()
            _isLoading.value = false
        }
    }

    private val _periodType = MutableStateFlow(PeriodType.MONTH)
    val periodType: StateFlow<PeriodType> = _periodType.asStateFlow()

    private val _anchorDate = MutableStateFlow(System.currentTimeMillis())
    val anchorDate: StateFlow<Long> = _anchorDate.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    val categories: StateFlow<List<String>> = repository.distinctCategories
        .map { custom -> DEFAULT_CATEGORIES + custom.filter { it !in DEFAULT_CATEGORIES } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_CATEGORIES)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val periodSummary: StateFlow<PeriodSummary> = combine(
        _periodType, _anchorDate
    ) { type, anchor ->
        computeRange(type, anchor)
    }.flatMapLatest { range ->
        repository.transactionsInRange(range.start, range.end).map { transactions -> range to transactions }
    }.combine(_categoryFilter) { (range, transactions), category ->
        val filtered = if (category == null) transactions else transactions.filter { it.category == category }
        buildSummary(filtered, range)
    }.flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            buildSummary(emptyList(), computeRange(PeriodType.MONTH, System.currentTimeMillis()))
        )

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private var pendingUri: Uri? = null

    fun setPeriodType(type: PeriodType) {
        _periodType.value = type
    }

    fun nextPeriod() = shiftPeriod(1)

    fun previousPeriod() = shiftPeriod(-1)

    private fun shiftPeriod(direction: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = _anchorDate.value }
        when (_periodType.value) {
            PeriodType.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, direction)
            PeriodType.MONTH -> cal.add(Calendar.MONTH, direction)
            PeriodType.YEAR -> cal.add(Calendar.YEAR, direction)
        }
        _anchorDate.value = cal.timeInMillis
    }

    fun importPdf(uri: Uri) {
        pendingUri = uri
        runImport(uri, password = null, hadPreviousAttempt = false)
    }

    fun submitPassword(password: String) {
        val uri = pendingUri ?: return
        runImport(uri, password = password, hadPreviousAttempt = true)
    }

    fun dismissImport() {
        pendingUri = null
        _importState.value = ImportState.Idle
    }

    private fun runImport(uri: Uri, password: String?, hadPreviousAttempt: Boolean) {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val extraction = withContext(Dispatchers.IO) {
                PdfStatementParser.extractText(context, uri, password, hadPreviousAttempt)
            }
            when (extraction) {
                is PdfExtractionResult.PasswordRequired -> {
                    _importState.value = ImportState.NeedsPassword(extraction.wrongAttempt)
                }
                is PdfExtractionResult.Error -> {
                    _importState.value = ImportState.Error(extraction.message)
                }
                is PdfExtractionResult.Success -> {
                    val parsed = withContext(Dispatchers.Default) {
                        PdfStatementParser.parseTransactions(extraction.text)
                    }
                    if (parsed.isEmpty()) {
                        _importState.value = ImportState.Error(
                            "Couldn't detect any transactions in this PDF. It may be in an unsupported format."
                        )
                        return@launch
                    }
                    val totalCredit = parsed.filter { it.type == TxnType.CREDIT }.sumOf { it.amount }
                    val totalDebit = parsed.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }
                    val fileName = queryFileName(context, uri) ?: "Statement"
                    val statement = BankStatementEntity(
                        fileName = fileName,
                        uploadedAt = System.currentTimeMillis(),
                        periodStart = parsed.minOf { it.date },
                        periodEnd = parsed.maxOf { it.date },
                        transactionCount = parsed.size,
                        totalCredit = totalCredit,
                        totalDebit = totalDebit
                    )
                    val txnEntities = parsed.map {
                        StatementTransactionEntity(
                            statementId = 0,
                            date = it.date,
                            description = it.description,
                            amount = it.amount,
                            type = it.type,
                            balance = it.balance,
                            category = com.example.parser.CategoryDetector.detect(it.description)
                        )
                    }
                    withContext(Dispatchers.IO) {
                        repository.saveStatement(statement, txnEntities)
                    }
                    pendingUri = null
                    _importState.value = ImportState.Success(parsed.size)
                }
            }
        }
    }

    private fun queryFileName(context: android.content.Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun addManualEntry(
        type: TxnType,
        dateMillis: Long,
        amount: Double,
        description: String,
        statementName: String,
        category: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val statement = BankStatementEntity(
                fileName = statementName,
                uploadedAt = System.currentTimeMillis(),
                periodStart = dateMillis,
                periodEnd = dateMillis,
                transactionCount = 1,
                totalCredit = if (type == TxnType.CREDIT) amount else 0.0,
                totalDebit = if (type == TxnType.DEBIT) amount else 0.0
            )
            val transaction = StatementTransactionEntity(
                statementId = 0,
                date = dateMillis,
                description = description,
                amount = amount,
                type = type,
                balance = null,
                category = category
            )
            repository.saveStatement(statement, listOf(transaction))
        }
    }

    fun setTransactionCategory(transactionId: Long, category: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransactionCategory(transactionId, category)
        }
    }

    /**
     * Runs keyword-based auto-detection over transactions that predate this feature (or were
     * otherwise never categorized) and tags whatever it can confidently match. Returns how many
     * were actually tagged, since not every uncategorized transaction will match a known keyword.
     */
    suspend fun autoCategorizeExisting(): Int {
        val uncategorized = withContext(Dispatchers.IO) { repository.uncategorizedTransactions() }
        var taggedCount = 0
        withContext(Dispatchers.IO) {
            for (txn in uncategorized) {
                val detected = com.example.parser.CategoryDetector.detect(txn.description)
                if (detected != null) {
                    repository.updateTransactionCategory(txn.id, detected)
                    taggedCount++
                }
            }
        }
        return taggedCount
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun exportRange(outputUri: Uri, startMillis: Long, endMillis: Long) {
        _exportState.value = ExportState.Loading
        viewModelScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                repository.transactionsInRange(startMillis, endMillis).first()
            }
            if (transactions.isEmpty()) {
                _exportState.value = ExportState.Error("No transactions found in that date range.")
                return@launch
            }
            val totalCredit = transactions.filter { it.type == TxnType.CREDIT }.sumOf { it.amount }
            val totalDebit = transactions.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }
            val rangeLabel = "${CurrencyFormatUtils.formatDateShort(startMillis)} - ${CurrencyFormatUtils.formatDateShort(endMillis)}"
            val context = getApplication<Application>().applicationContext

            val success = withContext(Dispatchers.IO) {
                com.example.parser.PdfStatementExporter.export(
                    context = context,
                    outputUri = outputUri,
                    rangeLabel = rangeLabel,
                    transactions = transactions.sortedBy { it.date },
                    totalCredit = totalCredit,
                    totalDebit = totalDebit
                )
            }

            _exportState.value = if (success) {
                ExportState.Success
            } else {
                ExportState.Error("Failed to write the PDF file.")
            }
        }
    }

    fun dismissExport() {
        _exportState.value = ExportState.Idle
    }

    fun deleteStatement(statementId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStatement(statementId)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transactionId)
        }
    }

    private fun computeRange(type: PeriodType, anchor: Long): PeriodRange {
        return when (type) {
            PeriodType.WEEK -> {
                val (start, end) = CurrencyFormatUtils.getWeekRange(anchor)
                val lbl = CurrencyFormatUtils.formatWeekRange(start, end)
                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                PeriodRange(type, start, end, lbl, dayLabels)
            }
            PeriodType.MONTH -> {
                val cal = Calendar.getInstance().apply { timeInMillis = anchor }
                val (start, end) = CurrencyFormatUtils.getMonthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                val lbl = CurrencyFormatUtils.formatMonthYear(anchor)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dayLabels = (1..daysInMonth).map { it.toString() }
                PeriodRange(type, start, end, lbl, dayLabels)
            }
            PeriodType.YEAR -> {
                val cal = Calendar.getInstance().apply { timeInMillis = anchor }
                val year = cal.get(Calendar.YEAR)
                val (start, end) = CurrencyFormatUtils.getYearRange(year)
                val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                PeriodRange(type, start, end, year.toString(), monthLabels)
            }
        }
    }

    /**
     * [transactions] is expected to already be scoped to [range] by the DB query (WHERE date
     * BETWEEN ...) so total data volume here stays bounded to one period, not the full history.
     */
    private fun buildSummary(transactions: List<StatementTransactionEntity>, range: PeriodRange): PeriodSummary {
        val creditBuckets = DoubleArray(range.bucketLabels.size)
        val debitBuckets = DoubleArray(range.bucketLabels.size)
        var totalCredit = 0.0
        var totalDebit = 0.0
        val cal = Calendar.getInstance()

        for (txn in transactions) {
            cal.timeInMillis = txn.date
            val bucketIndex = when (range.type) {
                PeriodType.WEEK -> ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
                PeriodType.MONTH -> cal.get(Calendar.DAY_OF_MONTH) - 1
                PeriodType.YEAR -> cal.get(Calendar.MONTH)
            }
            if (txn.type == TxnType.CREDIT) {
                totalCredit += txn.amount
                if (bucketIndex in range.bucketLabels.indices) creditBuckets[bucketIndex] += txn.amount
            } else {
                totalDebit += txn.amount
                if (bucketIndex in range.bucketLabels.indices) debitBuckets[bucketIndex] += txn.amount
            }
        }

        val chartEntries = range.bucketLabels.mapIndexed { i, lbl ->
            ChartEntry(lbl, creditBuckets[i], debitBuckets[i])
        }

        return PeriodSummary(
            label = range.label,
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            // Already ordered by date DESC from the DAO query.
            transactions = transactions,
            chartEntries = chartEntries
        )
    }

    private data class PeriodRange(
        val type: PeriodType,
        val start: Long,
        val end: Long,
        val label: String,
        val bucketLabels: List<String>
    )
}
