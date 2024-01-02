package code.sdk.core.util

import android.text.TextUtils
import java.net.URL

object URLUtilX {
   private val TAG = URLUtilX::class.java.simpleName

    /**
     * 去除URL参数
     *
     * @param url
     * @return
     */
    fun getBaseUrl(url: String): String {
        if (TextUtils.isEmpty(url)) {
            //ObfuscationStub7.inject();
            return url
        }
        var baseUrl = url
        val markIndex = url.indexOf("?")
        if (markIndex > 0) {
            baseUrl = url.substring(0, markIndex)
        }
        return baseUrl
    }

    fun parseHost(url: String?): String {
        if (url.isNullOrEmpty()) {
            return ""
        }
        var host: String
        try {
            host = URL(url).host
        } catch (e: Exception) {
            val startIndex = url.indexOf("://")
            var urlTemp = url
            if (startIndex != -1) {
                urlTemp = url.substring(startIndex + 3)
            }
            var endIndex = urlTemp.indexOf(":")
            if (endIndex == -1) {
                endIndex = urlTemp.indexOf("/")
            }
            if (endIndex == -1) {
                endIndex = urlTemp.length
            }
            host = urlTemp.substring(0, endIndex)
        }
        return host
    }
}
