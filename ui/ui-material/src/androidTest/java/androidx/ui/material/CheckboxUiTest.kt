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

import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.MediumTest
import androidx.ui.core.TestTag
import androidx.ui.core.dp
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.ToggleableState.Checked
import androidx.ui.foundation.selection.ToggleableState.Indeterminate
import androidx.ui.foundation.selection.ToggleableState.Unchecked
import androidx.ui.layout.Column
import androidx.ui.test.assertIsChecked
import androidx.ui.test.assertIsNotChecked
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.copyWith
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
class CheckboxUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    // TODO(b/126881459): this should be the default semantic for checkbox
    private val defaultCheckboxCheckedSemantics = createFullSemantics(
        isEnabled = true,
        isChecked = true
    )

    private val defaultCheckboxUncheckedSemantics = defaultCheckboxCheckedSemantics.copyWith {
        isChecked = false
    }

    private val defaultTag = "myCheckbox"

    @Test
    fun checkBoxTest_defaultSemantics() {
        composeTestRule.setMaterialContent {
            Column {
                TestTag(tag = "checkboxUnchecked") {
                    Checkbox(false, {})
                }
                TestTag(tag = "checkboxChecked") {
                    Checkbox(true, {})
                }
            }
        }

        findByTag("checkboxUnchecked")
            .assertSemanticsIsEqualTo(defaultCheckboxUncheckedSemantics)

        findByTag("checkboxChecked")
            .assertSemanticsIsEqualTo(defaultCheckboxCheckedSemantics)
    }

    @Test
    fun checkBoxTest_toggle() {
        composeTestRule.setMaterialContent {
            val (checked, onCheckedChange) = +state { false }
            TestTag(tag = defaultTag) {
                Checkbox(checked, onCheckedChange)
            }
        }

        findByTag(defaultTag)
            .assertIsNotChecked()
            .doClick()
            .assertIsChecked()
    }

    @Test
    fun checkBoxTest_toggle_twice() {
        composeTestRule.setMaterialContent {
            val (checked, onCheckedChange) = +state { false }
            TestTag(tag = defaultTag) {
                Checkbox(checked, onCheckedChange)
            }
        }

        findByTag(defaultTag)
            .assertIsNotChecked()
            .doClick()
            .assertIsChecked()
            .doClick()
            .assertIsNotChecked()
    }

    @Test
    fun checkBoxTest_untoggleable_whenNoLambda() {

        composeTestRule.setMaterialContent {
            val (checked, _) = +state { false }
            TestTag(tag = defaultTag) {
                Checkbox(checked, null)
            }
        }

        findByTag(defaultTag)
            .assertIsNotChecked()
            .doClick()
            .assertIsNotChecked()
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenChecked() {
        materialSizeTestForValue(Checked)
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenUnchecked() {
        materialSizeTestForValue(Unchecked)
    }

    @Test
    fun checkBoxTest_MaterialSize_WhenIndeterminate() {
        materialSizeTestForValue(Indeterminate)
    }

    private fun materialSizeTestForValue(checkboxValue: ToggleableState) {
        composeTestRule
            .setMaterialContentAndTestSizes {
                TriStateCheckbox(value = checkboxValue, onClick = null)
            }
            .assertIsSquareWithSize { 2.dp.toIntPx() * 2 + 20.dp.toIntPx() }
    }
}