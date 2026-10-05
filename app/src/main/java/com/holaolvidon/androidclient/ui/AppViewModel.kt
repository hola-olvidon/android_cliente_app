package com.holaolvidon.androidclient.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holaolvidon.androidclient.alarm.AlarmRingService
import com.holaolvidon.androidclient.alarm.AlarmScheduler
import com.holaolvidon.androidclient.alarm.AudioCache
import com.holaolvidon.androidclient.alarm.RingingState
import com.holaolvidon.androidclient.data.Alarm
import com.holaolvidon.androidclient.data.ApiClient
import com.holaolvidon.androidclient.data.Settings
import com.holaolvidon.androidclient.data.TenantSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

data class UiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val connected: Boolean = false,
    val successMessage: String? = null,
    val showSettings: Boolean = false,
    val tenants: List<TenantSummary> = emptyList(),
    val selectedTenantId: String? = null,
    val tenantAlarms: List<Alarm> = emptyList(),
    val subscribedTenantIds: Set<String> = emptySet(),
    val manualSubscriptions: Map<String, Set<String>> = emptyMap(),
    val followedAlarms: List<Alarm> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null,
    val serverTimeZone: String? = null,
    val ringingAlarmTitle: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = Settings(app)
    private val apiClient = ApiClient()
    private val scheduler = AlarmScheduler(app)
    private val audioCache = AudioCache(app)

    private val _uiState = MutableStateFlow(
        UiState(
            baseUrl = settings.baseUrl,
            apiKey = settings.apiKey,
            connected = settings.configured,
            subscribedTenantIds = settings.subscribedTenantIds,
            manualSubscriptions = settings.manualSubscriptions,
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        // Si ya se conectó antes, saltamos el login y cargamos en segundo plano.
        if (_uiState.value.connected) {
            silentConnect()
        }

        // Refleja en la UI si hay una alarma sonando (para mostrar el overlay "Detener").
        viewModelScope.launch {
            RingingState.ringing.collect { title ->
                _uiState.value = _uiState.value.copy(ringingAlarmTitle = title)
            }
        }
    }

    /** Detiene la alarma que está sonando (envía la acción de parada al servicio). */
    fun stopRinging() {
        val app = getApplication<Application>()
        val intent = Intent(app, AlarmRingService::class.java)
            .setAction(AlarmRingService.ACTION_STOP)
        app.startService(intent)
    }

    fun updateBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun updateApiKey(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    /** Conexión explícita desde la pantalla de login: valida, guarda y muestra éxito. */
    fun connect() {
        val s = _uiState.value
        settings.baseUrl = s.baseUrl
        settings.apiKey = s.apiKey
        settings.configured = true

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val tenants = withContext(Dispatchers.IO) {
                    apiClient.fetchTenants(s.baseUrl, s.apiKey)
                }
                val zona = loadServerTimeZone(s)
                _uiState.value = _uiState.value.copy(
                    tenants = tenants,
                    serverTimeZone = zona,
                    connected = true,
                    loading = false,
                    successMessage = "Conexión exitosa",
                )
                startPolling()
                refreshFollowedAlarms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connected = false,
                    loading = false,
                    error = e.message ?: "Error desconocido",
                )
            }
        }
    }

    /** Conexión silenciosa al arrancar (ya configurado): no muestra mensaje de éxito. */
    private fun silentConnect() {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                val tenants = withContext(Dispatchers.IO) {
                    apiClient.fetchTenants(s.baseUrl, s.apiKey)
                }
                val zona = loadServerTimeZone(s)
                _uiState.value = _uiState.value.copy(
                    tenants = tenants,
                    serverTimeZone = zona,
                )
                startPolling()
                refreshFollowedAlarms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Error desconocido")
            }
        }
    }

    /** Refresca la lista de tenants y las alarmas seguidas (botón "Actualizar"). */
    fun refresh() {
        val s = _uiState.value
        if (!s.connected) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val tenants = withContext(Dispatchers.IO) {
                    apiClient.fetchTenants(s.baseUrl, s.apiKey)
                }
                val zona = loadServerTimeZone(s)
                _uiState.value = _uiState.value.copy(
                    tenants = tenants,
                    serverTimeZone = zona,
                    loading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Error desconocido",
                )
            }
        }
        refreshFollowedAlarms()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                refreshFollowedAlarms()
            }
        }
    }

    // --- Navegación ---

    fun openSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }

    fun openTenant(tenantId: String) {
        val s = _uiState.value
        var subscribed = s.subscribedTenantIds
        if (tenantId !in subscribed && tenantId !in s.manualSubscriptions) {
            // Por defecto, abrir un tenant lo suscribe a todas sus alarmas.
            subscribed = subscribed + tenantId
            settings.subscribedTenantIds = subscribed
        }
        _uiState.value = _uiState.value.copy(
            selectedTenantId = tenantId,
            subscribedTenantIds = subscribed,
        )
        loadTenantAlarms(tenantId)
        refreshFollowedAlarms()
    }

    fun closeTenant() {
        _uiState.value = _uiState.value.copy(selectedTenantId = null, tenantAlarms = emptyList())
    }

    private fun loadTenantAlarms(tenantId: String) {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                val tenant = withContext(Dispatchers.IO) {
                    apiClient.fetchTenant(s.baseUrl, s.apiKey, tenantId)
                }
                _uiState.value = _uiState.value.copy(
                    tenantAlarms = tenant.alarms.sortedBy { it.horaProgramada },
                    error = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Error desconocido")
            }
        }
    }

    // --- Suscripciones ---

    fun toggleSubscribeAll(tenantId: String, enabled: Boolean) {
        val s = _uiState.value
        val subscribed = if (enabled) s.subscribedTenantIds + tenantId
        else s.subscribedTenantIds - tenantId
        val manual = if (enabled) s.manualSubscriptions - tenantId
        else s.manualSubscriptions + (tenantId to emptySet())

        settings.subscribedTenantIds = subscribed
        settings.manualSubscriptions = manual
        _uiState.value = _uiState.value.copy(
            subscribedTenantIds = subscribed,
            manualSubscriptions = manual,
        )
        refreshFollowedAlarms()
    }

    fun toggleAlarm(tenantId: String, alarmId: String, enabled: Boolean) {
        val s = _uiState.value
        val current = s.manualSubscriptions[tenantId] ?: emptySet()
        val next = if (enabled) current + alarmId else current - alarmId

        // Si marcamos manualmente todas las alarmas del tenant, pasamos automáticamente
        // a "suscribirme a todas" (el toggle "seleccionar todo" se activa solo).
        val tenantAlarms = s.tenantAlarms.filter { it.tenantId == tenantId }
        val allSelected = enabled && tenantAlarms.isNotEmpty() && tenantAlarms.all { it.id in next }

        val subscribed = if (allSelected) s.subscribedTenantIds + tenantId else s.subscribedTenantIds
        val manual = if (allSelected) s.manualSubscriptions - tenantId
        else s.manualSubscriptions + (tenantId to next)

        settings.subscribedTenantIds = subscribed
        settings.manualSubscriptions = manual
        _uiState.value = _uiState.value.copy(
            subscribedTenantIds = subscribed,
            manualSubscriptions = manual,
        )
        refreshFollowedAlarms()
    }

    // --- Alarmas seguidas ---

    fun refreshFollowedAlarms() {
        val s = _uiState.value
        if (!s.connected) return

        val tenantIds = s.subscribedTenantIds + s.manualSubscriptions.keys
        if (tenantIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                followedAlarms = emptyList(),
                error = null,
                lastUpdated = null,
            )
            rescheduleAlarms()
            return
        }

        viewModelScope.launch {
            try {
                val alarms = withContext(Dispatchers.IO) {
                    fetchAlarmsForIds(s, tenantIds.toList())
                }
                val newState = _uiState.value.copy(
                    followedAlarms = alarms,
                    lastUpdated = System.currentTimeMillis(),
                    error = null,
                )
                _uiState.value = newState
                rescheduleAlarms()
                preCacheAudio(effectiveSubscribed(newState))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Error desconocido")
            }
        }
    }

    /** Obtiene, en paralelo, las alarmas de cada tenant (tolerando fallos parciales). */
    private suspend fun fetchAlarmsForIds(s: UiState, ids: List<String>): List<Alarm> =
        coroutineScope {
            val deferreds = ids.map { id ->
                async(Dispatchers.IO) { apiClient.fetchTenant(s.baseUrl, s.apiKey, id) }
            }

            val alarms = mutableListOf<Alarm>()
            var successCount = 0
            var firstError: Exception? = null

            for ((id, deferred) in ids.zip(deferreds)) {
                try {
                    alarms.addAll(deferred.await().alarms)
                    successCount++
                } catch (e: Exception) {
                    if (firstError == null) firstError = e
                }
            }

            if (successCount == 0 && firstError != null) throw firstError!!
            alarms
        }

    /** Alarmas efectivamente suscritas: todas las del tenant (modo todas) o las manuales. */
    private fun effectiveSubscribed(s: UiState): List<Alarm> =
        s.followedAlarms.filter { alarm ->
            alarm.tenantId in s.subscribedTenantIds ||
                alarm.id in (s.manualSubscriptions[alarm.tenantId] ?: emptySet())
        }

    /**
     * Obtiene la zona horaria del servidor. No es fatal si el endpoint no existe o falla:
     * en ese caso se devuelve `null` y se usará "UTC" al programar.
     */
    private suspend fun loadServerTimeZone(s: UiState): String? =
        withContext(Dispatchers.IO) {
            runCatching { apiClient.fetchConfig(s.baseUrl, s.apiKey).zonaHoraria }.getOrNull()
        }

    /** Resuelve la zona horaria del servidor a un [ZoneId] válido (fallback "UTC"). */
    private fun resolveZone(s: UiState): ZoneId =
        s.serverTimeZone?.let { z -> runCatching { ZoneId.of(z) }.getOrNull() }
            ?: ZoneId.of("UTC")

    /** Programa las alarmas locales en función de las suscripciones activas. */
    private fun rescheduleAlarms() {
        val s = _uiState.value
        scheduler.schedule(effectiveSubscribed(s), s.baseUrl, s.apiKey, resolveZone(s))
    }

    /** Descarga (una sola vez) el audio de las alarmas suscritas para tenerlo local a la hora. */
    private fun preCacheAudio(alarms: List<Alarm>) {
        val s = _uiState.value
        val urls = alarms.mapNotNull { it.urlAudio }.filter { it.isNotBlank() }.distinct()
        if (urls.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // En paralelo (acotado) para que todos los audios estén en local antes de la alarma,
                // incluso si son varios o "pesados".
                urls.chunked(PARALLEL_DOWNLOADS).forEach { batch ->
                    batch.map { url -> async { audioCache.getOrDownload(s.baseUrl, s.apiKey, url) } }
                        .forEach { it.await() }
                }
            }
        }
    }

    // --- Desconexión ---

    fun disconnect() {
        pollJob?.cancel()
        scheduler.cancelAll()
        settings.configured = false
        settings.subscribedTenantIds = emptySet()
        settings.manualSubscriptions = emptyMap()
        _uiState.value = _uiState.value.copy(
            connected = false,
            showSettings = false,
            selectedTenantId = null,
            tenantAlarms = emptyList(),
            tenants = emptyList(),
            subscribedTenantIds = emptySet(),
            manualSubscriptions = emptyMap(),
            followedAlarms = emptyList(),
            error = null,
            successMessage = null,
            lastUpdated = null,
            serverTimeZone = null,
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
        private const val PARALLEL_DOWNLOADS = 4
    }
}
