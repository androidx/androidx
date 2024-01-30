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
class RequestFocusEnterTest {
    @get:Rule
    val rule = createComposeRule()

    private val focusRequester = FocusRequester()
    private var enterTriggered = false
    private lateinit var focusState: FocusState

    @Test
    fun gainingFocus_doesNotTriggersEnter() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .focusProperties {
                        enter = { enterTriggered = true; Default }
                    }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(enterTriggered).isFalse()
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun gainingFocus_doesNotTriggersEnterForChild() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusProperties {
                            enter = { enterTriggered = true; Default }
                        }
                        .focusTarget()
                    )
                }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(enterTriggered).isFalse()
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun gainingFocus_triggersEnterForParent() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties {
                        enter = { enterTriggered = true; Default }
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

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(enterTriggered).isTrue()
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun gainingFocus_triggersEnterForGrandparent() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties {
                        enter = { enterTriggered = true; Default }
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

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(enterTriggered).isTrue()
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun cancellingFocusGain_usingEnterProperty() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .focusProperties { enter = { Cancel } }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun cancellingFocusGain_usingEnterPropertyOnChild() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(Modifier.focusProperties { enter = { Cancel } })
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun cancellingFocusGain_usingEnterPropertyOnParent() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties { enter = { Cancel } }
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

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun cancellingFocusGain_usingEnterPropertyOnGrandparent() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties { enter = { Cancel } }
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

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun cancellingFocusGain_usingACustomDestination() {
        // Arrange.
        val customDestination = FocusRequester()
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier
                        .focusProperties { enter = { customDestination } }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
                    )
                    Box(Modifier.focusTarget())
                }
                Box(Modifier.focusTarget()) {
                    Box(
                        Modifier
                            .focusRequester(customDestination)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun cancellingFocusGain_usingAChildAsACustomDestination() {
        // Arrange.
        val customDestination = FocusRequester()
        lateinit var destinationFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties { enter = { customDestination } }
                    .focusTarget()
            ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                    Box(
                        Modifier
                            .focusRequester(customDestination)
                            .onFocusChanged { destinationFocusState = it }
                            .focusTarget()
                    )
            }
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(destinationFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun cancellingFocusGain_usingASiblingAsACustomDestination() {
        // Arrange.
        val customDestination = FocusRequester()
        lateinit var destinationFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties { enter = { customDestination } }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
            Box(
                Modifier
                    .focusRequester(customDestination)
                    .onFocusChanged { destinationFocusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(destinationFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun cancellingFocusGain_usingParentAsACustomDestination() {
        // Arrange.
        val customDestination = FocusRequester()
        lateinit var destinationFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusRequester(customDestination)
                    .focusProperties { enter = { customDestination } }
                    .onFocusChanged { destinationFocusState = it }
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

        // Act.
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(destinationFocusState.isFocused).isTrue()
        }
    }

    @Test
    fun redirectingFocusRequestOnChild1ToChild2_focusEnterIsCalled() {
        // Arrange.
        val (initialFocus, child1, child2) = FocusRequester.createRefs()
        var enterCount = 0
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusProperties {
                            enter = {
                                enterCount++
                                child2
                            }
                        }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .focusRequester(child1)
                            .focusTarget()
                    )
                    Box(
                        Modifier
                            .focusRequester(child2)
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { child1.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(enterCount).isEqualTo(1) }

        // Reset - To ensure that focus enter is called every time we enter.
        rule.runOnIdle {
            initialFocus.requestFocus()
            enterCount = 0
        }

        // Act.
        rule.runOnIdle { child1.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(enterCount).isEqualTo(1) }
    }
}
