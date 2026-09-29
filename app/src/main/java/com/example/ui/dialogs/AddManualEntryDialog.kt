package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.TxnType
import com.example.ui.components.CurrencyFormatUtils
import com.example.ui.components.MAX_MANUAL_AMOUNT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualEntryDialog(
    categories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (type: TxnType, dateMillis: Long, amount: Double, description: String, statementName: String, category: String?) -> Unit
) {
    var type by remember { mutableStateOf(TxnType.DEBIT) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var statementName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var customCategory by remember { mutableStateOf("") }
    var attemptedSave by remember { mutableStateOf(false) }

    val finalCategory = customCategory.trim().ifBlank { selectedCategory }

    val amount = amountText.toDoubleOrNull()
    val amountIsValid = amount != null && amount > 0.0 && amount <= MAX_MANUAL_AMOUNT
    val nameIsValid = statementName.isNotBlank()
    val isValid = amountIsValid && nameIsValid

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == TxnType.DEBIT,
                        onClick = { type = TxnType.DEBIT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("manual_entry_type_debit")
                    ) { Text("Debit") }
                    SegmentedButton(
                        selected = type == TxnType.CREDIT,
                        onClick = { type = TxnType.CREDIT },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("manual_entry_type_credit")
                    ) { Text("Credit") }
                }

                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().testTag("manual_entry_date_field")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatUtils.formatDateShort(selectedDateMillis),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick date")
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    isError = attemptedSave && !amountIsValid,
                    supportingText = {
                        if (attemptedSave && !amountIsValid) {
                            Text("Enter an amount between ₹1 and ${CurrencyFormatUtils.formatInr(MAX_MANUAL_AMOUNT)}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("manual_entry_amount")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("e.g. Groceries, Rent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_entry_description")
                )

                OutlinedTextField(
                    value = statementName,
                    onValueChange = { statementName = it },
                    label = { Text("Statement name") },
                    placeholder = { Text("e.g. Cash Expenses") },
                    singleLine = true,
                    isError = attemptedSave && !nameIsValid,
                    supportingText = {
                        if (attemptedSave && !nameIsValid) Text("Give this entry a name")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("manual_entry_statement_name")
                )

                Text("Category (optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                                customCategory = ""
                            },
                            label = { Text(category) },
                            modifier = Modifier.testTag("manual_entry_category_$category")
                        )
                    }
                }
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = {
                        customCategory = it
                        if (it.isNotBlank()) selectedCategory = null
                    },
                    label = { Text("Or type a custom category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_entry_custom_category")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attemptedSave = true
                    if (isValid) {
                        onSave(
                            type,
                            selectedDateMillis,
                            amount!!,
                            description.ifBlank { "Manual entry" },
                            statementName.trim(),
                            finalCategory
                        )
                    }
                },
                modifier = Modifier.testTag("manual_entry_save")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
