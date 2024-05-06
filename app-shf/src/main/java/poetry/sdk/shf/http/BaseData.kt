package poetry.sdk.shf.http

import org.json.JSONObject

interface BaseData {
    fun toJSONObject(): JSONObject
    fun <T : BaseData> fromJSONObject(jsonObject: JSONObject?): T
}