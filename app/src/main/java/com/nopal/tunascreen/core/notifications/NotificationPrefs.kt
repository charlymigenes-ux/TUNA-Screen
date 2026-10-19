package com.nopal.tunascreen.core.notifications

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Eventos de máquina que pueden avisar al usuario. */
enum class AlertEvent(val storageKey: String, val title: String, val detail: String) {
    HEATING_READY("heating_ready", "Calentamiento listo", "El extrusor o la cama llegaron a su temperatura."),
    COOLED_DOWN("cooled_down", "Enfriado", "El extrusor o la cama ya se enfriaron."),
    JOB_FINISHED("job_finished", "Trabajo terminado", "Una máquina terminó su trabajo."),
    JOB_PAUSED("job_paused", "Trabajo en pausa", "Un trabajo se pausó."),
    JOB_ERROR("job_error", "Error en una máquina", "Una máquina reporta un error."),
    MACHINE_OFFLINE("machine_offline", "Máquina sin conexión", "Una máquina dejó de responder."),
}

data class NotificationPrefs(
    val enabled: Boolean = true,
    val visual: Boolean = true,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val events: Set<AlertEvent> = AlertEvent.entries.toSet(),
)

private val Context.notificationDataStore by preferencesDataStore(name = "tuna_screen_notifications")

@Singleton
class NotificationStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val visualKey = booleanPreferencesKey("visual")
    private val soundKey = booleanPreferencesKey("sound")
    private val vibrateKey = booleanPreferencesKey("vibrate")
    private fun eventKey(event: AlertEvent) = booleanPreferencesKey("event_${event.storageKey}")

    val prefs: Flow<NotificationPrefs> = context.notificationDataStore.data.map { p: Preferences ->
        NotificationPrefs(
            enabled = p[enabledKey] ?: true,
            visual = p[visualKey] ?: true,
            sound = p[soundKey] ?: true,
            vibrate = p[vibrateKey] ?: true,
            events = AlertEvent.entries.filter { p[eventKey(it)] ?: true }.toSet(),
        )
    }

    suspend fun setEnabled(value: Boolean) = update { it[enabledKey] = value }
    suspend fun setVisual(value: Boolean) = update { it[visualKey] = value }
    suspend fun setSound(value: Boolean) = update { it[soundKey] = value }
    suspend fun setVibrate(value: Boolean) = update { it[vibrateKey] = value }
    suspend fun setEvent(event: AlertEvent, value: Boolean) = update { it[eventKey(event)] = value }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        context.notificationDataStore.edit(block)
    }
}
