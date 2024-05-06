package poetry.util

import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset

fun Closeable?.safeClose() {
    if (this != null) {
        try {
            this.close()
        } catch (_: Exception) {
        }
    }
}

fun InputStream?.readText(charset: Charset = Charsets.UTF_8): String {
    val bytes = this?.readBytes()
    return if (bytes != null) String(bytes, charset) else ""
}

fun readRawResource(resId: Int): String {
    val resource = AppGlobal.application!!.resources
    val inputStream = resource.openRawResource(resId)
    return inputStream.readText()
}
