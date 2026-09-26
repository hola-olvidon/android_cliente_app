package com.holaolvidon.androidclient.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP mínimo para la API móvil del backend (protegida con el header `X-API-KEY`).
 *
 * Lanza excepciones con mensajes claros y accionables:
 * - [ApiException] para respuestas HTTP con código de error (401, 404, 500...).
 * - [NetworkException] para problemas de red/tiempo de espera o respuestas no válidas.
 */
class ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Obtiene la lista de tenants disponibles (`GET /mobile/tenants`). */
    fun fetchTenants(baseUrl: String, apiKey: String): List<TenantSummary> {
        val body = get("${baseUrl.trimEnd('/')}/mobile/tenants", apiKey)
        return try {
            val array = JSONArray(body)
            val result = ArrayList<TenantSummary>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                result.add(
                    TenantSummary(
                        id = o.getString("id"),
                        nombre = o.optString("nombre", "Tenant"),
                    ),
                )
            }
            result
        } catch (e: Exception) {
            throw NetworkException(
                "Respuesta del servidor no válida (no es JSON o no tiene el formato esperado): " +
                    (e.message ?: "error de parseo"),
            )
        }
    }

    /** Obtiene un tenant con sus alarmas (`GET /tenants/:id`). */
    fun fetchTenant(baseUrl: String, apiKey: String, tenantId: String): Tenant {
        val body = get("${baseUrl.trimEnd('/')}/tenants/$tenantId", apiKey)
        return try {
            parseTenant(JSONObject(body))
        } catch (e: Exception) {
            throw NetworkException(
                "Respuesta del servidor no válida (no es JSON o no tiene el formato esperado): " +
                    (e.message ?: "error de parseo"),
            )
        }
    }

    private fun get(url: String, apiKey: String): String {
        val request = Request.Builder()
            .url(url)
            .header("X-API-KEY", apiKey)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    throw ApiException(response.code, friendlyMessage(response.code, body))
                }
                body ?: "{}"
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw NetworkException(
                "No se pudo conectar al servidor. Revisa que la URL sea correcta " +
                    "y que el backend esté corriendo (¿puerto 3000?).",
            )
        } catch (e: Exception) {
            throw NetworkException("Error de red inesperado: ${e.message}")
        }
    }

    /** Traduce un código HTTP a un mensaje claro para el usuario. */
    private fun friendlyMessage(code: Int, body: String?): String {
        val serverMsg = runCatching { JSONObject(body ?: "{}").optString("message") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

        val detail = when (code) {
            400 -> "Solicitud inválida."
            401 -> "Clave de API (X-API-KEY) inválida o no proporcionada. " +
                "Revisa que coincida con MOBILE_API_KEY del backend."
            404 -> "Recurso no encontrado. Verifica la URL y el ID del tenant."
            500 -> "Error interno del servidor."
            else -> "Error HTTP $code."
        }

        return if (serverMsg != null && !detail.contains(serverMsg)) {
            "$detail Detalle: $serverMsg"
        } else {
            detail
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
            try {
                OffsetDateTime.parse(value).toInstant().toEpochMilli()
            } catch (e2: DateTimeParseException) {
                0L
            }
        }
    }
}

/** Error de la API con el código HTTP. */
class ApiException(val status: Int, message: String) : Exception(message)

/** Error de red o de formato de respuesta. */
class NetworkException(message: String) : Exception(message)
