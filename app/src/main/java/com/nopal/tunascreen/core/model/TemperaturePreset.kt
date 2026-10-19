package com.nopal.tunascreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Perfil de precalentamiento por material; vienen de la configuración de
 * NOPAL (GET /api/tunascreen/temperature-presets). */
@Serializable
data class TemperaturePreset(
    val id: String,
    val name: String,
    val extruder: Double,
    @SerialName("heater_bed") val heaterBed: Double,
)

@Serializable
data class TemperaturePresetsResponse(
    val active: String? = null,
    val materials: List<TemperaturePreset> = emptyList(),
)

/** Valores de NOPAL por defecto, solo si el servidor aún no expone el endpoint. */
val DefaultTemperaturePresets = listOf(
    TemperaturePreset("pla", "PLA", 200.0, 60.0),
    TemperaturePreset("petg", "PETG", 235.0, 75.0),
    TemperaturePreset("abs", "ABS", 245.0, 100.0),
    TemperaturePreset("tpu", "TPU", 220.0, 50.0),
)
