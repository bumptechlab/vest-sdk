package code.sdk.core

import org.json.JSONException
import org.json.JSONObject

class Configuration {
    var channel: String? = ""
    var brand: String? = null
    var country: String? = null
    var shfSpareHosts: Array<String?>? =null
    var shfBaseHost: String? = null
    var shfDispatcher: String? = null

    /* adjust start */
    var adjustAppId: String? = null
    var adjustEventStart: String? = null
    var adjustEventGreeting: String? = null
    var adjustEventAccess: String? = null
    var adjustEventUpdated: String? = null

    /* thinking data start */
    var thinkingDataAppId: String? = ""
    var thinkingDataHost : String?= ""

    companion object {
        fun fromJson(json: String?): Configuration? {
            var configuration: Configuration? = null
            try {
                val jsonObject = JSONObject(json)
                configuration = Configuration()
                configuration.channel = jsonObject.optString("channel")
                configuration.brand = jsonObject.optString("brand")
                configuration.country = jsonObject.optString("country")
                configuration.shfBaseHost = jsonObject.optString("shf_base_domain")
                val shfSpareHosts = jsonObject.optJSONArray("shf_spare_domains")
                val shfSpareHostList = ArrayList<String>()
                if (shfSpareHosts != null) {
                    for (i in 0 until shfSpareHosts.length()) {
                        shfSpareHostList.add(shfSpareHosts.optString(i))
                    }
                }
                configuration.shfDispatcher = jsonObject.optString("shf_dispatcher")
                configuration.shfSpareHosts = shfSpareHostList.toArray(arrayOf<String>())
                configuration.adjustAppId = jsonObject.optString("adjust_app_id")
                configuration.adjustEventStart = jsonObject.optString("adjust_event_start")
                configuration.adjustEventGreeting = jsonObject.optString("adjust_event_greeting")
                configuration.adjustEventAccess = jsonObject.optString("adjust_event_access")
                configuration.adjustEventUpdated = jsonObject.optString("adjust_event_updated")
                configuration.thinkingDataAppId = jsonObject.optString("thinking_data_app_id")
                configuration.thinkingDataHost = jsonObject.optString("thinking_data_host")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return configuration
        }
    }
}
