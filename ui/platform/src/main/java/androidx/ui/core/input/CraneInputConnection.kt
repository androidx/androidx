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
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import androidx.ui.input.EditorState

/**
 * An implementation of InputConnection for Crane
 *
 * The CraneInputConnection calls given client's onUpdateEditorState whenever the IME changes the
 * text buffer.
 */
internal class CraneInputConnection(
    /** The serving view */
    val view: View,
    val onUpdateEditorState: (EditorState) -> Unit,
    val onEditorActionPerformed: (Any) -> Unit,
    val onKeyEventForwarded: (Any) -> Unit,
    val mEditable: Editable
) : BaseInputConnection(view, true) {

    /**
     * The batch nest depth.
     * if this variable is 1 or larger, IME is in batch mode.
     * Do not emit any update event during batch mode.
     */
    private var mBatchDepth = 0

    /**
     * Emit onUpdateEditorState to sync up the text buffer with the client's one.
     */
    private fun syncEditState() {
        if (mBatchDepth > 0) {
            // still in batch mode. do nothing
            return
        }

        onUpdateEditorState(editable.toEditorState())
    }

    override fun getEditable(): Editable = mEditable

    override fun beginBatchEdit(): Boolean {
        mBatchDepth++
        return super.beginBatchEdit()
    }

    override fun endBatchEdit(): Boolean {
        val result = super.endBatchEdit()
        mBatchDepth--
        syncEditState()
        return result
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        val result = super.setSelection(start, end)
        syncEditState()
        return result
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        // TODO(nona): Implement send key event
        syncEditState()
        onKeyEventForwarded(event)
        return true
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        // TODO(nona): Implement editor action
        syncEditState()
        onEditorActionPerformed(actionCode)
        return true
    }
}