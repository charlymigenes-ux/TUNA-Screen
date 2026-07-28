package com.nopal.tunascreen.core.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "tuna_screen_session")

/** Única fuente de verdad de la sesión pareada -- host/puerto/token/
 * device_id de NOPAL. Si `token` es null, la app todavía no está pareada
 * y debe mostrar Discovery/Pairing en vez de saltar directo a Taller/Máquina. */
data class NopalSession(
    val host: String?,
    val port: Int?,
    val token: String?,
    val deviceId: String?,
) {
    val isPaired: Boolean get() = !host.isNullOrBlank() && !token.isNullOrBlank()
    val baseUrl: String? get() = if (host != null && port != null) "http://$host:$port" else null
}

@Singleton
class TokenStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val TOKEN = stringPreferencesKey("token")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    val session: Flow<NopalSession> = context.dataStore.data.map { prefs ->
        NopalSession(
            host = prefs[Keys.HOST],
            port = prefs[Keys.PORT],
            token = prefs[Keys.TOKEN],
            deviceId = prefs[Keys.DEVICE_ID],
        )
    }

    suspend fun currentSession(): NopalSession = session.first()

    /** Token leído de forma sincrónica-bloqueante por el Interceptor de
     * OkHttp (que no es suspend) -- ver NetworkModule.authInterceptor. */
    fun currentTokenBlocking(): String? = kotlinx.coroutines.runBlocking { currentSession().token }

    fun currentBaseUrlBlocking(): String? = kotlinx.coroutines.runBlocking { currentSession().baseUrl }

    suspend fun savePairing(host: String, port: Int, token: String, deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOST] = host
            prefs[Keys.PORT] = port
            prefs[Keys.TOKEN] = token
            prefs[Keys.DEVICE_ID] = deviceId
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
