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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class ClearFocusExitTest {
    @get:Rule
    val rule = createComposeRule()

    val focusRequester = FocusRequester()
    var clearTriggered = false
    lateinit var focusState: FocusState
    lateinit var focusManager: FocusManager

    @Test
    fun clearFocus_doesNotTriggersExit() {
        // Arrange.
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .focusProperties {
                        exit = { clearTriggered = true; Default }
                    }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(clearTriggered).isFalse()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun clearFocus_doesNotTriggersExitForChild() {
        // Arrange.
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusProperties {
                            exit = { clearTriggered = true; Default }
                        }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(clearTriggered).isFalse()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun clearFocus_triggersExitForParent() {
        // Arrange.
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusProperties {
                        exit = { clearTriggered = true; Default }
                    }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(clearTriggered).isTrue()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun clearFocus_triggersExitForGrandparent() {
        // Arrange.
        val focusRequester = FocusRequester()
        var clearTriggered = false
        lateinit var focusState: FocusState
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusProperties {
                        exit = { clearTriggered = true; Default }
                    }
                    .focusTarget()
            ) {
                Box {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(clearTriggered).isTrue()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun cancellingClearFocus_usingExitProperty() {
        // Arrange.
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { exit = { Cancel } }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun cancellingClearFocus_usingExitPropertyOnChild() {
        // Arrange.
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusProperties { exit = { Cancel } }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun cancellingClearFocus_usingExitPropertyOnParent() {
        // Arrange.
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusProperties { exit = { Cancel } }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun cancellingClearFocus_usingExitPropertyOnGrandparent() {
        // Arrange.
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusProperties { exit = { Cancel } }
                    .focusTarget()
            ) {
                Box {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun clearFocus_usingChildAsCustomExitDestination() {
        // Arrange.
        val customDestination = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusProperties { exit = { customDestination } }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                    )
                Box(
                    Modifier
                        .focusRequester(customDestination)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun clearFocus_usingSelfAsCustomExitDestination() {
        // Arrange.
        val customDestination = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusProperties { exit = { customDestination } }
                    .focusRequester(customDestination)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun clearFocus_usingSiblingAsCustomExitDestination() {
        // Arrange.
        val customDestination = FocusRequester()
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box {
                Box(
                    Modifier
                        .focusProperties { exit = { customDestination } }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
                    )
                }
                Box(
                    Modifier
                        .focusRequester(customDestination)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }
}
