package code.util

import android.text.TextUtils
import code.util.AESUtil.decryptTime
import code.util.AESUtil.isAESData
import code.util.AppGlobal.getApplication
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object AssetsUtil {
    const val TIME_FLAG = 0x868686
    const val JS_FLAG = 0x666666
    
    fun getAssetsFlagData(flag: Int): String? {
        val assetManager = getApplication().assets
        //获取assets目录所有文件名称
        var files: Array<String?>? = null
        try {
            files = assetManager.list("")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (files != null) {
            for (str in files) {
                var inputStream: InputStream? = null
                try {
                    //读取4字节数据
                    inputStream = assetManager.open(str!!)
                    val bytes = ByteArray(8)
                    inputStream.read(bytes)
                    //根据自定义的文件特征获取时间数据
                    if (isAESData(bytes, flag)) {
                        // 读取文件内容
                        val reader =
                            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                        val time = decryptTime(stringBuilder.toString().toByteArray())
                        if (!TextUtils.isEmpty(time)) {
                            return time
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    IOUtil.close(inputStream)
                }
            }
        }
        return null
    }
}