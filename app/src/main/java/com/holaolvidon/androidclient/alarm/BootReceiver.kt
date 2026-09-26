package com.holaolvidon.androidclient.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.holaolvidon.androidclient.data.Alarm
import com.holaolvidon.androidclient.data.ApiClient
import com.holaolvidon.androidclient.data.Settings
import java.time.ZoneId

/**
 * Al reiniciar el dispositivo, Android elimina las alarmas de [AlarmManager]. Este receiver
 * reprograma las alarmas de los tenants suscritos usando la configuración persistida en [Settings].
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        Thread {
            try {
                reschedule(context)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun reschedule(context: Context) {
        val settings = Settings(context)
        if (!settings.configured) return

        val baseUrl = settings.baseUrl
        val apiKey = settings.apiKey
        val subscribed = settings.subscribedTenantIds
        val manual = settings.manualSubscriptions
        val tenantIds = subscribed + manual.keys
        if (tenantIds.isEmpty()) return

        val apiClient = ApiClient()
        val alarms = mutableListOf<Alarm>()
        for (id in tenantIds) {
            try {
                alarms.addAll(apiClient.fetchTenant(baseUrl, apiKey, id).alarms)
            } catch (_: Exception) {
                // Tolerar fallos parciales: si un tenant falla, seguimos con los demás.
            }
        }

        val zona = runCatching { apiClient.fetchConfig(baseUrl, apiKey).zonaHoraria }.getOrNull()
        val zone = zona?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.of("UTC")

        val followed = alarms.filter { alarm ->
            alarm.tenantId in subscribed ||
                alarm.id in (manual[alarm.tenantId] ?: emptySet())
        }

        AlarmScheduler(context).schedule(followed, baseUrl, apiKey, zone)
    }
}
