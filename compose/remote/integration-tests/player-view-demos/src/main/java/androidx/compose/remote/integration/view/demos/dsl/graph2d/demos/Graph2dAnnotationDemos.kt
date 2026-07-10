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

import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.LegendPosition
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.chart2d
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.comboChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.daysFromCivil
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import kotlin.math.sin

/**
 * Phase-B feature demos: annotations (reference lines/bands, callouts, event markers), the
 * secondary y axis, log scale, the calendar time axis, dashed strokes, label rotation and the
 * right-hand legend.
 */
private val months =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Reference band + target line + launch event + peak callout over a monthly line. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dAnnotations(): ByteArray =
    graph2dDoc(900, 560, "Annotated KPIs") {
        chart2d("Weekly Active Users") {
            xAxis { categories = months }
            yAxis { title = "Users (k)" }
            refBand(45, 55, label = "Goal zone", color = 0xFF54A24B.toInt())
            line(listOf(31, 34, 33, 38, 41, 47, 52, 58, 54, 49, 51, 56), markers = true)
            refLine(50, label = "Target", color = 0xFFE45756.toInt())
            eventLine("Jun", label = "v2 launch")
            annotate("Aug", 58, "All-time high")
        }
    }

/** Dual-axis combo: revenue bars (left axis) + conversion-rate line (right axis). */
@Suppress("RestrictedApiAndroidX")
public fun graph2dDualAxis(): ByteArray =
    graph2dDoc(900, 540, "Revenue vs Conversion") {
        comboChart(
            categories = listOf("Q1", "Q2", "Q3", "Q4"),
            bars = "Revenue (\$M)" to listOf(12, 19, 15, 25),
            lines = listOf("Conversion %" to listOf(2.4, 3.1, 2.8, 3.6)),
            title = "Revenue vs Conversion",
            yTitle = "Revenue (\$M)",
            secondaryLines = true,
            y2Title = "Conversion (%)",
        )
    }

/** Log10 value axis: five decades of growth stay readable. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLogScale(): ByteArray =
    graph2dDoc(900, 540, "Log Scale") {
        chart2d("User Growth (log scale)") {
            xAxis { categories = (2017..2026).map { it.toString() } }
            yAxis {
                title = "Users"
                log = true
            }
            line(
                listOf(
                    820,
                    2_400,
                    9_100,
                    31_000,
                    96_000,
                    240_000,
                    810_000,
                    2_300_000,
                    6_800_000,
                    21_000_000,
                ),
                markers = true,
            )
            refLine(1_000_000, label = "1M users")
        }
    }

/** Calendar time axis (epoch-day x values): month-boundary ticks + a dated event marker. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dTimeAxis(): ByteArray =
    graph2dDoc(940, 540, "Time Axis") {
        val start = daysFromCivil(2025, 9, 1)
        val release = daysFromCivil(2026, 2, 10)
        val pts =
            List(40) { i ->
                val day = start + i * 7
                val v = 40f + i * 1.1f + 9f * sin(i * 0.55f) + (if (day >= release) 14f else 0f)
                day to v
            }
        chart2d("Daily Sessions") {
            xAxis { time = true }
            yAxis { title = "Sessions (k)" }
            points(pts, connect = true)
            eventLine(release, label = "3.0 release")
        }
    }

/** Dashed forecast line, dashed gridlines, rotated category labels and a right-hand legend. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dStyleExtras(): ByteArray =
    graph2dDoc(940, 560, "Style Extras") {
        val theme = GraphTheme.Light.copy(gridDash = floatArrayOf(4f, 6f))
        chart2d("Bookings Outlook", theme = theme) {
            legendPosition = LegendPosition.Right
            xAxis {
                categories =
                    listOf(
                        "January",
                        "February",
                        "March",
                        "April",
                        "May",
                        "June",
                        "July",
                        "August",
                        "September",
                    )
                labelAngle = -35f
            }
            yAxis { title = "Bookings (k)" }
            line(listOf(58, 62, 67, 71, 69, 76, 82, 80, 87), name = "Actual", markers = true)
            line(
                listOf(56, 60, 65, 70, 72, 75, 79, 84, 90),
                name = "Forecast",
                dash = floatArrayOf(10f, 7f),
            )
        }
    }
