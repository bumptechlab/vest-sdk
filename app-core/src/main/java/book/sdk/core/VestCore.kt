package book.sdk.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import book.sdk.core.event.SDKEvent
import book.sdk.core.manager.ActivityManager
import book.sdk.core.manager.AdjustManager
import book.sdk.core.manager.ConfigurationManager
import book.sdk.core.manager.ThinkingDataManager
import book.sdk.core.util.GoogleAdIdInitializer
import book.sdk.core.util.PreferenceUtil
import book.sdk.core.util.TestUtil
import book.util.AppGlobal
import book.util.LogUtil
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject

object VestCore {
    private val TAG = VestCore::class.java.simpleName
    private val WEBVIEW_ACTIVITY_CLASS_NAME = "book.sdk.ui.WebActivity"
    private var isTestIntentHandled = false

    fun init(context: Context, configAssets: String?, loggable: Boolean?) {
        if (loggable != null) {
            TestUtil.setLoggable(loggable)
            LogUtil.setDebug(loggable)
        }
        setUncaughtException()
        LogUtil.setDebug(TestUtil.isLoggable())
        ConfigurationManager.init(context, configAssets)
        registerActivityLifecycleCallbacks()
        GoogleAdIdInitializer.init()
        initThirdSDK()
    }

    fun isTestIntentHandled(): Boolean {
        return isTestIntentHandled
    }

    private fun registerActivityLifecycleCallbacks() {
        AppGlobal.application?.registerActivityLifecycleCallbacks(object :
            SimpleLifecycleCallbacks() {
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
        LogUtil.d(TAG, "[Vest-Core] onActivityCreated: intent=%s", intent)
        if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
            LogUtil.d(
                TAG, "[Vest-Core] onActivityCreated: %s is a launcher activity",
                if (intent.component != null) intent.component!!.flattenToString() else ""
            )
            isTestIntentHandled = TestUtil.handleIntent(activity)
        }
    }

    private fun interceptDestroyedActivity(activity: Activity) {
        val intent = activity.intent ?: return
        LogUtil.d(TAG, "[Vest-Core] onActivityDestroyed: intent=%s", intent)
        if (ActivityManager.mInstance.isActivityEmpty()) {
            isTestIntentHandled = false
            LogUtil.d(TAG, "[Vest-Core] onActivityDestroyed: activity stack is empty, reset")
        }
    }

    fun initThirdSDK() {
        ThinkingDataManager.init(AppGlobal.application)
        AdjustManager.init(AppGlobal.application!!)
    }

    fun updateThirdSDK() {
        ThinkingDataManager.initTDEvents()
        AdjustManager.initParams()
    }

    fun getTargetCountry(): String {
        val targetCountry = PreferenceUtil.readTargetCountry()
        LogUtil.d(TAG, "[Vest-Core] read target country: %s", targetCountry)
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
            LogUtil.e(TAG, e, "[Vest-Core] on uncaughtException: ")
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
                LogUtil.i(TAG, "[Vest-Core] setUncaughtException json: %s", json)
                ThinkingDataManager.trackEvent("td_crash", json)
                ThinkingDataManager.flush()
            } catch (exception: Throwable) {
                LogUtil.e(TAG, exception, "[Vest-Core] setUncaughtException errorInLogging: ")
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

    fun toWebViewActivity(context: Context, url: String) {
        try {
            if (!URLUtil.isValidUrl(url)) {
                LogUtil.e(
                    TAG, "Activity[%s] launched aborted for invalid url: %s",
                    WEBVIEW_ACTIVITY_CLASS_NAME, url
                )
                return
            }
            val intent = Intent()
            intent.setClassName(context, WEBVIEW_ACTIVITY_CLASS_NAME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("key_path_url_value", addRandomTimestamp(url))
            intent.putExtra("key_is_game_value", true)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            LogUtil.e(
                TAG, e, "Activity[%s] not found, please import 'vest-sdk' library",
                WEBVIEW_ACTIVITY_CLASS_NAME
            )
        } catch (e: Exception) {
            LogUtil.e(
                TAG, e, "Activity[%s] launched error",
                WEBVIEW_ACTIVITY_CLASS_NAME
            )
        }
    }

    private fun addRandomTimestamp(url: String): String {
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            LogUtil.e(TAG, "url parse error: %s", url)
            return url
        }
        val ts = System.currentTimeMillis().toString()
        val queryParameterNames = uri.queryParameterNames
        return if (queryParameterNames.size > 0) {
            "$url&t=$ts"
        } else {
            "$url?t=$ts"
        }
    }
}
