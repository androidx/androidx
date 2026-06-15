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
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderScatter
import kotlin.math.sqrt

/**
 * Relationship charts (scatter / bubble / regression / connected scatter and friends). These are
 * thin wrappers over the `chart2d { points(...) / xyLine(...) / fitLine(...) }` numeric-x layers —
 * use the builder directly to combine or restyle them.
 */

/** Scatter plot of (x, y) points. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.scatterPlot(
    points: List<Pair<Number, Number>>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { this.title = xTitle }
        yAxis { this.title = yTitle }
        points(points, color = color)
    }

/** One bubble of a [bubbleChart]: a point at ([x], [y]) whose area is proportional to [size]. */
public class BubblePoint(public val x: Number, public val y: Number, public val size: Number)

/** Bubble chart: scatter where a third value sets each point's area. */
@Suppress("RestrictedApiAndroidX")
@JvmName("bubbleChartPoints")
public fun RcScope.bubbleChart(
    points: List<BubblePoint>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { this.title = xTitle }
        yAxis { this.title = yTitle }
        points(
            points.map { it.x to it.y },
            color = color ?: theme.seriesColor(1),
            sizes = points.map { it.size },
        )
    }

/** Bubble chart from (x, y, size) triples. */
@Deprecated(
    "Use the BubblePoint overload — named fields beat positional Triple members",
    ReplaceWith(
        "bubbleChart(points.map { BubblePoint(it.first, it.second, it.third) }, title, xTitle, yTitle, color, theme, modifier)"
    ),
)
@Suppress("RestrictedApiAndroidX")
public fun RcScope.bubbleChart(
    points: List<Triple<Number, Number, Number>>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    bubbleChart(
        points.map { BubblePoint(it.first, it.second, it.third) },
        title,
        xTitle,
        yTitle,
        color,
        theme,
        modifier,
    )

/** Regression plot: a scatter with an ordinary-least-squares fitted line. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.regressionPlot(
    points: List<Pair<Number, Number>>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    lineColor: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val xs = FloatArray(points.size) { points[it].first.toFloat() }
    val ys = FloatArray(points.size) { points[it].second.toFloat() }
    // Ordinary least squares (host-side).
    val mx = if (xs.isEmpty()) 0f else xs.average().toFloat()
    val my = if (ys.isEmpty()) 0f else ys.average().toFloat()
    var sxy = 0f
    var sxx = 0f
    for (i in xs.indices) {
        sxy += (xs[i] - mx) * (ys[i] - my)
        sxx += (xs[i] - mx) * (xs[i] - mx)
    }
    val slope = if (sxx == 0f) 0f else sxy / sxx
    val intercept = my - slope * mx
    chart2d(title, theme, modifier) {
        xAxis { this.title = xTitle }
        yAxis { this.title = yTitle }
        points(points, color = color)
        fitLine(slope, intercept, color = lineColor)
    }
}

/**
 * ROC curve: true-positive vs false-positive rate over the unit square, with the chance diagonal.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.rocCurve(
    points: List<Pair<Number, Number>>,
    title: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis {
            this.title = "False positive rate"
            min = 0f
            max = 1f
            includeZero = true
        }
        yAxis {
            this.title = "True positive rate"
            min = 0f
            max = 1f
            includeZero = true
        }
        xyLine(listOf(0 to 0, 1 to 1), color = theme.zeroLineColor, width = theme.gridStroke + 1f)
        xyLine(points, color = color ?: theme.seriesColor(0), width = theme.lineStroke)
    }

/** Function plot: samples `y = f(x)` over `[xMin, xMax]` and draws it as a smooth-ish line. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.functionPlot(
    xMin: Double,
    xMax: Double,
    samples: Int = 120,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
    f: (Double) -> Double,
) {
    require(samples >= 2) { "graph2d: functionPlot needs samples >= 2 (was $samples)" }
    val pts =
        List(samples) {
            val x = xMin + (xMax - xMin) * it / (samples - 1)
            x.toFloat() to f(x).toFloat()
        }
    chart2d(title, theme, modifier) {
        xAxis { this.title = xTitle }
        yAxis { this.title = yTitle }
        xyLine(pts, color = color, width = theme.lineStroke)
    }
}

/** Quadrant chart: a scatter split into four zones by dividers, with corner labels. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.quadrantChart(
    points: List<Pair<Number, Number>>,
    xDivider: Number,
    yDivider: Number,
    cornerLabels: List<String> = emptyList(),
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    require(cornerLabels.isEmpty() || cornerLabels.size == 4) {
        "graph2d: quadrantChart cornerLabels needs exactly 4 entries (was ${cornerLabels.size})"
    }
    val xs = FloatArray(points.size) { points[it].first.toFloat() }
    val ys = FloatArray(points.size) { points[it].second.toFloat() }
    Canvas(modifier = modifier) {
        val xNice = niceAxis(xs.minOrNull() ?: 0f, xs.maxOrNull() ?: 1f, includeZero = false)
        val yNice = niceAxis(ys.minOrNull() ?: 0f, ys.maxOrNull() ?: 1f, includeZero = false)
        val xLabels = xNice.ticks.map { NumberFormat.auto(xNice.step).format(it) }
        val yLabels = yNice.ticks.map { NumberFormat.auto(yNice.step).format(it) }
        val cart =
            computeCartesian(
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
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        val xd = cart.xScale!!.map(xDivider.toFloat())
        val yd = cart.value.map(yDivider.toFloat())
        strokeLineR(
            theme.zeroLineColor,
            theme.axisStroke + 0.5f,
            xd,
            cart.plotTop.rf,
            xd,
            cart.plotBottom,
        )
        strokeLineR(
            theme.zeroLineColor,
            theme.axisStroke + 0.5f,
            cart.plotLeft.rf,
            yd,
            cart.plotRight,
            yd,
        )
        renderScatter(
            cart,
            xs,
            ys,
            sizes = null,
            color = color ?: theme.seriesColor(0),
            connect = false,
        )
        if (cornerLabels.size == 4) {
            val g = theme.labelGap + 2f
            labelR(
                cornerLabels[0],
                cart.plotRight - g,
                cart.plotTop.rf + (g + theme.labelSize),
                1f,
                0f,
                theme.subtitleColor,
                theme.labelSize,
            )
            labelR(
                cornerLabels[1],
                cart.plotLeft.rf + g,
                cart.plotTop.rf + (g + theme.labelSize),
                -1f,
                0f,
                theme.subtitleColor,
                theme.labelSize,
            )
            labelR(
                cornerLabels[2],
                cart.plotLeft.rf + g,
                cart.plotBottom - g,
                -1f,
                0f,
                theme.subtitleColor,
                theme.labelSize,
            )
            labelR(
                cornerLabels[3],
                cart.plotRight - g,
                cart.plotBottom - g,
                1f,
                0f,
                theme.subtitleColor,
                theme.labelSize,
            )
        }
        drawAxes(cart, xTitle, yTitle)
    }
}

/** Normal Q-Q plot: sample quantiles vs theoretical normal quantiles, with a reference line. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.qqPlot(
    sample: List<Number>,
    title: String? = null,
    color: Int? = null,
    lineColor: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val s = FloatArray(sample.size) { sample[it].toFloat() }.also { it.sort() }
    val n = s.size.coerceAtLeast(1)
    val theo = FloatArray(s.size) { Stats.invNormCdf((it + 0.5f) / n) }
    val mean = if (s.isEmpty()) 0f else s.average().toFloat()
    var sd = 0f
    if (s.size > 1) {
        var acc = 0f
        for (v in s) acc += (v - mean) * (v - mean)
        sd = sqrt(acc / (s.size - 1))
    }
    chart2d(title, theme, modifier) {
        xAxis { this.title = "Theoretical quantiles" }
        yAxis { this.title = "Sample quantiles" }
        fitLine(sd, mean, color = lineColor)
        points(List(s.size) { theo[it] to s[it] }, color = color)
    }
}

/**
 * Connected scatter: points joined by a line in their given order (e.g. a trajectory over time).
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.connectedScatter(
    points: List<Pair<Number, Number>>,
    title: String? = null,
    xTitle: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { this.title = xTitle }
        yAxis { this.title = yTitle }
        points(points, color = color ?: theme.seriesColor(2), connect = true)
    }
