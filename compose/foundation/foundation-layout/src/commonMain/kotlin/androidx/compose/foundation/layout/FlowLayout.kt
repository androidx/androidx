package androidx.compose.foundation.layout

import androidx.annotation.FloatRange
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
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowRowWithWeights
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
 * @param content The content as a [RowScope]
 *
 * @see FlowColumn
 * @see [androidx.compose.foundation.layout.Row]
 */
@Composable
@ExperimentalLayoutApi
inline fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable FlowRowScope.() -> Unit
) {
    val measurePolicy = rowMeasurementHelper(
        horizontalArrangement,
        verticalArrangement,
        maxItemsInEachRow
    )
    Layout(
        content = { FlowRowScopeInstance.content() },
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
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumnWithWeights
 *
 * @param modifier The modifier to be applied to the Row.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalArrangement The horizontal arrangement of the layout's virtual columns
 * @param maxItemsInEachColumn The maximum number of items per column
 * @param content The content as a [ColumnScope]
 *
 * @see FlowRow
 * @see [androidx.compose.foundation.layout.Column]
 */
@Composable
@ExperimentalLayoutApi
inline fun FlowColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachColumn: Int = Int.MAX_VALUE,
    content: @Composable FlowColumnScope.() -> Unit
) {
    val measurePolicy = columnMeasurementHelper(
        verticalArrangement,
        horizontalArrangement,
        maxItemsInEachColumn
    )
    Layout(
        content = { FlowColumnScopeInstance.content() },
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

@PublishedApi
@Composable
internal fun rowMeasurementHelper(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    maxItemsInMainAxis: Int,
): MeasurePolicy {
    return remember(horizontalArrangement, verticalArrangement, maxItemsInMainAxis) {
        FlowMeasurePolicy(
            orientation = LayoutOrientation.Horizontal,
            horizontalArrangement = horizontalArrangement,
            mainAxisArrangementSpacing = horizontalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_TOP,
            verticalArrangement = verticalArrangement,
            crossAxisArrangementSpacing = verticalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis
        )
    }
}

@PublishedApi
@Composable
internal fun columnMeasurementHelper(
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    maxItemsInMainAxis: Int,
): MeasurePolicy {
    return remember(verticalArrangement, horizontalArrangement, maxItemsInMainAxis) {
        FlowMeasurePolicy(
            orientation = LayoutOrientation.Vertical,
            verticalArrangement = verticalArrangement,
            mainAxisArrangementSpacing = verticalArrangement.spacing,
            crossAxisSize = SizeMode.Wrap,
            crossAxisAlignment = CROSS_AXIS_ALIGNMENT_START,
            horizontalArrangement = horizontalArrangement,
            crossAxisArrangementSpacing = horizontalArrangement.spacing,
            maxItemsInMainAxis = maxItemsInMainAxis,
        )
    }
}

/**
 * Returns a Flow Measure Policy
 */
private data class FlowMeasurePolicy(
    private val orientation: LayoutOrientation,
    private val horizontalArrangement: Arrangement.Horizontal?,
    private val verticalArrangement: Arrangement.Vertical?,
    private val mainAxisArrangementSpacing: Dp,
    private val crossAxisSize: SizeMode,
    private val crossAxisAlignment: CrossAxisAlignment,
    private val crossAxisArrangementSpacing: Dp,
    private val maxItemsInMainAxis: Int,
) : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        if (measurables.isEmpty()) {
            return layout(0, 0) {}
        }
        val placeables: Array<Placeable?> = arrayOfNulls(measurables.size)
        val measureHelper = RowColumnMeasurementHelper(
            orientation,
            horizontalArrangement,
            verticalArrangement,
            mainAxisArrangementSpacing,
            crossAxisSize,
            crossAxisAlignment,
            measurables,
            placeables,
        )
        val orientationIndependentConstraints =
            OrientationIndependentConstraints(constraints, orientation)
        val flowResult = breakDownItems(
            measureHelper,
            orientation,
            orientationIndependentConstraints,
            maxItemsInMainAxis,
        )
        val items = flowResult.items
        val crossAxisSizes = IntArray(items.size) { index ->
            items[index].crossAxisSize
        }
        // space in between children, except for the last child
        val outPosition = IntArray(crossAxisSizes.size)
        var totalCrossAxisSize = flowResult.crossAxisTotalSize
        val totalCrossAxisSpacing =
            crossAxisArrangementSpacing.roundToPx() * (items.size - 1)
        totalCrossAxisSize += totalCrossAxisSpacing
        // cross axis arrangement
        if (orientation == LayoutOrientation.Horizontal) {
            with(requireNotNull(verticalArrangement) { "null verticalArrangement" }) {
                arrange(
                    totalCrossAxisSize,
                    crossAxisSizes,
                    outPosition
                )
            }
        } else {
            with(requireNotNull(horizontalArrangement) { "null horizontalArrangement" }) {
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
                    this@measure.layoutDirection
                )
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = if (orientation == LayoutOrientation.Horizontal) {
        minIntrinsicMainAxisSize(
            measurables,
            height,
            mainAxisArrangementSpacing.roundToPx(),
            crossAxisArrangementSpacing.roundToPx()
        )
    } else {
        intrinsicCrossAxisSize(
            measurables,
            height,
            mainAxisArrangementSpacing.roundToPx(),
            crossAxisArrangementSpacing.roundToPx()
        )
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = if (orientation == LayoutOrientation.Horizontal) {
        intrinsicCrossAxisSize(
            measurables,
            width,
            mainAxisArrangementSpacing.roundToPx(),
            crossAxisArrangementSpacing.roundToPx()
        )
    } else {
        minIntrinsicMainAxisSize(
            measurables,
            width,
            mainAxisArrangementSpacing.roundToPx(),
            crossAxisArrangementSpacing.roundToPx(),
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = if (orientation == LayoutOrientation.Horizontal) {
        intrinsicCrossAxisSize(
            measurables,
            width,
            mainAxisArrangementSpacing.roundToPx(),
            crossAxisArrangementSpacing.roundToPx()
        )
    } else {
        maxIntrinsicMainAxisSize(
            measurables,
            width,
            mainAxisArrangementSpacing.roundToPx(),
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = if (orientation == LayoutOrientation.Horizontal) {
        maxIntrinsicMainAxisSize(
            measurables,
            height,
            mainAxisArrangementSpacing.roundToPx(),
        )
    } else {
        intrinsicCrossAxisSize(
            measurables,
            height,
            mainAxisArrangementSpacing.roundToPx(),
            crossAxisArrangementSpacing.roundToPx()
        )
    }

    fun minIntrinsicMainAxisSize(
        measurables: List<IntrinsicMeasurable>,
        crossAxisAvailable: Int,
        mainAxisSpacing: Int,
        crossAxisSpacing: Int,
    ) = minIntrinsicMainAxisSize(
        measurables,
        mainAxisSize = minMainAxisIntrinsicItemSize,
        crossAxisSize = minCrossAxisIntrinsicItemSize,
        crossAxisAvailable,
        mainAxisSpacing,
        crossAxisSpacing,
        maxItemsInMainAxis
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
        crossAxisSpacing: Int
    ) = intrinsicCrossAxisSize(
        measurables,
        mainAxisSize = minMainAxisIntrinsicItemSize,
        crossAxisSize = minCrossAxisIntrinsicItemSize,
        mainAxisAvailable,
        mainAxisSpacing,
        crossAxisSpacing,
        maxItemsInMainAxis
    )

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
private fun minIntrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisAvailable: Int,
    mainAxisSpacing: Int,
    crossAxisSpacing: Int,
    maxItemsInMainAxis: Int
): Int {
    val mainAxisSizes = IntArray(children.size) { 0 }
    val crossAxisSizes = IntArray(children.size) { 0 }

    for (index in children.indices) {
        val child = children[index]
        val mainAxisItemSize = child.mainAxisSize(index, crossAxisAvailable)
        mainAxisSizes[index] = mainAxisItemSize
        crossAxisSizes[index] = child.crossAxisSize(index, mainAxisItemSize)
    }

    val maxMainAxisSize = mainAxisSizes.sum()
    var mainAxisUsed = maxMainAxisSize
    var crossAxisUsed = crossAxisSizes.maxOf { it }

    val minimumItemSize = mainAxisSizes.maxOf { it }
    var low = minimumItemSize
    var high = maxMainAxisSize
    while (low < high) {
        if (crossAxisUsed == crossAxisAvailable) {
            return mainAxisUsed
        }
        val mid = (low + high) / 2
        mainAxisUsed = mid
        crossAxisUsed = intrinsicCrossAxisSize(
            children,
            mainAxisSizes,
            crossAxisSizes,
            mainAxisUsed,
            mainAxisSpacing,
            crossAxisSpacing,
            maxItemsInMainAxis
        )

        if (crossAxisUsed == crossAxisAvailable) {
            return mainAxisUsed
        } else if (crossAxisUsed > crossAxisAvailable) {
            low = mid + 1
        } else {
            high = mid - 1
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
    maxItemsInMainAxis: Int
): Int {
    return intrinsicCrossAxisSize(
        children,
        { index, _ -> mainAxisSizes[index] },
        { index, _ -> crossAxisSizes[index] },
        mainAxisAvailable,
        mainAxisSpacing,
        crossAxisSpacing,
        maxItemsInMainAxis
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
    maxItemsInMainAxis: Int
): Int {
    if (children.isEmpty()) {
        return 0
    }
    var nextChild = children.getOrNull(0)
    var nextCrossAxisSize = nextChild?.crossAxisSize(0, mainAxisAvailable) ?: 0
    var nextMainAxisSize = nextChild?.mainAxisSize(0, nextCrossAxisSize) ?: 0

    var remaining = mainAxisAvailable
    var currentCrossAxisSize = 0
    var totalCrossAxisSize = 0
    var lastBreak = 0

    children.fastForEachIndexed { index, _ ->
        nextChild!!
        val childCrossAxisSize = nextCrossAxisSize
        val childMainAxisSize = nextMainAxisSize
        remaining -= childMainAxisSize
        currentCrossAxisSize = maxOf(currentCrossAxisSize, childCrossAxisSize)

        // look ahead to simplify logic
        nextChild = children.getOrNull(index + 1)
        nextCrossAxisSize = nextChild?.crossAxisSize(index + 1, mainAxisAvailable) ?: 0
        nextMainAxisSize = nextChild?.mainAxisSize(index + 1, nextCrossAxisSize)
            ?.plus(mainAxisSpacing) ?: 0

        if (remaining < 0 || index + 1 == children.size ||
            (index + 1) - lastBreak == maxItemsInMainAxis ||
            remaining - nextMainAxisSize < 0
        ) {
            totalCrossAxisSize += currentCrossAxisSize + crossAxisSpacing
            currentCrossAxisSize = 0
            remaining = mainAxisAvailable
            lastBreak = index + 1
            nextMainAxisSize -= mainAxisSpacing
        }
    }
    // remove the last spacing for the last row or column
    totalCrossAxisSize -= crossAxisSpacing
    return totalCrossAxisSize
}

/**
 * Breaks down items based on space, size and maximum items in main axis.
 * When items run out of space or the maximum items to fit in the main axis is reached,
 * it moves to the next "line" and moves the next batch of items to a new list of items
 */
internal fun MeasureScope.breakDownItems(
    measureHelper: RowColumnMeasurementHelper,
    orientation: LayoutOrientation,
    constraints: OrientationIndependentConstraints,
    maxItemsInMainAxis: Int,
): FlowResult {
    val items = mutableVectorOf<RowColumnMeasureHelperResult>()
    val mainAxisMax = constraints.mainAxisMax
    val mainAxisMin = constraints.mainAxisMin
    val crossAxisMax = constraints.crossAxisMax
    val measurables = measureHelper.measurables
    val placeables = measureHelper.placeables

    val spacing = ceil(measureHelper.arrangementSpacing.toPx()).toInt()
    val subsetConstraints = OrientationIndependentConstraints(
        mainAxisMin,
        mainAxisMax,
        0,
        crossAxisMax
    )
    // nextSize of the list, pre-calculated
    var nextSize: Pair<Int, Int>? = measurables.getOrNull(0)?.measureAndCache(
        subsetConstraints, orientation
    ) { placeable ->
        placeables[0] = placeable
    }
    var nextMainAxisSize: Int? = nextSize?.first
    var nextCrossAxisSize: Int? = nextSize?.second

    var startBreakLineIndex = 0
    val endBreakLineList = arrayOfNulls<Int>(measurables.size)
    val crossAxisSizes = arrayOfNulls<Int>(measurables.size)
    var endBreakLineIndex = 0

    var leftOver = mainAxisMax
    // figure out the mainAxisTotalSize which will be minMainAxis when measuring the row/column
    var mainAxisTotalSize = mainAxisMin
    var currentLineMainAxisSize = 0
    var currentLineCrossAxisSize = 0
    for (index in measurables.indices) {
        val itemMainAxisSize = nextMainAxisSize!!
        val itemCrossAxisSize = nextCrossAxisSize!!
        currentLineMainAxisSize += itemMainAxisSize
        currentLineCrossAxisSize = maxOf(currentLineCrossAxisSize, itemCrossAxisSize)
        leftOver -= itemMainAxisSize
        nextSize = measurables.getOrNull(index + 1)?.measureAndCache(
            subsetConstraints, orientation
        ) { placeable ->
            placeables[index + 1] = placeable
        }
        nextMainAxisSize = nextSize?.first?.plus(spacing)
        nextCrossAxisSize = nextSize?.second ?: 0
        if (index + 1 >= measurables.size ||
            (index + 1) - startBreakLineIndex >= maxItemsInMainAxis ||
            leftOver - (nextMainAxisSize ?: 0) < 0
        ) {
            mainAxisTotalSize = maxOf(mainAxisTotalSize, currentLineMainAxisSize)
            mainAxisTotalSize = minOf(mainAxisTotalSize, mainAxisMax)
            startBreakLineIndex = index + 1
            endBreakLineList[endBreakLineIndex] = index + 1
            crossAxisSizes[endBreakLineIndex] = currentLineCrossAxisSize
            endBreakLineIndex++
            currentLineMainAxisSize = 0
            currentLineCrossAxisSize = 0
            leftOver = mainAxisMax
            // only add spacing for next items in the row or column, not the starting indexes
            nextMainAxisSize = nextMainAxisSize?.minus(spacing)
        }
    }

    val subsetBoxConstraints = subsetConstraints.copy(
        mainAxisMin = mainAxisTotalSize
    )

    startBreakLineIndex = 0
    var crossAxisTotalSize = 0

    endBreakLineIndex = 0
    var endIndex = endBreakLineList.getOrNull(endBreakLineIndex)
    while (endIndex != null) {
        var crossAxisSize = crossAxisSizes[endBreakLineIndex]
        val result = measureHelper.measureWithoutPlacing(
            this,
            subsetBoxConstraints.copy(
                crossAxisMax = crossAxisSize!!
            ).toBoxConstraints(orientation),
            startBreakLineIndex,
            endIndex
        )
        crossAxisTotalSize += result.crossAxisSize
        mainAxisTotalSize = maxOf(mainAxisTotalSize, result.mainAxisSize)
        items.add(
            result
        )
        startBreakLineIndex = endIndex
        endBreakLineIndex++
        endIndex = endBreakLineList.getOrNull(endBreakLineIndex)
    }

    crossAxisTotalSize = maxOf(crossAxisTotalSize, constraints.crossAxisMin)
    mainAxisTotalSize = maxOf(mainAxisTotalSize, constraints.mainAxisMin)
    return FlowResult(
        mainAxisTotalSize,
        crossAxisTotalSize,
        items,
    )
}

internal fun Measurable.mainAxisMin(orientation: LayoutOrientation, crossAxisSize: Int) =
    if (orientation == LayoutOrientation.Horizontal) {
        minIntrinsicWidth(crossAxisSize)
    } else {
        minIntrinsicHeight(crossAxisSize)
    }

internal fun Measurable.crossAxisMin(orientation: LayoutOrientation, mainAxisSize: Int) =
    if (orientation == LayoutOrientation.Horizontal) {
        minIntrinsicHeight(mainAxisSize)
    } else {
        minIntrinsicWidth(mainAxisSize)
    }

internal fun Placeable.mainAxisSize(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) width else height

internal fun Placeable.crossAxisSize(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) height else width

private val CROSS_AXIS_ALIGNMENT_TOP = CrossAxisAlignment.vertical(Alignment.Top)
private val CROSS_AXIS_ALIGNMENT_START = CrossAxisAlignment.horizontal(Alignment.Start)

// We measure and cache to improve performance dramatically, instead of using intrinsics
// This only works so far for fixed size items.
// For weighted items, we continue to use their intrinsic widths.
// This is because their fixed sizes are only determined after we determine
// the number of items that can fit in the row/column it only lies on.
private fun Measurable.measureAndCache(
    constraints: OrientationIndependentConstraints,
    orientation: LayoutOrientation,
    storePlaceable: (Placeable?) -> Unit
): Pair<Int, Int> {
    val itemSize: Pair<Int, Int> = if (
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
        Pair(mainAxis, crossAxis)
    } else {
        val mainAxis = mainAxisMin(orientation, Constraints.Infinity)
        val crossAxis = crossAxisMin(orientation, mainAxis)
        Pair(mainAxis, crossAxis)
    }
    return itemSize
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
