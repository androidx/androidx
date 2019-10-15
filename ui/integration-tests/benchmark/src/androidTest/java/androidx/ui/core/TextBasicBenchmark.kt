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
        fun initParameters(): Array<Any> = arrayOf(32, 512)
    }

    /**
     * Measure the time taken to compose a [Text] composable from scratch with the given input.
     * This is the time taken to call the [Text] composable function.
     */
    @Test
    fun first_compose() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstCompose(
                activity,
                TextBasicTestCase(activity, textLength, textGenerator)
            )
        }
    }

    /**
     * Measure the time taken by the first time measure the [Text] composable with the given input.
     * This is mainly the time used to measure all the [Measurable]s in the [Text] composable.
     */
    @Test
    fun first_measure() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstMeasure(
                activity,
                TextBasicTestCase(activity, textLength, textGenerator)
            )
        }
    }

    /**
     * Measure the time taken by the first time layout the [Text] composable with the given input.
     * This is mainly the time used to place [Placeable]s in [Text] composable.
     */
    @Test
    fun first_layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstLayout(
                activity,
                TextBasicTestCase(activity, textLength, textGenerator)
            )
        }
    }

    /**
     * Measure the time taken by first time draw the [Text] composable with the given input.
     */
    @Test
    fun first_draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureFirstDraw(
                activity,
                TextBasicTestCase(activity, textLength, textGenerator)
            )
        }
    }

    /**
     * Measure the time taken by layout the [Text] composable after the layout constrains changed.
     * This is mainly the time used to re-measure and re-layout the composable.
     */
    @Test
    fun layout() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureLayoutPerf(
                activity,
                TextBasicTestCase(activity, textLength, textGenerator)
            )
        }
    }

    /**
     * Measure the time taken by redrawing the [Text] composable.
     */
    @Test
    fun draw() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureDrawPerf(
                activity,
                TextBasicTestCase(activity, textLength, textGenerator)
            )
        }
    }
}