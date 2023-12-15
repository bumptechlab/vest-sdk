package code.sdk.core;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.URLUtil;

import java.util.Set;

import code.util.LogUtil;


public class JumpCenter {
    public static final String TAG = JumpCenter.class.getSimpleName();
    private static String WEBVIEW_ACTIVITY_CLASS_NAME = "code.sdk.ui.WebActivity";

    public static void toWebViewActivity(Context context, String url) {
        try {
            if (!URLUtil.isValidUrl(url)) {
                LogUtil.e(TAG, "Activity[%s] launched aborted for invalid url: %s", WEBVIEW_ACTIVITY_CLASS_NAME, url);
                return;
            }
            Intent intent = new Intent();
            intent.setClassName(context, WEBVIEW_ACTIVITY_CLASS_NAME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("key_path_url_value", addRandomTimestamp(url));
            intent.putExtra("key_is_game_value", true);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogUtil.e(TAG, e, "Activity[%s] not found, please import 'vest-sdk' library", WEBVIEW_ACTIVITY_CLASS_NAME);
        } catch (Exception e) {
            LogUtil.e(TAG, e, "Activity[%s] launched error", WEBVIEW_ACTIVITY_CLASS_NAME);
        }
    }

    private static String addRandomTimestamp(String url) {
        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            //ObfuscationStub1.inject();
            LogUtil.e(TAG, "url parse error: %s", url);
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
