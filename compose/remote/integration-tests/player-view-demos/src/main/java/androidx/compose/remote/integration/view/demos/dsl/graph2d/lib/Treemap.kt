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

import android.annotation.SuppressLint
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.fillMaxSize

/**
 * Treemap: nested rectangles whose area is proportional to value, packed with the squarified
 * algorithm (Bruls et al.) so cells stay close to square. Layout runs host-side in a normalized
 * `[0,1]²` box (it depends only on the values), then each cell maps reactively into the live plot
 * rect — so the treemap reflows to its canvas.
 */

/** Squarified layout → `[n][x,y,w,h]` in `[0,1]`, indexed to match [values]. */
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
internal fun treemapLayout(values: FloatArray): Array<FloatArray> {
    val n = values.size
    val rects = Array(n) { FloatArray(4) }
    if (n == 0) return rects
    val order = (0 until n).sortedByDescending { values[it] }
    val total = order.sumOf { values[it].toDouble() }.coerceAtLeast(1e-9)
    val area = DoubleArray(n) { values[order[it]].toDouble() / total } // areas sum to 1

    var x = 0.0
    var y = 0.0
    var w = 1.0
    var h = 1.0
    var start = 0
    while (start < n) {
        val short = minOf(w, h)
        var end = start
        var rowSum = 0.0
        var bestWorst = Double.MAX_VALUE
        // Grow the row while squareness improves (areas are sorted descending).
        while (end < n) {
            val newSum = rowSum + area[end]
            val rmax = area[start]
            val rmin = area[end]
            val ss = newSum * newSum
            val ww = short * short
            val worst = maxOf(ww * rmax / ss, ss / (ww * rmin))
            if (worst <= bestWorst) {
                bestWorst = worst
                rowSum = newSum
                end++
            } else {
                break
            }
        }
        // Lay the row [start, end) along the shorter side.
        if (w >= h) {
            val colW = rowSum / h
            var cy = y
            for (t in start until end) {
                val cellH = if (rowSum > 0) area[t] / rowSum * h else 0.0
                rects[order[t]] =
                    floatArrayOf(x.toFloat(), cy.toFloat(), colW.toFloat(), cellH.toFloat())
                cy += cellH
            }
            x += colW
            w -= colW
        } else {
            val rowH = rowSum / w
            var cx = x
            for (t in start until end) {
                val cellW = if (rowSum > 0) area[t] / rowSum * w else 0.0
                rects[order[t]] =
                    floatArrayOf(cx.toFloat(), y.toFloat(), cellW.toFloat(), rowH.toFloat())
                cx += cellW
            }
            y += rowH
            h -= rowH
        }
        start = end
    }
    return rects
}

/** Treemap of named, sized rectangles. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.treemap(
    items: List<Pair<String, Number>>,
    title: String? = null,
    colors: IntArray? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = items.map { it.first }
    val values = FloatArray(items.size) { items[it].second.toFloat() }
    val layout = treemapLayout(values)
    Canvas(modifier = modifier) {
        if (theme.background ushr 24 != 0) {
            fillRectR(theme.background, 0f.rf, 0f.rf, componentWidth(), componentHeight())
        }
        val pad = theme.outerPad
        var top = pad
        if (title != null) {
            label(
                title,
                pad,
                pad + theme.titleSize,
                -1f,
                1f,
                theme.titleColor,
                theme.titleSize,
                bold = true,
            )
            top += theme.titleSize + theme.labelGap * 2f
        }
        val plotLeft = pad
        val compW = componentWidth()
        val compH = componentHeight()
        val plotW = (compW - 2f * pad).flush()
        val plotH = (compH - pad - top).flush()
        for (i in labels.indices) {
            val r = layout[i]
            val color = colors?.let { it[i % it.size] } ?: theme.seriesColor(i)
            val x0 = plotW * r[0] + plotLeft
            val y0 = plotH * r[1] + top
            val x1 = plotW * (r[0] + r[2]) + plotLeft
            val y1 = plotH * (r[1] + r[3]) + top
            fillRectR(
                color,
                x0 + theme.treemapGap,
                y0 + theme.treemapGap,
                x1 - theme.treemapGap,
                y1 - theme.treemapGap,
            )
            if (r[2] > 0.14f && r[3] > 0.1f) {
                val cx = plotW * (r[0] + r[2] * 0.5f) + plotLeft
                val cy = plotH * (r[1] + r[3] * 0.5f) + top
                labelR(labels[i], cx, cy, 0f, 0f, 0xFFFFFFFF.toInt(), theme.labelSize, bold = true)
            }
        }
    }
}
