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
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.TestTag
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
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
class SwitchUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val defaultUncheckedSwitchSemantics = createFullSemantics(
        isEnabled = true,
        isChecked = false
    )
    private val defaultCheckedSwitchSemantics = defaultUncheckedSwitchSemantics.copyWith {
        isChecked = true
    }
    private val defaultSwitchTag = "switch"

    @Test
    fun switch_defaultSemantics() {
        composeTestRule.setMaterialContent {
            Column {
                TestTag(tag = "checked") {
                    Switch(checked = true)
                }
                TestTag(tag = "unchecked") {
                    Switch(checked = false)
                }
            }
        }

        findByTag("checked").assertSemanticsIsEqualTo(defaultCheckedSwitchSemantics)
        findByTag("unchecked").assertSemanticsIsEqualTo(defaultUncheckedSwitchSemantics)
    }

    @Test
    fun switch_toggle() {
        composeTestRule.setMaterialContent {
            val (checked, onChecked) = +state { false }
            TestTag(tag = defaultSwitchTag) {
                Switch(checked = checked, onClick = { onChecked(!checked) })
            }
        }
        findByTag(defaultSwitchTag)
            .assertIsNotChecked()
            .doClick()
            .assertIsChecked()
    }

    @Test
    fun switch_toggleTwice() {

        composeTestRule.setMaterialContent {
            val (checked, onChecked) = +state { false }
            TestTag(tag = defaultSwitchTag) {
                Switch(checked = checked, onClick = { onChecked(!checked) })
            }
        }
        findByTag(defaultSwitchTag)
            .assertIsNotChecked()
            .doClick()
            .assertIsChecked()
            .doClick()
            .assertIsNotChecked()
    }

    @Test
    fun switch_uncheckableWithNoLambda() {
        composeTestRule.setMaterialContent {
            val (checked, _) = +state { false }
            TestTag(tag = defaultSwitchTag) {
                Switch(checked = checked)
            }
        }
        findByTag(defaultSwitchTag)
            .assertIsNotChecked()
            .doClick()
            .assertIsNotChecked()
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
        var switchSize: PxSize? = null
        composeTestRule.setMaterialContent {
            Container(
                constraints = DpConstraints(
                    maxWidth = 5000.dp,
                    maxHeight = 5000.dp
                )
            ) {
                OnChildPositioned(onPositioned = { coordinates ->
                    switchSize = coordinates.size
                }) {
                    Switch(checked = checked)
                }
            }
        }
        withDensity(composeTestRule.density) {
            Truth.assertThat(switchSize?.width?.round())
                .isEqualTo(34.dp.toIntPx() + 2.dp.toIntPx() * 2)
            Truth.assertThat(switchSize?.height?.round())
                .isEqualTo(20.dp.toIntPx() + 2.dp.toIntPx() * 2)
        }
    }
}