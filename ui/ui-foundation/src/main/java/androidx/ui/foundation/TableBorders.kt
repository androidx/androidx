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
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.SolidColor
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
 * @param defaultBorderBrush The default brush to be used for borders that do not specify a style.
 * @param defaultBorderWidth The default width to be used for borders that do not specify a style.
 */
fun TableChildren.drawBorders(
    defaultBorderWidth: Dp = Dp.Hairline,
    defaultBorderBrush: Brush = SolidColor(Color.Black),
    block: DrawBordersReceiver.() -> Unit
) {
    tableDecoration(overlay = true) {
        val paint = remember { Paint() }
        Draw { canvas, _ ->
            val borders = DrawBordersReceiver(
                rowCount = verticalOffsets.size - 1,
                columnCount = horizontalOffsets.size - 1,
                defaultBorderWidth = defaultBorderWidth,
                defaultBorderBrush = defaultBorderBrush
            ).also(block).borders
            for ((borderWidth, borderBrush, row, column, orientation) in borders) {
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
                borderBrush.applyTo(paint)
                paint.strokeWidth = borderWidth.toPx().value
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
    private val defaultBorderWidth: Dp,
    private val defaultBorderBrush: Brush
) {
    internal val borders = mutableListOf<BorderInfo>()

    /**
     * Add all borders.
     */
    fun all(borderWidth: Dp = defaultBorderWidth, borderBrush: Brush = defaultBorderBrush) {
        allVertical(borderWidth, borderBrush)
        allHorizontal(borderWidth, borderBrush)
    }

    /**
     * Add all outer borders.
     */
    fun outer(borderWidth: Dp = defaultBorderWidth, borderBrush: Brush = defaultBorderBrush) {
        left(borderWidth, borderBrush)
        top(borderWidth, borderBrush)
        right(borderWidth, borderBrush)
        bottom(borderWidth, borderBrush)
    }

    /**
     * Add a vertical border before the first column.
     */
    fun left(borderWidth: Dp = defaultBorderWidth, borderBrush: Brush = defaultBorderBrush) =
        vertical(column = 0, borderWidth = borderWidth, borderBrush = borderBrush)

    /**
     * Add a horizontal border before the first row.
     */
    fun top(borderWidth: Dp = defaultBorderWidth, borderBrush: Brush = defaultBorderBrush) =
        horizontal(row = 0, borderWidth = borderWidth, borderBrush = borderBrush)

    /**
     * Add a vertical border after the last column.
     */
    fun right(borderWidth: Dp = defaultBorderWidth, borderBrush: Brush = defaultBorderBrush) =
        vertical(column = columnCount, borderWidth = borderWidth, borderBrush = borderBrush)

    /**
     * Add a horizontal border after the last row.
     */
    fun bottom(borderWidth: Dp = defaultBorderWidth, borderBrush: Brush = defaultBorderBrush) =
        horizontal(row = rowCount, borderWidth = borderWidth, borderBrush = borderBrush)

    /**
     * Add all vertical borders.
     */
    fun allVertical(borderWidth: Dp = defaultBorderWidth, borderBrush: Brush = defaultBorderBrush) {
        for (column in 0..columnCount) {
            vertical(column, borderWidth = borderWidth, borderBrush = borderBrush)
        }
    }

    /**
     * Add all horizontal borders.
     */
    fun allHorizontal(
        borderWidth: Dp = defaultBorderWidth,
        borderBrush: Brush = defaultBorderBrush
    ) {
        for (row in 0..rowCount) {
            horizontal(row, borderWidth = borderWidth, borderBrush = borderBrush)
        }
    }

    /**
     * Add a vertical border before [column] at the rows specified by [rows].
     */
    fun vertical(
        column: Int,
        rows: IntRange = 0 until rowCount,
        borderWidth: Dp = defaultBorderWidth,
        borderBrush: Brush = defaultBorderBrush
    ) {
        if (column in 0..columnCount && 0 <= rows.start && rows.endInclusive < rowCount) {
            for (row in rows) {
                borders += BorderInfo(
                    borderWidth = borderWidth,
                    borderBrush = borderBrush,
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
        borderWidth: Dp = defaultBorderWidth,
        borderBrush: Brush = defaultBorderBrush
    ) {
        if (row in 0..rowCount && 0 <= columns.start && columns.endInclusive < columnCount) {
            for (column in columns) {
                borders += BorderInfo(
                    borderWidth = borderWidth,
                    borderBrush = borderBrush,
                    row = row,
                    column = column,
                    orientation = BorderOrientation.Horizontal
                )
            }
        }
    }
}

internal data class BorderInfo(
    val borderWidth: Dp,
    val borderBrush: Brush,
    val row: Int,
    val column: Int,
    val orientation: BorderOrientation
)

internal enum class BorderOrientation { Vertical, Horizontal }
