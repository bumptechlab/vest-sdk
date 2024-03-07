package book.sdk.command

import android.net.Uri
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import book.util.AssetsUtil
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class AssetLoaderManager private constructor() {
    private val COCOS_H5_ENGINE_NAME = "cocos2d-js-min"
    fun shouldInterceptRequest(uri: Uri): WebResourceResponse? {
        val path = uri.path
        return if (!path.isNullOrEmpty() && path.contains(COCOS_H5_ENGINE_NAME)) {
            try {
                val jsString = AssetsUtil.getAssetsFlagData(AssetsUtil.JS_FLAG)
                val inputStream: InputStream =
                    ByteArrayInputStream(jsString!!.toByteArray(StandardCharsets.UTF_8))
                WebResourceResponse(
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(path),
                    "utf-8", inputStream
                )
            } catch (e: Exception) {
                null
            }
        } else null
    }

    companion object {
        val mInstance by lazy { AssetLoaderManager() }
    }
}
