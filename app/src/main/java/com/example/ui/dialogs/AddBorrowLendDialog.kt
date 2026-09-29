package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.model.BorrowLendType
import com.example.ui.components.CurrencyFormatUtils
import com.example.ui.components.MAX_MANUAL_AMOUNT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBorrowLendDialog(
    onDismiss: () -> Unit,
    onSave: (personInfo: String, amount: Double, type: BorrowLendType, dateMillis: Long) -> Unit
) {
    var type by remember { mutableStateOf(BorrowLendType.LENT) }
    var personInfo by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull()
    val amountIsValid = amount != null && amount > 0.0 && amount <= MAX_MANUAL_AMOUNT
    val infoIsValid = personInfo.isNotBlank()
    val isValid = amountIsValid && infoIsValid

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
        title = { Text("Add Borrow / Lend Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == BorrowLendType.LENT,
                        onClick = { type = BorrowLendType.LENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("borrow_lend_type_lent")
                    ) { Text("Lent") }
                    SegmentedButton(
                        selected = type == BorrowLendType.BORROWED,
                        onClick = { type = BorrowLendType.BORROWED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("borrow_lend_type_borrowed")
                    ) { Text("Borrowed") }
                }

                OutlinedTextField(
                    value = personInfo,
                    onValueChange = { personInfo = it },
                    label = { Text("Who / Info") },
                    placeholder = { Text("e.g. Rahul, office loan") },
                    singleLine = true,
                    isError = attemptedSave && !infoIsValid,
                    supportingText = {
                        if (attemptedSave && !infoIsValid) Text("Enter who this is with")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("borrow_lend_info")
                )

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
                    modifier = Modifier.fillMaxWidth().testTag("borrow_lend_amount")
                )

                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().testTag("borrow_lend_date_field")
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attemptedSave = true
                    if (isValid) {
                        onSave(personInfo.trim(), amount!!, type, selectedDateMillis)
                    }
                },
                modifier = Modifier.testTag("borrow_lend_save")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
