package com.nopal.tunascreen.feature.machine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachineMacro
import com.nopal.tunascreen.core.model.ConsoleMessage
import com.nopal.tunascreen.core.repository.NopalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class MachineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NopalRepository,
) : ViewModel() {

    val machineId: String = checkNotNull(savedStateHandle.get<String>("machineId"))

    val machine: StateFlow<Machine?> = repository.machines
        .map { list -> list.find { it.id == machineId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.machine(machineId))

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _macros = MutableStateFlow<List<MachineMacro>>(emptyList())
    val macros: StateFlow<List<MachineMacro>> = _macros.asStateFlow()

    private val _console = MutableStateFlow<List<ConsoleMessage>>(emptyList())
    val console: StateFlow<List<ConsoleMessage>> = _console.asStateFlow()

    fun home(axes: String = "") = dispatch(
        "home",
        if (axes.isBlank()) JsonObject(emptyMap()) else buildJsonObject { put("axes", axes) },
    )
    fun pause() = dispatch("pause")
    fun resume() = dispatch("resume")
    fun cancel() = dispatch("cancel")
    fun move(axis: String, distance: Double, feed: Double = 1500.0) =
        dispatch("move", buildJsonObject {
            put("axis", axis)
            put("distance", distance)
            put("feed", feed)
        })

    fun extrude(distance: Double, feed: Double = 300.0) =
        dispatch("extrude", buildJsonObject {
            put("distance", distance)
            put("feed", feed)
        })

    fun setTemperature(heater: String, target: Double) =
        dispatch("set_temperature", buildJsonObject {
            put("heater", heater)
            put("target", target)
        })

    fun setLaserPower(on: Boolean, power: Double) =
        dispatch("set_laser_power", buildJsonObject {
            put("on", on)
            put("power", power)
        })

    fun setAirAssist(on: Boolean) =
        dispatch("set_air_assist", buildJsonObject { put("on", on) })

    fun setWorkZero(axes: String = "XYZ") =
        dispatch("set_work_zero", buildJsonObject { put("axes", axes) })

    fun setSpindle(on: Boolean, rpm: Double) =
        dispatch("set_spindle", buildJsonObject {
            put("on", on)
            put("rpm", rpm)
        })

    fun setCoolant(on: Boolean) =
        dispatch("set_coolant", buildJsonObject { put("on", on) })

    fun setFan(percent: Double) =
        dispatch("set_fan", buildJsonObject { put("percent", percent) })

    fun setSpeedFactor(percent: Double) =
        dispatch("set_speed_factor", buildJsonObject { put("percent", percent) })

    fun setFlowFactor(percent: Double) =
        dispatch("set_flow_factor", buildJsonObject { put("percent", percent) })

    fun setZOffset(offset: Double) =
        dispatch("set_z_offset", buildJsonObject { put("offset", offset) })

    fun runMacro(name: String) =
        dispatch("run_macro", buildJsonObject { put("macro", name) })

    fun sendConsoleCommand(command: String) =
        dispatch("send_console_command", buildJsonObject { put("command", command) })

    fun loadMacros() {
        viewModelScope.launch {
            repository.getMacros(machineId)
                .onSuccess { _macros.value = it }
                .onFailure { _actionError.value = "No se pudieron cargar los macros" }
        }
    }

    fun loadConsole(count: Int = 50) {
        viewModelScope.launch {
            repository.getConsole(machineId, count)
                .onSuccess { _console.value = it }
                .onFailure { _actionError.value = "No se pudo cargar la consola" }
        }
    }

    private fun dispatch(action: String, params: JsonObject = JsonObject(emptyMap())) {
        viewModelScope.launch {
            _actionError.value = null
            repository.sendAction(machineId, action, params)
                .onFailure { _actionError.value = "No se pudo ejecutar \"$action\"" }
        }
    }

    fun dismissError() {
        _actionError.value = null
    }
}
