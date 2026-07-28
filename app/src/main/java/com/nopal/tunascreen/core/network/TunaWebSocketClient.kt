package com.nopal.tunascreen.core.network

import android.util.Log
import com.nopal.tunascreen.core.model.Machine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

sealed interface TunaEvent {
    data class MachinesUpdate(val machines: List<Machine>) : TunaEvent
    data object Connected : TunaEvent
    data class Disconnected(val willRetry: Boolean) : TunaEvent
}

@Serializable
private data class WsEnvelope(
    val type: String,
    @kotlinx.serialization.SerialName("api_version") val apiVersion: Int = 1,
    val machines: List<Machine> = emptyList(),
)

/** Cliente de /ws/tunascreen -- canal de solo push del lado NOPAL (ver
 * backend/api/tunascreen.py::tunascreen_ws), reconexión con backoff
 * exponencial (1s, 2s, 4s, 8s, tope 10s) porque una tablet dedicada puede
 * quedar horas prendida frente a un Wi-Fi de taller que se cae seguido. */
class TunaWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    private companion object {
        const val TAG = "TunaWebSocket"
    }

    fun connect(baseUrl: String, token: String): Flow<TunaEvent> = callbackFlow {
        val session = WsSession(this, baseUrl, token)
        session.open()
        awaitClose { session.close() }
    }

    private inner class WsSession(
        private val scope: ProducerScope<TunaEvent>,
        private val baseUrl: String,
        private val token: String,
    ) {
        private var socket: WebSocket? = null
        private var closedByClient = false
        private var attempt = 0
        private var reconnectJob: Job? = null

        fun open() {
            if (closedByClient) return
            val wsUrl = baseUrl.replaceFirst("http", "ws").trimEnd('/') + "/ws/tunascreen"
            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()
            socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (closedByClient) {
                        webSocket.close(1000, "pantalla cerrada")
                        return
                    }
                    socket = webSocket
                    reconnectJob?.cancel()
                    reconnectJob = null
                    attempt = 0
                    Log.i(TAG, "WebSocket conectado")
                    scope.trySend(TunaEvent.Connected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching { json.decodeFromString(WsEnvelope.serializer(), text) }
                        .onSuccess { envelope ->
                            if (envelope.type == "machines") {
                                scope.trySend(TunaEvent.MachinesUpdate(envelope.machines))
                            }
                        }
                        .onFailure { error ->
                            Log.w(TAG, "Snapshot WebSocket inválido", error)
                        }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (socket === webSocket) socket = null
                    Log.w(TAG, "WebSocket falló; se intentará reconectar", t)
                    scope.trySend(TunaEvent.Disconnected(willRetry = !closedByClient))
                    scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (socket === webSocket) socket = null
                    Log.i(TAG, "WebSocket cerrado ($code): $reason")
                    scope.trySend(TunaEvent.Disconnected(willRetry = !closedByClient))
                    if (!closedByClient) scheduleReconnect()
                }
            })
        }

        private fun scheduleReconnect() {
            if (closedByClient || reconnectJob?.isActive == true) return
            val delayMs = minOf(1000L * (1L shl attempt.coerceAtMost(4)), 10_000L)
            attempt++
            reconnectJob = scope.launch {
                delay(delayMs)
                reconnectJob = null
                if (!closedByClient) open()
            }
        }

        fun close() {
            closedByClient = true
            reconnectJob?.cancel()
            reconnectJob = null
            socket?.close(1000, "pantalla cerrada")
            socket = null
        }
    }
}
