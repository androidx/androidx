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

package androidx.compose.foundation.text2.input.internal

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.ui.text.TextRange
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.math.max
import kotlin.math.min

@VisibleForTesting
internal const val SIC_DEBUG = false
private const val TAG = "StatelessIC"
private const val DEBUG_CLASS = "StatelessInputConnection"

private val EmptyTextFieldValue = TextFieldValue()

/**
 * An input connection that delegates its reads and writes to the active text input session in
 * [AndroidTextInputAdapter]. InputConnections are requested and used by framework to create bridge
 * from IME to an active editor.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class StatelessInputConnection(
    private val activeSessionProvider: () -> EditableTextInputSession?
) : InputConnection {
    /**
     * The depth of the batch session. 0 means no session.
     *
     * Sometimes InputConnection does not call begin/endBatchEdit functions before calling other
     * edit functions like commitText or setComposingText. StatelessInputConnection starts and
     * finishes a new artificial batch for every EditCommand to make sure that there is always
     * an ongoing batch. EditCommands are only applied when batchDepth reaches 0.
     */
    private var batchDepth: Int = 0

    /**
     * The input state from the currently active [TextInputSession] in
     * [AndroidTextInputAdapter]. Returns null if there is no active session.
     */
    private val valueOrNull: TextFieldCharSequence?
        get() = activeSessionProvider()?.value

    /**
     * The input state from the currently active [TextInputSession] in
     * [AndroidTextInputAdapter]. Returns empty TextFieldValue if there is no active session.
     */
    private val value: TextFieldCharSequence
        get() = valueOrNull ?: TextFieldCharSequence()

    /**
     * Recording of editing operations for batch editing
     */
    private val editCommands = mutableListOf<EditCommand>()

    /**
     * If this InputConnection itself is active. This value becomes false only if [closeConnection]
     * gets called.
     */
    private var isICActive: Boolean = true

    /**
     * Returns whether this input connection is still active and also executes the given lambda if
     * it is active.
     */
    private inline fun ensureActive(block: () -> Unit): Boolean {
        val combinedActive = isICActive && activeSessionProvider() != null
        return combinedActive.also {
            if (it) {
                block()
            }
        }
    }

    /**
     * Add edit op to internal list with wrapping batch edit. It's not guaranteed by IME that
     * batch editing will be used for every operation. Instead, [StatelessInputConnection] creates
     * its own mini batches for every edit op. These batches are only applied when batch depth
     * reaches 0, meaning that artificial batches won't be applied until the real batches are
     * completed.
     */
    private fun addEditCommandWithBatch(editCommand: EditCommand) {
        beginBatchEditInternal()
        try {
            editCommands.add(editCommand)
        } finally {
            endBatchEditInternal()
        }
    }

    // region Methods for batch editing and session control
    override fun beginBatchEdit(): Boolean = ensureActive {
        logDebug("beginBatchEdit()")
        return beginBatchEditInternal()
    }

    private fun beginBatchEditInternal(): Boolean {
        batchDepth++
        return true
    }

    override fun endBatchEdit(): Boolean {
        logDebug("endBatchEdit()")
        return endBatchEditInternal()
    }

    private fun endBatchEditInternal(): Boolean {
        batchDepth--
        if (batchDepth == 0 && editCommands.isNotEmpty()) {
            // apply the changes to active input session.
            activeSessionProvider()?.requestEdits(editCommands.toMutableList())
            editCommands.clear()
        }
        return batchDepth > 0
    }

    override fun closeConnection() {
        logDebug("closeConnection()")
        editCommands.clear()
        batchDepth = 0
        isICActive = false
    }

    //endregion

    // region Callbacks for text editing

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = ensureActive {
        logDebug("commitText(\"$text\", $newCursorPosition)")
        addEditCommandWithBatch(CommitTextCommand(text.toString(), newCursorPosition))
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean = ensureActive {
        logDebug("setComposingRegion($start, $end)")
        addEditCommandWithBatch(SetComposingRegionCommand(start, end))
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean =
        ensureActive {
            logDebug("setComposingText(\"$text\", $newCursorPosition)")
            addEditCommandWithBatch(SetComposingTextCommand(text.toString(), newCursorPosition))
        }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
        ensureActive {
            logDebug("deleteSurroundingTextInCodePoints($beforeLength, $afterLength)")
            addEditCommandWithBatch(
                DeleteSurroundingTextInCodePointsCommand(beforeLength, afterLength)
            )
            return true
        }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
        ensureActive {
            logDebug("deleteSurroundingText($beforeLength, $afterLength)")
            addEditCommandWithBatch(DeleteSurroundingTextCommand(beforeLength, afterLength))
            return true
        }

    override fun setSelection(start: Int, end: Int): Boolean = ensureActive {
        logDebug("setSelection($start, $end)")
        addEditCommandWithBatch(SetSelectionCommand(start, end))
        return true
    }

    override fun finishComposingText(): Boolean = ensureActive {
        logDebug("finishComposingText()")
        addEditCommandWithBatch(FinishComposingTextCommand)
        return true
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean = ensureActive {
        logDebug("sendKeyEvent($event)")
        activeSessionProvider()?.sendKeyEvent(event)
        return true
    }

    // endregion

    // region Callbacks for retrieving editing buffer info by IME

    override fun getTextBeforeCursor(maxChars: Int, flags: Int): CharSequence {
        // TODO(b/135556699) should return styled text
        val result = value.getTextBeforeSelection(maxChars).toString()
        logDebug("getTextBeforeCursor($maxChars, $flags): $result")
        return result
    }

    override fun getTextAfterCursor(maxChars: Int, flags: Int): CharSequence {
        // TODO(b/135556699) should return styled text
        val result = value.getTextAfterSelection(maxChars).toString()
        logDebug("getTextAfterCursor($maxChars, $flags): $result")
        return result
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        // https://source.chromium.org/chromium/chromium/src/+/master:content/public/android/java/src/org/chromium/content/browser/input/TextInputState.java;l=56;drc=0e20d1eb38227949805a4c0e9d5cdeddc8d23637
        val result: CharSequence? = if (value.selection.collapsed) {
            null
        } else {
            // TODO(b/135556699) should return styled text
            value.getSelectedText().toString()
        }
        logDebug("getSelectedText($flags): $result")
        return result
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = ensureActive {
        logDebug("requestCursorUpdates($cursorUpdateMode)")
        return false
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
        logDebug("getExtractedText($request, $flags)")
//        extractedTextMonitorMode = (flags and InputConnection.GET_EXTRACTED_TEXT_MONITOR) != 0
//        if (extractedTextMonitorMode) {
//            currentExtractedTextRequestToken = request?.token ?: 0
//        }
        // TODO(halilibo): Implement extracted text monitor
        // TODO(b/135556699) should return styled text
        return value.toExtractedText()
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        logDebug("getCursorCapsMode($reqModes)")
        return TextUtils.getCapsMode(value, value.selection.min, reqModes)
    }

    // endregion

    // region Editor action and Key events.

    override fun performContextMenuAction(id: Int): Boolean = ensureActive {
        logDebug("performContextMenuAction($id)")
        when (id) {
            android.R.id.selectAll -> {
                addEditCommandWithBatch(SetSelectionCommand(0, value.length))
            }
            // TODO(siyamed): Need proper connection to cut/copy/paste
            android.R.id.cut -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_CUT)
            android.R.id.copy -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_COPY)
            android.R.id.paste -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_PASTE)
            android.R.id.startSelectingText -> {} // not supported
            android.R.id.stopSelectingText -> {} // not supported
            android.R.id.copyUrl -> {} // not supported
            android.R.id.switchInputMethod -> {} // not supported
            else -> {
                // not supported
            }
        }
        return false
    }

    private fun sendSynthesizedKeyEvent(code: Int) {
        sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    override fun performEditorAction(editorAction: Int): Boolean = ensureActive {
        logDebug("performEditorAction($editorAction)")

        val imeAction = when (editorAction) {
            EditorInfo.IME_ACTION_UNSPECIFIED -> ImeAction.Default
            EditorInfo.IME_ACTION_DONE -> ImeAction.Done
            EditorInfo.IME_ACTION_SEND -> ImeAction.Send
            EditorInfo.IME_ACTION_SEARCH -> ImeAction.Search
            EditorInfo.IME_ACTION_PREVIOUS -> ImeAction.Previous
            EditorInfo.IME_ACTION_NEXT -> ImeAction.Next
            EditorInfo.IME_ACTION_GO -> ImeAction.Go
            else -> {
                logDebug("IME sent an unrecognized editor action: $editorAction")
                ImeAction.Default
            }
        }

        activeSessionProvider()?.onImeAction(imeAction)
        return true
    }

    // endregion

    // region Unsupported callbacks

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        logDebug("commitCompletion(${text?.text})")
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        // logDebug("commitCorrection($correctionInfo),autoCorrect:$autoCorrect")
        // Should add an event here so that we can implement the autocorrect highlight
        // Bug: 170647219
        // TODO(halilibo): Implement autoCorrect from ImeOptions
        return true
    }

    override fun getHandler(): Handler? {
        logDebug("getHandler()")
        return null // Returns null means using default Handler
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        logDebug("clearMetaKeyStates($states)")
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        logDebug("reportFullscreenMode($enabled)")
        return false // This value is ignored according to the API docs.
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = ensureActive {
        logDebug("performPrivateCommand($action, $data)")
        return true // API doc says we should return true even if we didn't understand the command.
    }

    override fun commitContent(
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean = ensureActive {
        logDebug("commitContent($inputContentInfo, $flags, $opts)")
        // TODO(halilibo): Support commit content in BasicTextField2
        return false
    }

    // endregion

    /**
     * Returns the text before the selection.
     *
     * @param maxChars maximum number of characters (inclusive) before the minimum value in
     * [TextFieldCharSequence.selection].
     *
     * @see TextRange.min
     */
    fun TextFieldCharSequence.getTextBeforeSelection(maxChars: Int): CharSequence =
        subSequence(max(0, selection.min - maxChars), selection.min)

    /**
     * Returns the text after the selection.
     *
     * @param maxChars maximum number of characters (exclusive) after the maximum value in
     * [TextFieldCharSequence.selection].
     *
     * @see TextRange.max
     */
    fun TextFieldCharSequence.getTextAfterSelection(maxChars: Int): CharSequence =
        subSequence(selection.max, min(selection.max + maxChars, length))

    /**
     * Returns the currently selected text.
     */
    fun TextFieldCharSequence.getSelectedText(): CharSequence =
        subSequence(selection.min, selection.max)

    private fun logDebug(message: String) {
        if (SIC_DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.$message, $isICActive, ${activeSessionProvider() != null}")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun TextFieldCharSequence.toExtractedText(): ExtractedText {
    val res = ExtractedText()
    res.text = this
    res.startOffset = 0
    res.partialEndOffset = length
    res.partialStartOffset = -1 // -1 means full text
    res.selectionStart = selection.min
    res.selectionEnd = selection.max
    res.flags = if ('\n' in this) 0 else ExtractedText.FLAG_SINGLE_LINE
    return res
}