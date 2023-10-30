package code.sdk.shf.remote;

import com.androidx.h5.data.model.BaseData;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteConfig implements BaseData {
    public static final String TAG = RemoteConfig.class.getSimpleName();

    private String url;

    private boolean swi;

    private String country;

    private String message;

    public RemoteConfig() {
    }

    public RemoteConfig(boolean swi, String url, String country) {
        this.swi = swi;
        this.url = url;
        this.country = country;
    }

    public boolean isSwi() {
        return swi;
    }

    public String getGameUrl() {
        return url;
    }

    public String getCountry() {
        return this.country;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    public String toString() {
        return "RemoteConfig{" +
                "url='" + url + '\'' +
                ", swi=" + swi +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("h5_url", getGameUrl());
            jsonObject.put("switch", isSwi());
            jsonObject.put("msg", getMessage());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }

    public RemoteConfig fromJSONObject(JSONObject jsonObject) {
        RemoteConfig remoteConfig = null;
        if (jsonObject != null) {
            boolean switcher = jsonObject.optBoolean("switch");
            String url = jsonObject.optString("h5_url");
            String message = jsonObject.optString("msg");
            String country = jsonObject.optString("country");
            remoteConfig = new RemoteConfig(switcher, url, country);
            remoteConfig.setMessage(message);
        }
        return remoteConfig;
    }

}
