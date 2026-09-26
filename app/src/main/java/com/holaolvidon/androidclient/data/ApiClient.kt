package com.holaolvidon.androidclient.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

/** Cliente HTTP mínimo para la API móvil del backend (protegida con `X-API-KEY`). */
class ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Obtiene el tenant y todas sus alarmas desde `GET /tenants/{tenantId}`.
     *
     * @throws ApiException si el servidor responde con un código distinto de 2xx.
     */
    fun fetchTenant(baseUrl: String, apiKey: String, tenantId: String): Tenant {
        val url = "${baseUrl.trimEnd('/')}/tenants/$tenantId"

        val request = Request.Builder()
            .url(url)
            .header("X-API-KEY", apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()

            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(body ?: "{}").optString("message")
                }.getOrNull()
                throw ApiException(response.code, message ?: "Error ${response.code}")
            }

            return parseTenant(JSONObject(body ?: "{}"))
        }
    }

    private fun parseTenant(json: JSONObject): Tenant {
        val id = json.getString("id")
        val nombre = json.optString("nombre", "Tenant")

        val alarmsArray: JSONArray = json.optJSONArray("alarms") ?: JSONArray()
        val alarms = ArrayList<Alarm>(alarmsArray.length())

        for (i in 0 until alarmsArray.length()) {
            val a = alarmsArray.getJSONObject(i)
            alarms.add(
                Alarm(
                    id = a.getString("id"),
                    tenantId = a.optString("tenantId", id),
                    titulo = a.optString("titulo", "Alarma"),
                    horaProgramada = parseDate(a.optString("horaProgramada")),
                    urlAudio = a.optString("urlAudio").takeIf { it.isNotBlank() },
                    activa = a.optBoolean("activa", true),
                ),
            )
        }

        return Tenant(id, nombre, alarms)
    }

    private fun parseDate(value: String): Long {
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (e: DateTimeParseException) {
            // Fallback por si la fecha viene con offset pero sin 'Z' explícita.
            try {
                OffsetDateTime.parse(value).toInstant().toEpochMilli()
            } catch (e2: DateTimeParseException) {
                0L
            }
        }
    }
}

/** Error de la API con el código HTTP, para mostrar un mensaje claro en la UI. */
class ApiException(val status: Int, message: String) : Exception(message)
