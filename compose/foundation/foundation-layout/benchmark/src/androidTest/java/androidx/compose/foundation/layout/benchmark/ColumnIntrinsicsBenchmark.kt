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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.ui.Modifier
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Benchmark that runs [RectsInColumnTestCase] and tests performance for intrinsic measurements. */
@LargeTest
@RunWith(Parameterized::class)
class ColumnIntrinsicsBenchmark(
    private val modifierDebugName: String,
    private val modifier: Modifier,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Array<Any>> =
            arrayOf(
                arrayOf("minWidth", Modifier.requiredWidth(IntrinsicSize.Min)),
                arrayOf("maxWidth", Modifier.requiredWidth(IntrinsicSize.Max)),
                arrayOf("minHeight", Modifier.requiredHeight(IntrinsicSize.Min)),
                arrayOf("maxHeight", Modifier.requiredHeight(IntrinsicSize.Max)),
            )
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val rectsInColumnCaseFactory = { RectsInColumnTestCase(10, modifier) }

    @Test
    fun first_measure_withIntrinsicSize() {
        benchmarkRule.benchmarkFirstMeasure(rectsInColumnCaseFactory)
    }

    @Test
    fun layout_withIntrinsicSize() {
        benchmarkRule.benchmarkLayoutPerf(rectsInColumnCaseFactory)
    }
}
