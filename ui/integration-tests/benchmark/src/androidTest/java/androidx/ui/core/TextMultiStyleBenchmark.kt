/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.benchmarkDrawPerf
import androidx.ui.benchmark.benchmarkFirstCompose
import androidx.ui.benchmark.benchmarkFirstDraw
import androidx.ui.benchmark.benchmarkFirstLayout
import androidx.ui.benchmark.benchmarkFirstMeasure
import androidx.ui.benchmark.benchmarkLayoutPerf
import androidx.ui.integration.test.core.text.TextMultiStyleTestCase
import androidx.ui.integration.test.TextBenchmarkTestRule
import androidx.ui.integration.test.cartesian
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The benchmark for [Text] composable with the input being a styled text in the form of
 * AnnotatedString.
 */
@LargeTest
@RunWith(Parameterized::class)
class TextMultiStyleBenchmark(
    private val textLength: Int,
    private val styleCount: Int
) {
    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule()

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0} styleCount={1}")
        fun initParameters() = cartesian(
            arrayOf(32, 512),
            arrayOf(0, 32)
        )
    }

    /**
     * Measure the time taken to compose a [Text] composable from scratch with styled text as input.
     * This is the time taken to call the [Text] composable function.
     */
    @Test
    fun first_compose() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstCompose {
                TextMultiStyleTestCase(
                    textLength,
                    styleCount,
                    textGenerator
                )
            }
        }
    }

    /**
     * Measure the time taken by first time measure the Text composable with styled text as input.
     * This is mainly the time used to measure all the [Measurable]s in the [Text] composable.
     */
    @Test
    fun first_measure() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstMeasure {
                TextMultiStyleTestCase(
                    textLength,
                    styleCount,
                    textGenerator
                )
            }
        }
    }

    /**
     * Measure the time taken by first time layout the Text composable with styled text as input.
     * This is mainly the time used to place [Placeable]s in [Text] composable.
     */
    @Test
    fun first_layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstLayout {
                TextMultiStyleTestCase(
                    textLength,
                    styleCount,
                    textGenerator
                )
            }
        }
    }

    /**
     * Measure the time taken by first time draw the Text composable with styled text
     * as input.
     */
    @Test
    fun first_draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstDraw {
                TextMultiStyleTestCase(
                    textLength,
                    styleCount,
                    textGenerator
                )
            }
        }
    }

    /**
     * Measure the time taken by layout a Text composable with styled text input, when
     * layout constrains changed.
     * This is mainly the time used to re-measure and re-layout the composable.
     */
    @Test
    fun layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkLayoutPerf {
                TextMultiStyleTestCase(
                    textLength,
                    styleCount,
                    textGenerator
                )
            }
        }
    }

    /**
     * Measure the time taken by re-draw a Text composable with styled text input.
     */
    @Test
    fun draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkDrawPerf {
                TextMultiStyleTestCase(
                    textLength,
                    styleCount,
                    textGenerator
                )
            }
        }
    }
}
