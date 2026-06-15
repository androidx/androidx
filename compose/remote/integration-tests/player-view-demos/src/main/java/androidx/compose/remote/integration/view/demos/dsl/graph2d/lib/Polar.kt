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

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.polarBackground
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderGauge
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderPie
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderPolarBars
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderRadar
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderRadialBars
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderRose
import kotlin.math.roundToInt

/**
 * Part-to-whole + radial charts on a polar engine (center + radius from the live canvas size). Like
 * every chart these are [RcScope] extensions that fill their Canvas; title is host-positioned
 * top-left and the shared bottom legend is reactive.
 */
private fun RcCanvasScope.drawTitleTop(theme: GraphTheme, title: String?): Float {
    val pad = theme.outerPad
    var reserve = pad
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
        reserve += theme.titleSize + theme.labelGap
    }
    return reserve
}

/** Pie chart of named slices. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.pieChart(
    slices: List<Pair<String, Number>>,
    title: String? = null,
    donut: Boolean = false,
    colors: IntArray? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val names = slices.map { it.first }
    val values = FloatArray(slices.size) { slices[it].second.toFloat() }
    requireFinite(values, "pieChart values")
    val sliceColors = colors ?: IntArray(values.size) { theme.seriesColor(it) }
    Canvas(modifier = modifier) {
        polarBackground(theme)
        val total = values.sum().let { if (it <= 0f) 1f else it }
        val topReserve = drawTitleTop(theme, title)
        val entries =
            names.mapIndexed { i, n ->
                LegendEntry(
                    "$n  ${(values[i] / total * 100f).roundToInt()}%",
                    sliceColors[i % sliceColors.size],
                )
            }
        val rows = legendRowCount(entries)
        val bottomReserve =
            theme.outerPad + rows * (theme.legendSize + theme.labelGap) + theme.outerPad
        renderPie(values, sliceColors, donut, theme, topReserve, bottomReserve)
        drawLegend(entries, componentWidth(), componentHeight(), theme)
    }
}

/** Donut chart (a pie with a hollow center). */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.donutChart(
    slices: List<Pair<String, Number>>,
    title: String? = null,
    colors: IntArray? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit = pieChart(slices, title, donut = true, colors = colors, theme = theme, modifier = modifier)

/** Radar (spider) chart comparing named series across ≥3 axes. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.radarChart(
    axes: List<String>,
    series: List<Pair<String, List<Number>>>,
    title: String? = null,
    maxValue: Number? = null,
    rings: Int = 4,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val data = series.map { p -> FloatArray(p.second.size) { p.second[it].toFloat() } }
    Canvas(modifier = modifier) {
        polarBackground(theme)
        var dataMax = maxValue?.toFloat() ?: 0f
        if (maxValue == null) for (d in data) for (v in d) if (v > dataMax) dataMax = v
        val maxV =
            if (maxValue != null) maxValue.toFloat() else niceAxis(0f, dataMax, rings + 1).max
        val colors = IntArray(data.size) { theme.seriesColor(it) }
        val topReserve = drawTitleTop(theme, title)
        val entries = series.mapIndexed { i, p -> LegendEntry(p.first, colors[i]) }
        val rows = legendRowCount(entries)
        val bottomReserve =
            theme.outerPad + rows * (theme.legendSize + theme.labelGap) + theme.outerPad
        renderRadar(axes, data, colors, maxV, rings, theme, topReserve, bottomReserve)
        drawLegend(entries, componentWidth(), componentHeight(), theme)
    }
}

/** Radial gauge / dial: one value against a range, shown as a 270° arc with center readout. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.gaugeChart(
    value: Number,
    min: Number = 0,
    max: Number = 100,
    title: String? = null,
    label: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val v = value.toFloat()
    val lo = min.toFloat()
    val hi = max.toFloat()
    Canvas(modifier = modifier) {
        polarBackground(theme)
        val topReserve = drawTitleTop(theme, title)
        val frac = if (hi == lo) 0f else (v - lo) / (hi - lo)
        renderGauge(
            v.roundToInt().toString(),
            label,
            frac,
            color ?: theme.seriesColor(0),
            lo.roundToInt().toString(),
            hi.roundToInt().toString(),
            theme,
            topReserve,
            theme.outerPad,
        )
    }
}

private fun RcCanvasScope.polarValues(
    items: List<Pair<String, Number>>,
    maxValue: Number?,
    theme: GraphTheme,
): Triple<List<String>, FloatArray, Float> {
    val labels = items.map { it.first }
    val values = FloatArray(items.size) { items[it].second.toFloat() }
    var dataMax = maxValue?.toFloat() ?: 0f
    if (maxValue == null) for (x in values) if (x > dataMax) dataMax = x
    return Triple(labels, values, if (dataMax <= 0f) 1f else dataMax)
}

/** Radial (circular) bar chart: concentric "racetrack" rings, one per category. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.radialBarChart(
    items: List<Pair<String, Number>>,
    title: String? = null,
    maxValue: Number? = null,
    colors: IntArray? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Canvas(modifier = modifier) {
        polarBackground(theme)
        val (labels, values, maxV) = polarValues(items, maxValue, theme)
        @Suppress("NAME_SHADOWING")
        val colors = colors ?: IntArray(values.size) { theme.seriesColor(it) }
        val topReserve = drawTitleTop(theme, title)
        renderRadialBars(labels, values, colors, maxV, theme, topReserve, theme.outerPad)
    }
}

/** Nightingale rose / coxcomb: equal-angle sectors with area-proportional radius. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.roseChart(
    items: List<Pair<String, Number>>,
    title: String? = null,
    maxValue: Number? = null,
    rings: Int = 4,
    colors: IntArray? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Canvas(modifier = modifier) {
        polarBackground(theme)
        val (labels, values, maxV) = polarValues(items, maxValue, theme)
        @Suppress("NAME_SHADOWING")
        val colors = colors ?: IntArray(values.size) { theme.seriesColor(it) }
        val topReserve = drawTitleTop(theme, title)
        renderRose(labels, values, colors, maxV, rings, theme, topReserve, theme.outerPad)
    }
}

/** Polar bar chart: radial bars (with angular gaps) at radius ∝ value, over a circular grid. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.polarBarChart(
    items: List<Pair<String, Number>>,
    title: String? = null,
    maxValue: Number? = null,
    rings: Int = 4,
    colors: IntArray? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Canvas(modifier = modifier) {
        polarBackground(theme)
        val (labels, values, maxV) = polarValues(items, maxValue, theme)
        @Suppress("NAME_SHADOWING")
        val colors = colors ?: IntArray(values.size) { theme.seriesColor(it) }
        val topReserve = drawTitleTop(theme, title)
        renderPolarBars(labels, values, colors, maxV, rings, theme, topReserve, theme.outerPad)
    }
}
