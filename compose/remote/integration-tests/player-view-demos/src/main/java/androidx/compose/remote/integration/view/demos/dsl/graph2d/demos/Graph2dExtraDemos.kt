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

package androidx.compose.remote.integration.view.demos.dsl.graph2d.demos

import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Candle
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.DumbbellRow
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.PyramidRow
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.SlopeRow
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.candlestickChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.dotPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.dumbbellChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.heatmap
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.populationPyramid
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.regressionPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.rocCurve
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.slopeChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.waffleChart

/**
 * Expansion demos — charts filling gaps in the catalog (heatmap, waffle, dot, dumbbell, slope,
 * population pyramid, candlestick, regression, ROC).
 */

/** Matrix heatmap with cell annotations. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dHeatmap(): ByteArray {
    val cols = listOf("6a", "9a", "12p", "3p", "6p", "9p", "12a")
    val rows = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    val data =
        listOf(
            listOf(2, 8, 14, 11, 9, 5, 1),
            listOf(3, 9, 16, 13, 10, 6, 2),
            listOf(4, 10, 18, 15, 12, 7, 2),
            listOf(3, 11, 17, 14, 13, 9, 3),
            listOf(5, 13, 20, 18, 16, 12, 6),
        )
    return graph2dDoc(820, 520, "Activity Heatmap") {
        heatmap(
            rows,
            cols,
            data,
            title = "Sessions by Day × Hour",
            ramp = Palette.Blues,
            showValues = true,
        )
    }
}

/** Waffle / unit chart of proportions. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dWaffle(): ByteArray =
    graph2dDoc(640, 680, "Energy Mix") {
        waffleChart(
            parts = listOf("Solar" to 38, "Wind" to 27, "Hydro" to 18, "Gas" to 12, "Other" to 5),
            title = "Energy Mix",
        )
    }

/** Cleveland dot plot (sorted). */
@Suppress("RestrictedApiAndroidX")
public fun graph2dDotPlot(): ByteArray =
    graph2dDoc(760, 520, "Revenue by Region") {
        dotPlot(
            items =
                listOf("North" to 42, "South" to 31, "East" to 55, "West" to 38, "Central" to 27),
            title = "Revenue by Region",
            xTitle = "Revenue (\$M)",
        )
    }

/** Dumbbell chart: before vs after. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dDumbbell(): ByteArray =
    graph2dDoc(760, 520, "Before vs After") {
        dumbbellChart(
            items =
                listOf(
                    DumbbellRow("Alpha", 34, 58),
                    DumbbellRow("Bravo", 41, 49),
                    DumbbellRow("Charlie", 22, 64),
                    DumbbellRow("Delta", 55, 71),
                    DumbbellRow("Echo", 38, 44),
                ),
            title = "Score: Before vs After",
            xTitle = "Score",
        )
    }

/** Slope chart between two periods. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dSlope(): ByteArray =
    graph2dDoc(680, 560, "2023 → 2024") {
        slopeChart(
            items =
                listOf(
                    SlopeRow("Product A", 42, 58),
                    SlopeRow("Product B", 50, 46),
                    SlopeRow("Product C", 28, 40),
                    SlopeRow("Product D", 61, 55),
                    SlopeRow("Product E", 35, 52),
                ),
            leftHeader = "2023",
            rightHeader = "2024",
            title = "Share Shift, 2023 → 2024",
        )
    }

/** Population pyramid. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dPyramid(): ByteArray =
    graph2dDoc(720, 600, "Population") {
        populationPyramid(
            bands =
                listOf(
                    PyramidRow("0-9", 62, 60),
                    PyramidRow("10-19", 58, 56),
                    PyramidRow("20-29", 71, 69),
                    PyramidRow("30-39", 66, 67),
                    PyramidRow("40-49", 54, 57),
                    PyramidRow("50-59", 44, 49),
                    PyramidRow("60-69", 33, 40),
                    PyramidRow("70+", 21, 32),
                ),
            leftLabel = "Male",
            rightLabel = "Female",
            title = "Population by Age",
        )
    }

/** Candlestick financial chart. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dCandlestick(): ByteArray {
    var s = 99
    fun rnd(): Float {
        s = (s * 1103515245 + 12345) and 0x7fffffff
        return (s % 1000) / 1000f
    }
    var price = 100f
    val candles =
        (1..14).map { d ->
            val open = price
            val close = open + (rnd() - 0.45f) * 10f
            val high = maxOf(open, close) + rnd() * 4f
            val low = minOf(open, close) - rnd() * 4f
            price = close
            Candle("D$d", open, high, low, close)
        }
    return graph2dDoc(860, 520, "Price") {
        candlestickChart(candles, title = "Daily Price", yTitle = "\$")
    }
}

/** Regression plot: scatter + OLS fit. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dRegression(): ByteArray {
    var s = 7
    fun rnd(): Float {
        s = (s * 1103515245 + 12345) and 0x7fffffff
        return (s % 100000) / 100000f
    }
    val pts =
        (0 until 50).map {
            val x = 10f + rnd() * 80f
            val y = 15f + 0.6f * x + (rnd() - 0.5f) * 30f
            Pair<Number, Number>(x, y)
        }
    return graph2dDoc(760, 540, "Correlation") {
        regressionPlot(
            pts,
            title = "Spend vs Revenue",
            xTitle = "Spend (k)",
            yTitle = "Revenue (k)",
        )
    }
}

/** ROC curve with the chance diagonal. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dRoc(): ByteArray =
    graph2dDoc(620, 600, "ROC") {
        rocCurve(
            points =
                listOf(
                    0.0 to 0.0,
                    0.03 to 0.38,
                    0.08 to 0.6,
                    0.15 to 0.74,
                    0.28 to 0.86,
                    0.45 to 0.93,
                    0.65 to 0.97,
                    0.85 to 0.99,
                    1.0 to 1.0,
                ),
            title = "Classifier ROC (AUC ≈ 0.9)",
        )
    }
