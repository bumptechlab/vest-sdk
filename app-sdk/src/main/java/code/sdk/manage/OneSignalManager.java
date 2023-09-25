package code.sdk.manage;

import android.content.Context;
import android.text.TextUtils;

import com.onesignal.OSNotificationOpenedResult;
import com.onesignal.OneSignal;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import code.sdk.core.Constant;
import code.sdk.core.VestCore;
import code.sdk.core.manager.ThinkingDataManager;
import code.sdk.core.util.CocosPreferenceUtil;
import code.util.JSONUtil;
import code.util.LogUtil;
import code.util.NumberUtil;

public class OneSignalManager {

    private static final String TAG = OneSignalManager.class.getSimpleName();

    private static final String ONESIGNAL_APP_ID_BRAZIL = "69bdf678-0455-4ea2-9b10-0fb2e57c4f83";
    private static final String ONESIGNAL_APP_ID_INDIA = "5883d10d-8cfa-430d-a3de-4c1a3266b81d";
    private static final String ONESIGNAL_APP_ID_INDONESIA = "7b088a62-4fc3-4fda-a877-a3fd0dc6b919";
    private static final String ONESIGNAL_APP_ID_TEST = "5ffeb866-fa21-41d9-88ca-ab44c2d11a46";
    private static OneSignalNotificationListener sOneSignalNotificationListener = null;

    public static String getOneSignalAppId() {
        String appId = "";
        if (VestCore.getTargetCountry().equalsIgnoreCase(Constant.TARGET_COUNTRY_BRAZIL)) {
            appId = ONESIGNAL_APP_ID_BRAZIL;
        } else if (VestCore.getTargetCountry().equalsIgnoreCase(Constant.TARGET_COUNTRY_INDIA)) {
            appId = ONESIGNAL_APP_ID_INDIA;
        } else if (VestCore.getTargetCountry().equalsIgnoreCase(Constant.TARGET_COUNTRY_INDONESIA)) {
            appId = ONESIGNAL_APP_ID_INDONESIA;
        }
        return appId;
    }

    public static void init(Context context) {
        if (!assertOk()) return;
        String appId = getOneSignalAppId();
        LogUtil.d(TAG, "[OneSignal] init with appId: %s", appId);
        if (TextUtils.isEmpty(appId)) {
            return;
        }
        // Enable verbose OneSignal logging to debug issues if needed.
        if (LogUtil.isDebug()) {
            OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        } else {
            OneSignal.setLogLevel(OneSignal.LOG_LEVEL.NONE, OneSignal.LOG_LEVEL.NONE);
        }
        // OneSignal Initialization
        OneSignal.initWithContext(context);
        OneSignal.setAppId(appId);
        OneSignal.setNotificationOpenedHandler(new OneSignal.OSNotificationOpenedHandler() {
            @Override
            public void notificationOpened(OSNotificationOpenedResult osNotificationOpenedResult) {
                sNotificationClickData = osNotificationOpenedResult.getNotification().getAdditionalData();
                LogUtil.d(TAG, "[OneSignal] Notification clicked: %s", sNotificationClickData);

                String from = "";
                if (sNotificationClickData != null) {
                    JSONObject openDialog = sNotificationClickData.optJSONObject("open_dialog");
                    if (openDialog != null) {
                        from = openDialog.optString("id");
                    }
                }
                //记录Notification点击
                JSONObject trackData = new JSONObject();
                JSONUtil.putJsonValue(trackData, "from", from);
                ThinkingDataManager.trackEvent("push_message_click", trackData);
            }
        });
    }

    private static JSONObject sNotificationClickData = null;

    public static void setOneSignalNotificationListener(OneSignalNotificationListener oneSignalNotificationListener) {
        sOneSignalNotificationListener = oneSignalNotificationListener;
    }

    /**
     * handle notification when Cocos is ready
     */
    public static void handleNotification() {
        LogUtil.d(TAG, "[OneSignal] handle notification: " + sNotificationClickData);
        if (sNotificationClickData != null) {
            if (sOneSignalNotificationListener != null) {
                sOneSignalNotificationListener.onOpenNotification(sNotificationClickData);
            }
            sNotificationClickData = null; //消费了当前的通知
        }
    }

    public static void showPrompt() {
        if (!assertOk()) return;
        if (!isInitDone()) {
            LogUtil.w(TAG, "[OneSignal] not init, could not showPrompt");
            return;
        }
        // promptForPushNotifications will show the native Android notification permission prompt.
        // We recommend removing the following code and instead using an In-App Message to prompt for notification permission (See step 7)
        OneSignal.promptForPushNotifications();
    }

    public static void disablePush(boolean disable) {
        OneSignal.disablePush(disable);
    }

    public static void setup() {
        if (!assertOk()) return;
        if (!isInitDone()) {
            LogUtil.w(TAG, "[OneSignal] not init, could not setup");
            return;
        }
        try {
            String accountId = getExternalUserId();
            LogUtil.d(TAG, "[OneSignal] Set external user id: " + accountId);
            if (!TextUtils.isEmpty(accountId)) {
                setupOnAccount(accountId);
            } else {
                setupOnEmptyAccount();
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e, "Fail to setup onesignal userId");
        }
    }

    private static void setupOnAccount(String accountId) {
        OneSignal.setExternalUserId(accountId, new OneSignal.OSExternalUserIdUpdateCompletionHandler() {
            @Override
            public void onSuccess(JSONObject results) {
                LogUtil.d(TAG, "[OneSignal] Set external user id success: " + results);
            }

            @Override
            public void onFailure(OneSignal.ExternalIdError error) {
                String errorMsg = error == null ? "" : error.getMessage();
                LogUtil.e(TAG, "[OneSignal] Set external user id failure: " + errorMsg);
            }
        });
        sendBaseTags();
    }

    private static void setupOnEmptyAccount() {
        OneSignal.removeExternalUserId(new OneSignal.OSExternalUserIdUpdateCompletionHandler() {
            @Override
            public void onSuccess(JSONObject results) {
                LogUtil.d(TAG, "[OneSignal] Remove external user id success: " + results);
            }

            @Override
            public void onFailure(OneSignal.ExternalIdError externalIdError) {
                String errorMsg = externalIdError == null ? "" : externalIdError.getMessage();
                LogUtil.e(TAG, "[OneSignal] Remove external user id failure: " + errorMsg);
            }

        });
        clearTags();
    }

    private static void sendTags(JSONObject oneSignalTags) {
        try {
            LogUtil.d(TAG, "[OneSignal] send tags json: " + oneSignalTags.toString());
            OneSignal.sendTags(oneSignalTags, new OneSignal.ChangeTagsUpdateHandler() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    LogUtil.d(TAG, "[OneSignal] Send tags success: " + jsonObject);
                }

                @Override
                public void onFailure(OneSignal.SendTagsError sendTagsError) {
                    String errorMsg = sendTagsError == null ? "" : sendTagsError.getMessage();
                    LogUtil.e(TAG, "[OneSignal] Send tags failure: " + errorMsg);
                }
            });
        } catch (Exception e) {
            LogUtil.e(TAG, e, "Fail to send tags");
        }
    }


    private static void clearTags() {
        String[] allTags = new String[]{
                OneSignalTags.KEY_RECHARGE,
                OneSignalTags.KEY_CUR_LEVEL,
                OneSignalTags.KEY_LAST_LEVEL
        };
        List<String> tagList = Arrays.asList(allTags);
        OneSignal.deleteTags(tagList, new OneSignal.ChangeTagsUpdateHandler() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                LogUtil.d(TAG, "[OneSignal] Delete tags success: " + jsonObject);
            }

            @Override
            public void onFailure(OneSignal.SendTagsError sendTagsError) {
                String errorMsg = sendTagsError == null ? "" : sendTagsError.getMessage();
                LogUtil.e(TAG, "[OneSignal] Delete tags failure: " + errorMsg);
            }
        });
    }

    private static void sendBaseTags() {
        JSONObject oneSignalTags = buildBaseTags();
        sendTags(oneSignalTags);
    }

    private static JSONObject buildBaseTags() {
        JSONObject oneSignalTags = new JSONObject();
        try {
            String userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_USER_ID);

            String curMonthLvText = CocosPreferenceUtil.getString("_int_" + userID + "_cur_month_lv");
            int curMonthLv = NumberUtil.parseInt(curMonthLvText);

            String lastMonthLvText = CocosPreferenceUtil.getString("_int_" + userID + "_last_month_lv");
            int lastMonthLv = NumberUtil.parseInt(lastMonthLvText);

            String latestRechargeText = CocosPreferenceUtil.getString("_int_" + userID + "_latest_recharge");
            long latestRecharge = NumberUtil.parseLong(latestRechargeText) / 1000;

            oneSignalTags.put(OneSignalTags.KEY_CUR_LEVEL, curMonthLv);
            oneSignalTags.put(OneSignalTags.KEY_LAST_LEVEL, lastMonthLv);
            oneSignalTags.put(OneSignalTags.KEY_RECHARGE, latestRecharge);
        } catch (Exception e) {
            //ObfuscationStub6.inject();
        }
        return oneSignalTags;
    }

    private static String getExternalUserId() {
        String userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_USER_ID);
        if (TextUtils.isEmpty(userID)) {
            return "";
        }
        return getTargetCountry() + "-" + userID;
    }

    public static String getTargetCountry() {
        return VestCore.getTargetCountry().toUpperCase();
    }

    public static void initTester(Context context) {
        if (!assertOk()) return;
        try {
            init(context);
            showPrompt();
            String userId = "ituser-" + System.currentTimeMillis();
            OneSignal.setExternalUserId(userId, new OneSignal.OSExternalUserIdUpdateCompletionHandler() {
                @Override
                public void onSuccess(JSONObject results) {
                    LogUtil.d(TAG, "[OneSignal] Set external user id success: " + results);
                }

                @Override
                public void onFailure(OneSignal.ExternalIdError error) {
                    String errorMsg = error == null ? "" : error.getMessage();
                    LogUtil.e(TAG, "[OneSignal] Set external user id failure: " + errorMsg);
                }
            });
            sendBaseTags();
        } catch (Exception e) {
            //ObfuscationStub6.inject();
            LogUtil.e(TAG, e, "[OneSignal] init tester failed: %s", e.getMessage());
        }
    }

    private static boolean isInitDone() {
        boolean isInitDone = false;
        try {
            Field initDoneField = OneSignal.class.getDeclaredField("initDone");
            initDoneField.setAccessible(true);
            isInitDone = (Boolean) initDoneField.get(null);
            LogUtil.d(TAG, "[OneSignal] initDone: " + isInitDone);
        } catch (Exception e) {
            LogUtil.e(TAG, e, "[OneSignal] read OneSignal.initDone fail");
        }
        return isInitDone;
    }


    /**
     * 通过反射断言，引用方是否引入了OneSignal
     * 如果没引用则不使用OneSignal
     *
     * @return
     */
    private static boolean assertOk() {
        try {
            Class oneClz = Class.forName("com.onesignal.OneSignal");
            return true;
        } catch (Exception e) {
            LogUtil.w(TAG, e, "[OneSignal]assert fail");
        }
        return false;
    }
}
