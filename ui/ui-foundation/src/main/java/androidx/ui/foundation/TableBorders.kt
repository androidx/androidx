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

package androidx.ui.foundation

import androidx.compose.remember
import androidx.ui.core.Draw
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.Table
import androidx.ui.layout.TableChildren
import androidx.ui.unit.Dp

/**
 * Adds border drawing for a [Table] layout, when placed inside the [TableChildren] block.
 *
 * Example usage:
 *
 * @sample androidx.ui.foundation.samples.TableWithBorders
 *
 * @param defaultBorder The default [Border] appearance to be used for borders that do not
 * specify a style.
 */
// TODO (popam): this should be a modifier b/150276337
fun TableChildren.drawBorders(
    defaultBorder: Border = Border(color = Color.Black, size = Dp.Hairline),
    block: DrawBordersReceiver.() -> Unit
) {
    tableDecoration(overlay = true) {
        val paint = remember { Paint() }
        @Suppress("DEPRECATION") // remove when b/150276337 is fixed
        Draw { canvas, _ ->
            val borders = DrawBordersReceiver(
                rowCount = verticalOffsets.size - 1,
                columnCount = horizontalOffsets.size - 1,
                defaultBorder = defaultBorder
            ).also(block).borders
            for ((border, row, column, orientation) in borders) {
                val p1 = Offset(
                    dx = horizontalOffsets[column].value.toFloat(),
                    dy = verticalOffsets[row].value.toFloat()
                )
                val p2 = when (orientation) {
                    BorderOrientation.Vertical -> p1.copy(
                        dy = verticalOffsets[row + 1].value.toFloat()
                    )
                    BorderOrientation.Horizontal -> p1.copy(
                        dx = horizontalOffsets[column + 1].value.toFloat()
                    )
                }
                // TODO(calintat): Reset paint when that operation is available.
                border.brush.applyTo(paint)
                paint.strokeWidth = border.size.toPx().value
                canvas.drawLine(p1, p2, paint)
            }
        }
    }
}

/**
 * Collects information about the borders specified by [drawBorders]
 * when its body is executed with a [DrawBordersReceiver] instance as argument.
 */
class DrawBordersReceiver internal constructor(
    private val rowCount: Int,
    private val columnCount: Int,
    private val defaultBorder: Border
) {
    internal val borders = mutableListOf<BorderInfo>()

    /**
     * Add all borders.
     */
    fun all(border: Border = defaultBorder) {
        allVertical(border)
        allHorizontal(border)
    }

    /**
     * Add all outer borders.
     */
    fun outer(border: Border = defaultBorder) {
        left(border)
        top(border)
        right(border)
        bottom(border)
    }

    /**
     * Add a vertical border before the first column.
     */
    fun left(border: Border = defaultBorder) = vertical(column = 0, border = border)

    /**
     * Add a horizontal border before the first row.
     */
    fun top(border: Border = defaultBorder) = horizontal(row = 0, border = border)

    /**
     * Add a vertical border after the last column.
     */
    fun right(border: Border = defaultBorder) = vertical(column = columnCount, border = border)

    /**
     * Add a horizontal border after the last row.
     */
    fun bottom(border: Border = defaultBorder) = horizontal(row = rowCount, border = border)

    /**
     * Add all vertical borders.
     */
    fun allVertical(border: Border = defaultBorder) {
        for (column in 0..columnCount) {
            vertical(column, border = border)
        }
    }

    /**
     * Add all horizontal borders.
     */
    fun allHorizontal(border: Border = defaultBorder) {
        for (row in 0..rowCount) {
            horizontal(row, border = border)
        }
    }

    /**
     * Add a vertical border before [column] at the rows specified by [rows].
     */
    fun vertical(
        column: Int,
        rows: IntRange = 0 until rowCount,
        border: Border = defaultBorder
    ) {
        if (column in 0..columnCount && 0 <= rows.start && rows.endInclusive < rowCount) {
            for (row in rows) {
                borders += BorderInfo(
                    border = border,
                    row = row,
                    column = column,
                    orientation = BorderOrientation.Vertical
                )
            }
        }
    }

    /**
     * Add a horizontal border before [row] at the columns specified by [columns].
     */
    fun horizontal(
        row: Int,
        columns: IntRange = 0 until columnCount,
        border: Border = defaultBorder
    ) {
        if (row in 0..rowCount && 0 <= columns.start && columns.endInclusive < columnCount) {
            for (column in columns) {
                borders += BorderInfo(
                    border = border,
                    row = row,
                    column = column,
                    orientation = BorderOrientation.Horizontal
                )
            }
        }
    }
}

internal data class BorderInfo(
    val border: Border,
    val row: Int,
    val column: Int,
    val orientation: BorderOrientation
)

internal enum class BorderOrientation { Vertical, Horizontal }
