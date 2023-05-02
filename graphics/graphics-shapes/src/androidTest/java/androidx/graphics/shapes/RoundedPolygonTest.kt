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
import androidx.core.graphics.plus
import androidx.core.graphics.times
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class RoundedPolygonTest {

    val rounding = CornerRounding(.1f)
    val perVtxRounded = listOf(rounding, rounding, rounding, rounding)

    @Test
    fun numVertsConstructorTest() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            RoundedPolygon(2)
        }

        val square = RoundedPolygon(4)
        var min = PointF(-1f, -1f)
        var max = PointF(1f, 1f)
        assertInBounds(square.toCubicShape(), min, max)

        val doubleSquare = RoundedPolygon(4, 2f)
        min *= 2f
        max *= 2f
        assertInBounds(doubleSquare.toCubicShape(), min, max)

        val squareRounded = RoundedPolygon(4, rounding = rounding)
        min = PointF(-1f, -1f)
        max = PointF(1f, 1f)
        assertInBounds(squareRounded.toCubicShape(), min, max)

        val squarePVRounded = RoundedPolygon(4, perVertexRounding = perVtxRounded)
        min = PointF(-1f, -1f)
        max = PointF(1f, 1f)
        assertInBounds(squarePVRounded.toCubicShape(), min, max)
    }

    @Test
    fun verticesConstructorTest() {
        val p0 = PointF(1f, 0f)
        val p1 = PointF(0f, 1f)
        val p2 = PointF(-1f, 0f)
        val p3 = PointF(0f, -1f)

        Assert.assertThrows(IllegalArgumentException::class.java) {
            RoundedPolygon(listOf(p0, p1))
        }

        val manualSquare = RoundedPolygon(listOf(p0, p1, p2, p3))
        var min = PointF(-1f, -1f)
        var max = PointF(1f, 1f)
        assertInBounds(manualSquare.toCubicShape(), min, max)

        val offset = PointF(1f, 2f)
        val manualSquareOffset = RoundedPolygon(
            listOf(p0 + offset, p1 + offset, p2 + offset, p3 + offset), center = offset)
        min = PointF(0f, 1f)
        max = PointF(2f, 3f)
        assertInBounds(manualSquareOffset.toCubicShape(), min, max)

        val manualSquareRounded = RoundedPolygon(listOf(p0, p1, p2, p3), rounding = rounding)
        min = PointF(-1f, -1f)
        max = PointF(1f, 1f)
        assertInBounds(manualSquareRounded.toCubicShape(), min, max)

        val manualSquarePVRounded = RoundedPolygon(listOf(p0, p1, p2, p3),
            perVertexRounding = perVtxRounded)
        min = PointF(-1f, -1f)
        max = PointF(1f, 1f)
        assertInBounds(manualSquarePVRounded.toCubicShape(), min, max)
    }

    @Test
    fun roundingSpaceUsageTest() {
        val p0 = PointF(0f, 0f)
        val p1 = PointF(1f, 0f)
        val p2 = PointF(0.5f, 1f)
        val pvRounding = listOf(
            CornerRounding(1f, 0f),
            CornerRounding(1f, 1f),
            CornerRounding.Unrounded,
        )
        val polygon = RoundedPolygon(
            vertices = listOf(p0, p1, p2),
            perVertexRounding = pvRounding
        )

        // Since there is not enough room in the p0 -> p1 side even for the roundings, we shouldn't
        // take smoothing into account, so the corners should end in the middle point.
        val lowerEdgeFeature = polygon.features.first { it is RoundedPolygon.Edge }
            as RoundedPolygon.Edge
        assertEquals(1, lowerEdgeFeature.cubics.size)

        val lowerEdge = lowerEdgeFeature.cubics.first()
        assertEqualish(0.5f, lowerEdge.p0.x)
        assertEqualish(0.0f, lowerEdge.p0.y)
        assertEqualish(0.5f, lowerEdge.p3.x)
        assertEqualish(0.0f, lowerEdge.p3.y)
    }

    /*
     * In the following tests, we check how much was cut for the top left (vertex 0) and bottom
     * left corner (vertex 3).
     * In particular, both vertex are competing for space in the left side.
     *
     *   Vertex 0            Vertex 1
     *      *---------------------*
     *      |                     |
     *      *---------------------*
     *   Vertex 3            Vertex 2
     */
    private val points = 20

    @Test
    fun unevenSmoothingTest() {
        // Vertex 3 has the default 0.5 radius, 0 smoothing.
        // Vertex 0 has 0.4 radius, and smoothing varying from 0 to 1.
        repeat(points + 1) {
            val smooth = it.toFloat() / points
            doUnevenSmoothTest(
                CornerRounding(0.4f, smooth),
                expectedV0SX = 0.4f * (1 + smooth),
                expectedV0SY = (0.4f * (1 + smooth)).coerceAtMost(0.5f),
                expectedV3SY = 0.5f,
            )
        }
    }

    @Test
    fun unevenSmoothingTest2() {
        // Vertex 3 has 0.2f radius and 0.2f smoothing, so it takes at most 0.4f
        // Vertex 0 has 0.4f radius and smoothing varies from 0 to 1, when it reaches 0.5 it starts
        // competing with vertex 3 for space.
        repeat(points + 1) {
            val smooth = it.toFloat() / points

            val smoothWantedV0 = 0.4f * smooth
            val smoothWantedV3 = 0.2f

            // There is 0.4f room for smoothing
            val factor = (0.4f / (smoothWantedV0 + smoothWantedV3)).coerceAtMost(1f)
            doUnevenSmoothTest(
                CornerRounding(0.4f, smooth),
                expectedV0SX = 0.4f * (1 + smooth),
                expectedV0SY = 0.4f + factor * smoothWantedV0,
                expectedV3SY = 0.2f + factor * smoothWantedV3,
                rounding3 = CornerRounding(0.2f, 1f)
            )
        }
    }

    @Test
    fun unevenSmoothingTest3() {
        // Vertex 3 has 0.6f radius.
        // Vertex 0 has 0.4f radius and smoothing varies from 0 to 1. There is no room for smoothing
        // on the segment between these vertices, but vertex 0 can still have smoothing on the top
        // side.
        repeat(points + 1) {
            val smooth = it.toFloat() / points

            doUnevenSmoothTest(
                CornerRounding(0.4f, smooth),
                expectedV0SX = 0.4f * (1 + smooth),
                expectedV0SY = 0.4f,
                expectedV3SY = 0.6f,
                rounding3 = CornerRounding(0.6f)
            )
        }
    }

    private fun doUnevenSmoothTest(
        // Corner rounding parameter for vertex 0 (top left)
        rounding0: CornerRounding,
        expectedV0SX: Float, // Expected total cut from vertex 0 towards vertex 1
        expectedV0SY: Float, // Expected total cut from vertex 0 towards vertex 3
        expectedV3SY: Float, // Expected total cut from vertex 3 towards vertex 0
        // Corner rounding parameter for vertex 3 (bottom left)
        rounding3: CornerRounding = CornerRounding(0.5f)
    ) {
        val p0 = PointF(0f, 0f)
        val p1 = PointF(5f, 0f)
        val p2 = PointF(5f, 1f)
        val p3 = PointF(0f, 1f)

        val pvRounding = listOf(
            rounding0,
            CornerRounding.Unrounded,
            CornerRounding.Unrounded,
            rounding3,
        )
        val polygon = RoundedPolygon(
            vertices = listOf(p0, p1, p2, p3),
            perVertexRounding = pvRounding
        )
        val (e01, _, _, e30) = polygon.features.filterIsInstance<RoundedPolygon.Edge>()
        val msg = "r0 = ${show(rounding0)}, r3 = ${show(rounding3)}"
        assertEqualish(expectedV0SX, e01.cubics.first().p0.x, msg)
        assertEqualish(expectedV0SY, e30.cubics.first().p3.y, msg)
        assertEqualish(expectedV3SY, 1f - e30.cubics.first().p0.y, msg)
    }

    private fun show(cr: CornerRounding) = "(r=${cr.radius}, s=${cr.smoothing})"
}
