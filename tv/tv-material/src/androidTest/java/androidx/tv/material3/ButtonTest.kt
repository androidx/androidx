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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@OptIn(
    ExperimentalTestApi::class,
    ExperimentalTvMaterial3Api::class
)
@RunWith(AndroidJUnit4::class)
class ButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun filledButton_defaultSemantics() {
        rule.setContent {
            Box {
                Button(modifier = Modifier.testTag(FilledButtonTag), onClick = {}) {
                    Text("FilledButton")
                }
            }
        }

        rule.onNodeWithTag(FilledButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun filledButton_longClickSemantics() {
        rule.setContent {
            Box {
                Button(
                    modifier = Modifier.testTag(FilledButtonTag),
                    onClick = {},
                    onLongClick = {}) {
                    Text("FilledButton")
                }
            }
        }

        rule.onNodeWithTag(FilledButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assertIsEnabled()
    }

    @Test
    fun filledButton_disabledSemantics() {
        rule.setContent {
            Box {
                Button(
                    modifier = Modifier.testTag(FilledButtonTag),
                    onClick = {},
                    enabled = false
                ) {
                    Text("FilledButton")
                }
            }
        }

        rule.onNodeWithTag(FilledButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun filledButton_findByTag_andClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "FilledButtonText"

        rule.setContent {
            Box {
                Button(modifier = Modifier.testTag(FilledButtonTag), onClick = onClick) {
                    Text(text)
                }
            }
        }
        rule.onNodeWithTag(FilledButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun filledButton_findByTag_andLongClick() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val text = "FilledButtonText"

        rule.setContent {
            Box {
                Button(
                    modifier = Modifier.testTag(FilledButtonTag),
                    onClick = {},
                    onLongClick = onLongClick) {
                    Text(text)
                }
            }
        }
        rule.onNodeWithTag(FilledButtonTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun filledButton_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                Button(
                    modifier = Modifier.testTag(FilledButtonTag),
                    onClick = { enabled = false },
                    enabled = enabled
                ) {
                    Text("Hello")
                }
            }
        }
        rule.onNodeWithTag(FilledButtonTag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun filledButton_clickIs_independent_betweenButtons() {
        var watchButtonCounter = 0
        val watchButtonOnClick: () -> Unit = { ++watchButtonCounter }
        val watchButtonTag = "WatchButton"

        var playButtonCounter = 0
        val playButtonOnClick: () -> Unit = { ++playButtonCounter }
        val playButtonTag = "PlayButton"

        rule.setContent {
            Column {
                Button(
                    modifier = Modifier.testTag(watchButtonTag),
                    onClick = watchButtonOnClick
                ) {
                    Text("Watch")
                }
                Button(
                    modifier = Modifier.testTag(playButtonTag),
                    onClick = playButtonOnClick
                ) {
                    Text("Play")
                }
            }
        }

        rule.onNodeWithTag(watchButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(watchButtonCounter).isEqualTo(1)
            Truth.assertThat(playButtonCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(playButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(watchButtonCounter).isEqualTo(1)
            Truth.assertThat(playButtonCounter).isEqualTo(1)
        }
    }

    @Test
    fun filledButton_buttonPositioning() {
        rule.setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag(FilledButtonTag)
            ) {
                Text(
                    "FilledButton",
                    modifier = Modifier
                        .testTag(FilledButtonTextTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(FilledButtonTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(FilledButtonTextTag).getUnclippedBoundsInRoot()

        (textBounds.left - buttonBounds.left).assertIsEqualTo(
            16.dp,
            "padding between the start of the button and the start of the text."
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the text and the end of the button."
        )
    }

    @Test
    fun filledButtonWithIcon_positioning() {
        rule.setContent {
            Button(
                onClick = {},
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .testTag(FilledButtonTag)
            ) {
                Box(
                    modifier = Modifier
                        .size(FilledButtonIconSize)
                        .testTag(FilledButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
                Spacer(Modifier.size(FilledButtonIconSpacing))
                Text(
                    "Liked it",
                    modifier = Modifier
                        .testTag(FilledButtonTextTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val textBounds = rule.onNodeWithTag(FilledButtonTextTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(FilledButtonIconTag).getUnclippedBoundsInRoot()
        val buttonBounds = rule.onNodeWithTag(FilledButtonTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            expected = 12.dp,
            subject = "Padding between start of button and start of icon."
        )

        (textBounds.left - iconBounds.right).assertIsEqualTo(
            expected = FilledButtonIconSpacing,
            subject = "Padding between end of icon and start of text."
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            expected = 16.dp,
            subject = "padding between end of text and end of button."
        )
    }

    @Test
    fun outlinedButton_defaultSemantics() {
        rule.setContent {
            Box {
                OutlinedButton(modifier = Modifier.testTag(OutlinedButtonTag), onClick = {}) {
                    Text("OutlinedButton")
                }
            }
        }

        rule.onNodeWithTag(OutlinedButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun outlinedButton_longClickSemantics() {
        rule.setContent {
            Box {
                OutlinedButton(
                    modifier = Modifier.testTag(OutlinedButtonTag),
                    onClick = {},
                    onLongClick = {}) {
                    Text("OutlinedButton")
                }
            }
        }

        rule.onNodeWithTag(OutlinedButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assertIsEnabled()
    }

    @Test
    fun outlinedButton_disabledSemantics() {
        rule.setContent {
            Box {
                OutlinedButton(
                    modifier = Modifier.testTag(OutlinedButtonTag),
                    onClick = {},
                    enabled = false
                ) {
                    Text("OutlinedButton")
                }
            }
        }

        rule.onNodeWithTag(OutlinedButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun outlinedButton_findByTag_andClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "OutlinedButtonText"

        rule.setContent {
            Box {
                OutlinedButton(modifier = Modifier.testTag(OutlinedButtonTag), onClick = onClick) {
                    Text(text)
                }
            }
        }
        rule.onNodeWithTag(OutlinedButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun outlinedButton_findByTag_andLongClick() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val text = "OutlinedButtonText"

        rule.setContent {
            Box {
                OutlinedButton(
                    modifier = Modifier.testTag(OutlinedButtonTag),
                    onClick = {},
                    onLongClick = onLongClick) {
                    Text(text)
                }
            }
        }
        rule.onNodeWithTag(OutlinedButtonTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun outlinedButton_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                OutlinedButton(
                    modifier = Modifier.testTag(OutlinedButtonTag),
                    onClick = { enabled = false },
                    enabled = enabled
                ) {
                    Text("Hello")
                }
            }
        }
        rule.onNodeWithTag(OutlinedButtonTag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun outlinedButton_clickIs_independent_betweenButtons() {
        var watchButtonCounter = 0
        val watchButtonOnClick: () -> Unit = { ++watchButtonCounter }
        val watchButtonTag = "WatchButton"

        var playButtonCounter = 0
        val playButtonOnClick: () -> Unit = { ++playButtonCounter }
        val playButtonTag = "PlayButton"

        rule.setContent {
            Column {
                OutlinedButton(
                    modifier = Modifier.testTag(watchButtonTag),
                    onClick = watchButtonOnClick
                ) {
                    Text("Watch")
                }
                OutlinedButton(
                    modifier = Modifier.testTag(playButtonTag),
                    onClick = playButtonOnClick
                ) {
                    Text("Play")
                }
            }
        }

        rule.onNodeWithTag(watchButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(watchButtonCounter).isEqualTo(1)
            Truth.assertThat(playButtonCounter).isEqualTo(0)
        }

        rule.onNodeWithTag(playButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(watchButtonCounter).isEqualTo(1)
            Truth.assertThat(playButtonCounter).isEqualTo(1)
        }
    }

    @Test
    fun outlinedButton_buttonPositioning() {
        rule.setContent {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.testTag(OutlinedButtonTag)
            ) {
                Text(
                    "OutlinedButton",
                    modifier = Modifier
                        .testTag(OutlinedButtonTextTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(OutlinedButtonTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(OutlinedButtonTextTag).getUnclippedBoundsInRoot()

        (textBounds.left - buttonBounds.left).assertIsEqualTo(
            16.dp,
            "padding between the start of the button and the start of the text."
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the text and the end of the button."
        )
    }

    @Test
    fun outlinedButton_buttonWithIcon_positioning() {
        rule.setContent {
            OutlinedButton(
                onClick = {},
                contentPadding = OutlinedButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .testTag(OutlinedButtonTag)
            ) {
                Box(
                    modifier = Modifier
                        .size(OutlinedButtonIconSize)
                        .testTag(OutlinedButtonIconTag)
                        .semantics(mergeDescendants = true) {}
                )
                Spacer(Modifier.size(OutlinedButtonIconSpacing))
                Text(
                    "Liked it",
                    modifier = Modifier
                        .testTag(OutlinedButtonTextTag)
                        .semantics(mergeDescendants = true) {}
                )
            }
        }

        val textBounds = rule.onNodeWithTag(OutlinedButtonTextTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(OutlinedButtonIconTag).getUnclippedBoundsInRoot()
        val buttonBounds = rule.onNodeWithTag(OutlinedButtonTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            expected = 12.dp,
            subject = "Padding between start of button and start of icon."
        )

        (textBounds.left - iconBounds.right).assertIsEqualTo(
            expected = OutlinedButtonIconSpacing,
            subject = "Padding between end of icon and start of text."
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            expected = 16.dp,
            subject = "padding between end of text and end of button."
        )
    }
}

private const val FilledButtonTag = "FilledButton"
private const val FilledButtonTextTag = "FilledButtonText"
private const val FilledButtonIconTag = "FilledButtonIcon"
private val FilledButtonIconSize = 18.0.dp
private val FilledButtonIconSpacing = 8.dp

private const val OutlinedButtonTag = "OutlinedButton"
private const val OutlinedButtonTextTag = "OutlinedButtonText"
private const val OutlinedButtonIconTag = "OutlinedButtonIcon"
private val OutlinedButtonIconSize = 18.0.dp
private val OutlinedButtonIconSpacing = 8.dp
