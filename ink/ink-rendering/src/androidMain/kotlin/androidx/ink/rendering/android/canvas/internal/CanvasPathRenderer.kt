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
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import androidx.annotation.FloatRange
import androidx.core.graphics.withMatrix
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.MutableVec
import androidx.ink.geometry.PartitionedMesh
import androidx.ink.geometry.outlinesToPath
import androidx.ink.geometry.populateMatrix
import androidx.ink.geometry.populatePathFromOutlines
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import java.util.WeakHashMap

/**
 * Renders Ink objects using [Canvas.drawPath]. This is the best [Canvas] Ink renderer to use before
 * [android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE] for both quality (anti-aliasing) and
 * performance compared to a solution built on [Canvas.drawVertices], and even on higher OS versions
 * when the desired behavior for self-intersection of translucent strokes is to discard the extra
 * layers.
 *
 * This is not thread safe, so if it must be used from multiple threads, the caller is responsible
 * for synchronizing access. If it is being used in two very different contexts where there are
 * unlikely to be cached mesh data in common, the easiest solution to thread safety is to have two
 * different instances of this object.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
internal class CanvasPathRenderer(textureStore: TextureBitmapStore = TextureBitmapStore { null }) :
    CanvasStrokeRenderer {

    /**
     * Holds onto rendering data for each [PartitionedMesh] (the shape of a [Stroke]) so the data
     * can be created once and then reused on each call to [draw]. The [WeakHashMap] ensures that
     * this renderer does not hold onto [PartitionedMesh] instances that would otherwise be garbage
     * collected.
     *
     * Before API 28, drawing a [Path] with a transform on the [Canvas] often leads to blurry
     * results, because the [Canvas] transform was not taken into account when rasterizing the
     * [Path]. Due to that issue, the [Path] constructed from the [PartitionedMesh] is transformed
     * out of the default stroke coordinates to be in screen coordinates. The transform used to do
     * this is kept as part of the cache data, so if the transform is updated (e.g. during
     * panning/zooming/rotating), then the [Path] data must be regenerated.
     *
     * Starting with API 28, [Path] rendering takes the [Canvas] transform into account properly, so
     * this workaround isn't necessary, and the [Path] data is kept in stroke coordinates and does
     * not need to be regenerated based on the transform.
     */
    private val strokePathCache = WeakHashMap<PartitionedMesh, PartitionedMeshPathData>()

    /**
     * Holds onto rendering data for each [InProgressStroke], so the data can be created once and
     * then reused on each call to [draw]. Because [InProgressStroke] is mutable, this cache is
     * based not just on the existence of data, but whether that data's version number matches that
     * of the [InProgressStroke]. The [WeakHashMap] ensures that this renderer does not hold onto
     * [InProgressStroke] instances that would otherwise be garbage collected.
     */
    private val inProgressStrokePathCache = WeakHashMap<InProgressStroke, InProgressPathData>()

    private val paintCache =
        BrushPaintCache(
            textureStore,
            additionalPaintFlags = Paint.ANTI_ALIAS_FLAG,
            applyColorFilterToTexture = true,
        )

    private val scratchPoint = MutableVec()

    /** Scratch [Matrix] used for draw calls taking an [AffineTransform]. */
    private val scratchAffineTransformMatrix = Matrix()

    /** Scratch [Matrix] used to invert the `strokeToScreenTransform` input value to [draw]. */
    private val scratchScreenToStrokeTransform = Matrix()

    // First and last inputs for the stroke being rendered, reused so that we don't need to allocate
    // new ones for every stroke.
    private val scratchFirstInput = StrokeInput()
    private val scratchLastInput = StrokeInput()

    private fun draw(
        canvas: Canvas,
        path: Path,
        strokeToScreenTransform: Matrix,
        brushPaint: BrushPaint,
        color: ComposeColor,
        @FloatRange(from = 0.0) brushSize: Float,
        firstInput: StrokeInput,
        lastInput: StrokeInput,
    ) {
        // TODO: b/373649230 - Use [textureAnimationProgress] in renderer.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val paint = paintCache.obtain(brushPaint, color, brushSize, firstInput, lastInput)
            // On API 28 and above, both the Path and the Canvas are in stroke coordinate space.
            canvas.drawPath(path, paint)
        } else {
            // Below API 28, the Path is in screen coordinates to avoid rendering issues. Make sure
            // that
            // the Paint and the Canvas both take this into account, as by default they expect the
            // Path
            // to be in stroke coordinates.
            val paint =
                paintCache.obtain(
                    brushPaint,
                    color,
                    brushSize,
                    firstInput,
                    lastInput,
                    strokeToScreenTransform,
                )
            strokeToScreenTransform.invert(scratchScreenToStrokeTransform)
            canvas.withMatrix(scratchScreenToStrokeTransform) { canvas.drawPath(path, paint) }
        }
    }

    override fun draw(
        canvas: Canvas,
        stroke: Stroke,
        strokeToScreenTransform: AffineTransform,
        textureAnimationProgress: Float,
    ) {
        strokeToScreenTransform.populateMatrix(scratchAffineTransformMatrix)
        draw(canvas, stroke, scratchAffineTransformMatrix, textureAnimationProgress)
    }

    override fun draw(
        canvas: Canvas,
        stroke: Stroke,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    ) {
        if (stroke.inputs.isEmpty()) return // nothing to draw
        stroke.inputs.populate(0, scratchFirstInput)
        stroke.inputs.populate(stroke.inputs.size - 1, scratchLastInput)
        for (groupIndex in 0 until stroke.shape.getRenderGroupCount()) {
            draw(
                canvas,
                obtainPath(stroke.shape, groupIndex, strokeToScreenTransform),
                strokeToScreenTransform,
                stroke.brush.family.coats[groupIndex].paint,
                stroke.brush.composeColor,
                stroke.brush.size,
                scratchFirstInput,
                scratchLastInput,
            )
        }
    }

    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: AffineTransform,
        textureAnimationProgress: Float,
    ) {
        strokeToScreenTransform.populateMatrix(scratchAffineTransformMatrix)
        draw(canvas, inProgressStroke, scratchAffineTransformMatrix, textureAnimationProgress)
    }

    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    ) {
        val brush =
            checkNotNull(inProgressStroke.brush) {
                "Attempting to draw an InProgressStroke that has not been started."
            }
        val inputCount = inProgressStroke.getInputCount()
        if (inputCount == 0) return // nothing to draw
        inProgressStroke.populateInput(scratchFirstInput, 0)
        inProgressStroke.populateInput(scratchLastInput, inputCount - 1)
        for (coatIndex in 0 until inProgressStroke.getBrushCoatCount()) {
            draw(
                canvas,
                obtainPath(inProgressStroke, coatIndex, strokeToScreenTransform),
                strokeToScreenTransform,
                brush.family.coats[coatIndex].paint,
                brush.composeColor,
                brush.size,
                scratchFirstInput,
                scratchLastInput,
            )
        }
    }

    /**
     * Obtain a [Path] for the specified render group of the given [PartitionedMesh], which may be
     * cached or new.
     */
    private fun obtainPath(
        shape: PartitionedMesh,
        groupIndex: Int,
        strokeToScreenTransform: Matrix,
    ): Path {
        val cachedPathData = strokePathCache[shape]
        val pathData =
            if (cachedPathData == null) {
                PartitionedMeshPathData.create(shape, strokeToScreenTransform).also {
                    strokePathCache[shape] = it
                }
            } else {
                cachedPathData.maybeUpdate(shape, strokeToScreenTransform)
                cachedPathData
            }
        return pathData.paths[groupIndex]
    }

    /**
     * Obtain a [Path] for brush coat [coatIndex] of the given [InProgressStroke], which may be
     * cached or new.
     *
     * The resulting [Path] will be in screen coordinates.
     */
    private fun obtainPath(
        inProgressStroke: InProgressStroke,
        coatIndex: Int,
        strokeToScreenTransform: Matrix,
    ): Path {
        val cachedPathData = inProgressStrokePathCache[inProgressStroke]
        if (
            cachedPathData != null &&
                cachedPathData.version == inProgressStroke.version &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ||
                    cachedPathData.strokeToScreenTransform == strokeToScreenTransform)
        ) {
            return cachedPathData.paths[coatIndex]
        }
        val inProgressPathData =
            computeInProgressPathData(inProgressStroke, strokeToScreenTransform)
        inProgressStrokePathCache[inProgressStroke] = inProgressPathData
        return inProgressPathData.paths[coatIndex]
    }

    private fun computeInProgressPathData(
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: Matrix,
    ): InProgressPathData {
        val paths = buildList {
            for (coatIndex in 0 until inProgressStroke.getBrushCoatCount()) {
                val path = Path()
                path.fillFrom(inProgressStroke, coatIndex)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    path.transform(strokeToScreenTransform)
                }
                add(path)
            }
        }
        return InProgressPathData(inProgressStroke.version, strokeToScreenTransform, paths)
    }

    /** Create a new [Path] for the given [InProgressStroke]. */
    private fun Path.fillFrom(inProgressStroke: InProgressStroke, coatIndex: Int) {
        rewind()
        for (outlineIndex in 0 until inProgressStroke.getOutlineCount(coatIndex)) {
            val outlineVertexCount = inProgressStroke.getOutlineVertexCount(coatIndex, outlineIndex)
            if (outlineVertexCount == 0) continue

            inProgressStroke.populateOutlinePosition(
                coatIndex,
                outlineIndex,
                outlineVertexIndex = 0,
                scratchPoint,
            )
            moveTo(scratchPoint.x, scratchPoint.y)

            for (outlineVertexIndex in 1 until outlineVertexCount) {
                inProgressStroke.populateOutlinePosition(
                    coatIndex,
                    outlineIndex,
                    outlineVertexIndex,
                    scratchPoint,
                )
                lineTo(scratchPoint.x, scratchPoint.y)
            }

            close()
        }
    }

    /**
     * A snapshot of the outline(s) of [InProgressStroke] at a particular
     * [InProgressStroke.version], with one [Path] object for each brush coat.
     */
    private class InProgressPathData(
        val version: Long,
        val strokeToScreenTransform: Matrix,
        val paths: List<Path>,
    )

    /**
     * On Android API<28, [paths] has been transformed into screen coordinates by
     * [strokeToScreenTransform], and must be repopulated and retransformed if this transform
     * changes.
     *
     * On Android API 28+, [paths] are all in stroke coordinates, and [strokeToScreenTransform] is
     * not used for cache invalidation.
     */
    private class PartitionedMeshPathData
    private constructor(
        /** Do not modify directly! */
        val strokeToScreenTransform: Matrix,
        /** Do not modify directly! */
        val paths: List<Path>,
        /**
         * For defensive coding - make sure updates are from the same shape, without holding a
         * reference to the shape itself. Not used for any real functionality.
         */
        private val shapeNativeAddress: Long,
    ) {
        companion object {
            fun create(
                shape: PartitionedMesh,
                strokeToScreenTransform: Matrix
            ): PartitionedMeshPathData {
                val paths = buildList {
                    for (groupIndex in 0 until shape.getRenderGroupCount()) {
                        val path = shape.outlinesToPath(groupIndex) // stroke coordinates
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                            path.transform(strokeToScreenTransform)
                        }
                        add(path)
                    }
                }
                return PartitionedMeshPathData(
                    Matrix(strokeToScreenTransform),
                    paths,
                    shape.getNativeAddress(),
                )
            }
        }

        /** Update [paths] only if API < 28 and transforms are different. */
        fun maybeUpdate(shape: PartitionedMesh, strokeToScreenTransform: Matrix) {
            check(shape.getNativeAddress() == shapeNativeAddress) {
                "Must update PartitionedMeshData using the same PartitionedMesh used to create it."
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ||
                    strokeToScreenTransform == this.strokeToScreenTransform
            ) {
                return
            }
            for (groupIndex in 0 until shape.getRenderGroupCount()) {
                val path = paths[groupIndex]
                shape.populatePathFromOutlines(groupIndex, path)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    path.transform(strokeToScreenTransform)
                }
            }
            this.strokeToScreenTransform.set(strokeToScreenTransform)
        }
    }
}
