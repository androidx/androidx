/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalMetricApi::class)

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import android.graphics.Canvas
import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TextListScrollBenchmark(
    private val styled: Boolean,
    private val prefetch: Boolean,
    private val enableContentCapture: Boolean,
) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics =
                listOf(
                    FrameTimingMetric(),
                    TraceSectionMetric(
                        sectionName = "TextStringSimpleNode::measure",
                        label = "measureText",
                        mode = TraceSectionMetric.Mode.Sum,
                    ),
                    TraceSectionMetric(
                        sectionName = "BackgroundTextMeasurement",
                        label = "bgMeasureText",
                        mode = TraceSectionMetric.Mode.Sum,
                    ),
                    TraceSectionMetric(
                        sectionName = "compose:lazy:prefetch:measure",
                        label = "premeasure",
                        mode = TraceSectionMetric.Mode.Sum,
                    ),
                    TraceSectionMetric(
                        sectionName = "compose:lazy:prefetch:compose",
                        label = "precompose",
                        mode = TraceSectionMetric.Mode.Sum,
                    ),
                    TraceSectionMetric(
                        sectionName = "ContentCapture:sendPendingContentCaptureEvents",
                        mode = TraceSectionMetric.Mode.Sum,
                    ),
                ),
            compilationMode = CompilationMode.Full(),
            iterations = 3,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkConfig.Prefetch, prefetch)
                intent.putExtra(BenchmarkConfig.Styled, styled)
                intent.putExtra(BenchmarkConfig.EnableContentCapture, enableContentCapture)
                intent.putExtra(BenchmarkConfig.WordCount, 2)
                intent.putExtra(BenchmarkConfig.TextCount, 18)
                intent.putExtra(BenchmarkConfig.WordLength, 8)
                tryFreeTextLayoutCache()
                startActivityAndWait(intent)
            },
        ) {
            val content = device.findObject(By.res("android", "content"))
            content.setGestureMargin(device.displayWidth / 5)
            for (i in 1..5) {
                content.fling(Direction.DOWN)
            }
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.TEXT_LIST_ACTIVITY"

        object BenchmarkConfig {
            val WordCount = "word_count" // Integer
            val TextCount = "text_count" // Integer
            val WordLength = "word_length" // Integer
            val Styled = "styled" // Boolean
            val Prefetch = "prefetch" // Boolean
            val EnableContentCapture = "enableContentCapture"
        }

        @Parameterized.Parameters(name = "styled={0}, prefetch={1}, enableContentCapture={2}")
        @JvmStatic
        fun parameters() =
            cartesian(
                arrayOf(false), // styled
                arrayOf(true, false), // prefetch
                arrayOf(true, false), // enableContentCapture
            )
    }
}

/**
 * Tries to find and call Canvas#freeTextLayoutCaches through reflection. Above API 32 this function
 * is hidden and inaccessible even by reflection. In this case we should just rely on randomized
 * words.
 */
fun tryFreeTextLayoutCache() {
    try {
        val freeCaches = Canvas::class.java.getDeclaredMethod("freeTextLayoutCaches")
        freeCaches.isAccessible = true
        freeCaches.invoke(null)
    } catch (e: Exception) {
        Log.w("FreeTextLayoutCaches", "Cannot free text layout cache", e)
        // ignore
    }
}

/** Creates a cartesian product of the given arrays. */
fun cartesian(vararg arrays: Array<Any?>): List<Array<Any?>> {
    return arrays.fold(listOf(arrayOf())) { acc, list ->
        // add items from the current list
        // to each list that was accumulated
        acc.flatMap { accListItem -> list.map { accListItem + it } }
    }
}
