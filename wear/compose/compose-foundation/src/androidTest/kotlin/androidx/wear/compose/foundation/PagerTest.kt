/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class PagerTest {
    @get:Rule val rule = createComposeRule()

    private val pagerTestTag = "Pager"

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun horizontal_pager_with_nested_scaling_lazy_column_swipes_along_one_page_at_a_time() {
        val pageCount = 5
        lateinit var pagerState: PagerState

        rule.setContent {
            pagerState = rememberPagerState { pageCount }

            HorizontalPager(state = pagerState, modifier = Modifier.testTag(pagerTestTag)) { page ->
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { BasicText(text = "Page $page") }
                }
            }
        }

        val listOfPageIndices = 0 until pageCount

        // Swipe along to end of pager
        for (i in listOfPageIndices) {
            Assert.assertEquals(i, pagerState.currentPage)
            rule.onNodeWithText("Page $i").assertIsDisplayed()
            rule.onNodeWithTag(pagerTestTag).performTouchInput { swipeLeft() }
        }

        // Swipe along back to start of pager
        for (i in listOfPageIndices.reversed()) {
            Assert.assertEquals(i, pagerState.currentPage)
            rule.onNodeWithText("Page $i").assertIsDisplayed()
            rule.onNodeWithTag(pagerTestTag).performTouchInput { swipeRight() }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun vertical_pager_with_nested_scaling_lazy_column_swipes_along_one_page_at_a_time() {
        val pageCount = 5
        lateinit var pagerState: PagerState

        rule.setContent {
            pagerState = rememberPagerState { pageCount }

            VerticalPager(state = pagerState, modifier = Modifier.testTag(pagerTestTag)) { page ->
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { BasicText(text = "Page $page") }
                }
            }
        }

        val listOfPageIndices = 0 until pageCount

        // Swipe along to end of pager
        for (i in listOfPageIndices) {
            Assert.assertEquals(i, pagerState.currentPage)
            rule.onNodeWithText("Page $i").assertIsDisplayed()
            rule.onNodeWithTag(pagerTestTag).performTouchInput { swipeUp() }
        }

        // Swipe along back to start of pager
        for (i in listOfPageIndices.reversed()) {
            Assert.assertEquals(i, pagerState.currentPage)
            rule.onNodeWithText("Page $i").assertIsDisplayed()
            rule.onNodeWithTag(pagerTestTag).performTouchInput { swipeDown() }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun verifyScrollsToEachPage(
        pageCount: Int,
        pagerState: PagerState,
        scrollScope: CoroutineScope
    ) {
        val listOfPageIndices = 0 until pageCount

        // Scroll to each page then return to page 0
        for (i in listOfPageIndices) {
            rule.runOnIdle { Assert.assertEquals(0, pagerState.currentPage) }
            rule.onNodeWithText("Page 0").assertIsDisplayed()

            rule.runOnIdle { scrollScope.launch { pagerState.animateScrollToPage(i) } }

            rule.runOnIdle { Assert.assertEquals(i, pagerState.currentPage) }
            rule.onNodeWithText("Page $i").assertIsDisplayed()

            rule.runOnIdle { scrollScope.launch { pagerState.animateScrollToPage(0) } }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun horizontal_pager_with_nested_scaling_lazy_column_is_able_to_scroll_to_arbitrary_page() {
        val pageCount = 5
        lateinit var pagerState: PagerState
        lateinit var scrollScope: CoroutineScope

        rule.setContent {
            pagerState = rememberPagerState { pageCount }
            scrollScope = rememberCoroutineScope()

            HorizontalPager(state = pagerState, modifier = Modifier.testTag(pagerTestTag)) { page ->
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { BasicText(text = "Page $page") }
                }
            }
        }

        verifyScrollsToEachPage(pageCount, pagerState, scrollScope)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun vertical_pager_with_nested_scaling_lazy_column_is_able_to_scroll_to_arbitrary_page() {
        val pageCount = 5
        lateinit var pagerState: PagerState
        lateinit var scrollScope: CoroutineScope

        rule.setContent {
            pagerState = rememberPagerState { pageCount }
            scrollScope = rememberCoroutineScope()

            VerticalPager(state = pagerState, modifier = Modifier.testTag(pagerTestTag)) { page ->
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { BasicText(text = "Page $page") }
                }
            }
        }

        verifyScrollsToEachPage(pageCount, pagerState, scrollScope)
    }
}
