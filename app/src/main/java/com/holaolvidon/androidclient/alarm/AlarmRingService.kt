package com.holaolvidon.androidclient.alarm

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder

/**
 * Servicio en primer plano que suena cuando una alarma se dispara. Reproduce el audio de la alarma
 * desde el archivo local (descargándolo una única vez si hiciera falta) y, si no hay audio, el tono
 * de alarma por defecto. La notificación incluye una acción para detenerlo.
 */
class AlarmRingService : Service() {

    @Volatile
    private var stopped = false
    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Alarma"
        val audioUrl = intent?.getStringExtra(AlarmScheduler.EXTRA_AUDIO)
        val baseUrl = intent?.getStringExtra(AlarmScheduler.EXTRA_BASE_URL) ?: ""
        val apiKey = intent?.getStringExtra(AlarmScheduler.EXTRA_API_KEY) ?: ""

        startForeground(NOTIFICATION_ID, NotificationHelper.buildRingingNotification(this, title))

        Thread {
            val local = audioUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { AudioCache(this).getOrDownload(baseUrl, apiKey, it) }

            if (stopped) return@Thread

            if (local != null) {
                playFile(local.absolutePath)
            } else {
                playDefault()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun playFile(path: String) {
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(alarmAudioAttributes())
            mp.setDataSource(path)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            mp.setOnErrorListener { _, _, _ ->
                playDefault()
                true
            }
        } catch (e: Exception) {
            runCatching { mp.release() }
            playDefault()
        }
    }

    private fun playDefault() {
        if (stopped) return
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(alarmAudioAttributes())
            mp.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            mp.isLooping = true
            mp.prepare()
            mp.start()
            mp.setOnErrorListener { _, _, _ ->
                stopSelf()
                true
            }
        } catch (e: Exception) {
            runCatching { mp.release() }
            stopSelf()
        }
    }

    private fun alarmAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    override fun onDestroy() {
        stopped = true
        player?.let { runCatching { it.release() } }
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.holaolvidon.androidclient.alarm.STOP"
    }
}
