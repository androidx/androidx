/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalTvMaterial3Api::class
)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ChipTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun assistChip_defaultSemantics() {
        rule.setContent {
            Box {
                AssistChip(
                    modifier = Modifier.testTag(AssistChipTag),
                    onClick = {}
                ) { Text("Test Text") }
            }
        }

        rule.onNodeWithTag(AssistChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
    }

    @Test
    fun assistChip_longClickSemantics() {
        var count by mutableStateOf(0)
        rule.setContent {
            Box {
                AssistChip(
                    modifier = Modifier.testTag(AssistChipTag),
                    onClick = {},
                    onLongClick = { count++ }
                ) { Text("Test Text") }
            }
        }

        val node = rule.onNodeWithTag(AssistChipTag)

        assert(count == 0)

        node
            .requestFocus()
            .assertHasClickAction()
            .assertIsEnabled()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.waitForIdle()

        assert(count == 1)
    }

    @Test
    fun assistChip_disabledSemantics() {
        rule.setContent {
            Box {
                AssistChip(
                    modifier = Modifier.testTag(AssistChipTag),
                    onClick = {},
                    enabled = false
                ) { Text("Test Text") }
            }
        }

        rule.onNodeWithTag(AssistChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun assistChip_findByTag_andClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                AssistChip(
                    modifier = Modifier.testTag(AssistChipTag),
                    onClick = onClick
                ) { Text("Test Text") }
            }
        }
        rule.onNodeWithTag(AssistChipTag)
            .requestFocus()
            .performKeyInput {
                pressKey(Key.DirectionCenter)
            }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun assistChip_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                AssistChip(
                    modifier = Modifier.testTag(AssistChipTag),
                    onClick = { enabled = false },
                    enabled = enabled
                ) { Text("Test Text") }
            }
        }
        rule.onNodeWithTag(AssistChipTag)
            .requestFocus()
            // Confirm the filterChip starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun assistChip_clickIs_independent_betweenChips() {
        var loginCounter = 0
        val loginChipOnClick: () -> Unit = { ++loginCounter }
        val loginChipTag = "LoginChip"

        var registerCounter = 0
        val registerChipOnClick: () -> Unit = { ++registerCounter }
        val registerChipTag = "RegisterChip"

        rule.setContent {
            Column {
                AssistChip(
                    modifier = Modifier.testTag(loginChipTag),
                    onClick = loginChipOnClick
                ) { Text("Login") }
                AssistChip(
                    modifier = Modifier.testTag(registerChipTag),
                    onClick = registerChipOnClick
                ) { Text("Register") }
            }
        }

        rule.onNodeWithTag(loginChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(registerChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(1)
        }
    }

    @Test
    fun filterChip_defaultSemantics() {
        rule.setContent {
            Box {
                FilterChip(
                    modifier = Modifier.testTag(FilterChipTag),
                    onClick = {},
                    selected = false
                ) {
                    Text("Test Text")
                }
            }
        }

        rule.onNodeWithTag(FilterChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
    }

    @Test
    fun filterChip_longClickSemantics() {
        var count by mutableStateOf(0)
        rule.setContent {
            Box {
                FilterChip(
                    modifier = Modifier.testTag(FilterChipTag),
                    onClick = {},
                    onLongClick = { count++ },
                    selected = false
                ) {
                    Text("Test Text")
                }
            }
        }

        val node = rule.onNodeWithTag(FilterChipTag)

        assert(count == 0)

        node
            .requestFocus()
            .assertHasClickAction()
            .assertIsEnabled()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.waitForIdle()

        assert(count == 1)
    }

    @Test
    fun filterChip_disabledSemantics() {
        rule.setContent {
            Box {
                FilterChip(
                    modifier = Modifier.testTag(FilterChipTag),
                    onClick = {},
                    enabled = false,
                    selected = false
                ) { Text("Test Text") }
            }
        }

        rule.onNodeWithTag(FilterChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsNotEnabled()
    }

    @Test
    fun filterChip_findByTag_andClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                FilterChip(
                    modifier = Modifier.testTag(FilterChipTag),
                    onClick = onClick,
                    selected = false
                ) { Text("Test Text") }
            }
        }
        rule.onNodeWithTag(FilterChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle { Truth.assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun filterChip_showLeadingIcon_onClick() {
        var isSelected by mutableStateOf(false)
        rule.setContent {
            Box {
                FilterChip(
                    modifier = Modifier.testTag(FilterChipTag),
                    selected = isSelected,
                    onClick = { isSelected = true },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .testTag(FilterChipLeadingContentTag)
                                .size(FilterChipDefaults.IconSize),
                        )
                    }
                ) {
                    Text(text = "Test Text")
                }
            }
        }
        rule.onNodeWithTag(FilterChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitUntil { isSelected }
        rule.onNodeWithTag(FilterChipLeadingContentTag, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun filterChip_showCustomIcon_onClick() {
        var isSelected by mutableStateOf(false)
        rule.setContent {
            Box {
                FilterChip(
                    modifier = Modifier.testTag(FilterChipTag),
                    selected = isSelected,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(DefaultIconSize)
                                .semantics { contentDescription = "Add Icon" }
                        )
                    },
                    onClick = { isSelected = true }
                ) {
                    Text(text = "Test Text")
                }
            }
        }
        rule.onNodeWithTag(FilterChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithContentDescription("Filter Selected").assertDoesNotExist()
        rule.onNodeWithContentDescription("Add Icon").assertIsDisplayed()
    }

    @Test
    fun filterChip_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                FilterChip(
                    modifier = Modifier.testTag(FilterChipTag),
                    onClick = { enabled = false },
                    enabled = enabled,
                    selected = true
                ) {
                    Text("Test Text")
                }
            }
        }
        rule.onNodeWithTag(FilterChipTag)
            .requestFocus()
            // Confirm the filterChip starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun filterChip_clickIs_independent_betweenChips() {
        var loginCounter = 0
        val loginChipOnClick: () -> Unit = { ++loginCounter }
        val loginChipTag = "LoginChip"

        var registerCounter = 0
        val registerChpOnClick: () -> Unit = { ++registerCounter }
        val registerChipTag = "RegisterChip"

        rule.setContent {
            Column {
                FilterChip(
                    modifier = Modifier.testTag(loginChipTag),
                    onClick = loginChipOnClick,
                    selected = true
                ) { Text("Login") }
                FilterChip(
                    modifier = Modifier.testTag(registerChipTag),
                    onClick = registerChpOnClick,
                    selected = true
                ) { Text("Register") }
            }
        }

        rule.onNodeWithTag(loginChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(registerChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(1)
        }
    }

    @Test
    fun inputChip_defaultSemantics() {
        rule.setContent {
            Box {
                InputChip(
                    modifier = Modifier.testTag(InputChipTag),
                    onClick = {},
                    selected = false
                ) {
                    Text("Test Text")
                }
            }
        }

        rule.onNodeWithTag(InputChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
    }

    @Test
    fun inputChip_longClickSemantics() {
        var count by mutableStateOf(0)
        rule.setContent {
            Box {
                InputChip(
                    modifier = Modifier.testTag(InputChipTag),
                    onClick = {},
                    onLongClick = { count++ },
                    selected = false
                ) {
                    Text("Test Text")
                }
            }
        }

        val node = rule.onNodeWithTag(InputChipTag)

        assert(count == 0)

        node
            .requestFocus()
            .assertHasClickAction()
            .assertIsEnabled()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.waitForIdle()

        assert(count == 1)
    }

    @Test
    fun inputChip_disabledSemantics() {
        rule.setContent {
            Box {
                InputChip(
                    modifier = Modifier.testTag(InputChipTag),
                    onClick = {},
                    enabled = false,
                    selected = false
                ) {
                    Text("Test Text")
                }
            }
        }

        rule.onNodeWithTag(InputChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsNotEnabled()
    }

    @Test
    fun inputChip_findByTag_andClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "Test Text"

        rule.setContent {
            Box {
                InputChip(
                    modifier = Modifier.testTag(InputChipTag),
                    onClick = onClick,
                    selected = false
                ) {
                    Text(text)
                }
            }
        }
        rule.onNodeWithTag(InputChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun inputChip_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                InputChip(
                    modifier = Modifier.testTag(InputChipTag),
                    onClick = { enabled = false },
                    enabled = enabled,
                    selected = true
                ) {
                    Text("Test Text")
                }
            }
        }
        rule.onNodeWithTag(InputChipTag)
            .requestFocus()
            // Confirm the filterChip starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun inputChip_clickIs_independent_betweenChips() {
        var loginCounter = 0
        val loginChipOnClick: () -> Unit = { ++loginCounter }
        val loginChipTag = "LoginChip"
        val loginChipText = "Login"

        var registerCounter = 0
        val registerChipOnClick: () -> Unit = { ++registerCounter }
        val registerChipTag = "RegisterChip"
        val registerChipText = "Register"

        rule.setContent {
            Column {
                InputChip(
                    modifier = Modifier.testTag(loginChipTag),
                    onClick = loginChipOnClick,
                    selected = true
                ) {
                    Text(loginChipText)
                }
                InputChip(
                    modifier = Modifier.testTag(registerChipTag),
                    onClick = registerChipOnClick,
                    selected = true
                ) {
                    Text(registerChipText)
                }
            }
        }

        rule.onNodeWithTag(loginChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(registerChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(1)
        }
    }

    @Test
    fun suggestionChip_defaultSemantics() {
        rule.setContent {
            Box {
                SuggestionChip(
                    modifier = Modifier.testTag(SuggestionChipTag),
                    onClick = {}
                ) {
                    Text("mvTvSelectableChip")
                }
            }
        }

        rule.onNodeWithTag(SuggestionChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
    }

    @Test
    fun suggestionChip_longClickSemantics() {
        var count by mutableStateOf(0)
        rule.setContent {
            Box {
                SuggestionChip(
                    modifier = Modifier.testTag(SuggestionChipTag),
                    onLongClick = { count++ },
                    onClick = {}
                ) {
                    Text("mvTvSelectableChip")
                }
            }
        }

        val node = rule.onNodeWithTag(SuggestionChipTag)

        assert(count == 0)

        node
            .requestFocus()
            .assertHasClickAction()
            .assertIsEnabled()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.waitForIdle()

        assert(count == 1)
    }

    @Test
    fun suggestionChip_disabledSemantics() {
        rule.setContent {
            Box {
                SuggestionChip(
                    modifier = Modifier.testTag(SuggestionChipTag),
                    onClick = {},
                    enabled = false
                ) {
                    Text("mvTvSelectableChip")
                }
            }
        }

        rule.onNodeWithTag(SuggestionChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun suggestionChip_findByTag_andClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myTvSuggestionChip"

        rule.setContent {
            Box {
                SuggestionChip(
                    modifier = Modifier.testTag(SuggestionChipTag),
                    onClick = onClick
                ) {
                    Text(text = text)
                }
            }
        }
        rule.onNodeWithTag(SuggestionChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun suggestionChip_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                SuggestionChip(
                    modifier = Modifier.testTag(SuggestionChipTag),
                    onClick = { enabled = false },
                    enabled = enabled
                ) {
                    Text("Hello")
                }
            }
        }
        rule.onNodeWithTag(SuggestionChipTag)
            .requestFocus()
            // Confirm the chip starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun suggestionChip_clickIs_independent_betweenChips() {
        var loginCounter = 0
        val loginChipOnClick: () -> Unit = { ++loginCounter }
        val loginChipTag = "LoginChip"
        val loginChipText = "Login"

        var registerCounter = 0
        val registerChipOnClick: () -> Unit = { ++registerCounter }
        val registerChipTag = "RegisterChip"
        val registerChipText = "Register"

        rule.setContent {
            Column {
                SuggestionChip(
                    modifier = Modifier.testTag(loginChipTag),
                    onClick = loginChipOnClick
                ) {
                    Text(text = loginChipText)
                }

                SuggestionChip(
                    modifier = Modifier.testTag(registerChipTag),
                    onClick = registerChipOnClick
                ) {
                    Text(text = registerChipText)
                }
            }
        }

        rule.onNodeWithTag(loginChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(registerChipTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(loginCounter).isEqualTo(1)
            Truth.assertThat(registerCounter).isEqualTo(1)
        }
    }
}

private const val AssistChipTag = "AssistChipTag"
private const val AssistChipLabelTag = "AssistChipLabel"
private const val AssistChipLeadingIconTag = "AssistChipLeadingIcon"

private const val FilterChipTag = "FilterChip"
private const val FilterChipLabelTag = "FilterChipLabel"
private const val FilterChipLeadingContentTag = "FilterChipLeadingContent"

private const val InputChipTag = "InputChip"
private const val InputChipLabelTag = "InputChipLabel"
private const val InputChipAvatarTag = "InputChipAvatar"
private const val InputChipLeadingContentTag = "InputChipLeadingContent"
private const val InputChipTrailingContentTag = "InputChipTrailingContent"

private const val SuggestionChipTag = "SuggestionChip"
private const val SuggestionChipLabelTag = "SuggestionChipLabel"

private val DefaultIconSize = 24.dp
