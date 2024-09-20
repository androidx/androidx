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

package androidx.ink.rendering.android.canvas

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withTranslation
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.ImmutableAffineTransform
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.ImmutableVec
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.rendering.test.R
import androidx.ink.strokes.InProgressStroke

/** An [Activity] to support [CanvasStrokeRendererTest]. */
@OptIn(ExperimentalInkCustomBrushApi::class)
class CanvasStrokeRendererTestActivity : Activity() {
    @OptIn(ExperimentalInkCustomBrushApi::class)
    private val textureStore = TextureBitmapStore { uri ->
        when (uri) {
            TEXTURE_URI_AIRPLANE_EMOJI -> R.drawable.airplane_emoji
            TEXTURE_URI_CHECKERBOARD -> R.drawable.checkerboard_black_and_transparent
            TEXTURE_URI_CIRCLE -> R.drawable.circle
            TEXTURE_URI_POOP_EMOJI -> R.drawable.poop_emoji
            else -> null
        }?.let { BitmapFactory.decodeResource(resources, it) }
    }
    private val meshRenderer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @OptIn(ExperimentalInkCustomBrushApi::class) CanvasStrokeRenderer.create(textureStore)
        } else {
            null
        }
    private val pathRenderer =
        @OptIn(ExperimentalInkCustomBrushApi::class)
        CanvasStrokeRenderer.create(textureStore, forcePathRendering = true)
    private val defaultRenderer = CanvasStrokeRenderer.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.canvas_stroke_renderer_test)
    }

    fun addStrokeRows(labelsAndStrokes: List<Pair<String, InProgressStroke>>) {
        val grid = findViewById<GridLayout>(R.id.stroke_grid)
        for ((label, stroke) in labelsAndStrokes) {
            val row = grid.rowCount
            grid.rowCount = row + 1
            grid.addView(
                TextView(this).apply {
                    text = label
                    setTextSize(10.0F)
                },
                gridLayoutParams(row, col = 0, Gravity.CENTER_VERTICAL),
            )
            if (meshRenderer != null) {
                grid.addView(
                    StrokeView(this, meshRenderer, stroke),
                    gridLayoutParams(row, col = 1, Gravity.FILL),
                )
            } else {
                grid.addView(
                    TextView(this).apply { text = "N/A" },
                    gridLayoutParams(row, col = 1, Gravity.CENTER),
                )
            }
            grid.addView(
                StrokeView(this, pathRenderer, stroke),
                gridLayoutParams(row, col = 2, Gravity.FILL),
            )
            grid.addView(
                StrokeView(this, defaultRenderer, stroke),
                gridLayoutParams(row, col = 3, Gravity.FILL),
            )
        }
    }

    private fun gridLayoutParams(row: Int, col: Int, gravity: Int): GridLayout.LayoutParams {
        val params =
            GridLayout.LayoutParams(
                GridLayout.spec(row, /* weight= */ 1f),
                GridLayout.spec(col, /* weight= */ 0f),
            )
        if (gravity == Gravity.FILL) {
            params.width = 0
            params.height = 0
        }
        params.setGravity(gravity)
        return params
    }

    private class StrokeView(
        context: Context,
        val renderer: CanvasStrokeRenderer,
        val inProgressStroke: InProgressStroke,
    ) : View(context) {

        val bounds = BoxAccumulator().also { inProgressStroke.populateMeshBounds(0, it) }.box!!
        val finishedStrokeTranslateX = 20 + (bounds.xMax - bounds.xMin)
        val scaledStrokeTranslateY = 10 + (bounds.yMax - bounds.yMin)
        val scaleValueY = 0.5f
        val totalGridBounds =
            ImmutableBox.fromTwoPoints(
                ImmutableVec(bounds.xMin, bounds.yMin),
                ImmutableVec(
                    finishedStrokeTranslateX + (bounds.xMax - bounds.xMin),
                    scaledStrokeTranslateY + scaleValueY * (bounds.yMax - bounds.yMin),
                ),
            )

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.translate(
                (width - (totalGridBounds.xMax - totalGridBounds.xMin)) / 2,
                (height - (totalGridBounds.yMax - totalGridBounds.yMin)) / 2,
            )
            renderer.draw(canvas, inProgressStroke, AffineTransform.IDENTITY)

            // Draw Stroke next to InProgressStroke, with a small gap between them.
            val stroke = inProgressStroke.toImmutable()
            canvas.withTranslation(finishedStrokeTranslateX) {
                renderer.draw(
                    canvas,
                    stroke,
                    ImmutableAffineTransform.translate(ImmutableVec(finishedStrokeTranslateX, 0f)),
                )
            }

            // Draw the InProgressStroke and Stroke again in a second row with a non-trivial
            // transform
            // and using android.graphics.Matrix instead of AffineTransform.
            val transform =
                Matrix().apply {
                    setSkew(0.5f, 0f)
                    postScale(1f, scaleValueY)
                    postTranslate(0f, scaledStrokeTranslateY)
                }
            canvas.withMatrix(transform) { renderer.draw(canvas, inProgressStroke, transform) }

            transform.postTranslate(finishedStrokeTranslateX, 0f)
            canvas.withMatrix(transform) { renderer.draw(canvas, stroke, transform) }
        }
    }

    companion object {
        const val TEXTURE_URI_AIRPLANE_EMOJI = "ink://ink/texture:airplane-emoji"
        const val TEXTURE_URI_CHECKERBOARD = "ink://ink/texture:checkerboard-overlay-pen"
        const val TEXTURE_URI_CIRCLE = "ink://ink/texture:circle"
        const val TEXTURE_URI_POOP_EMOJI = "ink://ink/texture:poop-emoji"
    }
}
