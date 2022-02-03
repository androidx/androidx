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

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
public class ScalingLazyListLayoutInfoTest {
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

    @FlakyTest(bugId = 217762753)
    @Test
    fun visibleItemsAreCorrect() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                autoCentering = true
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.centerItemIndex).isEqualTo(1)
            assertThat(state.centerItemScrollOffset).isEqualTo(0)
            state.layoutInfo.assertVisibleItems(count = 4)
        }
    }

    @Test
    fun visibleItemsAreCorrectSetExplicitInitialItemIndex() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(initialCenterItemIndex = 0)
                    .also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                autoCentering = true
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.centerItemIndex).isEqualTo(0)
            assertThat(state.centerItemScrollOffset).isEqualTo(0)
            state.layoutInfo.assertVisibleItems(count = 3)
        }
    }

    @Test
    fun visibleItemsAreCorrectNoAutoCentering() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 4)
        }
    }

    @Test
    fun visibleItemsAreCorrectForReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                reverseLayout = true,
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.centerItemIndex).isEqualTo(1)
            state.layoutInfo.assertVisibleItems(count = 4)
        }
    }

    @FlakyTest(bugId = 216138159)
    @Test
    fun visibleItemsAreCorrectForReverseLayoutWithAutoCentering() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(initialCenterItemIndex = 0)
                    .also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                reverseLayout = true,
                autoCentering = true
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.centerItemIndex).isEqualTo(0)
            assertThat(state.centerItemScrollOffset).isEqualTo(0)
            state.layoutInfo.assertVisibleItems(count = 3)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScrolling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Test
    fun largeItemLargerThanViewPortDoesNotGetScaled() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp
                ),
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp * 5))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
            val firstItem = state.layoutInfo.visibleItemsInfo.first()
            assertThat(firstItem.offset).isLessThan(0)
            assertThat(firstItem.offset + firstItem.size).isGreaterThan(itemSizePx)
            assertThat(state.layoutInfo.visibleItemsInfo.first().scale).isEqualTo(1.0f)
        }
    }

    @Test
    fun itemInsideScalingLinesDoesNotGetScaled() {
        lateinit var state: ScalingLazyListState
        val centerItemIndex = 2
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(centerItemIndex).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3
                ),
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            // Get the middle item on the screen
            val centerScreenItem =
                state.layoutInfo.visibleItemsInfo.find { it.index == centerItemIndex }
            // and confirm its offset is 0
            assertThat(centerScreenItem!!.offset).isEqualTo(0)
            // And that it is not scaled
            assertThat(centerScreenItem.scale).isEqualTo(1.0f)
        }
    }

    @Test
    fun itemOutsideScalingLinesDoesGetScaled() {
        lateinit var state: ScalingLazyListState
        val centerItemIndex = 2
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(centerItemIndex).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 4
                ),
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            // Get the middle item on the screen
            val edgeScreenItem =
                state.layoutInfo.visibleItemsInfo.find { it.index == 0 }

            // And that it is it scaled
            assertThat(edgeScreenItem!!.scale).isLessThan(1.0f)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollingReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                reverseLayout = true,
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Test
    fun visibleItemsAreCorrectCenterPivotNoOffset() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(2).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 2f + defaultItemSpacingDp * 1f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 3, startIndex = 1)
            assertThat(state.centerItemIndex).isEqualTo(2)
            assertThat(state.centerItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun visibleItemsAreCorrectCenterPivotWithOffset() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(2, -5).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 2f + defaultItemSpacingDp * 1f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 3, startIndex = 1)
            assertThat(state.centerItemIndex).isEqualTo(2)
            assertThat(state.centerItemScrollOffset).isEqualTo(-5)
        }
    }

    @Test
    fun visibleItemsAreCorrectCenterPivotNoOffsetReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(2).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 2f + defaultItemSpacingDp * 1f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                reverseLayout = true
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 3, startIndex = 1)
            assertThat(state.centerItemIndex).isEqualTo(2)
            assertThat(state.centerItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun visibleItemsAreCorrectCenterPivotWithOffsetReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(2, -5).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 2f + defaultItemSpacingDp * 1f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                reverseLayout = true
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 3, startIndex = 1)
            assertThat(state.centerItemIndex).isEqualTo(2)
            assertThat(state.centerItemScrollOffset).isEqualTo(-5)
        }
    }

    @Test
    fun visibleItemsAreCorrectNoScalingForReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 4f + defaultItemSpacingDp * 3f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                reverseLayout = true
            ) {
                items(15) {
                    Box(Modifier.requiredSize(itemSizeDp).testTag("Item:$it"))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        // Assert that items are being shown at the end of the parent as this is reverseLayout
        rule.onNodeWithTag(testTag = "Item:8").assertIsDisplayed()

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 5, startIndex = 6)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollNoScaling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(initialCenterItemIndex = 0)
                    .also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                contentPadding = PaddingValues(vertical = 100.dp),
            ) {
                items(5) {
                    Box(
                        Modifier
                            .requiredSize(itemSizeDp)
                            .testTag("Item:$it"))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        rule.onNodeWithTag(testTag = "Item:0").assertIsDisplayed()

        val scrollAmount = (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()).roundToInt()
        rule.runOnIdle {
            assertThat(state.centerItemIndex).isEqualTo(0)
            assertThat(state.centerItemScrollOffset).isEqualTo(0)

            runBlocking {
                state.scrollBy(scrollAmount.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 4)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(-scrollAmount)
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-scrollAmount.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 3)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollNoScalingForReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(8).also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 4f + defaultItemSpacingDp * 3f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                reverseLayout = true
            ) {
                items(15) {
                    Box(Modifier.requiredSize(itemSizeDp).testTag("Item:$it"))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.waitForIdle()

        rule.onNodeWithTag(testTag = "Item:8").assertIsDisplayed()

        val scrollAmount = (itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()).roundToInt()
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 5, startIndex = 6)
            assertThat(state.centerItemIndex).isEqualTo(8)
            assertThat(state.centerItemScrollOffset).isEqualTo(0)

            runBlocking {
                state.scrollBy(scrollAmount.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 5, startIndex = 7)
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-scrollAmount.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 5, startIndex = 6)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterDispatchRawDeltaScrollNoScaling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(initialCenterItemIndex = 0)
                    .also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                contentPadding = PaddingValues(vertical = 100.dp)
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        val scrollAmount = itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()
        rule.runOnIdle {
            runBlocking {
                state.dispatchRawDelta(scrollAmount)
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset)
                .isEqualTo(-scrollAmount.roundToInt())
        }

        rule.runOnIdle {
            runBlocking {
                state.dispatchRawDelta(-scrollAmount)
            }
            state.layoutInfo.assertVisibleItems(count = 3, startIndex = 0)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterDispatchRawDeltaScrollNoScalingForReverseLayout() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f),
                reverseLayout = true,
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }
        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        val firstItemOffset = state.layoutInfo.visibleItemsInfo.first().offset
        rule.runOnIdle {
            runBlocking {
                state.dispatchRawDelta(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(firstItemOffset)
        }

        rule.runOnIdle {
            runBlocking {
                state.dispatchRawDelta(-(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(firstItemOffset)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithCustomSpacing() {
        lateinit var state: ScalingLazyListState
        val spacing: Dp = 10.dp
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + spacing * 2.5f),
                verticalArrangement = Arrangement.spacedBy(spacing),
                autoCentering = false
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            val spacingPx = with(rule.density) {
                spacing.roundToPx()
            }
            state.layoutInfo.assertVisibleItems(
                count = 4,
                spacing = spacingPx
            )
        }
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
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                autoCentering = false
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingFun(state, currentInfo)
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Test
    fun visibleItemsAreObservableWhenWeDispatchRawDeltaScroll() {
        lateinit var state: ScalingLazyListState
        val currentInfo = StableRef<ScalingLazyListLayoutInfo?>(null)
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                autoCentering = false
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingFun(state, currentInfo)
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking {
                state.dispatchRawDelta(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Composable
    fun ObservingIsScrollInProgressTrueFun(
        state: ScalingLazyListState,
        currentInfo: StableRef<Boolean?>
    ) {
        // If isScrollInProgress is ever true record it - otherwise leave the value as null
        if (state.isScrollInProgress) {
            currentInfo.value = true
        }
    }

    @Test
    fun isScrollInProgressIsObservableWhenWeScroll() {
        lateinit var state: ScalingLazyListState
        var scope: CoroutineScope? = null
        val currentInfo = StableRef<Boolean?>(null)
        rule.setContent {
            scope = rememberCoroutineScope()
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingIsScrollInProgressTrueFun(state, currentInfo)
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        scope!!.launch {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            state.animateScrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            assertThat(currentInfo.value).isTrue()
        }
    }

    @Composable
    fun ObservingCentralItemIndexFun(
        state: ScalingLazyListState,
        currentInfo: StableRef<Int?>
    ) {
        currentInfo.value = state.centerItemIndex
    }

    @Test
    fun isCentralListItemIndexObservableWhenWeScroll() {
        lateinit var state: ScalingLazyListState
        var scope: CoroutineScope? = null
        val currentInfo = StableRef<Int?>(null)
        rule.setContent {
            scope = rememberCoroutineScope()
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f),
                autoCentering = false
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingCentralItemIndexFun(state, currentInfo)
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        scope!!.launch {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            state.animateScrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            assertThat(currentInfo.value).isEqualTo(2)
        }
    }

    @Test
    fun visibleItemsAreObservableWhenResize() {
        lateinit var state: ScalingLazyListState
        var size by mutableStateOf(itemSizeDp * 2)
        var currentInfo: ScalingLazyListLayoutInfo? = null
        @Composable
        fun observingFun() {
            currentInfo = state.layoutInfo
        }
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it }
            ) {
                item {
                    Box(Modifier.requiredSize(size))
                }
            }
            observingFun()
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, unscaledSize = itemSizePx * 2)
            currentInfo = null
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, unscaledSize = itemSizePx)
        }
    }

    @Test
    fun viewportOffsetsAreCorrect() {
        val sizePx = 45
        val sizeDp = with(rule.density) { sizePx.toDp() }
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                Modifier.requiredSize(sizeDp),
                state = rememberScalingLazyListState().also { state = it }
            ) {
                items(4) {
                    Box(Modifier.requiredSize(sizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx)
        }
    }

    @Test
    fun viewportOffsetsAreCorrectWithContentPadding() {
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val topPaddingDp = with(rule.density) { startPaddingPx.toDp() }
        val bottomPaddingDp = with(rule.density) { endPaddingPx.toDp() }
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                Modifier.requiredSize(sizeDp),
                contentPadding = PaddingValues(top = topPaddingDp, bottom = bottomPaddingDp),
                state = rememberScalingLazyListState().also { state = it }
            ) {
                items(4) {
                    Box(Modifier.requiredSize(sizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
        }
    }

    @Test
    fun viewportOffsetsAreCorrectWithAutoCentering() {
        val sizePx = 45
        val sizeDp = with(rule.density) { sizePx.toDp() }
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                Modifier.requiredSize(sizeDp),
                state = rememberScalingLazyListState().also { state = it },
                autoCentering = true
            ) {
                items(4) {
                    Box(Modifier.requiredSize(sizeDp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx)
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it }
            ) {
                items(count) {
                    Box(Modifier.requiredSize(10.dp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.runOnIdle {
            assertThat(state.layoutInfo.totalItemsCount).isEqualTo(10)
            count = 20
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.totalItemsCount).isEqualTo(20)
        }
    }

    private fun ScalingLazyListLayoutInfo.assertVisibleItems(
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
}

@Stable
public class StableRef<T>(var value: T)