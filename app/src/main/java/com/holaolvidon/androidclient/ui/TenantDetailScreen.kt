package com.holaolvidon.androidclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.holaolvidon.androidclient.data.Alarm

/** Detalle de un tenant: toggle "todas las alarmas" (por defecto) o selección manual. */
@Composable
fun TenantDetailScreen(state: UiState, viewModel: AppViewModel, padding: PaddingValues) {
    val tenantId = state.selectedTenantId
    val tenantName = tenantId?.let { id ->
        state.tenants.firstOrNull { it.id == id }?.nombre ?: id
    } ?: ""
    val subscribeAll = tenantId?.let { it in state.subscribedTenantIds } ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = viewModel::closeTenant) { Text("Volver") }
            Text(
                text = tenantName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Suscribirme a todas las alarmas",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = subscribeAll,
                onCheckedChange = { enabled -> tenantId?.let { viewModel.toggleSubscribeAll(it, enabled) } },
            )
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.tenantAlarms.isEmpty()) {
            Text(
                text = "Este tenant no tiene alarmas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(state.tenantAlarms, key = { it.id }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        subscribeAll = subscribeAll,
                        manualChecked = tenantId?.let { alarm.id in (state.manualSubscriptions[it] ?: emptySet()) } ?: false,
                        onToggle = { enabled -> tenantId?.let { viewModel.toggleAlarm(it, alarm.id, enabled) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    subscribeAll: Boolean,
    manualChecked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(alarm.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    alarm.recurrencia?.let { formatRecurrence(it) }
                        ?: formatTime(alarm.horaProgramada),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!alarm.activa) {
                    Text(
                        "Inactiva",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            if (subscribeAll) {
                Switch(checked = true, enabled = false, onCheckedChange = {})
            } else {
                Switch(checked = manualChecked, onCheckedChange = onToggle)
            }
        }
    }
}
