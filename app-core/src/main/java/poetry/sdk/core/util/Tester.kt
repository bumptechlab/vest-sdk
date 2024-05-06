package poetry.sdk.core.util

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import poetry.sdk.core.BuildConfig
import poetry.sdk.core.VestCore
import poetry.util.LogUtil

object Tester {
    private val TAG = Tester::class.java.simpleName

    fun isLoggable(): Boolean {
        return if (PreferenceUtil.hasLoggable()) { //如果后门设置了日志开关，则使用后门日志开关
            PreferenceUtil.readLoggable()
        } else {
            poetry.sdk.core.BuildConfig.DEBUG
        }
    }

    fun setLoggable(loggable: Boolean) {
        PreferenceUtil.saveLoggable(loggable)
    }

    fun handleIntent(context: Context): Boolean {
        val activity: Activity? = DeviceUtil.findActivity(context)
        if (activity == null) {
            Log.w(TAG, "[HandleIntent] activity not found in context")
            return false
        }
        val intent = activity.intent
        if (intent == null) {
            Log.d(TAG, "[HandleIntent] intent is null")
            return false
        }
        Log.d(TAG, "[HandleIntent] intent=$intent")
        val bundle = intent.getBundleExtra("bundle")
        printBundleInfo(bundle)
        if (bundle != null) {
            val channel: String = bundle.getString("game_channel", "").trim()
            val brand: String = bundle.getString("game_brd", "").trim()
            val installreferrer: String =
                bundle.getString("game_installreferrer", "").trim()
            val loggable: Boolean = bundle.getBoolean("log", false)
            val gameUrl: String = bundle.getString("customizeUrl", "").trim()
            if (!TextUtils.isEmpty(channel)) {
                PreferenceUtil.saveChannel(channel)
            }
            if (!TextUtils.isEmpty(brand)) {
                PreferenceUtil.saveParentBrand(brand)
            }
            if (!TextUtils.isEmpty(installreferrer)) {
                PreferenceUtil.saveInstallReferrer(installreferrer)
            }
            setLoggable(loggable)
            LogUtil.setDebug(loggable)
            if (!TextUtils.isEmpty(gameUrl)) {
                DeviceUtil.finishActivitySafety(activity)
                VestCore.initThirdSDK()
                VestCore.toWebViewActivity(context, gameUrl)
                return true
            }
        }
        return false
    }

    private fun printBundleInfo(bundle: Bundle?) {
        if (bundle == null) {
            Log.w(TAG, "[HandleIntent] bundle is null from intent")
            return
        }
        val keySet: Set<String> = bundle.keySet()
        val builder = StringBuilder()
        val keyIterator = keySet.iterator()
        builder.append("{")
        while (keyIterator.hasNext()) {
            val key = keyIterator.next()
            val value: Any? = bundle.get(key)
            builder.append("$key=$value")
            builder.append(", ")
        }
        if (builder.isNotEmpty()) {
            builder.delete(builder.length - 2, builder.length)
        }
        builder.append("}")
        Log.d(TAG, "[HandleIntent] bundle=$builder")
    }

}
