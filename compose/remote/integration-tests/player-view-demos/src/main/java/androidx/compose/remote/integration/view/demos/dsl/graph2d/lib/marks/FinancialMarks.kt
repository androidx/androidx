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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Cartesian
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.estimateTextWidth
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.labelR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineR
import kotlin.math.roundToInt

/**
 * Financial / ranking marks. Waterfall + Pareto sit on a categorical [Cartesian]; funnel and bullet
 * compute their own reactive layout from the canvas size.
 */

/** Waterfall: floating bars from the running cumulative, colored by sign, optional total bar. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderWaterfall(
    cart: Cartesian,
    deltas: FloatArray,
    showTotal: Boolean,
    posColor: Int,
    negColor: Int,
    totalColor: Int,
) {
    val baseY = cart.value.map(0f)
    var cum = 0f
    var prevTopY: RcFloat? = null
    var prevX1: RcFloat? = null
    for (i in deltas.indices) {
        val from = cum
        val to = cum + deltas[i]
        cum = to
        val x0 = cart.band.bandStart(i)
        val x1 = x0 + cart.band.bandWidth
        val yFrom = cart.value.map(from)
        val yTo = cart.value.map(to)
        val color = if (deltas[i] >= 0f) posColor else negColor
        if (deltas[i] >= 0f) fillRectR(color, x0, yTo, x1, yFrom)
        else fillRectR(color, x0, yFrom, x1, yTo)
        if (prevTopY != null && prevX1 != null) {
            strokeLineR(cart.theme.zeroLineColor, cart.theme.gridStroke, prevX1, yFrom, x0, yFrom)
        }
        prevTopY = yTo
        prevX1 = x1
    }
    if (showTotal) {
        val i = deltas.size
        val x0 = cart.band.bandStart(i)
        val x1 = x0 + cart.band.bandWidth
        val yTotal = cart.value.map(cum)
        if (cum >= 0f) fillRectR(totalColor, x0, yTotal, x1, baseY)
        else fillRectR(totalColor, x0, baseY, x1, yTotal)
    }
}

/** Funnel: stacked centered trapezoids, width ∝ value. Builder pre-draws background + title. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderFunnel(
    labels: List<String>,
    values: FloatArray,
    theme: GraphTheme,
    topReserve: Float,
) {
    val n = values.size
    if (n == 0) return
    var maxV = values[0]
    for (v in values) if (v > maxV) maxV = v
    if (maxV <= 0f) return
    val pad = theme.outerPad
    val compW = componentWidth()
    val compH = componentHeight()
    val cx = (compW * 0.5f).flush()
    val halfW = (compW * 0.5f - pad).flush() // max half-width
    val areaH = (compH - (pad + topReserve)).flush()
    val stageH = (areaH * (1f / n)).flush()

    fun half(v: Float): RcFloat = halfW * (v / maxV)
    for (i in 0 until n) {
        val yT = stageH * i.toFloat() + (topReserve)
        val yB = stageH * (i + 1f) + (topReserve)
        val wT = half(values[i])
        val wB = half(if (i + 1 < n) values[i + 1] else values[i])
        val color = theme.seriesColor(i)
        val path = remotePath((cx - wT).toFloat(), yT.toFloat())
        path.lineTo((cx + wT).toFloat(), yT.toFloat())
        path.lineTo((cx + wB).toFloat(), yB.toFloat())
        path.lineTo((cx - wB).toFloat(), yB.toFloat())
        path.close()
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        drawPath(path.getPath())
        val pct = (values[i] / maxV * 100f).roundToInt()
        labelR(
            "${labels[i]}  $pct%",
            cx,
            yT + stageH * 0.5f,
            0f,
            0f,
            0xFFFFFFFF.toInt(),
            theme.labelSize,
            bold = true,
        )
    }
}

/** One bullet row spec. */
internal class BulletRow(
    val label: String,
    val value: Float,
    val target: Float,
    val max: Float,
    val ranges: FloatArray,
)

/** Bullet KPI rows: qualitative range bands, a measure bar, and a target tick. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderBullets(
    items: List<BulletRow>,
    theme: GraphTheme,
    topReserve: Float,
) {
    if (items.isEmpty()) return
    val pad = theme.outerPad
    val labelW =
        (items.maxOfOrNull { estimateTextWidth(it.label, theme.labelSize) } ?: 0f) + theme.labelGap
    val trackLeft = pad + labelW
    val compW = componentWidth()
    val compH = componentHeight()
    val trackW = (compW - pad - trackLeft).flush()
    val areaH = (compH - (pad + topReserve)).flush()
    val rowH = (areaH * (1f / items.size)).flush()

    for (i in items.indices) {
        val item = items[i]
        val yTop = rowH * i.toFloat() + topReserve
        val yMid = yTop + rowH * 0.5f
        val bandHalf = rowH * 0.34f
        val barHalf = rowH * 0.16f
        fun xAt(v: Float): RcFloat = trackW * (v / item.max) + trackLeft
        // Qualitative range bands (light → darker grey).
        var prev = 0f
        for (j in item.ranges.indices) {
            val shade =
                Palette.lerp(
                    0xFFE8E8EA.toInt(),
                    0xFFB8B8BE.toInt(),
                    j.toFloat() / maxOf(1, item.ranges.size - 1),
                )
            fillRectR(shade, xAt(prev), yMid - bandHalf, xAt(item.ranges[j]), yMid + bandHalf)
            prev = item.ranges[j]
        }
        // Measure bar.
        fillRectR(theme.titleColor, trackLeft.rf, yMid - barHalf, xAt(item.value), yMid + barHalf)
        // Target tick.
        strokeLineR(
            0xFFE45756.toInt(),
            3f,
            xAt(item.target),
            yMid - bandHalf,
            xAt(item.target),
            yMid + bandHalf,
        )
        // Label.
        labelR(
            item.label,
            (trackLeft - theme.labelGap).rf,
            yMid,
            1f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
}
