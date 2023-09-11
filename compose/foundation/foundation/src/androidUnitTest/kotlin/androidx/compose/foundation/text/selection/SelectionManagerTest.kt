/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class SelectionManagerTest {
    private val selectionRegistrar = spy(SelectionRegistrarImpl())
    private val selectable = FakeSelectable()
    private val selectableId = 1L
    private val selectionManager = SelectionManager(selectionRegistrar)
    private var onSelectionChangeCalledTimes = 0

    private val containerLayoutCoordinates = MockCoordinates()

    private val startSelectableId = 2L
    private val startSelectable = mock<Selectable> {
        whenever(it.selectableId).thenReturn(startSelectableId)
    }

    private val endSelectableId = 3L
    private val endSelectable = mock<Selectable> {
        whenever(it.selectableId).thenReturn(endSelectableId)
    }

    private val middleSelectableId = 4L
    private val middleSelectable = mock<Selectable> {
        whenever(it.selectableId).thenReturn(middleSelectableId)
    }

    private val lastSelectableId = 5L
    private val lastSelectable = mock<Selectable> {
        whenever(it.selectableId).thenReturn(lastSelectableId)
    }

    private val fakeSelection =
        Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 0,
                selectableId = startSelectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = 5,
                selectableId = endSelectableId
            )
        )

    private val hapticFeedback = mock<HapticFeedback>()
    private val clipboardManager = mock<ClipboardManager>()
    private val textToolbar = mock<TextToolbar>()

    @Before
    fun setup() {
        selectable.clear()
        selectable.selectableId = selectableId
        selectionRegistrar.subscribe(selectable)
        selectionRegistrar.subselections = mapOf(
            selectableId to fakeSelection,
            startSelectableId to fakeSelection,
            endSelectableId to fakeSelection
        )
        selectionManager.containerLayoutCoordinates = containerLayoutCoordinates
        selectionManager.hapticFeedBack = hapticFeedback
        selectionManager.clipboardManager = clipboardManager
        selectionManager.textToolbar = textToolbar
        selectionManager.selection = fakeSelection
        selectionManager.onSelectionChange = { onSelectionChangeCalledTimes++ }
    }

    @Test
    fun updateSelection_onInitial_returnsTrue() {
        val startHandlePosition = Offset(x = 5f, y = 5f)
        val endHandlePosition = Offset(x = 25f, y = 5f)
        selectable.apply {
            textToReturn = AnnotatedString("hello")
            rawStartHandleOffset = 0
            rawEndHandleOffset = 5
        }

        val actual = selectionManager.updateSelection(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = endHandlePosition - Offset(x = 5f, y = 0f),
            isStartHandle = false,
            adjustment = SelectionAdjustment.None,
        )

        assertThat(actual).isTrue()
        assertThat(onSelectionChangeCalledTimes).isEqualTo(1)
    }

    @Test
    fun updateSelection_onNoChange_returnsFalse() {
        val startHandlePosition = Offset(x = 5f, y = 5f)
        val endHandlePosition = Offset(x = 25f, y = 5f)
        selectable.apply {
            textToReturn = AnnotatedString("hello")
            rawStartHandleOffset = 0
            rawEndHandleOffset = 5
            rawPreviousHandleOffset = 5
        }

        // run once to set context for the "previous" selection update
        selectionManager.updateSelection(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = endHandlePosition,
            isStartHandle = false,
            adjustment = SelectionAdjustment.None,
        )

        // run again since we are testing the "no changes" case
        val actual = selectionManager.updateSelection(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = endHandlePosition,
            isStartHandle = false,
            adjustment = SelectionAdjustment.None,
        )

        assertThat(actual).isFalse()
        assertThat(onSelectionChangeCalledTimes).isEqualTo(1)
    }

    @Test
    fun updateSelection_onChange_returnsTrue() {
        val startHandlePosition = Offset(x = 5f, y = 5f)
        val endHandlePosition = Offset(x = 25f, y = 5f)
        selectable.apply {
            textToReturn = AnnotatedString("hello")
            rawStartHandleOffset = 0
            rawEndHandleOffset = 5
            rawPreviousHandleOffset = 5
        }

        // run once to set context for the "previous" selection update
        selectionManager.updateSelection(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = endHandlePosition,
            isStartHandle = false,
            adjustment = SelectionAdjustment.None,
        )

        // run again with a change in end handle
        selectable.rawEndHandleOffset = 4
        val actual = selectionManager.updateSelection(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = endHandlePosition - Offset(x = 5f, y = 0f),
            isStartHandle = false,
            adjustment = SelectionAdjustment.None,
        )

        assertThat(actual).isTrue()
        assertThat(onSelectionChangeCalledTimes).isEqualTo(2)
    }

    @Test
    fun shouldPerformHaptics_notInTouchMode_returnsFalse() {
        selectionManager.isInTouchMode = false
        selectable.textToReturn = AnnotatedString("hello")
        val actual = selectionManager.shouldPerformHaptics()
        assertThat(actual).isFalse()
    }

    @Test
    fun shouldPerformHaptics_allEmptyTextSelectables_returnsFalse() {
        selectionManager.isInTouchMode = true
        selectable.textToReturn = AnnotatedString("")
        val actual = selectionManager.shouldPerformHaptics()
        assertThat(actual).isFalse()
    }

    @Test
    fun shouldPerformHaptics_inTouchModeAndNonEmpty_returnsTrue() {
        selectionManager.isInTouchMode = true
        selectable.textToReturn = AnnotatedString("hello")
        val actual = selectionManager.shouldPerformHaptics()
        assertThat(actual).isTrue()
    }

    @Test
    fun mergeSelections_selectAll() {
        val anotherSelectableId = 100L
        val selectableAnother = mock<Selectable>()
        whenever(selectableAnother.selectableId).thenReturn(anotherSelectableId)

        selectionRegistrar.subscribe(selectableAnother)

        selectionManager.selectAll(
            selectableId = selectableId,
            previousSelection = fakeSelection
        )

        verify(selectableAnother, times(0)).getSelectAllSelection()
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun isNonEmptySelection_whenNonEmptySelection_sameLine_returnsTrue() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text)
        val startOffset = text.indexOf('e')
        val endOffset = text.indexOf('m')
        selectable.textToReturn = annotatedString
        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        selectionManager.selection = selection
        selectionRegistrar.subselections = mapOf(selectableId to selection)

        assertThat(selectionManager.isNonEmptySelection()).isTrue()
    }

    @Test
    fun isNonEmptySelection_whenEmptySelection_sameLine_returnsFalse() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text)
        val startOffset = text.indexOf('e')
        selectable.textToReturn = annotatedString
        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        selectionManager.selection = selection
        selectionRegistrar.subselections = mapOf(selectableId to selection)

        assertThat(selectionManager.isNonEmptySelection()).isFalse()
    }

    @Test
    fun isNonEmptySelection_whenNonEmptySelection_multiLine_returnsTrue() {
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
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = startSelectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = endSelectableId
            ),
            handlesCrossed = true
        )

        selectionRegistrar.subselections = mapOf(
            endSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = endSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = endOffset,
                    selectableId = endSelectableId
                ),
                handlesCrossed = true
            ),
            middleSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = middleSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = middleSelectableId
                ),
                handlesCrossed = true
            ),
            startSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = startOffset,
                    selectableId = startSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = startSelectableId
                ),
                handlesCrossed = true
            ),
        )

        assertThat(selectionManager.isNonEmptySelection()).isTrue()
    }

    @Test
    fun isNonEmptySelection_whenEmptySelection_multiLine_returnsFalse() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text)
        val startOffset = text.length
        val endOffset = 0

        selectionRegistrar.subscribe(startSelectable)
        selectionRegistrar.subscribe(middleSelectable)
        selectionRegistrar.subscribe(endSelectable)
        selectionRegistrar.subscribe(lastSelectable)
        selectionRegistrar.sorted = true
        whenever(startSelectable.getText()).thenReturn(annotatedString)
        whenever(middleSelectable.getText()).thenReturn(AnnotatedString(""))
        whenever(endSelectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = startSelectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = endSelectableId
            ),
            handlesCrossed = false
        )

        selectionRegistrar.subselections = mapOf(
            startSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = startSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = startSelectableId
                ),
                handlesCrossed = false
            ),
            middleSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = middleSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = middleSelectableId
                ),
                handlesCrossed = false
            ),
            endSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = endSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = endSelectableId
                ),
                handlesCrossed = false
            ),
        )

        assertThat(selectionManager.isNonEmptySelection()).isFalse()
    }

    @Test
    fun isNonEmptySelection_whenEmptySelection_multiLineCrossed_returnsFalse() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text)
        val startOffset = 0
        val endOffset = text.length

        selectionRegistrar.subscribe(endSelectable)
        selectionRegistrar.subscribe(middleSelectable)
        selectionRegistrar.subscribe(startSelectable)
        selectionRegistrar.subscribe(lastSelectable)
        selectionRegistrar.sorted = true
        whenever(startSelectable.getText()).thenReturn(annotatedString)
        whenever(middleSelectable.getText()).thenReturn(AnnotatedString(""))
        whenever(endSelectable.getText()).thenReturn(annotatedString)
        selectionManager.selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = startSelectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = endSelectableId
            ),
            handlesCrossed = true
        )

        selectionRegistrar.subselections = mapOf(
            startSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = startSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = startSelectableId
                ),
                handlesCrossed = true
            ),
            middleSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = middleSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = middleSelectableId
                ),
                handlesCrossed = true
            ),
            endSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = endSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = endSelectableId
                ),
                handlesCrossed = true
            ),
        )

        assertThat(selectionManager.isNonEmptySelection()).isFalse()
    }

    @Test
    fun getSelectedText_selection_null_return_null() {
        selectionManager.selection = null
        selectionRegistrar.subselections = emptyMap()

        assertThat(selectionManager.getSelectedText()).isNull()
        assertThat(selectable.getTextCalledTimes).isEqualTo(0)
    }

    @Test
    fun getSelectedText_not_crossed_single_widget() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('e')
        val endOffset = text.indexOf('m')
        selectable.textToReturn = annotatedString
        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )
        selectionManager.selection = selection
        selectionRegistrar.subselections = mapOf(selectableId to selection)

        assertThat(selectionManager.getSelectedText())
            .isEqualTo(annotatedString.subSequence(startOffset, endOffset))
        assertThat(selectable.getTextCalledTimes).isEqualTo(1)
    }

    @Test
    fun getSelectedText_crossed_single_widget() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        selectionManager.selection = selection
        selectionRegistrar.subselections = mapOf(selectableId to selection)

        assertThat(selectionManager.getSelectedText())
            .isEqualTo(annotatedString.subSequence(endOffset, startOffset))
        assertThat(selectable.getTextCalledTimes).isEqualTo(1)
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
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = startSelectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = endSelectableId
            ),
            handlesCrossed = false
        )

        selectionRegistrar.subselections = mapOf(
            startSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = startOffset,
                    selectableId = startSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = startSelectableId
                ),
                handlesCrossed = false
            ),
            middleSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = middleSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = middleSelectableId
                ),
                handlesCrossed = false
            ),
            endSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = endSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = endOffset,
                    selectableId = endSelectableId
                ),
                handlesCrossed = false
            ),
        )

        val result = annotatedString.subSequence(startOffset, annotatedString.length) +
            annotatedString + annotatedString.subSequence(0, endOffset)
        assertThat(selectionManager.getSelectedText()).isEqualTo(result)
        assertThat(selectable.getTextCalledTimes).isEqualTo(0)
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
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = startSelectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = endSelectableId
            ),
            handlesCrossed = true
        )

        selectionRegistrar.subselections = mapOf(
            endSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = endSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = endOffset,
                    selectableId = endSelectableId
                ),
                handlesCrossed = true
            ),
            middleSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = annotatedString.length,
                    selectableId = middleSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = middleSelectableId
                ),
                handlesCrossed = true
            ),
            startSelectableId to Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = startOffset,
                    selectableId = startSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = startSelectableId
                ),
                handlesCrossed = true
            ),
        )

        val result = annotatedString.subSequence(endOffset, annotatedString.length) +
            annotatedString + annotatedString.subSequence(0, startOffset)
        assertThat(selectionManager.getSelectedText()).isEqualTo(result)
        assertThat(selectable.getTextCalledTimes).isEqualTo(0)
        verify(startSelectable, times(1)).getText()
        verify(middleSelectable, times(1)).getText()
        verify(endSelectable, times(1)).getText()
        verify(lastSelectable, times(0)).getText()
    }

    @Test
    fun copy_selection_null_not_trigger_clipboardManager() {
        selectionManager.selection = null
        selectionRegistrar.subselections = emptyMap()

        selectionManager.copy()

        verify(clipboardManager, times(0)).setText(any())
    }

    @Test
    fun copy_selection_not_null_trigger_clipboardManager_setText() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        selectionManager.selection = selection
        selectionRegistrar.subselections = mapOf(selectableId to selection)

        selectionManager.copy()

        verify(clipboardManager, times(1)).setText(
            annotatedString.subSequence(
                endOffset,
                startOffset
            )
        )
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        selectable.layoutCoordinatesToReturn = containerLayoutCoordinates
        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        selectionManager.selection = selection
        selectionRegistrar.subselections = mapOf(selectableId to selection)
        selectionManager.hasFocus = true

        selectionManager.showToolbar = true

        verify(textToolbar, times(1)).showMenu(
            any(),
            any(),
            isNull(),
            isNull(),
            isNull()
        )
    }

    @Test
    fun showSelectionToolbar_withoutFocus_notTrigger_textToolbar_showMenu() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        val selection = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = startOffset,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = endOffset,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )
        selectionManager.selection = selection
        selectionRegistrar.subselections = mapOf(selectableId to selection)
        selectionManager.hasFocus = false

        selectionManager.showToolbar = true

        verify(textToolbar, never()).showMenu(
            any(),
            any(),
            isNull(),
            isNull(),
            isNull()
        )
    }

    @Test
    fun onRelease_selectionMap_is_setToEmpty() {
        val fakeSelection =
            Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = startSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 5,
                    selectableId = endSelectableId
                )
            )
        var selection: Selection? = fakeSelection
        val lambda: (Selection?) -> Unit = { selection = it }
        val spyLambda = spy(lambda)
        selectionManager.onSelectionChange = spyLambda
        selectionManager.selection = fakeSelection

        selectionManager.onRelease()

        verify(selectionRegistrar).subselections = emptyMap()

        assertThat(selection).isNull()
        verify(spyLambda, times(1)).invoke(null)
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun notifySelectableChange_clears_selection() {
        val fakeSelection =
            Selection(
                start = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = startSelectableId
                ),
                end = Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 5,
                    selectableId = startSelectableId
                )
            )
        var selection: Selection? = fakeSelection
        val lambda: (Selection?) -> Unit = { selection = it }
        val spyLambda = spy(lambda)
        selectionManager.onSelectionChange = spyLambda
        selectionManager.selection = fakeSelection

        selectionRegistrar.subselections = mapOf(
            startSelectableId to fakeSelection
        )
        selectionRegistrar.notifySelectableChange(startSelectableId)

        verify(selectionRegistrar).subselections = emptyMap()
        assertThat(selection).isNull()
        verify(spyLambda, times(1)).invoke(null)
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
