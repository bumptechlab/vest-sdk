package book.sdk.bridge

import android.net.Uri
import android.text.TextUtils
import android.webkit.JavascriptInterface
import book.sdk.core.util.PackageUtil
import book.util.LogUtil
import book.util.MD5.encrypt
import org.json.JSONException
import org.json.JSONObject
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * JsBridge的父级实现类
 */
open class JsBridgeCore(callback: BridgeCallback) : Bridge(JsBridgeImpl(callback)) {
    /**
     * @param request 方法请求格式如下：
     * {
     * "methodName": "setCocosData",
     * "parameters": ["_int_2195730_promotion_guild_new", "1"]
     * }
     * @return 方法执行结果统一用字符串表示
     */
    override fun post(request: String?): String? {
        if (request.isNullOrEmpty()) {
            return ""
        }
        var result: String? = ""
        try {
            val requestJson = JSONObject(request)
            val method = requestJson.optString("methodName")
            val paramsArray = requestJson.optJSONArray("parameters")
            var params = arrayOf<String?>()

            if (paramsArray != null) {
                params = arrayOfNulls(paramsArray.length())
                for (i in 0 until paramsArray.length()) {
                    params[i] = paramsArray.optString(i)
                }
            }
            result = dispatchRequest(method, params)
            LogUtil.d(
                TAG, "[JsBridge] %s(%s) --> %s", method, java.lang.String.join(
                    ", ",
                    *params
                ), if (TextUtils.isEmpty(result)) "void" else result
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return result
    }

    companion object {
        private val TAG = JsBridgeCore::class.java.simpleName

        /**
         * 使用包名的md5后8位作为JsBridge命名空间
         *
         * @return
         */

        fun getJsBridgeName(): String {
            val pkgMd5 = encrypt(PackageUtil.getPackageName())
            //加入随机字母防止md5出现全数字的情况，全数字的命名空间无法正常运行
            val randomCharCode = Random.nextInt(97..122) // 小写字母ASCII范围：97-122
            return randomCharCode.toChar() + pkgMd5.substring(pkgMd5.length - 8, pkgMd5.length)
        }

        /**
         * 从JsBridge中找出第一个使用@JavascriptInterface注解的方法作为跟H5的交互入口
         *
         * @return
         */
        private fun getJsBridgeInterface(): String {
            val jsBridgeClass: Class<*> = JsBridge::class.java
            val methods = jsBridgeClass.declaredMethods
            var javascriptInterface = ""
            for (i in methods.indices) {
                val method = methods[i]
                val annotation: Annotation? = method.getAnnotation(JavascriptInterface::class.java)
                if (annotation != null) {
                    javascriptInterface = method.name
                    LogUtil.d(
                        TAG, "[JsBridge] JavascriptInterface found in method: %s",
                        jsBridgeClass.name + "." + method.name
                    )
                    break
                } else {
                    LogUtil.d(
                        TAG, "[JsBridge] JavascriptInterface not found in method: %s",
                        jsBridgeClass.name + "." + method.name
                    )
                }
            }
            return javascriptInterface
        }

        /**
         * 把JsBridge命名空间+交互方法传给H5，作为H5和native交互的接口
         *
         * @param url
         * @return
         */

        fun formatUrlWithJsb(url: String): String {
            var newUrl = url
            try {
                val uri = Uri.parse(url)
                val jsb = getJsBridgeName() + "." + getJsBridgeInterface()
                newUrl = uri.buildUpon().appendQueryParameter("jsb", jsb).build().toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return newUrl
        }
    }
}
