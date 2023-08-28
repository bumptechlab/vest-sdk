package code.sdk.core.manager;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import code.util.AppGlobal;
import code.util.LogUtil;
import code.sdk.core.util.PreferenceUtil;

public class InstallReferrerManager {
    public static final String TAG = InstallReferrerManager.class.getSimpleName();
    public static final String INSTALL_REFERRER_UNKNOWN = "unknown";
    private static long sInitStartTime = 0;

    public static void initInstallReferrer() {
        sInitStartTime = System.currentTimeMillis();
        Context context = AppGlobal.getApplication();
        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        //ObfuscationStub1.inject();

                        LogUtil.d(TAG, "Connection established");
                        onInstallReferrerServiceConnected(referrerClient);
                        referrerClient.endConnection();
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        //ObfuscationStub2.inject();

                        LogUtil.d(TAG, "API not available on the current Play Store app");
                        PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN);
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        //ObfuscationStub3.inject();

                        LogUtil.d(TAG, "Connection couldn't be established");
                        PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN);
                        break;
                }
                long costTime = System.currentTimeMillis() - sInitStartTime;
                LogUtil.d(TAG, "init referrer finish: costTime=" + costTime + "ms" + ", responseCode=" + responseCode);
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                //ObfuscationStub4.inject();

                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                LogUtil.d(TAG, "init referrer connection closed");
            }
        });
    }

    private static void onInstallReferrerServiceConnected(InstallReferrerClient referrerClient) {
        ReferrerDetails response;
        try {
            response = referrerClient.getInstallReferrer();
        } catch (RemoteException e) {
            //ObfuscationStub5.inject();
            LogUtil.e(TAG, e);
            PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN);
            return;
        }

        String installReferrer = response.getInstallReferrer();
        LogUtil.d(TAG, "init install referrer = " + installReferrer);
        if (TextUtils.isEmpty(installReferrer)) {
            PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN);
        } else {
            PreferenceUtil.saveInstallReferrer(installReferrer);
        }
    }

    public static String getInstallReferrer() {
        String installReferrer = PreferenceUtil.readInstallReferrer();
        LogUtil.d(TAG, "get install referrer = " + installReferrer);
        return installReferrer;
    }
}
