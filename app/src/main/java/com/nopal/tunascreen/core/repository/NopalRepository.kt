package com.nopal.tunascreen.core.repository

import com.nopal.tunascreen.core.auth.TokenStore
import com.nopal.tunascreen.core.model.ActionRequest
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.PairConfirmRequest
import com.nopal.tunascreen.core.model.PairConfirmResponse
import com.nopal.tunascreen.core.model.ServerInfo
import com.nopal.tunascreen.core.network.NopalApiService
import com.nopal.tunascreen.core.network.TunaEvent
import com.nopal.tunascreen.core.network.TunaWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionState { CONNECTING, LIVE, DISCONNECTED }

/**
 * Única fuente de verdad de las máquinas para toda la app: carga inicial
 * por REST (getMachines) y después se mantiene actualizada por WS -- los
 * ViewModels de Taller/Máquina solo observan `machines`, nunca llaman a
 * NopalApiService directo para leer estado (sí para acciones, ver
 * sendAction).
 */
@Singleton
class NopalRepository @Inject constructor(
    private val api: NopalApiService,
    private val webSocketClient: TunaWebSocketClient,
    private val tokenStore: TokenStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _machines = MutableStateFlow<List<Machine>>(emptyList())
    val machines: StateFlow<List<Machine>> = _machines.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socketJob: kotlinx.coroutines.Job? = null

    fun machine(id: String): Machine? = _machines.value.find { it.id == id }

    /** Llamado una vez al entrar a Taller/Máquina (después del pairing o al
     * reabrir la app ya pareada) -- carga por REST y deja el WS abierto
     * para las actualizaciones en vivo. */
    fun start() {
        if (socketJob?.isActive == true) return
        socketJob = scope.launch {
            refreshOnce()
            val session = tokenStore.currentSession()
            val baseUrl = session.baseUrl ?: return@launch
            val token = session.token ?: return@launch
            _connectionState.value = ConnectionState.CONNECTING
            webSocketClient.connect(baseUrl, token).collect { event ->
                when (event) {
                    is TunaEvent.Connected -> _connectionState.value = ConnectionState.LIVE
                    is TunaEvent.MachinesUpdate -> applyMachines(event.machines)
                    is TunaEvent.Disconnected -> {
                        // Durante el backoff conservamos el último snapshot y
                        // mostramos "Conectando"; no fingimos que todas las
                        // máquinas desaparecieron por un corte breve de Wi-Fi.
                        _connectionState.value = if (event.willRetry) {
                            ConnectionState.CONNECTING
                        } else {
                            ConnectionState.DISCONNECTED
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        socketJob?.cancel()
        socketJob = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    suspend fun refreshOnce() {
        runCatching { api.getMachines() }.onSuccess { applyMachines(it.machines) }
    }

    private fun applyMachines(incoming: List<Machine>) {
        val currentById = _machines.value.associateBy { it.id }
        val merged = incoming.map { machine ->
            currentById[machine.id]?.takeIf { it == machine } ?: machine
        }
        if (_machines.value != merged) {
            _machines.value = merged
        }
    }

    suspend fun checkServer(baseUrl: String): Result<ServerInfo> =
        runCatching { api.getInfo(baseUrl.trimEnd('/') + "/api/tunascreen/info") }

    suspend fun confirmPairing(baseUrl: String, code: String, deviceName: String): Result<PairConfirmResponse> =
        runCatching {
            // Todavía no hay token guardado -- este único call sale con el
            // host/puerto "candidatos" recién tipeados, no con la sesión
            // pareada (que ni existe aún). Se guarda una vez confirmado, no
            // antes, para no dejar un host a medio configurar si falla.
            val host = baseUrl.substringAfter("://").substringBefore(":")
            val port = baseUrl.substringAfterLast(":").toIntOrNull() ?: 8420
            tokenStore.savePairing(host, port, token = "", deviceId = "")
            val response = api.confirmPairing(PairConfirmRequest(code = code, deviceName = deviceName))
            tokenStore.savePairing(host, port, token = response.token, deviceId = response.deviceId)
            response
        }

    suspend fun sendAction(machineId: String, action: String, params: JsonObject): Result<Boolean> =
        runCatching {
            val response = api.sendAction(ActionRequest(machineId, action, params))
            check(response.success) { "NOPAL rechazó la acción $action" }
            true
        }

    suspend fun forgetPairing() {
        stop()
        tokenStore.clear()
        _machines.value = emptyList()
    }
}
