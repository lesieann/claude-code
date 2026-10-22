package com.lesieann.passvault.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lesieann.passvault.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 回傳一個啟動「選擇備份檔 → 輸入備份主密碼 → 還原」流程的函式。 */
@Composable
fun rememberBackupImporter(vm: VaultViewModel): () -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pending = runCatching {
            context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        }.getOrElse {
            vm.message = "無法讀取檔案"
            null
        }
    }
    pending?.let { bytes ->
        PasswordPromptDialog(
            title = "還原備份",
            text = "請輸入建立此備份時的主密碼。還原後會取代目前裝置上的保險箱。",
            onConfirm = { pw ->
                pending = null
                vm.importBackup(bytes, pw)
            },
            onDismiss = { pending = null },
        )
    }
    return { launcher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
}

@Composable
fun rememberBackupExporter(vm: VaultViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        vm.endFilePicker()
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = vm.exportBackup() ?: run {
            vm.message = "保險箱已上鎖，請解鎖後再匯出"
            return@rememberLauncherForActivityResult
        }
        vm.message = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) }
            "已匯出加密備份"
        }.getOrElse { "匯出失敗：${it.message}" }
    }
    return {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        vm.beginFilePicker()
        launcher.launch("passvault-backup-$date.json")
    }
}

/** 選擇 Google 密碼管理工具／Chrome 匯出的 CSV 檔，解析後交給 ViewModel 顯示確認畫面。 */
@Composable
fun rememberCsvImporter(vm: VaultViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        vm.endFilePicker()
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }
            .onSuccess(vm::prepareCsvImport)
            .onFailure { vm.message = "無法讀取檔案" }
    }
    return {
        vm.beginFilePicker()
        launcher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream"))
    }
}
