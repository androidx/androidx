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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PickerGroupTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun supports_test_tag() {
        rule.setContentWithTheme {
            PickerGroup(modifier = Modifier.testTag(TEST_TAG_1)) {
                addPickerColumns(count = 1, selectedColumn = 0)
            }
        }

        rule.onNodeWithTag(TEST_TAG_1).assertExists()
    }

    @Test
    fun state_returns_initially_selected_index_at_start() {
        val initiallySelectedColumn = 1
        var selectedIndex = initiallySelectedColumn
        rule.setContentWithTheme {
            PickerGroup { addPickerColumns(count = 2, selectedColumn = selectedIndex) }
        }

        rule.waitForIdle()

        assertThat(selectedIndex).isEqualTo(initiallySelectedColumn)
    }

    @Test
    fun pickers_are_added_to_picker_group() {
        rule.setContentWithTheme {
            PickerGroup {
                addPickerColumnWithTag(TEST_TAG_1, isSelected = true)
                addPickerColumnWithTag(TEST_TAG_2, isSelected = false)
            }
        }

        rule.onNodeWithTag(TEST_TAG_1).assertExists()
        rule.onNodeWithTag(TEST_TAG_2).assertExists()
    }

    @Test
    fun picker_changes_focus_when_clicked() {
        lateinit var selectedIndex: MutableState<Int>
        val talkBackOff = overrideTalkBackState(touchExplorationServiceState = false)

        rule.setContentWithTheme {
            selectedIndex = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalTouchExplorationStateProvider provides talkBackOff) {
                PickerGroup {
                    addPickerColumnWithTag(
                        TEST_TAG_1,
                        isSelected = selectedIndex.value == 0,
                        onSelected = { selectedIndex.value = 0 },
                    )
                    addPickerColumnWithTag(
                        TEST_TAG_2,
                        isSelected = selectedIndex.value == 1,
                        onSelected = { selectedIndex.value = 1 },
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG_2).performClick()
        rule.waitForIdle()

        assertThat(selectedIndex.value).isEqualTo(1)
    }

    @Composable
    private fun PickerGroupScope.addPickerColumns(count: Int, selectedColumn: Int) =
        repeat(count) {
            PickerGroupItem(
                pickerState = rememberPickerState(10),
                selected = selectedColumn == it,
                onSelected = {},
            ) { index: Int, _: Boolean ->
                Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
            }
        }

    @Composable
    private fun PickerGroupScope.addPickerColumnWithTag(
        tag: String,
        isSelected: Boolean,
        onSelected: () -> Unit = {},
    ) =
        PickerGroupItem(
            selected = isSelected,
            pickerState = rememberPickerState(10),
            modifier = Modifier.testTag(tag),
            onSelected = onSelected,
        ) { _: Int, _: Boolean ->
            Box(modifier = Modifier.size(20.dp))
        }

    private fun overrideTalkBackState(
        touchExplorationServiceState: Boolean
    ): TouchExplorationStateProvider {
        return TouchExplorationStateProvider { rememberUpdatedState(touchExplorationServiceState) }
    }

    @Test
    fun autoCenterFalse_independentMultiScroll() {
        val pickerState1 = PickerState(10, initiallySelectedIndex = 5)
        val pickerState2 = PickerState(10, initiallySelectedIndex = 5)
        val talkBackOff = overrideTalkBackState(touchExplorationServiceState = false)
        lateinit var selectedColumn: MutableState<Int>

        rule.setContentWithTheme {
            selectedColumn = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalTouchExplorationStateProvider provides talkBackOff) {
                PickerGroup(autoCenter = false) {
                    PickerGroupItem(
                        pickerState = pickerState1,
                        selected = selectedColumn.value == 0,
                        onSelected = { selectedColumn.value = 0 },
                        modifier = Modifier.testTag(TEST_TAG_1),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                    PickerGroupItem(
                        pickerState = pickerState2,
                        selected = selectedColumn.value == 1,
                        onSelected = { selectedColumn.value = 1 },
                        modifier = Modifier.testTag(TEST_TAG_2),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                }
            }
        }

        val bounds1 = rule.onNodeWithTag(TEST_TAG_1).fetchSemanticsNode().boundsInRoot
        val bounds2 = rule.onNodeWithTag(TEST_TAG_2).fetchSemanticsNode().boundsInRoot

        assertThat(selectedColumn.value).isEqualTo(0)

        // Action 1: Put two fingers down simultaneously on different columns.
        rule.onRoot().performTouchInput {
            down(pointerId = 1, bounds1.center)
            down(pointerId = 2, bounds2.center)
            up(pointerId = 1)
            up(pointerId = 2)
        }

        rule.waitForIdle()

        // Assertion 1: When autoCenter is false, single-pointer semantics are NOT enforced.
        // Therefore, the second touch successfully registers and changes the selection to Column 1.
        assertThat(selectedColumn.value).isEqualTo(1)

        val initialIndex1 = pickerState1.selectedOptionIndex
        val initialIndex2 = pickerState2.selectedOptionIndex

        // Action 2: Swipe both fingers up simultaneously to trigger scrolling on both columns.
        rule.onRoot().performTouchInput {
            down(pointerId = 1, bounds1.center)
            down(pointerId = 2, bounds2.center)

            // Perform a proper swipe over time to generate velocity for the Picker to fling
            val steps = 10
            val delta = Offset(0f, -bounds1.height / steps)
            for (i in 1..steps) {
                moveBy(pointerId = 1, delta, delayMillis = 16)
                moveBy(pointerId = 2, delta, delayMillis = 16)
            }

            up(pointerId = 1)
            up(pointerId = 2)
        }

        rule.waitForIdle()

        // Assertion 2: Because autoCenter is false, both columns should process their
        // respective touch events and scroll independently.
        assertThat(pickerState1.selectedOptionIndex).isNotEqualTo(initialIndex1)
        assertThat(pickerState2.selectedOptionIndex).isNotEqualTo(initialIndex2)
    }

    @Test
    fun autoCenterTrue_sequentialTwoFinger_doesNotSelectOrScrollSecond() {
        val pickerState1 = PickerState(10, initiallySelectedIndex = 5)
        val pickerState2 = PickerState(10, initiallySelectedIndex = 5)
        val talkBackOff = overrideTalkBackState(touchExplorationServiceState = false)
        lateinit var selectedColumn: MutableState<Int>

        rule.setContentWithTheme {
            selectedColumn = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalTouchExplorationStateProvider provides talkBackOff) {
                PickerGroup(autoCenter = true) {
                    PickerGroupItem(
                        pickerState = pickerState1,
                        selected = selectedColumn.value == 0,
                        onSelected = { selectedColumn.value = 0 },
                        modifier = Modifier.testTag(TEST_TAG_1),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                    PickerGroupItem(
                        pickerState = pickerState2,
                        selected = selectedColumn.value == 1,
                        onSelected = { selectedColumn.value = 1 },
                        modifier = Modifier.testTag(TEST_TAG_2),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                }
            }
        }

        val bounds1 = rule.onNodeWithTag(TEST_TAG_1).fetchSemanticsNode().boundsInRoot
        val bounds2 = rule.onNodeWithTag(TEST_TAG_2).fetchSemanticsNode().boundsInRoot

        assertThat(selectedColumn.value).isEqualTo(0)

        val initialIndex1 = pickerState1.selectedOptionIndex
        val initialIndex2 = pickerState2.selectedOptionIndex

        // Action: Finger 1 touches down on Column 0. While dragging Finger 1,
        // Finger 2 touches down on Column 1 and also tries to drag.
        rule.onRoot().performTouchInput {
            down(pointerId = 1, bounds1.center)
            down(pointerId = 2, bounds2.center)

            // Perform a proper swipe over time
            val steps = 10
            val delta = Offset(0f, -bounds1.height / steps)
            for (i in 1..steps) {
                moveBy(pointerId = 1, delta, delayMillis = 16)
                moveBy(pointerId = 2, delta, delayMillis = 16)
            }

            up(pointerId = 1)
            up(pointerId = 2)
        }

        rule.waitForIdle()

        // Assertion 1: Because autoCenter is true and touch exploration is off, single-pointer
        // semantics are strictly enforced. The second touch (pointerId 2) must be completely
        // ignored, so Column 1 should NOT become selected.
        assertThat(selectedColumn.value).isEqualTo(0)

        // Assertion 2: The primary touch (pointerId 1) was allowed, so the first picker
        // should have successfully scrolled.
        assertThat(pickerState1.selectedOptionIndex).isNotEqualTo(initialIndex1)

        // Assertion 3: The secondary touch (pointerId 2) was consumed/blocked at the initial
        // phase, so the second picker should NOT have scrolled.
        assertThat(pickerState2.selectedOptionIndex).isEqualTo(initialIndex2)
    }

    @Test
    fun autoCenterTrue_lingeringFinger_doesNotSelectOrScroll() {
        val pickerState1 = PickerState(10, initiallySelectedIndex = 5)
        val pickerState2 = PickerState(10, initiallySelectedIndex = 5)
        val talkBackOff = overrideTalkBackState(touchExplorationServiceState = false)
        lateinit var selectedColumn: MutableState<Int>

        rule.setContentWithTheme {
            selectedColumn = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalTouchExplorationStateProvider provides talkBackOff) {
                PickerGroup(autoCenter = true) {
                    PickerGroupItem(
                        pickerState = pickerState1,
                        selected = selectedColumn.value == 0,
                        onSelected = { selectedColumn.value = 0 },
                        modifier = Modifier.testTag(TEST_TAG_1),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                    PickerGroupItem(
                        pickerState = pickerState2,
                        selected = selectedColumn.value == 1,
                        onSelected = { selectedColumn.value = 1 },
                        modifier = Modifier.testTag(TEST_TAG_2),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                }
            }
        }

        val bounds1 = rule.onNodeWithTag(TEST_TAG_1).fetchSemanticsNode().boundsInRoot
        val bounds2 = rule.onNodeWithTag(TEST_TAG_2).fetchSemanticsNode().boundsInRoot

        assertThat(selectedColumn.value).isEqualTo(0)

        val initialIndex1 = pickerState1.selectedOptionIndex
        val initialIndex2 = pickerState2.selectedOptionIndex

        // Action: Start a multi-touch gesture. Then lift the primary finger (pointerId 1)
        // while leaving the secondary finger (pointerId 2) on the screen. Try to swipe
        // with the lingering secondary finger.
        rule.onRoot().performTouchInput {
            down(pointerId = 1, bounds1.center)
            down(pointerId = 2, bounds2.center)

            // Lift the first finger, leaving the second one down
            up(pointerId = 1)

            // Now try to swipe with the lingering second finger
            val steps = 10
            val delta = Offset(0f, -bounds1.height / steps)
            for (i in 1..steps) {
                moveBy(pointerId = 2, delta, delayMillis = 16)
            }

            up(pointerId = 2)
        }

        rule.waitForIdle()

        // Assertion: Once a secondary touch is ignored by the single-pointer restriction,
        // it must remain ignored for its entire lifespan. Lifting the primary finger does
        // not "promote" the lingering finger to become the active pointer.
        // Therefore, Column 1 should neither become selected nor scroll.
        assertThat(selectedColumn.value).isEqualTo(0)
        assertThat(pickerState1.selectedOptionIndex).isEqualTo(initialIndex1)
        assertThat(pickerState2.selectedOptionIndex).isEqualTo(initialIndex2)
    }

    @Test
    fun autoCenterTrue_fullRelease_allowsNewGesture() {
        val pickerState1 = PickerState(10, initiallySelectedIndex = 5)
        val pickerState2 = PickerState(10, initiallySelectedIndex = 5)
        val talkBackOff = overrideTalkBackState(touchExplorationServiceState = false)
        lateinit var selectedColumn: MutableState<Int>

        rule.setContentWithTheme {
            selectedColumn = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalTouchExplorationStateProvider provides talkBackOff) {
                PickerGroup(autoCenter = true) {
                    PickerGroupItem(
                        pickerState = pickerState1,
                        selected = selectedColumn.value == 0,
                        onSelected = { selectedColumn.value = 0 },
                        modifier = Modifier.testTag(TEST_TAG_1),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                    PickerGroupItem(
                        pickerState = pickerState2,
                        selected = selectedColumn.value == 1,
                        onSelected = { selectedColumn.value = 1 },
                        modifier = Modifier.testTag(TEST_TAG_2),
                    ) { index: Int, _: Boolean ->
                        Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
                    }
                }
            }
        }

        val bounds1 = rule.onNodeWithTag(TEST_TAG_1).fetchSemanticsNode().boundsInRoot
        val bounds2 = rule.onNodeWithTag(TEST_TAG_2).fetchSemanticsNode().boundsInRoot

        assertThat(selectedColumn.value).isEqualTo(0)

        // Action 1: Execute a multi-touch gesture (touching Column 0, then Column 1).
        // Then, fully lift ALL fingers off the screen to end the gesture.
        rule.onRoot().performTouchInput {
            down(pointerId = 1, bounds1.center)
            down(pointerId = 2, bounds2.center)

            // Full release
            up(pointerId = 1)
            up(pointerId = 2)
        }

        rule.waitForIdle()

        // Assertion 1: Verify the multi-touch was handled correctly (second touch ignored).
        assertThat(selectedColumn.value).isEqualTo(0)

        // Action 2: Start a brand new, independent gesture on Column 1.
        rule.onRoot().performTouchInput {
            down(pointerId = 3, bounds2.center)
            up(pointerId = 3)
        }

        rule.waitForIdle()

        // Assertion 2: The single-pointer restriction must completely reset once all fingers
        // are lifted (!anyPointerDown). This brand new gesture should be recognized as a valid
        // new interaction, successfully selecting Column 1.
        assertThat(selectedColumn.value).isEqualTo(1)
    }
}

private const val TEST_TAG_1 = "random string 1"
private const val TEST_TAG_2 = "random string 2"
