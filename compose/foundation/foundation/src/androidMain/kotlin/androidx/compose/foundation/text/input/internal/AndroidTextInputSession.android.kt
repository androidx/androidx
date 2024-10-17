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
import android.os.CancellationSignal
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.PreviewableHandwritingGesture
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.internal.HandwritingGestureApi34.performHandwritingGesture
import androidx.compose.foundation.text.input.internal.HandwritingGestureApi34.previewHandwritingGesture
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Enable to print logs during debugging, see [logDebug]. */
@VisibleForTesting internal const val TIA_DEBUG = false
private const val TIA_TAG = "AndroidTextInputSession"

internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    updateSelectionState: (() -> Unit)?,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?,
    viewConfiguration: ViewConfiguration?
): Nothing {
    platformSpecificTextInputSession(
        state = state,
        layoutState = layoutState,
        imeOptions = imeOptions,
        receiveContentConfiguration = receiveContentConfiguration,
        onImeAction = onImeAction,
        updateSelectionState = updateSelectionState,
        composeImm = ComposeInputMethodManager(view),
        stylusHandwritingTrigger = stylusHandwritingTrigger,
        viewConfiguration = viewConfiguration
    )
}

@VisibleForTesting
internal suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    updateSelectionState: (() -> Unit)?,
    composeImm: ComposeInputMethodManager,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?,
    viewConfiguration: ViewConfiguration?
): Nothing {
    coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) {
            state.collectImeNotifications { oldValue, newValue, restartImeIfContentChanges ->
                val oldSelection = oldValue.selection
                val newSelection = newValue.selection
                val oldComposition = oldValue.composition
                val newComposition = newValue.composition

                // No need to restart the IME if there wasn't a composing region. This is useful
                // to not unnecessarily restart filtered digit only, or password fields.
                if (
                    restartImeIfContentChanges &&
                        oldValue.composition != null &&
                        !oldValue.contentEquals(newValue)
                ) {
                    composeImm.restartInput()
                } else if (oldSelection != newSelection || oldComposition != newComposition) {
                    // Don't call updateSelection if input is going to be restarted anyway
                    composeImm.updateSelection(
                        selectionStart = newSelection.min,
                        selectionEnd = newSelection.max,
                        compositionStart = newComposition?.min ?: -1,
                        compositionEnd = newComposition?.max ?: -1
                    )
                }
            }
        }

        stylusHandwritingTrigger?.let {
            launch {
                // When the editor is just focused, we need to wait for imm.startInput
                // before calling startStylusHandwriting. We need to wait for one frame
                // because TextInputService.startInput also waits for one frame before
                // actually calling imm.restartInput.
                withFrameMillis {}
                it.collect { composeImm.startStylusHandwriting() }
            }
        }

        val cursorUpdatesController =
            CursorAnchorInfoController(
                composeImm = composeImm,
                textFieldState = state,
                textLayoutState = layoutState,
                monitorScope = this,
            )

        startInputMethod { outAttrs ->
            logDebug { "createInputConnection(value=\"${state.visualText}\")" }

            val textInputSession =
                object : TextInputSession {
                    override val text: TextFieldCharSequence
                        get() = state.visualText

                    override fun requestEdit(block: TextFieldBuffer.() -> Unit) {
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

                    override fun onCommitContent(
                        transferableContent: TransferableContent
                    ): Boolean {
                        return receiveContentConfiguration?.onCommitContent(transferableContent)
                            ?: false
                    }

                    override fun requestCursorUpdates(cursorUpdateMode: Int) {
                        cursorUpdatesController.requestUpdates(cursorUpdateMode)
                    }

                    override fun performHandwritingGesture(gesture: HandwritingGesture): Int {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            return state.performHandwritingGesture(
                                gesture,
                                layoutState,
                                updateSelectionState,
                                viewConfiguration
                            )
                        }
                        return InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED
                    }

                    override fun previewHandwritingGesture(
                        gesture: PreviewableHandwritingGesture,
                        cancellationSignal: CancellationSignal?
                    ): Boolean {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            return state.previewHandwritingGesture(
                                gesture,
                                layoutState,
                                cancellationSignal
                            )
                        }
                        return false
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

/**
 * Even though [star/star] should be enough to cover all cases, some IMEs do not like when it's the
 * only mime type that's declared as supported. IMEs claim that they do not have the necessary
 * explicit information that the editor will support image or video content. Instead we also add
 * those types specifically to make sure that IMEs can send everything.
 */
private val ALL_MIME_TYPES = arrayOf("*/*", "image/*", "video/*")

private fun logDebug(tag: String = TIA_TAG, content: () -> String) {
    if (TIA_DEBUG) {
        Log.d(tag, content())
    }
}
