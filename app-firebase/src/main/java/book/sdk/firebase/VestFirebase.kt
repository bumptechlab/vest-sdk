package book.sdk.firebase

import android.content.Context
import android.text.TextUtils
import android.webkit.URLUtil
import book.sdk.core.VestCore
import book.sdk.core.VestInspectCallback
import book.sdk.core.VestInspectResult
import book.sdk.core.event.SDKEvent
import book.sdk.core.manager.AdjustManager
import book.sdk.core.manager.InitInspector
import book.sdk.core.manager.InstallReferrerManager
import book.sdk.core.util.GoogleAdIdInitializer
import book.sdk.core.util.PackageUtil
import book.sdk.core.util.PreferenceUtil
import book.util.ImitateChecker
import book.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit

class VestFirebase private constructor() {
    var TAG = VestFirebase::class.java.simpleName
    var mVestInspectCallback: VestInspectCallback? = null
    var mJob: Job? = null
    var mContext: Context? = null

    init {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    private object InstanceHolder {
        val INSTANCE = VestFirebase()
    }

    companion object {
        private val TAG = VestFirebase::class.java.simpleName

        @JvmStatic
        fun getInstance(): VestFirebase {
            return InstanceHolder.INSTANCE
        }
    }

    /**
     * setup duration of silent period for requesting A/B switching starting from the date of apk build
     *
     * @param time     duration of time
     * @param timeUnit time unit for example DAYS, HOURS, MINUTES, SECONDS
     */
    fun setInspectDelayTime(time: Long, timeUnit: TimeUnit): VestFirebase {
        val timeMills = timeUnit.toMillis(time)
        PreferenceUtil.saveInspectDelay(timeMills)
        return this
    }

    /**
     * setup the date of apk build
     *
     * @param releaseTime time format：yyyy-MM-dd HH:mm:ss
     */
    fun setReleaseTime(releaseTime: String?): VestFirebase {
        PreferenceUtil.saveReleaseTime(releaseTime)
        return this
    }


    /**
     * trying to request A/B switching, fetch config from firebase server
     *
     * @param context
     * @param vestInspectCallback
     */
    fun inspect(context: Context?, vestInspectCallback: VestInspectCallback?): VestFirebase {
        mContext = context
        mVestInspectCallback = vestInspectCallback
        init()
        inspect()
        return this
    }

    private fun init() {
        //子品牌为空则使用父品牌（来自config配置）
        val childBrd = PackageUtil.getChildBrand()
        if (childBrd.isNullOrEmpty()) {
            PreferenceUtil.saveChildBrand(PackageUtil.getParentBrand())
        }
    }

    private fun inspect() {
        mJob = MainScope().launch {
            AdjustManager.trackEventStart(null)
            flow {
                if (!canInspect()) {
                    emit("")
                    return@flow
                }
                fetchRemoteFirebase(this)
            }.flowOn(Dispatchers.IO)
                .catch {
                    LogUtil.e(TAG, it, "[Vest-Firebase] onInspect error")
                    if (mVestInspectCallback != null) {
                        mVestInspectCallback?.onShowASide(VestInspectResult.REASON_FIREBASE_ERROR)
                    }
                }
                .onStart {
                    LogUtil.d(TAG, "[Vest-Firebase] onInspect start")
                }
                .onCompletion {
                    LogUtil.d(TAG, "[Vest-Firebase] onInspect finish")
                }
                .collect {
                    LogUtil.d(TAG, "[Vest-Firebase] onInspectTarget: $it")
                    val url = it
                    if (mVestInspectCallback != null) {
                        if (url.isEmpty()) {
                            mVestInspectCallback?.onShowASide(VestInspectResult.REASON_OFF_ON_SERVER)
                        } else {
                            val launchBSuccess = VestCore.toWebViewActivity(
                                mContext,
                                url,
                                VestCore.WEBVIEW_TYPE_INNER
                            )
                            mVestInspectCallback?.onShowBSide(url, launchBSuccess)
                        }
                    }
                }
        }
    }

    private suspend fun fetchRemoteFirebase(flowCollector: FlowCollector<String>) {
        val remoteSourceFirebase = RemoteSourceFirebase { success, remoteConfig ->
            //上报事件
            AdjustManager.trackEventGreeting(null)

            var url = ""
            if (success) {
                LogUtil.d(
                    TAG,
                    "[Vest-Firebase] fetch remote config success: " + remoteConfig.toString()
                )
                url = remoteConfig?.l ?: ""
            } else {
                LogUtil.d(TAG, "[Vest-Firebase] fetch remote config fail")
            }

            //链接无效||开关关闭，直接走缓存
            if (!URLUtil.isValidUrl(url) || remoteConfig?.s == false) {
                val cachedUrl = PreferenceUtil.readFirebaseUrl()
                if (!cachedUrl.isNullOrEmpty()) {
                    url = cachedUrl
                } else {
                    url = ""
                }
            } else {
                //开关开启且链接有效
                PreferenceUtil.saveFirebaseUrl(url)

                PreferenceUtil.saveTargetCountry(remoteConfig!!.c)
                PreferenceUtil.saveChildBrand(remoteConfig.b)
            }

            if (URLUtil.isValidUrl(url)) {
                VestCore.updateThirdSDK()
            }

            flowCollector.emit(url)
        }
        remoteSourceFirebase.fetch()
    }

    private fun canInspect(): Boolean {
        //模拟器，直接跳A
        if (ImitateChecker.isImitate()) {
            LogUtil.d(TAG, "[Vest-Firebase] inspect cancel, it's emulator")
            return false
        }

        //静默期未到，直接跳A
        val inspectStartTime = PreferenceUtil.getReleaseTime()
        val inspectDelay = PreferenceUtil.getInspectDelay()
        if (inspectStartTime > 0 && inspectDelay > 0) {
            val inspectTimeMills = inspectStartTime + inspectDelay
            LogUtil.d(TAG, "[Vest-Firebase] inspect cancel, it's not the time")
            return System.currentTimeMillis() > inspectTimeMills
        }

        val installReferrer = InstallReferrerManager.getInstallReferrer()
        if (TextUtils.isEmpty(installReferrer) || InstallReferrerManager.INSTALL_REFERRER_UNKNOWN == installReferrer) {
            InstallReferrerManager.initInstallReferrer()
        }
        GoogleAdIdInitializer.init()
        //非自然安装量，跳A
        val inspected = InitInspector().inspect();
        return inspected
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceiveEvent(sdkEvent: SDKEvent) {
        when (sdkEvent.event) {
            "onCreate" -> onCreate()
            "onPause" -> onPause()
            "onResume" -> onResume()
            "onDestroy" -> onDestroy()
            else -> {}
        }
    }

    private fun onCreate() {
        LogUtil.d(TAG, "[Vest-Firebase] onCreate")
    }

    private fun onPause() {
        LogUtil.d(TAG, "[Vest-Firebase] onPause")
        if (mJob != null) {
            if (mJob?.isActive!!) {
                LogUtil.d(TAG, "[Vest-Firebase] inspect job is active, cancel now")
                mJob?.cancel()
            } else {
                LogUtil.d(TAG, "[Vest-Firebase] inspect job is not active")
            }
        } else {
            LogUtil.d(TAG, "[Vest-Firebase] inspect job is null")
        }
    }

    private fun onResume() {
        LogUtil.d(TAG, "[Vest-Firebase] onResume")
    }

    private fun onDestroy() {
        LogUtil.d(TAG, "[Vest-Firebase] onDestroy")
    }

}