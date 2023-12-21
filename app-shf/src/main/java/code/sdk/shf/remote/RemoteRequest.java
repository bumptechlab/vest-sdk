package code.sdk.shf.remote;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteRequest {
    public static final String TAG = RemoteRequest.class.getSimpleName();

    /**
     * Api版本
     * 1: 请求返回：switch + h5_url + country
     * 2: 请求返回：switch + h5_url + country + child_brand + jump_urls
     */
    private int version;
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

    /**
     * 是否加密返回的字段
     * rkey > 0时会替换响应中的Json key，替换规则如下：
     * 假设pkg参数为：com.superquicklodi.okms
     * MD5("com.superquicklodi.okms")="41b880285e306b7b5f273cf62272ba3f"，MD5使用全小写。
     * switch：取“41b880285e306b7b5f273cf62272ba3f”的前4个字节并在前面加“s”为”s41b8”替换掉响应中的switch
     * h5_url：取“41b880285e306b7b5f273cf62272ba3f”的最后4个字节并在前面加“u”为”uba3f”替换掉响应中的h5_url
     * jump_urls：取“41b880285e306b7b5f273cf62272ba3f”的最后4个字节并在前面加“j”为”jba3f”替换掉响应中的jump_urls
     * http_dns：取“41b880285e306b7b5f273cf62272ba3f”的5~8 4个字节并在前面加“t”为”t8028”替换掉响应中的http_dns
     * native_addr：取“41b880285e306b7b5f273cf62272ba3f”的9~12 4个字节并在前面加“n”为”n5e30”替换掉响应中的native_addr
     * hot_update_addr：取“41b880285e306b7b5f273cf62272ba3f”的13~16 4个字节并在前面加“h”为”h6b7b”替换掉响应中的hot_update_addr
     * pem_addr：取“41b880285e306b7b5f273cf62272ba3f”的17~20 4个字节并在前面加“p”为”p5f27”替换掉响应中的pem_addr
     * child_brd：取“41b880285e306b7b5f273cf62272ba3f”的25-28 4个字节并在前面加“b”为”b2272”替换掉响应中的child_brd
     * country：取“41b880285e306b7b5f273cf62272ba3f”的21~24 4个字节并在前面加“c”为”c3cf6”替换掉响应中的country，取值范围:
     */
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("version", getVersion());
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
