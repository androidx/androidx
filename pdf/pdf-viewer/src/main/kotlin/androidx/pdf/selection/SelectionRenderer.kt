/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.selection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.PointF
import android.graphics.PorterDuff.Mode
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfRect
import androidx.pdf.R
import kotlin.math.roundToInt

/**
 * Draws content-selection related UI to a [Canvas]. UI state required for drawing, including the
 * [Canvas], and selection state are provided externally; this class is stateless.
 */
internal class SelectionRenderer(
    private val context: Context,
    private val rightHandleFactory: () -> Drawable = {
        requireNotNull(context.getDrawable(R.drawable.selection_drag_handle_right)).also {
            it.colorFilter =
                PorterDuffColorFilter(
                    context.getColor(R.color.pdf_viewer_selection_handles),
                    Mode.SRC_ATOP,
                )
        }
    },
    private val leftHandleFactory: () -> Drawable = {
        requireNotNull(context.getDrawable(R.drawable.selection_drag_handle_left)).also {
            it.colorFilter =
                PorterDuffColorFilter(
                    context.getColor(R.color.pdf_viewer_selection_handles),
                    Mode.SRC_ATOP,
                )
        }
    },
) {
    private val rightHandle: Drawable by lazy { rightHandleFactory() }

    private val leftHandle: Drawable by lazy { leftHandleFactory() }

    fun drawSelectionOnPage(
        model: SelectionModel,
        pageNum: Int,
        canvas: Canvas,
        locationInView: RectF,
        currentZoom: Float,
    ) {
        // Draw the bounds first so the handles appear on top of them
        model.documentSelection.selectedContents[pageNum]?.forEach {
            it.bounds.forEach { drawBoundsOnPage(canvas, it, locationInView) }
        }

        // TODO(b/441196273): Drag handles for modifying a selection are currently only supported
        // for selections that consist exclusively of text. For mixed-content or non-text
        // selections, the handles are disabled as this functionality is not yet supported.
        if (model.documentSelection.containsOnlyTextSelections) {
            model.startBoundary.let {
                val startLoc = it.location
                if (startLoc.pageNum == pageNum) {
                    val pointInView =
                        PointF(locationInView.left + startLoc.x, locationInView.top + startLoc.y)
                    drawHandleAtPosition(canvas, pointInView, isRight = it.isRtl, currentZoom)
                }
            }

            model.endBoundary.let {
                val endLoc = it.location
                if (endLoc.pageNum == pageNum) {
                    val pointInView =
                        PointF(locationInView.left + endLoc.x, locationInView.top + endLoc.y)
                    drawHandleAtPosition(canvas, pointInView, isRight = !it.isRtl, currentZoom)
                }
            }
        }
    }

    private fun drawHandleAtPosition(
        canvas: Canvas,
        pointInView: PointF,
        isRight: Boolean,
        currentZoom: Float,
    ) {
        // The sharp point of the handle is found at a particular point in the image - (25%, 10%)
        // for the right handle, and (75%, 10%) for a left handle. We apply these as negative
        // margins so that the handle's point is at the point specified
        val relativePointLocX = if (isRight) -0.25f else -0.75f
        val relativePointLocY = -0.10f
        val drawable = if (isRight) rightHandle else leftHandle
        // Don't draw ridiculously large selection handles at low zoom levels
        val scale = minOf(1 / currentZoom, 0.5f)
        val left = pointInView.x + relativePointLocX * drawable.intrinsicWidth * scale
        val top = pointInView.y + relativePointLocY * drawable.intrinsicHeight * scale
        drawable.setBounds(
            left.roundToInt(),
            top.roundToInt(),
            (left + drawable.intrinsicWidth * scale).roundToInt(),
            (top + drawable.intrinsicHeight * scale).roundToInt(),
        )
        drawable.draw(canvas)
    }

    private fun drawBoundsOnPage(canvas: Canvas, bounds: PdfRect, pageLocationInView: RectF) {
        val boundsRect = RectF(bounds.left, bounds.top, bounds.right, bounds.bottom)
        boundsRect.offset(pageLocationInView.left, pageLocationInView.top)
        canvas.drawRect(boundsRect, BOUNDS_PAINT)
    }
}

/**
 * Note the use of [Mode.MULTIPLY], which means this paint will darken the light areas of the
 * destination, but leave dark areas unchanged, like a fluorescent highlighter
 */
@VisibleForTesting
internal val BOUNDS_PAINT =
    Paint().apply {
        style = Style.FILL
        xfermode = PorterDuffXfermode(Mode.MULTIPLY)
        setARGB(255, 160, 215, 255)
        isAntiAlias = true
        isDither = true
    }
