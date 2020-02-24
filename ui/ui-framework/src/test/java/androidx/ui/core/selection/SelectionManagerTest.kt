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

package androidx.ui.core.selection

import androidx.test.filters.SmallTest
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedbackType
import androidx.ui.core.LayoutCoordinates
import androidx.ui.text.style.TextDirection
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SelectionManagerTest {
    private val selectionRegistrar = spy(SelectionRegistrarImpl())
    private val selectable = mock<Selectable>()
    private val selectionManager = SelectionManager(selectionRegistrar)

    private val containerLayoutCoordinates = mock<LayoutCoordinates>()
    private val startSelectable = mock<Selectable>()
    private val endSelectable = mock<Selectable>()

    private val startCoordinates = PxPosition(3.px, 30.px)
    private val endCoordinates = PxPosition(3.px, 600.px)

    private val fakeSelection =
        Selection(
            start = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = 0,
                selectable = startSelectable
            ),
            end = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = 5,
                selectable = endSelectable
            )
        )

    private val hapticFeedback = mock<HapticFeedback>()

    @Before
    fun setup() {
        selectionRegistrar.subscribe(selectable)
        selectionManager.containerLayoutCoordinates = containerLayoutCoordinates
        selectionManager.hapticFeedBack = hapticFeedback
    }

    @Test
    fun mergeSelections_sorting() {
        whenever((containerLayoutCoordinates.childToLocal(any(), any())))
            .thenReturn(PxPosition.Origin)

        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates
        )

        verify(selectionRegistrar, times(1)).sort(containerLayoutCoordinates)
    }

    @Test
    fun mergeSelections_single_selectable_calls_getSelection_once() {
        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates,
            previousSelection = fakeSelection
        )

        val fakeNewSelection = mock<Selection>()

        whenever(selectable.getSelection(any(), any(), any(), any(), any(), any()))
            .thenReturn(fakeNewSelection)

        verify(selectable, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.containerLayoutCoordinates,
                longPress = false,
                previousSelection = fakeSelection
            )
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun mergeSelections_multiple_selectables_calls_getSelection_multiple_times() {
        val selectable_another = mock<Selectable>()
        selectionRegistrar.subscribe(selectable_another)
        whenever((containerLayoutCoordinates.childToLocal(any(), any())))
            .thenReturn(PxPosition.Origin)

        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates,
            previousSelection = fakeSelection
        )

        verify(selectable, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.containerLayoutCoordinates,
                longPress = false,
                previousSelection = fakeSelection
            )
        verify(selectable_another, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.containerLayoutCoordinates,
                longPress = false,
                previousSelection = fakeSelection
            )
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun mergeSelections_selection_does_not_change_hapticFeedBack_Not_triggered() {
        val selection: Selection? = mock()
        whenever(selectable.getSelection(any(), any(), any(), any(), any(), any()))
            .thenReturn(selection)

        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates,
            previousSelection = selection
        )

        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun cancel_selection_calls_getSelection_selection_becomes_null() {
        val fakeSelection =
            Selection(
                start = Selection.AnchorInfo(
                    direction = TextDirection.Ltr,
                    offset = 0,
                    selectable = startSelectable
                ),
                end = Selection.AnchorInfo(
                    direction = TextDirection.Ltr,
                    offset = 5,
                    selectable = endSelectable
                )
            )
        var selection: Selection? = fakeSelection
        val lambda: (Selection?) -> Unit = { selection = it }
        val spyLambda = spy(lambda)
        selectionManager.onSelectionChange = spyLambda
        selectionManager.selection = fakeSelection

        selectionManager.onRelease()

        verify(selectable, times(1))
            .getSelection(
                startPosition = PxPosition((-1).px, (-1).px),
                endPosition = PxPosition((-1).px, (-1).px),
                containerLayoutCoordinates = selectionManager.containerLayoutCoordinates,
                longPress = false,
                previousSelection = fakeSelection
            )
        assertThat(selection).isNull()
        verify(spyLambda, times(1)).invoke(null)
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
