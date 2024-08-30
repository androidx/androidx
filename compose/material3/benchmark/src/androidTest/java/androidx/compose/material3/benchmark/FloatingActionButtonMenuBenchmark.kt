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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FloatingActionButtonMenuBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val floatingActionButtonMenuTestCaseFactory = { FloatingActionButtonMenuTestCase() }

    @Ignore
    @Test
    fun fabMenu_first_compose() {
        benchmarkRule.benchmarkFirstCompose(floatingActionButtonMenuTestCaseFactory)
    }

    @Ignore
    @Test
    fun fabMenu_measure() {
        benchmarkRule.benchmarkFirstMeasure(floatingActionButtonMenuTestCaseFactory)
    }

    @Ignore
    @Test
    fun fabMenu_layout() {
        benchmarkRule.benchmarkFirstLayout(floatingActionButtonMenuTestCaseFactory)
    }

    @Ignore
    @Test
    fun fabMenu_draw() {
        benchmarkRule.benchmarkFirstDraw(floatingActionButtonMenuTestCaseFactory)
    }

    @Test
    fun fabMenu_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(floatingActionButtonMenuTestCaseFactory)
    }

    @Test
    fun fabMenu_toggle_recomposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = floatingActionButtonMenuTestCaseFactory,
            assertOneRecomposition = false
        )
    }
}

internal class FloatingActionButtonMenuTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private var state by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun MeasuredContent() {
        Box(Modifier.fillMaxSize()) {
            FloatingActionButtonMenu(
                modifier = Modifier.align(Alignment.BottomEnd),
                expanded = state,
                button = {
                    ToggleFloatingActionButton(
                        checked = state,
                        onCheckedChange = { /* Do nothing */ }
                    ) {
                        Spacer(Modifier.size(24.dp))
                    }
                }
            ) {
                repeat(6) {
                    FloatingActionButtonMenuItem(
                        onClick = { /* Do nothing */ },
                        icon = { Spacer(Modifier.size(24.dp)) },
                        text = { Spacer(Modifier.size(24.dp)) },
                    )
                }
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        state = !state
    }
}
