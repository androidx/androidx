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
import androidx.collection.IntIntPair
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * [FlowRow] is a layout that fills items from left to right (ltr) in LTR layouts
 * or right to left (rtl) in RTL layouts and when it runs out of space, moves to
 * the next "row" or "line" positioned on the bottom, and then continues filling items
 * until the items run out.
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowRow
 *
 * When a Modifier [RowScope.weight] is provided, it scales the item
 * based on the number items that fall on the row it was placed in.
 *
 * Note that if two or more Text components are placed in a [Row], normally they should be aligned
 * by their first baselines. [FlowRow] as a general purpose container does not do it automatically
 * so developers need to handle this manually. This is achieved by adding a
 * [RowScope.alignByBaseline] modifier to every such Text component. By default this modifier
 * aligns by [androidx.compose.ui.layout.FirstBaseline]. If, however, you need to align Texts
 * by [androidx.compose.ui.layout.LastBaseline] for example, use a more general [RowScope.alignBy]
 * modifier.
 *
 * @param modifier The modifier to be applied to the Row.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param verticalArrangement The vertical arrangement of the layout's virtual rows.
 * @param maxItemsInEachRow The maximum number of items per row
 * @param maxLines The max number of rows
 * @param overflow The strategy to handle overflowing items
 * @param content The content as a [RowScope]
 *
 * @see FlowColumn
 * @see ContextualFlowRow
 * @see [androidx.compose.foundation.layout.Row]
 */
@Composable
@ExperimentalLayoutApi
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    overflow: FlowRowOverflow = FlowRowOverflow.Clip,
    content: @Composable FlowRowScope.() -> Unit
) {
    val overflowState = remember(overflow) {
        overflow.createOverflowState()
    }
    val measurePolicy = rowMeasurementMultiContentHelper(
        horizontalArrangement,
        verticalArrangement,
        maxItemsInEachRow,
        maxLines,
        overflowState
    )
    val list: List<@Composable () -> Unit> = remember(overflow, content) {
        val mutableList: MutableList<@Composable () -> Unit> = mutableListOf()
        mutableList.add { FlowRowScopeInstance.content() }
        overflow.addOverflowComposables(overflowState, mutableList)
        mutableList
    }

    Layout(
        contents = list,
        measurePolicy = measurePolicy,
        modifier = modifier
    )
}

/**
 * [FlowColumn] is a layout that fills items from top to bottom, and when it runs out of space
 * on the bottom, moves to the next "column" or "line"
 * on the right or left based on ltr or rtl layouts,
 * and then continues filling items from top to bottom.
 *
 * It supports ltr in LTR layouts, by placing the first column to the left, and then moving
 * to the right
 * It supports rtl in RTL layouts, by placing the first column to the right, and then moving
 * to the left
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumn
 *
 * When a Modifier [ColumnScope.weight] is provided, it scales the item
 * based on the number items that fall on the column it was placed in.
 *
 * @param modifier The modifier to be applied to the Row.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalArrangement The horizontal arrangement of the layout's virtual columns
 * @param maxItemsInEachColumn The maximum number of items per column
 * @param maxLines The max number of rows
 * @param overflow The strategy to handle overflowing items
 * @param content The content as a [ColumnScope]
 *
 * @see FlowRow
 * @see ContextualFlowColumn
 * @see [androidx.compose.foundation.layout.Column]
 */
@Composable
@ExperimentalLayoutApi
fun FlowColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachColumn: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    overflow: FlowColumnOverflow = FlowColumnOverflow.Clip,
    content: @Composable FlowColumnScope.() -> Unit
) {
    val overflowState = remember(overflow) {
        overflow.createOverflowState()
    }
    val measurePolicy = columnMeasurementMultiContentHelper(
        verticalArrangement,
        horizontalArrangement,
        maxItemsInEachColumn,
        maxLines,
        overflowState
    )
    val list: List<@Composable () -> Unit> = remember(overflow, content) {
        val mutableList: MutableList<@Composable () -> Unit> = mutableListOf()
        mutableList.add { FlowColumnScopeInstance.content() }
        overflow.addOverflowComposables(overflowState, mutableList)
        mutableList
    }
    Layout(
        contents = list,
        measurePolicy = measurePolicy,
        modifier = modifier
    )
}

/**
 * Scope for the children of [FlowRow].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface FlowRowScope : RowScope {
    /**
     * Have the item fill (possibly only partially) the max height of the tallest item in the
     * row it was placed in, within the [FlowRow].
     *
     * @param fraction The fraction of the max height of the tallest item
     * between `0` and `1`, inclusive.
     *
     * Example usage:
     * @sample androidx.compose.foundation.layout.samples.SimpleFlowRow_EqualHeight
     */
    @ExperimentalLayoutApi
    fun Modifier.fillMaxRowHeight(
        @FloatRange(from = 0.0, to = 1.0)
        fraction: Float = 1f,
    ): Modifier
}

/**
 * Scope for the overflow [FlowRow].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface FlowRowOverflowScope : FlowRowScope {
    /**
    * Total Number of Items available to show in [FlowRow]
    * This includes items that may not be displayed.
    *
    * In [ContextualFlowRow], this matches the
    * [ContextualFlowRow]'s `itemCount` parameter
    */
    @ExperimentalLayoutApi
    val totalItemCount: Int

    /**
     * Total Number of Items displayed in the [FlowRow]
     */
    @ExperimentalLayoutApi
    val shownItemCount: Int
}

/**
 * Scope for the children of [FlowColumn].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface FlowColumnScope : ColumnScope {
    /**
     * Have the item fill (possibly only partially) the max width of the tallest item in the
     * column it was placed in, within the [FlowColumn].
     *
     * @param fraction The fraction of the max width of the tallest item
     * between `0` and `1`, inclusive.
     *
     * Example usage:
     * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumn_EqualWidth
     */
    @ExperimentalLayoutApi
    fun Modifier.fillMaxColumnWidth(
        @FloatRange(from = 0.0, to = 1.0)
        fraction: Float = 1f,
    ): Modifier
}

/**
 * Scope for the overflow [FlowColumn].
 */
@LayoutScopeMarker
@Immutable
@ExperimentalLayoutApi
interface FlowColumnOverflowScope : FlowColumnScope {
    /**
     * Total Number of Items available to show in [FlowColumn]
     * This includes items that may not be displayed.
     *
     * In [ContextualFlowColumn], this matches the
     * [ContextualFlowColumn]'s `itemCount` parameter
     */
    @ExperimentalLayoutApi
    val totalItemCount: Int

    /**
     * Total Number of Items displayed in the [FlowColumn]
     */
    @ExperimentalLayoutApi
    val shownItemCount: Int
}

@OptIn(ExperimentalLayoutApi::class)
internal object FlowRowScopeInstance : RowScope by RowScopeInstance, FlowRowScope {
    override fun Modifier.fillMaxRowHeight(fraction: Float): Modifier {
        require(fraction >= 0.0) { "invalid fraction $fraction; must be greater than " +
            "or equal to zero" }
        require(fraction <= 1.0) { "invalid fraction $fraction; must not be greater " +
            "than 1.0" }
        return this.then(
            FillCrossAxisSizeElement(
                fraction = fraction,
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
internal class FlowRowOverflowScopeImpl(
    private val state: FlowLayoutOverflowState
) : FlowRowScope by FlowRowScopeInstance, FlowRowOverflowScope {
    override val totalItemCount: Int
        get() = state.itemCount

    override val shownItemCount: Int
        get() = state.noOfItemsShown
}

@OptIn(ExperimentalLayoutApi::class)
internal class FlowColumnOverflowScopeImpl(
    private val state: FlowLayoutOverflowState
) : FlowColumnScope by FlowColumnScopeInstance, FlowColumnOverflowScope {
    override val totalItemCount: Int
        get() = state.itemCount

    override val shownItemCount: Int
        get() = state.noOfItemsShown
}

@OptIn(ExperimentalLayoutApi::class)
internal object FlowColumnScopeInstance : ColumnScope by ColumnScopeInstance, FlowColumnScope {
    override fun Modifier.fillMaxColumnWidth(fraction: Float): Modifier {
        require(fraction >= 0.0) { "invalid fraction $fraction; must be greater than or " +
            "equal to zero" }
        require(fraction <= 1.0) { "invalid fraction $fraction; must not be greater " +
            "than 1.0" }
        return this.then(
            FillCrossAxisSizeElement(
                fraction = fraction,
            )
        )
    }
}

internal data class FlowLayoutData(
    var fillCrossAxisFraction: Float
)

internal class FillCrossAxisSizeNode(
    var fraction: Float,
) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
            it.flowLayoutData = it.flowLayoutData ?: FlowLayoutData(fraction)
            it.flowLayoutData!!.fillCrossAxisFraction = fraction
        }
}

internal class FillCrossAxisSizeElement(
    val fraction: Float
) : ModifierNodeElement<FillCrossAxisSizeNode>() {
    override fun create(): FillCrossAxisSizeNode {
        return FillCrossAxisSizeNode(fraction)
    }

    override fun update(node: FillCrossAxisSizeNode) {
        node.fraction = fraction
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "fraction"
        value = fraction
        properties["fraction"] = fraction
    }

    override fun hashCode(): Int {
        var result = fraction.hashCode()
        result *= 31
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? FillCrossAxisSizeNode ?: return false
        return fraction == otherModifier.fraction
    }
}

@OptIn(ExperimentalLayoutApi::class)
@PublishedApi
@Composable
internal fun rowMeasurementHelper(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    maxItemsInMainAxis: Int,
): MeasurePolicy {
    return remember(
        horizontalArrangement,
        verticalArrangement,
        maxItemsInMainAxis,
    ) {
        val measurePolicy = FlowMeasurePolicy(
            orientation = LayoutOrientation.Horizontal,
            horizontalArrangement = horizontalArrangement,
            mainAxisArrangementSpacing = horizontalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_TOP,
            verticalArrangement = verticalArrangement,
            crossAxisArrangementSpacing = verticalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis,
            maxLines = Int.MAX_VALUE,
            overflow = FlowRowOverflow.Visible.createOverflowState()
        ) as MultiContentMeasurePolicy

        MeasurePolicy { measurables, constraints ->
            with(measurePolicy) {
                this@MeasurePolicy.measure(listOf(measurables), constraints)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rowMeasurementMultiContentHelper(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflowState: FlowLayoutOverflowState,
): MultiContentMeasurePolicy {
    return remember(
        horizontalArrangement,
        verticalArrangement,
        maxItemsInMainAxis,
        maxLines,
        overflowState
    ) {
        FlowMeasurePolicy(
            orientation = LayoutOrientation.Horizontal,
            horizontalArrangement = horizontalArrangement,
            mainAxisArrangementSpacing = horizontalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_TOP,
            verticalArrangement = verticalArrangement,
            crossAxisArrangementSpacing = verticalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis,
            maxLines = maxLines,
            overflow = overflowState
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@PublishedApi
@Composable
internal fun columnMeasurementHelper(
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    maxItemsInMainAxis: Int,
): MeasurePolicy {
    return remember(
        verticalArrangement,
        horizontalArrangement,
        maxItemsInMainAxis,
    ) {
        val measurePolicy = FlowMeasurePolicy(
            orientation = LayoutOrientation.Vertical,
            verticalArrangement = verticalArrangement,
            mainAxisArrangementSpacing = verticalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_START,
            horizontalArrangement = horizontalArrangement,
            crossAxisArrangementSpacing = horizontalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis,
            maxLines = Int.MAX_VALUE,
            overflow = FlowRowOverflow.Visible.createOverflowState()
        )
        MeasurePolicy { measurables, constraints ->
            with(measurePolicy) {
                this@MeasurePolicy.measure(listOf(measurables), constraints)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun columnMeasurementMultiContentHelper(
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflowState: FlowLayoutOverflowState
): MultiContentMeasurePolicy {
    return remember(
        verticalArrangement,
        horizontalArrangement,
        maxItemsInMainAxis,
        maxLines,
        overflowState
    ) {
        FlowMeasurePolicy(
            orientation = LayoutOrientation.Vertical,
            verticalArrangement = verticalArrangement,
            mainAxisArrangementSpacing = verticalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_START,
            horizontalArrangement = horizontalArrangement,
            crossAxisArrangementSpacing = horizontalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis,
            maxLines = maxLines,
            overflow = overflowState
        )
    }
}

/**
 * Returns a Flow Measure Policy
 */
@OptIn(ExperimentalLayoutApi::class)
private data class FlowMeasurePolicy(
    private val orientation: LayoutOrientation,
    private val horizontalArrangement: Arrangement.Horizontal,
    private val verticalArrangement: Arrangement.Vertical,
    private val mainAxisArrangementSpacing: Dp,
    private val crossAxisSize: SizeMode,
    private val crossAxisAlignment: CrossAxisAlignment,
    private val crossAxisArrangementSpacing: Dp,
    private val maxItemsInMainAxis: Int,
    private val maxLines: Int,
    private val overflow: FlowLayoutOverflowState,
) : MultiContentMeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        if (maxLines == 0 || maxItemsInMainAxis == 0 || measurables.isEmpty() ||
            constraints.maxHeight == 0 && overflow.type != FlowLayoutOverflow.OverflowType.Visible
        ) {
            return layout(0, 0) {}
        }
        val list = measurables.first()
        if (list.isEmpty()) {
            return layout(0, 0) {}
        }
        val seeMoreMeasurable = measurables.getOrNull(1)?.firstOrNull()
        val collapseMeasurable = measurables.getOrNull(2)?.firstOrNull()
        overflow.itemCount = list.size
        overflow.setOverflowMeasurables(
            seeMoreMeasurable,
            collapseMeasurable,
            orientation,
            constraints,
        )
        return breakDownItems(
            orientation,
            horizontalArrangement,
            verticalArrangement,
            crossAxisSize,
            crossAxisAlignment,
            list.iterator(),
            constraints,
            maxItemsInMainAxis,
            maxLines,
            overflow
        )
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int
    ): Int {
        overflow.setOverflowMeasurables(
            seeMoreMeasurable = measurables.getOrNull(1)?.firstOrNull(),
            collapseMeasurable = measurables.getOrNull(2)?.firstOrNull(),
            orientation = orientation,
            constraints = Constraints(maxHeight = height)
        )
        return if (orientation == LayoutOrientation.Horizontal) {
            minIntrinsicMainAxisSize(
                measurables.firstOrNull() ?: listOf(),
                height,
                mainAxisArrangementSpacing.roundToPx(),
                crossAxisArrangementSpacing.roundToPx(),
                maxLines = maxLines,
                maxItemsInMainAxis = maxItemsInMainAxis,
                overflow = overflow
            )
        } else {
            intrinsicCrossAxisSize(
                measurables.firstOrNull() ?: listOf(),
                height,
                mainAxisArrangementSpacing.roundToPx(),
                crossAxisArrangementSpacing.roundToPx(),
                maxLines = maxLines,
                maxItemsInMainAxis = maxItemsInMainAxis,
                overflow = overflow
            )
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int
    ): Int {
        overflow.setOverflowMeasurables(
            seeMoreMeasurable = measurables.getOrNull(1)?.firstOrNull(),
            collapseMeasurable = measurables.getOrNull(2)?.firstOrNull(),
            orientation = orientation,
            constraints = Constraints(maxWidth = width)
        )
        return if (orientation == LayoutOrientation.Horizontal) {
            intrinsicCrossAxisSize(
                measurables.firstOrNull() ?: listOf(),
                width,
                mainAxisArrangementSpacing.roundToPx(),
                crossAxisArrangementSpacing.roundToPx(),
                maxLines = maxLines,
                maxItemsInMainAxis = maxItemsInMainAxis,
                overflow = overflow
            )
        } else {
            minIntrinsicMainAxisSize(
                measurables.firstOrNull() ?: listOf(),
                width,
                mainAxisArrangementSpacing.roundToPx(),
                crossAxisArrangementSpacing.roundToPx(),
                maxLines = maxLines,
                maxItemsInMainAxis = maxItemsInMainAxis,
                overflow = overflow
            )
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int
    ): Int {
        overflow.setOverflowMeasurables(
            seeMoreMeasurable = measurables.getOrNull(1)?.firstOrNull(),
            collapseMeasurable = measurables.getOrNull(2)?.firstOrNull(),
            orientation = orientation,
            constraints = Constraints(maxWidth = width)
        )
        return if (orientation == LayoutOrientation.Horizontal) {
            intrinsicCrossAxisSize(
                measurables.firstOrNull() ?: listOf(),
                width,
                mainAxisArrangementSpacing.roundToPx(),
                crossAxisArrangementSpacing.roundToPx(),
                maxLines = maxLines,
                maxItemsInMainAxis = maxItemsInMainAxis,
                overflow = overflow
            )
        } else {
            maxIntrinsicMainAxisSize(
                measurables.firstOrNull() ?: listOf(),
                width,
                mainAxisArrangementSpacing.roundToPx(),
            )
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int
    ): Int {
        overflow.setOverflowMeasurables(
            seeMoreMeasurable = measurables.getOrNull(1)?.firstOrNull(),
            collapseMeasurable = measurables.getOrNull(2)?.firstOrNull(),
            orientation = orientation,
            constraints = Constraints(maxHeight = height)
        )
        return if (orientation == LayoutOrientation.Horizontal) {
            maxIntrinsicMainAxisSize(
                measurables.firstOrNull() ?: listOf(),
                height,
                mainAxisArrangementSpacing.roundToPx(),
            )
        } else {
            intrinsicCrossAxisSize(
                measurables.firstOrNull() ?: listOf(),
                height,
                mainAxisArrangementSpacing.roundToPx(),
                crossAxisArrangementSpacing.roundToPx(),
                maxLines = maxLines,
                maxItemsInMainAxis = maxItemsInMainAxis,
                overflow = overflow
            )
        }
    }

    fun minIntrinsicMainAxisSize(
        measurables: List<IntrinsicMeasurable>,
        crossAxisAvailable: Int,
        mainAxisSpacing: Int,
        crossAxisSpacing: Int,
        maxItemsInMainAxis: Int,
        maxLines: Int,
        overflow: FlowLayoutOverflowState
    ) = minIntrinsicMainAxisSize(
        measurables,
        mainAxisSize = minMainAxisIntrinsicItemSize,
        crossAxisSize = minCrossAxisIntrinsicItemSize,
        crossAxisAvailable,
        mainAxisSpacing,
        crossAxisSpacing,
        maxItemsInMainAxis,
        maxLines,
        overflow
    )

    fun maxIntrinsicMainAxisSize(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        arrangementSpacing: Int
    ) = maxIntrinsicMainAxisSize(
        measurables,
        maxMainAxisIntrinsicItemSize,
        height,
        arrangementSpacing,
        maxItemsInMainAxis
    )

    fun intrinsicCrossAxisSize(
        measurables: List<IntrinsicMeasurable>,
        mainAxisAvailable: Int,
        mainAxisSpacing: Int,
        crossAxisSpacing: Int,
        maxItemsInMainAxis: Int,
        maxLines: Int,
        overflow: FlowLayoutOverflowState
    ) = intrinsicCrossAxisSize(
        measurables,
        mainAxisSize = minMainAxisIntrinsicItemSize,
        crossAxisSize = minCrossAxisIntrinsicItemSize,
        mainAxisAvailable,
        mainAxisSpacing,
        crossAxisSpacing,
        maxItemsInMainAxis = maxItemsInMainAxis,
        overflow = overflow,
        maxLines = maxLines
    ).first

    val maxMainAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
        if (orientation == LayoutOrientation.Horizontal) { _, h ->
            maxIntrinsicWidth(h)
        }
        else { _, w ->
            maxIntrinsicHeight(w)
        }

    val maxCrossAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
        if (orientation == LayoutOrientation.Horizontal) { _, w ->
            maxIntrinsicHeight(w)
        }
        else { _, h ->
            maxIntrinsicWidth(h)
        }

    val minCrossAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
        if (orientation == LayoutOrientation.Horizontal) { _, w ->
            minIntrinsicHeight(w)
        }
        else { _, h ->
            minIntrinsicWidth(h)
        }

    val minMainAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
        if (orientation == LayoutOrientation.Horizontal) { _, h ->
            minIntrinsicWidth(h)
        }
        else { _, w ->
            minIntrinsicHeight(w)
        }
}

private fun maxIntrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisAvailable: Int,
    mainAxisSpacing: Int,
    maxItemsInMainAxis: Int
): Int {
    var fixedSpace = 0
    var currentFixedSpace = 0
    var lastBreak = 0
    children.fastForEachIndexed { index, child ->
        val size = child.mainAxisSize(index, crossAxisAvailable) + mainAxisSpacing
        if (index + 1 - lastBreak == maxItemsInMainAxis || index + 1 == children.size) {
            lastBreak = index
            currentFixedSpace += size
            currentFixedSpace -= mainAxisSpacing // no mainAxisSpacing for last item in main axis
            fixedSpace = max(fixedSpace, currentFixedSpace)
            currentFixedSpace = 0
        } else {
            currentFixedSpace += size
        }
    }
    return fixedSpace
}

/**
 * Slower algorithm but needed to determine the minimum main axis size
 * Uses a binary search to search different scenarios to see the minimum main axis size
 */
@OptIn(ExperimentalLayoutApi::class)
private fun minIntrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisAvailable: Int,
    mainAxisSpacing: Int,
    crossAxisSpacing: Int,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflow: FlowLayoutOverflowState
): Int {
    if (children.isEmpty()) {
        return 0
    }
    val mainAxisSizes = IntArray(children.size) { 0 }
    val crossAxisSizes = IntArray(children.size) { 0 }

    for (index in children.indices) {
        val child = children[index]
        val mainAxisItemSize = child.mainAxisSize(index, crossAxisAvailable)
        mainAxisSizes[index] = mainAxisItemSize
        crossAxisSizes[index] = child.crossAxisSize(index, mainAxisItemSize)
    }

    var maxItemsThatCanBeShown = if (
        maxLines != Int.MAX_VALUE &&
        maxItemsInMainAxis != Int.MAX_VALUE) {
        maxItemsInMainAxis * maxLines
    } else {
        Int.MAX_VALUE
    }
    val mustHaveEllipsis = when {
        maxItemsThatCanBeShown < children.size &&
            (overflow.type == FlowLayoutOverflow.OverflowType.ExpandIndicator ||
                overflow.type == FlowLayoutOverflow.OverflowType.ExpandOrCollapseIndicator)
        -> true
        maxItemsThatCanBeShown >= children.size &&
            maxLines >= overflow.minLinesToShowCollapse &&
            overflow.type == FlowLayoutOverflow.OverflowType.ExpandOrCollapseIndicator ->
            true
        else -> false
    }
    maxItemsThatCanBeShown -= if (mustHaveEllipsis) 1 else 0
    maxItemsThatCanBeShown = min(maxItemsThatCanBeShown, children.size)
    val maxMainAxisSize = mainAxisSizes.sum().run { this + ((children.size - 1) * mainAxisSpacing) }
    var mainAxisUsed = maxMainAxisSize
    var crossAxisUsed = crossAxisSizes.maxOf { it }

    val minimumItemSize = mainAxisSizes.maxOf { it }
    var low = minimumItemSize
    var high = maxMainAxisSize
    while (low <= high) {
        if (crossAxisUsed == crossAxisAvailable) {
            return mainAxisUsed
        }
        val mid = (low + high) / 2
        mainAxisUsed = mid
        val pair = intrinsicCrossAxisSize(
            children,
            mainAxisSizes,
            crossAxisSizes,
            mainAxisUsed,
            mainAxisSpacing,
            crossAxisSpacing,
            maxItemsInMainAxis,
            maxLines,
            overflow
        )
        crossAxisUsed = pair.first
        val itemShown = pair.second

        if (crossAxisUsed > crossAxisAvailable || itemShown < maxItemsThatCanBeShown) {
            low = mid + 1
            if (low > high) {
                return low
            }
        } else if (crossAxisUsed < crossAxisAvailable) {
            high = mid - 1
        } else {
            return mainAxisUsed
        }
    }

    return mainAxisUsed
}

/**
 * FlowRow: Intrinsic height (cross Axis) is based on a specified width
 * FlowColumn: Intrinsic width (crossAxis) based on a specified height
 */
private fun intrinsicCrossAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSizes: IntArray,
    crossAxisSizes: IntArray,
    mainAxisAvailable: Int,
    mainAxisSpacing: Int,
    crossAxisSpacing: Int,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflow: FlowLayoutOverflowState
): IntIntPair {
    return intrinsicCrossAxisSize(
        children,
        { index, _ -> mainAxisSizes[index] },
        { index, _ -> crossAxisSizes[index] },
        mainAxisAvailable,
        mainAxisSpacing,
        crossAxisSpacing,
        maxItemsInMainAxis,
        maxLines,
        overflow
    )
}

/** FlowRow: Intrinsic height (cross Axis) is based on a specified width
 ** FlowColumn: Intrinsic width (crossAxis) based on a specified height
 */
private fun intrinsicCrossAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    mainAxisAvailable: Int,
    mainAxisSpacing: Int,
    crossAxisSpacing: Int,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflow: FlowLayoutOverflowState
): IntIntPair {
    if (children.isEmpty()) {
        return IntIntPair(0, 0)
    }
    val buildingBlocks = FlowLayoutBuildingBlocks(
        maxItemsInMainAxis = maxItemsInMainAxis,
        overflow = overflow,
        maxLines = maxLines,
        constraints = OrientationIndependentConstraints(
            mainAxisMin = 0,
            mainAxisMax = mainAxisAvailable,
            crossAxisMin = 0,
            crossAxisMax = Constraints.Infinity
        ),
        mainAxisSpacing = mainAxisSpacing,
        crossAxisSpacing = crossAxisSpacing,
    )
    var nextChild = children.getOrNull(0)
    var nextCrossAxisSize = nextChild?.crossAxisSize(0, mainAxisAvailable) ?: 0
    var nextMainAxisSize = nextChild?.mainAxisSize(0, nextCrossAxisSize) ?: 0

    var remaining = mainAxisAvailable
    var currentCrossAxisSize = 0
    var totalCrossAxisSize = 0
    var lastBreak = 0
    var lineIndex = 0

    var wrapInfo = buildingBlocks.getWrapInfo(
        nextItemHasNext = children.size > 1,
        nextIndexInLine = 0,
        leftOver = IntIntPair(remaining, Constraints.Infinity),
        nextSize = if (nextChild == null) null else IntIntPair(nextMainAxisSize, nextCrossAxisSize),
        lineIndex = lineIndex,
        totalCrossAxisSize = totalCrossAxisSize,
        currentLineCrossAxisSize = currentCrossAxisSize,
        isWrappingRound = false,
        isEllipsisWrap = false
    )

    if (wrapInfo.isLastItemInContainer) {
        val size = overflow.ellipsisSize(
            hasNext = nextChild != null,
            lineIndex = 0,
            totalCrossAxisSize = 0,
        )?.second ?: 0
        val noOfItemsShown = 0
        return IntIntPair(size, noOfItemsShown)
    }

    var noOfItemsShown = 0
    for (index in children.indices) {
        val childCrossAxisSize = nextCrossAxisSize
        val childMainAxisSize = nextMainAxisSize
        remaining -= childMainAxisSize
        noOfItemsShown = index + 1
        currentCrossAxisSize = maxOf(currentCrossAxisSize, childCrossAxisSize)

        // look ahead to simplify logic
        nextChild = children.getOrNull(index + 1)
        nextCrossAxisSize = nextChild?.crossAxisSize(index + 1, mainAxisAvailable) ?: 0
        nextMainAxisSize = nextChild?.mainAxisSize(index + 1, nextCrossAxisSize)
            ?.plus(mainAxisSpacing) ?: 0

        wrapInfo = buildingBlocks.getWrapInfo(
            nextItemHasNext = index + 2 < children.size,
            nextIndexInLine = (index + 1) - lastBreak,
            leftOver = IntIntPair(remaining, Constraints.Infinity),
            nextSize = if (nextChild == null) {
                null
            } else {
                IntIntPair(nextMainAxisSize, nextCrossAxisSize)
            },
            lineIndex = lineIndex,
            totalCrossAxisSize = totalCrossAxisSize,
            currentLineCrossAxisSize = currentCrossAxisSize,
            isWrappingRound = false,
            isEllipsisWrap = false
        )
        if (wrapInfo.isLastItemInLine) {
            totalCrossAxisSize += currentCrossAxisSize + crossAxisSpacing
            val ellipsisWrapInfo = buildingBlocks.getWrapEllipsisInfo(
                wrapInfo,
                hasNext = nextChild != null,
                leftOverMainAxis = remaining,
                lastContentLineIndex = lineIndex,
                totalCrossAxisSize = totalCrossAxisSize,
                nextIndexInLine = (index + 1) - lastBreak,
            )
            currentCrossAxisSize = 0
            remaining = mainAxisAvailable
            lastBreak = index + 1
            nextMainAxisSize -= mainAxisSpacing
            lineIndex++
            if (wrapInfo.isLastItemInContainer) {
                ellipsisWrapInfo?.ellipsisSize?.let {
                    if (!ellipsisWrapInfo.placeEllipsisOnLastContentLine) {
                        totalCrossAxisSize += it.second + crossAxisSpacing
                    }
                }
                break
            }
        }
    }
    // remove the last spacing for the last row or column
    totalCrossAxisSize -= crossAxisSpacing
    return IntIntPair(totalCrossAxisSize, noOfItemsShown)
}

/**
 * Breaks down items based on space, size and maximum items in main axis.
 * When items run out of space or the maximum items to fit in the main axis is reached,
 * it moves to the next "line" and moves the next batch of items to a new list of items
 */
internal fun MeasureScope.breakDownItems(
    orientation: LayoutOrientation,
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    sizeMode: SizeMode,
    crossAxisAlignment: CrossAxisAlignment,
    measurablesIterator: Iterator<Measurable>,
    constraints: Constraints,
    maxItemsInMainAxis: Int,
    maxLines: Int,
    overflow: FlowLayoutOverflowState,
): MeasureResult {
    val items = mutableVectorOf<RowColumnMeasureHelperResult>()
    val independentConstraints = OrientationIndependentConstraints(constraints, orientation)
    val mainAxisMax = independentConstraints.mainAxisMax
    val mainAxisMin = independentConstraints.mainAxisMin
    val crossAxisMax = independentConstraints.crossAxisMax
    val placeables = mutableIntObjectMapOf<Placeable?>()
    val measurables = mutableListOf<Measurable>()

    val mainAxisSpacingDp = if (orientation == LayoutOrientation.Horizontal) {
        horizontalArrangement.spacing
    } else {
        verticalArrangement.spacing
    }
    val crossAxisSpacingDp = if (orientation == LayoutOrientation.Horizontal) {
        verticalArrangement.spacing
    } else {
        horizontalArrangement.spacing
    }

    val spacing = ceil(mainAxisSpacingDp.toPx()).toInt()
    val crossAxisSpacing = ceil(crossAxisSpacingDp.toPx()).toInt()
    val subsetConstraints = OrientationIndependentConstraints(
        mainAxisMin,
        mainAxisMax,
        0,
        crossAxisMax
    )
    // nextSize of the list, pre-calculated
    var index = 0
    var measurable: Measurable?
    var nextSize = measurablesIterator.hasNext().run {
        measurable = if (!this) null else measurablesIterator.safeNext()
        measurable?.measureAndCache(subsetConstraints, orientation) { placeable ->
            placeables[0] = placeable
        }
    }
    var nextMainAxisSize: Int? = nextSize?.first
    var nextCrossAxisSize: Int? = nextSize?.second

    var startBreakLineIndex = 0
    val endBreakLineList = mutableIntListOf()
    val crossAxisSizes = mutableIntListOf()
    var lineIndex = 0

    var leftOver = mainAxisMax
    var leftOverCrossAxis = crossAxisMax
    val buildingBlocks = FlowLayoutBuildingBlocks(
        maxItemsInMainAxis = maxItemsInMainAxis,
        mainAxisSpacing = spacing,
        crossAxisSpacing = crossAxisSpacing,
        constraints = independentConstraints,
        maxLines = maxLines,
        overflow = overflow
    )
    var ellipsisWrapInfo: FlowLayoutBuildingBlocks.WrapEllipsisInfo? = null
    var wrapInfo = buildingBlocks.getWrapInfo(
        nextItemHasNext = measurablesIterator.hasNext(),
        leftOver = IntIntPair(leftOver, leftOverCrossAxis),
        totalCrossAxisSize = 0,
        nextSize = nextSize,
        currentLineCrossAxisSize = 0,
        nextIndexInLine = 0,
        isWrappingRound = false,
        isEllipsisWrap = false,
        lineIndex = 0
    ).also { wrapInfo ->
        if (wrapInfo.isLastItemInContainer) {
            ellipsisWrapInfo = buildingBlocks.getWrapEllipsisInfo(
                wrapInfo,
                nextSize != null,
                lastContentLineIndex = -1,
                totalCrossAxisSize = 0,
                leftOver,
                nextIndexInLine = 0
            )
        }
    }

    // figure out the mainAxisTotalSize which will be minMainAxis when measuring the row/column
    var mainAxisTotalSize = mainAxisMin
    var crossAxisTotalSize = 0
    var currentLineMainAxisSize = 0
    var currentLineCrossAxisSize = 0
    while (!wrapInfo.isLastItemInContainer && measurable != null) {
        val itemMainAxisSize = nextMainAxisSize!!
        val itemCrossAxisSize = nextCrossAxisSize!!
        currentLineMainAxisSize += itemMainAxisSize
        currentLineCrossAxisSize = maxOf(currentLineCrossAxisSize, itemCrossAxisSize)
        leftOver -= itemMainAxisSize
        overflow.itemShown = index + 1
        measurables.add(measurable!!)
        nextSize = measurablesIterator.hasNext().run {
            measurable = if (!this) null else measurablesIterator.safeNext()
            measurable?.measureAndCache(subsetConstraints, orientation) { placeable ->
                placeables[index + 1] = placeable
            }
        }
        nextMainAxisSize = nextSize?.first?.plus(spacing)
        nextCrossAxisSize = nextSize?.second

        wrapInfo = buildingBlocks.getWrapInfo(
            nextItemHasNext = measurablesIterator.hasNext(),
            leftOver = IntIntPair(leftOver, leftOverCrossAxis),
            totalCrossAxisSize = crossAxisTotalSize,
            nextSize = if (nextSize == null) null else
                IntIntPair(nextMainAxisSize!!, nextCrossAxisSize!!),
            currentLineCrossAxisSize = currentLineCrossAxisSize,
            nextIndexInLine = (index + 1) - startBreakLineIndex,
            isWrappingRound = false,
            isEllipsisWrap = false,
            lineIndex = lineIndex
        )
        if (wrapInfo.isLastItemInLine) {
            mainAxisTotalSize = maxOf(mainAxisTotalSize, currentLineMainAxisSize)
            mainAxisTotalSize = minOf(mainAxisTotalSize, mainAxisMax)
            crossAxisTotalSize += currentLineCrossAxisSize
            ellipsisWrapInfo = buildingBlocks.getWrapEllipsisInfo(
                wrapInfo,
                nextSize != null,
                lastContentLineIndex = lineIndex,
                totalCrossAxisSize = crossAxisTotalSize,
                leftOver,
                (index + 1) - startBreakLineIndex
            )
            crossAxisSizes.add(currentLineCrossAxisSize)
            leftOver = mainAxisMax
            leftOverCrossAxis = crossAxisMax - crossAxisTotalSize - crossAxisSpacing
            startBreakLineIndex = index + 1
            endBreakLineList.add(index + 1)
            currentLineMainAxisSize = 0
            currentLineCrossAxisSize = 0
            // only add spacing for next items in the row or column, not the starting indexes
            nextMainAxisSize = nextMainAxisSize?.minus(spacing)
            lineIndex++
            crossAxisTotalSize += crossAxisSpacing
        }
        index++
    }

    val measureHelper = RowColumnMeasurementHelper(
        orientation,
        horizontalArrangement,
        verticalArrangement,
        sizeMode,
        crossAxisAlignment,
        RowColumnMeasurablesWrapper(
            measurables,
            placeables,
            ellipsisWrapInfo?.ellipsis,
            ellipsisWrapInfo?.placeEllipsisOnLastContentLine,
        ),
    )

    ellipsisWrapInfo?.let {
        lineIndex = endBreakLineList.lastIndex
        if (it.placeEllipsisOnLastContentLine) {
            val lastLineCrossAxis = crossAxisSizes[lineIndex]
            crossAxisSizes[lineIndex] = max(lastLineCrossAxis, it.ellipsisSize.second)
        } else {
            crossAxisSizes.add(it.ellipsisSize.second)
        }
    }

    val subsetBoxConstraints = subsetConstraints.copy(
        mainAxisMin = mainAxisTotalSize
    )

    crossAxisTotalSize = 0
    measureHelper.listWrapper.forEachLine(
        endBreakLineList
    ) { currentLineIndex, startIndex, endIndex ->
        val crossAxisSize = crossAxisSizes[currentLineIndex]
        val result = measureHelper.measureWithoutPlacing(
            this,
            subsetBoxConstraints.copy(
                crossAxisMax = crossAxisSize
            ).toBoxConstraints(orientation),
            startIndex,
            endIndex
        )
        crossAxisTotalSize += result.crossAxisSize
        mainAxisTotalSize = maxOf(mainAxisTotalSize, result.mainAxisSize)
        items.add(
            result
        )
    }

    crossAxisTotalSize = maxOf(crossAxisTotalSize, independentConstraints.crossAxisMin)
    mainAxisTotalSize = maxOf(mainAxisTotalSize, independentConstraints.mainAxisMin)
    val flowResult = FlowResult(
        mainAxisTotalSize,
        crossAxisTotalSize,
        items,
    )

    if (flowResult.items.isEmpty()) {
        return layout(
        constraints.constrainWidth(0),
        constraints.constrainHeight(0)) {}
    }

    return handleFlowResult(flowResult, constraints, measureHelper)
}

private fun Iterator<Measurable>.safeNext(): Measurable? {
    return try {
        next()
    } catch (e: ArrayIndexOutOfBoundsException) {
        null
    }
}

internal fun IntrinsicMeasurable.mainAxisMin(orientation: LayoutOrientation, crossAxisSize: Int) =
    if (orientation == LayoutOrientation.Horizontal) {
        minIntrinsicWidth(crossAxisSize)
    } else {
        minIntrinsicHeight(crossAxisSize)
    }

internal fun IntrinsicMeasurable.crossAxisMin(orientation: LayoutOrientation, mainAxisSize: Int) =
    if (orientation == LayoutOrientation.Horizontal) {
        minIntrinsicHeight(mainAxisSize)
    } else {
        minIntrinsicWidth(mainAxisSize)
    }

internal fun Placeable.mainAxisSize(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) measuredWidth else measuredHeight

internal fun Placeable.crossAxisSize(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) measuredHeight else measuredWidth

internal val CROSS_AXIS_ALIGNMENT_TOP = CrossAxisAlignment.vertical(Alignment.Top)
internal val CROSS_AXIS_ALIGNMENT_START = CrossAxisAlignment.horizontal(Alignment.Start)

// We measure and cache to improve performance dramatically, instead of using intrinsics
// This only works so far for fixed size items.
// For weighted items, we continue to use their intrinsic widths.
// This is because their fixed sizes are only determined after we determine
// the number of items that can fit in the row/column it only lies on.
private fun Measurable.measureAndCache(
    constraints: OrientationIndependentConstraints,
    orientation: LayoutOrientation,
    storePlaceable: (Placeable?) -> Unit
): IntIntPair {
    return if (
        rowColumnParentData.weight == 0f &&
        rowColumnParentData?.flowLayoutData?.fillCrossAxisFraction == null
    ) {
        // fixed sizes: measure once
        val placeable = measure(
            constraints.copy(
                mainAxisMin = 0,
            ).toBoxConstraints(orientation)
        ).also(storePlaceable)
        val mainAxis = placeable.mainAxisSize(orientation)
        val crossAxis = placeable.crossAxisSize(orientation)
        IntIntPair(mainAxis, crossAxis)
    } else {
        val mainAxis = mainAxisMin(orientation, Constraints.Infinity)
        val crossAxis = crossAxisMin(orientation, mainAxis)
        IntIntPair(mainAxis, crossAxis)
    }
}

internal fun MeasureScope.handleFlowResult(
    flowResult: FlowResult,
    constraints: Constraints,
    measureHelper: RowColumnMeasurementHelper
): MeasureResult {
    val orientation = measureHelper.orientation
    val verticalArrangement = measureHelper.verticalArrangement
    val horizontalArrangement = measureHelper.horizontalArrangement
    val items = flowResult.items
    val crossAxisSizes = IntArray(items.size) { index ->
        items[index].crossAxisSize
    }
    // space in between children, except for the last child
    val outPosition = IntArray(crossAxisSizes.size)
    var totalCrossAxisSize = flowResult.crossAxisTotalSize
    // cross axis arrangement
    if (orientation == LayoutOrientation.Horizontal) {
        with(requireNotNull(verticalArrangement) { "null verticalArrangement" }) {
            val totalCrossAxisSpacing = spacing.roundToPx() * (items.size - 1)
            totalCrossAxisSize += totalCrossAxisSpacing
            arrange(
                totalCrossAxisSize,
                crossAxisSizes,
                outPosition
            )
        }
    } else {
        with(requireNotNull(horizontalArrangement) { "null horizontalArrangement" }) {
            val totalCrossAxisSpacing = spacing.roundToPx() * (items.size - 1)
            totalCrossAxisSize += totalCrossAxisSpacing
            arrange(
                totalCrossAxisSize,
                crossAxisSizes,
                layoutDirection,
                outPosition
            )
        }
    }

    var layoutWidth: Int
    var layoutHeight: Int
    if (orientation == LayoutOrientation.Horizontal) {
        layoutWidth = flowResult.mainAxisTotalSize
        layoutHeight = totalCrossAxisSize
    } else {
        layoutWidth = totalCrossAxisSize
        layoutHeight = flowResult.mainAxisTotalSize
    }
    layoutWidth = constraints.constrainWidth(layoutWidth)
    layoutHeight = constraints.constrainHeight(layoutHeight)

    return layout(layoutWidth, layoutHeight) {
        flowResult.items.forEachIndexed { currentRowOrColumnIndex,
            measureResult ->
            measureHelper.placeHelper(
                this,
                measureResult,
                outPosition[currentRowOrColumnIndex],
                this@handleFlowResult.layoutDirection
            )
        }
    }
}

/**
 * FlowResult when broken down to multiple rows or columns based on [breakDownItems] algorithm
 *
 * @param mainAxisTotalSize the total size of the main axis
 * @param crossAxisTotalSize the total size of the cross axis when taken into account
 * the cross axis sizes of all items
 * @param items the row or column measurements for each row or column
 */
internal class FlowResult(
    val mainAxisTotalSize: Int,
    val crossAxisTotalSize: Int,
    val items: MutableVector<RowColumnMeasureHelperResult>,
)
