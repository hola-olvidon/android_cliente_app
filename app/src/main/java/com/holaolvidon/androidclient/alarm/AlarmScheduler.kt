package com.holaolvidon.androidclient.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.holaolvidon.androidclient.data.Alarm

/**
 * Programa/cancela alarmas exactas con [AlarmManager]. Guarda en SharedPreferences los IDs
 * programados para poder cancelar los que dejen de ser necesarios en cada refresco.
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs =
        context.getSharedPreferences("alarm_scheduler", Context.MODE_PRIVATE)

    /** Programa una alarma exacta por cada alarma activa con hora futura; cancela las obsoletas. */
    fun schedule(alarms: List<Alarm>) {
        val now = System.currentTimeMillis()
        val toSchedule = alarms.filter { it.activa && it.horaProgramada > now }
        val newIds = toSchedule.map { it.id }.toSet()

        val oldIds = prefs.getStringSet(PREF_IDS, emptySet()) ?: emptySet()
        oldIds.filter { it !in newIds }.forEach { cancel(it) }

        for (alarm in toSchedule) {
            setAlarm(alarm.horaProgramada, pendingIntent(alarm))
        }

        prefs.edit().putStringSet(PREF_IDS, newIds).apply()
    }

    /** Cancela todas las alarmas programadas (p. ej. al desconectar). */
    fun cancelAll() {
        val oldIds = prefs.getStringSet(PREF_IDS, emptySet()) ?: emptySet()
        oldIds.forEach { cancel(it) }
        prefs.edit().remove(PREF_IDS).apply()
    }

    private fun cancel(alarmId: String) {
        alarmManager.cancel(pendingIntent(alarmId))
    }

    /** Usa alarma exacta si está permitida; si no, cae a alarma inexacta (sin crashear). */
    private fun setAlarm(triggerAtMillis: Long, pi: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun pendingIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, alarm.titulo)
            putExtra(EXTRA_AUDIO, alarm.urlAudio)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(alarmId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val PREF_IDS = "scheduled_ids"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUDIO = "extra_audio"
    }
}
