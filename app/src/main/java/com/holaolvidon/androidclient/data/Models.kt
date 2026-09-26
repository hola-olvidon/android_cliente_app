package com.holaolvidon.androidclient.data

/** Tenant resumido para la lista de selección (`GET /mobile/tenants`). */
data class TenantSummary(
    val id: String,
    val nombre: String,
)

/** Alarma tal como la devuelve el backend. */
data class Alarm(
    val id: String,
    val tenantId: String,
    val titulo: String,
    val horaProgramada: Long, // epoch millis (UTC)
    val urlAudio: String?,
    val activa: Boolean,
)

/** Tenant con sus alarmas incluidas (`GET /tenants/:id`). */
data class Tenant(
    val id: String,
    val nombre: String,
    val alarms: List<Alarm>,
)
