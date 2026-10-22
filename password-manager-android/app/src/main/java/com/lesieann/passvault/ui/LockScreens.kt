package com.lesieann.passvault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
private fun LockLayout(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Lock, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Hint(subtitle)
            Spacer(Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
fun SetupScreen(vm: VaultViewModel) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val restore = rememberBackupImporter(vm)

    LockLayout(
        title = "建立保險箱",
        subtitle = "設定一組主密碼來加密你的所有資料。主密碼無法找回，忘記就無法解鎖，請務必牢記。",
    ) {
        PasswordField(password, { password = it }, "主密碼（至少 ${VaultViewModel.MIN_MASTER_LENGTH} 個字元）")
        StrengthMeter(password)
        Spacer(Modifier.height(12.dp))
        PasswordField(
            confirm, { confirm = it }, "再次輸入主密碼",
            imeAction = ImeAction.Done,
            onDone = { vm.create(password, confirm) },
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.create(password, confirm) }, modifier = Modifier.fillMaxWidth()) {
            Text("建立")
        }
        TextButton(onClick = restore) { Text("從備份還原") }
    }
}

@Composable
fun UnlockScreen(vm: VaultViewModel) {
    var password by remember { mutableStateOf("") }
    val activity = LocalContext.current as FragmentActivity
    val biometricUsable = vm.biometricEnabled && BiometricAuth.isAvailable(activity)

    val startBiometric = {
        vm.biometricUnlockCipher()?.let { cipher ->
            BiometricAuth.authenticate(
                activity, "解鎖密碼保險箱", cipher,
                onSuccess = vm::completeBiometricUnlock,
                onError = { vm.message = it },
            )
        }
    }
    // 進入解鎖畫面時自動跳出生物辨識
    LaunchedEffect(Unit) { if (biometricUsable) startBiometric() }

    LockLayout(title = "保險箱已上鎖", subtitle = "輸入主密碼以解鎖") {
        PasswordField(
            password, { password = it }, "主密碼",
            imeAction = ImeAction.Done,
            onDone = { vm.unlock(password) },
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.unlock(password) }, enabled = password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("解鎖")
        }
        if (biometricUsable) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { startBiometric() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Fingerprint, null)
                Spacer(Modifier.size(8.dp))
                Text("使用指紋解鎖")
            }
        }
    }
}
