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

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.LineInterp
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.NumberFormat
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.areaChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.chart2d
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.lineChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.multiLineChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.sparkline
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.splineChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.stackedAreaChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.stepChart

/**
 * Phase 2 demos — the time-series / trend (line + area) family. Each wraps a chart *component* in a
 * [graph2dDoc] document, and doubles as a worked example of the public API.
 */
private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug")

/** Single line with point markers and auto-scaled tight axis. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLineBasic(): ByteArray =
    graph2dDoc(800, 500, "Monthly Active Users") {
        lineChart(
            categories = months,
            values = listOf(42, 45, 41, 48, 52, 49, 55, 58),
            title = "Monthly Active Users",
            yTitle = "MAU (k)",
        )
    }

/** Several series sharing axes, with a legend. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLineMulti(): ByteArray =
    graph2dDoc(900, 520, "Active Users by Product") {
        multiLineChart(
            categories = months,
            series =
                listOf(
                    "Product A" to listOf(42, 45, 41, 48, 52, 49, 55, 58),
                    "Product B" to listOf(30, 33, 38, 36, 40, 44, 43, 47),
                    "Product C" to listOf(18, 20, 22, 27, 25, 30, 34, 33),
                ),
            title = "Active Users by Product",
            yTitle = "Users (k)",
            markers = true,
        )
    }

/** Smoothed spline line. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLineSpline(): ByteArray =
    graph2dDoc(800, 500, "Smoothed Trend") {
        splineChart(
            categories = months,
            values = listOf(12.5, 18.2, 15.8, 22.4, 28.1, 24.6, 31.0, 29.3),
            title = "Smoothed Trend",
            yTitle = "Index",
        )
    }

/** Step line (discrete level changes). */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLineStep(): ByteArray =
    graph2dDoc(800, 500, "Pricing Tier Over Time") {
        stepChart(
            categories = months,
            values = listOf(2, 2, 3, 3, 3, 5, 5, 8),
            title = "Pricing Tier Over Time",
            yTitle = "Tier",
        )
    }

/** Filled area chart. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dAreaBasic(): ByteArray =
    graph2dDoc(800, 500, "Cumulative Signups") {
        areaChart(
            categories = months,
            values = listOf(42, 45, 41, 48, 52, 49, 55, 58),
            title = "Cumulative Signups",
            yTitle = "Signups (k)",
            interp = LineInterp.Spline,
        )
    }

/** Stacked area: component contributions over time. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dAreaStacked(): ByteArray =
    graph2dDoc(900, 520, "Traffic by Source") {
        stackedAreaChart(
            categories = months,
            series =
                listOf(
                    "Organic" to listOf(20, 22, 24, 26, 30, 28, 33, 36),
                    "Paid" to listOf(12, 14, 13, 18, 20, 22, 21, 25),
                    "Referral" to listOf(6, 7, 9, 8, 11, 13, 12, 15),
                ),
            title = "Traffic by Source",
            yTitle = "Sessions (k)",
        )
    }

/** 100% stacked area: composition over time. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dAreaPercent(): ByteArray =
    graph2dDoc(900, 520, "Traffic Composition") {
        stackedAreaChart(
            categories = months,
            series =
                listOf(
                    "Organic" to listOf(20, 22, 24, 26, 30, 28, 33, 36),
                    "Paid" to listOf(12, 14, 13, 18, 20, 22, 21, 25),
                    "Referral" to listOf(6, 7, 9, 8, 11, 13, 12, 15),
                ),
            title = "Traffic Composition",
            percent = true,
        )
    }

/** Dark theme multi-line with an SI-formatted axis. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLineDark(): ByteArray =
    graph2dDoc(900, 520, "Requests per Minute") {
        chart2d("Requests per Minute", theme = GraphTheme.Dark) {
            subtitle = "API gateway, last 8 intervals"
            xAxis { categories = months }
            yAxis {
                title = "RPM"
                format = NumberFormat.SI
            }
            line(
                listOf(120000, 138000, 132000, 165000, 180000, 172000, 210000, 235000),
                name = "v2",
                markers = true,
            )
            line(
                listOf(90000, 96000, 102000, 110000, 118000, 126000, 130000, 142000),
                name = "v1",
                markers = true,
            )
        }
    }

/** Animated grow-in line (rises from the baseline). */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLineAnimated(): ByteArray =
    graph2dDoc(800, 500, "Animated Area") {
        areaChart(
            categories = months,
            values = listOf(42, 45, 41, 48, 52, 49, 55, 58),
            title = "Animated Area",
            yTitle = "Value",
            interp = LineInterp.Spline,
            animate = true,
        )
    }

/** Interactive multi-line chart: drag horizontally to move the crosshair and read values. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLineInteractive(): ByteArray =
    graph2dDoc(900, 560, "Interactive") {
        multiLineChart(
            categories = months,
            series =
                listOf(
                    "Product A" to listOf(42, 45, 41, 48, 52, 49, 55, 58),
                    "Product B" to listOf(30, 33, 38, 36, 40, 44, 43, 47),
                    "Product C" to listOf(18, 20, 22, 27, 25, 30, 34, 33),
                ),
            title = "Drag to Inspect",
            yTitle = "Users (k)",
            markers = true,
            interactive = true,
        )
    }

/**
 * A chrome-less filled sparkline used like a canvas: the chart fills the (padded) canvas
 * reactively, so the light card background and 14px inset come purely from the modifier chain.
 */
@Suppress("RestrictedApiAndroidX")
public fun graph2dSparkline(): ByteArray =
    graph2dDoc(400, 120, "Sparkline") {
        sparkline(
            values = listOf(3, 5, 4, 6, 5, 7, 6, 8, 7, 9, 8, 11),
            fill = true,
            modifier = Modifier.fillMaxSize().background(0xFFF5F5F7.toInt()).padding(14f),
        )
    }
