package poetry.sdk.shf.http

import org.json.JSONException
import org.json.JSONObject

/**
 * 服务器返回的数据结构框架如下
 * { status: 0, msg: "ok", data: {} }
 * 该基础类了描述了这种数据结构，其中的泛型T代表数据data字段的内容
 */
class BaseResponse<T : BaseData> {
    var status = 0
    var msg: String? = null
    var data: T? = null
        private set

    fun setData(data: T) {
        this.data = data
    }

    fun fromJson(json: String, data: T?): BaseResponse<T> {
        var data: T? = data
        val response = BaseResponse<T>()
        try {
            val jsonObject = JSONObject(json)
            response.status = jsonObject.optInt("status")
            response.msg = jsonObject.optString("msg")
            if (data != null) {
                data = data.fromJSONObject(jsonObject.optJSONObject("data"))
                response.setData(data)
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        return response
    }

    fun toJson(): String {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("status", status)
            jsonObject.put("msg", msg)
            jsonObject.put("data", data!!.toJSONObject())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        return jsonObject.toString()
    }

    fun isSuccess() = status == RESULT_CODE_SUCCESS

    override fun toString(): String {
        return "BaseResponse{" +
                "status=" + status +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}'
    }

    companion object {
        /**
         * 请求成功
         */
        const val RESULT_CODE_SUCCESS = 0
    }
}