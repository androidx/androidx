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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input.internal

import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.view.inputmethod.EditorInfoCompat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.annotations.TestOnly

/** Enable to print logs during debugging, see [logDebug]. */
@VisibleForTesting
internal const val TIA_DEBUG = false
private const val TAG = "AndroidTextInputSession"

private var inputConnectionCreatedListener: ((EditorInfo, InputConnection) -> Unit)? = null

@TestOnly
@VisibleForTesting
internal fun setInputConnectionCreatedListenerForTests(
    listener: ((EditorInfo, InputConnection) -> Unit)?
) {
    inputConnectionCreatedListener = { info, connection -> listener?.invoke(info, connection) }
}

internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TextFieldState,
    imeOptions: ImeOptions,
    filter: TextEditFilter?,
    onImeAction: ((ImeAction) -> Unit)?
): Nothing {
    val composeImm = ComposeInputMethodManager(view)

    coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) {
            state.editProcessor.collectResets { old, new ->
                val needUpdateSelection =
                    (old.selectionInChars != new.selectionInChars) ||
                        old.compositionInChars != new.compositionInChars
                if (needUpdateSelection) {
                    composeImm.updateSelection(
                        selectionStart = new.selectionInChars.min,
                        selectionEnd = new.selectionInChars.max,
                        compositionStart = new.compositionInChars?.min ?: -1,
                        compositionEnd = new.compositionInChars?.max ?: -1
                    )
                }

                if (!old.contentEquals(new)) {
                    composeImm.restartInput()
                }
            }
        }

        startInputMethod { outAttrs ->
            logDebug { "createInputConnection(value=\"${state.text}\")" }

            val textInputSession = object : EditableTextInputSession {
                override val value: TextFieldCharSequence
                    get() = state.text

                override fun requestEdits(editCommands: List<EditCommand>) {
                    state.editProcessor.update(editCommands, filter)
                }

                override fun sendKeyEvent(keyEvent: KeyEvent) {
                    composeImm.sendKeyEvent(keyEvent)
                }

                override val imeOptions: ImeOptions
                    get() = imeOptions

                override fun onImeAction(imeAction: ImeAction) {
                    onImeAction?.invoke(imeAction)
                }

                override val isOpen: Boolean
                    get() = true

                override fun dispose() {
                    // noop
                }
            }

            outAttrs.update(state.text, textInputSession.imeOptions)
            StatelessInputConnection { textInputSession }.also {
                inputConnectionCreatedListener?.invoke(outAttrs, it)
            }
        }
    }
}

/**
 * Fills necessary info of EditorInfo.
 */
internal fun EditorInfo.update(textFieldValue: TextFieldCharSequence, imeOptions: ImeOptions) {
    this.imeOptions = when (imeOptions.imeAction) {
        ImeAction.Default -> {
            if (imeOptions.singleLine) {
                // this is the last resort to enable single line
                // Android IME still shows return key even if multi line is not send
                // TextView.java#onCreateInputConnection
                EditorInfo.IME_ACTION_DONE
            } else {
                EditorInfo.IME_ACTION_UNSPECIFIED
            }
        }

        ImeAction.None -> EditorInfo.IME_ACTION_NONE
        ImeAction.Go -> EditorInfo.IME_ACTION_GO
        ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
        ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
        ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
        ImeAction.Send -> EditorInfo.IME_ACTION_SEND
        ImeAction.Done -> EditorInfo.IME_ACTION_DONE
        else -> error("invalid ImeAction")
    }

    this.inputType = when (imeOptions.keyboardType) {
        KeyboardType.Text -> InputType.TYPE_CLASS_TEXT
        KeyboardType.Ascii -> {
            this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
            InputType.TYPE_CLASS_TEXT
        }

        KeyboardType.Number ->
            InputType.TYPE_CLASS_NUMBER

        KeyboardType.Phone ->
            InputType.TYPE_CLASS_PHONE

        KeyboardType.Uri ->
            InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI

        KeyboardType.Email ->
            InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        KeyboardType.Password ->
            InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD

        KeyboardType.NumberPassword ->
            InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD

        KeyboardType.Decimal ->
            InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL

        else -> error("Invalid Keyboard Type")
    }

    if (!imeOptions.singleLine) {
        if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
            // TextView.java#setInputTypeSingleLine
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE

            if (imeOptions.imeAction == ImeAction.Default) {
                this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
            }
        }
    }

    if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
        when (imeOptions.capitalization) {
            KeyboardCapitalization.Characters -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }

            KeyboardCapitalization.Words -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }

            KeyboardCapitalization.Sentences -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }

            else -> {
                /* do nothing */
            }
        }

        if (imeOptions.autoCorrect) {
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }
    }

    this.initialSelStart = textFieldValue.selectionInChars.start
    this.initialSelEnd = textFieldValue.selectionInChars.end

    EditorInfoCompat.setInitialSurroundingText(this, textFieldValue)

    this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
}

/**
 * Adds [resetListener] to this [EditProcessor] and then suspends until cancelled, removing the
 * listener before continuing.
 */
private suspend inline fun EditProcessor.collectResets(
    resetListener: EditProcessor.ResetListener
): Nothing {
    suspendCancellableCoroutine<Nothing> { continuation ->
        addResetListener(resetListener)
        continuation.invokeOnCancellation {
            removeResetListener(resetListener)
        }
    }
}

private fun hasFlag(bits: Int, flag: Int): Boolean = (bits and flag) == flag

private fun logDebug(tag: String = TAG, content: () -> String) {
    if (TIA_DEBUG) {
        Log.d(tag, content())
    }
}
