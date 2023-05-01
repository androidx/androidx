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
}