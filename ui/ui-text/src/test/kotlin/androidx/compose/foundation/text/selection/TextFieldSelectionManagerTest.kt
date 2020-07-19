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

package androidx.compose.foundation.text.selection

import androidx.test.filters.SmallTest
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedbackType
import androidx.ui.core.texttoolbar.TextToolbar
import androidx.ui.geometry.Rect
import androidx.ui.geometry.Offset
import androidx.ui.input.OffsetMap
import androidx.ui.input.TextFieldValue
import androidx.ui.text.AnnotatedString
import androidx.compose.foundation.text.TextFieldState
import androidx.ui.text.TextLayoutInput
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.style.ResolvedTextDirection
import androidx.ui.text.style.TextOverflow
import androidx.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
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
class TextFieldSelectionManagerTest {
    private val text = "Hello World"
    private val density = Density(density = 1f)
    private val offsetMap = OffsetMap.identityOffsetMap
    private var value = TextFieldValue(text)
    private val lambda: (TextFieldValue) -> Unit = { value = it }
    private val spyLambda = spy(lambda)
    private val state = TextFieldState(mock())

    private val dragBeginPosition = Offset.Zero
    private val dragDistance = Offset(300f, 15f)
    private val beginOffset = 0
    private val dragOffset = text.indexOf('r')
    private val fakeTextRange = TextRange(0, "Hello".length)
    private val dragTextRange = TextRange("Hello".length + 1, text.length)

    private val manager = TextFieldSelectionManager()

    private val clipboardManager = mock<ClipboardManager>()
    private val textToolbar = mock<TextToolbar>()
    private val hapticFeedback = mock<HapticFeedback>()

    @Before
    fun setup() {
        manager.offsetMap = offsetMap
        manager.onValueChange = lambda
        manager.state = state
        manager.value = value
        manager.clipboardManager = clipboardManager
        manager.textToolbar = textToolbar
        manager.hapticFeedBack = hapticFeedback

        state.layoutResult = mock()
        whenever(state.layoutResult!!.layoutInput).thenReturn(
            TextLayoutInput(
                text = AnnotatedString(text),
                style = TextStyle.Default,
                placeholders = mock(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = mock(),
                constraints = Constraints()
            )
        )
        whenever(state.layoutResult!!.getOffsetForPosition(dragBeginPosition)).thenReturn(
            beginOffset
        )
        whenever(state.layoutResult!!.getOffsetForPosition(dragDistance)).thenReturn(dragOffset)
        whenever(state.layoutResult!!.getWordBoundary(beginOffset)).thenReturn(fakeTextRange)
        whenever(state.layoutResult!!.getWordBoundary(dragOffset)).thenReturn(dragTextRange)
        whenever(state.layoutResult!!.getBidiRunDirection(any()))
            .thenReturn(ResolvedTextDirection.Ltr)
        whenever(state.layoutResult!!.getBoundingBox(any())).thenReturn(Rect.zero)
    }

    @Test
    fun TextFieldSelectionManager_init() {
        assertThat(manager.offsetMap).isEqualTo(offsetMap)
        assertThat(manager.onValueChange).isEqualTo(lambda)
        assertThat(manager.state).isEqualTo(state)
        assertThat(manager.value).isEqualTo(value)
    }

    @Test
    fun TextFieldSelectionManager_longPressDragObserver_onLongPress() {
        manager.longPressDragObserver.onLongPress(dragBeginPosition)

        assertThat(state.selectionIsOn).isTrue()
        assertThat(value.selection).isEqualTo(fakeTextRange)
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_longPressDragObserver_onDrag() {
        manager.longPressDragObserver.onLongPress(dragBeginPosition)
        manager.longPressDragObserver.onDrag(dragDistance)

        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
        verify(
            hapticFeedback,
            times(2)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onStart_startHandle() {
        manager.handleDragObserver(isStartHandle = true).onStart(Offset.Zero)

        assertThat(state.draggingHandle).isTrue()
        verify(spyLambda, times(0)).invoke(any())
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onStart_endHandle() {
        manager.handleDragObserver(isStartHandle = false).onStart(Offset.Zero)

        assertThat(state.draggingHandle).isTrue()
        verify(spyLambda, times(0)).invoke(any())
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDrag_startHandle() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        val result = manager.handleDragObserver(isStartHandle = true).onDrag(dragDistance)

        assertThat(result).isEqualTo(dragDistance)
        assertThat(value.selection).isEqualTo(TextRange(dragOffset, "Hello".length))
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDrag_endHandle() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        val result = manager.handleDragObserver(isStartHandle = false).onDrag(dragDistance)

        assertThat(result).isEqualTo(dragDistance)
        assertThat(value.selection).isEqualTo(TextRange(0, dragOffset))
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onStop() {
        manager.handleDragObserver(false).onStart(Offset.Zero)
        manager.handleDragObserver(false).onDrag(Offset.Zero)

        manager.handleDragObserver(false).onStop(Offset.Zero)

        assertThat(state.draggingHandle).isFalse()
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun copy_selection_collapse() {
        manager.value = TextFieldValue(text = text, selection = TextRange(4, 4))

        manager.copy()

        verify(clipboardManager, times(0)).setText(any())
    }

    @Test
    fun copy_selection_not_null() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.copy()

        verify(clipboardManager, times(1)).setText(AnnotatedString("Hello"))
        assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
        assertThat(state.selectionIsOn).isFalse()
    }

    @Test
    fun paste_clipBoardManager_null() {
        manager.clipboardManager = null

        manager.paste()

        verify(spyLambda, times(0)).invoke(any())
    }

    @Test
    fun paste_clipBoardManager_empty() {
        whenever(clipboardManager.getText()).thenReturn(null)

        manager.paste()

        verify(spyLambda, times(0)).invoke(any())
    }

    @Test
    fun paste_clipBoardManager_not_empty() {
        whenever(clipboardManager.getText()).thenReturn(AnnotatedString("Hello"))
        manager.value = TextFieldValue(
            text = text,
            selection = TextRange("Hel".length, "Hello Wo".length)
        )

        manager.paste()

        assertThat(value.text).isEqualTo("HelHellorld")
        assertThat(value.selection).isEqualTo(TextRange("Hello Wo".length, "Hello Wo".length))
        assertThat(state.selectionIsOn).isFalse()
    }

    @Test
    fun cut_selection_collapse() {
        manager.value = TextFieldValue(text = text, selection = TextRange(4, 4))

        manager.cut()

        verify(clipboardManager, times(0)).setText(any())
    }

    @Test
    fun cut_selection_not_null() {
        manager.value = TextFieldValue(
            text = text + text,
            selection = TextRange("Hello".length, text.length)
        )

        manager.cut()

        verify(clipboardManager, times(1)).setText(AnnotatedString(" World"))
        assertThat(value.text).isEqualTo("HelloHello World")
        assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
        assertThat(state.selectionIsOn).isFalse()
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_Clipboard_empty_not_show_paste() {
        manager.value = TextFieldValue(
            text = text + text,
            selection = TextRange("Hello".length, text.length)
        )

        manager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(any(), any(), isNull(), any())
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_selection_collapse_not_show_copy_cut() {
        whenever(clipboardManager.getText()).thenReturn(AnnotatedString(text))
        manager.value = TextFieldValue(
            text = text + text,
            selection = TextRange(0, 0)
        )

        manager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(any(), isNull(), any(), isNull())
    }
}
