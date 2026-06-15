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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderDotPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderDumbbell
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderPyramid
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderSlope

private fun RcCanvasScope.horizCartesian(
    theme: GraphTheme,
    title: String?,
    xTitle: String?,
    categories: List<String>,
    vLo: Float,
    vHi: Float,
    includeZero: Boolean,
): Cartesian {
    val yNice = niceAxis(vLo, vHi, includeZero = includeZero)
    val yLabels = yNice.ticks.map { NumberFormat.auto(yNice.step).format(it) }
    return computeCartesian(
        Orientation.Horizontal,
        theme,
        componentWidth(),
        componentHeight(),
        yNice,
        categories,
        yLabels,
        hasTitle = title != null,
        hasSubtitle = false,
        xAxisTitle = xTitle,
        yAxisTitle = null,
        legendRows = 0,
        xKind = XKind.Band,
    )
}

private fun RcCanvasScope.rankBgTitle(theme: GraphTheme, title: String?): Float {
    if (theme.background ushr 24 != 0)
        fillRectR(theme.background, 0f.rf, 0f.rf, componentWidth(), componentHeight())
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
        reserve += theme.titleSize + theme.labelGap * 2f
    }
    return reserve
}

/** Cleveland dot plot: one dot per (optionally sorted) category. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.dotPlot(
    items: List<Pair<String, Number>>,
    title: String? = null,
    xTitle: String? = null,
    sorted: Boolean = true,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val ordered = if (sorted) items.sortedBy { it.second.toFloat() } else items
    val labels = ordered.map { it.first }
    val values = FloatArray(ordered.size) { ordered[it].second.toFloat() }
    Canvas(modifier = modifier) {
        val lo = values.minOrNull() ?: 0f
        val hi = values.maxOrNull() ?: 1f
        val cart = horizCartesian(theme, title, xTitle, labels, lo, hi, includeZero = true)
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderDotPlot(cart, values, color ?: theme.seriesColor(0))
        drawAxes(cart, xTitle, null)
    }
}

/** A dumbbell row: a category whose value moved [from] → [to] (e.g. before/after). */
public class DumbbellRow(public val label: String, public val from: Number, public val to: Number)

/** Dumbbell chart: two values per category (e.g. before/after) joined by a bar. */
@Suppress("RestrictedApiAndroidX")
@JvmName("dumbbellChartRows")
public fun RcScope.dumbbellChart(
    items: List<DumbbellRow>,
    title: String? = null,
    xTitle: String? = null,
    colorA: Int? = null,
    colorB: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = items.map { it.label }
    val a = FloatArray(items.size) { items[it].from.toFloat() }
    val b = FloatArray(items.size) { items[it].to.toFloat() }
    Canvas(modifier = modifier) {
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (i in a.indices) {
            lo = minOf(lo, a[i], b[i])
            hi = maxOf(hi, a[i], b[i])
        }
        if (lo > hi) {
            lo = 0f
            hi = 1f
        }
        val cart = horizCartesian(theme, title, xTitle, labels, lo, hi, includeZero = false)
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderDumbbell(cart, a, b, colorA ?: theme.axisColor, colorB ?: theme.seriesColor(0))
        drawAxes(cart, xTitle, null)
    }
}

/** Dumbbell chart from (label, from, to) triples. */
@Deprecated(
    "Use the DumbbellRow overload — named fields beat positional Triple members",
    ReplaceWith(
        "dumbbellChart(items.map { DumbbellRow(it.first, it.second, it.third) }, title, xTitle, colorA, colorB, theme, modifier)"
    ),
)
@Suppress("RestrictedApiAndroidX")
public fun RcScope.dumbbellChart(
    items: List<Triple<String, Number, Number>>,
    title: String? = null,
    xTitle: String? = null,
    colorA: Int? = null,
    colorB: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    dumbbellChart(
        items.map { DumbbellRow(it.first, it.second, it.third) },
        title,
        xTitle,
        colorA,
        colorB,
        theme,
        modifier,
    )

/** OHLC candle for one period. */
public class Candle(
    public val label: String,
    public val open: Number,
    public val high: Number,
    public val low: Number,
    public val close: Number,
)

/** Candlestick (or OHLC-bar) financial chart. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.candlestickChart(
    candles: List<Candle>,
    title: String? = null,
    yTitle: String? = null,
    ohlc: Boolean = false,
    upColor: Int? = null,
    downColor: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { categories = candles.map { it.label } }
        yAxis {
            this.title = yTitle
            includeZero = false
        }
        candles(candles, upColor = upColor, downColor = downColor, ohlc = ohlc)
    }

/** A slope-chart row: a label whose value goes from [left] to [right] across two periods. */
public class SlopeRow(public val label: String, public val left: Number, public val right: Number)

/** Slope chart: change between two conditions/periods, one connecting line per item. */
@Suppress("RestrictedApiAndroidX")
@JvmName("slopeChartRows")
public fun RcScope.slopeChart(
    items: List<SlopeRow>,
    leftHeader: String,
    rightHeader: String,
    title: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = items.map { it.label }
    val left = FloatArray(items.size) { items[it].left.toFloat() }
    val right = FloatArray(items.size) { items[it].right.toFloat() }
    Canvas(modifier = modifier) {
        val topReserve = rankBgTitle(theme, title)
        renderSlope(labels, left, right, leftHeader, rightHeader, theme, topReserve)
    }
}

/** Slope chart from (label, left, right) triples. */
@Deprecated(
    "Use the SlopeRow overload — named fields beat positional Triple members",
    ReplaceWith(
        "slopeChart(items.map { SlopeRow(it.first, it.second, it.third) }, leftHeader, rightHeader, title, theme, modifier)"
    ),
)
@Suppress("RestrictedApiAndroidX")
public fun RcScope.slopeChart(
    items: List<Triple<String, Number, Number>>,
    leftHeader: String,
    rightHeader: String,
    title: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    slopeChart(
        items.map { SlopeRow(it.first, it.second, it.third) },
        leftHeader,
        rightHeader,
        title,
        theme,
        modifier,
    )

/** A population-pyramid band: a label (age band) with [left]/[right] group counts. */
public class PyramidRow(public val label: String, public val left: Number, public val right: Number)

/** Population pyramid: back-to-back horizontal bars for two groups across age bands. */
@Suppress("RestrictedApiAndroidX")
@JvmName("populationPyramidRows")
public fun RcScope.populationPyramid(
    bands: List<PyramidRow>,
    leftLabel: String,
    rightLabel: String,
    title: String? = null,
    leftColor: Int? = null,
    rightColor: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val names = bands.map { it.label }
    val l = FloatArray(bands.size) { bands[it].left.toFloat() }
    val r = FloatArray(bands.size) { bands[it].right.toFloat() }
    val lc = leftColor ?: theme.seriesColor(0)
    val rc = rightColor ?: theme.seriesColor(1)
    Canvas(modifier = modifier) {
        val topReserve = rankBgTitle(theme, title)
        val entries = listOf(LegendEntry(leftLabel, lc), LegendEntry(rightLabel, rc))
        val rows = legendRowCount(entries)
        val bottomReserve =
            theme.outerPad + rows * (theme.legendSize + theme.labelGap) + theme.outerPad
        renderPyramid(names, l, r, lc, rc, theme, topReserve, bottomReserve)
        drawLegend(entries, componentWidth(), componentHeight(), theme)
    }
}

/** Population pyramid from (label, left, right) triples. */
@Deprecated(
    "Use the PyramidRow overload — named fields beat positional Triple members",
    ReplaceWith(
        "populationPyramid(bands.map { PyramidRow(it.first, it.second, it.third) }, leftLabel, rightLabel, title, leftColor, rightColor, theme, modifier)"
    ),
)
@Suppress("RestrictedApiAndroidX")
public fun RcScope.populationPyramid(
    bands: List<Triple<String, Number, Number>>,
    leftLabel: String,
    rightLabel: String,
    title: String? = null,
    leftColor: Int? = null,
    rightColor: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    populationPyramid(
        bands.map { PyramidRow(it.first, it.second, it.third) },
        leftLabel,
        rightLabel,
        title,
        leftColor,
        rightColor,
        theme,
        modifier,
    )
