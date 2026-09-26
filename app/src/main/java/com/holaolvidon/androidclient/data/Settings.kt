package com.holaolvidon.androidclient.data

import android.content.Context
import android.content.SharedPreferences

/** Persistencia local (SharedPreferences) de la configuración y de las selecciones del usuario. */
class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var selectedTenantIds: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED, value).apply()

    var subscribedAlarmIds: Set<String>
        get() = prefs.getStringSet(KEY_SUBSCRIBED, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SUBSCRIBED, value).apply()

    companion object {
        private const val KEY_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SELECTED = "selected_tenants"
        private const val KEY_SUBSCRIBED = "subscribed_alarms"

        // Valor por defecto apuntando al host del emulador Android.
        const val DEFAULT_URL = "http://10.0.2.2:3000"
        const val DEFAULT_API_KEY = "clave_secreta_movil"
    }
}
