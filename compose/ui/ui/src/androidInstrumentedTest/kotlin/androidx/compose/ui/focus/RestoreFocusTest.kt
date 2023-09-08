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
import androidx.compose.runtime.key
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class RestoreFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noSavedChild_doesNotRestoreChild() {
        // Arrange.
        val (parent, child1) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        lateinit var child1State: FocusState
        lateinit var child2State: FocusState
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(parent)
                    .focusTarget()
            ) {
                key(1) {
                    Box(
                        Modifier
                            .focusRequester(child1)
                            .onFocusChanged { child1State = it }
                            .focusTarget()
                    )
                }
                key(2) {
                    Box(
                        Modifier
                            .onFocusChanged { child2State = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { child1.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }
        val restoredSuccessfully = rule.runOnIdle { parent.restoreFocusedChild() }

        // Assert.
        rule.runOnIdle {
            assertThat(restoredSuccessfully).isFalse()
            assertThat(child1State.isFocused).isFalse()
            assertThat(child2State.isFocused).isFalse()
        }
    }

    @Test
    fun restoresSavedChild() {
        // Arrange.
        val (parent, child2) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        lateinit var child1State: FocusState
        lateinit var child2State: FocusState
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(parent)
                    .focusTarget()
            ) {
                key(1) {
                    Box(
                        Modifier
                            .onFocusChanged { child1State = it }
                            .focusTarget()
                    )
                }
                key(2) {
                    Box(
                        Modifier
                            .focusRequester(child2)
                            .onFocusChanged { child2State = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { child2.requestFocus() }

        // Act.
        val savedSuccessfully = rule.runOnIdle { parent.saveFocusedChild() }
        rule.runOnIdle { focusManager.clearFocus() }
        val restoredSuccessfully = rule.runOnIdle { parent.restoreFocusedChild() }

        // Assert.
        rule.runOnIdle {
            assertThat(savedSuccessfully).isTrue()
            assertThat(restoredSuccessfully).isTrue()
            assertThat(child1State.isFocused).isFalse()
            assertThat(child2State.isFocused).isTrue()
        }
    }

    @Test
    fun withoutUniqueKeysRestoresFirstMatchingChild() {
        // Arrange.
        val (parent, child2) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        lateinit var child1State: FocusState
        lateinit var child2State: FocusState
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(parent)
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .onFocusChanged { child1State = it }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(child2)
                        .onFocusChanged { child2State = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { child2.requestFocus() }

        // Act.
        val savedSuccessfully = rule.runOnIdle { parent.saveFocusedChild() }
        rule.runOnIdle { focusManager.clearFocus() }
        val restoredSuccessfully = rule.runOnIdle { parent.restoreFocusedChild() }

        // Assert.
        rule.runOnIdle {
            assertThat(savedSuccessfully).isTrue()
            assertThat(restoredSuccessfully).isTrue()
            assertThat(child1State.isFocused).isTrue()
            assertThat(child2State.isFocused).isFalse()
        }
    }

    @Test
    fun doesNotRestoreGrandChild_butFocusesOnChildInstead() {
        // Arrange.
        val (parent, grandChild) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        lateinit var childState: FocusState
        lateinit var grandChildState: FocusState
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .focusRequester(parent)
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .onFocusChanged { childState = it }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .focusRequester(grandChild)
                            .onFocusChanged { grandChildState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { grandChild.requestFocus() }

        // Act.
        val savedSuccessfully = rule.runOnIdle { parent.saveFocusedChild() }
        rule.runOnIdle { focusManager.clearFocus() }
        val restoredSuccessfully = rule.runOnIdle { parent.restoreFocusedChild() }

        // Assert.
        rule.runOnIdle {
            assertThat(savedSuccessfully).isTrue()
            assertThat(restoredSuccessfully).isTrue()
            assertThat(childState.isFocused).isTrue()
            assertThat(grandChildState.isFocused).isFalse()
        }
    }
}
