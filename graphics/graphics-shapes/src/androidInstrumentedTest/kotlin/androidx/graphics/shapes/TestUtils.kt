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

import android.graphics.Bitmap
import androidx.core.graphics.get
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

private val Epsilon = 1e-4f

// Test equality within Epsilon
internal fun assertPointsEqualish(expected: Point, actual: Point) {
    val msg = "$expected vs. $actual"
    assertEquals(msg, expected.x, actual.x, Epsilon)
    assertEquals(msg, expected.y, actual.y, Epsilon)
}

internal fun equalish(f0: Float, f1: Float, epsilon: Float): Boolean {
    return abs(f0 - f1) < epsilon
}

internal fun pointsEqualish(p0: Point, p1: Point): Boolean {
    return equalish(p0.x, p1.x, Epsilon) && equalish(p0.y, p1.y, Epsilon)
}

internal fun cubicsEqualish(c0: Cubic, c1: Cubic): Boolean {
    return pointsEqualish(Point(c0.anchor0X, c0.anchor0Y), Point(c1.anchor0X, c1.anchor0Y)) &&
        pointsEqualish(Point(c0.anchor1X, c0.anchor1Y), Point(c1.anchor1X, c1.anchor1Y)) &&
        pointsEqualish(Point(c0.control0X, c0.control0Y), Point(c1.control0X, c1.control0Y)) &&
        pointsEqualish(Point(c0.control1X, c0.control1Y), Point(c1.control1X, c1.control1Y))
}

internal fun assertCubicsEqualish(expected: Cubic, actual: Cubic) {
    assertPointsEqualish(
        Point(expected.anchor0X, expected.anchor0Y),
        Point(actual.anchor0X, actual.anchor0Y)
    )
    assertPointsEqualish(
        Point(expected.control0X, expected.control0Y),
        Point(actual.control0X, actual.control0Y)
    )
    assertPointsEqualish(
        Point(expected.control1X, expected.control1Y),
        Point(actual.control1X, actual.control1Y)
    )
    assertPointsEqualish(
        Point(expected.anchor1X, expected.anchor1Y),
        Point(actual.anchor1X, actual.anchor1Y)
    )
}

internal fun assertCubicListsEqualish(expected: List<Cubic>, actual: List<Cubic>) {
    assertEquals(expected.size, actual.size)
    for (i in expected.indices) {
        assertCubicsEqualish(expected[i], actual[i])
    }
}

internal fun assertFeaturesEqualish(expected: Feature, actual: Feature) {
    assertCubicListsEqualish(expected.cubics, actual.cubics)
    assertEquals(expected::class, actual::class)

    if (expected is Feature.Corner && actual is Feature.Corner) {
        pointsEqualish(expected.vertex, actual.vertex)
        pointsEqualish(expected.roundedCenter, actual.roundedCenter)
        assertEquals(expected.convex, actual.convex)
    }
}

internal fun assertPolygonsEqualish(expected: RoundedPolygon, actual: RoundedPolygon) {
    assertCubicListsEqualish(expected.cubics, actual.cubics)

    assertEquals(expected.features.size, actual.features.size)
    for (i in expected.features.indices) {
        assertFeaturesEqualish(expected.features[i], actual.features[i])
    }
}

internal fun assertPointGreaterish(expected: Point, actual: Point) {
    assertTrue(actual.x >= expected.x - Epsilon)
    assertTrue(actual.y >= expected.y - Epsilon)
}

internal fun assertPointLessish(expected: Point, actual: Point) {
    assertTrue(actual.x <= expected.x + Epsilon)
    assertTrue(actual.y <= expected.y + Epsilon)
}

internal fun assertEqualish(expected: Float, actual: Float, message: String? = null) {
    assertEquals(message ?: "", expected, actual, Epsilon)
}

internal fun assertInBounds(shape: List<Cubic>, minPoint: Point, maxPoint: Point) {
    for (cubic in shape) {
        assertPointGreaterish(minPoint, Point(cubic.anchor0X, cubic.anchor0Y))
        assertPointLessish(maxPoint, Point(cubic.anchor0X, cubic.anchor0Y))
        assertPointGreaterish(minPoint, Point(cubic.control0X, cubic.control0Y))
        assertPointLessish(maxPoint, Point(cubic.control0X, cubic.control0Y))
        assertPointGreaterish(minPoint, Point(cubic.control1X, cubic.control1Y))
        assertPointLessish(maxPoint, Point(cubic.control1X, cubic.control1Y))
        assertPointGreaterish(minPoint, Point(cubic.anchor1X, cubic.anchor1Y))
        assertPointLessish(maxPoint, Point(cubic.anchor1X, cubic.anchor1Y))
    }
}

internal fun identityTransform() = PointTransformer { x, y -> TransformResult(x, y) }

internal fun scaleTransform(sx: Float, sy: Float) = PointTransformer { x, y ->
    TransformResult(x * sx, y * sy)
}

internal fun translateTransform(dx: Float, dy: Float) = PointTransformer { x, y ->
    TransformResult(x + dx, y + dy)
}

internal fun assertBitmapsEqual(b0: Bitmap, b1: Bitmap) {
    assertEquals(b0.width, b1.width)
    assertEquals(b0.height, b1.height)
    for (row in 0 until b0.height) {
        for (col in 0 until b0.width) {
            assertEquals("Pixels at ($col, $row) not equal", b0.get(col, row), b1.get(col, row))
        }
    }
}
