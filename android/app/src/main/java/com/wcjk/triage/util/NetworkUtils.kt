package com.wcjk.triage.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    fun isAvailable(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun localIp(ctx: Context): String {
        try {
            NetworkInterface.getNetworkInterfaces().asSequence().forEach { ni ->
                ni.inetAddresses.asSequence().forEach { a ->
                    if (!a.isLoopbackAddress && a is Inet4Address) return a.hostAddress ?: ""
                }
            }
        } catch (_: Exception) {}
        return ""
    }

    fun macAddress(): String {
        try {
            NetworkInterface.getNetworkInterfaces().asSequence().forEach { ni ->
                if (!ni.name.equals("wlan0", true)) return@forEach
                val mac = ni.hardwareAddress ?: return ""
                return mac.joinToString(":") { "%02X".format(it) }
            }
        } catch (_: Exception) {}
        return ""
    }
}
