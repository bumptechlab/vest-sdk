package code.util

import android.text.TextUtils
import java.text.DecimalFormat
import java.util.Locale
import java.util.regex.Pattern

/**
 * 字符串格式校验工具类
 *
 *
 * 校验一些常见的字符串格式，如邮箱、密码、手机号等
 */
class FormatUtil {
    var regx = ""
    var pattern = Pattern.compile(regx)
    var matcher = pattern.matcher("MM-3_3")
    var isMatch = matcher.matches()

    companion object {
       private val TAG = FormatUtil::class.java.simpleName

        /**
         * 正则表达式：验证用户名
         */
        private val REGEX_USERNAME = "^[a-zA-Z]\\w{5,20}$"

        /**
         * 正则表达式：验证密码
         */
        private val REGEX_PASSWORD = "^[a-zA-Z0-9]{4,12}$"

        /**
         * 正则表达式：验证手机号
         */
        private val REGEX_MOBILE = "^1[3456789]\\d{9}$"

        /**
         * 正则表达式：验证邮箱
         */
        private val REGEX_EMAIL =
            "^([a-z0-9A-Z]+[-|.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$"

        /**
         * 正则表达式：验证汉字
         */
        private val REGEX_CHINESE = "^[\u4e00-\u9fa5],{0,}$"

        /**
         * 正则表达式：验证身份证
         */
        private val REGEX_ID_CARD = "(^\\d{18}$)|(^\\d{15}$)"

        /**
         * 正则表达式：验证URL
         */
        private val REGEX_URL = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?"

        /**
         * 正则表达式：验证IP地址
         */
        private val REGEX_IP_ADDRESS = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)"

        /**
         * 正则表达式：验证字母和数字，加号，减号和下划线
         */
        private val LETTER_AND_NUMBERS = "^[A-Za-z0-9_\\-\\+]+$"

        /**
         * 校验用户名
         */
        fun isUsername(username: String): Boolean {
            return Pattern.matches(REGEX_USERNAME, username)
        }

        /**
         * 校验密码
         */
        fun isPassword(password: String): Boolean {
            return Pattern.matches(REGEX_PASSWORD, password)
        }

        /**
         * 校验手机号
         */
        fun isMobile(mobile: String): Boolean {
            return Pattern.matches(REGEX_MOBILE, mobile)
        }

        /**
         * 校验邮箱
         */
        fun isEmail(email: String): Boolean {
            return Pattern.matches(REGEX_EMAIL, email)
        }

        /**
         * 校验汉字
         */
        fun isChinese(chinese: String): Boolean {
            return Pattern.matches(REGEX_CHINESE, chinese)
        }

        /**
         * 校验身份证
         */
        fun isIDCard(idCard: String): Boolean {
            return Pattern.matches(REGEX_ID_CARD, idCard)
        }

        /**
         * 校验URL
         */
        fun isUrl(url: String): Boolean {
            return Pattern.matches(REGEX_URL, url)
        }

        /**
         * 校验IP地址
         */
        fun isIPAddress(ipAddress: String): Boolean {
            return Pattern.matches(REGEX_IP_ADDRESS, ipAddress)
        }

        /**
         * 验证字母和数字，加号，减号，下划线
         *
         * @param input
         * @return
         */
        fun isLetterAndNumbers(input: String): Boolean {
            return Pattern.matches(LETTER_AND_NUMBERS, input)
        }

        /**
         * 货币金额显示格式（保留小数点后两位）
         */
        fun formatCurrency(money: Float): String {
            return String.format(Locale.getDefault(), "%.2f", money)
        }

        /**
         * 手机号中间部分做星号隐藏
         */
        fun hideMobilePhone(phone: String): String {
            return phone.replace("(\\d{3})\\d{4}(\\d{4})".toRegex(), "$1****$2")
        }

        /**
         * 解析整型数字字符串
         */
        fun parseInt(numberText: String): Int {
            var number = 0
            try {
                number = numberText.toInt()
            } catch (e: Exception) {
            }
            return number
        }

        /**
         * 解析Long整型数字字符串
         */
        fun parseLong(numberText: String): Long {
            var number: Long = 0
            try {
                number = numberText.toLong()
            } catch (e: Exception) {
            }
            return number
        }

        /**
         * 解析浮点型数字字符串
         */
        fun parseFloat(numberText: String): Float {
            var number = 0f
            try {
                number = numberText.toFloat()
            } catch (e: Exception) {
            }
            return number
        }

        /**
         * 检查是否包含Emoji表情
         */
        fun containsEmoji(str: String): Boolean {
            val len = str.length
            for (i in 0 until len) {
                if (isEmojiCharacter(str[i])) {
                    return true
                }
            }
            return false
        }

        /**
         * 检查是否是Emoji表情字符
         */
        fun isEmojiCharacter(codePoint: Char): Boolean {
            return !(codePoint.code == 0x0 || codePoint.code == 0x9 || codePoint.code == 0xA || codePoint.code == 0xD || codePoint.code >= 0x20 && codePoint.code <= 0xD7FF || codePoint.code >= 0xE000 && codePoint.code <= 0xFFFD || codePoint.code >= 0x10000 && codePoint.code <= 0x10FFFF)
        }

        /**
         * 判断字符是否是中文
         *
         * @param c 字符
         * @return 是否是中文
         */
        fun isChinese(c: Char): Boolean {
            val ub = Character.UnicodeBlock.of(c)
            return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub === Character.UnicodeBlock.GENERAL_PUNCTUATION || ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
        }

        /**
         * 判断字符串是否是乱码
         *
         * @param text 字符串
         * @return 是否是乱码
         */
        fun isMessyCode(text: String): Boolean {
            var isMessyCode = false
            if (!TextUtils.isEmpty(text)) {
                val chars = text.trim { it <= ' ' }.toCharArray()
                for (i in chars.indices) {
                    val c = chars[i]
                    if (!isASCIILetters(c) && !isChinese(c)) {
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
        fun isASCIILetters(letter: Char): Boolean {
            return letter.code in 32..126
        }

        fun formatSize(size: Long): String {
            val KB: Long = 1024
            val MB = (1024 * 1024).toLong()
            val GB = (1024 * 1024 * 1024).toLong()
            val df = DecimalFormat("0.00") //格式化小数
            return if (size > GB) {
                df.format((size / GB.toFloat()).toDouble()) + "GB"
            } else if (size > MB) {
                df.format((size / MB.toFloat()).toDouble()) + "MB"
            } else if (size > KB) {
                df.format((size / KB.toFloat()).toDouble()) + "KB"
            } else {
                size.toString() + "B"
            }
        }
    }
}