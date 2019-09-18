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
import androidx.ui.test.cases.SimpleRadioButton1TestCase
import androidx.ui.test.cases.SimpleRadioButton2TestCase
import androidx.ui.test.cases.SimpleRadioButton3TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class SimpleRadioButtonBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    private val activity: Activity get() = activityRule.activity

    @Test
    fun radio_button_1_first_compose() {
        benchmarkRule.measureFirstCompose(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_first_measure() {
        benchmarkRule.measureFirstMeasure(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_first_layout() {
        benchmarkRule.measureFirstLayout(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_first_draw() {
        benchmarkRule.measureFirstDraw(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_update_recompose() {
        benchmarkRule.toggleStateMeasureRecompose(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_update_measure() {
        benchmarkRule.toggleStateMeasureMeasure(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_update_layout() {
        benchmarkRule.toggleStateMeasureLayout(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_update_draw() {
        benchmarkRule.toggleStateMeasureDraw(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_layout() {
        benchmarkRule.measureLayoutPerf(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_1_draw() {
        benchmarkRule.measureDrawPerf(activity, SimpleRadioButton1TestCase(activity))
    }

    @Test
    fun radio_button_2_first_compose() {
        benchmarkRule.measureFirstCompose(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_first_measure() {
        benchmarkRule.measureFirstMeasure(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_first_layout() {
        benchmarkRule.measureFirstLayout(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_first_draw() {
        benchmarkRule.measureFirstDraw(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_update_recompose() {
        benchmarkRule.toggleStateMeasureRecompose(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_update_measure() {
        benchmarkRule.toggleStateMeasureMeasure(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_update_layout() {
        benchmarkRule.toggleStateMeasureLayout(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_update_draw() {
        benchmarkRule.toggleStateMeasureDraw(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_layout() {
        benchmarkRule.measureLayoutPerf(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_2_draw() {
        benchmarkRule.measureDrawPerf(activity, SimpleRadioButton2TestCase(activity))
    }

    @Test
    fun radio_button_3_first_compose() {
        benchmarkRule.measureFirstCompose(activity, SimpleRadioButton3TestCase(activity))
    }

    @Test
    fun radio_button_3_first_measure() {
        benchmarkRule.measureFirstMeasure(activity, SimpleRadioButton3TestCase(activity))
    }

    @Test
    fun radio_button_3_first_layout() {
        benchmarkRule.measureFirstLayout(activity, SimpleRadioButton3TestCase(activity))
    }

    @Test
    fun radio_button_3_first_draw() {
        benchmarkRule.measureFirstDraw(activity, SimpleRadioButton3TestCase(activity))
    }

    @Test
    fun radio_button_3_update_measure() {
        benchmarkRule.toggleStateMeasureMeasure(activity, SimpleRadioButton3TestCase(activity),
            toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_update_layout() {
        benchmarkRule.toggleStateMeasureLayout(activity, SimpleRadioButton3TestCase(activity),
            toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_update_draw() {
        benchmarkRule.toggleStateMeasureDraw(activity, SimpleRadioButton3TestCase(activity),
            toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_layout() {
        benchmarkRule.measureLayoutPerf(activity, SimpleRadioButton3TestCase(activity))
    }

    @Test
    fun radio_button_3_draw() {
        benchmarkRule.measureDrawPerf(activity, SimpleRadioButton3TestCase(activity))
    }
}
