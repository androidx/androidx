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

package androidx.ui.core.focus

import androidx.compose.foundation.Box
import androidx.test.filters.SmallTest
import androidx.ui.core.focus.FocusState2.Active
import androidx.ui.core.focus.FocusState2.ActiveParent
import androidx.ui.core.focus.FocusState2.Captured
import androidx.ui.core.focus.FocusState2.Disabled
import androidx.ui.core.focus.FocusState2.Inactive
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdle
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@OptIn(ExperimentalFocus::class)
@RunWith(Parameterized::class)
class RequestFocusTest(val propagateFocus: Boolean) {
    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "propagateFocus = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun active_isUnchanged() {
        // Arrange.
        val focusModifier = FocusModifier2(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun captured_isUnchanged() {

        // Arrange.
        val focusModifier = FocusModifier2(Captured)
        composeTestRule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun disabled_isUnchanged() {
        // Arrange.
        val focusModifier = FocusModifier2(Disabled)
        composeTestRule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(focusModifier.focusState).isEqualTo(Disabled)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_withNoFocusedChild_throwsException() {
        // Arrange.
        val focusModifier = FocusModifier2(ActiveParent)
        composeTestRule.setFocusableContent {
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }
    }

    @Test
    fun activeParent_propagateFocus() {
        // Arrange.
        val focusModifier = FocusModifier2(ActiveParent)
        val childFocusModifier = FocusModifier2(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = focusModifier) {
                Box(modifier = childFocusModifier)
            }
        }
        runOnIdle {
            focusModifier.focusedChild = childFocusModifier.focusNode
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            when (propagateFocus) {
                true -> {
                    // Unchanged.
                    assertThat(focusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(focusModifier.focusState).isEqualTo(Active)
                    assertThat(focusModifier.focusedChild).isNull()
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun inactiveRoot_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val rootFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = rootFocusModifier)
        }

        // Act.
        runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(rootFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun inactiveRootWithChildren_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val rootFocusModifier = FocusModifier2(Inactive)
        val childFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = rootFocusModifier) {
                Box(modifier = childFocusModifier)
            }
        }

        // Act.
        runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            when (propagateFocus) {
                true -> {
                    // Unchanged.
                    assertThat(rootFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(rootFocusModifier.focusState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun inactiveNonRootWithChilcren() {
        // Arrange.
        val parentFocusModifier = FocusModifier2(Active)
        val focusModifier = FocusModifier2(Inactive)
        val childFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            when (propagateFocus) {
                true -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun rootNode() {
        // Arrange.
        val rootFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = rootFocusModifier)
        }

        // Act.
        runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(rootFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun rootNodeWithChildren() {
        // Arrange.
        val rootFocusModifier = FocusModifier2(Inactive)
        val childFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = rootFocusModifier) {
                Box(modifier = childFocusModifier)
            }
        }

        // Act.
        runOnIdle {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            when (propagateFocus) {
                true -> assertThat(rootFocusModifier.focusState).isEqualTo(ActiveParent)
                false -> assertThat(rootFocusModifier.focusState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier2(Inactive)
        val parentFocusModifier = FocusModifier2(Inactive)
        val childFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdle {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            when (propagateFocus) {
                true -> assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                false -> assertThat(parentFocusModifier.focusState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier2(Inactive)
        val parentFocusModifier = FocusModifier2(Inactive)
        val childFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdle {
            childFocusModifier.focusNode.requestFocus(propagateFocus)
        }
        // Assert.
        runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        // Arrange.
        val grandParentFocusModifier = FocusModifier2(Inactive)
        val parentFocusModifier = FocusModifier2(Inactive)
        val childFocusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdle {
            childFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(childFocusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_parentIsFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier2(Active)
        val focusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }

        // After executing requestFocus, siblingNode will be 'Active'.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_childIsFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier2(ActiveParent)
        val focusModifier = FocusModifier2(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }
        runOnIdle {
            parentFocusModifier.focusedChild = focusModifier.focusNode
        }

        // Act.
        runOnIdle {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            when (propagateFocus) {
                true -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusState).isEqualTo(Active)
                }
                false -> {
                    assertThat(parentFocusModifier.focusState).isEqualTo(Active)
                    assertThat(focusModifier.focusState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun requestFocus_childHasCapturedFocus() {
        // Arrange.
        val parentFocusModifier = FocusModifier2(ActiveParent)
        val focusModifier = FocusModifier2(Captured)
        composeTestRule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }
        runOnIdle {
            parentFocusModifier.focusedChild = focusModifier.focusNode
        }

        // Act.
        runOnIdle {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_siblingIsFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier2(ActiveParent)
        val focusModifier = FocusModifier2(Inactive)
        val siblingModifier = FocusModifier2(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
                Box(modifier = siblingModifier)
            }
        }
        runOnIdle {
            parentFocusModifier.focusedChild = siblingModifier.focusNode
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Active)
            assertThat(siblingModifier.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_siblingHasCapturedFocused() {
        // Arrange.
        val parentFocusModifier = FocusModifier2(ActiveParent)
        val focusModifier = FocusModifier2(Inactive)
        val siblingModifier = FocusModifier2(Captured)
        composeTestRule.setFocusableContent {
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
                Box(modifier = siblingModifier)
            }
        }
        runOnIdle {
            parentFocusModifier.focusedChild = siblingModifier.focusNode
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(parentFocusModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Inactive)
            assertThat(siblingModifier.focusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_cousinIsFocused() {
        // Arrange.
        val grandParentModifier = FocusModifier2(ActiveParent)
        val parentModifier = FocusModifier2(Inactive)
        val focusModifier = FocusModifier2(Inactive)
        val auntModifier = FocusModifier2(ActiveParent)
        val cousinModifier = FocusModifier2(Active)
        composeTestRule.setFocusableContent {
            Box(modifier = grandParentModifier) {
                Box(modifier = parentModifier) {
                    Box(modifier = focusModifier)
                }
                Box(modifier = auntModifier) {
                    Box(modifier = cousinModifier)
                }
            }
        }
        runOnIdle {
            grandParentModifier.focusedChild = auntModifier.focusNode
            auntModifier.focusedChild = cousinModifier.focusNode
        }

        // Verify Setup.
        runOnIdle {
            assertThat(cousinModifier.focusState).isEqualTo(Active)
            assertThat(focusModifier.focusState).isEqualTo(Inactive)
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(cousinModifier.focusState).isEqualTo(Inactive)
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_grandParentIsFocused() {
        // Arrange.
        val grandParentModifier = FocusModifier2(Active)
        val parentModifier = FocusModifier2(Inactive)
        val focusModifier = FocusModifier2(Inactive)
        composeTestRule.setFocusableContent {
            Box(modifier = grandParentModifier) {
                Box(modifier = parentModifier) {
                    Box(modifier = focusModifier)
                }
            }
        }

        // Act.
        runOnIdle {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdle {
            assertThat(grandParentModifier.focusState).isEqualTo(ActiveParent)
            assertThat(parentModifier.focusState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusState).isEqualTo(Active)
        }
    }
}