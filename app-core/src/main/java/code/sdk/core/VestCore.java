package code.sdk.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.androidx.h5.utils.AESKeyStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

import code.sdk.core.manager.ActivityManager;
import code.sdk.core.manager.AdjustManager;
import code.sdk.core.manager.ConfigurationManager;
import code.sdk.core.manager.ThinkingDataManager;
import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.GoogleAdIdInitializer;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.TestUtil;
import code.util.AppGlobal;
import code.util.LogUtil;

public class VestCore {

    private static final String TAG = VestCore.class.getSimpleName();
    private static Context mContext;

    public static void init(Context context, String configAssets) {
        mContext = context;
        recordFirstLaunchTime();
        setUncaughtException();
        AESKeyStore.init();
        LogUtil.setDebug(TestUtil.isLoggable());
        ConfigurationManager.getInstance().init(context, configAssets);
        registerActivityLifecycleCallbacks();
        GoogleAdIdInitializer.init();
    }

    private static void recordFirstLaunchTime() {
        long firstLaunchTime = PreferenceUtil.getFirstLaunchTime();
        if (firstLaunchTime == 0) {
            PreferenceUtil.saveFirstLaunchTime(System.currentTimeMillis());
        }
    }

    public static void registerActivityLifecycleCallbacks() {
        AppGlobal.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                Intent intent = activity.getIntent();
                Set<String> categories = intent.getCategories();
                LogUtil.d(TAG, "onActivityCreated: intent=%s, savedInstanceState=%s", intent, savedInstanceState);
                if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                    LogUtil.d(TAG, "onActivityCreated: this is a launcher activity");
                    TestUtil.handleIntent(activity);
                }
                ActivityManager.getInstance().push(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                ActivityManager.getInstance().remove(activity);
            }
        });
    }

    public static void initThirdSDK() {
        ThinkingDataManager.init(AppGlobal.getApplication());
        AdjustManager.init(AppGlobal.getApplication());
    }

    public static String getTargetCountry() {
        String targetCountry = PreferenceUtil.readTargetCountry();
        if (TextUtils.isEmpty(targetCountry)) {
            targetCountry = ConfigPreference.readTargetCountry();
        }
        return targetCountry;
    }


    public static void onCreate() {

    }

    public static void onDestroy() {
        ThinkingDataManager.flush();
    }


    /**
     * 捕获异常上报
     */
    private static void setUncaughtException() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                LogUtil.e(TAG, e, "on uncaughtException: ");
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
                    LogUtil.i(TAG, "setUncaughtException json: %s", json);
                    ThinkingDataManager.trackEvent("td_crash", json);
                    ThinkingDataManager.flush();
                } catch (Throwable exception) {
                    LogUtil.e(TAG, exception, "setUncaughtException errorInLogging: ");
                } finally {
                    try {
                        Thread.sleep(1000);
                        ActivityManager.getInstance().finishAll();
                        System.exit(1);
                        Process.killProcess(Process.myPid());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }
}
