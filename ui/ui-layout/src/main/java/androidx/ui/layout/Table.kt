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

package androidx.ui.layout

import androidx.annotation.FloatRange
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.Measurable
import androidx.ui.core.ParentData
import androidx.ui.core.Placeable
import androidx.ui.core.coerceAtLeast
import androidx.ui.core.coerceIn
import androidx.ui.core.isFinite
import androidx.ui.core.max

/**
 * Collects information about the children of a [Table] when
 * its body is executed with a [TableChildren] as argument.
 */
class TableChildren internal constructor() {

    internal val tableChildren = mutableListOf<@Composable() () -> Unit>()
    private var rowGroup = 0

    fun tableRow(children: @Composable() () -> Unit) {
        tableChildren += {
            ParentData(data = TableChildData(rowGroup++), children = children)
        }
    }
}

/**
 * Parent data associated with children to assign a row group.
 */
private data class TableChildData(val rowGroup: Int)

private val Measurable.rowGroup get() = (parentData as TableChildData).rowGroup

/**
 * Used to specify the size of a [Table]'s column.
 */
sealed class TableColumnWidth {
    /**
     * Sizes the column to be the width of the widest child in that column.
     */
    object Wrap : TableColumnWidth()

    /**
     * Sizes the column by taking a part of the remaining space
     * once all the other columns have been measured according to [flex].
     */
    data class Flex(internal val flex: Float) : TableColumnWidth()

    /**
     * Sizes the column to a specific width.
     */
    data class Fixed(internal val width: Dp) : TableColumnWidth()

    /**
     * Sizes the column to a fraction of the table’s maximum width constraint.
     */
    data class Fraction(
        @FloatRange(from = 0.0, to = 1.0) internal val fraction: Float
    ) : TableColumnWidth()
}

/**
* Layout model that arranges its children into rows and columns.
*/
@Composable
fun Table(
    childAlignment: Alignment = Alignment.TopLeft,
    columnWidth: (columnIndex: Int) -> TableColumnWidth = { TableColumnWidth.Flex(1f) },
    @Children(composable = false) block: TableChildren.() -> Unit
) {
    val children: @Composable() () -> Unit = with(TableChildren()) {
        apply(block)
        val composable = @Composable {
            tableChildren.forEach { it() }
        }
        composable
    }

    Layout(children) { m, constraints ->
        // Group the measurables into rows using rowGroup.
        val measurables = m.groupBy { it.rowGroup }.values.toTypedArray()

        val rows = measurables.size
        val columns = measurables.map { it.size }.max() ?: 0

        var totalFlex = 0f
        var availableSpace = if (constraints.maxWidth.isFinite()) {
            constraints.maxWidth
        } else {
            constraints.minWidth
        }

        val rowHeights = Array(rows) { IntPx.Zero }
        val columnWidths = Array(columns) { IntPx.Zero }

        val placeables = Array(rows) { arrayOfNulls<Placeable>(columns) }

        // Compute widths of non-flex columns.
        for (j in 0 until columns) {
            when (val spec = columnWidth(j)) {
                is TableColumnWidth.Flex -> {
                    totalFlex += spec.flex
                }
                is TableColumnWidth.Fixed -> {
                    columnWidths[j] = spec.width.toIntPx()
                }
                is TableColumnWidth.Fraction -> {
                    columnWidths[j] = if (constraints.maxWidth.isFinite()) {
                        constraints.maxWidth * spec.fraction
                    } else {
                        IntPx.Zero
                    }
                }
                is TableColumnWidth.Wrap -> {
                    // Measure children in intrinsic columns.
                    for (i in 0 until rows) {
                        val placeable = measurables[i][j].measure(Constraints())
                        placeables[i][j] = placeable
                        rowHeights[i] = max(rowHeights[i], placeable.height)
                        columnWidths[j] = max(columnWidths[j], placeable.width)
                    }
                }
            }
            availableSpace -= columnWidths[j]
        }

        availableSpace = availableSpace.coerceAtLeast(IntPx.Zero)

        // Compute widths of flex columns.
        for (j in 0 until columns) {
            val spec = columnWidth(j)
            if (spec is TableColumnWidth.Flex) {
                columnWidths[j] = availableSpace * (spec.flex / totalFlex)
            }
        }

        // Measure the remaining children.
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                if (placeables[i][j] == null) {
                    val placeable = measurables[i][j].measure(
                        Constraints(maxWidth = columnWidths[j])
                    )
                    placeables[i][j] = placeable
                    rowHeights[i] = max(rowHeights[i], placeable.height)
                }
            }
        }

        // TODO(calintat): Figure out what to do when these exceed max constraints.
        val tableWidth = columnWidths.sum().coerceIn(constraints.minWidth, constraints.maxWidth)
        val tableHeight = rowHeights.sum().coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(tableWidth, tableHeight) {
            for (i in 0 until rows) {
                for (j in 0 until columns) {
                    val placeable = placeables[i][j]!!
                    val position = childAlignment.align(
                        IntPxSize(
                            width = columnWidths[j] - placeable.width,
                            height = rowHeights[i] - placeable.height
                        )
                    )
                    placeable.place(
                        x = columnWidths.take(j).sum() + position.x,
                        y = rowHeights.take(i).sum() + position.y
                    )
                }
            }
        }
    }
}

private fun Array<IntPx>.sum() = this.fold(IntPx.Zero) { a, b -> a + b }
private fun Collection<IntPx>.sum() = this.fold(IntPx.Zero) { a, b -> a + b }
