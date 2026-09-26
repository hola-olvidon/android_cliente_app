package com.holaolvidon.androidclient.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.holaolvidon.androidclient.data.Alarm
import java.time.ZoneId

/**
 * Programa/cancela alarmas exactas con [AlarmManager]. Guarda en SharedPreferences las claves
 * programadas para poder cancelar las que dejen de ser necesarias en cada refresco.
 *
 * Una alarma recurrente se expande a varias ocurrencias (una clave por ocurrencia `"$id@$millis"`),
 * interpretando los horarios en la zona horaria del servidor ([zone]).
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs =
        context.getSharedPreferences("alarm_scheduler", Context.MODE_PRIVATE)

    /** Programa las ocurrencias futuras de cada alarma activa; cancela las obsoletas. */
    fun schedule(alarms: List<Alarm>, baseUrl: String, apiKey: String, zone: ZoneId) {
        val now = System.currentTimeMillis()
        val newKeys = mutableSetOf<String>()

        for (alarm in alarms) {
            if (!alarm.activa) continue

            if (alarm.recurrencia == null) {
                // Alarma de una sola vez.
                if (alarm.horaProgramada > now) {
                    val key = "${alarm.id}@${alarm.horaProgramada}"
                    newKeys.add(key)
                    setAlarm(alarm.horaProgramada, pendingIntent(alarm, baseUrl, apiKey, key))
                }
            } else {
                // Alarma recurrente: programar cada ocurrencia dentro de la ventana.
                val occurrences =
                    RecurrenceExpander.nextOccurrences(alarm.recurrencia, now, zone)
                for (ms in occurrences) {
                    val key = "${alarm.id}@$ms"
                    newKeys.add(key)
                    setAlarm(ms, pendingIntent(alarm, baseUrl, apiKey, key))
                }
            }
        }

        val oldKeys = prefs.getStringSet(PREF_IDS, emptySet()) ?: emptySet()
        oldKeys.filter { it !in newKeys }.forEach { cancel(it) }

        prefs.edit().putStringSet(PREF_IDS, newKeys).apply()
    }

    /** Cancela todas las alarmas programadas (p. ej. al desconectar). */
    fun cancelAll() {
        val oldKeys = prefs.getStringSet(PREF_IDS, emptySet()) ?: emptySet()
        oldKeys.forEach { cancel(it) }
        prefs.edit().remove(PREF_IDS).apply()
    }

    private fun cancel(key: String) {
        alarmManager.cancel(pendingIntent(key))
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

    private fun pendingIntent(alarm: Alarm, baseUrl: String, apiKey: String, key: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, alarm.titulo)
            putExtra(EXTRA_AUDIO, alarm.urlAudio)
            putExtra(EXTRA_BASE_URL, baseUrl)
            putExtra(EXTRA_API_KEY, apiKey)
        }
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(key: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val PREF_IDS = "scheduled_ids"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUDIO = "extra_audio"
        const val EXTRA_BASE_URL = "extra_base_url"
        const val EXTRA_API_KEY = "extra_api_key"
    }
}
