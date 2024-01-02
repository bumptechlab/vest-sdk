package code.util

object NumberUtil {

    fun parseInt(intText: String?): Int {
        var intValue = 0
        try {
            intValue = intText!!.toInt()
        } catch (_: Exception) {
        }
        return intValue
    }


    fun parseLong(longText: String?): Long {
        var longValue: Long = 0
        try {
            longValue = longText!!.toLong()
        } catch (_: Exception) {
        }
        return longValue
    }

    fun parseFloat(floatText: String?): Float {
        var floatValue = 0f
        try {
            floatValue = floatText!!.toFloat()
        } catch (_: Exception) {
        }
        return floatValue
    }
}