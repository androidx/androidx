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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.FloatRange
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke

/**
 * Renders a single [androidx.ink.brush.BrushCoat] of a [Stroke] or [InProgressStroke]. A different
 * implementation of this interface may be used for each coat.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
internal interface CanvasStrokeCoatRenderer {

    /**
     * Returns `true` iff the [stroke] coat at [coatIndex] can be drawn with the [BrushPaint] at the
     * given [paintPreferenceIndex].
     */
    fun canDraw(canvas: Canvas, stroke: Stroke, coatIndex: Int, paintPreferenceIndex: Int): Boolean

    fun draw(
        canvas: Canvas,
        stroke: Stroke,
        coatIndex: Int,
        paintPreferenceIndex: Int,
        strokeToScreenTransform: Matrix,
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false) textureAnimationProgress: Float,
    )

    /**
     * Returns `true` iff the [inProgressStroke] coat at [coatIndex] can be drawn with the
     * [BrushPaint] at the given [paintPreferenceIndex].
     */
    fun canDraw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        coatIndex: Int,
        paintPreferenceIndex: Int,
    ): Boolean

    fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        coatIndex: Int,
        paintPreferenceIndex: Int,
        strokeToScreenTransform: Matrix,
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false) textureAnimationProgress: Float,
    )
}

/**
 * Returns the texture mapping mode used by the [BrushPaint]. (This is actually specified separately
 * in each texture layer, but currently, we require all texture layers in the same paint to use the
 * same texture mapping mode.)
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
internal fun BrushPaint.getTextureMapping(): BrushPaint.TextureMapping =
    textureLayers.firstOrNull()?.mapping ?: BrushPaint.TextureMapping.TILING

@OptIn(ExperimentalInkCustomBrushApi::class)
internal fun Brush.getPaint(coatIndex: Int, paintPreferenceIndex: Int) =
    family.coats[coatIndex].paintPreferences[paintPreferenceIndex]
