package com.holaolvidon.androidclient.alarm

import com.holaolvidon.androidclient.data.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Expande una regla de recurrencia a instantes concretos (epoch ms) posteriores a `now`, dentro de una
 * ventana temporal, interpretando los horarios en la zona horaria del servidor (`zone`).
 *
 * Es el espejo de `Backend/src/alarms/recurrence.ts#nextOccurrences`. `diasSemana` usa ISO:
 * 1 = Lunes ... 7 = Domingo.
 */
object RecurrenceExpander {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun nextOccurrences(
        recurrence: Recurrence,
        now: Long,
        zone: ZoneId,
        windowDays: Long = 7,
        maxCount: Int = 200,
    ): List<Long> {
        val result = mutableListOf<Long>()
        val windowEnd = now + windowDays * DAY_MS
        val fechaFinLimit = recurrence.fechaFin?.let { ff ->
            runCatching {
                LocalDate.parse(ff).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            }.getOrNull()
        } ?: Long.MAX_VALUE

        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val startDate = today.minusDays(1)

        fun push(ms: Long) {
            if (ms <= now) return
            if (ms > windowEnd) return
            if (ms >= fechaFinLimit) return
            if (result.size >= maxCount) return
            result.add(ms)
        }

        fun at(date: LocalDate, time: LocalTime): Long =
            date.atTime(time).atZone(zone).toInstant().toEpochMilli()

        when (recurrence.tipo) {
            "diaria" -> {
                val time = LocalTime.parse(recurrence.hora!!)
                var d = startDate
                var i = 0
                while (result.size < maxCount && i < 400) {
                    push(at(d, time))
                    d = d.plusDays(1); i++
                }
            }

            "semanal" -> {
                val time = LocalTime.parse(recurrence.hora!!)
                val dias = recurrence.diasSemana.toSet()
                var d = startDate
                var i = 0
                while (result.size < maxCount && i < 400) {
                    if (d.dayOfWeek.value in dias) push(at(d, time))
                    d = d.plusDays(1); i++
                }
            }

            "mensual" -> {
                val time = LocalTime.parse(recurrence.hora!!)
                var month = YearMonth.from(startDate)
                var i = 0
                while (result.size < maxCount && i < 48) {
                    val lastDay = month.lengthOfMonth()
                    for (dia in recurrence.diasMes) {
                        push(at(month.atDay(minOf(dia, lastDay)), time))
                    }
                    month = month.plusMonths(1); i++
                }
            }

            "anual" -> {
                val time = LocalTime.parse(recurrence.hora!!)
                var year = startDate.year
                var i = 0
                while (result.size < maxCount && i < 6) {
                    for (fecha in recurrence.fechas) {
                        val mes = fecha.substring(0, 2).toInt()
                        val dia = fecha.substring(3, 5).toInt()
                        val date = runCatching { LocalDate.of(year, mes, dia) }.getOrNull()
                        date?.let { push(at(it, time)) }
                    }
                    year++; i++
                }
            }

            "intervalo" -> {
                val inicio = LocalTime.parse(recurrence.horaInicio!!)
                val fin = LocalTime.parse(recurrence.horaFin!!)
                val dias = recurrence.diasSemana.toSet()
                val step = recurrence.intervaloMinutos
                var d = startDate
                var i = 0
                while (result.size < maxCount && i < 400) {
                    if (d.dayOfWeek.value in dias) {
                        var t = inicio
                        while (t.isBefore(fin) && result.size < maxCount) {
                            push(at(d, t))
                            t = t.plusMinutes(step.toLong())
                        }
                    }
                    d = d.plusDays(1); i++
                }
            }
        }

        return result.sorted().take(maxCount)
    }

    /**
     * Primera ocurrencia futura de la regla, o `null` si no hay ninguna dentro del horizonte de
     * búsqueda (hasta ~6 años para anuales, ~48 meses para mensuales, etc.).
     */
    fun nextOccurrence(recurrence: Recurrence, now: Long, zone: ZoneId): Long? =
        nextOccurrences(recurrence, now, zone, windowDays = 4000, maxCount = 1).firstOrNull()
}
