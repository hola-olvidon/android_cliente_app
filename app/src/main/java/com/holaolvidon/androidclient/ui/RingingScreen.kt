package com.holaolvidon.androidclient.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Pantalla a pantalla completa con el botón grande "Detener". Se usa tanto en el overlay de la app
 * (cuando está en primer plano) como en [com.holaolvidon.androidclient.alarm.AlarmRingingActivity]
 * (full-screen intent sobre la pantalla de bloqueo).
 *
 * Se envuelve en un [Surface] para que el color del texto se adapte al tema (claro/oscuro): el
 * [Surface] provee `contentColor = onSurface`, de modo que las letras contrastan con el fondo en
 * ambos temas.
 */
@Composable
fun RingingScreen(title: String, onStop: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            ) {
                Text(
                    "⏰ Alarma sonando",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Detener", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
