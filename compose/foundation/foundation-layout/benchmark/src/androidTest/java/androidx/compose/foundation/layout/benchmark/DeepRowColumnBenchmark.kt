/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark that runs [DeepRowColumnTestCase]. The purpose of this benchmark is to measure compose,
 * measure, and layout performance of Row and Column, which are extremely common layouts.
 */
@LargeTest
@RunWith(Parameterized::class)
class DeepRowColumnBenchmark(
    private val depth: Int,
    private val breadth: Int,
    private val weight: Boolean,
    private val align: Boolean,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "depth={0}_breadth={1}_weight={2}_align={3}")
        fun initParameters(): Array<Any> =
            arrayOf(
                arrayOf(1, 3, true, false),
                arrayOf(1, 3, false, true),
                arrayOf(1, 3, false, false),
                arrayOf(1, 100, false, false),
                arrayOf(100, 1, false, false),
            )
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactory = { DeepRowColumnTestCase(weight, align, depth, breadth) }

    @Test
    fun compose() {
        benchmarkRule.benchmarkFirstCompose(caseFactory)
    }

    @Test
    fun measure() {
        benchmarkRule.benchmarkFirstMeasure(caseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkFirstLayout(caseFactory)
    }
}
