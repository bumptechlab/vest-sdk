package code.sdk.core.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import code.sdk.core.BuildConfig
import code.sdk.core.JumpCenter
import code.sdk.core.VestCore
import code.sdk.core.util.PreferenceUtil.saveLoggable
import code.util.AppGlobal
import code.util.LogUtil
import java.util.Arrays

object TestUtil {
   private val TAG = TestUtil::class.java.simpleName
    
    fun isLoggable(): Boolean {
        //ObfuscationStub7.inject();
        var isLoggable = false
        isLoggable = if (PreferenceUtil.hasLoggable()) { //如果后门设置了日志开关，则使用后门日志开关
            PreferenceUtil.readLoggable()
        } else {
            BuildConfig.DEBUG
        }
        return isLoggable
    }

    fun setLoggable(loggable: Boolean) {
        saveLoggable(loggable)
    }

    fun handleIntent(context: Context): Boolean {
        val activity: Activity? = DeviceUtil.findActivity(context)
        if (activity == null) {
            Log.w(TAG, "[HandleIntent] activity not found in context")
            return false
        }
        val intent = activity.getIntent()
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
            printDebugInfo()
            if (!TextUtils.isEmpty(gameUrl)) {
                DeviceUtil.finishActivitySafety(activity)
                VestCore.initThirdSDK()
                JumpCenter.toWebViewActivity(context, gameUrl)
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

    fun printDebugInfo() {
        LogUtil.i(TAG, "[SDK] name: %s, version: %s, buildNumber: %s",
            BuildConfig.SDK_NAME,
            BuildConfig.SDK_VERSION,
            BuildConfig.BUILD_NUMBER)
        LogUtil.i(TAG,
            "[App] package: %s, versionCode: %d, versionName: %s, buildVersion:%s, brand: %s, channel: %s",
            PackageUtil.getPackageName(),
            PackageUtil.getPackageVersionCode(),
            PackageUtil.getPackageVersionName(),
            PackageUtil.getBuildVersion(),
            PackageUtil.getParentBrand(),
            PackageUtil.getChannel())
        LogUtil.i(TAG, "[Device] sdkInt: %d, sdkVersion: %s, isEmulator: %s",
            Build.VERSION.SDK_INT,
            Build.VERSION.RELEASE,
            ImitateChecker.isImitate())
        LogUtil.i(TAG,
            "[Adjust] APP_ID: %s, EVENT_START: %s, EVENT_GREETING: %s, EVENT_ACCESS: %s, EVENT_UPDATED: %s",
            ConfigPreference.readAdjustAppId(),
            ConfigPreference.readAdjustEventStart(),
            ConfigPreference.readAdjustEventGreeting(),
            ConfigPreference.readAdjustEventAccess(),
            ConfigPreference.readAdjustEventUpdated())
        LogUtil.i(TAG, "[ThinkingData] AppId: %s, Host: %s",
            ConfigPreference.readThinkingDataAppId(),
            ConfigPreference.readThinkingDataHost())
        LogUtil.i(TAG, "[SHF] BASE_HOST: %s, SPARE_HOSTS: %s, SHF_DISPATCHER: %s",
            ConfigPreference.readSHFBaseHost(),
            Arrays.asList<String>(*ConfigPreference.readSHFSpareHosts()),
            ConfigPreference.readShfDispatcher())
        LogUtil.i(TAG, "[Constant] CHN: %s", ConfigPreference.readChannel())
        LogUtil.i(TAG, "[Constant] BRD: %s", ConfigPreference.readBrand())
        LogUtil.i(TAG, "Keystore Hash: %s",
            java.lang.String.join(",", PackageUtil.getKeystoreHashes(AppGlobal.getApplication())))
    }
}
