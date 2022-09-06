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
        assertEquals(p0, cubic.p0)
        assertEquals(p1, cubic.p1)
        assertEquals(p2, cubic.p2)
        assertEquals(p3, cubic.p3)
    }

    @Test
    fun copyTest() {
        val copy = Cubic(cubic)
        assertEquals(p0, copy.p0)
        assertEquals(p1, copy.p1)
        assertEquals(p2, copy.p2)
        assertEquals(p3, copy.p3)
        assertEquals(cubic.p0, copy.p0)
        assertEquals(cubic.p1, copy.p1)
        assertEquals(cubic.p2, copy.p2)
        assertEquals(cubic.p3, copy.p3)
    }

    @Test
    fun circularArcTest() {
        val arcCubic = Cubic.circularArc(zero, p0, p3)
        assertEquals(p0, arcCubic.p0)
        assertEquals(p3, arcCubic.p3)
    }

    @Test
    fun divTest() {
        var divCubic = cubic / 1f
        assertCubicsEqua1ish(cubic, divCubic)
        divCubic = cubic / 1
        assertCubicsEqua1ish(cubic, divCubic)
        divCubic = cubic / 2f
        assertPointsEqualish(p0 / 2f, divCubic.p0)
        assertPointsEqualish(p1 / 2f, divCubic.p1)
        assertPointsEqualish(p2 / 2f, divCubic.p2)
        assertPointsEqualish(p3 / 2f, divCubic.p3)
        divCubic = cubic / 2
        assertPointsEqualish(p0 / 2f, divCubic.p0)
        assertPointsEqualish(p1 / 2f, divCubic.p1)
        assertPointsEqualish(p2 / 2f, divCubic.p2)
        assertPointsEqualish(p3 / 2f, divCubic.p3)
    }

    @Test
    fun timesTest() {
        var timesCubic = cubic * 1f
        assertEquals(p0, timesCubic.p0)
        assertEquals(p1, timesCubic.p1)
        assertEquals(p2, timesCubic.p2)
        assertEquals(p3, timesCubic.p3)
        timesCubic = cubic * 1
        assertEquals(p0, timesCubic.p0)
        assertEquals(p1, timesCubic.p1)
        assertEquals(p2, timesCubic.p2)
        assertEquals(p3, timesCubic.p3)
        timesCubic = cubic * 2f
        assertPointsEqualish(p0 * 2f, timesCubic.p0)
        assertPointsEqualish(p1 * 2f, timesCubic.p1)
        assertPointsEqualish(p2 * 2f, timesCubic.p2)
        assertPointsEqualish(p3 * 2f, timesCubic.p3)
        timesCubic = cubic * 2
        assertPointsEqualish(p0 * 2f, timesCubic.p0)
        assertPointsEqualish(p1 * 2f, timesCubic.p1)
        assertPointsEqualish(p2 * 2f, timesCubic.p2)
        assertPointsEqualish(p3 * 2f, timesCubic.p3)
    }

    @Test
    fun plusTest() {
        val offsetCubic = cubic * 2f
        var plusCubic = cubic + offsetCubic
        assertPointsEqualish(p0 + offsetCubic.p0, plusCubic.p0)
        assertPointsEqualish(p1 + offsetCubic.p1, plusCubic.p1)
        assertPointsEqualish(p2 + offsetCubic.p2, plusCubic.p2)
        assertPointsEqualish(p3 + offsetCubic.p3, plusCubic.p3)
    }

    @Test
    fun reverseTest() {
        val reverseCubic = cubic.reverse()
        assertEquals(p3, reverseCubic.p0)
        assertEquals(p2, reverseCubic.p1)
        assertEquals(p1, reverseCubic.p2)
        assertEquals(p0, reverseCubic.p3)
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
        val lineCubic = Cubic.straightLine(p0, p3)
        assertEquals(p0, lineCubic.p0)
        assertEquals(p3, lineCubic.p3)
        assertBetween(p0, p3, lineCubic.p1)
        assertBetween(p0, p3, lineCubic.p2)
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
        assertEquals(cubic.p0, split0.p0)
        assertEquals(cubic.p3, split1.p3)
        assertBetween(cubic.p0, cubic.p3, split0.p3)
        assertBetween(cubic.p0, cubic.p3, split1.p0)
    }

    @Test
    fun pointOnCurveTest() {
        var halfway = cubic.pointOnCurve(.5f)
        assertBetween(cubic.p0, cubic.p3, halfway)
        val straightLineCubic = Cubic.straightLine(p0, p3)
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
        assertPointsEqualish(cubic.p0 + translationVector, transformedCubic.p0)
        assertPointsEqualish(cubic.p1 + translationVector, transformedCubic.p1)
        assertPointsEqualish(cubic.p2 + translationVector, transformedCubic.p2)
        assertPointsEqualish(cubic.p3 + translationVector, transformedCubic.p3)
    }
}