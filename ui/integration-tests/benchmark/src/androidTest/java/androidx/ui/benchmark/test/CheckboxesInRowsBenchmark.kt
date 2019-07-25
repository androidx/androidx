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
import androidx.ui.benchmark.toggleStateMeasureRecompose
import androidx.ui.test.cases.CheckboxesInRowsTestCase
import androidx.ui.test.DisableTransitions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark that runs [CheckboxesInRowsTestCase].
 */
@LargeTest
@RunWith(Parameterized::class)
class CheckboxesInRowsBenchmark(private val numberOfCheckboxes: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    private val activity: Activity get() = activityRule.activity

    @Test
    fun first_compose() {
        benchmarkRule.measureFirstCompose(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun first_measure() {
        benchmarkRule.measureFirstMeasure(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun first_layout() {
        benchmarkRule.measureFirstLayout(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun first_draw() {
        benchmarkRule.measureFirstDraw(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun toggleCheckbox_recompose() {
        benchmarkRule.toggleStateMeasureRecompose(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun toggleCheckbox_measure() {
        benchmarkRule.toggleStateMeasureMeasure(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun toggleCheckbox_layout() {
        benchmarkRule.toggleStateMeasureLayout(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun toggleCheckbox_draw() {
        benchmarkRule.toggleStateMeasureDraw(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun layout() {
        benchmarkRule.measureLayoutPerf(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }

    @Test
    fun draw() {
        benchmarkRule.measureDrawPerf(activity,
            CheckboxesInRowsTestCase(activity, numberOfCheckboxes))
    }
}