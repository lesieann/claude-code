package com.lesieann.passvault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lesieann.passvault.VaultViewModel
import com.lesieann.passvault.data.Categories

@Composable
fun CsvImportDialog(pending: VaultViewModel.PendingImport, onConfirm: (category: String) -> Unit, onDismiss: () -> Unit) {
    var category by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("匯入 ${pending.entries.size} 筆密碼？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val notes = buildList {
                    if (pending.duplicates > 0) add("${pending.duplicates} 筆已經在保險箱裡，會略過")
                    if (pending.skippedRows > 0) add("${pending.skippedRows} 行沒有帳號也沒有密碼，會略過")
                }
                notes.forEach { Hint("• $it") }
                Hint("前幾筆：" + pending.entries.take(3).joinToString("、") { it.title })
                OutlinedTextField(
                    category, { category = it },
                    label = { Text("放進分類（可留空）") },
                    placeholder = { Text(Categories.UNCATEGORIZED_LABEL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(category) }) { Text("匯入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
