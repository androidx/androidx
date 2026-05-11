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
        assertEquals("Red channel mismatch", expected.red, actual.red, tolerance)
        assertEquals("Green channel mismatch", expected.green, actual.green, tolerance)
        assertEquals("Blue channel mismatch", expected.blue, actual.blue, tolerance)
        assertEquals("Alpha channel mismatch", expected.alpha, actual.alpha, tolerance)
    }
}
