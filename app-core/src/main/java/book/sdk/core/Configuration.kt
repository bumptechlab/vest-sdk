package book.sdk.core

import org.json.JSONException
import org.json.JSONObject

class Configuration {
    var channel: String? = ""
    var brand: String? = null
    var country: String? = null
    var shfSpareHosts: Array<String>? = null
    var shfBaseHost: String? = null
    var shfDispatcher: String? = null

    /* adjust start */
    var adjustAppId: String? = null
    var adjustEventStart: String? = null
    var adjustEventGreeting: String? = null
    var adjustEventAccess: String? = null
    var adjustEventUpdated: String? = null

}

fun String.toConfiguration(): Configuration? {
    var configuration: Configuration? = null
    try {
        val jsonObject = JSONObject(this)
        configuration = Configuration()
        configuration.apply {
            channel = jsonObject.optString("channel")
            brand = jsonObject.optString("brand")
            country = jsonObject.optString("country")
            shfBaseHost = jsonObject.optString("shf_base_domain")
            val shfSpareHostArray = jsonObject.optJSONArray("shf_spare_domains")
            val shfSpareHostList = mutableListOf<String>()
            if (shfSpareHostArray != null) {
                for (i in 0 until shfSpareHostArray.length()) {
                    shfSpareHostList.add(shfSpareHostArray.optString(i))
                }
            }
            shfDispatcher = jsonObject.optString("shf_dispatcher")
            shfSpareHosts = shfSpareHostList.toTypedArray()
            adjustAppId = jsonObject.optString("adjust_app_id")
            adjustEventStart = jsonObject.optString("adjust_event_start")
            adjustEventGreeting = jsonObject.optString("adjust_event_greeting")
            adjustEventAccess = jsonObject.optString("adjust_event_access")
            adjustEventUpdated = jsonObject.optString("adjust_event_updated")
        }
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    return configuration
}
