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

import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.geometry.AffineTransform
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke

/**
 * Renders Ink objects using [CanvasMeshRenderer], but falls back to using [CanvasPathRenderer] when
 * mesh rendering is not possible.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
internal class CanvasStrokeUnifiedRenderer(
    private val textureStore: TextureBitmapStore = TextureBitmapStore { null }
) : CanvasStrokeRenderer {

    private val meshRenderer by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @OptIn(ExperimentalInkCustomBrushApi::class) CanvasMeshRenderer(textureStore)
        } else {
            null
        }
    }
    private val pathRenderer by lazy { CanvasPathRenderer(textureStore) }

    private fun getDelegateRendererOrThrow(stroke: Stroke): CanvasStrokeRenderer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val renderer = checkNotNull(meshRenderer)
            if (renderer.canDraw(stroke)) {
                return renderer
            }
        }
        for (groupIndex in 0 until stroke.shape.getRenderGroupCount()) {
            if (stroke.shape.getOutlineCount(groupIndex) > 0) {
                return pathRenderer
            }
        }
        throw IllegalArgumentException("Cannot draw $stroke")
    }

    override fun draw(canvas: Canvas, stroke: Stroke, strokeToScreenTransform: AffineTransform) {
        getDelegateRendererOrThrow(stroke).draw(canvas, stroke, strokeToScreenTransform)
    }

    override fun draw(canvas: Canvas, stroke: Stroke, strokeToScreenTransform: Matrix) {
        getDelegateRendererOrThrow(stroke).draw(canvas, stroke, strokeToScreenTransform)
    }

    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: AffineTransform,
    ) {
        val delegateRenderer = meshRenderer ?: pathRenderer
        delegateRenderer.draw(canvas, inProgressStroke, strokeToScreenTransform)
    }

    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: Matrix,
    ) {
        val delegateRenderer = meshRenderer ?: pathRenderer
        delegateRenderer.draw(canvas, inProgressStroke, strokeToScreenTransform)
    }
}
