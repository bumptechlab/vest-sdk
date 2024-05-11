package poetry.sdk.shf

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.webkit.URLUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import poetry.sdk.core.util.CocosPreferenceUtil
import poetry.sdk.core.util.GoogleAdIdInitializer
import poetry.sdk.core.util.PreferenceUtil
import poetry.sdk.core.util.Tester
import poetry.sdk.shf.remote.RemoteCallback
import poetry.sdk.shf.remote.RemoteConfig
import poetry.sdk.shf.remote.RemoteSourceSHF
import poetry.util.AppGlobal
import poetry.util.ImitateChecker
import poetry.util.LogUtil
import poetry.util.LogUtil.setDebug
import poetry.util.isUrlAvailable
import java.util.concurrent.TimeUnit

class VestSHF private constructor() {
    private val mLaunchConfig = LaunchConfig()
    private var mIsCheckUrl = true
    private var mIsJump = false
    private var mIsPause = false
    private var mIsRunning = false
    private var mInspectJob: Job? = null
    private var mCheckConfigJob: Job? = null
    private var mContext: Context? = null
    private var mVestInspectCallback: VestInspectCallback? = null

    init {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    private object InstanceHolder {
        val INSTANCE = VestSHF()
    }

    companion object {
        private val TAG = VestSHF::class.java.simpleName

        @JvmStatic
        fun getInstance(): VestSHF {
            return InstanceHolder.INSTANCE
        }
    }

    /**
     * trying to request A/B switching, depends on setReleaseTime & setInspectDelayTime & backend config
     *
     * @param context
     * @param vestInspectCallback
     */
    fun inspect(context: Context?, vestInspectCallback: VestInspectCallback?): VestSHF {
        mContext = context
        mVestInspectCallback = vestInspectCallback
        inspect()
        return this
    }

    private fun inspect() {
        mInspectJob = MainScope().launch(Dispatchers.IO) {
            if (mIsRunning) {
                LogUtil.w(TAG, "[Vest-SHF] vest-sdk inspecting, SHF request aborted!")
                return@launch
            }
            mIsRunning = true
            val isTestIntentHandled = VestCore.isTestIntentHandled()
            setDebug(Tester.isLoggable())
            if (isTestIntentHandled) {
                LogUtil.d(TAG, "[Vest-SHF] open WebView using intent, SHF request aborted!")
                mIsRunning = false
                return@launch
            }
            if (!canInspect()) {
                mIsJump = true
                mIsRunning = false
                //在UI线程回调
                withContext(Dispatchers.Main) {
                    if (mVestInspectCallback != null) {
                        mVestInspectCallback?.onShowASide(VestInspectResult.REASON_NOT_THE_TIME)
                    }
                }
                return@launch
            }
            startInspect()
        }
    }

    /**
     * Check whether the url format is correct, default true
     *
     * @param isCheckUrl true: Check the url, false not check
     */
    fun setCheckUrl(isCheckUrl: Boolean): VestSHF {
        mIsCheckUrl = isCheckUrl
        return this
    }

    /**
     * setup duration of silent period for requesting A/B switching starting from the date of apk build
     *
     * @param time     duration of time
     * @param timeUnit time unit for example DAYS, HOURS, MINUTES, SECONDS
     */
    fun setInspectDelayTime(time: Long, timeUnit: TimeUnit): VestSHF {
        val timeMills = timeUnit.toMillis(time)
        PreferenceUtil.saveInspectDelay(timeMills)
        return this
    }

    /**
     * setup the date of apk build
     *
     * @param releaseTime time format：yyyy-MM-dd HH:mm:ss
     */
    fun setReleaseTime(releaseTime: String?): VestSHF {
        PreferenceUtil.saveReleaseTime(releaseTime)
        return this
    }

    private suspend fun canInspect(): Boolean {
        LogUtil.w(TAG, "[Vest-SHF] start checking inspect enable")
        if (ImitateChecker.isImitate()) {
            LogUtil.dT(TAG, "[Vest-SHF] abort checking inspect on emulator", true)
            return false
        }
        //读取assets目录下所有文件，找出特殊标记的文件读取数据时间
        val inspectStartTime = PreferenceUtil.getReleaseTime()
        val inspectDelay = PreferenceUtil.getInspectDelay()
        if (inspectStartTime > 0 && inspectDelay > 0) {
            //获取最终静默截止时间
            val inspectTimeMills = inspectStartTime + inspectDelay
            LogUtil.d(
                TAG,
                "[Vest-SHF] inspect time result:${System.currentTimeMillis() >= inspectTimeMills}",
                true
            )
            if (System.currentTimeMillis() < inspectTimeMills) return false
        } else {
            LogUtil.w(TAG, "[Vest-SHF] inspect date not set, continue inspecting")
            return true
        }
        return true
    }

    private suspend fun startInspect() {
        LogUtil.d(TAG, "[Vest-SHF] inspect start")
        mLaunchConfig.startMills = System.currentTimeMillis()
        val installReferrer = InstallReferrerManager.getInstallReferrer()
        if (TextUtils.isEmpty(installReferrer) || InstallReferrerManager.INSTALL_REFERRER_UNKNOWN == installReferrer) {
            InstallReferrerManager.initInstallReferrer()
        }
        GoogleAdIdInitializer.init()
        val inspected = InitInspector().inspect()
        LogUtil.d(TAG, "[Vest-SHF] onInspectResult: $inspected")
        if (inspected) {
            val remoteSource = RemoteSourceSHF(AppGlobal.application!!)
            remoteSource.setCallback(object : RemoteCallback {
                override fun onResult(success: Boolean, remoteConfig: RemoteConfig?) {
                    if (success && remoteConfig != null) {
                        LogUtil.d(
                            TAG,
                            "[Vest-SHF] inspect success on fetch config: $remoteConfig",
                            true
                        )
                        checkRemoteConfig(remoteConfig)
                    } else {
                        LogUtil.dT(TAG, "[Vest-SHF] inspect error on fetch config", true)
                        checkRemoteConfig(null)
                    }
                }
            })
            remoteSource.fetch()
        } else {
            LogUtil.eT(TAG, "[Vest-SHF] inspect not pass", true)
            checkRemoteConfig(null)
        }
    }

    /**
     * Firebase & SHF use the same jump Logic
     *
     * @param remoteConfig
     */
    @SuppressLint("CheckResult")
    private fun checkRemoteConfig(remoteConfig: RemoteConfig?) {
        mCheckConfigJob = MainScope().launch(Dispatchers.IO) {
            val remoteSwitcher = remoteConfig != null && remoteConfig.isSwitcher
            var childBrand = if (remoteConfig != null) remoteConfig.childBrd else ""
            val targetCountry = if (remoteConfig != null) remoteConfig.country else ""
            val webViewType = if (remoteConfig != null) remoteConfig.h5Type else ""
            val jumpUrls = if (remoteConfig != null) remoteConfig.urls else ""

            val savedSwitcher = PreferenceUtil.readSwitcher()
            val savedGameUrl = PreferenceUtil.readGameUrl()
            if (remoteSwitcher) {
                if (jumpUrls?.isNotEmpty()!!) {
                    PreferenceUtil.saveGameUrls(jumpUrls)
                }
                if (webViewType?.isNotEmpty()!!) {
                    PreferenceUtil.saveWebViewType(webViewType)
                }
            }
            LogUtil.d(TAG, "[Vest-SHF] savedSwitcher: $savedSwitcher, savedGameUrl: $savedGameUrl")

            if (mIsCheckUrl) {
                val url = findAvailableUrl()
                if (url.isEmpty()) {
                    LogUtil.d(
                        TAG,
                        "[Vest-SHF] check url: there's not any available url, switcher: $remoteSwitcher"
                    )
                    mLaunchConfig.isGotoB = false
                } else {
                    if (savedSwitcher) {
                        LogUtil.d(
                            TAG,
                            "[Vest-SHF] check url: available url found for old user: $url, switcher: $remoteSwitcher"
                        )
                        mLaunchConfig.isGotoB = true
                    } else {
                        LogUtil.d(
                            TAG,
                            "[Vest-SHF] check url: available url found for new user: $url, switcher: $remoteSwitcher"
                        )
                        mLaunchConfig.isGotoB = remoteSwitcher
                        PreferenceUtil.saveSwitcher(remoteSwitcher)
                    }
                    mLaunchConfig.gameUrl = url
                    //切换连接时需要清除缓存
                    if (url != savedGameUrl) {
                        CocosPreferenceUtil.removeGameCache()
                    }
                    PreferenceUtil.saveGameUrl(url)

                    val urlBrand = parseBrdFromUrl(url)
                    if (!urlBrand.isNullOrEmpty()) {
                        childBrand = urlBrand
                    }
                }
                //这里的执行需要按照严格顺序：
                //1.先保存目标国家
                //2.初始化TD/Adjust SDK
                //3.继续跳转地址
                LogUtil.d(
                    TAG, "[Vest-SHF] save targetCountry: $targetCountry, childBrand: $childBrand"
                )
                if (!targetCountry.isNullOrEmpty()) {
                    PreferenceUtil.saveTargetCountry(targetCountry)
                }
                if (!childBrand.isNullOrEmpty()) {
                    PreferenceUtil.saveChildBrand(childBrand)
                }
                VestCore.updateThirdSDK()
                if (remoteConfig != null) {
                    AdjustManager.trackEventGreeting(null)
                }
            } else {
                LogUtil.d(TAG, "[Vest-SHF] check url: skipping, switcher: $remoteSwitcher")
                if (savedSwitcher) {
                    mLaunchConfig.isGotoB = true
                } else {
                    mLaunchConfig.isGotoB = remoteSwitcher
                    PreferenceUtil.saveSwitcher(remoteSwitcher)
                }
                mLaunchConfig.gameUrl = if (remoteConfig == null) "" else remoteConfig.urls
                if (remoteConfig != null) {
                    AdjustManager.trackEventGreeting(null)
                }
            }
            mLaunchConfig.webViewType = PreferenceUtil.getWebViewType()

            withContext(Dispatchers.Main) {
                val delayMills = System.currentTimeMillis() - mLaunchConfig.startMills
                LogUtil.d(TAG, "[Vest-SHF] go to activity after delay %d mills", delayMills)
                if (delayMills < mLaunchConfig.launchOverTime) {
                    delay(mLaunchConfig.launchOverTime - delayMills)
                }
                LogUtil.d(
                    TAG,
                    "[Vest-SHF] LaunchConfig: isGotoB=${mLaunchConfig.isGotoB}, gameUrl=${mLaunchConfig.gameUrl}"
                )
                if (mLaunchConfig.isGotoB) {
                    var launchBSuccess = false
                    if (mIsCheckUrl) {
                        LogUtil.d(TAG, "[Vest-SHF] show B-side activity inside vest-sdk")
                        launchBSuccess = VestCore.toWebViewActivity(
                            mContext, mLaunchConfig.gameUrl, mLaunchConfig.webViewType
                        )
                    } else {
                        LogUtil.d(TAG, "[Vest-SHF] show B-side activity outside vest-sdk")
                    }
                    mVestInspectCallback?.onShowBSide(mLaunchConfig.gameUrl ?: "", launchBSuccess)
                } else {
                    LogUtil.d(TAG, "[Vest-SHF] show A-side activity")
                    mVestInspectCallback?.onShowASide(VestInspectResult.REASON_OFF_ON_SERVER)
                }
                AdjustManager.trackEventStart(null)
                mIsJump = true
                mIsRunning = false
            }
        }
    }

    private fun parseBrdFromUrl(url: String): String? {
        var urlBrand: String? = null
        try {
            //以url参数品牌为第一优先级
            urlBrand = Uri.parse(url).getQueryParameter("brd")
            LogUtil.d(TAG, "[Vest-SHF] parse url brand: $urlBrand")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return urlBrand
    }

    private fun findAvailableUrl(): String {
        val remoteUrls = PreferenceUtil.readGameUrls()
        val savedGameUrl = PreferenceUtil.readGameUrl()
        //list all urls including cached url for URL checking
        val urlList: MutableList<String> = ArrayList()
        if (remoteUrls.contains(savedGameUrl)) {
            if (URLUtil.isValidUrl(savedGameUrl)) {
                urlList.add(savedGameUrl!!)
            }
        }
        for (i in remoteUrls.indices) {
            val url = remoteUrls[i]
            if (URLUtil.isValidUrl(url) && !urlList.contains(url)) {
                urlList.add(url)
            }
        }
        LogUtil.d(TAG, "[Vest-SHF] check url list: $urlList")
        var url = ""
        for (curUrl in urlList) {
            val isAvailable = curUrl.isUrlAvailable()
            LogUtil.d(TAG, "[Vest-SHF] check url: $curUrl, available：$isAvailable")
            if (isAvailable) {
                url = curUrl
                break
            }
        }
        return url
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
        LogUtil.d(TAG, "[Vest-SHF] onCreate mIsJump: %b", mIsJump)
    }

    private fun onPause() {
        LogUtil.d(TAG, "[Vest-SHF] onPause mIsJump: %b", mIsJump)
        mIsPause = true
        mIsRunning = false

        if (mInspectJob != null) {
            if (mInspectJob?.isActive!!) {
                LogUtil.d(TAG, "[Vest-SHF] inspect job is active, cancel now")
                mInspectJob?.cancel()
            } else {
                LogUtil.d(TAG, "[Vest-SHF] inspect job is not active")
            }
        } else {
            LogUtil.d(TAG, "[Vest-SHF] inspect job is null")
        }

        if (mCheckConfigJob != null) {
            if (mCheckConfigJob?.isActive!!) {
                LogUtil.d(TAG, "[Vest-SHF] check config job is active, cancel now")
                mCheckConfigJob?.cancel()
            } else {
                LogUtil.d(TAG, "[Vest-SHF] check config job is not active")
            }
        } else {
            LogUtil.d(TAG, "[Vest-SHF] check config job is null")
        }
    }

    private fun onResume() {
        LogUtil.d(TAG, "[Vest-SHF] onResume mIsJump: %b", mIsJump)
        if (!mIsJump && mIsPause) {
            inspect()
        }
        mIsPause = false
    }

    private fun onDestroy() {
        mIsJump = false
        LogUtil.d(TAG, "[Vest-SHF] onDestroy mIsJump: %b", mIsJump)
    }


}