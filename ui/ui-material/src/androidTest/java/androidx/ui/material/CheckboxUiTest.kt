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
package androidx.ui.material

import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Strings
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.ToggleableState.Indeterminate
import androidx.ui.foundation.selection.ToggleableState.Off
import androidx.ui.foundation.selection.ToggleableState.On
import androidx.ui.layout.Column
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class CheckboxUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val defaultTag = "myCheckbox"

    @Test
    fun checkBoxTest_defaultSemantics() {
        composeTestRule.setMaterialContent {
            Column {
                Checkbox(false, {}, modifier = Modifier.testTag(tag = "checkboxUnchecked"))
                Checkbox(true, {}, modifier = Modifier.testTag("checkboxChecked"))
            }
        }

        findByTag("checkboxUnchecked")
            .assertIsEnabled()
            .assertIsOff()
            .assertValueEquals(Strings.Unchecked)

        findByTag("checkboxChecked")
            .assertIsEnabled()
            .assertIsOn()
            .assertValueEquals(Strings.Checked)
    }

    @Test
    fun checkBoxTest_toggle() {
        composeTestRule.setMaterialContent {
            val (checked, onCheckedChange) = state { false }
            Checkbox(checked, onCheckedChange, modifier = Modifier.testTag(defaultTag))
        }

        findByTag(defaultTag)
            .assertIsOff()
            .doClick()
            .assertIsOn()
    }

    @Test
    fun checkBoxTest_toggle_twice() {
        composeTestRule.setMaterialContent {
            val (checked, onCheckedChange) = state { false }
            Checkbox(checked, onCheckedChange, modifier = Modifier.testTag(defaultTag))
        }

        findByTag(defaultTag)
            .assertIsOff()
            .doClick()
            .assertIsOn()
            .doClick()
            .assertIsOff()
    }

    @Test
    fun checkBoxTest_untoggleable_whenNoLambda() {

        composeTestRule.setMaterialContent {
            val (checked, _) = state { false }
            Checkbox(checked, {}, enabled = false, modifier = Modifier.testTag(defaultTag))
        }

        findByTag(defaultTag)
            .assertHasNoClickAction()
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
        composeTestRule
            .setMaterialContentAndCollectSizes {
                TriStateCheckbox(state = checkboxValue, onClick = {}, enabled = false)
            }
            .assertIsSquareWithSize { 2.dp.toIntPx() * 2 + 20.dp.toIntPx() }
    }
}