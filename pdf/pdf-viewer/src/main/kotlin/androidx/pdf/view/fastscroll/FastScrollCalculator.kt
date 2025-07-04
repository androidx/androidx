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
import androidx.annotation.RestrictTo
import androidx.pdf.R
import kotlin.math.roundToInt

/** Class to compute scroll for [FastScroller] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FastScrollCalculator(private val context: Context) {
    internal val scrollerTopMarginPx = context.getDimensions(R.dimen.scroller_top_margin).toInt()
    internal val scrollerBottomMarginPx =
        context.getDimensions(R.dimen.scroller_bottom_margin).toInt()

    /**
     * Constrains the vertical scroll position to ensure it remains within the valid bounds of the
     * view.
     *
     * @param scrollY The raw vertical scroll position in pixels.
     * @param viewHeight The height of the view in pixels.
     * @param thumbHeightPx Thumbnail height in pixels
     * @return The constrained vertical scroll position.
     */
    public fun constrainScrollPosition(scrollY: Float, viewHeight: Int, thumbHeightPx: Int): Int {
        return scrollY
            .roundToInt()
            .coerceIn(scrollerTopMarginPx, viewHeight - (scrollerBottomMarginPx + thumbHeightPx))
    }

    /**
     * Calculates the vertical position of the fast scroll scrubber.
     *
     * This method determines the scrubber's position based on the current scroll state, zoom level,
     * and the estimated height of the document content. It takes into account the visible height of
     * the view and the scrollable range to accurately position the scrubber within the fast scroll
     * track.
     *
     * @param scrollY The current vertical scroll position in pixels.
     * @param viewHeight The height of the view in pixels.
     * @param thumbHeightPx Height of the thumbnail in pixels
     * @param estimatedFullHeight Estimated height of the document including all pages in pixels.
     * @return The calculated vertical position of the fast scroll scrubber, constrained to the
     *   valid bounds of the view.
     */
    public fun computeThumbPosition(
        scrollY: Int,
        viewHeight: Int,
        thumbHeightPx: Int,
        estimatedFullHeight: Float,
    ): Int {
        val scrollbarLength = getScrollbarLength(viewHeight, thumbHeightPx)

        val scrollableHeight = estimatedFullHeight - viewHeight
        val scrollPercent = scrollY / scrollableHeight
        var deltaY = scrollPercent * scrollbarLength

        // Offset the scrollbar position by the top margin.
        // This ensures the scrollbar starts at the margin when the content is at the top.
        deltaY += scrollerTopMarginPx

        return constrainScrollPosition(deltaY, viewHeight, thumbHeightPx)
    }

    /**
     * Calculates the content scroll position corresponding to a given fast scroll position.
     *
     * This method determines the vertical scroll position within the document content that
     * corresponds to the provided fast scroll position. It takes into account the estimated height
     * of the content(in pixels) and the view height to accurately map the fast scroll position to
     * the content scroll position(in pixels).
     *
     * @param fastScrollY The vertical position of the fast scroll scrubber in pixels.
     * @param viewHeight The height of the view in pixels.
     * @param thumbHeightPx Height of the thumbnail in pixels.
     * @param estimatedFullHeight Estimated height of the document including all pages in pixels.
     * @return The calculated content scroll position in pixels.
     */
    public fun computeViewScroll(
        fastScrollY: Int,
        viewHeight: Int,
        thumbHeightPx: Int,
        estimatedFullHeight: Float,
    ): Int {
        val scrollbarLength = getScrollbarLength(viewHeight, thumbHeightPx)

        // Calculate the offset of the fast scroll position from the top margin.
        val scrollYOffset =
            (fastScrollY.toFloat() - scrollerTopMarginPx).coerceIn(0F, scrollbarLength.toFloat())

        val scrollFraction = scrollYOffset / scrollbarLength
        val scrollableHeight = estimatedFullHeight - viewHeight

        return (scrollFraction * scrollableHeight).roundToInt()
    }

    /**
     * Calculates the vertical length of the fast scroll track.
     *
     * This method determines the vertical range available for the fast scroll thumb to move, which
     * is the view's height minus the top and bottom margins and the thumb's height.
     *
     * @param viewHeight The height of the view in pixels.
     * @param thumbHeightPx The height of the fast scroll thumb in pixels.
     * @return The length of the fast scroll track in pixels.
     */
    private fun getScrollbarLength(viewHeight: Int, thumbHeightPx: Int): Int =
        viewHeight - (scrollerTopMarginPx + scrollerBottomMarginPx + thumbHeightPx)
}
