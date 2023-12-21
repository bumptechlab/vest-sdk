package code.sdk.shf.remote;

import org.json.JSONException;
import org.json.JSONObject;

import code.sdk.core.util.PackageUtil;
import code.sdk.shf.http.BaseData;
import code.util.MD5;

public class RemoteConfig implements BaseData {
    public static final String TAG = RemoteConfig.class.getSimpleName();

    private String urls;

    private boolean switcher;

    private String country;

    private String childBrd;

    private String message;

    public RemoteConfig() {
    }

    public String getChildBrd() {
        return childBrd;
    }

    public void setChildBrd(String childBrd) {
        this.childBrd = childBrd;
    }

    public void setSwitcher(boolean swi) {
        this.switcher = swi;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public boolean isSwitcher() {
        return switcher;
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
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("switch", isSwitcher());
            jsonObject.put("jump_urls", getUrls());
            jsonObject.put("msg", getMessage());
            jsonObject.put("country", getCountry());
            jsonObject.put("child_brd", getChildBrd());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }


    public RemoteConfig fromJSONObject(JSONObject jsonObject) {
        RemoteConfig remoteConfig = null;
        if (jsonObject != null) {
            remoteConfig = new RemoteConfig();
            String md5Pkg = MD5.encrypt(PackageUtil.getPackageName());
            String switcher = "s" + md5Pkg.substring(0, 4);
            String jump_urls = "j" + md5Pkg.substring(md5Pkg.length() - 4);
            String country = "c" + md5Pkg.substring(20, 24);
            String child_brd = "b" + md5Pkg.substring(24, 28);
            remoteConfig.setSwitcher(jsonObject.optBoolean(switcher));
            remoteConfig.setUrls(jsonObject.optString(jump_urls));
            remoteConfig.setMessage(jsonObject.optString("msg"));
            remoteConfig.setCountry(jsonObject.optString(country));
            remoteConfig.setChildBrd(jsonObject.optString(child_brd));
        }
        return remoteConfig;
    }

    @Override
    public String toString() {
        return "RemoteConfig{" +
                "urls='" + urls + '\'' +
                ", switcher=" + switcher +
                ", country='" + country + '\'' +
                ", childBrd='" + childBrd + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
