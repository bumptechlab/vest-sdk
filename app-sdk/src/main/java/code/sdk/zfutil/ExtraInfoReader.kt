package code.sdk.zfutil

import org.json.JSONException
import org.json.JSONObject
import java.io.File

object ExtraInfoReader {
    /**
     * easy api for get extra info.<br></br>
     *
     * @param apkFile apk file
     * @return null if not found
     */
    operator fun get(apkFile: File?): ExtraInfo? {
        val result = getMap(apkFile) ?: return null
        return ExtraInfo(result)
    }

    /**
     * get extra info by map
     *
     * @param apkFile apk file
     * @return null if not found
     */
    fun getMap(apkFile: File?): Map<String, String>? {
        try {
            val rawString = getRaw(apkFile) ?: return null
            val jsonObject = JSONObject(rawString)
            val keys = jsonObject.keys()
            val result: MutableMap<String, String> = HashMap()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = jsonObject.getString(key)
            }
            return result
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * get raw string from block
     *
     * @param apkFile apk file
     * @return null if not found
     */
    fun getRaw(apkFile: File?): String? {
        return PayloadReader.getString(apkFile, ApkPackageUtil.EXTRA_INFO_BLOCK_ID)
    }
}
