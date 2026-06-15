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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderHeatmap
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderWaffle

private fun RcCanvasScope.gridBgTitle(theme: GraphTheme, title: String?): Float {
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

/**
 * Matrix heatmap: a grid of cells colored by value (sequential [ramp]). Rows and columns are
 * labeled; set [showValues] to annotate each cell. `values[row][col]`.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.heatmap(
    rowLabels: List<String>,
    colLabels: List<String>,
    values: List<List<Number>>,
    title: String? = null,
    ramp: IntArray = Palette.Blues,
    showValues: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val grid =
        Array(values.size) { r -> FloatArray(values[r].size) { c -> values[r][c].toFloat() } }
    Canvas(modifier = modifier) {
        val topReserve = gridBgTitle(theme, title)
        renderHeatmap(rowLabels, colLabels, grid, ramp, showValues, theme, topReserve)
    }
}

/**
 * Waffle / unit chart: a grid of squares colored to show each category's share of the whole.
 * Defaults to a 10×10 (=100-cell) grid so each cell ≈ 1%.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.waffleChart(
    parts: List<Pair<String, Number>>,
    title: String? = null,
    rows: Int = 10,
    cols: Int = 10,
    colors: IntArray? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val names = parts.map { it.first }
    val values = FloatArray(parts.size) { parts[it].second.toFloat() }
    requireFinite(values, "waffleChart values")
    val partColors = colors ?: IntArray(values.size) { theme.seriesColor(it) }
    Canvas(modifier = modifier) {
        val total = values.sum().let { if (it <= 0f) 1f else it }
        val topReserve = gridBgTitle(theme, title)
        val entries =
            names.mapIndexed { i, n ->
                LegendEntry(
                    "$n  ${kotlin.math.round(values[i] / total * 100f).toInt()}%",
                    partColors[i % partColors.size],
                )
            }
        val rowsLegend = legendRowCount(entries)
        val bottomReserve =
            theme.outerPad + rowsLegend * (theme.legendSize + theme.labelGap) + theme.outerPad
        renderWaffle(values, partColors, rows, cols, theme, topReserve, bottomReserve)
        drawLegend(entries, componentWidth(), componentHeight(), theme)
    }
}

/**
 * Confusion matrix: a square [classes]×[classes] heatmap of `matrix[actual][predicted]` counts,
 * annotated. A thin specialization of [heatmap].
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.confusionMatrix(
    classes: List<String>,
    matrix: List<List<Number>>,
    title: String? = null,
    ramp: IntArray = Palette.Blues,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit = heatmap(classes, classes, matrix, title, ramp, showValues = true, theme, modifier)
