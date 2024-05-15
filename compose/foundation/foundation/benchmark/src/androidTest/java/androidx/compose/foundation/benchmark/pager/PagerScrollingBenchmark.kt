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

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.benchmark.lazy.LazyBenchmarkTestCase
import androidx.compose.foundation.benchmark.lazy.LazyItem
import androidx.compose.foundation.benchmark.lazy.toggleStateBenchmark
import androidx.compose.foundation.benchmark.lazy.toggleStateBenchmarkDraw
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerScrollingBenchmark(private val testCase: PagerScrollingTestCase) {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun scrollProgrammatically_noNewPages() {
        benchmarkRule.toggleStateBenchmark {
            PagerRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    @Test
    fun scrollProgrammatically_newPageComposed() {
        benchmarkRule.toggleStateBenchmark {
            PagerRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    @Test
    fun scrollProgrammatically_noNewPages_withoutKeys() {
        benchmarkRule.toggleStateBenchmark {
            PagerRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                useKeys = false,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    @Test
    fun scrollProgrammatically_newPageComposed_withoutKeys() {
        benchmarkRule.toggleStateBenchmark {
            PagerRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                useKeys = false,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    @Test
    fun scrollViaPointerInput_noNewPages() {
        benchmarkRule.toggleStateBenchmark {
            PagerRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    @Test
    fun scrollViaPointerInput_newPageComposed() {
        benchmarkRule.toggleStateBenchmark {
            PagerRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    @Test
    fun drawAfterScroll_noNewPages() {
        // this test makes sense only when run on the Android version which supports RenderNodes
        // as this tests how efficiently we move RenderNodes.
        Assume.assumeTrue(supportsRenderNode || supportsMRenderNode)
        benchmarkRule.toggleStateBenchmarkDraw {
            PagerRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    @Test
    fun drawAfterScroll_newPageComposed() {
        // this test makes sense only when run on the Android version which supports RenderNodes
        // as this tests how efficiently we move RenderNodes.
        Assume.assumeTrue(supportsRenderNode || supportsMRenderNode)
        benchmarkRule.toggleStateBenchmarkDraw {
            PagerRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                beyondBoundsPageCount = testCase.beyondBoundsPageCount
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<PagerScrollingTestCase> =
            arrayOf(
                PagerScrollingTestCase(
                    "HorizontalPager_WithoutExtraPages",
                    isVertical = false,
                    beyondBoundsPageCount = 0,
                    HorizontalPagerContent
                ),
                PagerScrollingTestCase(
                    "HorizontalPager_WithExtraPages",
                    isVertical = false,
                    beyondBoundsPageCount = 1,
                    HorizontalPagerContent
                ),
                PagerScrollingTestCase(
                    "VerticalPager_WithoutExtraPages",
                    isVertical = true,
                    beyondBoundsPageCount = 0,
                    VerticalPagerContent
                ),
                PagerScrollingTestCase(
                    "VerticalPager_WithExtraPages",
                    isVertical = true,
                    beyondBoundsPageCount = 1,
                    VerticalPagerContent
                )
            )

        // Copied from AndroidComposeTestCaseRunner
        private val supportsRenderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        private val supportsMRenderNode = Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}

@OptIn(ExperimentalFoundationApi::class)
data class PagerScrollingTestCase(
    private val name: String,
    val isVertical: Boolean,
    val beyondBoundsPageCount: Int,
    val content: @Composable PagerRemeasureTestCase.(
        PagerState,
        useKeys: Boolean,
        beyondBoundsPageCount: Int
    ) -> Unit
) {
    override fun toString(): String {
        return name
    }
}

@OptIn(ExperimentalFoundationApi::class)
class PagerRemeasureTestCase(
    val addNewItemOnToggle: Boolean,
    val content: @Composable PagerRemeasureTestCase.(
        PagerState,
        useKeys: Boolean,
        beyondBoundsPageCount: Int
    ) -> Unit,
    val isVertical: Boolean,
    val usePointerInput: Boolean = false,
    val useKeys: Boolean = true,
    val beyondBoundsPageCount: Int,
    val pageCount: Int = 100
) : LazyBenchmarkTestCase(isVertical, usePointerInput) {

    val pages = List(pageCount) { LazyItem(it) }

    private lateinit var pagerState: PagerState

    override fun beforeToggleCheck() {
        Assert.assertEquals(0, pagerState.currentPage)
        Assert.assertEquals(0.0f, pagerState.currentPageOffsetFraction)
    }

    override fun afterToggleCheck() {
        Assert.assertEquals(0, pagerState.currentPage)
        val pageSizeWithSpacing =
            (pagerState.layoutInfo.pageSpacing + pagerState.layoutInfo.pageSize)
        val fraction = scrollingHelper.scrollAmount / pageSizeWithSpacing.toFloat()
        Assert.assertEquals(fraction, pagerState.currentPageOffsetFraction)
    }

    override suspend fun programmaticScroll(amount: Int) {
        pagerState.scrollBy(amount.toFloat())
    }

    override fun setUp() {
        runBlocking {
            pagerState.scrollToPage(0, 0.0f)
        }
    }

    override fun tearDown() {
        runBlocking {
            try {
                pagerState.scroll { }
            } catch (_: CancellationException) {
                // prevent snapping
            }
        }
    }

    @Composable
    override fun Content() {
        val scrollBy = if (addNewItemOnToggle) {
            with(LocalDensity.current) { 5.dp.roundToPx() }
        } else {
            1
        }
        InitializeScrollHelper(scrollAmount = scrollBy)
        pagerState = rememberPagerState { pageCount }
        content(pagerState, useKeys, beyondBoundsPageCount)
    }
}

val NoOpInfoProvider = object : SnapLayoutInfoProvider {
    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float =
        0.0f

    override fun calculateSnapOffset(velocity: Float): Float {
        return 0f
    }
}

val VerticalPagerContent: @Composable PagerRemeasureTestCase.(
    state: PagerState,
    useKeys: Boolean,
    beyondBoundsPageCount: Int
) -> Unit =
    { state, useKeys, beyondBoundsPageCount ->
        val flingBehavior = rememberSnapFlingBehavior(
            snapLayoutInfoProvider = NoOpInfoProvider
        )
        VerticalPager(
            state = state, modifier = Modifier
                .requiredHeight(400.dp)
                .fillMaxWidth(),
            key = if (useKeys) {
                { pages[it].index }
            } else {
                null
            },
            pageSize = PageSize.Fixed(30.dp),
            beyondViewportPageCount = beyondBoundsPageCount,
            flingBehavior = flingBehavior
        ) {
            Box(Modifier.fillMaxSize())
        }
    }

val HorizontalPagerContent: @Composable PagerRemeasureTestCase.(
    state: PagerState,
    useKeys: Boolean,
    beyondBoundsPageCount: Int
) -> Unit =
    { state, useKeys, beyondBoundsPageCount ->
        val flingBehavior = rememberSnapFlingBehavior(
            snapLayoutInfoProvider = NoOpInfoProvider
        )
        HorizontalPager(
            state = state, modifier = Modifier
                .requiredWidth(400.dp)
                .fillMaxHeight(),
            key = if (useKeys) {
                { pages[it].index }
            } else {
                null
            },
            pageSize = PageSize.Fixed(30.dp),
            beyondViewportPageCount = beyondBoundsPageCount,
            flingBehavior = flingBehavior
        ) {
            Box(Modifier.fillMaxSize())
        }
    }
