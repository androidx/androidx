/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.test.filters.MediumTest
import androidx.ui.core.TestTag
import androidx.ui.core.Text
import androidx.ui.foundation.selection.Toggleable
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.TriStateToggleable
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ToggleableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun toggleableTest_defaultSemantics() {
        composeTestRule.setContent {
            Column {
                TestTag(tag = "checkedToggleable") {
                    TriStateToggleable(ToggleableState.Checked, onToggle = {}) {
                        Text("ToggleableText")
                    }
                }
                TestTag(tag = "unCheckedToggleable") {
                    TriStateToggleable(ToggleableState.Unchecked, onToggle = {}) {
                        Text("ToggleableText")
                    }
                }
                TestTag(tag = "indeterminateToggleable") {
                    TriStateToggleable(ToggleableState.Indeterminate, onToggle = {}) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        findByTag("checkedToggleable")
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = true,
                    toggleableState = ToggleableState.Checked
                )
            )
            .assertHasClickAction()
        findByTag("unCheckedToggleable")
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = true,
                    toggleableState = ToggleableState.Unchecked
                )
            )
            .assertHasClickAction()
        findByTag("indeterminateToggleable")
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = true,
                    toggleableState = ToggleableState.Indeterminate
                )
            )
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_booleanOverload_defaultSemantics() {
        composeTestRule.setContent {
            Column {
                TestTag(tag = "checkedToggleable") {
                    Toggleable(checked = true, onCheckedChange = {}) {
                        Text("ToggleableText")
                    }
                }
                TestTag(tag = "unCheckedToggleable") {
                    Toggleable(checked = false, onCheckedChange = {}) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        findByTag("checkedToggleable")
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = true,
                    toggleableState = ToggleableState.Checked
                )
            )
            .assertHasClickAction()
        findByTag("unCheckedToggleable")
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = true,
                    toggleableState = ToggleableState.Unchecked
                )
            )
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_disabledSemantics() {
        composeTestRule.setContent {
            Center {
                TestTag(tag = "myToggleable") {
                    TriStateToggleable(value = ToggleableState.Checked) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        findByTag("myToggleable")
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = false
                )
            )
            .assertHasNoClickAction()
    }

    @Test
    fun toggleableTest_toggle() {
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }

        composeTestRule.setContent {
            Center {
                TestTag(tag = "myToggleable") {
                    Toggleable(checked = checked, onCheckedChange = onCheckedChange) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        findByTag("myToggleable")
            .doClick()

        Truth
            .assertThat(checked)
            .isEqualTo(false)
    }
}