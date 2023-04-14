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
import androidx.core.graphics.minus
import androidx.test.filters.SmallTest
import kotlin.AssertionError
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests the utility shape-creating functions like Circle and Star
 */
@SmallTest
class ShapesTest {

    val Zero = PointF(0f, 0f)
    val Epsilon = .01f

    fun distance(start: PointF, end: PointF): Float {
        val vector = end - start
        return sqrt(vector.x * vector.x + vector.y * vector.y)
    }

    /**
     * Test that the given point is radius distance away from [center]. If two radii are provided
     * it is sufficient to lie on either one (used for testing points on stars).
     */
    fun assertPointOnRadii(
        point: PointF,
        radius1: Float,
        radius2: Float = radius1,
        center: PointF = Zero
    ) {
        val dist = distance(center, point)
        try {
            assertEquals(radius1, dist, Epsilon)
        } catch (e: AssertionError) {
            assertEquals(radius2, dist, Epsilon)
        }
    }

    fun assertCubicOnRadii(
        cubic: Cubic,
        radius1: Float,
        radius2: Float = radius1,
        center: PointF = Zero
    ) {
        assertPointOnRadii(cubic.p0, radius1, radius2, center)
        assertPointOnRadii(cubic.p3, radius1, radius2, center)
    }

    /**
     * Tests points along the curve of the cubic by comparing the distance from that point to the
     * center, compared to the requested radius. The test is very lenient since the Circle shape is
     * only a 4x cubic approximation of the circle and varies from the true circle.
     */
    fun assertCircularCubic(cubic: Cubic, radius: Float, center: PointF) {
        var t = 0f
        while (t <= 1f) {
            val pointOnCurve = cubic.pointOnCurve(t)
            val distanceToPoint = distance(center, pointOnCurve)
            assertEquals(radius, distanceToPoint, Epsilon)
            t += .1f
        }
    }

    fun assertCircleShape(shape: CubicShape, radius: Float = 1f, center: PointF = Zero) {
        for (cubic in shape.cubics) {
            assertCircularCubic(cubic, radius, center)
        }
    }

    @Test
    fun circleTest() {
        val circle = RoundedPolygon.circle()
        assertCircleShape(circle.toCubicShape())

        val bigCircle = RoundedPolygon.circle(radius = 3f)
        assertCircleShape(bigCircle.toCubicShape(), radius = 3f)

        val center = PointF(1f, 2f)
        val offsetCircle = RoundedPolygon.circle(center = center)
        assertCircleShape(offsetCircle.toCubicShape(), center = center)
    }

    /**
     * Stars are complicated. For the unrounded version, we can check whether the vertices
     * are the right distance from the center. For the rounded versions, just check that the
     * shape is within the appropriate bounds.
     */
    @Test
    fun starTest() {
        var star = RoundedPolygon.star(4, innerRadius = .5f)
        var shape = star.toCubicShape()
        var radius = 1f
        var innerRadius = .5f
        for (cubic in shape.cubics) {
            assertCubicOnRadii(cubic, radius, innerRadius)
        }

        val center = PointF(1f, 2f)
        star = RoundedPolygon.star(4, innerRadius = innerRadius, center = center)
        shape = star.toCubicShape()
        for (cubic in shape.cubics) {
            assertCubicOnRadii(cubic, radius, innerRadius, center)
        }

        radius = 4f
        innerRadius = 2f
        star = RoundedPolygon.star(4, radius, innerRadius)
        shape = star.toCubicShape()
        for (cubic in shape.cubics) {
            assertCubicOnRadii(cubic, radius, innerRadius)
        }
    }

    @Test
    fun roundedStarTest() {
        val rounding = CornerRounding(.1f)
        val innerRounding = CornerRounding(.2f)
        val perVtxRounded = listOf<CornerRounding>(rounding, innerRounding, rounding, innerRounding,
            rounding, innerRounding, rounding, innerRounding)

        var star = RoundedPolygon.star(4, innerRadius = .5f, rounding = rounding)
        val min = PointF(-1f, -1f)
        val max = PointF(1f, 1f)
        assertInBounds(star.toCubicShape(), min, max)

        star = RoundedPolygon.star(4, innerRadius = .5f, innerRounding = innerRounding)
        assertInBounds(star.toCubicShape(), min, max)

        star = RoundedPolygon.star(
            4, innerRadius = .5f, rounding = rounding,
            innerRounding = innerRounding
        )
        assertInBounds(star.toCubicShape(), min, max)

        star = RoundedPolygon.star(4, innerRadius = .5f, perVertexRounding = perVtxRounded)
        assertInBounds(star.toCubicShape(), min, max)

        assertThrows(IllegalArgumentException::class.java) {
            star = RoundedPolygon.star(
                6, innerRadius = .5f,
                perVertexRounding = perVtxRounded
            )
        }
    }
}