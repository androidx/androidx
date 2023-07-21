/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.benchmark

import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DateRangePickerBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val dateRangePickerTestCaseFactory = { DateRangePickerTestCase() }
    private val dateRangeInputTestCaseFactory = { DateRangeInputTestCase() }

    @Test
    fun first_compose_pickerMode() {
        benchmarkRule.benchmarkFirstCompose(dateRangePickerTestCaseFactory)
    }

    @Test
    fun first_compose_inputMode() {
        benchmarkRule.benchmarkFirstCompose(dateRangeInputTestCaseFactory)
    }

    @Test
    fun dateRangePicker_measure() {
        benchmarkRule.benchmarkFirstMeasure(dateRangePickerTestCaseFactory)
    }

    @Test
    fun dateRangeInput_measure() {
        benchmarkRule.benchmarkFirstMeasure(
            caseFactory = dateRangeInputTestCaseFactory,
            allowPendingChanges = true
        )
    }

    @Test
    fun dateRangePicker_layout() {
        benchmarkRule.benchmarkFirstLayout(dateRangePickerTestCaseFactory)
    }

    @Test
    fun dateRangeInput_layout() {
        benchmarkRule.benchmarkFirstLayout(
            caseFactory = dateRangeInputTestCaseFactory,
            allowPendingChanges = true
        )
    }

    @Test
    fun dateRangePicker_draw() {
        benchmarkRule.benchmarkFirstDraw(dateRangePickerTestCaseFactory)
    }

    @Test
    fun dateRangeInput_draw() {
        benchmarkRule.benchmarkFirstDraw(
            caseFactory = dateRangeInputTestCaseFactory,
            allowPendingChanges = true
        )
    }
}

/**
 * A [DateRangePicker] test case when initiated in its default picker mode.
 */
internal class DateRangePickerTestCase : LayeredComposeTestCase() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        DateRangePicker(state = rememberDateRangePickerState())
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

/**
 * A [DateRangePicker] test case when initiated in an input mode.
 */
internal class DateRangeInputTestCase : LayeredComposeTestCase() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        DateRangePicker(
            state = rememberDateRangePickerState(initialDisplayMode = DisplayMode.Input)
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
