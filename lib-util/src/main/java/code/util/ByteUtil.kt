package code.util

import android.text.TextUtils

object ByteUtil {
    private val TAG = ByteUtil::class.java.simpleName

    /**
     * byte数组转16进制字符串
     *
     * @param bytes byte数组
     * @return 16进制字符串
     */
    fun byteArrayToHexStr(bytes: ByteArray): String {
        var strHex: String
        val sb = StringBuilder()
        for (aByte in bytes) {
            strHex = Integer.toHexString(aByte.toInt() and 0xFF)
            sb.append(" ").append(if (strHex.length == 1) "0" else "")
                .append(strHex) // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim { it <= ' ' }
    }

    /**
     * byte字节转int
     *
     * @param b byte字节
     * @return int
     */
    fun byteToInt(b: Byte): Int {
        val x = b.toInt() and 0xff
        return if (x == 127) {
            0
        } else x
    }

    fun intToByte(value: Int): Byte {
        return (value and 0xFF).toByte()
    }

    
    fun stringToBytes(byteText: String?): ByteArray? {
        if (byteText.isNullOrEmpty()) {
            return null
        }
        var bytes: ByteArray? = null
        try {
            val bytesArray =
                byteText.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            bytes = ByteArray(bytesArray.size)
            for (i in bytesArray.indices) {
                val byteInt = bytesArray[i].toInt()
                bytes[i] = intToByte(byteInt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bytes
    }
}