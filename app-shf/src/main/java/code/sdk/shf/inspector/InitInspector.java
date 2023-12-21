package code.sdk.shf.inspector;

import android.text.TextUtils;

import code.sdk.core.manager.InstallReferrerManager;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.GoogleAdIdInitializer;
import code.sdk.core.util.PreferenceUtil;
import code.util.LogUtil;

/**
 * check install referrer & googleAdId
 */
public class InitInspector {
    public static final String TAG = InitInspector.class.getSimpleName();
    private static final long TIMEOUT = 3_000;

    public boolean inspect() {
        String installReferrer = InstallReferrerManager.getInstallReferrer();
        long startTime = System.currentTimeMillis();
        while (TextUtils.isEmpty(installReferrer) || GoogleAdIdInitializer.needUpdateGoogleAdId()) {
            installReferrer = InstallReferrerManager.getInstallReferrer();
            GoogleAdIdInitializer.init();
            if (System.currentTimeMillis() - startTime > TIMEOUT) {
                LogUtil.d(TAG, "[InitInspector] inspect timeout!");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ObfuscationStub7.inject();
            }
        }
        if (GoogleAdIdInitializer.needUpdateGoogleAdId()) {
            PreferenceUtil.saveGoogleADID(DeviceUtil.getDeviceID());
        }
        if (TextUtils.isEmpty(installReferrer)) {
            LogUtil.d(TAG, "[InitInspector] install referrer empty!");
            return false;
        }
        return true;
    }
}
