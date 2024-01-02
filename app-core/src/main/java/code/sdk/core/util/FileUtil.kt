package code.sdk.core.util

import android.content.Context
import android.graphics.Bitmap
import code.util.AppGlobal.getApplication
import code.util.IOUtil.close
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object FileUtil {
   private val TAG = FileUtil::class.java.simpleName
    private const val READ_CACHE_LENGTH = 8192
    fun getSelfApkFile(): File {
        val context: Context = getApplication()
        return File(context.packageResourcePath)
    }

    fun ensureFile(file: File?) {
        if (file != null && !file.exists()) {
            ensureDirectory(file.parentFile)
            try {
                file.createNewFile()
            } catch (e: IOException) {
                //ObfuscationStub2.inject();
            }
        }
    }

    fun ensureDirectory(directory: File?) {
        if (directory != null && !directory.exists()) {
            directory.mkdirs()
        } else {
            //ObfuscationStub3.inject();
        }
    }

    @Throws(IOException::class)
    fun copyFile(src: File?, dst: File) {
        copyFile(FileInputStream(src), dst)
    }

    @Throws(IOException::class)
    private fun copyFile(src: InputStream, dst: File) {
        var ou: BufferedOutputStream? = null
        try {
            ou = BufferedOutputStream(FileOutputStream(dst))
            val buffer = ByteArray(READ_CACHE_LENGTH)
            var read: Int
            while (src.read(buffer).also { read = it } != -1) {
                ou.write(buffer, 0, read)
            }
        } catch (e: Exception) {
            //ObfuscationStub4.inject();
        } finally {
            close(src)
            close(ou)
        }
    }

    fun saveBitmap(path: String, bitmap: Bitmap) {
        val file = File(path)
        saveBitmap(file, bitmap)
    }

    fun saveBitmap(file: File?, bitmap: Bitmap) {
        var out: FileOutputStream? = null
        try {
            ensureFile(file)
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } catch (e: Exception) {
            //ObfuscationStub1.inject();
        } finally {
            close(out)
        }
    }

    fun readFile(path: String): String? {
        val file = File(path)
        return readFile(file)
    }

    fun readFile(file: File?): String? {
        var inputStream: InputStream? = null
        var streamReader: InputStreamReader? = null
        var bufferedReader: BufferedReader? = null
        try {
            inputStream = FileInputStream(file)
            streamReader = InputStreamReader(inputStream, StandardCharsets.UTF_8)
            bufferedReader = BufferedReader(streamReader)
            val builder = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                builder.append(line)
            }
            return builder.toString()
        } catch (e: Exception) {
            //ObfuscationStub2.inject();
        } finally {
            close(inputStream)
            close(streamReader)
            close(bufferedReader)
        }
        return null
    }

    fun readFileWithBytes(file: File?): ByteArray {
        var inputStream: BufferedInputStream? = null
        var out: ByteArrayOutputStream? = null
        try {
            inputStream = BufferedInputStream(FileInputStream(file))
            out = ByteArrayOutputStream()
            val buffer = ByteArray(READ_CACHE_LENGTH)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        } catch (e: Exception) {
            //ObfuscationStub3.inject();
        } finally {
            close(inputStream)
            close(out)
        }
        return byteArrayOf()
    }

    fun writeFile(path: String, content: String?): Boolean {
        val file = File(path)
        return writeFile(file, content)
    }

    fun writeFile(file: File?, content: String?): Boolean {
        var outputStream: OutputStream? = null
        var streamWriter: OutputStreamWriter? = null
        var bufferedWriter: BufferedWriter? = null
        var success = false
        try {
            ensureFile(file)
            outputStream = FileOutputStream(file)
            streamWriter = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
            bufferedWriter = BufferedWriter(streamWriter)
            bufferedWriter.write(content)
            bufferedWriter.flush()
            success = true
        } catch (e: Exception) {
            //ObfuscationStub4.inject();
        } finally {
            close(outputStream)
            close(streamWriter)
            close(bufferedWriter)
        }
        return success
    }

    fun writeFileWithBytes(file: File?, bytes: ByteArray?) {
        var out: FileOutputStream? = null
        try {
            //ObfuscationStub1.inject();
            out = FileOutputStream(file)
            out.write(bytes)
        } catch (e: Exception) {
            //ObfuscationStub5.inject();
        } finally {
            close(out)
        }
    }

    fun deleteFile(file: File): Boolean {
        val files = file.listFiles()
        if (!files.isNullOrEmpty()) {
            for (deleteFile in files) {
                if (deleteFile.isDirectory) {
                    if (!deleteFile(deleteFile)) {
                        return false
                    }
                } else {
                    if (!deleteFile.delete()) {
                        return false
                    }
                }
            }
        }
        //ObfuscationStub6.inject();
        return file.delete()
    }
}
