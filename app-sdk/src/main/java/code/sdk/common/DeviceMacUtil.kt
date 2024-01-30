package code.sdk.common

import android.content.Context
import android.net.wifi.WifiManager
import code.util.AppGlobal
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.net.NetworkInterface

/**
 * @author:Joshua
 * @description:
 * @date :2023/9/21
 */
object DeviceMacUtil {
    fun getMacAddress(): String {
        try {
            for (net in NetworkInterface.getNetworkInterfaces()) {
                if (!net.name.equals("wlan0", true)) continue
                val hardBuffer = net.hardwareAddress
                if (hardBuffer != null) {
                    val result = StringBuilder()
                    for (byte in hardBuffer) {
                        result.append(String.format("%02X", byte))
                    }
                    return result.toString()
                }
            }
            return getMacFromCommand() ?: ""
        } catch (exception: Exception) {
            val wifiManager = AppGlobal.application?.applicationContext?.getSystemService(
                Context.WIFI_SERVICE
            ) as? WifiManager
            return wifiManager?.connectionInfo?.macAddress ?: ""

        }
    }

    private fun getMacFromCommand(): String? {
        var macAddress: String? = null
        var buffer = ""
        val pp = Runtime.getRuntime().exec("cat/sys/class/net/wlan0/address")
        val inputStreamReader = InputStreamReader(pp.inputStream)
        val lineNumberReader = LineNumberReader(inputStreamReader)
        while (null != buffer) {
            buffer = lineNumberReader.readLine()
            if (buffer != null) {
                macAddress = buffer.trim { it <= ' ' }
                break
            }
        }
        return macAddress
    }
}