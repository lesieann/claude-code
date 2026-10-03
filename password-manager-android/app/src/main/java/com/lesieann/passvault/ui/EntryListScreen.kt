package com.lesieann.passvault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lesieann.passvault.VaultViewModel
import com.lesieann.passvault.data.VaultEntry
import com.lesieann.passvault.util.SecureClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(vm: VaultViewModel) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val all = vm.entries
    val filtered = remember(all, query) {
        val q = query.trim()
        all.filter {
            q.isEmpty() || it.title.contains(q, true) || it.username.contains(q, true) || it.url.contains(q, true)
        }.sortedBy { it.title.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("密碼保險箱") },
                actions = {
                    IconButton(onClick = vm::lock) { Icon(Icons.Filled.Lock, "上鎖") }
                    IconButton(onClick = vm::openSettings) { Icon(Icons.Filled.Settings, "設定") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openEntry(null) }) { Icon(Icons.Filled.Add, "新增") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜尋名稱、帳號或網址") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "清除") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Hint(if (all.isEmpty()) "還沒有任何密碼，點右下角 + 新增第一筆。" else "找不到符合的項目")
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(filtered, key = { it.id }) { entry ->
                        EntryRow(
                            entry,
                            onClick = { vm.openEntry(entry.id) },
                            onCopyUser = {
                                SecureClipboard.copy(context, "帳號", entry.username, sensitive = false)
                                vm.message = "已複製帳號"
                            },
                            onCopyPassword = {
                                SecureClipboard.copy(context, "密碼", entry.password, sensitive = true)
                                vm.message = "已複製密碼，${SecureClipboard.CLEAR_AFTER_MS / 1000} 秒後自動清除"
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: VaultEntry,
    onClick: () -> Unit,
    onCopyUser: () -> Unit,
    onCopyPassword: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val sub = listOf(entry.username, entry.url).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    entry.title.trim().take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = {
            Row {
                if (entry.username.isNotEmpty()) {
                    IconButton(onClick = onCopyUser) { Icon(Icons.Filled.Person, "複製帳號") }
                }
                if (entry.password.isNotEmpty()) {
                    IconButton(onClick = onCopyPassword) { Icon(Icons.Filled.Key, "複製密碼") }
                }
            }
        },
    )
}
