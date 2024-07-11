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

package androidx.compose.ui.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LayoutCoordinatesBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun localPositionOfWithLayer() {
        benchmarkRule.runBenchmarkFor({ LayoutCoordinatesTestCase(true) }) {
            benchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }

            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                testCase.coordinates1.localPositionOf(testCase.coordinates2)
                testCase.coordinates2.localPositionOf(testCase.coordinates1)
            }
        }
    }

    @Test
    fun localPositionOfNoLayer() {
        benchmarkRule.runBenchmarkFor({ LayoutCoordinatesTestCase(false) }) {
            benchmarkRule.runOnUiThread { doFramesUntilNoChangesPending() }

            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                testCase.coordinates1.localPositionOf(testCase.coordinates2)
                testCase.coordinates2.localPositionOf(testCase.coordinates1)
            }
        }
    }

    private class LayoutCoordinatesTestCase(val useLayer: Boolean) : ComposeTestCase {
        lateinit var coordinates1: LayoutCoordinates
        lateinit var coordinates2: LayoutCoordinates

        @Composable
        private fun NestedContent(depth: Int, isFirst: Boolean) {
            if (depth == 0) {
                Box(
                    Modifier.fillMaxSize().onPlaced {
                        if (isFirst) coordinates1 = it else coordinates2 = it
                    }
                )
            } else {
                val modifier = if (useLayer) Modifier.graphicsLayer {} else Modifier
                Box(modifier.padding(1.dp)) { NestedContent(depth - 1, isFirst) }
            }
        }

        @Composable
        override fun Content() {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { NestedContent(10, true) }
                Box(Modifier.weight(1f).fillMaxWidth()) { NestedContent(10, false) }
            }
        }
    }
}
