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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalExpandedNavigationRail
import androidx.compose.material3.ModalExpandedNavigationRailState
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.rememberModalExpandedNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavigationRailBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { NavigationRailTestCase() }
    private val collapsedWideRailTestCaseFactory = { NavigationRailTestCase(true) }
    private val expandedWideRailTestCaseFactory = { NavigationRailTestCase(true, true) }
    private val modalExpandedRailTestCaseFactory = { ModalExpandedRailTestCase() }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(testCaseFactory)
    }

    @Test
    fun changeSelection() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            testCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun wideNavigationRail_collapsed_firstPixel() {
        benchmarkRule.benchmarkFirstRenderUntilStable(collapsedWideRailTestCaseFactory)
    }

    @Test
    fun wideNavigationRail_collapsed_changeSelection() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            collapsedWideRailTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun wideNavigationRail_collapsed_expands() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            { NavigationRailTestCase(isWideNavRail = true, changeSelectionToggleTestCase = false) },
            assertOneRecomposition = false,
        )
    }

    @Test
    fun wideNavigationRail_expanded_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(expandedWideRailTestCaseFactory)
    }

    @Test
    fun wideNavigationRail_expanded_changeSelection() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            expandedWideRailTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun wideNavigationRail_expanded_collapses() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                NavigationRailTestCase(
                    isWideNavRail = true,
                    expanded = true,
                    changeSelectionToggleTestCase = false
                )
            },
            assertOneRecomposition = false,
        )
    }

    @Test
    fun modalExpandedNavigationRail_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(modalExpandedRailTestCaseFactory)
    }

    @Test
    fun modalExpandedNavigationRail_stateChange() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            modalExpandedRailTestCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class NavigationRailTestCase(
    private val isWideNavRail: Boolean = false,
    private var expanded: Boolean = false,
    private val changeSelectionToggleTestCase: Boolean = true,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var selectedIndexState: MutableIntState
    private lateinit var actualExpanded: MutableState<Boolean>

    @Composable
    override fun MeasuredContent() {
        selectedIndexState = remember { mutableIntStateOf(0) }
        actualExpanded = remember { mutableStateOf(expanded) }

        if (isWideNavRail) {
            WideNavigationRail(expanded = actualExpanded.value) {
                WideNavigationRailItem(
                    selected = selectedIndexState.value == 0,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                    railExpanded = actualExpanded.value,
                    label = { Spacer(Modifier.size(24.dp)) }
                )
                WideNavigationRailItem(
                    selected = selectedIndexState.value == 1,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                    railExpanded = actualExpanded.value,
                    label = { Spacer(Modifier.size(24.dp)) }
                )
            }
        } else {
            NavigationRail {
                NavigationRailItem(
                    selected = selectedIndexState.value == 0,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                )
                NavigationRailItem(
                    selected = selectedIndexState.value == 1,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                )
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        if (isWideNavRail) {
            MaterialExpressiveTheme { content() }
        } else {
            MaterialTheme { content() }
        }
    }

    override fun toggleState() {
        if (changeSelectionToggleTestCase) {
            // Case where item selection changes.
            selectedIndexState.value = if (selectedIndexState.value == 0) 1 else 0
        } else {
            // Case where rail expands if it's collapsed, or collapses if it's expanded.
            actualExpanded.value = !actualExpanded.value
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class ModalExpandedRailTestCase() : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: ModalExpandedNavigationRailState
    private lateinit var scope: CoroutineScope

    @Composable
    override fun MeasuredContent() {
        state = rememberModalExpandedNavigationRailState()
        scope = rememberCoroutineScope()

        ModalExpandedNavigationRail(
            onDismissRequest = {},
            railState = state,
        ) {
            WideNavigationRailItem(
                selected = true,
                onClick = {},
                icon = { Spacer(Modifier.size(24.dp)) },
                railExpanded = true,
                label = { Spacer(Modifier.size(24.dp)) }
            )
            WideNavigationRailItem(
                selected = false,
                onClick = {},
                icon = { Spacer(Modifier.size(24.dp)) },
                railExpanded = true,
                label = { Spacer(Modifier.size(24.dp)) }
            )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialExpressiveTheme { content() }
    }

    override fun toggleState() {
        if (state.isOpen) {
            scope.launch { state.close() }
        } else {
            scope.launch { state.open() }
        }
    }
}
