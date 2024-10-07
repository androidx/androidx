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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.RotaryInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.MockRotaryResolution
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapSensitivity
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PagerTest {
    @get:Rule val rule = createComposeRule()

    private val pagerTestTag = "Pager"
    private var lcItemSizePx: Float = 20f
    private var lcItemSizeDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) { lcItemSizeDp = lcItemSizePx.toDp() }
    }

    @Test
    fun horizontal_pager_with_nested_scaling_lazy_column_swipes_along_one_page_at_a_time() {
        val pageCount = 5
        lateinit var pagerState: PagerState

        rule.setContent {
            pagerState = rememberPagerState { pageCount }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.testTag(pagerTestTag),
                swipeToDismissEdgeZoneFraction = 0f,
            ) // disable swipe to dismiss as it conflicts with swipeRight()
            { page ->
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

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun horizontal_pager_with_nested_scaling_lazy_column_swipes_along_one_page_at_a_time_avoiding_swiping_from_edge() {
        val pageCount = 5
        lateinit var pagerState: PagerState

        rule.setContent {
            pagerState = rememberPagerState { pageCount }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.testTag(pagerTestTag),
                swipeToDismissEdgeZoneFraction = 0.15f,
            ) { page ->
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
            rule.onNodeWithTag(pagerTestTag).performTouchInput {
                // swipe left - only along middle 60% portion of screen
                val totalX = right - left
                val start = right - 0.2f * totalX
                val end = left + 0.2f * totalX
                swipeLeft(start, end)
            }
        }

        // Swipe along back to start of pager
        for (i in listOfPageIndices.reversed()) {
            Assert.assertEquals(i, pagerState.currentPage)
            rule.onNodeWithText("Page $i").assertIsDisplayed()
            rule.onNodeWithTag(pagerTestTag).performTouchInput {
                // swipe right - only along middle 60% portion of screen
                val totalX = right - left
                val start = left + 0.2f * totalX
                val end = right - 0.2f * totalX
                swipeRight(start, end)
            }
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
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

    @OptIn(ExperimentalWearFoundationApi::class)
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

    @OptIn(ExperimentalWearFoundationApi::class)
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun vertical_pager_scrolled_by_2_pages_with_rotary_high_res() {
        verticalPagerRotaryScrolledBy(
            lowRes = false,
            userScrollEnabled = true,
            rotaryScrollableBehavior = { RotaryScrollableDefaults.snapBehavior(it) },
            rotaryScrollInput = { pagerState ->
                for (i in 0..1) {
                    rotateToScrollVertically(
                        pagerState.layoutInfo.pageSize.toFloat() /
                            RotarySnapSensitivity.HIGH.minThresholdDivider + 1
                    )
                    advanceEventTime(100)
                }
            },
            expectedPageTarget = 2
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun vertical_pager_scrolled_by_2_pages_with_rotary_lowRes() {
        verticalPagerRotaryScrolledBy(
            lowRes = true,
            userScrollEnabled = true,
            rotaryScrollableBehavior = { RotaryScrollableDefaults.snapBehavior(it) },
            rotaryScrollInput = {
                for (i in 0..1) {
                    rotateToScrollVertically(100f)
                    advanceEventTime(100)
                }
            },
            expectedPageTarget = 2
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun vertical_pager_not_rotary_scrolled_with_disabled_userScrolledEnabled() {
        verticalPagerRotaryScrolledBy(
            lowRes = false,
            userScrollEnabled = false,
            rotaryScrollableBehavior = { RotaryScrollableDefaults.snapBehavior(it) },
            rotaryScrollInput = { pagerState ->
                rotateToScrollVertically(
                    pagerState.layoutInfo.pageSize.toFloat() /
                        RotarySnapSensitivity.HIGH.minThresholdDivider + 1
                )
            },
            expectedPageTarget = 0
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun vertical_pager_not_rotary_scrolled_without_rotaryScrollableBehavior() {
        verticalPagerRotaryScrolledBy(
            lowRes = false,
            userScrollEnabled = true,
            rotaryScrollableBehavior = { null },
            rotaryScrollInput = { pagerState ->
                rotateToScrollVertically(
                    pagerState.layoutInfo.pageSize.toFloat() /
                        RotarySnapSensitivity.HIGH.minThresholdDivider + 1
                )
            },
            expectedPageTarget = 0
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun horizontal_pager_scrolled_by_2_pages_with_rotary_high_res() {
        horizontalPagerRotaryScrolledBy(
            lowRes = false,
            userScrollEnabled = true,
            rotaryScrollableBehavior = { RotaryScrollableDefaults.snapBehavior(it) },
            rotaryScrollInput = { pagerState ->
                for (i in 0..1) {
                    rotateToScrollVertically(
                        pagerState.layoutInfo.pageSize.toFloat() /
                            RotarySnapSensitivity.HIGH.minThresholdDivider + 1
                    )
                    advanceEventTime(100)
                }
            },
            expectedPageTarget = 2
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun horizontal_pager_scrolled_by_2_pages_with_rotary_lowRes() {
        horizontalPagerRotaryScrolledBy(
            lowRes = true,
            userScrollEnabled = true,
            rotaryScrollableBehavior = { RotaryScrollableDefaults.snapBehavior(it) },
            rotaryScrollInput = {
                for (i in 0..1) {
                    rotateToScrollVertically(100f)
                    advanceEventTime(100)
                }
            },
            expectedPageTarget = 2
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun horizontal_pager_not_rotary_scrolled_with_disabled_userScrolledEnabled() {
        horizontalPagerRotaryScrolledBy(
            lowRes = false,
            userScrollEnabled = false,
            rotaryScrollableBehavior = { RotaryScrollableDefaults.snapBehavior(it) },
            rotaryScrollInput = { pagerState ->
                rotateToScrollVertically(
                    pagerState.layoutInfo.pageSize.toFloat() /
                        RotarySnapSensitivity.HIGH.minThresholdDivider + 1
                )
            },
            expectedPageTarget = 0
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun horizontal_pager_not_rotary_scrolled_without_rotaryScrollableBehavior() {
        horizontalPagerRotaryScrolledBy(
            lowRes = false,
            userScrollEnabled = true,
            rotaryScrollableBehavior = { null },
            rotaryScrollInput = { pagerState ->
                rotateToScrollVertically(
                    pagerState.layoutInfo.pageSize.toFloat() /
                        RotarySnapSensitivity.HIGH.minThresholdDivider + 1
                )
            },
            expectedPageTarget = 0
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun content_in_horizontalPager_rotary_scrolled_without_rotaryScrollableBehavior() {
        lateinit var pagerState: PagerState
        val pageCount = 5
        lateinit var lcStates: MutableList<LazyListState>

        rule.setContent {
            pagerState = rememberPagerState { pageCount }
            lcStates = MutableList(pageCount) { rememberLazyListState() }
            MockRotaryResolution(lowRes = false) {
                HorizontalPager(
                    modifier = Modifier.testTag(pagerTestTag).size(100.dp),
                    state = pagerState,
                ) { page ->
                    DefaultLazyColumn(lcStates[page])
                }
            }
        }

        rule.onNodeWithTag(pagerTestTag).performRotaryScrollInput {
            rotateToScrollVertically(lcItemSizePx * 5)
        }

        // We expect HorizontalPager not to be scrolled and remain on the 0th page.
        // At the same time a LazyColumn on this page should be scrolled.
        rule.runOnIdle { Assert.assertEquals(0, pagerState.currentPage) }
        rule.runOnIdle { Assert.assertEquals(5, lcStates[0].firstVisibleItemIndex) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun content_in_horizontalPager_not_rotary_scrolled_with_rotaryScrollableBehavior() {
        lateinit var pagerState: PagerState
        val pageCount = 5
        lateinit var lcStates: MutableList<LazyListState>

        rule.setContent {
            pagerState = rememberPagerState { pageCount }
            lcStates = MutableList(pageCount) { rememberLazyListState() }
            MockRotaryResolution(lowRes = false) {
                HorizontalPager(
                    modifier = Modifier.testTag(pagerTestTag).size(100.dp),
                    state = pagerState,
                    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(pagerState)
                ) { page ->
                    DefaultLazyColumn(lcStates[page])
                }
            }
        }

        rule.onNodeWithTag(pagerTestTag).performRotaryScrollInput {
            rotateToScrollVertically(
                pagerState.layoutInfo.pageSize.toFloat() /
                    RotarySnapSensitivity.HIGH.minThresholdDivider + 1
            )
        }

        // We expect HorizontalPager to be scrolled by 1 page.
        rule.runOnIdle { Assert.assertEquals(1, pagerState.currentPage) }
        // At the same time LazyColumns shouldn't be scrolled.
        for (lcState in lcStates) {
            rule.runOnIdle { Assert.assertEquals(0, lcState.firstVisibleItemIndex) }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun content_in_verticalPager_rotary_scrolled_without_rotaryScrollableBehavior() {
        lateinit var pagerState: PagerState
        val pageCount = 5
        lateinit var lcStates: MutableList<LazyListState>

        rule.setContent {
            pagerState = rememberPagerState { pageCount }
            lcStates = MutableList(pageCount) { rememberLazyListState() }
            MockRotaryResolution(lowRes = false) {
                VerticalPager(
                    modifier = Modifier.testTag(pagerTestTag).size(100.dp),
                    state = pagerState,
                    rotaryScrollableBehavior = null
                ) { page ->
                    DefaultLazyColumn(lcStates[page])
                }
            }
        }

        rule.onNodeWithTag(pagerTestTag).performRotaryScrollInput {
            rotateToScrollVertically(lcItemSizePx * 5)
        }

        // We expect VerticalPager not to be scrolled and remain on the 0th page.
        // At the same time a LazyColumn on this page should be scrolled.
        rule.runOnIdle { Assert.assertEquals(0, pagerState.currentPage) }
        rule.runOnIdle { Assert.assertEquals(5, lcStates[0].firstVisibleItemIndex) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun content_in_verticalPager_not_rotary_scrolled_with_rotaryScrollableBehavior() {
        lateinit var pagerState: PagerState
        val pageCount = 5
        lateinit var lcStates: MutableList<LazyListState>

        rule.setContent {
            pagerState = rememberPagerState { pageCount }
            lcStates = MutableList(pageCount) { rememberLazyListState() }
            MockRotaryResolution(lowRes = false) {
                HorizontalPager(
                    modifier = Modifier.testTag(pagerTestTag).size(100.dp),
                    state = pagerState,
                    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(pagerState)
                ) { page ->
                    DefaultLazyColumn(lcStates[page])
                }
            }
        }

        rule.onNodeWithTag(pagerTestTag).performRotaryScrollInput {
            rotateToScrollVertically(
                pagerState.layoutInfo.pageSize.toFloat() /
                    RotarySnapSensitivity.HIGH.minThresholdDivider + 1
            )
        }

        // We expect VerticalPager to be scrolled by 1 page.
        rule.runOnIdle { Assert.assertEquals(1, pagerState.currentPage) }
        // At the same time LazyColumns shouldn't be scrolled.
        for (lcState in lcStates) {
            rule.runOnIdle { Assert.assertEquals(0, lcState.firstVisibleItemIndex) }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun verticalPagerRotaryScrolledBy(
        expectedPageTarget: Int,
        lowRes: Boolean,
        userScrollEnabled: Boolean,
        rotaryScrollableBehavior: @Composable (pagerState: PagerState) -> RotaryScrollableBehavior?,
        rotaryScrollInput: RotaryInjectionScope.(pagerState: PagerState) -> Unit
    ) {
        lateinit var pagerState: PagerState
        val pageCount = 5

        rule.setContent {
            pagerState = rememberPagerState { pageCount }

            MockRotaryResolution(lowRes = lowRes) {
                VerticalPager(
                    modifier = Modifier.testTag(pagerTestTag),
                    state = pagerState,
                    userScrollEnabled = userScrollEnabled,
                    rotaryScrollableBehavior = rotaryScrollableBehavior(pagerState)
                ) { page ->
                    BasicText(text = "Page $page")
                }
            }
        }

        rule.onNodeWithTag(pagerTestTag).performRotaryScrollInput { rotaryScrollInput(pagerState) }

        rule.runOnIdle { Assert.assertEquals(expectedPageTarget, pagerState.currentPage) }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun horizontalPagerRotaryScrolledBy(
        expectedPageTarget: Int,
        lowRes: Boolean,
        userScrollEnabled: Boolean,
        rotaryScrollableBehavior: @Composable (pagerState: PagerState) -> RotaryScrollableBehavior?,
        rotaryScrollInput: RotaryInjectionScope.(pagerState: PagerState) -> Unit
    ) {
        lateinit var pagerState: PagerState
        val pageCount = 5

        rule.setContent {
            pagerState = rememberPagerState { pageCount }

            MockRotaryResolution(lowRes = lowRes) {
                HorizontalPager(
                    modifier = Modifier.testTag(pagerTestTag),
                    state = pagerState,
                    userScrollEnabled = userScrollEnabled,
                    rotaryScrollableBehavior = rotaryScrollableBehavior(pagerState)
                ) { page ->
                    BasicText(text = "Page $page")
                }
            }
        }

        rule.onNodeWithTag(pagerTestTag).performRotaryScrollInput { rotaryScrollInput(pagerState) }

        rule.runOnIdle { Assert.assertEquals(expectedPageTarget, pagerState.currentPage) }
    }

    @Composable
    fun DefaultLazyColumn(state: LazyListState) {
        LazyColumn(
            state = state,
            modifier =
                Modifier.rotaryScrollable(
                    RotaryScrollableDefaults.behavior(state),
                    rememberActiveFocusRequester()
                )
        ) {
            for (i in 0..20) {
                item { BasicText(modifier = Modifier.height(lcItemSizeDp), text = "Page content") }
            }
        }
    }
}
