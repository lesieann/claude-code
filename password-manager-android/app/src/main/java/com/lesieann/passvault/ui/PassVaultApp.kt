package com.lesieann.passvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.lesieann.passvault.Screen
import com.lesieann.passvault.VaultViewModel

@Composable
fun PassVaultApp(vm: VaultViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val message = vm.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.message = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (val screen = vm.screen) {
            Screen.Setup -> SetupScreen(vm)
            Screen.Locked -> UnlockScreen(vm)
            Screen.List -> EntryListScreen(vm)
            is Screen.Edit -> EntryEditScreen(vm, screen.entryId)
            Screen.Settings -> SettingsScreen(vm)
        }
        if (vm.offerBiometric && vm.screen == Screen.List && vm.pendingImport == null) {
            BiometricOfferDialog(vm)
        }
        vm.pendingImport?.let { pending ->
            CsvImportDialog(pending, onConfirm = vm::confirmCsvImport, onDismiss = vm::cancelCsvImport)
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding(),
        )
        if (vm.busy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    // 擋住底下的點擊
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun BiometricOfferDialog(vm: VaultViewModel) {
    val activity = LocalContext.current as FragmentActivity
    // 手機沒有設定指紋（或只有不夠安全的臉部解鎖）就不問
    if (!remember { BiometricAuth.isAvailable(activity) }) return
    AlertDialog(
        onDismissRequest = { vm.dismissBiometricOffer(neverAskAgain = false) },
        icon = { Icon(Icons.Filled.Fingerprint, null) },
        title = { Text("用指紋解鎖？") },
        text = { Text("之後打開 App 只要按指紋就能解鎖，不用每次輸入主密碼。主密碼仍然可以使用，也可以隨時在設定裡關閉。") },
        confirmButton = {
            TextButton(onClick = {
                vm.dismissBiometricOffer(neverAskAgain = false)
                BiometricAuth.enroll(activity, vm)
            }) { Text("啟用") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { vm.dismissBiometricOffer(neverAskAgain = true) }) { Text("不要再問") }
                TextButton(onClick = { vm.dismissBiometricOffer(neverAskAgain = false) }) { Text("以後再說") }
            }
        },
    )
}
