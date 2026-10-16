package com.lesieann.passvault.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilterChip
import com.lesieann.passvault.data.Categories
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lesieann.passvault.VaultViewModel
import com.lesieann.passvault.data.VaultEntry
import com.lesieann.passvault.util.SecureClipboard
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(vm: VaultViewModel, entryId: String?) {
    val context = LocalContext.current
    val original = remember(entryId) { entryId?.let(vm::entry) ?: VaultEntry() }
    var title by remember { mutableStateOf(original.title) }
    var username by remember { mutableStateOf(original.username) }
    var password by remember { mutableStateOf(original.password) }
    var url by remember { mutableStateOf(original.url) }
    var notes by remember { mutableStateOf(original.notes) }
    var category by remember { mutableStateOf(original.category) }
    val suggestions = remember(vm.entries) { Categories.suggestions(vm.entries) }
    var showGenerator by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val current = original.copy(title = title.trim(), username = username, password = password, url = url.trim(), notes = notes, category = category.trim())
    val dirty = current.copy(updatedAt = 0) != original.copy(updatedAt = 0)
    val goBack: () -> Unit = {
        if (dirty) {
            confirmDiscard = true
        } else {
            vm.backToList()
        }
    }
    BackHandler(onBack = goBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "新增項目" else "編輯項目") },
                navigationIcon = {
                    IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    if (entryId != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "刪除") }
                    }
                    IconButton(onClick = { vm.save(current) }) { Icon(Icons.Filled.Check, "儲存") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("名稱 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                username, { username = it },
                label = { Text("帳號 / Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                trailingIcon = {
                    if (username.isNotEmpty()) IconButton(onClick = {
                        SecureClipboard.copy(context, "帳號", username, sensitive = false)
                        vm.message = "已複製帳號"
                    }) { Icon(Icons.Filled.ContentCopy, "複製帳號") }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            PasswordField(password, { password = it }, "密碼") {
                IconButton(onClick = { showGenerator = true }) { Icon(Icons.Filled.Password, "產生密碼") }
                if (password.isNotEmpty()) IconButton(onClick = {
                    SecureClipboard.copy(context, "密碼", password, sensitive = true)
                    vm.message = "已複製密碼，${SecureClipboard.CLEAR_AFTER_MS / 1000} 秒後自動清除"
                }) { Icon(Icons.Filled.ContentCopy, "複製密碼") }
            }
            StrengthMeter(password)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                url, { url = it },
                label = { Text("網址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                category, { category = it },
                label = { Text("分類") },
                placeholder = { Text(Categories.UNCATEGORIZED_LABEL) },
                singleLine = true,
                trailingIcon = {
                    if (category.isNotEmpty()) IconButton(onClick = { category = "" }) { Icon(Icons.Filled.Clear, "清除分類") }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                items(suggestions, key = { it }) { c ->
                    FilterChip(
                        selected = category.trim() == c,
                        onClick = { category = if (category.trim() == c) "" else c },
                        label = { Text(c) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(notes, { notes = it }, label = { Text("備註") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            if (entryId != null) {
                Spacer(Modifier.height(16.dp))
                Hint("上次修改：" + DateFormat.getDateTimeInstance().format(Date(original.updatedAt)))
            }
        }
    }

    if (showGenerator) {
        GeneratorDialog(onUse = { password = it; showGenerator = false }, onDismiss = { showGenerator = false })
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "刪除「${original.title}」？",
            text = "此動作無法復原。",
            confirmLabel = "刪除",
            onConfirm = { confirmDelete = false; vm.delete(original.id) },
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "放棄變更？",
            text = "尚未儲存的修改將會遺失。",
            confirmLabel = "放棄",
            onConfirm = { confirmDiscard = false; vm.backToList() },
            onDismiss = { confirmDiscard = false },
        )
    }
}
