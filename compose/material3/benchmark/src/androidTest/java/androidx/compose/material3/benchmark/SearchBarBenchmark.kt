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

import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class SearchBarBenchmark(private val type: SearchBarType) {
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = SearchBarType.values()
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { SearchBarTestCase(type) }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkFirstRenderUntilStable(testCaseFactory)
    }

    @Test
    fun changeActiveState() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = testCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal class SearchBarTestCase(
    private val type: SearchBarType
) : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: MutableState<Boolean>

    @Composable
    override fun MeasuredContent() {
        state = remember { mutableStateOf(true) }

        when (type) {
            SearchBarType.FullScreen ->
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    active = state.value,
                    onActiveChange = { state.value = it },
                    content = {},
                )
            SearchBarType.Docked ->
                DockedSearchBar(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    active = state.value,
                    onActiveChange = { state.value = it },
                    content = {},
                )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }

    override fun toggleState() {
        state.value = !state.value
    }
}

enum class SearchBarType {
    FullScreen, Docked
}
