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

package androidx.wear.compose.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PickerGroupTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_test_tag() {
        rule.setContentWithTheme {
            PickerGroup(
                pickers = getPickerColumns(1),
                modifier = Modifier.testTag(TEST_TAG_1),
                pickerGroupState = rememberPickerGroupState()
            )
        }

        rule.onNodeWithTag(TEST_TAG_1).assertExists()
    }

    @Test
    fun state_returns_initially_selected_index_at_start() {
        lateinit var pickerGroupState: PickerGroupState
        val initiallySelectedColumn = 1
        rule.setContentWithTheme {
            PickerGroup(
                pickers = getPickerColumns(2),
                pickerGroupState = rememberPickerGroupState(initiallySelectedColumn).also {
                    pickerGroupState = it
                }
            )
        }

        rule.waitForIdle()

        assertThat(pickerGroupState.selectedIndex).isEqualTo(initiallySelectedColumn)
    }

    @Test
    fun remember_pickerGroupState_after_updates() {
        lateinit var selectedPicker: MutableState<Int>
        rule.setContentWithTheme {
            selectedPicker = remember { mutableStateOf(0) }
            val pickerGroupState = rememberPickerGroupState(selectedPicker.value)
            Text(text = "${pickerGroupState.selectedIndex}")
        }

        selectedPicker.value = 2
        rule.waitForIdle()

        rule.onNodeWithText("2").assertExists()
    }
    @Test
    fun pickers_are_added_to_picker_group() {
        val pickerColumnZero = getPickerColumnWithTag(TEST_TAG_1)
        val pickerColumnOne = getPickerColumnWithTag(TEST_TAG_2)

        rule.setContentWithTheme {
            PickerGroup(pickerColumnZero, pickerColumnOne)
        }

        rule.onNodeWithTag(TEST_TAG_1).assertExists()
        rule.onNodeWithTag(TEST_TAG_2).assertExists()
    }

    @Test
    fun picker_changes_focus_when_clicked() {
        lateinit var pickerGroupState: PickerGroupState
        val touchExplorationStateProvider =
            getTouchExplorationServiceState(touchExplorationServiceState = false)
        val pickerColumnZero = getPickerColumnWithTag(TEST_TAG_1)
        val pickerColumnOne = getPickerColumnWithTag(TEST_TAG_2)

        rule.setContentWithTheme {
            pickerGroupState = rememberPickerGroupState()
            PickerGroup(
                pickerColumnZero,
                pickerColumnOne,
                pickerGroupState = pickerGroupState,
                touchExplorationStateProvider = touchExplorationStateProvider
            )
        }

        rule.onNodeWithTag(TEST_TAG_2).performClick()
        rule.waitForIdle()

        assertThat(pickerGroupState.selectedIndex).isEqualTo(1)
    }

    private fun getPickerColumns(count: Int): Array<PickerGroupItem> = Array(count) {
        PickerGroupItem(pickerState = PickerState(10)) { _: Int, _: Boolean ->
            Box(modifier = Modifier.size(20.dp))
        }
    }

    private fun getPickerColumnWithTag(
        tag: String,
        onSelected: () -> Unit = {}
    ): PickerGroupItem {
        return PickerGroupItem(
            pickerState = PickerState(10),
            modifier = Modifier.testTag(tag),
            onSelected = onSelected
        ) { _: Int, _: Boolean ->
            Box(modifier = Modifier.size(20.dp))
        }
    }

    private fun getTouchExplorationServiceState(
        touchExplorationServiceState: Boolean
    ): TouchExplorationStateProvider {
        return TouchExplorationStateProvider { rememberUpdatedState(touchExplorationServiceState) }
    }

    private val TEST_TAG_1 = "random string 1"
    private val TEST_TAG_2 = "random string 2"
}
