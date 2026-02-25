/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalGridApi::class)

package androidx.compose.foundation.layout

import androidx.annotation.FloatRange
import androidx.annotation.IntRange as AndroidXIntRange
import androidx.collection.LongList
import androidx.collection.MutableIntList
import androidx.collection.MutableIntSet
import androidx.collection.MutableObjectList
import androidx.collection.mutableLongListOf
import androidx.compose.foundation.layout.GridScope.Companion.GridIndexUnspecified
import androidx.compose.foundation.layout.GridScope.Companion.MaxGridIndex
import androidx.compose.foundation.layout.internal.JvmDefaultWithCompatibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A 2D layout composable that arranges children into a grid of rows and columns.
 *
 * The [Grid] allows defining explicit tracks (columns and rows) with various sizing capabilities,
 * including fixed sizes (`dp`), flexible fractions (`fr`), percentages, and content-based sizing
 * (`Auto`).
 *
 * **Key Features:**
 * * **Explicit vs. Implicit:** You define the main structure via [config] (explicit tracks). If
 *   items are placed outside these defined bounds, or if auto-placement creates new rows/columns,
 *   the grid automatically extends using implicit sizing (defaults to `Auto`).
 * * **Flexible Sizing:** Use [Fr] units (e.g., `1.fr`, `2.fr`) to distribute available space
 *   proportionally among tracks.
 * * **Auto-placement:** Items without a specific [GridScope.gridItem] modifier flow automatically
 *   into the next available cell based on the configured [GridFlow]. .
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleGrid
 * @sample androidx.compose.foundation.layout.samples.GridWithSpanningItems
 * @sample androidx.compose.foundation.layout.samples.GridWithAutoPlacement
 * @param config A block that defines the columns, rows, and gaps of the grid. This block runs
 *   during the measure pass, enabling efficient updates based on state.
 * @param modifier The modifier to be applied to the layout.
 * @param content The content of the grid. Direct children can use [GridScope.gridItem] to configure
 *   their position and span.
 * @see GridScope.gridItem
 * @see GridConfigurationScope
 */
@Composable
@ExperimentalGridApi
inline fun Grid(
    noinline config: GridConfigurationScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable GridScope.() -> Unit,
) {
    // Capture the latest config lambda in a State object.
    // This ensures we always have access to the latest lambda without recreating the policy.
    val currentConfig = rememberUpdatedState(config)

    // Create a stable MeasurePolicy instance.
    // We use 'remember' without keys so the policy instance itself never changes.
    // The policy reads 'currentConfig.value' inside measure(), triggering invalidation
    // when the config changes.
    val measurePolicy = remember { GridMeasurePolicy(currentConfig) }

    Layout(
        content = { GridScopeInstance.content() },
        modifier = modifier,
        measurePolicy = measurePolicy,
    )
}

/** Scope for the children of [Grid]. */
@LayoutScopeMarker
@Immutable
@JvmDefaultWithCompatibility
@ExperimentalGridApi
interface GridScope {
    /**
     * Configures the position, span, and alignment of an element within a [Grid] layout.
     *
     * Apply this modifier to direct children of a [Grid] composable.
     *
     * **Default Behavior:** If this modifier is not applied to a child, the child will be
     * automatically placed in the next available cell (spanning 1 row and 1 column) according to
     * the configured [GridFlow].
     *
     * **Indexing:** Grid row and column indices are **1-based**.
     * * **Positive** values count from the start (1 is the first row/column).
     * * **Negative** values count from the end (-1 is the last explicitly defined row/column).
     *
     * **Auto-placement:** If [row] or [column] are left to their default value
     * ([GridIndexUnspecified]), the [Grid] layout will automatically place the item based on the
     * configured [GridFlow].
     *
     * @param row The specific 1-based row index to place the item in. Positive values count from
     *   the start (1 is the first row). Negative values count from the end (-1 is the last row).
     *   Must be within the range [-[MaxGridIndex], [MaxGridIndex]]. Defaults to
     *   [GridIndexUnspecified] for auto-placement.
     * @param column The specific 1-based column index to place the item in. Positive values count
     *   from the start (1 is the first column). Negative values count from the end (-1 is the last
     *   column). Must be within the range [-[MaxGridIndex], [MaxGridIndex]]. Defaults to
     *   [GridIndexUnspecified] for auto-placement.
     * @param rowSpan The number of rows this item should occupy. Must be greater than 0. Defaults
     *   to 1.
     * @param columnSpan The number of columns this item should occupy. Must be greater than 0.
     *   Defaults to 1.
     * @param alignment Specifies how the content should be aligned within the grid cell(s) it
     *   occupies. Defaults to [Alignment.TopStart].
     * @throws IllegalArgumentException if [row] or [column] (when specified) are outside the valid
     *   range, or if [rowSpan] or [columnSpan] are less than 1.
     * @see GridIndexUnspecified
     * @see MaxGridIndex
     */
    @Stable
    fun Modifier.gridItem(
        @AndroidXIntRange(from = -MaxGridIndex.toLong(), to = MaxGridIndex.toLong())
        row: Int = GridIndexUnspecified,
        @AndroidXIntRange(from = -MaxGridIndex.toLong(), to = MaxGridIndex.toLong())
        column: Int = GridIndexUnspecified,
        @AndroidXIntRange(from = 1) rowSpan: Int = 1,
        @AndroidXIntRange(from = 1) columnSpan: Int = 1,
        alignment: Alignment = Alignment.TopStart,
    ): Modifier

    /**
     * Configures the position, span, and alignment of an element within a [Grid] layout using
     * ranges.
     *
     * This convenience overload converts [IntRange] inputs into row/column indices and spans.
     *
     * **Equivalence:**
     * - `rows = 4..5` maps to `row = 4`, `rowSpan = 2`.
     * - `columns = 1..1` maps to `column = 1`, `columnSpan = 1`.
     *
     * Example: `Modifier.gridItem(rows = 2..3, columns = 1..2)` is functionally equivalent to
     * `Modifier.gridItem(row = 2, rowSpan = 2, column = 1, columnSpan = 2)`.
     *
     * @param rows The range of rows to occupy (e.g., `1..2`). The start determines the row index,
     *   and the size of the range determines the span.
     * @param columns The range of columns to occupy (e.g., `1..3`). The start determines the column
     *   index, and the size of the range determines the span.
     * @param alignment Specifies how the content should be aligned within the grid cell(s).
     *   Defaults to [Alignment.TopStart].
     * @see Modifier.gridItem
     */
    @Stable
    fun Modifier.gridItem(
        rows: IntRange,
        columns: IntRange,
        alignment: Alignment = Alignment.TopStart,
    ): Modifier

    companion object {
        /**
         * The maximum allowed index for a row or column (inclusive).
         *
         * This hard limit prevents performance degradation, layout timeouts, or memory issues
         * potentially caused by accidental loop overflows or unreasonably large sparse grid
         * definitions.
         */
        @ExperimentalGridApi const val MaxGridIndex: Int = 1000

        /**
         * Sentinel value indicating that a grid position (row or column) is not manually specified
         * and should be determined automatically by the layout flow.
         */
        @ExperimentalGridApi const val GridIndexUnspecified: Int = 0
    }
}

/** Internal implementation of [GridScope]. Stateless object to avoid allocations. */
@PublishedApi
@ExperimentalGridApi
internal object GridScopeInstance : GridScope {

    override fun Modifier.gridItem(
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int,
        alignment: Alignment,
    ): Modifier {
        if (row != GridIndexUnspecified) {
            require(row in -MaxGridIndex..MaxGridIndex) {
                "row must be between -$MaxGridIndex and $MaxGridIndex"
            }
        }
        if (column != GridIndexUnspecified) {
            require(column in -MaxGridIndex..MaxGridIndex) {
                "column must be between -$MaxGridIndex and $MaxGridIndex"
            }
        }
        require(rowSpan > 0) { "rowSpan must be > 0" }
        require(columnSpan > 0) { "columnSpan must be > 0" }
        return this.then(GridItemElement(row, column, rowSpan, columnSpan, alignment))
    }

    override fun Modifier.gridItem(
        rows: IntRange,
        columns: IntRange,
        alignment: Alignment,
    ): Modifier {
        require(!rows.isEmpty()) { "Row range ($rows) cannot be empty" }
        require(!columns.isEmpty()) { "Column range ($columns) cannot be empty" }

        val row = rows.first
        val rowSpan = rows.last - rows.first + 1
        val column = columns.first
        val columnSpan = columns.last - columns.first + 1
        return this.gridItem(row, column, rowSpan, columnSpan, alignment)
    }
}

/**
 * Scope for configuring the structure of a [Grid].
 *
 * This interface is implemented by the configuration block in [Grid]. It allows defining columns,
 * rows, and gaps.
 *
 * The order in which [column] and [row] functions are called within the `config` block is
 * important. Tracks are added to the grid definition sequentially based on these calls. For
 * example, calling `column(100.dp)` twice defines two columns.
 *
 * Gap configuration calls ([gap], [rowGap], [columnGap]) follow a "last-call-wins" policy for their
 * respective axes.
 *
 * @sample androidx.compose.foundation.layout.samples.GridConfigurationDslSample
 * @sample androidx.compose.foundation.layout.samples.GridWithConstraints
 */
@LayoutScopeMarker
@ExperimentalGridApi
interface GridConfigurationScope : Density {

    /**
     * The layout constraints passed to this [Grid] from its parent.
     *
     * These constraints represent the minimum and maximum size limits that the parent has imposed
     * on this Grid. This can be useful for creating responsive layouts that adapt based on
     * available space.
     *
     * @see Constraints
     */
    val constraints: Constraints

    /**
     * The direction in which items that do not specify a position are placed. Defaults to
     * [GridFlow.Row].
     */
    var flow: GridFlow

    /** Defines a fixed-width column. Maps to [GridTrackSize.Fixed]. */
    fun column(size: Dp)

    /** Defines a flexible column. Maps to [GridTrackSize.Flex]. */
    fun column(weight: Fr)

    /**
     * Defines a percentage-based column. Maps to [GridTrackSize.Percentage].
     *
     * @param percentage The percentage (0.0 to 1.0) of the available space.
     */
    fun column(@FloatRange(from = 0.0, to = 1.0) percentage: Float)

    /** Defines a new column track with the specified [size]. */
    fun column(size: GridTrackSize)

    /** Defines a fixed-width row. Maps to [GridTrackSize.Fixed]. */
    fun row(size: Dp)

    /** Defines a flexible row. Maps to [GridTrackSize.Flex]. */
    fun row(weight: Fr)

    /**
     * Defines a percentage-based row. Maps to [GridTrackSize.Percentage].
     *
     * @param percentage The percentage (0.0 to 1.0) of the available space.
     */
    fun row(@FloatRange(from = 0.0, to = 1.0) percentage: Float)

    /** Defines a new row track with the specified [size]. */
    fun row(size: GridTrackSize)

    /**
     * Sets both the row and column gaps (gutters) to [all].
     *
     * **Precedence:** If this is called multiple times, or mixed with [columnGap] or [rowGap], the
     * **last call** takes precedence.
     *
     * @throws IllegalArgumentException if [all] is negative.
     */
    fun gap(all: Dp)

    /**
     * Sets independent gaps for rows and columns.
     *
     * **Precedence:** If this is called multiple times, or mixed with [columnGap] or [rowGap], the
     * **last call** takes precedence.
     *
     * @throws IllegalArgumentException if [row] or [column] is negative.
     */
    fun gap(row: Dp, column: Dp)

    /**
     * Sets the gap (gutter) size between columns.
     *
     * **Precedence:** If this is called multiple times, the **last call** takes precedence. This
     * call will overwrite the column component of any previous [gap] call.
     *
     * @throws IllegalArgumentException if [gap] is negative.
     */
    fun columnGap(gap: Dp)

    /**
     * Sets the gap (gutter) size between rows.
     *
     * **Precedence:** If this is called multiple times, the **last call** takes precedence. This
     * call will overwrite the row component of any previous [gap] call.
     *
     * @throws IllegalArgumentException if [gap] is negative.
     */
    fun rowGap(gap: Dp)

    /** Creates an [Fr] unit from an [Int]. */
    @Stable
    @ExperimentalGridApi
    val Int.fr: Fr
        get() = Fr(this.toFloat())

    /** Creates an [Fr] unit from a [Float]. */
    @Stable
    @ExperimentalGridApi
    val Float.fr: Fr
        get() = Fr(this)

    /** Creates an [Fr] unit from a [Double]. */
    @Stable
    @ExperimentalGridApi
    val Double.fr: Fr
        get() = Fr(this.toFloat())
}

/** Adds multiple columns with the specified [specs]. */
@ExperimentalGridApi
fun GridConfigurationScope.columns(vararg specs: GridTrackSpec) {
    for (spec in specs) {
        if (spec is GridTrackSize) {
            column(spec)
        }
    }
}

/** Adds multiple rows with the specified [specs]. */
@ExperimentalGridApi
fun GridConfigurationScope.rows(vararg specs: GridTrackSpec) {
    for (spec in specs) {
        if (spec is GridTrackSize) {
            row(spec)
        }
    }
}

/** Defines the direction in which auto-placed items flow within the grid. */
@JvmInline
@ExperimentalGridApi
value class GridFlow @PublishedApi internal constructor(private val bits: Int) {

    companion object {
        /** Items are placed filling the first row, then moving to the next row. */
        @ExperimentalGridApi
        inline val Row
            get() = GridFlow(0)

        /** Items are placed filling the first column, then moving to the next column. */
        @ExperimentalGridApi
        inline val Column
            get() = GridFlow(1)
    }

    override fun toString(): String =
        when (this) {
            Row -> "Row"
            Column -> "Column"
            else -> "GridFlow($bits)"
        }
}

/**
 * Represents a flexible unit used for sizing [Grid] tracks.
 *
 * One [Fr] unit represents a fraction of the *remaining* space in the grid container after
 * [GridTrackSize.Fixed] and [GridTrackSize.Percentage] tracks have been allocated.
 *
 * When multiple tracks use [Fr] units (e.g., `1.fr`, `2.fr`, `1.fr`), the remaining space is
 * divided proportionally to their weights. The total number of "fractional units" is the sum of all
 * weights (in the example, 1 + 2 + 1 = 4). Each track receives a share of the space equal to its
 * weight divided by the total weight.
 * - The `1.fr` tracks would each get 1/4 of the remaining space.
 * - The `2.fr` track would get 2/4 (or 1/2) of the remaining space.
 */
@JvmInline
@ExperimentalGridApi
value class Fr(val value: Float) {
    override fun toString(): String = "$value.fr"
}

/**
 * Marker interface to enable vararg usage with [GridTrackSize].
 *
 * This allows the configuration DSL to accept [GridTrackSize] items in a vararg (e.g.,
 * `columns(Fixed(10.dp), Flex(1.fr))`), bypassing the Kotlin limitation on value class varargs.
 */
@ExperimentalGridApi sealed interface GridTrackSpec

/**
 * Defines the size of a track (a row or a column) in a [Grid].
 *
 * Use the companion functions (e.g., [Fixed], [Flex]) to create instances.
 */
@Immutable
@JvmInline
@ExperimentalGridApi
value class GridTrackSize internal constructor(internal val encodedValue: Long) : GridTrackSpec {

    internal val type: Int
        get() = (encodedValue ushr 32).toInt()

    internal val value: Float
        get() = Float.fromBits(encodedValue.toInt())

    override fun toString(): String =
        when (type) {
            TypeFixed -> "Fixed(${value}dp)"
            TypePercentage -> "Percentage($value)"
            TypeFlex -> "Flex(${value}fr)"
            TypeMinContent -> "MinContent"
            TypeMaxContent -> "MaxContent"
            TypeAuto -> "Auto"
            else -> "Unknown"
        }

    companion object {
        internal const val TypeFixed = 1
        internal const val TypePercentage = 2
        internal const val TypeFlex = 3
        internal const val TypeMinContent = 4
        internal const val TypeMaxContent = 5
        internal const val TypeAuto = 6

        /**
         * A track with a fixed [Dp] size.
         *
         * @param size The size of the track.
         * @throws IllegalArgumentException if [size] is negative or [Dp.Unspecified].
         */
        @Stable
        fun Fixed(size: Dp): GridTrackSize {
            require(size != Dp.Unspecified && size.value >= 0f) {
                "Fixed size must be non-negative and specified (was $size)"
            }
            return pack(TypeFixed, size.value)
        }

        /**
         * A track sized as a percentage of the **total** available size of the grid container.
         * **Note:** In this implementation, percentages are calculated based on the **remaining
         * available space after gaps**. This differs from the W3C CSS Grid spec, where percentages
         * are based on the container size regardless of gaps. This behavior prevents unexpected
         * overflows when mixing gaps and percentages (e.g., `50%` + `50%` + `gap` will fit
         * perfectly here, but would overflow in CSS).
         *
         * @param value The percentage of the container size.
         * @throws IllegalArgumentException if [value] is negative.
         */
        @Stable
        fun Percentage(@FloatRange(from = 0.0) value: Float): GridTrackSize {
            require(value >= 0f) { "Percentage cannot be negative" }
            return pack(TypePercentage, value)
        }

        /**
         * A flexible track that takes a share of the **remaining** space after Fixed and Percentage
         * tracks are allocated.
         *
         * @param weight The flexible weight. Space is distributed proportional to this weight
         *   divided by the total flex weight. Must be non-negative.
         * @throws IllegalArgumentException if [weight] is negative.
         */
        @Stable
        fun Flex(@FloatRange(from = 0.0) weight: Fr): GridTrackSize {
            require(weight.value >= 0f) { "Flex weight must be positive" }
            return pack(TypeFlex, weight.value)
        }

        /** A track that sizes itself to fit the minimum intrinsic size of its contents. */
        val MinContent = pack(TypeMinContent, 0f)

        /** A track that sizes itself to fit the maximum intrinsic size of its contents. */
        val MaxContent = pack(TypeMaxContent, 0f)

        /**
         * A track that behaves as minmax(min-content, max-content). It occupies at least its
         * minimum content size, and grows to fit its maximum content size if space is available.
         */
        val Auto = pack(TypeAuto, 0f)

        private fun pack(type: Int, value: Float): GridTrackSize {
            // Pack Type (High 32) and Float bits (Low 32) into one Long.
            // Mask 0xFFFFFFFFL prevents sign extension when casting int to long.
            val raw = (type.toLong() shl 32) or (value.toRawBits().toLong() and 0xFFFFFFFFL)
            return GridTrackSize(raw)
        }
    }
}

/**
 * The modifier element that creates and updates [GridItemNode].
 *
 * @property row The 1-based row index, or [GridScope.GridIndexUnspecified] for auto-placement.
 * @property column The 1-based column index, or [GridScope.GridIndexUnspecified] for
 *   auto-placement.
 * @property rowSpan The number of rows the item should occupy.
 * @property columnSpan The number of columns the item should occupy.
 * @property alignment The alignment of the content within the grid cell.
 * @see GridItemNode
 */
private class GridItemElement(
    val row: Int,
    val column: Int,
    val rowSpan: Int,
    val columnSpan: Int,
    val alignment: Alignment,
) : ModifierNodeElement<GridItemNode>() {
    override fun create(): GridItemNode = GridItemNode(row, column, rowSpan, columnSpan, alignment)

    override fun update(node: GridItemNode) {
        node.row = row
        node.column = column
        node.rowSpan = rowSpan
        node.columnSpan = columnSpan
        node.alignment = alignment
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "gridItem"
        properties["row"] = row
        properties["column"] = column
        properties["rowSpan"] = rowSpan
        properties["columnSpan"] = columnSpan
        properties["alignment"] = alignment
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GridItemElement) return false

        if (row != other.row) return false
        if (column != other.column) return false
        if (rowSpan != other.rowSpan) return false
        if (columnSpan != other.columnSpan) return false
        if (alignment != other.alignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + column
        result = 31 * result + rowSpan
        result = 31 * result + columnSpan
        result = 31 * result + alignment.hashCode()
        return result
    }
}

/**
 * The modifier node that provides parent data to the [Grid] layout.
 *
 * This class implements [ParentDataModifierNode], allowing the parent [Grid] layout to inspect the
 * configuration (row, column, spans) of this specific child during the measurement phase via the
 * [modifyParentData] method.
 *
 * @property row The 1-based row index, or [GridScope.GridIndexUnspecified] for auto-placement.
 * @property column The 1-based column index, or [GridScope.GridIndexUnspecified] for
 *   auto-placement.
 * @property rowSpan The number of rows the item should occupy.
 * @property columnSpan The number of columns the item should occupy.
 * @property alignment The alignment of the content within the grid cell.
 * @throws IllegalArgumentException if [rows] or [columns] ranges are empty, or if the derived
 *   row/column indices or spans do not meet the requirements of the primary [gridItem] function.
 * @see GridScope.gridItem for the public API and input validation.
 */
private class GridItemNode(
    var row: Int,
    var column: Int,
    var rowSpan: Int,
    var columnSpan: Int,
    var alignment: Alignment,
) : Modifier.Node(), ParentDataModifierNode {
    override fun Density.modifyParentData(parentData: Any?) = this@GridItemNode
}

/** A stable MeasurePolicy that reads configuration from a State. */
@PublishedApi
@ExperimentalGridApi
internal class GridMeasurePolicy(
    private val configState: State<GridConfigurationScope.() -> Unit>
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        // 1. Run Configuration DSL
        val gridConfig = GridConfigurationScopeImpl(this, constraints).apply(configState.value)

        // 2. Resolve Grid Item Indices (Resolve explicit and Auto placement)
        // This calculates the concrete index (row, col) for every item and determines total grid
        // size.
        val resolvedGridItemsResult =
            resolveGridItemIndices(
                measurables = measurables,
                columnSpecs = gridConfig.columnSpecs,
                rowSpecs = gridConfig.rowSpecs,
                flow = gridConfig.flow,
            )

        // 3. Resolve Track Sizes
        val trackSizes =
            calculateGridTrackSizes(
                density = this,
                gridItems = resolvedGridItemsResult.gridItems,
                columnSpecs = gridConfig.columnSpecs,
                rowSpecs = gridConfig.rowSpecs,
                totalColCount = resolvedGridItemsResult.gridSize.width,
                totalRowCount = resolvedGridItemsResult.gridSize.height,
                columnGap = gridConfig.columnGap,
                rowGap = gridConfig.rowGap,
                constraints = constraints,
            )

        // 4. Measure Children
        // Measures content constraints based on track sizes and mutates GridItem with result.
        measureItems(
            gridItems = resolvedGridItemsResult.gridItems,
            trackSizes = trackSizes,
            layoutDirection = layoutDirection,
        )

        // 5. Layout
        // Coerce the final size within constraints.
        // If content is larger, it will overflow (report Max).
        // If content is smaller than Min, it will expand (report Min).
        val layoutWidth = constraints.constrainWidth(trackSizes.totalWidth)
        val layoutHeight = constraints.constrainHeight(trackSizes.totalHeight)
        return layout(layoutWidth, layoutHeight) {
            val columnOffsets =
                calculateTrackOffsets(trackSizes.columnWidths, trackSizes.columnGapPx)
            val rowOffsets = calculateTrackOffsets(trackSizes.rowHeights, trackSizes.rowGapPx)
            resolvedGridItemsResult.gridItems.forEach { gridItem ->
                val placeable = gridItem.placeable
                // Only place if measurement succeeded (guard against edge cases)
                if (placeable != null) {
                    val x = columnOffsets[gridItem.column] + gridItem.offsetX
                    val y = rowOffsets[gridItem.row] + gridItem.offsetY
                    placeable.place(x, y)
                }
            }
        }
    }
}

private class GridConfigurationScopeImpl(density: Density, override val constraints: Constraints) :
    GridConfigurationScope, Density by density {
    val columnSpecs = mutableLongListOf()
    val rowSpecs = mutableLongListOf()
    var columnGap: Dp = 0.dp
    var rowGap: Dp = 0.dp

    override var flow: GridFlow = GridFlow.Row

    override fun column(size: Dp) {
        column(GridTrackSize.Fixed(size))
    }

    override fun column(weight: Fr) {
        column(GridTrackSize.Flex(weight))
    }

    override fun column(percentage: Float) {
        column(GridTrackSize.Percentage(percentage))
    }

    override fun column(size: GridTrackSize) {
        columnSpecs.add(size.encodedValue)
    }

    override fun row(size: Dp) {
        row(GridTrackSize.Fixed(size))
    }

    override fun row(weight: Fr) {
        row(GridTrackSize.Flex(weight))
    }

    override fun row(percentage: Float) {
        row(GridTrackSize.Percentage(percentage))
    }

    override fun row(size: GridTrackSize) {
        rowSpecs.add(size.encodedValue)
    }

    override fun gap(all: Dp) {
        require(all.value >= 0f) { "Gap must be non-negative" }
        columnGap = all
        rowGap = all
    }

    override fun gap(row: Dp, column: Dp) {
        require(row.value >= 0f) { "Row gap must be non-negative" }
        require(column.value >= 0f) { "Column gap must be non-negative" }
        rowGap = row
        columnGap = column
    }

    override fun columnGap(gap: Dp) {
        require(gap.value >= 0f) { "Column gap must be non-negative" }
        columnGap = gap
    }

    override fun rowGap(gap: Dp) {
        require(gap.value >= 0f) { "Row gap must be non-negative" }
        rowGap = gap
    }
}

/**
 * A mutable state object representing a single child in the Grid throughout the layout lifecycle.
 *
 * This object is created once during the [resolveGridItemIndices] phase (containing only placement
 * info) and is reused and mutated during the [measureItems] phase to store the resulting
 * [Placeable] and calculation offsets. This significantly reduces object allocation per layout
 * pass.
 */
private class GridItem(
    val measurable: Measurable,
    var row: Int,
    var column: Int,
    var rowSpan: Int,
    var columnSpan: Int,
    val alignment: Alignment,
    var placeable: Placeable? = null,
    var offsetX: Int = 0,
    var offsetY: Int = 0,
)

/**
 * The output of the [resolveGridItemIndices] algorithm.
 *
 * This container holds the complete layout plan required for subsequent measurement phases. It
 * encapsulates both the individual item positions and the aggregate dimensions of the grid.
 *
 * The [gridSize] is critical because it reveals the extent of the "Implicit Grid" — tracks that
 * were not explicitly defined by the user but were created automatically to accommodate auto-placed
 * items or items with out-of-bounds indices.
 *
 * @property gridItems The list of all items with their resolved (row, column) coordinates.
 * @property gridSize The total number of rows and columns required to house all items. (width =
 *   total columns, height = total rows).
 */
private class ResolvedGridItemIndicesResult(
    val gridItems: MutableObjectList<GridItem>,
    val gridSize: IntSize,
)

/**
 * The "Master Blueprint" holding the calculated pixel dimensions for the entire grid.
 *
 * This class acts as a lookup table during the measurement phase. Instead of recalculating sizes
 * for every item, we compute the track sizes once and pass this object around.
 *
 * @property columnWidths Array containing the exact width in pixels for each column index.
 * @property rowHeights Array containing the exact height in pixels for each row index.
 * @property totalWidth The sum of all column widths plus gaps.
 * @property totalHeight The sum of all row heights plus gaps.
 * @property columnGapPx The spacing between columns.
 * @property rowGapPx The spacing between rows.
 */
private class GridTrackSizes(
    val columnWidths: IntArray,
    val rowHeights: IntArray,
    val totalWidth: Int,
    val totalHeight: Int,
    val columnGapPx: Int,
    val rowGapPx: Int,
)

/**
 * Executes the "Sparse Packing" auto-placement algorithm to resolve every item's position.
 *
 * This function is the "Engine" of the auto-placement logic. It transforms a list of raw
 * measurables (with potentially unspecified `row`/`column` values) into a concrete plan where every
 * item has a specific (row, column) coordinate.
 *
 * **Algorithm Overview:**
 * 1. **Explicit Placement:** Items with both `row` and `column` manually specified are placed
 *    first. They anchor the grid and do not move.
 * 2. **Auto-Placement Cursor:** A "cursor" (current row/column pointer) tracks the next available
 *    position.
 * 3. **Filling Gaps:** The algorithm iterates through the remaining items. For each item:
 * - It advances the cursor to the first slot that can accommodate the item's span without
 *   overlapping existing items.
 * - It respects the [flow] direction (Row-major vs Column-major).
 * - It creates "Implicit Tracks" (expanding the grid bounds) if an item is placed outside the
 *   currently defined area.
 *
 * @param measurables The raw list of children to place.
 * @param columnSpecs The explicit column definitions (used to determine wrapping points).
 * @param rowSpecs The explicit row definitions (used to determine wrapping points).
 * @param flow The direction ([GridFlow.Row] or [GridFlow.Column]) to fill the grid.
 * @return A [ResolvedGridItemIndicesResult] containing the final positions and the *total* grid
 *   dimensions (Explicit + Implicit).
 */
private fun resolveGridItemIndices(
    measurables: List<Measurable>,
    columnSpecs: LongList,
    rowSpecs: LongList,
    flow: GridFlow,
): ResolvedGridItemIndicesResult {
    val gridItems = MutableObjectList<GridItem>(measurables.size)

    // Key = (row shl 16) | (column & 0xFFFF)
    // Supports up to 65,535 rows/cols (well within MaxGridIndex).
    val occupiedCells = MutableIntSet()

    val explicitColCount = columnSpecs.size
    val explicitRowCount = rowSpecs.size

    // Track the effective size of the grid (starts at explicit size, expands if items are placed
    // outside)
    var maxRow = explicitRowCount
    var maxCol = explicitColCount

    // Pack into Int (Row in high 16 bits, Col in low 16 bits)
    // Supports up to 65,535 rows/cols.
    fun packCoordinate(row: Int, column: Int): Int = (row shl 16) or (column and 0xFFFF)

    // Checks if the target area (defined by start position and span) overlaps with any existing
    // item.
    fun isAreaOccupied(startRow: Int, startCol: Int, rowSpan: Int, colSpan: Int): Boolean {
        // Fast-path: Check boundary limits first
        if (startRow + rowSpan > MaxGridIndex || startCol + colSpan > MaxGridIndex) return true
        for (r in startRow until startRow + rowSpan) {
            for (c in startCol until startCol + colSpan) {
                if (occupiedCells.contains(packCoordinate(r, c))) return true
            }
        }
        return false
    }

    // Marks the cells in the area as occupied.
    fun markAreaOccupied(startRow: Int, startCol: Int, rowSpan: Int, colSpan: Int) {
        for (r in startRow until startRow + rowSpan) {
            for (c in startCol until startCol + colSpan) {
                occupiedCells.add(packCoordinate(r, c))
            }
        }
    }

    // The "Cursor" tracks the position of the last auto-placed item.
    // Subsequent auto-placed items attempt to start searching from here to avoid re-scanning the
    // whole grid.
    var autoPlacementCursorRow = 0
    var autoPlacementCursorCol = 0

    measurables.fastForEach { measurable ->
        val data = measurable.parentData as? GridItemNode
        val rowSpan = data?.rowSpan ?: 1
        val colSpan = data?.columnSpan ?: 1

        // Convert 1-based user indices to 0-based internal indices.
        // Returns null if the user index was unspecified (Auto).
        val requestedRow =
            resolveToZeroBasedIndex(data?.row ?: GridIndexUnspecified, explicitRowCount)
        val requestedCol =
            resolveToZeroBasedIndex(data?.column ?: GridIndexUnspecified, explicitColCount)

        var finalRow = -1
        var finalCol = -1

        // 1. Fully Explicit (Row & Column fixed)
        // We simply place it there. Overlaps are allowed for explicit placement.
        if (requestedRow != -1 && requestedCol != -1) {
            finalRow = requestedRow
            finalCol = requestedCol
        }
        // 2. Fixed Row (Search for Column)
        else if (requestedRow != -1) {
            // Search for the first available column in the specified row.
            finalRow = requestedRow
            var candidateCol = 0

            // If flowing by Row, and we are on the cursor's row, start searching from cursor
            if (flow == GridFlow.Row && requestedRow == autoPlacementCursorRow) {
                candidateCol = autoPlacementCursorCol
            }
            while (candidateCol < MaxGridIndex) {
                if (!isAreaOccupied(requestedRow, candidateCol, rowSpan, colSpan)) {
                    finalCol = candidateCol
                    break
                }
                candidateCol++
            }
        }
        // 3. Fixed Column (Search for Row)
        else if (requestedCol != -1) {
            // Search for the first available row in the specified column.
            finalCol = requestedCol
            var candidateRow = 0
            // If flowing by Column, and we are on the cursor's col, start searching from cursor
            if (flow == GridFlow.Column && requestedCol == autoPlacementCursorCol) {
                candidateRow = autoPlacementCursorRow
            }
            while (candidateRow < MaxGridIndex) {
                if (!isAreaOccupied(candidateRow, requestedCol, rowSpan, colSpan)) {
                    finalRow = candidateRow
                    break
                }
                candidateRow++
            }
        }
        // 4. Fully Auto (Search for Slot)
        else {
            // Start searching from the current cursor position.
            var candidateRow = autoPlacementCursorRow
            var candidateCol = autoPlacementCursorCol

            while (candidateRow < MaxGridIndex && candidateCol < MaxGridIndex) {
                // Wrapping Logic
                // If the item doesn't fit in the current track (explicit bounds), wrap to next.
                if (flow == GridFlow.Row) {
                    // If we have explicit columns and exceed them...
                    if (explicitColCount > 0 && candidateCol + colSpan > explicitColCount) {
                        // If we are NOT at start, wrap.
                        // If we ARE at start (0) and still don't fit, we must overflow (create
                        // implicit track).
                        if (candidateCol > 0) {
                            candidateCol = 0
                            candidateRow++
                            continue // Re-evaluate wrapping at new position
                        }
                    }
                } else { // GridFlow.Column
                    if (explicitRowCount > 0 && candidateRow + rowSpan > explicitRowCount) {
                        if (candidateRow > 0) {
                            candidateRow = 0
                            candidateCol++
                            continue
                        }
                    }
                }

                if (!isAreaOccupied(candidateRow, candidateCol, rowSpan, colSpan)) {
                    finalRow = candidateRow
                    finalCol = candidateCol
                    break
                }

                // Increment
                if (flow == GridFlow.Row) {
                    candidateCol++
                    // If we drift too far right without wrapping (infinite grid), force wrap safety
                    if (candidateCol > MaxGridIndex) {
                        candidateCol = 0
                        candidateRow++
                    }
                } else {
                    candidateRow++
                    if (candidateRow > MaxGridIndex) {
                        candidateRow = 0
                        candidateCol++
                    }
                }
            }
        }

        // If auto-placement failed to find a spot (e.g. MaxGridIndex reached),
        // we default to 0,0 to avoid crashing, though visual overlap will occur.
        val placementRow = max(0, finalRow)
        val placementCol = max(0, finalCol)

        markAreaOccupied(placementRow, placementCol, rowSpan, colSpan)

        // Populate the mutable GridItem
        gridItems.add(
            GridItem(
                measurable = measurable,
                row = placementRow,
                column = placementCol,
                rowSpan = rowSpan,
                columnSpan = colSpan,
                alignment = data?.alignment ?: Alignment.TopStart,
            )
        )

        // Expand total grid bounds if necessary
        maxRow = max(maxRow, placementRow + rowSpan)
        maxCol = max(maxCol, placementCol + colSpan)

        // Update Cursor (Only for non-explicit placements)
        // Only update cursor if the item was NOT fully explicit.
        // Explicit items are "out of flow" and shouldn't drag the cursor with them.
        if (requestedRow == -1 || requestedCol == -1) {
            if (flow == GridFlow.Row) {
                autoPlacementCursorRow = placementRow
                autoPlacementCursorCol = placementCol + colSpan
            } else {
                autoPlacementCursorRow = placementRow + rowSpan
                autoPlacementCursorCol = placementCol
            }
        }
    }

    return ResolvedGridItemIndicesResult(gridItems, IntSize(maxCol, maxRow))
}

/**
 * Resolves a 1-based user index (positive or negative) to a 0-based concrete index.
 *
 * @param index The user-provided index (e.g., 1, -1, or [GridIndexUnspecified]).
 * @param maxCount The number of explicit tracks defined (used for negative index resolution).
 * @return The 0-based index, or -1 if the index was unspecified or invalid (e.g. negative index out
 *   of bounds).
 */
private fun resolveToZeroBasedIndex(index: Int, maxCount: Int): Int {
    if (index == GridIndexUnspecified) return -1

    // Positive Index (e.g., 5): Maps to 4.
    // Always valid (allows creating implicit tracks if > maxCount).
    if (index > 0) return index - 1

    // Negative Index (e.g., -1): Maps to maxCount - 1.
    // Must check if it points to a valid explicit track [0..maxCount-1].
    // If it points before 0 (e.g. -5 in a 2-row grid), it is invalid.
    val resolved = maxCount + index
    return if (resolved >= 0) resolved else -1
}

/**
 * Resolves the abstract [GridTrackSize] specifications for all rows and columns into concrete pixel
 * dimensions. This function is the core of the size calculation logic for the [Grid].
 *
 * **Calculation Order:** The calculation is performed in two main phases:
 * 1. **Column Widths:** Column widths are calculated first. This is crucial because the height of
 *    many UI elements (like text) depends on the available width.
 * 2. **Row Heights:** Row heights are calculated second, utilizing the resolved column widths to
 *    accurately measure items, especially those with content that wraps.
 *
 * **Track Type Resolution:** Within each phase, different [GridTrackSize] types are resolved as
 * follows:
 * - [GridTrackSize.Fixed]: Converted directly to pixels using the [density].
 * - [GridTrackSize.Percentage]: Calculated based on the available space for tracks (after
 *   subtracting gaps). Falls back to content-based size (MaxContent) if the available space on that
 *   axis is infinite (e.g., in a scrollable container).
 * - [GridTrackSize.MinContent], [GridTrackSize.MaxContent], [GridTrackSize.Auto]: Determined by
 *   measuring the intrinsic sizes of the items within the track. `Auto` typically behaves like
 *   `MaxContent`.
 * - [GridTrackSize.Flex]: Initially sized to their minimum content size. After all other types and
 *   spanning items are accounted for, any remaining space is distributed proportionally among flex
 *   tracks.
 *
 * **Implicit Tracks:** Tracks not explicitly defined in [columnSpecs] or [rowSpecs] (i.e., indices
 * beyond the spec list sizes) are treated as `GridTrackSize.Auto`.
 *
 * **Spanning Items:** The function accounts for items spanning multiple tracks, potentially
 * increasing the sizes of growable tracks ([Auto], [MinContent], [MaxContent], [Flex]) to
 * accommodate them.
 *
 * @param density The current screen density, used for converting Dp to pixels.
 * @param gridItems The list of all grid items, including their placement and spans.
 * @param columnSpecs The explicit configurations for columns.
 * @param rowSpecs The explicit configurations for rows.
 * @param totalColCount The total number of columns in the grid (explicit + implicit).
 * @param totalRowCount The total number of rows in the grid (explicit + implicit).
 * @param constraints The layout constraints from the parent composable.
 * @param columnGap The spacing in Dp between columns.
 * @param rowGap The spacing in Dp between rows.
 * @return A [GridTrackSizes] object containing the calculated pixel sizes for each column and row,
 *   the total grid dimensions, and the gap sizes in pixels.
 */
private fun calculateGridTrackSizes(
    density: Density,
    gridItems: MutableObjectList<GridItem>,
    columnSpecs: LongList,
    rowSpecs: LongList,
    totalColCount: Int, // Total (Implicit + Explicit)
    totalRowCount: Int, // Total (Implicit + Explicit)
    constraints: Constraints,
    columnGap: Dp,
    rowGap: Dp,
): GridTrackSizes {
    val colGapPx = with(density) { columnGap.roundToPx() }
    val rowGapPx = with(density) { rowGap.roundToPx() }

    // Group items by track index to avoid O(Tracks * Items) loop
    // Array of lists, where index corresponds to the column index
    val itemsByColumn = arrayOfNulls<MutableObjectList<GridItem>>(totalColCount)
    // Array of lists, where index corresponds to the row index
    val itemsByRow = arrayOfNulls<MutableObjectList<GridItem>>(totalRowCount)

    gridItems.forEach { item ->
        // Populate Column Lookup
        if (item.column < totalColCount) {
            val list =
                itemsByColumn[item.column]
                    ?: MutableObjectList<GridItem>().also { itemsByColumn[item.column] = it }
            list.add(item)
        }
        // Populate Row Lookup
        if (item.row < totalRowCount) {
            val list =
                itemsByRow[item.row]
                    ?: MutableObjectList<GridItem>().also { itemsByRow[item.row] = it }
            list.add(item)
        }
    }

    // --- Phase 1: Calculate Column Widths ---
    // Use totalColCount for array size
    val columnWidths = IntArray(totalColCount)

    val totalTrackWidth =
        calculateColumnWidths(
            density = density,
            explicitSpecs = columnSpecs,
            totalCount = totalColCount,
            availableSpace = constraints.maxWidth,
            outSizes = columnWidths,
            itemsByColumn = itemsByColumn,
            constraints = constraints,
            gridItems = gridItems,
            columnGap = colGapPx,
        )

    // --- Phase 2: Calculate Row Heights ---
    val rowHeights = IntArray(totalRowCount)

    val totalTrackHeight =
        calculateRowHeights(
            density = density,
            explicitSpecs = rowSpecs,
            totalCount = totalRowCount,
            availableSpace = constraints.maxHeight,
            outSizes = rowHeights,
            itemsByRow = itemsByRow,
            constraints = constraints,
            columnWidths = columnWidths,
            gridItems = gridItems,
            rowGap = rowGapPx,
        )

    val totalColumnGap = max(0, columnSpecs.size - 1) * colGapPx
    val totalRowGap = max(0, rowSpecs.size - 1) * rowGapPx

    return GridTrackSizes(
        columnWidths = columnWidths,
        rowHeights = rowHeights,
        columnGapPx = colGapPx,
        rowGapPx = rowGapPx,
        totalWidth = totalTrackWidth + totalColumnGap,
        totalHeight = totalTrackHeight + totalRowGap,
    )
}

/**
 * Calculates the specific pixel width of every column in the grid.
 *
 * This function implements the horizontal axis sizing logic. It resolves column widths based on
 * explicit configuration, available space, and content intrinsic sizes.
 *
 * **Algorithm Overview:**
 * 1. **Pass 1 (Base Sizes):** Calculates the initial width of each column based on its
 *    [GridTrackSize].
 * * **Implicit Tracks:** Indices beyond `explicitSpecs` default to [GridTrackSize.Auto].
 * * **Fixed:** Resolves directly to pixels.
 * * **Percentage:** Resolves against total available width. Falls back to `Auto` (MaxContent) if
 *   width is infinite (e.g., inside a container made horizontally scrollable with the
 *   `horizontalScroll` modifier).
 * * **Flex:** Starts at `min-content` size to prevent collapse if content exists.
 * * **Auto/Content-based:** Measured using the intrinsic width of items in that column.
 * 2. **Pass 1.5 (Spanning Items):** Increases column widths if an item spanning multiple columns
 *    requires more width than the sum of those columns.
 * 3. **Pass 1.8 (Expand Auto Tracks):** Distributes remaining available space to `Auto` tracks,
 *    allowing them to grow from their `min-content` floor toward their `max-content` cap. This
 *    ensures `Auto` columns are responsive but don't aggressively consume space needed for `Flex`
 *    columns.
 * 4. **Pass 2 (Flex Distribution):** Distributes any remaining horizontal space among
 *    [GridTrackSize.Flex] columns according to their weight.
 *
 * @param density Used for Dp-to-Px conversion.
 * @param explicitSpecs The user-defined column configurations.
 * @param totalCount The total number of columns (explicit + implicit).
 * @param availableSpace The maximum width available (or [Constraints.Infinity]).
 * @param outSizes Output array where calculated widths are stored. **Mutated in-place**.
 * @param itemsByColumn Optimization lookup: List of items starting in each column index.
 * @param constraints Parent constraints (used for fallback behavior and cross-axis limits).
 * @param gridItems All items in the grid (used for spanning logic).
 * @param columnGap The spacing between columns.
 * @return The total used width in pixels (sum of all column widths).
 */
private fun calculateColumnWidths(
    density: Density,
    explicitSpecs: LongList,
    totalCount: Int,
    availableSpace: Int,
    outSizes: IntArray,
    itemsByColumn: Array<MutableObjectList<GridItem>?>,
    constraints: Constraints,
    gridItems: MutableObjectList<GridItem>,
    columnGap: Int,
): Int {
    if (totalCount == 0) return 0

    var totalFlex = 0f
    // Calculate total space consumed by gaps.
    // e.g., 3 columns have 2 gaps. (N-1) * gap.
    val totalGapSpace = (columnGap * (totalCount - 1)).coerceAtLeast(0)

    // Calculate space available for actual tracks (Total - Gaps).
    // If availableSpace is Infinity, availableTrackSpace value becomes Constraints.Infinity
    val availableTrackSpace =
        if (availableSpace == Constraints.Infinity) {
            Constraints.Infinity
        } else {
            (availableSpace - totalGapSpace).coerceAtLeast(0)
        }

    // Height constraint used when measuring intrinsic width.
    // Usually Infinity (standard intrinsic measurement), unless parent enforces strict height.
    val crossAxisAvailable =
        if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity

    // Keep track of which columns are Auto so we can expand them later
    val autoIndices = MutableIntList()

    // Store max intrinsic widths for Auto tracks here to avoid re-measuring later.
    val autoColumnMaxSizes = IntArray(totalCount)

    // --- Pass 1: Base Sizes (Single-Span Items) ---
    // Iterate through every column index (both explicit and implicit).
    for (index in 0 until totalCount) {
        // If index exceeds explicit specs, treat it as an Implicit Auto track.
        val specRaw =
            if (index < explicitSpecs.size) explicitSpecs[index]
            else GridTrackSize.Auto.encodedValue
        val spec = GridTrackSize(specRaw)

        val size =
            when (spec.type) {
                GridTrackSize.TypeFixed -> with(density) { spec.value.dp.roundToPx() }

                GridTrackSize.TypePercentage -> {
                    if (availableTrackSpace != Constraints.Infinity) {
                        (spec.value * availableTrackSpace).roundToInt()
                    } else {
                        // If the Grid is in a horizontally scrolling container
                        // (infinite width), we cannot calculate a percentage of "Infinity".
                        // We default to 'Auto' (MaxIntrinsic) so the content remains visible.
                        calculateMaxIntrinsicWidth(itemsByColumn[index], crossAxisAvailable)
                    }
                }

                GridTrackSize.TypeFlex -> {
                    totalFlex += spec.value
                    // Flex tracks start at their 'min-content' size.
                    // This implements `minmax(min-content, <flex-factor>fr)`.
                    // It ensures that even if there is no remaining space to distribute,
                    // the column is at least wide enough to show its content.
                    calculateMinIntrinsicWidth(itemsByColumn[index], crossAxisAvailable)
                }

                GridTrackSize.TypeMinContent ->
                    calculateMinIntrinsicWidth(itemsByColumn[index], crossAxisAvailable)
                GridTrackSize.TypeMaxContent ->
                    calculateMaxIntrinsicWidth(itemsByColumn[index], crossAxisAvailable)
                GridTrackSize.TypeAuto -> {
                    if (availableTrackSpace == Constraints.Infinity) {
                        // If infinite space, Auto behaves like MaxContent
                        calculateMaxIntrinsicWidth(itemsByColumn[index], crossAxisAvailable)
                    } else {
                        // Finite space: Auto needs Min (for base) AND Max (for growth).
                        val packed =
                            calculateMinMaxIntrinsicWidth(itemsByColumn[index], crossAxisAvailable)
                        // Unpack the Long (High 32 = Max, Low 32 = Min)
                        val max = (packed ushr 32).toInt()
                        val min = (packed and 0xFFFFFFFFL).toInt()

                        autoIndices.add(index)
                        // Cache Max for Pass 1.8
                        autoColumnMaxSizes[index] = max
                        // Return Min for Base Size
                        min
                    }
                }
                // Measure the max intrinsic width of all items in this column.
                else -> calculateMaxIntrinsicWidth(itemsByColumn[index], crossAxisAvailable)
            }
        outSizes[index] = size
    }

    // --- Pass 1.5: Spanning Items ---
    // If an item spans 2 columns, and those 2 columns (base sizes) sum to 100px, but the item
    // is 150px wide, we must grow the columns by 50px.
    distributeSpanningSpace(
        explicitSpecs = explicitSpecs,
        sizes = outSizes,
        gridItems = gridItems,
        isRowAxis = false,
        constraints = constraints,
        crossAxisSizes = null, // Not needed for column width calculation
        gap = columnGap,
    )

    // --- Pass 1.8: Expand Auto Tracks ---
    // Only strictly needed if we are constrained.
    // Auto tracks consume space AFTER fixed/min-content, but BEFORE Flex tracks.
    if (availableTrackSpace != Constraints.Infinity && autoIndices.isNotEmpty()) {
        expandAutoTracks(
            autoTrackIndices = autoIndices,
            outSizes = outSizes,
            maxSizes = autoColumnMaxSizes,
            availableSpace = availableTrackSpace,
        )
    }

    var usedSpace = 0
    for (size in outSizes) {
        usedSpace += size
    }

    // --- Pass 2: Flex Distribution ---
    // If we have finite width and unused space, distribute it to Flex columns.
    val remainingSpace =
        if (availableTrackSpace == Constraints.Infinity) 0
        else max(0, availableTrackSpace - usedSpace)

    var totalAddedFromFlex = 0
    if (totalFlex > 0 && remainingSpace > 0) {
        var distributed = 0
        var accumulatedFlex = 0f

        for (index in 0 until totalCount) {
            val specRaw =
                if (index < explicitSpecs.size) explicitSpecs[index]
                else GridTrackSize.Auto.encodedValue
            val spec = GridTrackSize(specRaw)

            if (spec.type == GridTrackSize.TypeFlex) {
                accumulatedFlex += spec.value
                // Distribute space proportionally based on weight.
                // Uses an accumulation algorithm to avoid rounding errors summing to >
                // remainingSpace.
                val targetSpace = (accumulatedFlex / totalFlex * remainingSpace).roundToInt()
                val share = max(0, targetSpace - distributed)

                outSizes[index] += share
                distributed += share
                totalAddedFromFlex = distributed
            }
        }
    }

    return usedSpace + totalAddedFromFlex
}

/**
 * Calculates the specific pixel height of every row in the grid.
 *
 * This function implements the vertical axis sizing logic. Unlike columns (which are usually fixed
 * or determined by parent width), row heights often depend on the *width* of the content within
 * them. Therefore, this function **must** be called after [calculateColumnWidths].
 *
 * **Algorithm Overview:**
 * 1. **Pass 1 (Base Sizes):** Calculates the initial height of each row based on its
 *    [GridTrackSize].
 * * **Implicit Tracks:** Indices beyond `explicitSpecs` default to [GridTrackSize.Auto].
 * * **Auto/Content-based:** Measured using the pre-calculated `columnWidths`. This ensures text
 *   wraps correctly within its specific cell width.
 * * **Percentage:** Resolves against total height. Falls back to `Auto` if height is infinite
 *   (e.g., inside a ScrollView).
 * * **Flex:** Starts at `min-content` size to prevent collapse if content exists.
 * 2. **Pass 1.5 (Spanning Items):** Increases row heights if an item spanning multiple rows is
 *    taller than the sum of those rows.
 * 3. **Pass 1.8 (Expand Auto Tracks):** Distributes remaining available space to `Auto` tracks,
 *    allowing them to grow from their `min-content` floor toward their `max-content` cap. This
 *    ensures `Auto` columns are responsive but don't aggressively consume space needed for `Flex`
 *    columns.
 * 4. **Pass 2 (Flex Distribution):** Distributes any remaining vertical space among
 *    [GridTrackSize.Flex] rows according to their weight.
 *
 * @param density Used for Dp-to-Px conversion.
 * @param explicitSpecs The user-defined row configurations.
 * @param totalCount The total number of rows (explicit + implicit).
 * @param availableSpace The maximum height available (or [Constraints.Infinity]).
 * @param outSizes Output array where calculated heights are stored. **Mutated in-place**.
 * @param itemsByRow Optimization lookup: List of items starting in each row index.
 * @param constraints Parent constraints (used for max height limits).
 * @param columnWidths The resolved widths of columns. **Critical** for measuring text height.
 * @param gridItems All items in the grid (used for spanning logic).
 * @param rowGap The spacing between rows.
 * @return The total used height in pixels (sum of all row heights).
 */
private fun calculateRowHeights(
    density: Density,
    explicitSpecs: LongList,
    totalCount: Int,
    availableSpace: Int,
    outSizes: IntArray,
    itemsByRow: Array<MutableObjectList<GridItem>?>,
    constraints: Constraints,
    columnWidths: IntArray,
    gridItems: MutableObjectList<GridItem>,
    rowGap: Int,
): Int {
    if (totalCount == 0) return 0

    var totalFlex = 0f
    // Calculate total space consumed by gaps.
    // e.g., 3 columns have 2 gaps. (N-1) * gap.
    val totalGapSpace = (rowGap * (totalCount - 1)).coerceAtLeast(0)

    // Calculate space available for actual tracks (Total - Gaps).
    // If availableSpace is Infinity, availableTrackSpace value becomes Constraints.Infinity
    val availableTrackSpace =
        if (availableSpace == Constraints.Infinity) {
            Constraints.Infinity
        } else {
            (availableSpace - totalGapSpace).coerceAtLeast(0)
        }

    // Keep track of which columns are Auto so we can expand them later
    val autoIndices = MutableIntList()

    val autoRowMaxSizes = IntArray(totalCount)

    // --- Pass 1: Base Sizes (Single-Span Items) ---
    // We iterate through every row index (both explicit and implicit).
    for (index in 0 until totalCount) {
        // If index exceeds explicit specs, treat it as an Implicit Auto track.
        val specRaw =
            if (index < explicitSpecs.size) explicitSpecs[index]
            else GridTrackSize.Auto.encodedValue
        val spec = GridTrackSize(specRaw)

        val size =
            when (spec.type) {
                GridTrackSize.TypeFixed -> with(density) { spec.value.dp.roundToPx() }

                GridTrackSize.TypePercentage -> {
                    if (availableTrackSpace != Constraints.Infinity) {
                        (spec.value * availableTrackSpace).roundToInt()
                    } else {
                        // If the Grid is in a vertically scrolling container
                        // (infinite height), we cannot calculate a percentage of "Infinity".
                        // We default to 'Auto' (MaxIntrinsic) so the content remains visible.
                        calculateMaxIntrinsicHeight(
                            items = itemsByRow[index],
                            columnWidths = columnWidths,
                            fallbackWidth = constraints.maxWidth,
                        )
                    }
                }

                GridTrackSize.TypeFlex -> {
                    totalFlex += spec.value
                    // Flex tracks start at their 'min-content' size.
                    // This implements `minmax(min-content, <flex-factor>fr)`.
                    // It ensures that even if there is no remaining space to distribute,
                    // the row is at least tall enough to show its content.
                    calculateMinIntrinsicHeight(
                        items = itemsByRow[index],
                        columnWidths = columnWidths,
                        fallbackWidth = constraints.maxWidth,
                    )
                }

                GridTrackSize.TypeMinContent ->
                    calculateMinIntrinsicHeight(
                        items = itemsByRow[index],
                        columnWidths = columnWidths,
                        fallbackWidth = constraints.maxWidth,
                    )
                GridTrackSize.TypeMaxContent ->
                    calculateMaxIntrinsicHeight(
                        items = itemsByRow[index],
                        columnWidths = columnWidths,
                        fallbackWidth = constraints.maxWidth,
                    )
                GridTrackSize.TypeAuto -> {
                    // If infinite space, Auto behaves like MaxContent
                    if (availableTrackSpace == Constraints.Infinity) {
                        calculateMaxIntrinsicHeight(
                            items = itemsByRow[index],
                            columnWidths = columnWidths,
                            fallbackWidth = constraints.maxWidth,
                        )
                    } else {
                        // Finite space: Auto needs Min (for base) and Max (for growth).
                        val packed =
                            calculateMinMaxIntrinsicHeight(
                                itemsByRow[index],
                                columnWidths,
                                constraints.maxWidth,
                            )
                        // Unpack the Long (High 32 = Max, Low 32 = Min)
                        val max = (packed ushr 32).toInt()
                        val min = (packed and 0xFFFFFFFFL).toInt()

                        autoIndices.add(index)
                        // Cache Max for Pass 1.8
                        autoRowMaxSizes[index] = max
                        // Return Min for Base Size
                        min
                    }
                }
                else ->
                    calculateMaxIntrinsicHeight(
                        items = itemsByRow[index],
                        columnWidths = columnWidths,
                        fallbackWidth = constraints.maxWidth,
                    )
            }
        outSizes[index] = size
    }

    // --- Pass 1.5: Spanning Items ---
    // If an item spans 2 rows, and those 2 rows (base sizes) sum to 100px, but the item
    // is 150px tall, we must grow the rows by 50px.
    distributeSpanningSpace(
        explicitSpecs = explicitSpecs,
        sizes = outSizes,
        gridItems = gridItems,
        isRowAxis = true,
        constraints = constraints,
        crossAxisSizes = columnWidths,
        gap = rowGap,
    )

    // --- Pass 1.8: Expand Auto Tracks ---
    // Only strictly needed if we are constrained.
    // Auto tracks consume space AFTER fixed/min-content, but BEFORE Flex tracks.
    if (availableTrackSpace != Constraints.Infinity && autoIndices.isNotEmpty()) {
        expandAutoTracks(
            autoTrackIndices = autoIndices,
            outSizes = outSizes,
            maxSizes = autoRowMaxSizes,
            availableSpace = availableTrackSpace,
        )
    }

    var usedSpace = 0
    for (size in outSizes) {
        usedSpace += size
    }

    // --- Pass 2: Flex Distribution ---
    //
    // If we have finite height and unused space, distribute it to Flex rows.
    val remainingSpace =
        if (availableTrackSpace == Constraints.Infinity) 0
        else max(0, availableTrackSpace - usedSpace)

    var totalAddedFromFlex = 0
    if (totalFlex > 0 && remainingSpace > 0) {
        var distributed = 0
        var accumulatedFlex = 0f

        for (index in 0 until totalCount) {
            val specRaw =
                if (index < explicitSpecs.size) explicitSpecs[index]
                else GridTrackSize.Auto.encodedValue
            val spec = GridTrackSize(specRaw)

            if (spec.type == GridTrackSize.TypeFlex) {
                accumulatedFlex += spec.value
                // Distribute space proportionally based on weight.
                // Uses an accumulation algorithm to avoid rounding errors summing to >
                // remainingSpace.
                val targetSpace = (accumulatedFlex / totalFlex * remainingSpace).roundToInt()
                val share = max(0, targetSpace - distributed)

                outSizes[index] += share
                distributed += share
                totalAddedFromFlex = distributed
            }
        }
    }

    return usedSpace + totalAddedFromFlex
}

private fun calculateMaxIntrinsicWidth(
    items: MutableObjectList<GridItem>?,
    heightConstraint: Int,
): Int {
    if (items == null) return 0
    var maxSize = 0
    items.forEach { item ->
        if (item.columnSpan == 1) {
            val size = item.measurable.maxIntrinsicWidth(heightConstraint)
            if (size > maxSize) maxSize = size
        }
    }
    return maxSize
}

private fun calculateMinIntrinsicWidth(
    items: MutableObjectList<GridItem>?,
    heightConstraint: Int,
): Int {
    if (items == null) return 0
    var maxSize = 0
    items.forEach { item ->
        if (item.columnSpan == 1) {
            val size = item.measurable.minIntrinsicWidth(heightConstraint)
            if (size > maxSize) maxSize = size
        }
    }
    return maxSize
}

private fun calculateMaxIntrinsicHeight(
    items: MutableObjectList<GridItem>?,
    columnWidths: IntArray,
    fallbackWidth: Int,
): Int {
    if (items == null) return 0
    var maxSize = 0
    items.forEach { item ->
        if (item.rowSpan == 1) {
            val colIndex = item.column
            val width = if (colIndex < columnWidths.size) columnWidths[colIndex] else fallbackWidth
            val size = item.measurable.maxIntrinsicHeight(width)
            if (size > maxSize) maxSize = size
        }
    }
    return maxSize
}

private fun calculateMinIntrinsicHeight(
    items: MutableObjectList<GridItem>?,
    columnWidths: IntArray,
    fallbackWidth: Int,
): Int {
    if (items == null) return 0
    var maxSize = 0
    items.forEach { item ->
        if (item.rowSpan == 1) {
            val colIndex = item.column
            val width = if (colIndex < columnWidths.size) columnWidths[colIndex] else fallbackWidth
            val size = item.measurable.minIntrinsicHeight(width)
            if (size > maxSize) maxSize = size
        }
    }
    return maxSize
}

/**
 * Calculates both the minimum and maximum intrinsic widths of the provided [items] in a single
 * pass.
 *
 * @param items The list of items in this column.
 * @param heightConstraint The available height to measure against (often [Constraints.Infinity]).
 * @return A packed [Long] containing both values to avoid object allocation:
 * * **High 32 bits:** The maximum intrinsic width. Extract via `(packed ushr 32).toInt()`.
 * * **Low 32 bits:** The minimum intrinsic width. Extract via `(packed and 0xFFFFFFFFL).toInt()`.
 */
private fun calculateMinMaxIntrinsicWidth(
    items: MutableObjectList<GridItem>?,
    heightConstraint: Int,
): Long {
    if (items == null) return 0L
    var maxMin = 0
    var maxMax = 0
    items.forEach { item ->
        if (item.columnSpan == 1) {
            val min = item.measurable.minIntrinsicWidth(heightConstraint)
            val max = item.measurable.maxIntrinsicWidth(heightConstraint)
            if (min > maxMin) maxMin = min
            if (max > maxMax) maxMax = max
        }
    }
    return (maxMax.toLong() shl 32) or (maxMin.toLong() and 0xFFFFFFFFL)
}

/**
 * Calculates both the minimum and maximum intrinsic heights of the provided [items] in a single
 * pass.
 *
 * @param items The list of items in this row.
 * @param columnWidths The calculated pixel widths of all columns. Used to measure height correctly.
 * @param fallbackWidth The width to use if an item resides in an implicit column (index out of
 *   bounds).
 * @return A packed [Long] containing both values:
 * * **High 32 bits:** The maximum intrinsic height. Extract via `(packed ushr 32).toInt()`.
 * * **Low 32 bits:** The minimum intrinsic height. Extract via `(packed and 0xFFFFFFFFL).toInt()`.
 */
private fun calculateMinMaxIntrinsicHeight(
    items: MutableObjectList<GridItem>?,
    columnWidths: IntArray,
    fallbackWidth: Int,
): Long {
    if (items == null) return 0L
    var maxMin = 0
    var maxMax = 0
    items.forEach { item ->
        if (item.rowSpan == 1) {
            val colIndex = item.column
            val width = if (colIndex < columnWidths.size) columnWidths[colIndex] else fallbackWidth
            val min = item.measurable.minIntrinsicHeight(width)
            val max = item.measurable.maxIntrinsicHeight(width)
            if (min > maxMin) maxMin = min
            if (max > maxMax) maxMax = max
        }
    }
    return (maxMax.toLong() shl 32) or (maxMin.toLong() and 0xFFFFFFFFL)
}

/**
 * Increases the size of "growable" tracks (Auto, Flex, MinContent, MaxContent) to accommodate items
 * that span across multiple tracks.
 *
 * This represents **Pass 1.5** of the grid sizing algorithm. It runs after base track sizes
 * (Pass 1) are calculated but before flexible space (Pass 2) is distributed.
 *
 * **The Problem:** An item spanning 2 columns might have a minimum intrinsic width of 200px. If the
 * base size of those 2 columns (plus the gap) only equals 150px, the item will be clipped or
 * overlap.
 *
 * **The Solution (Deficit Distribution):**
 * 1. Calculate the **Deficit**: `RequiredSize - (SumOfTracks + SumOfGaps)`.
 * 2. Distribute this deficit evenly among the tracks involved in the span, *excluding* rigid tracks
 *    ([GridTrackSize.Fixed] and [GridTrackSize.Percentage]).
 *
 * @param explicitSpecs The user-defined track specifications. Used to determine if a track is rigid
 *   (Fixed/Percentage) or growable (Intrinsic).
 * @param sizes The current calculated pixel sizes of the tracks.
 * @param gridItems The list of all items to check for spanning requirements.
 * @param isRowAxis `true` if calculating Row Heights, `false` if calculating Column Widths.
 * @param constraints The parent layout constraints.
 * @param crossAxisSizes The calculated sizes of the *opposite* axis (e.g., Column Widths when
 *   calculating Row Heights). This is crucial for correctly measuring the intrinsic height of items
 *   that wrap text based on specific column widths.
 * @param gap The spacing between tracks.
 */
private fun distributeSpanningSpace(
    explicitSpecs: LongList,
    sizes: IntArray,
    gridItems: MutableObjectList<GridItem>,
    isRowAxis: Boolean,
    constraints: Constraints,
    crossAxisSizes: IntArray?,
    gap: Int,
) {
    gridItems.forEach { item ->
        val trackIndex = if (isRowAxis) item.row else item.column
        val span = if (isRowAxis) item.rowSpan else item.columnSpan

        // Single-span items were already handled during Base Size calculation (Pass 1).
        if (span <= 1) return@forEach

        val endIndex = (trackIndex + span).coerceAtMost(sizes.size)

        // --- Step 1: Analyze current space & identifying growable tracks ---
        // We sum the current size of all tracks this item spans to see if they are already big
        // enough.
        var currentSpannedSize = 0
        var tracksToGrowCount = 0

        for (i in trackIndex until endIndex) {
            currentSpannedSize += sizes[i]

            // Implicit tracks (indices >= specs.size) default to Auto.
            val specRaw =
                if (i < explicitSpecs.size) explicitSpecs[i] else GridTrackSize.Auto.encodedValue
            val spec = GridTrackSize(specRaw)

            // Fixed and Percentage tracks are considered "Rigid". They respect the user's explicit
            // definition and do not expand to fit content from spanning items.
            // Only Intrinsic tracks (Auto, Flex, Min/MaxContent) absorb the deficit.
            if (spec.type != GridTrackSize.TypeFixed && spec.type != GridTrackSize.TypePercentage) {
                tracksToGrowCount++
            }
        }

        // --- Step 2: Calculate the Item's Required Size (Intrinsic Measurement) ---
        // This differs based on the axis.
        val requiredSize =
            if (isRowAxis) {
                // Case: Calculating Row Heights.
                // To get the correct intrinsic height (e.g., for wrapping text), we need to know
                // the exact width the item occupies. This is the sum of the columns it spans.
                var itemWidth = 0
                if (crossAxisSizes != null) {
                    val colStart = item.column
                    val colEnd = (colStart + item.columnSpan).coerceAtMost(crossAxisSizes.size)
                    for (i in colStart until colEnd) {
                        itemWidth += crossAxisSizes[i]
                    }
                    // Add the gaps that are included in the span.
                    val spannedGaps = max(0, item.columnSpan - 1) * gap
                    itemWidth += spannedGaps
                } else {
                    // If we don't know column widths, constrain only by parent max.
                    itemWidth = constraints.maxWidth
                }
                item.measurable.maxIntrinsicHeight(itemWidth)
            } else {
                // Case: Calculating Column Widths.
                // Intrinsic width is typically calculated against infinite height (maxContent),
                // or the parent's bounded height if specified.
                val heightConstraint =
                    if (constraints.hasBoundedHeight) constraints.maxHeight
                    else Constraints.Infinity
                item.measurable.maxIntrinsicWidth(heightConstraint)
            }

        // --- Step 3: Distribute Deficit ---
        val deficit = requiredSize - currentSpannedSize

        // If the item needs more space than currently available, and we have eligible tracks to
        // grow, we distribute the missing pixels evenly.
        if (deficit > 0 && tracksToGrowCount > 0) {
            val share = deficit / tracksToGrowCount
            var remainder = deficit % tracksToGrowCount

            for (i in trackIndex until endIndex) {
                val specRaw =
                    if (i < explicitSpecs.size) explicitSpecs[i]
                    else GridTrackSize.Auto.encodedValue
                val spec = GridTrackSize(specRaw)

                // Only add space to the "growable" tracks identified in Step 1.
                if (
                    spec.type != GridTrackSize.TypeFixed &&
                        spec.type != GridTrackSize.TypePercentage
                ) {
                    // Add the base share + 1 pixel if we still have remainder to distribute.
                    // This ensures (share * count) + remainder == total deficit.
                    val add = share + if (remainder > 0) 1 else 0
                    sizes[i] += add
                    if (remainder > 0) remainder--
                }
            }
        }
    }
}

/**
 * Expands [GridTrackSize.Auto] tracks from their minimum intrinsic size toward their maximum
 * intrinsic size using the remaining available space.
 *
 * This behavior allows tracks to occupy at least their minimum content size, growing to fit their
 * maximum content size if the container allows it.
 *
 * This runs **Pass 1.8**, after `MinContent` base sizes are calculated but before `Flex` tracks
 * receive space. This ensures `Auto` tracks typically size to fit their content comfortably before
 * `Flex` tracks consume the rest of the container.
 *
 * **Distribution Logic:**
 * 1. **Growth Potential:** For each Auto track, we calculate `Potential = MaxIntrinsicSize -
 *    MinIntrinsicSize`.
 * 2. **Proportional Allocation:**
 * - If `remainingSpace` covers the total potential, all Auto tracks become their
 *   `MaxIntrinsicSize`.
 * - If space is scarce, it is distributed **proportionally** based on potential. A track with a
 *   large difference between its Min and Max (e.g., a long paragraph that can wrap) receives more
 *   space than a track with little potential (e.g., an icon).
 *
 * @param autoTrackIndices The list of indices corresponding to [GridTrackSize.Auto] tracks.
 * @param outSizes The array of current track sizes in pixels. **Mutated in-place.**
 * - **Input:** Contains the `min-content` size (Pass 1 result).
 * - **Output:** Contains the expanded size (up to `max-content`).
 *
 * @param maxSizes The array of pre-calculated maximum intrinsic sizes for these tracks.
 * @param availableSpace The total constrained size of the container (width or height).
 */
private fun expandAutoTracks(
    autoTrackIndices: MutableIntList,
    outSizes: IntArray,
    maxSizes: IntArray,
    availableSpace: Int,
) {
    if (autoTrackIndices.isEmpty()) return

    // 1. Calculate how much space is currently used by all tracks (Fixed + MinContent + etc)
    var usedSpace = 0
    for (size in outSizes) {
        usedSpace += size
    }

    val remainingSpace = availableSpace - usedSpace
    if (remainingSpace <= 0) return

    // 2. Calculate the "Growth Potential" for each auto track (Max - Min)
    // We also sum the total potential to determine distribution shares.
    val growthPotentials = IntArray(autoTrackIndices.size)
    var totalGrowthPotential = 0

    autoTrackIndices.forEachIndexed { i, trackIndex ->
        val currentSize = outSizes[trackIndex]
        val maxIntrinsicSize = maxSizes[trackIndex]
        val potential = max(0, maxIntrinsicSize - currentSize)
        growthPotentials[i] = potential
        totalGrowthPotential += potential
    }

    // 3. If there is no potential to grow (all tracks are already at max content), exit.
    if (totalGrowthPotential == 0) return

    // 4. Distribute space
    // If we have enough space to satisfy everyone's max potential, just set them all to max.
    if (remainingSpace >= totalGrowthPotential) {
        for (i in autoTrackIndices.indices) {
            val trackIndex = autoTrackIndices[i]
            outSizes[trackIndex] += growthPotentials[i]
        }
    } else {
        // Otherwise, distribute proportionally based on how much each track WANTS to grow.
        // This ensures a fair distribution where tracks with huge content get more space
        // than tracks that only need a few more pixels.
        for (i in autoTrackIndices.indices) {
            val trackIndex = autoTrackIndices[i]
            val share =
                (growthPotentials[i].toFloat() / totalGrowthPotential * remainingSpace).roundToInt()
            outSizes[trackIndex] += share
        }
    }
}

/**
 * Measures the content of every grid item based on its resolved position and span.
 *
 * This function converts abstract grid coordinates (row/column indices) into concrete pixel
 * constraints. It determines the exact width and height of the cell(s) an item spans and measures
 * the child content against those bounds.
 *
 * This method calculates the span size in O(1) time using the pre-computed offset arrays: `Size =
 * (End_Offset + End_Size) - Start_Offset`
 *
 * This function mutates the provided [gridItems] list, updating each item with its measured
 * [Placeable] and calculated (x, y) offsets.
 *
 * @param gridItems The list of all grid items.
 * @param trackSizes The calculated pixel sizes for every row and column track.
 * @param layoutDirection The current layout direction.
 */
private fun measureItems(
    gridItems: MutableObjectList<GridItem>,
    trackSizes: GridTrackSizes,
    layoutDirection: LayoutDirection,
) {
    val rowCount = trackSizes.rowHeights.size
    val colCount = trackSizes.columnWidths.size

    gridItems.forEach { item ->
        val row = item.row
        val col = item.column

        if (row < rowCount && col < colCount) {
            var width = 0
            val colLimit = (col + item.columnSpan).coerceAtMost(colCount)
            for (i in col until colLimit) {
                width += trackSizes.columnWidths[i]
            }
            // Add gaps for spanned columns
            val colSpanActual = colLimit - col
            if (colSpanActual > 1) {
                width += (colSpanActual - 1) * trackSizes.columnGapPx
            }

            var height = 0
            val rowLimit = (row + item.rowSpan).coerceAtMost(rowCount)
            for (i in row until rowLimit) {
                height += trackSizes.rowHeights[i]
            }
            // Add gaps for spanned rows
            val rowSpanActual = rowLimit - row
            if (rowSpanActual > 1) {
                height += (rowSpanActual - 1) * trackSizes.rowGapPx
            }

            // Use loose constraints to allow alignment to work.
            // If strict fixed constraints are used, child size == cell size, so alignment is
            // ignored.
            val constraints = Constraints(maxWidth = width, maxHeight = height)
            val placeable = item.measurable.measure(constraints)

            // Calculate Alignment Offset
            val containerSize = IntSize(width, height)
            val contentSize = IntSize(placeable.width, placeable.height)
            val alignmentOffset =
                item.alignment.align(
                    size = contentSize,
                    space = containerSize,
                    layoutDirection = layoutDirection,
                )

            item.placeable = placeable
            // Alignment.align already accounts for RTL (Start = right side) relative to 0,0.
            item.offsetX = alignmentOffset.x
            item.offsetY = alignmentOffset.y
        }
    }
}

/**
 * Computes the cumulative starting position (offset) for each track.
 *
 * This function converts a list of track sizes (e.g., column widths or row heights) into absolute
 * coordinates by accumulating the size of previous tracks and the specified [gapPx] between them.
 *
 * Example logic:
 * - Offset[0] = 0
 * - Offset[1] = Size[0] + Gap
 * - Offset[2] = Size[0] + Gap + Size[1] + Gap
 *
 * @param sizes An array containing the size of each individual track.
 * @param gapPx The spacing in pixels to insert between consecutive tracks.
 * @return An [IntArray] of the same length as [sizes], where index `i` contains the starting
 *   coordinate of that track.
 */
private fun calculateTrackOffsets(sizes: IntArray, gapPx: Int): IntArray {
    val offsets = IntArray(sizes.size)
    var current = 0
    for (i in sizes.indices) {
        offsets[i] = current
        current += sizes[i] + gapPx
    }
    return offsets
}
