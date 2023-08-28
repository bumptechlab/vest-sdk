package code.sdk.analysis.remote;

import org.json.JSONException;
import org.json.JSONObject;

//审核数据上报请求实体类
public class AnalysisRequest {

    public static final String TAG = AnalysisRequest.class.getSimpleName();

    /**
     * 设备id
     */
    private String deviceId;

    /**
     * 品牌
     */
    private String brandCode;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 包名
     */
    private String packageName;


    /**
     * 系统类型 android/ios
     */
    private String platform;

    /**
     * sim卡信息
     */
    private String simCountryCode;

    /**
     * 系统国家地区
     */
    private String sysCountryCode;

    /**
     * 系统语言
     */
    private String language;

    /**
     * 来源
     */
    private String referrer;

    /**
     * 游戏ID
     */
    private String gameId;

    /**
     * 最后一次登录时间
     */
    private long lastLoginTime;

    /**
     * 用户注册时间
     */
    private long createTime;

    /**
     * 游戏时间
     */
    private long gameDuration;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode;
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

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(long gameDuration) {
        this.gameDuration = gameDuration;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("aid", getDeviceId());
            jsonObject.put("brd", getBrandCode());
            jsonObject.put("chn", getChannel());
            jsonObject.put("pkg", getPackageName());
            jsonObject.put("platform", getPlatform());
            jsonObject.put("mcc", getSimCountryCode());
            jsonObject.put("cgi", getSysCountryCode());
            jsonObject.put("lang", getLanguage());
            jsonObject.put("referrer", getReferrer());
            jsonObject.put("gid", getGameId());
            jsonObject.put("llt", getLastLoginTime());
            jsonObject.put("ct", getCreateTime());
            jsonObject.put("gdt", getGameDuration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }


    public static AnalysisRequest fromJson(String json){
        AnalysisRequest analysisRequest = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            analysisRequest = new AnalysisRequest();
            analysisRequest.setDeviceId(jsonObject.optString("aid"));
            analysisRequest.setBrandCode(jsonObject.optString("brd"));
            analysisRequest.setChannel(jsonObject.optString("chn"));
            analysisRequest.setPackageName(jsonObject.optString("pkg"));
            analysisRequest.setPlatform(jsonObject.optString("platform"));
            analysisRequest.setSimCountryCode(jsonObject.optString("mcc"));
            analysisRequest.setSysCountryCode(jsonObject.optString("cgi"));
            analysisRequest.setLanguage(jsonObject.optString("lang"));
            analysisRequest.setReferrer(jsonObject.optString("referrer"));
            analysisRequest.setGameId(jsonObject.optString("gid"));
            analysisRequest.setLastLoginTime(jsonObject.optLong("llt"));
            analysisRequest.setCreateTime(jsonObject.optLong("ct"));
            analysisRequest.setGameDuration(jsonObject.optLong("gdt"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return analysisRequest;
    }

    @Override
    public String toString() {
        return "DotRequest{" +
                "deviceId='" + deviceId + '\'' +
                ", brandCode='" + brandCode + '\'' +
                ", channel='" + channel + '\'' +
                ", packageName='" + packageName + '\'' +
                ", platform='" + platform + '\'' +
                ", simCountryCode='" + simCountryCode + '\'' +
                ", sysCountryCode='" + sysCountryCode + '\'' +
                ", language='" + language + '\'' +
                ", referrer='" + referrer + '\'' +
                ", gameId='" + gameId + '\'' +
                ", createTime='" + createTime + '\'' +
                ", lastLoginTime='" + lastLoginTime + '\'' +
                ", gameDuration='" + gameDuration + '\'' +
                '}';
    }
}
