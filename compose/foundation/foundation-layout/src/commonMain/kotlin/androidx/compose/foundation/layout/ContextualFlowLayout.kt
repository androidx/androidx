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

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.internal.requirePrecondition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * [ContextualFlowRow] is a specialized version of the [FlowRow] layout. It is designed to enable
 * users to make contextual decisions during the construction of [FlowRow] layouts.
 *
 * This component is particularly advantageous when dealing with a large collection of items,
 * allowing for efficient management and display. Unlike traditional [FlowRow] that composes all
 * items regardless of their visibility, ContextualFlowRow smartly limits composition to only those
 * items that are visible within its constraints, such as [maxLines] or `maxHeight`. This approach
 * ensures optimal performance and resource utilization by composing fewer items than the total
 * number available, based on the current context and display parameters.
 *
 * While maintaining the core functionality of the standard [FlowRow], [ContextualFlowRow] operates
 * on an index-based system and composes items sequentially, one after another. This approach
 * provides a perfect way to make contextual decisions and can be an easier way to handle problems
 * such as dynamic see more buttons such as (N+ buttons).
 *
 * Example:
 *
 * @sample androidx.compose.foundation.layout.samples.ContextualFlowRowMaxLineDynamicSeeMore
 * @param itemCount The total number of item composable
 * @param modifier The modifier to be applied to the Row.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param verticalArrangement The vertical arrangement of the layout's virtual rows.
 * @param itemVerticalAlignment The cross axis/vertical alignment of an item in the column.
 * @param maxItemsInEachRow The maximum number of items per row
 * @param maxLines The maximum number of rows
 * @param overflow The strategy to handle overflowing items
 * @param content The indexed-based content of [ContextualFlowRowScope]
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
    itemVerticalAlignment: Alignment.Vertical = Alignment.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    overflow: ContextualFlowRowOverflow = ContextualFlowRowOverflow.Clip,
    content: @Composable ContextualFlowRowScope.(index: Int) -> Unit,
) {
    val overflowState = remember(overflow) { overflow.createOverflowState() }
    val list: List<@Composable () -> Unit> =
        remember(overflow) {
            val mutableList: MutableList<@Composable () -> Unit> = mutableListOf()
            overflow.addOverflowComposables(overflowState, mutableList)
            mutableList
        }
    val measurePolicy =
        contextualRowMeasurementHelper(
            horizontalArrangement,
            verticalArrangement,
            itemVerticalAlignment,
            maxItemsInEachRow,
            maxLines,
            overflowState,
            itemCount,
            list
        ) { index, info ->
            val scope =
                ContextualFlowRowScopeImpl(
                    info.lineIndex,
                    info.positionInLine,
                    maxWidthInLine = info.maxMainAxisSize,
                    maxHeight = info.maxCrossAxisSize
                )
            scope.content(index)
        }
    SubcomposeLayout(modifier = modifier, measurePolicy = measurePolicy)
}

/**
 * [ContextualFlowColumn] is a specialized version of the [FlowColumn] layout. It is designed to
 * enable users to make contextual decisions during the construction of [FlowColumn] layouts.
 *
 * This component is particularly advantageous when dealing with a large collection of items,
 * allowing for efficient management and display. Unlike traditional [FlowColumn] that composes all
 * items regardless of their visibility, ContextualFlowColumn smartly limits composition to only
 * those items that are visible within its constraints, such as [maxLines] or `maxWidth`. This
 * approach ensures optimal performance and resource utilization by composing fewer items than the
 * total number available, based on the current context and display parameters.
 *
 * While maintaining the core functionality of the standard [FlowColumn], [ContextualFlowColumn]
 * operates on an index-based system and composes items sequentially, one after another. This
 * approach provides a perfect way to make contextual decisions and can be an easier way to handle
 * problems such as dynamic see more buttons such as (N+ buttons).
 *
 * Example:
 *
 * @sample androidx.compose.foundation.layout.samples.ContextualFlowColMaxLineDynamicSeeMore
 * @param itemCount The total number of item composable
 * @param modifier The modifier to be applied to the Row.
 * @param verticalArrangement The vertical arrangement of the layout's virtual column.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param itemHorizontalAlignment The cross axis/horizontal alignment of an item in the column.
 * @param maxItemsInEachColumn The maximum number of items per column
 * @param maxLines The maximum number of columns
 * @param overflow The straoadtegy to handle overflowing items
 * @param content The indexed-based content of [ContextualFlowColumnScope]
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
    itemHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    maxItemsInEachColumn: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    overflow: ContextualFlowColumnOverflow = ContextualFlowColumnOverflow.Clip,
    content: @Composable ContextualFlowColumnScope.(index: Int) -> Unit,
) {
    val overflowState = remember(overflow) { overflow.createOverflowState() }
    val list: List<@Composable () -> Unit> =
        remember(overflow) {
            val mutableList: MutableList<@Composable () -> Unit> = mutableListOf()
            overflow.addOverflowComposables(overflowState, mutableList)
            mutableList
        }
    val measurePolicy =
        contextualColumnMeasureHelper(
            verticalArrangement,
            horizontalArrangement,
            itemHorizontalAlignment,
            maxItemsInEachColumn,
            maxLines,
            overflowState,
            itemCount,
            list,
        ) { index, info ->
            val scope =
                ContextualFlowColumnScopeImpl(
                    info.lineIndex,
                    info.positionInLine,
                    maxHeightInLine = info.maxMainAxisSize,
                    maxWidth = info.maxCrossAxisSize
                )
            scope.content(index)
        }

    SubcomposeLayout(modifier = modifier, measurePolicy = measurePolicy)
}

/** Defines the scope for items within a [ContextualFlowRow]. */
@LayoutScopeMarker
@Stable
@ExperimentalLayoutApi
interface ContextualFlowRowScope : RowScope {
    /**
     * Have the item fill (possibly only partially) the max height of the tallest item in the row it
     * was placed in, within the [FlowRow].
     *
     * @param fraction The fraction of the max height of the tallest item between `0` and `1`,
     *   inclusive.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleFlowRow_EqualHeight
     */
    @ExperimentalLayoutApi
    fun Modifier.fillMaxRowHeight(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f,
    ): Modifier

    /**
     * Identifies the row or column index where the UI component(s) are to be placed, provided they
     * do not exceed the specified [maxWidthInLine] and [maxHeight] for that row or column.
     *
     * Should the component(s) surpass these dimensions, their placement may shift to the subsequent
     * row/column or they may be omitted from display, contingent upon the defined constraints.
     *
     * Example:
     *
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowRow_ItemPosition
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowColumn_ItemPosition
     */
    val lineIndex: Int

    /**
     * Marks the index within the current row/column where the next component is to be inserted,
     * assuming it conforms to the row's or column's [maxWidthInLine] and [maxHeight] limitations.
     *
     * In scenarios where multiple UI components are returned in one index call, this parameter is
     * relevant solely to the first returned UI component, presuming it complies with the row's or
     * column's defined constraints.
     *
     * Example:
     *
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowRow_ItemPosition
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowColumn_ItemPosition
     */
    val indexInLine: Int

    /**
     * Specifies the maximum permissible width (main-axis) for the upcoming UI component at the
     * given [lineIndex] and [indexInLine]. Exceeding this width may result in the component being
     * reallocated to the following row within the [ContextualFlowRow] structure, subject to
     * existing constraints.
     */
    val maxWidthInLine: Dp

    /**
     * Determines the maximum allowable height (cross-axis) for the forthcoming UI component,
     * aligned with its [lineIndex] and [indexInLine]. Should this height threshold be exceeded, the
     * component's visibility will depend on the overflow settings, potentially leading to its
     * exclusion.
     */
    val maxHeight: Dp
}

/** Scope for the overflow [ContextualFlowRow]. */
@LayoutScopeMarker
@Stable
@ExperimentalLayoutApi
interface ContextualFlowRowOverflowScope : FlowRowOverflowScope

/** Scope for the overflow [ContextualFlowColumn]. */
@LayoutScopeMarker
@Stable
@ExperimentalLayoutApi
interface ContextualFlowColumnOverflowScope : FlowColumnOverflowScope

/** Provides a scope for items within a [ContextualFlowColumn]. */
@LayoutScopeMarker
@Stable
@ExperimentalLayoutApi
interface ContextualFlowColumnScope : ColumnScope {
    /**
     * Have the item fill (possibly only partially) the max width of the widest item in the column
     * it was placed in, within the [FlowColumn].
     *
     * @param fraction The fraction of the max width of the widest item between `0` and `1`,
     *   inclusive.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumn_EqualWidth
     */
    @ExperimentalLayoutApi
    fun Modifier.fillMaxColumnWidth(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f,
    ): Modifier

    /**
     * Identifies the row or column index where the UI component(s) are to be placed, provided they
     * do not exceed the specified [maxWidth] and [maxHeightInLine] for that row or column.
     *
     * Should the component(s) surpass these dimensions, their placement may shift to the subsequent
     * row/column or they may be omitted from display, contingent upon the defined constraints.
     *
     * Example:
     *
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowRow_ItemPosition
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowColumn_ItemPosition
     */
    val lineIndex: Int

    /**
     * Marks the index within the current row/column where the next component is to be inserted,
     * assuming it conforms to the row's or column's [maxWidth] and [maxHeightInLine] limitations.
     *
     * In scenarios where multiple UI components are returned in one index call, this parameter is
     * relevant solely to the first returned UI component, presuming it complies with the row's or
     * column's defined constraints.
     *
     * Example:
     *
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowRow_ItemPosition
     * @sample androidx.compose.foundation.layout.samples.ContextualFlowColumn_ItemPosition
     */
    val indexInLine: Int

    /**
     * Sets the maximum width (cross-axis dimension) that the upcoming UI component can occupy,
     * based on its [lineIndex] and [indexInLine]. Exceeding this width might result in the
     * component not being displayed, depending on the [ContextualFlowColumnOverflow.Visible]
     * overflow configuration.
     */
    val maxWidth: Dp

    /**
     * Establishes the maximum height (main-axis dimension) permissible for the next UI component,
     * aligned with its [lineIndex] and [indexInLine]. Should the component's height exceed this
     * limit, it may be shifted to the subsequent column in [ContextualFlowColumn], subject to the
     * predefined constraints.
     */
    val maxHeightInLine: Dp
}

@OptIn(ExperimentalLayoutApi::class)
internal class ContextualFlowRowScopeImpl(
    override val lineIndex: Int,
    override val indexInLine: Int,
    override val maxWidthInLine: Dp,
    override val maxHeight: Dp
) : RowScope by RowScopeInstance, ContextualFlowRowScope {
    override fun Modifier.fillMaxRowHeight(fraction: Float): Modifier {
        requirePrecondition(fraction >= 0.0f && fraction <= 1.0f) {
            "invalid fraction $fraction; must be >= 0 and <= 1.0"
        }
        return this.then(
            FillCrossAxisSizeElement(
                fraction = fraction,
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
internal class ContextualFlowColumnScopeImpl(
    override val lineIndex: Int,
    override val indexInLine: Int,
    override val maxWidth: Dp,
    override val maxHeightInLine: Dp
) : ColumnScope by ColumnScopeInstance, ContextualFlowColumnScope {
    override fun Modifier.fillMaxColumnWidth(fraction: Float): Modifier {
        requirePrecondition(fraction >= 0.0f && fraction <= 1.0f) {
            "invalid fraction $fraction; must be >= 0 and <= 1.0"
        }
        return this.then(
            FillCrossAxisSizeElement(
                fraction = fraction,
            )
        )
    }
}

@ExperimentalLayoutApi
internal class ContextualFlowRowOverflowScopeImpl(private val state: FlowLayoutOverflowState) :
    FlowRowOverflowScope by FlowRowOverflowScopeImpl(state), ContextualFlowRowOverflowScope

@ExperimentalLayoutApi
internal class ContextualFlowColumnOverflowScopeImpl(private val state: FlowLayoutOverflowState) :
    FlowColumnOverflowScope by FlowColumnOverflowScopeImpl(state),
    ContextualFlowColumnOverflowScope

@Composable
internal fun contextualRowMeasurementHelper(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    itemVerticalAlignment: Alignment.Vertical,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflowState: FlowLayoutOverflowState,
    itemCount: Int,
    overflowComposables: List<@Composable () -> Unit>,
    getComposable: @Composable (index: Int, info: FlowLineInfo) -> Unit
): (SubcomposeMeasureScope, Constraints) -> MeasureResult {
    return remember(
        horizontalArrangement,
        verticalArrangement,
        itemVerticalAlignment,
        maxItemsInMainAxis,
        maxLines,
        overflowState,
        itemCount,
        getComposable
    ) {
        FlowMeasureLazyPolicy(
                isHorizontal = true,
                horizontalArrangement = horizontalArrangement,
                mainAxisSpacing = horizontalArrangement.spacing,
                crossAxisAlignment = CrossAxisAlignment.vertical(itemVerticalAlignment),
                verticalArrangement = verticalArrangement,
                crossAxisArrangementSpacing = verticalArrangement.spacing,
                maxItemsInMainAxis = maxItemsInMainAxis,
                itemCount = itemCount,
                overflow = overflowState,
                maxLines = maxLines,
                getComposable = getComposable,
                overflowComposables = overflowComposables
            )
            .getMeasurePolicy()
    }
}

@Composable
internal fun contextualColumnMeasureHelper(
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    itemHorizontalAlignment: Alignment.Horizontal,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflowState: FlowLayoutOverflowState,
    itemCount: Int,
    overflowComposables: List<@Composable () -> Unit>,
    getComposable: @Composable (index: Int, info: FlowLineInfo) -> Unit
): (SubcomposeMeasureScope, Constraints) -> MeasureResult {
    return remember(
        verticalArrangement,
        horizontalArrangement,
        itemHorizontalAlignment,
        maxItemsInMainAxis,
        maxLines,
        overflowState,
        itemCount,
        getComposable
    ) {
        FlowMeasureLazyPolicy(
                isHorizontal = false,
                verticalArrangement = verticalArrangement,
                mainAxisSpacing = verticalArrangement.spacing,
                crossAxisAlignment = CrossAxisAlignment.horizontal(itemHorizontalAlignment),
                horizontalArrangement = horizontalArrangement,
                crossAxisArrangementSpacing = horizontalArrangement.spacing,
                maxItemsInMainAxis = maxItemsInMainAxis,
                itemCount = itemCount,
                overflow = overflowState,
                maxLines = maxLines,
                overflowComposables = overflowComposables,
                getComposable = getComposable
            )
            .getMeasurePolicy()
    }
}

/** Returns a Flow Measure Policy */
@OptIn(ExperimentalLayoutApi::class)
private data class FlowMeasureLazyPolicy(
    override val isHorizontal: Boolean,
    override val horizontalArrangement: Arrangement.Horizontal,
    override val verticalArrangement: Arrangement.Vertical,
    private val mainAxisSpacing: Dp,
    override val crossAxisAlignment: CrossAxisAlignment,
    private val crossAxisArrangementSpacing: Dp,
    private val itemCount: Int,
    private val maxLines: Int,
    private val maxItemsInMainAxis: Int,
    private val overflow: FlowLayoutOverflowState,
    private val overflowComposables: List<@Composable () -> Unit>,
    private val getComposable: @Composable (index: Int, info: FlowLineInfo) -> Unit
) : FlowLineMeasurePolicy {

    fun getMeasurePolicy(): (SubcomposeMeasureScope, Constraints) -> MeasureResult {
        return { measureScope, constraints -> measureScope.measure(constraints) }
    }

    private fun SubcomposeMeasureScope.measure(constraints: Constraints): MeasureResult {
        if (
            itemCount <= 0 ||
                (maxLines == 0 ||
                    maxItemsInMainAxis == 0 ||
                    constraints.maxHeight == 0 &&
                        overflow.type != FlowLayoutOverflow.OverflowType.Visible)
        ) {
            return layout(0, 0) {}
        }
        val measurablesIterator =
            ContextualFlowItemIterator(itemCount) { index, info ->
                this.subcompose(index) { getComposable(index, info) }
            }
        overflow.itemCount = itemCount
        overflow.setOverflowMeasurables(this@FlowMeasureLazyPolicy, constraints) {
            canExpand,
            shownItemCount ->
            val composableIndex = if (canExpand) 0 else 1
            overflowComposables.getOrNull(composableIndex)?.run {
                this@measure.subcompose("$canExpand$itemCount$shownItemCount", this).getOrNull(0)
            }
        }
        return breakDownItems(
            this@FlowMeasureLazyPolicy,
            measurablesIterator,
            mainAxisSpacing,
            crossAxisArrangementSpacing,
            OrientationIndependentConstraints(
                constraints,
                if (isHorizontal) {
                    LayoutOrientation.Horizontal
                } else {
                    LayoutOrientation.Vertical
                }
            ),
            maxItemsInMainAxis,
            maxLines,
            overflow
        )
    }
}

internal class ContextualFlowItemIterator(
    private val itemCount: Int,
    private val getMeasurables: (index: Int, info: FlowLineInfo) -> List<Measurable>
) : Iterator<Measurable> {
    private val _list: MutableList<Measurable> = mutableListOf()
    private var itemIndex: Int = 0
    private var listIndex = 0
    val list: List<Measurable>
        get() = _list

    override fun hasNext(): Boolean {
        return listIndex < list.size || itemIndex < itemCount
    }

    override fun next(): Measurable {
        return getNext()
    }

    internal fun getNext(info: FlowLineInfo = FlowLineInfo()): Measurable {
        // when we are at the end of the list, we fetch a new item from getMeasurables
        // and add to the list.
        // otherwise, we continue through the list.
        return if (listIndex < list.size) {
            val measurable = list[listIndex]
            listIndex++
            measurable
        } else if (itemIndex < itemCount) {
            val measurables = getMeasurables(itemIndex, info)
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
            throw IndexOutOfBoundsException("No item returned at index call. Index: $itemIndex")
        }
    }
}

/**
 * Contextual Line Info for the current lazy call for [ContextualFlowRow] or [ContextualFlowColumn]
 */
internal class FlowLineInfo(
    internal var lineIndex: Int = 0,
    internal var positionInLine: Int = 0,
    internal var maxMainAxisSize: Dp = 0.dp,
    internal var maxCrossAxisSize: Dp = 0.dp,
) {

    /** To allow reuse of the same object to reduce allocation, simply update the same value */
    internal fun update(
        lineIndex: Int,
        positionInLine: Int,
        maxMainAxisSize: Dp,
        maxCrossAxisSize: Dp,
    ) {
        this.lineIndex = lineIndex
        this.positionInLine = positionInLine
        this.maxMainAxisSize = maxMainAxisSize
        this.maxCrossAxisSize = maxCrossAxisSize
    }
}
