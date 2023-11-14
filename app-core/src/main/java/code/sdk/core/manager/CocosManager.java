package code.sdk.core.manager;

import android.text.TextUtils;

import code.sdk.core.util.CocosPreferenceUtil;
import code.util.LogUtil;
import code.util.NumberUtil;

public class CocosManager {

    private static final String TAG = CocosManager.class.getSimpleName();

    public static String getUserId() {
        String userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_USER_ID);
        if (TextUtils.isEmpty(userID)) {
            userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_COMMON_USER_ID);
        }
        return userID;
    }

    public static int getCocosFrameVersion() {
        String cocosFrameVersionString = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_COCOS_FRAME_VERSION);
        int cocosFrameVersion = 0;
        LogUtil.d(TAG, "read CocosFrameVersion: " + cocosFrameVersionString);
        if (!TextUtils.isEmpty(cocosFrameVersionString)) {
            String cocosFrameVersionNumber = cocosFrameVersionString.replaceAll("[.]", "");
            LogUtil.d(TAG, "parse CocosFrameVersion: " + cocosFrameVersionNumber);
            cocosFrameVersion = NumberUtil.parseInt(cocosFrameVersionNumber);
        }
        return cocosFrameVersion;
    }

}
