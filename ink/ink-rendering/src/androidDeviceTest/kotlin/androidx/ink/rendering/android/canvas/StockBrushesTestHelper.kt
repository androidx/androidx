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

package androidx.ink.rendering.android.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.withMatrix
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockTextureBitmapStore
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.rendering.test.R
import androidx.ink.storage.decode
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch

/** Helper class for drawing a set of strokes for testing [StockBrushes]. */
@OptIn(ExperimentalInkCustomBrushApi::class)
class StockBrushesTestHelper(private val context: Context) {
    private val resources = context.resources
    val cursiveStylusInputs = getStrokeInputBatchById(R.raw.cursive_stylus_inputbatch)
    val cursiveTouchInputs = getStrokeInputBatchById(R.raw.cursive_touch_inputbatch)
    val octogonStylusInputs = getStrokeInputBatchById(R.raw.octogon_stylus_inputbatch)
    val octogonTouchInputs = getStrokeInputBatchById(R.raw.octogon_touch_inputbatch)
    val helloWorldDocument =
        List(39) { index ->
                val resourceId =
                    resources.getIdentifier("testdoc_stroke_${index}", "raw", context.packageName)
                getStrokeInputBatchById(resourceId)
            }
            .toList()
    val synthesizedAsteriskDrawing =
        listOf(
            buildLine(0f, 10f, 0f, 75f),
            buildLine(10f, 10f, 75f, 75f),
            buildLine(10f, 0f, 75f, 0f),
            buildLine(10f, -10f, 75f, -75f),
            buildLine(0f, -10f, 0f, -75f),
            buildLine(-10f, -10f, -75f, -75f),
            buildLine(-10f, 0f, -75f, 0f),
            buildLine(-10f, 10f, -75f, 75f),
        )

    private var width = 480
    private var height = 800
    private var cellWidth = 0f
    private var cellHeight = 0f
    private var cellOffsetLeft = 0f
    private var cellOffsetTop = 0f
    private var scale = 1f
    private var strokes = emptyList<List<List<Stroke>>>()
    private var bounds = BoxAccumulator()
    private val textureStore =
        StockTextureBitmapStore(resources).apply {
            check(
                addTexture(
                    "emoji_heart",
                    checkNotNull(BitmapFactory.decodeResource(resources, R.drawable.emoji_heart)),
                ) == null
            )
        }
    private val renderer = CanvasStrokeRenderer.create(textureStore)

    /**
     * Sets the strokes to be drawn such that strokesGrid[row][col] contains a list of strokes to be
     * drawn within that cell of that row and column of a 2D grid.
     */
    private fun setStrokes(strokesGrid: List<List<List<Stroke>>>) {
        strokes = strokesGrid
        if (strokesGrid.isEmpty() || strokesGrid[0].isEmpty() || strokesGrid[0][0].isEmpty()) return
        // Assuming all strokes have the same (x,y) input path, but may have different brushes,
        // determine the MBR of all strokes.
        for (row in strokesGrid) {
            for (cell in row) {
                for (stroke in cell) {
                    bounds.add(stroke.shape.computeBoundingBox())
                }
            }
        }
        val padding = 5f
        // Assuming all cells have the same sets of input path, but may have different brushes,
        // determine the MBR of all strokes.
        bounds.box?.let {
            // Each "cell" in the grid of strokes will be as wide as the meshes + some padding.
            cellWidth = it.xMax - it.xMin + padding
            cellHeight = it.yMax - it.yMin + padding
            // Within each cell, translate the strokes' meshes such that the topleft most parts of
            // the
            // mbr align with the topleft part of the cell (save some padding).
            cellOffsetLeft = it.xMin - padding
            cellOffsetTop = it.yMin - padding
        }
        // Scale every stroke to fit within the grid by using the view's dimensions and the number
        // of strokes to be drawn in each row and column.
        val scaleX = width.toFloat() / (strokesGrid[0].size.toFloat() * cellWidth)
        val scaleY = height.toFloat() / (strokesGrid.size.toFloat() * cellHeight)
        scale = Math.min(scaleX, scaleY)
    }

    private fun buildLine(
        xStart: Float,
        yStart: Float,
        xEnd: Float,
        yEnd: Float,
    ): ImmutableStrokeInputBatch =
        MutableStrokeInputBatch()
            .apply {
                val xStep = (xEnd - xStart) / 10f
                val yStep = (yEnd - yStart) / 10f
                for (i in 0 until 10) {
                    add(InputToolType.STYLUS, xStart + xStep * i, yStart + yStep * i, i * 300L)
                }
            }
            .toImmutable()

    private fun getStrokeInputBatchById(resourceId: Int): ImmutableStrokeInputBatch {
        return context.resources.openRawResource(resourceId).use { StrokeInputBatch.decode(it) }
    }

    fun drawToBitmap(strokeGrid: List<List<List<Stroke>>>): Bitmap {
        setStrokes(strokeGrid)
        return ImageDiffer.createBitmap(width, height) { canvas ->
            // Draw strokes in scaled 2D grid.
            strokes.forEachIndexed { i, row ->
                row.forEachIndexed { j, cell ->
                    for (stroke in cell) {
                        val translateX = j * cellWidth - cellOffsetLeft
                        val translateY = i * cellHeight - cellOffsetTop
                        val transform = createTranslation(translateX, translateY)
                        canvas.withMatrix(transform) { renderer.draw(canvas, stroke, transform) }
                    }
                }
            }
        }
    }

    private fun createTranslation(translateX: Float, translateY: Float): Matrix {
        return Matrix().apply {
            setScale(scale, scale)
            postTranslate(translateX * scale, translateY * scale)
        }
    }
}
