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
package androidx.compose.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.state.ToggleableState.Indeterminate
import androidx.compose.ui.state.ToggleableState.Off
import androidx.compose.ui.state.ToggleableState.On
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckboxUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val defaultTag = "myCheckbox"

    @Test
    fun checkBoxTest_defaultSemantics() {
        rule.setMaterialContent {
            Column {
                Checkbox(false, {}, modifier = Modifier.testTag(tag = "checkboxUnchecked"))
                Checkbox(true, {}, modifier = Modifier.testTag("checkboxChecked"))
            }
        }

        rule.onNodeWithTag("checkboxUnchecked")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
            .assertIsOff()

        rule.onNodeWithTag("checkboxChecked")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
            .assertIsOn()
    }

    @Test
    fun checkBoxTest_toggle() {
        rule.setMaterialContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            Checkbox(checked, onCheckedChange, modifier = Modifier.testTag(defaultTag))
        }

        rule.onNodeWithTag(defaultTag)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun checkBoxTest_toggle_twice() {
        rule.setMaterialContent {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            Checkbox(checked, onCheckedChange, modifier = Modifier.testTag(defaultTag))
        }

        rule.onNodeWithTag(defaultTag)
            .assertIsOff()
            .performClick()
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun checkBoxTest_untoggleable_whenEmptyLambda() {
        val parentTag = "parent"

        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(parentTag)) {
                Checkbox(
                    checked,
                    {},
                    enabled = false,
                    modifier = Modifier.testTag(defaultTag).semantics { focused = true }
                )
            }
        }

        rule.onNodeWithTag(defaultTag)
            .assertHasClickAction()

        // Check not merged into parent
        rule.onNodeWithTag(parentTag)
            .assert(isNotFocusable())
    }

    @Test
    fun checkBoxTest_untoggleableAndMergeable_whenNullLambda() {
        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(defaultTag)) {
                Checkbox(
                    checked,
                    null,
                    modifier = Modifier.semantics { focused = true }
                )
            }
        }

        rule.onNodeWithTag(defaultTag)
            .assertHasNoClickAction()
            .assert(isFocusable()) // Check merged into parent
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenChecked() {
        materialSizeTestForValue(On)
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenUnchecked() {
        materialSizeTestForValue(Off)
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenIndeterminate() {
        materialSizeTestForValue(Indeterminate)
    }

    private fun materialSizeTestForValue(checkboxValue: ToggleableState) {
        rule
            .setMaterialContentForSizeAssertions {
                TriStateCheckbox(state = checkboxValue, onClick = {}, enabled = false)
            }
            .assertIsSquareWithSize(2.dp * 2 + 20.dp)
    }

    @Test
    fun checkBoxTest_clickInMinimumTouchTarget(): Unit = with(rule.density) {
        val tag = "switch"
        var state by mutableStateOf(Off)
        rule.setMaterialContent {
            // Box is needed because otherwise the control will be expanded to fill its parent
            Box(Modifier.fillMaxSize()) {
                TriStateCheckbox(
                    state = state,
                    onClick = { state = On },
                    modifier = Modifier.align(Alignment.Center).requiredSize(2.dp).testTag(tag)
                )
            }
        }
        val pokePosition = 48.dp.roundToPx().toFloat() - 1f
        rule.onNodeWithTag(tag)
            .assertIsOff()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performGesture {
                click(position = Offset(pokePosition, pokePosition))
            }.assertIsOn()
    }
}