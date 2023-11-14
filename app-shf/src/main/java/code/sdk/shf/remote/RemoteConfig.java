package code.sdk.shf.remote;

import org.json.JSONException;
import org.json.JSONObject;

import code.sdk.shf.http.BaseData;

public class RemoteConfig implements BaseData {
    public static final String TAG = RemoteConfig.class.getSimpleName();

    private String url;

    private boolean switcher;

    private String country;

    private String message;

    public RemoteConfig() {
    }

    public RemoteConfig(boolean swi, String url, String country) {
        this.switcher = swi;
        this.url = url;
        this.country = country;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSwitcher(boolean swi) {
        this.switcher = swi;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isSwitcher() {
        return switcher;
    }

    public String getUrl() {
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
                ", switcher=" + switcher +
                ", country='" + country + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("switch", isSwitcher());
            jsonObject.put("h5_url", getUrl());
            jsonObject.put("msg", getMessage());
            jsonObject.put("country", getCountry());
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
