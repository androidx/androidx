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
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
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
                1,
                modifier = Modifier.testTag(TEST_TAG),
                state = rememberPickerState()
            ) {
                Box(modifier = Modifier.size(20.dp))
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun can_scroll_picker_up_on_start() {
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    5,
                    state = rememberPickerState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 3),
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        // TODO(): Remove when we can specify the initially selected item in the state.
        val initiallySelectedItem = state.selectedOption
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = Offset(centerX, bottom),
                end = Offset(centerX, bottom - itemSizePx * 16), // 3 loops + 1 element
                endVelocity = 0f // Ensure it's not a fling.
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(initiallySelectedItem + 1)
    }

    @Test
    fun can_scroll_picker_down_on_start() {
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    5,
                    state = rememberPickerState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 3),
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        val initiallySelectedItem = state.selectedOption
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = Offset(centerX, top),
                end = Offset(centerX, top + itemSizePx * 16), // 3 loops + 1 element
                endVelocity = 0f // Ensure it's not a fling.
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(initiallySelectedItem - 1)
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
                    20,
                    state = rememberPickerState().also { state = it },
                    modifier = Modifier.testTag(TEST_TAG)
                        .requiredSize(itemSizeDp * 11 + separationDp * 10 * separationSign),
                    separation = separationDp * separationSign
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        val initiallySelectedItem = state.selectedOption
        val itemsToScroll = 4

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(
                startY = bottom,
                endY = bottom -
                (itemSizePx + separationPx * separationSign) * itemsToScroll,
                durationMillis = 1000L
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(initiallySelectedItem + itemsToScroll)
    }

    /* TODO(199476914): Add tests for non-wraparound pickers to ensure they have the correct range
     * of scroll.
     */
}
