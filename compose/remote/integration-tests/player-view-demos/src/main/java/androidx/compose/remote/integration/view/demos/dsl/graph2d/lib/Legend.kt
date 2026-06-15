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

package androidx.compose.remote.integration.view.demos.dsl.graph2d.lib

import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcFloat

/** A legend entry: a swatch color and its label. */
public class LegendEntry(public val label: String, public val color: Int)

/** Estimated width (px) of one legend entry including swatch + gap + trailing spacing. */
private fun entryWidth(e: LegendEntry, theme: GraphTheme): Float =
    theme.legendSize +
        theme.labelGap +
        estimateTextWidth(e.label, theme.legendSize) +
        theme.outerPad

/** Rows the legend reserves at the bottom (currently a single centered row). */
public fun legendRowCount(entries: List<LegendEntry>): Int = if (entries.isEmpty()) 0 else 1

/** Width (px) a right-side legend column reserves (widest entry + padding). */
public fun legendColumnWidth(entries: List<LegendEntry>, theme: GraphTheme): Float =
    if (entries.isEmpty()) 0f else (entries.maxOf { entryWidth(it, theme) } + theme.labelGap)

/**
 * Draw the legend as a column along the right edge, top-aligned with the plot. The column width is
 * host-known; its left edge tracks the live canvas width so it hugs the right margin on resize.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.drawLegendColumn(
    entries: List<LegendEntry>,
    compW: RcFloat,
    plotTop: Float,
    theme: GraphTheme,
) {
    if (entries.isEmpty()) return
    val swatch = theme.legendSize
    val colW = legendColumnWidth(entries, theme)
    val x = compW - (colW - theme.labelGap)
    var y = plotTop
    for (e in entries) {
        fillRectR(e.color, x, y.rf, x + swatch, (y + swatch).rf)
        labelR(
            e.label,
            x + (swatch + theme.labelGap),
            (y + swatch * 0.5f).rf,
            -1f,
            0f,
            theme.labelColor,
            theme.legendSize,
        )
        y += theme.legendSize + theme.labelGap * 1.5f
    }
}

/**
 * Draw the legend as a single horizontal row, centered and pinned to the bottom of the live canvas.
 * The row's total width is host-known (from label widths); only its center offset and baseline are
 * reactive, so the legend stays centered as the canvas resizes.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.drawLegend(
    entries: List<LegendEntry>,
    compW: RcFloat,
    compH: RcFloat,
    theme: GraphTheme,
) {
    if (entries.isEmpty()) return
    val swatch = theme.legendSize
    val rowW = entries.sumOf { entryWidth(it, theme).toDouble() }.toFloat()
    val startX = (compW - rowW) * 0.5f
    val y = compH - (theme.outerPad + theme.legendSize)
    var off = 0f
    for (e in entries) {
        val x = startX + off
        fillRectR(e.color, x, y, x + swatch, y + swatch)
        labelR(
            e.label,
            x + (swatch + theme.labelGap),
            y + swatch * 0.5f,
            -1f,
            0f,
            theme.labelColor,
            theme.legendSize,
        )
        off += entryWidth(e, theme)
    }
}
