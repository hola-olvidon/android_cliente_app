package com.holaolvidon.androidclient.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado global (a nivel de proceso) de la alarma que está sonando. El [AlarmRingService] lo actualiza
 * al empezar/terminar; la UI lo observa para mostrar un overlay de "Detener" cuando la app está en
 * primer plano (incluso si las notificaciones están deshabilitadas).
 */
object RingingState {
    private val _ringing = MutableStateFlow<String?>(null)

    /** Título de la alarma sonando, o `null` si no hay ninguna. */
    val ringing: StateFlow<String?> = _ringing.asStateFlow()

    fun start(title: String) {
        _ringing.value = title
    }

    fun stop() {
        _ringing.value = null
    }
}
