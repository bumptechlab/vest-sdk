package code.sdk.manage;

import org.json.JSONObject;

public interface OneSignalNotificationListener {

    public void onOpenNotification(JSONObject data);
}
