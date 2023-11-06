/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal.undo

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.autoSaver
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test

class UndoManagerSaverTest {

    @Test
    fun savesAndRestoresTextAndSelection() {
        val undoManager = UndoManager<Int>()

        undoManager.record(1)
        undoManager.record(2)
        undoManager.record(3)

        undoManager.undo()

        // undoStack; 1-2 redoStack; 3

        val saver = UndoManager.createSaver(autoSaver<Int>())
        val saved = with(saver) {
            TestSaverScope.save(undoManager)
        }
        assertNotNull(saved)
        val restoredState = saver.restore(saved)

        assertNotNull(restoredState)
        assertThat(restoredState.canUndo).isTrue()
        assertThat(restoredState.canRedo).isTrue()

        var redoValue = undoManager.redo()

        assertThat(redoValue).isEqualTo(3)

        val undoValues = mutableListOf<Int>()
        while (undoManager.canUndo) {
            undoValues += undoManager.undo()
        }

        assertThat(undoValues).containsExactly(3, 2, 1)
    }

    private object TestSaverScope : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }
}
