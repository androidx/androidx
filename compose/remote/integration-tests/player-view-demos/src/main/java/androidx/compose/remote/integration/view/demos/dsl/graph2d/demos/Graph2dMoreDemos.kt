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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GanttTask
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.confusionMatrix
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.functionPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.ganttChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.graph2dDoc
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.likertChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.qqPlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.quadrantChart
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.ridgelinePlot
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.treemap
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("RestrictedApiAndroidX")
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
        (mean + sd * sqrt(-2f * ln(u1)) * cos(2f * Math.PI.toFloat() * u2)).toDouble()
    }
}

/** Squarified treemap. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dTreemap(): ByteArray =
    graph2dDoc(760, 600, "Market Caps") {
        treemap(
            items =
                listOf(
                    "Apple" to 3400,
                    "Microsoft" to 3100,
                    "Nvidia" to 2900,
                    "Alphabet" to 2100,
                    "Amazon" to 1900,
                    "Meta" to 1300,
                    "Tesla" to 800,
                    "Other" to 1500,
                ),
            title = "Market Caps (\$B)",
        )
    }

/** Confusion matrix. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dConfusion(): ByteArray =
    graph2dDoc(640, 600, "Confusion Matrix") {
        confusionMatrix(
            classes = listOf("Cat", "Dog", "Bird", "Fish"),
            matrix =
                listOf(
                    listOf(52, 4, 3, 1),
                    listOf(5, 47, 2, 1),
                    listOf(2, 3, 44, 6),
                    listOf(0, 1, 5, 49),
                ),
            title = "Classifier Confusion",
        )
    }

/** Quadrant chart. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dQuadrant(): ByteArray =
    graph2dDoc(720, 600, "Portfolio") {
        quadrantChart(
            points =
                listOf(
                    20 to 70,
                    35 to 30,
                    55 to 80,
                    60 to 25,
                    72 to 60,
                    80 to 85,
                    45 to 50,
                    28 to 18,
                ),
            xDivider = 50,
            yDivider = 50,
            cornerLabels = listOf("Stars", "Question Marks", "Dogs", "Cash Cows"),
            title = "Growth–Share Matrix",
            xTitle = "Market Growth",
            yTitle = "Market Share",
        )
    }

/** Function plot. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dFunction(): ByteArray =
    graph2dDoc(820, 520, "Function") {
        functionPlot(
            xMin = -6.5,
            xMax = 6.5,
            samples = 160,
            title = "f(x) = sin(x)·e^(−x²/20)",
            xTitle = "x",
            yTitle = "f(x)",
        ) { x ->
            sin(x) * kotlin.math.exp(-x * x / 20.0)
        }
    }

/** Normal Q-Q plot. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dQQ(): ByteArray =
    graph2dDoc(620, 600, "Q-Q") {
        qqPlot(sample = gaussian(120, 50f, 10f, 17), title = "Normal Q-Q Plot")
    }

/** Gantt chart. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dGantt(): ByteArray =
    graph2dDoc(840, 520, "Schedule") {
        ganttChart(
            tasks =
                listOf(
                    GanttTask("Research", 0, 6),
                    GanttTask("Design", 5, 12),
                    GanttTask("Build", 11, 24),
                    GanttTask("Test", 22, 30),
                    GanttTask("Launch", 29, 33),
                ),
            title = "Project Schedule",
            xTitle = "Day",
        )
    }

/** Ridgeline / joy plot. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dRidgeline(): ByteArray =
    graph2dDoc(720, 620, "Distributions") {
        ridgelinePlot(
            groups =
                listOf(
                    "Jan" to gaussian(120, 30f, 6f, 3),
                    "Feb" to gaussian(120, 34f, 7f, 9),
                    "Mar" to gaussian(120, 41f, 8f, 21),
                    "Apr" to gaussian(120, 52f, 9f, 33),
                    "May" to gaussian(120, 60f, 7f, 41),
                ),
            title = "Monthly Temperature Distribution",
        )
    }

/** Likert / diverging stacked survey. */
@Suppress("RestrictedApiAndroidX")
public fun graph2dLikert(): ByteArray =
    graph2dDoc(820, 520, "Survey") {
        likertChart(
            questions =
                listOf(
                    "Easy to use" to listOf(4, 8, 12, 41, 35),
                    "Reliable" to listOf(6, 14, 18, 38, 24),
                    "Good value" to listOf(10, 20, 25, 30, 15),
                    "Would recommend" to listOf(3, 7, 10, 35, 45),
                ),
            levels = listOf("Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree"),
            title = "Customer Survey",
        )
    }
