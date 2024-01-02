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

/**
 * Encapsulates a Character Set ECI, according to "Extended Channel Interpretations" 5.3.1.1
 * of ISO 18004.
 *
 * @author Sean Owen
 */
enum class CharacterSetECI {
    Cp437(intArrayOf(0, 2)),
    ISO8859_1(intArrayOf(1, 3), "ISO-8859-1"),
    ISO8859_2(4, "ISO-8859-2"),
    ISO8859_3(5, "ISO-8859-3"),
    ISO8859_4(6, "ISO-8859-4"),
    ISO8859_5(7, "ISO-8859-5"),
    ISO8859_6(8, "ISO-8859-6"),
    ISO8859_7(9, "ISO-8859-7"),
    ISO8859_8(10, "ISO-8859-8"),
    ISO8859_9(11, "ISO-8859-9"),
    ISO8859_10(12, "ISO-8859-10"),
    ISO8859_11(13, "ISO-8859-11"),
    ISO8859_13(15, "ISO-8859-13"),
    ISO8859_14(16, "ISO-8859-14"),
    ISO8859_15(17, "ISO-8859-15"),
    ISO8859_16(18, "ISO-8859-16"),
    SJIS(20, "Shift_JIS"),
    Cp1250(21, "windows-1250"),
    Cp1251(22, "windows-1251"),
    Cp1252(23, "windows-1252"),
    Cp1256(24, "windows-1256"),
    UnicodeBigUnmarked(25, "UTF-16BE", "UnicodeBig"),
    UTF8(26, "UTF-8"),
    ASCII(intArrayOf(27, 170), "US-ASCII"),
    Big5(28),
    GB18030(29, "GB2312", "EUC_CN", "GBK"),
    EUC_KR(30, "EUC-KR");

    private val values: IntArray
    private val otherEncodingNames: Array<String>

    constructor(value: Int) : this(intArrayOf(value))
    constructor(value: Int, vararg otherEncodingNames: String) {
        values = intArrayOf(value)
        this.otherEncodingNames = otherEncodingNames as Array<String>
    }

    constructor(values: IntArray, vararg otherEncodingNames: String) {
        this.values = values
        this.otherEncodingNames = otherEncodingNames as Array<String>
    }

    fun getValue() = values[0]

    companion object {
        private val VALUE_TO_ECI: MutableMap<Int, CharacterSetECI> = HashMap()
        private val NAME_TO_ECI: MutableMap<String, CharacterSetECI> = HashMap()

        init {
            for (eci in entries) {
                for (value in eci.values) {
                    VALUE_TO_ECI[value] = eci
                }
                NAME_TO_ECI[eci.name] = eci
                for (name in eci.otherEncodingNames) {
                    NAME_TO_ECI[name] = eci
                }
            }
        }

        /**
         * @param name character set ECI encoding name
         * @return CharacterSetECI representing ECI for character encoding, or null if it is legal
         * but unsupported
         */
        
        fun getCharacterSetECIByName(name: String): CharacterSetECI? {
            return NAME_TO_ECI[name]
        }
    }
}
