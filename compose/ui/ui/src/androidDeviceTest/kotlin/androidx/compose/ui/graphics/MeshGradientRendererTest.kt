/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeshGradientRendererTest {

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMeshGradientRendererDraw() {
        val renderer = MeshGradientRenderer()
        val rows = 1
        val columns = 1
        val positions =
            floatArrayOf(
                0f,
                0f, // (0,0)
                1f,
                0f, // (0,1)
                0f,
                1f, // (1,0)
                1f,
                1f, // (1,1)
            )
        val colors =
            intArrayOf(
                Color.Red.toArgb(),
                Color.Red.toArgb(),
                Color.Red.toArgb(),
                Color.Red.toArgb(),
            )

        val width = 100
        val height = 100
        val imageBitmap = ImageBitmap(width, height)

        imageBitmap.drawInto {
            renderer.apply {
                draw(
                    rows = rows,
                    columns = columns,
                    hasBicubicColor = false,
                    positions = positions,
                    colors = colors,
                    leftBezierOffsets = null,
                    topBezierOffsets = null,
                    rightBezierOffsets = null,
                    bottomBezierOffsets = null,
                )
            }
        }

        val pixelMap = imageBitmap.toPixelMap()
        // Should be all red
        for (i in 0 until width) {
            for (j in 0 until height) {
                assertEqualsWithTolerance(Color.Red, pixelMap[i, j], 0.03f)
            }
        }
    }

    @Test
    fun testInferBezierControlPoints() {
        val rows = 1
        val columns = 2
        val numPoints = (rows + 1) * (columns + 1)
        val positions = FloatArray(numPoints * 2)

        positions.setOffset(0, Offset(0f, 0f))
        positions.setOffset(2, Offset(0.5f, 0f))
        positions.setOffset(4, Offset(1f, 0f))
        positions.setOffset(6, Offset(0f, 1f))
        positions.setOffset(8, Offset(0.5f, 1f))
        positions.setOffset(10, Offset(1f, 1f))

        val rightOffsets = FloatArray(numPoints * 2) { Offset.Unspecified.x }
        val leftOffsets = FloatArray(numPoints * 2) { Offset.Unspecified.x }

        // Testing horizontal inference
        inferBezierControlPointsIfRequired(
            rows = rows,
            columns = columns,
            positions = positions,
            leftBezierOffsets = leftOffsets,
            topBezierOffsets = null,
            rightBezierOffsets = rightOffsets,
            bottomBezierOffsets = null,
        )

        // For the middle point (0, 1) in the grid (index 1), neighbors are (0,0) and (0,2).
        // Distance is 0.5. horizontalVector = (right - left).normalized() * 0.33f = (1,0) * 0.33f =
        // (0.33, 0)
        // rightControlPoint = horizontalVector * distance = (0.33, 0) * 0.5 = (0.165, 0)
        val middlePointIndex = 1
        val offsetIndex = middlePointIndex * 2

        val expectedRightOffset = 0.33f * 0.5f
        assertEquals(expectedRightOffset, rightOffsets[offsetIndex], 0.01f)
        assertEquals(0f, rightOffsets[offsetIndex + 1], 0.01f)

        val expectedLeftOffset = -0.33f * 0.5f
        assertEquals(expectedLeftOffset, leftOffsets[offsetIndex], 0.01f)
        assertEquals(0f, leftOffsets[offsetIndex + 1], 0.01f)
    }

    private fun FloatArray.setOffset(index: Int, offset: Offset) {
        this[index] = offset.x
        this[index + 1] = offset.y
    }

    private fun ImageBitmap.drawInto(block: DrawScope.() -> Unit) =
        CanvasDrawScope()
            .draw(
                Density(1.0f),
                LayoutDirection.Ltr,
                Canvas(this),
                Size(width.toFloat(), height.toFloat()),
                block,
            )

    private fun assertEqualsWithTolerance(expected: Color, actual: Color, tolerance: Float = 0.0f) {
        assertEquals("Red channel mismatch", expected.red, actual.red, tolerance)
        assertEquals("Green channel mismatch", expected.green, actual.green, tolerance)
        assertEquals("Blue channel mismatch", expected.blue, actual.blue, tolerance)
        assertEquals("Alpha channel mismatch", expected.alpha, actual.alpha, tolerance)
    }
}
