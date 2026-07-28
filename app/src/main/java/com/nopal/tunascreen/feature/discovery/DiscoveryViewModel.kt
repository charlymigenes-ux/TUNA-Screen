package com.nopal.tunascreen.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopal.tunascreen.core.model.NopalServer
import com.nopal.tunascreen.core.network.NopalDiscoveryService
import com.nopal.tunascreen.core.repository.NopalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveryUiState(
    val discovered: List<NopalServer> = emptyList(),
    val manualHost: String = "",
    val manualPort: String = "8420",
    val checking: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryService: NopalDiscoveryService,
    private val repository: NopalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        // Ver NopalDiscoveryService: hoy nunca emite nada porque NOPAL
        // todavía no anuncia _nopal._tcp -- se deja andando igual para que
        // funcione solo cuando ese anuncio se agregue del lado backend.
        viewModelScope.launch {
            discoveryService.discover().collect { server ->
                _uiState.value = _uiState.value.copy(
                    discovered = _uiState.value.discovered.filterNot { it.host == server.host } + server,
                )
            }
        }
    }

    fun onManualHostChange(value: String) {
        _uiState.value = _uiState.value.copy(manualHost = value, error = null)
    }

    fun onManualPortChange(value: String) {
        _uiState.value = _uiState.value.copy(manualPort = value, error = null)
    }

    /** Confirma que host:puerto responde como un NOPAL real antes de dejar
     * avanzar a Pairing -- evita mandar al usuario a tipear un código de 6
     * dígitos contra una IP que ni siquiera es NOPAL. */
    fun checkAndProceed(host: String, port: Int, onVerified: () -> Unit) {
        _uiState.value = _uiState.value.copy(checking = true, error = null)
        viewModelScope.launch {
            val result = repository.checkServer("http://$host:$port")
            _uiState.value = _uiState.value.copy(checking = false)
            result.onSuccess { onVerified() }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        error = "No se pudo conectar con $host:$port -- ¿es un servidor NOPAL?",
                    )
                }
        }
    }
}
