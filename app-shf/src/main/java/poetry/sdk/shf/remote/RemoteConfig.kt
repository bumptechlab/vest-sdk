package poetry.sdk.shf.remote

import poetry.sdk.core.util.PackageUtil
import poetry.sdk.shf.http.BaseData
import poetry.util.MD5
import org.json.JSONException
import org.json.JSONObject

class RemoteConfig : BaseData {
    private val TAG = RemoteConfig::class.java.simpleName

    var urls: String? = null
    var isSwitcher = false
    var country: String? = null
    var childBrd: String? = null
    var message: String? = null
    var h5Type: String? = null //H5 跳转类型，1表示内部跳转，2表示外部跳转
    override fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("switch", isSwitcher)
            jsonObject.put("jump_urls", urls)
            jsonObject.put("msg", message)
            jsonObject.put("country", country)
            jsonObject.put("child_brd", childBrd)
            jsonObject.put("h5_type", h5Type)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        return jsonObject
    }


    override fun <T : BaseData> fromJSONObject(jsonObject: JSONObject?): T {
        var remoteConfig: RemoteConfig? = null
        if (jsonObject != null) {
            remoteConfig = RemoteConfig()
            if (RemoteManagerSHF.SHF_API_ENCRYPT) {
                val md5Pkg = MD5.encrypt(PackageUtil.getPackageName())
                remoteConfig.apply {
                    this.isSwitcher = jsonObject.optBoolean("s" + md5Pkg.substring(0, 4))
                    this.urls = jsonObject.optString("j" + md5Pkg.substring(md5Pkg.length - 4))
                    this.message = jsonObject.optString("msg")
                    this.country = jsonObject.optString("c" + md5Pkg.substring(20, 24))
                    this.childBrd = jsonObject.optString("b" + md5Pkg.substring(24, 28))
                    this.h5Type = jsonObject.optString("r" + md5Pkg.substring(md5Pkg.length - 4))
                }
            } else {
                remoteConfig.apply {
                    this.isSwitcher = jsonObject.optBoolean("switch")
                    this.urls = jsonObject.optString("jump_urls")
                    this.message = jsonObject.optString("msg")
                    this.country = jsonObject.optString("country")
                    this.childBrd = jsonObject.optString("child_brd")
                    this.h5Type = jsonObject.optString("h5_type")
                }
            }
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
                ", h5Type='" + h5Type + '\'' +
                '}'
    }
}