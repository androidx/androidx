/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.shapes

import android.graphics.Matrix
import android.graphics.PointF
import androidx.core.graphics.div
import androidx.core.graphics.plus
import androidx.core.graphics.times
import androidx.test.filters.SmallTest
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class CubicTest {

    // These points create a roughly circular arc in the upper-right quadrant around (0,0)
    val zero = PointF(0f, 0f)
    val p0 = PointF(1f, 0f)
    val p1 = PointF(1f, .5f)
    val p2 = PointF(.5f, 1f)
    val p3 = PointF(0f, 1f)
    val cubic = Cubic(p0, p1, p2, p3)

    @Test
    fun constructionTest() {
        assertEquals(p0, PointF(cubic.anchor0X, cubic.anchor0Y))
        assertEquals(p1, PointF(cubic.control0X, cubic.control0Y))
        assertEquals(p2, PointF(cubic.control1X, cubic.control1Y))
        assertEquals(p3, PointF(cubic.anchor1X, cubic.anchor1Y))
    }

    @Test
    fun copyTest() {
        val copy = Cubic(cubic)
        assertEquals(p0, PointF(copy.anchor0X, copy.anchor0Y))
        assertEquals(p1, PointF(copy.control0X, copy.control0Y))
        assertEquals(p2, PointF(copy.control1X, copy.control1Y))
        assertEquals(p3, PointF(copy.anchor1X, copy.anchor1Y))
        assertEquals(PointF(cubic.anchor0X, cubic.anchor0Y),
            PointF(copy.anchor0X, copy.anchor0Y))
        assertEquals(PointF(cubic.control0X, cubic.control0Y),
            PointF(copy.control0X, copy.control0Y))
        assertEquals(PointF(cubic.control1X, cubic.control1Y),
            PointF(copy.control1X, copy.control1Y))
        assertEquals(PointF(cubic.anchor1X, cubic.anchor1Y),
            PointF(copy.anchor1X, copy.anchor1Y))
    }

    @Test
    fun circularArcTest() {
        val arcCubic = Cubic.circularArc(zero.x, zero.y, p0.x, p0.y, p3.x, p3.y)
        assertEquals(p0, PointF(arcCubic.anchor0X, arcCubic.anchor0Y))
        assertEquals(p3, PointF(arcCubic.anchor1X, arcCubic.anchor1Y))
    }

    @Test
    fun divTest() {
        var divCubic = cubic / 1f
        assertCubicsEqua1ish(cubic, divCubic)
        divCubic = cubic / 1
        assertCubicsEqua1ish(cubic, divCubic)
        divCubic = cubic / 2f
        assertPointsEqualish(p0 / 2f, PointF(divCubic.anchor0X, divCubic.anchor0Y))
        assertPointsEqualish(p1 / 2f, PointF(divCubic.control0X, divCubic.control0Y))
        assertPointsEqualish(p2 / 2f, PointF(divCubic.control1X, divCubic.control1Y))
        assertPointsEqualish(p3 / 2f, PointF(divCubic.anchor1X, divCubic.anchor1Y))
        divCubic = cubic / 2
        assertPointsEqualish(p0 / 2f, PointF(divCubic.anchor0X, divCubic.anchor0Y))
        assertPointsEqualish(p1 / 2f, PointF(divCubic.control0X, divCubic.control0Y))
        assertPointsEqualish(p2 / 2f, PointF(divCubic.control1X, divCubic.control1Y))
        assertPointsEqualish(p3 / 2f, PointF(divCubic.anchor1X, divCubic.anchor1Y))
    }

    @Test
    fun timesTest() {
        var timesCubic = cubic * 1f
        assertEquals(p0, PointF(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertEquals(p1, PointF(timesCubic.control0X, timesCubic.control0Y))
        assertEquals(p2, PointF(timesCubic.control1X, timesCubic.control1Y))
        assertEquals(p3, PointF(timesCubic.anchor1X, timesCubic.anchor1Y))
        timesCubic = cubic * 1
        assertEquals(p0, PointF(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertEquals(p1, PointF(timesCubic.control0X, timesCubic.control0Y))
        assertEquals(p2, PointF(timesCubic.control1X, timesCubic.control1Y))
        assertEquals(p3, PointF(timesCubic.anchor1X, timesCubic.anchor1Y))
        timesCubic = cubic * 2f
        assertPointsEqualish(p0 * 2f, PointF(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertPointsEqualish(p1 * 2f, PointF(timesCubic.control0X, timesCubic.control0Y))
        assertPointsEqualish(p2 * 2f, PointF(timesCubic.control1X, timesCubic.control1Y))
        assertPointsEqualish(p3 * 2f, PointF(timesCubic.anchor1X, timesCubic.anchor1Y))
        timesCubic = cubic * 2
        assertPointsEqualish(p0 * 2f, PointF(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertPointsEqualish(p1 * 2f, PointF(timesCubic.control0X, timesCubic.control0Y))
        assertPointsEqualish(p2 * 2f, PointF(timesCubic.control1X, timesCubic.control1Y))
        assertPointsEqualish(p3 * 2f, PointF(timesCubic.anchor1X, timesCubic.anchor1Y))
    }

    @Test
    fun plusTest() {
        val offsetCubic = cubic * 2f
        var plusCubic = cubic + offsetCubic
        assertPointsEqualish(p0 + PointF(offsetCubic.anchor0X, offsetCubic.anchor0Y),
            PointF(plusCubic.anchor0X, plusCubic.anchor0Y))
        assertPointsEqualish(p1 + PointF(offsetCubic.control0X, offsetCubic.control0Y),
            PointF(plusCubic.control0X, plusCubic.control0Y))
        assertPointsEqualish(p2 + PointF(offsetCubic.control1X, offsetCubic.control1Y),
            PointF(plusCubic.control1X, plusCubic.control1Y))
        assertPointsEqualish(p3 + PointF(offsetCubic.anchor1X, offsetCubic.anchor1Y),
            PointF(plusCubic.anchor1X, plusCubic.anchor1Y))
    }

    @Test
    fun reverseTest() {
        val reverseCubic = cubic.reverse()
        assertEquals(p3, PointF(reverseCubic.anchor0X, reverseCubic.anchor0Y))
        assertEquals(p2, PointF(reverseCubic.control0X, reverseCubic.control0Y))
        assertEquals(p1, PointF(reverseCubic.control1X, reverseCubic.control1Y))
        assertEquals(p0, PointF(reverseCubic.anchor1X, reverseCubic.anchor1Y))
    }

    fun assertBetween(end0: PointF, end1: PointF, actual: PointF) {
        val minX = min(end0.x, end1.x)
        val minY = min(end0.y, end1.y)
        val maxX = max(end0.x, end1.x)
        val maxY = max(end0.y, end1.y)
        assertTrue(minX <= actual.x)
        assertTrue(minY <= actual.y)
        assertTrue(maxX >= actual.x)
        assertTrue(maxY >= actual.y)
    }

    @Test
    fun straightLineTest() {
        val lineCubic = Cubic.straightLine(p0.x, p0.y, p3.x, p3.y)
        assertEquals(p0, PointF(lineCubic.anchor0X, lineCubic.anchor0Y))
        assertEquals(p3, PointF(lineCubic.anchor1X, lineCubic.anchor1Y))
        assertBetween(p0, p3, PointF(lineCubic.control0X, lineCubic.control0Y))
        assertBetween(p0, p3, PointF(lineCubic.control1X, lineCubic.control1Y))
    }

    @Test
    fun interpolateTest() {
        val twiceCubic = cubic + cubic * 2f
        val quadCubic = cubic + cubic * 4f
        val halfway = Cubic.interpolate(cubic, quadCubic, .5f)
        assertCubicsEqua1ish(twiceCubic, halfway)
    }

    @Test
    fun splitTest() {
        val (split0, split1) = cubic.split(.5f)
        assertEquals(PointF(cubic.anchor0X, cubic.anchor0Y),
            PointF(split0.anchor0X, split0.anchor0Y))
        assertEquals(PointF(cubic.anchor1X, cubic.anchor1Y),
            PointF(split1.anchor1X, split1.anchor1Y))
        assertBetween(PointF(cubic.anchor0X, cubic.anchor0Y),
            PointF(cubic.anchor1X, cubic.anchor1Y),
            PointF(split0.anchor1X, split0.anchor1Y))
        assertBetween(PointF(cubic.anchor0X, cubic.anchor0Y),
            PointF(cubic.anchor1X, cubic.anchor1Y),
            PointF(split1.anchor0X, split1.anchor0Y))
    }

    @Test
    fun pointOnCurveTest() {
        var halfway = cubic.pointOnCurve(.5f)
        assertBetween(PointF(cubic.anchor0X, cubic.anchor0Y),
            PointF(cubic.anchor1X, cubic.anchor1Y), halfway)
        val straightLineCubic = Cubic.straightLine(p0.x, p0.y, p3.x, p3.y)
        halfway = straightLineCubic.pointOnCurve(.5f)
        val computedHalfway = PointF(p0.x + .5f * (p3.x - p0.x), p0.y + .5f * (p3.y - p0.y))
        assertPointsEqualish(computedHalfway, halfway)
    }

    @Test
    fun transformTest() {
        val matrix = Matrix()
        var transformedCubic = Cubic(cubic)
        transformedCubic.transform(matrix)
        assertCubicsEqua1ish(cubic, transformedCubic)

        transformedCubic = Cubic(cubic)
        matrix.setScale(3f, 3f)
        transformedCubic.transform(matrix)
        assertCubicsEqua1ish(cubic * 3f, transformedCubic)

        val tx = 200f
        val ty = 300f
        val translationVector = PointF(tx, ty)
        transformedCubic = Cubic(cubic)
        matrix.setTranslate(tx, ty)
        transformedCubic.transform(matrix)
        assertPointsEqualish(PointF(cubic.anchor0X, cubic.anchor0Y) + translationVector,
            PointF(transformedCubic.anchor0X, transformedCubic.anchor0Y))
        assertPointsEqualish(PointF(cubic.control0X, cubic.control0Y) + translationVector,
            PointF(transformedCubic.control0X, transformedCubic.control0Y))
        assertPointsEqualish(PointF(cubic.control1X, cubic.control1Y) + translationVector,
            PointF(transformedCubic.control1X, transformedCubic.control1Y))
        assertPointsEqualish(PointF(cubic.anchor1X, cubic.anchor1Y) + translationVector,
            PointF(transformedCubic.anchor1X, transformedCubic.anchor1Y))
    }
}
