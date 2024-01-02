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
package code.sdk.image


/**
 *
 * A simple, fast array of bits, represented compactly by an array of ints internally.
 *
 * @author Sean Owen
 */
class BitArray : Cloneable {

   private val TAG = BitArray::class.java.simpleName

    /**
     * @return underlying array of ints. The first element holds the first 32 bits, and the least
     * significant bit is bit 0.
     */
    var bitArray: IntArray
        private set
    var size: Int
        private set

    constructor() {
        size = 0
        bitArray = IntArray(1)
    }

    constructor(size: Int) {
        this.size = size
        bitArray = makeArray(size)
    }

    internal constructor(bits: IntArray, size: Int) {
        bitArray = bits
        this.size = size
    }

    val sizeInBytes: Int
        get() = (size + 7) / 8

    private fun ensureCapacity(size: Int) {
        if (size > bitArray.size * 32) {
            val newBits = makeArray(size)
            System.arraycopy(bitArray, 0, newBits, 0, bitArray.size)
            bitArray = newBits
        }
    }

    /**
     * @param i bit to get
     * @return true iff bit i is set
     */
    operator fun get(i: Int): Boolean {
        return bitArray[i / 32] and (1 shl (i and 0x1F)) != 0
    }

    /**
     * Sets bit i.
     *
     * @param i bit to set
     */
    fun set(i: Int) {
        bitArray[i / 32] = bitArray[i / 32] or (1 shl (i and 0x1F))
    }

    /**
     * Flips bit i.
     *
     * @param i bit to set
     */
    fun flip(i: Int) {
        bitArray[i / 32] = bitArray[i / 32] xor (1 shl (i and 0x1F))
    }

    /**
     * @param from first bit to check
     * @return index of first bit that is set, starting from the given index, or size if none are set
     * at or beyond this given index
     * @see .getNextUnset
     */
    fun getNextSet(from: Int): Int {
        if (from >= size) {
            return size
        }
        var bitsOffset = from / 32
        var currentBits = bitArray[bitsOffset]
        currentBits = currentBits and ((1 shl (from and 0x1F)) - 1).inv()
        while (currentBits == 0) {
            if (++bitsOffset == bitArray.size) {
                return size
            }
            currentBits = bitArray[bitsOffset]
        }
        val result = bitsOffset * 32 + Integer.numberOfTrailingZeros(currentBits)
        return if (result > size) size else result
    }

    /**
     * @param from index to start looking for unset bit
     * @return index of next unset bit, or `size` if none are unset until the end
     * @see .getNextSet
     */
    fun getNextUnset(from: Int): Int {
        if (from >= size) {
            return size
        }
        var bitsOffset = from / 32
        var currentBits = bitArray[bitsOffset].inv()
        currentBits = currentBits and ((1 shl (from and 0x1F)) - 1).inv()
        while (currentBits == 0) {
            if (++bitsOffset == bitArray.size) {
                return size
            }
            currentBits = bitArray[bitsOffset].inv()
        }
        val result = bitsOffset * 32 + Integer.numberOfTrailingZeros(currentBits)
        return if (result > size) size else result
    }

    /**
     * Sets a block of 32 bits, starting at bit i.
     *
     * @param i first bit to set
     * @param newBits the new value of the next 32 bits. Note again that the least-significant bit
     * corresponds to bit i, the next-least-significant to i+1, and so on.
     */
    fun setBulk(i: Int, newBits: Int) {
        bitArray[i / 32] = newBits
    }

    /**
     * Clears all bits (sets to false).
     */
    fun clear() {
        val max = bitArray.size
        for (i in 0 until max) {
            bitArray[i] = 0
        }
    }

    fun appendBit(bit: Boolean) {
        ensureCapacity(size + 1)
        if (bit) {
            bitArray[size / 32] = bitArray[size / 32] or (1 shl (size and 0x1F))
        }
        size++
    }

    /**
     * Appends the least-significant bits, from value, in order from most-significant to
     * least-significant. For example, appending 6 bits from 0x000001E will append the bits
     * 0, 1, 1, 1, 1, 0 in that order.
     *
     * @param value `int` containing bits to append
     * @param numBits bits from value to append
     */
    fun appendBits(value: Int, numBits: Int) {
        require(!(numBits < 0 || numBits > 32)) { "Num bits must be between 0 and 32" }
        ensureCapacity(size + numBits)
        for (numBitsLeft in numBits downTo 1) {
            appendBit(value shr numBitsLeft - 1 and 0x01 == 1)
        }
    }

    fun appendBitArray(other: BitArray) {
        val otherSize = other.size
        ensureCapacity(size + otherSize)
        for (i in 0 until otherSize) {
            appendBit(other[i])
        }
    }

    fun xor(other: BitArray) {
        require(size == other.size) { "Sizes don't match" }
        for (i in bitArray.indices) {
            bitArray[i] = bitArray[i] xor other.bitArray[i]
        }
    }

    /**
     *
     * @param bitOffset first bit to start writing
     * @param array array to write into. Bytes are written most-significant byte first. This is the opposite
     * of the internal representation, which is exposed by [.getBitArray]
     * @param offset position in array to start writing
     * @param numBytes how many bytes to write
     */
    fun toBytes(bitOffset: Int, array: ByteArray, offset: Int, numBytes: Int) {
        var bit = bitOffset
        for (i in 0 until numBytes) {
            var theByte = 0
            for (j in 0..7) {
                if (get(bit)) {
                    theByte = theByte or (1 shl 7 - j)
                }
                bit++
            }
            array[offset + i] = theByte.toByte()
        }
    }

    /**
     * Reverses all bits in the array.
     */
    fun reverse() {
        val newBits = IntArray(bitArray.size)
        val len = (size - 1) / 32
        val oldBitsLen = len + 1
        for (i in 0 until oldBitsLen) {
            var x = bitArray[i].toLong()
            x = x shr 1 and 0x55555555L or (x and 0x55555555L shl 1)
            x = x shr 2 and 0x33333333L or (x and 0x33333333L shl 2)
            x = x shr 4 and 0x0f0f0f0fL or (x and 0x0f0f0f0fL shl 4)
            x = x shr 8 and 0x00ff00ffL or (x and 0x00ff00ffL shl 8)
            x = x shr 16 and 0x0000ffffL or (x and 0x0000ffffL shl 16)
            newBits[len - i] = x.toInt()
        }
        if (size != oldBitsLen * 32) {
            val leftOffset = oldBitsLen * 32 - size
            var currentInt = newBits[0] ushr leftOffset
            for (i in 1 until oldBitsLen) {
                val nextInt = newBits[i]
                currentInt = currentInt or (nextInt shl 32 - leftOffset)
                newBits[i - 1] = currentInt
                currentInt = nextInt ushr leftOffset
            }
            newBits[oldBitsLen - 1] = currentInt
        }
        bitArray = newBits
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BitArray) {
            return false
        }
        return size == other.size && bitArray.contentEquals(other.bitArray)
    }

    override fun hashCode(): Int {
        return 31 * size + bitArray.contentHashCode()
    }

    override fun toString(): String {
        val result = StringBuilder(size)
        for (i in 0 until size) {
            if (i and 0x07 == 0) {
                result.append(' ')
            }
            result.append(if (get(i)) 'X' else '.')
        }
        return result.toString()
    }

    public override fun clone(): BitArray {
        return BitArray(bitArray.clone(), size)
    }


    private fun makeArray(size: Int): IntArray {
        return IntArray((size + 31) / 32)
    }
}
