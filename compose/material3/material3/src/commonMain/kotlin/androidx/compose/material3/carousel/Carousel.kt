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
import androidx.collection.IntIntMap
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import kotlin.math.ceil
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
 * @param preferredItemWidth The width the fully visible items would like to be in the main axis.
 * This width is a target and will likely be adjusted by carousel in order to fit a whole number of
 * items within the container
 * @param modifier A modifier instance to be applied to this carousel container
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures
 * @param minSmallItemWidth The minimum allowable width of small items in dp. Depending on the
 * [preferredItemWidth] and the width of the carousel, the small item width will be chosen from a
 * range of [minSmallItemWidth] and [maxSmallItemWidth]
 * @param maxSmallItemWidth The maximum allowable width of small items in dp. Depending on the
 * [preferredItemWidth] and the width of the carousel, the small item width will be chosen from a
 * range of [minSmallItemWidth] and [maxSmallItemWidth]
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped. You can use it to add a padding before the first item or
 * after the last one. Use [itemSpacing] to add spacing between the items.
 * @param content The carousel's content Composable
 *
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun HorizontalMultiBrowseCarousel(
    state: CarouselState,
    preferredItemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior =
        CarouselDefaults.singleAdvanceFlingBehavior(state = state),
    minSmallItemWidth: Dp = CarouselDefaults.MinSmallItemSize,
    maxSmallItemWidth: Dp = CarouselDefaults.MaxSmallItemSize,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable CarouselScope.(itemIndex: Int) -> Unit
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
                    itemCount = state.itemCountState.value.invoke(),
                    itemSpacing = itemSpacingPx,
                    minSmallItemSize = minSmallItemWidth.toPx(),
                    maxSmallItemSize = maxSmallItemWidth.toPx(),
                )
            }
        },
        contentPadding = contentPadding,
        modifier = modifier,
        itemSpacing = itemSpacing,
        flingBehavior = flingBehavior,
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
 * @param itemWidth The width of items in the carousel
 * @param modifier A modifier instance to be applied to this carousel container
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped. You can use it to add a padding before the first item or
 * after the last one. Use [itemSpacing] to add spacing between the items.
 * @param content The carousel's content Composable
 *
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun HorizontalUncontainedCarousel(
    state: CarouselState,
    itemWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior = CarouselDefaults.noSnapFlingBehavior(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable CarouselScope.(itemIndex: Int) -> Unit
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
        modifier = modifier,
        itemSpacing = itemSpacing,
        flingBehavior = flingBehavior,
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
 * @param orientation The layout orientation of the carousel
 * @param keylineList The list of keylines that are fixed positions along the scrolling axis which
 * define the state an item should be in when its center is co-located with the keyline's position.
 * @param contentPadding a padding around the whole content. This will add padding for the
 * @param modifier A modifier instance to be applied to this carousel outer layout
 * content after it has been clipped. You can use it to add a padding before the first item or
 * after the last one. Use [itemSpacing] to add spacing between the items.
 * @param itemSpacing The amount of space used to separate items in the carousel
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures
 * @param content The carousel's content Composable where each call is passed the index, from the
 * total item count, of the item being composed
 * TODO: Add sample link
 */
@ExperimentalMaterial3Api
@Composable
internal fun Carousel(
    state: CarouselState,
    orientation: Orientation,
    keylineList: (availableSpace: Float, itemSpacing: Float) -> KeylineList?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    flingBehavior: TargetedFlingBehavior =
        CarouselDefaults.singleAdvanceFlingBehavior(state = state),
    content: @Composable CarouselScope.(itemIndex: Int) -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val beforeContentPadding = contentPadding.calculateBeforeContentPadding(orientation)
    val afterContentPadding = contentPadding.calculateAfterContentPadding(orientation)
    val pageSize = remember(keylineList) {
        CarouselPageSize(keylineList, beforeContentPadding, afterContentPadding)
    }

    val outOfBoundsPageCount = remember(pageSize.strategy.itemMainAxisSize) {
        calculateOutOfBounds(pageSize.strategy)
    }
    val carouselScope = CarouselScopeImpl

    val snapPositionMap = remember(pageSize.strategy.itemMainAxisSize) {
        calculateSnapPositions(
            pageSize.strategy,
            state.itemCountState.value()
        )
    }
    val snapPosition = remember(snapPositionMap) { KeylineSnapPosition(snapPositionMap) }

    if (orientation == Orientation.Horizontal) {
        HorizontalPager(
            state = state.pagerState,
            // Only pass cross axis padding as main axis padding will be handled by the strategy
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding()
            ),
            pageSize = pageSize,
            pageSpacing = itemSpacing,
            outOfBoundsPageCount = outOfBoundsPageCount,
            snapPosition = snapPosition,
            flingBehavior = flingBehavior,
            modifier = modifier
        ) { page ->
            Box(
                modifier = Modifier.carouselItem(
                    index = page,
                    state = state,
                    strategy = pageSize.strategy,
                    itemPositionMap = snapPositionMap,
                    isRtl = isRtl
                )
            ) {
                carouselScope.content(page)
            }
        }
    } else if (orientation == Orientation.Vertical) {
        VerticalPager(
            state = state.pagerState,
            // Only pass cross axis padding as main axis padding will be handled by the strategy
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
            ),
            pageSize = pageSize,
            pageSpacing = itemSpacing,
            outOfBoundsPageCount = outOfBoundsPageCount,
            snapPosition = snapPosition,
            flingBehavior = flingBehavior,
            modifier = modifier
        ) { page ->
            Box(
                modifier = Modifier.carouselItem(
                    index = page,
                    state = state,
                    strategy = pageSize.strategy,
                    itemPositionMap = snapPositionMap,
                    isRtl = isRtl
                )
            ) {
                carouselScope.content(page)
            }
        }
    }
}

@Composable
private fun PaddingValues.calculateBeforeContentPadding(orientation: Orientation): Float {
    val dpValue = if (orientation == Orientation.Vertical) {
        calculateTopPadding()
    } else {
        calculateStartPadding(LocalLayoutDirection.current)
    }

    return with(LocalDensity.current) { dpValue.toPx() }
}

@Composable
private fun PaddingValues.calculateAfterContentPadding(orientation: Orientation): Float {
    val dpValue = if (orientation == Orientation.Vertical) {
        calculateBottomPadding()
    } else {
        calculateEndPadding(LocalLayoutDirection.current)
    }

    return with(LocalDensity.current) { dpValue.toPx() }
}

internal fun calculateOutOfBounds(strategy: Strategy): Int {
    if (!strategy.isValid()) {
        return PagerDefaults.OutOfBoundsPageCount
    }
    var totalKeylineSpace = 0f
    var totalNonAnchorKeylines = 0
    strategy.defaultKeylines.fastFilter { !it.isAnchor }.fastForEach {
        totalKeylineSpace += it.size
        totalNonAnchorKeylines += 1
    }
    val itemsLoaded = ceil(totalKeylineSpace / strategy.itemMainAxisSize).toInt()
    val itemsToLoad = totalNonAnchorKeylines - itemsLoaded

    // We must also load the next item when scrolling
    return itemsToLoad + 1
}

/**
 * A [PageSize] implementation that maintains a strategy that is kept up-to-date with the
 * latest available space of the container.
 *
 * @param keylineList The list of keylines that are fixed positions along the scrolling axis which
 * define the state an item should be in when its center is co-located with the keyline's position.
 */
private class CarouselPageSize(
    keylineList: (availableSpace: Float, itemSpacing: Float) -> KeylineList?,
    private val beforeContentPadding: Float,
    private val afterContentPadding: Float
) : PageSize {
    val strategy = Strategy(keylineList)
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
        strategy.apply(
            availableSpace.toFloat(),
            pageSpacing.toFloat(),
            beforeContentPadding,
            afterContentPadding
        )
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
 * @param itemPositionMap the position of each index when it is the current item
 * @param isRtl true if the layout direction is right-to-left
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.carouselItem(
    index: Int,
    state: CarouselState,
    strategy: Strategy,
    itemPositionMap: IntIntMap,
    isRtl: Boolean
): Modifier {
    if (!strategy.isValid()) return this
    val isVertical = state.pagerState.layoutInfo.orientation == Orientation.Vertical

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
        val scrollOffset = calculateCurrentScrollOffset(state, strategy, itemPositionMap)
        val maxScrollOffset = calculateMaxScrollOffset(state, strategy)
        // TODO: Reduce the number of times a keyline for the same scroll offset is calculated
        val keylines = strategy.getKeylineListForScrollOffset(scrollOffset, maxScrollOffset)

        // Find center of the item at this index
        val itemSizeWithSpacing = strategy.itemMainAxisSize + strategy.itemSpacing
        val unadjustedCenter =
            (index * itemSizeWithSpacing) + (strategy.itemMainAxisSize / 2f) - scrollOffset

        // Find the keyline before and after this item's center and create an interpolated
        // keyline that the item should use for its clip shape and offset
        val keylineBefore =
            keylines.getKeylineBefore(unadjustedCenter)
        val keylineAfter =
            keylines.getKeylineAfter(unadjustedCenter)
        val progress = getProgress(keylineBefore, keylineAfter, unadjustedCenter)
        val interpolatedKeyline = lerp(keylineBefore, keylineAfter, progress)
        val isOutOfKeylineBounds = keylineBefore == keylineAfter

        // Clip the item
        clip = true
        shape = object : Shape {
            // TODO: Find a way to use the shape of the item set by the client for each item
            // TODO: Allow corner size customization
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
            translationX = if (isRtl) -translation else translation
        }
    }
}

/** Calculates the current scroll offset given item count, sizing, spacing, and snap position. */
@OptIn(ExperimentalMaterial3Api::class)
internal fun calculateCurrentScrollOffset(
    state: CarouselState,
    strategy: Strategy,
    snapPositionMap: IntIntMap
): Float {
    val itemSizeWithSpacing = strategy.itemMainAxisSize + strategy.itemSpacing
    val currentItemScrollOffset =
        (state.pagerState.currentPage * itemSizeWithSpacing) +
            (state.pagerState.currentPageOffsetFraction * itemSizeWithSpacing)
    return currentItemScrollOffset -
        (if (snapPositionMap.size > 0) snapPositionMap[state.pagerState.currentPage] else 0)
}

/** Returns the max scroll offset given the item count, sizing, and spacing. */
@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
internal fun calculateMaxScrollOffset(state: CarouselState, strategy: Strategy): Float {
    val itemCount = state.pagerState.pageCount.toFloat()
    val maxScrollPossible = (strategy.itemMainAxisSize * itemCount) +
        (strategy.itemSpacing * (itemCount - 1))

    return (maxScrollPossible - strategy.availableSpace).coerceAtLeast(0f)
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
 * Contains the default values used by [Carousel].
 */
@ExperimentalMaterial3Api
internal object CarouselDefaults {
    /** The minimum size that a carousel strategy can choose its small items to be. **/
    val MinSmallItemSize = 40.dp

    /** The maximum size that a carousel strategy can choose its small items to be. **/
    val MaxSmallItemSize = 56.dp

    /**
     * A [TargetedFlingBehavior] that limits a fling to one item at a time. [snapAnimationSpec] can
     * be used to control the snap animation.
     *
     * @param state The [CarouselState] that controls which Carousel this TargetedFlingBehavior will
     * be applied to.
     * @param snapAnimationSpec The animation spec used to finally snap to the position.
     * @return An instance of [TargetedFlingBehavior] that performs snapping to the next item.
     * The animation will be governed by the post scroll velocity and the Carousel will use
     * [snapAnimationSpec] to approach the snapped position
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
     * post-scroll, depending on the gesture velocity.
     * If the gesture has a high enough velocity to approach the target item, the Carousel will use
     * [decayAnimationSpec] followed by [snapAnimationSpec] for the final step of the animation.
     * If the gesture doesn't have enough velocity, it will use [snapAnimationSpec] +
     * [snapAnimationSpec] in a similar fashion.
     *
     * @param state The [CarouselState] that controls which Carousel this TargetedFlingBehavior will
     * be applied to.
     * @param decayAnimationSpec The animation spec used to approach the target offset when the
     * the fling velocity is large enough to naturally decay.
     * @param snapAnimationSpec The animation spec used to finally snap to the position.
     * @return An instance of [TargetedFlingBehavior] that performs flinging based on the gesture
     * velocity and then snapping to the closest item post-fling.
     * The animation will be governed by the post scroll velocity and the Carousel will use
     * [snapAnimationSpec] to approach the snapped position
     */
    @Composable
    fun multiBrowseFlingBehavior(
        state: CarouselState,
        decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    ): TargetedFlingBehavior {
        val pagerSnapDistance = object : PagerSnapDistance {
            override fun calculateTargetPage(
                startPage: Int,
                suggestedTargetPage: Int,
                velocity: Float,
                pageSize: Int,
                pageSpacing: Int
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
     * velocity and does not snap to anything post-fling.
     */
    @Composable
    fun noSnapFlingBehavior(): TargetedFlingBehavior {
        val splineDecay = rememberSplineBasedDecay<Float>()
        val decayLayoutInfoProvider = remember {
            object : SnapLayoutInfoProvider {
                override fun calculateApproachOffset(initialVelocity: Float): Float {
                    return splineDecay.calculateTargetValue(0f, initialVelocity)
                }

                override fun calculateSnappingOffset(currentVelocity: Float): Float = 0f
            }
        }

        return rememberSnapFlingBehavior(snapLayoutInfoProvider = decayLayoutInfoProvider)
    }

    internal val AnchorSize = 10.dp
    internal const val MediumLargeItemDiffThreshold = 0.85f
}
