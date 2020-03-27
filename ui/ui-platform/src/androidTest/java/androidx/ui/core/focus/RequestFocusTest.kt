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
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Inactive
import androidx.ui.foundation.Box
import androidx.ui.test.createComposeRule

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
        composeTestRule.runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(Active).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun captured_isUnchanged() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(Captured).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(focusModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun disabled_isUnchanged() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(Disabled).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(focusModifier.focusDetailedState).isEqualTo(Disabled)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_withNoFocusedChild_throwsException() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(ActiveParent).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)
        }
    }

    @Test
    fun activeParent_propagateFocus() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(ActiveParent)
            val childFocusModifier = createFocusModifier(Active)
            composeTestRule.setContent {
                Box(modifier = focusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }
            focusModifier.focusedChild = childFocusModifier.focusNode

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
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
        composeTestRule.runOnUiThread {
            // Arrange.
            val rootFocusModifier = createFocusModifier(Inactive).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            rootFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(rootFocusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun inactiveRootWithChildren_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val rootFocusModifier = createFocusModifier(Inactive)
            val childFocusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = rootFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }

            // Act.
            rootFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
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
        composeTestRule.runOnUiThread {
            // Arrange.
            val parentFocusModifier = createFocusModifier(Active)
            val focusModifier = createFocusModifier(Inactive)
            val childFocusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier) {
                        Box(modifier = childFocusModifier)
                    }
                }
            }

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
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
        composeTestRule.runOnUiThread {
            // Arrange.
            val rootFocusModifier = createFocusModifier(Inactive).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            rootFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(rootFocusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun rootNodeWithChildren() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val rootFocusModifier = createFocusModifier(Inactive)
            val childFocusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = rootFocusModifier) {
                    Box(modifier = childFocusModifier)
                }
            }

            // Act.
            rootFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            when (propagateFocus) {
                true -> assertThat(rootFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                false -> assertThat(rootFocusModifier.focusDetailedState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val grandParentFocusModifier = createFocusModifier(Inactive)
            val parentFocusModifier = createFocusModifier(Inactive)
            val childFocusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = grandParentFocusModifier) {
                    Box(modifier = parentFocusModifier) {
                        Box(modifier = childFocusModifier)
                    }
                }
            }

            // Act.
            parentFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            when (propagateFocus) {
                true -> assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
                false -> assertThat(parentFocusModifier.focusDetailedState).isEqualTo(Active)
            }
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val grandParentFocusModifier = createFocusModifier(Inactive)
            val parentFocusModifier = createFocusModifier(Inactive)
            val childFocusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = grandParentFocusModifier) {
                    Box(modifier = parentFocusModifier) {
                        Box(modifier = childFocusModifier)
                    }
                }
            }

            // Act.
            childFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val grandParentFocusModifier = createFocusModifier(Inactive)
            val parentFocusModifier = createFocusModifier(Inactive)
            val childFocusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = grandParentFocusModifier) {
                    Box(modifier = parentFocusModifier) {
                        Box(modifier = childFocusModifier)
                    }
                }
            }

            // Act.
            childFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(childFocusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_parentIsFocused() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val parentFocusModifier = createFocusModifier(Active)
            val focusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier)
                }
            }

            // After executing requestFocus, siblingNode will be 'Active'.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_childIsFocused() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val parentFocusModifier = createFocusModifier(ActiveParent)
            val focusModifier = createFocusModifier(Active)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier)
                }
            }
            parentFocusModifier.focusedChild = focusModifier.focusNode

            // Act.
            parentFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
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
        composeTestRule.runOnUiThread {
            // Arrange.
            val parentFocusModifier = createFocusModifier(ActiveParent)
            val focusModifier = createFocusModifier(Captured)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier)
                }
            }
            parentFocusModifier.focusedChild = focusModifier.focusNode

            // Act.
            parentFocusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_siblingIsFocused() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val parentFocusModifier = createFocusModifier(ActiveParent)
            val focusModifier = createFocusModifier(Inactive)
            val siblingModifier = createFocusModifier(Active)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier)
                    Box(modifier = siblingModifier)
                }
            }
            parentFocusModifier.focusedChild = siblingModifier.focusNode

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
            assertThat(siblingModifier.focusDetailedState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_siblingHasCapturedFocused() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val parentFocusModifier = createFocusModifier(ActiveParent)
            val focusModifier = createFocusModifier(Inactive)
            val siblingModifier = createFocusModifier(Captured)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier)
                    Box(modifier = siblingModifier)
                }
            }
            parentFocusModifier.focusedChild = siblingModifier.focusNode

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(parentFocusModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Inactive)
            assertThat(siblingModifier.focusDetailedState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_cousinIsFocused() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val grandParentModifier = createFocusModifier(ActiveParent)
            val parentModifier = createFocusModifier(Inactive)
            val focusModifier = createFocusModifier(Inactive)
            val auntModifier = createFocusModifier(ActiveParent)
            val cousinModifier = createFocusModifier(Active)
            composeTestRule.setContent {
                Box(modifier = grandParentModifier) {
                    Box(modifier = parentModifier) {
                        Box(modifier = focusModifier)
                    }
                    Box(modifier = auntModifier) {
                        Box(modifier = cousinModifier)
                    }
                }
            }
            grandParentModifier.focusedChild = auntModifier.focusNode
            auntModifier.focusedChild = cousinModifier.focusNode

            // Verify Setup.
            assertThat(cousinModifier.focusDetailedState).isEqualTo(Active)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Inactive)

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(cousinModifier.focusDetailedState).isEqualTo(Inactive)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_grandParentIsFocused() {
        composeTestRule.runOnUiThread {
            // Arrange.
            val grandParentModifier = createFocusModifier(Active)
            val parentModifier = createFocusModifier(Inactive)
            val focusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = grandParentModifier) {
                    Box(modifier = parentModifier) {
                        Box(modifier = focusModifier)
                    }
                }
            }

            // Act.
            focusModifier.focusNode.requestFocus(propagateFocus)

            // Assert.
            assertThat(grandParentModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(parentModifier.focusDetailedState).isEqualTo(ActiveParent)
            assertThat(focusModifier.focusDetailedState).isEqualTo(Active)
        }
    }
}