package code.util;

import android.text.TextUtils;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串格式校验工具类
 * <p>
 * 校验一些常见的字符串格式，如邮箱、密码、手机号等
 */

public class FormatUtil {
    public static final String TAG = FormatUtil.class.getSimpleName();

    /**
     * 正则表达式：验证用户名
     */
    private static final String REGEX_USERNAME = "^[a-zA-Z]\\w{5,20}$";

    /**
     * 正则表达式：验证密码
     */
    private static final String REGEX_PASSWORD = "^[a-zA-Z0-9]{4,12}$";

    /**
     * 正则表达式：验证手机号
     */
    private static final String REGEX_MOBILE = "^1[3456789]\\d{9}$";

    /**
     * 正则表达式：验证邮箱
     */
    private static final String REGEX_EMAIL = "^([a-z0-9A-Z]+[-|.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";

    /**
     * 正则表达式：验证汉字
     */
    private static final String REGEX_CHINESE = "^[\u4e00-\u9fa5],{0,}$";

    /**
     * 正则表达式：验证身份证
     */
    private static final String REGEX_ID_CARD = "(^\\d{18}$)|(^\\d{15}$)";

    /**
     * 正则表达式：验证URL
     */
    private static final String REGEX_URL = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";

    /**
     * 正则表达式：验证IP地址
     */
    private static final String REGEX_IP_ADDRESS = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)";

    /**
     * 正则表达式：验证字母和数字，加号，减号和下划线
     */
    private static final String LETTER_AND_NUMBERS = "^[A-Za-z0-9_\\-\\+]+$";

    String regx = "";
    Pattern pattern = Pattern.compile(regx);
    Matcher matcher = pattern.matcher("MM-3_3");
    boolean isMatch = matcher.matches();

    /**
     * 校验用户名
     */
    public static boolean isUsername(String username) {
        return Pattern.matches(REGEX_USERNAME, username);
    }

    /**
     * 校验密码
     */
    public static boolean isPassword(String password) {
        return Pattern.matches(REGEX_PASSWORD, password);
    }

    /**
     * 校验手机号
     */
    public static boolean isMobile(String mobile) {
        return Pattern.matches(REGEX_MOBILE, mobile);
    }

    /**
     * 校验邮箱
     */
    public static boolean isEmail(String email) {
        return Pattern.matches(REGEX_EMAIL, email);
    }

    /**
     * 校验汉字
     */
    public static boolean isChinese(String chinese) {
        return Pattern.matches(REGEX_CHINESE, chinese);
    }

    /**
     * 校验身份证
     */
    public static boolean isIDCard(String idCard) {
        return Pattern.matches(REGEX_ID_CARD, idCard);
    }

    /**
     * 校验URL
     */
    public static boolean isUrl(String url) {
        return Pattern.matches(REGEX_URL, url);
    }

    /**
     * 校验IP地址
     */
    public static boolean isIPAddress(String ipAddress) {
        return Pattern.matches(REGEX_IP_ADDRESS, ipAddress);
    }

    /**
     * 验证字母和数字，加号，减号，下划线
     *
     * @param input
     * @return
     */
    public static boolean isLetterAndNumbers(String input) {
        return Pattern.matches(LETTER_AND_NUMBERS, input);
    }

    /**
     * 货币金额显示格式（保留小数点后两位）
     */
    public static String formatCurrency(float money) {
        return String.format(Locale.getDefault(), "%.2f", money);
    }

    /**
     * 手机号中间部分做星号隐藏
     */
    public static String hideMobilePhone(String phone) {
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 解析整型数字字符串
     */
    public static int parseInt(String numberText) {
        int number = 0;
        try {
            number = Integer.parseInt(numberText);
        } catch (Exception e) {
        }
        return number;
    }

    /**
     * 解析Long整型数字字符串
     */
    public static long parseLong(String numberText) {
        long number = 0;
        try {
            number = Long.parseLong(numberText);
        } catch (Exception e) {
        }
        return number;
    }

    /**
     * 解析浮点型数字字符串
     */
    public static float parseFloat(String numberText) {
        float number = 0f;
        try {
            number = Float.parseFloat(numberText);
        } catch (Exception e) {
        }
        return number;
    }

    /**
     * 检查是否包含Emoji表情
     */
    public static boolean containsEmoji(String str) {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (isEmojiCharacter(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否是Emoji表情字符
     */
    public static boolean isEmojiCharacter(char codePoint) {
        return !((codePoint == 0x0) ||
                (codePoint == 0x9) ||
                (codePoint == 0xA) ||
                (codePoint == 0xD) ||
                ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||
                ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF)));
    }

    /**
     * 判断字符是否是中文
     *
     * @param c 字符
     * @return 是否是中文
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    /**
     * 判断字符串是否是乱码
     *
     * @param text 字符串
     * @return 是否是乱码
     */
    public static boolean isMessyCode(String text) {
        boolean isMessyCode = false;
        if (!TextUtils.isEmpty(text)) {
            char[] chars = text.trim().toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (!isASCIILetters(c) && !isChinese(c)) {
                    isMessyCode = true;
                    break;
                }
            }
        }
        return isMessyCode;
    }

    /**
     * 是否为ASCII表里面的文字
     *
     * @param text 字符串
     * @return 是否是乱码
     */
    public static boolean isASCIILetters(char letter) {
        return letter >= 32 && letter <= 126;
    }

    public static String formatSize(long size) {
        long KB = 1024;
        long MB = 1024 * 1024;
        long GB = 1024 * 1024 * 1024;
        DecimalFormat df = new DecimalFormat("0.00");//格式化小数
        String formattedSize = "";
        if (size > GB) {
            formattedSize = df.format(size / (float) GB) + "GB";
        } else if (size > MB) {
            formattedSize = df.format(size / (float) MB) + "MB";
        } else if (size > KB) {
            formattedSize = df.format(size / (float) KB) + "KB";
        } else {
            formattedSize = size + "B";
        }
        return formattedSize;
    }
}
