package book.sdk.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import android.database.Cursor
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import book.util.AppGlobal
import book.util.LogUtil
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Locale
import java.util.UUID

object DeviceUtil {
    private val TAG = DeviceUtil::class.java.simpleName

    private val INVALID_DEVICE_IDS = arrayOf(
        "00000000-0000-0000-0000-000000000000",
        "0000000000000000",
        "02:00:00:00:00:00", //Mac地址的不合法形式
        "9774d56d682e549c" //Android ID的不合法形式
    )

    private fun isInvalidDeviceId(deviceId: String?): Boolean {
        return deviceId.isNullOrEmpty() || INVALID_DEVICE_IDS.contains(deviceId)
    }

    /**
     * get device id orderly
     * remove Android ID because it will be changed after app signature changed
     *
     * 1.GSF ID
     * 2.GOOGLE AD ID
     * 3.UUID
     *
     * @return device id
     */
    fun getDeviceID(): String? {
        var deviceId: String? = PreferenceUtil.readDeviceID()
        //GSF ID
        if (isInvalidDeviceId(deviceId)) {
            deviceId = gsfAndroidId
            LogUtil.d(TAG, "getDeviceId: GoogleServiceFrameworkId: $deviceId")
        }
        //Google AD ID
        if (isInvalidDeviceId(deviceId)) {
            deviceId = googleAdId
            LogUtil.d(TAG, "getDeviceId: GoogleADId: $deviceId")
        }
        //UUID
        if (isInvalidDeviceId(deviceId)) {
            deviceId = UUID.randomUUID().toString()
            LogUtil.d(TAG, "getDeviceId: UUID: $deviceId")
        }
        PreferenceUtil.saveDeviceID(deviceId)
        LogUtil.d(TAG, "getDeviceId: DeviceId: $deviceId")
        return deviceId
    }

    var googleAdId: String? = null
        get() {
            field = PreferenceUtil.readGoogleADID() ?: ""
            return field
        }

    /**
     * get Google Service Framework id
     *
     * @return gsf id
     */
    @OptIn(ExperimentalStdlibApi::class)
    var gsfAndroidId: String? = null
        get() {
            if (field.isNullOrEmpty()) {
                var cursor: Cursor? = null
                try {
                    val URI = Uri.parse("content://com.google.android.gsf.gservices")
                    val ID_KEY = "android_id"
                    val params = arrayOf(ID_KEY)
                    cursor =
                        AppGlobal.application?.contentResolver?.query(URI, null, null, params, null)
                    if (cursor != null && cursor.moveToFirst() && cursor.columnCount >= 2) {
                        val id = cursor.getString(1)
                        field = if (id.isNullOrEmpty() || "null" == id) null else id.toLong()
                            .toHexString()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    cursor?.close()
                }
            }
            return field
        }

    fun getSimCountryCode(context: Context): String {
        val telManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountryCode = telManager.simCountryIso
        return if (simCountryCode.isNullOrEmpty()) "" else simCountryCode
    }

    fun getNetworkCountryCode(context: Context): String {
        val telManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkCountryCode = telManager.networkCountryIso
        return if (networkCountryCode.isNullOrEmpty()) "" else networkCountryCode
    }

    fun getAllSimCountryIso(context: Context): List<String> {
        val countryIso: MutableList<String> = ArrayList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val slotCount = subManager.activeSubscriptionInfoCountMax
            for (i in 0 until slotCount) {
                val subscriptionId = getSubIdBySlotId(context, i)
                var curCountryIso = getSimCountryIsoBySubId(context, subscriptionId)
                //get SimCountryIso by phoneId if can not get by subId
                if (curCountryIso.isNullOrEmpty()) {
                    curCountryIso = getSimCountryIsoByPhoneId(context, i)
                }
                if (!curCountryIso.isNullOrEmpty()) {
                    countryIso.add(curCountryIso)
                }
            }
        }
        //we can get default country iso at least
        if (countryIso.isEmpty()) {
            countryIso.add(getSimCountryCode(context))
        }
        return countryIso
    }

    private fun getSubIdBySlotId(context: Context, slotIndex: Int): Int {
        var subId = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val sm =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val obj = ReflectionUtil.invokeMethod(
                sm, "android.telephony.SubscriptionManager", "getSubId", arrayOf(
                    Int::class.javaPrimitiveType
                ), slotIndex
            )
            val subIds = if (obj == null) intArrayOf() else (obj as IntArray)
            if (subIds.isNotEmpty()) {
                subId = subIds[0]
            }
        }
        return subId
    }

    /**
     * sdk版本<=29用这个方法获取
     *
     * @param context
     * @param subId
     * @return
     */
    private fun getSimCountryIsoBySubId(context: Context, subId: Int): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountryIsoObj = ReflectionUtil.invokeMethod(
            tm,
            "android.telephony.TelephonyManager", "getSimCountryIso",
            arrayOf<Class<*>?>(Int::class.javaPrimitiveType), subId
        )
        return if (simCountryIsoObj == null) "" else (simCountryIsoObj as String)
    }

    /**
     * sdk版本>29用这个方法获取，但是phoneId不确定，可取0-10之间数值
     *
     * @param context
     * @param phoneId
     * @return
     */
    private fun getSimCountryIsoByPhoneId(context: Context, phoneId: Int): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountryIsoObj = ReflectionUtil.invokeMethod(
            tm,
            "android.telephony.TelephonyManager", "getSimCountryIsoForPhone",
            arrayOf<Class<*>?>(Int::class.javaPrimitiveType), phoneId
        )
        return if (simCountryIsoObj == null) "" else (simCountryIsoObj as String)
    }

    fun getLanguage(context: Context): String {
        val locale: Locale? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }
        var language = ""
        if (locale != null) {
            language = locale.language
        }
        if (language.isNullOrEmpty()) {
            language = Locale.getDefault().language
        }
        return if (language.isNullOrEmpty()) "" else language.lowercase(Locale.getDefault())
    }

    fun openMarket(context: Context, packageName: String): Boolean {
        var success = openGooglePlay(context, packageName)
        if (!success) {
            LogUtil.d(TAG, "Open GooglePlay fail, use build-in market")
            success = openBuildInMarket(context, packageName)
        }
        return success
    }

    fun finishActivitySafety(activity: Activity) {
        try {
            activity.finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openGooglePlay(context: Context, packageName: String): Boolean {
        var success = false
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
            intent.setPackage("com.android.vending")
            if (intent.resolveActivity(context.packageManager) != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                success = true
            } else { //没有应用市场，通过浏览器跳转到Google Play
                val intent2 = Intent(Intent.ACTION_VIEW)
                intent2.data =
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                if (intent2.resolveActivity(context.packageManager) != null) {
                    intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent2)
                    success = true
                } else {
                    LogUtil.w(TAG, "Can not find any component to open GooglePlay")
                }
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, "Open GooglePlay fail", e)
        }
        return success
    }

    private fun openBuildInMarket(context: Context, packageName: String): Boolean {
        var success = false
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data =
                    Uri.parse("market://details?id=$packageName") //跳转到应用市场，非Google Play市场一般情况也实现了这个接口
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            success = true
        } catch (e: Exception) {
            LogUtil.e(TAG, "Open BuildIn Market fail", e)
        }
        return success
    }

    fun getPackageInfo(context: Context, packageName: String?): PackageInfo? {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(packageName!!, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return packageInfo
    }

    fun getINetAddress(host: String?): String? {
        var hostAddress: String? = ""
        try {
            val inetAddress = InetAddress.getByName(host)
            hostAddress = inetAddress.hostAddress
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hostAddress
    }


    fun findActivity(context: Context?): Activity? {
        if (context is Activity) {
            return context
        }
        return if (context is ContextWrapper) {
            findActivity(context.baseContext)
        } else {
            null
        }
    }

    fun isDomainAvailable(host: String?): Boolean {
        var isAvailable = false
        try {
            val ReturnStr = InetAddress.getByName(host)
            val IPAddress = ReturnStr.hostAddress
            isAvailable = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isAvailable
    }

    /**
     * 需要权限：android.permission.ACCESS_WIFI_STATE
     */
    @Deprecated("此方法已经无法获取到mac地址")
    var macAddress: String? = null
        get() {
            try {
                var wifiInterfaceName = ""
                for (net in NetworkInterface.getNetworkInterfaces()) {
                    if (net.name.equals("wlan0", true)) {
                        wifiInterfaceName = net.name
                        break
                    }
                }
                if (wifiInterfaceName.isNotEmpty()) {
                    var macBytes = NetworkInterface.getByName(wifiInterfaceName).hardwareAddress
                    if (macBytes != null) {
                        var macBuilder = StringBuilder()
                        for ((index, value) in macBytes.withIndex()) {
                            macBuilder.append(String.format("%02X", value))
                            if (index < macBytes.size - 1) {
                                macBuilder.append(":")
                            }
                        }
                        return macBuilder.toString()
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
        val pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address")
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
    /* public */

}
