package code.sdk.core.util;

import android.text.TextUtils;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.OnDeviceIdsRead;

import code.util.AppGlobal;
import code.util.LogUtil;

public class GoogleAdIdInitializer {

    private static final String TAG = GoogleAdIdInitializer.class.getSimpleName();

    private static boolean isWaitingGoogleAdId = false;

    public static boolean needUpdateGoogleAdId() {
        String googleAdId = PreferenceUtil.readGoogleADID();
        String preDeviceId = DeviceUtil.preGetDeviceID().first;
        return TextUtils.isEmpty(preDeviceId) && TextUtils.isEmpty(googleAdId);
    }

    public static void init() {
        if (needUpdateGoogleAdId() && !isWaitingGoogleAdId()) {
            LogUtil.d(TAG, "need update GoogleAdId");
            isWaitingGoogleAdId = true;
            Adjust.getGoogleAdId(AppGlobal.getApplication(), new OnDeviceIdsRead() {
                @Override
                public void onGoogleAdIdRead(String s) {
                    LogUtil.d(TAG, "onGoogleAdIdRead: %s", s);
                    isWaitingGoogleAdId = false;
                    PreferenceUtil.saveGoogleADID(s);
                }
            });
        }
    }

    public static boolean isWaitingGoogleAdId() {
        return isWaitingGoogleAdId;
    }
}
