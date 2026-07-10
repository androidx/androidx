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
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcStrokeJoin
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Cartesian
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Stats
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.estimateTextWidth
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.labelR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineR

/** Gantt: a horizontal time bar per task on a horizontal cartesian (value = time on x). */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderGantt(
    cart: Cartesian,
    starts: FloatArray,
    ends: FloatArray,
    colors: IntArray,
) {
    for (i in starts.indices) {
        val cy = cart.band.center(i)
        val half = cart.band.bandWidth * 0.34f
        val x0 = cart.value.map(starts[i])
        val x1 = cart.value.map(ends[i])
        fillRectR(colors[i % colors.size], x0, cy - half, x1, cy + half)
    }
}

/**
 * Ridgeline (joy plot): one filled KDE per group, stacked with overlap over a shared value axis.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderRidgeline(
    labels: List<String>,
    curves: List<Stats.Curve>,
    xLo: Float,
    xHi: Float,
    theme: GraphTheme,
    topReserve: Float,
) {
    val n = labels.size
    if (n == 0) return
    val pad = theme.outerPad
    val labelW =
        (labels.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f) + theme.labelGap
    val plotLeft = pad + labelW
    val compW = componentWidth()
    val compH = componentHeight()
    val labelLine = theme.labelSize + theme.labelGap
    val plotRight = (compW - pad).flush()
    val plotW = (compW - pad - plotLeft).flush()
    val plotTop = topReserve
    val plotBottom = (compH - pad - labelLine).flush()
    val rowStep = ((compH - pad - labelLine - plotTop) * (1f / n)).flush()
    var maxD = 1e-6f
    for (c in curves) for (d in c.ys) if (d > maxD) maxD = d
    val spanX = (xHi - xLo).let { if (it == 0f) 1f else it }
    val scaleX = (plotW * (1f / spanX)).flush()
    fun x(v: Float): RcFloat = scaleX * (v - xLo) + plotLeft
    val amp = theme.ridgelineOverlap
    for (i in 0 until n) {
        val curve = curves[i]
        val gN = curve.xs.size
        if (gN < 2) continue
        val color = theme.seriesColor(i)
        val yBase = rowStep * (i + 1f) + plotTop
        val path = remotePath(x(curve.xs[0]).toFloat(), yBase.toFloat())
        for (g in 0 until gN) {
            val yy = yBase - rowStep * (amp * (curve.ys[g] / maxD))
            path.lineTo(x(curve.xs[g]).toFloat(), yy.toFloat())
        }
        path.lineTo(x(curve.xs[gN - 1]).toFloat(), yBase.toFloat())
        path.close()
        paint {
            color(Palette.fadeAlpha(color, theme.ridgelineFillAlpha))
            style(RcPaintStyle.Fill)
        }
        drawPath(path.getPath())
        paint {
            color(color)
            style(RcPaintStyle.Stroke)
            strokeWidth(1.5f)
            strokeJoin(RcStrokeJoin.Round)
        }
        drawPath(path.getPath())
        labelR(
            labels[i],
            (plotLeft - theme.labelGap).rf,
            yBase,
            1f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
    strokeLineR(theme.axisColor, theme.axisStroke, plotLeft.rf, plotBottom, plotRight, plotBottom)
}

/** Likert / diverging stacked bars: response levels diverging from a center line per question. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderLikert(
    questions: List<String>,
    data: Array<FloatArray>,
    levelColors: IntArray,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val n = questions.size
    if (n == 0) return
    val levels = levelColors.size
    val mid = levels / 2
    val odd = levels % 2 == 1
    val pad = theme.outerPad
    val labelW =
        (questions.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f) + theme.labelGap
    val trackLeft = pad + labelW
    val compW = componentWidth()
    val compH = componentHeight()
    val cx = ((compW - pad + trackLeft) * 0.5f).flush()
    val halfW = ((compW - pad - trackLeft) * 0.5f).flush()
    // Percentages and the largest one-sided extent.
    val pct = Array(n) { FloatArray(levels) }
    var maxSide = 1e-3f
    for (q in 0 until n) {
        val total = data[q].sum().let { if (it <= 0f) 1f else it }
        for (l in 0 until levels) pct[q][l] = data[q][l] / total * 100f
        var left = if (odd) pct[q][mid] / 2f else 0f
        var right = if (odd) pct[q][mid] / 2f else 0f
        for (l in 0 until mid) left += pct[q][l]
        for (l in (if (odd) mid + 1 else mid) until levels) right += pct[q][l]
        if (left > maxSide) maxSide = left
        if (right > maxSide) maxSide = right
    }
    val pxPerPct = (halfW * (1f / maxSide)).flush()
    val areaH = (compH - (topReserve + bottomReserve)).flush()
    val rowH = (areaH * (1f / n)).flush()
    for (q in 0 until n) {
        val yMid = rowH * (q + 0.5f) + topReserve
        val barTop = yMid - rowH * theme.likertBarFrac
        val barBot = yMid + rowH * theme.likertBarFrac
        var xr = cx
        if (odd) {
            val w = pxPerPct * (pct[q][mid] / 2f)
            fillRectR(levelColors[mid], xr, barTop, xr + w, barBot)
            xr = (xr + w).flush()
        }
        for (l in (if (odd) mid + 1 else mid) until levels) {
            val w = pxPerPct * pct[q][l]
            fillRectR(levelColors[l], xr, barTop, xr + w, barBot)
            xr = (xr + w).flush()
        }
        var xl = cx
        if (odd) {
            val w = pxPerPct * (pct[q][mid] / 2f)
            fillRectR(levelColors[mid], xl - w, barTop, xl, barBot)
            xl = (xl - w).flush()
        }
        for (l in mid - 1 downTo 0) {
            val w = pxPerPct * pct[q][l]
            fillRectR(levelColors[l], xl - w, barTop, xl, barBot)
            xl = (xl - w).flush()
        }
        labelR(
            questions[q],
            (trackLeft - theme.labelGap).rf,
            yMid,
            1f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
    strokeLineR(theme.zeroLineColor, theme.axisStroke, cx, topReserve.rf, cx, compH - bottomReserve)
}
