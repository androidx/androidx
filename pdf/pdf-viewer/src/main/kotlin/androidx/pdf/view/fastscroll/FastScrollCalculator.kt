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

/** Class to compute scroll for [FastScroller] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FastScrollCalculator(
    private val context: Context,
) {
    internal val scrollerTopMarginDp = context.getDimensions(R.dimen.scroller_top_margin).toInt()
    internal val scrollerBottomMarginDp =
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
            .toInt()
            .coerceIn(
                scrollerTopMarginDp.dpToPx(context),
                viewHeight - (scrollerBottomMarginDp.dpToPx(context) + thumbHeightPx)
            )
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
     * @param zoom The current zoom level.
     * @param viewHeight The height of the view in pixels.
     * @param thumbHeightPx Height of the thumbnail in pixels
     * @param estimatedFullHeight Estimated height of the document including all pages
     * @return The calculated vertical position of the fast scroll scrubber, constrained to the
     *   valid bounds of the view.
     */
    public fun computeThumbPosition(
        scrollY: Int,
        zoom: Float,
        viewHeight: Int,
        thumbHeightPx: Int,
        estimatedFullHeight: Int
    ): Int {
        val scrollbarBottom = viewHeight - scrollerBottomMarginDp.dpToPx(context)
        val scrollbarLength = scrollbarBottom - scrollerTopMarginDp.dpToPx(context)

        val position = scrollY / zoom
        val scrollRange = estimatedFullHeight - (viewHeight / zoom)
        val tempThumbY =
            (scrollbarLength * position / scrollRange).toInt() + scrollerTopMarginDp.dpToPx(context)
        return constrainScrollPosition(tempThumbY.toFloat(), viewHeight, thumbHeightPx)
    }

    /**
     * Calculates the content scroll position corresponding to a given fast scroll position.
     *
     * This method determines the vertical scroll position within the document content that
     * corresponds to the provided fast scroll position. It takes into account the estimated height
     * of the content, the view height, and the current zoom level to accurately map the fast scroll
     * position to the content scroll position.
     *
     * @param fastScrollY The vertical position of the fast scroll scrubber in pixels.
     * @param viewHeight The height of the view in pixels.
     * @param zoom The current zoom level. Defaults to 1.0f (no zoom).
     * @param estimatedFullHeight Estimated height of the document including all pages
     * @return The calculated content scroll position in pixels.
     */
    public fun computeViewScroll(
        fastScrollY: Int,
        viewHeight: Int,
        zoom: Float,
        estimatedFullHeight: Int
    ): Int {
        val scrollbarBottom = viewHeight - scrollerBottomMarginDp.dpToPx(context)
        val scrollbarLength = scrollbarBottom - scrollerTopMarginDp.dpToPx(context)

        val fraction = fastScrollY.toFloat() / scrollbarLength
        val scrollRange = estimatedFullHeight - (viewHeight / zoom)
        return (scrollRange * fraction * zoom).toInt()
    }
}
