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

import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.BubblePoint
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.bubbleChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.connectedScatter
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.donutChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.pieChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.radarChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.scatterPlot

/**
 * Phase 4 demos — part-to-whole (pie / donut), radial (radar) and relationship (scatter / bubble /
 * connected scatter) charts.
 */
private val marketShare =
    listOf("Android" to 42, "iOS" to 31, "Web" to 14, "Desktop" to 8, "Other" to 5)

/** Pie chart with percentage labels and a legend. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dPie(): ByteArray =
    graph2dDoc(640, 640, "Platform Share") { pieChart(marketShare, title = "Platform Share") }

/** Donut chart (hollow center). */
@Suppress("RestrictedApiAndroidX")
public fun graph2dDonut(): ByteArray =
    graph2dDoc(640, 640, "Platform Share") {
        donutChart(marketShare, title = "Platform Share", theme = GraphTheme.Dark)
    }

/** Radar chart comparing two products across five attributes. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dRadar(): ByteArray =
    graph2dDoc(680, 680, "Product Comparison") {
        radarChart(
            axes = listOf("Speed", "Battery", "Camera", "Price", "Display", "Support"),
            series =
                listOf(
                    "Model X" to listOf(8, 6, 9, 5, 8, 7),
                    "Model Y" to listOf(6, 9, 6, 8, 7, 5),
                ),
            title = "Product Comparison",
            maxValue = 10,
        )
    }

/** Deterministic correlated scatter sample. */
private fun scatterPoints(n: Int, seed: Int): List<Pair<Number, Number>> {
    var s = seed
    fun rnd(): Float {
        s = (s * 1103515245 + 12345) and 0x7fffffff
        return (s % 100000) / 100000f
    }
    return List(n) {
        val x = 10f + rnd() * 80f
        val y = 20f + 0.7f * x + (rnd() - 0.5f) * 35f
        Pair<Number, Number>(x, y)
    }
}

/** Scatter plot showing a positive correlation. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dScatter(): ByteArray =
    graph2dDoc(720, 540, "Spend vs Revenue") {
        scatterPlot(
            points = scatterPoints(60, 19),
            title = "Spend vs Revenue",
            xTitle = "Ad Spend (k)",
            yTitle = "Revenue (k)",
        )
    }

/** Bubble chart: a third value sets each point's size. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBubble(): ByteArray =
    graph2dDoc(720, 560, "Market Map") {
        bubbleChart(
            points =
                listOf(
                    BubblePoint(20, 35, 8),
                    BubblePoint(35, 52, 30),
                    BubblePoint(48, 41, 14),
                    BubblePoint(55, 70, 55),
                    BubblePoint(62, 58, 22),
                    BubblePoint(74, 84, 70),
                    BubblePoint(81, 66, 40),
                    BubblePoint(40, 28, 12),
                ),
            title = "Market Map",
            xTitle = "Growth (%)",
            yTitle = "Satisfaction",
        )
    }

/** Connected scatter: a trajectory through (x, y) space in order. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dConnectedScatter(): ByteArray =
    graph2dDoc(720, 560, "Phase Trajectory") {
        connectedScatter(
            points =
                listOf(
                    30 to 20,
                    42 to 35,
                    55 to 30,
                    64 to 48,
                    60 to 64,
                    48 to 72,
                    36 to 64,
                    33 to 48,
                    40 to 38,
                    52 to 44,
                ),
            title = "Phase Trajectory",
            xTitle = "Position",
            yTitle = "Velocity",
        )
    }
