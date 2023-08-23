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

import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CheckboxBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val checkboxTestCaseFactory = { CheckboxTestCase() }

    @Ignore
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(checkboxTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(checkboxTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(checkboxTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(checkboxTestCaseFactory)
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(checkboxTestCaseFactory)
    }

    @Test
    fun toggle_recomposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = checkboxTestCaseFactory,
            assertOneRecomposition = false
        )
    }
}

internal class CheckboxTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private var state by mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        Checkbox(checked = state, onCheckedChange = null)
    }

    override fun toggleState() {
        state = !state
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
