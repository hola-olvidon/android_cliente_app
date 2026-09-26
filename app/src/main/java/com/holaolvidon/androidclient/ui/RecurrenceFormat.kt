package com.holaolvidon.androidclient.ui

import com.holaolvidon.androidclient.alarm.RecurrenceExpander
import com.holaolvidon.androidclient.data.Alarm
import com.holaolvidon.androidclient.data.Recurrence
import java.time.ZoneId

private val DIAS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

/** Próximo instante (epoch ms) en que sonará la alarma, o `null` si no tiene más ejecuciones futuras. */
fun nextExecution(alarm: Alarm, now: Long, zone: ZoneId): Long? =
    if (alarm.recurrencia == null) {
        alarm.horaProgramada.takeIf { it > now }
    } else {
        RecurrenceExpander.nextOccurrence(alarm.recurrencia, now, zone)
    }

/** Resumen legible de una regla de recurrencia (espejo del `formatRecurrence` del frontend). */
fun formatRecurrence(r: Recurrence): String {
    val days = r.diasSemana.mapNotNull { d -> DIAS.getOrNull(d - 1) }.joinToString(", ")

    val body = when (r.tipo) {
        "diaria" -> "Todos los días ${r.hora}"
        "semanal" -> "$days ${r.hora}"
        "mensual" -> "Día ${r.diasMes.joinToString(", ")} de cada mes ${r.hora}"
        "anual" -> "${r.fechas.joinToString(", ")} ${r.hora}"
        "intervalo" -> "Cada ${r.intervaloMinutos} min ${r.horaInicio}–${r.horaFin} $days"
        else -> r.tipo
    }

    return r.fechaFin?.let { "$body · hasta $it" } ?: body
}
