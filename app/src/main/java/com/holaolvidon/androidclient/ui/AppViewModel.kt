package com.holaolvidon.androidclient.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holaolvidon.androidclient.alarm.AlarmScheduler
import com.holaolvidon.androidclient.data.Alarm
import com.holaolvidon.androidclient.data.ApiClient
import com.holaolvidon.androidclient.data.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val tenantId: String = "",
    val pollIntervalSeconds: Long = Settings.DEFAULT_INTERVAL,
    val tenantName: String = "",
    val alarms: List<Alarm> = emptyList(),
    val connected: Boolean = false,
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
            tenantId = settings.tenantId,
            pollIntervalSeconds = settings.pollIntervalSeconds,
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun updateBaseUrl(value: String) = _uiState.value = _uiState.value.copy(baseUrl = value)
    fun updateApiKey(value: String) = _uiState.value = _uiState.value.copy(apiKey = value)
    fun updateTenantId(value: String) = _uiState.value = _uiState.value.copy(tenantId = value)
    fun updateInterval(value: Long) = _uiState.value = _uiState.value.copy(pollIntervalSeconds = value)

    /** Guarda la configuración, arranca el sondeo y hace una primera consulta. */
    fun connect() {
        val s = _uiState.value
        settings.baseUrl = s.baseUrl
        settings.apiKey = s.apiKey
        settings.tenantId = s.tenantId
        settings.pollIntervalSeconds = s.pollIntervalSeconds

        startPolling()
        refresh()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(_uiState.value.pollIntervalSeconds * 1000L)
                refresh()
            }
        }
    }

    /** Consulta el tenant y sus alarmas; reprograma las alarmas locales con el resultado. */
    fun refresh() {
        val s = _uiState.value
        if (s.tenantId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val tenant = apiClient.fetchTenant(s.baseUrl, s.apiKey, s.tenantId)
                scheduler.schedule(tenant.alarms)
                _uiState.value = _uiState.value.copy(
                    tenantName = tenant.nombre,
                    alarms = tenant.alarms,
                    connected = true,
                    loading = false,
                    lastUpdated = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connected = false,
                    loading = false,
                    error = e.message ?: "Error desconocido",
                )
            }
        }
    }

    /** Detiene el sondeo y cancela las alarmas locales programadas. */
    fun disconnect() {
        pollJob?.cancel()
        scheduler.cancelAll()
        _uiState.value = _uiState.value.copy(
            connected = false,
            tenantName = "",
            alarms = emptyList(),
            error = null,
            lastUpdated = null,
        )
    }
}
