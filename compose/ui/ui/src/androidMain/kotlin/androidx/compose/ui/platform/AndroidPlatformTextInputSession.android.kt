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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.WeakReference
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.NullableInputConnectionWrapper
import androidx.compose.ui.text.input.TextInputService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manages a top-level input session, as created by [Owner.textInputSession].
 *
 * On Android there are three levels of input sessions:
 * 1. [PlatformTextInputModifierNode.establishTextInputSession]: The app is performing some
 *    initialization before requesting the keyboard.
 * 2. [PlatformTextInputSession.startInputMethod]: The app has requested the keyboard with a
 *    particular implementation for [View.onCreateInputConnection] represented by a
 *    [PlatformTextInputMethodRequest].
 * 3. [View.onCreateInputConnection]: The system has responded to the keyboard request by asking the
 *    view for a new [InputConnection].
 *
 * Each of these sessions is a parent of the next, in terms of lifetime and cancellation.
 *
 * This class manages child sessions started by [startInputMethod]. Each child session is
 * represented by an [InputMethodSession], which in turn coordinates between multiple calls to
 * [createInputConnection] for a single session.
 */
@OptIn(InternalTextApi::class, InternalComposeUiApi::class)
internal class AndroidPlatformTextInputSession(
    override val view: View,
    private val textInputService: TextInputService,
    private val coroutineScope: CoroutineScope
) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {
    /** Coordinates between calls to [startInputMethod]. */
    private val methodSessionMutex = SessionMutex<InputMethodSession>()

    /**
     * Returns true if [startInputMethod] has been called and is ready to respond to
     * [createInputConnection].
     */
    val isReadyForConnection: Boolean
        get() = methodSessionMutex.currentSession?.isActive == true

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing =
        methodSessionMutex.withSessionCancellingPrevious(
            sessionInitializer = {
                InputMethodSession(
                    request = request,
                    onAllConnectionsClosed = { coroutineScope.cancel() }
                )
            }
        ) { methodSession ->
            @Suppress("RemoveExplicitTypeArguments")
            suspendCancellableCoroutine<Nothing> { continuation ->
                // Show the keyboard and ask the IMM to restart input.
                textInputService.startInput()

                // The cleanup needs to be executed synchronously, otherwise the stopInput call
                // might come too late and end up overriding the next field's startInput. This
                // is prevented by the queuing in TextInputService, but can still happen when
                // focus transfers from a compose text field to a view-based text field
                // (EditText).
                continuation.invokeOnCancellation {
                    methodSession.dispose()

                    // If this session was cancelled because another session was requested, this
                    // call will be a noop.
                    textInputService.stopInput()
                }
            }
        }

    /**
     * Creates a new [InputConnection] using the current [InputMethodSession]'s request, or returns
     * null if there's no active [InputMethodSession] (i.e. [startInputMethod] hasn't been called
     * yet or the session is being torn down).
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection? =
        methodSessionMutex.currentSession?.createInputConnection(outAttrs)
}

/**
 * Coordinates between calls to [View.onCreateInputConnection] for a single
 * [AndroidPlatformTextInputSession]'s input method session. Instances of this class correspond to
 * calls to [AndroidPlatformTextInputSession.startInputMethod]. This class ensures that old
 * connections are disposed before new ones are created.
 *
 * @param onAllConnectionsClosed Called when all created [InputConnection]s receive
 *   [InputConnection.closeConnection] call.
 */
private class InputMethodSession(
    private val request: PlatformTextInputMethodRequest,
    private val onAllConnectionsClosed: () -> Unit
) {
    private val lock = Any()
    private var connections = mutableVectorOf<WeakReference<NullableInputConnectionWrapper>>()
    private var disposed = false

    val isActive: Boolean
        get() = !disposed

    /**
     * Creates a new [InputConnection] and initializes [outAttrs] by calling this session's
     * [PlatformTextInputMethodRequest.createInputConnection]. If a previous connection is active,
     * it will be disposed (closed and reference cleared) before creating the new one.
     *
     * Returns null if [dispose] has been called.
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection? {
        synchronized(lock) {
            if (disposed) return null

            // Do not manually dispose a previous InputConnection until it's collected by the GC or
            // an explicit call is received to its `onConnectionClosed` callback.

            val connectionDelegate = request.createInputConnection(outAttrs)
            return NullableInputConnectionWrapper(
                    delegate = connectionDelegate,
                    onConnectionClosed = { closedConnection ->
                        // We should not cancel any ongoing input session because connection is
                        // closed
                        // from InputConnection. This may happen at any time and does not indicate
                        // whether the system is trying to stop an input session. The platform may
                        // request a new InputConnection immediately after closing this one.
                        // Instead we should just clear all the resources used by this
                        // inputConnection,
                        // because the platform guarantees that it will never reuse a closed
                        // connection.
                        closedConnection.disposeDelegate()
                        val removeIndex = connections.indexOfFirst { it == closedConnection }
                        if (removeIndex >= 0) connections.removeAt(removeIndex)
                        if (connections.isEmpty()) {
                            onAllConnectionsClosed()
                        }
                    }
                )
                .also { connections.add(WeakReference(it)) }
        }
    }

    /**
     * Disposes the current [InputConnection]. After calling this method, all future calls to
     * [createInputConnection] will return null.
     *
     * This function is only called from coroutine cancellation routine so it's not required to
     * cancel the coroutine from here.
     */
    fun dispose() {
        synchronized(lock) {
            // Manually close the delegate in case the system forgets to.
            disposed = true
            connections.forEach { it.get()?.disposeDelegate() }
            connections.clear()
        }
    }
}
