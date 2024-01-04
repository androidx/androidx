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

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

private val Epsilon = 1e-4f

// Test equality within Epsilon
fun assertPointsEqualish(expected: PointF, actual: PointF) {
    assertEquals(expected.x, actual.x, Epsilon)
    assertEquals(expected.y, actual.y, Epsilon)
}

fun assertCubicsEqua1ish(expected: Cubic, actual: Cubic) {
    assertPointsEqualish(PointF(expected.anchor0X, expected.anchor0Y),
        PointF(actual.anchor0X, actual.anchor0Y))
    assertPointsEqualish(PointF(expected.control0X, expected.control0Y),
        PointF(actual.control0X, actual.control0Y))
    assertPointsEqualish(PointF(expected.control1X, expected.control1Y),
        PointF(actual.control1X, actual.control1Y))
    assertPointsEqualish(PointF(expected.anchor1X, expected.anchor1Y),
        PointF(actual.anchor1X, actual.anchor1Y))
}

fun assertPointGreaterish(expected: PointF, actual: PointF) {
    assertTrue(actual.x >= expected.x - Epsilon)
    assertTrue(actual.y >= expected.y - Epsilon)
}

fun assertPointLessish(expected: PointF, actual: PointF) {
    assertTrue(actual.x <= expected.x + Epsilon)
    assertTrue(actual.y <= expected.y + Epsilon)
}

fun assertEqualish(expected: Float, actual: Float, message: String? = null) {
    assertEquals(message ?: "", expected, actual, Epsilon)
}

fun assertInBounds(shape: CubicShape, minPoint: PointF, maxPoint: PointF) {
    val cubics = shape.cubics
    for (cubic in cubics) {
        assertPointGreaterish(minPoint, PointF(cubic.anchor0X, cubic.anchor0Y))
        assertPointLessish(maxPoint, PointF(cubic.anchor0X, cubic.anchor0Y))
        assertPointGreaterish(minPoint, PointF(cubic.control0X, cubic.control0Y))
        assertPointLessish(maxPoint, PointF(cubic.control0X, cubic.control0Y))
        assertPointGreaterish(minPoint, PointF(cubic.control1X, cubic.control1Y))
        assertPointLessish(maxPoint, PointF(cubic.control1X, cubic.control1Y))
        assertPointGreaterish(minPoint, PointF(cubic.anchor1X, cubic.anchor1Y))
        assertPointLessish(maxPoint, PointF(cubic.anchor1X, cubic.anchor1Y))
    }
}
