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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class TooltipBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val plainTooltipTestCaseFactory = { TooltipTestCase(TooltipType.Plain) }
    private val richTooltipTestCaseFactory = { TooltipTestCase(TooltipType.Rich) }

    @Test
    fun plainTooltipFirstPixel() {
        benchmarkRule.benchmarkToFirstPixel(plainTooltipTestCaseFactory)
    }

    @Test
    fun richTooltipFirstPixel() {
        benchmarkRule.benchmarkToFirstPixel(richTooltipTestCaseFactory)
    }

    @Test
    fun plainTooltipVisibilityTest() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = plainTooltipTestCaseFactory,
            assertOneRecomposition = false
        )
    }

    @Test
    fun richTooltipVisibilityTest() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = richTooltipTestCaseFactory,
            assertOneRecomposition = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class TooltipTestCase(val tooltipType: TooltipType) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: TooltipState
    private lateinit var scope: CoroutineScope

    @Composable
    override fun MeasuredContent() {
        state = rememberTooltipState()
        scope = rememberCoroutineScope()

        val tooltip: @Composable TooltipScope.() -> Unit
        val positionProvider: PopupPositionProvider
        when (tooltipType) {
            TooltipType.Plain -> {
                tooltip = { PlainTooltipTest() }
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider()
            }
            TooltipType.Rich -> {
                tooltip = { RichTooltipTest() }
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider()
            }
        }

        TooltipBox(positionProvider = positionProvider, tooltip = tooltip, state = state) {}
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        if (state.isVisible) {
            state.dismiss()
        } else {
            scope.launch { state.show() }
        }
    }

    @Composable
    private fun TooltipScope.PlainTooltipTest() {
        PlainTooltip { Text("Text") }
    }

    @Composable
    private fun TooltipScope.RichTooltipTest() {
        RichTooltip(
            title = { Text("Subhead") },
            action = { TextButton(onClick = {}) { Text(text = "Action") } }
        ) {
            Text(text = "Text")
        }
    }
}

private enum class TooltipType {
    Plain,
    Rich
}
