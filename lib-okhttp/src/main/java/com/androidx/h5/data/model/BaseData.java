package com.androidx.h5.data.model;

import org.json.JSONObject;

public interface BaseData {

    public JSONObject toJSONObject();

    public <T extends BaseData> T fromJSONObject(JSONObject jsonObject);

}
