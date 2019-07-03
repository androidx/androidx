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

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.ui.core.TextRange
import androidx.ui.input.BackspaceKeyEditOp
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.DeleteSurroundingTextEditOp
import androidx.ui.input.DeleteSurroundingTextInCodePointsEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.FinishComposingTextEditOp
import androidx.ui.input.InputEventListener
import androidx.ui.input.MoveCursorEditOp
import androidx.ui.input.SetComposingRegionEditOp
import androidx.ui.input.SetComposingTextEditOp
import androidx.ui.input.SetSelectionEditOp

private val DEBUG = false
private val TAG = "RecordingIC"

internal class RecordingInputConnection(
    /**
     * An input event listener.
     */
    val eventListener: InputEventListener
) : InputConnection {

    // The depth of the batch session. 0 means no session.
    private var batchDepth: Int = 0

    // The input state.
    var inputState: InputState = InputState("", TextRange(0, 0), null)
        set(value) {
            if (DEBUG) { Log.d(TAG, "New InputState has set: $inputState") }
            field = value
        }

    // The recoding editing ops.
    private val editOps = mutableListOf<EditOperation>()

    // Add edit op to internal list with wrapping batch edit.
    private fun addEditOpWithBatch(editOp: EditOperation) {
        beginBatchEdit()
        try {
            editOps.add(editOp)
        } finally {
            endBatchEdit()
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Callbacks for text editing session
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun beginBatchEdit(): Boolean {
        if (DEBUG) { Log.d(TAG, "beginBatchEdit()") }
        batchDepth++
        return true
    }

    override fun endBatchEdit(): Boolean {
        if (DEBUG) { Log.d(TAG, "endBatchEdit()") }
        batchDepth--
        if (batchDepth == 0) {
            eventListener.onEditOperations(editOps.toList())
            editOps.clear()
        }
        return batchDepth > 0
    }

    override fun closeConnection() {
        if (DEBUG) { Log.d(TAG, "closeConnection()") }
        editOps.clear()
        batchDepth = 0
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Callbacks for text editing
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "commitText($text, $newCursorPosition)") }
        addEditOpWithBatch(CommitTextEditOp(text.toString(), newCursorPosition))
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "setComposingRegion($start, $end)") }
        addEditOpWithBatch(SetComposingRegionEditOp(start, end))
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "setComposingText($text, $newCursorPosition)") }
        addEditOpWithBatch(SetComposingTextEditOp(text.toString(), newCursorPosition))
        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "deleteSurroundingTextInCodePoints($beforeLength, $afterLength)") }
        addEditOpWithBatch(DeleteSurroundingTextInCodePointsEditOp(beforeLength, afterLength))
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "deleteSurroundingText($beforeLength, $afterLength)") }
        addEditOpWithBatch(DeleteSurroundingTextEditOp(beforeLength, afterLength))
        return true
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "setSelection($start, $end)") }
        addEditOpWithBatch(SetSelectionEditOp(start, end))
        return true
    }

    override fun finishComposingText(): Boolean {
        if (DEBUG) { Log.d(TAG, "finishComposingText()") }
        addEditOpWithBatch(FinishComposingTextEditOp())
        return true
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        if (DEBUG) { Log.d(TAG, "sendKeyEvent($event)") }
        if (event.action != KeyEvent.ACTION_DOWN) {
            return true // Only interested in KEY_DOWN event.
        }

        val op = when (event.keyCode) {
            KeyEvent.KEYCODE_DEL -> BackspaceKeyEditOp()
            KeyEvent.KEYCODE_DPAD_LEFT -> MoveCursorEditOp(-1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> MoveCursorEditOp(1)
            else -> null
        }

        op?.let { addEditOpWithBatch(it) }
        return true
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Callbacks for retrieving editing buffers
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun getTextBeforeCursor(maxChars: Int, flags: Int): CharSequence {
        if (DEBUG) { Log.d(TAG, "getTextBeforeCursor($maxChars, $flags)") }
        return inputState.getTextBeforeSelection(maxChars)
    }

    override fun getTextAfterCursor(maxChars: Int, flags: Int): CharSequence {
        if (DEBUG) { Log.d(TAG, "getTextAfterCursor($maxChars, $flags)") }
        return inputState.getTextAfterSelection(maxChars)
    }

    override fun getSelectedText(flags: Int): CharSequence {
        if (DEBUG) { Log.d(TAG, "getSelectedText($flags)") }
        return inputState.getSelectedText()
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "requestCursorUpdates($cursorUpdateMode)") }
        TODO("not implemented")
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
        if (DEBUG) { Log.d(TAG, "getExtractedText($request, $flags)") }
        TODO("not implemented")
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Editor action and Key events.
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun performContextMenuAction(id: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "performContextMenuAction($id)") }
        TODO("not implemented")
    }

    override fun performEditorAction(editorAction: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "performEditorAction($editorAction)") }
        TODO("not implemented")
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Unsupported callbacks
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        if (DEBUG) { Log.d(TAG, "commitCompletion(${text?.text})") }
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        if (DEBUG) { Log.d(TAG, "commitCorrection($correctionInfo)") }
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun getHandler(): Handler? {
        if (DEBUG) { Log.d(TAG, "getHandler()") }
        return null // Returns null means using default Handler
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        if (DEBUG) { Log.d(TAG, "clearMetaKeyStates($states)") }
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        if (DEBUG) { Log.d(TAG, "reportFullscreenMode($enabled)") }
        return false // This value is ignored according to the API docs.
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        if (DEBUG) { Log.d(TAG, "getCursorCapsMode($reqModes)") }
        return TextUtils.getCapsMode(inputState.text, inputState.selection.start, reqModes)
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        if (DEBUG) { Log.d(TAG, "performPrivateCommand($action, $data)") }
        return true // API doc says we should return true even if we didn't understand the command.
    }

    override fun commitContent(
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        if (DEBUG) { Log.d(TAG, "commitContent($inputContentInfo, $flags, $opts)") }
        return false // We don't accept any contents.
    }
}