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

package androidx.ink.rendering.android.canvas.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.withMatrix
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke

/**
 * An [Activity] to support [CanvasStrokeUnifiedRendererLegacyTest] by rendering a simple stroke.
 */
@SuppressLint("UseSdkSuppress") // SdkSuppress is on the test class.
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CanvasMeshRendererScreenshotTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(StrokeView(this))
    }

    private inner class StrokeView(context: Context) : View(context) {

        // TODO: b/369408056 - Change back to twist-style input points
        private val inputs =
            MutableStrokeInputBatch()
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 0F, elapsedTimeMillis = 100)
                .addOrThrow(InputToolType.UNKNOWN, x = 40F, y = 40F, elapsedTimeMillis = 150)
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 70F, elapsedTimeMillis = 200)
                .addOrThrow(InputToolType.UNKNOWN, x = 30F, y = 100F, elapsedTimeMillis = 250)
                .asImmutable()

        // Pink twist stroke.
        private val brush =
            Brush.createWithColorIntArgb(
                family = StockBrushes.markerLatest,
                colorIntArgb = 0x80CC1A99.toInt(),
                size = 10F,
                epsilon = 0.1F,
            )
        private val stroke = Stroke(brush, inputs)
        private val transform = Matrix.IDENTITY_MATRIX

        // Green twist stroke, rotated and scaled up.
        private val brush2 = brush.copyWithColorIntArgb(colorIntArgb = 0xCC33E666.toInt())
        private val stroke2 = stroke.copy(brush2)
        private val transform2 =
            Matrix().apply {
                postRotate(/* degrees= */ 30F)
                postScale(7F, 7F)
            }

        // Stroke with no inputs, and therefore an empty [ModeledShape].
        private val emptyStroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)

        private val renderer = @OptIn(ExperimentalInkCustomBrushApi::class) CanvasMeshRenderer()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val xBetweenStrokes = 115F
            val y = 100F
            canvas.translate(20F, y)

            // The empty stroke should of course not be visible, but the [draw] call should succeed.
            renderer.draw(canvas, emptyStroke, Matrix.IDENTITY_MATRIX)

            // Expected result: pink stroke on left, large green rotated stroke on right.
            canvas.withMatrix(transform) { renderer.draw(canvas, stroke, transform) }

            canvas.translate(xBetweenStrokes, 0F)
            canvas.withMatrix(transform2) { renderer.draw(canvas, stroke2, transform2) }
        }
    }
}
