/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import android.view.inputmethod.BaseInputConnection
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.input.pointer.MatrixPositionCalculator
import androidx.compose.ui.text.input.LegacyEditorFocusListener
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputServiceAndroid
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Allows tests to inject a custom [PlatformTextInputService]. */
internal var platformTextInputServiceInterceptor:
    (PlatformTextInputService) -> PlatformTextInputService =
    {
        it
    }

/**
 * Owns the text input services of a [ComposeUiHost]: the legacy [TextInputService], the
 * [SoftwareKeyboardController], and the [SessionMutex] for [textInputSession].
 *
 * All members must be called on the UI thread.
 *
 * Input methods reach the host only through [ComposeUiHost.startInputMethod]:
 * - Each [PlatformTextInputSession.startInputMethod] call holds one host call (see
 *   [AndroidPlatformTextInputSession]).
 * - While an editor that uses the deprecated [TextInputService] directly is focused and no
 *   [textInputSession] is active, one host call with [legacyInputMethodRequest] is held open. It is
 *   cancelled while any [textInputSession] is active (even before `startInputMethod` is called) and
 *   re-launched if the legacy editor is still focused when all sessions end (note that exiting a
 *   session that called `startInputMethod` calls `TextInputService.stopInput()`, which clears
 *   legacy focus).
 *
 * @param coroutineContext Provides the UI-thread dispatcher for the legacy host call.
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiHostApi::class)
internal class AndroidPlatformTextInputManager(
    private val host: ComposeUiHost,
    private val matrixPositionCalculator: MatrixPositionCalculator,
    private val afterFrameCommandExecutor: Executor,
    private val nextFrameCommandExecutor: Executor,
    private val coroutineContext: () -> CoroutineContext,
) {
    private var _legacyTextInputServiceAndroid: TextInputServiceAndroid? = null
    private val legacyTextInputServiceAndroid: TextInputServiceAndroid
        get() =
            _legacyTextInputServiceAndroid
                ?: TextInputServiceAndroid(
                        host.view,
                        matrixPositionCalculator,
                        afterFrameCommandExecutor = afterFrameCommandExecutor,
                        nextFrameCommandExecutor = nextFrameCommandExecutor,
                    )
                    .also { service ->
                        _legacyTextInputServiceAndroid = service
                        service.legacyEditorFocusListener = legacyEditorFocusListener
                    }

    private val legacyInputMethodRequest: PlatformTextInputMethodRequest =
        PlatformTextInputMethodRequest { outAttrs ->
            // Defensive fallback: stopInput() cancels the host call synchronously before returning,
            // so createInputConnection normally is not invoked after focus is lost.
            legacyTextInputServiceAndroid.createInputConnection(outAttrs)
                ?: BaseInputConnection(host.view, false)
        }

    private var legacyInputMethodJob: Job? = null
    private var activeSessionCount = 0

    private fun updateLegacyInputMethodCall() {
        if (_legacyTextInputServiceAndroid?.isEditorFocused() == true && activeSessionCount == 0) {
            if (legacyInputMethodJob?.isActive != true) {
                // Use an unparented SupervisorJob so legacy editor state does not keep
                // Recomposer.close() alive or inherit/propagate failures to the Recomposer Job.
                val scopeJob = SupervisorJob()
                legacyInputMethodJob = scopeJob
                CoroutineScope(coroutineContext() + scopeJob).launch(
                    start = CoroutineStart.UNDISPATCHED
                ) {
                    try {
                        host.startInputMethod(legacyInputMethodRequest)
                    } finally {
                        scopeJob.cancel()
                    }
                }
            }
        } else {
            legacyInputMethodJob?.cancel()
            legacyInputMethodJob = null
        }
    }

    private val legacyEditorFocusListener = LegacyEditorFocusListener {
        updateLegacyInputMethodCall()
    }

    /**
     * If [platformTextInputServiceInterceptor] replaced the service with a
     * [TextInputServiceAndroid].
     */
    private var interceptedTextInputServiceAndroid: TextInputServiceAndroid? = null

    private var _textInputService: TextInputService? = null

    /**
     * The legacy text input service, also used by [AndroidPlatformTextInputSession] to drive
     * `startInput` and `stopInput`.
     */
    val textInputService: TextInputService
        get() =
            _textInputService
                ?: TextInputService(
                        platformTextInputServiceInterceptor(legacyTextInputServiceAndroid).also {
                            interceptedTextInputServiceAndroid = it as? TextInputServiceAndroid
                        }
                    )
                    .also { _textInputService = it }

    private val textInputSessionMutex = SessionMutex<AndroidPlatformTextInputSession>()

    private var _softwareKeyboardController: SoftwareKeyboardController? = null
    val softwareKeyboardController: SoftwareKeyboardController
        get() =
            _softwareKeyboardController
                ?: DelegatingSoftwareKeyboardController(textInputService).also {
                    _softwareKeyboardController = it
                }

    suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing {
        activeSessionCount++
        updateLegacyInputMethodCall()
        try {
            textInputSessionMutex.withSessionCancellingPrevious(
                sessionInitializer = {
                    AndroidPlatformTextInputSession(
                        host = host,
                        textInputService = textInputService,
                        coroutineScope = it,
                        onConnectionCreated = {
                            (interceptedTextInputServiceAndroid ?: legacyTextInputServiceAndroid)
                                .onInputConnectionCreated()
                        },
                    )
                },
                session = session,
            )
        } finally {
            activeSessionCount--
            updateLegacyInputMethodCall()
        }
    }
}
