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

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.ui.text.TextRange
import androidx.ui.input.EditOperation
import androidx.ui.input.EditorState
import androidx.ui.input.InputEventListener
import androidx.ui.input.KeyboardType
import androidx.ui.input.TextInputService

/**
 * Provide Android specific input service with the Operating System.
 */
internal class TextInputServiceAndroid(val view: View) : TextInputService {
    /** True if the currently editable widget has connected */
    private var editorHasFocus = false

    /**
     *  The following three observers are set when the editable widget has initiated the input
     *  session
     */
    private var onEditCommand: (List<EditOperation>) -> Unit = {}
    private var onEditorActionPerformed: (Any) -> Unit = {}

    private var state = InputState(text = "", selection = TextRange(0, 0))
    private var keyboardType = KeyboardType.Text
    private var ic: RecordingInputConnection? = null

    /**
     * The editable buffer used for BaseInputConnection.
     */
    private val imm =
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    /**
     * Creates new input connection.
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!editorHasFocus) {
            return null
        }
        fillEditorInfo(keyboardType, outAttrs)

        return RecordingInputConnection(
            initState = state,
            eventListener = object : InputEventListener {
                override fun onEditOperations(editOps: List<EditOperation>) {
                    onEditCommand(editOps)
                }
            }
        ).also { ic = it }
    }

    /**
     * Returns true if some editable component is focused.
     */
    fun isEditorFocused(): Boolean = editorHasFocus

    override fun startInput(
        initState: EditorState,
        keyboardType: KeyboardType,
        onEditCommand: (List<EditOperation>) -> Unit,
        onEditorActionPerformed: (Any) -> Unit
    ) {
        editorHasFocus = true
        state = initState.toInputState()
        this.keyboardType = keyboardType
        this.onEditCommand = onEditCommand
        this.onEditorActionPerformed = onEditorActionPerformed

        view.requestFocus()
        view.post {
            imm.restartInput(view)
            imm.showSoftInput(view, 0)
        }
    }

    override fun stopInput() {
        editorHasFocus = false
        onEditCommand = {}
        onEditorActionPerformed = {}

        imm.restartInput(view)
    }

    override fun showSoftwareKeyboard() {
        imm.showSoftInput(view, 0)
    }

    override fun onStateUpdated(state: EditorState) {
        this.state = state.toInputState()
        ic?.updateInputState(this.state, imm, view)
    }

    /**
     * Fills necessary info of EditorInfo.
     */
    private fun fillEditorInfo(keyboardType: KeyboardType, outInfo: EditorInfo) {
        when (keyboardType) {
            KeyboardType.Text -> outInfo.inputType = InputType.TYPE_CLASS_TEXT
            KeyboardType.ASCII -> {
                outInfo.inputType = InputType.TYPE_CLASS_TEXT
                outInfo.imeOptions = EditorInfo.IME_FLAG_FORCE_ASCII
            }
            KeyboardType.Number -> outInfo.inputType = InputType.TYPE_CLASS_NUMBER
            KeyboardType.Phone -> outInfo.inputType = InputType.TYPE_CLASS_PHONE
            KeyboardType.URI ->
                outInfo.inputType = InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
            KeyboardType.Email ->
                outInfo.inputType =
                    InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            else -> throw IllegalArgumentException("Unknown KeyboardType: $keyboardType")
        }
        outInfo.imeOptions = outInfo.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
    }
}

private fun EditorState.toInputState(): InputState =
    InputState(
        text = text, // TODO(nona): call toString once AnnotatedString is in use.
        selection = selection,
        composition = composition)
