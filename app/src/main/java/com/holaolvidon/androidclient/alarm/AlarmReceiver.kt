package com.holaolvidon.androidclient.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Se dispara cuando una alarma programada con [AlarmScheduler] llega a su hora.
 * Arranca [AlarmRingService] (servicio en primer plano) que muestra la notificación y reproduce
 * el audio de la alarma desde el archivo local (o el tono por defecto si no hay).
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TITLE, intent.getStringExtra(AlarmScheduler.EXTRA_TITLE))
            putExtra(AlarmScheduler.EXTRA_AUDIO, intent.getStringExtra(AlarmScheduler.EXTRA_AUDIO))
            putExtra(AlarmScheduler.EXTRA_BASE_URL, intent.getStringExtra(AlarmScheduler.EXTRA_BASE_URL))
            putExtra(AlarmScheduler.EXTRA_API_KEY, intent.getStringExtra(AlarmScheduler.EXTRA_API_KEY))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
