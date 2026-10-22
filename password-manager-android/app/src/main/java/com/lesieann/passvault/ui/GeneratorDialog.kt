package com.lesieann.passvault.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lesieann.passvault.util.PasswordGenerator

@Composable
fun GeneratorDialog(onUse: (String) -> Unit, onDismiss: () -> Unit) {
    var options by remember { mutableStateOf(PasswordGenerator.Options()) }
    var seed by remember { mutableIntStateOf(0) }
    val noneSelected = !(options.upper || options.lower || options.digits || options.symbols)
    // seed 改變時重新產生
    val generated = remember(options, seed) {
        if (noneSelected) "" else PasswordGenerator.generate(options)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("產生密碼") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        generated.ifEmpty { "請至少選擇一種字元" },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { seed++ }) { Icon(Icons.Filled.Refresh, "重新產生") }
                }
                StrengthMeter(generated)
                Spacer(Modifier.height(12.dp))
                Text("長度：${options.length}")
                Slider(
                    value = options.length.toFloat(),
                    onValueChange = { options = options.copy(length = it.toInt()) },
                    valueRange = 8f..64f,
                    steps = 64 - 8 - 1,
                )
                Option("大寫字母 A-Z", options.upper) { options = options.copy(upper = it) }
                Option("小寫字母 a-z", options.lower) { options = options.copy(lower = it) }
                Option("數字 0-9", options.digits) { options = options.copy(digits = it) }
                Option("符號 !@#…", options.symbols) { options = options.copy(symbols = it) }
                Option("排除易混淆字元 (Il1O0o)", options.avoidAmbiguous) { options = options.copy(avoidAmbiguous = it) }
            }
        },
        confirmButton = { TextButton(onClick = { onUse(generated) }, enabled = generated.isNotEmpty()) { Text("使用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun Option(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange)
        Text(label)
    }
}
