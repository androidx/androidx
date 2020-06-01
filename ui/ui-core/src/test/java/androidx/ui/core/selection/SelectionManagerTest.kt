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

import androidx.compose.frames.commit
import androidx.compose.frames.open
import androidx.test.filters.SmallTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedbackType
import androidx.ui.core.texttoolbar.TextToolbar
import androidx.ui.geometry.Rect
import androidx.ui.text.AnnotatedString
import androidx.ui.text.length
import androidx.ui.text.style.TextDirection
import androidx.ui.text.subSequence
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
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

    private val containerLayoutCoordinates = mock<LayoutCoordinates> {
        on { isAttached } doReturn true
    }
    private val startSelectable = mock<Selectable>()
    private val endSelectable = mock<Selectable>()
    private val middleSelectable = mock<Selectable>()
    private val lastSelectable = mock<Selectable>()

    private val startCoordinates = PxPosition(3f, 30f)
    private val endCoordinates = PxPosition(3f, 600f)

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
    private val clipboardManager = mock<ClipboardManager>()
    private val textToolbar = mock<TextToolbar>()

    @Before
    fun setup() {
        open(false) // we open a Frame so state reads are allowed
        selectionRegistrar.subscribe(selectable)
        selectionManager.containerLayoutCoordinates = containerLayoutCoordinates
        selectionManager.hapticFeedBack = hapticFeedback
        selectionManager.clipboardManager = clipboardManager
        selectionManager.textToolbar = textToolbar
    }

    @After
    fun after() {
        commit() // we close the Frame
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
                containerLayoutCoordinates = selectionManager.requireContainerCoordinates(),
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
                containerLayoutCoordinates = selectionManager.requireContainerCoordinates(),
                longPress = false,
                previousSelection = fakeSelection
            )
        verify(selectable_another, times(1))
            .getSelection(
                startPosition = startCoordinates,
                endPosition = endCoordinates,
                containerLayoutCoordinates = selectionManager.requireContainerCoordinates(),
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
    fun getSelectedText_selection_null_return_null() {
        selectionManager.selection = null

        assertThat(selectionManager.getSelectedText()).isNull()
        verify(selectable, times(0)).getText()
    }

    @Test
    fun getSelectedText_not_crossed_single_widget() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('e')
        val endOffset = text.indexOf('m')
        whenever(selectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = startOffset,
                selectable = selectable),
            end = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = false
        )

        assertThat(selectionManager.getSelectedText())
            .isEqualTo(annotatedString.subSequence(startOffset, endOffset))
        verify(selectable, times(1)).getText()
    }

    @Test
    fun getSelectedText_crossed_single_widget() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        whenever(selectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = startOffset,
                selectable = selectable),
            end = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = true
        )

        assertThat(selectionManager.getSelectedText())
            .isEqualTo(annotatedString.subSequence(endOffset, startOffset))
        verify(selectable, times(1)).getText()
    }

    @Test
    fun getSelectedText_not_crossed_multi_widgets() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')

        selectionRegistrar.subscribe(startSelectable)
        selectionRegistrar.subscribe(middleSelectable)
        selectionRegistrar.subscribe(endSelectable)
        selectionRegistrar.subscribe(lastSelectable)
        selectionRegistrar.sorted = true
        whenever(startSelectable.getText()).thenReturn(annotatedString)
        whenever(middleSelectable.getText()).thenReturn(annotatedString)
        whenever(endSelectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = startOffset,
                selectable = startSelectable),
            end = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = endOffset,
                selectable = endSelectable
            ),
            handlesCrossed = false
        )

        val result = annotatedString.subSequence(startOffset, annotatedString.length) +
                annotatedString + annotatedString.subSequence(0, endOffset)
        assertThat(selectionManager.getSelectedText()).isEqualTo(result)
        verify(selectable, times(0)).getText()
        verify(startSelectable, times(1)).getText()
        verify(middleSelectable, times(1)).getText()
        verify(endSelectable, times(1)).getText()
        verify(lastSelectable, times(0)).getText()
    }

    @Test
    fun getSelectedText_crossed_multi_widgets() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')

        selectionRegistrar.subscribe(endSelectable)
        selectionRegistrar.subscribe(middleSelectable)
        selectionRegistrar.subscribe(startSelectable)
        selectionRegistrar.subscribe(lastSelectable)
        selectionRegistrar.sorted = true
        whenever(startSelectable.getText()).thenReturn(annotatedString)
        whenever(middleSelectable.getText()).thenReturn(annotatedString)
        whenever(endSelectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = startOffset,
                selectable = startSelectable),
            end = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = endOffset,
                selectable = endSelectable
            ),
            handlesCrossed = true
        )

        val result = annotatedString.subSequence(endOffset, annotatedString.length) +
                annotatedString + annotatedString.subSequence(0, startOffset)
        assertThat(selectionManager.getSelectedText()).isEqualTo(result)
        verify(selectable, times(0)).getText()
        verify(startSelectable, times(1)).getText()
        verify(middleSelectable, times(1)).getText()
        verify(endSelectable, times(1)).getText()
        verify(lastSelectable, times(0)).getText()
    }

    @Test
    fun copy_selection_null_not_trigger_clipboardmanager() {
        selectionManager.selection = null

        selectionManager.copy()

        verify(clipboardManager, times(0)).setText(any())
    }

    @Test
    fun copy_selection_not_null_trigger_clipboardmanager_setText() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        whenever(selectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = startOffset,
                selectable = selectable),
            end = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = true
        )

        selectionManager.copy()

        verify(clipboardManager, times(1)).setText(
            annotatedString.subSequence(
                endOffset,
                startOffset
            )
        )
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showCopyMenu() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        whenever(selectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = startOffset,
                selectable = selectable),
            end = Selection.AnchorInfo(
                direction = TextDirection.Ltr,
                offset = endOffset,
                selectable = selectable
            ),
            handlesCrossed = true
        )

        selectionManager.showSelectionToolbar()

        verify(textToolbar, times(1)).showCopyMenu(
            eq(Rect.zero),
            any(),
            any()
        )
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
                startPosition = PxPosition(-1f, -1f),
                endPosition = PxPosition(-1f, -1f),
                containerLayoutCoordinates = selectionManager.requireContainerCoordinates(),
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
