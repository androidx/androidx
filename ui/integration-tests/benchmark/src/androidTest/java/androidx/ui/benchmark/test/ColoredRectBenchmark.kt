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

package androidx.ui.benchmark.test

import android.app.Activity
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.test.DisableTransitions
import androidx.ui.test.RectanglesInColumnTestCase
import androidx.ui.test.recomposeSyncAssertHadChanges
import androidx.ui.test.recomposeSyncAssertNoChanges
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark that runs [RectanglesInColumnTestCase]. Currently we test re-composition time.
 */
@LargeTest
@RunWith(Parameterized::class)
class ColoredRectBenchmark(private val numberOfRectangles: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    @Test
    fun toggleRectangleColor_recompose() {
        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                val testCase = RectanglesInColumnTestCase(activityRule.activity, numberOfRectangles)
                    .apply { runSetup() }
                testCase.compositionContext.recomposeSyncAssertNoChanges()

                benchmarkRule.measureRepeated {
                    runWithTimingDisabled {
                        testCase.toggleState()
                    }
                    testCase.compositionContext.recomposeSyncAssertHadChanges()
                }
            }
        })
    }
}
