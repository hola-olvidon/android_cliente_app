package com.holaolvidon.androidclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Permite revisar/editar la URL y la clave, reconectar o desconectar. */
@Composable
fun SettingsScreen(state: UiState, viewModel: AppViewModel, padding: PaddingValues) {
    var url by remember { mutableStateOf(state.baseUrl) }
    var key by remember { mutableStateOf(state.apiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = viewModel::closeSettings) { Text("Volver") }
            Text("Ajustes", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL del servidor") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Clave de API (X-API-KEY)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.updateBaseUrl(url)
                viewModel.updateApiKey(key)
                viewModel.closeSettings()
                viewModel.connect()
            },
            enabled = url.isNotBlank() && key.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Guardar y reconectar")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = viewModel::disconnect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Desconectar")
        }
    }
}
