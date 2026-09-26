package com.holaolvidon.androidclient.ui

import androidx.compose.runtime.Composable

/** Conmuta entre la pantalla de configuración y la lista de alarmas según el estado de conexión. */
@Composable
fun AppRoot(state: UiState, viewModel: AppViewModel) {
    if (state.connected) {
        AlarmsScreen(
            state = state,
            onRefresh = viewModel::refresh,
            onDisconnect = viewModel::disconnect,
        )
    } else {
        SettingsScreen(state = state, viewModel = viewModel)
    }
}
