package com.holaolvidon.androidclient.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/** Conmuta entre conexión, ajustes, lista de tenants y detalle de tenant; muestra mensajes. */
@Composable
fun AppRoot(state: UiState, viewModel: AppViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when {
            !state.connected -> ConnectScreen(state = state, viewModel = viewModel, padding = padding)
            state.showSettings -> SettingsScreen(state = state, viewModel = viewModel, padding = padding)
            state.selectedTenantId != null ->
                TenantDetailScreen(state = state, viewModel = viewModel, padding = padding)
            else -> TenantsScreen(state = state, viewModel = viewModel, padding = padding)
        }
    }
}
