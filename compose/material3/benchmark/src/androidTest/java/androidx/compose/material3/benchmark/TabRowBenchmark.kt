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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import org.junit.Rule
import org.junit.Test

class TabRowBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val tabRowTestCaseFactory = { TabRowTestCase() }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkFirstRenderUntilStable(tabRowTestCaseFactory)
    }

    @Test
    fun selectTab() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = tabRowTestCaseFactory,
            assertOneRecomposition = false
        )
    }
}

internal class TabRowTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private var state: Int by mutableStateOf(0)

    @Composable
    override fun MeasuredContent() {
        val titles = listOf("TAB 1", "TAB 2", "TAB 3")
        PrimaryTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = state == index,
                    onClick = { state = index }
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
        if (state == 0) {
            state = 1
        } else {
            state = 0
        }
    }
}
