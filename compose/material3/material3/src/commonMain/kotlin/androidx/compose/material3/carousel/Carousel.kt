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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * A enumeration of ways items can be aligned along a carousel's main axis
 */
internal enum class CarouselAlignment {
    /** Start aligned carousels place focal items at the start/top of the container */
    Start,
    /** Center aligned carousels place focal items in the middle of the container */
    Center,
    /** End aligned carousels place focal items at the end/bottom of the container */
    End
}

/**
 * An enumeration of orientations that determine a carousel's main axis
 */
internal enum class Orientation {
    /** Vertical orientation representing Y axis */
    Vertical,
    /** Horizontal orientation representing X axis */
    Horizontal
}

/**
 * A modifier that handles clipping and translating an item as it moves along the scrolling axis
 * of a Carousel.
 *
 * @param index the index of the item in the carousel
 * @param viewportSize the size of the carousel container
 * @param orientation the orientation of the carousel
 * @param itemsCount the total number of items in the carousel
 * @param scrollOffset the amount the carousel has been scrolled in pixels
 * @param strategy the strategy used to mask and translate items in the carousel
 */
internal fun Modifier.carouselItem(
    index: Int,
    itemsCount: Int,
    viewportSize: IntSize,
    orientation: Orientation,
    scrollOffset: Float,
    strategy: Strategy
): Modifier {
    val isVertical = orientation == Orientation.Vertical
    val mainAxisCarouselSize = if (isVertical) viewportSize.height else viewportSize.width
    if (mainAxisCarouselSize == 0) {
        return this
    }
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

/**
 * <a href=https://m3.material.io/components/carousel/overview" class="external" target="_blank">Material Design Carousel</a>
 *
 * A Carousel that scrolls horizontally. Carousels contain a collection of items that changes sizes
 * according to their placement and the chosen strategy.
 *
 * @param state The state object to be used to control the carousel's state.
 * @param modifier A modifier instance to be applied to this carousel outer layout
 * @param content The carousel's content Composable.
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun HorizontalCarousel(
    state: CarouselState,
    modifier: Modifier = Modifier,
    content: @Composable CarouselScope.(item: Int) -> Unit
) = Carousel(
        state = state,
        modifier = modifier,
        orientation = Orientation.Horizontal,
        content = content
)

/**
 * <a href=https://m3.material.io/components/carousel/overview" class="external" target="_blank">Material Design Carousel</a>
 *
 * A Carousel that scrolls vertically. Carousels contain a collection of items that changes sizes
 * according to their placement and the chosen strategy.
 *
 * @param state The state object to be used to control the carousel's state.
 * @param modifier A modifier instance to be applied to this carousel outer layout
 * @param content The carousel's content Composable.
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun VerticalCarousel(
    state: CarouselState,
    modifier: Modifier = Modifier,
    content: @Composable CarouselScope.(item: Int) -> Unit
) = Carousel(
        state = state,
        modifier = modifier,
        orientation = Orientation.Vertical,
        content = content
)

/**
 * <a href=https://m3.material.io/components/carousel/overview" class="external" target="_blank">Material Design Carousel</a>
 *
 * Carousels contain a collection of items that changes sizes according to their placement and the
 * chosen strategy.
 *
 * @param state The state object to be used to control the carousel's state.
 * @param modifier A modifier instance to be applied to this carousel outer layout
 * @param orientation The layout orientation of the carousel
 * @param content The carousel's content Composable.
 * TODO: Add sample link
 */
// TODO: b/321997456 - Remove lint suppression once version checks are added in lint or library
// moves to beta
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterial3Api
@Composable
internal fun Carousel(
    state: CarouselState,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    content: @Composable CarouselScope.(item: Int) -> Unit
) {
    // TODO: Update page size according to strategy
    val pageSize = PageSize.Fill
    // TODO: Update out of bounds page count numbers
    val outOfBoundsPageCount = 1
    val carouselScope = CarouselScopeImpl
    if (orientation == Orientation.Horizontal) {
        HorizontalPager(
            state = state.pagerState,
            pageSize = pageSize,
            outOfBoundsPageCount = outOfBoundsPageCount,
            modifier = modifier
        ) { page ->
            carouselScope.content(page)
        }
    } else if (orientation == Orientation.Vertical) {
        VerticalPager(
            state = state.pagerState,
            pageSize = pageSize,
            outOfBoundsPageCount = outOfBoundsPageCount,
            modifier = modifier
        ) { page ->
            carouselScope.content(page)
        }
    }
}
