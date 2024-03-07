package book.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import java.io.File
import java.net.URL
import java.text.DecimalFormat
import java.util.Locale


/**
 * 获取app的apk安装文件
 */
fun getAppApkFile(): File {
    val context: Context = AppGlobal.application!!
    return File(context.packageResourcePath)
}

/**
 * 支持http, https, ws等协议的域名解析
 */
fun String?.parseHost(): String {
    val url = this
    if (url.isNullOrEmpty()) {
        return ""
    }
    var host: String
    try {
        host = URL(url).host
    } catch (e: Exception) {
        //remove scheme
        val startIndex = url.indexOf("://")
        var urlTemp = url
        if (startIndex != -1) {
            urlTemp = url.substring(startIndex + 3)
        }
        //remove port and path
        var endIndex = urlTemp.indexOf(":")
        if (endIndex == -1) {
            endIndex = urlTemp.indexOf("/")
        }
        if (endIndex == -1) {
            endIndex = urlTemp.length
        }
        host = urlTemp.substring(0, endIndex)
    }
    return host
}

fun String?.parseInt(): Int {
    var intValue = 0
    try {
        intValue = this!!.toInt()
    } catch (_: Exception) {
    }
    return intValue
}


fun String?.parseLong(): Long {
    var longValue: Long = 0
    try {
        longValue = this!!.toLong()
    } catch (_: Exception) {
    }
    return longValue
}

fun String?.parseFloat(): Float {
    var floatValue = 0f
    try {
        floatValue = this!!.toFloat()
    } catch (_: Exception) {
    }
    return floatValue
}

/**
 * 校验用户名
 */
fun CharSequence.isUsername(): Boolean {
    return "^[a-zA-Z]\\w{5,20}$".toRegex().matches(this)
}

/**
 * 校验密码
 */
fun CharSequence.isPassword(): Boolean {
    return "^[a-zA-Z0-9]{4,12}$".toRegex().matches(this)
}

/**
 * 校验手机号
 */
fun CharSequence.isMobile(): Boolean {
    return "^1[3456789]\\d{9}$".toRegex().matches(this)
}

/**
 * 校验邮箱
 */
fun CharSequence.isEmail(): Boolean {
    return "^([a-z0-9A-Z]+[-|.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$".toRegex()
        .matches(this)
}

/**
 * 校验汉字
 */
fun CharSequence.isChinese(chinese: String): Boolean {
    return "^[\u4e00-\u9fa5],{0,}$".toRegex().matches(this)
}

/**
 * 校验身份证
 */
fun CharSequence.isIDCard(idCard: String): Boolean {
    return "(^\\d{18}$)|(^\\d{15}$)".toRegex().matches(this)
}

/**
 * 校验URL
 */
fun CharSequence.isUrl(): Boolean {
    return "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?".toRegex().matches(this)
}

/**
 * 校验IP地址
 */
fun CharSequence.isIPAddress(): Boolean {
    return "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)".toRegex().matches(this)
}

/**
 * 验证字母和数字，加号，减号，下划线
 *
 * @param input
 * @return
 */
fun CharSequence.isLetterAndNumbers(): Boolean {
    return "^[A-Za-z0-9_\\-\\+]+$".toRegex().matches(this)
}

/**
 * 货币金额显示格式（保留小数点后两位）
 */
fun Float.toCurrency(): String {
    return String.format(Locale.getDefault(), "%.2f", this)
}

/**
 * 手机号中间部分做星号隐藏
 */
fun CharSequence.hideMobilePhone(): String {
    return this.replace("(\\d{3})\\d{4}(\\d{4})".toRegex(), "$1****$2")
}

/**
 * 检查是否包含Emoji表情
 */
fun CharSequence.containsEmoji(): Boolean {
    val len = this.length
    for (i in 0 until len) {
        if (this[i].isEmojiCharacter()) {
            return true
        }
    }
    return false
}

/**
 * 检查是否是Emoji表情字符
 */
fun Char.isEmojiCharacter(): Boolean {
    return !(this.code == 0x0 || this.code == 0x9 || this.code == 0xA || this.code == 0xD || this.code >= 0x20 && this.code <= 0xD7FF || this.code >= 0xE000 && this.code <= 0xFFFD || this.code >= 0x10000 && this.code <= 0x10FFFF)
}

/**
 * 判断字符是否是中文
 *
 * @param c 字符
 * @return 是否是中文
 */
fun Char.isChinese(): Boolean {
    val ub = Character.UnicodeBlock.of(this)
    return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub === Character.UnicodeBlock.GENERAL_PUNCTUATION || ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
}

/**
 * 判断字符串是否是乱码
 *
 * @param text 字符串
 * @return 是否是乱码
 */
fun String.isMessyCode(): Boolean {
    var isMessyCode = false
    if (!TextUtils.isEmpty(this)) {
        val chars = this.trim() { it <= ' ' }.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            if (!c.isASCIILetters() && !c.isChinese()) {
                isMessyCode = true
                break
            }
        }
    }
    return isMessyCode
}

/**
 * 是否为ASCII表里面的文字
 *
 * @param letter 字符串
 * @return 是否是乱码
 */
fun Char.isASCIILetters(): Boolean {
    return this.code in 32..126
}

fun Long.formatSize(): String {
    val KB: Long = 1024
    val MB = (1024 * 1024).toLong()
    val GB = (1024 * 1024 * 1024).toLong()
    val df = DecimalFormat("0.00") //格式化小数
    return if (this > GB) {
        df.format((this / GB.toFloat()).toDouble()) + "GB"
    } else if (this > MB) {
        df.format((this / MB.toFloat()).toDouble()) + "MB"
    } else if (this > KB) {
        df.format((this / KB.toFloat()).toDouble()) + "KB"
    } else {
        this.toString() + "B"
    }
}

var isMainThread: Boolean = Looper.getMainLooper() === Looper.myLooper()


