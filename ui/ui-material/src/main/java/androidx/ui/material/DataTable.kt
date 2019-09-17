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

package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.ParentData
import androidx.ui.core.dp
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.drawBorders
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.shape.border.Border
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Table
import androidx.ui.layout.TableColumnWidth
import androidx.ui.material.ripple.Ripple
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontWeight

/**
 * Configuration for a row of a [DataTable].
 */
data class DataRow(
    /**
     * The content for this row indexed by column.
     */
    val children: @Composable() (index: Int) -> Unit,

    /**
     * Whether this row is selected.
     *
     * If [onSelectedChange] is non-null for any row in the table, then a checkbox is shown at the
     * start of each row. The checkbox will be checked if and only if the row is selected (true).
     */
    val selected: Boolean = false,

    /**
     * Called when the user selects or unselects a selectable row.
     *
     * If this is not null, then the row is selectable. The current selection state of the row
     * is given by [selected]. A row whose [onSelectedChange] callback is null is ignored for
     * the purpose of determining the state of the 'all' checkbox, and its checkbox is disabled.
     */
    val onSelectedChange: ((Boolean) -> Unit)? = null
)

/**
 * Data tables display information in a grid-like format of rows and columns. They organize
 * information in a way that’s easy to scan, so that users can look for patterns and insights.
 *
 * Example usage:
 *
 * @sample androidx.ui.material.samples.SimpleDataTable
 *
 * @param rows The data to show in each row (excluding the header row).
 * @param columns The number of columns in the table.
 * @param header The header of the given column.
 * @param numeric Whether the given column represents numeric data.
 * @param rowHeight The height of each row (excluding the header row).
 * @param headerHeight The height of the header row.
 * @param cellSpacing The padding to apply around each cell.
 * @param border The style used for the table borders.
 * @param selectedColor The color used to indicate selected rows.
 * @param onSelectAll Called when the user selects or unselects every row using the 'all' checkbox.
 */
@Composable
fun DataTable(
    rows: List<DataRow>,
    columns: Int,
    header: @Composable() (index: Int) -> Unit,
    numeric: (Int) -> Boolean = { false },
    rowHeight: Dp = RowHeight,
    headerHeight: Dp = HeaderHeight,
    cellSpacing: EdgeInsets = CellSpacing,
    border: Border = Border(color = BorderColor, width = BorderWidth),
    selectedColor: Color = +themeColor { primary.copy(alpha = 0.08f) },
    onSelectAll: (Boolean) -> Unit = { rows.forEach { row -> row.onSelectedChange?.invoke(it) } }
) {
    val selectableRows = rows.filter { it.onSelectedChange != null }
    val showCheckboxes = selectableRows.isNotEmpty()

    Table(
        columns = columns + if (showCheckboxes) 1 else 0,
        alignment = { j ->
            if (numeric(j - if (showCheckboxes) 1 else 0)) {
                Alignment.CenterRight
            } else {
                Alignment.CenterLeft
            }
        },
        columnWidth = { j ->
            if (showCheckboxes && j == 0) {
                TableColumnWidth.Wrap
            } else {
                TableColumnWidth.Wrap.flexible(flex = 1f)
            }
        }
    ) {
        // Table borders
        drawBorders(defaultBorder = border) {
            allHorizontal()
        }

        // Header row
        tableRow {
            if (showCheckboxes) {
                Container(height = headerHeight, padding = cellSpacing) {
                    val parentState = when (selectableRows.count { it.selected }) {
                        selectableRows.size -> ToggleableState.Checked
                        0 -> ToggleableState.Unchecked
                        else -> ToggleableState.Indeterminate
                    }
                    TriStateCheckbox(value = parentState, onClick = {
                        onSelectAll(parentState != ToggleableState.Checked)
                    })
                }
            }
            for (j in 0 until columns) {
                Container(height = headerHeight, padding = cellSpacing) {
                    CurrentTextStyleProvider(value = TextStyle(fontWeight = FontWeight.w500)) {
                        header(index = j)
                    }
                }
            }
        }

        // Data rows
        rows.forEach { row ->
            tableRow {
                if (showCheckboxes) {
                    Container(height = rowHeight, padding = cellSpacing) {
                        Checkbox(row.selected, row.onSelectedChange)
                    }
                }
                for (j in 0 until columns) {
                    Container(height = rowHeight, padding = cellSpacing) {
                        row.children(index = j)
                    }
                }
            }
        }

        // Data rows ripples
        tableDecoration(overlay = false) {
            val children = @Composable {
                rows.forEachIndexed { index, row ->
                    if (row.onSelectedChange == null) return@forEachIndexed
                    ParentData(data = index) {
                        Ripple(bounded = true) {
                            Clickable(onClick = { row.onSelectedChange.invoke(!row.selected) }) {
                                ColoredRect(
                                    color = if (row.selected) selectedColor else Color.Transparent
                                )
                            }
                        }
                    }
                }
            }
            Layout(children) { measurables, constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) {
                    measurables.forEach { measurable ->
                        val i = measurable.parentData as Int
                        val placeable = measurable.measure(
                            Constraints.tightConstraints(
                                width = constraints.maxWidth,
                                height = verticalOffsets[i + 2] - verticalOffsets[i + 1]
                            )
                        )
                        placeable.place(
                            x = IntPx.Zero,
                            y = verticalOffsets[i + 1]
                        )
                    }
                }
            }
        }
    }
}

private val RowHeight = 52.dp
private val HeaderHeight = 56.dp
private val CellSpacing = EdgeInsets(left = 16.dp, right = 16.dp)
private val BorderColor = Color(0xFFC6C6C6.toInt())
private val BorderWidth = 1.dp
