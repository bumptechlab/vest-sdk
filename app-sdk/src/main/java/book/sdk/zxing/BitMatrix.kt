/*
 * Copyright 2007 ZXing authors
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


/**
 *
 * Represents a 2D matrix of bits. In function arguments below, and throughout the common
 * module, x is the column position, and y is the row position. The ordering is always x, y.
 * The origin is at the top-left.
 *
 *
 * Internally the bits are represented in a 1-D array of 32-bit ints. However, each row begins
 * with a new int. This is done intentionally so that we can copy out a row into a BitArray very
 * efficiently.
 *
 *
 * The ordering of bits is row-major. Within each int, the least significant bits are used first,
 * meaning they represent lower x values. This is compatible with BitArray's implementation.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
class BitMatrix : Cloneable {

   private val TAG = BitMatrix::class.java.simpleName

    /**
     * @return The width of the matrix
     */
    val width: Int

    /**
     * @return The height of the matrix
     */
    val height: Int
    private val rowSize: Int
    private val bits: IntArray

    constructor(dimension: Int) : this(dimension, dimension)
    constructor(width: Int, height: Int) {
        require(!(width < 1 || height < 1)) { "Both dimensions must be greater than 0" }
        this.width = width
        this.height = height
        rowSize = (width + 31) / 32
        bits = IntArray(rowSize * height)
    }

    private constructor(width: Int, height: Int, rowSize: Int, bits: IntArray) {
        this.width = width
        this.height = height
        this.rowSize = rowSize
        this.bits = bits
    }

    /**
     *
     * Gets the requested bit, where true means black.
     *
     * @param x The horizontal component (i.e. which column)
     * @param y The vertical component (i.e. which row)
     * @return value of given bit in matrix
     */
    operator fun get(x: Int, y: Int): Boolean {
        val offset = y * rowSize + x / 32
        return bits[offset] ushr (x and 0x1f) and 1 != 0
    }

    /**
     *
     * Sets the given bit to true.
     *
     * @param x The horizontal component (i.e. which column)
     * @param y The vertical component (i.e. which row)
     */
    operator fun set(x: Int, y: Int) {
        val offset = y * rowSize + x / 32
        bits[offset] = bits[offset] or (1 shl (x and 0x1f))
    }

    /**
     *
     * Flips the given bit.
     *
     * @param x The horizontal component (i.e. which column)
     * @param y The vertical component (i.e. which row)
     */
    fun flip(x: Int, y: Int) {
        val offset = y * rowSize + x / 32
        bits[offset] = bits[offset] xor (1 shl (x and 0x1f))
    }

    /**
     * Clears all bits (sets to false).
     */
    fun clear() {
        val max = bits.size
        for (i in 0 until max) {
            bits[i] = 0
        }
    }

    /**
     *
     * Sets a square region of the bit matrix to true.
     *
     * @param left The horizontal position to begin at (inclusive)
     * @param top The vertical position to begin at (inclusive)
     * @param width The width of the region
     * @param height The height of the region
     */
    fun setRegion(left: Int, top: Int, width: Int, height: Int) {
        require(!(top < 0 || left < 0)) { "Left and top must be nonnegative" }
        require(!(height < 1 || width < 1)) { "Height and width must be at least 1" }
        val right = left + width
        val bottom = top + height
        require(!(bottom > this.height || right > this.width)) { "The region must fit inside the matrix" }
        for (y in top until bottom) {
            val offset = y * rowSize
            for (x in left until right) {
                bits[offset + x / 32] = bits[offset + x / 32] or (1 shl (x and 0x1f))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BitMatrix) {
            return false
        }
        return width == other.width && height == other.height && rowSize == other.rowSize &&
                bits.contentEquals(other.bits)
    }

    override fun hashCode(): Int {
        var hash = width
        hash = 31 * hash + width
        hash = 31 * hash + height
        hash = 31 * hash + rowSize
        hash = 31 * hash + bits.contentHashCode()
        return hash
    }

    /**
     * @return string representation using "X" for set and " " for unset bits
     */
    override fun toString(): String {
        return toString("X ", "  ")
    }

    /**
     * @param setString representation of a set bit
     * @param unsetString representation of an unset bit
     * @return string representation of entire matrix utilizing given strings
     */
    fun toString(setString: String, unsetString: String): String {
        return buildToString(setString, unsetString, "\n")
    }

    /**
     * @param setString representation of a set bit
     * @param unsetString representation of an unset bit
     * @param lineSeparator newline character in string representation
     * @return string representation of entire matrix utilizing given strings and line separator
     */
    @Deprecated("call {@link #toString(String, String)} only, which uses \n line separator always")
    fun toString(setString: String, unsetString: String, lineSeparator: String): String {
        return buildToString(setString, unsetString, lineSeparator)
    }

    private fun buildToString(
        setString: String,
        unsetString: String,
        lineSeparator: String
    ): String {
        val result = StringBuilder(height * (width + 1))
        for (y in 0 until height) {
            for (x in 0 until width) {
                result.append(if (get(x, y)) setString else unsetString)
            }
            result.append(lineSeparator)
        }
        return result.toString()
    }

    public override fun clone(): BitMatrix {
        return BitMatrix(width, height, rowSize, bits.clone())
    }

    fun parse(stringRepresentation: String?, setString: String, unsetString: String): BitMatrix {
        requireNotNull(stringRepresentation)
        val bits = BooleanArray(stringRepresentation.length)
        var bitsPos = 0
        var rowStartPos = 0
        var rowLength = -1
        var nRows = 0
        var pos = 0
        while (pos < stringRepresentation.length) {
            if (stringRepresentation[pos] == '\n' ||
                stringRepresentation[pos] == '\r'
            ) {
                if (bitsPos > rowStartPos) {
                    if (rowLength == -1) {
                        rowLength = bitsPos - rowStartPos
                    } else require(bitsPos - rowStartPos == rowLength) { "row lengths do not match" }
                    rowStartPos = bitsPos
                    nRows++
                }
                pos++
            } else if (stringRepresentation.startsWith(setString, pos)) {
                pos += setString.length
                bits[bitsPos] = true
                bitsPos++
            } else if (stringRepresentation.startsWith(unsetString, pos)) {
                pos += unsetString.length
                bits[bitsPos] = false
                bitsPos++
            } else {
                throw IllegalArgumentException(
                    "illegal character encountered: " + stringRepresentation.substring(pos)
                )
            }
        }
        if (bitsPos > rowStartPos) {
            if (rowLength == -1) {
                rowLength = bitsPos - rowStartPos
            } else require(bitsPos - rowStartPos == rowLength) { "row lengths do not match" }
            nRows++
        }
        val matrix = BitMatrix(rowLength, nRows)
        for (i in 0 until bitsPos) {
            if (bits[i]) {
                matrix[i % rowLength] = i / rowLength
            }
        }
        return matrix
    }
}
