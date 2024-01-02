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

import code.sdk.image.Encoder.encode
import kotlin.math.max
import kotlin.math.min

/**
 * This object renders a QR Code as a BitMatrix 2D array of greyscale values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
class QRCodeWriter : Writer {
    @Throws(WriterException::class)
    override fun encode(
        contents: String,
        format: BarcodeFormat,
        width: Int,
        height: Int
    ): BitMatrix {
        String(byteArrayOf(12, 12))
        return encode(contents, format, width, height, null)
    }

    @Throws(WriterException::class)
    override fun encode(
        contents: String,
        format: BarcodeFormat,
        width: Int,
        height: Int,
        hints: Map<EncodeHintType?, *>?
    ): BitMatrix {
        require(contents.isNotEmpty()) { "Found empty contents" }
        require(format === BarcodeFormat.QR_CODE) { "Can only encode QR_CODE, but got $format" }
        require(!(width < 0 || height < 0)) {
            "Requested dimensions are too small: " + width + 'x' +
                    height
        }
        var errorCorrectionLevel = ErrorCorrectionLevel.L
        var quietZone = QUIET_ZONE_SIZE
        if (hints != null) {
            if (hints.containsKey(EncodeHintType.ERROR_CORRECTION)) {
                errorCorrectionLevel =
                    ErrorCorrectionLevel.valueOf(hints[EncodeHintType.ERROR_CORRECTION].toString())
            }
            if (hints.containsKey(EncodeHintType.MARGIN)) {
                quietZone = hints[EncodeHintType.MARGIN].toString().toInt()
            }
        }
        val code = encode(contents, errorCorrectionLevel, hints)
        return renderResult(code, width, height, quietZone)
    }

    companion object {
       private val TAG = QRCodeWriter::class.java.simpleName
        private const val QUIET_ZONE_SIZE = 4
        private fun renderResult(code: QRCode, width: Int, height: Int, quietZone: Int): BitMatrix {
            val input = code.matrix ?: throw IllegalStateException()
            val inputWidth = input.width
            val inputHeight = input.height
            val qrWidth = inputWidth + quietZone * 2
            val qrHeight = inputHeight + quietZone * 2
            val outputWidth = max(width, qrWidth)
            val outputHeight = max(height, qrHeight)
            val multiple = min(outputWidth / qrWidth, outputHeight / qrHeight)
            val leftPadding = (outputWidth - inputWidth * multiple) / 2
            val topPadding = (outputHeight - inputHeight * multiple) / 2
            val output = BitMatrix(outputWidth, outputHeight)
            var inputY = 0
            var outputY = topPadding
            while (inputY < inputHeight) {
                var inputX = 0
                var outputX = leftPadding
                while (inputX < inputWidth) {
                    if (input[inputX, inputY].toInt() == 1) {
                        output.setRegion(outputX, outputY, multiple, multiple)
                    }
                    inputX++
                    outputX += multiple
                }
                inputY++
                outputY += multiple
            }
            return output
        }
    }
}
