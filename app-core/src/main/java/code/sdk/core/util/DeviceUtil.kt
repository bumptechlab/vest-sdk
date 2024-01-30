package code.sdk.core.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings.Secure
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Pair
import code.util.AppGlobal
import code.util.LogUtil
import java.io.File
import java.net.InetAddress
import java.util.Locale
import java.util.UUID

object DeviceUtil {
    private val TAG = DeviceUtil::class.java.simpleName

    /* public */
    private const val XIAOMI_VIRTUAL_DEVICE_ID_NULL = "0000000000000000"

    /**
     * 判断是否有能同步获取到的设备ID（包括sp、file存储）
     *
     * @return Pair <deviceId></deviceId>,isReadFromFile>
     */
    fun preGetDeviceID(): Pair<String?, Boolean> {
        var deviceId: String? = PreferenceUtil.readDeviceID()
        var isReadFromFile = false
        if (deviceId.isNullOrEmpty()) {
            deviceId = readDeviceIDFromFile()
            LogUtil.d(TAG, "getDeviceId: CacheFile: $deviceId")
            isReadFromFile = !deviceId.isNullOrEmpty()
        }
        //GSF ID
        if (deviceId.isNullOrEmpty()) {
            deviceId = gsfAndroidId
            LogUtil.d(TAG, "getDeviceId: GoogleServiceFrameworkId: $deviceId")
        }

        //Android ID
        if (deviceId.isNullOrEmpty()) {
            deviceId = androidId
            LogUtil.d(TAG, "getDeviceId: AndroidId: $deviceId")
            if (XIAOMI_VIRTUAL_DEVICE_ID_NULL == deviceId) {
                deviceId = null
            }
        }
        if (!deviceId.isNullOrEmpty()) {
            saveDeviceID(deviceId, isReadFromFile)
        }
        return Pair.create(deviceId, isReadFromFile)
    }

    /**
     * get device id orderly
     * warn:avoid getting the deviceID before Adjust.OnDeviceIdsRead(in MainApplication) callback!
     *
     *
     * 1.GSF ID
     * 2.ANDROID_ID(XIAOMI can shutdown/reset the virtual ANDROID_ID )
     * 3.Adjust ID
     * 4.UUID
     *
     * @return device id
     */
    fun getDeviceID(): String? {
        val pair = preGetDeviceID()
        var deviceId = pair.first
        val isReadFromFile = pair.second
        if (deviceId.isNullOrEmpty()) {
            deviceId = googleAdId
            LogUtil.d(TAG, "getDeviceId: GoogleADId: $deviceId")
        }
        //UUID
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            LogUtil.d(TAG, "getDeviceId: UUID: $deviceId")
        }
        saveDeviceID(deviceId, isReadFromFile)
        LogUtil.d(TAG, "getDeviceId: DeviceId: $deviceId")
        return deviceId
    }

    var googleAdId: String? = null
        get() {
            field = PreferenceUtil.readGoogleADID() ?: ""
            return field
        }

    private fun saveDeviceID(deviceID: String?, isReadFromFile: Boolean) {
        PreferenceUtil.saveDeviceID(deviceID)
        if (!isReadFromFile) {
            saveDeviceIDToFile(deviceID)
        }
    }

    /**
     * get Google Service Framework id
     *
     * @return gsf id
     */
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
                        field =
                            if (id.isNullOrEmpty() || "null" == id) null else java.lang.Long.toHexString(
                                id.toLong()
                            )
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
        //ObfuscationStub7.inject();
        val telManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountryCode = telManager.simCountryIso
        return if (simCountryCode.isNullOrEmpty()) "" else simCountryCode
    }

    fun getNetworkCountryCode(context: Context): String {
        //ObfuscationStub8.inject();
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
            if (subIds.size > 0) {
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
    fun getSimCountryIsoBySubId(context: Context, subId: Int): String {
        //ObfuscationStub2.inject();
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
    fun getSimCountryIsoByPhoneId(context: Context, phoneId: Int): String {
        //ObfuscationStub3.inject();
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountryIsoObj = ReflectionUtil.invokeMethod(
            tm,
            "android.telephony.TelephonyManager", "getSimCountryIsoForPhone",
            arrayOf<Class<*>?>(Int::class.javaPrimitiveType), phoneId
        )
        return if (simCountryIsoObj == null) "" else (simCountryIsoObj as String)
    }

    fun getLanguage(context: Context): String {
        val locale: Locale?
        locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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

    fun openGooglePlay(context: Context, packageName: String): Boolean {
        var success = false
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(Uri.parse("market://details?id=$packageName"))
            intent.setPackage("com.android.vending")
            if (intent.resolveActivity(context.packageManager) != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                success = true
            } else { //没有应用市场，通过浏览器跳转到Google Play
                val intent2 = Intent(Intent.ACTION_VIEW)
                intent2.setData(Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                if (intent2.resolveActivity(context.packageManager) != null) {
                    intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    fun openBuildInMarket(context: Context, packageName: String): Boolean {
        var success = false
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(Uri.parse("market://details?id=$packageName")) //跳转到应用市场，非Google Play市场一般情况也实现了这个接口
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            //ObfuscationStub6.inject();
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

    /* public */ /* private */
    private var deviceIdFile: File? = null
        get() = File(
            AppGlobal.application!!.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "did.dat"
        )

    private fun saveDeviceIDToFile(deviceId: String?) {
        if (deviceId?.isNotEmpty()!!) {
            deviceIdFile?.writeText(deviceId, Charsets.UTF_8)
        }
    }

    private fun readDeviceIDFromFile(): String? {
        LogUtil.d(TAG, "getDeviceId: deviceIdFile=$deviceIdFile")
        return if (deviceIdFile?.exists()!!) deviceIdFile?.readText(charset = Charsets.UTF_8) else null
    }


    private var androidId: String? = null
        @SuppressLint("HardwareIds")
        get() = Secure.getString(AppGlobal.application?.contentResolver, Secure.ANDROID_ID)
    /* private */
}
