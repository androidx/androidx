/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.benchmark.test.view

import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.benchmarkDrawPerf
import androidx.ui.benchmark.benchmarkFirstDraw
import androidx.ui.benchmark.benchmarkFirstLayout
import androidx.ui.benchmark.benchmarkFirstMeasure
import androidx.ui.benchmark.benchmarkFirstSetContent
import androidx.ui.benchmark.benchmarkLayoutPerf
import androidx.ui.test.cases.view.AndroidCheckboxesInLinearLayoutTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark that runs [AndroidCheckboxesInLinearLayoutTestCase].
 */
@LargeTest
@RunWith(Parameterized::class)
class AndroidCheckboxesInLinearLayoutBenchmark(private val numberOfCheckboxes: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun first_setContent() {
        benchmarkRule.benchmarkFirstSetContent(
            AndroidCheckboxesInLinearLayoutTestCase(numberOfCheckboxes))
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(
            AndroidCheckboxesInLinearLayoutTestCase(numberOfCheckboxes))
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(
            AndroidCheckboxesInLinearLayoutTestCase(numberOfCheckboxes))
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(
            AndroidCheckboxesInLinearLayoutTestCase(numberOfCheckboxes))
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(
            AndroidCheckboxesInLinearLayoutTestCase(numberOfCheckboxes))
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(
            AndroidCheckboxesInLinearLayoutTestCase(numberOfCheckboxes))
    }
}