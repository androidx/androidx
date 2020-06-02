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
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.toggleable
import androidx.ui.foundation.selection.triStateToggleable
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
import androidx.ui.test.find
import androidx.ui.test.findByTag
import androidx.ui.test.isToggleable
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
                Box(Modifier
                    .triStateToggleable(state = ToggleableState.On, onClick = {})
                    .testTag("checkedToggleable"),
                    children = {
                        Text("ToggleableText")
                    })
                Box(Modifier
                    .triStateToggleable(state = ToggleableState.Off, onClick = {})
                    .testTag("unCheckedToggleable"),
                    children = {
                        Text("ToggleableText")
                    })
                Box(Modifier
                    .triStateToggleable(state = ToggleableState.Indeterminate, onClick = {})
                    .testTag("indeterminateToggleable"),
                    children = {
                        Text("ToggleableText")
                    })
            }
        }

        fun hasIndeterminateState(): SemanticsMatcher = SemanticsMatcher.expectValue(
            FoundationSemanticsProperties.ToggleableState, ToggleableState.Indeterminate
        )

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
                Box(Modifier
                    .toggleable(value = true, onValueChange = {})
                    .testTag("checkedToggleable"),
                    children = {
                        Text("ToggleableText")
                    })
                Box(Modifier
                    .toggleable(value = false, onValueChange = {})
                    .testTag("unCheckedToggleable"),
                    children = {
                        Text("ToggleableText")
                    })
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
                Box(
                    Modifier.triStateToggleable(
                        state = ToggleableState.On,
                        onClick = {},
                        enabled = false
                    ), children = {
                        Text("ToggleableText")
                    })
            }
        }

        find(isToggleable())
            .assertIsNotEnabled()
            .assertHasNoClickAction()
    }

    @Test
    fun toggleableTest_toggle() {
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }

        composeTestRule.setContent {
            Stack {
                Box(
                    Modifier.toggleable(value = checked, onValueChange = onCheckedChange),
                    children = {
                        Text("ToggleableText")
                    }
                )
            }
        }

        find(isToggleable())
            .doClick()

        runOnIdleCompose {
            assertThat(checked).isEqualTo(false)
        }
    }
}