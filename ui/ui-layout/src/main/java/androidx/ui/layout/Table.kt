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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.Measurable
import androidx.ui.core.ParentData
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
* Layout model that arranges its children into rows and columns.
*/
@Composable
fun Table(
    childAlignment: Alignment = Alignment.TopLeft,
    @Children(composable = false) block: TableChildren.() -> Unit
) {
    val children: @Composable() () -> Unit = with(TableChildren()) {
        apply(block)
        val composable = @Composable {
            tableChildren.forEach { it() }
        }
        composable
    }

    Layout(children) { measurables, constraints ->
        // Group the measurables into rows using rowGroup.
        val measurablesByRow = measurables.groupBy { it.rowGroup }.values

        val rows = measurablesByRow.size
        val columns = measurablesByRow.map { it.size }.max() ?: 0

        require(rows > 0 && columns > 0)

        // Get the preferred size of each measurable (with loose constraints).
        val placeables = measurablesByRow.map { row -> row.map { it.measure(Constraints()) } }

        // Calculate the height of each row and width of each column.
        // TODO(calintat): Allow for different sizing strategies for rows/columns.
        val rowHeights = placeables.map { row -> row.map { it.height }.max() }
        val columnWidths = if (constraints.maxWidth.isFinite()) {
            List(size = columns) { constraints.maxWidth / columns }
        } else {
            List(size = columns) { j -> placeables.map { row -> row[j].height }.max() }
        }

        // TODO(calintat): Figure out what to do when rowHeights.sum() exceeds max height constraint
        val tableWidth = max(columnWidths.sum(), constraints.minWidth)
        val tableHeight = rowHeights.sum().coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(tableWidth, tableHeight) {
            placeables.forEachIndexed { i, row ->
                row.forEachIndexed { j, placeable ->
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

private fun Collection<IntPx>.sum() = this.fold(IntPx.Zero) { a, b -> a + b }
private fun Collection<IntPx>.max() = this.fold(IntPx.Zero) { a, b -> max(a, b) }
