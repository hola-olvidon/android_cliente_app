package com.holaolvidon.androidclient.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer

/**
 * Se dispara cuando una alarma programada con [AlarmScheduler] llega a su hora.
 * Muestra una notificación (con sonido y vibración) y, si existe `urlAudio`,
 * intenta reproducirlo como refuerzo (best-effort).
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Alarma"
        val audioUrl = intent.getStringExtra(AlarmScheduler.EXTRA_AUDIO)

        NotificationHelper.showAlarmNotification(context, title)

        if (!audioUrl.isNullOrBlank()) {
            playAudio(audioUrl)
        }
    }

    private fun playAudio(audioUrl: String) {
        // `goAsync()` amplía la vida del receiver lo suficiente para un audio corto.
        val pending = goAsync()
        Thread {
            val player = MediaPlayer()
            try {
                player.setDataSource(audioUrl)
                player.prepare()
                player.start()
                player.setOnCompletionListener { mp ->
                    runCatching { mp.release() }
                    pending.finish()
                }
                player.setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    pending.finish()
                    true
                }
            } catch (e: Exception) {
                runCatching { player.release() }
                pending.finish()
            }
        }.start()
    }
}
