package com.app.rondacanaria.data.network

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    const val DEFAULT_PORT = 8888

    /**
     * Obtiene la dirección IPv4 local del dispositivo en la interfaz activa (Wi-Fi o Hotspot AP).
     * Ignora interfaces loopback (127.0.0.1) e interfaces inactivas.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val sortedInterfaces = interfaces.sortedByDescending { iface ->
                when {
                    iface.name.startsWith("wlan") -> 3
                    iface.name.startsWith("ap") -> 2
                    iface.name.startsWith("rndis") -> 1
                    else -> 0
                }
            }

            for (iface in sortedInterfaces) {
                if (!iface.isUp || iface.isLoopback) continue

                val addresses = Collections.list(iface.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress
                        if (!hostAddress.isNullOrBlank() && !hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
