/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.ui.core.InnerPlaceable
import androidx.ui.core.LayoutNode
import androidx.ui.core.Owner
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Inactive

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock

@SmallTest
@RunWith(Parameterized::class)
class RequestFocusTest(val propagateFocus: Boolean) {
    lateinit var owner: Owner

    @Before
    fun setup() {
        owner = mock(Owner::class.java)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "propagateFocus = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun active_isUnchanged() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            focusState = Active
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Active)
    }

    @Test
    fun captured_isUnchanged() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            focusState = Captured
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Captured)
    }

    @Test
    fun disabled_isUnchanged() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            focusState = Disabled
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Disabled)
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeParent_withNoFocusedChild_throwsException() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            focusState = ActiveParent
        }

        // Act.
        focusNode.requestFocus(propagateFocus)
    }

    @Test
    fun activeParent_propagateFocus() {
        // Arrange.
        val child = createFocusNode().apply {
            focusState = Active
        }
        val focusNode = createFocusNode().apply {
            focusState = ActiveParent
            layoutNode.insertAt(0, child.layoutNode)
            layoutNode.layoutNodeWrapper = this
            focusedChild = child
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        when (propagateFocus) {
            true -> {
                // Unchanged.
                assertThat(focusNode.focusState).isEqualTo(ActiveParent)
                assertThat(child.focusState).isEqualTo(Active)
            }
            false -> {
                assertThat(focusNode.focusState).isEqualTo(Active)
                assertThat(focusNode.focusedChild).isNull()
                assertThat(child.focusState).isEqualTo(Inactive)
            }
        }
    }

    @Test
    fun inactive_root_propagateFocusSendsRequestToOwner_systemCannotGrantFocus() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(false)
        val rootFocusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Inactive
        }

        // Act.
        rootFocusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(rootFocusNode.focusState).isEqualTo(Inactive)
    }

    @Test
    fun inactiveRoot_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val rootFocusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Inactive
        }

        // Act.
        rootFocusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(rootFocusNode.focusState).isEqualTo(Active)
    }

    @Test
    fun inactiveRootWithChildren_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val child = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val rootFocusNode = createFocusNode().apply {
            layoutNode.insertAt(0, child.layoutNode)
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Inactive
        }

        // Act.
        rootFocusNode.requestFocus(propagateFocus)

        // Assert.
        when (propagateFocus) {
            true -> {
                // Unchanged.
                assertThat(rootFocusNode.focusState).isEqualTo(ActiveParent)
                assertThat(child.focusState).isEqualTo(Active)
            }
            false -> {
                assertThat(rootFocusNode.focusState).isEqualTo(Active)
                assertThat(child.focusState).isEqualTo(Inactive)
            }
        }
    }

    @Test
    fun inactiveNonRootWithChilcren() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val child = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, child.layoutNode)
            focusState = Inactive
        }
        val parent = createFocusNode().apply {
            layoutNode.insertAt(0, focusNode.layoutNode)
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Active
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        when (propagateFocus) {
            true -> {
                assertThat(parent.focusState).isEqualTo(ActiveParent)
                assertThat(focusNode.focusState).isEqualTo(ActiveParent)
                assertThat(child.focusState).isEqualTo(Active)
            }
            false -> {
                assertThat(parent.focusState).isEqualTo(ActiveParent)
                assertThat(focusNode.focusState).isEqualTo(Active)
                assertThat(child.focusState).isEqualTo(Inactive)
            }
        }
    }

    @Test
    fun rootNode() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val rootNode = createFocusNode().apply {
            layoutNode.attach(owner)
        }

        // Act.
        rootNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(rootNode.focusState).isEqualTo(Active)
    }

    @Test
    fun rootNodeWithChildren() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val childNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val rootNode = createFocusNode().apply {
            layoutNode.insertAt(0, childNode.layoutNode)
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
        }

        // Act.
        rootNode.requestFocus(propagateFocus)

        // Assert.
        when (propagateFocus) {
            true -> assertThat(rootNode.focusState).isEqualTo(ActiveParent)
            false -> assertThat(rootNode.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val childNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, childNode.layoutNode)
            focusState = Inactive
        }
        val grandparentNode = createFocusNode().apply {
            layoutNode.insertAt(0, parentNode.layoutNode)
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Inactive
        }

        // Act.
        parentNode.requestFocus(propagateFocus)

        // Assert.
        when (propagateFocus) {
            true -> assertThat(parentNode.focusState).isEqualTo(ActiveParent)
            false -> assertThat(parentNode.focusState).isEqualTo(Active)
        }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val childNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, childNode.layoutNode)
            focusState = Inactive
        }
        val grandparentNode = createFocusNode().apply {
            layoutNode.insertAt(0, parentNode.layoutNode)
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Inactive
        }

        // Act.
        childNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val childNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, childNode.layoutNode)
            focusState = Inactive
        }
        val grandparentNode = createFocusNode().apply {
            layoutNode.insertAt(0, parentNode.layoutNode)
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Inactive
        }

        // Act.
        childNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(childNode.focusState).isEqualTo(Active)
    }

    @Test
    fun requestFocus_parentIsFocused() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, focusNode.layoutNode)
            layoutNode.attach(owner)
            focusState = Active
        }

        // Verify Setup.
        assertThat(parentNode.focusState).isEqualTo(Active)
        assertThat(focusNode.focusState).isEqualTo(Inactive)

        // After executing requestFocus, siblingNode will be 'Active'.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Active)
    }

    @Test
    fun requestFocus_childIsFocused() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Active
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, focusNode.layoutNode)
            focusedChild = focusNode
            focusState = ActiveParent
        }

        // Verify Setup.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Active)

        // Act.
        parentNode.requestFocus(propagateFocus)

        // Assert.
        when (propagateFocus) {
            true -> {
                assertThat(parentNode.focusState).isEqualTo(ActiveParent)
                assertThat(focusNode.focusState).isEqualTo(Active)
            }
            false -> {
                assertThat(parentNode.focusState).isEqualTo(Active)
                assertThat(focusNode.focusState).isEqualTo(Inactive)
            }
        }
    }

    @Test
    fun requestFocus_childHasCapturedFocus() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Captured
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, focusNode.layoutNode)
            focusedChild = focusNode
            focusState = ActiveParent
        }

        // Verify Setup.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Captured)

        // Act.
        parentNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Captured)
    }

    @Test
    fun requestFocus_siblingIsFocused() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val siblingNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Active
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, focusNode.layoutNode)
            layoutNode.insertAt(1, siblingNode.layoutNode)
            layoutNode.attach(owner)
            focusedChild = siblingNode
            focusState = ActiveParent
        }

        // Verify Setup.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Inactive)
        assertThat(siblingNode.focusState).isEqualTo(Active)

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Active)
        assertThat(siblingNode.focusState).isEqualTo(Inactive)
    }

    @Test
    fun requestFocus_siblingHasCapturedFocused() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val siblingNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Captured
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, focusNode.layoutNode)
            layoutNode.insertAt(1, siblingNode.layoutNode)
            layoutNode.attach(owner)
            focusedChild = siblingNode
            focusState = ActiveParent
        }

        // Verify Setup.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Inactive)
        assertThat(siblingNode.focusState).isEqualTo(Captured)

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Inactive)
        assertThat(siblingNode.focusState).isEqualTo(Captured)
    }

    @Test
    fun requestFocus_cousinIsFocused() {
        // Arrange.
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }

        val cousinNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Active
        }

        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, focusNode.layoutNode)
            focusState = Inactive
        }
        val auntNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, cousinNode.layoutNode)
            focusedChild = cousinNode
            focusState = ActiveParent
        }

        val grandparentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, parentNode.layoutNode)
            layoutNode.insertAt(1, auntNode.layoutNode)
            layoutNode.attach(owner)
            focusedChild = auntNode
            focusState = ActiveParent
        }

        // Verify Setup.
        assertThat(cousinNode.focusState).isEqualTo(Active)
        assertThat(focusNode.focusState).isEqualTo(Inactive)

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(cousinNode.focusState).isEqualTo(Inactive)
        assertThat(focusNode.focusState).isEqualTo(Active)
    }

    @Test
    fun requestFocus_grandParentIsFocused() {
        // Arrange.
        whenever(owner.requestFocus()).thenReturn(true)
        val focusNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            focusState = Inactive
        }
        val parentNode = createFocusNode().apply {
            layoutNode.layoutNodeWrapper = this
            layoutNode.insertAt(0, focusNode.layoutNode)
            focusState = Inactive
        }
        val grandparentNode = createFocusNode().apply {
            layoutNode.insertAt(0, parentNode.layoutNode)
            layoutNode.layoutNodeWrapper = this
            layoutNode.attach(owner)
            focusState = Active
        }

        // Verify Setup.
        assertThat(grandparentNode.focusState).isEqualTo(Active)
        assertThat(parentNode.focusState).isEqualTo(Inactive)
        assertThat(focusNode.focusState).isEqualTo(Inactive)

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(grandparentNode.focusState).isEqualTo(ActiveParent)
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
        assertThat(focusNode.focusState).isEqualTo(Active)
    }

    private fun createFocusNode() = ModifiedFocusNode(InnerPlaceable(LayoutNode()))
}