package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun CategoryPickerDialog(
    categories: List<String>,
    currentCategory: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    var customText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = category == currentCategory,
                            onClick = { onSelect(category) },
                            label = { Text(category) },
                            modifier = Modifier.testTag("category_option_$category")
                        )
                    }
                }

                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Custom category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_category_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (customText.isNotBlank()) onSelect(customText.trim()) },
                modifier = Modifier.testTag("custom_category_add")
            ) { Text("Add Custom") }
        },
        dismissButton = {
            Row {
                if (currentCategory != null) {
                    TextButton(onClick = { onSelect(null) }) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
