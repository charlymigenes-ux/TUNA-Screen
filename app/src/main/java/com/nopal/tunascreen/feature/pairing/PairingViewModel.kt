package com.nopal.tunascreen.feature.pairing

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopal.tunascreen.core.repository.NopalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val code: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val paired: Boolean = false,
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val repository: NopalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(code = value.filter(Char::isDigit).take(6), error = null)
    }

    fun confirm(host: String, port: Int) {
        val code = _uiState.value.code
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(error = "El código tiene 6 dígitos")
            return
        }
        _uiState.value = _uiState.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            val result = repository.confirmPairing("http://$host:$port", code, deviceName.ifBlank { "TUNA-Screen" })
            result.onSuccess {
                _uiState.value = _uiState.value.copy(submitting = false, paired = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(submitting = false, error = "Código inválido o vencido")
            }
        }
    }
}
