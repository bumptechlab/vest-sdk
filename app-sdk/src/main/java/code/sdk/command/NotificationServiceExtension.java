package code.sdk.command;

import android.content.Context;

import com.onesignal.OSNotification;
import com.onesignal.OSNotificationReceivedEvent;
import com.onesignal.OneSignal;

import org.json.JSONObject;

import code.sdk.core.manager.ThinkingDataManager;
import code.util.JSONUtil;
import code.util.LogUtil;

public class NotificationServiceExtension implements OneSignal.OSRemoteNotificationReceivedHandler {

    private static final String TAG = NotificationServiceExtension.class.getSimpleName();

    @Override
    public void remoteNotificationReceived(Context context, OSNotificationReceivedEvent notificationReceivedEvent) {

        ThinkingDataManager.trackEvent("push_message", trackData(notificationReceivedEvent));

    }

    private JSONObject trackData(OSNotificationReceivedEvent notificationReceivedEvent) {
        OSNotification notification = notificationReceivedEvent.getNotification();
        JSONObject data = notification.getAdditionalData();
        LogUtil.i(TAG, "[OneSignal] Notification received, title: %s, content: %s, data: %s",
                notification.getTitle(), notification.getBody(), data);

        // If complete isn't call within a time period of 25 seconds, OneSignal internal logic will show the original notification
        // To omit displaying a notification, pass `null` to complete()
        notificationReceivedEvent.complete(notification);

        String from = "";
        if (data != null) {
            JSONObject openDialog = data.optJSONObject("open_dialog");
            if (openDialog != null) {
                from = openDialog.optString("id");
            }
        }
        JSONObject trackData = new JSONObject();
        JSONUtil.putJsonValue(trackData, "from", from);
        return trackData;
    }
}
