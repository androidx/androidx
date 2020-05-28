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
import androidx.ui.layout.Column
import androidx.ui.layout.Stack
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
class SwitchUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val defaultSwitchTag = "switch"

    @Test
    fun switch_defaultSemantics() {
        composeTestRule.setMaterialContent {
            Column {
                Switch(modifier = Modifier.testTag("checked"), checked = true, onCheckedChange = {})
                Switch(
                    modifier = Modifier.testTag("unchecked"),
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }

        findByTag("checked")
            .assertIsEnabled()
            .assertIsOn()
            .assertValueEquals(Strings.Checked)
        findByTag("unchecked")
            .assertIsEnabled()
            .assertIsOff()
            .assertValueEquals(Strings.Unchecked)
    }

    @Test
    fun switch_toggle() {
        composeTestRule.setMaterialContent {
            val (checked, onChecked) = state { false }

            // Stack is needed because otherwise the control will be expanded to fill its parent
            Stack {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        findByTag(defaultSwitchTag)
            .assertIsOff()
            .doClick()
            .assertIsOn()
    }

    @Test
    fun switch_toggleTwice() {
        composeTestRule.setMaterialContent {
            val (checked, onChecked) = state { false }

            // Stack is needed because otherwise the control will be expanded to fill its parent
            Stack {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        findByTag(defaultSwitchTag)
            .assertIsOff()
            .doClick()
            .assertIsOn()
            .doClick()
            .assertIsOff()
    }

    @Test
    fun switch_uncheckableWithNoLambda() {
        composeTestRule.setMaterialContent {
            val (checked, _) = state { false }
            Switch(
                modifier = Modifier.testTag(defaultSwitchTag),
                checked = checked,
                onCheckedChange = {},
                enabled = false
            )
        }
        findByTag(defaultSwitchTag)
            .assertHasNoClickAction()
    }

    @Test
    fun switch_materialSizes_whenChecked() {
        materialSizesTestForValue(true)
    }

    @Test
    fun switch_materialSizes_whenUnchecked() {
        materialSizesTestForValue(false)
    }

    private fun materialSizesTestForValue(checked: Boolean) {
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Switch(checked = checked, onCheckedChange = {}, enabled = false)
            }
            .assertWidthEqualsTo { 34.dp.toIntPx() + 2.dp.toIntPx() * 2 }
            .assertHeightEqualsTo { 20.dp.toIntPx() + 2.dp.toIntPx() * 2 }
    }
}