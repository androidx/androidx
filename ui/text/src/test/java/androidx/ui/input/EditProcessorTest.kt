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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class EditProcessorTest {

    @Test
    fun test_new_state_and_edit_commands() {
        val proc = EditProcessor()
        val tis: TextInputService = mock()

        proc.onNewState(EditorState("ABCDE", TextRange(0, 0)), tis)
        val captor = argumentCaptor<EditorState>()
        verify(tis, times(1)).onStateUpdated(captor.capture())
        assertEquals(1, captor.allValues.size)
        assertEquals("ABCDE", captor.firstValue.text)
        assertEquals(0, captor.firstValue.selection.start)
        assertEquals(0, captor.firstValue.selection.end)

        reset(tis)
        val newState = proc.onEditCommands(listOf(
            CommitTextEditOp("X", 1)
        ))

        assertEquals("XABCDE", newState.text)
        assertEquals(1, newState.selection.start)
        assertEquals(1, newState.selection.end)
        // onEditCommands should not fire onStateUpdated since need to pass it to developer first.
        verify(tis, never()).onStateUpdated(any())
    }
}