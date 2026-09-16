/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the draw traversal cost of LayoutNodes with [Modifier.drawBehind].
 *
 * Compares two topologies with 100 drawing nodes each:
 * - [drawTraversal_flat]: 100 sibling nodes in a 10x10 grid.
 * - [drawTraversal_nested]: 10 sibling chains nested 10 levels deep.
 *
 * Any timing divergence between the two isolates the cost of hierarchy depth during draw.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DrawTraversalBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    /** Measures redraw of 100 drawing nodes in a flat 10x10 grid. */
    @Test
    fun drawTraversal_flat() {
        benchmarkRule.benchmarkDrawPerf { FlatDrawTraversalTestCase() }
    }

    /**
     * Measures redraw of the same 100 drawing nodes arranged as 10 sibling chains, each nested 10
     * levels deep, isolating the cost of traversal depth when read against [drawTraversal_flat].
     */
    @Test
    fun drawTraversal_nested() {
        benchmarkRule.benchmarkDrawPerf { NestedDrawTraversalTestCase() }
    }
}

/**
 * A 10x10 grid of fixed-size [Box]es, each drawing a single rect via [Modifier.drawBehind]. All
 * sizes are fixed so measure/layout settle once and stay settled.
 */
private class FlatDrawTraversalTestCase : ComposeTestCase {
    @Composable
    override fun Content() {
        Column {
            repeat(GridSize) {
                Row {
                    repeat(GridSize) {
                        Box(Modifier.size(NodeSize).drawBehind { drawRect(NodeColor) })
                    }
                }
            }
        }
    }
}

/**
 * The same 100 drawing nodes as [FlatDrawTraversalTestCase], but arranged as [GridSize] sibling
 * chains each nested [GridSize] levels deep. Every level is the same fixed size, so measure/layout
 * remain trivial; only the shape of the draw traversal differs.
 */
private class NestedDrawTraversalTestCase : ComposeTestCase {
    @Composable
    override fun Content() {
        Column { repeat(GridSize) { NestedDrawBox(depth = GridSize) } }
    }

    @Composable
    private fun NestedDrawBox(depth: Int) {
        Box(Modifier.size(NodeSize).drawBehind { drawRect(NodeColor) }) {
            if (depth > 1) {
                NestedDrawBox(depth - 1)
            }
        }
    }
}

private const val GridSize = 10
private val NodeSize
    get() = 8.dp
private val NodeColor
    get() = Color.Gray
