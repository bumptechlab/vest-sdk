package code.sdk.core.manager;

import android.app.Activity;
import android.os.Process;

import java.util.Stack;

import code.sdk.core.util.DeviceUtil;

public class ActivityManager {
    private static ActivityManager sInstance;
    private Stack<Activity> mActivityStack;

    public static ActivityManager getInstance() {
        if (sInstance == null) {
            sInstance = new ActivityManager();
        }
        return sInstance;
    }

    private ActivityManager() {
        mActivityStack = new Stack<>();
    }

    public void push(Activity activity) {
        if (activity == null) {
            return;
        }
        mActivityStack.push(activity);
    }

    public void remove(Activity activity) {
        if (activity == null) {
            return;
        }
        mActivityStack.remove(activity);
    }

    public void finishAll() {
        try {
            while (!mActivityStack.isEmpty()) {
                Activity activity = mActivityStack.pop();
                if (activity != null) {
                    DeviceUtil.finishActivitySafety(activity);
                }
            }
            System.exit(0);
            Process.killProcess(Process.myPid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
