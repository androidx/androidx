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

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.geometry.AffineTransform
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.rendering.android.canvas.internal.CanvasPathRenderer
import androidx.ink.rendering.android.canvas.internal.CanvasStrokeUnifiedRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke

/** Renders strokes to a [Canvas]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public interface CanvasStrokeRenderer {

    /**
     * Render a single [stroke] on the provided [canvas], with its positions transformed by
     * [strokeToCanvasTransform].
     *
     * To avoid needing to calculate and maintain [strokeToCanvasTransform], consider using
     * [ViewStrokeRenderer] instead.
     *
     * TODO: b/353561141 - Reference ComposeStrokeRenderer above once implemented.
     *
     * The [strokeToCanvasTransform] should represent the complete transformation from stroke
     * coordinates to the canvas, modulo translation. Any existing transforms applied to [canvas]
     * should be undone prior to calling [draw].
     */
    public fun draw(canvas: Canvas, stroke: Stroke, strokeToCanvasTransform: AffineTransform)

    /**
     * Render a single [stroke] on the provided [canvas], with its positions transformed by
     * [strokeToCanvasTransform].
     *
     * To avoid needing to calculate and maintain [strokeToCanvasTransform], consider using
     * [ViewStrokeRenderer].
     *
     * TODO: b/353561141 - Reference ComposeStrokeRenderer above once implemented.
     *
     * The [strokeToCanvasTransform] must be affine. It should represent the complete transformation
     * from stroke coordinates to the canvas, modulo translation. Any existing transforms applied to
     * [canvas] should be undone prior to calling [draw].
     */
    public fun draw(canvas: Canvas, stroke: Stroke, strokeToCanvasTransform: Matrix)

    /**
     * Render a single [inProgressStroke] on the provided [canvas], with its positions transformed
     * by [strokeToCanvasTransform].
     *
     * The [strokeToCanvasTransform] should represent the complete transformation from stroke
     * coordinates to the canvas, modulo translation. Any existing transforms applied to [canvas]
     * should be undone prior to calling [draw].
     */
    public fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToCanvasTransform: AffineTransform,
    )

    /**
     * Render a single [inProgressStroke] on the provided [canvas], with its positions transformed
     * by [strokeToCanvasTransform].
     *
     * The [strokeToCanvasTransform] must be affine. It should represent the complete transformation
     * from stroke coordinates to the canvas, modulo translation. Any existing transforms applied to
     * [canvas] should be undone prior to calling [draw].
     */
    public fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToCanvasTransform: Matrix,
    )

    /**
     * The distance beyond a stroke geometry's bounds that rendering might affect. This is currently
     * only applicable to in-progress stroke rendering, where the smallest possible region of the
     * screen is redrawn to optimize performance. But with a custom [CanvasStrokeRenderer], certain
     * effects like drop shadows or blurs may render beyond the stroke's geometry, and setting a
     * higher value here can ensure that artifacts are not left on screen after an in-progress
     * stroke has moved on from a particular region of the screen. This value should be set to the
     * lowest value that avoids the artifacts.
     *
     * Custom [CanvasStrokeRenderer] implementations are generally less efficient than achieving the
     * same effect with a custom [BrushTip], as well as being less compatible with intersection and
     * hit testing, and more features over time.
     *
     * Custom renderers are possible to maximize control over the final effect on screen, but
     * consider filing a feature request to support your use case with [BrushTip] directly. The more
     * your rendering relies on a bigger value here, the more likely it will run into complications
     * later on as your client integration gets more complex or as we add more features.
     */
    @Px public fun strokeModifiedRegionOutsetPx(): Int = 3

    public companion object {

        init {
            NativeLoader.load()
        }

        /** Create a [CanvasStrokeRenderer] that is appropriate to the device's API version. */
        public fun create(): CanvasStrokeRenderer {
            @OptIn(ExperimentalInkCustomBrushApi::class)
            return create(textureStore = TextureBitmapStore { null })
        }

        /**
         * Create a [CanvasStrokeRenderer] that is appropriate to the device's API version.
         *
         * @param textureStore The [TextureBitmapStore] that will be called to retrieve image data
         *   for drawing textured strokes.
         * @param forcePathRendering Overrides the drawing strategy selected based on API version to
         *   always draw strokes using [Canvas.drawPath] instead of [Canvas.drawMesh].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @ExperimentalInkCustomBrushApi
        @JvmOverloads
        public fun create(
            textureStore: TextureBitmapStore,
            forcePathRendering: Boolean = false,
        ): CanvasStrokeRenderer {
            if (!forcePathRendering) return CanvasStrokeUnifiedRenderer(textureStore)
            return CanvasPathRenderer(textureStore)
        }
    }
}
