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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.xr.glimmer.benchmark.pager

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.benchmark.sendIndirectSwipeEvents
import androidx.xr.glimmer.pager.GlimmerHorizontalPager
import androidx.xr.glimmer.pager.GlimmerPagerState
import androidx.xr.glimmer.testutils.createGlimmerRule
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class GlimmerPagerBenchmark {

    @get:Rule(0) val benchmarkRule = ComposeBenchmarkRule()

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun glimmerHorizontalPager_firstCompose() {
        benchmarkRule.benchmarkFirstCompose { GlimmerPagerTestCase() }
    }

    @Test
    fun glimmerHorizontalPager_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel { GlimmerPagerTestCase() }
    }

    @Test
    fun glimmerHorizontalPager_indirectScroll_firstFrame() {
        with(benchmarkRule) {
            runBenchmarkFor({ GlimmerPagerTestCase() }) {
                runOnUiThread { doFramesUntilNoChangesPending() }

                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        getTestCase().requestScrollToPage(page = 0, pageOffsetFraction = 0f)
                        doFramesUntilNoChangesPending()
                        assertEquals(0, getTestCase().focusedPageIndex)
                    }

                    getTestCase().scrollOnePageForwardWithIndirectEvents()
                    doFrame()
                }
            }
        }
    }

    @Test
    fun glimmerHorizontalPager_focusChange_firstFrame() {
        with(benchmarkRule) {
            runBenchmarkFor({ GlimmerPagerTestCase() }) {
                runOnUiThread { doFramesUntilNoChangesPending() }

                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        // Scroll to an offset that is close to, but below, the 0.5 threshold. At
                        // this offset, the focus shouldn't change yet.
                        getTestCase().requestScrollToPage(page = 0, pageOffsetFraction = 0.49f)
                        // Do only a single frame to start the benchmark without triggering the snap
                        // animation, which would otherwise snap "Page 0" back to start position.
                        doFrame()
                        assertEquals(0, getTestCase().focusedPageIndex)
                    }

                    getTestCase().scrollOnePageForwardWithIndirectEvents()
                    // Do first frame after focus change getting triggered.
                    doFrame()

                    runWithMeasurementDisabled { assertEquals(1, getTestCase().focusedPageIndex) }
                }
            }
        }
    }
}

private class GlimmerPagerTestCase : LayeredComposeTestCase() {

    var focusedPageIndex = -1

    private val pagerState = GlimmerPagerState(pageCount = { 10 })
    private lateinit var view: ViewRootForTest

    @Composable
    override fun ContentWrappers(content: @Composable (() -> Unit)) {
        view = LocalView.current as ViewRootForTest
        GlimmerTheme { content() }
    }

    @Composable
    override fun MeasuredContent() {
        GlimmerHorizontalPager(state = pagerState, modifier = Modifier.size(PagerSize)) { page ->
            Box(
                modifier =
                    Modifier.onFocusChanged { if (it.hasFocus) focusedPageIndex = page }
                        .focusable()
                        .fillMaxSize()
            )
        }
    }

    fun requestScrollToPage(page: Int, pageOffsetFraction: Float) {
        runBlocking { pagerState.requestScrollToPage(page, pageOffsetFraction) }
    }

    fun scrollOnePageForwardWithIndirectEvents() {
        view.sendIndirectSwipeEvents(
            from = Offset.Zero,
            to = Offset(pagerState.layoutInfo.pageSize.toFloat() / 4f, 0f),
            axis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
        )
    }
}

private val PagerSize = 300.dp
