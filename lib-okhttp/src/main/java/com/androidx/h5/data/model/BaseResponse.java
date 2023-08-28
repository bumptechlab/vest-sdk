package com.androidx.h5.data.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 服务器返回的数据结构框架如下
 * { status: 0, msg: "ok", data: {} }
 * 该基础类了描述了这种数据结构，其中的泛型T代表数据data字段的内容
 */

public class BaseResponse<T extends BaseData> {

    /**
     * 请求成功
     */
    public static final int RESULT_CODE_SUCCESS = 0;
    private int status;

    private String msg;

    private T data;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public BaseResponse<T> fromJson(String json, T data) {
        BaseResponse<T> response = new BaseResponse<T>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            response.setStatus(jsonObject.optInt("status"));
            response.setMsg(jsonObject.optString("msg"));
            if (data != null) {
                data = data.fromJSONObject(jsonObject.optJSONObject("data"));
                response.setData(data);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("status", getStatus());
            jsonObject.put("msg", getMsg());
            jsonObject.put("data", data.toJSONObject());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject.toString();
    }

    public boolean isSuccess() {
        return getStatus() == RESULT_CODE_SUCCESS;
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "status=" + status +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
