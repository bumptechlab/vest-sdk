package poetry.sdk.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Base64
import poetry.util.AppGlobal
import poetry.util.LogUtil
import java.security.MessageDigest

object PackageUtil {
    private val TAG = PackageUtil::class.java.simpleName
    fun getLaunchIntentForPackage(packageName: String): Intent? {
        val context: Context = AppGlobal.application!!
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage(packageName)
    }


    fun getPackageName(): String = AppGlobal.application!!.packageName

    fun getPackageVersionName(): String {
        val context: Context = AppGlobal.application!!
        val pm = context.packageManager
        try {
            val pi = pm.getPackageInfo(context.packageName, 0)
            return pi.versionName
        } catch (e: Exception) {
        }
        return ""
    }

    fun getPackageVersionCode(): Int {
        val context: Context = AppGlobal.application!!
        val pm = context.packageManager
        try {
            val pi = pm.getPackageInfo(context.packageName, 0)
            return pi.versionCode
        } catch (e: Exception) {
        }
        return 0
    }


    fun getChannel(): String? {
        var channel = PreferenceUtil.readChannel()
        if (!TextUtils.isEmpty(channel)) {
            return channel
        }
        channel = ConfigPreference.readChannel()
        PreferenceUtil.saveChannel(channel)
        return channel
    }

    fun getChannelByCountry(): String {
        if (PreferenceUtil.readTargetCountry() == "GVN") {
            return getVNChannel()
        } else {
            return getChannel()?:"major"
        }
    }

    private fun getVNChannel(): String {
        val chn = PreferenceUtil.readChannel()
        val childBrd = PreferenceUtil.readChildBrand()
        if (TextUtils.isEmpty(childBrd) || TextUtils.isEmpty(chn)) {
            return ""
        }
        return "a-vn2-${childBrd}-${chn}"
    }

    fun getParentBrand(): String? {
        var parentBrand = PreferenceUtil.readParentBrand()
        if (!TextUtils.isEmpty(parentBrand)) {
            return parentBrand
        }
        parentBrand = ConfigPreference.readBrand()
        PreferenceUtil.saveParentBrand(parentBrand)
        return parentBrand
    }

    fun getChildBrand(): String? {
        val childBrd = PreferenceUtil.readChildBrand()
        LogUtil.d(TAG, "read child brand: %s", childBrd)
        return childBrd
    }

    fun getBuildVersion(): String {
        val version = readMetaData(poetry.sdk.core.BuildConfig.KEY_BUILD_VERSION)
        val versionBytes = Base64.decode(version, Base64.DEFAULT)
        return String(versionBytes)
    }

    private fun readMetaData(key: String?): String {
        val context: Context = AppGlobal.application!!
        val pm = context.packageManager
        var value = ""
        try {
            val applicationInfo = pm.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            val data = applicationInfo.metaData[key]
            if (data is String) {
                value = data
            } else if (data is Int) {
                value = data.toString()
            } else if (data is Float) {
                value = data.toString()
            }
        } catch (e: Exception) {
        }
        return value
    }

    fun getAppName(): String? {
        var appName = PreferenceUtil.readAppName()
        if (!TextUtils.isEmpty(appName)) {
            return appName
        }
        val context: Context = AppGlobal.application!!
        val pm = context.packageManager
        try {
            val pi = pm.getPackageInfo(
                context.packageName, 0
            )
            appName = pi.applicationInfo.loadLabel(pm).toString()
            PreferenceUtil.saveAppName(appName)
        } catch (e: Exception) {
        }
        return appName
    }

    fun getKeystoreHashes(context: Context): List<String> {
        val hashList: MutableList<String> = ArrayList()
        try {
            val info = context.packageManager.getPackageInfo(
                context.packageName, PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                hashList.add(hash)
            }
        } catch (e: Exception) {
        }
        return hashList
    }
}
