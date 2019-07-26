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

import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorState
import androidx.ui.input.FinishComposingTextEditOp
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.SetSelectionEditOp
import androidx.ui.input.TextInputService
import androidx.ui.painting.Canvas
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextPainter
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InputFieldDelegateTest {

    private lateinit var canvas: Canvas
    private lateinit var painter: TextPainter
    private lateinit var processor: EditProcessor
    private lateinit var onValueChange: (EditorState) -> Unit
    private lateinit var onEditorActionPerformed: (Any) -> Unit
    private lateinit var textInputService: TextInputService
    private lateinit var layoutCoordinates: LayoutCoordinates

    val creditCardOffsetTranslator = object : OffsetMap {
        override fun originalToTransformed(offset: Int): Int {
            if (offset <= 3) return offset
            if (offset <= 7) return offset + 1
            if (offset <= 11) return offset + 2
            if (offset <= 16) return offset + 3
            return 19
        }

        override fun transformedToOriginal(offset: Int): Int {
            if (offset <= 4) return offset
            if (offset <= 9) return offset - 1
            if (offset <= 14) return offset - 2
            if (offset <= 19) return offset - 3
            return 16
        }
    }

    private val identityOffsetMap = object : OffsetMap {
        override fun originalToTransformed(offset: Int): Int = offset
        override fun transformedToOriginal(offset: Int): Int = offset
    }

    /**
     * Test implementation of offset map which doubles the offset in transformed text.
     */
    private val skippingOffsetMap = object : OffsetMap {
        override fun originalToTransformed(offset: Int): Int = offset * 2
        override fun transformedToOriginal(offset: Int): Int = offset / 2
    }

    @Before
    fun setup() {
        painter = mock()
        canvas = mock()
        processor = mock()
        onValueChange = mock()
        onEditorActionPerformed = mock()
        textInputService = mock()
        layoutCoordinates = mock()
    }

    @Test
    fun draw_selection_test() {
        val selection = TextRange(0, 1)
        val selectionColor = Color.Blue

        InputFieldDelegate.draw(
            canvas = canvas,
            textPainter = painter,
            value = EditorState(text = "Hello, World", selection = selection),
            editorStyle = EditorStyle(selectionColor = selectionColor),
            hasFocus = true,
            offsetMap = identityOffsetMap
        )

        verify(painter, times(1)).paintBackground(
            eq(selection.start), eq(selection.end), eq(selectionColor), eq(canvas))
        verify(painter, times(1)).paint(eq(canvas))

        verify(painter, never()).paintCursor(any(), any())
    }

    @Test
    fun draw_cursor_test() {
        val cursor = TextRange(1, 1)

        InputFieldDelegate.draw(
            canvas = canvas,
            textPainter = painter,
            value = EditorState(text = "Hello, World", selection = cursor),
            editorStyle = EditorStyle(),
            hasFocus = true,
            offsetMap = identityOffsetMap
        )

        verify(painter, times(1)).paintCursor(eq(cursor.start), eq(canvas))
        verify(painter, times(1)).paint(eq(canvas))
        verify(painter, never()).paintBackground(any(), any(), any(), any())
    }

    @Test
    fun dont_draw_cursor_test() {
        val cursor = TextRange(1, 1)

        InputFieldDelegate.draw(
            canvas = canvas,
            textPainter = painter,
            value = EditorState(text = "Hello, World", selection = cursor),
            editorStyle = EditorStyle(),
            hasFocus = false,
            offsetMap = identityOffsetMap
        )

        verify(painter, never()).paintCursor(any(), any())
        verify(painter, times(1)).paint(eq(canvas))
        verify(painter, never()).paintBackground(any(), any(), any(), any())
    }

    @Test
    fun draw_composition_test() {
        val composition = TextRange(0, 1)
        val compositionColor = Color.Red

        val cursor = TextRange(1, 1)

        InputFieldDelegate.draw(
            canvas = canvas,
            textPainter = painter,
            value = EditorState(text = "Hello, World", selection = cursor,
                composition = composition),
            editorStyle = EditorStyle(compositionColor = compositionColor),
            hasFocus = true,
            offsetMap = identityOffsetMap
        )

        verify(painter, times(1)).paintBackground(
            eq(composition.start), eq(composition.end), eq(compositionColor), eq(canvas))
        verify(painter, times(1)).paint(eq(canvas))
        verify(painter, times(1)).paintCursor(eq(cursor.start), any())
    }

    @Test
    fun test_on_edit_command() {
        val ops = listOf(CommitTextEditOp("Hello, World", 1))
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))

        whenever(processor.onEditCommands(ops)).thenReturn(dummyEditorState)

        InputFieldDelegate.onEditCommand(ops, processor, onValueChange)

        verify(onValueChange, times(1)).invoke(eq(dummyEditorState))
    }

    @Test
    fun test_on_release() {
        val position = PxPosition(100.px, 200.px)
        val offset = 10
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))

        whenever(painter.getOffsetForPosition(position)).thenReturn(offset)

        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(dummyEditorState)

        InputFieldDelegate.onRelease(position, painter, processor,
            identityOffsetMap, onValueChange)

        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is SetSelectionEditOp)
        verify(onValueChange, times(1)).invoke(eq(dummyEditorState))
    }

    @Test
    fun test_draw_order() {
        val canvas: Canvas = mock()

        InputFieldDelegate.draw(
            canvas = canvas,
            textPainter = painter,
            value = EditorState(
                text = "Hello, World", selection = TextRange(1, 1),
                composition = TextRange(1, 3)
            ),
            editorStyle = EditorStyle(compositionColor = Color.Red),
            hasFocus = true,
            offsetMap = identityOffsetMap
            )

        inOrder(painter) {
            verify(painter).paintBackground(eq(1), eq(3), eq(Color.Red), eq(canvas))
            verify(painter).paintCursor(eq(1), eq(canvas))
        }
    }

    @Test
    fun show_soft_input() {
        InputFieldDelegate.onPress(textInputService)
        verify(textInputService).showSoftwareKeyboard()
    }

    @Test
    fun on_focus() {
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))
        InputFieldDelegate.onFocus(textInputService, dummyEditorState, processor,
            KeyboardType.Text, ImeAction.Unspecified, onValueChange, onEditorActionPerformed)
        verify(textInputService).startInput(
            eq(dummyEditorState),
            eq(KeyboardType.Text),
            eq(ImeAction.Unspecified),
            any(),
            eq(onEditorActionPerformed)
        )
    }

    @Test
    fun on_blur() {
        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(EditorState())

        InputFieldDelegate.onBlur(textInputService, processor, onValueChange)

        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is FinishComposingTextEditOp)
        verify(textInputService).stopInput()
    }

    @Test
    fun notify_focused_rect() {
        val dummyRect = Rect(0f, 1f, 2f, 3f)
        whenever(painter.getBoundingBox(any())).thenReturn(dummyRect)
        val dummyPoint = PxPosition(5.px, 6.px)
        whenever(layoutCoordinates.localToRoot(any())).thenReturn(dummyPoint)
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))
        InputFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            painter,
            layoutCoordinates,
            textInputService,
            true /* hasFocus */,
            identityOffsetMap
        )
        verify(textInputService).notifyFocusedRect(any())
    }

    @Test
    fun notify_focused_rect_without_focus() {
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))
        InputFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            painter,
            layoutCoordinates,
            textInputService,
            false /* hasFocus */,
            identityOffsetMap
        )
        verify(textInputService, never()).notifyFocusedRect(any())
    }

    @Test
    fun notify_rect_tail() {
        val dummyRect = Rect(0f, 1f, 2f, 3f)
        whenever(painter.getBoundingBox(any())).thenReturn(dummyRect)
        val dummyPoint = PxPosition(5.px, 6.px)
        whenever(layoutCoordinates.localToRoot(any())).thenReturn(dummyPoint)
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(12, 12))
        InputFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            painter,
            layoutCoordinates,
            textInputService,
            true /* hasFocus */,
            identityOffsetMap
        )
        verify(textInputService).notifyFocusedRect(any())
    }

    @Test
    fun notify_rect_empty() {
        val dummyHeight = 64f
        whenever(painter.preferredLineHeight).thenReturn(dummyHeight)
        val dummyPoint = PxPosition(5.px, 6.px)
        whenever(layoutCoordinates.localToRoot(any())).thenReturn(dummyPoint)
        val dummyEditorState = EditorState(text = "", selection = TextRange(0, 0))
        InputFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            painter,
            layoutCoordinates,
            textInputService,
            true, /* hasFocus */
            identityOffsetMap)
        val captor = argumentCaptor<Rect>()
        verify(textInputService).notifyFocusedRect(captor.capture())
        assertEquals(dummyHeight, captor.firstValue.height)
    }

    @Test
    fun layout() {
        val constraints = Constraints(
            minWidth = 0.px.round(),
            maxWidth = 1024.px.round(),
            minHeight = 0.px.round(),
            maxHeight = 2048.px.round()
        )

        val dummyText = AnnotatedString(text = "Hello, World")
        whenever(painter.text).thenReturn(dummyText)
        whenever(painter.style).thenReturn(TextStyle())
        whenever(painter.density).thenReturn(Density(1.0f))
        whenever(painter.resourceLoader).thenReturn(mock())
        whenever(painter.height).thenReturn(512.0f)

        val res = InputFieldDelegate.layout(painter, constraints)
        assertEquals(1024.px.round(), res.first)
        assertEquals(512.px.round(), res.second)

        verify(painter, times(1)).layout(constraints)
    }

    @Test
    fun check_draw_uses_offset_map() {
        val selection = TextRange(1, 3)
        val selectionColor = Color.Blue

        InputFieldDelegate.draw(
            canvas = canvas,
            textPainter = painter,
            value = EditorState(text = "Hello, World", selection = selection),
            editorStyle = EditorStyle(selectionColor = selectionColor),
            hasFocus = true,
            offsetMap = skippingOffsetMap
        )

        val selectionStartInTransformedText = selection.start * 2
        val selectionEmdInTransformedText = selection.end * 2

        verify(painter, times(1)).paintBackground(
            eq(selectionStartInTransformedText),
            eq(selectionEmdInTransformedText),
            eq(selectionColor),
            eq(canvas))
    }

    @Test
    fun check_notify_rect_uses_offset_map() {
        val dummyRect = Rect(0f, 1f, 2f, 3f)
        val dummyPoint = PxPosition(5.px, 6.px)
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 3))
        whenever(painter.getBoundingBox(any())).thenReturn(dummyRect)
        whenever(layoutCoordinates.localToRoot(any())).thenReturn(dummyPoint)

        InputFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            painter,
            layoutCoordinates,
            textInputService,
            true /* hasFocus */,
            skippingOffsetMap
        )
        verify(painter).getBoundingBox(6)
        verify(textInputService).notifyFocusedRect(any())
    }

    @Test
    fun check_on_release_uses_offset_map() {
        val position = PxPosition(100.px, 200.px)
        val offset = 10
        val dummyEditorState = EditorState(text = "Hello, World", selection = TextRange(1, 1))

        whenever(painter.getOffsetForPosition(position)).thenReturn(offset)

        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(dummyEditorState)

        InputFieldDelegate.onRelease(position, painter, processor, skippingOffsetMap, onValueChange)

        val cursorOffsetInTransformedText = offset / 2
        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is SetSelectionEditOp)
        val setSelectionEditOp = captor.firstValue[0] as SetSelectionEditOp
        assertEquals(cursorOffsetInTransformedText, setSelectionEditOp.start)
        assertEquals(cursorOffsetInTransformedText, setSelectionEditOp.end)
        verify(onValueChange, times(1)).invoke(eq(dummyEditorState))
    }

    @Test
    fun use_identity_mapping_if_visual_transformation_is_null() {
        val (visualText, offsetMap) = InputFieldDelegate.applyVisualFilter(
            EditorState(text = "Hello, World"),
            null)

        assertEquals("Hello, World", visualText.text)
        for (i in 0..visualText.text.length) {
            // Identity mapping returns if no visual filter is provided.
            assertEquals(i, offsetMap.originalToTransformed(i))
            assertEquals(i, offsetMap.transformedToOriginal(i))
        }
    }
}
