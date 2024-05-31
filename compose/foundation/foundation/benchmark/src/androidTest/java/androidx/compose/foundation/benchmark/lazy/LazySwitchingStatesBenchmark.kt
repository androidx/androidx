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

package androidx.compose.foundation.benchmark.lazy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.setupContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazySwitchingStatesBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun lazyColumn_switchingItems_composition() {
        benchmarkRule.runBenchmark(composition = true, switchingStateCount = NUMBER_OF_LAZY_ITEMS)
    }

    @Test
    fun lazyColumn_switchingItems_measure() {
        benchmarkRule.runBenchmark(composition = false, switchingStateCount = NUMBER_OF_LAZY_ITEMS)
    }

    @Test
    fun lazyColumn_switchingItems_composition_one_state() {
        benchmarkRule.runBenchmark(composition = true, switchingStateCount = 1)
    }

    @Test
    fun lazyColumn_switchingItems_measure_one_state() {
        benchmarkRule.runBenchmark(composition = false, switchingStateCount = 1)
    }

    private fun ComposeBenchmarkRule.runBenchmark(composition: Boolean, switchingStateCount: Int) {
        runBenchmarkFor(
            { LazyColumnSwitchingItemsCase(readInComposition = composition) },
        ) {
            runOnUiThread {
                setupContent()
                doFramesUntilIdle()
            }

            measureRepeatedOnUiThread {
                runWithTimingDisabled {
                    assertNoPendingChanges()
                    repeat(switchingStateCount) { getTestCase().toggle(it) }
                    doFramesUntilIdle()
                    assertNoPendingChanges()
                }

                repeat(switchingStateCount) { getTestCase().toggle(it) }
                doFramesUntilIdle()
            }
        }
    }
}

// The number is based on height of items below (20 visible + 5 extra).
private const val NUMBER_OF_LAZY_ITEMS = 25

class LazyColumnSwitchingItemsCase(private val readInComposition: Boolean = false) :
    ComposeTestCase {
    val items = List(NUMBER_OF_LAZY_ITEMS) { mutableStateOf(false) }

    @Composable
    override fun Content() {
        LazyColumn(
            Modifier.requiredHeight(400.dp).fillMaxWidth(),
            flingBehavior = NoFlingBehavior
        ) {
            items(items) { state ->
                val color =
                    if (readInComposition) {
                        if (state.value) Color.Blue else Color.Red
                    } else {
                        Color.Red
                    }
                Box(
                    Modifier.width(20.dp).height(20.dp).drawBehind {
                        val rectColor =
                            if (readInComposition) {
                                color
                            } else {
                                if (state.value) Color.Blue else Color.Red
                            }
                        drawRoundRect(rectColor, cornerRadius = CornerRadius(20f))
                    }
                )
            }
        }
    }

    fun toggle(index: Int) {
        Snapshot.withoutReadObservation { items[index].value = !items[index].value }
    }
}
