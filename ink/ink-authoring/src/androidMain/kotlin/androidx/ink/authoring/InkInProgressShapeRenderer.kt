/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.OpenForTesting
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureAnimationProgressHelper
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer

/**
 * An implementation of [InProgressShapeRenderer] that just wraps
 * [androidx.ink.rendering.android.canvas.CanvasStrokeRenderer].
 */
@OpenForTesting
@ExperimentalCustomShapeWorkflowApi
internal open class InkInProgressShapeRenderer(
    private val canvasStrokeRenderer: CanvasStrokeRenderer
) : InProgressShapeRenderer<InkInProgressShape> {

    override fun draw(canvas: Canvas, shape: InkInProgressShape, strokeToScreenTransform: Matrix) {
        check(!shape.isCanceled()) { "Internal error: Tried to draw canceled stroke shape" }
        val textureAnimationDurationMillis = shape.textureAnimationDurationMillis
        @OptIn(ExperimentalInkCustomBrushApi::class)
        canvasStrokeRenderer.draw(
            canvas = canvas,
            inProgressStroke = shape.inProgressStroke,
            strokeToScreenTransform = strokeToScreenTransform,
            textureAnimationProgress =
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    shape.lastUpdateSystemElapsedTimeMillis,
                    textureAnimationDurationMillis,
                ),
        )
    }
}
