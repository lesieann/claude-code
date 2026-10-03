package com.lesieann.passvault.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lesieann.passvault.VaultViewModel
import com.lesieann.passvault.util.SecureClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: VaultViewModel) {
    val activity = LocalContext.current as FragmentActivity
    val biometricAvailable = remember { BiometricAuth.isAvailable(activity) }
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }
    val export = rememberBackupExporter(vm)
    val restore = rememberBackupImporter(vm)

    BackHandler(onBack = vm::backToList)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = vm::backToList) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
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
            Section("生物辨識")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("使用指紋 / 臉部解鎖")
                    Hint(if (biometricAvailable) "金鑰保存在 Android Keystore，新增生物辨識資料後需重新啟用" else "此裝置未設定強生物辨識")
                }
                Switch(
                    checked = vm.biometricEnabled,
                    enabled = biometricAvailable,
                    onCheckedChange = { on ->
                        if (on) {
                            runCatching { vm.biometricEnrollCipher() }
                                .onSuccess { cipher ->
                                    BiometricAuth.authenticate(
                                        activity, "啟用生物辨識解鎖", cipher,
                                        onSuccess = vm::completeBiometricEnroll,
                                        onError = { vm.message = it },
                                    )
                                }
                                .onFailure { vm.message = "無法建立金鑰：${it.message}" }
                        } else {
                            vm.disableBiometric()
                        }
                    },
                )
            }

            Divider()
            Section("變更主密碼")
            PasswordField(current, { current = it }, "目前的主密碼")
            Spacer(Modifier.height(8.dp))
            PasswordField(new, { new = it }, "新的主密碼")
            StrengthMeter(new)
            Spacer(Modifier.height(8.dp))
            PasswordField(confirm, { confirm = it }, "再次輸入新的主密碼", imeAction = ImeAction.Done)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    vm.changeMasterPassword(current, new, confirm)
                    if (new == confirm && new.length >= VaultViewModel.MIN_MASTER_LENGTH) {
                        current = ""; new = ""; confirm = ""
                    }
                },
                enabled = current.isNotEmpty() && new.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("變更主密碼") }
            Hint("變更後需要重新啟用生物辨識。", Modifier.padding(top = 4.dp))

            Divider()
            Section("備份")
            Hint("備份檔仍以主密碼加密，可存到雲端硬碟或電腦。還原時需要當時的主密碼。")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = export, modifier = Modifier.fillMaxWidth()) { Text("匯出加密備份") }
            OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("從備份還原") }

            Divider()
            Section("安全性")
            Hint(
                "• App 進入背景超過 ${VaultViewModel.AUTO_LOCK_MS / 1000} 秒會自動上鎖\n" +
                    "• 複製的密碼會在 ${SecureClipboard.CLEAR_AFTER_MS / 1000} 秒後從剪貼簿清除\n" +
                    "• 禁止截圖，且不會出現在最近使用的 App 預覽中\n" +
                    "• 資料只存在本機，App 不需要網路權限，也不參與系統備份",
            )
        }
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "從備份還原？",
            text = "目前保險箱中的所有資料將被備份檔取代。建議先匯出目前的備份。",
            confirmLabel = "選擇備份檔",
            onConfirm = { confirmRestore = false; restore() },
            onDismiss = { confirmRestore = false },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = 20.dp))
}
