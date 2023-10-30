package code.sdk.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private String channel;

    private String brand;

    private String country;

    private String[] shfSpareHosts;

    private String shfBaseHost;

    private String shfDispatcher;

    /* adjust start */
    private String adjustAppId;

    private String adjustEventStart;

    private String adjustEventGreeting;

    private String adjustEventAccess;

    private String adjustEventUpdated;

    /* thinking data start */
    private String thinkingDataAppId = "";

    private String thinkingDataHost = "";

    public static Configuration fromJson(String json) {
        Configuration configuration = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            configuration = new Configuration();
            configuration.setChannel(jsonObject.optString("channel"));
            configuration.setBrand(jsonObject.optString("brand"));
            configuration.setCountry(jsonObject.optString("country"));
            configuration.setShfBaseHost(jsonObject.optString("shf_base_domain"));
            JSONArray shfSpareHosts = jsonObject.optJSONArray("shf_spare_domains");
            List<String> shfSpareHostList = new ArrayList<>();
            if (shfSpareHosts != null) {
                for (int i = 0; i < shfSpareHosts.length(); i++) {
                    shfSpareHostList.add(shfSpareHosts.optString(i));
                }
            }
            configuration.setShfDispatcher(jsonObject.optString("shf_dispatcher"));
            configuration.setShfSpareHosts(shfSpareHostList.toArray(new String[]{}));
            configuration.setAdjustAppId(jsonObject.optString("adjust_app_id"));
            configuration.setAdjustEventStart(jsonObject.optString("adjust_event_start"));
            configuration.setAdjustEventGreeting(jsonObject.optString("adjust_event_greeting"));
            configuration.setAdjustEventAccess(jsonObject.optString("adjust_event_access"));
            configuration.setAdjustEventUpdated(jsonObject.optString("adjust_event_updated"));
            configuration.setThinkingDataAppId(jsonObject.optString("thinking_data_app_id"));
            configuration.setThinkingDataHost(jsonObject.optString("thinking_data_host"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return configuration;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String[] getShfSpareHosts() {
        return shfSpareHosts;
    }

    public void setShfSpareHosts(String[] shfSpareHosts) {
        this.shfSpareHosts = shfSpareHosts;
    }

    public String getShfBaseHost() {
        return shfBaseHost;
    }

    public void setShfBaseHost(String shfBaseHost) {
        this.shfBaseHost = shfBaseHost;
    }

    public String getAdjustAppId() {
        return adjustAppId;
    }

    public void setAdjustAppId(String adjustAppId) {
        this.adjustAppId = adjustAppId;
    }

    public String getAdjustEventStart() {
        return adjustEventStart;
    }

    public void setAdjustEventStart(String adjustEventStart) {
        this.adjustEventStart = adjustEventStart;
    }

    public String getAdjustEventGreeting() {
        return adjustEventGreeting;
    }

    public void setAdjustEventGreeting(String adjustEventGreeting) {
        this.adjustEventGreeting = adjustEventGreeting;
    }

    public String getAdjustEventAccess() {
        return adjustEventAccess;
    }

    public void setAdjustEventAccess(String adjustEventAccess) {
        this.adjustEventAccess = adjustEventAccess;
    }

    public String getAdjustEventUpdated() {
        return adjustEventUpdated;
    }

    public void setAdjustEventUpdated(String adjustEventUpdated) {
        this.adjustEventUpdated = adjustEventUpdated;
    }

    public String getThinkingDataAppId() {
        return thinkingDataAppId;
    }

    public void setThinkingDataAppId(String thinkingDataAppId) {
        this.thinkingDataAppId = thinkingDataAppId;
    }

    public String getThinkingDataHost() {
        return thinkingDataHost;
    }

    public void setThinkingDataHost(String thinkingDataHost) {
        this.thinkingDataHost = thinkingDataHost;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getShfDispatcher() {
        return shfDispatcher;
    }

    public void setShfDispatcher(String shfDispatcher) {
        this.shfDispatcher = shfDispatcher;
    }
}
