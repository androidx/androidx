/*
 * Copyright 2020 The Android Open Source Project
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

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusManagerCompositionLocalTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private lateinit var inputModeManager: InputModeManager
    private val focusStates = mutableListOf<FocusState>()

    @Test
    fun clearFocus_singleLayout_focusIsRestoredAfterClear() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.setTestContent(extraItemForInitialFocus = false) {
            Box(
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusStates += it }
                        .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            when (inputModeManager.inputMode) {
                Keyboard -> {
                    assertThat(focusStates).containsExactly(Inactive, Active).inOrder()
                    assertThat(focusManager.rootFocusState.hasFocus).isTrue()
                }
                Touch -> {
                    assertThat(focusStates).containsExactly(Inactive).inOrder()
                    assertThat(focusManager.rootFocusState.hasFocus).isFalse()
                }
            }
        }
    }

    @Test
    fun clearFocus_entireHierarchyIsCleared() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var focusState: FocusState
        lateinit var parentFocusState: FocusState
        lateinit var grandparentFocusState: FocusState
        val focusRequester = FocusRequester()
        rule.setTestContent {
            focusManager = LocalFocusManager.current
            Box(modifier = Modifier.onFocusChanged { grandparentFocusState = it }.focusTarget()) {
                Box(modifier = Modifier.onFocusChanged { parentFocusState = it }.focusTarget()) {
                    Box(
                        modifier =
                            Modifier.focusRequester(focusRequester)
                                .onFocusChanged { focusState = it }
                                .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(grandparentFocusState.hasFocus).isTrue()
            assertThat(parentFocusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(grandparentFocusState.hasFocus).isFalse()
            assertThat(parentFocusState.hasFocus).isFalse()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Ignore("b/325466015")
    @Test
    fun takeFocus_whenRootIsInactive() {
        // Arrange.
        lateinit var view: View
        rule.setTestContent(extraItemForInitialFocus = false) {
            view = LocalView.current
            Box(modifier = Modifier.onFocusChanged { focusStates += it }.focusTarget())
        }

        // Act.
        rule.runOnIdle {
            focusStates.clear()
            view.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(ActiveParent)
            assertThat(focusStates).containsExactly(Active)
        }
    }

    @Test
    fun releaseFocus_whenRootIsInactive() {
        // Arrange.
        lateinit var view: View
        rule.setTestContent(extraItemForInitialFocus = false) {
            view = LocalView.current
            Box(modifier = Modifier.onFocusChanged { focusStates += it }.focusTarget())
        }

        // Act.
        rule.runOnIdle { view.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
            assertThat(focusStates).containsExactly(Inactive)
        }
    }

    @Test
    fun releaseFocus_whenOwnerFocusIsCleared() {
        // Arrange.
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setTestContent(extraItemForInitialFocus = false) {
            view = LocalView.current
            Box(
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusStates += it }
                        .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { view.clearFocus() }

        // Assert.
        rule.runOnIdle {
            when (inputModeManager.inputMode) {
                Keyboard -> {
                    // Focus is re-assigned to the initially focused item (default focus).
                    assertThat(focusManager.rootFocusState).isEqualTo(ActiveParent)
                    assertThat(focusStates).containsExactly(Inactive, Active).inOrder()
                }
                Touch -> {
                    assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
                    assertThat(focusStates).containsExactly(Inactive)
                }
                else -> error("Invalid input mode")
            }
        }
    }

    @Test
    fun clearFocus_whenRootIsInactive() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.setTestContent {
            Box(
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusStates += it }
                        .focusTarget()
            )
        }
        rule.runOnIdle { focusStates.clear() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            when (inputModeManager.inputMode) {
                Keyboard -> {
                    assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
                    assertThat(focusStates).isEmpty()
                }
                Touch -> {
                    assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
                    assertThat(focusStates).isEmpty()
                }
                else -> error("Invalid input mode")
            }
        }
    }

    @Test
    fun clearFocus_whenRootIsActiveParent() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.setTestContent(extraItemForInitialFocus = false) {
            Box(
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusStates += it }
                        .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            when (inputModeManager.inputMode) {
                Keyboard -> {
                    assertThat(focusManager.rootFocusState).isEqualTo(ActiveParent)
                    assertThat(focusStates).containsExactly(Inactive, Active).inOrder()
                }
                Touch -> {
                    assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
                    assertThat(focusStates).containsExactly(Inactive)
                }
                else -> error("Invalid input mode")
            }
        }
    }

    @Test
    fun clearFocus_whenHierarchyHasCapturedFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.setTestContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusStates += it }
                        .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusManager.rootFocusState).isEqualTo(ActiveParent)
            assertThat(focusStates).isEmpty()
        }
    }

    @Test
    fun clearFocus_forced_whenHierarchyHasCapturedFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.setTestContent(extraItemForInitialFocus = false) {
            Box(
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusStates += it }
                        .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            focusStates.clear()
        }

        // Act.
        rule.runOnIdle { focusManager.clearFocus(force = true) }

        // Assert.
        rule.runOnIdle {
            when (inputModeManager.inputMode) {
                Keyboard -> {
                    // Focus is re-assigned to the initially focused item (default focus).
                    assertThat(focusManager.rootFocusState).isEqualTo(ActiveParent)
                    assertThat(focusStates).containsExactly(Inactive, Active).inOrder()
                }
                Touch -> {
                    assertThat(focusManager.rootFocusState).isEqualTo(Inactive)
                    assertThat(focusStates).containsExactly(Inactive).inOrder()
                }
                else -> error("Invalid input mode")
            }
        }
    }

    private val FocusManager.rootFocusState: FocusState
        get() = (this as FocusOwnerImpl).rootFocusNode.focusState

    private fun ComposeContentTestRule.setTestContent(
        extraItemForInitialFocus: Boolean = true,
        content: @Composable () -> Unit
    ) {
        setFocusableContent(extraItemForInitialFocus) {
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current
            content()
        }
    }
}
