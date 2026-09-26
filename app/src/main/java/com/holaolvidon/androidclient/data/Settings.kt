package com.holaolvidon.androidclient.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

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

    /** True tras la primera conexión exitosa; permite saltar el login en arranques posteriores. */
    var configured: Boolean
        get() = prefs.getBoolean(KEY_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIGURED, value).apply()

    /** Tenants suscritos en modo "todas las alarmas". */
    var subscribedTenantIds: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED, value).apply()

    /** Suscripciones manuales: tenantId -> conjunto de alarmas seleccionadas. */
    var manualSubscriptions: Map<String, Set<String>>
        get() {
            val raw = prefs.getString(KEY_MANUAL, "{}") ?: "{}"
            return try {
                val obj = JSONObject(raw)
                val map = mutableMapOf<String, Set<String>>()
                obj.keys().forEach { tenantId ->
                    val arr = obj.optJSONArray(tenantId) ?: JSONArray()
                    map[tenantId] = (0 until arr.length()).map { arr.getString(it) }.toSet()
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val obj = JSONObject()
            value.forEach { (tenantId, alarmIds) ->
                obj.put(tenantId, JSONArray(alarmIds.toList()))
            }
            prefs.edit().putString(KEY_MANUAL, obj.toString()).apply()
        }

    companion object {
        private const val KEY_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CONFIGURED = "configured"
        private const val KEY_SELECTED = "selected_tenants"
        private const val KEY_MANUAL = "manual_subscriptions"

        // Valor por defecto apuntando al host del emulador Android.
        const val DEFAULT_URL = "http://10.0.2.2:3000"
        const val DEFAULT_API_KEY = "clave_secreta_movil"
    }
}
