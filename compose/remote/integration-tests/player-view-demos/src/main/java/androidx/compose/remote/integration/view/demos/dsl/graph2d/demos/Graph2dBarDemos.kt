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
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.BarLayout
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.NumberFormat
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Orientation
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.barChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.chart2d
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.divergingBarChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.groupedBarChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.horizontalBarChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.lollipopChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.stackedBarChart

/**
 * Demos for the graph2d bar-chart family. Each wraps a chart *component* (an `RcScope` extension)
 * in a [graph2dDoc] document, and doubles as a worked example of the public API.
 */
private val quarters = listOf("Q1", "Q2", "Q3", "Q4")
private val regions = listOf("North", "South", "East", "West", "Central")

/** Simplest call: a single-series column chart with auto-scaling and nice ticks. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarBasic(): ByteArray =
    graph2dDoc(800, 500, "Quarterly Revenue") {
        barChart(
            categories = quarters,
            values = listOf(12, 19, 7, 23),
            title = "Quarterly Revenue",
            yTitle = "Revenue (\$M)",
            valueLabels = true,
        )
    }

/** Horizontal bars — ideal for ranked categories with longer labels. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarHorizontal(): ByteArray =
    graph2dDoc(800, 500, "GDP by Country") {
        horizontalBarChart(
            categories = listOf("United States", "China", "Japan", "Germany", "India"),
            values = listOf(27.4, 17.9, 4.2, 4.4, 3.7),
            title = "GDP by Country (\$T)",
        )
    }

/** Grouped (clustered) bars: three years compared per region, with a legend. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarGrouped(): ByteArray =
    graph2dDoc(900, 520, "Sales by Region") {
        groupedBarChart(
            categories = regions,
            series =
                listOf(
                    "2022" to listOf(18, 12, 9, 14, 7),
                    "2023" to listOf(22, 15, 11, 13, 9),
                    "2024" to listOf(26, 19, 16, 18, 12),
                ),
            title = "Sales by Region",
            yTitle = "Units (k)",
        )
    }

/** Stacked bars: component contributions summing to a category total. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarStacked(): ByteArray =
    graph2dDoc(800, 520, "Revenue Mix") {
        stackedBarChart(
            categories = quarters,
            series =
                listOf(
                    "Hardware" to listOf(12, 15, 11, 18),
                    "Software" to listOf(8, 10, 14, 12),
                    "Services" to listOf(5, 6, 7, 9),
                ),
            title = "Revenue Mix",
            yTitle = "Revenue (\$M)",
        )
    }

/** 100% stacked bars: relative composition (percentages) per category. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarPercentStacked(): ByteArray =
    graph2dDoc(800, 520, "Revenue Composition") {
        stackedBarChart(
            categories = quarters,
            series =
                listOf(
                    "Hardware" to listOf(12, 15, 11, 18),
                    "Software" to listOf(8, 10, 14, 12),
                    "Services" to listOf(5, 6, 7, 9),
                ),
            title = "Revenue Composition",
            percent = true,
        )
    }

/** Lollipop chart: a clean, low-ink single-series comparison. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarLollipop(): ByteArray =
    graph2dDoc(800, 500, "Active Users by Region") {
        lollipopChart(
            categories = regions,
            values = listOf(26, 19, 16, 18, 12),
            title = "Active Users by Region",
            yTitle = "Users (k)",
        )
    }

/** Diverging bars: positive/negative values from a zero baseline, color-coded by sign. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarDiverging(): ByteArray =
    graph2dDoc(800, 520, "Monthly Net Profit") {
        divergingBarChart(
            categories = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
            values = listOf(8.2, -3.1, 5.6, -1.4, 9.0, -6.3),
            title = "Monthly Net Profit",
            yTitle = "\$M",
        )
    }

/** Dark dashboard theme with a subtitle, SI value format, and modifier padding. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarDark(): ByteArray =
    graph2dDoc(800, 520, "Daily Active Users") {
        chart2d(
            "Daily Active Users",
            theme = GraphTheme.Dark,
            modifier = Modifier.fillMaxSize().padding(8f),
        ) {
            subtitle = "Last 7 days"
            xAxis { categories = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
            yAxis {
                title = "DAU"
                format = NumberFormat.SI
            }
            bars(listOf(124000, 138000, 142000, 151000, 149000, 92000, 88000))
            valueLabels = true
        }
    }

/** Animated grow-in: bars ease up from the baseline on first render (reactive, time-driven). */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarAnimated(): ByteArray =
    graph2dDoc(800, 500, "Animated Revenue") {
        chart2d("Animated Revenue") {
            animate = true
            xAxis { categories = quarters }
            yAxis { title = "Revenue (\$M)" }
            bars(listOf(12, 19, 7, 23))
        }
    }

/**
 * Custom [GraphTheme]: the same grouped chart restyled end-to-end via `.copy(...)` — thick navy
 * axes, larger fonts, a brand palette, fatter bars and a tinted plot. Exercises the style fields so
 * a visual diff against [graph2dBarGrouped] confirms the theme actually drives the marks.
 */
@Suppress("RestrictedApiAndroidX")
public fun graph2dThemeCustom(): ByteArray {
    val brand = intArrayOf(0xFF1A3D7C.toInt(), 0xFFE07A1F.toInt(), 0xFF2E8B6B.toInt())
    val house =
        GraphTheme.Light.copy(
            plotBackground = 0xFFF5F7FB.toInt(),
            axisColor = 0xFF1A3D7C.toInt(),
            axisStroke = 3f,
            gridColor = 0xFFCBD6EA.toInt(),
            gridStroke = 1.5f,
            titleColor = 0xFF1A3D7C.toInt(),
            titleSize = 32f,
            axisTitleSize = 18f,
            labelSize = 16f,
            valueLabelSize = 15f,
            legendSize = 17f,
            palette = brand,
            barPadInner = 0.32f,
            barGroupGap = 0.16f,
        )
    return graph2dDoc(900, 560, "Custom Theme") {
        groupedBarChart(
            categories = quarters,
            series =
                listOf(
                    "Plan" to listOf(14, 18, 16, 22),
                    "Actual" to listOf(12, 19, 15, 25),
                    "Forecast" to listOf(16, 20, 18, 24),
                ),
            title = "Quarterly Performance",
            yTitle = "Revenue (\$M)",
            theme = house,
        )
    }
}

/**
 * Combo chart via the layered builder: revenue bars with a target line and error bars overlaid on
 * the same axes — `chart2d { bars(...); line(...); errorBars(...) }`. Layers draw in declaration
 * order, so the line and whiskers sit on top of the bars.
 */
@Suppress("RestrictedApiAndroidX")
public fun graph2dCombo(): ByteArray =
    graph2dDoc(900, 540, "Revenue vs Target") {
        chart2d("Revenue vs Target") {
            xAxis { categories = quarters }
            yAxis { title = "Revenue (\$M)" }
            bars(listOf(12, 19, 15, 25), name = "Revenue")
            errorBars(
                listOf(12, 19, 15, 25),
                listOf(1.5f, 2f, 1f, 2.5f),
                color = 0xFF5F6368.toInt(),
            )
            line(listOf(14, 16, 18, 22), name = "Target", markers = true)
            legend = true
        }
    }

/** A polished "kitchen-sink" grouped chart used as the family hero. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBarShowcase(): ByteArray =
    graph2dDoc(900, 560, "Quarterly Performance") {
        chart2d("Quarterly Performance") {
            subtitle = "Plan vs Actual vs Forecast"
            orientation = Orientation.Vertical
            barLayout = BarLayout.Grouped
            xAxis { categories = quarters }
            yAxis { title = "Revenue (\$M)" }
            bars(listOf(14, 18, 16, 22), name = "Plan")
            bars(listOf(12, 19, 15, 25), name = "Actual")
            bars(listOf(16, 20, 18, 24), name = "Forecast")
            legend = true
        }
    }
