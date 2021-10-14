/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
// These tests are in addition to ScalingLazyListLayoutInfoTest which handles scroll events at an
// absolute level and is designed to exercise scrolling through the UI directly.
public class ScalingLazyColumnTest {
    @get:Rule
    val rule = createComposeRule()

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
    fun visibleItemsAreCorrectAfterScrolling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(
                        itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                    ),
                ) {
                    items(5) {
                        Box(Modifier.requiredSize(itemSizeDp))
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
        }

        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollingReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(
                        itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                    ),
                    reverseLayout = true,
                ) {
                    items(5) {
                        Box(Modifier.requiredSize(itemSizeDp))
                    }
                }
            }
        }

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
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(
                        itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                    ),
                    scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
                ) {
                    items(5) {
                        Box(Modifier.requiredSize(itemSizeDp).testTag("Item:" + it))
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(
                startY = bottom,
                endY = bottom - (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()),
            )
        }
        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
        assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollNoScalingForReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            WithTouchSlop(0f) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(
                        itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                    ),
                    scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                    reverseLayout = true
                ) {
                    items(5) {
                        Box(Modifier.requiredSize(itemSizeDp).testTag("Item:" + it))
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeDown(
                startY = top,
                endY = top + (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            )
        }
        rule.waitForIdle()
        state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
        assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
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
                    modifier = Modifier
                        .testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
                ) {
                    items(6) {
                        Box(Modifier.requiredSize(itemSizeDp))
                    }
                }
                ObservingFun(state, currentInfo)
            }
        }
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
        spacing: Int = defaultItemSpacingPx
    ) {
        assertThat(visibleItemsInfo.size).isEqualTo(count)
        var currentIndex = startIndex
        var previousEndOffset = -1
        visibleItemsInfo.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertThat(it.size).isEqualTo((unscaledSize * it.scale).roundToInt())
            currentIndex++
            if (previousEndOffset != -1) {
                assertThat(spacing).isEqualTo(it.offset - previousEndOffset)
            }
            previousEndOffset = it.offset + it.size
        }
    }
}
