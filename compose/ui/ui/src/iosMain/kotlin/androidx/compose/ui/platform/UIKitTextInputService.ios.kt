/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.text.input.ComposeTextInputConnection
import androidx.compose.ui.text.input.NativeTextInputConnection
import androidx.compose.ui.text.input.SelectionContainerConnection
import androidx.compose.ui.text.input.TextInputConnection
import androidx.compose.ui.text.input.stateSnapshot
import androidx.compose.ui.text.input.usingNativeTextInput
import androidx.compose.ui.window.FocusedViewsList
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIView

@OptIn(ExperimentalComposeUiApi::class)
internal class UIKitTextInputService(
    private var updateView: () -> Unit,
    private val view: UIView,
    private val viewConfiguration: ViewConfiguration,
    private val focusedViewsList: FocusedViewsList?,
    private var onInputStarted: () -> Unit,
    private var focusManager: () -> ComposeSceneFocusManager?,
    coroutineContext: CoroutineContext
) {

    private val coroutineScope = CoroutineScope(coroutineContext)

    private var currentInputConnection: TextInputConnection? by mutableStateOf(null)

    private var updateEditMenuState = {}

    val hasInvalidations: Boolean
        get() = currentInputConnection?.hasInvalidations ?: false

    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        coroutineScope {
            launch {
                snapshotFlow { request.stateSnapshot() }.collect {
                    currentInputConnection?.onTextFieldValueUpdated(it)
                }
            }
            launch {
                snapshotFlow {
                    Triple(
                        request.textFieldRectInRoot(),
                        request.textClippingRectInRoot(),
                        request.unclippedTextOffsetInRoot(),
                    )
                }.collect {
                    currentInputConnection?.onViewGeometryUpdated()
                }
            }
            suspendCancellableCoroutine<Nothing> { continuation ->
                startInput(request)

                continuation.invokeOnCancellation {
                    stopInput()
                }
            }
        }
    }

    private fun startInput(request: PlatformTextInputMethodRequest) {
        val usingNativeTextInput = request.imeOptions.platformImeOptions?.usingNativeTextInput ?: false

        currentInputConnection?.stop()
        currentInputConnection = if (usingNativeTextInput) {
            NativeTextInputConnection(
                updateView = updateView,
                view = view,
                coroutineScope = coroutineScope,
                focusedViewsList = focusedViewsList,
                focusManager = focusManager,
            )
        } else {
            ComposeTextInputConnection(
                updateView = updateView,
                view = view,
                coroutineScope = coroutineScope,
                viewConfiguration = viewConfiguration,
                focusedViewsList = focusedViewsList,
                focusManager = focusManager
            )
        }
        currentInputConnection?.start(request)
        updateEditMenuState()
        onInputStarted()
    }

    private fun stopInput() {
        currentInputConnection?.stop()
        currentInputConnection = null
    }

    fun showSoftwareKeyboard() {
        currentInputConnection?.showKeyboard()
    }

    fun hideSoftwareKeyboard() {
        currentInputConnection?.dismissKeyboard()
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean =
        currentInputConnection?.onPreviewKeyEvent(event) ?: false

    val textToolbar: TextToolbar by lazy(LazyThreadSafetyMode.NONE) {
        object : TextToolbar {
            override val status: TextToolbarStatus
                get() = (currentInputConnection as? ComposeTextInputConnection)?.toolbarStatus ?: TextToolbarStatus.Hidden

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                if (currentInputConnection == null) {
                    // Entry point for showing the context menu in SelectionContainer scenarios, where
                    // there is no active text input session. iOS requires a UIView that can become first
                    // responder in order to host the context menu, so we create a dedicated connection
                    // backed by a hidden view for this purpose.
                    // Note: start() is intentionally not called here — it establishes a text editing
                    // session (requiring a PlatformTextInputMethodRequest) which is not applicable for
                    // SelectionContainer.
                    currentInputConnection = SelectionContainerConnection(
                        view, coroutineScope, viewConfiguration, focusManager
                    )
                }
                (currentInputConnection as? ComposeTextInputConnection)?.showToolbarMenu(
                    rect = rect,
                    onCopyRequested = onCopyRequested,
                    onPasteRequested = onPasteRequested,
                    onCutRequested = onCutRequested,
                    onSelectAllRequested = onSelectAllRequested
                )
            }

            override fun hide() {
                (currentInputConnection as? ComposeTextInputConnection)?.hideToolbar()

                if (currentInputConnection is SelectionContainerConnection) {
                    // stop() removes the view from the hierarchy and resigns first responder,
                    // without requiring a prior start() call.
                    currentInputConnection?.stop()
                    currentInputConnection = null
                }
            }
        }
    }

    val nativeTextInputContext = object : UIKitNativeTextInputContext {
        override fun usingNativeTextInput(): Boolean =
            currentInputConnection is NativeTextInputConnection

        override fun updateNativeTextInputEditMenuState(
            copy: (() -> Unit)?,
            paste: (() -> Unit)?,
            cut: (() -> Unit)?,
            selectAll: (() -> Unit)?,
            customActions: List<UIKitNativeTextInputContextMenuCustomAction>?
        ) {
            fun update() {
                currentInputConnection?.setAvailableEditMenuActions(
                    copy = copy,
                    paste = paste,
                    cut = cut,
                    selectAll = selectAll,
                    customActions = customActions
                )
                updateEditMenuState = {}
            }

            if (currentInputConnection == null) {
                // Fixes race conditions when the `updateNativeTextInputEditMenuState` called before
                // the input session start.
                updateEditMenuState = ::update
            } else {
                update()
            }
        }

        override fun updateNativeTextInputTintColor(color: Color?) {
            (currentInputConnection as? NativeTextInputConnection)?.updateNativeTextInputTintColor(
                color
            )
        }
    }

    fun dispose() {
        stopInput()
        onInputStarted = {}
        updateView = {}
        focusManager = { null }
    }
}
