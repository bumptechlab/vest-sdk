package code.util

import android.util.Log

/**
 * 日志开关
 */
object LogUtil {
    private val TAG = "Shell"

    private var isDebug = true


    fun isDebug(): Boolean {
        return isDebug
    }


    fun setDebug(debug: Boolean) {
        isDebug = debug
        Log.d(TAG, ">>>> Log Enable: $debug <<<<")
    }

    fun v(tag: String, format: String, vararg args: Any?) {
        if (isDebug) {
            Log.v(TAG, buildMsg(tag, String.format(format, *args)))
        }
    }

    fun v(tag: String, msg: String) {
        if (isDebug) {
            Log.v(TAG, buildMsg(tag, msg))
        }
    }


    fun d(tag: String, format: String, vararg args: Any?) {
        if (isDebug) {
            Log.d(TAG, buildMsg(tag, String.format(format, *args)))
        }
    }


    fun d(tag: String, msg: String) {
        if (isDebug) {
            Log.d(TAG, buildMsg(tag, msg))
        }
    }


    fun i(tag: String, format: String, vararg args: Any?) {
        if (isDebug) {
            Log.i(TAG, buildMsg(tag, String.format(format, *args)))
        }
    }


    fun i(tag: String, msg: String) {
        if (isDebug) {
            Log.i(TAG, buildMsg(tag, msg))
        }
    }


    fun w(tag: String, format: String, vararg args: Any?) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, String.format(format, *args)))
        }
    }


    fun w(tag: String, msg: String) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, msg))
        }
    }

    fun w(tag: String, throwable: Throwable?, format: String, vararg args: Any?) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, String.format(format, *args)), throwable)
        }
    }

    fun w(tag: String, throwable: Throwable?, msg: String) {
        if (isDebug) {
            Log.w(TAG, buildMsg(tag, msg), throwable)
        }
    }


    fun e(tag: String, format: String, vararg args: Any?) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, String.format(format, *args)))
        }
    }


    fun e(tag: String, msg: String) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, msg))
        }
    }


    fun e(tag: String, throwable: Throwable?, format: String, vararg args: Any?) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, String.format(format, *args)), throwable)
        }
    }


    fun e(tag: String, throwable: Throwable?, msg: String) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, msg), throwable)
        }
    }


    fun e(tag: String, e: Throwable?) {
        if (isDebug) {
            Log.e(TAG, buildMsg(tag, getStackTraceString(e)))
        }
    }

    fun getStackTraceString(tr: Throwable?): String {
        return Log.getStackTraceString(tr)
    }

    private fun buildMsg(tag: String, msg: String): String {
        val buffer = StringBuilder()
        val stackTraceElement = Thread.currentThread().stackTrace[4]
        buffer.append(tag)
        buffer.append("[(")
        buffer.append(stackTraceElement.fileName)
        buffer.append(":")
        buffer.append(stackTraceElement.lineNumber)
        buffer.append(")")
        //        buffer.append("#");
//        buffer.append(stackTraceElement.getMethodName());
//        buffer.append(" -> ");
//        buffer.append(Thread.currentThread().getName());
        buffer.append("] $ ")
        buffer.append(msg)
        return buffer.toString()
    }
}