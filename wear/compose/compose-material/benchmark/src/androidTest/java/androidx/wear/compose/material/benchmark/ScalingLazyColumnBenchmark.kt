/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for Wear Compose ScalingLazyColumn.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ScalingLazyColumnBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val scalingLazyColumnCaseFactory = { ScalingLazyColumnTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(scalingLazyColumnCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(scalingLazyColumnCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(scalingLazyColumnCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(scalingLazyColumnCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(scalingLazyColumnCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(scalingLazyColumnCaseFactory)
    }
}

internal class ScalingLazyColumnTestCase : LayeredComposeTestCase() {
    private var itemSizeDp: Dp = 10.dp
    private var defaultItemSpacingDp: Dp = 4.dp

    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    override fun MeasuredContent() {
        ScalingLazyColumn(
            state = rememberScalingLazyListState(),
            modifier = Modifier.requiredSize(
                itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
            ),
        ) {
            items(10) { it ->
                Box(Modifier.requiredSize(itemSizeDp)) {
                    Text(text = "Item $it")
                }
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}