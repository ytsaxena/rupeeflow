package com.example.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.CurrencyFormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDateRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val state = rememberDateRangePickerState()
    val startMillis = state.selectedStartDateMillis
    val endMillis = state.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (startMillis != null && endMillis != null) {
                        onConfirm(startMillis, endMillis)
                    }
                },
                enabled = startMillis != null && endMillis != null,
                modifier = Modifier.testTag("export_range_confirm")
            ) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            title = {
                Text(
                    text = "Select statement date range",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            headline = {
                val label = if (startMillis != null && endMillis != null) {
                    "${CurrencyFormatUtils.formatDateShort(startMillis)} - ${CurrencyFormatUtils.formatDateShort(endMillis)}"
                } else {
                    "Pick a start and end date"
                }
                Text(text = label, modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 8.dp))
            }
        )
    }
}
