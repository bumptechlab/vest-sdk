package poetry.sdk.bridge

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import org.json.JSONObject
import poetry.sdk.core.VestCore
import poetry.sdk.core.manager.AdjustManager
import poetry.sdk.core.util.CocosPreferenceUtil
import poetry.sdk.core.util.DeviceUtil
import poetry.sdk.core.util.PackageUtil
import poetry.util.AppGlobal
import java.util.Locale

open class JsBridgeImpl(private val mCallback: BridgeCallback?) : BridgeInterface {
    companion object {
        private val TAG = JsBridgeImpl::class.java.simpleName
        private const val BRIDGE_VERSION = 7
    }

    override fun close() {
        mCallback?.finish()
    }

    override fun refresh() {
        mCallback?.refresh()
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

    override fun getDeviceID(): String? {
        return DeviceUtil.getDeviceID()
    }

    override fun getChannel(): String? {
        return PackageUtil.getChannelByCountry()
    }

    override fun getBrand(): String? {
        return PackageUtil.getChildBrand()
    }

    override fun getAdjustDeviceID(): String? {
        return AdjustManager.getAdjustDeviceID()
    }

    override fun getGoogleADID(): String {
        return DeviceUtil.googleAdId!!
    }

    override fun setCocosData(key: String?, value: String?) {
        if (key.isNullOrEmpty()) {
            return
        }
        CocosPreferenceUtil.putString(key, value)
        if (CocosPreferenceUtil.KEY_USER_ID == key || CocosPreferenceUtil.KEY_COMMON_USER_ID == key) {
            //原来这里执行TD的账号登录
        }
        if (CocosPreferenceUtil.KEY_COCOS_FRAME_VERSION == key) {
            AdjustManager.updateCocosFrameVersion()
        }
    }

    override fun getCocosData(key: String?): String? {
        return if (key.isNullOrEmpty()) {
            ""
        } else CocosPreferenceUtil.getString(key)
    }

    override fun getCocosAllData(): String {
        val map = CocosPreferenceUtil.getAll()
        val obj = JSONObject(map)
        return obj.toString()
    }

    override fun getBridgeVersion(): Int {
        return BRIDGE_VERSION
    }

    override fun getTDTargetCountry(): String? {
        return VestCore.getTargetCountry()?.uppercase(Locale.getDefault())
    }

    override fun openUrlByBrowser(url: String?) {
        mCallback?.openUrlByBrowser(url)
    }

    override fun openUrlByWebView(url: String?) {
        mCallback?.openUrlByWebView(url)
    }

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

    /**
     * 幅度震动,振幅值在[0,255]，可以实现曲线震感效果
     * 传入,分割的字符串，如默认值"20,180,80,120"
     * @param patterns  20, 180, 80, 120
     */

    private var mVibrator: Vibrator? = null
    override fun amplitudeVibrator(patterns: String) {
        val pattern: LongArray = patterns
            .split(",")
            .filter { it.isNotEmpty() && it.toLong() >= 0 && it.toLong() <= 255 }
            .map { it.trim().toLong() }
            .toLongArray()
        if (pattern.isEmpty()) return
        if (mVibrator == null) {
            mVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mVibratorManager = AppGlobal.application!!.getSystemService(
                    VibratorManager::class.java)
                mVibratorManager.defaultVibrator
            } else {
                AppGlobal.application!!.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator//获得一个震动的服务
            }
        }
        mVibrator?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            mVibrator?.vibrate(pattern, -1)
        }
    }
    /**
     * 根据设定时长按照系统默认震动强度，持续震动,默认0.3s
     * @param milliseconds 设定时长
     */
    override fun continuedVibrator(milliseconds: Long) {
        if (mVibrator == null) {
            mVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mVibratorManager = AppGlobal.application!!.getSystemService(
                    VibratorManager::class.java)
                mVibratorManager.defaultVibrator
            } else {
                AppGlobal.application!!.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator//获得一个震动的服务
            }
        }
        mVibrator?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            mVibrator?.vibrate(milliseconds)
        }
    }
}
