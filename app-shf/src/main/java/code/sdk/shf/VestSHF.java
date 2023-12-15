package code.sdk.shf;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class VestSHF {

    private static final String TAG = VestSHF.class.getSimpleName();
    private LaunchConfig mLaunchConfig = new LaunchConfig();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsCheckUrl = true;

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
        boolean isTestIntentHandled = VestCore.isTestIntentHandled();
        TestUtil.printDebugInfo();
        LogUtil.setDebug(TestUtil.isLoggable());
        if (isTestIntentHandled) {
            LogUtil.d(TAG, "[SHF] open WebView using intent, SHF request aborted!");
            return;
        }
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                    emitter.onNext(canInspect());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(canInspect -> {
                    if (!canInspect) {
                        if (vestInspectCallback != null) {
                            vestInspectCallback.onShowVestGame(VestGameReason.REASON_NOT_THE_TIME);
                        }
                        return;
                    }
                    startInspect();
                });
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
     * @param releaseTime time format：2023-12-04 16:27:20
     */
    public void setReleaseTime(String releaseTime) {
        PreferenceUtil.saveReleaseTime(releaseTime);
    }


    private boolean canInspect() {
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
            LogUtil.d(TAG, "[SHF] inspect date not set");
        }
        return canInspect;
    }

    private void startInspect() {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
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
                .subscribe(new Observer<RemoteConfig>() {
                    //这是新加入的方法，在订阅后发送数据之前，
                    //回首先调用这个方法，而Disposable可用于取消订阅
                    @Override
                    public void onSubscribe(Disposable d) {
                        LogUtil.d(TAG, "[SHF] inspect start");
                    }

                    @Override
                    public void onNext(RemoteConfig remoteConfig) {
                        LogUtil.d(TAG, "[SHF] inspect result: " + remoteConfig);
                        checkRemoteConfig(remoteConfig);
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogUtil.e(TAG, "[SHF] inspect encounter an error: " + (e == null ? "" : e.getMessage()));
                        checkRemoteConfig(null);
                    }

                    @Override
                    public void onComplete() {
                        LogUtil.d(TAG, "[SHF] inspect complete");
                    }
                });

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
    private void checkRemoteConfig(RemoteConfig config) {
        boolean savedSwitcher = PreferenceUtil.readSwitcher();
        String savedGameUrl = PreferenceUtil.readGameUrl();
        String remoteGameUrl = config == null ? null : config.getUrl();
        boolean savedUrlValid = true;
        boolean remoteUrlValid = true;
        if (mIsCheckUrl) {
            savedUrlValid = URLUtil.isValidUrl(savedGameUrl);
            remoteUrlValid = URLUtil.isValidUrl(remoteGameUrl);
        }


        LogUtil.d(TAG, "[SHF] checkRemoteConfig: %s, savedSwitcher: %s, savedGameUrl: %s",
                config, savedSwitcher, savedGameUrl);
        if (savedSwitcher) {// 如果是老用户,且服务器有新链接,以服务器的新链接为准,如果关闭马甲,后台不会返回gameUrl
            if (remoteUrlValid) {
                PreferenceUtil.saveGameUrl(remoteGameUrl);
                LogUtil.d(TAG, "[SHF] checkRemoteConfig[master]: switch on -> update url: %s", remoteGameUrl);
                mLaunchConfig.setGoWeb(true);
                mLaunchConfig.setGameUrl(remoteGameUrl);
            } else if (savedUrlValid) {// 如果没有返回新链接,则以老链接为主
                //ObfuscationStub8.inject();
                LogUtil.d(TAG, "[SHF] checkRemoteConfig[master]: switch on -> read cached url: %s", savedGameUrl);
                mLaunchConfig.setGoWeb(true);
                mLaunchConfig.setGameUrl(savedGameUrl);
                // validate game url (could be set by cocos)
                new Thread(new GameUrlValidatorRunnable(savedGameUrl)).start();
            } else {// 如果没有老链接,就进入马甲游戏
                LogUtil.d(TAG, "[SHF] checkRemoteConfig[master]: switch off -> no cached url");
                mLaunchConfig.setGoWeb(false);
            }
        } else {// 新用户
            if (config == null) {
                LogUtil.d(TAG, "[SHF] checkRemoteConfig[guest]: switch off -> config is empty");
                mLaunchConfig.setGoWeb(false);
            } else if (config.isSwitcher() && remoteUrlValid) {
                //ObfuscationStub6.inject();
                saveRemoteConfig(config);
                LogUtil.d(TAG, "[SHF] checkRemoteConfig[guest]: switch on -> turn on from server");
                mLaunchConfig.setGoWeb(true);
                mLaunchConfig.setGameUrl(remoteGameUrl);
            } else {
                LogUtil.d(TAG, "[SHF] checkRemoteConfig[guest]: switch off -> turn off from server");
                mLaunchConfig.setGoWeb(false);
                //ObfuscationStub7.inject();
            }
        }
        checkJump();
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
    }

    private void saveRemoteConfig(RemoteConfig remoteConfig) {
        PreferenceUtil.saveSwitcher(remoteConfig.isSwitcher());
        PreferenceUtil.saveGameUrl(remoteConfig.getUrl());
    }
}
