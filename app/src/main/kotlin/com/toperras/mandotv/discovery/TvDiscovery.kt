package com.toperras.mandotv.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.ArrayDeque

/** Tele encontrada en la red. */
data class DiscoveredTv(val name: String, val host: String)

/**
 * Busca teles Android TV / Google TV en la wifi mediante mDNS
 * (servicio «_androidtvremote2._tcp», el mismo que usa la app oficial).
 *
 * Usa NsdManager del sistema, que gestiona él mismo el multicast: no hace
 * falta MulticastLock ni permisos extra. La búsqueda se detiene sola al
 * dejar de recoger el Flow (al salir de la pantalla).
 */
class TvDiscovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun androidTvs(): Flow<List<DiscoveredTv>> = callbackFlow {
        val found = linkedMapOf<String, DiscoveredTv>()
        // NsdManager solo resuelve un servicio a la vez: los ponemos en cola.
        val pending = ArrayDeque<NsdServiceInfo>()
        var resolving = false

        fun resolveNext() {
            if (resolving) return
            val next = pending.pollFirst() ?: return
            resolving = true
            @Suppress("DEPRECATION") // La alternativa solo existe desde Android 14; esta funciona en todas.
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    resolving = false
                    resolveNext()
                }

                override fun onServiceResolved(info: NsdServiceInfo) {
                    @Suppress("DEPRECATION")
                    val host = info.host?.hostAddress
                    if (host != null && !host.contains(':')) { // solo IPv4
                        found[info.serviceName] = DiscoveredTv(info.serviceName, host)
                        trySend(found.values.toList())
                    }
                    resolving = false
                    resolveNext()
                }
            })
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

            override fun onServiceFound(info: NsdServiceInfo) {
                pending.add(info)
                resolveNext()
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                found.remove(info.serviceName)
                trySend(found.values.toList())
            }
        }

        trySend(emptyList())
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { nsd.stopServiceDiscovery(listener) } }
    }

    private companion object {
        const val SERVICE_TYPE = "_androidtvremote2._tcp."
    }
}
