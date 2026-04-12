/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.demos

import android.annotation.SuppressLint
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Drag2DScope
import androidx.compose.foundation.gestures.Draggable2DState
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasurePolicy
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.abs
import kotlin.math.sign

private val tableItems: Array<Array<String>> =
    Array(20) { row -> Array(20) { column -> "$row,$column" } }

@Preview
@Composable
fun Lazy2DGridDemo(modifier: Modifier = Modifier) {
    Lazy2DGrid(rows = 20, columns = 20, modifier = modifier) { row, column ->
        Box(modifier = Modifier.size(64.dp).padding(8.dp).background(Color.Red)) {
            Text(tableItems[row][column])
        }
    }
}

private class Lazy2DGridItemProvider(
    rows: Int,
    private val columns: Int,
    private val content: @Composable (row: Int, column: Int) -> Unit,
) : LazyLayoutItemProvider {
    override val itemCount: Int = rows * columns

    @Composable
    override fun Item(index: Int, key: Any) {
        val (row, column) = decomposeIndex(index, columns)
        content.invoke(row, column)
    }
}

class Lazy2DGridState : Draggable2DState {
    var firstVisiblePosition = IntPosition(0, 0)
        private set

    var firstVisiblePositionScrollOffset = IntOffset.Zero
        private set

    internal var scrollToBeConsumed: Offset = Offset.Zero
    private var remeasurement: Remeasurement? = null

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@Lazy2DGridState.remeasurement = remeasurement
            }
        }

    private val dragState = Draggable2DState { onDrag(it) }

    private fun onDrag(offset: Offset) {
        scrollToBeConsumed += offset
        remeasurement?.forceRemeasure()
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend Drag2DScope.() -> Unit) =
        dragState.drag(dragPriority, block)

    override fun dispatchRawDelta(delta: Offset) = dragState.dispatchRawDelta(delta)

    internal fun updateScrollPosition(
        newPosition: IntPosition,
        newOffset: IntOffset,
        consumedScroll: Offset,
    ) {
        scrollToBeConsumed -= consumedScroll
        firstVisiblePosition = newPosition
        firstVisiblePositionScrollOffset = newOffset
    }

    override fun toString(): String {
        return "firstVisiblePosition=$firstVisiblePosition firstVisiblePositionOffset=$firstVisiblePositionScrollOffset"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Lazy2DGrid(
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    state: Lazy2DGridState = remember { Lazy2DGridState() },
    content: @Composable (row: Int, column: Int) -> Unit,
) {
    val itemProvider =
        remember(rows, columns) { { Lazy2DGridItemProvider(rows, columns, content) } }
    val measurePolicy =
        remember(rows, columns, state) {
            measure2dGrid(rows, columns, state) { newPosition, newOffset, scrollConsumed ->
                state.updateScrollPosition(newPosition, newOffset, scrollConsumed)
            }
        }

    LazyLayout(
        itemProvider,
        modifier.draggable2D(state).then(state.remeasurementModifier),
        null,
        measurePolicy,
    )
}

@ExperimentalFoundationApi
@SuppressLint("PrimitiveInCollection")
private fun measure2dGrid(
    rows: Int,
    columns: Int,
    state: Lazy2DGridState,
    updateScrollPosition: (IntPosition, IntOffset, Offset) -> Unit,
): LazyLayoutMeasurePolicy = LazyLayoutMeasurePolicy { constraints ->
    var currentFirstVisibleRowIndex =
        Snapshot.withoutReadObservation { state.firstVisiblePosition.row }
    var currentFirstVisibleRowScrollOffset =
        Snapshot.withoutReadObservation { state.firstVisiblePositionScrollOffset.y }

    if (currentFirstVisibleRowIndex >= rows) {
        // the data set has been updated and now we have less items that we were
        // scrolled to before
        currentFirstVisibleRowIndex = rows - 1
        currentFirstVisibleRowScrollOffset = 0
    }

    var scrollDeltaRow = state.scrollToBeConsumed.y.fastRoundToInt()

    // applying the whole requested scroll offset. we will figure out if we can't consume
    // all of it later
    currentFirstVisibleRowScrollOffset -= scrollDeltaRow

    // if the current scroll offset is less than minimally possible
    if (currentFirstVisibleRowIndex == 0 && currentFirstVisibleRowScrollOffset < 0) {
        scrollDeltaRow += currentFirstVisibleRowScrollOffset
        currentFirstVisibleRowScrollOffset = 0
    }

    // this will contain all the MeasuredItems representing the visible items
    val measuredRows = ArrayDeque<MeasuredRow>()
    val measuredItemProvider = LazyLayout2DGridMeasuredItemProvider()

    val maxOffset = constraints.maxHeight

    val firstVisibleColumnIndex =
        Snapshot.withoutReadObservation { state.firstVisiblePosition.column }
    val firstVisibleColumnOffset =
        Snapshot.withoutReadObservation { state.firstVisiblePositionScrollOffset.x }

    var maxWidthSize = 0
    // we had scrolled backward
    while (currentFirstVisibleRowScrollOffset < 0 && currentFirstVisibleRowIndex > 0) {
        val previous = currentFirstVisibleRowIndex - 1
        val measuredRow =
            applyScrollToRow(
                rowIndex = previous,
                rowOffset = currentFirstVisibleRowScrollOffset,
                firstVisibleColumnIndex = firstVisibleColumnIndex,
                firstVisibleColumnScrollOffset = firstVisibleColumnOffset,
                columns = columns,
                childConstraints = Constraints(),
                layoutMaxWidth = constraints.maxWidth,
                scrollToBeConsumed = state.scrollToBeConsumed.x,
                measuredItemProvider = measuredItemProvider,
            )
        measuredRows.add(0, measuredRow)
        maxWidthSize = maxOf(maxWidthSize, measuredRow.width)
        currentFirstVisibleRowScrollOffset += measuredRow.height
        currentFirstVisibleRowIndex = previous
    }

    // if we were scrolled backward, but there were not enough items before. this means
    // not the whole scroll was consumed
    if (currentFirstVisibleRowScrollOffset < 0) {
        val notConsumedScrollDelta = -currentFirstVisibleRowScrollOffset
        currentFirstVisibleRowScrollOffset = 0
        scrollDeltaRow -= notConsumedScrollDelta
    }

    var index = currentFirstVisibleRowIndex

    var currentLayoutHeightOffset = -currentFirstVisibleRowScrollOffset

    // first we need to skip items we already composed while composing backward
    var indexInVisibleItems = 0
    while (indexInVisibleItems < measuredRows.size) {
        if (currentLayoutHeightOffset >= maxOffset) {
            // this item is out of the bounds and will not be visible.
            measuredRows.removeAt(indexInVisibleItems)
        } else {
            index++
            currentLayoutHeightOffset += measuredRows[indexInVisibleItems].height
            indexInVisibleItems++
        }
    }

    // then composing visible items forward until we fill the whole viewport.
    // we want to have at least one item in visibleItems even if in fact all the items are
    // offscreen, this can happen if the content padding is larger than the available size.
    while (
        index < rows &&
            (currentLayoutHeightOffset < maxOffset ||
                currentLayoutHeightOffset <= 0 || // filling beforeContentPadding area
                measuredRows.isEmpty())
    ) {
        val measuredRow =
            applyScrollToRow(
                rowIndex = index,
                rowOffset = currentLayoutHeightOffset,
                firstVisibleColumnIndex = firstVisibleColumnIndex,
                firstVisibleColumnScrollOffset = firstVisibleColumnOffset,
                columns = columns,
                childConstraints = Constraints(),
                layoutMaxWidth = constraints.maxWidth,
                scrollToBeConsumed = state.scrollToBeConsumed.x,
                measuredItemProvider = measuredItemProvider,
            )
        currentLayoutHeightOffset += measuredRow.height

        if (currentLayoutHeightOffset <= 0 && index != rows - 1) {
            // this item is offscreen and will not be visible. advance firstVisibleItemIndex
            currentFirstVisibleRowIndex = index + 1
            currentFirstVisibleRowScrollOffset -= measuredRow.height
        } else {
            maxWidthSize = maxOf(maxWidthSize, measuredRow.width)
            measuredRows.add(measuredRow)
        }

        index++
    }

    if (currentLayoutHeightOffset < maxOffset) {
        val toScrollBack = maxOffset - currentLayoutHeightOffset
        currentFirstVisibleRowScrollOffset -= toScrollBack
        currentLayoutHeightOffset += toScrollBack
        while (currentFirstVisibleRowScrollOffset < 0 && currentFirstVisibleRowIndex > 0) {
            val previousIndex = currentFirstVisibleRowIndex - 1
            val measuredRow =
                applyScrollToRow(
                    scrollToBeConsumed = state.scrollToBeConsumed.x,
                    rowIndex = previousIndex,
                    rowOffset = currentLayoutHeightOffset,
                    firstVisibleColumnIndex = firstVisibleColumnIndex,
                    firstVisibleColumnScrollOffset = firstVisibleColumnOffset,
                    columns = columns,
                    childConstraints = Constraints(),
                    layoutMaxWidth = constraints.maxWidth,
                    measuredItemProvider = measuredItemProvider,
                )
            measuredRows.add(0, measuredRow)
            maxWidthSize = maxOf(maxWidthSize, measuredRow.width)
            currentFirstVisibleRowScrollOffset += measuredRow.height
            currentFirstVisibleRowIndex = previousIndex
        }
        scrollDeltaRow += toScrollBack
        if (currentFirstVisibleRowScrollOffset < 0) {
            scrollDeltaRow += currentFirstVisibleRowScrollOffset
            currentLayoutHeightOffset += currentFirstVisibleRowScrollOffset
            currentFirstVisibleRowScrollOffset = 0
        }
    }

    // report the amount of pixels we consumed. scrollDelta can be smaller than
    // scrollToBeConsumed if there were not enough items to fill the offered space or it
    // can be larger if items were resized, or if, for example, we were previously
    // displaying the item 15, but now we have only 10 items in total in the data set.
    val consumedScrollVertical =
        if (
            state.scrollToBeConsumed.y.fastRoundToInt().sign == scrollDeltaRow.sign &&
                abs(state.scrollToBeConsumed.y.fastRoundToInt()) >= abs(scrollDeltaRow)
        ) {
            scrollDeltaRow.toFloat()
        } else {
            state.scrollToBeConsumed.y
        }

    val layoutWidth = minOf(maxWidthSize, constraints.maxWidth)
    val layoutHeight = minOf(currentLayoutHeightOffset, constraints.maxHeight)

    val currentFirstColumnScrollOffset = measuredRows.first().firstColumnScrollOffset
    val currentFirstColumnIndex = measuredRows.first().firstColumnIndex
    val consumedScrollHorizontal = measuredRows.first().consumedScroll

    layout(layoutWidth, layoutHeight) {
            measuredRows.fastForEach {
                println(it)
                it.columns.fastForEach { item -> item.placeable.place(item.offset) }
            }
        }
        .also {
            updateScrollPosition(
                IntPosition(currentFirstVisibleRowIndex, currentFirstColumnIndex),
                IntOffset(
                    x = currentFirstColumnScrollOffset,
                    y = currentFirstVisibleRowScrollOffset,
                ),
                Offset(x = consumedScrollHorizontal, y = consumedScrollVertical),
            )
        }
}

@ExperimentalFoundationApi
private fun LazyLayoutMeasureScope.applyScrollToRow(
    scrollToBeConsumed: Float,
    rowIndex: Int,
    rowOffset: Int,
    firstVisibleColumnIndex: Int,
    firstVisibleColumnScrollOffset: Int,
    columns: Int,
    childConstraints: Constraints,
    layoutMaxWidth: Int,
    measuredItemProvider: LazyLayout2DGridMeasuredItemProvider,
): MeasuredRow {
    val measuredItems = mutableListOf<MeasuredItem>()

    var currentFirstVisibleColumnIndex = firstVisibleColumnIndex
    var currentFirstVisibleColumnScrollOffset = firstVisibleColumnScrollOffset
    if (currentFirstVisibleColumnIndex >= columns) {
        // the data set has been updated and now we have less items that we were
        // scrolled to before
        currentFirstVisibleColumnIndex = columns - 1
        currentFirstVisibleColumnScrollOffset = 0
    }

    // represents the real amount of scroll we applied as a result of this measure pass.
    var scrollDelta = scrollToBeConsumed.fastRoundToInt()

    // applying the whole requested scroll offset. we will figure out if we can't consume
    // all of it later
    currentFirstVisibleColumnScrollOffset -= scrollDelta

    // if the current scroll offset is less than minimally possible
    if (currentFirstVisibleColumnIndex == 0 && currentFirstVisibleColumnScrollOffset < 0) {
        scrollDelta += currentFirstVisibleColumnScrollOffset
        currentFirstVisibleColumnScrollOffset = 0
    }

    // define min and max offsets
    val maxOffset = layoutMaxWidth

    // tallest item in this row
    var maxHeightSize = 0

    // we had scrolled backward
    while (currentFirstVisibleColumnScrollOffset < 0 && currentFirstVisibleColumnIndex > 0) {
        val previous = currentFirstVisibleColumnIndex - 1

        val measuredItem =
            measureItem(
                rowIndex,
                previous,
                columns,
                childConstraints,
                currentFirstVisibleColumnScrollOffset,
                rowOffset,
                measuredItemProvider,
            )

        measuredItems.add(0, measuredItem)
        maxHeightSize = maxOf(maxHeightSize, measuredItem.height)
        currentFirstVisibleColumnScrollOffset += measuredItem.width
        currentFirstVisibleColumnIndex = previous
    }

    // if we were scrolled backward, but there were not enough items before. this means
    // not the whole scroll was consumed
    if (currentFirstVisibleColumnScrollOffset < 0) {
        val notConsumedScrollDelta = -currentFirstVisibleColumnScrollOffset
        currentFirstVisibleColumnScrollOffset = 0
        scrollDelta -= notConsumedScrollDelta
    }

    var index = currentFirstVisibleColumnIndex
    var currentLayoutWidthOffset = -currentFirstVisibleColumnScrollOffset

    // first we need to skip items we already composed while composing backward
    var indexInVisibleItems = 0
    while (indexInVisibleItems < measuredItems.size) {
        if (currentLayoutWidthOffset >= maxOffset) {
            // this item is out of the bounds and will not be visible.
            measuredItems.removeAt(indexInVisibleItems)
        } else {
            index++
            currentLayoutWidthOffset += measuredItems[indexInVisibleItems].placeable.width
            indexInVisibleItems++
        }
    }

    // then composing visible items forward until we fill the whole viewport.
    while (
        index < columns &&
            (currentLayoutWidthOffset < maxOffset ||
                currentLayoutWidthOffset <= 0 ||
                measuredItems.isEmpty())
    ) {
        val measuredItem =
            measureItem(
                rowIndex,
                index,
                columns,
                childConstraints,
                currentLayoutWidthOffset,
                rowOffset,
                measuredItemProvider,
            )
        currentLayoutWidthOffset += measuredItem.width

        if (currentLayoutWidthOffset <= 0 && index != columns - 1) {
            // this item is offscreen and will not be visible. advance firstVisibleItemIndex
            currentFirstVisibleColumnIndex = index + 1
            currentFirstVisibleColumnScrollOffset -= measuredItem.width
        } else {
            maxHeightSize = maxOf(maxHeightSize, measuredItem.height)
            measuredItems.add(measuredItem)
        }
        index++
    }

    // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
    // lets try to scroll back if we have enough items before firstVisibleItemIndex.
    if (currentLayoutWidthOffset < maxOffset) {
        val toScrollBack = maxOffset - currentLayoutWidthOffset
        currentFirstVisibleColumnScrollOffset -= toScrollBack
        currentLayoutWidthOffset += toScrollBack
        while (currentFirstVisibleColumnScrollOffset < 0 && currentFirstVisibleColumnIndex > 0) {
            val previousIndex = currentFirstVisibleColumnIndex - 1
            val measuredItem =
                measureItem(
                    rowIndex,
                    previousIndex,
                    columns,
                    childConstraints,
                    currentLayoutWidthOffset,
                    rowOffset,
                    measuredItemProvider,
                )
            measuredItems.add(0, measuredItem)
            maxHeightSize = maxOf(maxHeightSize, measuredItem.height)
            currentFirstVisibleColumnScrollOffset += measuredItem.width
            currentFirstVisibleColumnIndex = previousIndex
        }
        scrollDelta += toScrollBack
        if (currentFirstVisibleColumnScrollOffset < 0) {
            scrollDelta += currentFirstVisibleColumnScrollOffset
            currentLayoutWidthOffset += currentFirstVisibleColumnScrollOffset
            currentFirstVisibleColumnScrollOffset = 0
        }
    }

    // report the amount of pixels we consumed. scrollDelta can be smaller than
    // scrollToBeConsumed if there were not enough items to fill the offered space or it
    // can be larger if items were resized, or if, for example, we were previously
    // displaying the item 15, but now we have only 10 items in total in the data set.
    val consumedScroll =
        if (
            scrollToBeConsumed.fastRoundToInt().sign == scrollDelta.sign &&
                abs(scrollToBeConsumed.fastRoundToInt()) >= abs(scrollDelta)
        ) {
            scrollDelta.toFloat()
        } else {
            scrollToBeConsumed
        }

    return MeasuredRow(
        height = maxHeightSize,
        width = currentLayoutWidthOffset,
        columns = measuredItems,
        consumedScroll = consumedScroll,
        firstColumnIndex = currentFirstVisibleColumnIndex,
        firstColumnScrollOffset = currentFirstVisibleColumnScrollOffset,
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyLayoutMeasureScope.measureItem(
    rowIndex: Int,
    index: Int,
    columns: Int,
    childConstraints: Constraints,
    currentFirstColumnScrollOffset: Int,
    rowOffset: Int,
    measuredItemProvider: LazyLayout2DGridMeasuredItemProvider,
): MeasuredItem {
    val index = indexGenerator(rowIndex, index, columns)

    val itemPlaceables =
        measuredItemProvider.getPlaceables(
            scope = this,
            index = index,
            itemConstraints = childConstraints,
        )

    check(itemPlaceables.size == 1) { "Multi-item is not supported" }
    val item = itemPlaceables.first()
    // save placeable to be placed later.
    val measuredItem =
        MeasuredItem(
            IntPosition(rowIndex, index),
            item,
            IntOffset(currentFirstColumnScrollOffset, rowOffset),
        )
    return measuredItem
}

/** Class that caches [Placeable] to avoid repeatedly calling `measure()` on the same item. */
private class LazyLayout2DGridMeasuredItemProvider {
    private val placeablesCache = mutableIntObjectMapOf<List<Placeable>>()

    fun getPlaceables(
        scope: LazyLayoutMeasureScope,
        index: Int,
        itemConstraints: Constraints,
    ): List<Placeable> {
        val cachedPlaceable = placeablesCache[index]
        return if (cachedPlaceable != null) {
            cachedPlaceable
        } else {
            val placeables = scope.compose(index).map { it.measure(itemConstraints) }
            placeablesCache[index] = placeables
            placeables
        }
    }
}

class IntPosition(row: Int, column: Int) {
    private val backing = IntOffset(column, row)

    val row: Int
        get() = backing.y

    val column: Int
        get() = backing.x

    override fun toString(): String {
        return "IntPosition(row = $row, column = $column)"
    }
}

internal data class MeasuredItem(
    val position: IntPosition,
    val placeable: Placeable,
    val offset: IntOffset,
) {
    val height: Int
        get() = placeable.height

    val width: Int
        get() = placeable.width
}

private data class MeasuredRow(
    val height: Int,
    val width: Int,
    val columns: List<MeasuredItem>,
    val consumedScroll: Float,
    val firstColumnIndex: Int,
    val firstColumnScrollOffset: Int,
)

private fun indexGenerator(row: Int, column: Int, columnsCount: Int) = row * columnsCount + column

private fun decomposeIndex(index: Int, columnsCount: Int): Pair<Int, Int> =
    (index / columnsCount) to (index % columnsCount)
