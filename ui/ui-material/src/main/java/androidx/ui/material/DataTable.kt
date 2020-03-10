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
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Layout
import androidx.ui.core.ParentData
import androidx.ui.core.Text
import androidx.ui.foundation.Border
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.SimpleImage
import androidx.ui.foundation.drawBorders
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.graphics.Color
import androidx.ui.graphics.ImageAsset
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.Table
import androidx.ui.layout.TableColumnWidth
import androidx.ui.material.ripple.Ripple
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp

/**
 * Pagination configuration for a [DataTable].
 */
data class DataTablePagination(
    /**
     * The index of the current page (starting from zero).
     */
    val page: Int,

    /**
     * The number of rows to show on each page.
     */
    val rowsPerPage: Int,

    /**
     * The options to offer for the number of rows per page.
     *
     * The current value of [rowsPerPage] must be in this list.
     */
    val availableRowsPerPage: List<Int>,

    /**
     * Invoked when the user switches to another page.
     */
    val onPageChange: (Int) -> Unit,

    /**
     * Invoked when the user selects a different number of rows per page.
     */
    val onRowsPerPageChange: (Int) -> Unit
)

/**
 * Creates a pagination configuration for [DataTable] with the given initial values.
 *
 * Example usage:
 *
 * @sample androidx.ui.material.samples.DataTableWithPagination
 */
@Composable
fun DefaultDataTablePagination(
    initialPage: Int = 0,
    initialRowsPerPage: Int,
    availableRowsPerPage: List<Int>
): DataTablePagination {
    val page = state { initialPage }
    val rowsPerPage = state { initialRowsPerPage }
    return DataTablePagination(
        page = page.value,
        rowsPerPage = rowsPerPage.value,
        availableRowsPerPage = availableRowsPerPage,
        onPageChange = { page.value = it },
        onRowsPerPageChange = { rowsPerPage.value = it }
    )
}

/**
 * Sorting configuration for a [DataTable].
 */
data class DataTableSorting(
    /**
     * The index of the current column, if any, by which the data is sorted.
     *
     * When this is null, it implies that the table's sort order does not correspond to any of the
     * columns. Setting this to a non-null value will display a sort indicator next to that column.
     */
    val column: Int?,

    /**
     * Whether the column specified by [column], if non-null, is sorted in ascending order.
     */
    val ascending: Boolean,

    /**
     * The columns by which the data can be sorted.
     *
     * The current value of [column], if non-null, must be in this set.
     */
    val sortableColumns: Set<Int>,

    /**
     * Called when the user asks to sort the table.
     */
    val onSortChange: (column: Int, ascending: Boolean) -> Unit
)

/**
 * Creates a sorting configuration for [DataTable] with the given initial values.
 *
 * Example usage:
 *
 * @sample androidx.ui.material.samples.DataTableWithSorting
 */
@Composable
fun DefaultDataTableSorting(
    initialColumn: Int? = null,
    initialAscending: Boolean = true,
    sortableColumns: Set<Int>,
    onSortRequest: (column: Int, ascending: Boolean) -> Unit
): DataTableSorting {
    val column = state { initialColumn }
    val ascending = state { initialAscending }
    return DataTableSorting(
        column = column.value,
        ascending = ascending.value,
        sortableColumns = sortableColumns,
        onSortChange = { newColumn, newAscending ->
            column.value = newColumn
            ascending.value = newAscending
            onSortRequest(newColumn, newAscending)
        }
    )
}

/**
 * Collects information about the children of a [DataTable] when
 * its body is executed with a [DataTableChildren] as argument.
 */
class DataTableChildren internal constructor() {
    internal var header: HeaderRowInfo? = null
    internal val rows = mutableListOf<DataRowInfo>()

    /**
     * Creates a data row in a [DataTable] with the given content.
     *
     * If [onSelectedChange] is non-null for any row in the table, then a checkbox is shown at the
     * start of each row. The checkbox will be checked if and only if the row is selected (true).
     *
     * @param selected Whether this row is selected.
     * @param onSelectedChange Called when a user selects or unselects this row.
     */
    fun dataRow(
        selected: Boolean = false,
        onSelectedChange: ((Boolean) -> Unit)? = null,
        children: @Composable() (index: Int) -> Unit
    ) {
        rows += DataRowInfo(children, selected, onSelectedChange)
    }

    /**
     * Creates a data row in a [DataTable] with a [text] and an optional [icon].
     *
     * If [onSelectedChange] is non-null for any row in the table, then a checkbox is shown at the
     * start of each row. The checkbox will be checked if and only if the row is selected (true).
     *
     * @param text Text to display in each cell.
     * @param icon Optional image to draw to the left of the text in each cell.
     * @param selected Whether this row is selected.
     * @param onSelectedChange Called when a user selects or unselects this row.
     */
    fun dataRow(
        text: (index: Int) -> String,
        icon: (index: Int) -> ImageAsset? = { null },
        selected: Boolean = false,
        onSelectedChange: ((Boolean) -> Unit)? = null
    ) {
        val children: @Composable() (Int) -> Unit = { j ->
            val image = icon(j)
            if (image == null) {
                Text(text = text(j))
            } else {
                Row {
                    SimpleImage(image = image)
                    Spacer(LayoutWidth(2.dp))
                    Text(text = text(j))
                }
            }
        }
        rows += DataRowInfo(children, selected, onSelectedChange)
    }

    /**
     * Creates a header row in a [DataTable] with the given content.
     *
     * Note that the [onSelectAll] callback may be null, in which case the default behaviour will
     * be used, i.e. select or unselect all selectable rows using their onSelectedChange callbacks.
     *
     * @param onSelectAll Called when a user selects or unselects all rows using the 'all' checkbox.
     */
    fun headerRow(
        onSelectAll: ((Boolean) -> Unit)? = null,
        children: @Composable() (index: Int) -> Unit
    ) {
        header = HeaderRowInfo(children, onSelectAll)
    }

    /**
     * Creates a header row in a [DataTable] with a [text] and an optional [icon].
     *
     * Note that the [onSelectAll] callback may be null, in which case the default behaviour will
     * be used, i.e. select or unselect all selectable rows using their onSelectedChange callbacks.
     *
     * @param text Text to display in each column header
     * @param icon Optional image to draw to the left of the text in each column header.
     * @param onSelectAll Called when a user selects or unselects all rows using the 'all' checkbox.
     */
    fun headerRow(
        text: (index: Int) -> String,
        icon: (index: Int) -> ImageAsset? = { null },
        onSelectAll: ((Boolean) -> Unit)? = null
    ) {
        val children: @Composable() (Int) -> Unit = { j ->
            val image = icon(j)
            if (image == null) {
                Text(text = text(j))
            } else {
                Row {
                    SimpleImage(image = image)
                    Spacer(LayoutWidth(2.dp))
                    Text(text = text(j))
                }
            }
        }
        header = HeaderRowInfo(children, onSelectAll)
    }
}

/**
 * Configuration for the data row of a [DataTable].
 */
internal data class DataRowInfo(
    val children: @Composable() (index: Int) -> Unit,
    val selected: Boolean,
    val onSelectedChange: ((Boolean) -> Unit)?
)

/**
 * Configuration for the header row of a [DataTable].
 */
internal data class HeaderRowInfo(
    val children: @Composable() (index: Int) -> Unit,
    val onSelectAll: ((Boolean) -> Unit)?
)

/**
 * Data tables display information in a grid-like format of rows and columns. They organize
 * information in a way that’s easy to scan, so that users can look for patterns and insights.
 *
 * Example usage:
 *
 * @sample androidx.ui.material.samples.SimpleDataTable
 *
 * To make a data table paginated, you must provide a [pagination] configuration:
 *
 * @sample androidx.ui.material.samples.DataTableWithPagination
 *
 * To enable sorting when clicking on the column headers, provide a [sorting] configuration:
 *
 * @sample androidx.ui.material.samples.DataTableWithSorting
 *
 * @param columns The number of columns in the table.
 * @param numeric Whether the given column represents numeric data.
 * @param dataRowHeight The height of each row (excluding the header row).
 * @param headerRowHeight The height of the header row.
 * @param cellSpacing The padding to apply around each cell.
 * @param border [Border] class that specifies border appearance, such as size or color.
 * @param selectedColor The color used to indicate selected rows.
 * @param pagination Contains the pagination configuration. To disable pagination, set this to null.
 * @param sorting Contains the sorting configuration. To disable sorting, set this to null.
 */
@Composable
fun DataTable(
    columns: Int,
    numeric: (Int) -> Boolean = { false },
    dataRowHeight: Dp = DataRowHeight,
    headerRowHeight: Dp = HeaderRowHeight,
    cellSpacing: EdgeInsets = CellSpacing,
    border: Border = Border(color = BorderColor, size = BorderWidth),
    selectedColor: Color = MaterialTheme.colors().primary.copy(alpha = 0.08f),
    pagination: DataTablePagination? = null,
    sorting: DataTableSorting? = null,
    block: DataTableChildren.() -> Unit
) {
    val scope = DataTableChildren()
    scope.block()
    val rows = scope.rows
    val header = scope.header

    val selectableRows = rows.filter { it.onSelectedChange != null }
    val showCheckboxes = selectableRows.isNotEmpty()

    val visibleRows = if (pagination == null) {
        rows
    } else {
        rows.drop(pagination.rowsPerPage * pagination.page).take(pagination.rowsPerPage)
    }

    val table = @Composable {
        Table(
            columns = columns + if (showCheckboxes) 1 else 0,
            alignment = { j ->
                if (numeric(j - if (showCheckboxes) 1 else 0)) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
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
            if (header != null) {
                tableRow {
                    if (showCheckboxes) {
                        Container(height = headerRowHeight, padding = cellSpacing) {
                            val parentState = when (selectableRows.count { it.selected }) {
                                selectableRows.size -> ToggleableState.On
                                0 -> ToggleableState.Off
                                else -> ToggleableState.Indeterminate
                            }
                            TriStateCheckbox(value = parentState, onClick = {
                                val newValue = parentState != ToggleableState.On
                                if (header.onSelectAll != null) {
                                    header.onSelectAll.invoke(newValue)
                                } else {
                                    rows.forEach { it.onSelectedChange?.invoke(newValue) }
                                }
                            })
                        }
                    }
                    for (j in 0 until columns) {
                        Container(height = headerRowHeight, padding = cellSpacing) {
                            var fontWeight = FontWeight.W500
                            var onSort = {}
                            var enabled = false
                            var headerDecoration: @Composable() (() -> Unit)? = null

                            if (sorting != null && sorting.sortableColumns.contains(j)) {
                                if (sorting.column == j) {
                                    fontWeight = FontWeight.Bold
                                    onSort = {
                                        sorting.onSortChange(j, !sorting.ascending)
                                    }
                                    enabled = true
                                    headerDecoration = {
                                        // TODO(calintat): Replace with animated arrow icons.
                                        Text(text = if (sorting.ascending) "↑" else "↓")
                                        Spacer(LayoutWidth(2.dp))
                                    }
                                } else {
                                    onSort = {
                                        sorting.onSortChange(j, true)
                                    }
                                }
                            }

                            CurrentTextStyleProvider(TextStyle(fontWeight = fontWeight)) {
                                Ripple(bounded = true) {
                                    Clickable(onClick = onSort, enabled = enabled) {
                                        Row {
                                            headerDecoration?.invoke()
                                            header.children(index = j)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Data rows
            visibleRows.forEach { row ->
                tableRow {
                    if (showCheckboxes) {
                        Container(height = dataRowHeight, padding = cellSpacing) {
                            Checkbox(row.selected, row.onSelectedChange)
                        }
                    }
                    for (j in 0 until columns) {
                        Container(height = dataRowHeight, padding = cellSpacing) {
                            row.children(index = j)
                        }
                    }
                }
            }

            // Data rows ripples
            tableDecoration(overlay = false) {
                val children = @Composable {
                    visibleRows.forEachIndexed { index, row ->
                        if (row.onSelectedChange == null) return@forEachIndexed
                        ParentData(data = index) {
                            Ripple(bounded = true) {
                                Clickable(
                                    onClick = { row.onSelectedChange.invoke(!row.selected) }
                                ) {
                                    ColoredRect(
                                        color = if (row.selected) {
                                            selectedColor
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Layout(children) { measurables, constraints, _ ->
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        measurables.forEach { measurable ->
                            val i = measurable.parentData as Int
                            val placeable = measurable.measure(
                                Constraints.fixed(
                                    width = constraints.maxWidth,
                                    height = verticalOffsets[i + 2] - verticalOffsets[i + 1]
                                )
                            )
                            placeable.placeAbsolute(
                                x = IntPx.Zero,
                                y = verticalOffsets[i + 1]
                            )
                        }
                    }
                }
            }
        }
    }

    if (pagination == null) {
        table()
    } else {
        Column {
            table()
            Container(height = dataRowHeight, padding = cellSpacing) {
                Row(LayoutSize.Fill, arrangement = Arrangement.End) {
                    val pages = (rows.size - 1) / pagination.rowsPerPage + 1
                    val startRow = pagination.rowsPerPage * pagination.page
                    val endRow = (startRow + pagination.rowsPerPage).coerceAtMost(rows.size)
                    val modifier = LayoutGravity.Center

                    // TODO(calintat): Replace this with a dropdown menu whose items are taken
                    //  from availableRowsPerPage (filtered to those that are in the range
                    //  0 until rows.size). When an item is selected, it should invoke
                    //  onRowsPerPageChange with the appropriate value.
                    Text(text = "Rows per page: ${pagination.rowsPerPage}", modifier = modifier)

                    Spacer(LayoutWidth(32.dp))

                    Text(text = "${startRow + 1}-$endRow of ${rows.size}", modifier = modifier)

                    Spacer(LayoutWidth(32.dp))

                    // TODO(calintat): Replace this with an image button with chevron_left icon.
                    Container(modifier = modifier) {
                        Ripple(bounded = false) {
                            Clickable(onClick = {
                                val newPage = pagination.page - 1
                                if (newPage >= 0)
                                    pagination.onPageChange.invoke(newPage)
                            }) {
                                Text(text = "Prev")
                            }
                        }
                    }

                    Spacer(LayoutWidth(24.dp))

                    // TODO(calintat): Replace this with an image button with chevron_right icon.
                    Container(modifier = modifier) {
                        Ripple(bounded = false) {
                            Clickable(onClick = {
                                val newPage = pagination.page + 1
                                if (newPage < pages)
                                    pagination.onPageChange.invoke(newPage)
                            }) {
                                Text(text = "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

private val DataRowHeight = 52.dp
private val HeaderRowHeight = 56.dp
private val CellSpacing = EdgeInsets(left = 16.dp, right = 16.dp)
private val BorderColor = Color(0xFFC6C6C6)
private val BorderWidth = 1.dp
