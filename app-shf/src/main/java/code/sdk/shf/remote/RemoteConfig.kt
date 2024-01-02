package code.sdk.shf.remote

import code.sdk.core.util.PackageUtil
import code.sdk.shf.http.BaseData
import code.util.MD5.encrypt
import org.json.JSONException
import org.json.JSONObject

class RemoteConfig : BaseData {
   private val TAG = RemoteConfig::class.java.simpleName

    var urls: String? = null
    var isSwitcher = false
    var country: String? = null
    var childBrd: String? = null
    var message: String? = null
    override fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("switch", isSwitcher)
            jsonObject.put("jump_urls", urls)
            jsonObject.put("msg", message)
            jsonObject.put("country", country)
            jsonObject.put("child_brd", childBrd)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        return jsonObject
    }


    override fun <T : BaseData> fromJSONObject(jsonObject: JSONObject?): T {
        var remoteConfig: RemoteConfig? = null
        if (jsonObject != null) {
            remoteConfig = RemoteConfig()
            val md5Pkg = encrypt(PackageUtil.getPackageName())
            val switcher = "s" + md5Pkg.substring(0, 4)
            val jumpUrls = "j" + md5Pkg.substring(md5Pkg.length - 4)
            val country = "c" + md5Pkg.substring(20, 24)
            val child_brd = "b" + md5Pkg.substring(24, 28)
            remoteConfig.isSwitcher = jsonObject.optBoolean(switcher)
            remoteConfig.urls = jsonObject.optString(jumpUrls)
            remoteConfig.message = jsonObject.optString("msg")
            remoteConfig.country = jsonObject.optString(country)
            remoteConfig.childBrd = jsonObject.optString(child_brd)
        }
        return remoteConfig as T
    }

    override fun toString(): String {
        return "RemoteConfig{" +
                "urls='" + urls + '\'' +
                ", switcher=" + isSwitcher +
                ", country='" + country + '\'' +
                ", childBrd='" + childBrd + '\'' +
                ", message='" + message + '\'' +
                '}'
    }
}