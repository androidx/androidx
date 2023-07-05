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
        assertEquals(p0, PointF(cubic.anchorX0, cubic.anchorY0))
        assertEquals(p1, PointF(cubic.controlX0, cubic.controlY0))
        assertEquals(p2, PointF(cubic.controlX1, cubic.controlY1))
        assertEquals(p3, PointF(cubic.anchorX1, cubic.anchorY1))
    }

    @Test
    fun copyTest() {
        val copy = Cubic(cubic)
        assertEquals(p0, PointF(copy.anchorX0, copy.anchorY0))
        assertEquals(p1, PointF(copy.controlX0, copy.controlY0))
        assertEquals(p2, PointF(copy.controlX1, copy.controlY1))
        assertEquals(p3, PointF(copy.anchorX1, copy.anchorY1))
        assertEquals(PointF(cubic.anchorX0, cubic.anchorY0),
            PointF(copy.anchorX0, copy.anchorY0))
        assertEquals(PointF(cubic.controlX0, cubic.controlY0),
            PointF(copy.controlX0, copy.controlY0))
        assertEquals(PointF(cubic.controlX1, cubic.controlY1),
            PointF(copy.controlX1, copy.controlY1))
        assertEquals(PointF(cubic.anchorX1, cubic.anchorY1),
            PointF(copy.anchorX1, copy.anchorY1))
    }

    @Test
    fun circularArcTest() {
        val arcCubic = Cubic.circularArc(zero.x, zero.y, p0.x, p0.y, p3.x, p3.y)
        assertEquals(p0, PointF(arcCubic.anchorX0, arcCubic.anchorY0))
        assertEquals(p3, PointF(arcCubic.anchorX1, arcCubic.anchorY1))
    }

    @Test
    fun divTest() {
        var divCubic = cubic / 1f
        assertCubicsEqua1ish(cubic, divCubic)
        divCubic = cubic / 1
        assertCubicsEqua1ish(cubic, divCubic)
        divCubic = cubic / 2f
        assertPointsEqualish(p0 / 2f, PointF(divCubic.anchorX0, divCubic.anchorY0))
        assertPointsEqualish(p1 / 2f, PointF(divCubic.controlX0, divCubic.controlY0))
        assertPointsEqualish(p2 / 2f, PointF(divCubic.controlX1, divCubic.controlY1))
        assertPointsEqualish(p3 / 2f, PointF(divCubic.anchorX1, divCubic.anchorY1))
        divCubic = cubic / 2
        assertPointsEqualish(p0 / 2f, PointF(divCubic.anchorX0, divCubic.anchorY0))
        assertPointsEqualish(p1 / 2f, PointF(divCubic.controlX0, divCubic.controlY0))
        assertPointsEqualish(p2 / 2f, PointF(divCubic.controlX1, divCubic.controlY1))
        assertPointsEqualish(p3 / 2f, PointF(divCubic.anchorX1, divCubic.anchorY1))
    }

    @Test
    fun timesTest() {
        var timesCubic = cubic * 1f
        assertEquals(p0, PointF(timesCubic.anchorX0, timesCubic.anchorY0))
        assertEquals(p1, PointF(timesCubic.controlX0, timesCubic.controlY0))
        assertEquals(p2, PointF(timesCubic.controlX1, timesCubic.controlY1))
        assertEquals(p3, PointF(timesCubic.anchorX1, timesCubic.anchorY1))
        timesCubic = cubic * 1
        assertEquals(p0, PointF(timesCubic.anchorX0, timesCubic.anchorY0))
        assertEquals(p1, PointF(timesCubic.controlX0, timesCubic.controlY0))
        assertEquals(p2, PointF(timesCubic.controlX1, timesCubic.controlY1))
        assertEquals(p3, PointF(timesCubic.anchorX1, timesCubic.anchorY1))
        timesCubic = cubic * 2f
        assertPointsEqualish(p0 * 2f, PointF(timesCubic.anchorX0, timesCubic.anchorY0))
        assertPointsEqualish(p1 * 2f, PointF(timesCubic.controlX0, timesCubic.controlY0))
        assertPointsEqualish(p2 * 2f, PointF(timesCubic.controlX1, timesCubic.controlY1))
        assertPointsEqualish(p3 * 2f, PointF(timesCubic.anchorX1, timesCubic.anchorY1))
        timesCubic = cubic * 2
        assertPointsEqualish(p0 * 2f, PointF(timesCubic.anchorX0, timesCubic.anchorY0))
        assertPointsEqualish(p1 * 2f, PointF(timesCubic.controlX0, timesCubic.controlY0))
        assertPointsEqualish(p2 * 2f, PointF(timesCubic.controlX1, timesCubic.controlY1))
        assertPointsEqualish(p3 * 2f, PointF(timesCubic.anchorX1, timesCubic.anchorY1))
    }

    @Test
    fun plusTest() {
        val offsetCubic = cubic * 2f
        var plusCubic = cubic + offsetCubic
        assertPointsEqualish(p0 + PointF(offsetCubic.anchorX0, offsetCubic.anchorY0),
            PointF(plusCubic.anchorX0, plusCubic.anchorY0))
        assertPointsEqualish(p1 + PointF(offsetCubic.controlX0, offsetCubic.controlY0),
            PointF(plusCubic.controlX0, plusCubic.controlY0))
        assertPointsEqualish(p2 + PointF(offsetCubic.controlX1, offsetCubic.controlY1),
            PointF(plusCubic.controlX1, plusCubic.controlY1))
        assertPointsEqualish(p3 + PointF(offsetCubic.anchorX1, offsetCubic.anchorY1),
            PointF(plusCubic.anchorX1, plusCubic.anchorY1))
    }

    @Test
    fun reverseTest() {
        val reverseCubic = cubic.reverse()
        assertEquals(p3, PointF(reverseCubic.anchorX0, reverseCubic.anchorY0))
        assertEquals(p2, PointF(reverseCubic.controlX0, reverseCubic.controlY0))
        assertEquals(p1, PointF(reverseCubic.controlX1, reverseCubic.controlY1))
        assertEquals(p0, PointF(reverseCubic.anchorX1, reverseCubic.anchorY1))
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
        assertEquals(p0, PointF(lineCubic.anchorX0, lineCubic.anchorY0))
        assertEquals(p3, PointF(lineCubic.anchorX1, lineCubic.anchorY1))
        assertBetween(p0, p3, PointF(lineCubic.controlX0, lineCubic.controlY0))
        assertBetween(p0, p3, PointF(lineCubic.controlX1, lineCubic.controlY1))
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
        assertEquals(PointF(cubic.anchorX0, cubic.anchorY0),
            PointF(split0.anchorX0, split0.anchorY0))
        assertEquals(PointF(cubic.anchorX1, cubic.anchorY1),
            PointF(split1.anchorX1, split1.anchorY1))
        assertBetween(PointF(cubic.anchorX0, cubic.anchorY0),
            PointF(cubic.anchorX1, cubic.anchorY1),
            PointF(split0.anchorX1, split0.anchorY1))
        assertBetween(PointF(cubic.anchorX0, cubic.anchorY0),
            PointF(cubic.anchorX1, cubic.anchorY1),
            PointF(split1.anchorX0, split1.anchorY0))
    }

    @Test
    fun pointOnCurveTest() {
        var halfway = cubic.pointOnCurve(.5f)
        assertBetween(PointF(cubic.anchorX0, cubic.anchorY0),
            PointF(cubic.anchorX1, cubic.anchorY1), halfway)
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
        assertPointsEqualish(PointF(cubic.anchorX0, cubic.anchorY0) + translationVector,
            PointF(transformedCubic.anchorX0, transformedCubic.anchorY0))
        assertPointsEqualish(PointF(cubic.controlX0, cubic.controlY0) + translationVector,
            PointF(transformedCubic.controlX0, transformedCubic.controlY0))
        assertPointsEqualish(PointF(cubic.controlX1, cubic.controlY1) + translationVector,
            PointF(transformedCubic.controlX1, transformedCubic.controlY1))
        assertPointsEqualish(PointF(cubic.anchorX1, cubic.anchorY1) + translationVector,
            PointF(transformedCubic.anchorX1, transformedCubic.anchorY1))
    }
}
