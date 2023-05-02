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
    assertPointsEqualish(expected.p0, actual.p0)
    assertPointsEqualish(expected.p1, actual.p1)
    assertPointsEqualish(expected.p2, actual.p2)
    assertPointsEqualish(expected.p3, actual.p3)
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
        assertPointGreaterish(minPoint, cubic.p0)
        assertPointLessish(maxPoint, cubic.p0)
        assertPointGreaterish(minPoint, cubic.p1)
        assertPointLessish(maxPoint, cubic.p1)
        assertPointGreaterish(minPoint, cubic.p2)
        assertPointLessish(maxPoint, cubic.p2)
        assertPointGreaterish(minPoint, cubic.p3)
        assertPointLessish(maxPoint, cubic.p3)
    }
}
