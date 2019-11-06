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

import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.benchmarkDrawPerf
import androidx.ui.benchmark.benchmarkFirstCompose
import androidx.ui.benchmark.benchmarkFirstDraw
import androidx.ui.benchmark.benchmarkFirstLayout
import androidx.ui.benchmark.benchmarkFirstMeasure
import androidx.ui.benchmark.benchmarkLayoutPerf
import androidx.ui.benchmark.toggleStateBenchmarkDraw
import androidx.ui.benchmark.toggleStateBenchmarkLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasure
import androidx.ui.benchmark.toggleStateBenchmarkRecompose
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
    val benchmarkRule = ComposeBenchmarkRule(enableTransitions = true)

    @Test
    fun radio_button_1_first_compose() {
        benchmarkRule.benchmarkFirstCompose(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_first_layout() {
        benchmarkRule.benchmarkFirstLayout(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_first_draw() {
        benchmarkRule.benchmarkFirstDraw(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_layout() {
        benchmarkRule.benchmarkLayoutPerf(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_1_draw() {
        benchmarkRule.benchmarkDrawPerf(SimpleRadioButton1TestCase())
    }

    @Test
    fun radio_button_2_first_compose() {
        benchmarkRule.benchmarkFirstCompose(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_first_layout() {
        benchmarkRule.benchmarkFirstLayout(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_first_draw() {
        benchmarkRule.benchmarkFirstDraw(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_layout() {
        benchmarkRule.benchmarkLayoutPerf(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_2_draw() {
        benchmarkRule.benchmarkDrawPerf(SimpleRadioButton2TestCase())
    }

    @Test
    fun radio_button_3_first_compose() {
        benchmarkRule.benchmarkFirstCompose(SimpleRadioButton3TestCase())
    }

    @Test
    fun radio_button_3_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(SimpleRadioButton3TestCase())
    }

    @Test
    fun radio_button_3_first_layout() {
        benchmarkRule.benchmarkFirstLayout(SimpleRadioButton3TestCase())
    }

    @Test
    fun radio_button_3_first_draw() {
        benchmarkRule.benchmarkFirstDraw(SimpleRadioButton3TestCase())
    }

    @Test
    fun radio_button_3_update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(SimpleRadioButton3TestCase(),
            toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(SimpleRadioButton3TestCase(),
            toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(SimpleRadioButton3TestCase(),
            toggleCausesRecompose = false)
    }

    @Test
    fun radio_button_3_layout() {
        benchmarkRule.benchmarkLayoutPerf(SimpleRadioButton3TestCase())
    }

    @Test
    fun radio_button_3_draw() {
        benchmarkRule.benchmarkDrawPerf(SimpleRadioButton3TestCase())
    }
}
