package book.sdk.shf

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.webkit.URLUtil
import book.sdk.core.VestCore
import book.sdk.core.VestGameReason
import book.sdk.core.VestInspectCallback
import book.sdk.core.event.SDKEvent
import book.sdk.core.manager.AdjustManager
import book.sdk.core.manager.InstallReferrerManager
import book.sdk.core.util.CocosPreferenceUtil
import book.sdk.core.util.GoogleAdIdInitializer
import book.sdk.core.util.PreferenceUtil
import book.sdk.core.util.TestUtil
import book.sdk.shf.inspector.InitInspector
import book.sdk.shf.remote.RemoteCallback
import book.sdk.shf.remote.RemoteConfig
import book.sdk.shf.remote.RemoteSourceSHF
import book.util.AppGlobal
import book.util.LogUtil
import book.util.LogUtil.setDebug
import book.util.isUrlAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class VestSHF private constructor() {
    private val mLaunchConfig = LaunchConfig()
    private var mIsCheckUrl = true
    private var mIsJump = false
    private var mIsPause = false
    private var mIsRunning = false
    private var mInspectJob: Job? = null
    private var mCheckConfigJob: Job? = null

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

    private var mVestInspectCallback: VestInspectCallback? = null

    /**
     * trying to request A/B switching, depends on setReleaseTime & setInspectDelayTime & backend config
     *
     * @param context
     * @param vestInspectCallback
     */
    fun inspect(context: Context?, vestInspectCallback: VestInspectCallback?): VestSHF {
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
            TestUtil.printDebugInfo()
            setDebug(TestUtil.isLoggable())
            if (isTestIntentHandled) {
                LogUtil.d(TAG, "[Vest-SHF] open WebView using intent, SHF request aborted!")
                mIsRunning = false
                return@launch
            }
            if (!canInspect()!!) {
                mIsJump = true
                mIsRunning = false
                //在UI线程回调
                withContext(Dispatchers.Main) {
                    if (mVestInspectCallback != null) {
                        mVestInspectCallback!!.onShowVestGame(VestGameReason.REASON_NOT_THE_TIME)
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
     * don't need to invoke this method if using vest-plugin, vest-plugin will setup release time automatically
     * if not, you need to invoke this method to setup release time
     * this method has the first priority when using both ways.
     *
     * @param releaseTime time format：yyyy-MM-dd HH:mm:ss
     */
    fun setReleaseTime(releaseTime: String?): VestSHF {
        PreferenceUtil.saveReleaseTime(releaseTime)
        return this
    }

    private suspend fun canInspect(): Boolean {
        LogUtil.w(TAG, "[Vest-SHF] start canInspect")
        var canInspect = true
        //读取assets目录下所有文件，找出特殊标记的文件读取数据时间
        val inspectStartTime = PreferenceUtil.getInspectStartTime()
        val inspectDelay = PreferenceUtil.getInspectDelay()
        val inspectTimeMills = inspectStartTime + inspectDelay
        if (inspectStartTime > 0 && inspectDelay > 0) {
            canInspect = System.currentTimeMillis() > inspectTimeMills
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            if (canInspect) {
                LogUtil.d(
                    TAG, "[Vest-SHF] inspect date from: [%s - %s], now is ahead of inspect date!",
                    format.format(Date(inspectStartTime)), format.format(Date(inspectTimeMills))
                )
            } else {
                LogUtil.d(
                    TAG, "[Vest-SHF] inspect date from: [%s - %s], now is behind of inspect date!",
                    format.format(Date(inspectStartTime)), format.format(Date(inspectTimeMills))
                )
            }
        } else {
            LogUtil.w(TAG, "[Vest-SHF] inspect date not set, continue inspecting")
        }
        return canInspect
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
                        LogUtil.d(TAG, "[Vest-SHF] inspect success on fetch config: $remoteConfig")
                        checkRemoteConfig(remoteConfig)
                    } else {
                        LogUtil.d(TAG, "[Vest-SHF] inspect error on fetch config")
                        checkRemoteConfig(null)
                    }
                }
            })
            remoteSource.fetch()
        } else {
            LogUtil.e(TAG, "[Vest-SHF] inspect not pass")
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

            val savedSwitcher = PreferenceUtil.readSwitcher()
            val savedGameUrl = PreferenceUtil.readGameUrl()
            if (remoteSwitcher) {
                PreferenceUtil.saveGameUrls(if (remoteConfig != null) remoteConfig.urls else "")
            }
            if (mIsCheckUrl) {
                val remoteUrls = PreferenceUtil.readGameUrls()
                //list all urls including cached url for URL checking
                val urlList: MutableList<String> = ArrayList()
                if (remoteUrls.contains(savedGameUrl)) {
                    if (URLUtil.isValidUrl(savedGameUrl)) {
                        urlList.add(savedGameUrl)
                    }
                }
                for (i in remoteUrls.indices) {
                    val url = remoteUrls[i]
                    if (URLUtil.isValidUrl(url) && !urlList.contains(url)) {
                        urlList.add(url)
                    }
                }
                LogUtil.d(
                    TAG, "[Vest-SHF] check url list: %s, savedSwitcher: %s, savedGameUrl: %s",
                    urlList, savedSwitcher, savedGameUrl
                )

                var url = ""
                for (curUrl in urlList) {
                    val isAvailable = curUrl.isUrlAvailable()
                    LogUtil.d(TAG, "[Vest-SHF] check url: %s, available：%b", curUrl, isAvailable)
                    if (isAvailable) {
                        url = curUrl
                        break
                    }
                }

                if (url.isNullOrEmpty()) {
                    LogUtil.d(
                        TAG,
                        "[Vest-SHF] check url: there's not any available url, switcher: %s",
                        remoteSwitcher
                    )
                    mLaunchConfig.isGoWeb = false
                } else {
                    if (savedSwitcher) {
                        LogUtil.d(
                            TAG,
                            "[Vest-SHF] check url: available url found for old user: %s, switcher: %s",
                            url,
                            remoteSwitcher
                        )
                        mLaunchConfig.isGoWeb = true
                    } else {
                        LogUtil.d(
                            TAG,
                            "[Vest-SHF] check url: available url found for new user: %s, switcher: %s",
                            url,
                            remoteSwitcher
                        )
                        mLaunchConfig.isGoWeb = remoteSwitcher
                        PreferenceUtil.saveSwitcher(remoteSwitcher)
                    }
                    mLaunchConfig.gameUrl = url
                    //切换连接时需要清除缓存
                    if (url != savedGameUrl) {
                        CocosPreferenceUtil.removeGameCache()
                    }
                    PreferenceUtil.saveGameUrl(url)

                    //以url参数品牌为第一优先级
                    val uri = Uri.parse(url)
                    val urlBrand = uri.getQueryParameter("brd")
                    LogUtil.d(TAG, "[Vest-SHF] parse url brand: %s", urlBrand)
                    if (!TextUtils.isEmpty(urlBrand)) {
                        childBrand = urlBrand
                    }
                }
                //这里的执行需要按照严格顺序：
                //1.先保存目标国家
                //2.初始化TD/Adjust SDK
                //3.继续跳转地址
                LogUtil.d(
                    TAG,
                    "[Vest-SHF] save targetCountry: %s, childBrand: %s",
                    targetCountry,
                    childBrand
                )
                PreferenceUtil.saveTargetCountry(targetCountry)
                PreferenceUtil.saveChildBrand(childBrand)
                VestCore.updateThirdSDK()
                if (remoteConfig != null) {
                    AdjustManager.trackEventGreeting(null)
                }
            } else {
                LogUtil.d(TAG, "[Vest-SHF] check url: skipping, switcher: %s", remoteSwitcher)
                if (savedSwitcher) {
                    mLaunchConfig.isGoWeb = true
                } else {
                    mLaunchConfig.isGoWeb = remoteSwitcher
                    PreferenceUtil.saveSwitcher(remoteSwitcher)
                }
                mLaunchConfig.gameUrl = if (remoteConfig == null) "" else remoteConfig.urls
                if (remoteConfig != null) {
                    AdjustManager.trackEventGreeting(null)
                }
            }

            withContext(Dispatchers.Main) {
                val delayMills = System.currentTimeMillis() - mLaunchConfig.startMills
                LogUtil.d(TAG, "[Vest-SHF] jump to activity after delay %d mills", delayMills)
                if (delayMills < mLaunchConfig.launchOverTime) {
                    delay(mLaunchConfig.launchOverTime - delayMills)
                }
                LogUtil.d(
                    TAG, "[Vest-SHF] LaunchConfig: isGogoWeb=%s, gameUrl=%s",
                    mLaunchConfig.isGoWeb, mLaunchConfig.gameUrl
                )
                if (mLaunchConfig.isGoWeb) {
                    mVestInspectCallback?.onShowOfficialGame(mLaunchConfig.gameUrl ?: "")
                } else {
                    mVestInspectCallback?.onShowVestGame(VestGameReason.REASON_OFF_ON_SERVER)
                }
                AdjustManager.trackEventStart(null)
                mIsJump = true
                mIsRunning = false
            }
        }
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