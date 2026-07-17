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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ListItemBenchmark(private val type: ListType) {
    companion object {
        @Parameterized.Parameters(name = "{0}") @JvmStatic fun parameters() = ListType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val listItemTestCaseFactory = { ListItemTestCase(type) }

    @Ignore
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(listItemTestCaseFactory)
    }

    @Ignore
    @Test
    fun listItem_measure() {
        benchmarkRule.benchmarkFirstMeasure(listItemTestCaseFactory)
    }

    @Ignore
    @Test
    fun listItem_layout() {
        benchmarkRule.benchmarkFirstLayout(listItemTestCaseFactory)
    }

    @Ignore
    @Test
    fun listItem_draw() {
        benchmarkRule.benchmarkFirstDraw(listItemTestCaseFactory)
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(listItemTestCaseFactory)
    }

    @Test
    fun toggle_recomposeMeasureLayout() {
        assumeTrue(type == ListType.StandardSelectable || type == ListType.SegmentedSelectable)

        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = listItemTestCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

internal class ListItemTestCase(private val type: ListType) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var state by mutableStateOf(false)

    @Composable
    @Suppress("DEPRECATION")
    override fun MeasuredContent() {
        when (type) {
            ListType.Legacy -> {
                ListItem(
                    headlineContent = { Box(Modifier.size(width = 100.dp, height = 24.dp)) },
                    overlineContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    supportingContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    leadingContent = { Box(Modifier.size(24.dp)) },
                    trailingContent = { Box(Modifier.size(24.dp)) },
                )
            }
            ListType.Standard -> {
                ListItem(
                    content = { Box(Modifier.size(width = 100.dp, height = 24.dp)) },
                    overlineContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    supportingContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    leadingContent = { Box(Modifier.size(24.dp)) },
                    trailingContent = { Box(Modifier.size(24.dp)) },
                )
            }
            ListType.Segmented -> {
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    content = { Box(Modifier.size(width = 100.dp, height = 24.dp)) },
                    overlineContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    supportingContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    leadingContent = { Box(Modifier.size(24.dp)) },
                    trailingContent = { Box(Modifier.size(24.dp)) },
                )
            }
            ListType.StandardSelectable -> {
                ListItem(
                    selected = state,
                    onClick = {},
                    content = { Box(Modifier.size(width = 100.dp, height = 24.dp)) },
                    overlineContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    supportingContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    leadingContent = { Box(Modifier.size(24.dp)) },
                    trailingContent = { Box(Modifier.size(24.dp)) },
                )
            }
            ListType.SegmentedSelectable -> {
                SegmentedListItem(
                    selected = state,
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    content = { Box(Modifier.size(width = 100.dp, height = 24.dp)) },
                    overlineContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    supportingContent = { Box(Modifier.size(width = 80.dp, height = 16.dp)) },
                    leadingContent = { Box(Modifier.size(24.dp)) },
                    trailingContent = { Box(Modifier.size(24.dp)) },
                )
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

enum class ListType {
    Legacy,
    Standard,
    Segmented,
    StandardSelectable,
    SegmentedSelectable,
}
