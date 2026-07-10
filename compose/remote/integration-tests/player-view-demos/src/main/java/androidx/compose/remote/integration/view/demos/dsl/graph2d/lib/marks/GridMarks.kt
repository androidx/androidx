/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks

import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.estimateTextWidth
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.labelR
import kotlin.math.roundToInt

/**
 * Grid-based marks: a value heatmap and a waffle (unit) chart. Both compute a reactive plot rect
 * from the canvas size and tile it.
 */

/** Matrix heatmap: `values[row][col]` colored by a sequential [ramp], with row/column labels. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderHeatmap(
    rowLabels: List<String>,
    colLabels: List<String>,
    values: Array<FloatArray>,
    ramp: IntArray,
    showValues: Boolean,
    theme: GraphTheme,
    topReserve: Float,
) {
    val nRows = rowLabels.size
    val nCols = colLabels.size
    if (nRows == 0 || nCols == 0) return
    var lo = Float.MAX_VALUE
    var hi = -Float.MAX_VALUE
    for (r in values) for (v in r) {
        if (v < lo) lo = v
        if (v > hi) hi = v
    }
    val span = (hi - lo).let { if (it == 0f) 1f else it }
    val pad = theme.outerPad
    val rowLabelW =
        (rowLabels.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f) + theme.labelGap
    val colLabelH = theme.labelSize + theme.labelGap
    val plotLeft = pad + rowLabelW
    val plotTop = topReserve
    val compW = componentWidth()
    val compH = componentHeight()
    val cellW = ((compW - pad - plotLeft) * (1f / nCols)).flush()
    val cellH = ((compH - pad - colLabelH - plotTop) * (1f / nRows)).flush()

    for (r in 0 until nRows) {
        for (c in 0 until nCols) {
            val norm = (values[r][c] - lo) / span
            val color = Palette.sample(ramp, norm)
            val x0 = cellW * c.toFloat() + plotLeft
            val y0 = cellH * r.toFloat() + plotTop
            fillRectR(
                color,
                x0 + theme.cellGap,
                y0 + theme.cellGap,
                x0 + cellW - theme.cellGap,
                y0 + cellH - theme.cellGap,
            )
            if (showValues) {
                val txt =
                    if (values[r][c] == values[r][c].toLong().toFloat())
                        values[r][c].toLong().toString()
                    else (values[r][c] * 10f).roundToInt().div(10f).toString()
                val ink =
                    if (norm > theme.inkFlipThreshold) 0xFFFFFFFF.toInt() else theme.titleColor
                labelR(txt, x0 + cellW * 0.5f, y0 + cellH * 0.5f, 0f, 0f, ink, theme.labelSize)
            }
        }
        labelR(
            rowLabels[r],
            (plotLeft - theme.labelGap).rf,
            cellH * (r + 0.5f) + plotTop,
            1f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
    val labelY = cellH * nRows.toFloat() + (plotTop + theme.labelGap)
    for (c in 0 until nCols) {
        labelR(
            colLabels[c],
            cellW * (c + 0.5f) + plotLeft,
            labelY,
            0f,
            -1f,
            theme.labelColor,
            theme.labelSize,
        )
    }
}

/** Waffle / unit chart: a [rows]x[cols] grid of squares colored by category proportion. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderWaffle(
    values: FloatArray,
    colors: IntArray,
    rows: Int,
    cols: Int,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val cells = rows * cols
    val total = values.sum().let { if (it <= 0f) 1f else it }
    val counts = IntArray(values.size) { (values[it] / total * cells).roundToInt() }
    var assigned = counts.sum()
    // Clamp to exactly `cells`.
    var k = 0
    while (assigned > cells && counts.isNotEmpty()) {
        if (counts[k % counts.size] > 0) {
            counts[k % counts.size]--
            assigned--
        }
        k++
    }
    if (assigned < cells) counts[counts.size - 1] += cells - assigned
    val cellCat = IntArray(cells) { values.size - 1 }
    var idx = 0
    for (i in values.indices) repeat(counts[i]) { if (idx < cells) cellCat[idx++] = i }

    val pad = theme.outerPad
    val compW = componentWidth()
    val compH = componentHeight()
    val availH = compH - (topReserve + bottomReserve)
    val cell = ((compW - 2f * pad) * (1f / cols)).min(availH * (1f / rows)).flush()
    val gridW = cell * cols.toFloat()
    val gridH = cell * rows.toFloat()
    val gx = (compW - gridW) * 0.5f
    val gy = (availH - gridH) * 0.5f + topReserve
    val g = cell * theme.waffleCellGapFrac // gap

    for (cellIdx in 0 until cells) {
        val r = cellIdx / cols
        val c = cellIdx % cols
        val x0 = cell * c.toFloat() + gx
        val y0 = cell * (rows - 1 - r).toFloat() + gy // fill bottom-up
        fillRectR(
            colors[cellCat[cellIdx] % colors.size],
            x0 + g,
            y0 + g,
            x0 + cell - g,
            y0 + cell - g,
        )
    }
}
