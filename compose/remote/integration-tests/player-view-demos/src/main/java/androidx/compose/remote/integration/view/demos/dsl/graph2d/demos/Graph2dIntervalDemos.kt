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

import android.annotation.SuppressLint
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.ForestRow
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.bandChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.errorBarChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fanChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.forestPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc

/** Phase 7 demos — interval / forecast charts (band, fan, error-bar, forest). */
private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug")

/** Ribbon / band chart: a line with a shaded confidence band. */
@Suppress("RestrictedApiAndroidX")
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
public fun graph2dBand(): ByteArray {
    val center = listOf(50, 53, 49, 55, 60, 58, 63, 66)
    val lower = center.mapIndexed { i, v -> v.toDouble() - (4 + i) }
    val upper = center.mapIndexed { i, v -> v.toDouble() + (4 + i) }
    return graph2dDoc(820, 520, "Forecast with CI") {
        bandChart(
            months,
            center,
            lower,
            upper,
            title = "Forecast with Confidence Band",
            yTitle = "Value",
        )
    }
}

/** Fan chart: a median forecast with nested 50 / 80 / 95% bands that widen over the horizon. */
@Suppress("RestrictedApiAndroidX")
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
fun graph2dFan(): ByteArray {
    val median = listOf(60, 62, 63, 65, 67, 70, 72, 75)
    fun band(w: Double) =
        median.mapIndexed { i, v -> v.toDouble() - w * (1 + i * 0.6) } to
            median.mapIndexed { i, v -> v.toDouble() + w * (1 + i * 0.6) }
    return graph2dDoc(820, 520, "Forecast Fan") {
        fanChart(
            months,
            median,
            bands = listOf(band(5.0), band(3.0), band(1.5)), // widest first
            title = "Demand Forecast",
            yTitle = "Units (k)",
        )
    }
}

/** Error-bar chart: measurements with ± uncertainty. */
@Suppress("RestrictedApiAndroidX")
fun graph2dErrorBar(): ByteArray =
    graph2dDoc(800, 500, "Measurements") {
        errorBarChart(
            categories = listOf("A", "B", "C", "D", "E", "F"),
            values = listOf(23, 31, 27, 35, 29, 33),
            errors = listOf(3, 5, 2, 4, 6, 3),
            title = "Mean Response ± SE",
            yTitle = "Response",
        )
    }

/** Forest plot: meta-analysis effect estimates with confidence intervals vs a reference. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dForest(): ByteArray =
    graph2dDoc(820, 520, "Meta-analysis") {
        forestPlot(
            rows =
                listOf(
                    ForestRow("Study A", 0.30, 0.10, 0.50, 20),
                    ForestRow("Study B", -0.10, -0.40, 0.20, 15),
                    ForestRow("Study C", 0.50, 0.20, 0.80, 10),
                    ForestRow("Study D", 0.20, 0.05, 0.35, 30),
                    ForestRow("Study E", 0.00, -0.20, 0.20, 8),
                    ForestRow("Pooled", 0.22, 0.12, 0.32, 45),
                ),
            refValue = 0,
            title = "Treatment Effect (meta-analysis)",
            xTitle = "Effect size (log OR)",
        )
    }
