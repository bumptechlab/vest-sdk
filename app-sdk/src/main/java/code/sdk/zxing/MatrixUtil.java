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

package code.sdk.zxing;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
final class MatrixUtil {

  private MatrixUtil() {
  }

  private static final int[][] POSITION_DETECTION_PATTERN =  {
      {1, 1, 1, 1, 1, 1, 1},
      {1, 0, 0, 0, 0, 0, 1},
      {1, 0, 1, 1, 1, 0, 1},
      {1, 0, 1, 1, 1, 0, 1},
      {1, 0, 1, 1, 1, 0, 1},
      {1, 0, 0, 0, 0, 0, 1},
      {1, 1, 1, 1, 1, 1, 1},
  };

  private static final int[][] POSITION_ADJUSTMENT_PATTERN = {
      {1, 1, 1, 1, 1},
      {1, 0, 0, 0, 1},
      {1, 0, 1, 0, 1},
      {1, 0, 0, 0, 1},
      {1, 1, 1, 1, 1},
  };

  private static final int[][] POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE = {
      {-1, -1, -1, -1,  -1,  -1,  -1},  // Version 1
      { 6, 18, -1, -1,  -1,  -1,  -1},  // Version 2
      { 6, 22, -1, -1,  -1,  -1,  -1},  // Version 3
      { 6, 26, -1, -1,  -1,  -1,  -1},  // Version 4
      { 6, 30, -1, -1,  -1,  -1,  -1},  // Version 5
      { 6, 34, -1, -1,  -1,  -1,  -1},  // Version 6
      { 6, 22, 38, -1,  -1,  -1,  -1},  // Version 7
      { 6, 24, 42, -1,  -1,  -1,  -1},  // Version 8
      { 6, 26, 46, -1,  -1,  -1,  -1},  // Version 9
      { 6, 28, 50, -1,  -1,  -1,  -1},  // Version 10
      { 6, 30, 54, -1,  -1,  -1,  -1},  // Version 11
      { 6, 32, 58, -1,  -1,  -1,  -1},  // Version 12
      { 6, 34, 62, -1,  -1,  -1,  -1},  // Version 13
      { 6, 26, 46, 66,  -1,  -1,  -1},  // Version 14
      { 6, 26, 48, 70,  -1,  -1,  -1},  // Version 15
      { 6, 26, 50, 74,  -1,  -1,  -1},  // Version 16
      { 6, 30, 54, 78,  -1,  -1,  -1},  // Version 17
      { 6, 30, 56, 82,  -1,  -1,  -1},  // Version 18
      { 6, 30, 58, 86,  -1,  -1,  -1},  // Version 19
      { 6, 34, 62, 90,  -1,  -1,  -1},  // Version 20
      { 6, 28, 50, 72,  94,  -1,  -1},  // Version 21
      { 6, 26, 50, 74,  98,  -1,  -1},  // Version 22
      { 6, 30, 54, 78, 102,  -1,  -1},  // Version 23
      { 6, 28, 54, 80, 106,  -1,  -1},  // Version 24
      { 6, 32, 58, 84, 110,  -1,  -1},  // Version 25
      { 6, 30, 58, 86, 114,  -1,  -1},  // Version 26
      { 6, 34, 62, 90, 118,  -1,  -1},  // Version 27
      { 6, 26, 50, 74,  98, 122,  -1},  // Version 28
      { 6, 30, 54, 78, 102, 126,  -1},  // Version 29
      { 6, 26, 52, 78, 104, 130,  -1},  // Version 30
      { 6, 30, 56, 82, 108, 134,  -1},  // Version 31
      { 6, 34, 60, 86, 112, 138,  -1},  // Version 32
      { 6, 30, 58, 86, 114, 142,  -1},  // Version 33
      { 6, 34, 62, 90, 118, 146,  -1},  // Version 34
      { 6, 30, 54, 78, 102, 126, 150},  // Version 35
      { 6, 24, 50, 76, 102, 128, 154},  // Version 36
      { 6, 28, 54, 80, 106, 132, 158},  // Version 37
      { 6, 32, 58, 84, 110, 136, 162},  // Version 38
      { 6, 26, 54, 82, 110, 138, 166},  // Version 39
      { 6, 30, 58, 86, 114, 142, 170},  // Version 40
  };

  private static final int[][] TYPE_INFO_COORDINATES = {
      {8, 0},
      {8, 1},
      {8, 2},
      {8, 3},
      {8, 4},
      {8, 5},
      {8, 7},
      {8, 8},
      {7, 8},
      {5, 8},
      {4, 8},
      {3, 8},
      {2, 8},
      {1, 8},
      {0, 8},
  };

  private static final int VERSION_INFO_POLY = 0x1f25;  // 1 1111 0010 0101

  private static final int TYPE_INFO_POLY = 0x537;
  private static final int TYPE_INFO_MASK_PATTERN = 0x5412;

  static void clearMatrix(ByteMatrix matrix) {
    matrix.clear((byte) -1);
  }

  static void buildMatrix(BitArray dataBits,
                          ErrorCorrectionLevel ecLevel,
                          Version version,
                          int maskPattern,
                          ByteMatrix matrix) throws WriterException {
    clearMatrix(matrix);
    embedBasicPatterns(version, matrix);
    embedTypeInfo(ecLevel, maskPattern, matrix);
    maybeEmbedVersionInfo(version, matrix);
    embedDataBits(dataBits, maskPattern, matrix);
  }

  static void embedBasicPatterns(Version version, ByteMatrix matrix) throws WriterException {
    embedPositionDetectionPatternsAndSeparators(matrix);
    embedDarkDotAtLeftBottomCorner(matrix);

    maybeEmbedPositionAdjustmentPatterns(version, matrix);
    embedTimingPatterns(matrix);
  }

  static void embedTypeInfo(ErrorCorrectionLevel ecLevel, int maskPattern, ByteMatrix matrix)
      throws WriterException {
    BitArray typeInfoBits = new BitArray();
    makeTypeInfoBits(ecLevel, maskPattern, typeInfoBits);

    for (int i = 0; i < typeInfoBits.getSize(); ++i) {
      boolean bit = typeInfoBits.get(typeInfoBits.getSize() - 1 - i);

      int x1 = TYPE_INFO_COORDINATES[i][0];
      int y1 = TYPE_INFO_COORDINATES[i][1];
      matrix.set(x1, y1, bit);

      if (i < 8) {
        int x2 = matrix.getWidth() - i - 1;
        int y2 = 8;
        matrix.set(x2, y2, bit);
      } else {
        int x2 = 8;
        int y2 = matrix.getHeight() - 7 + (i - 8);
        matrix.set(x2, y2, bit);
      }
    }
  }

  static void maybeEmbedVersionInfo(Version version, ByteMatrix matrix) throws WriterException {
    if (version.getVersionNumber() < 7) {  // Version info is necessary if version >= 7.
      return;  // Don't need version info.
    }
    BitArray versionInfoBits = new BitArray();
    makeVersionInfoBits(version, versionInfoBits);

    int bitIndex = 6 * 3 - 1;  // It will decrease from 17 to 0.
    for (int i = 0; i < 6; ++i) {
      for (int j = 0; j < 3; ++j) {
        boolean bit = versionInfoBits.get(bitIndex);
        bitIndex--;
        matrix.set(i, matrix.getHeight() - 11 + j, bit);
        matrix.set(matrix.getHeight() - 11 + j, i, bit);
      }
    }
  }

  static void embedDataBits(BitArray dataBits, int maskPattern, ByteMatrix matrix)
      throws WriterException {
    int bitIndex = 0;
    int direction = -1;
    int x = matrix.getWidth() - 1;
    int y = matrix.getHeight() - 1;
    while (x > 0) {
      if (x == 6) {
        x -= 1;
      }
      while (y >= 0 && y < matrix.getHeight()) {
        for (int i = 0; i < 2; ++i) {
          int xx = x - i;
          if (!isEmpty(matrix.get(xx, y))) {
            continue;
          }
          boolean bit;
          if (bitIndex < dataBits.getSize()) {
            bit = dataBits.get(bitIndex);
            ++bitIndex;
          } else {
            bit = false;
          }

          if (maskPattern != -1 && MaskUtil.getDataMaskBit(maskPattern, xx, y)) {
            bit = !bit;
          }
          matrix.set(xx, y, bit);
        }
        y += direction;
      }
      direction = -direction;  // Reverse the direction.
      y += direction;
      x -= 2;  // Move to the left.
    }
    if (bitIndex != dataBits.getSize()) {
      throw new WriterException("Not all bits consumed: " + bitIndex + '/' + dataBits.getSize());
    }
  }

  static int findMSBSet(int value) {
    return 32 - Integer.numberOfLeadingZeros(value);
  }

  static int calculateBCHCode(int value, int poly) {
    if (poly == 0) {
      throw new IllegalArgumentException("0 polynomial");
    }
    int msbSetInPoly = findMSBSet(poly);
    value <<= msbSetInPoly - 1;
    while (findMSBSet(value) >= msbSetInPoly) {
      value ^= poly << (findMSBSet(value) - msbSetInPoly);
    }
    return value;
  }

  static void makeTypeInfoBits(ErrorCorrectionLevel ecLevel, int maskPattern, BitArray bits)
      throws WriterException {
    if (!QRCode.isValidMaskPattern(maskPattern)) {
      throw new WriterException("Invalid mask pattern");
    }
    int typeInfo = (ecLevel.getBits() << 3) | maskPattern;
    bits.appendBits(typeInfo, 5);

    int bchCode = calculateBCHCode(typeInfo, TYPE_INFO_POLY);
    bits.appendBits(bchCode, 10);

    BitArray maskBits = new BitArray();
    maskBits.appendBits(TYPE_INFO_MASK_PATTERN, 15);
    bits.xor(maskBits);

    if (bits.getSize() != 15) {  // Just in case.
      throw new WriterException("should not happen but we got: " + bits.getSize());
    }
  }

  static void makeVersionInfoBits(Version version, BitArray bits) throws WriterException {
    bits.appendBits(version.getVersionNumber(), 6);
    int bchCode = calculateBCHCode(version.getVersionNumber(), VERSION_INFO_POLY);
    bits.appendBits(bchCode, 12);

    if (bits.getSize() != 18) {  // Just in case.
      throw new WriterException("should not happen but we got: " + bits.getSize());
    }
  }

  private static boolean isEmpty(int value) {
    return value == -1;
  }

  private static void embedTimingPatterns(ByteMatrix matrix) {
    for (int i = 8; i < matrix.getWidth() - 8; ++i) {
      int bit = (i + 1) % 2;
      if (isEmpty(matrix.get(i, 6))) {
        matrix.set(i, 6, bit);
      }
      if (isEmpty(matrix.get(6, i))) {
        matrix.set(6, i, bit);
      }
    }
  }

  private static void embedDarkDotAtLeftBottomCorner(ByteMatrix matrix) throws WriterException {
    if (matrix.get(8, matrix.getHeight() - 8) == 0) {
      throw new WriterException();
    }
    matrix.set(8, matrix.getHeight() - 8, 1);
  }

  private static void embedHorizontalSeparationPattern(int xStart,
                                                       int yStart,
                                                       ByteMatrix matrix) throws WriterException {
    for (int x = 0; x < 8; ++x) {
      if (!isEmpty(matrix.get(xStart + x, yStart))) {
        throw new WriterException();
      }
      matrix.set(xStart + x, yStart, 0);
    }
  }

  private static void embedVerticalSeparationPattern(int xStart,
                                                     int yStart,
                                                     ByteMatrix matrix) throws WriterException {
    for (int y = 0; y < 7; ++y) {
      if (!isEmpty(matrix.get(xStart, yStart + y))) {
        throw new WriterException();
      }
      matrix.set(xStart, yStart + y, 0);
    }
  }

  private static void embedPositionAdjustmentPattern(int xStart, int yStart, ByteMatrix matrix) {
    for (int y = 0; y < 5; ++y) {
      for (int x = 0; x < 5; ++x) {
        matrix.set(xStart + x, yStart + y, POSITION_ADJUSTMENT_PATTERN[y][x]);
      }
    }
  }

  private static void embedPositionDetectionPattern(int xStart, int yStart, ByteMatrix matrix) {
    for (int y = 0; y < 7; ++y) {
      for (int x = 0; x < 7; ++x) {
        matrix.set(xStart + x, yStart + y, POSITION_DETECTION_PATTERN[y][x]);
      }
    }
  }

  private static void embedPositionDetectionPatternsAndSeparators(ByteMatrix matrix) throws WriterException {
    int pdpWidth = POSITION_DETECTION_PATTERN[0].length;
    embedPositionDetectionPattern(0, 0, matrix);
    embedPositionDetectionPattern(matrix.getWidth() - pdpWidth, 0, matrix);
    embedPositionDetectionPattern(0, matrix.getWidth() - pdpWidth, matrix);

    int hspWidth = 8;
    embedHorizontalSeparationPattern(0, hspWidth - 1, matrix);
    embedHorizontalSeparationPattern(matrix.getWidth() - hspWidth,
        hspWidth - 1, matrix);
    embedHorizontalSeparationPattern(0, matrix.getWidth() - hspWidth, matrix);

    int vspSize = 7;
    embedVerticalSeparationPattern(vspSize, 0, matrix);
    embedVerticalSeparationPattern(matrix.getHeight() - vspSize - 1, 0, matrix);
    embedVerticalSeparationPattern(vspSize, matrix.getHeight() - vspSize,
        matrix);
  }

  private static void maybeEmbedPositionAdjustmentPatterns(Version version, ByteMatrix matrix) {
    if (version.getVersionNumber() < 2) {  // The patterns appear if version >= 2
      return;
    }
    int index = version.getVersionNumber() - 1;
    int[] coordinates = POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[index];
    int numCoordinates = POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[index].length;
    for (int i = 0; i < numCoordinates; ++i) {
      for (int j = 0; j < numCoordinates; ++j) {
        int y = coordinates[i];
        int x = coordinates[j];
        if (x == -1 || y == -1) {
          continue;
        }
        if (isEmpty(matrix.get(x, y))) {
          embedPositionAdjustmentPattern(x - 2, y - 2, matrix);
        }
      }
    }
  }

}
