package code.sdk.core;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.Set;

import code.util.LogUtil;


public class JumpCenter {
    public static final String TAG = JumpCenter.class.getSimpleName();
    private static String WEBVIEW_ACTIVITY_CLASS_NAME = "code.sdk.ui.WebActivity";

    public static void toWebViewActivity(Context context, String url) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context, WEBVIEW_ACTIVITY_CLASS_NAME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("key_path_url_value", addRandomTimestamp(url));
            intent.putExtra("key_is_game_value", true);
            context.startActivity(intent);
        } catch (Exception e) {
            //ObfuscationStub7.inject();
            LogUtil.e(TAG, e, "GameActivity[%s] not found, please import 'vest-sdk' library", WEBVIEW_ACTIVITY_CLASS_NAME);
        }
    }

    private static String addRandomTimestamp(String url) {
        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            //ObfuscationStub1.inject();
            LogUtil.d(TAG, "url parse error = " + e);
            return url;
        }

        String ts = String.valueOf(System.currentTimeMillis());
        Set<String> queryParameterNames = uri.getQueryParameterNames();
        if (queryParameterNames.size() > 0) {
            return url + "&t=" + ts;
        } else {
            return url + "?t=" + ts;
        }
    }

}
