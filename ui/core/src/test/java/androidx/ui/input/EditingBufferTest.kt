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

package androidx.ui.input

import androidx.test.filters.SmallTest
import androidx.ui.core.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class EditingBufferTest {
    @Test
    fun test_insert() {
        val eb = EditingBuffer("", TextRange(0, 0))

        eb.replace(0, 0, "A")

        assertEquals("A", eb.toString())
        assertEquals(1, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Keep inserting text to the end of string. Cursor should follow.
        eb.replace(1, 1, "BC")
        assertEquals("ABC", eb.toString())
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Insert into middle position. Cursor should be end of inserted text.
        eb.replace(1, 1, "D")
        assertEquals("ADBC", eb.toString())
        assertEquals(2, eb.selectionStart)
        assertEquals(2, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_delete() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 0))

        eb.replace(0, 1, "")

        // Delete the left character at the cursor.
        assertEquals("BCDE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(0, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Delete the text before the cursor
        eb.replace(0, 2, "")
        assertEquals("DE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(0, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Delete end of the text.
        eb.replace(1, 2, "")
        assertEquals("D", eb.toString())
        assertEquals(1, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_setSelection() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 3))
        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.setSelection(0, 5) // Change the selection
        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(5, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.replace(0, 3, "X") // replace function cancel the selection and place cursor.
        assertEquals("XDE", eb.toString())
        assertEquals(1, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.setSelection(0, 2) // Set the selection again
        assertEquals("XDE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(2, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_setCompostion_and_clearComposition() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 0))

        eb.setComposition(0, 5) // Make all text as composition
        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(0, eb.selectionEnd)
        assertEquals(0, eb.compositionStart)
        assertEquals(5, eb.compositionEnd)

        eb.replace(2, 3, "X") // replace function cancel the composition text.
        assertEquals("ABXDE", eb.toString())
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.setComposition(2, 4) // set composition again
        assertEquals("ABXDE", eb.toString())
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertEquals(2, eb.compositionStart)
        assertEquals(4, eb.compositionEnd)

        eb.clearComposition() // clear the composition
        assertEquals("ABXDE", eb.toString())
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }
}
