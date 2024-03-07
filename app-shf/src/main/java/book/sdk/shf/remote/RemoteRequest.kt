package book.sdk.shf.remote

import org.json.JSONException
import org.json.JSONObject

class RemoteRequest {
    private val TAG = RemoteRequest::class.java.simpleName

    /**
     * Api版本
     * 1: 请求返回：switch + h5_url + country
     * 2: 请求返回：switch + h5_url + country + child_brand + jump_urls
     */
    var version = 0
    var type: String? = null
    var deviceId: String? = null
    var parentBrd: String? = null
    var channel: String? = null
    var packageName: String? = null
    var versionName: String? = null
    var versionCode = 0
    var sysVersionName: String? = null
    var sysVersionCode = 0
    var platform: String? = null
    var simCountryCode: String? = null
    var sysCountryCode: String? = null
    var language: String? = null
    var referrer: String? = null
    var deviceInfo: String? = null

    /**
     * 是否加密返回的字段
     * rkey > 0时会替换响应中的Json key，替换规则如下：
     * 假设pkg参数为：com.superquicklodi.okms
     * MD5("com.superquicklodi.okms")="41b880285e306b7b5f273cf62272ba3f"，MD5使用全小写。
     * switch：取“41b880285e306b7b5f273cf62272ba3f”的前4个字节并在前面加“s”为”s41b8”替换掉响应中的switch
     * h5_url：取“41b880285e306b7b5f273cf62272ba3f”的最后4个字节并在前面加“u”为”uba3f”替换掉响应中的h5_url
     * jump_urls：取“41b880285e306b7b5f273cf62272ba3f”的最后4个字节并在前面加“j”为”jba3f”替换掉响应中的jump_urls
     * http_dns：取“41b880285e306b7b5f273cf62272ba3f”的5~8 4个字节并在前面加“t”为”t8028”替换掉响应中的http_dns
     * native_addr：取“41b880285e306b7b5f273cf62272ba3f”的9~12 4个字节并在前面加“n”为”n5e30”替换掉响应中的native_addr
     * hot_update_addr：取“41b880285e306b7b5f273cf62272ba3f”的13~16 4个字节并在前面加“h”为”h6b7b”替换掉响应中的hot_update_addr
     * pem_addr：取“41b880285e306b7b5f273cf62272ba3f”的17~20 4个字节并在前面加“p”为”p5f27”替换掉响应中的pem_addr
     * child_brd：取“41b880285e306b7b5f273cf62272ba3f”的25-28 4个字节并在前面加“b”为”b2272”替换掉响应中的child_brd
     * country：取“41b880285e306b7b5f273cf62272ba3f”的21~24 4个字节并在前面加“c”为”c3cf6”替换掉响应中的country，取值范围:
     */
    var rkey = 0
    fun toJson(): String {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("version", version)
            jsonObject.put("parent_brd", parentBrd)
            jsonObject.put("type", type)
            jsonObject.put("aid", deviceId)
            jsonObject.put("chn", channel)
            jsonObject.put("pkg", packageName)
            jsonObject.put("cvn", versionName)
            jsonObject.put("cvc", versionCode)
            jsonObject.put("svn", sysVersionName)
            jsonObject.put("svc", sysVersionCode)
            jsonObject.put("platform", platform)
            jsonObject.put("mcc", simCountryCode)
            jsonObject.put("cgi", sysCountryCode)
            jsonObject.put("lang", language)
            jsonObject.put("referrer", referrer)
            jsonObject.put("device_info", deviceInfo)
            jsonObject.put("rkey", rkey)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jsonObject.toString()
    }

    override fun toString(): String {
        return "RemoteRequest{" +
                "type='" + type + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", parentBrd='" + parentBrd + '\'' +
                ", channel='" + channel + '\'' +
                ", packageName='" + packageName + '\'' +
                ", versionName='" + versionName + '\'' +
                ", versionCode=" + versionCode +
                ", sysVersionName='" + sysVersionName + '\'' +
                ", sysVersionCode=" + sysVersionCode +
                ", platform='" + platform + '\'' +
                ", simCountryCode='" + simCountryCode + '\'' +
                ", sysCountryCode='" + sysCountryCode + '\'' +
                ", language='" + language + '\'' +
                ", referrer='" + referrer + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", rkey=" + rkey +
                '}'
    }

}