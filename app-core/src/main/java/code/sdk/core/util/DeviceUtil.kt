package code.sdk.core.util

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
import android.text.TextUtils
import android.util.Pair
import code.util.AppGlobal.getApplication
import code.util.LogUtil.d
import code.util.LogUtil.e
import code.util.LogUtil.w
import java.io.File
import java.net.InetAddress
import java.util.Locale
import java.util.UUID

object DeviceUtil {
    private val TAG = DeviceUtil::class.java.simpleName

    /* public */
    private const val XIAOMI_VIRTUAL_DEVICEID_NULL = "0000000000000000"

    /**
     * 判断是否有能同步获取到的设备ID（包括sp、file存储）
     *
     * @return Pair <deviceId></deviceId>,isReadFromFile>
     */
    fun preGetDeviceID(): Pair<String?, Boolean> {
        var deviceID :String?= PreferenceUtil.readDeviceID()
        var isReadFromFile = false
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = readDeviceIDFromFile()
            d(TAG, "getDeviceId: CacheFile: %s", deviceID)
            isReadFromFile = !TextUtils.isEmpty(deviceID)
        }

        //GSF ID
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = getGsfAndroidId()
            d(TAG, "getDeviceId: GoogleServiceFrameworkId: %s", deviceID)
        }

        //Android ID
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = getAndroidID()
            d(TAG, "getDeviceId: AndroidId: %s", deviceID)
            if (XIAOMI_VIRTUAL_DEVICEID_NULL == deviceID) {
                deviceID = null
            }
        }
        if (!TextUtils.isEmpty(deviceID)) {
            saveDeviceID(deviceID, isReadFromFile)
        }
        return Pair.create(deviceID, isReadFromFile)
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
        var deviceID = pair.first
        val isReadFromFile = pair.second
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = getGoogleADID()
            d(TAG, "getDeviceId: GoogleADId: %s", deviceID)
        }
        //UUID
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = UUID.randomUUID().toString()
            d(TAG, "getDeviceId: UUID: %s", deviceID)
        }
        saveDeviceID(deviceID, isReadFromFile)
        d(TAG, "device-id:$deviceID")
        return deviceID
    }

    fun getGoogleADID(): String {
        //ObfuscationStub2.inject();
        val googleADID = PreferenceUtil.readGoogleADID()
        return if (!TextUtils.isEmpty(googleADID)) {
            googleADID
        } else ""
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
    private fun getGsfAndroidId(): String? {
        return try {
            val URI = Uri.parse("content://com.google.android.gsf.gservices")
            val ID_KEY = "android_id"
            val params = arrayOf(ID_KEY)
            val c = getApplication().contentResolver.query(URI, null, null, params, null)
            if (c == null || !c.moveToFirst() || c.columnCount < 2) return null
            val id = c.getString(1)
            if (TextUtils.isEmpty(id) || "null" == id) null else java.lang.Long.toHexString(id.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSimCountryCode(context: Context): String {
        //ObfuscationStub7.inject();
        val telManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountryCode = telManager.simCountryIso
        return if (TextUtils.isEmpty(simCountryCode)) "" else simCountryCode
    }

    fun getNetworkCountryCode(context: Context): String {
        //ObfuscationStub8.inject();
        val telManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkCountryCode = telManager.networkCountryIso
        return if (TextUtils.isEmpty(networkCountryCode)) "" else networkCountryCode
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
                if (TextUtils.isEmpty(curCountryIso)) {
                    curCountryIso = getSimCountryIsoByPhoneId(context, i)
                }
                if (!TextUtils.isEmpty(curCountryIso)) {
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
        if (TextUtils.isEmpty(language)) {
            language = Locale.getDefault().language
        }
        return if (TextUtils.isEmpty(language)) "" else language.lowercase(Locale.getDefault())
    }

    fun openMarket(context: Context, packageName: String): Boolean {
        var success = openGooglePlay(context, packageName)
        if (!success) {
            d(TAG, "Open GooglePlay fail, use build-in market")
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
                    w(TAG, "Can not find any component to open GooglePlay")
                }
            }
        } catch (e: Exception) {
            e(TAG, "Open GooglePlay fail", e)
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
            e(TAG, "Open BuildIn Market fail", e)
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

    fun gsfAndroidId(context: Context): String? {
        var gsfId: String? = ""
        try {
            val URI = Uri.parse("content://com.google.android.gsf.gservices")
            val ID_KEY = "android_id"
            val params = arrayOf(ID_KEY)
            val resolver = context.contentResolver
            if (resolver != null) {
                val cursor = resolver.query(
                    URI, null as Array<String?>?, null as String?, params,
                    null as String?
                )
                if (cursor != null) {
                    val c: Cursor = cursor
                    if (!c.moveToFirst() || c.columnCount < 2) {
                        return null
                    }
                    val id = c.getString(1)
                    if (!TextUtils.isEmpty(id) && id == "null") {
                        gsfId = java.lang.Long.toHexString(id.toLong())
                    }
                }
            }
        } catch (var7: Exception) {
            var7.printStackTrace()
        }
        return gsfId
    }

    /* public */ /* private */
    private fun saveDeviceIDToFile(deviceId: String?): Boolean {
        return if (TextUtils.isEmpty(deviceId)) {
            //ObfuscationStub4.inject();
            false
        } else FileUtil.writeFile(getDeviceIdFile(), deviceId)
    }

    private fun readDeviceIDFromFile(): String? {
        return FileUtil.readFile(getDeviceIdFile())
    }

    private fun getDeviceIdFile(): String {
        val context: Context = getApplication()
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val fileName = "did.dat"
        val file = File(dir, fileName)
        return file.absolutePath
    }

    private fun getAndroidID(): String {
        val context: Context = getApplication()
        return Secure.getString(context.contentResolver, Secure.ANDROID_ID)
    } /* private */
}
