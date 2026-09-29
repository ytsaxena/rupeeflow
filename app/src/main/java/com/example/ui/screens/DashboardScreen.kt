package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StatementTransactionEntity
import com.example.ui.components.CreditColor
import com.example.ui.components.CreditDebitPieChart
import com.example.ui.components.CurrencyFormatUtils
import com.example.ui.components.DebitColor
import com.example.ui.components.LoadingState
import com.example.ui.components.PeriodBarChart
import com.example.ui.components.TransactionRow
import com.example.ui.dialogs.CategoryPickerDialog
import com.example.viewmodel.PeriodType
import com.example.viewmodel.StatementViewModel

private const val MAX_INLINE_TRANSACTIONS = 100

private enum class ChartMode { TREND, SPLIT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StatementViewModel,
    onUploadClick: () -> Unit,
    onViewAllTransactions: () -> Unit = {},
    onAutoCategorizeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statements by viewModel.statements.collectAsStateWithLifecycle()
    val periodType by viewModel.periodType.collectAsStateWithLifecycle()
    val summary by viewModel.periodSummary.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    var chartMode by remember { mutableStateOf(ChartMode.TREND) }
    var categoryTarget by remember { mutableStateOf<StatementTransactionEntity?>(null) }

    categoryTarget?.let { target ->
        CategoryPickerDialog(
            categories = categories,
            currentCategory = target.category,
            onDismiss = { categoryTarget = null },
            onSelect = { category ->
                viewModel.setTransactionCategory(target.id, category)
                categoryTarget = null
            }
        )
    }

    if (isLoading) {
        LoadingState(modifier = modifier)
        return
    }

    if (statements.isEmpty()) {
        EmptyDashboard(onUploadClick = onUploadClick, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PeriodType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = periodType == type,
                        onClick = { viewModel.setPeriodType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PeriodType.entries.size),
                        modifier = Modifier.testTag("period_toggle_${type.name}")
                    ) {
                        Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousPeriod() }, modifier = Modifier.testTag("prev_period_button")) {
                    Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = summary.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { viewModel.nextPeriod() }, modifier = Modifier.testTag("next_period_button")) {
                    Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("category_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All") },
                    modifier = Modifier.testTag("category_filter_all")
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = categoryFilter == category,
                        onClick = {
                            viewModel.setCategoryFilter(if (categoryFilter == category) null else category)
                        },
                        label = { Text(category) },
                        modifier = Modifier.testTag("category_filter_$category")
                    )
                }
                IconButton(onClick = onAutoCategorizeClick, modifier = Modifier.testTag("auto_categorize_button")) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Auto-detect categories for uncategorized transactions",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "Credit",
                    amount = summary.totalCredit,
                    color = CreditColor,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Debit",
                    amount = summary.totalDebit,
                    color = DebitColor,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Net",
                    amount = summary.totalCredit - summary.totalDebit,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = chartMode == ChartMode.TREND,
                        onClick = { chartMode = ChartMode.TREND },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {},
                        modifier = Modifier.testTag("chart_mode_trend")
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = "Trend chart", modifier = Modifier.size(16.dp))
                    }
                    SegmentedButton(
                        selected = chartMode == ChartMode.SPLIT,
                        onClick = { chartMode = ChartMode.SPLIT },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {},
                        modifier = Modifier.testTag("chart_mode_split")
                    ) {
                        Icon(imageVector = Icons.Default.PieChart, contentDescription = "Credit vs debit split", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            when (chartMode) {
                ChartMode.TREND -> PeriodBarChart(entries = summary.chartEntries)
                ChartMode.SPLIT -> CreditDebitPieChart(totalCredit = summary.totalCredit, totalDebit = summary.totalDebit)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (summary.transactions.size > MAX_INLINE_TRANSACTIONS) {
                    Text(
                        text = "View all in Statements",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .testTag("dashboard_view_all_transactions")
                            .clickable(onClick = onViewAllTransactions)
                    )
                }
            }
        }

        if (summary.transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "No transactions in this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            val visibleTransactions = summary.transactions.take(MAX_INLINE_TRANSACTIONS)
            items(visibleTransactions, key = { it.id }) { txn ->
                TransactionRow(
                    transaction = txn,
                    onDelete = { viewModel.deleteTransaction(it.id) },
                    onCategoryClick = { categoryTarget = it }
                )
            }
            if (summary.transactions.size > MAX_INLINE_TRANSACTIONS) {
                item {
                    Text(
                        text = "Showing most recent $MAX_INLINE_TRANSACTIONS of ${summary.transactions.size} — see the Statements tab for the full breakdown",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = CurrencyFormatUtils.formatInr(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun EmptyDashboard(onUploadClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.UploadFile,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No statements yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Upload a bank statement PDF to see your credit, debit and spending trends here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onUploadClick, modifier = Modifier.testTag("empty_upload_button")) {
            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Upload your first statement")
        }
    }
}
