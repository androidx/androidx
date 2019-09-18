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
import androidx.ui.test.RandomTextGeneratorTestRule
import androidx.ui.test.cartesian
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress
@LargeTest
@RunWith(Parameterized::class)
class TextMultiStyleBenchmark(
    private val textLength: Int,
    private val styleCount: Int,
    private val hasMetricAffectingStyle: Boolean
) {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    @get:Rule
    val textGeneratorRule = RandomTextGeneratorTestRule()

    private val activity: Activity get() = activityRule.activity

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0} styleCount={1} hasMetricAffectingStyle={2}")
        fun initParameters() = cartesian(
                arrayOf(8, 64, 512),
                arrayOf(8, 64, 512),
                arrayOf(true, false)
            )
    }

    @Test
    fun first_compose() {
        textGeneratorRule.generator { textGenerator ->
            benchmarkRule.measureFirstCompose(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    hasMetricAffectingStyle,
                    textGenerator
                )
            )
        }
    }

    @Test
    fun first_measure() {
        textGeneratorRule.generator { textGenerator ->
            benchmarkRule.measureFirstMeasure(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    hasMetricAffectingStyle,
                    textGenerator
                )
            )
        }
    }

    @Test
    fun first_layout() {
        textGeneratorRule.generator { textGenerator ->
            benchmarkRule.measureFirstLayout(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    hasMetricAffectingStyle,
                    textGenerator
                )
            )
        }
    }

    @Test
    fun first_draw() {
        textGeneratorRule.generator { textGenerator ->
            benchmarkRule.measureFirstDraw(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    hasMetricAffectingStyle,
                    textGenerator
                )
            )
        }
    }

    @Test
    fun layout() {
        textGeneratorRule.generator { textGenerator ->
            benchmarkRule.measureLayoutPerf(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    hasMetricAffectingStyle,
                    textGenerator
                )
            )
        }
    }

    @Test
    fun draw() {
        textGeneratorRule.generator { textGenerator ->
            benchmarkRule.measureDrawPerf(
                activity,
                TextMultiStyleTestCase(
                    activity,
                    textLength,
                    styleCount,
                    hasMetricAffectingStyle,
                    textGenerator
                )
            )
        }
    }
}
