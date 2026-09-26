package com.holaolvidon.androidclient.data

/** Alarma tal como la devuelve el backend (`GET /tenants/:id`). */
data class Alarm(
    val id: String,
    val tenantId: String,
    val titulo: String,
    val horaProgramada: Long, // epoch millis (UTC)
    val urlAudio: String?,
    val activa: Boolean,
)

/** Tenant con sus alarmas incluidas. */
data class Tenant(
    val id: String,
    val nombre: String,
    val alarms: List<Alarm>,
)
