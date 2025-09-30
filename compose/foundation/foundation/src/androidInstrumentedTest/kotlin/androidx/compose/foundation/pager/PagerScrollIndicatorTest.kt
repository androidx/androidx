/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.contextmenu.test.assertNotNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerScrollIndicatorTest(val config: ParamConfig) : BasePagerTest(config) {

    private val PagerTestTag = "PagerTestTag"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Array<Any> =
            arrayOf(
                ParamConfig(Orientation.Vertical, layoutDirection = LayoutDirection.Ltr),
                ParamConfig(Orientation.Horizontal, layoutDirection = LayoutDirection.Ltr),
                ParamConfig(Orientation.Horizontal, layoutDirection = LayoutDirection.Rtl),
            )
    }

    @Test
    fun scrollIndicatorState_emptyPager() {
        val pagerSizePx = 100
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }
        createPager(
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageCount = { 0 },
        )

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.contentSize).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_whenContentFits_pageSizeFixed() {
        val pageCount = 2
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 4 // pager size more than the content
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }

        createPager(
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
        )

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.contentSize)
                .isEqualTo(fixedPageSizePx * pageCount)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_pageSizeFill_contentDoesNotFit() {
        val pageCount = 2
        val pagerSizePx = 100
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }

        createPager(
            pageCount = { pageCount },
            modifier =
                Modifier.requiredSize(pagerSize)
                    .testTag(PagerTestTag), // Ensure pager has a defined size
            pageSize = { PageSize.Fill }, // page size will be viewport size
        )

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.contentSize)
                .isEqualTo(pagerSizePx * pageCount)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_pageSizeFixed_contentDoesNotFit() {
        val pageCount = 4
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 2 // pager size less than the content
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }

        createPager(
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
        )

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.contentSize)
                .isEqualTo(fixedPageSizePx * pageCount)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_withPageSpacing() {
        val pageCount = 4
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 2 // pager size less than the content
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }
        val pageSpacingPx = 20
        val pageSpacing = with(rule.density) { pageSpacingPx.toDp() }

        createPager(
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
            pageSpacing = pageSpacing,
        )

        val expectedContentSize = ((fixedPageSizePx + pageSpacingPx) * pageCount) - pageSpacingPx

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_withContentPadding() {
        val pageCount = 4
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 2 // pager size less than the content
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }
        val startContentPaddingPx = 20
        val startContentPadding = with(rule.density) { startContentPaddingPx.toDp() }
        val endContentPaddingPx = 30
        val endContentPadding = with(rule.density) { endContentPaddingPx.toDp() }

        val contentPadding =
            if (config.orientation == Orientation.Vertical) {
                PaddingValues(top = startContentPadding, bottom = endContentPadding)
            } else {
                PaddingValues(start = startContentPadding, end = endContentPadding)
            }

        createPager(
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
            contentPadding = contentPadding,
        )

        val expectedContentSize =
            (fixedPageSizePx * pageCount) + startContentPaddingPx + endContentPaddingPx

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.contentSize).isEqualTo(expectedContentSize)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_withReverseLayout() {
        val pageCount = 4
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 2 // pager size less than the content
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }

        createPager(
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
            reverseLayout = true,
        )

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(0)
            assertThat(pagerState.scrollIndicatorState?.contentSize)
                .isEqualTo(fixedPageSizePx * pageCount)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_onScrollToPage() {
        val pageCount = 6
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 2 // pager size less than the content
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }
        val targetPageIndex = 3

        createPager(
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
        )

        rule.runOnIdle { runBlocking { pagerState.scrollToPage(targetPageIndex) } }

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset)
                .isEqualTo(targetPageIndex * fixedPageSizePx)
            assertThat(pagerState.scrollIndicatorState?.contentSize)
                .isEqualTo(fixedPageSizePx * pageCount)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_scrollByOffset() {
        val pageCount = 6
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 2 // pager size less than the content
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }
        val scrollAmountPx = 70

        createPager(
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
        )

        rule.waitForIdle()

        runBlocking { pagerState.scrollBy(scrollAmountPx.toFloat()) }

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(scrollAmountPx)
            assertThat(pagerState.scrollIndicatorState?.contentSize)
                .isEqualTo(fixedPageSizePx * pageCount)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }

    @Test
    fun scrollIndicatorState_withLargePageCount_valuesCoercedToIntMax() {
        val pageCount = Int.MAX_VALUE
        val fixedPageSizePx = 50
        val fixedPageSize = with(rule.density) { fixedPageSizePx.toDp() }
        val pagerSizePx = fixedPageSizePx * 2
        val pagerSize = with(rule.density) { pagerSizePx.toDp() }
        val initialPageIndex = pageCount - 10

        createPager(
            initialPage = initialPageIndex,
            pageCount = { pageCount },
            modifier = Modifier.requiredSize(pagerSize).testTag(PagerTestTag),
            pageSize = { PageSize.Fixed(fixedPageSize) },
        )

        rule.runOnIdle {
            assertNotNull(pagerState.scrollIndicatorState)
            assertThat(pagerState.scrollIndicatorState?.scrollOffset).isEqualTo(Int.MAX_VALUE)
            assertThat(pagerState.scrollIndicatorState?.contentSize).isEqualTo(Int.MAX_VALUE)
            assertThat(pagerState.scrollIndicatorState?.viewportSize).isEqualTo(pagerSizePx)
        }
    }
}
