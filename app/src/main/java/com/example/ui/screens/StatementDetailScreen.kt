package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.CurrencyFormatUtils
import com.example.ui.components.DebitColor
import com.example.ui.components.LoadingState
import com.example.ui.components.TransactionRow
import com.example.ui.dialogs.CategoryPickerDialog
import com.example.viewmodel.StatementViewModel

@Composable
fun StatementDetailScreen(
    viewModel: StatementViewModel,
    statementId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statements by viewModel.statements.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val statement = statements.find { it.id == statementId }
    val transactions by remember(statementId) { viewModel.transactionsForStatement(statementId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    if (isLoading) {
        LoadingState(modifier = modifier)
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this statement?") },
            text = { Text("This will permanently remove the statement and all ${transactions.size} of its transactions from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStatement(statementId)
                        showDeleteConfirm = false
                        onBack()
                    },
                    modifier = Modifier.testTag("confirm_delete_statement")
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("statement_detail_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("statement_detail_back")) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = statement?.fileName ?: "Statement",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        if (statement != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryColumn("Credit", statement.totalCredit, CreditColor)
                        SummaryColumn("Debit", statement.totalDebit, DebitColor)
                        SummaryColumn("Records", null, MaterialTheme.colorScheme.primary, "${statement.transactionCount}")
                    }
                }
            }

            item {
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("delete_statement_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Delete Statement")
                }
            }
        }

        items(transactions, key = { it.id }) { txn ->
            TransactionRow(
                transaction = txn,
                onDelete = { viewModel.deleteTransaction(it.id) },
                onCategoryClick = { categoryTarget = it }
            )
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
private fun SummaryColumn(title: String, amount: Double?, color: Color, overrideText: String? = null) {
    androidx.compose.foundation.layout.Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = overrideText ?: CurrencyFormatUtils.formatInr(amount ?: 0.0),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
