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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

private val Epsilon = 1e-4f

// Test equality within Epsilon
internal fun assertPointsEqualish(expected: Point, actual: Point) {
    val msg = "$expected vs. $actual"
    assertEquals(msg, expected.x, actual.x, Epsilon)
    assertEquals(msg, expected.y, actual.y, Epsilon)
}

internal fun assertCubicsEqualish(expected: Cubic, actual: Cubic) {
    assertPointsEqualish(Point(expected.anchor0X, expected.anchor0Y),
        Point(actual.anchor0X, actual.anchor0Y))
    assertPointsEqualish(Point(expected.control0X, expected.control0Y),
        Point(actual.control0X, actual.control0Y))
    assertPointsEqualish(Point(expected.control1X, expected.control1Y),
        Point(actual.control1X, actual.control1Y))
    assertPointsEqualish(Point(expected.anchor1X, expected.anchor1Y),
        Point(actual.anchor1X, actual.anchor1Y))
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

internal fun identityTransform() = PointTransformer { }

internal fun scaleTransform(sx: Float, sy: Float) = PointTransformer {
    x *= sx
    y *= sy
}

internal fun translateTransform(dx: Float, dy: Float) = PointTransformer {
    x += dx
    y += dy
}
