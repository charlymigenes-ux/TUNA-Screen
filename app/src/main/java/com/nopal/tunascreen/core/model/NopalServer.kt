package com.nopal.tunascreen.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Un NOPAL encontrado por mDNS/NSD o cargado a mano por IP -- no confundir
 * con `Machine` (una impresora/láser DENTRO de ese NOPAL). */
data class NopalServer(
    val host: String,
    val port: Int,
    val name: String? = null,
)

@Serializable
data class ServerInfo(
    val name: String,
    @kotlinx.serialization.SerialName("server_version") val serverVersion: String? = null,
    @kotlinx.serialization.SerialName("api_version") val apiVersion: Int = 1,
    @kotlinx.serialization.SerialName("pairing_open") val pairingOpen: Boolean = false,
    @kotlinx.serialization.SerialName("websocket_path") val websocketPath: String = "/ws/tunascreen",
)

@Serializable
data class PairConfirmRequest(
    val code: String,
    @kotlinx.serialization.SerialName("device_name") val deviceName: String,
)

@Serializable
data class PairConfirmResponse(
    @kotlinx.serialization.SerialName("device_id") val deviceId: String,
    val token: String,
)

/** `params` es JsonObject (no Map<String,String>) a propósito: NOPAL espera
 * tipos reales por acción (ej. "on": true como booleano JSON, no el string
 * "true") -- `dispatch_action` en tunascreen_service.py hace `bool(params.get("on"))`,
 * que con un string "false" daría True por error. Construir con
 * `buildJsonObject { put("on", true) }` de kotlinx.serialization.json. */
@Serializable
data class ActionRequest(
    @kotlinx.serialization.SerialName("machine_id") val machineId: String,
    val action: String,
    val params: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class ActionResponse(
    val success: Boolean = false,
    val action: String? = null,
    @kotlinx.serialization.SerialName("machine_id") val machineId: String? = null,
)

@Serializable
data class MachinesResponse(
    @kotlinx.serialization.SerialName("api_version") val apiVersion: Int = 1,
    val machines: List<Machine> = emptyList(),
)

@Serializable
data class MachineMacro(
    val name: String,
    val description: String = "",
)

@Serializable
data class MacrosResponse(
    val macros: List<MachineMacro> = emptyList(),
)

@Serializable
data class ConsoleMessage(
    val message: String = "",
    val type: String? = null,
    val time: Double? = null,
)

@Serializable
data class ConsoleResponse(
    val messages: List<ConsoleMessage> = emptyList(),
)
