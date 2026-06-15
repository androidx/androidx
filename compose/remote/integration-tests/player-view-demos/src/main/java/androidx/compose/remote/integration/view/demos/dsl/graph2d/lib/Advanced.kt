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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderGantt
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderLikert
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderRidgeline

private fun RcCanvasScope.advBgTitle(theme: GraphTheme, title: String?): Float {
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

/** A Gantt row: a task bar spanning [start] → [end] on the time axis. */
public class GanttTask(public val label: String, public val start: Number, public val end: Number)

/** Gantt chart: a horizontal time bar per task (start → end). */
@Suppress("RestrictedApiAndroidX")
@JvmName("ganttChartTasks")
public fun RcScope.ganttChart(
    tasks: List<GanttTask>,
    title: String? = null,
    xTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = tasks.map { it.label }
    val starts = FloatArray(tasks.size) { tasks[it].start.toFloat() }
    val ends = FloatArray(tasks.size) { tasks[it].end.toFloat() }
    val colors = IntArray(tasks.size) { theme.seriesColor(it) }
    Canvas(modifier = modifier) {
        var lo = starts.minOrNull() ?: 0f
        var hi = ends.maxOrNull() ?: 1f
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
        renderGantt(cart, starts, ends, colors)
        drawAxes(cart, xTitle, null)
    }
}

/** Gantt chart from (label, start, end) triples. */
@Deprecated(
    "Use the GanttTask overload — named fields beat positional Triple members",
    ReplaceWith(
        "ganttChart(tasks.map { GanttTask(it.first, it.second, it.third) }, title, xTitle, theme, modifier)"
    ),
)
@Suppress("RestrictedApiAndroidX")
public fun RcScope.ganttChart(
    tasks: List<Triple<String, Number, Number>>,
    title: String? = null,
    xTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    ganttChart(
        tasks.map { GanttTask(it.first, it.second, it.third) },
        title,
        xTitle,
        theme,
        modifier,
    )

/** Ridgeline / joy plot: stacked, overlapping KDEs — one per group — over a shared value axis. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.ridgelinePlot(
    groups: List<Pair<String, List<Number>>>,
    title: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = groups.map { it.first }
    val arrays = groups.map { p -> FloatArray(p.second.size) { p.second[it].toFloat() } }
    val curves = arrays.map { Stats.kde(it, gridN = 72) }
    var xLo = Float.MAX_VALUE
    var xHi = -Float.MAX_VALUE
    for (c in curves) {
        if (c.xs.first() < xLo) xLo = c.xs.first()
        if (c.xs.last() > xHi) xHi = c.xs.last()
    }
    Canvas(modifier = modifier) {
        val topReserve = advBgTitle(theme, title)
        renderRidgeline(labels, curves, xLo, xHi, theme, topReserve)
    }
}

/**
 * Likert chart: diverging stacked survey responses. [levels] names label the legend (low → high).
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.likertChart(
    questions: List<Pair<String, List<Number>>>,
    levels: List<String>,
    title: String? = null,
    palette: IntArray = Palette.Diverging,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val labels = questions.map { it.first }
    val nLevels = levels.size
    val data =
        Array(questions.size) { q ->
            FloatArray(nLevels) { l -> questions[q].second.getOrElse(l) { 0 }.toFloat() }
        }
    val colors =
        IntArray(nLevels) {
            Palette.sample(palette, if (nLevels == 1) 0f else it.toFloat() / (nLevels - 1))
        }
    Canvas(modifier = modifier) {
        val topReserve = advBgTitle(theme, title)
        val entries = levels.mapIndexed { i, n -> LegendEntry(n, colors[i]) }
        val rows = legendRowCount(entries)
        val bottomReserve =
            theme.outerPad + rows * (theme.legendSize + theme.labelGap) + theme.outerPad
        renderLikert(labels, data, colors, theme, topReserve, bottomReserve)
        drawLegend(entries, componentWidth(), componentHeight(), theme)
    }
}
