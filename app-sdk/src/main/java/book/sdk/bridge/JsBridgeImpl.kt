package book.sdk.bridge

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.app.ActivityManagerCompat
import book.sdk.core.manager.AdjustManager
import book.sdk.core.manager.ThinkingDataManager
import book.sdk.core.util.CocosPreferenceUtil
import book.sdk.core.util.DeviceUtil
import book.util.ImitateChecker
import book.sdk.core.util.PackageUtil
import book.sdk.core.util.PreferenceUtil
import book.util.ensureDirectory
import book.util.getAppApkFile
import book.sdk.download.DownloadTask
import book.sdk.download.DownloadTask.OnDownloadListener
import book.sdk.util.KeyChainUtil
import book.sdk.walle.ExtraInfoReader
import book.util.AppGlobal
import book.util.LogUtil
import org.json.JSONObject
import java.io.File

class JsBridgeImpl(private val mCallback: BridgeCallback?) : BridgeInterface {
    companion object {
        private val TAG = JsBridgeImpl::class.java.simpleName

        /**
         * 6: 支持HttpDns
         */
        private const val BRIDGE_VERSION = 7
        const val DIR_IMAGES = "images"
        const val PROMOTION_MATERIAL_FILENAME = "promotion_material_%s_%s"
        const val PROMOTION_IMAGE_FILENAME = "promotion_%d.jpg"
        const val PROMOTION_SHARE_FILENAME = "promotion_share_img.jpg"
    }

    override fun nativeLog(tag: String?, msg: String?) {
        var tags = tag
        if (tags.isNullOrEmpty()) {
            tags = TAG
        }
        LogUtil.d(tags!!, msg ?: "")
    }

    /* interface -> non-callback */
    override fun copyText(text: String?) {
        if (TextUtils.isEmpty(text)) {
            return
        }
        val manager =
            AppGlobal.application?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(null, text))
    }

    override fun getCopiedText(): String {
        val manager =
            AppGlobal.application?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (manager.hasPrimaryClip() && manager.primaryClip!!.itemCount > 0) {
            val clipData = manager.primaryClip!!.getItemAt(0)
            return if (TextUtils.isEmpty(clipData.text)) "" else clipData.text.toString()
        }
        return ""
    }

    override fun showNativeToast(toast: String?) {
        Toast.makeText(AppGlobal.application, toast, Toast.LENGTH_SHORT).show()
    }

    override fun initAdjustID(adjustAppID: String?) {
        if (TextUtils.isEmpty(adjustAppID)) {
            return
        }
        val adjustAppIDCache = PreferenceUtil.readAdjustAppID()
        if (TextUtils.equals(adjustAppID, adjustAppIDCache)) {
            return
        }
        PreferenceUtil.saveAdjustAppID(adjustAppID)
        AdjustManager.initAdjustSdk(AppGlobal.application, adjustAppID)
    }

    override fun trackAdjustEvent(eventToken: String?, jsonData: String?) {
        var jsonObj: JSONObject? = null
        try {
            jsonObj = JSONObject(jsonData!!)
        } catch (e: Exception) {
        }
        val s2sParams = HashMap<String, String>()
        if (jsonObj != null) {
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObj.optString(key)
                s2sParams[key] = value
            }
        } else {
        }
        AdjustManager.trackEvent(eventToken, s2sParams)
    }

    override fun trackAdjustEventStart(eventToken: String?) {
        AdjustManager.trackEventStart(eventToken)
    }

    override fun trackAdjustEventGreeting(eventToken: String?) {
        AdjustManager.trackEventGreeting(eventToken)
    }

    override fun trackAdjustEventAccess(eventToken: String?) {
        AdjustManager.trackEventAccess(eventToken)
    }

    override fun trackAdjustEventUpdated(eventToken: String?) {
        AdjustManager.trackEventUpdated(eventToken)
    }

    override fun getDeviceID(): String? {
        return DeviceUtil.getDeviceID()
    }

    override fun getDeviceInfoForLighthouse(): String {
        //return DeviceUtil.getDeviceInfoForLighthouse();
        return ""
    }

    override fun getSystemVersionCode(): Int {
        return Build.VERSION.SDK_INT
    }

    override fun getClientVersionCode(): Int {
        return PackageUtil.getPackageVersionCode()
    }

    override fun getPackageName(): String {
        return PackageUtil.getPackageName()
    }

    override fun getAppName(): String? {
        return PackageUtil.getAppName()
    }

    override fun getChannel(): String? {
        return PackageUtil.getChannel()
    }

    override fun getBrand(): String {
        return PackageUtil.getChildBrand()
    }

    override fun saveGameUrl(gameUrl: String?) {
        if (!URLUtil.isValidUrl(gameUrl)) {
            return
        }
        PreferenceUtil.saveGameUrl(gameUrl!!)
    }

    override fun saveAccountInfo(plainText: String?) {
        if (TextUtils.isEmpty(plainText)) {
            return
        }
        KeyChainUtil.saveAccountInfo(plainText!!)
    }

    override fun getAccountInfo(): String {
        return KeyChainUtil.getAccountInfo()
    }

    override fun getAdjustDeviceID(): String {
        return AdjustManager.getAdjustDeviceID()
    }

    override fun getGoogleADID(): String {
        return DeviceUtil.googleAdId!!
    }

    override fun getIDFA(): String {
        return "" // iOS only
    }

    override fun getReferID(): String? {
        val readInfo = ExtraInfoReader[getAppApkFile()]
            ?: return ""
        return readInfo.referID
    }

    override fun getAgentID(): String? {
        val readInfo = ExtraInfoReader[getAppApkFile()]
            ?: return ""
        return readInfo.agentID
    }

    override fun setCocosData(key: String?, value: String?) {
        if (TextUtils.isEmpty(key)) {
            return
        }
        CocosPreferenceUtil.putString(key, value)
        if (CocosPreferenceUtil.KEY_USER_ID == key || CocosPreferenceUtil.KEY_COMMON_USER_ID == key) {
            ThinkingDataManager.loginAccount()
        }
        if (CocosPreferenceUtil.KEY_COCOS_FRAME_VERSION == key) {
            AdjustManager.updateCocosFrameVersion()
        }
    }

    override fun getCocosData(key: String?): String? {
        return if (TextUtils.isEmpty(key)) {
            ""
        } else CocosPreferenceUtil.getString(key)
    }

    override fun getCocosAllData(): String {
        val map = CocosPreferenceUtil.getAll()
        val obj = JSONObject(map)
        return obj.toString()
    }

    override fun getLighterHost(): String {
        //String lighterHost = ConfigPreference.readLighterHost();
        //LogUtil.d(TAG, "[Lighthouse] Host: " + lighterHost);
        //return lighterHost;
        return ""
    }

    override fun getBridgeVersion(): Int {
        return BRIDGE_VERSION
    }

    override fun isFacebookEnable(): Boolean {
        return false
    }

    override fun getTDTargetCountry(): String {
        return ThinkingDataManager.getTargetCountry()
    }

    override fun openUrlByBrowser(url: String?) {
        mCallback?.openUrlByBrowser(url)
    }

    override fun openUrlByWebView(url: String?) {
        mCallback?.openUrlByWebView(url)
    }

    override fun openApp(target: String?, fallbackUrl: String?) {
        mCallback?.openApp(target, fallbackUrl)
    }

    override fun loadUrl(url: String?) {
        mCallback?.loadUrl(url)
    }

    override fun goBack() {
        mCallback?.goBack()
    }

    override fun close() {
        mCallback?.close()
    }

    override fun refresh() {
        mCallback?.refresh()
    }

    override fun clearCache() {
        mCallback?.clearCache()
    }

    override fun saveImage(url: String?) {
        mCallback?.saveImage(url)
    }

    override fun savePromotionMaterial(materialUrl: String?) {
        val context: Context = AppGlobal.application!!
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        dir.ensureDirectory()
        DownloadTask.mInstance.download(
            materialUrl,
            dir!!.absolutePath,
            object : OnDownloadListener {
                override fun onDownloadSuccess(saveFile: File) {
                    val fileName = String.format(
                        PROMOTION_MATERIAL_FILENAME,
                        PackageUtil.getPackageName(), PackageUtil.getChannel()
                    )
                    val destFile = File(dir, fileName)
                    val succeed = saveFile.renameTo(destFile)
                    LogUtil.d(TAG, "savePromotionMaterial - download succeed = $succeed")
                    mCallback?.savePromotionMaterialDone(succeed)
                }

                override fun onDownloading(progress: Int) {
                    LogUtil.d(TAG, "savePromotionMaterial - downloading = $progress%")
                }

                override fun onDownloadFailed() {
                    LogUtil.w(TAG, "savePromotionMaterial - download failed")
                    mCallback?.savePromotionMaterialDone(false)
                }
            })
    }

    override fun synthesizePromotionImage(qrCodeUrl: String?, size: Int, x: Int, y: Int) {
        mCallback?.synthesizePromotionImage(qrCodeUrl, size, x, y)
    }

    override fun shareUrl(url: String?) {
        mCallback?.shareUrl(url)
    }

    override fun loginFacebook() {
        mCallback?.loginFacebook()
    }

    override fun logoutFacebook() {
        mCallback?.logoutFacebook()
    }

    override fun preloadPromotionImage(imageUrl: String?) {
        val context: Context = AppGlobal.application!!
        val imagePath = File(context.filesDir, DIR_IMAGES)
        imagePath.ensureDirectory()
        DownloadTask.mInstance.download(
            imageUrl,
            imagePath.absolutePath,
            object : OnDownloadListener {
                override fun onDownloadSuccess(saveFile: File) {
                    val destFile = File(imagePath, PROMOTION_SHARE_FILENAME)
                    val succeed = saveFile.renameTo(destFile)
                    LogUtil.d(TAG, "preloadPromotionImage - download succeed = $succeed")
                    mCallback?.preloadPromotionImageDone(succeed)
                }

                override fun onDownloading(progress: Int) {
                    LogUtil.d(TAG, "preloadPromotionImage - downloading = $progress%")
                }

                override fun onDownloadFailed() {
                    LogUtil.w(TAG, "preloadPromotionImage - download failed")
                    mCallback?.preloadPromotionImageDone(false)
                }
            })
    }

    override fun shareToWhatsApp(text: String?) {
        val context: Context = AppGlobal.application!!
        val imagePath = File(context.filesDir, DIR_IMAGES)
        val destFile = File(imagePath, PROMOTION_SHARE_FILENAME)
        mCallback?.shareToWhatsApp(text, destFile)
    }

    override fun isHttpDnsEnable(): Boolean {
        return false
    }

    override fun httpdns(host: String?): String {
        return ""
    }

    override fun httpdnsInit(hosts: String?) {}
    override fun httpdnsRequestSync(req: String?, body: ByteArray?): String {
        return ""
    }

    override fun httpdnsRequestAsync(req: String?, body: ByteArray?) {}
    override fun httpdnsWsOpen(req: String?) {}
    override fun httpdnsWsSend(req: String?, body: ByteArray?): String {
        return ""
    }

    override fun httpdnsWsClose(req: String?) {}
    override fun httpdnsWsConnected(req: String?): String {
        return ""
    }

    override fun getBuildVersion(): String {
        return PackageUtil.getBuildVersion()
    }

    /**
     * @param accid   create game id
     * @param cretime create time
     */
    override fun onAnalysisStart(accid: String?, cretime: Long) {
    }

    override fun onAnalysisEnd() {
    }

    override fun memoryInfo(): String {
        try {
            val context: Context = AppGlobal.application!!
            val result = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            result.getMemoryInfo(mi)
            val json = JSONObject()
            json.put("DeviceTotalMem", mi.totalMem) //设备实际最大内存
            json.put("DeviceAvailMem", mi.availMem) //设备可用内存
            json.put("isLowMemDevice", ActivityManagerCompat.isLowRamDevice(result)) //是否低内存设备
            val r = Runtime.getRuntime()
            json.put("AppTotalMemory", r.totalMemory()) //App最大可用内存
            json.put("AppMaxMemory", r.maxMemory()) //App当前可用内存
            json.put("AppFreeMemory", r.freeMemory()) //App当前空闲内存
            LogUtil.i(TAG, "getMemInfo: $json")
            return json.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e(TAG, "logMem:获取内存信息失败：" + e.message)
        }
        return ""
    }

    override fun isEmulator(): Boolean {
        val isEmulator = ImitateChecker.isImitate()
        LogUtil.d(TAG, "isEmulator: %s", isEmulator)
        return isEmulator
    }

    override fun commonData(): String? {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("mac", DeviceUtil.macAddress)
            jsonObject.put("gsf_id", DeviceUtil.gsfAndroidId)
            jsonObject.toString()
        } catch (exception: Exception) {
            ""
        }
    }

    override fun exitApp() {
        book.sdk.core.manager.ActivityManager.mInstance.finishAll()
    }

    /**
     * handle notification when Cocos is ready
     */
    override fun handleNotification() {}
    override fun onWebViewLoadChanged(json: String?) {
        try {
            val jsonObject = JSONObject(json!!)
            val type = jsonObject.optInt("type")
            val orientation = jsonObject.optString("orientation")
            val hover = jsonObject.optBoolean("hover")
            val data = jsonObject.optString("data")
            mCallback?.openDataByWebView(type, orientation, hover, data)
        } catch (_: Exception) {
        }
    }
}
