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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

/**
 * [Material Design Carousel](https://m3.material.io/components/carousel/overview)
 *
 * A horizontal carousel meant to display many items at once for quick browsing of smaller content
 * like album art or photo thumbnails.
 *
 * Note that this carousel may adjust the size of items in order to ensure a mix of large, medium,
 * and small items fit perfectly into the available space and are arranged in a visually pleasing
 * way. Carousel then lays out items using the large item size and clips (or masks) items depending
 * on their scroll offset to create items which smoothly expand and collapse between the large,
 * medium, and small sizes.
 *
 * For more information, see
 * [design guidelines](https://m3.material.io/components/carousel/overview)
 *
 * Example of a multi-browse carousel:
 *
 * @sample androidx.compose.material3.samples.HorizontalMultiBrowseCarouselSample
 * @sample androidx.compose.material3.samples.CarouselWithShowAllButtonSample
 * @param state The state object to be used to control the carousel's state
 * @param preferredItemWidth The width that large, fully visible items would like to be in the
 *   horizontal axis. This width is a target and will likely be adjusted by carousel in order to fit
 *   a whole number of items within the container. Carousel adjusts small items first (between the
 *   [minSmallItemWidth] and [maxSmallItemWidth]) then medium items when present, and finally large
 *   items if necessary.
 * @param modifier A modifier instance to be applied to this carousel container
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed.
 * @param minSmallItemWidth The minimum allowable width of small items in dp. Depending on the
 *   [preferredItemWidth] and the width of the carousel, the small item width will be chosen from a
 *   range of [minSmallItemWidth] and [maxSmallItemWidth]
 * @param maxSmallItemWidth The maximum allowable width of small items in dp. Depending on the
 *   [preferredItemWidth] and the width of the carousel, the small item width will be chosen from a
 *   range of [minSmallItemWidth] and [maxSmallItemWidth]
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped. You can use it to add a padding before the first item or after the
 *   last one. Use [itemSpacing] to add spacing between the items.
 * @param content The carousel's content Composable
 */
@ExperimentalMaterial3Api
@Composable
fun HorizontalMultiBrowseCarousel(
    state: CarouselState,
    preferredItemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior =
        CarouselDefaults.singleAdvanceFlingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    minSmallItemWidth: Dp = CarouselDefaults.MinSmallItemSize,
    maxSmallItemWidth: Dp = CarouselDefaults.MaxSmallItemSize,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit,
) {
    val density = LocalDensity.current
    Carousel(
        state = state,
        orientation = Orientation.Horizontal,
        keylineList = { availableSpace, itemSpacingPx ->
            with(density) {
                multiBrowseKeylineList(
                    density = this,
                    carouselMainAxisSize = availableSpace,
                    preferredItemSize = preferredItemWidth.toPx(),
                    itemCount = state.pagerState.pageCountState.value.invoke(),
                    itemSpacing = itemSpacingPx,
                    minSmallItemSize = minSmallItemWidth.toPx(),
                    maxSmallItemSize = maxSmallItemWidth.toPx(),
                )
            }
        },
        contentPadding = contentPadding,
        // 2 is the max number of medium and small items that can be present in a multi-browse
        // carousel and should be the upper bounds max non focal visible items.
        maxNonFocalVisibleItemCount = 2,
        modifier = modifier,
        itemSpacing = itemSpacing,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

@ExperimentalMaterial3Api
@Deprecated(message = "Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun HorizontalMultiBrowseCarousel(
    state: CarouselState,
    preferredItemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior =
        CarouselDefaults.singleAdvanceFlingBehavior(state = state),
    minSmallItemWidth: Dp = CarouselDefaults.MinSmallItemSize,
    maxSmallItemWidth: Dp = CarouselDefaults.MaxSmallItemSize,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit,
) =
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = preferredItemWidth,
        modifier = modifier,
        itemSpacing = itemSpacing,
        flingBehavior = flingBehavior,
        userScrollEnabled = true,
        minSmallItemWidth = minSmallItemWidth,
        maxSmallItemWidth = maxSmallItemWidth,
        contentPadding = contentPadding,
        content = content,
    )

/**
 * [Material Design Carousel](https://m3.material.io/components/carousel/overview)
 *
 * A horizontal carousel that displays its items with the given size except for one item at the end
 * that is cut off.
 *
 * Note that the item size will be bound by the size of the carousel. Otherwise, this carousel lays
 * out as many items as it can in the given size, and changes the size of the last cut off item such
 * that there is a range of motion when items scroll off the edge.
 *
 * For more information, see
 * [design guidelines](https://m3.material.io/components/carousel/overview)
 *
 * Example of an uncontained carousel:
 *
 * @sample androidx.compose.material3.samples.HorizontalUncontainedCarouselSample
 * @param state The state object to be used to control the carousel's state
 * @param itemWidth The width of items in the carousel
 * @param modifier A modifier instance to be applied to this carousel container
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed.
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped. You can use it to add a padding before the first item or after the
 *   last one. Use [itemSpacing] to add spacing between the items.
 * @param content The carousel's content Composable
 */
@ExperimentalMaterial3Api
@Composable
fun HorizontalUncontainedCarousel(
    state: CarouselState,
    itemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior = CarouselDefaults.noSnapFlingBehavior(),
    userScrollEnabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit,
) {
    val density = LocalDensity.current
    Carousel(
        state = state,
        orientation = Orientation.Horizontal,
        keylineList = { availableSpace, itemSpacingPx ->
            with(density) {
                uncontainedKeylineList(
                    density = this,
                    carouselMainAxisSize = availableSpace,
                    itemSize = itemWidth.toPx(),
                    itemSpacing = itemSpacingPx,
                )
            }
        },
        contentPadding = contentPadding,
        // Since uncontained carousels only have one item that masks as it moves in/out of view,
        // there is no need to increase the max non focal count.
        maxNonFocalVisibleItemCount = 0,
        modifier = modifier,
        itemSpacing = itemSpacing,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

@ExperimentalMaterial3Api
@Deprecated(message = "Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun HorizontalUncontainedCarousel(
    state: CarouselState,
    itemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior = CarouselDefaults.noSnapFlingBehavior(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit,
) =
    HorizontalUncontainedCarousel(
        state = state,
        itemWidth = itemWidth,
        modifier = modifier,
        itemSpacing = itemSpacing,
        flingBehavior = flingBehavior,
        userScrollEnabled = true,
        contentPadding = contentPadding,
        content = content,
    )

/**
 * [Material Design Carousel](https://m3.material.io/components/carousel/overview)
 *
 * A horizontal carousel that centers one large item between two small items.
 *
 * @sample androidx.compose.material3.samples.HorizontalCenteredHeroCarouselSample
 * @param state The state object to be used to control the carousel's state
 * @param modifier A modifier instance to be applied to this carousel container
 * @param maxItemWidth The max width a large item is allowed to be in dp. The default value of
 *   [Dp.Unspecified] allows one large item to grow to fill the entire viewport minus space for two
 *   surrounding small items. Values other than unspecified will add additional large items as space
 *   allows.
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed.
 * @param minSmallItemWidth The minimum allowable width of small items in dp
 * @param maxSmallItemWidth The maximum allowable width of small items in dp
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped. You can use it to add a padding before the first item or after the
 *   last one. Use [itemSpacing] to add spacing between the items.
 * @param content The carousel's content Composable
 */
@ExperimentalMaterial3Api
@Composable
fun HorizontalCenteredHeroCarousel(
    state: CarouselState,
    modifier: Modifier = Modifier,
    maxItemWidth: Dp = Dp.Unspecified,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior =
        CarouselDefaults.singleAdvanceFlingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    minSmallItemWidth: Dp = CarouselDefaults.MinSmallItemSize,
    maxSmallItemWidth: Dp = CarouselDefaults.MaxSmallItemSize,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit,
) {
    val density = LocalDensity.current
    Carousel(
        state = state,
        orientation = Orientation.Horizontal,
        keylineList = { availableSpace, itemSpacingPx ->
            with(density) {
                heroKeylineList(
                    density = this,
                    carouselMainAxisSize = availableSpace,
                    maxItemSize = if (maxItemWidth.isSpecified) maxItemWidth.toPx() else null,
                    itemSpacing = itemSpacingPx,
                    itemCount = state.pagerState.pageCountState.value.invoke(),
                    isCentered = true,
                    minSmallItemSize = minSmallItemWidth.toPx(),
                    maxSmallItemSize = maxSmallItemWidth.toPx(),
                )
            }
        },
        contentPadding = contentPadding,
        // 2 is the max number of medium and small items that can be present in a centered hero
        // carousel and should be the upper bounds max non focal visible items.
        maxNonFocalVisibleItemCount = 2,
        modifier = modifier,
        itemSpacing = itemSpacing,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

/**
 * [Material Design Carousel](https://m3.material.io/components/carousel/overview)
 *
 * Carousels contain a collection of items that changes sizes according to their placement and the
 * chosen strategy.
 *
 * @param state The state object to be used to control the carousel's state.
 * @param orientation The layout orientation of the carousel
 * @param keylineList The list of keylines that are fixed positions along the scrolling axis which
 *   define the state an item should be in when its center is co-located with the keyline's
 *   position.
 * @param contentPadding a padding around the whole content. This will add padding for the
 * @param maxNonFocalVisibleItemCount the maximum number of items that are visible but not fully
 *   unmasked (focal) at one time. This number helps determine how many items should be composed to
 *   fill the entire viewport.
 * @param modifier A modifier instance to be applied to this carousel outer layout content after it
 *   has been clipped. You can use it to add a padding before the first item or after the last one.
 *   Use [itemSpacing] to add spacing between the items.
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed.
 * @param content The carousel's content Composable where each call is passed the index, from the
 *   total item count, of the item being composed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Carousel(
    state: CarouselState,
    orientation: Orientation,
    keylineList: (availableSpace: Float, itemSpacing: Float) -> KeylineList,
    contentPadding: PaddingValues,
    maxNonFocalVisibleItemCount: Int,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior =
        CarouselDefaults.singleAdvanceFlingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit,
) {
    val beforeContentPadding = contentPadding.calculateBeforeContentPadding(orientation)
    val afterContentPadding = contentPadding.calculateAfterContentPadding(orientation)
    val pageSize =
        remember(keylineList) {
            CarouselPageSize(keylineList, beforeContentPadding, afterContentPadding)
        }

    val snapPosition = KeylineSnapPosition(pageSize)

    if (orientation == Orientation.Horizontal) {
        HorizontalPager(
            state = state.pagerState,
            // Only pass cross axis padding as main axis padding will be handled by the strategy
            contentPadding =
                PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
            pageSize = pageSize,
            pageSpacing = itemSpacing,
            beyondViewportPageCount = maxNonFocalVisibleItemCount,
            snapPosition = snapPosition,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            modifier = modifier.semantics { role = Role.Carousel },
        ) { page ->
            val carouselItemInfo = remember { CarouselItemDrawInfoImpl() }
            val scope = remember { CarouselItemScopeImpl(itemInfo = carouselItemInfo) }
            val clipShape = remember {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        return Outline.Rectangle(carouselItemInfo.maskRect)
                    }
                }
            }

            Box(
                modifier =
                    Modifier.carouselItem(
                        index = page,
                        state = state,
                        strategy = { pageSize.strategy },
                        carouselItemDrawInfo = carouselItemInfo,
                        clipShape = clipShape,
                    )
            ) {
                scope.content(page)
            }
        }
    } else if (orientation == Orientation.Vertical) {
        VerticalPager(
            state = state.pagerState,
            // Only pass cross axis padding as main axis padding will be handled by the strategy
            contentPadding =
                PaddingValues(
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            pageSize = pageSize,
            pageSpacing = itemSpacing,
            beyondViewportPageCount = maxNonFocalVisibleItemCount,
            snapPosition = snapPosition,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            modifier = modifier.semantics { role = Role.Carousel },
        ) { page ->
            val carouselItemInfo = remember { CarouselItemDrawInfoImpl() }
            val scope = remember { CarouselItemScopeImpl(itemInfo = carouselItemInfo) }
            val clipShape = remember {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        return Outline.Rectangle(carouselItemInfo.maskRect)
                    }
                }
            }

            Box(
                modifier =
                    Modifier.carouselItem(
                        index = page,
                        state = state,
                        strategy = { pageSize.strategy },
                        carouselItemDrawInfo = carouselItemInfo,
                        clipShape = clipShape,
                    )
            ) {
                scope.content(page)
            }
        }
    }
}

@Composable
private fun PaddingValues.calculateBeforeContentPadding(orientation: Orientation): Float {
    val dpValue =
        if (orientation == Orientation.Vertical) {
            calculateTopPadding()
        } else {
            calculateStartPadding(LocalLayoutDirection.current)
        }

    return with(LocalDensity.current) { dpValue.toPx() }
}

@Composable
private fun PaddingValues.calculateAfterContentPadding(orientation: Orientation): Float {
    val dpValue =
        if (orientation == Orientation.Vertical) {
            calculateBottomPadding()
        } else {
            calculateEndPadding(LocalLayoutDirection.current)
        }

    return with(LocalDensity.current) { dpValue.toPx() }
}

/**
 * A [PageSize] implementation that maintains a strategy that is kept up-to-date with the latest
 * available space of the container.
 *
 * @param keylineList The list of keylines that are fixed positions along the scrolling axis which
 *   define the state an item should be in when its center is co-located with the keyline's
 *   position.
 */
internal class CarouselPageSize(
    private val keylineList: (availableSpace: Float, itemSpacing: Float) -> KeylineList,
    private val beforeContentPadding: Float,
    private val afterContentPadding: Float,
) : PageSize {

    private var strategyState by mutableStateOf(Strategy.Empty)
    val strategy: Strategy
        get() = strategyState

    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
        val keylines = keylineList.invoke(availableSpace.toFloat(), pageSpacing.toFloat())
        strategyState =
            Strategy(
                keylines,
                availableSpace.toFloat(),
                pageSpacing.toFloat(),
                beforeContentPadding,
                afterContentPadding,
            )

        // If a valid strategy is available, use the strategy's item size. Otherwise, default to
        // a full size item as Pager does by default.
        return if (strategy.isValid) {
            strategy.itemMainAxisSize.roundToInt()
        } else {
            availableSpace
        }
    }
}

/** This class defines ways items can be aligned along a carousel's main axis. */
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
 * A modifier that handles clipping and translating an item as it moves along the scrolling axis of
 * a Carousel.
 *
 * @param index the index of the item in the carousel
 * @param state the carousel state
 * @param strategy the strategy used to mask and translate items in the carousel
 * @param carouselItemDrawInfo the item info that should be updated with the changes in this
 *   modifier
 * @param clipShape the shape the item will clip itself to. This should be a rectangle with a bounds
 *   that match the carousel item info's mask rect. Corner radii and other shape customizations can
 *   be done by the client using [CarouselItemScope.maskClip] and [CarouselItemScope.maskBorder].
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.carouselItem(
    index: Int,
    state: CarouselState,
    strategy: () -> Strategy,
    carouselItemDrawInfo: CarouselItemDrawInfoImpl,
    clipShape: Shape,
): Modifier {
    return layout { measurable, constraints ->
        val strategyResult = strategy.invoke()
        if (!strategyResult.isValid) {
            // If there is no strategy, avoid displaying content
            return@layout layout(0, 0) {}
        }

        val isVertical = state.pagerState.layoutInfo.orientation == Orientation.Vertical
        val isRtl = layoutDirection == LayoutDirection.Rtl

        // Force the item to use the strategy's itemMainAxisSize along its main axis
        val mainAxisSize = strategyResult.itemMainAxisSize
        val itemConstraints =
            if (isVertical) {
                constraints.copy(
                    minWidth = constraints.minWidth,
                    maxWidth = constraints.maxWidth,
                    minHeight = mainAxisSize.roundToInt(),
                    maxHeight = mainAxisSize.roundToInt(),
                )
            } else {
                constraints.copy(
                    minWidth = mainAxisSize.roundToInt(),
                    maxWidth = mainAxisSize.roundToInt(),
                    minHeight = constraints.minHeight,
                    maxHeight = constraints.maxHeight,
                )
            }

        val placeable = measurable.measure(itemConstraints)
        // We always want to make the current item be the one at the front
        val itemZIndex =
            if (index == state.pagerState.currentPage) {
                1f
            } else {
                if (index == 0) {
                    0f
                } else {
                    // Other items should go in reverse placement order, that is, the ones with the
                    // higher indices should behind the ones with lower indices.
                    1f / index.toFloat()
                }
            }

        layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(
                0,
                0,
                zIndex = itemZIndex,
                layerBlock = {
                    val scrollOffset = calculateCurrentScrollOffset(state, strategyResult)
                    val maxScrollOffset = calculateMaxScrollOffset(state, strategyResult)
                    // TODO: Reduce the number of times keylins are calculated
                    val keylines =
                        strategyResult.getKeylineListForScrollOffset(scrollOffset, maxScrollOffset)
                    val roundedKeylines =
                        strategyResult.getKeylineListForScrollOffset(
                            scrollOffset = scrollOffset,
                            maxScrollOffset = maxScrollOffset,
                            roundToNearestStep = true,
                        )

                    // Find center of the item at this index
                    val itemSizeWithSpacing =
                        strategyResult.itemMainAxisSize + strategyResult.itemSpacing
                    val unadjustedCenter =
                        (index * itemSizeWithSpacing) + (strategyResult.itemMainAxisSize / 2f) -
                            scrollOffset

                    // Find the keyline before and after this item's center and create an
                    // interpolated
                    // keyline that the item should use for its clip shape and offset
                    val keylineBefore = keylines.getKeylineBefore(unadjustedCenter)
                    val keylineAfter = keylines.getKeylineAfter(unadjustedCenter)
                    val progress = getProgress(keylineBefore, keylineAfter, unadjustedCenter)
                    val interpolatedKeyline = lerp(keylineBefore, keylineAfter, progress)
                    val isOutOfKeylineBounds = keylineBefore == keylineAfter

                    val centerX =
                        if (isVertical) size.height / 2f else strategyResult.itemMainAxisSize / 2f
                    val centerY =
                        if (isVertical) strategyResult.itemMainAxisSize / 2f else size.height / 2f
                    val halfMaskWidth =
                        if (isVertical) size.width / 2f else interpolatedKeyline.size / 2f
                    val halfMaskHeight =
                        if (isVertical) interpolatedKeyline.size / 2f else size.height / 2f
                    val maskRect =
                        Rect(
                            left = centerX - halfMaskWidth,
                            top = centerY - halfMaskHeight,
                            right = centerX + halfMaskWidth,
                            bottom = centerY + halfMaskHeight,
                        )

                    // Update carousel item info
                    carouselItemDrawInfo.sizeState = interpolatedKeyline.size
                    carouselItemDrawInfo.minSizeState = roundedKeylines.minBy { it.size }.size
                    carouselItemDrawInfo.maxSizeState = roundedKeylines.firstFocal.size
                    carouselItemDrawInfo.maskRectState = maskRect

                    // Clip the item
                    clip = maskRect != Rect(0f, 0f, size.width, size.height)
                    shape = clipShape

                    // After clipping, the items will have white space between them. Translate the
                    // items to pin their edges together
                    var translation = interpolatedKeyline.offset - unadjustedCenter
                    if (isOutOfKeylineBounds) {
                        // If this item is beyond the first or last keyline, continue to offset the
                        // item by cutting its unadjustedOffset according to its masked size.
                        val outOfBoundsOffset =
                            (unadjustedCenter - interpolatedKeyline.unadjustedOffset) /
                                interpolatedKeyline.size
                        translation += outOfBoundsOffset
                    }
                    if (isVertical) {
                        translationY = translation
                    } else {
                        translationX = if (isRtl) -translation else translation
                    }
                },
            )
        }
    }
}

/** A modifier to draw keylines and other features over a Carousel to help with debugging. */
@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.drawDebugLines(
    state: CarouselState,
    pageSize: CarouselPageSize,
    strokeColor: Color = Color.Magenta,
    strokeWidth: Dp = 4.dp,
): Modifier = drawWithContent {
    drawContent()
    val strategyResult = pageSize.strategy
    val scrollOffset = calculateCurrentScrollOffset(state, strategyResult)
    val maxScrollOffset = calculateMaxScrollOffset(state, strategyResult)
    val keylines = strategyResult.getKeylineListForScrollOffset(scrollOffset, maxScrollOffset)
    val strokeWidthPx = strokeWidth.toPx()
    for (i in keylines.indices) {
        drawLine(
            color = strokeColor,
            start = Offset(x = keylines[i].offset, y = 0f),
            end = Offset(x = keylines[i].offset, y = 100f),
            strokeWidth = strokeWidthPx,
        )
    }
}

/** Calculates the current scroll offset given item count, sizing, spacing, and snap position. */
@OptIn(ExperimentalMaterial3Api::class)
internal fun calculateCurrentScrollOffset(state: CarouselState, strategy: Strategy): Float {
    val itemSizeWithSpacing = strategy.itemMainAxisSize + strategy.itemSpacing
    val currentItemScrollOffset =
        (state.pagerState.currentPage * itemSizeWithSpacing) +
            (state.pagerState.currentPageOffsetFraction * itemSizeWithSpacing)
    return currentItemScrollOffset -
        getSnapPositionOffset(strategy, state.pagerState.currentPage, state.pagerState.pageCount)
}

/** Returns the max scroll offset given the item count, sizing, and spacing. */
@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
internal fun calculateMaxScrollOffset(state: CarouselState, strategy: Strategy): Float {
    val itemCount = state.pagerState.pageCount.toFloat()
    val maxScrollPossible =
        (strategy.itemMainAxisSize * itemCount) + (strategy.itemSpacing * (itemCount - 1))

    return (maxScrollPossible - strategy.availableSpace).coerceAtLeast(0f)
}

/**
 * Returns a float between 0 and 1 that represents how far [unadjustedOffset] is between [before]
 * and [after].
 *
 * @param before the first keyline whose unadjustedOffset is less than [unadjustedOffset]
 * @param after the first keyline whose unadjustedOffset is greater than [unadjustedOffset]
 * @param unadjustedOffset the unadjustedOffset between [before] and [after]'s unadjustedOffset that
 *   a progress value will be returned for
 */
private fun getProgress(before: Keyline, after: Keyline, unadjustedOffset: Float): Float {
    if (before == after) {
        return 1f
    }

    val total = after.unadjustedOffset - before.unadjustedOffset
    return (unadjustedOffset - before.unadjustedOffset) / total
}

/** Contains the default values used by [Carousel]. */
@ExperimentalMaterial3Api
object CarouselDefaults {

    /**
     * A [TargetedFlingBehavior] that limits a fling to one item at a time. [snapAnimationSpec] can
     * be used to control the snap animation.
     *
     * @param state The [CarouselState] that controls which Carousel this TargetedFlingBehavior will
     *   be applied to.
     * @param snapAnimationSpec The animation spec used to finally snap to the position.
     * @return An instance of [TargetedFlingBehavior] that performs snapping to the next item. The
     *   animation will be governed by the post scroll velocity and the Carousel will use
     *   [snapAnimationSpec] to approach the snapped position
     */
    @Composable
    fun singleAdvanceFlingBehavior(
        state: CarouselState,
        snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    ): TargetedFlingBehavior {
        return PagerDefaults.flingBehavior(
            state = state.pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
            snapAnimationSpec = snapAnimationSpec,
        )
    }

    /**
     * A [TargetedFlingBehavior] that flings and snaps according to the gesture velocity.
     * [snapAnimationSpec] and [decayAnimationSpec] can be used to control the animation specs.
     *
     * The Carousel may use [decayAnimationSpec] or [snapAnimationSpec] to approach the target item
     * post-scroll, depending on the gesture velocity. If the gesture has a high enough velocity to
     * approach the target item, the Carousel will use [decayAnimationSpec] followed by
     * [snapAnimationSpec] for the final step of the animation. If the gesture doesn't have enough
     * velocity, it will use [snapAnimationSpec] + [snapAnimationSpec] in a similar fashion.
     *
     * @param state The [CarouselState] that controls which Carousel this TargetedFlingBehavior will
     *   be applied to.
     * @param decayAnimationSpec The animation spec used to approach the target offset when the the
     *   fling velocity is large enough to naturally decay.
     * @param snapAnimationSpec The animation spec used to finally snap to the position.
     * @return An instance of [TargetedFlingBehavior] that performs flinging based on the gesture
     *   velocity and then snapping to the closest item post-fling. The animation will be governed
     *   by the post scroll velocity and the Carousel will use [snapAnimationSpec] to approach the
     *   snapped position
     */
    @Composable
    fun multiBrowseFlingBehavior(
        state: CarouselState,
        decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    ): TargetedFlingBehavior {
        val pagerSnapDistance =
            object : PagerSnapDistance {
                override fun calculateTargetPage(
                    startPage: Int,
                    suggestedTargetPage: Int,
                    velocity: Float,
                    pageSize: Int,
                    pageSpacing: Int,
                ): Int {
                    return suggestedTargetPage
                }
            }
        return PagerDefaults.flingBehavior(
            state = state.pagerState,
            pagerSnapDistance = pagerSnapDistance,
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = snapAnimationSpec,
        )
    }

    /**
     * A [TargetedFlingBehavior] that flings according to the gesture velocity and does not snap
     * post-fling.
     *
     * @return An instance of [TargetedFlingBehavior] that performs flinging based on the gesture
     *   velocity and does not snap to anything post-fling.
     */
    @Composable
    fun noSnapFlingBehavior(): TargetedFlingBehavior {
        val decayLayoutInfoProvider = remember {
            object : SnapLayoutInfoProvider {
                override fun calculateSnapOffset(velocity: Float): Float = 0f
            }
        }

        return rememberSnapFlingBehavior(snapLayoutInfoProvider = decayLayoutInfoProvider)
    }

    /** The minimum size that a carousel strategy can choose its small items to be. * */
    val MinSmallItemSize = 40.dp

    /** The maximum size that a carousel strategy can choose its small items to be. * */
    val MaxSmallItemSize = 56.dp

    internal val AnchorSize = 10.dp
    internal const val MediumLargeItemDiffThreshold = 0.85f
}
