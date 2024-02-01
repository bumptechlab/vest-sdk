package code.sdk.shf

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.webkit.URLUtil
import code.sdk.core.VestCore
import code.sdk.core.VestGameReason
import code.sdk.core.VestInspectCallback
import code.sdk.core.event.SDKEvent
import code.sdk.core.manager.AdjustManager
import code.sdk.core.manager.InstallReferrerManager
import code.sdk.core.util.CocosPreferenceUtil
import code.sdk.core.util.GoogleAdIdInitializer
import code.sdk.core.util.PreferenceUtil
import code.sdk.core.util.TestUtil
import code.sdk.shf.inspector.InitInspector
import code.sdk.shf.remote.RemoteCallback
import code.sdk.shf.remote.RemoteConfig
import code.sdk.shf.remote.RemoteSourceSHF
import code.util.AppGlobal
import code.util.LogUtil
import code.util.LogUtil.setDebug
import code.util.UrlChecker.isUrlAvailable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class VestSHF private constructor() {
    private val mLaunchConfig = LaunchConfig()
    private val mHandler = Handler(Looper.getMainLooper())
    private val mDisposables = CompositeDisposable()
    private var mIsCheckUrl = true
    private var mIsJump = false
    private var mIsPause = false
    private var mIsRunning = false
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
    fun inspect(context: Context?, vestInspectCallback: VestInspectCallback?) {
        mVestInspectCallback = vestInspectCallback
        inspect()
    }

    private fun inspect() {
        if (mIsRunning) {
            LogUtil.w(TAG, "[Vest-SHF] vest-sdk inspecting, SHF request aborted!")
            return
        }
        mIsRunning = true
        val isTestIntentHandled = VestCore.isTestIntentHandled()
        TestUtil.printDebugInfo()
        setDebug(TestUtil.isLoggable())
        if (isTestIntentHandled) {
            LogUtil.d(TAG, "[Vest-SHF] open WebView using intent, SHF request aborted!")
            mIsRunning = false
            return
        }
        val disposable = Observable.create { emitter -> emitter.onNext(canInspect()) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { canInspect: Boolean? ->
                if (!canInspect!!) {
                    mIsJump = true
                    mIsRunning = false
                    if (mVestInspectCallback != null) {
                        mVestInspectCallback!!.onShowVestGame(VestGameReason.REASON_NOT_THE_TIME)
                    }
                    return@subscribe
                }
                startInspect()
            }
        mDisposables.add(disposable)
    }

    /**
     * Check whether the url format is correct, default true
     *
     * @param isCheckUrl true: Check the url, false not check
     */
    fun setCheckUrl(isCheckUrl: Boolean) {
        mIsCheckUrl = isCheckUrl
    }

    /**
     * setup duration of silent period for requesting A/B switching starting from the date of apk build
     *
     * @param time     duration of time
     * @param timeUnit time unit for example DAYS, HOURS, MINUTES, SECONDS
     */
    fun setInspectDelayTime(time: Long, timeUnit: TimeUnit) {
        val timeMills = timeUnit.toMillis(time)
        PreferenceUtil.saveInspectDelay(timeMills)
    }

    /**
     * setup the date of apk build
     * don't need to invoke this method if using vest-plugin, vest-plugin will setup release time automatically
     * if not, you need to invoke this method to setup release time
     * this method has the first priority when using both ways.
     *
     * @param releaseTime time format：yyyy-MM-dd HH:mm:ss
     */
    fun setReleaseTime(releaseTime: String?) {
        PreferenceUtil.saveReleaseTime(releaseTime)
    }

    private fun canInspect(): Boolean {
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
                LogUtil.d(TAG, "[Vest-SHF] inspect date from: [%s - %s], now is ahead of inspect date!",
                    format.format(Date(inspectStartTime)), format.format(Date(inspectTimeMills)))
            } else {
                LogUtil.d(TAG, "[Vest-SHF] inspect date from: [%s - %s], now is behind of inspect date!",
                    format.format(Date(inspectStartTime)), format.format(Date(inspectTimeMills)))
            }
        } else {
            LogUtil.w(TAG, "[Vest-SHF] inspect date not set, continue inspecting")
        }
        return canInspect
    }

    private fun startInspect() {
        LogUtil.d(TAG, "[Vest-SHF] inspect start")
        val disposable = Observable.create { emitter ->
            mLaunchConfig.startMills = System.currentTimeMillis()
            val installReferrer = InstallReferrerManager.getInstallReferrer()
            if (TextUtils.isEmpty(installReferrer) || InstallReferrerManager.INSTALL_REFERRER_UNKNOWN == installReferrer) {
                InstallReferrerManager.initInstallReferrer()
            }
            GoogleAdIdInitializer.init()
            val inspected = InitInspector().inspect()
            LogUtil.d(TAG, "[Vest-SHF] onInspectResult: $inspected")
            emitter.onNext(inspected)
        }.flatMap { inspected ->
            if (inspected) {
                createRemoteConfigObservable()
            } else {
                Observable.error(IllegalStateException("[Vest-SHF] inspect return false"))
            }
        }.subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ remoteConfig ->
                LogUtil.d(TAG, "[Vest-SHF] inspect result: $remoteConfig")
                checkRemoteConfig(remoteConfig)
            }) { e ->
                LogUtil.e(TAG, "[Vest-SHF] inspect encounter an error: " + e.message)
                checkRemoteConfig(null)
            }
        mDisposables.add(disposable)
    }

    private fun createRemoteConfigObservable(): ObservableSource<RemoteConfig> {
        return Observable.create { emitter ->
            val remoteSource = RemoteSourceSHF(AppGlobal.application!!)
            remoteSource.setCallback(object : RemoteCallback{
                override fun onResult(success: Boolean, remoteConfig: RemoteConfig?) {
                    if (success && remoteConfig != null) {
                        emitter.onNext(remoteConfig)
                    } else {
                        emitter.onError(IllegalStateException("remote config is null"))
                    }
                }
            })
            remoteSource.fetch()
        }
    }

    /**
     * Firebase & SHF use the same jump Logic
     *
     * @param remoteConfig
     */
    @SuppressLint("CheckResult")
    private fun checkRemoteConfig(remoteConfig: RemoteConfig?) {
        val remoteSwitcher = remoteConfig != null && remoteConfig.isSwitcher
        val savedSwitcher = PreferenceUtil.readSwitcher()
        val savedGameUrl = PreferenceUtil.readGameUrl()
        if (remoteSwitcher) {
            PreferenceUtil.saveGameUrls(remoteConfig!!.urls)
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
            LogUtil.d(TAG, "[Vest-SHF] check url: %s, savedSwitcher: %s, savedGameUrl: %s",
                urlList, savedSwitcher, savedGameUrl)
            val disposable = Observable.create(object :ObservableOnSubscribe<String>{
                override fun subscribe(emitter: ObservableEmitter<String>) {
                    for (url in urlList) {
                        val isAvailable = isUrlAvailable(url)
                        LogUtil.d(TAG, "[Vest-SHF] check url: %s, available：%b", url, isAvailable)
                        if (isAvailable) {
                            emitter.onNext(url)
                            return
                        }
                    }
                    emitter.onNext("")
                }

            }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { url: String? ->
                    var childBrand = if (remoteConfig != null) remoteConfig.childBrd else ""
                    val targetCountry = if (remoteConfig != null) remoteConfig.country else ""
                    if (url.isNullOrEmpty()) {
                        LogUtil.d(TAG,"[Vest-SHF] check url: there's not any available url, switcher: %s",
                            remoteSwitcher)
                        mLaunchConfig.isGoWeb = false
                    } else {
                        if (savedSwitcher) {
                            LogUtil.d(TAG,"[Vest-SHF] check url: available url found for old user: %s, switcher: %s",
                                url,remoteSwitcher)
                            mLaunchConfig.isGoWeb = true
                        } else {
                            LogUtil.d(TAG, "[Vest-SHF] check url: available url found for new user: %s, switcher: %s",
                                url,remoteSwitcher)
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
                        LogUtil.d(TAG, "parse url brand: %s", urlBrand)
                        if (!TextUtils.isEmpty(urlBrand)) {
                            childBrand = urlBrand
                        }
                    }
                    //这里的执行需要按照严格顺序：
                    //1.先保存目标国家
                    //2.初始化TD/Adjust SDK
                    //3.继续跳转地址
                    LogUtil.d(TAG, "save targetCountry: %s, childBrand: %s", targetCountry, childBrand)
                    PreferenceUtil.saveTargetCountry(targetCountry)
                    PreferenceUtil.saveChildBrand(childBrand)
                    VestCore.updateThirdSDK()
                    if (remoteConfig != null) {
                        AdjustManager.trackEventGreeting(null)
                    }
                    checkJump()
                }
            mDisposables.add(disposable)
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
            checkJump()
        }
    }

   private fun checkJump() {
        val delayMills = System.currentTimeMillis() - mLaunchConfig.startMills
        LogUtil.d(TAG, "[Vest-SHF] jump to activity after delay %d mills", delayMills)
        if (delayMills >= mLaunchConfig.launchOverTime) {
            doJump()
        } else {
            mHandler.removeCallbacks(mJumpDelayTask)
            mHandler.postDelayed(mJumpDelayTask, mLaunchConfig.launchOverTime - delayMills)
        }
    }

    private val mJumpDelayTask = Runnable { doJump() }


    private fun doJump() {
        LogUtil.d(TAG, "[Vest-SHF] LaunchConfig: isGogoWeb=%s, gameUrl=%s",
            mLaunchConfig.isGoWeb, mLaunchConfig.gameUrl)
        if (mLaunchConfig.isGoWeb) {
            mVestInspectCallback?.onShowOfficialGame(mLaunchConfig.gameUrl ?: "")
        } else {
            mVestInspectCallback?.onShowVestGame(VestGameReason.REASON_OFF_ON_SERVER)
        }
        AdjustManager.trackEventStart(null)
        mIsJump = true
        mIsRunning = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceiveSDKEvent(sdkEvent: SDKEvent) {
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
        try {
            mDisposables.clear()
        } catch (_: Exception) {
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