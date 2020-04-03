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

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.ui.geometry.Rect
import androidx.ui.text.TextRange
import kotlin.math.roundToInt

/**
 * Provide Android specific input service with the Operating System.
 */
internal class TextInputServiceAndroid(val view: View) : PlatformTextInputService {
    /** True if the currently editable composable has connected */
    private var editorHasFocus = false

    /**
     *  The following three observers are set when the editable composable has initiated the input
     *  session
     */
    private var onEditCommand: (List<EditOperation>) -> Unit = {}
    private var onImeActionPerformed: (ImeAction) -> Unit = {}

    private var state = EditorValue(text = "", selection = TextRange(0, 0))
    private var keyboardType = KeyboardType.Text
    private var imeAction = ImeAction.Unspecified
    private var ic: RecordingInputConnection? = null

    /**
     * The editable buffer used for BaseInputConnection.
     */
    private lateinit var imm: InputMethodManager

    /**
     * Creates new input connection.
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!editorHasFocus) {
            return null
        }
        fillEditorInfo(keyboardType, imeAction, outAttrs)

        return RecordingInputConnection(
            initState = state,
            eventListener = object : InputEventListener {
                override fun onEditOperations(editOps: List<EditOperation>) {
                    onEditCommand(editOps)
                }

                override fun onImeAction(imeAction: ImeAction) {
                    onImeActionPerformed(imeAction)
                }
            }
        ).also { ic = it }
    }

    /**
     * Returns true if some editable component is focused.
     */
    fun isEditorFocused(): Boolean = editorHasFocus

    override fun startInput(
        initModel: EditorValue,
        keyboardType: KeyboardType,
        imeAction: ImeAction,
        onEditCommand: (List<EditOperation>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        editorHasFocus = true
        state = initModel
        this.keyboardType = keyboardType
        this.imeAction = imeAction
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed

        view.requestFocus()
        view.post {
            imm.restartInput(view)
            imm.showSoftInput(view, 0)
        }
    }

    override fun stopInput() {
        editorHasFocus = false
        onEditCommand = {}
        onImeActionPerformed = {}

        imm.restartInput(view)
        editorHasFocus = false
    }

    override fun showSoftwareKeyboard() {
        imm.showSoftInput(view, 0)
    }

    override fun hideSoftwareKeyboard() {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onStateUpdated(model: EditorValue) {
        this.state = model
        ic?.updateInputState(this.state, imm, view)
    }

    override fun notifyFocusedRect(rect: Rect) {
        view.requestRectangleOnScreen(android.graphics.Rect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt()))
    }

    /**
     * Fills necessary info of EditorInfo.
     */
    private fun fillEditorInfo(
        keyboardType: KeyboardType,
        imeAction: ImeAction,
        outInfo: EditorInfo
    ) {
        outInfo.imeOptions = when (imeAction) {
            ImeAction.Unspecified -> EditorInfo.IME_ACTION_UNSPECIFIED
            ImeAction.NoAction -> EditorInfo.IME_ACTION_NONE
            ImeAction.Go -> EditorInfo.IME_ACTION_GO
            ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
            ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
            ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
            ImeAction.Send -> EditorInfo.IME_ACTION_SEND
            ImeAction.Done -> EditorInfo.IME_ACTION_DONE
            else -> throw IllegalArgumentException("Unknown ImeAction: $imeAction")
        }
        when (keyboardType) {
            KeyboardType.Text -> outInfo.inputType = InputType.TYPE_CLASS_TEXT
            KeyboardType.Ascii -> {
                outInfo.inputType = InputType.TYPE_CLASS_TEXT
                outInfo.imeOptions = outInfo.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
            }
            KeyboardType.Number -> outInfo.inputType = InputType.TYPE_CLASS_NUMBER
            KeyboardType.Phone -> outInfo.inputType = InputType.TYPE_CLASS_PHONE
            KeyboardType.Uri ->
                outInfo.inputType = InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
            KeyboardType.Email ->
                outInfo.inputType =
                    InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            KeyboardType.Password -> {
                outInfo.inputType =
                    InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            }
            KeyboardType.NumberPassword -> {
                outInfo.inputType =
                        InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
            }
            else -> throw IllegalArgumentException("Unknown KeyboardType: $keyboardType")
        }
        outInfo.imeOptions =
            outInfo.imeOptions or outInfo.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
    }
}