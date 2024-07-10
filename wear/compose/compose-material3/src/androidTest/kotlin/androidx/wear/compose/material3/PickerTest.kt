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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PickerTest {
    @get:Rule val rule = createComposeRule()

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
                state = rememberPickerState(1),
                contentDescription = CONTENT_DESCRIPTION,
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
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(itemSizeDp * 3),
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(0)
    }

    @Test
    fun state_returns_initially_selected_option_at_start() {
        val startValue = 5
        lateinit var state: PickerState

        rule.setContent {
            state =
                rememberPickerState(
                    initialNumberOfOptions = 10,
                    initiallySelectedOption = startValue
                )
        }

        assertThat(state.selectedOption).isEqualTo(startValue)
    }

    @Test
    fun can_scroll_picker_up_on_start() {
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(5).also { state = it },
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(itemSizeDp * 3),
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
                endVelocity = NOT_A_FLING_SPEED
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
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(itemSizeDp * 3),
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
                endVelocity = NOT_A_FLING_SPEED
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(numberOfOptions - 1)
    }

    @Test fun uses_positive_separation_correctly() = scroll_with_separation(1)

    @Test fun uses_negative_separation_correctly() = scroll_with_separation(-1)

    /**
     * Test that picker is properly scrolled with scrollOffset, which equals to a half of the
     * (itemSizePx + separationPx) and minus 1 pixel for making it definitely less than a half of an
     * item
     */
    @Test
    fun scroll_with_positive_separation_and_offset() =
        scroll_with_separation(1, scrollOffset = (itemSizePx + separationPx) / 2 - 1)

    /**
     * Test that picker is properly scrolled with scrollOffset, which equals to a half of the
     * (itemSizePx - separationPx) and minus 1 pixel for making it definitely less than a half of an
     * item
     */
    @Test
    fun scroll_with_negative_separation_and_offset() =
        scroll_with_separation(-1, scrollOffset = (itemSizePx - separationPx) / 2 - 1)

    private fun scroll_with_separation(separationSign: Int, scrollOffset: Int = 0) {
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(20).also { state = it },
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(itemSizeDp * 11 + separationDp * 10 * separationSign),
                    spacing = separationDp * separationSign
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        val itemsToScroll = 4

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            // Start at bottom - 5 to allow for around 2.dp padding around the Picker
            // (which was added to prevent jitter around the start of the gradient).
            swipeWithVelocity(
                start = Offset(centerX, bottom - 5),
                end =
                    Offset(
                        centerX,
                        bottom -
                            5 -
                            scrollOffset -
                            (itemSizePx + separationPx * separationSign) * itemsToScroll
                    ),
                endVelocity = NOT_A_FLING_SPEED
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(itemsToScroll)
    }

    @Test
    fun scroll_to_next_item_with_animation() {
        animateScrollTo(
            initialOption = 2,
            targetOption = 3,
            totalOptions = 10,
            expectedItemsScrolled = 1,
        )
    }

    @Test
    fun scroll_forward_by_two_items_with_animation() {
        animateScrollTo(
            initialOption = 2,
            targetOption = 4,
            totalOptions = 10,
            expectedItemsScrolled = 2,
        )
    }

    @Test
    fun scroll_to_prev_item_with_animation() {
        animateScrollTo(
            initialOption = 2,
            targetOption = 1,
            totalOptions = 5,
            expectedItemsScrolled = -1,
        )
    }

    @Test
    fun scroll_backward_by_two_items_with_animation() {
        animateScrollTo(
            initialOption = 3,
            targetOption = 1,
            totalOptions = 5,
            expectedItemsScrolled = -2,
        )
    }

    @Test
    fun scroll_forward_to_repeated_items_with_animation() {
        animateScrollTo(
            initialOption = 8,
            targetOption = 2,
            totalOptions = 10,
            expectedItemsScrolled = 4,
        )
    }

    @Test
    fun scroll_backward_to_repeated_items_with_animation() {
        animateScrollTo(
            initialOption = 2,
            targetOption = 8,
            totalOptions = 10,
            expectedItemsScrolled = -4,
        )
    }

    @Test
    fun scroll_to_the_closest_item_with_animation() {
        animateScrollTo(
            initialOption = 2,
            targetOption = 0,
            totalOptions = 4,
            expectedItemsScrolled = -2,
        )
    }

    @Test
    fun animate_scroll_cancels_previous_animation() {
        val initialOption = 5
        val totalOptions = 10
        val firstTarget = 7
        val secondTarget = 9

        val targetDelta = 4

        lateinit var state: PickerState
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            state =
                rememberPickerState(
                    initialNumberOfOptions = totalOptions,
                    initiallySelectedOption = initialOption
                )
            SimplePicker(state)
        }
        val initialItemIndex = state.scalingLazyListState.centerItemIndex

        // The first animation starts, but before it's finished - a second animation starts,
        // which cancels the first animation. In the end it doesn't matter how far picker was
        // scrolled during first animation, because the second animation should bring
        // picker to its final target.
        rule.runOnIdle {
            scope.launch {
                async { state.animateScrollToOption(firstTarget) }
                delay(100) // a short delay so that the first async will be triggered first
                async { state.animateScrollToOption(secondTarget) }
            }
        }
        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(secondTarget)
        assertThat(state.scalingLazyListState.centerItemIndex)
            .isEqualTo(initialItemIndex + targetDelta)
    }

    @Test fun scrolls_with_negative_separation() = scrolls_to_index_correctly(-1, 3)

    @Test fun scrolls_with_positive_separation() = scrolls_to_index_correctly(1, 7)

    @Test fun scrolls_with_no_separation() = scrolls_to_index_correctly(0, 13)

    private fun scrolls_to_index_correctly(separationSign: Int, targetIndex: Int) {
        val pickerDriver = PickerDriver(separationSign = separationSign)
        rule.setContent { pickerDriver.DrivedPicker() }

        rule.runOnIdle { runBlocking { pickerDriver.state.scrollToOption(targetIndex) } }
        rule.waitForIdle()

        assertThat(pickerDriver.state.selectedOption).isEqualTo(targetIndex)
        pickerDriver.verifyCenterItemIsCentered()
    }

    @Test
    fun keeps_selection_when_increasing_options() {
        val initialOption = 25
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state =
                        rememberPickerState(
                                initialNumberOfOptions = 28,
                                initiallySelectedOption = initialOption
                            )
                            .also { state = it },
                    contentDescription = CONTENT_DESCRIPTION,
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        val initialItemIndex = state.scalingLazyListState.centerItemIndex
        rule.runOnIdle { runBlocking { state.numberOfOptions = 31 } }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(initialOption)
        assertThat(state.scalingLazyListState.centerItemIndex).isEqualTo(initialItemIndex)
    }

    @Test
    fun scrolls_to_correct_index_after_increasing_options() {
        val initialOption = 5
        val targetOption = 43
        lateinit var state: PickerState
        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state =
                        rememberPickerState(
                                initialNumberOfOptions = 25,
                                initiallySelectedOption = initialOption
                            )
                            .also { state = it },
                    contentDescription = CONTENT_DESCRIPTION,
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.numberOfOptions = 57
                state.scrollToOption(targetOption)
            }
        }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(targetOption)
    }

    @Test
    fun scrolls_before_initialization_correctly() {
        lateinit var state: PickerState
        val targetIndex = 5
        rule.setContent {
            state = rememberPickerState(20)
            LaunchedEffect(state) { state.scrollToOption(targetIndex) }

            Picker(
                state = state,
                contentDescription = CONTENT_DESCRIPTION,
            ) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
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
            Picker(
                state = state,
                contentDescription = CONTENT_DESCRIPTION,
            ) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
        }

        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(targetIndex)
    }

    @Test
    fun small_scroll_down_snaps() = scroll_snaps {
        swipeWithVelocity(
            start = Offset(centerX, top),
            end = Offset(centerX, top + itemSizePx / 2),
            endVelocity = NOT_A_FLING_SPEED
        )
    }

    @Test
    fun small_scroll_up_snaps() = scroll_snaps {
        swipeWithVelocity(
            start = Offset(centerX, bottom),
            end = Offset(centerX, bottom - itemSizePx / 2),
            endVelocity = NOT_A_FLING_SPEED
        )
    }

    @Test
    fun small_scroll_snaps_with_separation() =
        scroll_snaps(separationSign = 1) {
            swipeWithVelocity(
                start = Offset(centerX, top),
                end = Offset(centerX, top + itemSizePx / 2),
                endVelocity = NOT_A_FLING_SPEED
            )
        }

    @Test
    fun fast_scroll_snaps() = scroll_snaps {
        swipeWithVelocity(
            start = Offset(centerX, top),
            end = Offset(centerX, top + 300),
            endVelocity = DO_FLING_SPEED
        )
    }

    @Test
    fun fast_scroll_with_separation_snaps() =
        scroll_snaps(separationSign = -1) {
            swipeWithVelocity(
                start = Offset(centerX, bottom),
                end = Offset(centerX, bottom - 300),
                endVelocity = DO_FLING_SPEED
            )
        }

    @Test
    fun displays_label_when_read_only() {
        val labelText = "Show Me"

        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(5),
                    readOnly = true,
                    readOnlyLabel = { Text(text = labelText) },
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(itemSizeDp * 3)
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithText(labelText).assertExists()
    }

    @Test
    fun hides_label_when_not_read_only() {
        val labelText = "Show Me"

        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(5),
                    readOnly = false,
                    readOnlyLabel = { Text(text = labelText) },
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(itemSizeDp * 3)
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithText(labelText).assertDoesNotExist()
    }

    @Test
    fun displays_selected_option_when_read_only() {
        val readOnly = mutableStateOf(false)
        val initialOption = 4
        val selectedOption = 2
        lateinit var state: PickerState

        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state =
                        rememberPickerState(
                                initialNumberOfOptions = 5,
                                initiallySelectedOption = initialOption
                            )
                            .also { state = it },
                    readOnly = readOnly.value,
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(itemSizeDp * 3),
                ) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
                LaunchedEffect(state) { state.scrollToOption(selectedOption) }
            }
        }
        readOnly.value = true
        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(selectedOption)
    }

    @Test
    fun can_scroll_picker_up_when_read_only() {
        val initialOption = 3
        lateinit var state: PickerState

        rule.setContent {
            WithTouchSlop(0f) {
                Picker(
                    state =
                        rememberPickerState(
                                initialNumberOfOptions = 5,
                                initiallySelectedOption = initialOption
                            )
                            .also { state = it },
                    readOnly = true,
                    contentDescription = CONTENT_DESCRIPTION,
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(itemSizeDp * 3),
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
                endVelocity = NOT_A_FLING_SPEED
            )
        }

        rule.waitForIdle()
        assertThat(state.selectedOption).isNotEqualTo(initialOption)
    }

    @Test
    fun scrolls_from_non_canonical_option_works() {
        lateinit var scope: CoroutineScope
        val pickerDriver = PickerDriver(separationSign = 1)
        rule.setContent {
            scope = rememberCoroutineScope()
            pickerDriver.DrivedPicker()
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            // Move before the first item, so we are not in the "canonical" items
            // (the ones that will be used on a recently created Picker)
            swipeWithVelocity(
                start = Offset(centerX, top),
                end = Offset(centerX, top + 300),
                endVelocity = DO_FLING_SPEED
            )
        }
        rule.waitForIdle()
        pickerDriver.readOnly.value = true
        rule.waitForIdle()
        rule.runOnUiThread {
            scope.launch {
                pickerDriver.state.scrollToOption(
                    (pickerDriver.state.selectedOption + 1) % pickerDriver.state.numberOfOptions
                )
                pickerDriver.readOnly.value = false
            }
        }
        rule.waitForIdle()

        pickerDriver.verifyCenterItemIsCentered()
    }

    @Test
    fun rememberPickerState_updates_after_new_inputs() {
        val numberOfOptions = 10
        lateinit var selectedOption: MutableState<Int>
        rule.setContent {
            selectedOption = remember { mutableStateOf(1) }
            val pickerState =
                rememberPickerState(
                    initialNumberOfOptions = numberOfOptions,
                    initiallySelectedOption = selectedOption.value
                )
            Text(text = "${pickerState.selectedOption}")
        }

        // Update selected option to a new value - should also update the PickerState instance,
        // and then recompose with the new value in the Text element.
        selectedOption.value = 2
        rule.waitForIdle()

        rule.onNodeWithText("2").assertExists()
    }

    private fun animateScrollTo(
        initialOption: Int,
        targetOption: Int,
        totalOptions: Int,
        expectedItemsScrolled: Int,
    ) {
        lateinit var state: PickerState
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            state =
                rememberPickerState(
                    initialNumberOfOptions = totalOptions,
                    initiallySelectedOption = initialOption
                )
            SimplePicker(state)
        }

        val initialItemIndex = state.scalingLazyListState.centerItemIndex
        rule.runOnIdle { scope.launch { async { state.animateScrollToOption(targetOption) } } }
        rule.waitForIdle()
        assertThat(state.selectedOption).isEqualTo(targetOption)
        assertThat(state.scalingLazyListState.centerItemIndex)
            .isEqualTo(initialItemIndex + expectedItemsScrolled)
    }

    @Composable
    private fun SimplePicker(state: PickerState) {
        WithTouchSlop(0f) {
            Picker(
                state = state,
                contentDescription = CONTENT_DESCRIPTION,
            ) {
                Box(Modifier.requiredSize(itemSizeDp))
            }
        }
    }

    private fun scroll_snaps(
        separationSign: Int = 0,
        touchInput: (TouchInjectionScope).() -> Unit,
    ) {
        val pickerDriver = PickerDriver(separationSign)
        rule.setContent { pickerDriver.DrivedPicker() }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput { touchInput() }
        rule.waitForIdle()

        pickerDriver.verifyCenterItemIsCentered()
    }

    private inner class PickerDriver(val separationSign: Int) {
        lateinit var state: PickerState
        val readOnly: MutableState<Boolean> = mutableStateOf(false)
        private lateinit var pickerLayoutCoordinates: LayoutCoordinates
        private val numberOfItems = 20
        private lateinit var centerItemLayoutCoordinates: LayoutCoordinates
        private var pickerHeightPx = 0f
        private val itemsToShow = 11

        @Composable
        fun DrivedPicker() {
            val pickerHeightDp =
                itemSizeDp * itemsToShow + separationDp * (itemsToShow - 1) * separationSign
            pickerHeightPx = with(LocalDensity.current) { pickerHeightDp.toPx() }
            WithTouchSlop(0f) {
                Picker(
                    state = rememberPickerState(numberOfItems).also { state = it },
                    modifier =
                        Modifier.testTag(TEST_TAG)
                            .requiredSize(pickerHeightDp)
                            .onGloballyPositioned { pickerLayoutCoordinates = it },
                    spacing = separationDp * separationSign,
                    readOnly = readOnly.value,
                    contentDescription = CONTENT_DESCRIPTION,
                ) { optionIndex ->
                    Box(
                        Modifier.requiredSize(itemSizeDp).onGloballyPositioned {
                            // Save the layout coordinates if we are at the center
                            if (optionIndex == selectedOption) {
                                centerItemLayoutCoordinates = it
                            }
                        }
                    )
                }
            }
        }

        fun verifyCenterItemIsCentered() {
            // Ensure that the option that ended up in the middle after the fling/scroll is centered
            assertThat(
                    centerItemLayoutCoordinates.positionInWindow().y -
                        pickerLayoutCoordinates.positionInWindow().y
                )
                .isWithin(0.1f)
                .of(pickerHeightPx / 2f - itemSizePx / 2f)
        }
    }

    // The threshold is 1f, and the specified velocity is not exactly achieved by swipeWithVelocity
    private val NOT_A_FLING_SPEED = 0.9f
    private val DO_FLING_SPEED = 10000f
    private val CONTENT_DESCRIPTION = "content description"

    /* TODO(199476914): Add tests for non-wraparound pickers to ensure they have the correct range
     * of scroll.
     */
}
