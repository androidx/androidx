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
import androidx.annotation.FloatRange
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.brush.color.toArgb
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.MutableVec
import androidx.ink.geometry.PartitionedMesh
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
internal class CanvasPathRenderer(
    private val textureStore: TextureBitmapStore = TextureBitmapStore { null }
) : CanvasStrokeRenderer {

    /**
     * Holds onto rendering data for each [PartitionedMesh] (the shape of a [Stroke]) so the data
     * can be created once and then reused on each call to [draw]. The [WeakHashMap] ensures that
     * this renderer does not hold onto [PartitionedMesh] instances that would otherwise be garbage
     * collected.
     */
    private val strokePathCache = WeakHashMap<PartitionedMesh, List<Path>>()

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
    private val scratchMatrix = Matrix()

    // First and last inputs for the stroke being rendered, reused so that we don't need to allocate
    // new ones for every stroke.
    private val scratchFirstInput = StrokeInput()
    private val scratchLastInput = StrokeInput()

    private fun draw(
        canvas: Canvas,
        path: Path,
        brushPaint: BrushPaint,
        color: ComposeColor,
        @FloatRange(from = 0.0) brushSize: Float,
        firstInput: StrokeInput,
        lastInput: StrokeInput,
    ) {
        val paint = paintCache.obtain(brushPaint, color.toArgb(), brushSize, firstInput, lastInput)
        canvas.drawPath(path, paint)
    }

    override fun draw(
        canvas: Canvas,
        stroke: Stroke,
        @Suppress("UNUSED_PARAMETER") strokeToScreenTransform: AffineTransform,
    ) {
        draw(canvas, stroke, scratchMatrix)
    }

    override fun draw(
        canvas: Canvas,
        stroke: Stroke,
        @Suppress("UNUSED_PARAMETER") strokeToScreenTransform: Matrix,
    ) {
        if (stroke.inputs.isEmpty()) return // nothing to draw
        stroke.inputs.populate(0, scratchFirstInput)
        stroke.inputs.populate(stroke.inputs.size - 1, scratchLastInput)
        for (groupIndex in 0 until stroke.shape.getRenderGroupCount()) {
            draw(
                canvas,
                obtainPath(stroke.shape, groupIndex),
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
        @Suppress("UNUSED_PARAMETER") strokeToScreenTransform: AffineTransform,
    ) {
        draw(canvas, inProgressStroke, scratchMatrix)
    }

    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        @Suppress("UNUSED_PARAMETER") strokeToScreenTransform: Matrix,
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
                obtainPath(inProgressStroke, coatIndex),
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
    private fun obtainPath(shape: PartitionedMesh, groupIndex: Int): Path {
        val paths =
            strokePathCache[shape] ?: createPaths(shape).also { strokePathCache[shape] = it }
        return paths[groupIndex]
    }

    /** Create new [Path]s for the given [PartitionedMesh], one for each render group. */
    private fun createPaths(shape: PartitionedMesh): List<Path> =
        buildList() {
            val point = MutableVec()
            for (groupIndex in 0 until shape.getRenderGroupCount()) {
                val path = Path()
                for (outlineIndex in 0 until shape.getOutlineCount(groupIndex)) {
                    val outlineVertexCount = shape.getOutlineVertexCount(groupIndex, outlineIndex)
                    if (outlineVertexCount == 0) continue

                    shape.populateOutlinePosition(groupIndex, outlineIndex, 0, point)
                    path.moveTo(point.x, point.y)

                    for (outlineVertexIndex in 1 until outlineVertexCount) {
                        shape.populateOutlinePosition(
                            groupIndex,
                            outlineIndex,
                            outlineVertexIndex,
                            point
                        )
                        path.lineTo(point.x, point.y)
                    }

                    path.close()
                }
                add(path)
            }
        }

    /**
     * Obtain a [Path] for brush coat [coatIndex] of the given [InProgressStroke], which may be
     * cached or new.
     */
    private fun obtainPath(inProgressStroke: InProgressStroke, coatIndex: Int): Path {
        val cachedPathData = inProgressStrokePathCache[inProgressStroke]
        if (cachedPathData != null && cachedPathData.version == inProgressStroke.version) {
            return cachedPathData.paths[coatIndex]
        }
        val inProgressPathData = computeInProgressPathData(inProgressStroke)
        inProgressStrokePathCache[inProgressStroke] = inProgressPathData
        return inProgressPathData.paths[coatIndex]
    }

    private fun computeInProgressPathData(inProgressStroke: InProgressStroke): InProgressPathData {
        val paths =
            buildList() {
                for (coatIndex in 0 until inProgressStroke.getBrushCoatCount()) {
                    val path = Path()
                    path.fillFrom(inProgressStroke, coatIndex)
                    add(path)
                }
            }
        return InProgressPathData(inProgressStroke.version, paths)
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
    private class InProgressPathData(val version: Long, val paths: List<Path>)
}
