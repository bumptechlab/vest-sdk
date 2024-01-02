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
 * See ISO 18004:2006, 6.4.1, Tables 2 and 3. This enum encapsulates the various modes in which
 * data can be encoded to bits in the QR code standard.
 *
 * @author Sean Owen
 */
enum class Mode(private val characterCountBitsForVersions: IntArray, val bits: Int) {
    TERMINATOR(intArrayOf(0, 0, 0), 0x00),

    // Not really a mode...
    NUMERIC(intArrayOf(10, 12, 14), 0x01),
    ALPHANUMERIC(intArrayOf(9, 11, 13), 0x02),
    STRUCTURED_APPEND(intArrayOf(0, 0, 0), 0x03),

    // Not supported
    BYTE(intArrayOf(8, 16, 16), 0x04),
    ECI(intArrayOf(0, 0, 0), 0x07),

    // character counts don't apply
    KANJI(intArrayOf(8, 10, 12), 0x08),
    FNC1_FIRST_POSITION(intArrayOf(0, 0, 0), 0x05),
    FNC1_SECOND_POSITION(intArrayOf(0, 0, 0), 0x09),

    /** See GBT 18284-2000; "Hanzi" is a transliteration of this mode name.  */
    HANZI(intArrayOf(8, 10, 12), 0x0D);

    /**
     * @param version version in question
     * @return number of bits used, in this QR Code symbol [Version], to encode the
     * count of characters that will follow encoded in this Mode
     */
    fun getCharacterCountBits(version: Version): Int {
        val number = version.versionNumber
        val offset = if (number <= 9) {
            0
        } else if (number <= 26) {
            1
        } else {
            2
        }
        return characterCountBitsForVersions[offset]
    }

    companion object {
        /**
         * @param bits four bits encoding a QR Code data mode
         * @return Mode encoded by these bits
         * @throws IllegalArgumentException if bits do not correspond to a known mode
         */
        fun forBits(bits: Int): Mode {
            return when (bits) {
                0x0 -> TERMINATOR
                0x1 -> NUMERIC
                0x2 -> ALPHANUMERIC
                0x3 -> STRUCTURED_APPEND
                0x4 -> BYTE
                0x5 -> FNC1_FIRST_POSITION
                0x7 -> ECI
                0x8 -> KANJI
                0x9 -> FNC1_SECOND_POSITION
                0xD -> HANZI
                else -> throw IllegalArgumentException()
            }
        }
    }
}
