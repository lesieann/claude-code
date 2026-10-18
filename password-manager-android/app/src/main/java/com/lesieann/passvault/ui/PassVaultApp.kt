package com.lesieann.passvault.ui

import androidx.compose.foundation.background
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
