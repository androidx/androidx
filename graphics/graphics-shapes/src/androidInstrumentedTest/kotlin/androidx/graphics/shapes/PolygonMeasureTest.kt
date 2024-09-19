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

import androidx.test.filters.SmallTest
import kotlin.math.PI
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class PolygonMeasureTest {
    private val measurer = LengthMeasurer()

    @Test fun measureSharpTriangle() = regularPolygonMeasure(3)

    @Test fun measureSharpPentagon() = regularPolygonMeasure(5)

    @Test fun measureSharpOctagon() = regularPolygonMeasure(8)

    @Test fun measureSharpDodecagon() = regularPolygonMeasure(12)

    @Test fun measureSharpIcosagon() = regularPolygonMeasure(20)

    @Test
    fun measureSlightlyRoundedHexagon() {
        irregularPolygonMeasure(RoundedPolygon(6, rounding = CornerRounding(0.15f)))
    }

    @Test
    fun measureMediumRoundedHexagon() {
        irregularPolygonMeasure(RoundedPolygon(6, rounding = CornerRounding(0.5f)))
    }

    @Test
    fun measureMaximumRoundedHexagon() {
        irregularPolygonMeasure(RoundedPolygon(6, rounding = CornerRounding(1f)))
    }

    @Test
    fun measureCircle() {
        // White box test: As the length measurer approximates arcs by linear segments,
        // this test validates if the chosen segment count approximates the arc length up to
        // an error of 1.5% from the true length
        val vertices = 4
        val polygon = RoundedPolygon.circle(numVertices = vertices)

        val actualLength = polygon.cubics.sumOf { LengthMeasurer().measureCubic(it).toDouble() }
        val expectedLength = 2 * PI

        assertEquals(expectedLength, actualLength, 0.015f * expectedLength)
    }

    @Test
    fun irregularTriangleAngleMeasure() =
        irregularPolygonMeasure(
            RoundedPolygon(
                vertices = floatArrayOf(0f, -1f, 1f, 1f, 0f, 0.5f, -1f, 1f),
                perVertexRounding =
                    listOf(
                        CornerRounding(0.2f, 0.5f),
                        CornerRounding(0.2f, 0.5f),
                        CornerRounding(0.4f, 0f),
                        CornerRounding(0.2f, 0.5f),
                    )
            )
        )

    @Test
    fun quarterAngleMeasure() =
        irregularPolygonMeasure(
            RoundedPolygon(
                vertices = floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f),
                perVertexRounding =
                    listOf(
                        CornerRounding.Unrounded,
                        CornerRounding.Unrounded,
                        CornerRounding(0.5f, 0.5f),
                        CornerRounding.Unrounded,
                    )
            )
        )

    @Test
    fun hourGlassMeasure() {
        // Regression test: Legacy measurer (AngleMeasurer) would skip the diagonal sides
        // as they are 0 degrees from the center.
        val unit = 1f
        val coordinates =
            floatArrayOf(
                // lower glass
                0f,
                0f,
                unit,
                unit,
                -unit,
                unit,

                // upper glass
                0f,
                0f,
                -unit,
                -unit,
                unit,
                -unit,
            )

        val diagonal = sqrt(unit * unit + unit * unit)
        val horizontal = 2 * unit
        val total = 4 * diagonal + 2 * horizontal

        val polygon = RoundedPolygon(coordinates)
        customPolygonMeasure(
            polygon,
            floatArrayOf(
                diagonal / total,
                horizontal / total,
                diagonal / total,
                diagonal / total,
                horizontal / total,
                diagonal / total,
            )
        )
    }

    private fun regularPolygonMeasure(
        sides: Int,
        rounding: CornerRounding = CornerRounding.Unrounded
    ) {
        val polygon = RoundedPolygon(sides, rounding = rounding)
        val measuredPolygon = MeasuredPolygon.measurePolygon(measurer, polygon)

        assertEquals(sides, measuredPolygon.size)

        assertEquals(0f, measuredPolygon.first().startOutlineProgress)
        assertEquals(1f, measuredPolygon.last().endOutlineProgress)
        measuredPolygon.forEachIndexed { index, measuredCubic ->
            assertEqualish(index.toFloat() / sides, measuredCubic.startOutlineProgress)
        }
    }

    private fun irregularPolygonMeasure(polygon: RoundedPolygon) {
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

    private fun customPolygonMeasure(polygon: RoundedPolygon, progresses: FloatArray) {
        irregularPolygonMeasure(polygon)

        val measuredPolygon = MeasuredPolygon.measurePolygon(measurer, polygon)
        require(measuredPolygon.size == progresses.size)

        measuredPolygon.forEachIndexed { index, measuredCubic ->
            assertEqualish(
                progresses[index],
                measuredCubic.endOutlineProgress - measuredCubic.startOutlineProgress
            )
        }
    }
}
