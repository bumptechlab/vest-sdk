package code.sdk.core.manager;

import android.text.TextUtils;

import code.sdk.core.VestCore;
import code.sdk.core.util.CocosPreferenceUtil;
import code.util.LogUtil;
import code.util.NumberUtil;

public class CocosManager {

    private static final String TAG = CocosManager.class.getSimpleName();
    private static final String[] FILTER_COUNTRY_CODES = new String[]{"IN", "ID", "BR", "GW", "VN"};

    public static String getUserId() {
        String userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_USER_ID);
        if (TextUtils.isEmpty(userID)) {
            userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_COMMON_USER_ID);
        }
        return userID;
    }

    public static int getCocosFrameVersionInt() {
        String cocosFrameVersion = getCocosFrameVersion();
        int cocosFrameVersionInt = 0;
        if (!TextUtils.isEmpty(cocosFrameVersion)) {
            String cocosFrameVersionNumber = cocosFrameVersion.replaceAll("[.]", "");
            LogUtil.d(TAG, "parse CocosFrameVersion: " + cocosFrameVersionNumber);
            cocosFrameVersionInt = NumberUtil.parseInt(cocosFrameVersionNumber);
        }
        return cocosFrameVersionInt;
    }

    public static String getCocosFrameVersion() {
        String cocosFrameVersion = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_COCOS_FRAME_VERSION);
        if (TextUtils.isEmpty(cocosFrameVersion)) {
            String targetCountry = VestCore.getTargetCountry();
            if (is1d0CountryCode(targetCountry)) {
                cocosFrameVersion = "1.0.0";
            } else {
                cocosFrameVersion = "2.0.0";
            }
        }
        LogUtil.d(TAG, "read CocosFrameVersion: " + cocosFrameVersion);
        return cocosFrameVersion;
    }

    private static boolean is1d0CountryCode(String countryCode) {
        boolean is1d0CountryCode = false;
        for (int i = 0; i < FILTER_COUNTRY_CODES.length; i++) {
            if (FILTER_COUNTRY_CODES[i].equalsIgnoreCase(countryCode)) {
                is1d0CountryCode = true;
                break;
            }
        }
        return is1d0CountryCode;
    }

}
