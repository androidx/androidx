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
import androidx.ui.core.FocusNode
import androidx.ui.core.Owner
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Inactive

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock

@SmallTest
@RunWith(Parameterized::class)
class RequestFocusTest(val propagateFocus: Boolean) {
    lateinit var host: Owner

    @Before
    fun setup() {
        host = mock(Owner::class.java)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "propagateFocus = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Test
    fun activeComponent() {
        // Arrange.
        val focusNode = FocusNode().apply {
            focusState = Active
            recompose = {}
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Active)
    }

    @Test
    fun capturedComponent() {
        // Arrange.
        val focusNode = FocusNode().apply {
            focusState = Captured
            recompose = {}
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Captured)
    }

    @Test
    fun disabledComponent() {
        // Arrange.
        val focusNode = FocusNode().apply {
            focusState = Disabled
            recompose = {}
        }

        // Act.
        focusNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Disabled)
    }

    @Test
    fun rootNode() {
        // Arrange.
        val rootNode = FocusNode().apply { recompose = {} }

        // Act.
        rootNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(rootNode.focusState).isEqualTo(Active)
    }

    @Test
    fun rootNodeWithChildren() {
        // Arrange.
        val childNode = FocusNode().apply { recompose = {} }
        val rootNode = FocusNode().apply {
            recompose = {}
            attach(host)
            insertAt(0, childNode)
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
        val childNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply { recompose = {} }
        val grandparentNode = FocusNode().apply {
            recompose = {}
            attach(host)
            insertAt(0, parentNode)
        }
        parentNode.insertAt(0, childNode)

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
        val childNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply { recompose = {} }
        val grandparentNode = FocusNode().apply {
            recompose = {}
            attach(host)
            insertAt(0, parentNode)
        }
        parentNode.insertAt(0, childNode)

        // Act.
        childNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(parentNode.focusState).isEqualTo(ActiveParent)
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        // Arrange.
        val childNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply { recompose = {} }
        val grandparentNode = FocusNode().apply {
            recompose = {}
            attach(host)
            insertAt(0, parentNode)
        }
        parentNode.insertAt(0, childNode)

        // Act.
        childNode.requestFocus(propagateFocus)

        // Assert.
        assertThat(childNode.focusState).isEqualTo(Active)
    }

    @Test
    fun requestFocus_parentIsFocused() {
        // Arrange.
        val focusNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply {
            attach(host)
            focusState = Active
            recompose = {}
            insertAt(0, focusNode)
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
        val focusNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply {
            attach(host)
            recompose = {}
            insertAt(0, focusNode)
        }
        focusNode.requestFocus(propagateFocus)

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
        val focusNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply {
            attach(host)
            recompose = {}
            insertAt(0, focusNode)
        }
        focusNode.apply {
            requestFocus(propagateFocus)
            captureFocus()
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
        val focusNode = FocusNode().apply { recompose = {} }
        val siblingNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply {
            recompose = {}
            focusState = Active
            attach(host)
            insertAt(0, focusNode)
            insertAt(1, siblingNode)
        }
        // After executing requestFocus, siblingNode will be 'Active'.
        siblingNode.requestFocus(propagateFocus)

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
        val focusNode = FocusNode().apply { recompose = {} }
        val siblingNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply {
            recompose = {}
            focusState = Active
            attach(host)
            insertAt(0, focusNode)
            insertAt(1, siblingNode)
        }
        // After executing requestFocus, siblingNode will be 'Active'.
        siblingNode.apply {
            requestFocus(propagateFocus)
            captureFocus()
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
        val focusNode = FocusNode().apply {
            focusState = Inactive
            recompose = {}
        }

        val cousinNode = FocusNode().apply {
            focusState = Inactive
            recompose = {}
        }

        val parentNode = FocusNode().apply {
            focusState = Inactive
            recompose = {}
        }
        val auntNode = FocusNode().apply {
            focusState = Inactive
            recompose = {}
        }

        val grandparentNode = FocusNode().apply {
            focusState = Active
            recompose = {}
        }

        grandparentNode.apply {
            attach(host)
            insertAt(0, parentNode)
            insertAt(1, auntNode)
        }

        parentNode.insertAt(0, focusNode)
        auntNode.insertAt(0, cousinNode)
        cousinNode.requestFocus(propagateFocus)

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
        val focusNode = FocusNode().apply { recompose = {} }
        val parentNode = FocusNode().apply { recompose = {} }
        val grandparentNode = FocusNode().apply { recompose = {} }

        grandparentNode.apply {
            attach(host)
            insertAt(0, parentNode)
            focusState = Active
        }
        parentNode.insertAt(0, focusNode)

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
}