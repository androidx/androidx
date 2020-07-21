/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.compose.Composable
import androidx.compose.ComposableContract
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.ExperimentalComposeApi
import androidx.compose.Recomposer
import androidx.ui.core.Constraints
import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutNode
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.MeasuringIntrinsicsMeasureBlocks
import androidx.ui.core.Placeable
import androidx.ui.core.Ref
import androidx.ui.core.Remeasurement
import androidx.ui.core.RemeasurementModifier
import androidx.ui.core.constrainHeight
import androidx.ui.core.constrainWidth
import androidx.ui.core.subcomposeInto
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private inline class ScrollDirection(val isForward: Boolean)

@Suppress("NOTHING_TO_INLINE")
private inline class DataIndex(val value: Int) {
    inline operator fun inc(): DataIndex = DataIndex(value + 1)
    inline operator fun dec(): DataIndex = DataIndex(value - 1)
    inline operator fun plus(i: Int): DataIndex = DataIndex(value + i)
    inline operator fun minus(i: Int): DataIndex = DataIndex(value - i)
    inline operator fun minus(i: DataIndex): DataIndex = DataIndex(value - i.value)
    inline operator fun compareTo(other: DataIndex): Int = value - other.value
}

private inline class LayoutIndex(val value: Int)

@OptIn(ExperimentalLayoutNodeApi::class)
internal class LazyItemsState<T>(val isVertical: Boolean) {
    lateinit var recomposer: Recomposer
    lateinit var itemContent: @Composable (T) -> Unit
    lateinit var items: List<T>

    var forceRecompose = false
    var compositionRef: CompositionReference? = null
    /**
     * Used to get the reference to populate [rootNode]
     */
    val rootNodeRef = Ref<LayoutNode>()
    /**
     * The root [LayoutNode]
     */
    private val rootNode get() = rootNodeRef.value!!
    /**
     * The measure blocks for [rootNode]
     */
    val measureBlocks: LayoutNode.MeasureBlocks = ListMeasureBlocks()
    /**
     * The layout direction
     */
    private var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    /**
     * The index of the first item that is composed into the layout tree
     */
    private var firstComposedItem = DataIndex(0)
    /**
     * The index of the last item that is composed into the layout tree
     */
    private var lastComposedItem = DataIndex(-1) // obviously-bogus sentinel value
    /**
     * Scrolling forward is positive - i.e., the amount that the item is offset backwards
     */
    private var firstItemScrollOffset = 0f
    /**
     * The amount of space remaining in the last item
     */
    private var lastItemRemainingSpace = 0f
    /**
     * The amount of scroll to be consumed in the next layout pass.  Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    private var scrollToBeConsumed = 0f
    /**
     * The children that have been measured this measure pass.
     * Used to avoid measuring twice in a single pass, which is illegal
     */
    private val measuredThisPass: MutableMap<DataIndex, Placeable> = mutableMapOf()

    /**
     * The listener to be passed to onScrollDeltaConsumptionRequested.
     * Cached to avoid recreations
     */
    val onScrollDelta: (Float) -> Float = { onScroll(it) }

    /**
     * Tracks the correspondence between the child layout nodes and their compositions, so that
     * they can be disposed later
     */
    private val compositionsForLayoutNodes = mutableMapOf<LayoutNode, Composition>()

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    private lateinit var remeasurement: Remeasurement

    /**
     * The modifier which provides [remeasurement].
     */
    val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@LazyItemsState.remeasurement = remeasurement
        }
    }

    private val Placeable.mainAxisSize get() = if (isVertical) height else width
    private val Placeable.crossAxisSize get() = if (!isVertical) height else width

    // TODO: really want an Int here
    private fun onScroll(distance: Float): Float {
        check(abs(scrollToBeConsumed) < 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed = distance
        remeasurement.forceRemeasure()
        val scrollConsumed = distance - scrollToBeConsumed

        if (abs(scrollToBeConsumed) < 0.5) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    private fun consumePendingScroll(childConstraints: Constraints) {
        val scrollDirection = ScrollDirection(isForward = scrollToBeConsumed < 0f)

        while (true) {
            // General outline:
            // Consume as much of the drag as possible via adjusting the scroll offset
            scrollToBeConsumed = consumeScrollViaOffset(scrollToBeConsumed)

            // TODO: What's the correct way to handle half a pixel of unconsumed scroll?

            // Allow up to half a pixel of scroll to remain unconsumed
            if (abs(scrollToBeConsumed) >= 0.5f) {
                // We need to bring another item onscreen. Can we?
                if (!composeAndMeasureNextItem(childConstraints, scrollDirection)) {
                    // Nope. Break out and return the rest of the drag
                    break
                }
                // Yay, we got another item! Our scroll offsets are populated again, go back and
                // consume them in the next round.
            } else {
                // We've consumed the whole scroll
                break
            }
        }
    }

    /**
     * @return The amount of scroll remaining unconsumed
     */
    private fun consumeScrollViaOffset(delta: Float): Float {
        if (delta < 0) {
            // Scrolling forward, content moves up
            // Consume via space at end
            // Remember: delta is *negative*
            if (lastItemRemainingSpace >= -delta) {
                // We can consume it all
                updateScrollOffsets(delta)
                return 0f
            } else {
                // All offset consumed, return the remaining offset to the caller
                // delta is negative, prevRemainingSpace/lastItemRemainingSpace are positive
                val prevRemainingSpace = lastItemRemainingSpace
                updateScrollOffsets(-prevRemainingSpace)
                return delta + prevRemainingSpace
            }
        } else {
            // Scrolling backward, content moves down
            // Consume via initial offset
            if (firstItemScrollOffset >= delta) {
                // We can consume it all
                updateScrollOffsets(delta)
                return 0f
            } else {
                // All offset consumed, return the remaining offset to the caller
                val prevRemainingSpace = firstItemScrollOffset
                updateScrollOffsets(prevRemainingSpace)
                return delta - prevRemainingSpace
            }
        }
    }

    /**
     * Must be called within a measure pass.
     *
     * @return `true` if an item was composed and measured, `false` if there are no more items in
     * the scroll direction
     */
    private fun composeAndMeasureNextItem(
        childConstraints: Constraints,
        scrollDirection: ScrollDirection
    ): Boolean {
        val nextItemIndex = if (scrollDirection.isForward) {
            if (items.size > lastComposedItem.value + 1) {
                lastComposedItem + 1
            } else {
                return false
            }
        } else {
            if (firstComposedItem.value > 0) {
                firstComposedItem - 1
            } else {
                return false
            }
        }

        val nextItem = composeChildForDataIndex(nextItemIndex)

        val childPlaceable = nextItem.measure(childConstraints, layoutDirection)
        measuredThisPass[nextItemIndex] = childPlaceable

        val childSize = childPlaceable.mainAxisSize

        // Add in our newly composed space so that it may be consumed
        if (scrollDirection.isForward) {
            lastItemRemainingSpace += childSize
        } else {
            firstItemScrollOffset += childSize
        }

        return true
    }

    /**
     * Does no bounds checking, just moves the start and last offsets in sync.
     * Assumes the caller has checked bounds.
     */
    private fun updateScrollOffsets(delta: Float) {
        // Scrolling forward is negative delta and consumes space, so add the negative
        lastItemRemainingSpace += delta
        // Scrolling forward is negative delta and adds offset, so subtract the negative
        firstItemScrollOffset -= delta
        @OptIn(ExperimentalLayoutNodeApi::class)
        rootNode.requestRemeasure()
    }

    @OptIn(ExperimentalLayoutNodeApi::class)
    private inner class ListMeasureBlocks : LayoutNode.NoIntrinsicsMeasureBlocks(
        error = "Intrinsic measurements are not supported by AdapterList (yet)"
    ) {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult {
            measuredThisPass.clear()
            if (forceRecompose) {
                rootNode.ignoreModelReads { recomposeAllChildren() }
            }

            this@LazyItemsState.layoutDirection = layoutDirection

            val maxMainAxis = if (isVertical) constraints.maxHeight else constraints.maxWidth
            val childConstraints = Constraints(
                maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity,
                maxHeight = if (!isVertical) constraints.maxHeight else Constraints.Infinity
            )

            // We're being asked to consume scroll by the Scrollable
            if (abs(scrollToBeConsumed) >= 0.5f) {
                // Consume it in advance, because it simplifies the rest of this method if we
                // know exactly how much scroll we've consumed - for instance, we can safely
                // discard anything off the start of the viewport, because we know we can fill
                // it, assuming nothing has shrunken on us (which has to be handled separately
                // anyway)
                consumePendingScroll(childConstraints)
            }

            var mainAxisUsed = (-firstItemScrollOffset).roundToInt()
            var maxCrossAxis = 0

            // The index of the first item that should be displayed, regardless of what is
            // currently displayed.  Will be moved forward as we determine what's offscreen
            var itemIndexOffset = firstComposedItem

            // TODO: handle the case where we can't fill the viewport due to children shrinking,
            //  but there are more items at the start that we could fill with
            var index = itemIndexOffset
            while (mainAxisUsed <= maxMainAxis && index.value < items.size) {
                val node = getNodeForDataIndex(index)
                val placeable = measuredThisPass.getOrPut(index) {
                    node.measure(childConstraints, layoutDirection)
                }
                val childMainAxisSize = placeable.mainAxisSize
                mainAxisUsed += childMainAxisSize

                maxCrossAxis = max(maxCrossAxis, placeable.crossAxisSize)

                if (mainAxisUsed < 0f) {
                    // this item is offscreen, remove it and the offset it took up
                    itemIndexOffset = index + 1
                    firstItemScrollOffset -= childMainAxisSize
                }

                index++
            }
            lastComposedItem = index - 1 // index is incremented after the last iteration

            // The number of layout children that we want to have, not including any offscreen
            // items at the start or end
            val numDesiredChildren = (lastComposedItem - itemIndexOffset + 1).value

            // Remove no-longer-needed items from the start of the list
            if (itemIndexOffset > firstComposedItem) {
                val count = (itemIndexOffset - firstComposedItem).value
                removeAndDisposeChildren(fromIndex = LayoutIndex(0), count = count)
            }
            firstComposedItem = itemIndexOffset

            lastItemRemainingSpace = if (mainAxisUsed > maxMainAxis) {
                (mainAxisUsed - maxMainAxis).toFloat()
            } else {
                0f
            }

            // Remove no-longer-needed items from the end of the list
            val layoutChildrenInNode = rootNode.children.size
            if (layoutChildrenInNode > numDesiredChildren) {
                val count = layoutChildrenInNode - numDesiredChildren
                removeAndDisposeChildren(
                    // We've already removed the extras at the start, so the desired children
                    // start at index 0
                    fromIndex = LayoutIndex(numDesiredChildren),
                    count = count
                )
            }

            // Wrap the content of the children on the cross axis
            val layoutWidth = if (isVertical) {
                constraints.constrainWidth(maxCrossAxis)
            } else {
                constraints.maxWidth
            }
            val layoutHeight = if (!isVertical) {
                constraints.constrainHeight(maxCrossAxis)
            } else {
                constraints.maxHeight
            }

            return measureScope.layout(layoutWidth, layoutHeight) {
                val currentMainAxis = (-firstItemScrollOffset).roundToInt()
                var x = if (isVertical) 0 else currentMainAxis
                var y = if (!isVertical) 0 else currentMainAxis
                rootNode.children.forEach {
                    it.place(x = x, y = y)
                    if (isVertical) {
                        y += it.height
                    } else {
                        x += it.width
                    }
                }
            }
        }
    }

    private fun removeAndDisposeChildren(
        fromIndex: LayoutIndex,
        count: Int
    ) {
        for (i in 1..count) {
            val node = rootNode.children[fromIndex.value]
            rootNode.removeAt(
                index = fromIndex.value,
                // remove one at a time to avoid creating unnecessary data structures
                // to store the nodes we're about to dispose
                count = 1
            )
            compositionFor(node).dispose()
            compositionsForLayoutNodes.remove(node)
        }
    }

    fun disposeAllChildren() {
        removeAndDisposeChildren(LayoutIndex(0), rootNode.children.size)
    }

    fun recomposeIfAttached() {
        if (rootNode.owner != null) {
            // TODO: run this in an `onPreCommit` callback for multithreaded/deferred composition
            //  safety
            recomposeAllChildren()
        }
    }

    private fun recomposeAllChildren() {
        for (idx in rootNode.children.indices) {
            val dataIdx = LayoutIndex(idx).toDataIndex()
            // Make sure that we're only recomposing items that still exist in the data.
            // Excess layout children will be removed in the next measure/layout pass
            if (dataIdx.value < items.size && dataIdx.value >= 0) {
                composeChildForDataIndex(dataIdx)
            }
        }
        forceRecompose = false
    }

    private fun compositionFor(childNode: LayoutNode): Composition {
        return compositionsForLayoutNodes[childNode] ?: throw IllegalStateException(
            "No composition found for child $childNode"
        )
    }

    private fun getNodeForDataIndex(dataIndex: DataIndex): LayoutNode {
        val layoutIndex = dataIndex.toLayoutIndex()
        val layoutChildren = rootNode.children
        val numLayoutChildren = layoutChildren.size
        check(layoutIndex.value <= numLayoutChildren) {
            "Index too high: $dataIndex, firstComposedItem: $firstComposedItem, " +
                    "layout index: $layoutIndex, current children: $numLayoutChildren"
        }
        return if (layoutIndex.value < numLayoutChildren) {
            layoutChildren[layoutIndex.value]
        } else {
            composeChildForDataIndex(dataIndex)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun DataIndex.toLayoutIndex(): LayoutIndex {
        return LayoutIndex(value - firstComposedItem.value)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun LayoutIndex.toDataIndex(): DataIndex {
        return DataIndex(value + firstComposedItem.value)
    }

    /**
     * Contract: this must be either a data index that is already in the LayoutNode, or one
     * immediately on either side of those.
     */
    private fun composeChildForDataIndex(dataIndex: DataIndex): LayoutNode {
        val layoutIndex = dataIndex.toLayoutIndex()

        check(layoutIndex.value >= -1 && layoutIndex.value <= rootNode.children.size) {
            "composeChildForDataIndex called with invalid index, data index: $dataIndex," +
                    " layout index: $layoutIndex"
        }

        val node: LayoutNode
        val atStart = layoutIndex.value == -1
        val atEnd = rootNode.children.size == layoutIndex.value
        if (atEnd || atStart) {
            // This is a new node, either at the end or the start
            node = LayoutNode()
            node.measureBlocks = ListItemMeasureBlocks

            // If it's at the end, then the value is already correct, because we don't need to
            // move any existing LayoutNodes.
            // If it's at the beginning, it has a layout index of -1 because it's being inserted
            // _before_ index 0, but this means that we need to insert it at index 0 and then
            // the others will be shifted forward.  This accounts for these different methods of
            // tracking.
            val newLayoutIndex = if (atStart) 0 else layoutIndex.value
            rootNode.insertAt(newLayoutIndex, node)
            if (atEnd) {
                lastComposedItem++
            } else {
                // atStart
                firstComposedItem--
            }
        } else {
            node = rootNode.children[layoutIndex.value]
        }
        // TODO(b/150390669): Review use of @ComposableContract(tracked = false)
        @OptIn(ExperimentalComposeApi::class)
        val composition = subcomposeInto(
            node,
            recomposer,
            compositionRef
        ) @ComposableContract(tracked = false) {
            itemContent(items[dataIndex.value])
        }
        compositionsForLayoutNodes[node] = composition
        return node
    }

    private val ListItemMeasureBlocks =
        MeasuringIntrinsicsMeasureBlocks { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val mainAxisSize = placeables.sumBy { it.mainAxisSize }
            val crossAxisSize = placeables.maxByOrNull { it.crossAxisSize }?.crossAxisSize ?: 0
            layout(
                width = if (!isVertical) mainAxisSize else crossAxisSize,
                height = if (isVertical) mainAxisSize else crossAxisSize
            ) {
                var y = 0
                var x = 0
                placeables.forEach { placeable ->
                    placeable.placeAbsolute(x, y)
                    if (isVertical) {
                        y += placeable.mainAxisSize
                    } else {
                        x += placeable.mainAxisSize
                    }
                }
            }
        }
}
