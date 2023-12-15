package code.sdk.shf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import code.sdk.core.VestCore;
import code.sdk.core.VestGameReason;
import code.sdk.core.VestInspectCallback;
import code.sdk.core.manager.AdjustManager;
import code.sdk.core.manager.InstallReferrerManager;
import code.sdk.core.util.GoogleAdIdInitializer;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.TestUtil;
import code.sdk.shf.inspector.InitInspector;
import code.sdk.shf.remote.RemoteConfig;
import code.sdk.shf.remote.RemoteSourceSHF;
import code.util.AppGlobal;
import code.util.LogUtil;
import code.util.UrlUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Keep
public class VestSHF {

    private static final String TAG = VestSHF.class.getSimpleName();
    private LaunchConfig mLaunchConfig = new LaunchConfig();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsCheckUrl = true;
    private boolean mIsJump = false;
    private boolean mIsPause = false;
    private boolean mIsRunning = false;
    private CompositeDisposable mDisposables = new CompositeDisposable();

    private VestSHF() {
    }

    private static class InstanceHolder {
        private static VestSHF INSTANCE = new VestSHF();
    }

    public static VestSHF getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private VestInspectCallback mVestInspectCallback = null;

    /**
     * trying to request A/B switching, depends on setReleaseTime & setInspectDelayTime & backend config
     *
     * @param context
     * @param vestInspectCallback
     */
    public void inspect(Context context, VestInspectCallback vestInspectCallback) {
        mVestInspectCallback = vestInspectCallback;
        inspect();
    }

    private void inspect() {
        if (mIsRunning) {
            LogUtil.w(TAG, "[SHF] vest-sdk inspecting, SHF request aborted!");
            return;
        }
        mIsRunning = true;
        boolean isTestIntentHandled = VestCore.isTestIntentHandled();
        TestUtil.printDebugInfo();
        LogUtil.setDebug(TestUtil.isLoggable());
        if (isTestIntentHandled) {
            LogUtil.d(TAG, "[SHF] open WebView using intent, SHF request aborted!");
            mIsRunning = false;
            return;
        }
        Disposable disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                    emitter.onNext(canInspect());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(canInspect -> {
                    if (!canInspect) {
                        mIsJump = true;
                        mIsRunning = false;
                        if (mVestInspectCallback != null) {
                            mVestInspectCallback.onShowVestGame(VestGameReason.REASON_NOT_THE_TIME);
                        }
                        return;
                    }
                    startInspect();
                });
        mDisposables.add(disposable);
    }

    /**
     * Check whether the url format is correct, default true
     *
     * @param isCheckUrl true: Check the url, false not check
     */
    public void setCheckUrl(boolean isCheckUrl) {
        mIsCheckUrl = isCheckUrl;
    }

    /**
     * setup duration of silent period for requesting A/B switching starting from the date of apk build
     *
     * @param time     duration of time
     * @param timeUnit time unit for example DAYS, HOURS, MINUTES, SECONDS
     */
    public void setInspectDelayTime(long time, TimeUnit timeUnit) {
        long timeMills = timeUnit.toMillis(time);
        PreferenceUtil.saveInspectDelay(timeMills);
    }

    /**
     * setup the date of apk build
     * don't need to invoke this method if using vest-plugin, vest-plugin will setup release time automatically
     * if not, you need to invoke this method to setup release time
     * this method has the first priority when using both ways.
     *
     * @param releaseTime time format：yyyy-MM-dd HH:mm:ss
     */
    public void setReleaseTime(String releaseTime) {
        PreferenceUtil.saveReleaseTime(releaseTime);
    }


    private boolean canInspect() {
        LogUtil.w(TAG, "[SHF] start canInspect");
        boolean canInspect = true;
        //读取assets目录下所有文件，找出特殊标记的文件读取数据时间
        long inspectStartTime = PreferenceUtil.getInspectStartTime();
        long inspectDelay = PreferenceUtil.getInspectDelay();
        long inspectTimeMills = inspectStartTime + inspectDelay;
        if (inspectStartTime > 0 && inspectDelay > 0) {
            canInspect = System.currentTimeMillis() > inspectTimeMills;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            if (canInspect) {
                LogUtil.d(TAG, "[SHF] inspect date from: [%s - %s], now is ahead of inspect date!",
                        format.format(new Date(inspectStartTime)), format.format(new Date(inspectTimeMills)));
            } else {
                LogUtil.d(TAG, "[SHF] inspect date from: [%s - %s], now is behind of inspect date!",
                        format.format(new Date(inspectStartTime)), format.format(new Date(inspectTimeMills)));
            }
        } else {
            LogUtil.w(TAG, "[SHF] inspect date not set, continue inspecting");
        }
        return canInspect;
    }

    private void startInspect() {
        LogUtil.d(TAG, "[SHF] inspect start");
        Disposable disposable = Observable.create(new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                        mLaunchConfig.setStartMills(System.currentTimeMillis());
                        String installReferrer = InstallReferrerManager.getInstallReferrer();
                        if (TextUtils.isEmpty(installReferrer) || InstallReferrerManager.INSTALL_REFERRER_UNKNOWN.equals(installReferrer)) {
                            InstallReferrerManager.initInstallReferrer();
                        }
                        GoogleAdIdInitializer.init();
                        boolean inspected = new InitInspector().inspect();
                        LogUtil.d(TAG, "[SHF] onInspectResult: " + inspected);
                        emitter.onNext(inspected);
                    }
                }).flatMap(new Function<Boolean, ObservableSource<RemoteConfig>>() {
                    @Override
                    public ObservableSource<RemoteConfig> apply(Boolean inspected) throws Throwable {
                        if (inspected) {
                            return createRemoteConfigObservable();
                        } else {
                            return Observable.error(new IllegalStateException("[SHF] inspect return false"));
                        }
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<RemoteConfig>() {
                    @Override
                    public void accept(RemoteConfig remoteConfig) throws Throwable {
                        LogUtil.d(TAG, "[SHF] inspect result: " + remoteConfig);
                        checkRemoteConfig(remoteConfig);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) throws Throwable {
                        LogUtil.e(TAG, "[SHF] inspect encounter an error: " + (e == null ? "" : e.getMessage()));
                        checkRemoteConfig(null);
                    }
                });
        mDisposables.add(disposable);

    }

    private ObservableSource<RemoteConfig> createRemoteConfigObservable() {
        return Observable.create(new ObservableOnSubscribe<RemoteConfig>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<RemoteConfig> emitter) throws Throwable {
                RemoteSourceSHF remoteSource = new RemoteSourceSHF(AppGlobal.getApplication());
                remoteSource.setCallback((success, remoteConfig) -> {
                    //这里的执行需要按照严格顺序：
                    //1.先保存目标国家
                    //2.初始化TD/Adjust SDK
                    //3.执行Adjust Track需要用到第一步保存的目标国家
                    if (success && remoteConfig != null) {
                        PreferenceUtil.saveTargetCountry(remoteConfig.getCountry());
                        PreferenceUtil.saveChildBrand(remoteConfig.getChildBrd());
                    }
                    VestCore.updateThirdSDK();
                    if (success && remoteConfig != null) {
                        AdjustManager.trackEventGreeting(null);
                        emitter.onNext(remoteConfig);
                    } else {
                        emitter.onError(new IllegalStateException("remote config is null"));
                    }
                });
                remoteSource.fetch();
            }
        });
    }

    /**
     * Firebase & SHF use the same jump Logic
     *
     * @param config
     */
    @SuppressLint("CheckResult")
    private void checkRemoteConfig(RemoteConfig config) {
        String[] urls = config == null ? PreferenceUtil.readGameUrls() : PreferenceUtil.saveGameUrls(config.getUrls());
        boolean switcher = config != null && config.isSwitcher();
        boolean savedSwitcher = PreferenceUtil.readSwitcher();
        String savedGameUrl = PreferenceUtil.readGameUrl();
        if (mIsCheckUrl) {
            //list all urls including cached url for URL checking
            List<String> urlList = new ArrayList<>();
            for (int i = 0; i < urls.length; i++) {
                String url = urls[i];
                if (URLUtil.isValidUrl(url) && !urlList.contains(url)) {
                    urlList.add(url);
                }
            }
            if (URLUtil.isValidUrl(savedGameUrl)) {
                urlList.add(savedGameUrl);
            }
            LogUtil.d(TAG, "[SHF] check url: %s, savedSwitcher: %s, savedGameUrl: %s", urlList, savedSwitcher, savedGameUrl);
            Disposable disposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
                        for (String url : urlList) {
                            boolean isValid = UrlUtil.isValidUrl(url);
                            LogUtil.d(TAG, "[SHF] check url: %s, isValid：%b", url, isValid);
                            if (isValid) {
                                emitter.onNext(url);
                                return;
                            }
                        }
                        emitter.onNext("");
                    }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(url -> {
                        if (TextUtils.isEmpty(url)) {
                            LogUtil.d(TAG, "[SHF] check url: there's not any valid url, switcher: %s", switcher);
                            mLaunchConfig.setGoWeb(false);
                        } else {
                            if (savedSwitcher) {
                                LogUtil.d(TAG, "[SHF] check url: valid url found for old user: %s, switcher: %s", url, switcher);
                                mLaunchConfig.setGoWeb(true);
                            } else {
                                LogUtil.d(TAG, "[SHF] check url: valid url found for new user: %s, switcher: %s", url, switcher);
                                mLaunchConfig.setGoWeb(switcher);
                                PreferenceUtil.saveSwitcher(switcher);
                            }
                            mLaunchConfig.setGameUrl(url);
                            PreferenceUtil.saveGameUrl(url);
                        }
                        checkJump();
                    });
            mDisposables.add(disposable);
        } else {
            LogUtil.d(TAG, "[SHF] check url: skipping, switcher: %s", switcher);
            mLaunchConfig.setGoWeb(switcher);
            mLaunchConfig.setGameUrl(config == null ? "" : config.getUrls());
            checkJump();
        }

    }

    public void checkJump() {
        long delayMills = System.currentTimeMillis() - mLaunchConfig.getStartMills();
        LogUtil.d(TAG, "[SHF] jump to activity after delay %d mills", delayMills);
        if (delayMills >= mLaunchConfig.getLaunchOverTime()) {
            doJump();
        } else {
            mHandler.removeCallbacks(mJumpDelayTask);
            mHandler.postDelayed(mJumpDelayTask, mLaunchConfig.getLaunchOverTime() - delayMills);
        }
    }

    private final Runnable mJumpDelayTask = () -> doJump();

    private void doJump() {
        LogUtil.d(TAG, "[SHF] LaunchConfig: isGogoWeb=%s, gameUrl=%s", mLaunchConfig.isGoWeb(), mLaunchConfig.getGameUrl());
        if (mLaunchConfig.isGoWeb()) {
            if (mVestInspectCallback != null) {
                mVestInspectCallback.onShowOfficialGame(mLaunchConfig.getGameUrl());
            }
        } else {
            if (mVestInspectCallback != null) {
                mVestInspectCallback.onShowVestGame(VestGameReason.REASON_OFF_ON_SERVER);
            }
        }
        mIsJump = true;
        mIsRunning = false;
    }

    public void onPause() {
        mIsPause = true;
        mIsRunning = false;
        LogUtil.d(TAG, "[SHF] onPause mIsJump: %b", mIsJump);
        try {
            mDisposables.clear();
        } catch (Exception e) {
        }

    }

    public void onResume() {
        LogUtil.d(TAG, "[SHF] onResume mIsJump: %b", mIsJump);
        if (!mIsJump && mIsPause) {
            inspect();
        }
        mIsPause = false;
    }

    public void onDestroy() {
        mIsJump = false;
        LogUtil.d(TAG, "[SHF] onDestroy mIsJump: %b", mIsJump);
    }
}
