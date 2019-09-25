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

import android.app.Activity
import androidx.benchmark.junit4.BenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.filters.Suppress
import androidx.test.rule.ActivityTestRule
import androidx.ui.benchmark.measureDrawPerf
import androidx.ui.benchmark.measureFirstCompose
import androidx.ui.benchmark.measureFirstDraw
import androidx.ui.benchmark.measureFirstLayout
import androidx.ui.benchmark.measureFirstMeasure
import androidx.ui.benchmark.measureLayoutPerf
import androidx.ui.test.DisableTransitions
import androidx.ui.test.TextBenchmarkTestRule
import androidx.ui.test.cartesian
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The benchmark for [Text] widget with the input being styled text in form of composable [Span]s.
 */
@Suppress
@LargeTest
@RunWith(Parameterized::class)
class TextWithSpanBenchmark(
    private val textLength: Int
) {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule()

    private val activity: Activity get() = activityRule.activity

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0}")
        fun initParameters() = cartesian(
            arrayOf(32, 512)
        )
    }

    /**
     * Measure the time taken to compose a Text widget with [Span]s as input.
     * This is mainly the time used to call the [Text] composable function. Different from other
     * [Text] widget benchmarks, this one include the time used to create [Span] tree.
     */
    @Test
    fun first_compose() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstCompose(
                activity,
                TextWithSpanTestCase(
                    activity,
                    textLength,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken to measure a Text widget with [Span]s as input.
     * This is mainly the time used to measure all the [Measurable]s in the [Text] widget.
     */
    @Test
    fun first_measure() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstMeasure(
                activity,
                TextWithSpanTestCase(
                    activity,
                    textLength,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken to layout a Text widget with [Span]s as input for the first time.
     * This is mainly the time used to place [Placeable]s in [Text] widget.
     */
    @Test
    fun first_layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstLayout(
                activity,
                TextWithSpanTestCase(
                    activity,
                    textLength,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken to draw a Text widget with [Span]s as input for the first time.
     */
    @Test
    fun first_draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstDraw(
                activity,
                TextWithSpanTestCase(
                    activity,
                    textLength,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken to layout a Text widget with [Span]s as input after layout
     * constrains changed.
     * This is mainly the time used to re-measure and re-layout the widget.
     */
    @Test
    fun layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureLayoutPerf(
                activity,
                TextWithSpanTestCase(
                    activity,
                    textLength,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken to draw a Text widget with [Span]s as input.
     */
    @Test
    fun draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureDrawPerf(
                activity,
                TextWithSpanTestCase(
                    activity,
                    textLength,
                    textGenerator
                )
            )
        }
    }
}
