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

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ProgressIndicatorBenchmark(private val type: ProgressIndicatorType) {
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = ProgressIndicatorType.values()
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { ProgressIndicatorTestCase(type) }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(testCaseFactory)
    }

    @Test
    fun changeProgress() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(testCaseFactory)
    }
}
internal class ProgressIndicatorTestCase(
    private val type: ProgressIndicatorType
) : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: MutableFloatState

    @Composable
    override fun MeasuredContent() {
        state = remember { mutableFloatStateOf(0f) }

        when (type) {
            ProgressIndicatorType.Linear ->
                LinearProgressIndicator(progress = state.value)
            ProgressIndicatorType.Circular ->
                CircularProgressIndicator(progress = state.value)
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 0f) 0.5f else 0.0f
    }
}

enum class ProgressIndicatorType {
    Linear, Circular
}
