package code.sdk.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import code.util.LogUtil.e

object JumpCenter {
    private val TAG = JumpCenter::class.java.simpleName
    private const val WEBVIEW_ACTIVITY_CLASS_NAME = "code.sdk.ui.WebActivity"

    fun toWebViewActivity(context: Context, url: String) {
        try {
            if (!URLUtil.isValidUrl(url)) {
                e(TAG, "Activity[%s] launched aborted for invalid url: %s",
                    WEBVIEW_ACTIVITY_CLASS_NAME, url)
                return
            }
            val intent = Intent()
            intent.setClassName(context, WEBVIEW_ACTIVITY_CLASS_NAME)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("key_path_url_value", addRandomTimestamp(url))
            intent.putExtra("key_is_game_value", true)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e(TAG, e, "Activity[%s] not found, please import 'vest-sdk' library",
                WEBVIEW_ACTIVITY_CLASS_NAME)
        } catch (e: Exception) {
            e(TAG, e, "Activity[%s] launched error", WEBVIEW_ACTIVITY_CLASS_NAME)
        }
    }

    private fun addRandomTimestamp(url: String): String {
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            //ObfuscationStub1.inject();
            e(TAG, "url parse error: %s", url)
            return url
        }
        val ts = System.currentTimeMillis().toString()
        val queryParameterNames = uri.queryParameterNames
        return if (queryParameterNames.size > 0) {
            "$url&t=$ts"
        } else {
            "$url?t=$ts"
        }
    }
}
