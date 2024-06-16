/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Assert.assertArrayEquals

/**
 * Creates a path from the specified shape, using the EvenOdd fill type to match SVG. The returned
 * path has its origin set to 0,0 for convenience.
 */
internal fun createSvgPath(svgShape: SvgShape) =
    Path().apply {
        addSvg(svgShape.pathData)
        val bounds = getBounds()
        translate(Offset(-bounds.left, -bounds.top))
        fillType = PathFillType.EvenOdd
    }

internal enum class SvgShape(val pathData: String) {
    Cubics(
        "M958.729,822.904L958.729,1086.67C958.729,1159.45 899.635,1218.55 826.848,1218.55L563.086,1218.55L355.844,1444.63L450.045,1218.55L337.004,1218.55C264.217,1218.55 205.123,1159.45 205.123,1086.67L205.123,822.904C205.123,750.117 264.217,691.023 337.004,691.023L826.848,691.023C899.635,691.023 958.729,750.117 958.729,822.904ZM581.925,850.888C544.745,781.585 470.384,781.585 433.204,816.236C396.023,850.888 396.023,920.191 433.204,989.493C459.23,1041.47 526.155,1093.45 581.925,1128.1C637.696,1093.45 704.621,1041.47 730.647,989.493C767.829,920.191 767.829,850.888 730.647,816.236C693.467,781.585 619.106,781.585 581.925,850.888Z"
    ),
    Quads(
        "M 664.72,242.306 L 664.72,423.585 Q 649.1189999999999,514.2250000000001 574.08,514.225 L 392.801,514.225 L 250.367,669.607 L 315.11,514.225 L 237.419,514.225 Q 146.779,498.62249999999995 146.779,423.585 L 146.779,242.306 Q 162.38,151.666 237.419,151.666 L 574.08,151.666 Q 664.72,167.267 664.72,242.306 M 375.55,220.052 Q 355.947,196.49699999999999 334.296,208.998 Q 310.741,228.5995 323.242,250.252 Q 294.48900000000003,239.65350000000004 281.988,261.306 Q 271.3895000000001,290.05899999999997 293.042,302.56 Q 262.842,307.758 262.842,332.76 Q 268.0400000000001,362.9599999999999 293.042,362.96 Q 269.48699999999997,382.5615 281.988,404.214 Q 301.58950000000004,427.769 323.242,415.268 Q 312.6435,444.02099999999996 334.296,456.522 Q 363.0490000000001,467.1189999999999 375.55,445.468 Q 380.74850000000004,475.66700000000003 405.749,475.667 Q 435.94899999999996,470.46849999999995 435.949,445.468 Q 455.552,469.02150000000006 477.203,456.522 Q 500.75800000000004,436.919 488.257,415.268 Q 517.01,425.865 529.511,404.214 Q 540.1095,375.461 518.457,362.96 Q 548.6569999999999,357.76200000000006 548.657,332.76 Q 543.4590000000001,302.56000000000006 518.457,302.56 Q 542.0120000000001,282.95849999999996 529.511,261.306 Q 509.9095,237.751 488.257,250.252 Q 498.8555,221.499 477.203,208.998 Q 448.45000000000005,198.3995 435.949,220.052 Q 430.751,189.85200000000003 405.749,189.852 Q 375.55000000000007,195.04999999999995 375.55,220.052Z"
    ),
    Lines(
        "M741.323,698.969L825.045,956.64L1095.98,956.64L876.789,1115.89L960.511,1373.56L741.323,1214.31L522.134,1373.56L605.857,1115.89L386.668,956.64L657.6,956.64L741.323,698.969Z"
    ),
    FillTypes(
        "M570.019,673.111L580.895,726.063C601.945,729.284 622.576,734.812 642.417,742.548L678.312,702.128C698.399,711.261 717.552,722.32 735.505,735.149L718.449,786.445C735.068,799.759 750.171,814.862 763.485,831.481L814.781,814.425C827.61,832.378 838.669,851.531 847.801,871.618L807.382,907.513C815.118,927.354 820.646,947.985 823.867,969.035L876.819,979.911C878.953,1001.87 878.953,1023.99 876.819,1045.95L823.867,1056.83C820.646,1077.88 815.118,1098.51 807.382,1118.35L847.801,1154.25C838.669,1174.33 827.61,1193.49 814.781,1211.44L763.485,1194.38C750.171,1211 735.068,1226.1 718.449,1239.42L735.505,1290.71C717.552,1303.54 698.399,1314.6 678.312,1323.74L642.417,1283.32C622.576,1291.05 601.945,1296.58 580.895,1299.8L570.019,1352.75C548.057,1354.89 525.94,1354.89 503.978,1352.75L493.101,1299.8C472.051,1296.58 451.42,1291.05 431.58,1283.32L395.684,1323.74C375.598,1314.6 356.444,1303.54 338.492,1290.71L355.548,1239.42C338.929,1226.1 323.826,1211 310.511,1194.38L259.215,1211.44C246.386,1193.49 235.328,1174.33 226.195,1154.25L266.614,1118.35C258.879,1098.51 253.351,1077.88 250.13,1056.83L197.178,1045.95C195.044,1023.99 195.044,1001.87 197.178,979.911L250.13,969.035C253.351,947.985 258.879,927.354 266.614,907.513L226.195,871.618C235.328,851.531 246.386,832.378 259.215,814.425L310.511,831.481C323.826,814.862 338.929,799.759 355.548,786.445L338.492,735.149C356.444,722.32 375.598,711.261 395.684,702.128L431.58,742.548C451.42,734.812 472.051,729.284 493.101,726.063L503.978,673.111C525.94,670.977 548.057,670.977 570.019,673.111ZM536.998,944.648C574.685,944.648 605.282,975.245 605.282,1012.93C605.282,1050.62 574.685,1081.22 536.998,1081.22C499.311,1081.22 468.714,1050.62 468.714,1012.93C468.714,975.245 499.311,944.648 536.998,944.648Z"
    ),
    Heart(
        "M648.094,783.362C721.785,623.533 869.168,623.533 942.86,703.447C1016.55,783.362 1016.55,943.19 942.86,1103.02C891.275,1222.89 758.631,1342.76 648.094,1422.68C537.557,1342.76 404.913,1222.89 353.329,1103.02C279.638,943.19 279.638,783.362 353.329,703.447C427.021,623.533 574.403,623.533 648.094,783.362Z"
    ),
}

private fun valueCountForType(type: PathSegment.Type) =
    when (type) {
        PathSegment.Type.Move -> 2
        PathSegment.Type.Line -> 4
        PathSegment.Type.Quadratic -> 6
        PathSegment.Type.Conic -> 8
        PathSegment.Type.Cubic -> 8
        PathSegment.Type.Close -> 0
        PathSegment.Type.Done -> 0
    }

internal fun assertPathEquals(
    expected: Path,
    actual: Path,
    points1: FloatArray = FloatArray(8),
    points2: FloatArray = FloatArray(8)
) {
    val iterator1 = expected.iterator()
    val iterator2 = actual.iterator()

    assertEquals(iterator1.calculateSize(), iterator2.calculateSize())

    while (iterator1.hasNext() && iterator2.hasNext()) {
        val type1 = iterator1.next(points1)
        val type2 = iterator2.next(points2)

        points1.fill(0.0f, valueCountForType(type1))
        points2.fill(0.0f, valueCountForType(type2))

        assertEquals(type1, type2)

        assertArrayEquals(points1, points2, 1e-7f)
    }
}

internal fun assertPointEquals(expected: Offset, actual: Offset) {
    assertEquals(expected.x, actual.x, 1e-6f)
    assertEquals(expected.y, actual.y, 1e-6f)
}

internal fun assertPointEquals(expected: Offset, actual: FloatArray, offset: Int) {
    assertEquals(expected.x, actual[0 + offset * 2], 1e-6f)
    assertEquals(expected.y, actual[1 + offset * 2], 1e-6f)
}

/**
 * Compares two bitmaps and fails the test if they are different. The two bitmaps are considered
 * different if more than [errorCount] pixels differ by more than [threshold] in any of the RGB
 * channels.
 */
internal fun compareBitmaps(bitmap1: Bitmap, bitmap2: Bitmap, errorCount: Int, threshold: Int = 1) {
    assertEquals(bitmap1.width, bitmap2.width)
    assertEquals(bitmap1.height, bitmap2.height)

    val p1 = IntArray(bitmap1.width * bitmap1.height)
    bitmap1.getPixels(p1, 0, bitmap1.width, 0, 0, bitmap1.width, bitmap1.height)

    val p2 = IntArray(bitmap2.width * bitmap2.height)
    bitmap2.getPixels(p2, 0, bitmap2.width, 0, 0, bitmap2.width, bitmap2.height)

    var count = 0
    for (y in 0 until bitmap1.height) {
        for (x in 0 until bitmap1.width) {
            val index = y * bitmap1.width + x

            val (r1, g1, b1, _) = p1[index]
            val (r2, g2, b2, _) = p2[index]

            if (abs(r1 - r2) > threshold || abs(g1 - g2) > threshold || abs(b1 - b2) > threshold) {
                count++
            }
        }
    }

    if (count > errorCount) {
        fail("More than $errorCount different pixels ($count) with error threshold=$threshold")
    }
}
