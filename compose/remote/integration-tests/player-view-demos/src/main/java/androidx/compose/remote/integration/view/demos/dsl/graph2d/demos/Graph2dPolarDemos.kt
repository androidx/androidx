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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.gaugeChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.polarBarChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.radialBarChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.roseChart

/** Phase 6 demos — polar / radial charts (gauge, radial bar, rose, polar bar). */

/** Radial gauge with a center readout. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dGauge(): ByteArray =
    graph2dDoc(560, 560, "Performance") {
        gaugeChart(value = 72, min = 0, max = 100, title = "Performance Index", label = "of 100")
    }

/** Radial (circular) bar chart — concentric rings. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dRadialBar(): ByteArray =
    graph2dDoc(620, 620, "Daily Activity") {
        radialBarChart(
            items = listOf("Move" to 86, "Exercise" to 64, "Stand" to 92, "Sleep" to 78),
            title = "Daily Activity (%)",
            maxValue = 100,
            theme = GraphTheme.Dark,
        )
    }

/** Nightingale rose / coxcomb. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dRose(): ByteArray =
    graph2dDoc(640, 640, "Monthly Rainfall") {
        roseChart(
            items =
                listOf(
                    "Jan" to 42,
                    "Feb" to 38,
                    "Mar" to 55,
                    "Apr" to 70,
                    "May" to 88,
                    "Jun" to 110,
                    "Jul" to 120,
                    "Aug" to 105,
                    "Sep" to 82,
                    "Oct" to 60,
                    "Nov" to 48,
                    "Dec" to 40,
                ),
            title = "Monthly Rainfall (mm)",
        )
    }

/** Polar bar chart — radial bars around a circular grid (e.g. a wind rose). */
@Suppress("RestrictedApiAndroidX")
public fun graph2dPolarBar(): ByteArray =
    graph2dDoc(640, 640, "Wind by Direction") {
        polarBarChart(
            items =
                listOf(
                    "N" to 18,
                    "NE" to 12,
                    "E" to 24,
                    "SE" to 30,
                    "S" to 22,
                    "SW" to 14,
                    "W" to 28,
                    "NW" to 20,
                ),
            title = "Wind by Direction",
        )
    }
