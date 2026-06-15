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
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.smoothStep
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.BarLayout
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.BarStyle
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Cartesian
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Orientation
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.SeriesSpec
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.labelR

/**
 * The bar-chart family rendered onto a resolved [Cartesian] system. Both bar *geometry* and the
 * plot extent are reactive: bar values map through `LinearScale.mapR` and bar x-positions come from
 * the reactive [BandScale], so bars re-resolve (and the whole chart reflows to its canvas) on the
 * player.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderBars(
    cart: Cartesian,
    series: List<SeriesSpec>,
    layout: BarLayout,
    style: BarStyle,
    animate: Boolean,
    valueLabels: Boolean,
    negativeColor: Int?,
) {
    if (series.isEmpty()) return
    val grow =
        if (animate)
            smoothStep(continuousSeconds(), 0f.rf, cart.theme.animateDurationSec.rf).flush()
        else null
    when (layout) {
        BarLayout.Stacked -> renderStacked(cart, series, grow, percent = false)
        BarLayout.Percent -> renderStacked(cart, series, grow, percent = true)
        BarLayout.Grouped -> renderGrouped(cart, series, style, grow, valueLabels, negativeColor)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.renderGrouped(
    cart: Cartesian,
    series: List<SeriesSpec>,
    style: BarStyle,
    grow: RcFloat?,
    valueLabels: Boolean,
    negativeColor: Int?,
) {
    val theme = cart.theme
    val s = series.size
    val arrays = series.map { remoteFloatArray(it.values) }
    val base = cart.value.map(0f)
    val n = cart.categories.size
    val slot = (cart.band.bandWidth * (1f / s)).flush()
    val gap = (slot * cart.theme.barGroupGap).flush()

    for (si in 0 until s) {
        val spec = series[si]
        val arr = arrays[si]
        for (i in 0 until n) {
            if (i >= spec.values.size) continue
            val v = spec.values[i]
            val color =
                when {
                    negativeColor != null && v < 0f -> negativeColor
                    spec.color != null -> spec.color
                    s == 1 -> theme.seriesColor(i)
                    else -> theme.seriesColor(si)
                }
            val x0 = cart.band.bandStart(i) + slot * si.toFloat() + gap
            val x1 = x0 + slot - gap * 2f
            val edge = animatedEdge(cart, arr[i], base, grow)
            if (cart.orientation == Orientation.Vertical) {
                if (style == BarStyle.Lollipop) {
                    drawLollipopV(
                        (x0 + x1) * 0.5f,
                        base,
                        edge,
                        color,
                        theme.axisStroke + 1.5f,
                        theme.lollipopDotScale,
                    )
                } else if (v >= 0f) {
                    fillRectR(color, x0, edge, x1, base)
                } else {
                    fillRectR(color, x0, base, x1, edge)
                }
                if (valueLabels && s == 1) {
                    val ly = cart.value.map(v) + (if (v >= 0f) -theme.labelGap else theme.labelGap)
                    val pan = if (v >= 0f) 1f else -1f
                    labelR(
                        fmt(v),
                        (x0 + x1) * 0.5f,
                        ly,
                        0f,
                        pan,
                        theme.valueLabelColor,
                        theme.valueLabelSize,
                    )
                }
            } else {
                if (style == BarStyle.Lollipop) {
                    drawLollipopH(
                        (x0 + x1) * 0.5f,
                        base,
                        edge,
                        color,
                        theme.axisStroke + 1.5f,
                        theme.lollipopDotScale,
                    )
                } else if (v >= 0f) {
                    fillRectR(color, base, x0, edge, x1)
                } else {
                    fillRectR(color, edge, x0, base, x1)
                }
                if (valueLabels && s == 1) {
                    val lx = cart.value.map(v) + (if (v >= 0f) theme.labelGap else -theme.labelGap)
                    val pan = if (v >= 0f) -1f else 1f
                    labelR(
                        fmt(v),
                        lx,
                        (x0 + x1) * 0.5f,
                        pan,
                        0f,
                        theme.valueLabelColor,
                        theme.valueLabelSize,
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.renderStacked(
    cart: Cartesian,
    series: List<SeriesSpec>,
    grow: RcFloat?,
    percent: Boolean,
) {
    val theme = cart.theme
    val n = cart.categories.size
    val base = cart.value.map(0f)
    for (i in 0 until n) {
        val total =
            if (percent) {
                var t = 0f
                for (spec in series) if (i < spec.values.size) t += maxOf(spec.values[i], 0f)
                if (t == 0f) 1f else t
            } else {
                1f
            }
        var cumPos = 0f
        var cumNeg = 0f
        val a0 = cart.band.bandStart(i)
        val a1 = a0 + cart.band.bandWidth
        for (si in series.indices) {
            val spec = series[si]
            if (i >= spec.values.size) continue
            val raw = spec.values[i]
            val v = if (percent) raw / total else raw
            val color = spec.color ?: theme.seriesColor(si)
            val from: Float
            val to: Float
            if (v >= 0f) {
                from = cumPos
                to = cumPos + v
                cumPos = to
            } else {
                from = cumNeg
                to = cumNeg + v
                cumNeg = to
            }
            val pHi = growEdge(cart.value.map(maxOf(from, to)), base, grow)
            val pLo = growEdge(cart.value.map(minOf(from, to)), base, grow)
            if (cart.orientation == Orientation.Vertical) {
                fillRectR(color, a0, pHi, a1, pLo)
            } else {
                fillRectR(color, pLo, a0, pHi, a1)
            }
        }
    }
}

/** Reactive bar edge: map the array value, then animate from [base] by [grow] if present. */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.animatedEdge(
    cart: Cartesian,
    value: RcFloat,
    base: RcFloat,
    grow: RcFloat?,
): RcFloat {
    val edge = cart.value.mapR(value)
    return if (grow == null) edge else base + (edge - base) * grow
}

/** Animate a mapped pixel toward [base] by [grow]. */
@Suppress("RestrictedApiAndroidX")
private fun growEdge(pixel: RcFloat, base: RcFloat, grow: RcFloat?): RcFloat =
    if (grow == null) pixel else base + (pixel - base) * grow

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawLollipopV(
    cx: RcFloat,
    base: RcFloat,
    edge: RcFloat,
    color: Int,
    w: Float,
    dotScale: Float,
) {
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(w)
        strokeCap(RcStrokeCap.Round)
    }
    drawLine(cx, base, cx, edge)
    paint {
        color(color)
        style(RcPaintStyle.Fill)
    }
    drawCircle(cx, edge, (w * dotScale).rf)
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawLollipopH(
    cy: RcFloat,
    base: RcFloat,
    edge: RcFloat,
    color: Int,
    w: Float,
    dotScale: Float,
) {
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(w)
        strokeCap(RcStrokeCap.Round)
    }
    drawLine(base, cy, edge, cy)
    paint {
        color(color)
        style(RcPaintStyle.Fill)
    }
    drawCircle(edge, cy, (w * dotScale).rf)
}

/** Compact host value label: integer when whole, else one decimal. */
private fun fmt(v: Float): String {
    if (v == v.toLong().toFloat()) return v.toLong().toString()
    val s = kotlin.math.round(v * 10f) / 10f
    return if (s == s.toLong().toFloat()) s.toLong().toString() else s.toString()
}
