package com.holaolvidon.androidclient.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holaolvidon.androidclient.alarm.AlarmScheduler
import com.holaolvidon.androidclient.data.Alarm
import com.holaolvidon.androidclient.data.ApiClient
import com.holaolvidon.androidclient.data.Settings
import com.holaolvidon.androidclient.data.TenantSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val connected: Boolean = false,
    val tenants: List<TenantSummary> = emptyList(),
    val selectedTenantIds: Set<String> = emptySet(),
    val tenantNames: Map<String, String> = emptyMap(),
    val alarms: List<Alarm> = emptyList(),
    val subscribedAlarmIds: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = Settings(app)
    private val apiClient = ApiClient()
    private val scheduler = AlarmScheduler(app)

    private val _uiState = MutableStateFlow(
        UiState(
            baseUrl = settings.baseUrl,
            apiKey = settings.apiKey,
            selectedTenantIds = settings.selectedTenantIds,
            subscribedAlarmIds = settings.subscribedAlarmIds,
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun updateBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun updateApiKey(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    /** Conecta: valida la clave y carga la lista de tenants. */
    fun connect() {
        val s = _uiState.value
        settings.baseUrl = s.baseUrl
        settings.apiKey = s.apiKey

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val tenants = withContext(Dispatchers.IO) {
                    apiClient.fetchTenants(s.baseUrl, s.apiKey)
                }
                _uiState.value = _uiState.value.copy(
                    tenants = tenants,
                    connected = true,
                    loading = false,
                )
                startPolling()
                refreshAlarms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connected = false,
                    loading = false,
                    error = e.message ?: "Error desconocido",
                )
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                refreshAlarms()
            }
        }
    }

    /** Selecciona/deselecciona un tenant y refresca las alarmas. */
    fun toggleTenant(tenantId: String) {
        val current = _uiState.value.selectedTenantIds
        val next = if (tenantId in current) current - tenantId else current + tenantId
        settings.selectedTenantIds = next
        _uiState.value = _uiState.value.copy(selectedTenantIds = next)
        refreshAlarms()
    }

    /** Activa/desactiva la suscripción a una alarma concreta. */
    fun toggleAlarm(alarmId: String) {
        val current = _uiState.value.subscribedAlarmIds
        val next = if (alarmId in current) current - alarmId else current + alarmId
        settings.subscribedAlarmIds = next
        _uiState.value = _uiState.value.copy(subscribedAlarmIds = next)
        rescheduleAlarms()
    }

    /** Consulta las alarmas de todos los tenants seleccionados. */
    fun refreshAlarms() {
        val s = _uiState.value
        if (!s.connected) return

        if (s.selectedTenantIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                alarms = emptyList(),
                tenantNames = emptyMap(),
                loading = false,
                error = null,
                lastUpdated = null,
            )
            rescheduleAlarms()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val (names, alarms) = withContext(Dispatchers.IO) { fetchAlarmsForSelected(s) }
                _uiState.value = _uiState.value.copy(
                    tenantNames = names,
                    alarms = alarms.sortedBy { it.horaProgramada },
                    loading = false,
                    lastUpdated = System.currentTimeMillis(),
                )
                rescheduleAlarms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Error desconocido",
                )
            }
        }
    }

    /** Obtiene, en paralelo, las alarmas de cada tenant seleccionado (tolerando fallos parciales). */
    private suspend fun fetchAlarmsForSelected(s: UiState): Pair<Map<String, String>, List<Alarm>> {
        val deferreds = s.selectedTenantIds.map { id ->
            async(Dispatchers.IO) { apiClient.fetchTenant(s.baseUrl, s.apiKey, id) }
        }

        val names = mutableMapOf<String, String>()
        val alarms = mutableListOf<Alarm>()
        var successCount = 0
        var firstError: Exception? = null

        for ((id, deferred) in s.selectedTenantIds.zip(deferreds)) {
            try {
                val tenant = deferred.await()
                names[id] = tenant.nombre
                alarms.addAll(tenant.alarms)
                successCount++
            } catch (e: Exception) {
                if (firstError == null) firstError = e
            }
        }

        // Si ninguno respondió, se muestra el primer error; si alguno respondió, se toleran los fallos.
        if (successCount == 0 && firstError != null) throw firstError!!

        return names to alarms
    }

    /** Programa las alarmas locales en función de las suscripciones activas. */
    private fun rescheduleAlarms() {
        val s = _uiState.value
        val subscribed = s.alarms.filter { it.id in s.subscribedAlarmIds }
        scheduler.schedule(subscribed)
    }

    /** Desconecta: detiene el sondeo, cancela alarmas y limpia el estado. */
    fun disconnect() {
        pollJob?.cancel()
        scheduler.cancelAll()
        _uiState.value = _uiState.value.copy(
            connected = false,
            tenants = emptyList(),
            selectedTenantIds = emptySet(),
            tenantNames = emptyMap(),
            alarms = emptyList(),
            subscribedAlarmIds = emptySet(),
            error = null,
            lastUpdated = null,
        )
        settings.selectedTenantIds = emptySet()
        settings.subscribedAlarmIds = emptySet()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
    }
}
