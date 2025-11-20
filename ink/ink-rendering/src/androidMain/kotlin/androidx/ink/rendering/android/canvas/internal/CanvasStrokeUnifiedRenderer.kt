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
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.populateMatrix
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke

/**
 * Renders Ink objects using [CanvasMeshRenderer], but falls back to using [CanvasPathRenderer] when
 * mesh rendering is not possible. This may happen if the mesh contents were modified (e.g. while in
 * a serialized form then deserialized) to a mesh format that the mesh renderer doesn't recognize.
 *
 * TODO: b/346530293 - Delete [forcePathRendering], use
 *   [androidx.ink.brush.BrushCoat.paintPreferences] and [androidx.ink.brush.BrushPaint.selfOverlap]
 *   instead.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
internal class CanvasStrokeUnifiedRenderer(
    private val textureStore: TextureBitmapStore = TextureBitmapStore { null },
    forcePathRendering: Boolean,
) : CanvasStrokeRenderer {

    private val scratchAffineTransformMatrix = Matrix()

    private val scratchMatrixValuesArray = FloatArray(9)

    /**
     * When the brush settings allow for it, we always prefer to draw with the mesh renderer than
     * the path renderer, as it is both more performant and more fully featured.
     */
    private val rendererPreferences: List<Lazy<CanvasStrokeCoatRenderer>> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !forcePathRendering) {
            add(lazy { CanvasMeshRenderer(textureStore) })
        }
        add(lazy { CanvasPathRenderer(textureStore) })
    }

    @ExperimentalInkCustomBrushApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override fun draw(
        canvas: Canvas,
        stroke: Stroke,
        strokeToScreenTransform: AffineTransform,
        textureAnimationProgress: Float,
    ) {
        strokeToScreenTransform.populateMatrix(scratchAffineTransformMatrix)
        draw(canvas, stroke, scratchAffineTransformMatrix, textureAnimationProgress)
    }

    @ExperimentalInkCustomBrushApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override fun draw(
        canvas: Canvas,
        stroke: Stroke,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    ) {
        assertIsAffine(strokeToScreenTransform)
        for (coatIndex in 0 until stroke.shape.getRenderGroupCount()) {
            drawCoat(canvas, stroke, coatIndex, strokeToScreenTransform, textureAnimationProgress)
        }
    }

    private fun drawCoat(
        canvas: Canvas,
        stroke: Stroke,
        coatIndex: Int,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    ) {
        // Try to render each paint option on each renderer until there's a match.
        for (paintPreferenceIndex in
            0 until stroke.brush.family.coats[coatIndex].paintPreferences.size) {
            for (lazyRenderer in rendererPreferences) {
                if (
                    lazyRenderer.value.canDraw(
                        canvas = canvas,
                        stroke = stroke,
                        coatIndex = coatIndex,
                        paintPreferenceIndex = paintPreferenceIndex,
                    )
                ) {
                    lazyRenderer.value.draw(
                        canvas = canvas,
                        stroke = stroke,
                        coatIndex = coatIndex,
                        paintPreferenceIndex = paintPreferenceIndex,
                        strokeToScreenTransform = strokeToScreenTransform,
                        textureAnimationProgress = textureAnimationProgress,
                    )
                    return
                }
            }
        }
        Log.i(
            "CanvasStrokeRenderer",
            "Coat $coatIndex of a Stroke with the following brush cannot be rendered: ${stroke.brush}",
        )
    }

    @ExperimentalInkCustomBrushApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: AffineTransform,
        textureAnimationProgress: Float,
    ) {
        strokeToScreenTransform.populateMatrix(scratchAffineTransformMatrix)
        draw(canvas, inProgressStroke, scratchAffineTransformMatrix, textureAnimationProgress)
    }

    @ExperimentalInkCustomBrushApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    ) {
        assertIsAffine(strokeToScreenTransform)
        val brush = checkNotNull(inProgressStroke.brush)
        for (coatIndex in 0 until inProgressStroke.getBrushCoatCount()) {
            drawCoat(
                canvas,
                inProgressStroke,
                brush,
                coatIndex,
                strokeToScreenTransform,
                textureAnimationProgress,
            )
        }
    }

    private fun drawCoat(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        brush: Brush,
        coatIndex: Int,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    ) {
        // Try to render each paint option on each renderer until it's successful.
        for (paintPreferenceIndex in 0 until brush.family.coats[coatIndex].paintPreferences.size) {
            for (lazyRenderer in rendererPreferences) {
                if (
                    lazyRenderer.value.canDraw(
                        canvas = canvas,
                        inProgressStroke = inProgressStroke,
                        coatIndex = coatIndex,
                        paintPreferenceIndex = paintPreferenceIndex,
                    )
                ) {
                    lazyRenderer.value.draw(
                        canvas = canvas,
                        inProgressStroke = inProgressStroke,
                        coatIndex = coatIndex,
                        paintPreferenceIndex = paintPreferenceIndex,
                        strokeToScreenTransform = strokeToScreenTransform,
                        textureAnimationProgress = textureAnimationProgress,
                    )
                    return
                }
            }
        }
        Log.i(
            "CanvasStrokeRenderer",
            "Coat $coatIndex of an InProgressStroke with the following brush cannot be rendered: $brush",
        )
    }

    /** Assert that the given [Matrix] is affine. */
    @VisibleForTesting
    internal fun assertIsAffine(transform: Matrix) {
        if (transform.isAffine) return
        // Don't throw yet - there seem to be cases where Matrix.isAffine returns false, but the
        // Matrix
        // is actually affine when examined directly. The above check is fast for the normal case.
        // See b/418261442 for more context.
        val vals = scratchMatrixValuesArray
        transform.getValues(vals)
        require(
            vals[Matrix.MPERSP_0] == 0F &&
                vals[Matrix.MPERSP_1] == 0F &&
                vals[Matrix.MPERSP_2] == 1F
        ) {
            "The matrix must be affine."
        }
    }
}
