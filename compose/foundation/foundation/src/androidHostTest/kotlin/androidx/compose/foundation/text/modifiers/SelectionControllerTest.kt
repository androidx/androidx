/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(JUnit4::class)
class SelectionControllerTest {

    private val selectableId = 1234L
    private val mockCoordinates: LayoutCoordinates = mock { on { isAttached } doReturn true }

    @Test
    fun updateInLookaheadTransition_true_notifiesSelectableChange() {
        val selectionRegistrar: SelectionRegistrar = mock()
        val controller = SelectionController(selectableId, selectionRegistrar, Color.Blue)

        controller.updateInLookaheadTransition(true)

        verify(selectionRegistrar).notifySelectableChange(selectableId)
        verify(selectionRegistrar, never()).notifyPositionChange(selectableId)
    }

    @Test
    fun updateInLookaheadTransition_true_masksLayoutCoordinatesToNull() {
        var subscribedSelectable: Selectable? = null
        val selectionRegistrar: SelectionRegistrar = mock {
            on { subscribe(any()) }
                .thenAnswer { invocation ->
                    val s = invocation.getArgument<Selectable>(0)
                    subscribedSelectable = s
                    s
                }
        }
        val controller = SelectionController(selectableId, selectionRegistrar, Color.Blue)
        controller.onPlaced()
        controller.updateLayoutCoordinates(mockCoordinates)

        assertThat(subscribedSelectable?.getLayoutCoordinates()).isEqualTo(mockCoordinates)

        // Enter transition
        controller.updateInLookaheadTransition(true)

        // Coordinates should be masked to null during approach morphing
        assertThat(subscribedSelectable?.getLayoutCoordinates()).isNull()
    }

    @Test
    fun updateInLookaheadTransition_false_restoresCoordinatesAndNotifiesPositionChange() {
        var subscribedSelectable: Selectable? = null
        val selectionRegistrar: SelectionRegistrar = mock {
            on { subscribe(any()) }
                .thenAnswer { invocation ->
                    val s = invocation.getArgument<Selectable>(0)
                    subscribedSelectable = s
                    s
                }
        }
        val controller = SelectionController(selectableId, selectionRegistrar, Color.Blue)
        controller.onPlaced()
        controller.updateLayoutCoordinates(mockCoordinates)

        // Start transition
        controller.updateInLookaheadTransition(true)
        assertThat(subscribedSelectable?.getLayoutCoordinates()).isNull()

        // Settle transition
        org.mockito.kotlin.clearInvocations(selectionRegistrar)
        controller.updateInLookaheadTransition(false)

        verify(selectionRegistrar).notifyPositionChange(selectableId)
        assertThat(subscribedSelectable?.getLayoutCoordinates()).isEqualTo(mockCoordinates)
    }

    @Test
    fun updateInLookaheadTransition_noChange_doesNotNotify() {
        val selectionRegistrar: SelectionRegistrar = mock()
        val controller = SelectionController(selectableId, selectionRegistrar, Color.Blue)

        // Initial state is false; updating with false should be a no-op
        controller.updateInLookaheadTransition(false)

        verify(selectionRegistrar, never()).notifySelectableChange(selectableId)
        verify(selectionRegistrar, never()).notifyPositionChange(selectableId)
    }
}
