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

package androidx.ui.core

import androidx.test.filters.SmallTest
import androidx.ui.focus.FocusDetailedState.Active
import androidx.ui.focus.FocusDetailedState.Captured
import androidx.ui.focus.FocusDetailedState.Inactive
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class FocusNodeTest {

    @Test
    fun captureReferenceToFocusNode() {
        // Arrange.
        val focusNode = FocusNode()
        val focusNodeRef = Ref<FocusNode>()

        // Act.
        focusNode.ref = focusNodeRef

        // Assert.
        assertThat(focusNodeRef.value).isEqualTo(focusNode)
    }

    @Test
    fun defaultFocusState() {
        // Arrange.
        val focusNode = FocusNode()

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Inactive)
    }

    @Test
    fun defaultLayoutCoordinates() {
        // Arrange.
        val focusNode = FocusNode()

        // Assert.
        assertThat(focusNode.layoutCoordinates).isNull()
    }

    @Test
    fun captureFocusfromActiveState() {
        // Arrange.
        val focusNode = FocusNode().apply { focusState = Active }

        // Act.
        focusNode.captureFocus()

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Captured)
    }

    @Test
    fun captureFocusfromNonActiveState() {
        // Arrange.
        val focusNode = FocusNode().apply { focusState = Inactive }

        // Act.
        focusNode.captureFocus()

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Inactive)
    }

    @Test
    fun freeFocusfromCapturedState() {
        // Arrange.
        val focusNode = FocusNode().apply { focusState = Captured }

        // Act.
        focusNode.freeFocus()

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(
            Active
        )
    }

    @Test
    fun freeFocusfromNonActiveState() {
        // Arrange.
        val focusNode = FocusNode().apply { focusState = Inactive }

        // Act.
        focusNode.freeFocus()

        // Assert.
        assertThat(focusNode.focusState).isEqualTo(Inactive)
    }
}