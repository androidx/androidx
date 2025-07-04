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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.demos

import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.demos.LinearLazyLayoutState.LinearMeasureResult
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Placeable.PlacementScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

@Preview
@Composable
fun LinearLazyLayoutDemo() {
    val itemsCount = remember { mutableIntStateOf(10) }
    val verticalOrientation = remember { mutableStateOf(true) }
    val verticalArrangementState = remember { mutableStateOf(Arrangement.Top) }
    val horizontalArrangementState = remember { mutableStateOf(Arrangement.Start) }
    val layoutState = rememberLinearLazyLayoutState(verticalOrientation.value, itemsCount.intValue)

    Column {
        Row {
            OrientationSwitcher(verticalOrientation)
            Spacer(Modifier.weight(1f))
            if (verticalOrientation.value) {
                VerticalArrangementSwitcher(verticalArrangementState)
            } else {
                HorizontalArrangementSwitcher(horizontalArrangementState)
            }
        }
        ItemCounter(itemsCount)

        LinearLazyLayout(
            state = layoutState,
            itemCount = itemsCount.intValue,
            verticalArrangement = verticalArrangementState.value,
            horizontalArrangement = horizontalArrangementState.value,
            modifier = Modifier.background(Color.LightGray),
        ) { index ->
            Text(
                text = "Item-$index",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.background(Color.Yellow).border(1.dp, Color.Black).padding(8.dp),
            )
        }
    }
}

@Composable
private fun ItemCounter(itemCount: MutableIntState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(
            content = { Text(text = "-50", style = MaterialTheme.typography.h6) },
            onClick = { itemCount.intValue = maxOf(0, itemCount.intValue - 50) },
            shape = CircleShape,
        )
        OutlinedButton(
            content = { Text(text = "-1", style = MaterialTheme.typography.h6) },
            onClick = { itemCount.intValue = maxOf(0, itemCount.intValue - 1) },
            shape = CircleShape,
        )
        Text(
            text = itemCount.intValue.toString(),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp),
        )
        OutlinedButton(
            content = { Text(text = "+1", style = MaterialTheme.typography.h6) },
            onClick = { itemCount.intValue = maxOf(0, itemCount.intValue + 1) },
            shape = CircleShape,
        )
        OutlinedButton(
            content = { Text(text = "+50", style = MaterialTheme.typography.h6) },
            onClick = { itemCount.intValue = maxOf(0, itemCount.intValue + 50) },
            shape = CircleShape,
        )
    }
}

@Composable
private fun OrientationSwitcher(orientationState: MutableState<Boolean>) {
    SwitcherButton(
        text = if (orientationState.value) "Vertical" else "Horizontal",
        onClick = { orientationState.value = !orientationState.value },
    )
}

@Composable
private fun VerticalArrangementSwitcher(arrangementState: MutableState<Arrangement.Vertical>) {
    val arrangements: List<Arrangement.Vertical> =
        listOf(
            Arrangement.Top,
            Arrangement.Center,
            Arrangement.Bottom,
            Arrangement.SpaceAround,
            Arrangement.SpaceEvenly,
            Arrangement.SpaceBetween,
        )
    SwitcherButton(
        text = "Vertical${arrangementState.value}",
        onClick = {
            val index = arrangements.indexOfFirst { it == arrangementState.value }
            val nextIndex = (index + 1) % arrangements.size
            arrangementState.value = arrangements[nextIndex]
        },
    )
}

@Composable
private fun HorizontalArrangementSwitcher(arrangementState: MutableState<Arrangement.Horizontal>) {
    val arrangements: List<Arrangement.Horizontal> =
        listOf(
            Arrangement.Start,
            Arrangement.Center,
            Arrangement.End,
            Arrangement.SpaceAround,
            Arrangement.SpaceEvenly,
            Arrangement.SpaceBetween,
        )
    SwitcherButton(
        text = "Horizontal${arrangementState.value}",
        onClick = {
            val index = arrangements.indexOfFirst { it == arrangementState.value }
            val nextIndex = (index + 1) % arrangements.size
            arrangementState.value = arrangements[nextIndex]
        },
    )
}

@Composable
private fun SwitcherButton(text: String, onClick: () -> Unit) {
    Button(modifier = Modifier.padding(6.dp), onClick = onClick, content = { Text(text) })
}

@Composable
fun LinearLazyLayout(
    state: LinearLazyLayoutState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    itemContent: @Composable (index: Int) -> Unit,
) {
    val measurePolicy =
        rememberLinearLazyLayoutMeasurePolicy(state, verticalArrangement, horizontalArrangement)
    val itemProvider =
        remember(itemCount) {
            object : LazyLayoutItemProvider {
                override val itemCount: Int = itemCount

                @Composable
                override fun Item(index: Int, key: Any) {
                    itemContent(index)
                }
            }
        }
    LazyLayout(
        itemProvider = { itemProvider },
        modifier =
            modifier
                .scrollable(
                    state = state,
                    flingBehavior = flingBehavior,
                    orientation =
                        if (state.isVertical) Orientation.Vertical else Orientation.Horizontal,
                )
                .clipToBounds(),
        measurePolicy = measurePolicy,
    )
}

private class LinearLazyLayoutMeasurePolicy(
    private val state: LinearLazyLayoutState,
    private val verticalArrangement: Arrangement.Vertical,
    private val horizontalArrangement: Arrangement.Horizontal,
) : (LazyLayoutMeasureScope, Constraints) -> MeasureResult, OrientationAware by state {

    override fun invoke(scope: LazyLayoutMeasureScope, constraints: Constraints): MeasureResult {
        return scope.measure(constraints)
    }

    private fun LazyLayoutMeasureScope.measure(constraints: Constraints): MeasureResult {
        val sizeProvider =
            LinearLazyLayoutMeasuredItemProvider(
                isVertical = isVertical,
                layoutConstraints = constraints,
            )

        val measureResult =
            state.measureResult(
                scope = this,
                offset = state.offset.floatValue,
                layoutConstraints = constraints,
                sizeProvider = sizeProvider,
            )

        return if (measureResult.fullItemsSize < constraints.mainAxisMaxSize) {
            layoutWithSpareSpace(constraints, sizeProvider, measureResult)
        } else {
            layoutWithoutSpareSpace(constraints, sizeProvider, measureResult)
        }
    }

    private fun LazyLayoutMeasureScope.layoutWithSpareSpace(
        layoutConstraints: Constraints,
        sizeProvider: LinearLazyLayoutMeasuredItemProvider,
        measureResult: LinearMeasureResult,
    ): MeasureResult {
        // Calculate items positions respecting arrangement parameters.
        val layoutPositions =
            arrange(
                measureResult = measureResult,
                sizeProvider = sizeProvider,
                layoutConstraints = layoutConstraints,
            )
        // Place the items to draw.
        return layout(width = layoutConstraints.maxWidth, height = layoutConstraints.maxHeight) {
            val globalOffset = measureResult.firstVisibleOffset.fastRoundToInt()
            for (index in measureResult.itemIndices) {
                val innerPlaceables = sizeProvider.getPlaceables(this@layoutWithSpareSpace, index)
                val position = layoutPositions[index - measureResult.firstVisibleIndex]
                var localOffset = position + globalOffset
                for (placeable in innerPlaceables) {
                    placeRelativeMainAxis(placeable, main = localOffset, cross = 0)
                    localOffset += placeable.mainAxisSize
                }
            }
        }
    }

    private fun LazyLayoutMeasureScope.layoutWithoutSpareSpace(
        layoutConstraints: Constraints,
        sizeProvider: LinearLazyLayoutMeasuredItemProvider,
        measureResult: LinearMeasureResult,
    ): MeasureResult {
        // Simply place the items one-by-one.
        return layout(width = layoutConstraints.maxWidth, height = layoutConstraints.maxHeight) {
            var globalOffset = measureResult.firstVisibleOffset.fastRoundToInt()
            for (index in measureResult.itemIndices) {
                val innerPlaceables =
                    sizeProvider.getPlaceables(this@layoutWithoutSpareSpace, index)
                for (placeable in innerPlaceables) {
                    placeRelativeMainAxis(placeable, main = globalOffset, cross = 0)
                    globalOffset += placeable.mainAxisSize
                }
            }
        }
    }

    private fun LazyLayoutMeasureScope.arrange(
        measureResult: LinearMeasureResult,
        sizeProvider: LinearLazyLayoutMeasuredItemProvider,
        layoutConstraints: Constraints,
    ): IntArray {
        val inputSizes =
            IntArray(measureResult.itemsCount) { localIndex ->
                val itemIndex = measureResult.firstVisibleIndex + localIndex
                sizeProvider.requireMainAxisItemSize(itemIndex)
            }
        val outputPositions = IntArray(measureResult.itemsCount)
        if (isVertical) {
            with(verticalArrangement) {
                arrange(
                    totalSize = layoutConstraints.mainAxisMaxSize,
                    sizes = inputSizes,
                    outPositions = outputPositions,
                )
            }
        } else {
            with(horizontalArrangement) {
                arrange(
                    totalSize = layoutConstraints.mainAxisMaxSize,
                    sizes = inputSizes,
                    outPositions = outputPositions,
                    layoutDirection = LayoutDirection.Ltr,
                )
            }
        }
        return outputPositions
    }
}

class LinearLazyLayoutState(override val isVertical: Boolean, private val itemCount: Int) :
    ScrollableState, OrientationAware {

    private val scrollableState = ScrollableState { scrollDelta ->
        offset.floatValue = scrollDelta
        scrollDelta
    }

    private var firstVisibleIndex: Int = 0
    private var firstVisibleOffset: Float = 0f
    private var lastVisibleIndex: Int = -1
    private var fullItemsSize: Int = 0

    val offset = mutableFloatStateOf(0f)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        scrollableState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return scrollableState.dispatchRawDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    internal fun measureResult(
        scope: LazyLayoutMeasureScope,
        sizeProvider: LinearLazyLayoutMeasuredItemProvider,
        offset: Float,
        layoutConstraints: Constraints,
    ): LinearMeasureResult {
        if (itemCount == 0) {
            return emptyResult()
        }

        calculateFirstVisibleIndexAndItsOffset(scope, sizeProvider, offset)

        calculateLastVisibleIndexAndVisibleItemsSize(scope, sizeProvider, layoutConstraints)

        return captureCurrentMeasureResults()
    }

    /**
     * Find a new [firstVisibleIndex] and [firstVisibleOffset] depending on direction and value of
     * [offset].
     */
    private fun calculateFirstVisibleIndexAndItsOffset(
        scope: LazyLayoutMeasureScope,
        sizeProvider: LinearLazyLayoutMeasuredItemProvider,
        offset: Float,
    ) {
        var scroll = (-firstVisibleOffset - offset).fastRoundToInt()
        var index = firstVisibleIndex
        var allowedArea = sizeProvider.getMainAxisItemSize(scope, index)

        while (scroll !in 0..<allowedArea) {
            if (scroll < 0f) {
                if (index == 0) {
                    firstVisibleIndex = 0
                    firstVisibleOffset = 0f
                    return
                }
                --index
                allowedArea = sizeProvider.getMainAxisItemSize(scope, index)
                scroll += allowedArea
            } else {
                if (index == itemCount - 1) {
                    firstVisibleIndex = itemCount - 1
                    firstVisibleOffset = 0f
                }
                ++index
                scroll -= allowedArea
                allowedArea = sizeProvider.getMainAxisItemSize(scope, index)
            }
        }

        firstVisibleIndex = index
        firstVisibleOffset = -scroll.toFloat()
    }

    /**
     * Once the [firstVisibleIndex] and [firstVisibleOffset] are ready we can calculate how many
     * items can fit into viewport. As a result we will find [lastVisibleIndex] and [fullItemsSize].
     */
    private fun calculateLastVisibleIndexAndVisibleItemsSize(
        scope: LazyLayoutMeasureScope,
        sizeProvider: LinearLazyLayoutMeasuredItemProvider,
        layoutConstraints: Constraints,
    ) {
        var itemsSize = 0
        var visibleContentSize = firstVisibleOffset.fastRoundToInt()
        var lastNonVisibleIndex = firstVisibleIndex

        while (
            lastNonVisibleIndex < itemCount &&
                visibleContentSize < layoutConstraints.mainAxisMaxSize
        ) {
            val itemSize = sizeProvider.getMainAxisItemSize(scope, lastNonVisibleIndex)
            itemsSize += itemSize
            visibleContentSize += itemSize
            ++lastNonVisibleIndex
        }

        // Check if scroll reaches the end of the list.
        if (
            visibleContentSize < layoutConstraints.mainAxisMaxSize &&
                (firstVisibleIndex != 0 || firstVisibleOffset != 0f)
        ) {
            visibleContentSize -= firstVisibleOffset.fastRoundToInt()
            firstVisibleOffset = 0f
            while (
                firstVisibleIndex > 0 && visibleContentSize < layoutConstraints.mainAxisMaxSize
            ) {
                --firstVisibleIndex
                val topItemSize = sizeProvider.getMainAxisItemSize(scope, firstVisibleIndex)
                visibleContentSize += topItemSize
                itemsSize += topItemSize
            }
            if (visibleContentSize > layoutConstraints.mainAxisMaxSize) {
                firstVisibleOffset =
                    (layoutConstraints.mainAxisMaxSize - visibleContentSize).toFloat()
            }
        }

        lastVisibleIndex = lastNonVisibleIndex - 1
        fullItemsSize = itemsSize
    }

    private fun captureCurrentMeasureResults(): LinearMeasureResult {
        return LinearMeasureResult(
            firstVisibleIndex = firstVisibleIndex,
            firstVisibleOffset = firstVisibleOffset,
            lastVisibleIndex = lastVisibleIndex,
            fullItemsSize = fullItemsSize,
        )
    }

    private fun emptyResult(): LinearMeasureResult {
        return LinearMeasureResult(
            firstVisibleIndex = 0,
            firstVisibleOffset = 0f,
            lastVisibleIndex = -1,
            fullItemsSize = 0,
        )
    }

    internal data class LinearMeasureResult(
        val firstVisibleIndex: Int,
        val firstVisibleOffset: Float,
        val lastVisibleIndex: Int,
        val fullItemsSize: Int,
    ) {
        /** All visible indices for the current phase. */
        val itemIndices: IntRange = firstVisibleIndex..lastVisibleIndex
        /** Number of visible items. */
        val itemsCount: Int = lastVisibleIndex - firstVisibleIndex + 1
    }
}

/** Class that caches values during measuring phase. */
internal class LinearLazyLayoutMeasuredItemProvider(
    override val isVertical: Boolean,
    layoutConstraints: Constraints,
) : OrientationAware {

    /** Cache for [Placeable] to avoid repeatedly calling `measure()` on the same item. */
    private val placeablesCache: MutableIntObjectMap<List<Placeable>> = mutableIntObjectMapOf()

    /** Cached values for `List<Placeable>.sumOf { height or width }` */
    private val indexToSizeMap: MutableIntIntMap = MutableIntIntMap()

    internal val itemConstraints: Constraints =
        Constraints(
            minWidth = 0,
            minHeight = 0,
            maxWidth = if (isVertical) layoutConstraints.maxWidth else Constraints.Infinity,
            maxHeight = if (isVertical) Constraints.Infinity else layoutConstraints.maxHeight,
        )

    internal fun getMainAxisItemSize(scope: LazyLayoutMeasureScope, index: Int): Int {
        return indexToSizeMap.getOrPut(index) {
            val placeables = getPlaceables(scope = scope, index = index)
            placeables.sumOf { it.mainAxisSize }
        }
    }

    /** Use this method only for visible indices in [LinearMeasureResult.itemIndices]. */
    internal fun requireMainAxisItemSize(index: Int): Int {
        if (index in indexToSizeMap) {
            return indexToSizeMap[index]
        } else {
            error("Item at index=$index was not measured before layout.")
        }
    }

    internal fun getPlaceables(scope: LazyLayoutMeasureScope, index: Int): List<Placeable> {
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

internal interface OrientationAware {
    val isVertical: Boolean

    val Placeable.mainAxisSize: Int
        get() = if (isVertical) height else width

    val Placeable.crossAxisSize: Int
        get() = if (isVertical) width else height

    val Constraints.mainAxisMaxSize: Int
        get() = if (isVertical) maxHeight else maxWidth

    val Constraints.crossAxisMaxSize: Int
        get() = if (isVertical) maxWidth else maxHeight

    fun PlacementScope.placeRelativeMainAxis(placeable: Placeable, main: Int, cross: Int) {
        if (isVertical) {
            placeable.placeRelative(x = cross, y = main)
        } else {
            placeable.placeRelative(x = main, y = cross)
        }
    }
}

@Composable
private fun rememberLinearLazyLayoutMeasurePolicy(
    state: LinearLazyLayoutState,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
): LinearLazyLayoutMeasurePolicy {
    return remember(state, verticalArrangement, horizontalArrangement) {
        LinearLazyLayoutMeasurePolicy(
            state = state,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
        )
    }
}

@Composable
fun rememberLinearLazyLayoutState(isVertical: Boolean, itemCount: Int): LinearLazyLayoutState {
    return remember(isVertical, itemCount) { LinearLazyLayoutState(isVertical, itemCount) }
}
