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
 * The benchmark for [Text] widget with the input being a styled text in the form of
 * AnnotatedString.
 */
@LargeTest
@RunWith(Parameterized::class)
class TextMultiStyleBenchmark(
    private val textLength: Int,
    private val styleCount: Int
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
        @Parameterized.Parameters(name = "length={0} styleCount={1}")
        fun initParameters() = cartesian(
                arrayOf(32, 512),
                arrayOf(0, 32)
            )
    }

    /**
     * Measure the time taken to compose a [Text] widget from scratch with styled text as input.
     * This is the time taken to call the [Text] composable function.
     */
    @Test
    fun first_compose() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstCompose(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken by first time measure the Text widget with styled text as input.
     * This is mainly the time used to measure all the [Measurable]s in the [Text] widget.
     */
    @Test
    fun first_measure() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstMeasure(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken by first time layout the Text widget with styled text as input.
     * This is mainly the time used to place [Placeable]s in [Text] widget.
     */
    @Test
    fun first_layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstLayout(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken by first time draw the Text widget with styled text
     * as input.
     */
    @Test
    fun first_draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstDraw(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken by layout a Text widget with styled text input, when
     * layout constrains changed.
     * This is mainly the time used to re-measure and re-layout the widget.
     */
    @Test
    fun layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureLayoutPerf(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    textGenerator
                )
            )
        }
    }

    /**
     * Measure the time taken by re-draw a Text widget with styled text input.
     */
    @Test
    fun draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureDrawPerf(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    textGenerator
                )
            )
        }
    }
}
