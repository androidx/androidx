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

package androidx.pdf.annotation.drawer

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.pdf.annotation.models.HighlightAnnotation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCanvas

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HighlightAnnotationDrawerTest {

    private lateinit var highlightAnnotationDrawer: HighlightAnnotationDrawer
    private lateinit var canvas: Canvas
    private lateinit var shadowCanvas: ShadowCanvas
    private lateinit var transform: Matrix

    @Before
    fun setUp() {
        highlightAnnotationDrawer = HighlightAnnotationDrawer()
        canvas = Canvas()
        shadowCanvas = Shadows.shadowOf(canvas)
        transform = Matrix()
    }

    @Test
    fun draw_withMultipleBounds_drawsRectsOnCanvas() {
        val bounds = listOf(RectF(0f, 0f, 10f, 20f), RectF(50f, 50f, 100f, 100f))
        val color = Color.YELLOW
        val annotation = HighlightAnnotation(pageNum = 0, bounds = bounds, color = color)

        highlightAnnotationDrawer.draw(annotation, canvas, transform)

        // Verify the number of rectangles drawn
        val drawnRectsCount = shadowCanvas.rectPaintHistoryCount
        assertThat(drawnRectsCount).isEqualTo(2)

        for (i in 0 until drawnRectsCount) {
            // Check the dimensions of the drawn rectangles
            assertThat(shadowCanvas.getDrawnRect(0).rect).isEqualTo(bounds[0])
            assertThat(shadowCanvas.getDrawnRect(1).rect).isEqualTo(bounds[1])

            val paint = shadowCanvas.getDrawnRect(i).paint
            assertThat(paint.color).isEqualTo(color)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertThat(paint.blendMode).isEqualTo(BlendMode.MULTIPLY)
            }
            assertThat(paint.style).isEqualTo(Paint.Style.FILL)
        }
    }

    @Test
    fun draw_appliesTransformToBounds() {
        val bounds = listOf(RectF(10f, 10f, 20f, 20f))
        val annotation = HighlightAnnotation(pageNum = 0, bounds = bounds, color = Color.RED)

        // Scale by 2
        transform.setScale(2f, 2f)

        highlightAnnotationDrawer.draw(annotation, canvas, transform)

        assertThat(shadowCanvas.rectPaintHistoryCount).isEqualTo(1)
        // (10,10,20,20) scaled by 2 should be (20,20,40,40)
        assertThat(shadowCanvas.getDrawnRect(0).rect).isEqualTo(RectF(20f, 20f, 40f, 40f))
    }
}
