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

import androidx.collection.LongObjectMap
import androidx.collection.emptyLongObjectMap
import androidx.collection.longObjectMapOf
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.util.fastForEach
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(ContextMenuFlagFlipperRunner::class)
class SelectionManagerTest {
    private val selectionRegistrar = spy(SelectionRegistrarImpl())
    private val selectable = FakeSelectable()
    private val selectableId = 1L
    private val selectionManager = SelectionManager(selectionRegistrar)
    private var onSelectionChangeCalledTimes = 0

    private val containerLayoutCoordinates = MockCoordinates()

    private val startSelectableId = 2L
    private val startSelectable =
        mock<Selectable> { whenever(it.selectableId).thenReturn(startSelectableId) }

    private val endSelectableId = 3L
    private val endSelectable =
        mock<Selectable> { whenever(it.selectableId).thenReturn(endSelectableId) }

    private val middleSelectableId = 4L
    private val middleSelectable =
        mock<Selectable> { whenever(it.selectableId).thenReturn(middleSelectableId) }

    private val lastSelectableId = 5L
    private val lastSelectable =
        mock<Selectable> { whenever(it.selectableId).thenReturn(lastSelectableId) }

    private val fakeSelection =
        Selection(
            start =
                Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 0,
                    selectableId = startSelectableId,
                ),
            end =
                Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = 5,
                    selectableId = endSelectableId,
                ),
        )

    private val hapticFeedback = mock<HapticFeedback>()
    private val clipboard = mock<Clipboard>()
    private val textToolbar = mock<TextToolbar>()

    @Before
    fun setup() {
        selectable.clear()
        selectable.selectableId = selectableId
        selectionRegistrar.subscribe(selectable)
        selectionRegistrar.subselections =
            longObjectMapOf(
                selectableId,
                fakeSelection,
                startSelectableId,
                fakeSelection,
                endSelectableId,
                fakeSelection,
            )
        selectionManager.containerLayoutCoordinates = containerLayoutCoordinates
        selectionManager.hapticFeedBack = hapticFeedback
        selectionManager.textToolbar = textToolbar
        selectionManager.selection = fakeSelection
        selectionManager.onSelectionChange = { onSelectionChangeCalledTimes++ }
        selectionManager.onCopyHandler = {}
    }

    @Test
    fun updateSelection_onInitial_returnsTrue() {
        val endHandlePosition = Offset(x = 25f, y = 5f)
        selectable.apply {
            textToReturn = AnnotatedString("hello")
            rawStartHandleOffset = 0
            rawEndHandleOffset = 5
        }

        val actual =
            selectionManager.updateSelection(
                position = endHandlePosition,
                previousHandlePosition = endHandlePosition - Offset(x = 5f, y = 0f),
                isStartHandle = false,
                adjustment = SelectionAdjustment.None,
            )

        assertThat(actual).isTrue()
        assertThat(onSelectionChangeCalledTimes).isEqualTo(1)
    }

    @Test
    fun updateSelection_onNoChange_returnsFalse() {
        val endHandlePosition = Offset(x = 25f, y = 5f)
        selectable.apply {
            textToReturn = AnnotatedString("hello")
            rawStartHandleOffset = 0
            rawEndHandleOffset = 5
            rawPreviousHandleOffset = 5
        }

        // run once to set context for the "previous" selection update
        selectionManager.updateSelection(
            position = endHandlePosition,
            previousHandlePosition = endHandlePosition,
            isStartHandle = false,
            adjustment = SelectionAdjustment.None,
        )

        // run again since we are testing the "no changes" case
        val actual =
            selectionManager.updateSelection(
                position = endHandlePosition,
                previousHandlePosition = endHandlePosition,
                isStartHandle = false,
                adjustment = SelectionAdjustment.None,
            )

        assertThat(actual).isFalse()
        assertThat(onSelectionChangeCalledTimes).isEqualTo(1)
    }

    @Test
    fun updateSelection_onChange_returnsTrue() {
        val endHandlePosition = Offset(x = 25f, y = 5f)
        selectable.apply {
            textToReturn = AnnotatedString("hello")
            rawStartHandleOffset = 0
            rawEndHandleOffset = 5
            rawPreviousHandleOffset = 5
        }

        // run once to set context for the "previous" selection update
        selectionManager.updateSelection(
            position = endHandlePosition,
            previousHandlePosition = endHandlePosition,
            isStartHandle = false,
            adjustment = SelectionAdjustment.None,
        )

        // run again with a change in end handle
        selectable.rawEndHandleOffset = 4
        val actual =
            selectionManager.updateSelection(
                position = endHandlePosition,
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
    fun mergeSelections_selectAllInSelectable() {
        val anotherSelectableId = 100L
        val selectableAnother = mock<Selectable>()
        whenever(selectableAnother.selectableId).thenReturn(anotherSelectableId)

        selectionRegistrar.subscribe(selectableAnother)

        selectionManager.selectAllInSelectable(
            selectableId = selectableId,
            previousSelection = fakeSelection,
        )

        verify(selectableAnother, times(0)).getSelectAllSelection()
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun isNonEmptySelection_whenNonEmptySelection_sameLine_returnsTrue() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text)
        val startOffset = text.indexOf('e')
        val endOffset = text.indexOf('m')
        selectable.textToReturn = annotatedString
        val selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = selectableId,
                    ),
                handlesCrossed = false,
            )
        selectionManager.selection = selection
        selectionRegistrar.subselections = longObjectMapOf(selectableId, selection)

        assertThat(selectionManager.isNonEmptySelection()).isTrue()
    }

    @Test
    fun isNonEmptySelection_whenEmptySelection_sameLine_returnsFalse() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text)
        val startOffset = text.indexOf('e')
        selectable.textToReturn = annotatedString
        val selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                handlesCrossed = false,
            )
        selectionManager.selection = selection
        selectionRegistrar.subselections = longObjectMapOf(selectableId, selection)

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
        selectionManager.selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = startSelectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = endSelectableId,
                    ),
                handlesCrossed = true,
            )

        selectionRegistrar.subselections =
            longObjectMapOf(
                endSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = endSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = endOffset,
                            selectableId = endSelectableId,
                        ),
                    handlesCrossed = true,
                ),
                middleSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = middleSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = middleSelectableId,
                        ),
                    handlesCrossed = true,
                ),
                startSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = startOffset,
                            selectableId = startSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = startSelectableId,
                        ),
                    handlesCrossed = true,
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
        selectionManager.selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = startSelectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = endSelectableId,
                    ),
                handlesCrossed = false,
            )

        selectionRegistrar.subselections =
            longObjectMapOf(
                startSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = startSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = startSelectableId,
                        ),
                    handlesCrossed = false,
                ),
                middleSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = middleSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = middleSelectableId,
                        ),
                    handlesCrossed = false,
                ),
                endSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = endSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = endSelectableId,
                        ),
                    handlesCrossed = false,
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
        selectionManager.selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = startSelectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = endSelectableId,
                    ),
                handlesCrossed = true,
            )

        selectionRegistrar.subselections =
            longObjectMapOf(
                startSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = startSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = startSelectableId,
                        ),
                    handlesCrossed = true,
                ),
                middleSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = middleSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = middleSelectableId,
                        ),
                    handlesCrossed = true,
                ),
                endSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = endSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = endSelectableId,
                        ),
                    handlesCrossed = true,
                ),
            )

        assertThat(selectionManager.isNonEmptySelection()).isFalse()
    }

    @Test
    fun getSelectedText_selection_null_return_null() {
        selectionManager.selection = null
        selectionRegistrar.subselections = emptyLongObjectMap()

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
        val selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = selectableId,
                    ),
                handlesCrossed = false,
            )
        selectionManager.selection = selection
        selectionRegistrar.subselections = longObjectMapOf(selectableId, selection)

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
        val selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = selectableId,
                    ),
                handlesCrossed = true,
            )
        selectionManager.selection = selection
        selectionRegistrar.subselections = longObjectMapOf(selectableId, selection)

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
        selectionManager.selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = startSelectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = endSelectableId,
                    ),
                handlesCrossed = false,
            )

        selectionRegistrar.subselections =
            longObjectMapOf(
                startSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = startOffset,
                            selectableId = startSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = startSelectableId,
                        ),
                    handlesCrossed = false,
                ),
                middleSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = middleSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = middleSelectableId,
                        ),
                    handlesCrossed = false,
                ),
                endSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = endSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = endOffset,
                            selectableId = endSelectableId,
                        ),
                    handlesCrossed = false,
                ),
            )

        val result = buildAnnotatedString {
            append(annotatedString.subSequence(startOffset, annotatedString.length))
            append("\n")
            append(annotatedString)
            append("\n")
            append(annotatedString.subSequence(0, endOffset))
        }
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
        selectionManager.selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = startSelectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = endSelectableId,
                    ),
                handlesCrossed = true,
            )

        selectionRegistrar.subselections =
            longObjectMapOf(
                endSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = endSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = endOffset,
                            selectableId = endSelectableId,
                        ),
                    handlesCrossed = true,
                ),
                middleSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = annotatedString.length,
                            selectableId = middleSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = middleSelectableId,
                        ),
                    handlesCrossed = true,
                ),
                startSelectableId,
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = startOffset,
                            selectableId = startSelectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = 0,
                            selectableId = startSelectableId,
                        ),
                    handlesCrossed = true,
                ),
            )

        val result = buildAnnotatedString {
            append(annotatedString.subSequence(endOffset, annotatedString.length))
            append("\n")
            append(annotatedString)
            append("\n")
            append(annotatedString.subSequence(0, startOffset))
        }
        assertThat(selectionManager.getSelectedText()).isEqualTo(result)
        assertThat(selectable.getTextCalledTimes).isEqualTo(0)
        verify(startSelectable, times(1)).getText()
        verify(middleSelectable, times(1)).getText()
        verify(endSelectable, times(1)).getText()
        verify(lastSelectable, times(0)).getText()
    }

    @Test
    fun copy_selection_null_not_trigger_clipboardManager() = runTest {
        selectionManager.selection = null
        selectionRegistrar.subselections = emptyLongObjectMap()

        selectionManager.copy()

        verify(clipboard, times(0)).setClipEntry(any())
    }

    @Test
    fun copy_selection_not_null_trigger_clipboardManager_setText() = runTest {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        val selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = selectableId,
                    ),
                handlesCrossed = true,
            )
        selectionManager.selection = selection
        selectionRegistrar.subselections = longObjectMapOf(selectableId, selection)

        var actualTextToCopy: AnnotatedString? = null
        selectionManager.onCopyHandler = { textToCopy -> actualTextToCopy = textToCopy }
        selectionManager.copy()

        assertThat(actualTextToCopy).isEqualTo(annotatedString.subSequence(endOffset, startOffset))
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        selectable.layoutCoordinatesToReturn = containerLayoutCoordinates
        val selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = selectableId,
                    ),
                handlesCrossed = true,
            )
        selectionManager.selection = selection
        selectionRegistrar.subselections = longObjectMapOf(selectableId, selection)
        selectionManager.hasFocus = true

        selectionManager.showToolbar = true

        verify(textToolbar, times(1)).showMenu(any(), any(), isNull(), isNull(), any(), isNull())
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun showSelectionToolbar_withoutFocus_notTrigger_textToolbar_showMenu() {
        val text = "Text Demo"
        val annotatedString = AnnotatedString(text = text)
        val startOffset = text.indexOf('m')
        val endOffset = text.indexOf('x')
        selectable.textToReturn = annotatedString
        val selection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = selectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = selectableId,
                    ),
                handlesCrossed = true,
            )
        selectionManager.selection = selection
        selectionRegistrar.subselections = longObjectMapOf(selectableId, selection)
        selectionManager.hasFocus = false

        selectionManager.showToolbar = true

        verify(textToolbar, never()).showMenu(any(), any(), isNull(), isNull(), isNull(), any())
    }

    @Test
    fun onRelease_selectionMap_is_setToEmpty() {
        val fakeSelection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = 0,
                        selectableId = startSelectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = 5,
                        selectableId = endSelectableId,
                    ),
            )
        var selection: Selection? = fakeSelection
        var onSelectionChangeInvocationCount = 0
        val onSelectionChangeLambda: (Selection?) -> Unit = { newSelection ->
            selection = newSelection
            onSelectionChangeInvocationCount++
        }
        selectionManager.onSelectionChange = onSelectionChangeLambda
        selectionManager.selection = fakeSelection

        selectionManager.onRelease()

        verify(selectionRegistrar).subselections = emptyLongObjectMap()

        assertThat(selection).isNull()
        assertThat(onSelectionChangeInvocationCount).isEqualTo(1)
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun notifySelectableChange_clears_selection() {
        val fakeSelection =
            Selection(
                start =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = 0,
                        selectableId = startSelectableId,
                    ),
                end =
                    Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = 5,
                        selectableId = startSelectableId,
                    ),
            )
        var selection: Selection? = fakeSelection
        var onSelectionChangeInvocationCount = 0
        val onSelectionChangeLambda: (Selection?) -> Unit = { newSelection ->
            selection = newSelection
            onSelectionChangeInvocationCount++
        }
        selectionManager.onSelectionChange = onSelectionChangeLambda
        selectionManager.selection = fakeSelection

        selectionRegistrar.subselections = longObjectMapOf(startSelectableId, fakeSelection)
        selectionRegistrar.notifySelectableChange(startSelectableId)

        verify(selectionRegistrar).subselections = emptyLongObjectMap()
        assertThat(selection).isNull()
        assertThat(onSelectionChangeInvocationCount).isEqualTo(1)
        verify(hapticFeedback, times(1)).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // region isEntireContainerSelected Tests
    @Test
    fun isEntireContainerSelected_noSelectables_returnsTrue() {
        isEntireContainerSelectedTest(expectedResult = true)
    }

    @Test
    fun isEntireContainerSelected_singleEmptySelectable_returnsTrue() {
        isEntireContainerSelectedTest(
            expectedResult = true,
            IsEntireContainerSelectedData(text = "", selection = null),
        )
    }

    @Test
    fun isEntireContainerSelected_multipleEmptySelectables_returnsTrue() {
        isEntireContainerSelectedTest(
            expectedResult = true,
            IsEntireContainerSelectedData(text = "", selection = null),
            IsEntireContainerSelectedData(text = "", selection = null),
            IsEntireContainerSelectedData(text = "", selection = null),
        )
    }

    @Test
    fun isEntireContainerSelected_emptySurroundingNonEmpty_fullySelected_returnsTrue() {
        isEntireContainerSelectedTest(
            expectedResult = true,
            IsEntireContainerSelectedData(text = "", selection = null),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "", selection = null),
        )
    }

    @Test
    fun isEntireContainerSelected_nonEmptySurroundingEmpty_fullySelected_returnsTrue() {
        isEntireContainerSelectedTest(
            expectedResult = true,
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "", selection = TextRange(0, 0)),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
        )
    }

    @Test
    fun isEntireContainerSelected_nonEmptyFirstTextNotSelected_returnsFalse() {
        isEntireContainerSelectedTest(
            expectedResult = false,
            IsEntireContainerSelectedData(text = "Text", selection = null),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
        )
    }

    @Test
    fun isEntireContainerSelected_nonEmptyLastTextNotSelected_returnsFalse() {
        isEntireContainerSelectedTest(
            expectedResult = false,
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "Text", selection = null),
        )
    }

    @Test
    fun isEntireContainerSelected_firstTextPartiallySelected_returnsFalse() {
        isEntireContainerSelectedTest(
            expectedResult = false,
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(1, 4)),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
        )
    }

    @Test
    fun isEntireContainerSelected_lastTextPartiallySelected_returnsFalse() {
        isEntireContainerSelectedTest(
            expectedResult = false,
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 4)),
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(0, 3)),
        )
    }

    @Test
    fun isEntireContainerSelected_reversedSelectionFullySelected_returnsTrue() {
        isEntireContainerSelectedTest(
            expectedResult = true,
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(4, 0)),
        )
    }

    @Test
    fun isEntireContainerSelected_reversedSelectionPartiallySelected_returnsFalse() {
        isEntireContainerSelectedTest(
            expectedResult = false,
            IsEntireContainerSelectedData(text = "Text", selection = TextRange(3, 0)),
        )
    }

    /**
     * Data necessary to set up a [SelectionManager.isEntireContainerSelected] unit test.
     *
     * @param text The text for the [Selectable] to return in [Selectable.getText].
     * @param selection The selection to be associated with the [SelectionRegistrar.subselections].
     *   Null implies "do not include this selectable in the sub-selection".
     */
    private data class IsEntireContainerSelectedData(val text: String, val selection: TextRange?)

    private fun isEntireContainerSelectedTest(
        expectedResult: Boolean,
        vararg selectableStates: IsEntireContainerSelectedData,
    ) {
        val selectables =
            selectableStates.mapIndexed { index, item ->
                FakeSelectable().apply {
                    selectableId = index + 1L
                    textToReturn = AnnotatedString(item.text)
                }
            }

        val registrar =
            SelectionRegistrarImpl().apply {
                selectables.fastForEach { subscribe(it) }
                subselections =
                    selectableStates
                        .withIndex()
                        .filter { it.value.selection != null }
                        .associate { (index, item) ->
                            val id = index + 1L
                            val selection = item.selection
                            id to
                                Selection(
                                    start =
                                        Selection.AnchorInfo(
                                            direction = ResolvedTextDirection.Ltr,
                                            offset = selection!!.start,
                                            selectableId = id,
                                        ),
                                    end =
                                        Selection.AnchorInfo(
                                            direction = ResolvedTextDirection.Ltr,
                                            offset = selection.end,
                                            selectableId = id,
                                        ),
                                    handlesCrossed = selection.reversed,
                                )
                        }
                        .toLongObjectMap()
            }

        val manager =
            SelectionManager(registrar).apply { containerLayoutCoordinates = MockCoordinates() }

        assertThat(manager.isEntireContainerSelected()).run {
            if (expectedResult) isTrue() else isFalse()
        }
    }

    // endregion isEntireContainerSelected Tests

    // region selectAll Tests
    @Test
    fun selectAll_noSelectables_noSelection() {
        selectAllTest(expectedSelection = null, expectedSubSelectionRanges = emptyMap())
    }

    @Test
    fun selectAll_singleUnSelectable_noSelection() {
        selectAllTest(
            expectedSelection = null,
            expectedSubSelectionRanges = emptyMap(),
            SelectAllData(text = "Text", selection = null),
        )
    }

    @Test
    fun selectAll_singleSelectable_selectedAsExpected() {
        selectAllTest(
            expectedSelection = expectedSelection(0, 4),
            expectedSubSelectionRanges = mapOf(1L to TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
        )
    }

    @Test
    fun selectAll_multiSelectable_selectedAsExpected() {
        selectAllTest(
            expectedSelection =
                expectedSelection(
                    startOffset = 0,
                    endOffset = 4,
                    startSelectableId = 1L,
                    endSelectableId = 3L,
                ),
            expectedSubSelectionRanges =
                mapOf(1L to TextRange(0, 4), 2L to TextRange(0, 4), 3L to TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
        )
    }

    @Test
    fun selectAll_multiSelectable_skipFirst_selectedAsExpected() {
        selectAllTest(
            expectedSelection =
                expectedSelection(
                    startOffset = 0,
                    endOffset = 4,
                    startSelectableId = 2L,
                    endSelectableId = 3L,
                ),
            expectedSubSelectionRanges = mapOf(2L to TextRange(0, 4), 3L to TextRange(0, 4)),
            SelectAllData(text = "Text", selection = null),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
        )
    }

    @Test
    fun selectAll_multiSelectable_skipMiddle_selectedAsExpected() {
        selectAllTest(
            expectedSelection =
                expectedSelection(
                    startOffset = 0,
                    endOffset = 4,
                    startSelectableId = 1L,
                    endSelectableId = 3L,
                ),
            expectedSubSelectionRanges = mapOf(1L to TextRange(0, 4), 3L to TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
            SelectAllData(text = "Text", selection = null),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
        )
    }

    @Test
    fun selectAll_multiSelectable_skipLast_selectedAsExpected() {
        selectAllTest(
            expectedSelection =
                expectedSelection(
                    startOffset = 0,
                    endOffset = 4,
                    startSelectableId = 1L,
                    endSelectableId = 2L,
                ),
            expectedSubSelectionRanges = mapOf(1L to TextRange(0, 4), 2L to TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
            SelectAllData(text = "Text", selection = TextRange(0, 4)),
            SelectAllData(text = "Text", selection = null),
        )
    }

    @Test
    fun startHandleLineHeight_valid() {
        val lineHeightPx = 15f
        selectionRegistrar.subscribe(startSelectable)
        whenever(startSelectable.getLineHeight(fakeSelection.start.offset)).thenReturn(lineHeightPx)

        assertThat(selectionManager.startHandleLineHeight).isEqualTo(lineHeightPx)
    }

    @Test
    fun startHandleLineHeight_no_selection_return_zero() {
        val lineHeightPx = 15f
        selectionRegistrar.subscribe(startSelectable)
        whenever(startSelectable.getLineHeight(fakeSelection.start.offset)).thenReturn(lineHeightPx)

        selectionManager.selection = null

        assertThat(selectionManager.startHandleLineHeight).isZero()
    }

    @Test
    fun endHandleLineHeight_valid() {
        val lineHeightPx = 15f
        selectionRegistrar.subscribe(endSelectable)
        whenever(endSelectable.getLineHeight(fakeSelection.end.offset)).thenReturn(lineHeightPx)

        assertThat(selectionManager.endHandleLineHeight).isEqualTo(lineHeightPx)
    }

    @Test
    fun endHandleLineHeight_no_selection_return_zero() {
        val lineHeightPx = 15f
        selectionRegistrar.subscribe(endSelectable)
        whenever(endSelectable.getLineHeight(fakeSelection.end.offset)).thenReturn(lineHeightPx)

        selectionManager.selection = null

        assertThat(selectionManager.endHandleLineHeight).isZero()
    }

    private fun expectedSelection(
        startOffset: Int,
        endOffset: Int,
        startSelectableId: Long = 1L,
        endSelectableId: Long = 1L,
        handlesCrossed: Boolean = false,
    ): Selection =
        Selection(
            start =
                Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = startOffset,
                    selectableId = startSelectableId,
                ),
            end =
                Selection.AnchorInfo(
                    direction = ResolvedTextDirection.Ltr,
                    offset = endOffset,
                    selectableId = endSelectableId,
                ),
            handlesCrossed = handlesCrossed,
        )

    /**
     * Data necessary to set up a [SelectionManager.selectAll] unit test.
     *
     * @param text The text for the [FakeSelectable] to return in [Selectable.getText].
     * @param selection The selection for the [FakeSelectable] to return in
     *   [Selectable.getSelectAllSelection].
     */
    private data class SelectAllData(val text: String, val selection: TextRange?)

    private fun selectAllTest(
        expectedSelection: Selection?,
        expectedSubSelectionRanges: Map<Long, TextRange>,
        vararg selectableStates: SelectAllData,
    ) {
        val selectables =
            selectableStates.mapIndexed { index, item ->
                val id = index + 1L
                val range = item.selection
                FakeSelectable().apply {
                    selectableId = id
                    textToReturn = AnnotatedString(item.text)
                    fakeSelectAllSelection =
                        range?.let {
                            Selection(
                                start =
                                    Selection.AnchorInfo(
                                        direction = ResolvedTextDirection.Ltr,
                                        offset = it.start,
                                        selectableId = id,
                                    ),
                                end =
                                    Selection.AnchorInfo(
                                        direction = ResolvedTextDirection.Ltr,
                                        offset = it.end,
                                        selectableId = id,
                                    ),
                                handlesCrossed = it.reversed,
                            )
                        }
                }
            }

        val registrar = SelectionRegistrarImpl().apply { selectables.fastForEach { subscribe(it) } }

        val expectedSubSelections =
            expectedSubSelectionRanges
                .mapValues { (id, range) ->
                    expectedSelection(
                        startOffset = range.start,
                        endOffset = range.end,
                        startSelectableId = id,
                        endSelectableId = id,
                        handlesCrossed = range.start > range.end,
                    )
                }
                .toLongObjectMap()

        SelectionManager(registrar).apply {
            containerLayoutCoordinates = MockCoordinates()
            onSelectionChange = { newSelection ->
                if (expectedSelection == null) {
                    fail("Expected no selection update, but received one anyways.")
                }
                assertThat(newSelection).isEqualTo(expectedSelection)
            }
            selectAll()
        }

        assertThat(registrar.subselections).isEqualTo(expectedSubSelections)
    }

    // endregion selectAll Tests

    private fun <T> Map<Long, T>.toLongObjectMap(): LongObjectMap<T> =
        mutableLongObjectMapOf<T>().apply {
            this@toLongObjectMap.keys.forEach { key -> put(key, this@toLongObjectMap[key]!!) }
        }
}
