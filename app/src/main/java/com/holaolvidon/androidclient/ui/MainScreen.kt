package com.holaolvidon.androidclient.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.holaolvidon.androidclient.data.Alarm
import com.holaolvidon.androidclient.data.TenantSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(state: UiState, viewModel: AppViewModel) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Alarmas", style = MaterialTheme.typography.titleLarge)
                    state.lastUpdated?.let { timestamp ->
                        Text(
                            "Actualizado: ${formatTime(timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = viewModel::refreshAlarms) { Text("Actualizar") }
                TextButton(onClick = viewModel::disconnect) { Text("Ajustes") }
            }

            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item { SectionTitle("Tenants") }
                items(state.tenants, key = { it.id }) { tenant ->
                    TenantRow(
                        tenant = tenant,
                        selected = tenant.id in state.selectedTenantIds,
                        onToggle = { viewModel.toggleTenant(tenant.id) },
                    )
                }

                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { SectionTitle("Alarmas") }

                when {
                    state.selectedTenantIds.isEmpty() -> item {
                        HintText("Selecciona al menos un tenant para ver sus alarmas.")
                    }
                    state.alarms.isEmpty() && !state.loading -> item {
                        HintText("Los tenants seleccionados no tienen alarmas.")
                    }
                    else -> items(state.alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            tenantName = state.tenantNames[alarm.tenantId] ?: alarm.tenantId,
                            subscribed = alarm.id in state.subscribedAlarmIds,
                            onToggle = { viewModel.toggleAlarm(alarm.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TenantRow(
    tenant: TenantSummary,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Text(tenant.nombre, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    tenantName: String,
    subscribed: Boolean,
    onToggle: () -> Unit,
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
                    "$tenantName · ${formatTime(alarm.horaProgramada)}",
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
            Switch(checked = subscribed, onCheckedChange = { onToggle() })
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}
