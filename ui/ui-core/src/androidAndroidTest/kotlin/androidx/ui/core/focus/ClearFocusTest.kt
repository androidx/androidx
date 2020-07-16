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

// TODO(b/160821157): Replace FocusDetailedState with FocusState2
@file:Suppress("DEPRECATION")

package androidx.ui.core.focus

import androidx.test.filters.SmallTest
import androidx.ui.core.focus.FocusDetailedState.Active
import androidx.ui.core.focus.FocusDetailedState.ActiveParent
import androidx.ui.core.focus.FocusDetailedState.Captured
import androidx.ui.core.focus.FocusDetailedState.Disabled
import androidx.ui.core.focus.FocusDetailedState.Inactive
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdle
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class ClearFocusTest(val forcedClear: Boolean) {
    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "forcedClear = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun active_isCleared() {
        // Arrange.
        val modifier = FocusModifierImpl(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test
    fun active_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifierImpl(ActiveParent)
        val modifier = FocusModifierImpl(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = parent) {
                Box(modifier = modifier)
            }
            parent.focusedChild = modifier.focusNode
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(parent.focusedChild).isNull()
            assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_noFocusedChild_throwsException() {
        // Arrange.
        val modifier = FocusModifierImpl(ActiveParent)
        composeTestRule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }
    }

    @Test
    fun activeParent_isClearedAndRemovedFromParentsFocusedChild() {
        // Arrange.
        val parent = FocusModifierImpl(ActiveParent)
        val modifier = FocusModifierImpl(ActiveParent)
        val child = FocusModifierImpl(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = parent) {
                Box(modifier = modifier) {
                    Box(modifier = child)
                }
            }
            parent.focusedChild = modifier.focusNode
            modifier.focusedChild = child.focusNode
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test
    fun activeParent_clearsEntireHierarchy() {
        // Arrange.
        val modifier = FocusModifierImpl(ActiveParent)
        val child = FocusModifierImpl(ActiveParent)
        val grandchild = FocusModifierImpl(ActiveParent)
        val greatgrandchild = FocusModifierImpl(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = modifier) {
                Box(modifier = child) {
                    Box(modifier = grandchild) {
                        Box(modifier = greatgrandchild)
                    }
                }
            }
            modifier.focusedChild = child.focusNode
            child.focusedChild = grandchild.focusNode
            grandchild.focusedChild = greatgrandchild.focusNode
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusedChild).isNull()
            assertThat(child.focusedChild).isNull()
            assertThat(grandchild.focusedChild).isNull()
            assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
            assertThat(child.focusDetailedState).isEqualTo(Inactive)
            assertThat(grandchild.focusDetailedState).isEqualTo(Inactive)
            assertThat(greatgrandchild.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test
    fun captured_isCleared_whenForced() {
        // Arrange.
        val modifier = FocusModifierImpl(Captured)
        composeTestRule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            when (forcedClear) {
                true -> {
                    assertThat(cleared).isTrue()
                    assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
                }
                false -> {
                    assertThat(cleared).isFalse()
                    assertThat(modifier.focusDetailedState).isEqualTo(Captured)
                }
            }
        }
    }

    @Test
    fun active_isClearedAndRemovedFromParentsFocusedChild_whenForced() {
        // Arrange.
        val parent = FocusModifierImpl(ActiveParent)
        val modifier = FocusModifierImpl(Captured)
        composeTestRule.setFocusableContent {
            Box(modifier = parent) {
                Box(modifier = modifier)
            }
            parent.focusedChild = modifier.focusNode
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            when (forcedClear) {
                true -> {
                    assertThat(cleared).isTrue()
                    assertThat(parent.focusedChild).isNull()
                    assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
                }
                false -> {
                    assertThat(cleared).isFalse()
                    assertThat(parent.focusedChild).isEqualTo(modifier.focusNode)
                    assertThat(modifier.focusDetailedState).isEqualTo(Captured)
                }
            }
        }
    }

    @Test
    fun Inactive_isUnchanged() {
        // Arrange.
        val modifier = FocusModifierImpl(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test
    fun Disabled_isUnchanged() {
        // Arrange.
        val modifier = FocusModifierImpl(Disabled)
        composeTestRule.setFocusableContent {
            Box(modifier = modifier)
        }

        // Act.
        val cleared = runOnIdle {
            modifier.focusNode!!.clearFocus(forcedClear)
        }

        // Assert.
        runOnIdle {
            assertThat(cleared).isTrue()
            assertThat(modifier.focusDetailedState).isEqualTo(Disabled)
        }
    }
}