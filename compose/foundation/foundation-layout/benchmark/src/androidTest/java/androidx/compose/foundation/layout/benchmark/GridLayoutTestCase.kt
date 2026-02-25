/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridFlow
import androidx.compose.foundation.layout.GridScope
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The execution engine for [GridLayoutBenchmark]. */
@OptIn(ExperimentalGridApi::class)
class GridLayoutTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private var gapSize by mutableStateOf(0.dp)

    @Composable
    override fun MeasuredContent() {
        // Capture state read to ensure structural invalidation on toggle
        val currentGap = gapSize

        val gridContent: @Composable GridScope.() -> Unit = remember {
            {
                val scope = this
                // Cache resolved modifiers tied to this specific GridScope.
                // This runs exactly once per test setup, avoiding allocations during the benchmark.
                val modifiers =
                    remember(scope) {
                        List(ITEM_COUNT) { index ->
                            // Row-major 1-based indexing
                            val row = (index / COLUMNS) + 1
                            val col = (index % COLUMNS) + 1
                            val color = COLORS[index % COLORS.size]

                            // Vary size slightly to prevent the layout engine from caching
                            // measurements
                            val size = 20.dp + (index % 3).dp

                            with(scope) {
                                Modifier.gridItem(row = row, column = col)
                                    .background(color)
                                    .size(size)
                            }
                        }
                    }

                repeat(ITEM_COUNT) { i -> Box(modifiers[i]) }
            }
        }

        Grid(
            modifier = Modifier.fillMaxSize(),
            config = {
                flow = GridFlow.Row
                gap(currentGap)
                repeat(COLUMNS) { column(TRACK_SIZE) }
                repeat(ROWS) { row(TRACK_SIZE) }
            },
            content = gridContent,
        )
    }

    override fun toggleState() {
        gapSize = if (gapSize == 0.dp) 5.dp else 0.dp
    }

    companion object {
        private const val COLUMNS = 10
        private const val ROWS = 10
        private const val ITEM_COUNT = 100
        private val TRACK_SIZE = GridTrackSize.Fixed(40.dp)
        private val COLORS = listOf(Color.Blue, Color.Green, Color.Red, Color.Yellow)
    }
}
