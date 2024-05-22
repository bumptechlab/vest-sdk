package poetry.sdk.bridge

import android.content.pm.PackageManager
import android.os.Build
import android.webkit.JavascriptInterface
import poetry.sdk.core.util.DeviceUtil
import poetry.sdk.core.util.PreferenceUtil
import poetry.util.AppGlobal
import poetry.util.LogUtil
import java.util.Locale

/**
 *
 * @author stefan
 * @date 2024-05-14
 */
class JsiJsBridge(private val mCallback: BridgeCallback?) {
    private val mJsBridgeImpl: JsBridgeImpl = JsBridgeImpl(mCallback)

    @JavascriptInterface
    fun getImageUrl(text: String?): Boolean {
        LogUtil.e(javaClass.name, "JsiJsBridge.getImageUrl")
        //因当前为精简版本，故不作实现
        return true
    }

    @JavascriptInterface
    fun cachePromotionImage(url: String?, type: String?) {
        LogUtil.e(javaClass.name, "JsiJsBridge.cachePromotionImage:$url,type:$type")
        //因当前为精简版本，故不作实现
    }

    @JavascriptInterface
    fun saveImageToAlbumAndCopyLink(url: String?, type: String?) {
        LogUtil.e(javaClass.name, "JsiJsBridge.saveImageToAlbumAndCopyLink:$url,type:$type")
        //因当前为精简版本，故不作实现
    }


    @JavascriptInterface
    fun getDeviceInfo(): String {
        var version = ""
        var versionCode = 1L
        try {
            val packageInfo =
                AppGlobal.application?.packageName?.let {
                    AppGlobal.application?.packageManager?.getPackageInfo(
                        it, 0
                    )
                }
            if (packageInfo != null) {
                version = packageInfo.versionName
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCode = packageInfo.longVersionCode
                } else {
                    versionCode = packageInfo.versionCode.toLong()
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        val builder = StringBuilder()
        builder.append("[aid:").append(DeviceUtil.getDeviceID()).append("],[code:").append(
            PreferenceUtil.readTargetCountry()
        ).append("],[lan:").append(Locale.getDefault().language).append("],[svc:")
            .append(Build.VERSION.SDK_INT).append("],[svn:").append(Build.VERSION.RELEASE)
            .append("],[cvn:").append(version).append("],[cvc:").append(versionCode)
            .append("],[chn:").append(mJsBridgeImpl.getChannel())
            .append("],[pkg:").append(AppGlobal.application?.packageName).append("]")
        return builder.toString()
    }


    @JavascriptInterface
    fun getAdid(): String? {
        val adid = mJsBridgeImpl.getAdjustDeviceID()
        LogUtil.dT(javaClass.name, "getAdid: $adid")
        return adid
    }

    @JavascriptInterface
    fun getGpsAdid(): String {
        val gpsAdid = mJsBridgeImpl.getGoogleADID()
        LogUtil.dT(javaClass.name, "getGpsAdid:$gpsAdid")
        return gpsAdid
    }
}