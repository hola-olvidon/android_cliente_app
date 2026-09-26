package com.holaolvidon.androidclient.data

import android.content.Context
import android.content.SharedPreferences

/** Persistencia local de la configuración de conexión (SharedPreferences). */
class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var tenantId: String
        get() = prefs.getString(KEY_TENANT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TENANT, value).apply()

    var pollIntervalSeconds: Long
        get() = prefs.getLong(KEY_INTERVAL, DEFAULT_INTERVAL)
        set(value) = prefs.edit().putLong(KEY_INTERVAL, value).apply()

    companion object {
        private const val KEY_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_TENANT = "tenant_id"
        private const val KEY_INTERVAL = "poll_interval"

        // Valor por defecto apuntando al host del emulador Android.
        const val DEFAULT_URL = "http://10.0.2.2:3000"
        const val DEFAULT_API_KEY = "clave_secreta_movil"
        const val DEFAULT_INTERVAL = 30L
    }
}
