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

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DatePickerBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val datePickerTestCaseFactory = { DatePickerTestCase() }
    private val dateInputTestCaseFactory = { DateInputTestCase() }

    @Test
    fun first_compose_pickerMode() {
        benchmarkRule.benchmarkFirstCompose(datePickerTestCaseFactory)
    }

    @Test
    fun first_compose_inputMode() {
        benchmarkRule.benchmarkFirstCompose(dateInputTestCaseFactory)
    }

    @Test
    fun datePicker_measure() {
        benchmarkRule.benchmarkFirstMeasure(
            caseFactory = datePickerTestCaseFactory,
            allowPendingChanges = true
        )
    }

    @Test
    fun dateInput_measure() {
        benchmarkRule.benchmarkFirstMeasure(
            caseFactory = dateInputTestCaseFactory,
            allowPendingChanges = true
        )
    }

    @Test
    fun datePicker_layout() {
        benchmarkRule.benchmarkFirstLayout(
            caseFactory = datePickerTestCaseFactory,
            allowPendingChanges = true
        )
    }

    @Test
    fun dateInput_layout() {
        benchmarkRule.benchmarkFirstLayout(
            caseFactory = dateInputTestCaseFactory,
            allowPendingChanges = true
        )
    }

    @Test
    fun datePicker_draw() {
        benchmarkRule.benchmarkFirstDraw(
            caseFactory = datePickerTestCaseFactory,
            allowPendingChanges = true
        )
    }

    @Test
    fun dateInput_draw() {
        benchmarkRule.benchmarkFirstDraw(
            caseFactory = dateInputTestCaseFactory,
            allowPendingChanges = true
        )
    }
}

/**
 * A [DatePicker] test case when initiated in its default picker mode.
 */
internal class DatePickerTestCase : LayeredComposeTestCase() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        DatePicker(state = rememberDatePickerState())
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

/**
 * A [DatePicker] test case when initiated in an input mode.
 */
internal class DateInputTestCase : LayeredComposeTestCase() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        DatePicker(state = rememberDatePickerState(initialDisplayMode = DisplayMode.Input))
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
