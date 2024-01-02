/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package code.sdk.image

import code.sdk.image.CharacterSetECI.Companion.getCharacterSetECIByName
import java.io.UnsupportedEncodingException
import kotlin.math.max

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
object Encoder {
   private val TAG = Encoder::class.java.simpleName
    private val ALPHANUMERIC_TABLE = intArrayOf(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
        36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
        -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
        25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1
    )
    private val DEFAULT_BYTE_MODE_ENCODING = "ISO-8859-1"
    private fun calculateMaskPenalty(matrix: ByteMatrix): Int {
        return (MaskUtil.applyMaskPenaltyRule1(matrix)
                + MaskUtil.applyMaskPenaltyRule2(matrix)
                + MaskUtil.applyMaskPenaltyRule3(matrix)
                + MaskUtil.applyMaskPenaltyRule4(matrix))
    }

    /**
     * @param content text to encode
     * @param ecLevel error correction level to use
     * @return [QRCode] representing the encoded QR code
     * @throws WriterException if encoding can't succeed, because of for example invalid content
     * or configuration
     */
    @Throws(WriterException::class)
    fun encode(
        content: String,
        ecLevel: ErrorCorrectionLevel,
        hints: Map<EncodeHintType?, *>? = null
    ): QRCode {
        var encoding = DEFAULT_BYTE_MODE_ENCODING
        if (hints != null && hints.containsKey(EncodeHintType.CHARACTER_SET)) {
            encoding = hints[EncodeHintType.CHARACTER_SET].toString()
        }
        val mode = chooseMode(content, encoding)
        val headerBits = BitArray()
        if (mode == Mode.BYTE && DEFAULT_BYTE_MODE_ENCODING != encoding) {
            val eci = getCharacterSetECIByName(encoding)
            if (eci != null) {
                appendECI(eci, headerBits)
            }
        }
        appendModeInfo(mode, headerBits)
        val dataBits = BitArray()
        appendBytes(content, mode, dataBits, encoding)
        val version: Version
        if (hints != null && hints.containsKey(EncodeHintType.QR_VERSION)) {
            val versionNumber = hints[EncodeHintType.QR_VERSION].toString().toInt()
            version = Version.getVersionForNumber(versionNumber)
            val bitsNeeded = calculateBitsNeeded(mode, headerBits, dataBits, version)
            if (!willFit(bitsNeeded, version, ecLevel)) {
                throw WriterException("Data too big for requested version")
            }
        } else {
            version = recommendVersion(ecLevel, mode, headerBits, dataBits)
        }
        val headerAndDataBits = BitArray()
        headerAndDataBits.appendBitArray(headerBits)
        val numLetters = if (mode == Mode.BYTE) dataBits.sizeInBytes else content.length
        appendLengthInfo(numLetters, version, mode, headerAndDataBits)
        headerAndDataBits.appendBitArray(dataBits)
        val ecBlocks = version.getECBlocksForLevel(ecLevel)
        val numDataBytes = version.totalCodewords - ecBlocks.totalECCodewords
        terminateBits(numDataBytes, headerAndDataBits)
        val finalBits = interleaveWithECBytes(
            headerAndDataBits,
            version.totalCodewords,
            numDataBytes,
            ecBlocks.numBlocks
        )
        val qrCode = QRCode()
        qrCode.ecLevel = ecLevel
        qrCode.mode = mode
        qrCode.version = version
        val dimension = version.getDimensionForVersion()
        val matrix = ByteMatrix(dimension, dimension)
        val maskPattern = chooseMaskPattern(finalBits, ecLevel, version, matrix)
        qrCode.maskPattern = maskPattern
        MatrixUtil.buildMatrix(finalBits, ecLevel, version, maskPattern, matrix)
        qrCode.matrix = matrix
        return qrCode
    }

    /**
     * Decides the smallest version of QR code that will contain all of the provided data.
     *
     * @throws WriterException if the data cannot fit in any version
     */
    @Throws(WriterException::class)
    private fun recommendVersion(
        ecLevel: ErrorCorrectionLevel,
        mode: Mode,
        headerBits: BitArray,
        dataBits: BitArray
    ): Version {
        val provisionalBitsNeeded =
            calculateBitsNeeded(mode, headerBits, dataBits, Version.getVersionForNumber(1))
        val provisionalVersion = chooseVersion(provisionalBitsNeeded, ecLevel)
        val bitsNeeded = calculateBitsNeeded(mode, headerBits, dataBits, provisionalVersion)
        return chooseVersion(bitsNeeded, ecLevel)
    }

    private fun calculateBitsNeeded(
        mode: Mode,
        headerBits: BitArray,
        dataBits: BitArray,
        version: Version
    ): Int {
        return headerBits.size + mode.getCharacterCountBits(version) + dataBits.size
    }

    /**
     * @return the code point of the table used in alphanumeric mode or
     * -1 if there is no corresponding code in the table.
     */
    fun getAlphanumericCode(code: Int): Int {
        return if (code < ALPHANUMERIC_TABLE.size) {
            ALPHANUMERIC_TABLE[code]
        } else -1
    }

    /**
     * Choose the best mode by examining the content. Note that 'encoding' is used as a hint;
     * if it is Shift_JIS, and the input is only double-byte Kanji, then we return [Mode.KANJI].
     */
    private fun chooseMode(content: String, encoding: String): Mode {
        if ("Shift_JIS" == encoding && isOnlyDoubleByteKanji(content)) {
            return Mode.KANJI
        }
        var hasNumeric = false
        var hasAlphanumeric = false
        for (element in content) {
            if (element in '0'..'9') {
                hasNumeric = true
            } else if (getAlphanumericCode(element.code) != -1) {
                hasAlphanumeric = true
            } else {
                return Mode.BYTE
            }
        }
        if (hasAlphanumeric) {
            return Mode.ALPHANUMERIC
        }
        return if (hasNumeric) {
            Mode.NUMERIC
        } else Mode.BYTE
    }

    private fun isOnlyDoubleByteKanji(content: String): Boolean {
        val bytes = try {
            content.toByteArray(charset("Shift_JIS"))
        } catch (ignored: UnsupportedEncodingException) {
            return false
        }
        val length = bytes.size
        if (length % 2 != 0) {
            return false
        }
        var i = 0
        while (i < length) {
            val byte1 = bytes[i].toInt() and 0xFF
            if ((byte1 < 0x81 || byte1 > 0x9F) && (byte1 < 0xE0 || byte1 > 0xEB)) {
                return false
            }
            i += 2
        }
        return true
    }

    @Throws(WriterException::class)
    private fun chooseMaskPattern(
        bits: BitArray,
        ecLevel: ErrorCorrectionLevel,
        version: Version,
        matrix: ByteMatrix
    ): Int {
        var minPenalty = Int.MAX_VALUE // Lower penalty is better.
        var bestMaskPattern = -1
        for (maskPattern in 0 until QRCode.NUM_MASK_PATTERNS) {
            MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix)
            val penalty = calculateMaskPenalty(matrix)
            if (penalty < minPenalty) {
                minPenalty = penalty
                bestMaskPattern = maskPattern
            }
        }
        return bestMaskPattern
    }

    @Throws(WriterException::class)
    private fun chooseVersion(numInputBits: Int, ecLevel: ErrorCorrectionLevel): Version {
        for (versionNum in 1..40) {
            val version = Version.getVersionForNumber(versionNum)
            if (willFit(numInputBits, version, ecLevel)) {
                return version
            }
        }
        throw WriterException("Data too big")
    }

    /**
     * @return true if the number of input bits will fit in a code with the specified version and
     * error correction level.
     */
    private fun willFit(
        numInputBits: Int,
        version: Version,
        ecLevel: ErrorCorrectionLevel
    ): Boolean {
        val numBytes = version.totalCodewords
        val ecBlocks = version.getECBlocksForLevel(ecLevel)
        val numEcBytes = ecBlocks.totalECCodewords
        val numDataBytes = numBytes - numEcBytes
        val totalInputBytes = (numInputBits + 7) / 8
        return numDataBytes >= totalInputBytes
    }

    /**
     * Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
     */
    @Throws(WriterException::class)
    fun terminateBits(numDataBytes: Int, bits: BitArray) {
        val capacity = numDataBytes * 8
        if (bits.size > capacity) {
            throw WriterException(
                "data bits cannot fit in the QR Code" + bits.size + " > " +
                        capacity
            )
        }
        run {
            var i = 0
            while (i < 4 && bits.size < capacity) {
                bits.appendBit(false)
                ++i
            }
        }
        val numBitsInLastByte = bits.size and 0x07
        if (numBitsInLastByte > 0) {
            for (i in numBitsInLastByte..7) {
                bits.appendBit(false)
            }
        }
        val numPaddingBytes = numDataBytes - bits.sizeInBytes
        for (i in 0 until numPaddingBytes) {
            bits.appendBits(if (i and 0x01 == 0) 0xEC else 0x11, 8)
        }
        if (bits.size != capacity) {
            throw WriterException("Bits size does not equal capacity")
        }
    }

    /**
     * Get number of data bytes and number of error correction bytes for block id "blockID". Store
     * the result in "numDataBytesInBlock", and "numECBytesInBlock". See table 12 in 8.5.1 of
     * JISX0510:2004 (p.30)
     */
    @Throws(WriterException::class)
    fun getNumDataBytesAndNumECBytesForBlockID(
        numTotalBytes: Int,
        numDataBytes: Int,
        numRSBlocks: Int,
        blockID: Int,
        numDataBytesInBlock: IntArray,
        numECBytesInBlock: IntArray
    ) {
        if (blockID >= numRSBlocks) {
            throw WriterException("Block ID too large")
        }
        val numRsBlocksInGroup2 = numTotalBytes % numRSBlocks
        val numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2
        val numTotalBytesInGroup1 = numTotalBytes / numRSBlocks
        val numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1
        val numDataBytesInGroup1 = numDataBytes / numRSBlocks
        val numDataBytesInGroup2 = numDataBytesInGroup1 + 1
        val numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1
        val numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2
        if (numEcBytesInGroup1 != numEcBytesInGroup2) {
            throw WriterException("EC bytes mismatch")
        }
        if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
            throw WriterException("RS blocks mismatch")
        }
        if (numTotalBytes !=
            (numDataBytesInGroup1 + numEcBytesInGroup1) *
            numRsBlocksInGroup1 + (numDataBytesInGroup2 + numEcBytesInGroup2) * numRsBlocksInGroup2
        ) {
            throw WriterException("Total bytes mismatch")
        }
        if (blockID < numRsBlocksInGroup1) {
            numDataBytesInBlock[0] = numDataBytesInGroup1
            numECBytesInBlock[0] = numEcBytesInGroup1
        } else {
            numDataBytesInBlock[0] = numDataBytesInGroup2
            numECBytesInBlock[0] = numEcBytesInGroup2
        }
    }

    /**
     * Interleave "bits" with corresponding error correction bytes. On success, store the result in
     * "result". The interleave rule is complicated. See 8.6 of JISX0510:2004 (p.37) for details.
     */
    @Throws(WriterException::class)
    fun interleaveWithECBytes(
        bits: BitArray,
        numTotalBytes: Int,
        numDataBytes: Int,
        numRSBlocks: Int
    ): BitArray {
        if (bits.sizeInBytes != numDataBytes) {
            throw WriterException("Number of bits and data bytes does not match")
        }
        var dataBytesOffset = 0
        var maxNumDataBytes = 0
        var maxNumEcBytes = 0
        val blocks: MutableCollection<BlockPair> = ArrayList(numRSBlocks)
        for (i in 0 until numRSBlocks) {
            val numDataBytesInBlock = IntArray(1)
            val numEcBytesInBlock = IntArray(1)
            getNumDataBytesAndNumECBytesForBlockID(
                numTotalBytes, numDataBytes, numRSBlocks, i,
                numDataBytesInBlock, numEcBytesInBlock
            )
            val size = numDataBytesInBlock[0]
            val dataBytes = ByteArray(size)
            bits.toBytes(8 * dataBytesOffset, dataBytes, 0, size)
            val ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0])
            blocks.add(BlockPair(dataBytes, ecBytes))
            maxNumDataBytes = max(maxNumDataBytes, size)
            maxNumEcBytes = max(maxNumEcBytes, ecBytes.size)
            dataBytesOffset += numDataBytesInBlock[0]
        }
        if (numDataBytes != dataBytesOffset) {
            throw WriterException("Data bytes does not match offset")
        }
        val result = BitArray()
        for (i in 0 until maxNumDataBytes) {
            for (block in blocks) {
                val dataBytes = block.dataBytes
                if (i < dataBytes.size) {
                    result.appendBits(dataBytes[i].toInt(), 8)
                }
            }
        }
        for (i in 0 until maxNumEcBytes) {
            for (block in blocks) {
                val ecBytes = block.errorCorrectionBytes
                if (i < ecBytes.size) {
                    result.appendBits(ecBytes[i].toInt(), 8)
                }
            }
        }
        if (numTotalBytes != result.sizeInBytes) {  // Should be same.
            throw WriterException(
                "Interleaving error: " + numTotalBytes + " and " +
                        result.sizeInBytes + " differ."
            )
        }
        return result
    }

    fun generateECBytes(dataBytes: ByteArray, numEcBytesInBlock: Int): ByteArray {
        val numDataBytes = dataBytes.size
        val toEncode = IntArray(numDataBytes + numEcBytesInBlock)
        for (i in 0 until numDataBytes) {
            toEncode[i] = dataBytes[i].toInt() and 0xFF
        }
        ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256).encode(toEncode, numEcBytesInBlock)
        val ecBytes = ByteArray(numEcBytesInBlock)
        for (i in 0 until numEcBytesInBlock) {
            ecBytes[i] = toEncode[numDataBytes + i].toByte()
        }
        return ecBytes
    }

    /**
     * Append mode info. On success, store the result in "bits".
     */
    fun appendModeInfo(mode: Mode, bits: BitArray) {
        bits.appendBits(mode.bits, 4)
    }

    /**
     * Append length info. On success, store the result in "bits".
     */
    @Throws(WriterException::class)
    fun appendLengthInfo(numLetters: Int, version: Version, mode: Mode, bits: BitArray) {
        val numBits = mode.getCharacterCountBits(version)
        if (numLetters >= 1 shl numBits) {
            throw WriterException(numLetters.toString() + " is bigger than " + ((1 shl numBits) - 1))
        }
        bits.appendBits(numLetters, numBits)
    }

    /**
     * Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits".
     */
    @Throws(WriterException::class)
    fun appendBytes(
        content: String,
        mode: Mode,
        bits: BitArray,
        encoding: String?
    ) {
        when (mode) {
            Mode.NUMERIC -> appendNumericBytes(content, bits)
            Mode.ALPHANUMERIC -> appendAlphanumericBytes(content, bits)
            Mode.BYTE -> append8BitBytes(content, bits, encoding)
            Mode.KANJI -> appendKanjiBytes(content, bits)
            else -> throw WriterException("Invalid mode: $mode")
        }
    }

    fun appendNumericBytes(content: CharSequence, bits: BitArray) {
        val length = content.length
        var i = 0
        while (i < length) {
            val num1 = content[i].code - '0'.code
            if (i + 2 < length) {
                val num2 = content[i + 1].code - '0'.code
                val num3 = content[i + 2].code - '0'.code
                bits.appendBits(num1 * 100 + num2 * 10 + num3, 10)
                i += 3
            } else if (i + 1 < length) {
                val num2 = content[i + 1].code - '0'.code
                bits.appendBits(num1 * 10 + num2, 7)
                i += 2
            } else {
                bits.appendBits(num1, 4)
                i++
            }
        }
    }

    @Throws(WriterException::class)
    fun appendAlphanumericBytes(content: CharSequence, bits: BitArray) {
        val length = content.length
        var i = 0
        while (i < length) {
            val code1 = getAlphanumericCode(content[i].code)
            if (code1 == -1) {
                throw WriterException()
            }
            if (i + 1 < length) {
                val code2 = getAlphanumericCode(content[i + 1].code)
                if (code2 == -1) {
                    throw WriterException()
                }
                bits.appendBits(code1 * 45 + code2, 11)
                i += 2
            } else {
                bits.appendBits(code1, 6)
                i++
            }
        }
    }

    @Throws(WriterException::class)
    fun append8BitBytes(content: String, bits: BitArray, encoding: String?) {
        val bytes = try {
            content.toByteArray(charset(encoding!!))
        } catch (uee: UnsupportedEncodingException) {
            throw WriterException(uee)
        }
        for (b in bytes) {
            bits.appendBits(b.toInt(), 8)
        }
    }

    @Throws(WriterException::class)
    fun appendKanjiBytes(content: String, bits: BitArray) {
        val bytes = try {
            content.toByteArray(charset("Shift_JIS"))
        } catch (uee: UnsupportedEncodingException) {
            throw WriterException(uee)
        }
        val length = bytes.size
        var i = 0
        while (i < length) {
            val byte1 = bytes[i].toInt() and 0xFF
            val byte2 = bytes[i + 1].toInt() and 0xFF
            val code = byte1 shl 8 or byte2
            var subtracted = -1
            if (code in 0x8140..0x9ffc) {
                subtracted = code - 0x8140
            } else if (code in 0xe040..0xebbf) {
                subtracted = code - 0xc140
            }
            if (subtracted == -1) {
                throw WriterException("Invalid byte sequence")
            }
            val encoded = (subtracted shr 8) * 0xc0 + (subtracted and 0xff)
            bits.appendBits(encoded, 13)
            i += 2
        }
    }

    private fun appendECI(eci: CharacterSetECI, bits: BitArray) {
        bits.appendBits(Mode.ECI.bits, 4)
        bits.appendBits(eci.getValue(), 8)
    }
}
