/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerContentPaddingTest(paramConfig: ParamConfig) : BasePagerTest(paramConfig) {

    private val PagerTag = "Pager"
    private val PageTag = "page"
    private val ContainerTag = "container"

    private var pageTotalSize: Dp = Dp.Infinity
    private var smallPaddingSize: Dp = Dp.Infinity
    private var pageTotalSizePx = 50f
    private var smallPaddingSizePx = 12f

    @Before
    fun before() {
        with(rule.density) {
            pageTotalSize = pageTotalSizePx.toDp()
            smallPaddingSize = smallPaddingSizePx.toDp()
        }
    }

    @Test
    fun contentPaddingIsApplied() {
        val containerSize = pageTotalSize * 2
        val largePaddingSize = pageTotalSize

        createPager(
            modifier = Modifier.requiredSize(containerSize).testTag(PagerTag),
            contentPadding =
                PaddingValues(mainAxis = largePaddingSize, crossAxis = smallPaddingSize),
            pageCount = { 1 },
            pageSize = { PageSize.Fixed(pageTotalSize) },
        ) {
            Spacer(Modifier.fillMaxCrossAxis().mainAxisSize(pageTotalSize).testTag(PageTag))
        }

        rule
            .onNodeWithTag(PageTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(smallPaddingSize)
            .assertStartPositionInRootIsEqualTo(largePaddingSize)
            .assertCrossAxisSizeIsEqualTo(containerSize - smallPaddingSize * 2)
            .assertMainAxisSizeIsEqualTo(pageTotalSize)

        pagerState.scrollBy(largePaddingSize)

        rule
            .onNodeWithTag(PageTag)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertMainAxisSizeIsEqualTo(pageTotalSize)
    }

    @Test
    fun contentPaddingIsNotAffectingScrollPosition() {
        createPager(
            modifier = Modifier.requiredSize(pageTotalSize * 2).testTag(PagerTag),
            contentPadding = PaddingValues(mainAxis = pageTotalSize),
            pageCount = { 1 },
            pageSize = { PageSize.Fixed(pageTotalSize) },
        ) {
            Spacer(Modifier.fillMaxCrossAxis().mainAxisSize(pageTotalSize).testTag(PageTag))
        }

        pagerState.assertScrollPosition(0, 0.dp)

        pagerState.scrollBy(pageTotalSize)

        pagerState.assertScrollPosition(0, pageTotalSize)
    }

    @Test
    fun scrollForwardItemWithinStartPaddingDisplayed() {
        val padding = pageTotalSize * 1.5f
        createPager(
            modifier = Modifier.requiredSize(padding * 2 + pageTotalSize).testTag(PagerTag),
            contentPadding = PaddingValues(mainAxis = padding),
            pageCount = { 4 },
            pageSize = { PageSize.Fixed(pageTotalSize) },
        ) {
            Spacer(Modifier.requiredSize(pageTotalSize).testTag(it.toString()))
        }

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(padding)
        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize + padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize * 2 + padding)

        pagerState.scrollBy(padding)

        pagerState.assertScrollPosition(1, padding - pageTotalSize)

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize * 2)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(pageTotalSize * 3)
    }

    @Test
    fun scrollBackwardItemWithinStartPaddingDisplayed() {
        val padding = pageTotalSize * 1.5f
        createPager(
            modifier = Modifier.requiredSize(padding * 2 + pageTotalSize).testTag(PagerTag),
            contentPadding = PaddingValues(mainAxis = padding),
            pageCount = { 4 },
            pageSize = { PageSize.Fixed(pageTotalSize) },
        ) {
            Spacer(Modifier.requiredSize(pageTotalSize).testTag(it.toString()))
        }

        pagerState.scrollBy(pageTotalSize * 3)
        pagerState.scrollBy(-pageTotalSize * 1.5f)

        pagerState.assertScrollPosition(1, pageTotalSize * 0.5f)

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(pageTotalSize * 1.5f - padding)
        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize * 2.5f - padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize * 3.5f - padding)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(pageTotalSize * 4.5f - padding)
    }

    @Test
    fun scrollForwardTillTheEnd() {
        val padding = pageTotalSize * 1.5f
        createPager(
            modifier = Modifier.requiredSize(padding * 2 + pageTotalSize).testTag(PagerTag),
            contentPadding = PaddingValues(mainAxis = padding),
            pageCount = { 4 },
            pageSize = { PageSize.Fixed(pageTotalSize) },
        ) {
            Spacer(Modifier.requiredSize(pageTotalSize).testTag(it.toString()))
        }

        pagerState.scrollBy(pageTotalSize * 3)

        pagerState.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize - padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize * 2 - padding)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(pageTotalSize * 3 - padding)

        // there are no space to scroll anymore, so it should change nothing
        pagerState.scrollBy(10.dp)

        pagerState.assertScrollPosition(3, 0.dp)

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize - padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize * 2 - padding)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(pageTotalSize * 3 - padding)
    }

    @Test
    fun scrollForwardTillTheEndAndABitBack() {
        val padding = pageTotalSize * 1.5f
        createPager(
            modifier = Modifier.requiredSize(padding * 2 + pageTotalSize).testTag(PagerTag),
            contentPadding = PaddingValues(mainAxis = padding),
            pageCount = { 4 },
            pageSize = { PageSize.Fixed(pageTotalSize) },
        ) {
            Spacer(Modifier.requiredSize(pageTotalSize).testTag(it.toString()))
        }

        pagerState.scrollBy(pageTotalSize * 3)
        pagerState.scrollBy(-pageTotalSize / 2)

        pagerState.assertScrollPosition(2, pageTotalSize / 2)

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize * 1.5f - padding)
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize * 2.5f - padding)
        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(pageTotalSize * 3.5f - padding)
    }

    @Test
    fun contentPaddingAndWrapContent() {
        rule.setContent {
            val state = rememberPagerState { 1 }
            Box(modifier = Modifier.testTag(ContainerTag)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding =
                        PaddingValues(
                            beforeContentCrossAxis = 2.dp,
                            beforeContent = 4.dp,
                            afterContentCrossAxis = 6.dp,
                            afterContent = 8.dp,
                        ),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Spacer(Modifier.requiredSize(pageTotalSize).testTag(PageTag))
                }
            }
        }

        rule
            .onNodeWithTag(PageTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(2.dp)
            .assertStartPositionInRootIsEqualTo(4.dp)
            .assertCrossAxisSizeIsEqualTo(pageTotalSize)
            .assertMainAxisSizeIsEqualTo(pageTotalSize)

        rule
            .onNodeWithTag(ContainerTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(pageTotalSize + 2.dp + 6.dp)
            .assertMainAxisSizeIsEqualTo(pageTotalSize + 4.dp + 8.dp)
    }

    @Test
    fun contentPaddingAndNoContent() {
        rule.setContent {
            val state = rememberPagerState { 0 }
            Box(modifier = Modifier.testTag(ContainerTag)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding =
                        PaddingValues(
                            beforeContentCrossAxis = 2.dp,
                            beforeContent = 4.dp,
                            afterContentCrossAxis = 6.dp,
                            afterContent = 8.dp,
                        ),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {}
            }
        }

        rule
            .onNodeWithTag(ContainerTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(8.dp)
            .assertMainAxisSizeIsEqualTo(12.dp)
    }

    @Test
    fun contentPaddingAndZeroSizedItem() {
        rule.setContent {
            val state = rememberPagerState { 1 }
            Box(modifier = Modifier.testTag(ContainerTag)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding =
                        PaddingValues(
                            beforeContentCrossAxis = 2.dp,
                            beforeContent = 4.dp,
                            afterContentCrossAxis = 6.dp,
                            afterContent = 8.dp,
                        ),
                    pageSize = PageSize.Fixed(0.dp),
                ) {
                    Box {}
                }
            }
        }

        rule
            .onNodeWithTag(ContainerTag)
            .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
            .assertStartPositionInRootIsEqualTo(0.dp)
            .assertCrossAxisSizeIsEqualTo(8.dp)
            .assertMainAxisSizeIsEqualTo(12.dp)
    }

    @Test
    fun contentPaddingAndReverseLayout() {
        val topPadding = pageTotalSize * 2
        val bottomPadding = pageTotalSize / 2
        val listSize = pageTotalSize * 3
        createPager(
            reverseLayout = true,
            modifier = Modifier.requiredSize(listSize),
            contentPadding =
                PaddingValues(beforeContent = topPadding, afterContent = bottomPadding),
            pageSize = { PageSize.Fixed(pageTotalSize) },
            pageCount = { 3 },
        ) { page ->
            Box(Modifier.requiredSize(pageTotalSize).testTag("$page"))
        }

        rule
            .onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(listSize - bottomPadding - pageTotalSize)
        rule
            .onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(listSize - bottomPadding - pageTotalSize * 2)
        // Partially visible.
        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(-pageTotalSize / 2)

        // Scroll to the top.
        pagerState.scrollBy(pageTotalSize * 2.5f)

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(topPadding)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    @Test
    fun contentLargePaddingAndReverseLayout() {
        val topPadding = pageTotalSize * 2
        val bottomPadding = pageTotalSize * 2
        val listSize = pageTotalSize * 3
        createPager(
            reverseLayout = true,
            modifier = Modifier.requiredSize(listSize),
            contentPadding =
                PaddingValues(beforeContent = topPadding, afterContent = bottomPadding),
            pageSize = { PageSize.Fixed(pageTotalSize) },
            pageCount = { 3 },
            prefetchEnabled = false,
        ) { page ->
            Box(Modifier.requiredSize(pageTotalSize).testTag("$page"))
        }

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(0.dp)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertDoesNotExist()

        // Scroll to the top.
        pagerState.scrollBy(pageTotalSize * 5f)

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(topPadding)
        // Shouldn't be visible
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    @Test
    fun overscrollWithContentPadding() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 2 }
            Box(
                modifier = Modifier.testTag(ContainerTag).size(pageTotalSize + smallPaddingSize * 2)
            ) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = smallPaddingSize),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").fillMaxSize())
                }
            }
        }

        rule
            .onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize)
            .assertMainAxisSizeIsEqualTo(pageTotalSize)

        rule
            .onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize + pageTotalSize)
            .assertMainAxisSizeIsEqualTo(pageTotalSize)

        rule.runOnIdle {
            runBlocking {
                // pageSizePx is the maximum offset, plus if we overscroll the content padding
                // the layout mechanism will decide the page 0 is not needed until we start
                // filling the over scrolled gap.
                (state as ScrollableState).scrollBy(pageTotalSizePx + smallPaddingSizePx * 1.5f)
            }
        }

        rule
            .onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize)
            .assertMainAxisSizeIsEqualTo(pageTotalSize)

        rule
            .onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(smallPaddingSize - pageTotalSize)
            .assertMainAxisSizeIsEqualTo(pageTotalSize)
    }

    @Test
    fun totalPaddingLargerParentSize_initialState() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }.also { it.prefetchingEnabled = false }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(pageTotalSize)

        rule.onNodeWithTag("1").assertDoesNotExist()

        rule.runOnIdle {
            state.assertScrollPosition(0, 0.dp)
            state.assertVisibleItems(0 to 0.dp)
            state.assertLayoutInfoOffsetRange(-pageTotalSize, pageTotalSize * 0.5f)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollByPadding() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.scrollBy(pageTotalSize)

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize)

        rule.onNodeWithTag("2").assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(1, 0.dp)
            state.assertVisibleItems(0 to -pageTotalSize, 1 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollToLastItem() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.runScrollToPage(3)

        rule.onNodeWithTag("1").assertDoesNotExist()

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(pageTotalSize)

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(2 to -pageTotalSize, 3 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollToLastItemByDelta() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.scrollBy(pageTotalSize * 3)

        rule.onNodeWithTag("1").assertIsNotDisplayed()

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(pageTotalSize)

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(2 to -pageTotalSize, 3 to 0.dp)
        }
    }

    @Test
    fun totalPaddingLargerParentSize_scrollTillTheEnd() {
        // the whole end content padding is displayed
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.scrollBy(pageTotalSize * 4.5f)

        rule.onNodeWithTag("2").assertIsNotDisplayed()

        rule.onNodeWithTag("3").assertStartPositionInRootIsEqualTo(-pageTotalSize * 0.5f)

        rule.runOnIdle {
            state.assertScrollPosition(3, pageTotalSize * 1.5f)
            state.assertVisibleItems(3 to -pageTotalSize * 1.5f)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_initialState() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize * 2),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        rule.onNodeWithTag("0").assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(0, 0.dp)
            state.assertVisibleItems(0 to 0.dp)
            state.assertLayoutInfoOffsetRange(-pageTotalSize * 2, -pageTotalSize * 0.5f)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollByPadding() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize * 2),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.scrollBy(pageTotalSize * 2)

        rule.onNodeWithTag("0").assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(pageTotalSize)

        rule.onNodeWithTag("2").assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(2, 0.dp)
            state.assertVisibleItems(0 to -pageTotalSize * 2, 1 to -pageTotalSize, 2 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollToLastItem() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize * 2),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.runScrollToPage(3)

        rule.onNodeWithTag("0").assertIsNotDisplayed()

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize)

        rule.onNodeWithTag("3").assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(1 to -pageTotalSize * 2, 2 to -pageTotalSize, 3 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollToLastItemByDelta() {
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize * 2),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.scrollBy(pageTotalSize * 3)

        rule.onNodeWithTag("0").assertIsNotDisplayed()

        rule.onNodeWithTag("1").assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2").assertStartPositionInRootIsEqualTo(pageTotalSize)

        rule.onNodeWithTag("3").assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(3, 0.dp)
            state.assertVisibleItems(1 to -pageTotalSize * 2, 2 to -pageTotalSize, 3 to 0.dp)
        }
    }

    @Test
    fun eachPaddingLargerParentSize_scrollTillTheEnd() {
        // only the end content padding is displayed
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            Box(modifier = Modifier.testTag(ContainerTag).size(pageTotalSize * 1.5f)) {
                HorizontalOrVerticalPager(
                    state = state,
                    contentPadding = PaddingValues(mainAxis = pageTotalSize * 2),
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").size(pageTotalSize))
                }
            }
        }

        state.scrollBy(
            pageTotalSize * 1.5f + // container size
                pageTotalSize * 2 + // start padding
                pageTotalSize * 3 // all pages
        )

        rule.onNodeWithTag("3").assertIsNotDisplayed()

        rule.runOnIdle {
            state.assertScrollPosition(3, pageTotalSize * 3.5f)
            state.assertVisibleItems(3 to -pageTotalSize * 3.5f)
        }
    }

    @Test
    fun unevenPaddingWithRtl() {
        val padding = PaddingValues(start = 20.dp, end = 8.dp)
        lateinit var state: PagerState
        rule.setContent {
            state = rememberPagerState { 4 }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalOrVerticalPager(
                    modifier = Modifier.testTag("list").mainAxisSize(pageTotalSize * 2),
                    state = state,
                    contentPadding = padding,
                    pageSize = PageSize.Fixed(pageTotalSize),
                ) {
                    Box(Modifier.testTag("$it").background(Color.Red).size(pageTotalSize)) {
                        BasicText("$it")
                    }
                }
            }
        }

        if (vertical) {
            rule
                .onNodeWithTag("0")
                .assertStartPositionInRootIsEqualTo(0.dp)
                .assertCrossAxisStartPositionInRootIsEqualTo(
                    padding.calculateLeftPadding(LayoutDirection.Rtl)
                )

            rule.onNodeWithTag("list").assertWidthIsEqualTo(28.dp + pageTotalSize)
        } else {
            rule
                .onNodeWithTag("0")
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
                .assertStartPositionInRootIsEqualTo(
                    // list width - pageSize - padding
                    pageTotalSize * 2 -
                        pageTotalSize -
                        padding.calculateRightPadding(LayoutDirection.Rtl)
                )
        }

        state.scrollBy(pageTotalSize * 4)

        if (vertical) {
            rule
                .onNodeWithTag("3")
                .assertStartPositionInRootIsEqualTo(pageTotalSize)
                .assertCrossAxisStartPositionInRootIsEqualTo(
                    padding.calculateLeftPadding(LayoutDirection.Rtl)
                )
        } else {
            rule
                .onNodeWithTag("3")
                .assertCrossAxisStartPositionInRootIsEqualTo(0.dp)
                .assertStartPositionInRootIsEqualTo(
                    padding.calculateLeftPadding(LayoutDirection.Rtl)
                )
        }
    }

    private fun PagerState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
        rule.waitForIdle()
    }

    private fun PagerState.runScrollToPage(page: Int) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) { scrollToPage(page) }
    }

    private fun PagerState.assertScrollPosition(index: Int, offset: Dp) =
        with(rule.density) {
            assertThat(firstVisiblePage).isEqualTo(index)
            assertThat(firstVisiblePageOffset.toDp().value).isWithin(0.5f).of(offset.value)
        }

    private fun PagerState.assertLayoutInfoOffsetRange(from: Dp, to: Dp) =
        with(rule.density) {
            assertThat(layoutInfo.viewportStartOffset to layoutInfo.viewportEndOffset)
                .isEqualTo(from.roundToPx() to to.roundToPx())
        }

    private fun PagerState.assertVisibleItems(vararg expected: Pair<Int, Dp>) =
        with(rule.density) {
            assertThat(layoutInfo.visiblePagesInfo.map { it.index to it.offset })
                .isEqualTo(expected.map { it.first to it.second.roundToPx() })
        }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun params() = AllOrientationsParams
    }
}
