package code.sdk.zfutil

import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min

internal object ApkPackageUtil {
    /**
     * APK Signing Block Magic Code: magic “APK Sig Block 42” (16 bytes)
     * "APK Sig Block 42" : 41 50 4B 20 53 69 67 20 42 6C 6F 63 6B 20 34 32
     */
    const val APK_SIG_BLOCK_MAGIC_HI = 0x3234206b636f6c42L // LITTLE_ENDIAN, High
    const val APK_SIG_BLOCK_MAGIC_LO = 0x20676953204b5041L // LITTLE_ENDIAN, Low
    private const val APK_SIG_BLOCK_MIN_SIZE = 32

    /*
     The v2 signature of the APK is stored as an ID-value pair with ID 0x7109871a
     (https://source.android.com/security/apksigning/v2.html#apk-signing-block)
      */
    const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a

    /**
     * The padding in APK SIG BLOCK (V3 scheme introduced)
     * See https://android.googlesource.com/platform/tools/apksig/+/master/src/main/java/com/android/apksig/internal/apk/ApkSigningBlockUtils.java
     */
    const val VERITY_PADDING_BLOCK_ID = 0x42726577
    const val ANDROID_COMMON_PAGE_ALIGNMENT_BYTES = 4096

    // Extra Info Block ID
    const val EXTRA_INFO_BLOCK_ID = 0x71777777
    const val DEFAULT_CHARSET = "UTF-8"
    private const val ZIP_EOCD_REC_MIN_SIZE = 22
    private const val ZIP_EOCD_REC_SIG = 0x06054b50
    private const val UINT16_MAX_VALUE = 0xffff
    private const val ZIP_EOCD_COMMENT_LENGTH_FIELD_OFFSET = 20

    @Throws(IOException::class)
    fun getCommentLength(fileChannel: FileChannel): Long {
        // End of central directory record (EOCD)
        // Offset    Bytes     Description[23]
        // 0           4       End of central directory signature = 0x06054b50
        // 4           2       Number of this disk
        // 6           2       Disk where central directory starts
        // 8           2       Number of central directory records on this disk
        // 10          2       Total number of central directory records
        // 12          4       Size of central directory (bytes)
        // 16          4       Offset of start of central directory, relative to start of archive
        // 20          2       Comment length (n)
        // 22          n       Comment
        // For a zip with no archive comment, the
        // end-of-central-directory record will be 22 bytes long, so
        // we expect to find the EOCD marker 22 bytes from the end.
        val archiveSize = fileChannel.size()
        if (archiveSize < ZIP_EOCD_REC_MIN_SIZE) {
            throw IOException("APK too small for ZIP End of Central Directory (EOCD) record")
        }
        // ZIP End of Central Directory (EOCD) record is located at the very end of the ZIP archive.
        // The record can be identified by its 4-byte signature/magic which is located at the very
        // beginning of the record. A complication is that the record is variable-length because of
        // the comment field.
        // The algorithm for locating the ZIP EOCD record is as follows. We search backwards from
        // end of the buffer for the EOCD record signature. Whenever we find a signature, we check
        // the candidate record's comment length is such that the remainder of the record takes up
        // exactly the remaining bytes in the buffer. The search is bounded because the maximum
        // size of the comment field is 65535 bytes because the field is an unsigned 16-bit number.
        val maxCommentLength = min(archiveSize - ZIP_EOCD_REC_MIN_SIZE, UINT16_MAX_VALUE.toLong())
        val eocdWithEmptyCommentStartPosition = archiveSize - ZIP_EOCD_REC_MIN_SIZE
        for (expectedCommentLength in 0..maxCommentLength) {
            val eocdStartPos = eocdWithEmptyCommentStartPosition - expectedCommentLength
            val byteBuffer = ByteBuffer.allocate(4)
            fileChannel.position(eocdStartPos)
            fileChannel.read(byteBuffer)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            if (byteBuffer.getInt(0) == ZIP_EOCD_REC_SIG) {
                val commentLengthByteBuffer = ByteBuffer.allocate(2)
                fileChannel.position(eocdStartPos + ZIP_EOCD_COMMENT_LENGTH_FIELD_OFFSET)
                fileChannel.read(commentLengthByteBuffer)
                commentLengthByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                val actualCommentLength = commentLengthByteBuffer.getShort(0).toLong()
                if (actualCommentLength == expectedCommentLength) {
                    return actualCommentLength
                }
            }
        }
        throw IOException("ZIP End of Central Directory (EOCD) record not found")
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun findCentralDirStartOffset(
        fileChannel: FileChannel,
        commentLength: Long = getCommentLength(fileChannel)
    ): Long {
        // End of central directory record (EOCD)
        // Offset    Bytes     Description[23]
        // 0           4       End of central directory signature = 0x06054b50
        // 4           2       Number of this disk
        // 6           2       Disk where central directory starts
        // 8           2       Number of central directory records on this disk
        // 10          2       Total number of central directory records
        // 12          4       Size of central directory (bytes)
        // 16          4       Offset of start of central directory, relative to start of archive
        // 20          2       Comment length (n)
        // 22          n       Comment
        // For a zip with no archive comment, the
        // end-of-central-directory record will be 22 bytes long, so
        // we expect to find the EOCD marker 22 bytes from the end.
        val zipCentralDirectoryStart = ByteBuffer.allocate(4)
        zipCentralDirectoryStart.order(ByteOrder.LITTLE_ENDIAN)
        fileChannel.position(fileChannel.size() - commentLength - 6) // 6 = 2 (Comment length) + 4 (Offset of start of central directory, relative to start of archive)
        fileChannel.read(zipCentralDirectoryStart)
        return zipCentralDirectoryStart.getInt(0).toLong()
    }

    
    @Throws(IOException::class, SignatureNotFoundException::class)
    fun findApkSigningBlock(
        fileChannel: FileChannel
    ): Pair<ByteBuffer, Long> {
        val centralDirOffset = findCentralDirStartOffset(fileChannel)
        return findApkSigningBlock(fileChannel, centralDirOffset)
    }

    @Throws(IOException::class, SignatureNotFoundException::class)
    fun findApkSigningBlock(
        fileChannel: FileChannel, centralDirOffset: Long
    ): Pair<ByteBuffer, Long> {

        // Find the APK Signing Block. The block immediately precedes the Central Directory.

        // FORMAT:
        // OFFSET       DATA TYPE  DESCRIPTION
        // * @+0  bytes uint64:    size in bytes (excluding this field)
        // * @+8  bytes payload
        // * @-24 bytes uint64:    size in bytes (same as the one above)
        // * @-16 bytes uint128:   magic
        if (centralDirOffset < APK_SIG_BLOCK_MIN_SIZE) {
            throw SignatureNotFoundException(
                "APK too small for APK Signing Block. ZIP Central Directory offset: "
                        + centralDirOffset
            )
        }
        // Read the magic and offset in file from the footer section of the block:
        // * uint64:   size of block
        // * 16 bytes: magic
        fileChannel.position(centralDirOffset - 24)
        val footer = ByteBuffer.allocate(24)
        fileChannel.read(footer)
        footer.order(ByteOrder.LITTLE_ENDIAN)
        if (footer.getLong(8) != APK_SIG_BLOCK_MAGIC_LO || footer.getLong(16) != APK_SIG_BLOCK_MAGIC_HI) {
            throw SignatureNotFoundException(
                "No APK Signing Block before ZIP Central Directory"
            )
        }
        // Read and compare size fields
        val apkSigBlockSizeInFooter = footer.getLong(0)
        if (apkSigBlockSizeInFooter < footer.capacity() || apkSigBlockSizeInFooter > Int.MAX_VALUE - 8) {
            throw SignatureNotFoundException(
                "APK Signing Block size out of range: $apkSigBlockSizeInFooter"
            )
        }
        val totalSize = (apkSigBlockSizeInFooter + 8).toInt()
        val apkSigBlockOffset = centralDirOffset - totalSize
        if (apkSigBlockOffset < 0) {
            throw SignatureNotFoundException(
                "APK Signing Block offset out of range: $apkSigBlockOffset"
            )
        }
        fileChannel.position(apkSigBlockOffset)
        val apkSigBlock = ByteBuffer.allocate(totalSize)
        fileChannel.read(apkSigBlock)
        apkSigBlock.order(ByteOrder.LITTLE_ENDIAN)
        val apkSigBlockSizeInHeader = apkSigBlock.getLong(0)
        if (apkSigBlockSizeInHeader != apkSigBlockSizeInFooter) {
            throw SignatureNotFoundException(
                "APK Signing Block sizes in header and footer do not match: "
                        + apkSigBlockSizeInHeader + " vs " + apkSigBlockSizeInFooter
            )
        }
        return Pair.of(apkSigBlock, apkSigBlockOffset)
    }

    
    @Throws(SignatureNotFoundException::class)
    fun findIdValues(apkSigningBlock: ByteBuffer): Map<Int, ByteBuffer> {
        checkByteOrderLittleEndian(apkSigningBlock)
        // FORMAT:
        // OFFSET       DATA TYPE  DESCRIPTION
        // * @+0  bytes uint64:    size in bytes (excluding this field)
        // * @+8  bytes pairs
        // * @-24 bytes uint64:    size in bytes (same as the one above)
        // * @-16 bytes uint128:   magic
        val pairs = sliceFromTo(apkSigningBlock, 8, apkSigningBlock.capacity() - 24)
        val idValues: MutableMap<Int, ByteBuffer> = LinkedHashMap() // keep order
        var entryCount = 0
        while (pairs.hasRemaining()) {
            entryCount++
            if (pairs.remaining() < 8) {
                throw SignatureNotFoundException(
                    "Insufficient data to read size of APK Signing Block entry #$entryCount"
                )
            }
            val lenLong = pairs.getLong()
            if (lenLong < 4 || lenLong > Int.MAX_VALUE) {
                throw SignatureNotFoundException(
                    "APK Signing Block entry #" + entryCount
                            + " size out of range: " + lenLong
                )
            }
            val len = lenLong.toInt()
            val nextEntryPos = pairs.position() + len
            if (len > pairs.remaining()) {
                throw SignatureNotFoundException(
                    "APK Signing Block entry #" + entryCount + " size out of range: " + len
                            + ", available: " + pairs.remaining()
                )
            }
            val id = pairs.getInt()
            idValues[id] = getByteBuffer(pairs, len - 4)
            pairs.position(nextEntryPos)
        }
        return idValues
    }

    /**
     * Returns new byte buffer whose content is a shared subsequence of this buffer's content
     * between the specified start (inclusive) and end (exclusive) positions. As opposed to
     * [ByteBuffer.slice], the returned buffer's byte order is the same as the source
     * buffer's byte order.
     */
    private fun sliceFromTo(source: ByteBuffer, start: Int, end: Int): ByteBuffer {
        require(start >= 0) { "start: $start" }
        require(end >= start) { "end < start: $end < $start" }
        val capacity = source.capacity()
        require(end <= source.capacity()) { "end > capacity: $end > $capacity" }
        val originalLimit = source.limit()
        val originalPosition = source.position()
        return try {
            source.position(0)
            source.limit(end)
            source.position(start)
            val result = source.slice()
            result.order(source.order())
            result
        } finally {
            source.position(0)
            source.limit(originalLimit)
            source.position(originalPosition)
        }
    }

    /**
     * Relative *get* method for reading `size` number of bytes from the current
     * position of this buffer.
     *
     *
     *
     * This method reads the next `size` bytes at this buffer's current position,
     * returning them as a `ByteBuffer` with start set to 0, limit and capacity set to
     * `size`, byte order set to this buffer's byte order; and then increments the position by
     * `size`.
     */
    @Throws(BufferUnderflowException::class)
    private fun getByteBuffer(source: ByteBuffer, size: Int): ByteBuffer {
        require(size >= 0) { "size: $size" }
        val originalLimit = source.limit()
        val position = source.position()
        val limit = position + size
        if (limit < position || limit > originalLimit) {
            throw BufferUnderflowException()
        }
        source.limit(limit)
        return try {
            val result = source.slice()
            result.order(source.order())
            source.position(limit)
            result
        } finally {
            source.limit(originalLimit)
        }
    }

    private fun checkByteOrderLittleEndian(buffer: ByteBuffer) {
        require(buffer.order() == ByteOrder.LITTLE_ENDIAN) { "ByteBuffer byte order must be little endian" }
    }

    internal class Pair<A, B> private constructor(first: A, second: B) {
        val first: A
        val second: B

        init {
            this.first = first
            this.second = second
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + (first.hashCode())
            result = prime * result + (second.hashCode())
            return result
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) {
                return true
            }
            if (obj == null) {
                return false
            }
            if (javaClass != obj.javaClass) {
                return false
            }
            val other = obj as Pair<*, *>
            if (first == null) {
                if (other.first != null) {
                    return false
                }
            } else if (first != other.first) {
                return false
            }
            if (second == null) {
                if (other.second != null) {
                    return false
                }
            } else if (second != other.second) {
                return false
            }
            return true
        }

        companion object {
            fun <A, B> of(first: A, second: B): Pair<A, B> {
                return Pair(first, second)
            }
        }
    }
}
