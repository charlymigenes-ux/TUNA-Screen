package com.nopal.tunascreen.core.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.nopal.tunascreen.core.model.NopalServer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val SERVICE_TYPE = "_nopal._tcp."

/**
 * Descubrimiento por mDNS/NSD nativo de Android -- NOPAL todavía NO anuncia
 * este servicio en la red (ver plan: es un follow-up del lado backend con
 * python-zeroconf), así que en la práctica hoy este flow nunca emite nada
 * y la pantalla de Discovery siempre cae al campo de IP manual. Se deja
 * implementado igual porque el lado Android no depende de ese follow-up
 * para funcionar -- cuando NOPAL empiece a anunciarse, esto funciona sin
 * tocar la app.
 */
@Singleton
class NopalDiscoveryService @Inject constructor(@ApplicationContext private val context: Context) {

    fun discover(): Flow<NopalServer> = callbackFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: return
                trySend(NopalServer(host = host, port = serviceInfo.port, name = serviceInfo.serviceName))
            }
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                nsdManager.resolveService(serviceInfo, resolveListener)
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        }
    }
}
