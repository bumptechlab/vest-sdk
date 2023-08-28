package code.util;

public class NumberUtil {

    public static int parseInt(String intText) {
        int intValue = 0;
        try {
            intValue = Integer.parseInt(intText);
        } catch (Exception e) {
        }
        return intValue;
    }

    public static long parseLong(String longText) {
        long longValue = 0;
        try {
            longValue = Long.parseLong(longText);
        } catch (Exception e) {
        }
        return longValue;
    }

    public static float parseFloat(String floatText) {
        float floatValue = 0;
        try {
            floatValue = Float.parseFloat(floatText);
        } catch (Exception e) {
        }
        return floatValue;
    }
}
