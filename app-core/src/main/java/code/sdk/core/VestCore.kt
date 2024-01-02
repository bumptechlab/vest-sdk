package code.sdk.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import code.sdk.core.event.SDKEvent
import code.sdk.core.manager.ActivityManager
import code.sdk.core.manager.AdjustManager.init
import code.sdk.core.manager.AdjustManager.initParams
import code.sdk.core.manager.ConfigurationManager
import code.sdk.core.manager.SimpleLifecycleCallbacks
import code.sdk.core.manager.ThinkingDataManager
import code.sdk.core.util.GoogleAdIdInitializer
import code.sdk.core.util.PreferenceUtil.readTargetCountry
import code.sdk.core.util.TestUtil.handleIntent
import code.sdk.core.util.TestUtil.isLoggable
import code.util.AESKeyStore
import code.util.AppGlobal.getApplication
import code.util.LogUtil.d
import code.util.LogUtil.e
import code.util.LogUtil.i
import code.util.LogUtil.setDebug
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject

object VestCore {
    private val TAG = VestCore::class.java.simpleName

    private var isTestIntentHandled = false

    fun init(context: Context, configAssets: String?) {
//        PreferenceUtil.getInspectStartTime();
        setUncaughtException()
        AESKeyStore.init()
        setDebug(isLoggable())
        ConfigurationManager.mInstance.init(context, configAssets)
        registerActivityLifecycleCallbacks()
        GoogleAdIdInitializer.init()
        initThirdSDK()
    }

    fun isTestIntentHandled(): Boolean {
        return isTestIntentHandled
    }

    fun registerActivityLifecycleCallbacks() {
        getApplication().registerActivityLifecycleCallbacks(object : SimpleLifecycleCallbacks() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ActivityManager.mInstance.push(activity)
                interceptLauncherActivity(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                ActivityManager.mInstance.remove(activity)
                interceptDestroyedActivity(activity)
            }
        })
    }

    private fun interceptLauncherActivity(activity: Activity) {
        val intent = activity.intent ?: return
        val categories = intent.categories
        d(TAG, "[Vest-Core] onActivityCreated: intent=%s", intent)
        if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
            d(TAG, "[Vest-Core] onActivityCreated: %s is a launcher activity",
                if (intent.component != null) intent.component!!.flattenToString() else "")
            isTestIntentHandled = handleIntent(activity)
        }
    }

    private fun interceptDestroyedActivity(activity: Activity) {
        val intent = activity.intent ?: return
        d(TAG, "[Vest-Core] onActivityDestroyed: intent=%s", intent)
        if (ActivityManager.mInstance.isActivityEmpty()) {
            isTestIntentHandled = false
            d(TAG, "[Vest-Core] onActivityDestroyed: activity stack is empty, reset")
        }
    }

    fun initThirdSDK() {
        ThinkingDataManager.init(getApplication())
        init(getApplication())
    }

    fun updateThirdSDK() {
        ThinkingDataManager.initTDEvents()
        initParams()
    }

    fun getTargetCountry(): String {
        val targetCountry = readTargetCountry()
        d(TAG, "[Vest-Core] read target country: %s", targetCountry)
        return targetCountry
    }

    fun onCreate() {
        EventBus.getDefault().post(SDKEvent("onCreate"))
    }

    fun onDestroy() {
        ThinkingDataManager.flush()
        EventBus.getDefault().post(SDKEvent("onDestroy"))
    }

    fun onPause() {
        EventBus.getDefault().post(SDKEvent("onPause"))
    }

    fun onResume() {
        EventBus.getDefault().post(SDKEvent("onResume"))
    }

    /**
     * 捕获异常上报
     */
    private fun setUncaughtException() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            e(TAG, e, "[Vest-Core] on uncaughtException: ")
            try {
                val json = JSONObject()
                val stackArray = JSONArray()
                val stackTrace = e.stackTrace
                if (stackTrace != null) {
                    for (i in stackTrace.indices) {
                        stackArray.put(stackTrace[i])
                    }
                }
                json.put("crash_stack", stackArray)
                json.put("crash_msg", e.localizedMessage)
                json.put("crash_cause", e.cause)
                i(TAG, "[Vest-Core] setUncaughtException json: %s", json)
                ThinkingDataManager.trackEvent("td_crash", json)
                ThinkingDataManager.flush()
            } catch (exception: Throwable) {
                e(TAG, exception, "[Vest-Core] setUncaughtException errorInLogging: ")
            } finally {
                try {
                    Thread.sleep(1000)
                    ActivityManager.mInstance.finishAll()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}
