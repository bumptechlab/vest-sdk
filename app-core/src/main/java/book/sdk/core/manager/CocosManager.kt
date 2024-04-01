package book.sdk.core.manager

import android.text.TextUtils
import book.sdk.core.VestCore
import book.sdk.core.util.CocosPreferenceUtil
import book.util.LogUtil
import book.util.parseInt

object CocosManager {
    private val TAG = CocosManager::class.java.simpleName
    private val FILTER_COUNTRY_CODES = arrayOf("IN", "ID", "BR", "GW", "VN")

    fun getUserId(): String? {
        var userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_USER_ID)
        if (TextUtils.isEmpty(userID)) {
            userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_COMMON_USER_ID)
        }
        return userID
    }

    fun getCocosFrameVersionInt(): Int {
        val cocosFrameVersion = getCocosFrameVersion()
        var cocosFrameVersionInt = 0
        if (!TextUtils.isEmpty(cocosFrameVersion)) {
            val cocosFrameVersionNumber = cocosFrameVersion.replace("[.]".toRegex(), "")
            LogUtil.d(TAG, "parse CocosFrameVersion: $cocosFrameVersionNumber")
            cocosFrameVersionInt = cocosFrameVersionNumber.parseInt()
        }
        return cocosFrameVersionInt
    }

    fun getCocosFrameVersion(): String {
        var cocosFrameVersion = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_COCOS_FRAME_VERSION)
        if (cocosFrameVersion.isNullOrEmpty()) {
            val targetCountry = VestCore.getTargetCountry()
            cocosFrameVersion = if (is1d0CountryCode(targetCountry)) {
                "1.0.0"
            } else {
                "2.0.0"
            }
        }
        LogUtil.d(TAG, "read CocosFrameVersion: $cocosFrameVersion")
        return cocosFrameVersion
    }

    private fun is1d0CountryCode(countryCode: String?): Boolean {
        var is1d0CountryCode = false
        for (i in FILTER_COUNTRY_CODES.indices) {
            if (FILTER_COUNTRY_CODES[i].equals(countryCode, ignoreCase = true)) {
                is1d0CountryCode = true
                break
            }
        }
        return is1d0CountryCode
    }
}
