package code.sdk.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Base64
import code.sdk.core.util.ConfigPreference.readBrand
import code.sdk.core.util.ConfigPreference.readChannel
import code.util.AppGlobal
import code.util.LogUtil.d
import java.security.MessageDigest

object PackageUtil {
   private val TAG = PackageUtil::class.java.simpleName
    fun getLaunchIntentForPackage(packageName: String): Intent? {
        //ObfuscationStub2.inject();
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
            //ObfuscationStub3.inject();
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
            //ObfuscationStub4.inject();
        }
        return 0
    }


    fun getChannel(): String {
        var channel = PreferenceUtil.readChannel()
        if (!TextUtils.isEmpty(channel)) {
            //ObfuscationStub5.inject();
            return channel
        }
        channel = readChannel()
        //        channel = readMetaData("chn");
        PreferenceUtil.saveChannel(channel)
        return channel
    }

    fun getParentBrand(): String {
        var parentBrand = PreferenceUtil.readParentBrand()
        if (!TextUtils.isEmpty(parentBrand)) {
            //ObfuscationStub6.inject();
            return parentBrand
        }
        parentBrand = readBrand()
        PreferenceUtil.saveParentBrand(parentBrand)
        return parentBrand
    }

    fun getChildBrand(): String {
        val childBrd = PreferenceUtil.readChildBrand()
        d(TAG, "read child brand: %s", childBrd)
        return childBrd
    }

    fun getBuildVersion(): String {
        val version = readMetaData("build.version")
        val versionBytes = Base64.decode(version, Base64.DEFAULT)
        return String(versionBytes)
    }

    fun readMetaData(key: String?): String {
        val context: Context = AppGlobal.application!!
        val pm = context.packageManager
        var value = ""
        try {
            val applicationInfo = pm.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            val data = applicationInfo.metaData[key]
            if (data is String) {
                //ObfuscationStub7.inject();
                value = data
                //ObfuscationStub8.inject();
            } else if (data is Int) {
                //ObfuscationStub0.inject();
                value = data.toString()
            } else if (data is Float) {
                //ObfuscationStub1.inject();
                value = data.toString()
            }
        } catch (e: Exception) {
            //ObfuscationStub2.inject();
        }
        return value
    }

    fun getAppName(): String {
        var appName = PreferenceUtil.readAppName()
        if (!TextUtils.isEmpty(appName)) {
            //ObfuscationStub3.inject();
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
            //ObfuscationStub4.inject();
        }
        return appName
    }

    fun getKeystoreHashes(context: Context): List<String> {
        val hashList: MutableList<String> = ArrayList()
        try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                hashList.add(hash)
            }
        } catch (e: Exception) {
            //ObfuscationStub5.inject();
        }
        return hashList
    }
}
