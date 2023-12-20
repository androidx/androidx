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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
class WideButtonTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun wideButton_defaultSemantics() {
        rule.setContent {
            Box {
                WideButton(
                    onClick = { },
                    modifier = Modifier
                        .testTag(WideButtonTag),
                    title = { Text(text = "Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                    },
                    subtitle = { Text(text = "Update device preferences") }
                )
            }
        }

        rule.onNodeWithTag(WideButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun wideButton_longClickSemantics() {
        rule.setContent {
            Box {
                WideButton(
                    onClick = { },
                    onLongClick = {},
                    modifier = Modifier
                        .testTag(WideButtonTag),
                    title = { Text(text = "Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                    },
                    subtitle = { Text(text = "Update device preferences") }
                )
            }
        }

        rule.onNodeWithTag(WideButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assertIsEnabled()
    }

    @Test
    fun wideButton_disabledSemantics() {
        rule.setContent {
            Box {
                WideButton(
                    onClick = { },
                    modifier = Modifier
                        .testTag(WideButtonTag),
                    enabled = false,
                    title = { Text(text = "Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                    },
                    subtitle = { Text(text = "Update device preferences") }
                )
            }
        }

        rule.onNodeWithTag(WideButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun wideButton_findByTagAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                WideButton(
                    onClick = onClick,
                    modifier = Modifier
                        .testTag(WideButtonTag),
                    title = { Text(text = "Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                    },
                    subtitle = { Text(text = "Update device preferences") }
                )
            }
        }
        rule.onNodeWithTag(WideButtonTag)
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun wideButton_findByTagAndLongClick() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                WideButton(
                    onClick = {},
                    onLongClick = onLongClick,
                    modifier = Modifier
                        .testTag(WideButtonTag),
                    title = { Text(text = "Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                    },
                    subtitle = { Text(text = "Update device preferences") }
                )
            }
        }

        rule.onNodeWithTag(WideButtonTag)
            .requestFocus()
            .performLongKeyPress(rule, Key.DirectionCenter)
        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun wideButton_canBeDisabled() {
        rule.setContent {
            var enabled by remember { mutableStateOf(true) }
            Box {
                WideButton(
                    onClick = { enabled = false },
                    modifier = Modifier
                        .testTag(WideButtonTag),
                    enabled = enabled,
                    title = { Text(text = "Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                    },
                    subtitle = { Text(text = "Update device preferences") }
                )
            }
        }
        rule.onNodeWithTag(WideButtonTag)
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
    fun wideButton_clickIsIndependentBetweenButtons() {
        var watchButtonCounter = 0
        val watchButtonOnClick: () -> Unit = { ++watchButtonCounter }
        val watchButtonTag = "WatchButton"

        var playButtonCounter = 0
        val playButtonOnClick: () -> Unit = { ++playButtonCounter }
        val playButtonTag = "PlayButton"

        rule.setContent {
            Column {
                WideButton(
                    onClick = watchButtonOnClick,
                    modifier = Modifier.testTag(watchButtonTag),
                ) {
                    Text(text = "Watch")
                }
                WideButton(
                    onClick = playButtonOnClick,
                    modifier = Modifier.testTag(playButtonTag),
                ) {
                    Text(text = "Play")
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
    fun wideButton_buttonPositioning() {
        rule.setContent {
            Box {
                WideButton(
                    onClick = { },
                    modifier = Modifier
                        .testTag(WideButtonTag),
                    contentPadding = WideButtonDefaults.ContentPadding,
                    title = {
                        Text(
                            text = "Email",
                            modifier = Modifier
                                .testTag(WideButtonTextTag)
                                .semantics(mergeDescendants = true) {}
                        )
                            },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "",
                            modifier = Modifier
                                .size(WideButtonIconSize)
                                .testTag(WideButtonIconTag)
                                .semantics(mergeDescendants = true) {}
                        )
                    }
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(WideButtonTag).getUnclippedBoundsInRoot()
        val leadingIconBounds = rule.onNodeWithTag(WideButtonIconTag).getUnclippedBoundsInRoot()

        (leadingIconBounds.left - buttonBounds.left).assertIsEqualTo(
            16.dp,
            "padding between the start of the button and the start of the leading icon."
        )
    }
}

private val WideButtonIconSize = 18.0.dp
private const val WideButtonTag = "WideButtonTag"
private const val WideButtonTextTag = "WideButtonText"
private const val WideButtonIconTag = "WideButtonIcon"
