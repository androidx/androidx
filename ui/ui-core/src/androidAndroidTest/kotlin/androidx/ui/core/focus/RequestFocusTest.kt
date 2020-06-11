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

import androidx.test.filters.SmallTest
import androidx.ui.core.focus.FocusDetailedState.Active
import androidx.ui.core.focus.FocusDetailedState.ActiveParent
import androidx.ui.core.focus.FocusDetailedState.Captured
import androidx.ui.core.focus.FocusDetailedState.Disabled
import androidx.ui.core.focus.FocusDetailedState.Inactive
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
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
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifierImpl(Active)
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun captured_isUnchanged() {

        // Arrange.
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifierImpl(Captured)
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(focusModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun disabled_isUnchanged() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifierImpl(Disabled)
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(focusModifier.focusDetailedState).isEqualTo(Disabled)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_withNoFocusedChild_throwsException() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifierImpl(ActiveParent)
            Box(modifier = focusModifier)
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }
    }

    @Test
    fun activeParent_propagateFocus() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        lateinit var childFocusModifier: FocusModifier

        composeTestRule.setFocusableContent {
            focusModifier = FocusModifierImpl(ActiveParent)
            childFocusModifier = FocusModifierImpl(Active)
            Box(modifier = focusModifier) {
                Box(modifier = childFocusModifier)
            }
        }
        runOnIdleCompose {
            focusModifier.focusedChild = childFocusModifier.focusNode
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            when (propagateFocus) {
                true -> {
                    // Unchanged.
                    assertThat(focusModifier.focusDetailedState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusDetailedState).isEqualTo(Active)
                }
                false -> {
                    assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
                    assertThat(focusModifier.focusedChild).isNull()
                    assertThat(childFocusModifier.focusDetailedState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun inactiveRoot_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        lateinit var rootFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            rootFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = rootFocusModifier)
        }

        // Act.
        runOnIdleCompose {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(rootFocusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun inactiveRootWithChildren_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        lateinit var rootFocusModifier: FocusModifier
        lateinit var childFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            rootFocusModifier = FocusModifierImpl(Inactive)
            childFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = rootFocusModifier) {
                Box(modifier = childFocusModifier)
            }
        }

        // Act.
        runOnIdleCompose {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            when (propagateFocus) {
                true -> {
                    // Unchanged.
                    assertThat(rootFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusDetailedState).isEqualTo(Active)
                }
                false -> {
                    assertThat(rootFocusModifier.focusDetailedState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusDetailedState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun inactiveNonRootWithChilcren() {
        // Arrange.
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        lateinit var childFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            parentFocusModifier = FocusModifierImpl(Active)
            focusModifier = FocusModifierImpl(Inactive)
            childFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            when (propagateFocus) {
                true -> {
                    assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusDetailedState).isEqualTo(ActiveParent)
                    assertThat(childFocusModifier.focusDetailedState).isEqualTo(Active)
                }
                false -> {
                    assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
                    assertThat(childFocusModifier.focusDetailedState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun rootNode() {
        // Arrange.
        lateinit var rootFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            rootFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = rootFocusModifier)
        }

        // Act.
        runOnIdleCompose {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(rootFocusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun rootNodeWithChildren() {
        // Arrange.
        lateinit var rootFocusModifier: FocusModifier
        lateinit var childFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            rootFocusModifier = FocusModifierImpl(Inactive)
            childFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = rootFocusModifier) {
                Box(modifier = childFocusModifier)
            }
        }

        // Act.
        runOnIdleCompose {
            rootFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            when (propagateFocus) {
                true -> assertThat(rootFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                false -> assertThat(rootFocusModifier.focusDetailedState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        // Arrange.
        lateinit var grandParentFocusModifier: FocusModifier
        lateinit var parentFocusModifier: FocusModifier
        lateinit var childFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            grandParentFocusModifier = FocusModifierImpl(Inactive)
            parentFocusModifier = FocusModifierImpl(Inactive)
            childFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdleCompose {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            when (propagateFocus) {
                true -> assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                false -> assertThat(parentFocusModifier.focusDetailedState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        // Arrange.
        lateinit var grandParentFocusModifier: FocusModifier
        lateinit var parentFocusModifier: FocusModifier
        lateinit var childFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            grandParentFocusModifier = FocusModifierImpl(Inactive)
            parentFocusModifier = FocusModifierImpl(Inactive)
            childFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdleCompose {
            childFocusModifier.focusNode.requestFocus(propagateFocus)
        }
        // Assert.
        runOnIdleCompose {
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        // Arrange.
        lateinit var grandParentFocusModifier: FocusModifier
        lateinit var parentFocusModifier: FocusModifier
        lateinit var childFocusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            grandParentFocusModifier = FocusModifierImpl(Inactive)
            parentFocusModifier = FocusModifierImpl(Inactive)
            childFocusModifier = FocusModifierImpl(Inactive)
            Box(modifier = grandParentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
        }

        // Act.
        runOnIdleCompose {
            childFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(childFocusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_parentIsFocused() {
        // Arrange.
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            parentFocusModifier = FocusModifierImpl(Active)
            focusModifier = FocusModifierImpl(Inactive)
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }

        // After executing requestFocus, siblingNode will be 'Active'.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_childIsFocused() {
        // Arrange.
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            parentFocusModifier = FocusModifierImpl(ActiveParent)
            focusModifier = FocusModifierImpl(Active)
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }
        runOnIdleCompose {
            parentFocusModifier.focusedChild = focusModifier.focusNode
        }

        // Act.
        runOnIdleCompose {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            when (propagateFocus) {
                true -> {
                    assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                    assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
                }
                false -> {
                    assertThat(parentFocusModifier.focusDetailedState).isEqualTo(Active)
                    assertThat(focusModifier.focusDetailedState).isEqualTo(Inactive)
                }
            }
        }
    }

    @Test
    fun requestFocus_childHasCapturedFocus() {
        // Arrange.
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            parentFocusModifier = FocusModifierImpl(ActiveParent)
            focusModifier = FocusModifierImpl(Captured)
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
            }
        }
        runOnIdleCompose {
            parentFocusModifier.focusedChild = focusModifier.focusNode
        }

        // Act.
        runOnIdleCompose {
            parentFocusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_siblingIsFocused() {
        // Arrange.
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        lateinit var siblingModifier: FocusModifier
        composeTestRule.setFocusableContent {
            parentFocusModifier = FocusModifierImpl(ActiveParent)
            focusModifier = FocusModifierImpl(Inactive)
            siblingModifier = FocusModifierImpl(Active)
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
                Box(modifier = siblingModifier)
            }
        }
        runOnIdleCompose {
            parentFocusModifier.focusedChild = siblingModifier.focusNode
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
            assertThat(siblingModifier.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_siblingHasCapturedFocused() {
        // Arrange.
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        lateinit var siblingModifier: FocusModifier
        composeTestRule.setFocusableContent {
            parentFocusModifier = FocusModifierImpl(ActiveParent)
            focusModifier = FocusModifierImpl(Inactive)
            siblingModifier = FocusModifierImpl(Captured)
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier)
                Box(modifier = siblingModifier)
            }
        }
        runOnIdleCompose {
            parentFocusModifier.focusedChild = siblingModifier.focusNode
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Inactive)
            assertThat(siblingModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_cousinIsFocused() {
        // Arrange.
        lateinit var grandParentModifier: FocusModifier
        lateinit var parentModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        lateinit var auntModifier: FocusModifier
        lateinit var cousinModifier: FocusModifier
        composeTestRule.setFocusableContent {
            grandParentModifier = FocusModifierImpl(ActiveParent)
            parentModifier = FocusModifierImpl(Inactive)
            focusModifier = FocusModifierImpl(Inactive)
            auntModifier = FocusModifierImpl(ActiveParent)
            cousinModifier = FocusModifierImpl(Active)
            Box(modifier = grandParentModifier) {
                Box(modifier = parentModifier) {
                    Box(modifier = focusModifier)
                }
                Box(modifier = auntModifier) {
                    Box(modifier = cousinModifier)
                }
            }
        }
        runOnIdleCompose {
            grandParentModifier.focusedChild = auntModifier.focusNode
            auntModifier.focusedChild = cousinModifier.focusNode
        }

        // Verify Setup.
        runOnIdleCompose {
            assertThat(cousinModifier.focusDetailedState).isEqualTo(Active)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Inactive)
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(cousinModifier.focusDetailedState).isEqualTo(Inactive)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_grandParentIsFocused() {
        // Arrange.
        lateinit var grandParentModifier: FocusModifier
        lateinit var parentModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            grandParentModifier = FocusModifierImpl(Active)
            parentModifier = FocusModifierImpl(Inactive)
            focusModifier = FocusModifierImpl(Inactive)
            Box(modifier = grandParentModifier) {
                Box(modifier = parentModifier) {
                    Box(modifier = focusModifier)
                }
            }
        }

        // Act.
        runOnIdleCompose {
            focusModifier.focusNode.requestFocus(propagateFocus)
        }

        // Assert.
        runOnIdleCompose {
            assertThat(grandParentModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(parentModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }
}