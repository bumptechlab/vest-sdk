package code.sdk.shf.remote;

import com.androidx.h5.data.model.BaseData;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteConfig implements BaseData {
    public static final String TAG = RemoteConfig.class.getSimpleName();

    private String url;

    private boolean swi;

    private boolean httpDns;

    private String country;

    private String message;

    public RemoteConfig() {
    }

    public RemoteConfig(boolean swi, String url, boolean httpDns, String country) {
        this.swi = swi;
        this.url = url;
        this.httpDns = httpDns;
        this.country = country;
    }

    public boolean isSwi() {
        return swi;
    }

    public String getGameUrl() {
        return url;
    }

    public boolean isHttpDns() {
        return this.httpDns;
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
                ", httpDns=" + httpDns +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("h5_url", getGameUrl());
            jsonObject.put("switch", isSwi());
            jsonObject.put("http_dns", isHttpDns());
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
            boolean httpDns = jsonObject.optBoolean("http_dns");
            String message = jsonObject.optString("msg");
            String country = jsonObject.optString("country");
            remoteConfig = new RemoteConfig(switcher, url, httpDns, country);
            remoteConfig.setMessage(message);
        }
        return remoteConfig;
    }

}