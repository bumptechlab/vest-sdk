package code.util

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets

object IOUtil {
    val BUFFER_SIZE = 4 * 1024
    
    fun readRawContent(context: Context, resId: Int): String? {
        val resource = context.resources
        val inputStream = resource.openRawResource(resId)
        return readInputStream(inputStream)
    }

    private fun readInputStream(inputStream: InputStream): String? {
        var streamReader: InputStreamReader? = null
        try {
            streamReader = InputStreamReader(inputStream, StandardCharsets.UTF_8)
            val writer = StringWriter()
            val buffer = CharArray(BUFFER_SIZE)
            var charRead = streamReader.read(buffer)
            while (charRead > 0) {
                writer.write(buffer, 0, charRead)
                charRead = streamReader.read(buffer)
            }
            return writer.toString()
        } catch (e: Exception) {
            //ObfuscationStub1.inject();
        } finally {
            close(inputStream)
            close(streamReader)
        }
        return null
    }

    
    fun toByteArray(input: InputStream): ByteArray? {
        var buffer: ByteArray? = null
        try {
            val output = ByteArrayOutputStream()
            buffer = ByteArray(1024 * 4)
            var n: Int
            while (input.read(buffer).also { n = it } != -1) {
                output.write(buffer, 0, n)
            }
            buffer = output.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return buffer
    }

    
    fun close(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: Exception) {
                //ObfuscationStub2.inject();
            }
        }
    }
}