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
package book.sdk.zxing

import book.sdk.zxing.MaskUtil.getDataMaskBit

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
internal object MatrixUtil {
    private val POSITION_DETECTION_PATTERN = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1)
    )
    private val POSITION_ADJUSTMENT_PATTERN = arrayOf(
        intArrayOf(1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1)
    )
    private val POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE = arrayOf(
        intArrayOf(-1, -1, -1, -1, -1, -1, -1),
        intArrayOf(6, 18, -1, -1, -1, -1, -1),
        intArrayOf(6, 22, -1, -1, -1, -1, -1),
        intArrayOf(6, 26, -1, -1, -1, -1, -1),
        intArrayOf(6, 30, -1, -1, -1, -1, -1),
        intArrayOf(6, 34, -1, -1, -1, -1, -1),
        intArrayOf(6, 22, 38, -1, -1, -1, -1),
        intArrayOf(6, 24, 42, -1, -1, -1, -1),
        intArrayOf(6, 26, 46, -1, -1, -1, -1),
        intArrayOf(6, 28, 50, -1, -1, -1, -1),
        intArrayOf(6, 30, 54, -1, -1, -1, -1),
        intArrayOf(6, 32, 58, -1, -1, -1, -1),
        intArrayOf(6, 34, 62, -1, -1, -1, -1),
        intArrayOf(6, 26, 46, 66, -1, -1, -1),
        intArrayOf(6, 26, 48, 70, -1, -1, -1),
        intArrayOf(6, 26, 50, 74, -1, -1, -1),
        intArrayOf(6, 30, 54, 78, -1, -1, -1),
        intArrayOf(6, 30, 56, 82, -1, -1, -1),
        intArrayOf(6, 30, 58, 86, -1, -1, -1),
        intArrayOf(6, 34, 62, 90, -1, -1, -1),
        intArrayOf(6, 28, 50, 72, 94, -1, -1),
        intArrayOf(6, 26, 50, 74, 98, -1, -1),
        intArrayOf(6, 30, 54, 78, 102, -1, -1),
        intArrayOf(6, 28, 54, 80, 106, -1, -1),
        intArrayOf(6, 32, 58, 84, 110, -1, -1),
        intArrayOf(6, 30, 58, 86, 114, -1, -1),
        intArrayOf(6, 34, 62, 90, 118, -1, -1),
        intArrayOf(6, 26, 50, 74, 98, 122, -1),
        intArrayOf(6, 30, 54, 78, 102, 126, -1),
        intArrayOf(6, 26, 52, 78, 104, 130, -1),
        intArrayOf(6, 30, 56, 82, 108, 134, -1),
        intArrayOf(6, 34, 60, 86, 112, 138, -1),
        intArrayOf(6, 30, 58, 86, 114, 142, -1),
        intArrayOf(6, 34, 62, 90, 118, 146, -1),
        intArrayOf(6, 30, 54, 78, 102, 126, 150),
        intArrayOf(6, 24, 50, 76, 102, 128, 154),
        intArrayOf(6, 28, 54, 80, 106, 132, 158),
        intArrayOf(6, 32, 58, 84, 110, 136, 162),
        intArrayOf(6, 26, 54, 82, 110, 138, 166),
        intArrayOf(6, 30, 58, 86, 114, 142, 170)
    )
    private val TYPE_INFO_COORDINATES = arrayOf(
        intArrayOf(8, 0),
        intArrayOf(8, 1),
        intArrayOf(8, 2),
        intArrayOf(8, 3),
        intArrayOf(8, 4),
        intArrayOf(8, 5),
        intArrayOf(8, 7),
        intArrayOf(8, 8),
        intArrayOf(7, 8),
        intArrayOf(5, 8),
        intArrayOf(4, 8),
        intArrayOf(3, 8),
        intArrayOf(2, 8),
        intArrayOf(1, 8),
        intArrayOf(0, 8)
    )
    private val VERSION_INFO_POLY = 0x1f25 // 1 1111 0010 0101
    private val TYPE_INFO_POLY = 0x537
    private val TYPE_INFO_MASK_PATTERN = 0x5412
    fun clearMatrix(matrix: ByteMatrix) {
        matrix.clear((-1.toByte()).toByte())
    }

    @Throws(WriterException::class)
    fun buildMatrix(
        dataBits: BitArray,
        ecLevel: ErrorCorrectionLevel,
        version: Version,
        maskPattern: Int,
        matrix: ByteMatrix
    ) {
        clearMatrix(matrix)
        embedBasicPatterns(version, matrix)
        embedTypeInfo(ecLevel, maskPattern, matrix)
        maybeEmbedVersionInfo(version, matrix)
        embedDataBits(dataBits, maskPattern, matrix)
    }

    @Throws(WriterException::class)
    fun embedBasicPatterns(version: Version, matrix: ByteMatrix) {
        embedPositionDetectionPatternsAndSeparators(matrix)
        embedDarkDotAtLeftBottomCorner(matrix)
        maybeEmbedPositionAdjustmentPatterns(version, matrix)
        embedTimingPatterns(matrix)
    }

    @Throws(WriterException::class)
    fun embedTypeInfo(ecLevel: ErrorCorrectionLevel, maskPattern: Int, matrix: ByteMatrix) {
        val typeInfoBits = BitArray()
        makeTypeInfoBits(ecLevel, maskPattern, typeInfoBits)
        for (i in 0 until typeInfoBits.size) {
            val bit = typeInfoBits[typeInfoBits.size - 1 - i]
            val x1 = TYPE_INFO_COORDINATES[i][0]
            val y1 = TYPE_INFO_COORDINATES[i][1]
            matrix[x1, y1] = bit
            if (i < 8) {
                val x2 = matrix.width - i - 1
                val y2 = 8
                matrix[x2, y2] = bit
            } else {
                val x2 = 8
                val y2 = matrix.height - 7 + (i - 8)
                matrix[x2, y2] = bit
            }
        }
    }

    @Throws(WriterException::class)
    fun maybeEmbedVersionInfo(version: Version, matrix: ByteMatrix) {
        if (version.versionNumber < 7) {  // Version info is necessary if version >= 7.
            return  // Don't need version info.
        }
        val versionInfoBits = BitArray()
        makeVersionInfoBits(version, versionInfoBits)
        var bitIndex = 6 * 3 - 1 // It will decrease from 17 to 0.
        for (i in 0..5) {
            for (j in 0..2) {
                val bit = versionInfoBits[bitIndex]
                bitIndex--
                matrix[i, matrix.height - 11 + j] = bit
                matrix[matrix.height - 11 + j, i] = bit
            }
        }
    }

    @Throws(WriterException::class)
    fun embedDataBits(dataBits: BitArray, maskPattern: Int, matrix: ByteMatrix) {
        var bitIndex = 0
        var direction = -1
        var x = matrix.width - 1
        var y = matrix.height - 1
        while (x > 0) {
            if (x == 6) {
                x -= 1
            }
            while (y >= 0 && y < matrix.height) {
                for (i in 0..1) {
                    val xx = x - i
                    if (!isEmpty(matrix[xx, y].toInt())) {
                        continue
                    }
                    var bit: Boolean
                    if (bitIndex < dataBits.size) {
                        bit = dataBits[bitIndex]
                        ++bitIndex
                    } else {
                        bit = false
                    }
                    if (maskPattern != -1 && getDataMaskBit(maskPattern, xx, y)) {
                        bit = !bit
                    }
                    matrix[xx, y] = bit
                }
                y += direction
            }
            direction = -direction // Reverse the direction.
            y += direction
            x -= 2 // Move to the left.
        }
        if (bitIndex != dataBits.size) {
            throw WriterException("Not all bits consumed: " + bitIndex + '/' + dataBits.size)
        }
    }

    fun findMSBSet(value: Int): Int {
        return 32 - Integer.numberOfLeadingZeros(value)
    }

    fun calculateBCHCode(value: Int, poly: Int): Int {
        var v = value
        require(poly != 0) { "0 polynomial" }
        val msbSetInPoly = findMSBSet(poly)
        v = v shl msbSetInPoly - 1
        while (findMSBSet(v) >= msbSetInPoly) {
            v = v xor (poly shl findMSBSet(v) - msbSetInPoly)
        }
        return v
    }

    @Throws(WriterException::class)
    fun makeTypeInfoBits(ecLevel: ErrorCorrectionLevel, maskPattern: Int, bits: BitArray) {
        if (!QRCode.isValidMaskPattern(maskPattern)) {
            throw WriterException("Invalid mask pattern")
        }
        val typeInfo = ecLevel.bits shl 3 or maskPattern
        bits.appendBits(typeInfo, 5)
        val bchCode = calculateBCHCode(typeInfo, TYPE_INFO_POLY)
        bits.appendBits(bchCode, 10)
        val maskBits = BitArray()
        maskBits.appendBits(TYPE_INFO_MASK_PATTERN, 15)
        bits.xor(maskBits)
        if (bits.size != 15) {  // Just in case.
            throw WriterException("should not happen but we got: " + bits.size)
        }
    }

    @Throws(WriterException::class)
    fun makeVersionInfoBits(version: Version, bits: BitArray) {
        bits.appendBits(version.versionNumber, 6)
        val bchCode = calculateBCHCode(version.versionNumber, VERSION_INFO_POLY)
        bits.appendBits(bchCode, 12)
        if (bits.size != 18) {  // Just in case.
            throw WriterException("should not happen but we got: " + bits.size)
        }
    }

    private fun isEmpty(value: Int): Boolean {
        return value == -1
    }

    private fun embedTimingPatterns(matrix: ByteMatrix) {
        for (i in 8 until matrix.width - 8) {
            val bit = (i + 1) % 2
            if (isEmpty(matrix[i, 6].toInt())) {
                matrix[i, 6] = bit
            }
            if (isEmpty(matrix[6, i].toInt())) {
                matrix[6, i] = bit
            }
        }
    }

    @Throws(WriterException::class)
    private fun embedDarkDotAtLeftBottomCorner(matrix: ByteMatrix) {
        if (matrix[8, matrix.height - 8].toInt() == 0) {
            throw WriterException()
        }
        matrix[8, matrix.height - 8] = 1
    }

    @Throws(WriterException::class)
    private fun embedHorizontalSeparationPattern(
        xStart: Int,
        yStart: Int,
        matrix: ByteMatrix
    ) {
        for (x in 0..7) {
            if (!isEmpty(matrix[xStart + x, yStart].toInt())) {
                throw WriterException()
            }
            matrix[xStart + x, yStart] = 0
        }
    }

    @Throws(WriterException::class)
    private fun embedVerticalSeparationPattern(
        xStart: Int,
        yStart: Int,
        matrix: ByteMatrix
    ) {
        for (y in 0..6) {
            if (!isEmpty(matrix[xStart, yStart + y].toInt())) {
                throw WriterException()
            }
            matrix[xStart, yStart + y] = 0
        }
    }

    private fun embedPositionAdjustmentPattern(xStart: Int, yStart: Int, matrix: ByteMatrix) {
        for (y in 0..4) {
            for (x in 0..4) {
                matrix[xStart + x, yStart + y] = POSITION_ADJUSTMENT_PATTERN[y][x]
            }
        }
    }

    private fun embedPositionDetectionPattern(xStart: Int, yStart: Int, matrix: ByteMatrix) {
        for (y in 0..6) {
            for (x in 0..6) {
                matrix[xStart + x, yStart + y] = POSITION_DETECTION_PATTERN[y][x]
            }
        }
    }

    @Throws(WriterException::class)
    private fun embedPositionDetectionPatternsAndSeparators(matrix: ByteMatrix) {
        val pdpWidth = POSITION_DETECTION_PATTERN[0].size
        embedPositionDetectionPattern(0, 0, matrix)
        embedPositionDetectionPattern(matrix.width - pdpWidth, 0, matrix)
        embedPositionDetectionPattern(0, matrix.width - pdpWidth, matrix)
        val hspWidth = 8
        embedHorizontalSeparationPattern(0, hspWidth - 1, matrix)
        embedHorizontalSeparationPattern(
            matrix.width - hspWidth,
            hspWidth - 1, matrix
        )
        embedHorizontalSeparationPattern(0, matrix.width - hspWidth, matrix)
        val vspSize = 7
        embedVerticalSeparationPattern(vspSize, 0, matrix)
        embedVerticalSeparationPattern(matrix.height - vspSize - 1, 0, matrix)
        embedVerticalSeparationPattern(
            vspSize, matrix.height - vspSize,
            matrix
        )
    }

    private fun maybeEmbedPositionAdjustmentPatterns(version: Version, matrix: ByteMatrix) {
        if (version.versionNumber < 2) {  // The patterns appear if version >= 2
            return
        }
        val index = version.versionNumber - 1
        val coordinates = POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[index]
        val numCoordinates = POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[index].size
        for (i in 0 until numCoordinates) {
            for (j in 0 until numCoordinates) {
                val y = coordinates[i]
                val x = coordinates[j]
                if (x == -1 || y == -1) {
                    continue
                }
                if (isEmpty(matrix[x, y].toInt())) {
                    embedPositionAdjustmentPattern(x - 2, y - 2, matrix)
                }
            }
        }
    }
}
