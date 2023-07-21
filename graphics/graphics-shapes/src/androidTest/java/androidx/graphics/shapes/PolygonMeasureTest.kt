/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class PolygonMeasureTest {
    @Test
    fun triangleAngleMeasure() = polygonAngleMeasure(3)

    @Test
    fun pentagonAngleMeasure() = polygonAngleMeasure(5)

    @Test
    fun dodecagonAngleMeasure() = polygonAngleMeasure(12)

    @Test
    fun irregularTriangleAngleMeasure() = irregularPolygonAngleMeasure(
        RoundedPolygon(
            vertices = listOf(
                PointF(0f, -1f),
                PointF(1f, 1f),
                PointF(0f, 0.5f),
                PointF(-1f, 1f)
            ),
            perVertexRounding = listOf(
                CornerRounding(0.2f, 0.5f),
                CornerRounding(0.2f, 0.5f),
                CornerRounding(0.4f, 0f),
                CornerRounding(0.2f, 0.5f),
            )
        )
    )

    @Test
    fun quarterAngleMeasure() = irregularPolygonAngleMeasure(
        RoundedPolygon(
            vertices = listOf(
                PointF(-1f, -1f),
                PointF(1f, -1f),
                PointF(1f, 1f),
                PointF(-1f, 1f)
            ),
            perVertexRounding = listOf(
                CornerRounding.Unrounded,
                CornerRounding.Unrounded,
                CornerRounding(0.5f, 0.5f),
                CornerRounding.Unrounded,
            )
        )
    )

    private fun polygonAngleMeasure(sides: Int) {
        val polygon = RoundedPolygon(sides)
        val measurer = AngleMeasurer(polygon.center)

        val measuredPolygon = MeasuredPolygon.measurePolygon(measurer, polygon)

        assertEquals(sides, measuredPolygon.size)

        assertEquals(0f, measuredPolygon.first().startOutlineProgress)
        assertEquals(1f, measuredPolygon.last().endOutlineProgress)
        measuredPolygon.forEachIndexed { index, measuredCubic ->
            assertEqualish(index.toFloat() / sides, measuredCubic.startOutlineProgress)
        }
    }

    private fun irregularPolygonAngleMeasure(polygon: RoundedPolygon) {
        val measurer = AngleMeasurer(polygon.center)

        val measuredPolygon = MeasuredPolygon.measurePolygon(measurer, polygon)

        assertEquals(0f, measuredPolygon.first().startOutlineProgress)
        assertEquals(1f, measuredPolygon.last().endOutlineProgress)
        measuredPolygon.forEachIndexed { index, measuredCubic ->
            if (index > 0) {
                assertEquals(
                    measuredPolygon[index - 1].endOutlineProgress,
                    measuredCubic.startOutlineProgress
                )
            }
            assertTrue(measuredCubic.endOutlineProgress >= measuredCubic.startOutlineProgress)
        }
    }
}