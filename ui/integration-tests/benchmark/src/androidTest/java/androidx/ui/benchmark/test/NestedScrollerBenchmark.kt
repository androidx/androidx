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
import androidx.benchmark.junit4.BenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.benchmark.measureDrawPerf
import androidx.ui.benchmark.measureFirstCompose
import androidx.ui.benchmark.measureFirstDraw
import androidx.ui.benchmark.measureFirstLayout
import androidx.ui.benchmark.measureFirstMeasure
import androidx.ui.benchmark.measureLayoutPerf
import androidx.ui.benchmark.toggleStateMeasureDraw
import androidx.ui.benchmark.toggleStateMeasureLayout
import androidx.ui.benchmark.toggleStateMeasureMeasure
import androidx.ui.test.DisableTransitions
import androidx.ui.test.cases.NestedScrollerTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class NestedScrollerBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    private val activity: Activity get() = activityRule.activity

    @Test
    fun first_compose() {
        benchmarkRule.measureFirstCompose(activity, NestedScrollerTestCase(activity))
    }

    @Test
    fun first_measure() {
        benchmarkRule.measureFirstMeasure(activity, NestedScrollerTestCase(activity))
    }

    @Test
    fun first_layout() {
        benchmarkRule.measureFirstLayout(activity, NestedScrollerTestCase(activity))
    }

    @Test
    fun first_draw() {
        benchmarkRule.measureFirstDraw(activity, NestedScrollerTestCase(activity))
    }

    @Test
    fun changeScroll_measure() {
        benchmarkRule.toggleStateMeasureMeasure(activity, NestedScrollerTestCase(activity),
            toggleCausesRecompose = false, firstDrawCausesRecompose = true)
    }

    @Test
    fun changeScroll_layout() {
        benchmarkRule.toggleStateMeasureLayout(activity, NestedScrollerTestCase(activity),
            toggleCausesRecompose = false, firstDrawCausesRecompose = true)
    }

    @Test
    fun changeScroll_draw() {
        benchmarkRule.toggleStateMeasureDraw(activity, NestedScrollerTestCase(activity),
            toggleCausesRecompose = false, firstDrawCausesRecompose = true)
    }

    @Test
    fun layout() {
        benchmarkRule.measureLayoutPerf(activity, NestedScrollerTestCase(activity))
    }

    @Test
    fun draw() {
        benchmarkRule.measureDrawPerf(activity, NestedScrollerTestCase(activity))
    }
}
