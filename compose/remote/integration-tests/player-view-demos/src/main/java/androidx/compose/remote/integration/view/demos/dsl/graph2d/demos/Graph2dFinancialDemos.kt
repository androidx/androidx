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

import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Bullet
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.bulletChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.funnelChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.paretoChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.waterfallChart

/** Phase 5 demos — financial / ranking charts (waterfall, funnel, bullet, Pareto). */

/** Waterfall (bridge): a running cumulative with up/down steps and a total bar. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dWaterfall(): ByteArray =
    graph2dDoc(820, 520, "Revenue Bridge") {
        waterfallChart(
            steps =
                listOf(
                    "Q1 Start" to 100,
                    "New Sales" to 45,
                    "Churn" to -20,
                    "Upsell" to 30,
                    "Refunds" to -15,
                ),
            title = "Revenue Bridge",
            yTitle = "\$M",
        )
    }

/** Funnel: stage-by-stage drop-off. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dFunnel(): ByteArray =
    graph2dDoc(720, 560, "Conversion Funnel") {
        funnelChart(
            stages =
                listOf(
                    "Visits" to 1000,
                    "Signups" to 620,
                    "Trials" to 380,
                    "Paid" to 210,
                    "Renewed" to 140,
                ),
            title = "Conversion Funnel",
        )
    }

/** Bullet chart: KPI rows vs target and qualitative ranges. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBullet(): ByteArray =
    graph2dDoc(760, 420, "KPIs vs Target") {
        bulletChart(
            items =
                listOf(
                    Bullet("Revenue", 78, 85, 100, listOf(50, 75, 100)),
                    Bullet("Profit", 62, 70, 100, listOf(40, 70, 100)),
                    Bullet("CSAT", 88, 80, 100, listOf(60, 80, 100)),
                    Bullet("Retention", 71, 75, 100, listOf(50, 80, 100)),
                ),
            title = "KPIs vs Target",
        )
    }

/** Pareto: sorted bars + cumulative percentage line. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dPareto(): ByteArray =
    graph2dDoc(820, 520, "Defect Causes") {
        paretoChart(
            data =
                listOf(
                    "Scratches" to 120,
                    "Cracks" to 80,
                    "Misalign" to 55,
                    "Color" to 30,
                    "Gaps" to 18,
                    "Other" to 9,
                ),
            title = "Defect Causes",
            yTitle = "Count",
        )
    }
