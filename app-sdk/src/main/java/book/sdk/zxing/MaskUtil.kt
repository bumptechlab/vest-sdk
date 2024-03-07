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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @author Satoru Takabayashi
 * @author Daniel Switkin
 * @author Sean Owen
 */
internal object MaskUtil {
    private const val N1 = 3
    private const val N2 = 3
    private const val N3 = 40
    private const val N4 = 10

    /**
     * Apply mask penalty rule 1 and return the penalty. Find repetitive cells with the same color and
     * give penalty to them. Example: 00000 or 11111.
     */
    fun applyMaskPenaltyRule1(matrix: ByteMatrix): Int {
        return applyMaskPenaltyRule1Internal(matrix, true) + applyMaskPenaltyRule1Internal(
            matrix,
            false
        )
    }

    /**
     * Apply mask penalty rule 2 and return the penalty. Find 2x2 blocks with the same color and give
     * penalty to them. This is actually equivalent to the spec's rule, which is to find MxN blocks and give a
     * penalty proportional to (M-1)x(N-1), because this is the number of 2x2 blocks inside such a block.
     */
    fun applyMaskPenaltyRule2(matrix: ByteMatrix): Int {
        var penalty = 0
        val array = matrix.array
        val width = matrix.width
        val height = matrix.height
        for (y in 0 until height - 1) {
            for (x in 0 until width - 1) {
                val value = array[y][x].toInt()
                if (value == array[y][x + 1].toInt() && value == array[y + 1][x].toInt() && value == array[y + 1][x + 1].toInt()) {
                    penalty++
                }
            }
        }
        return N2 * penalty
    }

    /**
     * Apply mask penalty rule 3 and return the penalty. Find consecutive runs of 1:1:3:1:1:4
     * starting with black, or 4:1:1:3:1:1 starting with white, and give penalty to them.  If we
     * find patterns like 000010111010000, we give penalty once.
     */
    fun applyMaskPenaltyRule3(matrix: ByteMatrix): Int {
        var numPenalties = 0
        val array = matrix.array
        val width = matrix.width
        val height = matrix.height
        for (y in 0 until height) {
            for (x in 0 until width) {
                val arrayY = array[y] // We can at least optimize this access
                if (x + 6 < width && arrayY[x].toInt() == 1 && arrayY[x + 1].toInt() == 0 && arrayY[x + 2].toInt() == 1 && arrayY[x + 3].toInt() == 1 && arrayY[x + 4].toInt() == 1 && arrayY[x + 5].toInt() == 0 && arrayY[x + 6].toInt() == 1 &&
                    (isWhiteHorizontal(arrayY, x - 4, x) || isWhiteHorizontal(
                        arrayY,
                        x + 7,
                        x + 11
                    ))
                ) {
                    numPenalties++
                }
                if (y + 6 < height && array[y][x].toInt() == 1 && array[y + 1][x].toInt() == 0 && array[y + 2][x].toInt() == 1 && array[y + 3][x].toInt() == 1 && array[y + 4][x].toInt() == 1 && array[y + 5][x].toInt() == 0 && array[y + 6][x].toInt() == 1 &&
                    (isWhiteVertical(array, x, y - 4, y) || isWhiteVertical(
                        array,
                        x,
                        y + 7,
                        y + 11
                    ))
                ) {
                    numPenalties++
                }
            }
        }
        return numPenalties * N3
    }

    private fun isWhiteHorizontal(rowArray: ByteArray, from: Int, to: Int): Boolean {
        var f = from
        var t = to
        f = max(f, 0)
        t = min(t, rowArray.size)
        for (i in f until t) {
            if (rowArray[i].toInt() == 1) {
                return false
            }
        }
        return true
    }

    private fun isWhiteVertical(array: Array<ByteArray>, col: Int, from: Int, to: Int): Boolean {
        var f = from
        var t = to
        f = max(f, 0)
        t = min(t, array.size)
        for (i in f until t) {
            if (array[i][col].toInt() == 1) {
                return false
            }
        }
        return true
    }

    /**
     * Apply mask penalty rule 4 and return the penalty. Calculate the ratio of dark cells and give
     * penalty if the ratio is far from 50%. It gives 10 penalty for 5% distance.
     */
    fun applyMaskPenaltyRule4(matrix: ByteMatrix): Int {
        var numDarkCells = 0
        val array = matrix.array
        val width = matrix.width
        val height = matrix.height
        for (y in 0 until height) {
            val arrayY = array[y]
            for (x in 0 until width) {
                if (arrayY[x].toInt() == 1) {
                    numDarkCells++
                }
            }
        }
        val numTotalCells = matrix.height * matrix.width
        val fivePercentVariances = abs(numDarkCells * 2 - numTotalCells) * 10 / numTotalCells
        return fivePercentVariances * N4
    }

    /**
     * Return the mask bit for "getMaskPattern" at "x" and "y". See 8.8 of JISX0510:2004 for mask
     * pattern conditions.
     */
    
    fun getDataMaskBit(maskPattern: Int, x: Int, y: Int): Boolean {
        val intermediate: Int
        val temp: Int
        when (maskPattern) {
            0 -> intermediate = y + x and 0x1
            1 -> intermediate = y and 0x1
            2 -> intermediate = x % 3
            3 -> intermediate = (y + x) % 3
            4 -> intermediate = y / 2 + x / 3 and 0x1
            5 -> {
                temp = y * x
                intermediate = (temp and 0x1) + temp % 3
            }

            6 -> {
                temp = y * x
                intermediate = (temp and 0x1) + temp % 3 and 0x1
            }

            7 -> {
                temp = y * x
                intermediate = temp % 3 + (y + x and 0x1) and 0x1
            }

            else -> throw IllegalArgumentException("Invalid mask pattern: $maskPattern")
        }
        return intermediate == 0
    }

    /**
     * Helper function for applyMaskPenaltyRule1. We need this for doing this calculation in both
     * vertical and horizontal orders respectively.
     */
    private fun applyMaskPenaltyRule1Internal(matrix: ByteMatrix, isHorizontal: Boolean): Int {
        var penalty = 0
        val iLimit = if (isHorizontal) matrix.height else matrix.width
        val jLimit = if (isHorizontal) matrix.width else matrix.height
        val array = matrix.array
        for (i in 0 until iLimit) {
            var numSameBitCells = 0
            var prevBit = -1
            for (j in 0 until jLimit) {
                val bit = (if (isHorizontal) array[i][j] else array[j][i]).toInt()
                if (bit == prevBit) {
                    numSameBitCells++
                } else {
                    if (numSameBitCells >= 5) {
                        penalty += N1 + (numSameBitCells - 5)
                    }
                    numSameBitCells = 1 // Include the cell itself.
                    prevBit = bit
                }
            }
            if (numSameBitCells >= 5) {
                penalty += N1 + (numSameBitCells - 5)
            }
        }
        return penalty
    }
}
