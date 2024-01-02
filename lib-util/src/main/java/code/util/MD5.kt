package code.util

import code.util.IOUtil.close
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object MD5 {
    
    fun encrypt(string: String?): String {
        return if (string.isNullOrEmpty()) {
            ""
        } else encrypt(string.toByteArray())
    }

    fun encrypt(bytes: ByteArray?): String {
        if (null == bytes || bytes.isEmpty()) {
            return ""
        }
        try {
            val md5 = MessageDigest.getInstance("MD5")
            val md5Bytes = md5.digest(bytes)
            val result = StringBuilder()
            for (b in md5Bytes) {
                var temp = Integer.toHexString(b.toInt() and 0xff)
                if (temp.length == 1) {
                    temp = "0$temp"
                }
                result.append(temp)
            }
            return result.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    // 计算文件的 MD5 值
    fun encrypt(file: File?): String {
        if (file == null || !file.isFile || !file.exists()) {
            return ""
        }
        var input: FileInputStream? = null
        val result = StringBuilder()
        val buffer = ByteArray(8192)
        var len: Int
        try {
            val md5 = MessageDigest.getInstance("MD5")
            input = FileInputStream(file)
            while (input.read(buffer).also { len = it } != -1) {
                md5.update(buffer, 0, len)
            }
            val bytes = md5.digest()
            for (b in bytes) {
                var temp = Integer.toHexString(b.toInt() and 0xff)
                if (temp.length == 1) {
                    temp = "0$temp"
                }
                result.append(temp)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close(input)
        }
        return result.toString()
    }
}