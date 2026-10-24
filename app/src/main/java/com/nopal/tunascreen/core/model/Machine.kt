package com.nopal.tunascreen.core.model

import kotlinx.serialization.Serializable

/** `type` que devuelve NOPAL: "printer" | "laser" | "cnc" -- láser y CNC son
 * el mismo hardware/servicio del lado NOPAL (laser_service.py), distintos
 * solo por su campo `kind`; acá se modelan como valores de `type` para que
 * la UI decida el layout (jog XY vs. spindle) sin necesitar saber eso. */
@Serializable
enum class MachineType {
    @kotlinx.serialization.SerialName("printer") PRINTER,
    @kotlinx.serialization.SerialName("laser") LASER,
    @kotlinx.serialization.SerialName("cnc") CNC,
}

@Serializable
data class TempPair(
    val current: Double? = null,
    val target: Double? = null,
)

@Serializable
data class Position(
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
)

@Serializable
data class JobProgress(
    val filename: String? = null,
    val percent: Int? = null,
    // El servidor puede mandar segundos con decimales (p. ej. 18404.52): se
    // aceptan como Double y se exponen como segundos enteros.
    @kotlinx.serialization.SerialName("elapsed_s") private val elapsedRaw: Double? = null,
    @kotlinx.serialization.SerialName("remaining_s") private val remainingRaw: Double? = null,
) {
    val elapsedS: Long? get() = elapsedRaw?.toLong()
    val remainingS: Long? get() = remainingRaw?.toLong()
}

/** Vocabulario normalizado por tunascreen_service.py: idle/printing/paused/
 * completed/cancelled/error/offline/unknown -- una sola marca de estado para
 * las 5 marcas de impresora + láser/CNC. */
@Serializable
data class MachineCamera(
    @kotlinx.serialization.SerialName("stream_url") val streamUrl: String? = null,
    val name: String? = null,
)

@Serializable
data class MachineStatus(
    val state: String = "unknown",
    val hotend: TempPair? = null,
    val bed: TempPair? = null,
    val position: Position? = null,
    @kotlinx.serialization.SerialName("homed_axes") val homedAxes: String? = null,
    val feed: Double? = null,
    val speed: Double? = null,
    @kotlinx.serialization.SerialName("laser_power") val laserPower: Double? = null,
    @kotlinx.serialization.SerialName("spindle_rpm") val spindleRpm: Double? = null,
    @kotlinx.serialization.SerialName("air_assist") val airAssist: Boolean? = null,
    val coolant: Boolean? = null,
    @kotlinx.serialization.SerialName("fan_percent") val fanPercent: Double? = null,
    @kotlinx.serialization.SerialName("speed_factor") val speedFactor: Double? = null,
    @kotlinx.serialization.SerialName("flow_factor") val flowFactor: Double? = null,
    @kotlinx.serialization.SerialName("z_offset") val zOffset: Double? = null,
    val camera: MachineCamera? = null,
    val job: JobProgress? = null,
)

@Serializable
data class Machine(
    val id: String,
    val name: String,
    val type: MachineType,
    val driver: String = "unknown",
    val online: Boolean = false,
    val capabilities: List<Capability> = emptyList(),
    val actions: List<String> = emptyList(),
    val status: MachineStatus = MachineStatus(),
) {
    fun has(capability: Capability): Boolean = capabilities.contains(capability)
    fun supports(action: String): Boolean = online && actions.contains(action)
}
