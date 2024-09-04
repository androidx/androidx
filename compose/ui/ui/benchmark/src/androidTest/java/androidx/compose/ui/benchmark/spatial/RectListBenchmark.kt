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
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.ui.benchmark.spatial

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.collection.mutableIntListOf
import androidx.compose.ui.spatial.RectList
import androidx.compose.ui.util.fastForEach
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.math.max
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RectListBenchmark {

    @get:Rule val rule = BenchmarkRule()

    private fun construct() = RectList()

    @Test
    fun b01_insertExampleDataLinear() {
        val testData = exampleLayoutRects
        rule.measureRepeated {
            val qt = construct()
            for (i in testData.indices) {
                val rect = testData[i]
                qt.insert(
                    i,
                    rect[0],
                    rect[1],
                    rect[2],
                    rect[3],
                    -1,
                    false,
                )
            }
        }
    }

    private fun insertRecursive(qt: RectList, item: Item, scrollableId: Int) {
        val bounds = item.bounds

        qt.insert(
            item.id,
            bounds[0],
            bounds[1],
            bounds[2],
            bounds[3],
            scrollableId,
            item.scrollable,
        )
        item.children.fastForEach {
            insertRecursive(qt, it, if (item.scrollable) item.id else scrollableId)
        }
    }

    @Test
    fun b01_insertExampleData() {
        val item = rootItem
        rule.measureRepeated {
            val qt = construct()
            insertRecursive(qt, item, -1)
        }
    }

    @Test
    fun b02_removeExampleData() {
        val testData = exampleLayoutRects
        rule.measureRepeated {
            val grid = runWithTimingDisabled {
                val qt = construct()
                insertRecursive(qt, rootItem, -1)
                qt
            }
            for (i in testData.indices) {
                grid.remove(i)
            }
        }
    }

    @Test
    fun b03_updateExampleItems() {
        val testData = exampleLayoutRects
        val r = Random(1234)
        rule.measureRepeated {
            val qt = runWithTimingDisabled {
                val qt = construct()
                insertRecursive(qt, rootItem, -1)
                qt
            }
            for (i in testData.indices) {
                val rect = testData[i]
                val x = r.nextInt(-100, 100)
                val y = r.nextInt(-100, 100)
                qt.update(
                    i,
                    max(rect[0] + x, 0),
                    max(rect[1] + y, 0),
                    max(rect[2] + x, 0),
                    max(rect[3] + y, 0),
                )
            }
        }
    }

    @Test
    fun b04_updateScrollableContainer() {
        val scrollableItems = scrollableItems
        val r = Random(1234)
        rule.measureRepeated {
            val qt = runWithTimingDisabled {
                val qt = construct()
                insertRecursive(qt, rootItem, -1)
                qt
            }
            scrollableItems.fastForEach {
                val x = r.nextInt(-100, 100)
                val y = r.nextInt(-100, 100)
                qt.updateSubhierarchy(it.id, x, y)
            }
        }
    }

    @Test
    fun b05_findOccludingRectsExampleItems() {
        val queries = occludingRectQueries
        rule.measureRepeated {
            val qt = runWithTimingDisabled {
                val qt = construct()
                insertRecursive(qt, rootItem, -1)
                qt
            }
            for (i in queries.indices) {
                val list = runWithTimingDisabled { mutableIntListOf() }
                val bounds = queries[i]
                qt.forEachIntersection(
                    bounds[0],
                    bounds[1],
                    bounds[2],
                    bounds[3],
                ) {
                    runWithTimingDisabled { list.add(it) }
                }
            }
        }
    }

    @Test
    fun b06_findKNearestNeighborsInDirection() {
        val queries = nearestNeighborQueries
        val numberOfResults = 4
        rule.measureRepeated {
            val qt = runWithTimingDisabled {
                val qt = construct()
                insertRecursive(qt, rootItem, -1)
                qt
            }
            for (i in queries.indices) {
                for (direction in 1..4) {
                    val list = runWithTimingDisabled { mutableIntListOf() }
                    val bounds = queries[i]
                    qt.findKNearestNeighbors(
                        direction,
                        numberOfResults,
                        bounds[0],
                        bounds[1],
                        bounds[2],
                        bounds[3],
                    ) { _, id, _, _, _, _ ->
                        runWithTimingDisabled { list.add(id) }
                    }
                }
            }
        }
    }

    @Test
    fun b06_findNearestNeighborInDirection() {
        val queries = nearestNeighborQueries
        rule.measureRepeated {
            val qt = runWithTimingDisabled {
                val qt = construct()
                insertRecursive(qt, rootItem, -1)
                qt
            }
            for (i in queries.indices) {
                for (direction in 1..4) {
                    val list = runWithTimingDisabled { mutableIntListOf() }
                    val bounds = queries[i]
                    val result =
                        qt.findNearestNeighbor(
                            direction,
                            bounds[0],
                            bounds[1],
                            bounds[2],
                            bounds[3],
                        )
                    runWithTimingDisabled { list.add(result) }
                }
            }
        }
    }

    @Test
    fun b07_findEligiblePointerInputs() {
        val queries = pointerInputQueries
        rule.measureRepeated {
            val qt = runWithTimingDisabled {
                val qt = construct()
                insertRecursive(qt, rootItem, -1)
                qt
            }
            for (i in queries.indices) {
                val list = runWithTimingDisabled { mutableIntListOf() }
                val bounds = queries[i]
                qt.forEachIntersection(
                    bounds[0],
                    bounds[1],
                ) {
                    runWithTimingDisabled { list.add(it) }
                }
            }
        }
    }
}
