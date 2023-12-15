package code.sdk.shf.remote;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteRequest {
    public static final String TAG = RemoteRequest.class.getSimpleName();

    private String type;

    private String deviceId;

    private String parentBrd;

    private String channel;

    private String packageName;

    private String versionName;

    private int versionCode;

    private String sysVersionName;

    private int sysVersionCode;

    private String platform;

    private String simCountryCode;

    private String sysCountryCode;

    private String language;

    private String referrer;

    private String deviceInfo;

    //是否替换响应body中的switch/h5_url这两个参数
    //大于0为需要替换，小于等于0为不需要替换，默认为0.
    private int rkey = 0;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParentBrd() {
        return parentBrd;
    }

    public void setParentBrd(String parentBrd) {
        this.parentBrd = parentBrd;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSimCountryCode() {
        return simCountryCode;
    }

    public void setSimCountryCode(String simCountryCode) {
        this.simCountryCode = simCountryCode;
    }

    public String getSysCountryCode() {
        return sysCountryCode;
    }

    public void setSysCountryCode(String sysCountryCode) {
        this.sysCountryCode = sysCountryCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSysVersionName() {
        return sysVersionName;
    }

    public void setSysVersionName(String sysVersionName) {
        this.sysVersionName = sysVersionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public int getSysVersionCode() {
        return sysVersionCode;
    }

    public void setSysVersionCode(int sysVersionCode) {
        this.sysVersionCode = sysVersionCode;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public int getRkey() {
        return rkey;
    }

    public void setRkey(int rkey) {
        this.rkey = rkey;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("version", 2);
            jsonObject.put("parent_brd", getParentBrd());
            jsonObject.put("type", getType());
            jsonObject.put("aid", getDeviceId());
            jsonObject.put("chn", getChannel());
            jsonObject.put("pkg", getPackageName());
            jsonObject.put("cvn", getVersionName());
            jsonObject.put("cvc", getVersionCode());
            jsonObject.put("svn", getSysVersionName());
            jsonObject.put("svc", getSysVersionCode());
            jsonObject.put("platform", getPlatform());
            jsonObject.put("mcc", getSimCountryCode());
            jsonObject.put("cgi", getSysCountryCode());
            jsonObject.put("lang", getLanguage());
            jsonObject.put("referrer", getReferrer());
            jsonObject.put("device_info", getDeviceInfo());
            jsonObject.put("rkey", getRkey());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    public String toString() {
        return "RemoteRequest{" +
                "type='" + type + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", parentBrd='" + parentBrd + '\'' +
                ", channel='" + channel + '\'' +
                ", packageName='" + packageName + '\'' +
                ", versionName='" + versionName + '\'' +
                ", versionCode=" + versionCode +
                ", sysVersionName='" + sysVersionName + '\'' +
                ", sysVersionCode=" + sysVersionCode +
                ", platform='" + platform + '\'' +
                ", simCountryCode='" + simCountryCode + '\'' +
                ", sysCountryCode='" + sysCountryCode + '\'' +
                ", language='" + language + '\'' +
                ", referrer='" + referrer + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", rkey=" + rkey +
                '}';
    }
}
