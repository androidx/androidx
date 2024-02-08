/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text.input.internal

import android.util.Log
import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Enable to print logs during debugging, see [logDebug]. */
@VisibleForTesting
internal const val TIA_DEBUG = false
private const val TIA_TAG = "AndroidTextInputSession"

internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?
): Nothing {
    platformSpecificTextInputSession(
        state = state,
        layoutState = layoutState,
        imeOptions = imeOptions,
        receiveContentConfiguration = receiveContentConfiguration,
        onImeAction = onImeAction,
        composeImm = ComposeInputMethodManager(view)
    )
}

@VisibleForTesting
internal suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    composeImm: ComposeInputMethodManager
): Nothing {
    coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) {
            state.collectImeNotifications { old, new ->
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

                // No need to restart the IME if keyboard type is configured as Password. IME
                // should not keep an internal input state if the content needs to be secured.
                if (!old.contentEquals(new) && imeOptions.keyboardType != KeyboardType.Password) {
                    composeImm.restartInput()
                }
            }
        }

        val cursorUpdatesController = CursorAnchorInfoController(
            composeImm = composeImm,
            textFieldState = state,
            textLayoutState = layoutState,
            monitorScope = this,
        )

        startInputMethod { outAttrs ->
            logDebug { "createInputConnection(value=\"${state.visualText}\")" }

            val textInputSession = object : TextInputSession {
                override val text: TextFieldCharSequence
                    get() = state.visualText

                override fun requestEdit(
                    notifyImeOfChanges: Boolean,
                    block: EditingBuffer.() -> Unit
                ) {
                    state.editUntransformedTextAsUser(
                        notifyImeOfChanges = notifyImeOfChanges,
                        block = block
                    )
                }

                override fun sendKeyEvent(keyEvent: KeyEvent) {
                    composeImm.sendKeyEvent(keyEvent)
                }

                override fun onImeAction(imeAction: ImeAction) {
                    onImeAction?.invoke(imeAction)
                }

                override fun onCommitContent(transferableContent: TransferableContent): Boolean {
                    return receiveContentConfiguration?.onCommitContent(transferableContent)
                        ?: false
                }

                override fun requestCursorUpdates(cursorUpdateMode: Int) {
                    cursorUpdatesController.requestUpdates(cursorUpdateMode)
                }
            }

            val hintMediaTypes = receiveContentConfiguration?.hintMediaTypes
            val contentMimeTypes: Array<String>? =
                if (!hintMediaTypes.isNullOrEmpty()) {
                    val arr = Array(hintMediaTypes.size) { "" }
                    hintMediaTypes.forEachIndexed { i, mediaType ->
                        arr[i] = mediaType.representation
                    }
                    arr
                } else {
                    null
                }

            outAttrs.update(
                text = state.visualText,
                selection = state.visualText.selectionInChars,
                imeOptions = imeOptions,
                contentMimeTypes = contentMimeTypes
            )
            StatelessInputConnection(textInputSession, outAttrs)
        }
    }
}

private fun logDebug(tag: String = TIA_TAG, content: () -> String) {
    if (TIA_DEBUG) {
        Log.d(tag, content())
    }
}
