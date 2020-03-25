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
import androidx.ui.core.InnerPlaceable
import androidx.ui.core.LayoutNode
import androidx.ui.focus.FocusDetailedState
import com.google.common.truth.Truth
import org.junit.Test

@SmallTest
class FreeFocusTest {

    private val focusNode = ModifiedFocusNode(InnerPlaceable(LayoutNode()))

    @Test
    fun active_freeFocus_retainFocusAsActive() {
        // Arrange.
        focusNode.focusState = FocusDetailedState.Active

        // Act.
        val success = focusNode.freeFocus()

        // Assert.
        Truth.assertThat(success).isFalse()
        Truth.assertThat(focusNode.focusState).isEqualTo(FocusDetailedState.Active)
    }

    @Test
    fun activeParent_freeFocus_retainFocusAsActiveParent() {
        // Arrange.
        focusNode.focusState = FocusDetailedState.ActiveParent

        // Act.
        val success = focusNode.freeFocus()

        // Assert.
        Truth.assertThat(success).isFalse()
        Truth.assertThat(focusNode.focusState).isEqualTo(FocusDetailedState.ActiveParent)
    }

    @Test
    fun captured_freeFocus_changesStateToActive() {
        // Arrange.
        focusNode.focusState = FocusDetailedState.Captured

        // Act.
        val success = focusNode.freeFocus()

        // Assert.
        Truth.assertThat(success).isTrue()
        Truth.assertThat(focusNode.focusState).isEqualTo(FocusDetailedState.Active)
    }

    @Test
    fun disabled_freeFocus_retainFocusAsDisabled() {
        // Arrange.
        focusNode.focusState = FocusDetailedState.Disabled

        // Act.
        val success = focusNode.freeFocus()

        // Assert.
        Truth.assertThat(success).isFalse()
        Truth.assertThat(focusNode.focusState).isEqualTo(FocusDetailedState.Disabled)
    }

    @Test
    fun inactive_freeFocus_retainFocusAsInactive() {
        // Arrange.
        focusNode.focusState = FocusDetailedState.Inactive

        // Act.
        val success = focusNode.freeFocus()

        // Assert.
        Truth.assertThat(success).isFalse()
        Truth.assertThat(focusNode.focusState).isEqualTo(FocusDetailedState.Inactive)
    }
}