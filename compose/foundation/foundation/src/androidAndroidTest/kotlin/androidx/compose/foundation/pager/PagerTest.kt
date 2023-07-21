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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
        val state = PagerStateImpl(0, 0f) { pageCount.value }
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
        val state = PagerStateImpl(0, 0f) { pageCount.value }
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
        val state = PagerStateImpl(0, 0f) { pageCount.value }
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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = AllOrientationsParams
    }
}