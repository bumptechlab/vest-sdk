package code.util;

import android.util.Log;

/**
 * 日志开关
 */

public class LogUtil {
    public static final String TAG = new String(new byte[] {83, 104, 101, 108, 108});
    private static boolean isDebug = true;

    public static boolean isDebug() {
        return isDebug;
    }

    public static void setDebug(boolean debug) {
        isDebug = debug;
        Log.d(TAG, ">>>> Log Enable: " + debug + " <<<<");
    }

    public static void v(String tag, String format, Object... args) {
        if (isDebug) {
            Log.v(TAG, buildMsg(tag, String.format(format, args)));
        }
    }

    public static void v(String tag, String msg) {
        if (isDebug) {
            Log.v(TAG, buildMsg(tag, msg));
        }
    }

    public static void d(String tag, String format, Object... args) {
        if (isDebug) {
            Log.d(TAG, buildMsg(tag, String.format(format, args)));
        }
    }

    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d(TAG, buildMsg(tag, msg));
        }
    }

    public static void i(String tag, String format, Object... args) {
        if (isDebug) {
            Log.i(TAG, buildMsg(tag, String.format(format, args)));
        }
    }

    public static void i(String tag, String msg) {
        if (isDebug) {
            Log.i(TAG, buildMsg(tag, msg));
        }
    }

    public static void w(String tag, String format, Object... args) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, String.format(format, args)));
        }
    }

    public static void w(String tag, String msg) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, msg));
        }
    }

    public static void w(String tag, Throwable throwable, String format, Object... args) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, String.format(format, args)), throwable);
        }
    }

    public static void w(String tag, Throwable throwable, String msg) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, msg), throwable);
        }
    }

    public static void e(String tag, String format, Object... args) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, String.format(format, args)));
        }
    }

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, msg));
        }
    }

    public static void e(String tag, Throwable throwable, String format, Object... args) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, String.format(format, args)), throwable);
        }
    }

    public static void e(String tag, Throwable throwable, String msg) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, msg), throwable);
        }
    }

    public static void e(String tag, Throwable e) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, getStackTraceString(e)));
        }
    }


    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    private static String buildMsg(String tag, String msg) {
        StringBuilder buffer = new StringBuilder();
        final StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
        buffer.append(tag);
        buffer.append("[(");
        buffer.append(stackTraceElement.getFileName());
        buffer.append(":");
        buffer.append(stackTraceElement.getLineNumber());
        buffer.append(")");
//        buffer.append("#");
//        buffer.append(stackTraceElement.getMethodName());
//        buffer.append(" -> ");
//        buffer.append(Thread.currentThread().getName());
        buffer.append("] $ ");
        buffer.append(msg);
        return buffer.toString();
    }
}
