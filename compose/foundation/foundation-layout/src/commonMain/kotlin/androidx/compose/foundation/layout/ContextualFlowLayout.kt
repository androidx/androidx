/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * [ContextualFlowRow] is a specialized version of the [FlowRow] layout. It is designed to
 * enable users to make contextual decisions during the construction of [FlowRow] layouts.
 *
 * This component is particularly advantageous when dealing with
 * a large collection of items, allowing for efficient management and display. Unlike traditional
 * [FlowRow] that composes all items regardless of their visibility, ContextualFlowRow smartly
 * limits composition to only those items that are visible within its constraints, such as
 * [maxLines] or `maxHeight`. This approach ensures optimal performance and resource utilization
 * by composing fewer items than the total number available, based on the current context and
 * display parameters.
 *
 * While maintaining the core functionality of the standard [FlowRow], [ContextualFlowRow]
 * operates on an index-based system and composes items sequentially, one after another.
 * This approach provides a perfect way to make contextual decisions and can be an easier way
 * to handle problems such as dynamic see more buttons such as (N+ buttons).
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.ContextualFlowRowMaxLineDynamicSeeMore
 *
 * @param modifier The modifier to be applied to the Row.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param verticalArrangement The vertical arrangement of the layout's virtual rows.
 * @param maxItemsInEachRow The maximum number of items per row
 * @param maxLines The maximum number of rows
 * @param overflow The strategy to handle overflowing items
 * @param itemCount The total number of item composable
 * @param content The indexed-based content of [ContextualFlowRowScope]
 *
 * @see FlowRow
 * @see ContextualFlowColumn
 */
@Composable
@ExperimentalLayoutApi
fun ContextualFlowRow(
    itemCount: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    overflow: ContextualFlowRowOverflow = ContextualFlowRowOverflow.Clip,
    content: @Composable ContextualFlowRowScope.(index: Int) -> Unit,
) {
    val overflowState = remember(overflow) {
        overflow.createOverflowState()
    }
    val list: List<@Composable () -> Unit> = remember(overflow) {
        val mutableList: MutableList<@Composable () -> Unit> = mutableListOf()
        overflow.addOverflowComposables(overflowState, mutableList)
        mutableList
    }
    val measurePolicy = contextualRowMeasurementHelper(
        horizontalArrangement,
        verticalArrangement,
        maxItemsInEachRow,
        maxLines,
        overflowState,
        itemCount,
        list
    ) { index ->
        ContextualFlowRowScopeInstance.content(index)
    }
    SubcomposeLayout(
        modifier = modifier,
        measurePolicy = measurePolicy
    )
}

/**
 * [ContextualFlowColumn] is a specialized version of the [FlowColumn] layout. It is designed to
 * enable users to make contextual decisions during the construction of [FlowColumn] layouts.
 *
 * This component is particularly advantageous when dealing with
 * a large collection of items, allowing for efficient management and display. Unlike traditional
 * [FlowColumn] that composes all items regardless of their visibility, ContextualFlowColumn smartly
 * limits composition to only those items that are visible within its constraints, such as
 * [maxLines] or `maxWidth`. This approach ensures optimal performance and resource utilization
 * by composing fewer items than the total number available, based on the current context and
 * display parameters.
 *
 * While maintaining the core functionality of the standard [FlowColumn], [ContextualFlowColumn]
 * operates on an index-based system and composes items sequentially, one after another.
 * This approach provides a perfect way to make contextual decisions and can be an easier way
 * to handle problems such as dynamic see more buttons such as (N+ buttons).
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.ContextualFlowColMaxLineDynamicSeeMore
 *
 * @param modifier The modifier to be applied to the Row.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param verticalArrangement The vertical arrangement of the layout's virtual column.
 * @param maxItemsInEachColumn The maximum number of items per column
 * @param maxLines The maximum number of columns
 * @param overflow The strategy to handle overflowing items
 * @param itemCount The total number of item composable
 * @param content The indexed-based content of [ContextualFlowColumnScope]
 *
 * @see FlowColumn
 * @see ContextualFlowRow
 */
@Composable
@ExperimentalLayoutApi
fun ContextualFlowColumn(
    itemCount: Int,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachColumn: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    overflow: ContextualFlowColumnOverflow = ContextualFlowColumnOverflow.Clip,
    content: @Composable ContextualFlowColumnScope.(index: Int) -> Unit,
) {
    val overflowState = remember(overflow) {
        overflow.createOverflowState()
    }
    val list: List<@Composable () -> Unit> = remember(overflow) {
        val mutableList: MutableList<@Composable () -> Unit> = mutableListOf()
        overflow.addOverflowComposables(overflowState, mutableList)
        mutableList
    }
    val measurePolicy = contextualColumnMeasureHelper(
        verticalArrangement,
        horizontalArrangement,
        maxItemsInEachColumn,
        maxLines,
        overflowState,
        itemCount,
        list,
    ) { index ->
        ContextualFlowColumnScopeInstance.content(index)
    }

    SubcomposeLayout(
        modifier = modifier,
        measurePolicy = measurePolicy
    )
}

/**
 * Scope for the children of [ContextualFlowRow].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface ContextualFlowRowScope : FlowRowScope

/**
 * Scope for the overflow [ContextualFlowRow].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface ContextualFlowRowOverflowScope : FlowRowOverflowScope

/**
 * Scope for the overflow [ContextualFlowColumn].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface ContextualFlowColumnOverflowScope : FlowColumnOverflowScope

/**
 * Scope for the children of [ContextualFlowColumn].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface ContextualFlowColumnScope : FlowColumnScope

@OptIn(ExperimentalLayoutApi::class)
internal object ContextualFlowRowScopeInstance :
    FlowRowScope by FlowRowScopeInstance, ContextualFlowRowScope

@OptIn(ExperimentalLayoutApi::class)
internal object ContextualFlowColumnScopeInstance : FlowColumnScope by FlowColumnScopeInstance,
    ContextualFlowColumnScope

@ExperimentalLayoutApi
internal class ContextualFlowRowOverflowScopeImpl(
    private val state: FlowLayoutOverflowState
) : FlowRowOverflowScope by FlowRowOverflowScopeImpl(state), ContextualFlowRowOverflowScope

@ExperimentalLayoutApi
internal class ContextualFlowColumnOverflowScopeImpl(
    private val state: FlowLayoutOverflowState
) : FlowColumnOverflowScope by FlowColumnOverflowScopeImpl(state), ContextualFlowColumnOverflowScope

@Composable
internal fun contextualRowMeasurementHelper(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflowState: FlowLayoutOverflowState,
    itemCount: Int,
    overflowComposables: List<@Composable () -> Unit>,
    getComposable: @Composable (
        index: Int
    ) -> Unit
): (SubcomposeMeasureScope, Constraints) -> MeasureResult {
    return remember(
        horizontalArrangement,
        verticalArrangement,
        maxItemsInMainAxis,
        maxLines,
        overflowState,
        itemCount,
        getComposable
    ) {
        FlowMeasureLazyPolicy(
            orientation = LayoutOrientation.Horizontal,
            horizontalArrangement = horizontalArrangement,
            mainAxisArrangementSpacing = horizontalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_TOP,
            verticalArrangement = verticalArrangement,
            crossAxisArrangementSpacing = verticalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis,
            itemCount = itemCount,
            overflow = overflowState,
            maxLines = maxLines,
            getComposable = getComposable,
            overflowComposables = overflowComposables
        ).getMeasurePolicy()
    }
}

@Composable
internal fun contextualColumnMeasureHelper(
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflowState: FlowLayoutOverflowState,
    itemCount: Int,
    overflowComposables: List<@Composable () -> Unit>,
    getComposable: @Composable (
        index: Int
    ) -> Unit
): (SubcomposeMeasureScope, Constraints) -> MeasureResult {
    return remember(
        verticalArrangement,
        horizontalArrangement,
        maxItemsInMainAxis,
        maxLines,
        overflowState,
        itemCount,
        getComposable
    ) {
        FlowMeasureLazyPolicy(
            orientation = LayoutOrientation.Vertical,
            verticalArrangement = verticalArrangement,
            mainAxisArrangementSpacing = verticalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_START,
            horizontalArrangement = horizontalArrangement,
            crossAxisArrangementSpacing = horizontalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis,
            itemCount = itemCount,
            overflow = overflowState,
            maxLines = maxLines,
            overflowComposables = overflowComposables,
            getComposable = getComposable
        ).getMeasurePolicy()
    }
}

/**
 * Returns a Flow Measure Policy
 */
@OptIn(ExperimentalLayoutApi::class)
private data class FlowMeasureLazyPolicy(
    private val orientation: LayoutOrientation,
    private val horizontalArrangement: Arrangement.Horizontal,
    private val verticalArrangement: Arrangement.Vertical,
    private val mainAxisArrangementSpacing: Dp,
    private val crossAxisSize: SizeMode,
    private val crossAxisAlignment: CrossAxisAlignment,
    private val crossAxisArrangementSpacing: Dp,
    private val itemCount: Int,
    private val maxLines: Int,
    private val maxItemsInMainAxis: Int,
    private val overflow: FlowLayoutOverflowState,
    private val overflowComposables: List<@Composable () -> Unit>,
    private val getComposable: @Composable (
        index: Int
    ) -> Unit
) {
    fun getMeasurePolicy(): (SubcomposeMeasureScope, Constraints) -> MeasureResult {
        return { measureScope, constraints ->
            measureScope.measure(constraints)
        }
    }

    private fun SubcomposeMeasureScope.measure(
        constraints: Constraints
    ): MeasureResult {
        if (itemCount <= 0 ||
            (maxLines == 0 ||
            maxItemsInMainAxis == 0 ||
            constraints.maxHeight == 0 &&
            overflow.type != FlowLayoutOverflow.OverflowType.Visible
                )) {
            return layout(0, 0) {}
        }
        val measurablesIterator = ContextualFlowItemIterator(
            itemCount
        ) { index ->
            this.subcompose(index) {
                getComposable(index)
            }
        }
        overflow.itemCount = itemCount
        overflow.setOverflowMeasurables(
            orientation,
            constraints
        ) { canExpand, noOfItemsShown ->
            val composableIndex = if (canExpand) 0 else 1
            overflowComposables.getOrNull(composableIndex)?.run {
                this@measure.subcompose("$canExpand$itemCount$noOfItemsShown",
                    this
                ).getOrNull(0)
            }
        }
        return breakDownItems(
            orientation,
            horizontalArrangement,
            verticalArrangement,
            crossAxisSize,
            crossAxisAlignment,
            measurablesIterator,
            constraints,
            maxItemsInMainAxis,
            maxLines,
            overflow
        )
    }
}

internal class ContextualFlowItemIterator(
    private val itemCount: Int,
    private val getMeasurables: (
        index: Int
    ) -> List<Measurable>
) : Iterator<Measurable> {
    private val _list: MutableList<Measurable> = mutableListOf()
    private var itemIndex: Int = 0
    private var listIndex = 0
    val list: List<Measurable> get() = _list

    override fun hasNext(): Boolean {
        return listIndex < list.size || itemIndex < itemCount
    }

    override fun next(): Measurable {
        // when we are at the end of the list, we fetch a new item from getMeasurables
        // and add to the list.
        // otherwise, we continue through the list.
        return if (listIndex < list.size) {
            val measurable = list[listIndex]
            listIndex++
            measurable
        } else if (itemIndex < itemCount) {
            val measurables = getMeasurables(itemIndex)
            itemIndex++
            if (measurables.isEmpty()) {
                next()
            } else {
                val measurable = measurables.first()
                _list.addAll(measurables)
                listIndex++
                measurable
            }
        } else {
            throw ArrayIndexOutOfBoundsException(
                "No item returned at index call. Index: $itemIndex"
            )
        }
    }
}
