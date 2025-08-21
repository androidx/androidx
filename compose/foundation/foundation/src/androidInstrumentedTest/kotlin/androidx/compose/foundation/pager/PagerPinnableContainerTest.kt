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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.list.assertIsNotPlaced
import androidx.compose.foundation.lazy.list.assertIsPlaced
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.PinnableContainer.PinnedHandle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.collections.removeFirst as removeFirstKt
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class PagerPinnableContainerTest {

    @get:Rule val rule = createComposeRule()

    private var pinnableContainer: PinnableContainer? = null

    private var pageSizeDp = Dp.Unspecified

    private val composed = mutableSetOf<Int>()
    private lateinit var pagerState: PagerState

    @Before
    fun setup() {
        pageSizeDp = with(rule.density) { 10.toDp() }
    }

    @Composable
    fun PageWithEffect(index: Int) {
        Box(Modifier.size(pageSizeDp).padding(2.dp).background(Color.Black).testTag("$index"))
        DisposableEffect(index) {
            composed.add(index)
            onDispose { composed.remove(index) }
        }
    }

    @Test
    fun pinnedPageIsComposedAndPlacedWhenScrolledOut() {
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state =
                    rememberPagerState { 100 }
                        .also {
                            pagerState = it
                            it.prefetchingEnabled = false
                        },
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 1) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(composed).contains(1)
            runBlocking { pagerState.scrollToPage(3) }
        }

        rule.waitUntil {
            // not visible pages were disposed
            !composed.contains(0)
        }

        rule.runOnIdle {
            // page 1 is still pinned
            assertThat(composed).contains(1)
        }

        rule.onNodeWithTag("1").assertExists().assertIsNotDisplayed().assertIsPlaced()
    }

    @Test
    fun pagesBetweenPinnedAndCurrentVisibleAreNotComposed() {
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state =
                    rememberPagerState { 100 }
                        .also {
                            pagerState = it
                            it.prefetchingEnabled = false
                        },
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 1) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle { runBlocking { pagerState.scrollToPage(4) } }

        rule.waitUntil {
            // not visible pages were disposed
            !composed.contains(0)
        }

        rule.runOnIdle {
            assertThat(composed).doesNotContain(0)
            assertThat(composed).contains(1)
            assertThat(composed).doesNotContain(2)
            assertThat(composed).doesNotContain(3)
            assertThat(composed).contains(4)
        }
    }

    @Test
    fun pinnedPageAfterVisibleOnesIsComposedAndPlacedWhenScrolledOut() {
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state =
                    rememberPagerState { 100 }
                        .also {
                            pagerState = it
                            it.prefetchingEnabled = false
                        },
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 4) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        rule.runOnIdle { runBlocking { pagerState.scrollToPage(4) } }

        rule.waitUntil {
            // wait for not visible pages to be disposed
            !composed.contains(1)
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
            assertThat(composed).contains(5)
        }

        rule.runOnIdle { runBlocking { pagerState.scrollToPage(0) } }

        rule.waitUntil {
            // wait for not visible pages to be disposed
            !composed.contains(5)
        }

        rule.runOnIdle {
            assertThat(composed).contains(0)
            assertThat(composed).contains(1)
            assertThat(composed).doesNotContain(2)
            assertThat(composed).doesNotContain(3)
            assertThat(composed).contains(4)
            assertThat(composed).doesNotContain(5)
        }
    }

    @Test
    fun pinnedPageCanBeUnpinned() {
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state =
                    rememberPagerState { 100 }
                        .also {
                            pagerState = it
                            it.prefetchingEnabled = false
                        },
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 1) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        val handle = rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle { runBlocking { pagerState.scrollToPage(3) } }

        rule.waitUntil {
            // wait for not visible pages to be disposed
            !composed.contains(0)
        }

        rule.runOnIdle { handle.release() }

        rule.waitUntil {
            // wait for unpinned page to be disposed
            !composed.contains(1)
        }

        rule.onNodeWithTag("1").assertIsNotPlaced()
    }

    @Test
    fun pinnedPageIsStillPinnedWhenReorderedAndNotVisibleAnymore() {
        var list by mutableStateOf(listOf(0, 1, 2, 3, 4))
        // Arrange.
        rule.setContent { Pager(list, 2, 3) }

        rule.runOnIdle {
            assertThat(composed).containsExactly(0, 1, 2)
            requireNotNull(pinnableContainer).pin()
        }

        rule.runOnIdle { list = listOf(0, 3, 4, 1, 2) }

        rule.waitUntil {
            // wait for not visible page to be disposed
            !composed.contains(1)
        }

        rule.runOnIdle {
            assertThat(composed).containsExactly(0, 3, 4, 2) // 2 is pinned
        }

        rule.onNodeWithTag("2").assertIsPlaced()
    }

    @Composable
    fun Pager(dataset: List<Int>, pinnedPage: Int, visiblePages: Int) {
        HorizontalPager(
            state = rememberPagerState { dataset.size }.also { it.prefetchingEnabled = false },
            modifier = Modifier.width(pageSizeDp * visiblePages),
            pageSize = PageSize.Fixed(pageSizeDp),
            key = { dataset[it] },
        ) { page ->
            if (dataset[page] == pinnedPage) {
                pinnableContainer = LocalPinnableContainer.current
            }
            PageWithEffect(dataset[page])
        }
    }

    @Test
    fun unpinnedWhenPagerStateChanges() {
        var state by
            mutableStateOf(
                PagerState(currentPage = 2, currentPageOffsetFraction = 0f, pageCount = { 100 })
                    .also { it.prefetchingEnabled = false }
            )
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state = state,
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 2) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(composed).contains(3)
            runBlocking { state.scrollToPage(0) }
        }

        rule.waitUntil {
            // wait for not visible page to be disposed
            !composed.contains(3)
        }

        rule.runOnIdle {
            assertThat(composed).contains(2)
            state =
                PagerState(currentPage = 0, currentPageOffsetFraction = 0f, pageCount = { 100 })
                    .also { it.prefetchingEnabled = false }
        }

        rule.waitUntil {
            // wait for pinned page to be disposed
            !composed.contains(2)
        }

        rule.onNodeWithTag("2").assertIsNotPlaced()
    }

    @Test
    fun pinAfterPagerStateChange() {
        var state by
            mutableStateOf(
                PagerState(currentPage = 0, currentPageOffsetFraction = 0f, pageCount = { 100 })
                    .also { it.prefetchingEnabled = false }
            )
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state = state,
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 0) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        rule.runOnIdle {
            state =
                PagerState(currentPage = 0, currentPageOffsetFraction = 0f, pageCount = { 100 })
                    .also { it.prefetchingEnabled = false }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(composed).contains(1)
            runBlocking { state.scrollToPage(2) }
        }

        rule.waitUntil {
            // wait for not visible page to be disposed
            !composed.contains(1)
        }

        rule.runOnIdle { assertThat(composed).contains(0) }
    }

    @Test
    fun pagesArePinnedBasedOnGlobalIndexes() {
        // Arrange.
        lateinit var state: PagerState
        rule.setContent {
            HorizontalPager(
                state =
                    rememberPagerState(initialPage = 3) { 100 }
                        .also {
                            state = it
                            it.prefetchingEnabled = false
                        },
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 3) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(composed).contains(4)
            runBlocking { state.scrollToPage(6) }
        }

        rule.waitUntil {
            // wait for not visible page to be disposed
            !composed.contains(4)
        }

        rule.runOnIdle { assertThat(composed).contains(3) }

        rule.onNodeWithTag("3").assertExists().assertIsNotDisplayed().assertIsPlaced()
    }

    @Test
    fun pinnedPageIsRemovedWhenNotVisible() {
        lateinit var state: PagerState
        var pageCount by mutableStateOf(10)
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state =
                    rememberPagerState(initialPage = 3) { pageCount }
                        .also {
                            state = it
                            it.prefetchingEnabled = false
                        },
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 3) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        rule.runOnIdle {
            requireNotNull(pinnableContainer).pin()
            assertThat(composed).contains(4)
            runBlocking { state.scrollToPage(0) }
        }

        rule.waitUntil {
            // wait for not visible page to be disposed
            !composed.contains(4)
        }

        rule.runOnIdle { pageCount = 3 }

        rule.waitUntil {
            // wait for pinned page to be disposed
            !composed.contains(3)
        }

        rule.onNodeWithTag("3").assertIsNotPlaced()
    }

    @Test
    fun pinnedPageIsRemovedWhenVisible() {
        var pages by mutableStateOf(listOf(0, 1, 2))
        // Arrange.
        rule.setContent { Pager(dataset = pages, pinnedPage = 1, visiblePages = 2) }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle { pages = listOf(0, 2) }

        rule.waitUntil {
            // wait for pinned page to be disposed
            !composed.contains(1)
        }

        rule.onNodeWithTag("1").assertIsNotPlaced()
    }

    @Test
    fun pinnedMultipleTimes() {
        // Arrange.
        rule.setContent {
            HorizontalPager(
                state =
                    rememberPagerState { 100 }
                        .also {
                            pagerState = it
                            it.prefetchingEnabled = false
                        },
                modifier = Modifier.size(pageSizeDp * 2),
                pageSize = PageSize.Fixed(pageSizeDp),
            ) { page ->
                if (page == 1) {
                    pinnableContainer = LocalPinnableContainer.current
                }
                PageWithEffect(page)
            }
        }

        val handles = mutableListOf<PinnedHandle>()
        rule.runOnIdle {
            handles.add(requireNotNull(pinnableContainer).pin())
            handles.add(requireNotNull(pinnableContainer).pin())
        }

        rule.runOnIdle {
            // pinned 3 times in total
            handles.add(requireNotNull(pinnableContainer).pin())
            assertThat(composed).contains(0)
            runBlocking { pagerState.scrollToPage(3) }
        }

        rule.waitUntil {
            // wait for not visible page to be disposed
            !composed.contains(0)
        }

        while (handles.isNotEmpty()) {
            rule.runOnIdle {
                assertThat(composed).contains(1)
                handles.removeFirstKt().release()
            }
        }

        rule.waitUntil {
            // wait for pinned page to be disposed
            !composed.contains(1)
        }
    }

    @Test
    fun pinningIsPropagatedToParentContainer() {
        var parentPinned = false
        val parentContainer =
            object : PinnableContainer {
                override fun pin(): PinnedHandle {
                    parentPinned = true
                    return PinnedHandle { parentPinned = false }
                }
            }
        // Arrange.
        rule.setContent {
            CompositionLocalProvider(LocalPinnableContainer provides parentContainer) {
                HorizontalPager(
                    state = rememberPagerState { 1 }.also { it.prefetchingEnabled = false },
                    pageSize = PageSize.Fixed(pageSizeDp),
                ) {
                    pinnableContainer = LocalPinnableContainer.current
                    Box(Modifier.size(pageSizeDp))
                }
            }
        }

        val handle = rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(parentPinned).isTrue()
            handle.release()
        }

        rule.runOnIdle { assertThat(parentPinned).isFalse() }
    }

    @Test
    fun parentContainerChange_pinningIsMaintained() {
        var parent1Pinned = false
        val parent1Container =
            object : PinnableContainer {
                override fun pin(): PinnedHandle {
                    parent1Pinned = true
                    return PinnedHandle { parent1Pinned = false }
                }
            }
        var parent2Pinned = false
        val parent2Container =
            object : PinnableContainer {
                override fun pin(): PinnedHandle {
                    parent2Pinned = true
                    return PinnedHandle { parent2Pinned = false }
                }
            }
        var parentContainer by mutableStateOf<PinnableContainer>(parent1Container)
        // Arrange.
        rule.setContent {
            CompositionLocalProvider(LocalPinnableContainer provides parentContainer) {
                HorizontalPager(
                    state = rememberPagerState { 1 }.also { it.prefetchingEnabled = false },
                    pageSize = PageSize.Fixed(pageSizeDp),
                ) {
                    pinnableContainer = LocalPinnableContainer.current
                    Box(Modifier.size(pageSizeDp))
                }
            }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(parent1Pinned).isTrue()
            assertThat(parent2Pinned).isFalse()
            parentContainer = parent2Container
        }

        rule.runOnIdle {
            assertThat(parent1Pinned).isFalse()
            assertThat(parent2Pinned).isTrue()
        }
    }
}
