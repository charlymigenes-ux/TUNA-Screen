package com.nopal.tunascreen.feature.workshop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.repository.ConnectionState
import com.nopal.tunascreen.core.repository.NopalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val repository: NopalRepository,
) : ViewModel() {

    val machines: StateFlow<List<Machine>> = repository.machines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionState.DISCONNECTED)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        // Modo Taller es la "casa" de la app una vez pareada -- acá se abre
        // el WS que se mantiene mientras el usuario navega entre Taller y
        // Máquina (ver NopalRepository.start(), es idempotente).
        repository.start()
    }

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            repository.refreshOnce()
            _refreshing.value = false
        }
    }

    fun pause(machineId: String) = sendAction(machineId, "pause")

    fun cancel(machineId: String) = sendAction(machineId, "cancel")

    private fun sendAction(machineId: String, action: String) {
        viewModelScope.launch {
            repository.sendAction(machineId, action, JsonObject(emptyMap()))
        }
    }
}
