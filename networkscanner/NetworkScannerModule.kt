package networkscanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.util.Log
import com.facebook.react.bridge.*
import com.networkscanner.NativeNetworkScannerSpec
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NetworkScannerModule(reactContext: ReactApplicationContext) : NativeNetworkScannerSpec(reactContext) {

    override fun getName() = NAME

    private fun getGatewayIP(): String? {
        val connectivityManager = reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network = connectivityManager.activeNetwork ?: return null
        val linkProperties: LinkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null

        return linkProperties.routes
            .firstOrNull { it.isDefaultRoute }
            ?.gateway?.hostAddress
    }

    @ReactMethod
    override fun scanNetwork(promise: Promise) {
        val gatewayIP = getGatewayIP() ?: return promise.reject("ERROR", "Failed to get IP GATEWAY")
        val subnet = gatewayIP.substringBeforeLast(".") // e.g., "192.168.1"

        val executor = Executors.newFixedThreadPool(20)
        val devices: WritableArray = Arguments.createArray()

        val taskCounter = AtomicInteger(254) // Untuk memastikan semua task selesai sebelum resolve
        for (i in 1..254) {
            executor.execute {
                try {
                    val ipAddress = "$subnet.$i"
                    val inetAddress = InetAddress.getByName(ipAddress)

                    if (inetAddress.isReachable(500)) {
                        Log.d("FIP", ipAddress)
                        isSonoff(ipAddress) { isSonoff ->
                            if (isSonoff) {
                                synchronized(devices) { devices.pushString(ipAddress) }
                            }
                            if (taskCounter.decrementAndGet() == 0) {
                                promise.resolve(devices)
                            }
                        }
                    } else {
                        Log.d("IP NOT FOUND", "not found")
                        if (taskCounter.decrementAndGet() == 0) {
                            promise.resolve(devices)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NetworkScanner", "Error scanning $i", e)
                    if (taskCounter.decrementAndGet() == 0) {
                        promise.resolve(devices)
                    }
                }
            }
        }
        executor.shutdown()
    }

    private fun isSonoff(ipAddress: String, callback: (Boolean) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val url = URL("http://$ipAddress/cm?cmnd=Status")
                val connection = url.openConnection()
                connection.connectTimeout = 100
                connection.getInputStream().use {
                    val response = it.bufferedReader().readText()
                    Log.d("Response Success", "$ipAddress $response")
                    if (response.contains("WARNING") || response.contains("FriendlyName")) {
                        callback(true)
                        return@execute
                    }
                }
            } catch (e: Exception) {
                Log.d("Response Err", e.message.toString())
            }
            callback(false)
        }
    }

    companion object {
        const val NAME = "NetworkScanner"
    }
}
