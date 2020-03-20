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
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.ActiveParent
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Disabled
import androidx.ui.focus.FocusDetailedState.Inactive
import com.google.common.truth.Truth
import org.junit.Test

@SmallTest
class CaptureFocusTest {

    private val focusNode = ModifiedFocusNode(InnerPlaceable(LayoutNode()))

    @Test
    fun active_captureFocus_changesStateToCaptured() {
        // Arrange.
        focusNode.focusState = Active

        // Act.
        val success = focusNode.captureFocus()

        // Assert.
        Truth.assertThat(success).isTrue()
        Truth.assertThat(focusNode.focusState).isEqualTo(Captured)
    }

    @Test
    fun activeParent_captureFocus_retainsStateAsActiveParent() {
        // Arrange.
        focusNode.focusState = ActiveParent

        // Act.
        val success = focusNode.captureFocus()

        // Assert.
        Truth.assertThat(success).isFalse()
        Truth.assertThat(focusNode.focusState).isEqualTo(ActiveParent)
    }

    @Test
    fun captured_captureFocus_retainsStateAsCaptured() {
        // Arrange.
        focusNode.focusState = Captured

        // Act.
        val success = focusNode.captureFocus()

        // Assert.
        Truth.assertThat(success).isTrue()
        Truth.assertThat(focusNode.focusState).isEqualTo(Captured)
    }

    @Test
    fun disabled_captureFocus_retainsStateAsDisabled() {
        // Arrange.
        focusNode.focusState = Disabled

        // Act.
        val success = focusNode.captureFocus()

        // Assert.
        Truth.assertThat(success).isFalse()
        Truth.assertThat(focusNode.focusState).isEqualTo(Disabled)
    }

    @Test
    fun inactive_captureFocus_retainsStateAsInactive() {
        // Arrange.
        focusNode.focusState = Inactive

        // Act.
        val success = focusNode.captureFocus()

        // Assert.
        Truth.assertThat(success).isFalse()
        Truth.assertThat(focusNode.focusState).isEqualTo(Inactive)
    }
}