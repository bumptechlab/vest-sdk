package poetry.sdk.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
        return PackageUtil.getChannel()
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
}
