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
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.Range
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.view.PdfView
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
 * @param trackDrawable The drawable used for the fast scroller track (drag handle).
 * @param pageIndicatorBackground The drawable used for the background of the page indicator.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FastScrollDrawer(
    internal val context: Context,
    private val pdfDocument: PdfDocument,
    private val thumbDrawable: Drawable,
    private val trackDrawable: Drawable,
    private val pageIndicatorBackground: Drawable,
) {

    private val thumbWidthDp = context.getDimensions(R.dimen.default_thumb_width).toInt()
    private val thumbHeightDp = context.getDimensions(R.dimen.default_thumb_height).toInt()
    private val trackWidthDp = context.getDimensions(R.dimen.default_track_width).toInt()
    private val trackHeightDp = context.getDimensions(R.dimen.default_track_height).toInt()
    private val scrubberEdgeOffsetDp = context.getDimensions(R.dimen.scrubber_edge_offset).toInt()
    private val pageIndicatorHeightDp = context.getDimensions(R.dimen.page_indicator_height).toInt()
    private val pageIndicatorRightMarginDp =
        context.getDimensions(R.dimen.page_indicator_right_margin).toInt()
    private val pageIndicatorTextOffsetDp =
        context.getDimensions(R.dimen.page_indicator_text_offset).toInt()
    private val pageIndicatorTextXOffsetDp =
        context.getDimensions(R.dimen.page_indicator_text_offset_x).toInt()
    private val pageIndicatorTextYOffsetDp =
        context.getDimensions(R.dimen.page_indicator_text_offset_y).toInt()
    private val pageIndicatorTextSize = context.getDimensions(R.dimen.page_indicator_text_size)

    private val textPaint: TextPaint =
        TextPaint().apply {
            color =
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSurface,
                    Color.BLACK
                )
            textSize = pageIndicatorTextSize.dpToPx(context)
            textAlign = Paint.Align.CENTER
        }

    private val thumbShadowDrawable: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.drag_indicator_shadow)

    internal val thumbWidthPx = thumbWidthDp.dpToPx(context)
    internal val thumbHeightPx = thumbHeightDp.dpToPx(context)

    internal var alpha: Int = VISIBLE_ALPHA
        set(value) {
            thumbDrawable.alpha = value
            trackDrawable.alpha = value
            pageIndicatorBackground.alpha = value
            thumbShadowDrawable?.alpha = value
            textPaint.alpha = value
        }

    internal var currentPageIndicatorLabel: String = ""

    /**
     * Draws the fast scroller on the canvas.
     *
     * This method performs the actual drawing of the fast scroll scrubber, including the thumb, the
     * drag handle (track), and the page indicator. It calculates the positions of these elements
     * based on the provided parameters and then uses the provided drawables to render them on the
     * canvas.
     *
     * @param canvas The canvas on which to draw the scrubber.
     * @param zoom The current zoom level.
     * @param scrollY The vertical position of the scrubber in pixels.
     * @param visibleAreaPx The rectangular area of the view that is currently visible.
     * @param visiblePages The range of pages that are currently visible.
     */
    public fun draw(
        canvas: Canvas,
        zoom: Float,
        scrollY: Int,
        visibleAreaPx: Rect,
        visiblePages: Range<Int>
    ) {
        val thumbLeftPx =
            (PdfView.toViewCoord(visibleAreaPx.right.toFloat(), zoom, scroll = 0) -
                    thumbWidthDp.dpToPx(context))
                .toInt() + scrubberEdgeOffsetDp.dpToPx(context)
        val thumbTopPx =
            (scrollY + PdfView.toViewCoord(visibleAreaPx.top.toFloat(), zoom, scroll = 0)).toInt()
        val thumbBottomPx = thumbTopPx + thumbHeightDp.dpToPx(context)
        val thumbRightPx =
            PdfView.toViewCoord(visibleAreaPx.right.toFloat(), zoom, scroll = 0).toInt() +
                scrubberEdgeOffsetDp.dpToPx(context)

        thumbShadowDrawable?.setBounds(
            thumbLeftPx - SHADOW_OFFSET_FROM_SCRUBBER_DP.dpToPx(context),
            thumbTopPx - SHADOW_OFFSET_FROM_SCRUBBER_DP.dpToPx(context),
            thumbRightPx + SHADOW_OFFSET_FROM_SCRUBBER_DP.dpToPx(context),
            thumbBottomPx + SHADOW_OFFSET_FROM_SCRUBBER_DP.dpToPx(context)
        )
        thumbShadowDrawable?.draw(canvas)

        thumbDrawable.setBounds(thumbLeftPx, thumbTopPx, thumbRightPx, thumbBottomPx)
        thumbDrawable.draw(canvas)

        drawDragHandle(canvas, thumbRightPx, thumbTopPx)
        drawPageIndicator(canvas, thumbLeftPx, thumbTopPx, visiblePages)
    }

    private fun drawPageIndicator(
        canvas: Canvas,
        thumbLeftPx: Int,
        thumbTopPx: Int,
        visiblePages: Range<Int>
    ) {
        currentPageIndicatorLabel = generateLabel(visiblePages)
        val labelWidth = textPaint.measureText(currentPageIndicatorLabel).toInt()
        val pageIndicatorWidthPx = labelWidth + (2 * pageIndicatorTextOffsetDp.dpToPx(context))

        val indicatorLeftPx =
            thumbLeftPx - pageIndicatorWidthPx - pageIndicatorRightMarginDp.dpToPx(context)
        val indicatorTopPx =
            thumbTopPx +
                ((thumbHeightDp.dpToPx(context) - pageIndicatorHeightDp.dpToPx(context)) / 2)
        pageIndicatorBackground.setBounds(
            /* left= */ indicatorLeftPx,
            /* top= */ indicatorTopPx,
            /* right= */ indicatorLeftPx + pageIndicatorWidthPx,
            /* bottom= */ indicatorTopPx + pageIndicatorHeightDp.dpToPx(context)
        )
        pageIndicatorBackground.draw(canvas)

        val xPos = indicatorLeftPx + (pageIndicatorWidthPx / 2)
        val yPos = indicatorTopPx + pageIndicatorTextYOffsetDp
        canvas.drawText(currentPageIndicatorLabel, xPos.toFloat(), yPos.toFloat(), textPaint)
    }

    private fun drawDragHandle(canvas: Canvas, thumbRight: Int, thumbTop: Int) {
        val thumbCenterX = thumbRight - thumbWidthDp.dpToPx(context) / 2
        val thumbCenterY = thumbTop + thumbHeightDp.dpToPx(context) / 2

        // Calculate the top-left corner of the track to center it
        val trackLeft = thumbCenterX - trackWidthDp.dpToPx(context) / 2
        val trackTop = thumbCenterY - trackHeightDp.dpToPx(context) / 2

        trackDrawable.setBounds(
            trackLeft,
            trackTop,
            trackLeft + trackWidthDp.dpToPx(context),
            trackTop + trackHeightDp.dpToPx(context)
        )
        trackDrawable.draw(canvas)
    }

    private fun generateLabel(range: Range<Int>): String {
        val res = context.resources

        return if (range.length() == 0) {
            res.getString(R.string.label_page_single, range.upper, pdfDocument.pageCount)
        } else if (range.length() == 1) {
            res.getString(R.string.label_page_single, range.lower + 1, pdfDocument.pageCount)
        } else {
            res.getString(
                R.string.label_page_range,
                range.lower + 1,
                range.upper + 1,
                pdfDocument.pageCount
            )
        }
    }

    public companion object {
        private const val SHADOW_OFFSET_FROM_SCRUBBER_DP = 2
        public const val VISIBLE_ALPHA: Int = 255
        public const val GONE_ALPHA: Int = 0
    }
}
