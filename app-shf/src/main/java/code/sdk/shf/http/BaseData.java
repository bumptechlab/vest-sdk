package code.sdk.shf.http;

import org.json.JSONObject;

public interface BaseData {

    public JSONObject toJSONObject();

    public <T extends BaseData> T fromJSONObject(JSONObject jsonObject);

}
