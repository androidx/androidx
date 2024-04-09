/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.roundToInt

/**
 * Creates a [ScalingLazyListState] that is remembered across compositions.
 *
 * @param initialCenterItemIndex the initial value for [ScalingLazyListState.centerItemIndex],
 * defaults to 1. This will place the 2nd list item (index == 1) in the center of the viewport and
 * the first item (index == 0) before it.
 *
 * @param initialCenterItemScrollOffset the initial value for
 * [ScalingLazyListState.centerItemScrollOffset] in pixels
 */
@Composable
public fun rememberScalingLazyListState(
    initialCenterItemIndex: Int = 1,
    initialCenterItemScrollOffset: Int = 0
): ScalingLazyListState {
    return rememberSaveable(saver = ScalingLazyListState.Saver) {
        ScalingLazyListState(
            initialCenterItemIndex,
            initialCenterItemScrollOffset
        )
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberScalingLazyListState].
 *
 * @param initialCenterItemIndex the initial value for [ScalingLazyListState.centerItemIndex],
 * defaults to 1. This will place the 2nd list item (index == 1) in the center of the viewport and
 * the first item (index == 0) before it.
 *
 * If the developer wants custom control over position and spacing they can switch off autoCentering
 * and provide contentPadding.
 *
 * @param initialCenterItemScrollOffset the initial value for
 * [ScalingLazyListState.centerItemScrollOffset]
 *
 * Note that it is not always possible for the values provided by [initialCenterItemIndex] and
 * [initialCenterItemScrollOffset] to be honored, e.g. If [initialCenterItemIndex] is set to a value
 * larger than the number of items initially in the list, or to an index that can not be placed in
 * the middle of the screen due to the contentPadding or autoCentering properties provided to the
 * [ScalingLazyColumn]. After the [ScalingLazyColumn] is initially drawn the actual values for the
 * [centerItemIndex] and [centerItemScrollOffset] can be read from the state.
 */
@Stable
class ScalingLazyListState constructor(
    private var initialCenterItemIndex: Int = 1,
    private var initialCenterItemScrollOffset: Int = 0
) : ScrollableState {

    internal var lazyListState: LazyListState = LazyListState(0, 0)

    internal class Configuration(
        val extraPaddingPx: Int,
        val beforeContentPaddingPx: Int,
        val afterContentPaddingPx: Int,
        val scalingParams: ScalingParams,
        val gapBetweenItemsPx: Int,
        val viewportHeightPx: Int,
        val reverseLayout: Boolean,
        val anchorType: ScalingLazyListAnchorType,
        val autoCentering: AutoCenteringParams?,
        val localInspectionMode: Boolean
    )
    internal val initialized = mutableStateOf(false)
    internal val config = mutableStateOf<Configuration?>(null)

    // The following three are used together when there is a post-initialization incomplete scroll
    // to finish next time the ScalingLazyColumn is visible
    private val incompleteScrollItem = mutableStateOf<Int?>(null)
    private val incompleteScrollOffset = mutableStateOf<Int?>(null)
    private val incompleteScrollAnimated = mutableStateOf(false)
    /**
     * The index of the item positioned closest to the viewport center
     */
    public val centerItemIndex: Int by derivedStateOf {
        (layoutInfo as? DefaultScalingLazyListLayoutInfo)?.let {
            if (it.initialized) it.centerItemIndex else null
        } ?: initialCenterItemIndex
    }

    internal val topAutoCenteringItemSizePx: Int by derivedStateOf {
        val config = config.value
        if (config == null || config.autoCentering == null) {
            0
        } else {
            (layoutInfo.beforeAutoCenteringPadding - config.gapBetweenItemsPx).coerceAtLeast(0)
        }
    }

    internal val bottomAutoCenteringItemSizePx: Int by derivedStateOf {
        val config = config.value
        if (
            config == null ||
            config.autoCentering == null ||
            layoutInfo.internalVisibleItemInfo().isEmpty()
        ) {
            0
        } else {
            (layoutInfo.afterAutoCenteringPadding - config.gapBetweenItemsPx).coerceAtLeast(0)
        }
    }

    /**
     * The offset of the item closest to the viewport center. Depending on the
     * [ScalingLazyListAnchorType] of the [ScalingLazyColumn] the offset will be relative to either
     * the items Edge or Center.
     *
     * A positive value indicates that the center item's anchor point is above the viewport
     * center-line, a negative value indicates that the center item anchor point is below the
     * viewport center-line.
     */
    public val centerItemScrollOffset: Int
        get() =
            (layoutInfo as? DefaultScalingLazyListLayoutInfo)?.let {
                if (it.initialized) it.centerItemScrollOffset else null
            } ?: initialCenterItemScrollOffset

    /**
     * The object of [ScalingLazyListLayoutInfo] calculated during the last layout pass. For
     * example, you can use it to calculate what items are currently visible.
     */
    public val layoutInfo: ScalingLazyListLayoutInfo by derivedStateOf(
        referentialEqualityPolicy()
    ) {
        val config = config.value
        if (config == null) {
            EmptyScalingLazyListLayoutInfo
        } else {
            // read list state once here to for performance reasons and to ensure consistency
            val lazyListLayoutInfo = lazyListState.layoutInfo
            val initialized = initialized.value

            val visibleItemsInfo = mutableListOf<ScalingLazyListItemInfo>()
            var newCenterItemIndex = 0
            var newCenterItemScrollOffset = 0
            val visible = initialized || config.localInspectionMode

            // The verticalAdjustment is used to allow for the extraPadding that the
            // ScalingLazyColumn employs to ensure that there are sufficient list items composed
            // by the underlying LazyList even when there is extreme scaling being applied that
            // could result in additional list items be eligible to be drawn.
            // It is important to adjust for this extra space when working out the viewport
            // center-line based coordinate system of the ScalingLazyList.
            val verticalAdjustment =
                lazyListLayoutInfo.viewportStartOffset + config.extraPaddingPx

            // Find the item in the middle of the viewport
            val centralItemArrayIndex =
                findItemNearestCenter(lazyListLayoutInfo, config, verticalAdjustment)
            if (centralItemArrayIndex != null) {
                val originalVisibleItemsInfo = lazyListLayoutInfo.visibleItemsInfo
                val centralItem = originalVisibleItemsInfo[centralItemArrayIndex]

                // Place the center item
                val centerItemInfo: ScalingLazyListItemInfo = calculateItemInfo(
                    centralItem.offset,
                    centralItem,
                    verticalAdjustment,
                    config.viewportHeightPx,
                    config.viewportCenterLinePx(),
                    config.scalingParams,
                    config.beforeContentPaddingPx,
                    config.anchorType,
                    config.autoCentering,
                    visible
                )
                visibleItemsInfo.add(
                    centerItemInfo
                )

                newCenterItemIndex = centerItemInfo.index
                newCenterItemScrollOffset = -centerItemInfo.offset

                // Find the adjusted position of the central item in the coordinate system of the
                // underlying LazyColumn by adjusting for any scaling
                val centralItemAdjustedUnderlyingOffset =
                    centralItem.offset + ((centerItemInfo.startOffset(config.anchorType) -
                        centerItemInfo.unadjustedStartOffset(config.anchorType))).roundToInt()

                // Go Up
                // nextItemBottomNoPadding uses the coordinate system of the underlying LazyList. It
                // keeps track of the top of the next potential list item that is a candidate to be
                // drawn in the viewport as we walk up the list items from the center. Going up
                // involved making offset smaller/negative as the coordinate system of the LazyList
                // starts at the top of the viewport. Note that the start of the lazy list
                // coordinates starts at '- start content padding in pixels' and goes beyond the
                // last visible list items to include the end content padding in pixels.

                // centralItem.offset is a startOffset in the coordinate system of the
                // underlying lazy list.
                var nextItemBottomNoPadding =
                    centralItemAdjustedUnderlyingOffset - config.gapBetweenItemsPx

                (centralItemArrayIndex - 1 downTo 0).forEach { ix ->
                    if (nextItemBottomNoPadding >= verticalAdjustment) {
                        val currentItem =
                            lazyListLayoutInfo.visibleItemsInfo[ix]
                        if (
                            !discardAutoCenteringListItem(config, lazyListLayoutInfo, currentItem)
                        ) {
                            val itemInfo = calculateItemInfo(
                                nextItemBottomNoPadding - currentItem.size,
                                currentItem,
                                verticalAdjustment,
                                config.viewportHeightPx,
                                config.viewportCenterLinePx(),
                                config.scalingParams,
                                config.beforeContentPaddingPx,
                                config.anchorType,
                                config.autoCentering,
                                visible
                            )
                            visibleItemsInfo.add(0, itemInfo)
                            nextItemBottomNoPadding =
                                nextItemBottomNoPadding - itemInfo.size - config.gapBetweenItemsPx
                        }
                    } else {
                        return@forEach
                    }
                }

                // Go Down
                // nextItemTopNoPadding uses the coordinate system of the underlying LazyList. It
                // keeps track of the top of the next potential list item that is a candidate to be
                // drawn in the viewport as we walk down the list items from the center.
                var nextItemTopNoPadding =
                    centralItemAdjustedUnderlyingOffset + centerItemInfo.size +
                        config.gapBetweenItemsPx

                (((centralItemArrayIndex + 1) until
                    originalVisibleItemsInfo.size)).forEach { ix ->
                    if ((nextItemTopNoPadding - config.viewportHeightPx) <= verticalAdjustment) {
                        val currentItem =
                            lazyListLayoutInfo.visibleItemsInfo[ix]
                        if (
                            !discardAutoCenteringListItem(config, lazyListLayoutInfo, currentItem)
                        ) {
                            val itemInfo = calculateItemInfo(
                                nextItemTopNoPadding,
                                currentItem,
                                verticalAdjustment,
                                config.viewportHeightPx,
                                config.viewportCenterLinePx(),
                                config.scalingParams,
                                config.beforeContentPaddingPx,
                                config.anchorType,
                                config.autoCentering,
                                visible
                            )

                            visibleItemsInfo.add(itemInfo)
                            nextItemTopNoPadding += itemInfo.size + config.gapBetweenItemsPx
                        }
                    } else {
                        return@forEach
                    }
                }
            }
            val totalItemsCount =
                if (config.autoCentering != null) {
                    (lazyListLayoutInfo.totalItemsCount - 2).coerceAtLeast(0)
                } else {
                    lazyListLayoutInfo.totalItemsCount
                }

            // Decide if we are ready for the 2nd stage of initialization
            // 1. We are not yet initialized and

            val readyForInitialScroll =
                if (!initialized) {
                    // 1. autoCentering is off or
                    // 2. The list has no items or
                    // 3. the before content autoCentering Spacer has been sized.
                    // NOTE: It is possible, if the first real item in the list is large, that the size
                    // of the Spacer is 0.
                    config.autoCentering == null || (
                        lazyListLayoutInfo.visibleItemsInfo.size >= 2 && (
                            // or Empty list (other than the 2 spacers)
                            lazyListLayoutInfo.visibleItemsInfo.size == 2 ||
                                // or first item is correctly size
                                topSpacerIsCorrectlySized(
                                    config,
                                    lazyListLayoutInfo.visibleItemsInfo,
                                    lazyListLayoutInfo.totalItemsCount
                                )
                            )
                        )
                } else {
                    // We are already initialized and have an incomplete scroll to finish
                    incompleteScrollItem.value != null
                }

            DefaultScalingLazyListLayoutInfo(
                internalVisibleItemsInfo = visibleItemsInfo,
                totalItemsCount = totalItemsCount,
                viewportStartOffset =
                    lazyListLayoutInfo.viewportStartOffset + config.extraPaddingPx,
                viewportEndOffset = lazyListLayoutInfo.viewportEndOffset - config.extraPaddingPx,
                centerItemIndex = if (initialized) newCenterItemIndex else 0,
                centerItemScrollOffset = if (initialized) newCenterItemScrollOffset else 0,
                reverseLayout = config.reverseLayout,
                orientation = lazyListLayoutInfo.orientation,
                viewportSize = IntSize(
                    width = lazyListLayoutInfo.viewportSize.width,
                    height = lazyListLayoutInfo.viewportSize.height - config.extraPaddingPx * 2
                ),
                beforeContentPadding = config.beforeContentPaddingPx,
                afterContentPadding = config.afterContentPaddingPx,
                beforeAutoCenteringPadding = calculateTopAutoCenteringPaddingPx(
                    config,
                    visibleItemsInfo,
                    totalItemsCount
                ),
                afterAutoCenteringPadding = calculateBottomAutoCenteringPaddingPx(
                    config,
                    visibleItemsInfo,
                    totalItemsCount),
                readyForInitialScroll = readyForInitialScroll,
                initialized = initialized,
                anchorType = config.anchorType,
            )
        }
    }

    private fun findItemNearestCenter(
        lazyListLayoutInfo: LazyListLayoutInfo,
        params: Configuration,
        verticalAdjustment: Int
    ): Int? {
        var result: Int? = null
        // Find the item in the middle of the viewport
        for (i in lazyListLayoutInfo.visibleItemsInfo.indices) {
            val item = lazyListLayoutInfo.visibleItemsInfo[i]
            if (!discardAutoCenteringListItem(params, lazyListLayoutInfo, item)) {
                val rawItemStart = item.offset - verticalAdjustment
                val rawItemEnd = rawItemStart + item.size + params.gapBetweenItemsPx / 2f
                result = i
                if (rawItemEnd > params.viewportCenterLinePx()) {
                    break
                }
            }
        }
        return result
    }

    companion object {
        /**
         * The default [Saver] implementation for [ScalingLazyListState].
         */
        val Saver = listSaver<ScalingLazyListState, Int>(
            save = {
                listOf(
                    it.centerItemIndex,
                    it.centerItemScrollOffset,
                )
            },
            restore = {
                val scalingLazyColumnState = ScalingLazyListState(it[0], it[1])
                scalingLazyColumnState
            }
        )
    }

    override val isScrollInProgress: Boolean
        get() {
            return lazyListState.isScrollInProgress
        }

    override fun dispatchRawDelta(delta: Float): Float {
        return lazyListState.dispatchRawDelta(delta)
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        lazyListState.scroll(scrollPriority = scrollPriority, block = block)
    }

    override val canScrollForward: Boolean
        get() = lazyListState.canScrollForward

    override val canScrollBackward: Boolean
        get() = lazyListState.canScrollBackward
    /**
     * Instantly brings the item at [index] to the center of the viewport and positions it based on
     * the [anchorType] and applies the [scrollOffset] pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     * scroll the item further upward (taking it partly offscreen).
     */
    public suspend fun scrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) {
        return scrollToItem(false, index, scrollOffset)
    }

    /**
     * Brings the item at [index] to the center of the viewport and positions it based on
     * the [anchorType] and applies the [scrollOffset] pixels.
     *
     * @param animated whether to animate the scroll
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     * scroll the item further upward (taking it partly offscreen).
     */
    internal suspend fun scrollToItem(
        animated: Boolean,
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int,
    ) {
        if (!initialized.value) {
            // We can't scroll yet, save to do it when we can (on the first composition).
            initialCenterItemIndex = index
            initialCenterItemScrollOffset = scrollOffset
            return
        }

        // Find the underlying LazyList index taking into account the Spacer added before
        // the first ScalingLazyColumn list item when autoCentering
        val layoutInfo = layoutInfo
        val config = config.value!!
        val targetIndex = index.coerceAtMost(layoutInfo.totalItemsCount)
        val lazyListStateIndex = targetIndex + if (config.autoCentering != null) 1 else 0

        val offsetToCenterOfViewport =
            config.beforeContentPaddingPx - config.viewportCenterLinePx()
        if (config.anchorType == ScalingLazyListAnchorType.ItemStart) {
            val offset = offsetToCenterOfViewport + scrollOffset
            return lazyListState.scrollToItem(animated, lazyListStateIndex, offset)
        } else {
            var item = lazyListState.layoutInfo.findItemInfoWithIndex(lazyListStateIndex)
            if (item == null) {
                val estimatedUnadjustedHeight = layoutInfo.averageUnadjustedItemSize()
                val estimatedOffset =
                    offsetToCenterOfViewport + (estimatedUnadjustedHeight / 2) + scrollOffset

                // Scroll the item into the middle of the viewport so that we know it is visible
                lazyListState.scrollToItem(animated, lazyListStateIndex, estimatedOffset)
                // Now we know that the item is visible find it and fine tune our position
                item = lazyListState.layoutInfo.findItemInfoWithIndex(lazyListStateIndex)
            }
            if (item != null) {
                // Decide if the item is in the right place
                if (centerItemIndex != index || centerItemScrollOffset != scrollOffset) {
                    val offset = offsetToCenterOfViewport + (item.size / 2) + scrollOffset
                    return lazyListState.scrollToItem(animated, lazyListStateIndex, offset)
                }
            } else {
                // The scroll has failed - this should only happen if the list is not currently
                // visible
                incompleteScrollItem.value = targetIndex
                incompleteScrollOffset.value = scrollOffset
                incompleteScrollAnimated.value = animated
            }
        }
        return
    }

    internal suspend fun scrollToInitialItem() {
        // First time initialization
        if (!initialized.value) {
            initialized.value = true
            scrollToItem(initialCenterItemIndex, initialCenterItemScrollOffset)
        }
        // Check whether we are becoming visible after an incomplete scrollTo/animatedScrollTo
        if (incompleteScrollItem.value != null) {
            val animated = incompleteScrollAnimated.value
            val targetIndex = incompleteScrollItem.value!!
            val targetOffset = incompleteScrollOffset.value!!
            // Reset the incomplete scroll indicator
            incompleteScrollItem.value = null
            scrollToItem(animated, targetIndex, targetOffset)
        }
        return
    }

    /**
     * Animate (smooth scroll) the given item at [index] to the center of the viewport and position
     * it based on the [anchorType] and applies the [scrollOffset] pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll (same as
     * [scrollToItem]) - note that positive offset refers to forward scroll, so in a
     * top-to-bottom list, positive offset will scroll the item further upward (taking it partly
     * offscreen)
     */
    public suspend fun animateScrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) {
        return scrollToItem(true, index, scrollOffset)
    }

    private fun discardAutoCenteringListItem(
        params: Configuration,
        lazyListLayoutInfo: LazyListLayoutInfo,
        item: LazyListItemInfo
    ): Boolean =
        params.autoCentering != null &&
            (item.index == 0 || item.index == lazyListLayoutInfo.totalItemsCount - 1)

    /**
     * Calculate the amount of top padding needed (if any) to make sure that the
     * [AutoCenteringParams.itemIndex] item can be placed in the center of the viewport at
     * [AutoCenteringParams.itemOffset]
     */
    private fun calculateTopAutoCenteringPaddingPx(
        params: Configuration,
        visibleItems: List<ScalingLazyListItemInfo>,
        totalItemCount: Int
    ): Int {
        if (params.autoCentering == null ||
            (visibleItems.isNotEmpty() && visibleItems.first().index != 0)) return 0

        // Work out the index we want to find - if there are less items in the list than would be
        // needed to make initialItemIndex be visible then use the last visible item
        val itemIndexToFind = params.autoCentering.itemIndex.coerceAtMost(totalItemCount - 1)

        // Find the initialCenterItem, if it is null that means it is not in view - therefore
        // we have more than enough content before it to make sure it can be scrolled to the center
        // of the viewport
        val initialCenterItem =
            visibleItems.fastFirstOrNull { it.index == itemIndexToFind }

        // Determine how much space we actually need
        var spaceNeeded = params.spaceNeeded(initialCenterItem)

        if (spaceNeeded > 0f) {
            // Now see how much content we already have
            visibleItems.fastMap {
                if (it.index < itemIndexToFind) {
                    // Reduce the space needed
                    spaceNeeded = spaceNeeded - params.gapBetweenItemsPx - it.unadjustedSize
                }
            }
        }
        return (spaceNeeded + params.gapBetweenItemsPx).coerceAtLeast(0)
    }

    /**
     * Determine if the top Spacer component in the underlying LazyColumn has the correct size. We
     * need to be sure that it has the correct size before we do scrollToInitialItem in order to
     * make sure that the initial scroll will be successful.
     */
    private fun topSpacerIsCorrectlySized(
        params: Configuration,
        visibleItems: List<LazyListItemInfo>,
        totalItemCount: Int
    ): Boolean {
        // If the top items has a non-zero size we know that it has been correctly inflated.
        if (visibleItems.first().size > 0) return true

        // Work out the index we want to find - if there are less items in the list than would be
        // needed to make initialItemIndex be visible then use the last visible item. The -2 is to
        // allow for the spacers, i.e. an underlying list of size 3 has 2 spacers in index 0 and 2
        // and one real item in underlying lazy column index 1.
        val itemIndexToFind =
            (params.autoCentering!!.itemIndex + 1).coerceAtMost(totalItemCount - 2)

        // Find the initialCenterItem, if it is null that means it is not in view - therefore
        // we have more than enough content before it to make sure it can be scrolled to the center
        // of the viewport
        val initialCenterItem =
            visibleItems.fastFirstOrNull { it.index == itemIndexToFind }

        // Determine how much space we actually need
        var spaceNeeded = params.spaceNeeded(initialCenterItem)

        if (spaceNeeded > 0f) {
            // Now see how much content we already have
            visibleItems.fastMap {
                if (it.index != 0 && it.index < itemIndexToFind) {
                    // Reduce the space needed
                    spaceNeeded = spaceNeeded - params.gapBetweenItemsPx - it.size
                }
            }
        }
        // Finally if the remaining space needed is less that the gap between items then we do not
        // need to add any additional space so the spacer being size zero is correct. Otherwise we
        // need to wait for it to be inflated.
        return spaceNeeded < params.gapBetweenItemsPx
    }

    private fun Configuration.spaceNeeded(item: ScalingLazyListItemInfo?) =
        viewportCenterLinePx() - gapBetweenItemsPx - autoCentering!!.itemOffset -
            (item?.unadjustedItemSizeAboveOffsetPoint(this) ?: 0)

    private fun Configuration.spaceNeeded(item: LazyListItemInfo?) =
        viewportCenterLinePx() - gapBetweenItemsPx - autoCentering!!.itemOffset -
            (item?.itemSizeAboveOffsetPoint(this) ?: 0)

    private fun calculateBottomAutoCenteringPaddingPx(
        params: Configuration,
        visibleItemsInfo: List<ScalingLazyListItemInfo>,
        totalItemsCount: Int
    ) = if (params.autoCentering != null && visibleItemsInfo.isNotEmpty() &&
        visibleItemsInfo.last().index == totalItemsCount - 1
    ) {
        // Round any fractional part up for the bottom spacer as we round down toward zero
        // for the viewport center line and item heights working from the top of the
        // viewport and then add 1 pixel if needed (for an odd height viewport) at the end
        // spacer
        params.viewportHeightPx - params.viewportCenterLinePx() -
            visibleItemsInfo.last().unadjustedItemSizeBelowOffsetPoint(params)
    } else {
        0
    }

    /**
     * Calculate the center line of the viewport. This is half of the viewport height rounded down
     * to the nearest int. This means that for a viewport with an odd number of pixels in height we
     * will have the area above the viewport being one pixel smaller, e.g. a 199 pixel high
     * viewport will be treated as having 99 pixels above and 100 pixels below the center line.
     */
    private fun Configuration.viewportCenterLinePx(): Int = viewportHeightPx / 2

    /**
     * How much of the items unscaled size would be above the point on the item that represents the
     * offset point. For an edge anchored item the offset point is the top of the item. For a center
     * anchored item the offset point is floor(height/2).
     */
    private fun ScalingLazyListItemInfo.unadjustedItemSizeAboveOffsetPoint(
        params: Configuration
    ) =
        if (params.anchorType == ScalingLazyListAnchorType.ItemStart) {
            0
        } else {
            this.unadjustedSize / 2
        }

    /**
     * How much of the items size would be above the point on the item that represents the
     * offset point. For an edge anchored item the offset point is the top of the item. For a center
     * anchored item the offset point is floor(height/2).
     */
    private fun LazyListItemInfo.itemSizeAboveOffsetPoint(params: Configuration) =
        if (params.anchorType == ScalingLazyListAnchorType.ItemStart) {
            0
        } else {
            this.size / 2
        }

    /**
     * How much of the items size would be below the point on the item that represents the
     * offset point. For an edge anchored item the offset point is the top of the item. For a center
     * anchored item the offset point is floor(height/2).
     */
    private fun ScalingLazyListItemInfo.unadjustedItemSizeBelowOffsetPoint(
        params: Configuration
    ) =
        this.unadjustedSize - unadjustedItemSizeAboveOffsetPoint(params)
}

private fun LazyListLayoutInfo.findItemInfoWithIndex(index: Int): LazyListItemInfo? {
    return this.visibleItemsInfo.fastFirstOrNull { it.index == index }
}

private suspend fun LazyListState.scrollToItem(animated: Boolean, index: Int, offset: Int) {
    if (animated) animateScrollToItem(index, offset) else scrollToItem(index, offset)
}

private fun ScalingLazyListLayoutInfo.averageUnadjustedItemSize(): Int {
    var totalSize = 0
    visibleItemsInfo.fastForEach { totalSize += it.unadjustedSize }
    return if (visibleItemsInfo.isNotEmpty())
        (totalSize.toFloat() / visibleItemsInfo.size).roundToInt()
    else 0
}

private object EmptyScalingLazyListLayoutInfo : ScalingLazyListLayoutInfo {
    override val visibleItemsInfo = emptyList<ScalingLazyListItemInfo>()
    override val viewportStartOffset = 0
    override val viewportEndOffset = 0
    override val totalItemsCount = 0
    override val viewportSize = IntSize.Zero
    override val orientation = Orientation.Vertical
    override val reverseLayout = false
    override val beforeContentPadding = 0
    override val afterContentPadding = 0
    override val beforeAutoCenteringPadding = 0
    override val afterAutoCenteringPadding = 0
    override val anchorType: ScalingLazyListAnchorType = ScalingLazyListAnchorType.ItemCenter
}

internal fun ScalingLazyListLayoutInfo.internalVisibleItemInfo() =
    (this as? DefaultScalingLazyListLayoutInfo)?.internalVisibleItemsInfo ?: emptyList()
