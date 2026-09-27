package com.holaolvidon.androidclient.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

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

    // El botón "atrás" del celular replica el botón "Volver": sale del detalle de tenant o de
    // ajustes en lugar de minimizar la app.
    BackHandler(enabled = state.showSettings || state.selectedTenantId != null) {
        if (state.showSettings) viewModel.closeSettings()
        else if (state.selectedTenantId != null) viewModel.closeTenant()
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            when {
                !state.connected -> ConnectScreen(state = state, viewModel = viewModel, padding = padding)
                state.showSettings -> SettingsScreen(state = state, viewModel = viewModel, padding = padding)
                state.selectedTenantId != null ->
                    TenantDetailScreen(state = state, viewModel = viewModel, padding = padding)
                else -> TenantsScreen(state = state, viewModel = viewModel, padding = padding)
            }
        }

        // Overlay a pantalla completa para detener la alarma cuando la app está en primer plano
        // (funciona incluso sin permiso de notificaciones).
        state.ringingAlarmTitle?.let { title ->
            RingingScreen(title = title, onStop = viewModel::stopRinging)
        }
    }
}
