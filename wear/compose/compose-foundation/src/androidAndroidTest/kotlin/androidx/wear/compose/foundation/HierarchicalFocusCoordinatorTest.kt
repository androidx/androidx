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

package androidx.wear.compose.foundation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalWearFoundationApi::class)
class HierarchicalFocusCoordinatorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun basic_OnFocusChange_works_1_of_3() = basic_OnFocusChange_works(0, 3)
    @Test
    fun basic_OnFocusChange_works_2_of_5() = basic_OnFocusChange_works(1, 5)
    @Test
    fun basic_OnFocusChange_works_4_of_4() = basic_OnFocusChange_works(3, 4)

    private fun basic_OnFocusChange_works(selected: Int, numItems: Int) {
        var focused = BooleanArray(numItems)

        rule.setContent {
            repeat(numItems) { ix ->
                HierarchicalFocusCoordinator({ ix == selected }) {
                    OnFocusChange(onFocusChanged = { focused[ix] = it })
                }
            }
        }

        rule.runOnIdle {
            checkFocus(selected, focused)
        }
    }

    @Test
    fun basic_selection_works_1_of_3() = basic_selection_works(0, 3)

    @Test
    fun basic_selection_works_2_of_5() = basic_selection_works(1, 5)

    @Test
    fun basic_selection_works_4_of_4() = basic_selection_works(3, 4)

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

        rule.runOnIdle {
            checkFocus(selected, focused)
        }
    }

    @Test
    fun nested_selection_initial() = nested_selection_works(0) { _, _ -> }

    @Test
    fun nested_selection_switch_top() = nested_selection_works(3) { top, _ ->
        top.value = 1
    }

    @Test
    fun nested_selection_switch_bottom() = nested_selection_works(1) { _, bottom ->
        bottom[0].value = 1
        bottom[1].value = 2
        bottom[2].value = 0
    }

    @Test
    fun nested_selection_switch_both() = nested_selection_works(5) { top, bottom ->
        bottom[0].value = 1
        bottom[1].value = 2
        bottom[2].value = 0
        top.value = 1
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
        act: (MutableState<Int>, Array<MutableState<Int>>) -> Unit,
    ) {
        val numItems = 3
        var focused = BooleanArray(numItems * numItems)
        val topSelected = mutableStateOf(0)
        val bottomSelected = Array<MutableState<Int>>(numItems) { mutableStateOf(0) }

        rule.setContent {
            Box {
                repeat(numItems) { topIx ->
                    HierarchicalFocusCoordinator({ topIx == topSelected.value }) {
                        Box {
                            repeat(numItems) { bottomIx ->
                                HierarchicalFocusCoordinator(
                                    { bottomIx == bottomSelected[topIx].value }
                                ) {
                                    FocusableTestItem { focused[topIx * numItems + bottomIx] = it }
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            act(topSelected, bottomSelected)
        }

        rule.runOnIdle {
            checkFocus(expectedSelected, focused)
        }
    }

    @Test
    public fun release_focus_works() {
        val selected = mutableStateOf(0)
        var focused = false
        rule.setContent {
            Box {
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

        rule.runOnIdle {
            Assert.assertFalse(focused)
        }
    }
    @Test
    public fun add_focusable_works() {
        var show by mutableStateOf(false)
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

        rule.runOnIdle {
            show = true
        }

        rule.runOnIdle {
            Assert.assertTrue(focused)
        }
    }

    @Test
    public fun focus_not_required_reported_correctly() {
        var focused = false
        rule.setContent {
            Box {
                HierarchicalFocusCoordinator(
                    requiresFocus = { false }
                ) {
                    FocusableTestItem { focused = it }
                }
            }
        }

        rule.runOnIdle {
            Assert.assertFalse(focused)
        }
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
                    requiresFocus = if (lambdaUpdated) {
                        { true }
                    } else {
                        { false }
                    }
                ) {
                    FocusableTestItem { focused = it }
                }
            }
        }

        rule.runOnIdle {
            lambdaUpdated = true
        }

        rule.runOnIdle {
            Assert.assertTrue(focused)
        }
    }
    @Composable
    private fun FocusableTestItem(onFocusChanged: (Boolean) -> Unit) {
        val focusRequester = rememberActiveFocusRequester()
        Box(
            Modifier
                .size(10.dp) // View.requestFocus() will not take focus if the view has no size.
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .focusable())
    }

    // Ensure that the expected element, and only it, is focused.
    private fun checkFocus(expectedSelected: Int, actualFocused: BooleanArray) {
        val focusedSet = actualFocused.indices.filter { actualFocused[it] }.toSet()
        Assert.assertEquals(setOf(expectedSelected), focusedSet)
    }
}