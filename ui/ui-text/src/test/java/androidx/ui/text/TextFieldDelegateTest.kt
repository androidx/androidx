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

package androidx.ui.text

import androidx.ui.core.Constraints
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.LayoutDirection
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorValue
import androidx.ui.input.FinishComposingTextEditOp
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.OffsetMap
import androidx.ui.input.SetSelectionEditOp
import androidx.ui.input.TextInputService
import androidx.ui.input.TransformedText
import androidx.ui.input.VisualTransformation
import androidx.ui.text.style.TextDecoration
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.round
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
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
class TextFieldDelegateTest {

    private lateinit var canvas: Canvas
    private lateinit var mDelegate: TextDelegate
    private lateinit var processor: EditProcessor
    private lateinit var onValueChange: (EditorValue) -> Unit
    private lateinit var onEditorActionPerformed: (Any) -> Unit
    private lateinit var textInputService: TextInputService
    private lateinit var layoutCoordinates: LayoutCoordinates
    private lateinit var multiParagraphIntrinsics: MultiParagraphIntrinsics
    private lateinit var textLayoutResult: TextLayoutResult

    private val layoutDirection = LayoutDirection.Ltr

    /**
     * Test implementation of offset map which doubles the offset in transformed text.
     */
    private val skippingOffsetMap = object : OffsetMap {
        override fun originalToTransformed(offset: Int): Int = offset * 2
        override fun transformedToOriginal(offset: Int): Int = offset / 2
    }

    @Before
    fun setup() {
        mDelegate = mock()
        canvas = mock()
        processor = mock()
        onValueChange = mock()
        onEditorActionPerformed = mock()
        textInputService = mock()
        layoutCoordinates = mock()
        multiParagraphIntrinsics = mock()
        textLayoutResult = mock()
    }

    @Test
    fun test_on_edit_command() {
        val ops = listOf(CommitTextEditOp("Hello, World", 1))
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(1, 1))

        whenever(processor.onEditCommands(ops)).thenReturn(dummyEditorState)

        TextFieldDelegate.onEditCommand(ops, processor, onValueChange)

        verify(onValueChange, times(1)).invoke(eq(
            EditorValue(
            text = dummyEditorState.text,
            selection = dummyEditorState.selection
        )
        ))
    }

    @Test
    fun test_on_release() {
        val position = PxPosition(100f, 200f)
        val offset = 10
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(1, 1))
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.

        whenever(textLayoutResult.getOffsetForPosition(position)).thenReturn(offset)

        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(dummyEditorState)

        TextFieldDelegate.onRelease(
            position,
            textLayoutResult,
            processor,
            OffsetMap.identityOffsetMap,
            onValueChange,
            textInputService,
            dummyInputSessionToken,
            true)

        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is SetSelectionEditOp)
        verify(onValueChange, times(1)).invoke(eq(
            EditorValue(
                text = dummyEditorState.text,
                selection = dummyEditorState.selection
            )
        ))
        verify(textInputService).showSoftwareKeyboard(eq(dummyInputSessionToken))
    }

    @Test
    fun test_on_release_do_not_place_cursor_if_focus_is_out() {
        val position = PxPosition(100f, 200f)
        val offset = 10
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.

        whenever(textLayoutResult.getOffsetForPosition(position)).thenReturn(offset)
        TextFieldDelegate.onRelease(
            position,
            textLayoutResult,
            processor,
            OffsetMap.identityOffsetMap,
            onValueChange,
            textInputService,
            dummyInputSessionToken,
            false)

        verify(onValueChange, never()).invoke(any())
        verify(textInputService).showSoftwareKeyboard(eq(dummyInputSessionToken))
    }

    @Test
    fun on_focus() {
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(1, 1))
        TextFieldDelegate.onFocus(textInputService, dummyEditorState, processor,
            KeyboardType.Text, ImeAction.Unspecified, onValueChange, onEditorActionPerformed)
        verify(textInputService).startInput(
            eq(
                EditorValue(
                text = dummyEditorState.text,
                selection = dummyEditorState.selection
            )
            ),
            eq(KeyboardType.Text),
            eq(ImeAction.Unspecified),
            any(),
            eq(onEditorActionPerformed)
        )
    }

    @Test
    fun on_blur() {
        val captor = argumentCaptor<List<EditOperation>>()
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.

        whenever(processor.onEditCommands(captor.capture())).thenReturn(EditorValue())

        TextFieldDelegate.onBlur(
            textInputService,
            dummyInputSessionToken,
            processor,
            true,
            onValueChange)

        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is FinishComposingTextEditOp)
        verify(textInputService).stopInput(eq(dummyInputSessionToken))
        verify(textInputService, never()).hideSoftwareKeyboard(any())
    }

    @Test
    fun on_blur_with_hiding() {
        val captor = argumentCaptor<List<EditOperation>>()
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.

        whenever(processor.onEditCommands(captor.capture())).thenReturn(EditorValue())

        TextFieldDelegate.onBlur(
            textInputService,
            dummyInputSessionToken,
            processor,
            false, // There is no next focused client. Hide the keyboard.
            onValueChange)

        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is FinishComposingTextEditOp)
        verify(textInputService).stopInput(eq(dummyInputSessionToken))
        verify(textInputService).hideSoftwareKeyboard(eq(dummyInputSessionToken))
    }

    @Test
    fun notify_focused_rect() {
        val dummyRect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(dummyRect)
        val dummyPoint = PxPosition(5f, 6f)
        whenever(layoutCoordinates.localToRoot(any())).thenReturn(dummyPoint)
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(1, 1))
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.
        TextFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            dummyInputSessionToken,
            true /* hasFocus */,
            OffsetMap.identityOffsetMap
        )
        verify(textInputService).notifyFocusedRect(eq(dummyInputSessionToken), any())
    }

    @Test
    fun notify_focused_rect_without_focus() {
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(1, 1))
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.
        TextFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            dummyInputSessionToken,
            false /* hasFocus */,
            OffsetMap.identityOffsetMap
        )
        verify(textInputService, never()).notifyFocusedRect(any(), any())
    }

    @Test
    fun notify_rect_tail() {
        val dummyRect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(dummyRect)
        val dummyPoint = PxPosition(5f, 6f)
        whenever(layoutCoordinates.localToRoot(any())).thenReturn(dummyPoint)
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(12, 12))
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.
        TextFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            dummyInputSessionToken,
            true /* hasFocus */,
            OffsetMap.identityOffsetMap
        )
        verify(textInputService).notifyFocusedRect(eq(dummyInputSessionToken), any())
    }

    @Test
    fun layout() {
        val constraints = Constraints(
            minWidth = 0.px.round(),
            maxWidth = 1280.px.round(),
            minHeight = 0.px.round(),
            maxHeight = 2048.px.round()
        )

        val dummyText = AnnotatedString(text = "Hello, World")
        whenever(mDelegate.text).thenReturn(dummyText)
        whenever(mDelegate.style).thenReturn(TextStyle())
        whenever(mDelegate.density).thenReturn(Density(1.0f))
        whenever(mDelegate.resourceLoader).thenReturn(mock())
        whenever(mDelegate.layout(any(), any(), eq(null))).thenReturn(textLayoutResult)
        whenever(textLayoutResult.size).thenReturn(IntPxSize(1024.ipx, 512.ipx))

        val (width, height, layoutResult) = TextFieldDelegate.layout(
            mDelegate,
            constraints,
            layoutDirection
        )
        assertEquals(1024.px.round(), width)
        assertEquals(512.px.round(), height)
        assertEquals(layoutResult, textLayoutResult)
    }

    @Test
    fun check_notify_rect_uses_offset_map() {
        val dummyRect = Rect(0f, 1f, 2f, 3f)
        val dummyPoint = PxPosition(5f, 6f)
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(1, 3))
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(dummyRect)
        whenever(layoutCoordinates.localToRoot(any())).thenReturn(dummyPoint)

        TextFieldDelegate.notifyFocusedRect(
            dummyEditorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            dummyInputSessionToken,
            true /* hasFocus */,
            skippingOffsetMap
        )
        verify(textLayoutResult).getBoundingBox(6)
        verify(textInputService).notifyFocusedRect(eq(dummyInputSessionToken), any())
    }

    @Test
    fun check_on_release_uses_offset_map() {
        val position = PxPosition(100f, 200f)
        val offset = 10
        val dummyEditorState = EditorValue(text = "Hello, World", selection = TextRange(1, 1))
        val dummyInputSessionToken = 10 // We are not using this value in this test. Just dummy.

        whenever(textLayoutResult.getOffsetForPosition(position)).thenReturn(offset)

        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(dummyEditorState)

        TextFieldDelegate.onRelease(
            position,
            textLayoutResult,
            processor,
            skippingOffsetMap,
            onValueChange,
            textInputService,
            dummyInputSessionToken,
            true)

        val cursorOffsetInTransformedText = offset / 2
        assertEquals(1, captor.allValues.size)
        assertEquals(1, captor.firstValue.size)
        assertTrue(captor.firstValue[0] is SetSelectionEditOp)
        val setSelectionEditOp = captor.firstValue[0] as SetSelectionEditOp
        assertEquals(cursorOffsetInTransformedText, setSelectionEditOp.start)
        assertEquals(cursorOffsetInTransformedText, setSelectionEditOp.end)
        verify(onValueChange, times(1)).invoke(eq(
            EditorValue(
                text = dummyEditorState.text,
                selection = dummyEditorState.selection
            )
        ))
    }

    @Test
    fun use_identity_mapping_if_none_visual_transformation() {
        val (visualText, offsetMap) =
            VisualTransformation.None.filter(AnnotatedString(text = "Hello, World"))

        assertEquals("Hello, World", visualText.text)
        for (i in 0..visualText.text.length) {
            // Identity mapping returns if no visual filter is provided.
            assertEquals(i, offsetMap.originalToTransformed(i))
            assertEquals(i, offsetMap.transformedToOriginal(i))
        }
    }

    @Test
    fun apply_composition_decoration() {
        val identityOffsetMap = object : OffsetMap {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }

        val input = TransformedText(
            transformedText = AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(color = Color.Red))
                append("Hello, World")
            }.toAnnotatedString(),
            offsetMap = identityOffsetMap
        )

        val result = TextFieldDelegate.applyCompositionDecoration(
            compositionRange = TextRange(3, 6),
            transformed = input
        )

        assertThat(result.transformedText.text).isEqualTo(input.transformedText.text)
        assertThat(result.transformedText.spanStyles.size).isEqualTo(2)
        assertThat(result.transformedText.spanStyles).contains(
            AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 3, 6)
        )
    }
}
