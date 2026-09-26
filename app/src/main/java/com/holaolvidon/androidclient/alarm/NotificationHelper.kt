package com.holaolvidon.androidclient.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.holaolvidon.androidclient.MainActivity
import com.holaolvidon.androidclient.R

object NotificationHelper {
    const val CHANNEL_ID = "alarms"
    const val SILENT_CHANNEL_ID = "alarms_ringing"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canal con sonido de alarma por defecto (fallback cuando no hay canción).
        val alarmChannel = NotificationChannel(
            CHANNEL_ID,
            "Alarmas",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificaciones de alarmas programadas"
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build(),
            )
            enableVibration(true)
        }

        // Canal silencioso: lo usa el servicio en primer plano mientras suena la canción,
        // para no duplicar el sonido por defecto del canal.
        val ringingChannel = NotificationChannel(
            SILENT_CHANNEL_ID,
            "Alarma sonando",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificación mostrada mientras suena el audio de la alarma"
            setSound(null, null)
            enableVibration(true)
        }

        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(ringingChannel)
    }

    /** Notificación de alarma base (contenido + pantalla completa). */
    fun buildAlarmNotification(
        context: Context,
        title: String,
        channelId: String = CHANNEL_ID,
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Alarma")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)
            .setAutoCancel(true)
            .build()
    }

    /** Notificación en primer plano mientras suena: canal silencioso + acción "Detener". */
    fun buildRingingNotification(context: Context, title: String): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, SILENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Alarma")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)
            .setAutoCancel(true)
            .addAction(0, "Detener", stopIntent)
            .setDeleteIntent(stopIntent)
            .build()
    }

    fun showAlarmNotification(context: Context, title: String) {
        ensureChannels(context)
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, buildAlarmNotification(context, title))
    }
}
