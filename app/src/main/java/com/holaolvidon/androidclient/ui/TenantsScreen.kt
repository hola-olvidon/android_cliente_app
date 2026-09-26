package com.holaolvidon.androidclient.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.holaolvidon.androidclient.data.TenantSummary

/** Lista de tenants; al hacer click en uno se abre su detalle de alarmas. */
@Composable
fun TenantsScreen(state: UiState, viewModel: AppViewModel, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tenants", style = MaterialTheme.typography.titleLarge)
                state.lastUpdated?.let { timestamp ->
                    Text(
                        "Actualizado: ${formatTime(timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.serverTimeZone?.let { zone ->
                    Text(
                        "Zona horaria del servidor: $zone — las alarmas suenan según esta zona.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = viewModel::refresh) { Text("Actualizar") }
            TextButton(onClick = viewModel::openSettings) { Text("Ajustes") }
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

        if (state.tenants.isEmpty() && !state.loading) {
            Text(
                text = "No hay tenants disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(state.tenants, key = { it.id }) { tenant ->
                    TenantRow(
                        tenant = tenant,
                        label = subscriptionLabel(state, tenant),
                        onClick = { viewModel.openTenant(tenant.id) },
                    )
                }
            }
        }
    }
}

private fun subscriptionLabel(state: UiState, tenant: TenantSummary): String? = when {
    tenant.id in state.subscribedTenantIds -> "Todas"
    else -> state.manualSubscriptions[tenant.id]
        ?.takeIf { it.isNotEmpty() }
        ?.let { "${it.size} alarmas" }
}

@Composable
private fun TenantRow(tenant: TenantSummary, label: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tenant.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
