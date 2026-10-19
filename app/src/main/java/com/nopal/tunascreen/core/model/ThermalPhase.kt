package com.nopal.tunascreen.core.model

/** Fase térmica de un calentador a partir de su temperatura actual y objetivo. */
enum class ThermalPhase { HEATING, COOLING, READY, IDLE }

/** Por debajo de esta temperatura (°C) un calentador apagado ya se considera frío. */
const val COLD_TEMP_C = 45.0

fun thermalPhaseOf(temp: TempPair?): ThermalPhase {
    val current = temp?.current ?: return ThermalPhase.IDLE
    val target = temp.target ?: 0.0
    return when {
        target > 0 && current < target - 2 -> ThermalPhase.HEATING
        target > 0 && current > target + 5 -> ThermalPhase.COOLING
        target > 0 -> ThermalPhase.READY
        current > COLD_TEMP_C -> ThermalPhase.COOLING
        else -> ThermalPhase.IDLE
    }
}
