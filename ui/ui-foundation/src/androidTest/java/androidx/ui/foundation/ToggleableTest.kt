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
import androidx.ui.foundation.selection.Toggleable
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.TriStateToggleable
import androidx.ui.foundation.semantics.FoundationSemanticsProperties
import androidx.ui.layout.Column
import androidx.ui.layout.Stack
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.assert
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
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
                    TriStateToggleable(ToggleableState.On, onClick = {}) {
                        Text("ToggleableText")
                    }
                }
                TestTag(tag = "unCheckedToggleable") {
                    TriStateToggleable(ToggleableState.Off, onClick = {}) {
                        Text("ToggleableText")
                    }
                }
                TestTag(tag = "indeterminateToggleable") {
                    TriStateToggleable(ToggleableState.Indeterminate, onClick = {}) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        fun hasIndeterminateState(): SemanticsMatcher = SemanticsMatcher.expectValue(
            FoundationSemanticsProperties.ToggleableState, ToggleableState.Indeterminate)

        findByTag("checkedToggleable")
            .assertIsEnabled()
            .assertIsOn()
            .assertHasClickAction()
        findByTag("unCheckedToggleable")
            .assertIsEnabled()
            .assertIsOff()
            .assertHasClickAction()
        findByTag("indeterminateToggleable")
            .assertIsEnabled()
            .assert(hasIndeterminateState())
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_booleanOverload_defaultSemantics() {
        composeTestRule.setContent {
            Column {
                TestTag(tag = "checkedToggleable") {
                    Toggleable(value = true, onValueChange = {}) {
                        Text("ToggleableText")
                    }
                }
                TestTag(tag = "unCheckedToggleable") {
                    Toggleable(value = false, onValueChange = {}) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        findByTag("checkedToggleable")
            .assertIsEnabled()
            .assertIsOn()
            .assertHasClickAction()
        findByTag("unCheckedToggleable")
            .assertIsEnabled()
            .assertIsOff()
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_disabledSemantics() {
        composeTestRule.setContent {
            Stack {
                TestTag(tag = "myToggleable") {
                    TriStateToggleable(state = ToggleableState.On, enabled = false, onClick = {}) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        findByTag("myToggleable")
            .assertIsNotEnabled()
            .assertHasNoClickAction()
    }

    @Test
    fun toggleableTest_toggle() {
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }

        composeTestRule.setContent {
            Stack {
                TestTag(tag = "myToggleable") {
                    Toggleable(value = checked, onValueChange = onCheckedChange) {
                        Text("ToggleableText")
                    }
                }
            }
        }

        findByTag("myToggleable")
            .doClick()

        runOnIdleCompose {
            assertThat(checked).isEqualTo(false)
        }
    }
}