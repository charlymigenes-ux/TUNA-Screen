package com.nopal.tunascreen.feature.workshop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nopal.tunascreen.core.auth.NopalSession
import com.nopal.tunascreen.core.auth.TokenStore
import com.nopal.tunascreen.core.repository.ConnectionState
import com.nopal.tunascreen.core.repository.NopalRepository
import com.nopal.tunascreen.ui.theme.AppearanceStore
import com.nopal.tunascreen.ui.theme.TunaThemeMode
import com.nopal.tunascreen.ui.theme.DisplayModeOverride
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    tokenStore: TokenStore,
    private val appearanceStore: AppearanceStore,
    private val repository: NopalRepository,
) : ViewModel() {

    val session: StateFlow<NopalSession> = tokenStore.session.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NopalSession(null, null, null, null),
    )

    val connectionState: StateFlow<ConnectionState> = repository.connectionState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConnectionState.DISCONNECTED,
    )

    val themeMode: StateFlow<TunaThemeMode> = appearanceStore.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TunaThemeMode.NOPAL,
    )

    val displayMode: StateFlow<DisplayModeOverride> = appearanceStore.displayMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DisplayModeOverride.AUTO,
    )

    fun setThemeMode(themeMode: TunaThemeMode) {
        viewModelScope.launch {
            appearanceStore.setThemeMode(themeMode)
        }
    }

    fun setDisplayMode(displayMode: DisplayModeOverride) {
        viewModelScope.launch {
            appearanceStore.setDisplayMode(displayMode)
        }
    }

    fun forgetPairing(onFinished: () -> Unit) {
        viewModelScope.launch {
            repository.forgetPairing()
            onFinished()
        }
    }
}
