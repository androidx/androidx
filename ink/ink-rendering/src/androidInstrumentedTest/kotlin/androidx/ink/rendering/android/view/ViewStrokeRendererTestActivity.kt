/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android.view

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.rendering.android.canvas.TestColors
import androidx.ink.rendering.test.R
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke

/** An [Activity] to support [ViewStrokeRendererTest]. */
class ViewStrokeRendererTestActivity : Activity() {
    private val strokeRenderer = CanvasStrokeRenderer.create()

    private val viewToScreenScaleX = 0.5F
    private val viewToScreenRotation = -45F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_stroke_renderer_test)

        // Use a non-trivial view -> screen transform to check that it is correctly applied.
        val layout = findViewById<RelativeLayout>(R.id.stroke_view_parent)
        layout.scaleX = viewToScreenScaleX
        layout.rotation = viewToScreenRotation
        layout.addView(StrokeView(this, strokeRenderer))
    }

    /** A [View] that draws multiple transformed strokes using [CanvasStrokeRenderer]. */
    private inner class StrokeView(context: Context, val strokeRenderer: CanvasStrokeRenderer) :
        View(context) {

        private val viewStrokeRenderer = ViewStrokeRenderer(strokeRenderer, this)

        private val inputsTwist =
            MutableStrokeInputBatch()
                .addOrThrow(InputToolType.UNKNOWN, x = 30F, y = 0F, elapsedTimeMillis = 100)
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 40F, elapsedTimeMillis = 150)
                .addOrThrow(InputToolType.UNKNOWN, x = 40F, y = 70F, elapsedTimeMillis = 200)
                .addOrThrow(InputToolType.UNKNOWN, x = 5F, y = 90F, elapsedTimeMillis = 250)
                .asImmutable()

        private val stroke =
            Stroke(
                Brush.createWithColorIntArgb(
                    family = StockBrushes.markerLatest,
                    colorIntArgb = TestColors.BLACK,
                    size = 10f,
                    epsilon = 0.1f,
                ),
                inputsTwist,
            )

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(TestColors.YELLOW)

            viewStrokeRenderer.drawWithStrokes(canvas) { scope ->
                canvas.translate(300F, 300F)
                canvas.scale(9F, 3F)

                canvas.save()
                canvas.rotate(-45F)
                scope.drawStroke(stroke)
                canvas.restore()

                canvas.translate(0F, 50F)
                scope.drawStroke(stroke)
            }
        }
    }
}
