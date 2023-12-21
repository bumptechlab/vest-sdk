package code.sdk.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

import code.sdk.core.event.SDKEvent;
import code.sdk.core.manager.ActivityManager;
import code.sdk.core.manager.AdjustManager;
import code.sdk.core.manager.ConfigurationManager;
import code.sdk.core.manager.SimpleLifecycleCallbacks;
import code.sdk.core.manager.ThinkingDataManager;
import code.sdk.core.util.GoogleAdIdInitializer;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.TestUtil;
import code.util.AESKeyStore;
import code.util.AppGlobal;
import code.util.LogUtil;

public class VestCore {

    private static final String TAG = VestCore.class.getSimpleName();
    private static boolean isTestIntentHandled;

    public static void init(Context context, String configAssets) {
//        PreferenceUtil.getInspectStartTime();
        setUncaughtException();
        AESKeyStore.init();
        LogUtil.setDebug(TestUtil.isLoggable());
        ConfigurationManager.getInstance().init(context, configAssets);
        registerActivityLifecycleCallbacks();
        GoogleAdIdInitializer.init();
        initThirdSDK();
    }

    public static boolean isTestIntentHandled() {
        return isTestIntentHandled;
    }

    public static void registerActivityLifecycleCallbacks() {
        AppGlobal.getApplication().registerActivityLifecycleCallbacks(new SimpleLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                ActivityManager.getInstance().push(activity);
                interceptLauncherActivity(activity);
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                ActivityManager.getInstance().remove(activity);
                interceptDestroyedActivity(activity);
            }
        });
    }

    private static void interceptLauncherActivity(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        Set<String> categories = intent.getCategories();
        LogUtil.d(TAG, "[Vest-Core] onActivityCreated: intent=%s", intent);
        if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
            LogUtil.d(TAG, "[Vest-Core] onActivityCreated: %s is a launcher activity", intent.getComponent() != null ? intent.getComponent().flattenToString() : "");
            isTestIntentHandled = TestUtil.handleIntent(activity);
        }
    }

    private static void interceptDestroyedActivity(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        LogUtil.d(TAG, "[Vest-Core] onActivityDestroyed: intent=%s", intent);
        if (ActivityManager.getInstance().isActivityEmpty()) {
            isTestIntentHandled = false;
            LogUtil.d(TAG, "[Vest-Core] onActivityDestroyed: activity stack is empty, reset");
        }
    }

    public static void initThirdSDK() {
        ThinkingDataManager.init(AppGlobal.getApplication());
        AdjustManager.init(AppGlobal.getApplication());
    }

    public static void updateThirdSDK() {
        ThinkingDataManager.initTDEvents();
        AdjustManager.initParams();
    }

    public static String getTargetCountry() {
        String targetCountry = PreferenceUtil.readTargetCountry();
        LogUtil.d(TAG, "[Vest-Core] read target country: %s", targetCountry);
        return targetCountry;
    }


    public static void onCreate() {
        EventBus.getDefault().post(new SDKEvent("onCreate"));
    }

    public static void onDestroy() {
        ThinkingDataManager.flush();
        EventBus.getDefault().post(new SDKEvent("onDestroy"));
    }

    public static void onPause() {
        EventBus.getDefault().post(new SDKEvent("onPause"));
    }

    public static void onResume() {
        EventBus.getDefault().post(new SDKEvent("onResume"));
    }

    /**
     * 捕获异常上报
     */
    private static void setUncaughtException() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                LogUtil.e(TAG, e, "[Vest-Core] on uncaughtException: ");
                try {
                    JSONObject json = new JSONObject();
                    JSONArray stackArray = new JSONArray();
                    StackTraceElement[] stackTrace = e.getStackTrace();
                    if (stackTrace != null) {
                        for (int i = 0; i < stackTrace.length; i++) {
                            stackArray.put(stackTrace[i]);
                        }
                    }
                    json.put("crash_stack", stackArray);
                    json.put("crash_msg", e.getLocalizedMessage());
                    json.put("crash_cause", e.getCause());
                    LogUtil.i(TAG, "[Vest-Core] setUncaughtException json: %s", json);
                    ThinkingDataManager.trackEvent("td_crash", json);
                    ThinkingDataManager.flush();
                } catch (Throwable exception) {
                    LogUtil.e(TAG, exception, "[Vest-Core] setUncaughtException errorInLogging: ");
                } finally {
                    try {
                        Thread.sleep(1000);
                        ActivityManager.getInstance().finishAll();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }
}
