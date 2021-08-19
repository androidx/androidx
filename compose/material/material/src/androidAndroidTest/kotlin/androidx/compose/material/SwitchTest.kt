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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
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
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SwitchTest {

    @get:Rule
    val rule = createComposeRule()

    private val defaultSwitchTag = "switch"

    @Test
    fun switch_defaultSemantics() {
        rule.setMaterialContent {
            Column {
                Switch(modifier = Modifier.testTag("checked"), checked = true, onCheckedChange = {})
                Switch(
                    modifier = Modifier.testTag("unchecked"),
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }

        rule.onNodeWithTag("checked")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsEnabled()
            .assertIsOn()
        rule.onNodeWithTag("unchecked")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsEnabled()
            .assertIsOff()
    }

    @Test
    fun switch_toggle() {
        rule.setMaterialContent {
            val (checked, onChecked) = remember { mutableStateOf(false) }

            // Box is needed because otherwise the control will be expanded to fill its parent
            Box {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        rule.onNodeWithTag(defaultSwitchTag)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun switch_toggleTwice() {
        rule.setMaterialContent {
            val (checked, onChecked) = remember { mutableStateOf(false) }

            // Box is needed because otherwise the control will be expanded to fill its parent
            Box {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        rule.onNodeWithTag(defaultSwitchTag)
            .assertIsOff()
            .performClick()
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun switch_uncheckableWithNoLambda() {
        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Switch(
                modifier = Modifier.testTag(defaultSwitchTag),
                checked = checked,
                onCheckedChange = {},
                enabled = false
            )
        }
        rule.onNodeWithTag(defaultSwitchTag)
            .assertHasClickAction()
    }

    @Test
    fun switch_untoggleable_whenEmptyLambda() {
        val parentTag = "parent"

        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(parentTag)) {
                Switch(
                    checked,
                    {},
                    enabled = false,
                    modifier = Modifier.testTag(defaultSwitchTag).semantics { focused = true }
                )
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .assertHasClickAction()

        // Check not merged into parent
        rule.onNodeWithTag(parentTag)
            .assert(isNotFocusable())
    }

    @Test
    fun switch_untoggleableAndMergeable_whenNullLambda() {
        rule.setMaterialContent {
            val (checked, _) = remember { mutableStateOf(false) }
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(defaultSwitchTag)) {
                Switch(
                    checked,
                    null,
                    modifier = Modifier.semantics { focused = true }
                )
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .assertHasNoClickAction()
            .assert(isFocusable()) // Check merged into parent
    }

    @Test
    fun switch_materialSizes_whenChecked() {
        materialSizesTestForValue(true)
    }

    @Test
    fun switch_materialSizes_whenUnchecked() {
        materialSizesTestForValue(false)
    }

    @Test
    fun switch_testDraggable() {
        val state = mutableStateOf(false)
        rule.setMaterialContent {

            // Box is needed because otherwise the control will be expanded to fill its parent
            Box {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = state.value,
                    onCheckedChange = { state.value = it }
                )
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeRight() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(true)
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeLeft() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(false)
        }
    }

    @Test
    fun switch_testDraggable_rtl() {
        val state = mutableStateOf(false)
        rule.setMaterialContent {

            // Box is needed because otherwise the control will be expanded to fill its parent
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box {
                    Switch(
                        modifier = Modifier.testTag(defaultSwitchTag),
                        checked = state.value,
                        onCheckedChange = { state.value = it }
                    )
                }
            }
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeLeft() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(true)
        }

        rule.onNodeWithTag(defaultSwitchTag)
            .performGesture { swipeRight() }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(false)
        }
    }

    // regression test for b/191375128
    @Test
    fun switch_stateRestoration_stateChangeWhileSaved() {
        val screenTwo = mutableStateOf(false)
        var items by mutableStateOf(listOf(1 to false, 2 to true))
        rule.setContent {
            Column {
                Button(onClick = { screenTwo.value = !screenTwo.value }) {
                    Text("switch screen")
                }
                val holder = rememberSaveableStateHolder()
                holder.SaveableStateProvider(screenTwo.value) {
                    if (screenTwo.value) {
                        // second screen, just some random content
                        Text("Second screen")
                    } else {
                        Column {
                            Text("screen one")
                            items.forEachIndexed { index, item ->
                                Row {
                                    Text("Item ${item.first}")
                                    Switch(
                                        modifier = Modifier.testTag(item.first.toString()),
                                        checked = item.second,
                                        onCheckedChange = {
                                            items = items.toMutableList().also {
                                                it[index] = item.first to !item.second
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("1").assertIsOff()
        rule.onNodeWithTag("2").assertIsOn()
        rule.runOnIdle {
            screenTwo.value = true
        }
        rule.runOnIdle {
            items = items.toMutableList().also {
                it[0] = items[0].first to !items[0].second
                it[1] = items[1].first to !items[1].second
            }
        }
        rule.runOnIdle {
            screenTwo.value = false
        }
        rule.onNodeWithTag("1").assertIsOn()
        rule.onNodeWithTag("2").assertIsOff()
    }

    private fun materialSizesTestForValue(checked: Boolean) = with(rule.density) {
        // The padding should be 2 DP, but we round to pixels when determining layout
        val paddingInPixels = 2.dp.roundToPx()

        // Convert back to DP so that we have an exact DP value to work with. We don't
        // want to multiply the error by two (one for each padding), so we get the exact
        // padding based on the expected pixels consumed by the padding.
        val paddingInDp = paddingInPixels.toDp()

        rule.setMaterialContentForSizeAssertions {
            Switch(checked = checked, onCheckedChange = {}, enabled = false)
        }
            .assertWidthIsEqualTo(34.dp + paddingInDp * 2)
            .assertHeightIsEqualTo(20.dp + paddingInDp * 2)
    }

    /**
     * A switch should have a minimum touch target of 48 DP x 48 DP and the reported size
     * should match that, despite the fact that we force the size to be smaller.
     */
    @Test
    fun switch_minTouchTargetArea(): Unit = with(rule.density) {
        var checked by mutableStateOf(false)
        rule.setMaterialContent {
            // Box is needed because otherwise the control will be expanded to fill its parent
            Box(Modifier.fillMaxSize()) {
                Switch(
                    modifier = Modifier.align(Alignment.Center)
                        .testTag(defaultSwitchTag)
                        .requiredSize(2.dp),
                    checked = checked,
                    onCheckedChange = { checked = it }
                )
            }
        }
        val pokePosition = 48.dp.roundToPx().toFloat() - 1f
        rule.onNodeWithTag(defaultSwitchTag)
            .assertIsOff()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performGesture {
                click(position = Offset(pokePosition, pokePosition))
            }.assertIsOn()
    }
}
