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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderForest

/**
 * Interval / forecast charts — band (ribbon / CI / error-band), fan, error-bar and forest. Band,
 * fan and error-bar are thin wrappers over the `chart2d { band(...) / errorBars(...) }` layers.
 */

/** Band / ribbon chart: a center line with a shaded confidence/uncertainty band around it. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.bandChart(
    categories: List<String>,
    center: List<Number>,
    lower: List<Number>,
    upper: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { this.categories = categories }
        yAxis {
            this.title = yTitle
            includeZero = false
        }
        band(lower, upper, color = color)
        line(center, color = color)
    }

/** Fan chart: a median forecast with nested widening uncertainty bands (widest first). */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.fanChart(
    categories: List<String>,
    median: List<Number>,
    bands: List<Pair<List<Number>, List<Number>>>,
    title: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { this.categories = categories }
        yAxis {
            this.title = yTitle
            includeZero = false
        }
        bands.forEachIndexed { i, (lo, hi) ->
            band(lo, hi, color = color, alpha = theme.fanBaseAlpha + i * theme.fanAlphaStep)
        }
        line(median, color = color)
    }

/** Error-bar chart: a point per category with symmetric ± error bars. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.errorBarChart(
    categories: List<String>,
    values: List<Number>,
    errors: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { this.categories = categories }
        yAxis {
            this.title = yTitle
            includeZero = false
        }
        errorBars(values, errors, color = color)
    }

/** A forest-plot row: an effect [estimate] with a confidence interval `[lo, hi]` and a [weight]. */
public class ForestRow(
    public val label: String,
    public val estimate: Number,
    public val lo: Number,
    public val hi: Number,
    public val weight: Number = 1,
)

/** Forest plot: per-row effect estimates + CIs against a reference line (horizontal). */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.forestPlot(
    rows: List<ForestRow>,
    refValue: Number = 0,
    title: String? = null,
    xTitle: String? = null,
    color: Int? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = rows.map { it.label }
    val est = FloatArray(rows.size) { rows[it].estimate.toFloat() }
    val los = FloatArray(rows.size) { rows[it].lo.toFloat() }
    val his = FloatArray(rows.size) { rows[it].hi.toFloat() }
    val weights = FloatArray(rows.size) { rows[it].weight.toFloat() }
    val ref = refValue.toFloat()
    Canvas(modifier = modifier) {
        var lo = ref
        var hi = ref
        for (x in los) if (x < lo) lo = x
        for (x in his) if (x > hi) hi = x
        val yNice = niceAxis(lo, hi, includeZero = false)
        val yLabels = yNice.ticks.map { NumberFormat.auto(yNice.step).format(it) }
        val cart =
            computeCartesian(
                Orientation.Horizontal,
                theme,
                componentWidth(),
                componentHeight(),
                yNice,
                labels,
                yLabels,
                hasTitle = title != null,
                hasSubtitle = false,
                xAxisTitle = xTitle,
                yAxisTitle = null,
                legendRows = 0,
                xKind = XKind.Band,
            )
        drawFrame(cart, componentWidth(), componentHeight(), title, null)
        drawGrid(cart, valueGrid = true)
        renderForest(cart, est, los, his, weights, ref, color ?: theme.seriesColor(0))
        drawAxes(cart, xTitle, null)
    }
}
