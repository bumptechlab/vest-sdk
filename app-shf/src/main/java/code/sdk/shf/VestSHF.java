package code.sdk.shf;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.URLUtil;

import code.sdk.core.VestCore;
import code.sdk.core.VestInspectCallback;
import code.sdk.core.manager.AdjustManager;
import code.sdk.core.manager.InstallReferrerManager;
import code.sdk.core.util.GoogleAdIdInitializer;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.TestUtil;
import code.sdk.core.util.UIUtil;
import code.sdk.shf.inspector.AbstractChainedInspector;
import code.sdk.shf.inspector.InitInspector;
import code.sdk.shf.remote.RemoteConfig;
import code.sdk.shf.remote.RemoteSourceSHF;
import code.util.AppGlobal;
import code.util.LogUtil;

public class VestSHF {

    private static final String TAG = VestSHF.class.getSimpleName();
    private LaunchConfig mLaunchConfig = new LaunchConfig();

    private static VestSHF sInstance;

    public static VestSHF getInstance() {
        if (sInstance == null) {
            sInstance = new VestSHF();
        }
        return sInstance;
    }

    private VestInspectCallback mVestInspectCallback = null;

    public void inspect(Context context, VestInspectCallback vestInspectCallback) {
        boolean isHandled = TestUtil.handleIntent(context);
        TestUtil.printDebugInfo();
        LogUtil.setDebug(TestUtil.isLoggable());
        mVestInspectCallback = vestInspectCallback;
        mLaunchConfig.setStartMills(System.currentTimeMillis());

        String installReferrer = InstallReferrerManager.getInstallReferrer();
        if (TextUtils.isEmpty(installReferrer) || InstallReferrerManager.INSTALL_REFERRER_UNKNOWN.equals(installReferrer)) {
            InstallReferrerManager.initInstallReferrer();
        }
        GoogleAdIdInitializer.init();
        if (isHandled) {
            LogUtil.d(TAG, "Open WebView using intent, SHF request aborted!");
            return;
        }
        new Thread(() -> {
            AbstractChainedInspector inspector = AbstractChainedInspector.makeChain(
                    new InitInspector()
            );
            boolean inspected = inspector.inspect();
            LogUtil.d(TAG, "inspected = " + inspected);
            onInspectResult(inspected);
        }).start();
    }

    private void onInspectResult(boolean inspected) {
        if (inspected) {
            //ObfuscationStub3.inject();
            fetchRemoteConfig();
        } else {
            //ObfuscationStub4.inject();
            mLaunchConfig.setConfigLoaded(true);
            checkRemoteConfig(null);
        }
    }

    private void fetchRemoteConfig() {
        RemoteSourceSHF remoteSource = new RemoteSourceSHF(AppGlobal.getApplication());
        remoteSource.setCallback((success, remoteConfig) -> {
            mLaunchConfig.setConfigLoaded(true);
            if (success && remoteConfig != null) {
                PreferenceUtil.saveHttpDnsEnable(remoteConfig.isHttpDns());
                PreferenceUtil.saveTargetCountry(remoteConfig.getCountry());
            } else {
                PreferenceUtil.saveHttpDnsEnable(false);
            }
            VestCore.initThirdSDK();
            if (success && remoteConfig != null) {
                AdjustManager.trackEventGreeting(null);
            }
            checkRemoteConfig(remoteConfig);
            LogUtil.d(TAG, "[HttpDns] isHttpDnsEnable: " + PreferenceUtil.readHttpDnsEnable());
        });
        remoteSource.fetch();
    }

    /**
     * Firebase & SHF use the same jump Logic
     *
     * @param config
     */
    private void checkRemoteConfig(RemoteConfig config) {
        boolean savedSwitcher = PreferenceUtil.readSwi();
        String savedGameUrl = PreferenceUtil.readGameUrl();
        LogUtil.d(TAG, "checkRemoteConfig: %s, savedSwitcher: %s, savedGameUrl: %s",
                config, savedSwitcher, savedGameUrl);
        if (!savedSwitcher) {
            // 新用户
            if (config == null) {
                LogUtil.d(TAG, "checkRemoteConfig[guest]: switch off -> config is empty");
                mLaunchConfig.setGoWeb(false);
            } else if (config.isSwi() && URLUtil.isValidUrl(config.getGameUrl())) {
                //ObfuscationStub6.inject();
                saveRemoteConfig(config);
                LogUtil.d(TAG, "checkRemoteConfig[guest]: switch on -> turn on from server");
                mLaunchConfig.setGoWeb(true);
                mLaunchConfig.setGameUrl(config.getGameUrl());
            } else {
                LogUtil.d(TAG, "checkRemoteConfig[guest]: switch off -> turn off from server");
                mLaunchConfig.setGoWeb(false);
                //ObfuscationStub7.inject();
            }
        } else {
            //如果是老用户,且服务器有新链接,以服务器的新链接为准,如果关闭马甲,后台不会返回gameUrl
            if (config != null && URLUtil.isValidUrl(config.getGameUrl())) {
                PreferenceUtil.saveGameUrl(config.getGameUrl());
                LogUtil.d(TAG, "checkRemoteConfig[master]: switch on -> update url: %s", config.getGameUrl());
                mLaunchConfig.setGoWeb(true);
                mLaunchConfig.setGameUrl(config.getGameUrl());
            }//如果没有返回新链接,则以老链接为主.
            else if (URLUtil.isValidUrl(savedGameUrl)) {
                //ObfuscationStub8.inject();
                LogUtil.d(TAG, "checkRemoteConfig[master]: switch on -> read cached url: %s", savedGameUrl);
                mLaunchConfig.setGoWeb(true);
                mLaunchConfig.setGameUrl(savedGameUrl);
                // validate game url (could be set by cocos)
                new Thread(new GameUrlValidatorRunnable(savedGameUrl)).start();
            }//如果没有老链接,就进入马甲游戏
            else {
                LogUtil.d(TAG, "checkRemoteConfig[master]: switch off -> no cached url");
                mLaunchConfig.setGoWeb(false);
            }
        }
        checkJump();
    }

    public void checkJump() {
        if (mLaunchConfig.isConfigLoaded()) {
            long delayMills = System.currentTimeMillis() - mLaunchConfig.getStartMills();
            LogUtil.d(TAG, "Jump to activity after delay %d mills", delayMills);
            if (delayMills >= mLaunchConfig.getLAUNCH_OVERTIME()) {
                doJump();
            } else {
                UIUtil.runOnUiThreadDelay(() -> {
                    doJump();
                }, mLaunchConfig.getLAUNCH_OVERTIME() - delayMills);
            }
        } else {
            LogUtil.d(TAG, "Jump to activity aborted");
        }
    }

    private void doJump() {
        LogUtil.d(TAG, "LaunchConfig: isGogoWeb=%s, gameUrl=%s", mLaunchConfig.isGoWeb(), mLaunchConfig.getGameUrl());
        if (mLaunchConfig.isGoWeb()) {
            if (mVestInspectCallback != null) {
                mVestInspectCallback.onShowOfficialGame(mLaunchConfig.getGameUrl());
            }
        } else {
            if (mVestInspectCallback != null) {
                mVestInspectCallback.onShowVestGame();
            }
        }
    }

    private void saveRemoteConfig(RemoteConfig remoteConfig) {
        PreferenceUtil.saveSwi(remoteConfig.isSwi());
        PreferenceUtil.saveGameUrl(remoteConfig.getGameUrl());
    }
}
