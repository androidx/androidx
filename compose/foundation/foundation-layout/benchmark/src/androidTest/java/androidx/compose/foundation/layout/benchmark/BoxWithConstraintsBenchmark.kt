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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkReuseFor
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * A suite of benchmarks for [BoxWithConstraints] / subcomposition behavior. In this benchmark we're
 * comparing simple Box vs BoxWithConstraints. We're also checking the performance of
 * [BoxWithConstraints] in the context of an app's layout depending on available space compared to
 * just using a theoretical CompositionLocal that contains screen width. This allows measuring the
 * performance impact of subcomposition on first composition and recomposition of a relatively
 * complex screen.
 */
class BoxWithConstraintsBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun no_boxwithconstraints_inner_recompose() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout({ NoWithConstraintsTestCase() })
    }

    @Test
    fun boxwithconstraints_inner_recompose() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout({ BoxWithConstraintsTestCase() })
    }

    @Test
    fun boxwithconstraints_app_benchmarkToFirstPixel() {
        benchmarkRule.benchmarkToFirstPixel { BoxWithConstraintsAppTestCase() }
    }

    @Test
    fun boxwithconstraints_app_benchmarkReuse() {
        benchmarkRule.benchmarkReuseFor { BoxWithConstraintsAppTestCase().MeasuredContent() }
    }

    @Test
    fun boxwithconstraints_app_toggleStateBenchmarkComposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout({ BoxWithConstraintsAppTestCase() })
    }

    @Test
    fun compositionlocal_app_benchmarkToFirstPixel() {
        benchmarkRule.benchmarkToFirstPixel { CompositionLocalAppTestCase() }
    }

    @Test
    fun compositionlocal_app_toggleStateBenchmarkComposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout({ CompositionLocalAppTestCase() })
    }
}
