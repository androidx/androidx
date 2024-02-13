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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * <a href=https://m3.material.io/components/carousel/overview" class="external" target="_blank">Material Design Carousel</a>
 *
 * A horizontal carousel meant to display many items at once for quick browsing of smaller content
 * like album art or photo thumbnails.
 *
 * Note that this carousel may adjust the size of large items. In order to ensure a mix of large,
 * medium, and small items fit perfectly into the available space and are arranged in a
 * visually pleasing way, this carousel finds the nearest number of large items that
 * will fit the container and adjusts their size to fit, if necessary.
 *
 * For more information, see <a href="https://material.io/components/carousel/overview">design
 * guidelines</a>.
 *
 * @param state The state object to be used to control the carousel's state
 * @param preferredItemSize The size fully visible items would like to be in the main axis. This
 * size is a target and will likely be adjusted by carousel in order to fit a whole number of
 * items within the container
 * @param modifier A modifier instance to be applied to this carousel container
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param minSmallSize The minimum allowable size of small masked items
 * @param maxSmallSize The maximum allowable size of small masked items
 * @param content The carousel's content Composable
 *
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun HorizontalMultiBrowseCarousel(
    state: CarouselState,
    preferredItemSize: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    minSmallSize: Dp = StrategyDefaults.MinSmallSize,
    maxSmallSize: Dp = StrategyDefaults.MaxSmallSize,
    content: @Composable CarouselScope.(itemIndex: Int) -> Unit
) {
    val density = LocalDensity.current
    Carousel(
        state = state,
        orientation = Orientation.Horizontal,
        keylineList = { availableSpace ->
            with(density) {
                multiBrowseKeylineList(
                    density = this,
                    carouselMainAxisSize = availableSpace,
                    preferredItemSize = preferredItemSize.toPx(),
                    itemSpacing = itemSpacing.toPx(),
                    minSmallSize = minSmallSize.toPx(),
                    maxSmallSize = maxSmallSize.toPx()
                )
            }
        },
        modifier = modifier,
        itemSpacing = itemSpacing,
        content = content
    )
}

/**
 * <a href=https://m3.material.io/components/carousel/overview" class="external" target="_blank">Material Design Carousel</a>
 *
 * A horizontal carousel that displays its items with the given size except for one item at the end
 * that is cut off.
 *
 * Note that the item size will be bound by the size of the carousel. Otherwise, this carousel lays
 * out as many items as it can in the given size, and changes the size of the last cut off item such
 * that there is a range of motion when items scroll off the edge.
 *
 * For more information, see <a href="https://material.io/components/carousel/overview">design
 * guidelines</a>.
 *
 * @param state The state object to be used to control the carousel's state
 * @param itemSize The size of items in the carousel
 * @param modifier A modifier instance to be applied to this carousel container
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param content The carousel's content Composable
 *
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun HorizontalUncontainedCarousel(
    state: CarouselState,
    itemSize: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    content: @Composable CarouselScope.(itemIndex: Int) -> Unit
) {
    val density = LocalDensity.current
    Carousel(
        state = state,
        orientation = Orientation.Horizontal,
        keylineList = {
            with(density) {
                uncontainedKeylineList(
                    density = this,
                    carouselMainAxisSize = state.pagerState.layoutInfo.viewportSize.width.toFloat(),
                    itemSize = itemSize.toPx(),
                    itemSpacing = itemSpacing.toPx(),
                )
            }
        },
        modifier = modifier,
        itemSpacing = itemSpacing,
        content = content
    )
}

/**
 * <a href=https://m3.material.io/components/carousel/overview" class="external" target="_blank">Material Design Carousel</a>
 *
 * Carousels contain a collection of items that changes sizes according to their placement and the
 * chosen strategy.
 *
 * @param state The state object to be used to control the carousel's state.
 * @param modifier A modifier instance to be applied to this carousel outer layout
 * @param keylineList The list of keylines that are fixed positions along the scrolling axis which
 * define the state an item should be in when its center is co-located with the keyline's position.
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param orientation The layout orientation of the carousel
 * @param content The carousel's content Composable where each call is passed the index, from the
 * total item count, of the item being composed
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun Carousel(
    state: CarouselState,
    orientation: Orientation,
    keylineList: (availableSpace: Float) -> KeylineList?,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    content: @Composable CarouselScope.(itemIndex: Int) -> Unit
) {
    val pageSize = remember(keylineList) { CarouselPageSize(keylineList) }

    // TODO: Update beyond bounds numbers according to Strategy
    val outOfBoundsPageCount = 2
    val carouselScope = CarouselScopeImpl

    if (orientation == Orientation.Horizontal) {
        HorizontalPager(
            state = state.pagerState,
            pageSize = pageSize,
            pageSpacing = itemSpacing,
            outOfBoundsPageCount = outOfBoundsPageCount,
            modifier = modifier
        ) { page ->
            Box(modifier = Modifier.carouselItem(page, state, pageSize.strategy)) {
                carouselScope.content(page)
            }
        }
    } else if (orientation == Orientation.Vertical) {
        VerticalPager(
            state = state.pagerState,
            pageSize = pageSize,
            pageSpacing = itemSpacing,
            outOfBoundsPageCount = outOfBoundsPageCount,
            modifier = modifier
        ) { page ->
            Box(modifier = Modifier.carouselItem(page, state, pageSize.strategy)) {
                carouselScope.content(page)
            }
        }
    }
}

/**
 * A [PageSize] implementation that maintains a strategy that is kept up-to-date with the
 * latest available space of the container.
 *
 * @param keylineList The list of keylines that are fixed positions along the scrolling axis which
 * define the state an item should be in when its center is co-located with the keyline's position.
 */
private class CarouselPageSize(keylineList: (availableSpace: Float) -> KeylineList?) : PageSize {
    val strategy = Strategy(keylineList)
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
        strategy.apply(availableSpace.toFloat())
        return if (strategy.isValid()) {
            strategy.itemMainAxisSize.roundToInt()
        } else {
            // If strategy does not have a valid arrangement, default to a
            // full size item, as Pager does by default.
            availableSpace
        }
    }
}

/**
 * This class defines ways items can be aligned along a carousel's main axis.
 */
@JvmInline
internal value class CarouselAlignment private constructor(internal val value: Int) {
    companion object {
        /** Start aligned carousels place focal items at the start/top of the container */
        val Start = CarouselAlignment(-1)

        /** Center aligned carousels place focal items in the middle of the container */
        val Center = CarouselAlignment(0)

        /** End aligned carousels place focal items at the end/bottom of the container */
        val End = CarouselAlignment(1)
    }
}

/**
 * A modifier that handles clipping and translating an item as it moves along the scrolling axis
 * of a Carousel.
 *
 * @param index the index of the item in the carousel
 * @param state the carousel state
 * @param strategy the strategy used to mask and translate items in the carousel
 */
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
internal fun Modifier.carouselItem(
    index: Int,
    state: CarouselState,
    strategy: Strategy
): Modifier {
    val viewportSize = state.pagerState.layoutInfo.viewportSize
    val orientation = state.pagerState.layoutInfo.orientation
    val isVertical = orientation == Orientation.Vertical
    val mainAxisCarouselSize = if (isVertical) viewportSize.height else viewportSize.width

    if (mainAxisCarouselSize == 0 || !strategy.isValid()) {
        return this
    }
    // Scroll offset calculation using currentPage and currentPageOffsetFraction
    val firstVisibleItemScrollOffset =
        state.pagerState.currentPageOffsetFraction * strategy.itemMainAxisSize
    val scrollOffset = (state.pagerState.currentPage * strategy.itemMainAxisSize) +
        firstVisibleItemScrollOffset
    val itemsCount = state.pagerState.pageCount

    val maxScrollOffset =
        itemsCount * strategy.itemMainAxisSize - mainAxisCarouselSize
    val keylines = strategy.getKeylineListForScrollOffset(scrollOffset, maxScrollOffset)

    // Find center of the item at this index
    val unadjustedCenter =
        (index * strategy.itemMainAxisSize) + (strategy.itemMainAxisSize / 2f) - scrollOffset

    // Find the keyline before and after this item's center and create an interpolated
    // keyline that the item should use for its clip shape and offset
    val keylineBefore =
        keylines.getKeylineBefore(unadjustedCenter)
    val keylineAfter =
        keylines.getKeylineAfter(unadjustedCenter)
    val progress = getProgress(keylineBefore, keylineAfter, unadjustedCenter)
    val interpolatedKeyline = lerp(keylineBefore, keylineAfter, progress)
    val isOutOfKeylineBounds = keylineBefore == keylineAfter

    return layout { measurable, constraints ->
        // Force the item to use the strategy's itemMainAxisSize along its main axis
        val mainAxisSize = strategy.itemMainAxisSize
        val itemConstraints = if (isVertical) {
            constraints.copy(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = mainAxisSize.roundToInt(),
                maxHeight = mainAxisSize.roundToInt()
            )
        } else {
            constraints.copy(
                minWidth = mainAxisSize.roundToInt(),
                maxWidth = mainAxisSize.roundToInt(),
                minHeight = constraints.minHeight,
                maxHeight = constraints.maxHeight
            )
        }

        val placeable = measurable.measure(itemConstraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }.graphicsLayer {
        // Clip the item
        clip = true
        shape = object : Shape {
            // TODO: Find a way to use the shape of the item set by the client for each item
            val roundedCornerShape = RoundedCornerShape(ShapeDefaults.ExtraLarge.topStart)
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val centerX =
                    if (isVertical) size.height / 2f else strategy.itemMainAxisSize / 2f
                val centerY =
                    if (isVertical) strategy.itemMainAxisSize / 2f else size.height / 2f
                val halfMaskWidth =
                    if (isVertical) size.width / 2f else interpolatedKeyline.size / 2f
                val halfMaskHeight =
                    if (isVertical) interpolatedKeyline.size / 2f else size.height / 2f
                val rect = Rect(
                    left = centerX - halfMaskWidth,
                    top = centerY - halfMaskHeight,
                    right = centerX + halfMaskWidth,
                    bottom = centerY + halfMaskHeight
                )
                val cornerSize =
                    roundedCornerShape.topStart.toPx(
                        Size(rect.width, rect.height),
                        density
                    )
                val cornerRadius = CornerRadius(cornerSize)
                return Outline.Rounded(
                    RoundRect(
                        rect = rect,
                        topLeft = cornerRadius,
                        topRight = cornerRadius,
                        bottomRight = cornerRadius,
                        bottomLeft = cornerRadius
                    )
                )
            }
        }

        // After clipping, the items will have white space between them. Translate the items to
        // pin their edges together
        var translation = interpolatedKeyline.offset - unadjustedCenter
        if (isOutOfKeylineBounds) {
            // If this item is beyond the first or last keyline, continue to offset the item
            // by cutting its unadjustedOffset according to its masked size.
            val outOfBoundsOffset = (unadjustedCenter - interpolatedKeyline.unadjustedOffset) /
                interpolatedKeyline.size
            translation += outOfBoundsOffset
        }
        if (isVertical) {
            translationY = translation
        } else {
            translationX = translation
        }
    }
}

/**
 * Returns a float between 0 and 1 that represents how far [unadjustedOffset] is between
 * [before] and [after].
 *
 * @param before the first keyline whose unadjustedOffset is less than [unadjustedOffset]
 * @param after the first keyline whose unadjustedOffset is greater than [unadjustedOffset]
 * @param unadjustedOffset the unadjustedOffset between [before] and [after]'s unadjustedOffset that
 * a progress value will be returned for
 */
private fun getProgress(before: Keyline, after: Keyline, unadjustedOffset: Float): Float {
    if (before == after) {
        return 1f
    }

    val total = after.unadjustedOffset - before.unadjustedOffset
    return (unadjustedOffset - before.unadjustedOffset) / total
}
