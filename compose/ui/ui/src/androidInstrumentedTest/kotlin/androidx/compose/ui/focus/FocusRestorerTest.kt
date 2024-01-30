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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.key
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalFoundationApi
@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusRestorerTest {
    @get:Rule
    val rule = createComposeRule()

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
                    .size(10.dp)
                    .focusRequester(parent)
                    .focusRestorer()
                    .focusGroup()
            ) {
                key(1) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .onFocusChanged { child1State = it }
                            .focusTarget()
                    )
                }
                key(2) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .focusRequester(child2)
                            .onFocusChanged { child2State = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { child2.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }
        rule.runOnIdle { parent.requestFocus() }

        // Assert.
        rule.runOnIdle {
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
                    .size(10.dp)
                    .focusRequester(parent)
                    .focusRestorer()
                    .focusGroup()
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .onFocusChanged { child1State = it }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .size(10.dp)
                        .focusRequester(child2)
                        .onFocusChanged { child2State = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { child2.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }
        rule.runOnIdle { parent.requestFocus() }

        // Assert.
        rule.runOnIdle {
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
                    .size(10.dp)
                    .focusRequester(parent)
                    .focusRestorer()
                    .focusGroup()
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .onFocusChanged { childState = it }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .focusRequester(grandChild)
                            .onFocusChanged { grandChildState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { grandChild.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }
        rule.runOnIdle { parent.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(childState.isFocused).isTrue()
            assertThat(grandChildState.isFocused).isFalse()
        }
    }

    @Test
    fun restorationFailed_fallbackToOnRestoreFailedDestination() {
        // Arrange.
        val (parent, child2) = FocusRequester.createRefs()
        lateinit var child1State: FocusState
        lateinit var child2State: FocusState
        rule.setFocusableContent {
            Box(
                Modifier
                    .size(10.dp)
                    .focusRequester(parent)
                    .focusRestorer { child2 }
                    .focusGroup()
            ) {
                key(1) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .onFocusChanged { child1State = it }
                            .focusTarget()
                    )
                }
                key(2) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .focusRequester(child2)
                            .onFocusChanged { child2State = it }
                            .focusTarget()
                    )
                }
            }
        }

        // Act.
        rule.runOnIdle { parent.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(child1State.isFocused).isFalse()
            assertThat(child2State.isFocused).isTrue()
        }
    }
}
