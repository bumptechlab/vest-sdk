package book.sdk.walle

import book.sdk.walle.ApkPackageUtil.findApkSigningBlock
import book.sdk.walle.ApkPackageUtil.findIdValues
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Arrays

object PayloadReader {
    /**
     * get string (UTF-8) by id
     *
     * @param apkFile apk file
     * @return null if not found
     */
    fun getString(apkFile: File?, id: Int): String? {
        val bytes = PayloadReader[apkFile, id] ?: return null
        try {
            return String(bytes, charset(ApkPackageUtil.DEFAULT_CHARSET))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * get bytes by id <br></br>
     *
     * @param apkFile apk file
     * @param id      id
     * @return bytes
     */
    operator fun get(apkFile: File?, id: Int): ByteArray? {
        val idValues = getAll(apkFile) ?: return null
        val byteBuffer = idValues[id] ?: return null
        return getBytes(byteBuffer)
    }

    /**
     * get data from byteBuffer
     *
     * @param byteBuffer buffer
     * @return useful data
     */
    private fun getBytes(byteBuffer: ByteBuffer): ByteArray {
        val array = byteBuffer.array()
        val arrayOffset = byteBuffer.arrayOffset()
        return Arrays.copyOfRange(
            array, arrayOffset + byteBuffer.position(),
            arrayOffset + byteBuffer.limit()
        )
    }

    /**
     * get all custom (id, buffer) <br></br>
     * Note: get final from byteBuffer, please use [getBytes][PayloadReader.getBytes]
     *
     * @param apkFile apk file
     * @return all custom (id, buffer)
     */
    private fun getAll(apkFile: File?): Map<Int, ByteBuffer>? {
        var idValues: Map<Int, ByteBuffer>? = null
        try {
            var randomAccessFile: RandomAccessFile? = null
            var fileChannel: FileChannel? = null
            try {
                randomAccessFile = RandomAccessFile(apkFile, "r")
                fileChannel = randomAccessFile.channel
                val apkSigningBlock2 = findApkSigningBlock(fileChannel).first
                idValues = findIdValues(apkSigningBlock2)
            } catch (ignore: IOException) {
            } finally {
                try {
                    fileChannel?.close()
                } catch (ignore: IOException) {
                }
                try {
                    randomAccessFile?.close()
                } catch (ignore: IOException) {
                }
            }
        } catch (ignore: SignatureNotFoundException) {
        }
        return idValues
    }
}
