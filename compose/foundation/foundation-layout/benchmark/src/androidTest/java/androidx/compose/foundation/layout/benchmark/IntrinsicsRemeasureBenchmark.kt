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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark that tests intrinsic measurement and incremental remeasurement in a [Row] with
 * [IntrinsicSize.Min] height containing pure `foundation-layout` and `ui` primitives ([Box],
 * [Modifier.padding], [Modifier.fillMaxHeight], [Modifier.graphicsLayer], [Spacer]).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class IntrinsicsRemeasureBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactory = { IntrinsicsRowTestCase(childrenCount = 10) }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(caseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(caseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(caseFactory)
    }

    @Test
    fun toggleOneChildSize_measure_incremental() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            caseFactory = caseFactory,
            invalidateLayoutCache = false,
        )
    }

    @Test
    fun toggleOneChildSize_composeMeasureLayout_incremental() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = caseFactory,
            invalidateLayoutCache = false,
        )
    }
}

/**
 * A [Row] of [childrenCount] children. Only the first child changes size on [toggleState]; the
 * remaining children keep a fixed height so the benchmark measures how much of the row is
 * remeasured when a single child invalidates the intrinsic height.
 */
private class IntrinsicsRowTestCase(private val childrenCount: Int) :
    LayeredComposeTestCase(), ToggleableTestCase {

    private var toggledHeight by mutableStateOf(ToggledHeightA)

    @Composable
    override fun MeasuredContent() {
        Row(Modifier.height(IntrinsicSize.Min)) {
            ChildItem(height = toggledHeight)
            repeat(childrenCount - 1) { ChildItem(height = FixedHeight) }
        }
    }

    override fun toggleState() {
        toggledHeight = if (toggledHeight == ToggledHeightA) ToggledHeightB else ToggledHeightA
    }

    private companion object {
        val ToggledHeightA = 24.dp
        val ToggledHeightB = 32.dp
        val FixedHeight = 40.dp
    }
}

@Composable
private fun ChildItem(height: Dp) {
    Box(Modifier.padding(4.dp).fillMaxHeight().graphicsLayer()) {
        Box(Modifier.padding(4.dp)) { Spacer(Modifier.size(width = 24.dp, height = height)) }
    }
}
