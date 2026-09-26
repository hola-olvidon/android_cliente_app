package com.holaolvidon.androidclient.data

import org.json.JSONArray
import org.json.JSONObject

/** Tenant resumido para la lista de selección (`GET /mobile/tenants`). */
data class TenantSummary(
    val id: String,
    val nombre: String,
)

/**
 * Regla de recurrencia de una alarma (espejo del backend). `tipo` determina qué campos se usan:
 * - "diaria": hora
 * - "semanal": diasSemana + hora
 * - "mensual": diasMes + hora
 * - "anual": fechas + hora
 * - "intervalo": diasSemana + horaInicio + horaFin + intervaloMinutos
 * - "fechaFin" ("YYYY-MM-DD") es opcional en cualquiera.
 *
 * `diasSemana` usa ISO: 1 = Lunes ... 7 = Domingo.
 */
data class Recurrence(
    val tipo: String,
    val hora: String? = null,
    val diasSemana: List<Int> = emptyList(),
    val diasMes: List<Int> = emptyList(),
    val fechas: List<String> = emptyList(),
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val intervaloMinutos: Int = 0,
    val fechaFin: String? = null,
)

/** Configuración pública del servidor (`GET /mobile/config`). */
data class ServerConfig(
    val zonaHoraria: String,
)

/** Alarma tal como la devuelve el backend. */
data class Alarm(
    val id: String,
    val tenantId: String,
    val titulo: String,
    val horaProgramada: Long, // epoch millis (UTC) — solo para alarmas de una sola vez
    val recurrencia: Recurrence?,
    val urlAudio: String?,
    val activa: Boolean,
)

/** Tenant con sus alarmas incluidas (`GET /tenants/:id`). */
data class Tenant(
    val id: String,
    val nombre: String,
    val alarms: List<Alarm>,
)

fun parseRecurrence(json: JSONObject?): Recurrence? {
    if (json == null) return null
    val tipo = json.optString("tipo", "").takeIf { it.isNotBlank() } ?: return null

    return Recurrence(
        tipo = tipo,
        hora = json.optString("hora").takeIf { it.isNotBlank() },
        diasSemana = json.optJSONArray("diasSemana")?.toIntList() ?: emptyList(),
        diasMes = json.optJSONArray("diasMes")?.toIntList() ?: emptyList(),
        fechas = json.optJSONArray("fechas")?.toStringList() ?: emptyList(),
        horaInicio = json.optString("horaInicio").takeIf { it.isNotBlank() },
        horaFin = json.optString("horaFin").takeIf { it.isNotBlank() },
        intervaloMinutos = json.optInt("intervaloMinutos", 0),
        fechaFin = json.optString("fechaFin").takeIf { it.isNotBlank() },
    )
}

private fun JSONArray.toIntList(): List<Int> {
    val result = ArrayList<Int>(length())
    for (i in 0 until length()) result.add(getInt(i))
    return result
}

private fun JSONArray.toStringList(): List<String> {
    val result = ArrayList<String>(length())
    for (i in 0 until length()) result.add(getString(i))
    return result
}
