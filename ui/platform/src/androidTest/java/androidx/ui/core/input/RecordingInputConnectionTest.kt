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

package androidx.ui.core.input

import androidx.test.filters.SmallTest
import androidx.ui.core.TextRange
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RecordingInputConnectionTest {

    private lateinit var ic: RecordingInputConnection
    private lateinit var listener: InputEventListener

    @Before
    fun setup() {
        listener = mock()
        ic = RecordingInputConnection(listener)
    }

    @Test
    fun getTextBeforeAndAfterCursorTest() {
        assertEquals("", ic.getTextBeforeCursor(100, 0))
        assertEquals("", ic.getTextAfterCursor(100, 0))

        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(0, 0))

        assertEquals("", ic.getTextBeforeCursor(100, 0))
        assertEquals("Hello, World", ic.getTextAfterCursor(100, 0))

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(1, 1))

        assertEquals("H", ic.getTextBeforeCursor(100, 0))
        assertEquals("ello, World", ic.getTextAfterCursor(100, 0))

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(12, 12))

        assertEquals("Hello, World", ic.getTextBeforeCursor(100, 0))
        assertEquals("", ic.getTextAfterCursor(100, 0))
    }

    @Test
    fun getTextBeforeAndAfterCursorTest_maxCharTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(0, 0))

        assertEquals("", ic.getTextBeforeCursor(5, 0))
        assertEquals("Hello", ic.getTextAfterCursor(5, 0))

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(1, 1))

        assertEquals("H", ic.getTextBeforeCursor(5, 0))
        assertEquals("ello,", ic.getTextAfterCursor(5, 0))

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(12, 12))

        assertEquals("World", ic.getTextBeforeCursor(5, 0))
        assertEquals("", ic.getTextAfterCursor(5, 0))
    }

    @Test
    fun getSelectedTextTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(0, 0))

        assertEquals("", ic.getSelectedText(0))

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(0, 1))

        assertEquals("H", ic.getSelectedText(0))

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.inputState = InputState(
            text = "Hello, World",
            selection = TextRange(0, 12))

        assertEquals("Hello, World", ic.getSelectedText(0))
    }

    @Test
    fun commitTextTest() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.inputState = InputState(text = "", selection = TextRange(0, 0))

        // Inserting "Hello, " into the empty text field.
        assertTrue(ic.commitText("Hello, ", 1))

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(CommitTextEditOp("Hello, ", 1), editOps[0])
    }

    @Test
    fun commitTextTest_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.inputState = InputState(text = "", selection = TextRange(0, 0))

        // IME set text "Hello, World." with two commitText API within the single batch session.
        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.commitText("Hello, ", 1))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.commitText("World.", 1))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(CommitTextEditOp("Hello, ", 1), editOps[0])
        assertEquals(CommitTextEditOp("World.", 1), editOps[1])
    }
}