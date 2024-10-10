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

import androidx.test.filters.SmallTest
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class CubicTest {

    // These points create a roughly circular arc in the upper-right quadrant around (0,0)
    private val zero = Point(0f, 0f)
    private val p0 = Point(1f, 0f)
    private val p1 = Point(1f, .5f)
    private val p2 = Point(.5f, 1f)
    private val p3 = Point(0f, 1f)
    val cubic = Cubic(p0, p1, p2, p3)

    @Test
    fun constructionTest() {
        assertEquals(p0, Point(cubic.anchor0X, cubic.anchor0Y))
        assertEquals(p1, Point(cubic.control0X, cubic.control0Y))
        assertEquals(p2, Point(cubic.control1X, cubic.control1Y))
        assertEquals(p3, Point(cubic.anchor1X, cubic.anchor1Y))
    }

    @Test
    fun circularArcTest() {
        val arcCubic = Cubic.circularArc(zero.x, zero.y, p0.x, p0.y, p3.x, p3.y)
        assertEquals(p0, Point(arcCubic.anchor0X, arcCubic.anchor0Y))
        assertEquals(p3, Point(arcCubic.anchor1X, arcCubic.anchor1Y))
    }

    @Test
    fun divTest() {
        var divCubic = cubic / 1f
        assertCubicsEqualish(cubic, divCubic)
        divCubic = cubic / 1
        assertCubicsEqualish(cubic, divCubic)
        divCubic = cubic / 2f
        assertPointsEqualish(p0 / 2f, Point(divCubic.anchor0X, divCubic.anchor0Y))
        assertPointsEqualish(p1 / 2f, Point(divCubic.control0X, divCubic.control0Y))
        assertPointsEqualish(p2 / 2f, Point(divCubic.control1X, divCubic.control1Y))
        assertPointsEqualish(p3 / 2f, Point(divCubic.anchor1X, divCubic.anchor1Y))
        divCubic = cubic / 2
        assertPointsEqualish(p0 / 2f, Point(divCubic.anchor0X, divCubic.anchor0Y))
        assertPointsEqualish(p1 / 2f, Point(divCubic.control0X, divCubic.control0Y))
        assertPointsEqualish(p2 / 2f, Point(divCubic.control1X, divCubic.control1Y))
        assertPointsEqualish(p3 / 2f, Point(divCubic.anchor1X, divCubic.anchor1Y))
    }

    @Test
    fun timesTest() {
        var timesCubic = cubic * 1f
        assertEquals(p0, Point(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertEquals(p1, Point(timesCubic.control0X, timesCubic.control0Y))
        assertEquals(p2, Point(timesCubic.control1X, timesCubic.control1Y))
        assertEquals(p3, Point(timesCubic.anchor1X, timesCubic.anchor1Y))
        timesCubic = cubic * 1
        assertEquals(p0, Point(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertEquals(p1, Point(timesCubic.control0X, timesCubic.control0Y))
        assertEquals(p2, Point(timesCubic.control1X, timesCubic.control1Y))
        assertEquals(p3, Point(timesCubic.anchor1X, timesCubic.anchor1Y))
        timesCubic = cubic * 2f
        assertPointsEqualish(p0 * 2f, Point(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertPointsEqualish(p1 * 2f, Point(timesCubic.control0X, timesCubic.control0Y))
        assertPointsEqualish(p2 * 2f, Point(timesCubic.control1X, timesCubic.control1Y))
        assertPointsEqualish(p3 * 2f, Point(timesCubic.anchor1X, timesCubic.anchor1Y))
        timesCubic = cubic * 2
        assertPointsEqualish(p0 * 2f, Point(timesCubic.anchor0X, timesCubic.anchor0Y))
        assertPointsEqualish(p1 * 2f, Point(timesCubic.control0X, timesCubic.control0Y))
        assertPointsEqualish(p2 * 2f, Point(timesCubic.control1X, timesCubic.control1Y))
        assertPointsEqualish(p3 * 2f, Point(timesCubic.anchor1X, timesCubic.anchor1Y))
    }

    @Test
    fun plusTest() {
        val offsetCubic = cubic * 2f
        var plusCubic = cubic + offsetCubic
        assertPointsEqualish(
            p0 + Point(offsetCubic.anchor0X, offsetCubic.anchor0Y),
            Point(plusCubic.anchor0X, plusCubic.anchor0Y)
        )
        assertPointsEqualish(
            p1 + Point(offsetCubic.control0X, offsetCubic.control0Y),
            Point(plusCubic.control0X, plusCubic.control0Y)
        )
        assertPointsEqualish(
            p2 + Point(offsetCubic.control1X, offsetCubic.control1Y),
            Point(plusCubic.control1X, plusCubic.control1Y)
        )
        assertPointsEqualish(
            p3 + Point(offsetCubic.anchor1X, offsetCubic.anchor1Y),
            Point(plusCubic.anchor1X, plusCubic.anchor1Y)
        )
    }

    @Test
    fun reverseTest() {
        val reverseCubic = cubic.reverse()
        assertEquals(p3, Point(reverseCubic.anchor0X, reverseCubic.anchor0Y))
        assertEquals(p2, Point(reverseCubic.control0X, reverseCubic.control0Y))
        assertEquals(p1, Point(reverseCubic.control1X, reverseCubic.control1Y))
        assertEquals(p0, Point(reverseCubic.anchor1X, reverseCubic.anchor1Y))
    }

    private fun assertBetween(end0: Point, end1: Point, actual: Point) {
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
        assertEquals(p0, Point(lineCubic.anchor0X, lineCubic.anchor0Y))
        assertEquals(p3, Point(lineCubic.anchor1X, lineCubic.anchor1Y))
        assertBetween(p0, p3, Point(lineCubic.control0X, lineCubic.control0Y))
        assertBetween(p0, p3, Point(lineCubic.control1X, lineCubic.control1Y))
    }

    @Test
    fun splitTest() {
        val (split0, split1) = cubic.split(.5f)
        assertEquals(Point(cubic.anchor0X, cubic.anchor0Y), Point(split0.anchor0X, split0.anchor0Y))
        assertEquals(Point(cubic.anchor1X, cubic.anchor1Y), Point(split1.anchor1X, split1.anchor1Y))
        assertBetween(
            Point(cubic.anchor0X, cubic.anchor0Y),
            Point(cubic.anchor1X, cubic.anchor1Y),
            Point(split0.anchor1X, split0.anchor1Y)
        )
        assertBetween(
            Point(cubic.anchor0X, cubic.anchor0Y),
            Point(cubic.anchor1X, cubic.anchor1Y),
            Point(split1.anchor0X, split1.anchor0Y)
        )
    }

    @Test
    fun pointOnCurveTest() {
        var halfway = cubic.pointOnCurve(.5f)
        assertBetween(
            Point(cubic.anchor0X, cubic.anchor0Y),
            Point(cubic.anchor1X, cubic.anchor1Y),
            halfway
        )
        val straightLineCubic = Cubic.straightLine(p0.x, p0.y, p3.x, p3.y)
        halfway = straightLineCubic.pointOnCurve(.5f)
        val computedHalfway = Point(p0.x + .5f * (p3.x - p0.x), p0.y + .5f * (p3.y - p0.y))
        assertPointsEqualish(computedHalfway, halfway)
    }

    @Test
    fun transformTest() {
        var transform = identityTransform()
        var transformedCubic = cubic.transformed(transform)
        assertCubicsEqualish(cubic, transformedCubic)

        transform = scaleTransform(3f, 3f)
        transformedCubic = cubic.transformed(transform)
        assertCubicsEqualish(cubic * 3f, transformedCubic)

        val tx = 200f
        val ty = 300f
        val translationVector = Point(tx, ty)
        transform = translateTransform(tx, ty)
        transformedCubic = cubic.transformed(transform)
        assertPointsEqualish(
            Point(cubic.anchor0X, cubic.anchor0Y) + translationVector,
            Point(transformedCubic.anchor0X, transformedCubic.anchor0Y)
        )
        assertPointsEqualish(
            Point(cubic.control0X, cubic.control0Y) + translationVector,
            Point(transformedCubic.control0X, transformedCubic.control0Y)
        )
        assertPointsEqualish(
            Point(cubic.control1X, cubic.control1Y) + translationVector,
            Point(transformedCubic.control1X, transformedCubic.control1Y)
        )
        assertPointsEqualish(
            Point(cubic.anchor1X, cubic.anchor1Y) + translationVector,
            Point(transformedCubic.anchor1X, transformedCubic.anchor1Y)
        )
    }

    @Test
    fun emptyCubicHasZeroLength() {
        assertTrue(Cubic.empty(10f, 10f).zeroLength())
    }
}
