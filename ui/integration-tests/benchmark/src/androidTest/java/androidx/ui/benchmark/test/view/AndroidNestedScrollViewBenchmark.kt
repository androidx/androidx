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
import androidx.ui.benchmark.toggleStateBenchmarkDraw
import androidx.ui.benchmark.toggleStateBenchmarkLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasure
import androidx.ui.test.cases.view.AndroidNestedScrollViewTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Benchmark that runs [AndroidNestedScrollViewTestCase].
 */
@LargeTest
@RunWith(JUnit4::class)
class AndroidNestedScrollViewBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun first_setContent() {
        benchmarkRule.benchmarkFirstSetContent(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun changeScroll_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun changeScroll_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun changeScroll_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(AndroidNestedScrollViewTestCase())
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(AndroidNestedScrollViewTestCase())
    }
}