/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerTest(val config: ParamConfig) : BasePagerTest(config) {

    @Before
    fun setUp() {
        placed.clear()
    }

    @Test
    fun userScrollEnabledIsOff_shouldNotAllowGestureScroll() {
        // Arrange

        createPager(
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        )

        // Act
        onPager().performTouchInput { swipeWithVelocityAcrossMainAxis(1000f) }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(0)
        }

        confirmPageIsInCorrectPosition(0, 0)
    }

    @Test
    fun userScrollEnabledIsOff_shouldAllowAnimationScroll() {
        // Arrange

        createPager(
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        )

        // Act
        rule.runOnIdle {
            scope.launch {
                pagerState.animateScrollToPage(5)
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
        }
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun userScrollEnabledIsOn_shouldAllowGestureScroll() {
        // Arrange
        createPager(
            initialPage = 5,
            userScrollEnabled = true,
            modifier = Modifier.fillMaxSize()
        )

        onPager().performTouchInput { swipeWithVelocityAcrossMainAxis(1000f) }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isNotEqualTo(5)
        }
        confirmPageIsInCorrectPosition(pagerState.currentPage)
    }

    @Test
    fun pageCount_pagerOnlyContainsGivenPageCountItems() {
        // Arrange

        // Act
        createPager(modifier = Modifier.fillMaxSize())

        // Assert
        repeat(DefaultPageCount) {
            rule.onNodeWithTag("$it").assertIsDisplayed()
            rule.runOnIdle {
                scope.launch {
                    pagerState.scroll {
                        scrollBy(pagerSize.toFloat())
                    }
                }
            }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("$DefaultPageCount").assertDoesNotExist()
    }

    @Test
    fun mutablePageCount_assertPagesAreChangedIfCountIsChanged() {
        // Arrange
        val pageCount = mutableStateOf(2)
        createPager(
            pageCount = { pageCount.value },
            modifier = Modifier.fillMaxSize(),
        )

        rule.onNodeWithTag("3").assertDoesNotExist()

        // Act
        pageCount.value = DefaultPageCount
        rule.waitForIdle()

        // Assert
        repeat(DefaultPageCount) {
            rule.onNodeWithTag("$it").assertIsDisplayed()
            rule.runOnIdle {
                scope.launch {
                    pagerState.scroll {
                        scrollBy(pagerSize.toFloat())
                    }
                }
            }
            rule.waitForIdle()
        }
    }

    @Test
    fun pageCount_readBeforeCompositionIsAccurate() {
        // Arrange
        val pageCount = mutableStateOf(2)
        val state = PagerState(0, 0f) { pageCount.value }
        assertThat(state.pageCount).isEqualTo(pageCount.value)
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

        rule.runOnIdle { pageCount.value = 5 }
        assertThat(state.pageCount).isEqualTo(pageCount.value)
    }

    @Test
    fun pageCount_changeInCountDoesNotCausePagerToRecompose() {
        // Arrange
        var recomposeCount = 0
        val pageCount = mutableStateOf(2)
        val state = PagerState(0, 0f) { pageCount.value }
        assertThat(state.pageCount).isEqualTo(pageCount.value)

        rule.setContent {
            HorizontalOrVerticalPager(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PagerTestTag)
                    .composed {
                        recomposeCount++
                        Modifier
                    },
                pageSize = PageSize.Fill,
                reverseLayout = config.reverseLayout,
                pageSpacing = config.pageSpacing,
                contentPadding = config.mainAxisContentPadding,
            ) {
                Page(index = it)
            }
        }

        assertThat(recomposeCount).isEqualTo(1)
        rule.runOnIdle { pageCount.value = 5 } // change count
        assertThat(state.pageCount).isEqualTo(pageCount.value)
        assertThat(recomposeCount).isEqualTo(1)
    }

    @Test
    fun pageCountDecreased_currentPageIsAdjustedAccordingly() {
        // Arrange
        val pageCount = mutableStateOf(5)
        val state = PagerState(0, 0f) { pageCount.value }
        assertThat(state.pageCount).isEqualTo(pageCount.value)

        rule.setContent {
            HorizontalOrVerticalPager(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PagerTestTag),
                pageSize = PageSize.Fill,
                reverseLayout = config.reverseLayout,
                pageSpacing = config.pageSpacing,
                contentPadding = config.mainAxisContentPadding,
            ) {
                Page(index = it)
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToPage(3)
            }
        }
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(3) }
        pageCount.value = 2 // change count, less than current page
        rule.runOnIdle {
            assertThat(state.pageCount).isEqualTo(pageCount.value)
            assertThat(state.currentPage).isEqualTo(1) // last page
        }
    }

    @Test
    fun pageCount_canBeMaxInt() {
        // Arrange

        // Act
        createPager(modifier = Modifier.fillMaxSize(), pageCount = { Int.MAX_VALUE })

        // Assert
        rule.runOnIdle {
            scope.launch {
                pagerState.scrollToPage(Int.MAX_VALUE)
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("${Int.MAX_VALUE - 1}").assertIsDisplayed()
    }

    @Test
    fun keyLambdaShouldUpdateWhenDatasetChanges() {
        lateinit var pagerState: PagerState
        val listA = mutableStateOf(listOf(1))

        @Composable
        fun MyComposable(data: List<Int>) {
            pagerState = rememberPagerState { data.size }
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pager"),
                state = pagerState,
                key = { data[it] }
            ) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                )
            }
        }

        rule.setContent {
            MyComposable(listA.value)
        }

        rule.runOnIdle {
            listA.value = listOf(1, 2)
        }

        assertThat(listA.value.size).isEqualTo(2)

        rule.onNodeWithTag("pager").performTouchInput {
            swipeLeft()
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(1)
        }
    }

    @Test
    fun pagerStateChange_flingBehaviorShouldRecreate() {
        var previousFlingBehavior: FlingBehavior? = null
        var latestFlingBehavior: FlingBehavior? = null
        val stateHolder = mutableStateOf(PagerState(0, 0.0f) { 10 })
        rule.setContent {
            HorizontalOrVerticalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PagerTestTag),
                state = stateHolder.value,
                pageSize = PageSize.Fill,
                flingBehavior = PagerDefaults.flingBehavior(state = stateHolder.value).also {
                    latestFlingBehavior = it
                    if (previousFlingBehavior == null) {
                        previousFlingBehavior = it
                    }
                }
            ) {
                Page(index = it)
            }
        }

        rule.runOnIdle {
            stateHolder.value = PagerState(0, 0.0f) { 20 }
        }

        rule.waitForIdle()
        assertThat(previousFlingBehavior).isNotEqualTo(latestFlingBehavior)
    }

    @Test
    fun pagerCreation_sumOfPageSizeIsSmallerThanPager_makeSurePagesAreAlignedToStartTop() {
        // arrange and act
        createPager(
            modifier = Modifier.size(500.dp),
            pageSize = { PageSize.Fixed(100.dp) },
            pageCount = { 3 })

        confirmPageIsInCorrectPosition(0, pageToVerifyPosition = 0)
        confirmPageIsInCorrectPosition(0, pageToVerifyPosition = 1)
        confirmPageIsInCorrectPosition(0, pageToVerifyPosition = 2)
    }

    @Test
    fun snapPositionChanges_shouldReLayoutPages() {
        val snapPosition = mutableStateOf<SnapPosition>(SnapPosition.Start)
        rule.setContent {
            HorizontalOrVerticalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PagerTestTag)
                    .onSizeChanged { pagerSize = if (vertical) it.height else it.width },
                state = rememberPagerState(initialPage = 5) {
                    40
                }.also {
                    pagerState = it
                },
                pageSize = PageSize.Fixed(250.dp), // make sure pages bleed in the layout
                snapPosition = snapPosition.value
            ) {
                Page(index = it)
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.first().index).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.first().offset).isEqualTo(0)
        }

        rule.runOnUiThread {
            snapPosition.value = SnapPosition.End
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.last().index).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.last().offset)
                .isEqualTo(pagerSize - pageSize)
        }
    }

    @Test
    fun pagerSizeChanges_shouldReLayoutPagesAccordingToSnapPosition() {
        val pagerSizeDp = mutableStateOf(500.dp)
        rule.setContent {
            HorizontalOrVerticalPager(
                modifier = Modifier
                    .mainAxisSize(pagerSizeDp.value)
                    .testTag(PagerTestTag)
                    .onSizeChanged { pagerSize = if (vertical) it.height else it.width },
                state = rememberPagerState(initialPage = 5) {
                    40
                }.also {
                    pagerState = it
                },
                pageSize = PageSize.Fixed(100.dp),
                snapPosition = SnapPosition.Center // snap position that depends on pager size
            ) {
                Page(index = it)
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            val page = pagerState.layoutInfo.visiblePagesInfo.first { it.index == 5 }
            // page is centered
            assertThat(page.offset).isEqualTo(pagerSize / 2 - pageSize / 2)
        }

        val previousPagerSize = pagerSize
        rule.runOnUiThread {
            pagerSizeDp.value = 300.dp
        }

        // make sure we continue in the same place
        rule.runOnIdle {
            assertThat(pagerSize).isNotEqualTo(previousPagerSize) // make sure pager size changed
            assertThat(pagerState.currentPage).isEqualTo(5)
            val page = pagerState.layoutInfo.visiblePagesInfo.first { it.index == 5 }
            // page is centered
            assertThat(page.offset).isEqualTo(pagerSize / 2 - pageSize / 2)
        }
    }

    @Test
    fun pageSizeChanges_shouldReLayoutPagesAccordingToSnapPosition() {
        val pageSizeDp = mutableStateOf(PageSize.Fixed(200.dp))
        rule.setContent {
            HorizontalOrVerticalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PagerTestTag)
                    .onSizeChanged { pagerSize = if (vertical) it.height else it.width },
                state = rememberPagerState(initialPage = 5) {
                    40
                }.also {
                    pagerState = it
                },
                pageSize = pageSizeDp.value,
                snapPosition = SnapPosition.End // snap position that depends on page size
            ) {
                Page(index = it)
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.last().index).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.last().offset)
                .isEqualTo(pagerSize - pageSize)
        }

        val previousPageSize = pageSize
        rule.runOnUiThread {
            pageSizeDp.value = PageSize.Fixed(250.dp)
        }

        // make sure we continue in the same place
        rule.runOnIdle {
            assertThat(pageSize).isNotEqualTo(previousPageSize) // make sure page size changed
            assertThat(pagerState.currentPage).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.last().index).isEqualTo(5)
            assertThat(pagerState.layoutInfo.visiblePagesInfo.last().offset)
                .isEqualTo(pagerSize - pageSize)
        }
    }

    @Test
    fun flingOnUnattachedPager_shouldNotCrash() {
        val pageSize = object : PageSize {
            override fun Density.calculateMainAxisPageSize(
                availableSpace: Int,
                pageSpacing: Int
            ): Int = 0
        }

        createPager(pageSize = { pageSize }, modifier = Modifier.fillMaxSize())

        onPager().performTouchInput { swipeWithVelocityAcrossMainAxis(1000f) }

        rule.onNodeWithTag("0").isNotDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = AllOrientationsParams
    }
}
