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

// A copy from
// compose/ui/ui/src/androidDeviceTest/kotlin/androidx/compose/ui/graphics/MeshGradientRendererTest.kt

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class MeshGradientRendererTest {

    @Test
    fun testMeshGradientRendererDraw() {
        val renderer = MeshGradientRenderer()
        val rows = 1
        val columns = 1
        val gradientConfig = MeshGradientConfig(rows, columns)
        gradientConfig.configure {
            setVertex(0, 0, Offset(0f, 0f), Color.Red)
            setVertex(0, 1, Offset(1f, 0f), Color.Red)
            setVertex(1, 0, Offset(0f, 1f), Color.Red)
            setVertex(1, 1, Offset(1f, 1f), Color.Red)
        }

        val width = 100
        val height = 100
        val imageBitmap = ImageBitmap(width, height)

        imageBitmap.drawInto { renderer.apply { draw(gradientConfig) } }

        val pixelMap = imageBitmap.toPixelMap()
        // Should be all red
        for (i in 0 until width) {
            for (j in 0 until height) {
                assertEqualsWithTolerance(Color.Red, pixelMap[i, j], 0.03f)
            }
        }
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
        assertEquals(expected.red, actual.red, tolerance, "Red channel mismatch")
        assertEquals(expected.green, actual.green, tolerance, "Green channel mismatch")
        assertEquals(expected.blue, actual.blue, tolerance, "Blue channel mismatch")
        assertEquals(expected.alpha, actual.alpha, tolerance, "Alpha channel mismatch")
    }
}
