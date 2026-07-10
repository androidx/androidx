/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.pager

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import androidx.xr.glimmer.internal.SingleItemScrollConstraintConnection
import kotlin.math.abs

/**
 * [GlimmerHorizontalPager] is a lazily-composed, horizontally scrollable layout that arranges its
 * pages sequentially. It is similar to [androidx.compose.foundation.pager.HorizontalPager], but
 * contains Glimmer behaviors and default values. Only one page is prominently displayed at a time,
 * and the pager uses snap animations to ensure that a page always settles exactly into the viewport
 * boundaries after a user's scrolling gesture ends.
 *
 * Note: When displaying text within a [GlimmerHorizontalPager], it is strongly recommended to set
 * [androidx.compose.ui.text.TextStyle.textMotion] to
 * [androidx.compose.ui.text.style.TextMotion.Animated]. This ensures smooth rendering during layout
 * animations or scaling transitions, preventing pixel-snapping artifacts.
 *
 * @sample androidx.xr.glimmer.samples.GlimmerHorizontalPagerSample
 * @param state the state object to be used to control or observe the pager's state.
 * @param modifier the modifier to apply to this layout.
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first page or after the last one. Use [pageSpacing] to add spacing between
 *   the pages.
 * @param beyondViewportPageCount Pages to compose and layout before and after the list of visible
 *   pages. Note: Be aware that using a large value for [beyondViewportPageCount] will cause a lot
 *   of pages to be composed, measured and placed which will defeat the purpose of using lazy
 *   loading. This should be used as an optimization to pre-load a couple of pages before and after
 *   the visible ones. This does not include the pages automatically composed and laid out by the
 *   pre-fetcher in the direction of the scroll during scroll events.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param verticalAlignment how pages are aligned vertically within the pager's viewport.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using [GlimmerPagerState.scroll] even when it is
 *   disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When specified, the scroll position
 *   will be maintained based on the key. If null, the position in the pager will represent the key.
 * @param pageContent a block that describes the content of a single page.
 */
@Composable
public fun GlimmerHorizontalPager(
    state: GlimmerPagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondViewportPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.Bottom,
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((page: Int) -> Any)? = null,
    pageContent: @Composable GlimmerPagerScope.(page: Int) -> Unit,
) {
    val singleItemScrollConstraintConnection =
        remember(state.foundationPagerState) {
            SingleItemScrollConstraintConnection(state.foundationPagerState)
        }

    HorizontalPager(
        state = state.foundationPagerState,
        modifier =
            modifier
                .horizontalPagerScrim(state)
                .nestedScroll(singleItemScrollConstraintConnection)
                .pagerAutoFocus(state),
        contentPadding = contentPadding,
        pageSize = PageSize.Fill,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        userScrollEnabled = userScrollEnabled,
        flingBehavior =
            PagerDefaults.flingBehavior(
                state = state.foundationPagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
            ),
        reverseLayout = reverseLayout,
        key = key,
    ) { page ->
        PageItemLayout(state, page) { GlimmerPagerScopeImpl.pageContent(page) }
    }
}

@Composable
private fun PageItemLayout(state: GlimmerPagerState, page: Int, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        var maxWidth = 0
        var maxHeight = 0
        val placeables =
            measurables.fastMap {
                it.measure(constraints).also { placeable ->
                    maxWidth = maxOf(maxWidth, placeable.width)
                    maxHeight = maxOf(maxHeight, placeable.height)
                }
            }

        layout(maxWidth, maxHeight) {
            val continuousPosition = state.currentPage + state.currentPageOffsetFraction
            val distanceFromSnappedPosition = abs(continuousPosition - page).coerceIn(0f, 1f)

            if (distanceFromSnappedPosition == 0f) {
                // Skip the transition effects if the page is centered.
                placeables.fastForEach { placeable -> placeable.placeRelativeWithLayer(0, 0) }
                return@layout
            }

            val scaleProgress =
                (distanceFromSnappedPosition / ScaleProgressThreshold).coerceIn(0f, 1f)
            val itemScale = lerp(1f, MinScale, scaleProgress)

            val itemAlpha = lerp(1f, 0f, distanceFromSnappedPosition)

            val blurProgress =
                (distanceFromSnappedPosition / BlurProgressThreshold).coerceIn(0f, 1f)
            val blurRadiusPx = lerp(0f, MaxBlurRadius.toPx(), blurProgress)

            // Align the page to the bottom of the viewport during scale transformation
            val itemTransformOrigin = TransformOrigin(0.5f, 1f)

            placeables.fastForEach { placeable ->
                placeable.placeRelativeWithLayer(0, 0) {
                    alpha = itemAlpha
                    scaleY = itemScale
                    transformOrigin = itemTransformOrigin
                    if (blurRadiusPx > 1f) {
                        renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Decal)
                    }
                }
            }
        }
    }
}

/** The minimum scale applied to a page when it is scrolled away from the snapped position. */
private const val MinScale = 0.9f

/** The threshold of scroll progress at which the scale reaches [MinScale]. */
private const val ScaleProgressThreshold = 0.69f

/** The threshold of scroll progress at which the blur effect reaches [MaxBlurRadius]. */
private const val BlurProgressThreshold = 0.82f
private val MaxBlurRadius = 2f.dp
