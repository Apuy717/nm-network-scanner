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

class NetworkScannerModule(reactContext: ReactApplicationContext) : NativeNetworkScannerSpec(reactContext) {

    override fun getName() = NAME

    private fun getGatewayIP(): String? {
        val connectivityManager = reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network = connectivityManager.activeNetwork ?: return null
        val linkProperties: LinkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null

        return linkProperties.routes
            .firstOrNull { it.isDefaultRoute } // Cari default gateway
            ?.gateway?.hostAddress
    }

    @ReactMethod
    override fun scanNetwork(promise: Promise) {
        val gatewayIP = getGatewayIP() ?: return promise.reject("ERROR", "Failed Got IP GATEWAY")
        val subnet = gatewayIP.substringBeforeLast(".") // Sample got: "192.168.1"

        val executor = Executors.newFixedThreadPool(20)
        val devices : WritableArray = Arguments.createArray()

        for (i in 1..254) {
            executor.execute {
                try {
                    val ipAddress = "$subnet.$i"
                    val inetAddress = InetAddress.getByName(ipAddress)

                    if (inetAddress.isReachable(100)) {
                        if (isSonoff(ipAddress)) {
                            synchronized(devices) { devices.pushString(ipAddress) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NetworkScanner", "Error scanning $i", e)
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)
        promise.resolve(devices)
    }


    private fun isSonoff(ipAddress: String): Boolean {
        try {
            val url = URL("http://$ipAddress/cm?cmnd=Status")
            val connection = url.openConnection()
            connection.connectTimeout = 500
            connection.getInputStream().use {
                val response = it.bufferedReader().readText()
                Log.d("Response", response)
                if (response.contains("{\"WARNING\":\"Need user=<username>&password=<password>\"}") || response.contains("FriendlyName")) {
                    return true
                }
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }

    companion object {
        const val NAME = "NetworkScanner"
    }
}
