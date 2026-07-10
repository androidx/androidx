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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderBoxPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderDensity
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderEcdf
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderHistogram
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderStrip
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderViolin

/**
 * The distribution family — canvas components like every other chart. The statistical transforms
 * run host-side ([Stats]); a numeric x axis is used for histogram/density/ECDF and the categorical
 * band for box/violin/strip. All fill their canvas reactively.
 */
private fun List<Number>.toFloats(): FloatArray = FloatArray(size) { this[it].toFloat() }

private fun RcCanvasScope.buildNumericXY(
    theme: GraphTheme,
    title: String?,
    xTitle: String?,
    yTitle: String?,
    xMin: Float,
    xMax: Float,
    yMin: Float,
    yMax: Float,
): Cartesian {
    val xNice = niceAxis(xMin, xMax, includeZero = false)
    val yNice = niceAxis(yMin, yMax, includeZero = true)
    val xLabels = xNice.ticks.map { NumberFormat.auto(xNice.step).format(it) }
    val yLabels = yNice.ticks.map { NumberFormat.auto(yNice.step).format(it) }
    return computeCartesian(
        Orientation.Vertical,
        theme,
        componentWidth(),
        componentHeight(),
        yNice,
        emptyList(),
        yLabels,
        hasTitle = title != null,
        hasSubtitle = false,
        xAxisTitle = xTitle,
        yAxisTitle = yTitle,
        legendRows = 0,
        xKind = XKind.Point,
        xAxisNumeric = xNice,
        xTickLabels = xLabels,
    )
}

private fun RcCanvasScope.buildCategorical(
    theme: GraphTheme,
    title: String?,
    xTitle: String?,
    yTitle: String?,
    categories: List<String>,
    yMin: Float,
    yMax: Float,
): Cartesian {
    val yNice = niceAxis(yMin, yMax, includeZero = false)
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
        xAxisTitle = xTitle,
        yAxisTitle = yTitle,
        legendRows = 0,
        xKind = XKind.Band,
    )
}

// ---------------------------------------------------------------------------
// Numeric-x distribution charts.
// ---------------------------------------------------------------------------

/** Histogram of a numeric sample (bins via Sturges' rule unless [bins] is set). */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.histogram(
    data: List<Number>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = "Count",
    bins: Int = 0,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val arr = data.toFloats()
    Canvas(modifier = modifier) {
        val hist = Stats.histogram(arr, bins)
        val cart =
            buildNumericXY(
                theme,
                title,
                xTitle,
                yTitle,
                hist.edges.first(),
                hist.edges.last(),
                0f,
                hist.maxCount.toFloat(),
            )
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderHistogram(cart, hist, color ?: theme.seriesColor(0))
        drawAxes(cart, xTitle, yTitle)
    }
}

/** Kernel density estimate of a numeric sample. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.densityPlot(
    data: List<Number>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = "Density",
    fill: Boolean = true,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val arr = data.toFloats()
    Canvas(modifier = modifier) {
        val curve = Stats.kde(arr, gridN = 96)
        var maxD = 0f
        for (d in curve.ys) if (d > maxD) maxD = d
        val cart =
            buildNumericXY(
                theme,
                title,
                xTitle,
                yTitle,
                curve.xs.first(),
                curve.xs.last(),
                0f,
                maxD,
            )
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderDensity(cart, curve, color ?: theme.seriesColor(2), fill)
        drawAxes(cart, xTitle, yTitle)
    }
}

/** Empirical CDF (step) of a numeric sample. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.ecdfPlot(
    data: List<Number>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = "Cumulative",
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val arr = data.toFloats()
    Canvas(modifier = modifier) {
        val curve = Stats.ecdf(arr)
        val cart =
            buildNumericXY(theme, title, xTitle, yTitle, curve.xs.first(), curve.xs.last(), 0f, 1f)
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderEcdf(cart, curve, color ?: theme.seriesColor(3))
        drawAxes(cart, xTitle, yTitle)
    }
}

// ---------------------------------------------------------------------------
// Categorical distribution charts (one distribution per group).
// ---------------------------------------------------------------------------

private fun pooledRange(groups: List<FloatArray>, pad: Float = 0.05f): Pair<Float, Float> {
    var lo = Float.MAX_VALUE
    var hi = -Float.MAX_VALUE
    for (g in groups) for (v in g) {
        if (v < lo) lo = v
        if (v > hi) hi = v
    }
    if (lo > hi) {
        lo = 0f
        hi = 1f
    }
    val span = (hi - lo).let { if (it == 0f) 1f else it }
    return (lo - span * pad) to (hi + span * pad)
}

/** Box-and-whisker plot, one box per named group. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.boxPlot(
    groups: List<Pair<String, List<Number>>>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val names = groups.map { it.first }
    val arrays = groups.map { it.second.toFloats() }
    Canvas(modifier = modifier) {
        val (lo, hi) = pooledRange(arrays)
        val cart = buildCategorical(theme, title, xTitle, yTitle, names, lo, hi)
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderBoxPlot(cart, arrays.map { Stats.box(it) })
        drawAxes(cart, xTitle, yTitle)
    }
}

/** Violin plot (mirrored KDE), one violin per named group. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.violinPlot(
    groups: List<Pair<String, List<Number>>>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val names = groups.map { it.first }
    val arrays = groups.map { it.second.toFloats() }
    Canvas(modifier = modifier) {
        val curves = arrays.map { Stats.kde(it, gridN = 64) }
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (c in curves) {
            if (c.xs.first() < lo) lo = c.xs.first()
            if (c.xs.last() > hi) hi = c.xs.last()
        }
        val cart = buildCategorical(theme, title, xTitle, yTitle, names, lo, hi)
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderViolin(cart, curves)
        drawAxes(cart, xTitle, yTitle)
    }
}

/** Strip plot: every observation as a jittered dot, one column per named group. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.stripPlot(
    groups: List<Pair<String, List<Number>>>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val names = groups.map { it.first }
    val arrays = groups.map { it.second.toFloats() }
    Canvas(modifier = modifier) {
        val (lo, hi) = pooledRange(arrays)
        val cart = buildCategorical(theme, title, xTitle, yTitle, names, lo, hi)
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderStrip(cart, arrays)
        drawAxes(cart, xTitle, yTitle)
    }
}
