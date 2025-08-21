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

package androidx.compose.foundation.pager

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.calculateDistanceToDesiredSnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerStateNonGestureScrollingTest(val config: ParamConfig) : BasePagerTest(config) {
    @Test
    fun pagerStateNotAttached_shouldReturnDefaultValues_andChangeAfterAttached() = runBlocking {
        // Arrange
        val state = PagerState(5, 0.2f) { DefaultPageCount }

        Truth.assertThat(state.currentPage).isEqualTo(5)
        Truth.assertThat(state.currentPageOffsetFraction).isEqualTo(0.2f)

        val currentPage = derivedStateOf { state.currentPage }
        val currentPageOffsetFraction = derivedStateOf { state.currentPageOffsetFraction }

        rule.setContent {
            HorizontalOrVerticalPager(
                state = state,
                modifier =
                    Modifier.fillMaxSize().testTag(PagerTestTag).onSizeChanged {
                        pagerSize = if (vertical) it.height else it.width
                    },
                pageSize = PageSize.Fill,
                reverseLayout = config.reverseLayout,
                pageSpacing = config.pageSpacing,
                contentPadding = config.mainAxisContentPadding,
            ) {
                Page(index = it)
            }
        }

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToPage(state.currentPage + 1)
        }

        rule.runOnIdle {
            Truth.assertThat(currentPage.value).isEqualTo(6)
            Truth.assertThat(currentPageOffsetFraction.value).isEqualTo(0.0f)
        }
    }

    @Test
    fun pageSizeIsZero_offsetFractionShouldNotBeNan() {
        // Arrange
        val zeroPageSize =
            object : PageSize {
                override fun Density.calculateMainAxisPageSize(
                    availableSpace: Int,
                    pageSpacing: Int,
                ): Int {
                    return 0
                }
            }

        rule.setContent {
            pagerState = rememberPagerState { DefaultPageCount }
            HorizontalOrVerticalPager(
                state = pagerState,
                modifier =
                    Modifier.size(0.dp).testTag(PagerTestTag).onSizeChanged {
                        pagerSize = if (vertical) it.height else it.width
                    },
                pageSize = zeroPageSize,
                reverseLayout = config.reverseLayout,
                pageSpacing = config.pageSpacing,
                contentPadding = config.mainAxisContentPadding,
            ) {
                Page(index = it)
            }
        }

        rule.runOnIdle { Truth.assertThat(pagerState.currentPageOffsetFraction).isNotNaN() }
    }

    @Test
    fun initialPageOnPagerState_shouldDisplayThatPageFirst() {
        // Arrange

        // Act
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize(), prefetchEnabled = false)

        // Assert
        rule.onNodeWithTag("4").assertDoesNotExist()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertDoesNotExist()
        confirmPageIsInCorrectPosition(pagerState.currentPage)
    }

    @Test
    fun testStateRestoration() {
        // Arrange
        val tester = StateRestorationTester(rule)
        lateinit var state: PagerState
        tester.setContent {
            state = rememberPagerState(pageCount = { DefaultPageCount })
            scope = rememberCoroutineScope()
            HorizontalOrVerticalPager(state = state, modifier = Modifier.fillMaxSize()) { Page(it) }
        }

        // Act
        rule.runOnIdle { scope.launch { state.scrollToPage(5, 0.2f) } }

        val previousPage = state.currentPage
        val previousOffset = state.currentPageOffsetFraction
        tester.emulateSavedInstanceStateRestore()

        // Assert
        rule.runOnIdle {
            Truth.assertThat(state.currentPage).isEqualTo(previousPage)
            Truth.assertThat(state.currentPageOffsetFraction).isEqualTo(previousOffset)
        }
    }

    @Test
    fun currentPageOffsetFraction_shouldNeverBeNan() {
        rule.setContent {
            val state = rememberPagerState(pageCount = { 10 })
            // Read state in composition, should never be Nan
            assertFalse { state.currentPageOffsetFraction.isNaN() }
            HorizontalOrVerticalPager(state = state) { Page(index = it) }
        }
    }

    @Test
    fun currentPage_pagerWithKeys_shouldBeTheSameAfterDatasetUpdate() {
        // Arrange
        class Data(val id: Int, val item: String)

        val data = mutableListOf(Data(3, "A"), Data(4, "B"), Data(5, "C"))

        val extraData = mutableListOf(Data(0, "D"), Data(1, "E"), Data(2, "F"))

        val dataset = mutableStateOf<List<Data>>(data)

        createPager(
            modifier = Modifier.fillMaxSize(),
            initialPage = 1,
            key = { dataset.value[it].id },
            pageCount = { dataset.value.size },
            pageContent = {
                val item = dataset.value[it]
                Box(modifier = Modifier.fillMaxSize().testTag(item.item))
            },
        )

        Truth.assertThat(dataset.value[pagerState.currentPage].item).isEqualTo("B")

        rule.runOnIdle {
            dataset.value = extraData + data // add new data
        }

        rule.waitForIdle()
        Truth.assertThat(pagerState.pageCount).isEqualTo(6) // all data is present
        rule.onNodeWithTag("B").assertIsDisplayed() // scroll kept
        Truth.assertThat(pagerState.currentPage).isEqualTo(4)
        Truth.assertThat(pagerState.currentPageOffsetFraction).isEqualTo(0.0f)
    }

    @Test
    fun getOffsetDistanceInPages_shouldBeBasedOnCurrentPage() {
        val pageToOffsetCalculations = mutableMapOf<Int, Float>()
        createPager(modifier = Modifier.fillMaxSize(), pageSize = { PageSize.Fixed(20.dp) }) {
            pageToOffsetCalculations[it] = pagerState.getOffsetDistanceInPages(it)
            Page(index = it)
        }

        for ((page, offset) in pageToOffsetCalculations) {
            val currentPage = pagerState.currentPage
            val currentPageOffset = pagerState.currentPageOffsetFraction
            Truth.assertThat(offset).isEqualTo((page - currentPage) - currentPageOffset)
        }
    }

    @Test
    fun scrollToPage_usingLaunchedEffect() {

        createPager(
            additionalContent = { LaunchedEffect(pagerState) { pagerState.scrollToPage(10) } }
        )

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10)
    }

    @Test
    fun scrollToPageWithOffset_usingLaunchedEffect() {
        createPager(
            additionalContent = { LaunchedEffect(pagerState) { pagerState.scrollToPage(10, 0.4f) } }
        )

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10, pageOffset = 0.4f)
    }

    @Test
    fun animatedScrollToPage_usingLaunchedEffect() {

        createPager(
            additionalContent = {
                LaunchedEffect(pagerState) { pagerState.animateScrollToPage(10) }
            }
        )

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10)
    }

    @Test
    fun animatedScrollToPage_emptyPager_shouldNotReact() {
        createPager(
            pageCount = { 0 },
            additionalContent = {
                LaunchedEffect(pagerState) { pagerState.animateScrollToPage(10) }
            },
        )
        Truth.assertThat(pagerState.currentPage).isEqualTo(0)
    }

    @Test
    fun animatedScrollToPageWithOffset_usingLaunchedEffect() {

        createPager(
            additionalContent = {
                LaunchedEffect(pagerState) { pagerState.animateScrollToPage(10, 0.4f) }
            }
        )

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10, pageOffset = 0.4f)
    }

    @Test
    fun animatedScrollToPage_viewPortNumberOfPages_usingLaunchedEffect_shouldNotPlaceALlPages() {

        createPager(
            additionalContent = {
                LaunchedEffect(pagerState) { pagerState.animateScrollToPage(DefaultPageCount - 1) }
            }
        )

        // Assert
        rule.runOnIdle {
            Truth.assertThat(pagerState.currentPage).isEqualTo(DefaultPageCount - 1)
            Truth.assertThat(placed).doesNotContain(DefaultPageCount / 2 - 1)
            Truth.assertThat(placed).doesNotContain(DefaultPageCount / 2)
            Truth.assertThat(placed).doesNotContain(DefaultPageCount / 2 + 1)
        }
        confirmPageIsInCorrectPosition(pagerState.currentPage)
    }

    @Test
    fun scrollTo_beforeFirstLayout_shouldWaitForStateAndLayoutSetting() {
        // Arrange

        rule.mainClock.autoAdvance = false

        // Act
        createPager(
            modifier = Modifier.fillMaxSize(),
            additionalContent = { LaunchedEffect(pagerState) { pagerState.scrollToPage(5) } },
        )

        // Assert
        Truth.assertThat(pagerState.currentPage).isEqualTo(5)
    }

    @Test
    fun updateCurrentPage_shouldUpdateCurrentPageImmediately() {
        createPager(modifier = Modifier.fillMaxSize())

        Truth.assertThat(pagerState.currentPage).isEqualTo(0)

        rule.runOnUiThread { scope.launch { with(pagerState) { scroll { updateCurrentPage(5) } } } }

        rule.runOnIdle { Truth.assertThat(pagerState.currentPage).isEqualTo(5) }
    }

    @Test
    fun updateCurrentPage_shouldUpdateCurrentPageOffsetFractionImmediately() {
        createPager(modifier = Modifier.fillMaxSize())

        Truth.assertThat(pagerState.currentPage).isEqualTo(0)

        rule.runOnUiThread {
            scope.launch { with(pagerState) { scroll { updateCurrentPage(5, 0.3f) } } }
        }

        rule.runOnIdle {
            Truth.assertThat(pagerState.currentPageOffsetFraction).isWithin(0.01f).of(0.3f)
        }
    }

    @Test
    fun updateTargetPage_shouldUpdateTargetPageImmediately_andResetIfNotMoved() {
        createPager(modifier = Modifier.fillMaxSize())

        Truth.assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage)

        rule.mainClock.autoAdvance = false

        rule.runOnUiThread {
            scope.launch {
                with(pagerState) {
                    scroll {
                        updateTargetPage(5)
                        delay(1_000L) // simulate an animation
                    }
                }
            }
        }

        rule.mainClock.advanceTimeByFrame() // pump a frame
        Truth.assertThat(pagerState.targetPage).isEqualTo(5) // target page changed
        rule.mainClock.advanceTimeBy(2_000L) // scroll block finished but we didn't move
        // target page reset
        Truth.assertThat(pagerState.currentPage).isEqualTo(0)
        Truth.assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage)
    }

    @Test
    fun currentPage_shouldUpdateWithSnapPositionInLayout() {
        // snap position is 200dp from edge of Pager
        val customSnapPosition =
            object : SnapPosition {
                override fun position(
                    layoutSize: Int,
                    itemSize: Int,
                    beforeContentPadding: Int,
                    afterContentPadding: Int,
                    itemIndex: Int,
                    itemCount: Int,
                ): Int {
                    return with(rule.density) { 200.dp.roundToPx() }
                }
            }

        createPager(
            modifier = Modifier.fillMaxSize(),
            snapPosition = customSnapPosition,
            pageSize = { PageSize.Fixed(100.dp) },
        )

        onPager().performTouchInput {
            swipeLeft()
            swipeLeft()
            swipeLeft()
        }

        with(pagerState.layoutInfo) {
            val viewPortSize = if (vertical) viewportSize.height else viewportSize.width
            assertThat(pagerState.currentPage)
                .isEqualTo(
                    visiblePagesInfo
                        .fastMaxBy {
                            -abs(
                                calculateDistanceToDesiredSnapPosition(
                                    mainAxisViewPortSize = viewPortSize,
                                    beforeContentPadding = beforeContentPadding,
                                    afterContentPadding = afterContentPadding,
                                    itemSize = pageSize,
                                    itemOffset = it.offset,
                                    itemIndex = it.index,
                                    snapPosition = customSnapPosition,
                                    itemCount = pagerState.pageCount,
                                )
                            )
                        }
                        ?.index
                )
        }
    }

    @Test
    fun snapPositionInLayout_startToStart_currentPageShouldBeCloserToStartOfLayout() {
        createPager(
            modifier = Modifier.fillMaxSize(),
            snapPosition = SnapPosition.Start,
            pageSize = { PageSize.Fixed(100.dp) },
        )

        onPager().performTouchInput {
            if (vertical) {
                swipeUp()
            } else {
                swipeLeft()
            }
        }

        rule.runOnIdle {
            // check we moved
            Truth.assertThat(pagerState.firstVisiblePage).isNotEqualTo(0)

            Truth.assertThat(pagerState.currentPage)
                .isEqualTo(pagerState.layoutInfo.visiblePagesInfo.first().index)

            // offset should be zero
            Truth.assertThat(pagerState.layoutInfo.visiblePagesInfo.first().offset).isEqualTo(0)
        }
    }

    @Test
    fun snapPositionInLayout_centerToCenter_currentPageShouldBeCloserToMiddleOfLayout() {
        createPager(
            modifier = Modifier.size(50.dp),
            snapPosition = SnapPosition.Center,
            pageSize = { PageSize.Fixed(10.dp) },
        )

        onPager().performTouchInput {
            if (vertical) {
                swipeUp()
            } else {
                swipeLeft()
            }
        }

        rule.runOnIdle {
            // find page whose offset is closest to the centre
            val candidatePage =
                pagerState.layoutInfo.visiblePagesInfo.fastMaxBy {
                    -(abs(it.offset - pagerSize / 2))
                }

            // check we moved
            Truth.assertThat(pagerState.firstVisiblePage).isNotEqualTo(0)
            Truth.assertThat(pagerState.currentPage).isEqualTo(candidatePage?.index)
        }
    }

    @Test
    fun snapPositionInLayout_endToEnd_currentPageShouldBeCloserToEndOfLayout() {
        createPager(
            modifier = Modifier.size(50.dp),
            snapPosition = SnapPosition.End,
            pageSize = { PageSize.Fixed(10.dp) },
        )

        onPager().performTouchInput {
            if (vertical) {
                swipeUp()
            } else {
                swipeLeft()
            }
        }

        rule.runOnIdle {
            // check we moved
            Truth.assertThat(pagerState.firstVisiblePage).isNotEqualTo(0)
            Truth.assertThat(pagerState.currentPage)
                .isEqualTo(pagerState.layoutInfo.visiblePagesInfo.last().index)
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromStart() {
        val pageSizePx = 100
        val pageSizeDp = with(rule.density) { pageSizePx.toDp() }
        createPager(
            modifier = Modifier.size(pageSizeDp * 1.5f),
            pageSize = { PageSize.Fixed(pageSizeDp) },
        )

        val delta = (pageSizePx / 3f).roundToInt()

        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                // small enough scroll to not cause any new items to be composed or old ones
                // disposed.
                pagerState.scrollBy(delta.toFloat())
            }
            rule.runOnIdle {
                assertThat(pagerState.firstVisiblePageOffset).isEqualTo(delta)
                assertThat(pagerState.canScrollForward).isTrue()
                assertThat(pagerState.canScrollBackward).isTrue()
            }
            // and scroll back to start
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.scrollBy(-delta.toFloat())
            }
            rule.runOnIdle {
                assertThat(pagerState.canScrollForward).isTrue()
                assertThat(pagerState.canScrollBackward).isFalse()
            }
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromEnd() {
        val pageSizePx = 100
        val pageSizeDp = with(rule.density) { pageSizePx.toDp() }
        createPager(
            modifier = Modifier.size(pageSizeDp * 1.5f),
            pageSize = { PageSize.Fixed(pageSizeDp) },
        )
        val delta = -(pageSizePx / 3f).roundToInt()
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                // scroll to the end of the list.
                pagerState.scrollToPage(DefaultPageCount)
                // small enough scroll to not cause any new items to be composed or old ones
                // disposed.
                pagerState.scrollBy(delta.toFloat())
            }
            rule.runOnIdle {
                assertThat(pagerState.canScrollForward).isTrue()
                assertThat(pagerState.canScrollBackward).isTrue()
            }
            // and scroll back to the end
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.scrollBy(-delta.toFloat())
            }
            rule.runOnIdle {
                assertThat(pagerState.canScrollForward).isFalse()
                assertThat(pagerState.canScrollBackward).isTrue()
            }
        }
    }

    @Test
    fun canScrollForwardAndBackward_afterSmallScrollFromEnd_withContentPadding() {
        val pageSizePx = 100
        val pageSizeDp = with(rule.density) { pageSizePx.toDp() }
        val afterContentPaddingDp = with(rule.density) { 2.toDp() }
        createPager(
            modifier = Modifier.size(pageSizeDp * 1.5f),
            pageSize = { PageSize.Fixed(pageSizeDp) },
            contentPadding = PaddingValues(afterContent = afterContentPaddingDp),
        )

        val delta = -(pageSizePx / 3f).roundToInt()
        runBlocking {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                // scroll to the end of the list.
                pagerState.scrollToPage(DefaultPageCount)

                assertThat(pagerState.canScrollForward).isFalse()
                assertThat(pagerState.canScrollBackward).isTrue()

                // small enough scroll to not cause any new pages to be composed or old ones
                // disposed.
                pagerState.scrollBy(delta.toFloat())
            }
            rule.runOnIdle {
                assertThat(pagerState.canScrollForward).isTrue()
                assertThat(pagerState.canScrollBackward).isTrue()
            }
            // and scroll back to the end
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.scrollBy(-delta.toFloat())
            }
            rule.runOnIdle {
                assertThat(pagerState.canScrollForward).isFalse()
                assertThat(pagerState.canScrollBackward).isTrue()
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() =
            mutableListOf<ParamConfig>().apply {
                for (orientation in TestOrientation) {
                    add(ParamConfig(orientation = orientation))
                }
            }
    }
}
