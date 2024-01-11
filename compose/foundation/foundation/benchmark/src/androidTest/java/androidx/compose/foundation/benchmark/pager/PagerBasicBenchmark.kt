/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class PagerBasicBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val fullPageBenchmark = { PagerTestCase() }
    private val multiPageBenchmark = { PagerTestCase(false) }

    @Test
    fun first_pixel_full_page() {
        benchmarkRule.benchmarkToFirstPixel(fullPageBenchmark)
    }

    @Test
    fun first_compose_full_page() {
        benchmarkRule.benchmarkFirstCompose(fullPageBenchmark)
    }

    @Test
    fun first_measure_full_page() {
        benchmarkRule.benchmarkFirstMeasure(fullPageBenchmark)
    }

    @Test
    fun first_layout_full_page() {
        benchmarkRule.benchmarkFirstLayout(fullPageBenchmark)
    }

    @Test
    fun first_draw_full_page() {
        benchmarkRule.benchmarkFirstDraw(fullPageBenchmark)
    }

    @Test
    fun layout_full_page() {
        benchmarkRule.benchmarkLayoutPerf(fullPageBenchmark)
    }

    @Test
    fun draw_full_page() {
        benchmarkRule.benchmarkDrawPerf(fullPageBenchmark)
    }

    @Test
    fun first_pixel_multi_page() {
        benchmarkRule.benchmarkToFirstPixel(multiPageBenchmark)
    }

    @Test
    fun first_compose_multi_page() {
        benchmarkRule.benchmarkFirstCompose(multiPageBenchmark)
    }

    @Test
    fun first_measure_multi_page() {
        benchmarkRule.benchmarkFirstMeasure(multiPageBenchmark)
    }

    @Test
    fun first_layout_multi_page() {
        benchmarkRule.benchmarkFirstLayout(multiPageBenchmark)
    }

    @Test
    fun first_draw_multi_page() {
        benchmarkRule.benchmarkFirstDraw(multiPageBenchmark)
    }

    @Test
    fun layout_multi_page() {
        benchmarkRule.benchmarkLayoutPerf(multiPageBenchmark)
    }

    @Test
    fun draw_multi_page() {
        benchmarkRule.benchmarkDrawPerf(multiPageBenchmark)
    }
}

@OptIn(ExperimentalFoundationApi::class)
class PagerTestCase(val fullPages: Boolean = true) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val pageSize = remember {
            if (fullPages) {
                PageSize.Fill
            } else {
                PageSize.Fixed(50.dp)
            }
        }
        HorizontalPager(
            state = rememberPagerState { 100 },
            modifier = Modifier.size(400.dp),
            pageSize = pageSize
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
