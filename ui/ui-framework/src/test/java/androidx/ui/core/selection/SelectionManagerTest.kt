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
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxPosition
import androidx.ui.core.px
import androidx.ui.text.style.TextDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SelectionManagerTest {
    private val selectionRegistrar = SelectionRegistrarImpl()
    private val selectable = mock<Selectable>()
    private val selectionManager = SelectionManager(selectionRegistrar)

    private val startLayoutCoordinates = mock<LayoutCoordinates>()
    private val endLayoutCoordinates = mock<LayoutCoordinates>()
    private val startCoordinates = PxPosition(3.px, 30.px)
    private val endCoordinates = PxPosition(3.px, 600.px)

    @Before
    fun setup() {
        val containerLayoutCoordinates = mock<LayoutCoordinates>()
        selectionRegistrar.subscribe(selectable)
        selectionManager.containerLayoutCoordinates = containerLayoutCoordinates
    }

    @Test
    fun mergeSelections_single_selectable_calls_getSelection_once() {
        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates
        )

        verify(selectable, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.containerLayoutCoordinates,
                longPress = false
            )
    }

    @Test
    fun mergeSelections_multiple_selectables_calls_getSelection_multiple_times() {
        val selectable_another = mock<Selectable>()
        selectionRegistrar.subscribe(selectable_another)

        selectionManager.mergeSelections(
            startPosition = startCoordinates,
            endPosition = endCoordinates
        )

        verify(selectable, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.containerLayoutCoordinates,
                longPress = false
            )
        verify(selectable_another, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.containerLayoutCoordinates,
                longPress = false
            )
    }

    @Test
    fun cancel_selection_calls_getSelection_selection_becomes_null() {
        val fakeSelection =
            Selection(
                start = Selection.AnchorInfo(
                    coordinates = startCoordinates,
                    direction = TextDirection.Ltr,
                    offset = 0,
                    layoutCoordinates = startLayoutCoordinates
                ),
                end = Selection.AnchorInfo(
                    coordinates = endCoordinates,
                    direction = TextDirection.Ltr,
                    offset = 5,
                    layoutCoordinates = endLayoutCoordinates
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
                longPress = false
            )
        assertThat(selection).isNull()
        verify(spyLambda, times(1)).invoke(null)
    }
}
