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
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.BulletRow
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderBullets
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderFunnel
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderWaterfall

/**
 * Financial / ranking charts — waterfall, funnel, bullet, Pareto. Canvas components like the rest.
 */
private fun RcCanvasScope.categoricalCartesian(
    theme: GraphTheme,
    title: String?,
    yTitle: String?,
    categories: List<String>,
    yMin: Float,
    yMax: Float,
    includeZero: Boolean,
    extraRightMargin: Float = 0f,
): Cartesian {
    val yNice = niceAxis(yMin, yMax, includeZero = includeZero)
    val yLabels = yNice.ticks.map { NumberFormat.auto(yNice.step).format(it) }
    return computeCartesian(
        Orientation.Vertical,
        theme,
        componentWidth(),
        componentHeight(),
        yNice,
        categories,
        yLabels,
        hasTitle = title != null,
        hasSubtitle = false,
        xAxisTitle = null,
        yAxisTitle = yTitle,
        legendRows = 0,
        xKind = XKind.Band,
        extraRightMargin = extraRightMargin,
    )
}

private fun RcCanvasScope.bgAndTitle(theme: GraphTheme, title: String?): Float {
    if (theme.background ushr 24 != 0) {
        fillRectR(theme.background, 0f.rf, 0f.rf, componentWidth(), componentHeight())
    }
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

/** Waterfall (bridge) chart: running cumulative with up/down steps and an optional total bar. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.waterfallChart(
    steps: List<Pair<String, Number>>,
    title: String? = null,
    yTitle: String? = null,
    showTotal: Boolean = true,
    totalLabel: String = "Total",
    positiveColor: Int? = null,
    negativeColor: Int? = null,
    totalColor: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = steps.map { it.first }
    val deltas = FloatArray(steps.size) { steps[it].second.toFloat() }
    Canvas(modifier = modifier) {
        var cum = 0f
        var lo = 0f
        var hi = 0f
        for (d in deltas) {
            cum += d
            if (cum < lo) lo = cum
            if (cum > hi) hi = cum
        }
        val cats = if (showTotal) labels + totalLabel else labels
        val cart = categoricalCartesian(theme, title, yTitle, cats, lo, hi, includeZero = true)
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderWaterfall(
            cart,
            deltas,
            showTotal,
            positiveColor ?: theme.seriesColor(2),
            negativeColor ?: theme.seriesColor(3),
            totalColor ?: theme.seriesColor(0),
        )
        drawAxes(cart, null, yTitle)
    }
}

/** Funnel chart: stacked centered trapezoids whose width is proportional to each stage's value. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.funnelChart(
    stages: List<Pair<String, Number>>,
    title: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = stages.map { it.first }
    val values = FloatArray(stages.size) { stages[it].second.toFloat() }
    Canvas(modifier = modifier) {
        val topReserve = bgAndTitle(theme, title)
        renderFunnel(labels, values, theme, topReserve)
    }
}

/**
 * A bullet-graph row: a measure [value] against a [target] and qualitative [ranges] (ascending).
 */
public class Bullet(
    public val label: String,
    public val value: Number,
    public val target: Number,
    public val max: Number,
    public val ranges: List<Number>,
)

/** Bullet chart: compact KPI rows comparing a measure to a target and qualitative ranges. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.bulletChart(
    items: List<Bullet>,
    title: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val rows =
        items.map {
            BulletRow(
                it.label,
                it.value.toFloat(),
                it.target.toFloat(),
                it.max.toFloat(),
                FloatArray(it.ranges.size) { j -> it.ranges[j].toFloat() },
            )
        }
    Canvas(modifier = modifier) {
        val topReserve = bgAndTitle(theme, title)
        renderBullets(rows, theme, topReserve)
    }
}

/**
 * Pareto chart: bars sorted descending plus a cumulative-% line on the secondary (right) axis.
 * Built on the layered builder — `bars(...)` + `line(..., secondary = true)` + `y2Axis { … }`.
 */
@Suppress("RestrictedApiAndroidX")
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
public fun RcScope.paretoChart(
    data: List<Pair<String, Number>>,
    title: String? = null,
    yTitle: String? = null,
    barColor: Int? = null,
    lineColor: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val sorted = data.sortedByDescending { it.second.toFloat() }
    val labels = sorted.map { it.first }
    val values = sorted.map { it.second.toFloat() }
    val total = values.sum().let { if (it <= 0f) 1f else it }
    var run = 0f
    val cumPct =
        values.map { v ->
            run += v
            run / total * 100f
        }
    chart2d(title, theme, modifier) {
        xAxis { categories = labels }
        yAxis { this.title = yTitle }
        bars(values, color = barColor ?: theme.seriesColor(0))
        line(cumPct, color = lineColor ?: theme.seriesColor(3), markers = true, secondary = true)
        y2Axis {
            ticks = listOf(0, 25, 50, 75, 100)
            format = NumberFormat { v -> "${v.toInt()}%" }
        }
    }
}
