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

import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.internal.HandwritingGestureApi34.performHandwritingGesture
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
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
    onImeAction: ((ImeAction) -> Unit)?,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?
): Nothing {
    platformSpecificTextInputSession(
        state = state,
        layoutState = layoutState,
        imeOptions = imeOptions,
        receiveContentConfiguration = receiveContentConfiguration,
        onImeAction = onImeAction,
        composeImm = ComposeInputMethodManager(view),
        stylusHandwritingTrigger = stylusHandwritingTrigger,
    )
}

@VisibleForTesting
internal suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    composeImm: ComposeInputMethodManager,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?
): Nothing {
    coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) {
            state.collectImeNotifications { oldValue, newValue, restartImeIfContentChanges ->
                val oldSelection = oldValue.selection
                val newSelection = newValue.selection
                val oldComposition = oldValue.composition
                val newComposition = newValue.composition

                if ((oldSelection != newSelection) || oldComposition != newComposition) {
                    composeImm.updateSelection(
                        selectionStart = newSelection.min,
                        selectionEnd = newSelection.max,
                        compositionStart = newComposition?.min ?: -1,
                        compositionEnd = newComposition?.max ?: -1
                    )
                }

                // No need to restart the IME if keyboard type is configured as Password. IME
                // should not keep an internal input state if the content needs to be secured.
                if (restartImeIfContentChanges &&
                    !oldValue.contentEquals(newValue) &&
                    imeOptions.keyboardType != KeyboardType.Password
                ) {
                    composeImm.restartInput()
                }
            }
        }

        stylusHandwritingTrigger?.let {
            launch(start = CoroutineStart.UNDISPATCHED) {
                it.collect { composeImm.startStylusHandwriting() }
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

                override fun requestEdit(block: EditingBuffer.() -> Unit) {
                    state.editUntransformedTextAsUser(
                        restartImeIfContentChanges = false,
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

                override fun performHandwritingGesture(gesture: HandwritingGesture): Int {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        return state.performHandwritingGesture(gesture, layoutState)
                    }
                    return InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED
                }
            }

            outAttrs.update(
                text = state.visualText,
                selection = state.visualText.selection,
                imeOptions = imeOptions,
                // only pass AllMimeTypes if we have a ReceiveContentConfiguration.
                contentMimeTypes = receiveContentConfiguration?.let { ALL_MIME_TYPES }
            )
            StatelessInputConnection(textInputSession, outAttrs)
        }
    }
}

private val ALL_MIME_TYPES = arrayOf("*/*")

private fun logDebug(tag: String = TIA_TAG, content: () -> String) {
    if (TIA_DEBUG) {
        Log.d(tag, content())
    }
}
