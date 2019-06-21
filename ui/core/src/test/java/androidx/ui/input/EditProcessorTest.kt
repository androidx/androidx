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
class EditProcessorTest {

    @Test
    fun test_new_state_and_edit_commands() {
        val proc = EditProcessor()

        proc.onNewState(EditorState("ABCDE", TextRange(0, 0)))

        val newState = proc.onEditCommands(listOf(
            CommitTextEditOp("X", 1)
        ))

        assertEquals("XABCDE", newState.text)
        assertEquals(1, newState.selection.start)
        assertEquals(1, newState.selection.end)
    }
}