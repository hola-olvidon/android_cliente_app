package com.holaolvidon.androidclient.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.holaolvidon.androidclient.ui.RingingScreen
import com.holaolvidon.androidclient.ui.theme.AndroidClientTheme

/**
 * Pantalla a pantalla completa que se muestra al disparar una alarma (full-screen intent), también
 * sobre la pantalla de bloqueo. Ofrece un botón grande "Detener" para apagar la alarma.
 */
class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mostrar sobre la pantalla de bloqueo y encender la pantalla (como la app Reloj).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Alarma"

        setContent {
            AndroidClientTheme {
                RingingScreen(
                    title = title,
                    onStop = {
                        stopService(
                            Intent(this, AlarmRingService::class.java)
                                .setAction(AlarmRingService.ACTION_STOP),
                        )
                        finish()
                    },
                )
            }
        }
    }
}
