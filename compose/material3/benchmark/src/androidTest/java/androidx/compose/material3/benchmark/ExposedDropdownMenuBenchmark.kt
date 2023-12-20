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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ExposedDropdownMenuBenchmark(private val expanded: Boolean) {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun edm_first_compose() {
        benchmarkRule.benchmarkFirstCompose { ExposedDropdownMenuTestCase(expanded) }
    }

    @Test
    fun edm_measure() {
        benchmarkRule.benchmarkFirstMeasure { ExposedDropdownMenuTestCase(expanded) }
    }

    @Test
    fun edm_layout() {
        benchmarkRule.benchmarkFirstLayout { ExposedDropdownMenuTestCase(expanded) }
    }

    @Test
    fun edm_draw() {
        benchmarkRule.benchmarkFirstDraw { ExposedDropdownMenuTestCase(expanded) }
    }

    @Test
    fun edm_textFieldAnchor_repositioned() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout({
            ExposedDropdownMenuTestCase(expanded)
        })
    }

    companion object {
        @Parameterized.Parameters(name = "expanded = {0}")
        @JvmStatic
        fun parameters() = arrayOf(true, false)
    }
}

internal class ExposedDropdownMenuTestCase(
    private val expanded: Boolean
) : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: MutableState<Dp>

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        val spacerHeight = remember { mutableStateOf(100.dp) }
        state = spacerHeight

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(state.value))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {},
            ) {
                Spacer(modifier = Modifier.size(100.dp).menuAnchor())
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {},
                    content = { Spacer(modifier = Modifier.height(50.dp).fillMaxWidth()) },
                )
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 100.dp) 200.dp else 100.dp
    }
}
