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

package androidx.compose.material3.carousel

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollScope
import androidx.compose.foundation.pager.LazyLayoutScrollScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

/**
 * The state that can be used to control all types of carousels.
 *
 * @param currentItem the current item to be scrolled to.
 * @param currentItemOffsetFraction the offset of the current item as a fraction of the item's size.
 *   This should vary between -0.5 and 0.5 and indicates how to offset the current item from the
 *   snapped position.
 * @param itemCount the number of items this Carousel will have.
 */
@ExperimentalMaterial3Api
class CarouselState(
    currentItem: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentItemOffsetFraction: Float = 0f,
    itemCount: () -> Int,
) : ScrollableState {
    internal var pagerState: CarouselPagerState =
        CarouselPagerState(currentItem, currentItemOffsetFraction, itemCount)

    override val isScrollInProgress: Boolean
        get() = pagerState.isScrollInProgress

    /**
     * The item that sits closest to the snapped position. This is an observable value and will
     * change as the carousel scrolls either by gesture or animation.
     *
     * Please refer to [PagerState.currentPage] for more information.
     */
    val currentItem: Int
        get() = pagerState.currentPage

    override fun dispatchRawDelta(delta: Float): Float {
        return pagerState.dispatchRawDelta(delta)
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        pagerState.scroll(scrollPriority, block)
    }

    /**
     * Scroll (jump immediately) to a given [item].
     *
     * @param item The destination item to scroll to
     */
    suspend fun scrollToItem(item: Int) = pagerState.scrollToPage(item, 0f)

    /**
     * Scroll animate to a given [item]. If the [item] is too far away from [currentItem], Carousel
     * will avoid composing all intermediate items by jumping to a nearer item before animating the
     * scroll.
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.material3.samples.HorizontalCenteredHeroCarouselSample
     * @param item the index of the item to scroll to with an animation
     * @param animationSpec an [AnimationSpec] used to scroll between the items.
     */
    suspend fun animateScrollToItem(item: Int, animationSpec: AnimationSpec<Float> = spring()) =
        with(pagerState) {
            if ((item == currentPage && currentPageOffsetFraction == 0f) || pageCount == 0) {
                return
            }

            val targetPage = if (pageCount > 0) item.coerceIn(0, pageCount - 1) else 0
            scroll {
                // Update the target page
                LazyLayoutScrollScope(this@with, this)
                    .animateScrollToPage(
                        pagerState = this@with,
                        targetPage = targetPage,
                        targetPageOffsetToSnappedPosition = 0f,
                        animationSpec = animationSpec,
                        updateTargetPage = { updateTargetPage(it) },
                    )
            }
        }

    @ExperimentalMaterial3Api
    companion object {
        /** To keep current item and item offset saved */
        val Saver: Saver<CarouselState, *> =
            listSaver(
                save = {
                    listOf(
                        it.pagerState.currentPage,
                        it.pagerState.currentPageOffsetFraction,
                        it.pagerState.pageCount,
                    )
                },
                restore = {
                    CarouselState(
                        currentItem = it[0] as Int,
                        currentItemOffsetFraction = it[1] as Float,
                        itemCount = { it[2] as Int },
                    )
                },
            )
    }
}

/**
 * Creates a [CarouselState] that is remembered across compositions.
 *
 * @param initialItem The initial item that should be scrolled to.
 * @param itemCount The number of items this Carousel will have.
 */
@ExperimentalMaterial3Api
@Composable
fun rememberCarouselState(initialItem: Int = 0, itemCount: () -> Int): CarouselState {
    return rememberSaveable(saver = CarouselState.Saver) {
            CarouselState(
                currentItem = initialItem,
                currentItemOffsetFraction = 0F,
                itemCount = itemCount,
            )
        }
        .apply { pagerState.pageCountState.value = itemCount }
}

internal const val MinPageOffset = -0.5f
internal const val MaxPageOffset = 0.5f

internal class CarouselPagerState(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int,
) : PagerState(currentPage, currentPageOffsetFraction) {
    var pageCountState = mutableStateOf(updatedPageCount)

    // Observe changes to the lambda within the MutableState
    override val pageCount: Int
        get() = pageCountState.value.invoke()

    companion object {
        /** To keep current page and current page offset saved */
        val Saver: Saver<CarouselPagerState, *> =
            listSaver(
                save = {
                    listOf(
                        it.currentPage,
                        (it.currentPageOffsetFraction).coerceIn(MinPageOffset, MaxPageOffset),
                        it.pageCountState.value,
                    )
                },
                restore = {
                    CarouselPagerState(
                        currentPage = it[0] as Int,
                        currentPageOffsetFraction = it[1] as Float,
                        updatedPageCount = { it[2] as Int },
                    )
                },
            )
    }
}

private const val MaxPagesForAnimateScroll = 3

/**
 * Animate a scroll to the item at index [targetPage].
 *
 * This method differs from [PagerState]'s default [PagerState.animateScrollToPage] by taking each
 * item's snap offset into account. Since pages at different indices might have different snap
 * offsets in Carousel due keyline shifting, a scroll distance from one index to another cannot be
 * calculated using `(targetIndex - currentIndex) * pageSize` alone. This method includes any
 * difference in snap offset between the current index and the target index.
 *
 * @param pagerState the [PagerState] for this carousel
 * @param targetPage the page to animate to
 * @param targetPageOffsetToSnappedPosition any offset to add to the scroll distance
 * @param animationSpec an [AnimationSpec] used to scroll between the pages.
 * @param updateTargetPage lambda in which to update the target page during programmatic scrolls
 */
private suspend fun LazyLayoutScrollScope.animateScrollToPage(
    pagerState: PagerState,
    targetPage: Int,
    targetPageOffsetToSnappedPosition: Float,
    animationSpec: AnimationSpec<Float>,
    updateTargetPage: ScrollScope.(Int) -> Unit,
) {
    updateTargetPage(targetPage)
    val forward = targetPage > firstVisibleItemIndex
    val visiblePages = lastVisibleItemIndex - firstVisibleItemIndex + 1
    if (
        ((forward && targetPage > lastVisibleItemIndex) ||
            (!forward && targetPage < firstVisibleItemIndex)) &&
            abs(targetPage - firstVisibleItemIndex) >= MaxPagesForAnimateScroll
    ) {
        val preJumpPosition =
            if (forward) {
                (targetPage - visiblePages).coerceAtLeast(firstVisibleItemIndex)
            } else {
                (targetPage + visiblePages).coerceAtMost(firstVisibleItemIndex)
            }

        // Pre-jump to 1 viewport away from destination page, if possible
        snapToItem(preJumpPosition, 0)
    }

    // The final delta displacement will be the difference between the pages offsets
    // discounting whatever offset the original page had scrolled plus the offset
    // fraction requested by the user plus any delta in snap offset due to keyline shifting.
    val displacement =
        pagerState.calculateScrollDistanceTo(pagerState.currentPage, targetPage) +
            targetPageOffsetToSnappedPosition

    var previousValue = 0f
    animate(0f, displacement, animationSpec = animationSpec) { currentValue, _ ->
        val delta = currentValue - previousValue
        val consumed = scrollBy(delta)
        previousValue += consumed
    }
}

/**
 * Calculate the distance required to scroll between [currentPage] and [targetPage]'s snapped
 * position.
 *
 * @param currentPage the page from where the scroll will begin
 * @param targetPage the page to scroll to
 */
private fun PagerState.calculateScrollDistanceTo(currentPage: Int, targetPage: Int): Float {
    val layoutSize =
        if (layoutInfo.orientation == Orientation.Horizontal) layoutInfo.viewportSize.width
        else layoutInfo.viewportSize.height
    // Get the snap offsets for the current and target pages to include any delta in the returned
    // scroll distance
    val currentPageSnapOffset =
        layoutInfo.snapPosition.position(
            layoutSize,
            layoutInfo.pageSize,
            layoutInfo.beforeContentPadding,
            layoutInfo.afterContentPadding,
            currentPage,
            pageCount,
        )
    val targetPageSnapOffset =
        layoutInfo.snapPosition.position(
            layoutSize,
            layoutInfo.pageSize,
            layoutInfo.beforeContentPadding,
            layoutInfo.afterContentPadding,
            targetPage,
            pageCount,
        )
    val snapOffsetDiff = currentPageSnapOffset - targetPageSnapOffset
    val targetPageDiff = targetPage - currentPage
    val pageSizeWithSpacing = layoutInfo.pageSize.toFloat() + layoutInfo.pageSpacing.toFloat()
    return (targetPageDiff * pageSizeWithSpacing) + snapOffsetDiff
}

/**
 * Interface to hold information about a Carousel item and its size.
 *
 * Example of CarouselItemDrawInfo usage:
 *
 * @sample androidx.compose.material3.samples.FadingHorizontalMultiBrowseCarouselSample
 */
@ExperimentalMaterial3Api
sealed interface CarouselItemDrawInfo {

    /** The size of the carousel item in the main axis in pixels */
    val size: Float

    /**
     * The minimum size of the carousel item in the main axis in pixels, eg. the size of the item as
     * it scrolls off the sides of the carousel
     */
    val minSize: Float

    /**
     * The maximum size of the carousel item in the main axis in pixels, eg. the size of the item
     * when it is at a focal position
     */
    val maxSize: Float

    /** The [Rect] by which the carousel item is being clipped. */
    val maskRect: Rect
}

@OptIn(ExperimentalMaterial3Api::class)
internal class CarouselItemDrawInfoImpl : CarouselItemDrawInfo {

    var sizeState by mutableFloatStateOf(0f)
    var minSizeState by mutableFloatStateOf(0f)
    var maxSizeState by mutableFloatStateOf(0f)
    var maskRectState by mutableStateOf(Rect.Zero)

    override val size: Float
        get() = sizeState

    override val minSize: Float
        get() = minSizeState

    override val maxSize: Float
        get() = maxSizeState

    override val maskRect: Rect
        get() = maskRectState
}
