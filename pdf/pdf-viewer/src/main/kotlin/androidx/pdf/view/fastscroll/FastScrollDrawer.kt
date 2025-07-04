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

package androidx.pdf.view.fastscroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.Range
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.util.buildPageIndicatorLabel
import com.google.android.material.color.MaterialColors

/**
 * Draws the visual elements of the fast scroller.
 *
 * This class is responsible for rendering the fast scroller UI, including the thumb, drag handle
 * (track), and page indicator. It uses drawables and text to provide a visual representation of the
 * current scroll position and allow for quick navigation within a PDF document.
 *
 * @param context The UI context used for loading resources and respecting configuration changes
 * @param pdfDocument The PDF document being displayed.
 * @param thumbDrawable The drawable used for the fast scroller thumb.
 * @param pageIndicatorBackground The drawable used for the background of the page indicator.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FastScrollDrawer(
    internal val context: Context,
    private val pdfDocument: PdfDocument,
    @get:VisibleForTesting public var thumbDrawable: Drawable?,
    @get:VisibleForTesting public var pageIndicatorBackground: Drawable?,
    @get:VisibleForTesting public var thumbMarginEnd: Int,
    @get:VisibleForTesting public var pageIndicatorMarginEnd: Int,
) {
    internal val thumbWidthPx = thumbDrawable?.intrinsicWidth ?: 0
    internal val thumbHeightPx = thumbDrawable?.intrinsicHeight ?: 0
    private val pageIndicatorHeightPx = pageIndicatorBackground?.intrinsicHeight ?: 0
    private val pageIndicatorTextOffsetPx =
        context.getDimensions(R.dimen.page_indicator_text_offset).toInt()
    private val pageIndicatorTextSize = context.getDimensions(R.dimen.page_indicator_text_size)

    private val textPaint: TextPaint =
        TextPaint().apply {
            color =
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSurface,
                    Color.BLACK,
                )
            textSize = pageIndicatorTextSize
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

    public var alpha: Int = GONE_ALPHA // Initially fast scroller should be hidden
        set(value) {
            thumbDrawable?.alpha = value
            pageIndicatorBackground?.alpha = value
            textPaint.alpha = value
        }

    internal var currentPageIndicatorLabel: String = ""

    internal var thumbBounds = RectF()
        private set

    internal var pageIndicatorBounds = RectF()
        private set

    /**
     * Draws the fast scroller on the canvas.
     *
     * This method performs the actual drawing of the fast scroll scrubber, including the thumb, the
     * drag handle (track), and the page indicator. It calculates the positions of these elements
     * based on the provided parameters and then uses the provided drawables to render them on the
     * canvas.
     *
     * @param canvas The canvas on which to draw the scrubber.
     * @param xOffset offset on x-axis in view coordinates.
     * @param yOffset offset on y-axis in view coordinates.
     * @param visiblePages The range of pages that are currently visible.
     */
    public fun draw(canvas: Canvas, xOffset: Int, yOffset: Int, visiblePages: Range<Int>) {
        // The intrinsic value is -1 if the width or height is not set for any drawable.
        if (thumbWidthPx < 0 || thumbHeightPx < 0 || pageIndicatorHeightPx < 0) {
            return
        }
        val thumbLeftPx = (xOffset - (thumbWidthPx + thumbMarginEnd))
        val thumbTopPx = yOffset
        val thumbBottomPx = thumbTopPx + thumbHeightPx
        val thumbRightPx = (xOffset - thumbMarginEnd)

        // TODO: b/410457433 - Add Shadow for the default fast scroller drawable

        thumbDrawable?.setBounds(thumbLeftPx, thumbTopPx, thumbRightPx, thumbBottomPx)
        thumbBounds.set(
            thumbLeftPx.toFloat(),
            thumbTopPx.toFloat(),
            thumbRightPx.toFloat(),
            thumbBottomPx.toFloat(),
        )
        thumbDrawable?.draw(canvas)

        drawPageIndicator(canvas, xOffset, thumbTopPx, visiblePages)
    }

    private fun drawPageIndicator(
        canvas: Canvas,
        xOffset: Int,
        thumbTopPx: Int,
        visiblePages: Range<Int>,
    ) {
        currentPageIndicatorLabel =
            buildPageIndicatorLabel(
                context,
                visiblePages,
                pdfDocument.pageCount,
                R.string.label_page_single,
                R.string.label_page_range,
            )
        val indicatorBounds =
            calculatePageIndicatorBounds(currentPageIndicatorLabel, xOffset, thumbTopPx)

        pageIndicatorBackground?.bounds = indicatorBounds
        pageIndicatorBounds.set(indicatorBounds)
        pageIndicatorBackground?.draw(canvas)

        val xPos = indicatorBounds.left + (indicatorBounds.width() / 2)
        val yPos =
            indicatorBounds.top + (indicatorBounds.height() / 2) -
                ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(currentPageIndicatorLabel, xPos.toFloat(), yPos, textPaint)
    }

    internal fun calculatePageIndicatorBounds(label: String, xOffset: Int, thumbTopPx: Int): Rect {
        val labelWidth = textPaint.measureText(label)
        val pageIndicatorWidthPx = (labelWidth + (2 * pageIndicatorTextOffsetPx)).toInt()
        val pageIndicatorHeightPx = pageIndicatorHeightPx

        val indicatorRightPx = xOffset - pageIndicatorMarginEnd
        val indicatorLeftPx = indicatorRightPx - pageIndicatorWidthPx
        val indicatorTopPx = thumbTopPx + ((thumbHeightPx - pageIndicatorHeightPx) / 2)
        val indicatorBottomPx = indicatorTopPx + pageIndicatorHeightPx

        return Rect(indicatorLeftPx, indicatorTopPx, indicatorRightPx, indicatorBottomPx)
    }

    public companion object {
        public const val VISIBLE_ALPHA: Int = 255
        public const val GONE_ALPHA: Int = 0
    }
}
