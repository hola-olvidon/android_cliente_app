package com.holaolvidon.androidclient.ui

import androidx.compose.runtime.Composable

/** Conmuta entre la pantalla de conexión y la principal según el estado de conexión. */
@Composable
fun AppRoot(state: UiState, viewModel: AppViewModel) {
    if (state.connected) {
        MainScreen(state = state, viewModel = viewModel)
    } else {
        ConnectScreen(state = state, viewModel = viewModel)
    }
}
