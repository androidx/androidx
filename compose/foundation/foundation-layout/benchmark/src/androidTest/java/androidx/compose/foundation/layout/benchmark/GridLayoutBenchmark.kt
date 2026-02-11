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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Baseline benchmark suite for the [androidx.compose.foundation.layout.Grid] layout. */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalGridApi::class)
class GridLayoutBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val gridCaseFactory = { GridLayoutTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(gridCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(gridCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(gridCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(gridCaseFactory)
    }

    @Test
    fun toggleState_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(gridCaseFactory)
    }

    /**
     * Measures the time taken to measure and layout when the grid configuration changes. This test
     * toggles the grid gap, forcing a full structural invalidation and re-layout without triggering
     * a full recomposition of children.
     */
    @Test
    fun toggleState_measureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(gridCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(gridCaseFactory)
    }

    @Test
    fun toggleState_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(gridCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(gridCaseFactory)
    }
}
