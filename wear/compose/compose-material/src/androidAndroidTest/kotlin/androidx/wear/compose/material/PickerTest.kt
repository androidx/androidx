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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PickerTest {
    @get:Rule
    val rule = createComposeRule()

    private var itemSizePx: Int = 20
    private var itemSizeDp: Dp = Dp.Infinity
    private var separationPx: Int = 10
    private var separationDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            separationDp = separationPx.toDp()
        }
    }

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Picker(
                modifier = Modifier.testTag(TEST_TAG),
                state = rememberPickerState(1)
            ) {
                Box(modifier = Modifier.size(20.dp))
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun start_on_first_item() {
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(5).also { state = it },
                    modifier = Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 3)
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(0)
    }

    @Test
    fun can_scroll_picker_up_on_start() {
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(5).also { state = it },
                    modifier = Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 3)
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = Offset(centerX, bottom),
                end = Offset(centerX, bottom - itemSizePx * 16), // 3 loops + 1 element
                endVelocity = 1f, // Ensure it's not a fling.
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(1)
    }

   @Test
   fun can_scroll_picker_down_on_start() {
        val numberOfOptions = 5
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(numberOfOptions).also { state = it },
                    modifier = Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 3),
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = Offset(centerX, top),
                end = Offset(centerX, top + itemSizePx * 16), // 3 loops + 1 element
                endVelocity = 1f, // Ensure it's not a fling.
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(numberOfOptions - 1)
    }

    @Test
    fun uses_positive_separation_correctly() =
        uses_separation_correctly(1)

    @Test
    fun uses_negative_separation_correctly() =
        uses_separation_correctly(-1)

    private fun uses_separation_correctly(separationSign: Int) {
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(20).also { state = it },
                    modifier = Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 11 + separationDp * 10 * separationSign),
                    separation = separationDp * separationSign
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        val itemsToScroll = 4

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = Offset(centerX, bottom),
                end = Offset(centerX, bottom -
                    (itemSizePx + separationPx * separationSign) * itemsToScroll),
                endVelocity = 1f, // Ensure it's not a fling.
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(itemsToScroll)
    }

    @Test
    fun scrolls_with_negative_separation() = scrolls_to_index_correctly(-1, 3)

    @Test
    fun scrolls_with_positive_separation() = scrolls_to_index_correctly(1, 7)

    @Test
    fun scrolls_with_no_separation() = scrolls_to_index_correctly(0, 13)

    private fun scrolls_to_index_correctly(separationSign: Int, targetIndex: Int) {
        lateinit var state: PickerState
        lateinit var pickerLayoutCoordinates: LayoutCoordinates
        lateinit var centerOptionLayoutCoordinates: LayoutCoordinates
        var pickerHeightPx = 0f
        rule.setContent {
            val pickerHeightDp = itemSizeDp * 11 + separationDp * 10 * separationSign
            pickerHeightPx = with(LocalDensity.current) { pickerHeightDp.toPx() }
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(20).also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(pickerHeightDp)
                        .onGloballyPositioned { pickerLayoutCoordinates = it },
                    separation = separationDp * separationSign
                ) { optionIndex ->
                    Box(Modifier.requiredSize(itemSizeDp).onGloballyPositioned {
                        // Save the layout coordinates of the selected option
                        if (optionIndex == targetIndex) {
                            centerOptionLayoutCoordinates = it
                        }
                    })
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToOption(targetIndex)
            }
        }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(targetIndex)
        assertThat(centerOptionLayoutCoordinates.positionInWindow().y -
            pickerLayoutCoordinates.positionInWindow().y)
            .isWithin(0.1f)
            .of(pickerHeightPx / 2f - itemSizePx / 2f)
    }

    @Test
    fun scrolls_before_initialization_correctly() {
        lateinit var state: PickerState
        val targetIndex = 5
        rule.setContent {
            state = rememberPickerState(20)
            LaunchedEffect(state) {
                state.scrollToOption(targetIndex)
            }

            Picker(state = state) { Box(Modifier.requiredSize(itemSizeDp)) }
        }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(targetIndex)
    }

    @Test
    fun setting_initial_value_on_state_works() {
        lateinit var state: PickerState
        val targetIndex = 5
        rule.setContent {
            state = rememberPickerState(20, initiallySelectedOption = targetIndex)
            Picker(state = state) { Box(Modifier.requiredSize(itemSizeDp)) }
        }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(targetIndex)
    }

    @Test
    fun small_scroll_down_snaps() = scroll_snaps {
        swipeWithVelocity(
            start = Offset(centerX, top),
            end = Offset(centerX, top + itemSizePx / 2),
            endVelocity = 1f, // Ensure it's not a fling.
        )
    }

    @Test
    fun small_scroll_up_snaps() = scroll_snaps {
        swipeWithVelocity(
            start = Offset(centerX, bottom),
            end = Offset(centerX, bottom - itemSizePx / 2),
            endVelocity = 1f, // Ensure it's not a fling.
        )
    }

    @Test
    fun small_scroll_snaps_with_separation() = scroll_snaps(separationSign = 1) {
        swipeWithVelocity(
            start = Offset(centerX, top),
            end = Offset(centerX, top + itemSizePx / 2),
            endVelocity = 1f, // Ensure it's not a fling.
        )
    }

    @Test
    fun fast_scroll_snaps() = scroll_snaps {
        swipeWithVelocity(
            start = Offset(centerX, top),
            end = Offset(centerX, top + 300),
            endVelocity = 10000f, // Ensure it IS a fling.
        )
    }

    @Test
    fun fast_scroll_with_separation_snaps() = scroll_snaps(separationSign = -1) {
        swipeWithVelocity(
            start = Offset(centerX, bottom),
            end = Offset(centerX, bottom - 300),
            endVelocity = 10000f, // Ensure it IS a fling.
        )
    }

    private fun scroll_snaps(separationSign: Int = 0, touch: (TouchInjectionScope).() -> Unit) {
        lateinit var state: PickerState
        lateinit var pickerLayoutCoordinates: LayoutCoordinates
        val numberOfItems = 20
        val optionLayoutCoordinates = Array<LayoutCoordinates?>(numberOfItems) { null }
        var pickerHeightPx = 0f
        val itemsToShow = 11
        rule.setContent {
            val pickerHeightDp = itemSizeDp * itemsToShow +
                separationDp * (itemsToShow - 1) * separationSign
            pickerHeightPx = with(LocalDensity.current) { pickerHeightDp.toPx() }
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(numberOfItems).also { state = it },
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(pickerHeightDp)
                        .onGloballyPositioned { pickerLayoutCoordinates = it },
                    separation = separationDp * separationSign
                ) { optionIndex ->
                    Box(Modifier.requiredSize(itemSizeDp).onGloballyPositioned {
                        // Save the layout coordinates
                        optionLayoutCoordinates[optionIndex] = it
                    })
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).performTouchInput { touch() }

        rule.waitForIdle()

        // Ensure that the option that ended up in the middle after the fling/scroll is centered
        assertThat(optionLayoutCoordinates[state.selectedOption]!!.positionInWindow().y -
            pickerLayoutCoordinates.positionInWindow().y)
            .isWithin(0.1f)
            .of(pickerHeightPx / 2f - itemSizePx / 2f)
    }

    /* TODO(199476914): Add tests for non-wraparound pickers to ensure they have the correct range
     * of scroll.
     */
}
