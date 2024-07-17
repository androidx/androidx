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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class NavigationBarBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { NavigationBarTestCase() }
    private val shortNavBarTopIconTestCaseFactory = { NavigationBarTestCase(true) }
    private val shortNavBarStartIconTestCaseFactory = {
        NavigationBarTestCase(true, NavigationItemIconPosition.Start)
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkFirstRenderUntilStable(testCaseFactory)
    }

    @Test
    fun changeSelection() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            testCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun shortNavigationBar_topIcon_firstPixel() {
        benchmarkRule.benchmarkFirstRenderUntilStable(shortNavBarTopIconTestCaseFactory)
    }

    @Test
    fun shortNavigationBar_topIcon_changeSelection() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            shortNavBarTopIconTestCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun shortNavigationBar_startIcon_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(shortNavBarStartIconTestCaseFactory)
    }

    @Test
    fun shortNavigationBar_startIcon_changeSelection() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            shortNavBarStartIconTestCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class NavigationBarTestCase(
    private val isShortNavBar: Boolean = false,
    private val shortNavBarIconPosition: NavigationItemIconPosition =
        NavigationItemIconPosition.Top,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var selectedIndexState: MutableIntState

    @Composable
    override fun MeasuredContent() {
        selectedIndexState = remember { mutableIntStateOf(0) }

        if (isShortNavBar) {
            ShortNavigationBar {
                ShortNavigationBarItem(
                    selected = selectedIndexState.value == 0,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                    iconPosition = shortNavBarIconPosition,
                    label = { Spacer(Modifier.size(24.dp)) }
                )
                ShortNavigationBarItem(
                    selected = selectedIndexState.value == 1,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                    iconPosition = shortNavBarIconPosition,
                    label = { Spacer(Modifier.size(24.dp)) }
                )
            }
        } else {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedIndexState.value == 0,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                )
                NavigationBarItem(
                    selected = selectedIndexState.value == 1,
                    onClick = {},
                    icon = { Spacer(Modifier.size(24.dp)) },
                )
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        if (isShortNavBar) {
            MaterialExpressiveTheme { content() }
        } else {
            MaterialTheme { content() }
        }
    }

    override fun toggleState() {
        selectedIndexState.value = if (selectedIndexState.value == 0) 1 else 0
    }
}
