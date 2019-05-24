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

import android.text.Editable
import android.view.View
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.TextRange
import androidx.ui.input.EditorState
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class CraneInputConnectionTest {

    internal lateinit var onUpdateEditorState: (EditorState) -> Unit
    internal lateinit var editable: Editable
    internal lateinit var ic: CraneInputConnection

    @Before
    fun setup() {
        onUpdateEditorState = mock()
        editable = Editable.Factory.getInstance().newEditable("")
        ic = CraneInputConnection(
            view = View(InstrumentationRegistry.getInstrumentation().targetContext),
            onUpdateEditorState = onUpdateEditorState,
            onEditorActionPerformed = {},
            onKeyEventForwarded = {},
            mEditable = editable
        )
    }

    @Test
    fun onUpdateEditorStateCalled_NeverCalled_by_creation() {
        // Creating input connection should not fire onUpdateEditorState
        verify(onUpdateEditorState, never()).invoke(any())
    }

    @Test
    fun onUpdateEditorStateCalled_called_by_commitText() {
        ic.commitText("H", 1)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(text = "H", selection = TextRange(1, 1), composition = null)
        )

        ic.commitText("e", 2)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(text = "He", selection = TextRange(2, 2), composition = null)
        )

        ic.commitText("llo", 5)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello",
                selection = TextRange(5, 5),
                composition = null
            )
        )
    }

    @Test
    fun onUpdateEditorStateCalled_called_by_setComposingText() {
        ic.setComposingText("H", 1)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "H", selection = TextRange(1, 1),
                composition = TextRange(0, 1)
            )
        )

        ic.setComposingText("He", 2)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "He", selection = TextRange(2, 2),
                composition = TextRange(0, 2)
            )
        )

        ic.setComposingText("Hello", 5)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello", selection = TextRange(5, 5),
                composition = TextRange(0, 5)
            )
        )
    }

    @Test
    fun onUpdateEditorStateCalled_called_by_setComposingRegin() {
        ic.commitText("Hello, World", 12)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(12, 12),
                composition = null
            )
        )

        ic.setComposingRegion(0, 12)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(12, 12),
                composition = TextRange(0, 12)
            )
        )

        ic.setComposingRegion(7, 12)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(12, 12),
                composition = TextRange(7, 12)
            )
        )
    }

    @Test
    fun onUpdateEditorStateCalled_called_by_deleteSurroundingText() {
        ic.commitText("Hello, World", 12)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(12, 12),
                composition = null
            )
        )

        ic.setSelection(7, 7)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(7, 7),
                composition = null
            )
        )

        ic.deleteSurroundingText(7, 0)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "World", selection = TextRange(0, 0),
                composition = null
            )
        )

        ic.deleteSurroundingText(0, 5)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(text = "", selection = TextRange(0, 0), composition = null)
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun onUpdateEditorStateCalled_called_by_deleteSurroundingTextInCodePoitns() {
        ic.commitText("Hello, World", 12)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(12, 12),
                composition = null
            )
        )

        ic.setSelection(7, 7)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(7, 7),
                composition = null
            )
        )

        ic.deleteSurroundingTextInCodePoints(7, 0)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "World", selection = TextRange(0, 0),
                composition = null
            )
        )

        ic.deleteSurroundingTextInCodePoints(0, 5)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(text = "", selection = TextRange(0, 0), composition = null)
        )
    }

    @Test
    fun onUpdateEditorStateCalled_called_by_finishComposingText() {
        ic.setComposingText("H", 1)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "H", selection = TextRange(1, 1),
                composition = TextRange(0, 1)
            )
        )

        ic.finishComposingText()
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "H", selection = TextRange(1, 1),
                composition = null
            )
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun onUpdateEditorStateCalled_called_by_closeConnection() {
        ic.setComposingText("H", 1)
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "H", selection = TextRange(1, 1),
                composition = TextRange(0, 1)
            )
        )

        ic.closeConnection()
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "H", selection = TextRange(1, 1),
                composition = null
            )
        )
    }

    @Test
    fun onUpdateEditorStateCalled_never_called_during_batchEdit() {
        ic.beginBatchEdit()
        verify(onUpdateEditorState, never()).invoke(any())

        ic.commitText("Hello, ", 7)
        verify(onUpdateEditorState, never()).invoke(any())

        ic.setComposingText("World", 12)
        verify(onUpdateEditorState, never()).invoke(any())

        ic.endBatchEdit()
        verify(onUpdateEditorState, times(1)).invoke(
            EditorState(
                text = "Hello, World", selection = TextRange(12, 12),
                composition = TextRange(7, 12)
            )
        )
    }
}