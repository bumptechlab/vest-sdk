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

/**
 *
 * Implements Reed-Solomon enbcoding, as the name implies.
 *
 * @author Sean Owen
 * @author William Rucklidge
 */
class ReedSolomonEncoder(private val field: GenericGF) {

   private val TAG = ReedSolomonEncoder::class.java.simpleName

    private val cachedGenerators: MutableList<GenericGFPoly>

    init {
        cachedGenerators = ArrayList()
        cachedGenerators.add(GenericGFPoly(field, intArrayOf(1)))
    }

    private fun buildGenerator(degree: Int): GenericGFPoly {
        if (degree >= cachedGenerators.size) {
            var lastGenerator = cachedGenerators[cachedGenerators.size - 1]
            for (d in cachedGenerators.size..degree) {
                val nextGenerator = lastGenerator.multiply(
                    GenericGFPoly(field, intArrayOf(1, field.exp(d - 1 + field.generatorBase)))
                )
                cachedGenerators.add(nextGenerator)
                lastGenerator = nextGenerator
            }
        }
        return cachedGenerators[degree]
    }

    fun encode(toEncode: IntArray, ecBytes: Int) {
        require(ecBytes != 0) { "No error correction bytes" }
        val dataBytes = toEncode.size - ecBytes
        require(dataBytes > 0) { "No data bytes provided" }
        val generator = buildGenerator(ecBytes)
        val infoCoefficients = IntArray(dataBytes)
        System.arraycopy(toEncode, 0, infoCoefficients, 0, dataBytes)
        var info = GenericGFPoly(field, infoCoefficients)
        info = info.multiplyByMonomial(ecBytes, 1)
        val remainder = info.divide(generator)[1]
        val coefficients = remainder.coefficients
        val numZeroCoefficients = ecBytes - coefficients.size
        for (i in 0 until numZeroCoefficients) {
            toEncode[dataBytes + i] = 0
        }
        System.arraycopy(
            coefficients,
            0,
            toEncode,
            dataBytes + numZeroCoefficients,
            coefficients.size
        )
    }
}
