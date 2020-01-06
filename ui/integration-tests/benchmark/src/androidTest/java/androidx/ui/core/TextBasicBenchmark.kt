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
import androidx.ui.integration.test.core.text.TextBasicTestCase
import androidx.ui.integration.test.TextBenchmarkTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The benchmark for [Text] composable with the input being a plain string.
 */
@LargeTest
@RunWith(Parameterized::class)
class TextBasicBenchmark(
    private val textLength: Int
) {

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule()

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0}")
        fun initParameters(): Array<Any> = arrayOf(32, 512)
    }

    /**
     * Measure the time taken to compose a [Text] composable from scratch with the given input.
     * This is the time taken to call the [Text] composable function.
     */
    @Test
    fun first_compose() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstCompose {
                TextBasicTestCase(textLength, textGenerator)
            }
        }
    }

    /**
     * Measure the time taken by the first time measure the [Text] composable with the given input.
     * This is mainly the time used to measure all the [Measurable]s in the [Text] composable.
     */
    @Test
    fun first_measure() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstMeasure {
                TextBasicTestCase(textLength, textGenerator)
            }
        }
    }

    /**
     * Measure the time taken by the first time layout the [Text] composable with the given input.
     * This is mainly the time used to place [Placeable]s in [Text] composable.
     */
    @Test
    fun first_layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstLayout {
                TextBasicTestCase(textLength, textGenerator)
            }
        }
    }

    /**
     * Measure the time taken by first time draw the [Text] composable with the given input.
     */
    @Test
    fun first_draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkFirstDraw {
                TextBasicTestCase(textLength, textGenerator)
            }
        }
    }

    /**
     * Measure the time taken by layout the [Text] composable after the layout constrains changed.
     * This is mainly the time used to re-measure and re-layout the composable.
     */
    @Test
    fun layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkLayoutPerf {
                TextBasicTestCase(textLength, textGenerator)
            }
        }
    }

    /**
     * Measure the time taken by redrawing the [Text] composable.
     */
    @Test
    fun draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.benchmarkDrawPerf {
                TextBasicTestCase(textLength, textGenerator)
            }
        }
    }
}