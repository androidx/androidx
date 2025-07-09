/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.startOffset
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager

/**
 * An interface for providing scroll information for different scrollable containers, such lists.
 * Used for scrolling away, showing, hiding or scaling screen elements based on scrollable state.
 *
 * [ScrollInfoProvider] can be used to create a ScrollAway modifier, typically applied to an object
 * that appears at the top of the screen to scroll it away vertically when a list is scrolled
 * upwards. The scrolled offset is typically calculated with reference to the position of an anchor
 * e.g. the top item.
 */
public interface ScrollInfoProvider {
    /**
     * Whether it is valid to scroll away the anchor item with the current configuration, For
     * example, if the selected anchor item does not exist, it is not valid to scroll away.
     */
    public val isScrollAwayValid: Boolean

    /** Whether the container is currently scrollable. */
    public val isScrollable: Boolean

    /**
     * Whether the list is currently scrolling (which can be used to show/hide a scroll indicator or
     * time text during the scroll operation).
     */
    public val isScrollInProgress: Boolean

    /**
     * The amount that the anchor item has been scrolled upwards in the y direction (in Pixels),
     * relative to the initial position of the scrolling container (so >= zero). In the event that
     * the anchor item is no longer visible on the screen and its offset cannot be calculated, the
     * returned offset is Float.NaN.
     */
    public val anchorItemOffset: Float

    /**
     * The amount of space between the last item (which may not be visible) and the bottom edge of
     * the viewport. This is always greater or equal to 0, if there is no (or negative) room
     * (including the case in which the last item is not on screen), 0 should be returned.
     */
    public val lastItemOffset: Float
}

/**
 * Function for creating a [ScrollInfoProvider] from a [ScalingLazyListState], for use with
 * [ScalingLazyColumn] - used to coordinate between scrollable content and scaffold content such as
 * [TimeText] which is scrolled away at the top of the screen and [EdgeButton] which is scaled.
 */
public fun ScrollInfoProvider(state: ScalingLazyListState): ScrollInfoProvider =
    ScalingLazyListStateScrollInfoProvider(state)

/**
 * Function for creating a [ScrollInfoProvider] from a [LazyListState], for use with [LazyColumn] -
 * used to coordinate between scrollable content and scaffold content such as [TimeText] which is
 * scrolled away at the top of the screen and [EdgeButton] which is scaled.
 */
public fun ScrollInfoProvider(state: LazyListState): ScrollInfoProvider =
    LazyListStateScrollInfoProvider(state)

/**
 * Function for creating a [ScrollInfoProvider] from a [TransformingLazyColumnState], for use with
 * [TransformingLazyColumn] - used to coordinate between scrollable content and scaffold content
 * such as [TimeText] which is scrolled away at the top of the screen and [EdgeButton] which is
 * scaled.
 */
public fun ScrollInfoProvider(state: TransformingLazyColumnState): ScrollInfoProvider =
    TransformingLazyColumnStateScrollInfoProvider(state)

/**
 * Function for creating a [ScrollInfoProvider] from a [ScrollState], for use with [Column] - used
 * to coordinate between scrollable content and scaffold content such as [TimeText] which is
 * scrolled away at the top of the screen and [EdgeButton] which is scaled.
 *
 * @param state the [ScrollState] to use as the base for creating the [ScrollInfoProvider]
 */
public fun ScrollInfoProvider(state: ScrollState): ScrollInfoProvider =
    ScrollStateScrollInfoProvider(state)

/**
 * Function for creating a [ScrollInfoProvider] from a [PagerState], for use with [HorizontalPager]
 * and [VerticalPager]
 * - used to coordinate when to fade out the PageIndicator and [TimeText]. The PageIndicator fades
 *   out when when scrolling is finished and the screen is in an idle state.
 *
 * @param state the [PagerState] to use as the base for creating the [ScrollInfoProvider]
 */
public fun ScrollInfoProvider(state: PagerState): ScrollInfoProvider =
    PagerStateScrollInfoProvider(state)

// Implementation of [ScrollInfoProvider] for [ScalingLazyColumn].
// Being in Foundation, this implementation has access to the ScalingLazyListState
// auto-centering params, which are internal.
private class ScalingLazyListStateScrollInfoProvider(val state: ScalingLazyListState) :
    ScrollInfoProvider {
    override val isScrollAwayValid
        get() =
            state.layoutInfo.totalItemsCount > (state.config.value?.autoCentering?.itemIndex ?: 1)

    override val isScrollable
        get() =
            state.layoutInfo.visibleItemsInfo.isNotEmpty() &&
                (state.canScrollBackward || state.canScrollForward)

    override val isScrollInProgress
        get() = state.isScrollInProgress

    override val anchorItemOffset: Float
        get() {
            val layoutInfo = state.layoutInfo
            return layoutInfo.visibleItemsInfo
                .fastFirstOrNull { it.index == 1 }
                ?.let {
                    val startOffset = it.startOffset(ScalingLazyListAnchorType.ItemStart)
                    if (initialStartOffset == null || startOffset > initialStartOffset!!) {
                        initialStartOffset = startOffset
                    }
                    -it.offset + initialStartOffset!!
                } ?: Float.NaN
        }

    override val lastItemOffset: Float
        get() {
            val screenHeightPx = state.config.value?.viewportHeightPx ?: 0
            val layoutInfo = state.layoutInfo
            val reverseLayout = state.config.value?.reverseLayout ?: false
            return if (reverseLayout) {
                layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                    if (it.index != 0) {
                        return@let 0f
                    }
                    val bottomEdge = -it.offset + screenHeightPx / 2 + it.size / 2
                    (screenHeightPx - bottomEdge).toFloat().coerceAtLeast(0f)
                } ?: 0f
            } else {
                layoutInfo.visibleItemsInfo.lastOrNull()?.let {
                    if (it.index != layoutInfo.totalItemsCount - 1) {
                        return@let 0f
                    }
                    val bottomEdge = it.offset + screenHeightPx / 2 + it.size / 2
                    (screenHeightPx - bottomEdge).toFloat().coerceAtLeast(0f)
                } ?: 0f
            }
        }

    override fun toString(): String {
        return "ScalingLazyListStateScrollInfoProvider(isScrollAwayValid=$isScrollAwayValid, " +
            "isScrollable=$isScrollable," +
            "isScrollInProgress=$isScrollInProgress, " +
            "anchorItemOffset=$anchorItemOffset, " +
            "lastItemOffset=$lastItemOffset)"
    }

    private var initialStartOffset: Float? = null
}

// Implementation of [ScrollInfoProvider] for [LazyColumn].
private class LazyListStateScrollInfoProvider(val state: LazyListState) : ScrollInfoProvider {
    override val isScrollAwayValid
        get() = state.layoutInfo.totalItemsCount > 0

    override val isScrollable
        get() = state.layoutInfo.totalItemsCount > 0

    override val isScrollInProgress
        get() = state.isScrollInProgress

    override val anchorItemOffset
        get() =
            state.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                if (it.index != 0) {
                    return@let Float.NaN
                }
                -it.offset.toFloat()
            } ?: Float.NaN

    override val lastItemOffset: Float
        get() {
            val layoutInfo = state.layoutInfo
            val lazyColumnHeightPx = layoutInfo.viewportSize.height
            val reverseLayout = state.layoutInfo.reverseLayout
            return if (reverseLayout) {
                layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                    if (it.index != 0) {
                        return@let 0f
                    }
                    val bottomEdge =
                        -it.offset + lazyColumnHeightPx + layoutInfo.viewportStartOffset
                    (lazyColumnHeightPx - bottomEdge).toFloat().coerceAtLeast(0f)
                } ?: 0f
            } else {
                layoutInfo.visibleItemsInfo.lastOrNull()?.let {
                    if (it.index != layoutInfo.totalItemsCount - 1) {
                        return@let 0f
                    }
                    val bottomEdge = it.offset + it.size - layoutInfo.viewportStartOffset
                    (lazyColumnHeightPx - bottomEdge).toFloat().coerceAtLeast(0f)
                } ?: 0f
            }
        }

    override fun toString(): String {
        return "LazyListStateScrollInfoProvider(isScrollAwayValid=$isScrollAwayValid, " +
            "isScrollable=$isScrollable," +
            "isScrollInProgress=$isScrollInProgress, " +
            "anchorItemOffset=$anchorItemOffset, " +
            "lastItemOffset=$lastItemOffset)"
    }
}

// Implementation of [ScrollInfoProvider] for [Column].
private class ScrollStateScrollInfoProvider(val state: ScrollState) : ScrollInfoProvider {
    override val isScrollAwayValid: Boolean
        get() = true

    override val isScrollable: Boolean
        get() = state.maxValue != 0

    // Work around the default implementation of ScrollState not providing a useful
    // isScrollInProgress
    private var prevOffset: Int? = null
    override val isScrollInProgress
        get() =
            state.value.let { currentOffset ->
                (state.isScrollInProgress || (prevOffset != null && prevOffset != currentOffset))
                    .also { prevOffset = currentOffset }
            }

    override val anchorItemOffset: Float
        get() = state.value.toFloat()

    override val lastItemOffset: Float
        get() = 0f

    override fun toString(): String {
        return "ScrollStateScrollInfoProvider(isScrollAwayValid=$isScrollAwayValid, " +
            "isScrollable=$isScrollable," +
            "isScrollInProgress=$isScrollInProgress, " +
            "anchorItemOffset=$anchorItemOffset, " +
            "lastItemOffset=$lastItemOffset)"
    }
}

// Implementation of [ScrollInfoProvider] for [TransformingLazyColumn].
private class TransformingLazyColumnStateScrollInfoProvider(
    val state: TransformingLazyColumnState
) : ScrollInfoProvider {
    override val isScrollAwayValid
        get() = state.layoutInfo.totalItemsCount > 0

    override val isScrollable
        get() =
            state.layoutInfo.totalItemsCount > 0 &&
                (state.canScrollBackward || state.canScrollForward)

    override val isScrollInProgress
        get() = state.isScrollInProgress

    // TODO: b/3364857296 - Rework using scroll anchor item.
    private var initialStartOffset: Float = Float.NaN

    override val anchorItemOffset: Float
        get() =
            state.layoutInfo.visibleItems.firstOrNull()?.let { item ->
                if (item.index != 0) {
                    return@let Float.NaN
                }
                val newOffset = item.offset.toFloat()
                if (initialStartOffset.isNaN()) {
                    initialStartOffset = newOffset
                }
                initialStartOffset - newOffset
            } ?: Float.NaN

    private var previousLastItemKey: Any? = null

    override val lastItemOffset: Float
        get() {
            val layoutInfo = state.layoutInfo
            val screenHeightPx = layoutInfo.viewportSize.height
            return layoutInfo.visibleItems.lastOrNull()?.let { lastItem ->
                if (lastItem.index != layoutInfo.totalItemsCount - 1) {
                    previousLastItemKey = null
                    return@let 0f
                }

                val animation =
                    if (!state.isScrollInProgress) {
                        state.animator.getAnimation(lastItem.key)
                    } else {
                        null
                    }

                val offset =
                    if (
                        animation?.isPlacementAnimationInProgress == true &&
                            previousLastItemKey == lastItem.key &&
                            animation.animatedScrollProgress.isSpecified
                    ) {
                        animation.finalOffset.y
                    } else {
                        lastItem.offset
                    }

                if (animation?.isPlacementAnimationInProgress != true) {
                    previousLastItemKey = lastItem.key
                }

                (screenHeightPx - offset - lastItem.transformedHeight).toFloat().coerceAtLeast(0f)
            } ?: 0f
        }

    override fun toString(): String {
        return "TransformingLazyColumnStateScrollInfoProvider(" +
            "isScrollAwayValid=$isScrollAwayValid, " +
            "isScrollable=$isScrollable," +
            "isScrollInProgress=$isScrollInProgress, " +
            "anchorItemOffset=$anchorItemOffset, " +
            "lastItemOffset=$lastItemOffset)"
    }
}

// Implementation of [ScrollInfoProvider] for [Pager].
private class PagerStateScrollInfoProvider(val state: PagerState) : ScrollInfoProvider {
    override val isScrollAwayValid: Boolean
        get() = false

    override val isScrollable: Boolean
        get() = state.canScrollBackward || state.canScrollForward

    override val isScrollInProgress: Boolean
        get() = state.isScrollInProgress

    override val anchorItemOffset: Float
        get() = Float.NaN

    override val lastItemOffset: Float
        get() = 0f
}
