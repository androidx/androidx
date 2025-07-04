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

// We are testing that we did not break the existing behavior
// TODO: b/369332589 - Move to the new API in a follow up cl
@file:Suppress("Deprecation")

package androidx.wear.compose.foundation

import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalWearFoundationApi::class)
@RunWith(TestParameterInjector::class)
class HierarchicalFocusTest {
    @get:Rule val rule = createComposeRule()

    @Test fun basic_OnFocusChange_works_1_of_3() = basic_OnFocusChange_works(0, 3)

    @Test fun basic_OnFocusChange_works_2_of_5() = basic_OnFocusChange_works(1, 5)

    @Test fun basic_OnFocusChange_works_4_of_4() = basic_OnFocusChange_works(3, 4)

    private fun basic_OnFocusChange_works(selected: Int, numItems: Int) {
        var focused = BooleanArray(numItems)

        rule.setContent {
            repeat(numItems) { ix ->
                HierarchicalFocusCoordinator({ ix == selected }) {
                    ActiveFocusListener(onFocusChanged = { focused[ix] = it })
                }
            }
        }

        rule.runOnIdle { checkFocus(selected, focused) }
    }

    @Test fun basic_UpdateFocus_works_0_to_0_of_3() = basic_UpdateFocus_works(0, 0, 3)

    @Test fun basic_UpdateFocus_works_0_to_1_of_3() = basic_UpdateFocus_works(0, 1, 3)

    @Test fun basic_UpdateFocus_works_2_to_0_of_5() = basic_UpdateFocus_works(2, 0, 5)

    private fun basic_UpdateFocus_works(initiallySelected: Int, selected: Int, numItems: Int) {
        var focused = BooleanArray(numItems)

        var currentlySelected by mutableStateOf(initiallySelected)

        rule.setContent {
            repeat(numItems) { ix ->
                HierarchicalFocusCoordinator({ ix == currentlySelected }) {
                    ActiveFocusListener(onFocusChanged = { focused[ix] = it })
                }
            }
        }

        rule.runOnIdle { currentlySelected = selected }

        rule.runOnIdle { checkFocus(selected, focused) }
    }

    @Test fun basic_selection_works_1_of_3() = basic_selection_works(0, 3)

    @Test fun basic_selection_works_2_of_5() = basic_selection_works(1, 5)

    @Test fun basic_selection_works_4_of_4() = basic_selection_works(3, 4)

    private fun basic_selection_works(selected: Int, numItems: Int) {
        var focused = BooleanArray(numItems)

        rule.setContent {
            Box {
                repeat(numItems) { ix ->
                    HierarchicalFocusCoordinator({ ix == selected }) {
                        FocusableTestItem { focused[ix] = it }
                    }
                }
            }
        }

        rule.runOnIdle { checkFocus(selected, focused) }
    }

    @Test fun nested_selection_initial() = nested_selection_works(0) { _, _ -> }

    @Test
    fun nested_selection_switch_top() = nested_selection_works(3) { top, _ -> top.intValue = 1 }

    @Test
    fun nested_selection_switch_bottom() =
        nested_selection_works(1) { _, bottom ->
            bottom[0].intValue = 1
            bottom[1].intValue = 2
            bottom[2].intValue = 0
        }

    @Test
    fun nested_selection_switch_both() =
        nested_selection_works(5) { top, bottom ->
            bottom[0].intValue = 1
            bottom[1].intValue = 2
            bottom[2].intValue = 0
            top.intValue = 1
        }

    /*
     * We have 3 top FocusControl groups, each having 3 bottom FocusControl groups, and the
     * leaf focusable items are numbered 0 to 8
     *
     * *------*---------*---*
     * |      | Bottom0 | 0 |
     * | Top0 | Bottom1 | 1 |
     * |      | Bottom3 | 2 |
     * *------*---------*---*
     * |      | Bottom0 | 3 |
     * | Top1 | Bottom1 | 4 |
     * |      | Bottom3 | 5 |
     * *------*---------*---*
     * |      | Bottom0 | 6 |
     * | Top2 | Bottom1 | 7 |
     * |      | Bottom3 | 8 |
     * *------*---------*---*
     */
    private fun nested_selection_works(
        expectedSelected: Int,
        act: (MutableIntState, Array<MutableIntState>) -> Unit,
    ) {
        val numItems = 3
        var focused = BooleanArray(numItems * numItems)
        val topSelected = mutableIntStateOf(0)
        val bottomSelected = Array(numItems) { mutableIntStateOf(0) }

        rule.setContent {
            Box {
                repeat(numItems) { topIx ->
                    HierarchicalFocusCoordinator({ topIx == topSelected.intValue }) {
                        Box {
                            repeat(numItems) { bottomIx ->
                                HierarchicalFocusCoordinator({
                                    bottomIx == bottomSelected[topIx].intValue
                                }) {
                                    FocusableTestItem { focused[topIx * numItems + bottomIx] = it }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle { act(topSelected, bottomSelected) }

        rule.runOnIdle { checkFocus(expectedSelected, focused) }
    }

    @Test
    public fun release_focus_works() {
        val selected = mutableStateOf(0)
        var focused = false
        rule.setContent {
            Box(Modifier.focusable()) {
                HierarchicalFocusCoordinator({ selected.value == 0 }) {
                    FocusableTestItem { focused = it }
                }
                HierarchicalFocusCoordinator({ selected.value == 1 }) {
                    // Nothing to see here
                }
            }
        }

        rule.runOnIdle {
            Assert.assertTrue(focused)

            // Select the "empty tab"
            selected.value = 1
        }

        rule.runOnIdle { Assert.assertFalse(focused) }
    }

    @Test
    public fun change_composition_works(@TestParameter targetShow: Boolean) {
        var show by mutableStateOf(!targetShow)
        var focused = false
        rule.setContent {
            Box {
                HierarchicalFocusCoordinator({ true }) {
                    if (show) {
                        FocusableTestItem { focused = it }
                    }
                }
            }
        }

        rule.runOnIdle { show = targetShow }

        rule.runOnIdle { Assert.assertEquals(targetShow, focused) }
    }

    @Test
    public fun focus_not_required_reported_correctly() {
        var focused = false
        rule.setContent {
            Box {
                HierarchicalFocusCoordinator(requiresFocus = { false }) {
                    FocusableTestItem { focused = it }
                }
            }
        }

        rule.runOnIdle { Assert.assertFalse(focused) }
    }

    @Test
    public fun updating_requiresFocus_lambda_works() {
        var lambdaUpdated by mutableStateOf(false)
        var focused = false
        rule.setContent {
            Box {
                HierarchicalFocusCoordinator(
                    // We switch between a lambda that always returns false and one that always
                    // return true given the state of lambdaUpdated.
                    requiresFocus =
                        if (lambdaUpdated) {
                            { true }
                        } else {
                            { false }
                        }
                ) {
                    FocusableTestItem { focused = it }
                }
            }
        }

        rule.runOnIdle { lambdaUpdated = true }

        rule.runOnIdle { Assert.assertTrue(focused) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    public fun no_focusable_children_clears_focus() {
        var focused = false
        var requiresFocus by mutableStateOf(true)
        rule.setContent {
            Box {
                HierarchicalFocusCoordinator(requiresFocus = { requiresFocus }) {
                    FocusableTestItem { focused = it }
                }
                HierarchicalFocusCoordinator(requiresFocus = { false }) {
                    Box(Modifier.size(50.dp))
                }
            }
        }

        rule.runOnIdle {
            Assert.assertTrue(focused)
            requiresFocus = false
        }

        rule.runOnIdle { Assert.assertFalse(focused) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    public fun nested_change_to_no_focus(@TestParameter focusableOnSecondRow: Boolean) {
        var focused = false
        var focusedSecondRow = false
        var requiresFocusFirstRow by mutableStateOf(false)
        var requiresFocusNested by mutableStateOf(true)
        rule.setContent {
            Box {
                // First "Row"
                HierarchicalFocusCoordinator(requiresFocus = { requiresFocusFirstRow }) {
                    HierarchicalFocusCoordinator(requiresFocus = { requiresFocusNested }) {
                        FocusableTestItem { focused = it }
                    }
                    HierarchicalFocusCoordinator(requiresFocus = { false }) {
                        Box(Modifier.size(50.dp))
                    }
                }
                // Second "Row"
                HierarchicalFocusCoordinator(requiresFocus = { !requiresFocusFirstRow }) {
                    if (focusableOnSecondRow) {
                        FocusableTestItem { focusedSecondRow = it }
                    }
                }
            }
        }

        rule.runOnIdle {
            // Initially, the second row is selected go to selecting the first row but no element on
            // it.
            requiresFocusFirstRow = true
            requiresFocusNested = false
        }

        rule.runOnIdle {
            // Nothing is selected on the first row, nor the second row.
            Assert.assertFalse(focused)
            Assert.assertFalse(focusedSecondRow)
        }
    }

    @Test
    fun adding_inactive_hfcs_before_no_callbacks() = adding_inactive_hfcs_no_callbacks(true, false)

    @Test
    fun adding_inactive_hfcs_after_no_callbacks() = adding_inactive_hfcs_no_callbacks(false, true)

    @Test
    fun adding_inactive_hfcs_both_no_callbacks() = adding_inactive_hfcs_no_callbacks(true, true)

    private fun adding_inactive_hfcs_no_callbacks(addBefore: Boolean, addAfter: Boolean) {
        val hasHfcBefore = mutableStateOf(false)
        val hasHfcAfter = mutableStateOf(false)
        var numCallbacks = 0
        rule.setContent {
            if (hasHfcBefore.value) {
                Box(Modifier.hierarchicalFocusGroup(active = false).size(50.dp))
            }
            Box(Modifier.hierarchicalOnFocusChanged { numCallbacks++ }.size(50.dp))
            if (hasHfcAfter.value) {
                Box(Modifier.hierarchicalFocusGroup(active = false).size(50.dp))
            }
        }

        rule.runOnIdle {
            numCallbacks = 0
            hasHfcBefore.value = addBefore
            hasHfcAfter.value = addAfter
        }

        rule.waitForIdle()

        // Adding inactive hfcs changes nothing.
        assertEquals(0, numCallbacks)
    }

    @Test
    fun disabled_hfc_does_nothing() {
        val focusRequester = FocusRequester()
        val selected = mutableIntStateOf(0)
        var activeFocusCalls = 0
        var focused = false
        rule.setContent {
            Column(Modifier.hierarchicalFocusGroup(false)) {
                repeat(3) {
                    Box(
                        Modifier.size(10.dp)
                            .hierarchicalFocusGroup(it == selected.intValue)
                            .hierarchicalOnFocusChanged { activeFocusCalls++ }
                            .focusable()
                    )
                }
                Box(
                    Modifier.size(10.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                )
            }

            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }

        rule.runOnIdle { selected.intValue = 1 }

        rule.runOnIdle { selected.intValue = 2 }

        rule.waitForIdle()

        assertEquals(0, activeFocusCalls)
        assertTrue(focused)
    }

    @Test
    fun on_active_focus_alone_works() {
        var activeFocus: Boolean? = null

        rule.setContent {
            Box(
                Modifier.size(50.dp)
                    .onFocusChanged { activeFocus = it.isFocused }
                    .requestFocusOnHierarchyActive()
                    .focusable()
            )
        }

        rule.runOnIdle { assertEquals(true, activeFocus) }
    }

    @Test
    fun non_leaf_requester_takes_priority() {
        var activeFocus1: Boolean? = null
        var activeFocus2: Boolean? = null

        rule.setContent {
            Box(
                Modifier.size(50.dp)
                    .onFocusChanged { activeFocus1 = it.isFocused }
                    .requestFocusOnHierarchyActive()
                    .focusable()
            ) {
                Box(
                    Modifier.size(50.dp)
                        .onFocusChanged { activeFocus2 = it.isFocused }
                        .requestFocusOnHierarchyActive()
                        .focusable()
                ) {}
            }
        }

        rule.waitForIdle()

        Assert.assertEquals(true, activeFocus1)
        Assert.assertEquals(false, activeFocus2)
    }

    @Composable
    private fun FocusableTestItem(onFocusChanged: (Boolean) -> Unit) {
        val focusRequester = rememberActiveFocusRequester()
        Box(
            Modifier.size(10.dp) // View.requestFocus() will not take focus if the view has no size.
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .focusable()
        )
    }

    // Ensure that the expected element, and only it, is focused.
    private fun checkFocus(expectedSelected: Int, actualFocused: BooleanArray) {
        val focusedSet = actualFocused.indices.filter { actualFocused[it] }.toSet()
        Assert.assertEquals(setOf(expectedSelected), focusedSet)
    }
}
