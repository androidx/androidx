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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
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
        val state = PagerStateImpl(5, 0.2f) { DefaultPageCount }

        Truth.assertThat(state.currentPage).isEqualTo(5)
        Truth.assertThat(state.currentPageOffsetFraction).isEqualTo(0.2f)

        val currentPage = derivedStateOf { state.currentPage }
        val currentPageOffsetFraction = derivedStateOf { state.currentPageOffsetFraction }

        rule.setContent {
            HorizontalOrVerticalPager(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PagerTestTag)
                    .onSizeChanged { pagerSize = if (vertical) it.height else it.width },
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
    fun initialPageOnPagerState_shouldDisplayThatPageFirst() {
        // Arrange

        // Act
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())

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
            HorizontalOrVerticalPager(
                state = state,
                modifier = Modifier.fillMaxSize()
            ) {
                Page(it)
            }
        }

        // Act
        rule.runOnIdle {
            scope.launch {
                state.scrollToPage(5)
            }
            runBlocking {
                state.scroll {
                    scrollBy(50f)
                }
            }
        }

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
            HorizontalOrVerticalPager(state = state) {
                Page(index = it)
            }
        }
    }

    @Test
    fun calculatePageCountOffset_shouldBeBasedOnCurrentPage() {
        val pageToOffsetCalculations = mutableMapOf<Int, Float>()
        createPager(modifier = Modifier.fillMaxSize(), pageSize = { PageSize.Fixed(20.dp) }) {
            pageToOffsetCalculations[it] = pagerState.getOffsetFractionForPage(it)
            Page(index = it)
        }

        for ((page, offset) in pageToOffsetCalculations) {
            val currentPage = pagerState.currentPage
            val currentPageOffset = pagerState.currentPageOffsetFraction
            Truth.assertThat(offset).isEqualTo((currentPage - page) + currentPageOffset)
        }
    }

    @Test
    fun scrollToPage_usingLaunchedEffect() {

        createPager(additionalContent = {
            LaunchedEffect(pagerState) {
                pagerState.scrollToPage(10)
            }
        })

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10)
    }

    @Test
    fun scrollToPageWithOffset_usingLaunchedEffect() {
        createPager(additionalContent = {
            LaunchedEffect(pagerState) {
                pagerState.scrollToPage(10, 0.4f)
            }
        })

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10, pageOffset = 0.4f)
    }

    @Test
    fun animatedScrollToPage_usingLaunchedEffect() {

        createPager(additionalContent = {
            LaunchedEffect(pagerState) {
                pagerState.animateScrollToPage(10)
            }
        })

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10)
    }

    @Test
    fun animatedScrollToPageWithOffset_usingLaunchedEffect() {

        createPager(additionalContent = {
            LaunchedEffect(pagerState) {
                pagerState.animateScrollToPage(10, 0.4f)
            }
        })

        Truth.assertThat(pagerState.currentPage).isEqualTo(10)
        confirmPageIsInCorrectPosition(10, pageOffset = 0.4f)
    }

    @Test
    fun animatedScrollToPage_viewPortNumberOfPages_usingLaunchedEffect_shouldNotPlaceALlPages() {

        createPager(additionalContent = {
            LaunchedEffect(pagerState) {
                pagerState.animateScrollToPage(DefaultPageCount - 1)
            }
        })

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
        createPager(modifier = Modifier.fillMaxSize(), additionalContent = {
            LaunchedEffect(pagerState) {
                pagerState.scrollToPage(5)
            }
        })

        // Assert
        Truth.assertThat(pagerState.currentPage).isEqualTo(5)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                add(ParamConfig(orientation = orientation))
            }
        }
    }
}