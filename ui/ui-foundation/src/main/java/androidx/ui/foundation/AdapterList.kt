/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import android.content.Context
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.Untracked
import androidx.compose.compositionReference
import androidx.compose.currentComposer
import androidx.compose.remember
import androidx.ui.core.Constraints
import androidx.ui.core.ContextAmbient
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutNode
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.MeasuringIntrinsicsMeasureBlocks
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.clipToBounds
import androidx.ui.core.materialize
import androidx.ui.core.subcomposeInto
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.ScrollableState
import androidx.ui.foundation.gestures.scrollable
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.round
import kotlin.math.abs
import kotlin.math.round

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

private class ListState<T> {
    lateinit var recomposer: Recomposer
    lateinit var itemCallback: @Composable (T) -> Unit
    lateinit var data: List<T>

    var forceRecompose = false
    var compositionRef: CompositionReference? = null
    /**
     * Should always be non-null when attached
     */
    var context: Context? = null
    /**
     * Used to get the reference to populate [rootNode]
     */
    val rootNodeRef = Ref<LayoutNode>()
    /**
     * The root [LayoutNode] of this [AdapterList]
     */
    val rootNode get() = rootNodeRef.value!!
    /**
     * The measure blocks for [rootNode]
     */
    val measureBlocks = ListMeasureBlocks()
    /**
     * The layout direction of the [AdapterList]
     */
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    /**
     * The index of the first item that is composed into the layout tree
     */
    var firstComposedItem = DataIndex(0)
    /**
     * The index of the last item that is composed into the layout tree
     */
    var lastComposedItem = DataIndex(-1) // obviously-bogus sentinel value
    /**
     * Scrolling forward is positive - i.e., the amount that the item is offset backwards
     */
    var firstItemScrollOffset = 0f
    /**
     * The amount of space remaining in the last item
     */
    var lastItemRemainingSpace = 0f
    /**
     * The amount of scroll to be consumed in the next layout pass.  Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    var scrollToBeConsumed = 0f
    /**
     * The children that have been measured this measure pass.
     * Used to avoid measuring twice in a single pass, which is illegal
     */
    private val measuredThisPass: MutableMap<DataIndex, Boolean> = mutableMapOf()

    /**
     * The listener to be passed to onScrollDeltaConsumptionRequested.
     * Cached to avoid recreations
     */
    val onScrollDeltaConsumptionRequestedListener: (Float) -> Float = { onScroll(it) }

    /**
     * Tracks the correspondence between the child layout nodes and their compositions, so that
     * they can be disposed later
     */
    private val compositionsForLayoutNodes = mutableMapOf<LayoutNode, Composition>()

    // TODO: really want an Int here
    private fun onScroll(distance: Float): Float {
        check(abs(scrollToBeConsumed) < 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed = distance
        rootNode.requestRemeasure()
        rootNode.owner!!.measureAndLayout()
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

    private fun consumePendingScroll() {
        val scrollDirection = ScrollDirection(isForward = scrollToBeConsumed < 0f)

        while (true) {
            // General outline:
            // Consume as much of the drag as possible via adjusting the scroll offset
            scrollToBeConsumed = consumeScrollViaOffset(scrollToBeConsumed)

            // TODO: What's the correct way to handle half a pixel of unconsumed scroll?

            // Allow up to half a pixel of scroll to remain unconsumed
            if (abs(scrollToBeConsumed) >= 0.5f) {
                // We need to bring another item onscreen. Can we?
                if (!composeAndMeasureNextItem(scrollDirection)) {
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
    private fun composeAndMeasureNextItem(scrollDirection: ScrollDirection): Boolean {
        val nextItemIndex = if (scrollDirection.isForward) {
            if (data.size > lastComposedItem.value + 1) {
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
        // TODO: axis
        val childConstraints = Constraints(maxWidth = rootNode.width, maxHeight = IntPx.Infinity)

        val childPlaceable = nextItem.measure(childConstraints, layoutDirection)
        measuredThisPass[nextItemIndex] = true

        val childHeight = childPlaceable.height

        // Add in our newly composed space so that it may be consumed
        if (scrollDirection.isForward) {
            lastItemRemainingSpace += childHeight.value
        } else {
            firstItemScrollOffset += childHeight.value
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
        rootNode.requestRemeasure()
    }

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
                // if there were models created and read inside this subcomposition
                // and we are going to modify these models within the same frame,
                // the composables which read this model will not be recomposed.
                // to make this possible we should switch to the next frame.
                FrameManager.nextFrame()
            }

            // We're being asked to consume scroll by the Scrollable
            if (abs(scrollToBeConsumed) >= 0.5f) {
                // Consume it in advance, because it simplifies the rest of this method if we
                // know exactly how much scroll we've consumed - for instance, we can safely
                // discard anything off the start of the viewport, because we know we can fill
                // it, assuming nothing has shrunken on us (which has to be handled separately
                // anyway)
                consumePendingScroll()
            }

            val width = constraints.maxWidth.value
            val height = constraints.maxHeight.value
            this@ListState.layoutDirection = layoutDirection
            // TODO: axis
            val childConstraints = Constraints(maxWidth = width.ipx, maxHeight = IntPx.Infinity)

            var heightUsed = round(-firstItemScrollOffset)

            // The index of the first item that should be displayed, regardless of what is
            // currently displayed.  Will be moved forward as we determine what's offscreen
            var itemIndexOffset = firstComposedItem

            // TODO: handle the case where we can't fill the viewport due to children shrinking,
            //  but there are more items at the start that we could fill with
            var index = itemIndexOffset
            while (heightUsed < height && index.value < data.size) {
                val node = getNodeForDataIndex(index)
                if (measuredThisPass[index] != true) {
                    node.measure(childConstraints, layoutDirection)
                    measuredThisPass[index] = true
                }
                val childHeight = node.height.value
                heightUsed += childHeight

                if (heightUsed < 0f) {
                    // this item is offscreen, remove it and the offset it took up
                    itemIndexOffset = index + 1
                    firstItemScrollOffset -= childHeight
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

            lastItemRemainingSpace = if (heightUsed > height) {
                (heightUsed - height)
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

            return measureScope.layout(width = width.ipx, height = height.ipx) {
                var currentY = round(-firstItemScrollOffset)
                rootNode.children.forEach {
                    it.place(x = IntPx.Zero, y = currentY.px.round())
                    currentY += it.height.value
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
            if (dataIdx.value < data.size && dataIdx.value >= 0) {
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
        // TODO(b/150390669): Review use of @Untracked
        val composition = subcomposeInto(context!!, node, recomposer, compositionRef) @Untracked {
            itemCallback(data[dataIndex.value])
        }
        compositionsForLayoutNodes[node] = composition
        return node
    }
}

private val ListItemMeasureBlocks = MeasuringIntrinsicsMeasureBlocks { measurables, constraints,
                                                                       _ ->
    val placeables = measurables.map { measurable ->
        measurable.measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth
            )
        )
    }
    val columnWidth = (placeables.maxBy { it.width.value }?.width ?: 0.ipx)
        .coerceAtLeast(constraints.minWidth)
    val columnHeight = placeables.sumBy { it.height.value }.ipx.coerceIn(
        constraints.minHeight,
        constraints.maxHeight
    )
    layout(columnWidth, columnHeight) {
        var top = 0.ipx
        placeables.forEach { placeable ->
            placeable.placeAbsolute(0.ipx, top)
            top += placeable.height
        }
    }
}

/**
 * A vertically scrolling list that only composes and lays out the currently visible items.
 *
 * @param data the backing list of data to display
 * @param modifier the modifier to apply to this `AdapterList`
 * @param itemCallback a callback that takes an item from [data] and emits the UI for that item.
 * May emit any number of components, which will be stacked vertically
 */
@Composable
fun <T> AdapterList(
    data: List<T>,
    modifier: Modifier = Modifier,
    itemCallback: @Composable (T) -> Unit
) {
    val state = remember { ListState<T>() }
    state.recomposer = currentComposer.recomposer
    state.itemCallback = itemCallback
    state.data = data
    state.context = ContextAmbient.current
    state.compositionRef = compositionReference()
    state.forceRecompose = true

    androidx.ui.core.LayoutNode(
        modifier = currentComposer.materialize(
            modifier
                .scrollable(
                    dragDirection = DragDirection.Vertical,
                    scrollableState = ScrollableState(
                        onScrollDeltaConsumptionRequested =
                        state.onScrollDeltaConsumptionRequestedListener
                    )
                )
                .clipToBounds()
        ),
        ref = state.rootNodeRef,
        measureBlocks = state.measureBlocks
    )
    state.recomposeIfAttached()
}
