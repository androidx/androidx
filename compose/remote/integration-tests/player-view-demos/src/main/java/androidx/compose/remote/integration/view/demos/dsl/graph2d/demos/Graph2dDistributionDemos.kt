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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.boxPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.densityPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.ecdfPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.histogram
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.stripPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.violinPlot
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Phase 3 demos — the distribution family (histogram, density/KDE, ECDF, box, violin, strip).
 * Sample data is generated deterministically host-side so the docs are stable.
 */

/** Deterministic gaussian sample via a fixed LCG + Box-Muller. */
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
private fun gaussian(n: Int, mean: Float, sd: Float, seed: Int): List<Double> {
    var s = seed
    fun rnd(): Float {
        s = (s * 1103515245 + 12345) and 0x7fffffff
        return (s % 100000) / 100000f
    }
    return List(n) {
        val u1 = rnd().coerceIn(1e-4f, 1f)
        val u2 = rnd()
        val z = sqrt(-2f * ln(u1)) * cos(2f * Math.PI.toFloat() * u2)
        (mean + sd * z).toDouble()
    }
}

@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
private val heights = gaussian(220, 172f, 8f, 7)

/** Histogram of a numeric sample. */
@Suppress("RestrictedApiAndroidX")
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
fun graph2dHistogram(): ByteArray =
    graph2dDoc(800, 500, "Height Distribution") {
        histogram(heights, title = "Height Distribution", xTitle = "Height (cm)")
    }

/** Kernel density estimate. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dDensity(): ByteArray =
    graph2dDoc(800, 500, "Height Density") {
        densityPlot(heights, title = "Height Density (KDE)", xTitle = "Height (cm)")
    }

/** Empirical cumulative distribution. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dEcdf(): ByteArray =
    graph2dDoc(800, 500, "Height ECDF") {
        ecdfPlot(heights, title = "Height ECDF", xTitle = "Height (cm)")
    }

private fun groups(): List<Pair<String, List<Number>>> =
    listOf(
        "Control" to gaussian(80, 50f, 9f, 11),
        "Drug A" to gaussian(80, 58f, 7f, 23),
        "Drug B" to gaussian(80, 64f, 12f, 41),
    )

/** Box-and-whisker comparison across groups. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dBoxPlot(): ByteArray =
    graph2dDoc(800, 520, "Response by Treatment") {
        boxPlot(groups(), title = "Response by Treatment", yTitle = "Score")
    }

/** Violin (mirrored KDE) comparison across groups. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dViolin(): ByteArray =
    graph2dDoc(800, 520, "Response Distribution") {
        violinPlot(groups(), title = "Response Distribution", yTitle = "Score")
    }

/** Strip plot: raw observations per group. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dStrip(): ByteArray =
    graph2dDoc(800, 520, "Raw Observations") {
        stripPlot(groups(), title = "Raw Observations", yTitle = "Score", theme = GraphTheme.Light)
    }
