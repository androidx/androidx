/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.material3.ElevatedToggleButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ToggleButtonBenchmark(private val type: ToggleButtonType) {

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = ToggleButtonType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val toggleButtonTestCaseFactory = { ToggleButtonTestCase(type) }

    @Test
    fun toggleButton_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(toggleButtonTestCaseFactory)
    }

    @Test
    fun toggleButton_toggleRecomposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            toggleButtonTestCaseFactory,
            assertOneRecomposition = false
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class ToggleButtonTestCase(private val type: ToggleButtonType) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var checked: MutableState<Boolean>

    @Composable
    override fun MeasuredContent() {
        checked = remember { mutableStateOf(false) }
        when (type) {
            ToggleButtonType.Filled ->
                ToggleButton(checked = checked.value, onCheckedChange = {}) { Text("Button") }
            ToggleButtonType.Elevated ->
                ElevatedToggleButton(checked = checked.value, onCheckedChange = {}) {
                    Text("Button")
                }
            ToggleButtonType.Tonal ->
                TonalToggleButton(checked = checked.value, onCheckedChange = {}) { Text("Button") }
            ToggleButtonType.Outlined ->
                OutlinedToggleButton(checked = checked.value, onCheckedChange = {}) {
                    Text("Button")
                }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        checked.value = !checked.value
    }
}

enum class ToggleButtonType {
    Filled,
    Elevated,
    Tonal,
    Outlined
}
