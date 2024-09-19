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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.TEST_TAG
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
// These tests are in addition to ScalingLazyListLayoutInfoTest which handles scroll events at an
// absolute level and is designed to exercise scrolling through the UI directly.
public class ScalingLazyColumnTest {
    private val scalingLazyColumnTag = "scalingLazyColumnTag"
    private val firstItemTag = "firstItemTag"

    @get:Rule val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity
    private var defaultItemSpacingDp: Dp = 4.dp
    private var defaultItemSpacingPx = Int.MAX_VALUE

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            defaultItemSpacingPx = defaultItemSpacingDp.roundToPx()
        }
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndNormalItemPaddingForZeroItemList() {
        initializationCorrectWithAutoCenteringAndNormalItemPaddingForNItemList(0)
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndNormalItemPaddingForOneItemList() {
        initializationCorrectWithAutoCenteringAndNormalItemPaddingForNItemList(1)
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndNormalItemPaddingForTwoItemList() {
        initializationCorrectWithAutoCenteringAndNormalItemPaddingForNItemList(2)
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndNormalItemPaddingForThreeItemList() {
        initializationCorrectWithAutoCenteringAndNormalItemPaddingForNItemList(3)
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndNormalItemPaddingForFourItemList() {
        initializationCorrectWithAutoCenteringAndNormalItemPaddingForNItemList(4)
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndNormalItemPaddingForFiveItemList() {
        initializationCorrectWithAutoCenteringAndNormalItemPaddingForNItemList(5)
    }

    private fun initializationCorrectWithAutoCenteringAndNormalItemPaddingForNItemList(
        itemCount: Int
    ) {
        lateinit var state: ScalingLazyListState
        val listSize = itemSizeDp * 3.5f
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(listSize),
                ) {
                    items(itemCount) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }

        rule.waitUntil { state.initialized.value }
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndCustomInterItemPadding() {
        lateinit var state: ScalingLazyListState
        val listSize = itemSizeDp * 3.5f
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(listSize),
                    scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1f),
                    // We want to arrange for the autoCentering spacer to be of size zero, but
                    // also for the gap between items to be needed.
                    verticalArrangement = Arrangement.spacedBy(itemSizeDp * 0.25f * 0.75f),
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }

        rule.waitUntil { state.initialized.value }
    }

    @Test
    fun initializationCorrectWithAutoCenteringAndLargeFirstItem() {
        lateinit var state: ScalingLazyListState
        val listSize = itemSizeDp * 3.5f
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(listSize),
                    scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1f),
                ) {
                    item { Box(Modifier.requiredSize(itemSizeDp * 2)) }
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }

        rule.waitUntil { state.initialized.value }
    }

    @Test
    fun autoCenteringCorrectSizeWithCenterAnchor() {
        lateinit var state: ScalingLazyListState
        val listSize = itemSizeDp * 3.5f
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(listSize),
                    anchorType = ScalingLazyListAnchorType.ItemCenter,
                    autoCentering = AutoCenteringParams(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    scalingParams =
                        ScalingLazyColumnDefaults.scalingParams(
                            edgeScale = 0f,
                            minTransitionArea = 0.5f,
                            maxTransitionArea = 0.5f
                        )
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        val listSizePx = with(rule.density) { listSize.roundToPx() }
        rule.runOnIdle {
            // Make sure that the edge items have been scaled
            assertThat(state.layoutInfo.visibleItemsInfo.first().scale).isLessThan(1.0f)
            // But that size of the Spacer is as expected - it should be half the viewport size
            // rounded down minus half the size of the center item rounded down minus the full size
            // of the 0th item
            assertThat(state.lazyListState.layoutInfo.visibleItemsInfo.first().size)
                .isEqualTo((listSizePx / 2) - (itemSizePx + itemSizePx / 2))
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
        rule.runOnIdle {
            // Check that the last item has been scrolled into view
            assertThat(state.lazyListState.layoutInfo.visibleItemsInfo.last().index)
                .isEqualTo(state.lazyListState.layoutInfo.totalItemsCount - 1)
            // And that size of the Spacer is as expected
            assertThat(state.lazyListState.layoutInfo.visibleItemsInfo.last().size)
                .isEqualTo(((listSizePx / 2f) - (itemSizePx / 2f)).roundToInt())
        }
    }

    @Test
    fun autoCenteringCorrectSizeWithStartAnchor() {
        lateinit var state: ScalingLazyListState
        val listSize = itemSizeDp * 3.5f
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(listSize),
                    anchorType = ScalingLazyListAnchorType.ItemStart,
                    autoCentering = AutoCenteringParams(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    scalingParams =
                        ScalingLazyColumnDefaults.scalingParams(
                            edgeScale = 0f,
                            minTransitionArea = 0.5f,
                            maxTransitionArea = 0.5f
                        )
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        val listSizePx = with(rule.density) { listSize.roundToPx() }

        rule.runOnIdle {
            // Make sure that the edge items have been scaled
            assertThat(state.layoutInfo.visibleItemsInfo.first().scale).isLessThan(1.0f)
            // But that size of the Spacer is as expected, it should be half the viewport size
            // rounded down minus the size of zeroth item in the list
            assertThat(state.lazyListState.layoutInfo.visibleItemsInfo.first().size)
                .isEqualTo((listSizePx / 2) - itemSizePx)
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp(endY = top) }
        rule.runOnIdle {
            // Check that the last item has been scrolled into view
            assertThat(state.lazyListState.layoutInfo.visibleItemsInfo.last().index)
                .isEqualTo(state.lazyListState.layoutInfo.totalItemsCount - 1)
            // And that size of the Spacer is as expected
            assertThat(state.lazyListState.layoutInfo.visibleItemsInfo.last().size)
                .isEqualTo(((listSizePx / 2f) - itemSizePx).roundToInt())
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScrolling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = null
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
        }

        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
    }

    @Test
    fun visibleItemsAreCorrectAfterAttemptedScrollWithUserScrollDisabled() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = null,
                    userScrollEnabled = false
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
        }

        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollingWithAutoCentering() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state =
                        rememberScalingLazyListState(initialCenterItemIndex = 0).also {
                            state = it
                        },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = AutoCenteringParams(itemIndex = 0)
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 3, startIndex = 0)

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
        }

        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollingWithSnap() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state =
                        rememberScalingLazyListState(initialCenterItemIndex = 0).also {
                            state = it
                        },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = AutoCenteringParams(itemIndex = 0),
                    flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state)
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        state.layoutInfo.assertVisibleItems(count = 3, startIndex = 0)

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            // Swipe by an amount that is not a whole item + gap
            swipeWithVelocity(
                start = Offset(centerX, bottom),
                end = Offset(centerX, bottom - (itemSizePx.toFloat())),
                endVelocity = 1f, // Ensure it's not a fling.
            )
        }

        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
        assertThat(state.centerItemIndex).isEqualTo(1)
        assertThat(state.centerItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollingWithSnapAndOffset() {
        lateinit var state: ScalingLazyListState
        val snapOffset = 5.dp
        var snapOffsetPx = 0
        rule.setContent {
            WithTouchSlop(0f) {
                snapOffsetPx = with(LocalDensity.current) { snapOffset.roundToPx() }

                ScalingLazyColumn(
                    state =
                        rememberScalingLazyListState(initialCenterItemIndex = 0).also {
                            state = it
                        },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = AutoCenteringParams(itemIndex = 0),
                    flingBehavior =
                        ScalingLazyColumnDefaults.snapFlingBehavior(
                            state = state,
                            snapOffset = snapOffset
                        )
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 3, startIndex = 0)

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            // Swipe by an amount that is not a whole item + gap
            swipeWithVelocity(
                start = Offset(centerX, bottom),
                end = Offset(centerX, bottom - (itemSizePx.toFloat())),
                endVelocity = 1f, // Ensure it's not a fling.
            )
        }

        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
        assertThat(state.centerItemIndex).isEqualTo(1)
        assertThat(state.centerItemScrollOffset).isEqualTo(snapOffsetPx)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollingReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    reverseLayout = true,
                    autoCentering = null
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeDown(
                startY = top,
                endY = top + (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            )
        }
        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollNoScaling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state =
                        rememberScalingLazyListState(initialCenterItemIndex = 0).also {
                            state = it
                        },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                    autoCentering = AutoCenteringParams(itemIndex = 0)
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp).testTag("Item:" + it)) }
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(
                startY = bottom,
                endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()),
            )
        }
        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
        assertThat(state.centerItemIndex).isEqualTo(1)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollNoScalingForReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState(8).also { state = it },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 4f + defaultItemSpacingDp * 3f),
                    scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                    reverseLayout = true
                ) {
                    items(15) { Box(Modifier.requiredSize(itemSizeDp).testTag("Item:" + it)) }
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeDown(
                startY = top,
                endY = top + (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            )
        }
        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 5, startIndex = 7)
        assertThat(state.centerItemIndex).isEqualTo(9)
        assertThat(state.centerItemScrollOffset).isEqualTo(0)
    }

    @Composable
    fun ObservingFun(
        state: ScalingLazyListState,
        currentInfo: StableRef<ScalingLazyListLayoutInfo?>
    ) {
        currentInfo.value = state.layoutInfo
    }

    @Test
    fun visibleItemsAreObservableWhenWeScroll() {
        lateinit var state: ScalingLazyListState
        val currentInfo = StableRef<ScalingLazyListLayoutInfo?>(null)
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = null
                ) {
                    items(6) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
                ObservingFun(state, currentInfo)
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        currentInfo.value = null
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(
                startY = bottom,
                endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()),
            )
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(milliseconds = 1000)
        assertThat(currentInfo.value).isNotNull()
        currentInfo.value!!.assertVisibleItems(count = 4, startIndex = 1)
    }

    fun ScalingLazyListLayoutInfo.assertVisibleItems(
        count: Int,
        startIndex: Int = 0,
        unscaledSize: Int = itemSizePx,
        spacing: Int = defaultItemSpacingPx,
        anchorType: ScalingLazyListAnchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        assertThat(visibleItemsInfo.size).isEqualTo(count)
        var currentIndex = startIndex
        var previousEndOffset = -1
        visibleItemsInfo.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertThat(it.size).isEqualTo((unscaledSize * it.scale).roundToInt())
            currentIndex++
            val startOffset = it.startOffset(anchorType).roundToInt()
            if (previousEndOffset != -1) {
                assertThat(spacing).isEqualTo(startOffset - previousEndOffset)
            }
            previousEndOffset = startOffset + it.size
        }
    }

    @Test
    fun itemFillingParentWidth() {
        lateinit var state: ScalingLazyListState
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(width = 100.dp, height = 150.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.fillParentMaxWidth().requiredHeight(50.dp).testTag(firstItemTag)
                    )
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithTag(firstItemTag).assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeight() {
        lateinit var state: ScalingLazyListState
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(width = 100.dp, height = 150.dp),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.requiredWidth(50.dp).fillParentMaxHeight().testTag(firstItemTag)
                    )
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithTag(firstItemTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSize() {
        lateinit var state: ScalingLazyListState
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(width = 100.dp, height = 150.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(listOf(0)) { Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag)) }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithTag(firstItemTag).assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentWidthFraction() {
        lateinit var state: ScalingLazyListState
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(width = 100.dp, height = 150.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.fillParentMaxWidth(0.7f)
                            .requiredHeight(50.dp)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithTag(firstItemTag).assertWidthIsEqualTo(70.dp).assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeightFraction() {
        lateinit var state: ScalingLazyListState
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(width = 100.dp, height = 150.dp)
            ) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.requiredWidth(50.dp)
                            .fillParentMaxHeight(0.3f)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithTag(firstItemTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(45.dp)
    }

    @Test
    fun itemFillingParentSizeFraction() {
        lateinit var state: ScalingLazyListState
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(width = 100.dp, height = 150.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(listOf(0)) { Spacer(Modifier.fillParentMaxSize(0.5f).testTag(firstItemTag)) }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithTag(firstItemTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(75.dp)
    }

    @Test
    fun itemFillingParentSizeParentResized() {
        lateinit var state: ScalingLazyListState
        var parentSize by mutableStateOf(100.dp)
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(parentSize),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                items(listOf(0)) { Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag)) }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle { parentSize = 150.dp }

        rule.onNodeWithTag(firstItemTag).assertWidthIsEqualTo(150.dp).assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun listSizeFitsContentsIfNotSet() {
        lateinit var state: ScalingLazyListState
        var itemSize by mutableStateOf(100.dp)
        rule.setContentWithTestViewConfiguration {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.testTag(scalingLazyColumnTag),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                items(listOf(0)) { Spacer(Modifier.size(itemSize).testTag(firstItemTag)) }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }

        rule.onNodeWithTag(scalingLazyColumnTag).assertWidthIsEqualTo(itemSize)

        rule.runOnIdle { itemSize = 150.dp }

        rule.onNodeWithTag(scalingLazyColumnTag).assertWidthIsEqualTo(itemSize)

        rule.runOnIdle { itemSize = 50.dp }

        rule.onNodeWithTag(scalingLazyColumnTag).assertWidthIsEqualTo(itemSize)
    }

    @Test
    fun listStateUsesInitialCenterItemIndex() {
        val startIndexValue = 5
        lateinit var state: ScalingLazyListState

        rule.setContent {
            state = rememberScalingLazyListState(initialCenterItemIndex = startIndexValue)
        }

        assertThat(state.centerItemIndex).isEqualTo(startIndexValue)
    }

    @Test
    fun listStateUsesInitialCenterItemScrollOffset() {
        val startScrollValue = 5
        lateinit var state: ScalingLazyListState

        rule.setContent {
            state = rememberScalingLazyListState(initialCenterItemScrollOffset = startScrollValue)
        }

        assertThat(state.centerItemScrollOffset).isEqualTo(startScrollValue)
    }

    @Test
    fun scrollToNotVisibleListWorks() {
        lateinit var state: ScalingLazyListState
        lateinit var showList: MutableState<Boolean>
        rule.setContent {
            showList = remember { mutableStateOf(true) }
            state = rememberScalingLazyListState()
            if (showList.value) {
                ScalingLazyColumn(
                    state = state,
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = AutoCenteringParams()
                ) {
                    items(25) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            } else {
                Box(
                    modifier =
                        Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
                )
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }

        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(TEST_TAG).assertIsDisplayed()

        showList.value = false

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()

        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }

        rule.waitForIdle()
        showList.value = true

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()

        state.layoutInfo.assertVisibleItems(count = 5, startIndex = 8)
        assertThat(state.centerItemIndex).isEqualTo(10)
        assertThat(state.centerItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollToNonExistentItemWorks() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            state = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = state,
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                autoCentering = AutoCenteringParams()
            ) {
                items(25) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }

        rule.runOnIdle { runBlocking { state.scrollToItem(50) } }

        state.layoutInfo.assertVisibleItems(count = 3, startIndex = 22)
        assertThat(state.centerItemIndex).isEqualTo(24)
        assertThat(state.centerItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun centerItemIndexPublishesUpdatesOnChangeOnly() {
        lateinit var state: ScalingLazyListState
        var recompositionCount = 0

        rule.setContent {
            state = rememberScalingLazyListState(initialCenterItemIndex = 0)

            WithTouchSlop(0f) {
                state.centerItemIndex
                recompositionCount++

                ScalingLazyColumn(
                    state = state,
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                    autoCentering = AutoCenteringParams(itemIndex = 0)
                ) {
                    items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
        }

        rule.waitForIdle()
        assertThat(recompositionCount).isEqualTo(2)
    }

    @Test
    fun scalingLazyColumnCanBeNestedOnHorizontalScrollingComponent() {
        rule.setContent {
            val horizontalScrollState = rememberScrollState()
            Box(
                modifier = Modifier.horizontalScroll(horizontalScrollState),
            ) {
                ScalingLazyColumn { item { Box(Modifier.size(10.dp)) } }
            }
        }
    }

    @Test
    fun scalingLazyColumnIsFocusedByDefault() {
        var focusSet = false

        rule.setContent {
            ScalingLazyColumn(
                modifier =
                    Modifier.testTag("scalingLazyColumn").onFocusChanged {
                        focusSet = it.isFocused
                    },
            ) {
                items(100) { BasicText("item $it") }
            }
        }

        assert(focusSet)
    }

    @Test
    fun scalingLazyColumnIsNotFocused_withDisabledRotary() {
        var focusSet = false

        rule.setContent {
            ScalingLazyColumn(
                modifier =
                    Modifier.testTag("scalingLazyColumn").onFocusChanged {
                        focusSet = it.isFocused
                    },
                // Disable rotary and focus as well
                rotaryScrollableBehavior = null,
            ) {
                items(100) { BasicText("item $it") }
            }
        }

        assert(!focusSet)
    }

    @Ignore("b/347700248 re-enable test after focusable semantics start working again")
    @Test
    fun scalingLazyColumnIsFocusedByDefault_withSemantics() {
        rule.setContent {
            ScalingLazyColumn(
                modifier = Modifier.testTag("scalingLazyColumn"),
            ) {
                items(100) { BasicText("item $it") }
            }
        }

        rule.onNodeWithTag("scalingLazyColumn").assertIsFocused()
    }

    @Test
    fun scalingLazyColumn_rotary_enabledScroll() {
        testScalingLazyColumnRotary(true, 3)
    }

    @Test
    fun scalingLazyColumn_noRotary_disabledScroll() {
        testScalingLazyColumnRotary(false, 1)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun testScalingLazyColumnRotary(
        userScrollEnabled: Boolean,
        scrollTarget: Int,
        scrollItems: Int = 2
    ) {
        lateinit var state: ScalingLazyListState

        rule.setContent {
            state = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = state,
                modifier = Modifier.testTag(scalingLazyColumnTag),
                userScrollEnabled = userScrollEnabled
            ) {
                items(100) {
                    BasicText(text = "item $it", modifier = Modifier.requiredSize(itemSizeDp))
                }
            }
        }
        rule.onNodeWithTag(scalingLazyColumnTag).performRotaryScrollInput {
            // try to scroll by N items
            rotateToScrollVertically(itemSizePx.toFloat() * scrollItems)
        }
        rule.waitForIdle()

        assertThat(state.centerItemIndex).isEqualTo(scrollTarget)
    }
}

internal const val TestTouchSlop = 18f

internal fun ComposeContentTestRule.setContentWithTestViewConfiguration(
    composable: @Composable () -> Unit
) {
    this.setContent { WithTouchSlop(TestTouchSlop, composable) }
}
