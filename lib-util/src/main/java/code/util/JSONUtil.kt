package code.util

import org.json.JSONObject

object JSONUtil {
    
    fun putJsonValue(jsonObject: JSONObject, key: String, value: Any) {
        try {
            jsonObject.put(key, value)
        } catch (_: Exception) {
        }
    }
}