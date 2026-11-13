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
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.collection.mutableObjectListOf
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.WeakReference
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.NullableInputConnectionWrapper
import androidx.compose.ui.text.input.TextInputService
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
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
 * represented by an [InputMethodSession], which is passed to [ComposeUiHost.startInputMethod] and
 * coordinates between multiple calls to [View.onCreateInputConnection] for a single session.
 */
@OptIn(InternalTextApi::class, ExperimentalComposeUiHostApi::class)
internal class AndroidPlatformTextInputSession(
    private val host: ComposeUiHost,
    private val textInputService: TextInputService,
    private val coroutineScope: CoroutineScope,
    private val onConnectionCreated: () -> Unit = {},
) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {

    override val view: View
        get() = host.view

    /** Tracks the active [startInputMethod] coroutine so a new call can cancel it synchronously. */
    private val activeSessionJob = AtomicReference<Job?>(null)

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing =
        coroutineScope {
            ensureActive()
            val currentJob = coroutineContext.job
            val methodSession =
                InputMethodSession(
                    request = request,
                    fallbackView = view,
                    onConnectionCreated = onConnectionCreated,
                    onAllConnectionsClosed = { coroutineScope.cancel() },
                )
            val previousJob = activeSessionJob.getAndSet(currentJob)
            try {
                // Cancel the previous session synchronously so its host cancellation handler,
                // dispose(), and stopInput() run before the new host request is installed, without
                // suspending in join() (which would leave a gap where onCheckIsTextEditor and
                // onCreateInputConnection see no active request).
                previousJob?.cancel()

                // The host makes the request visible to the view before its first suspension, so
                // it is in place when startInput asks the IMM to restart input.
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        host.startInputMethod(methodSession)
                    } catch (e: CancellationException) {
                        // A child CancellationException does not cancel coroutineScope; cancel it
                        // so startInput() is skipped or stopped instead of hanging without a host.
                        currentJob.cancel(e)
                        throw e
                    }
                }
                // Skip startInput if host.startInputMethod failed or cancelled synchronously.
                ensureActive()

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

                        // If this session was cancelled because another session was requested,
                        // this call will be a noop.
                        textInputService.stopInput()
                    }
                }
            } finally {
                methodSession.dispose()
                activeSessionJob.compareAndSet(currentJob, null)
            }
        }
}

/**
 * Coordinates between calls to [View.onCreateInputConnection] for a single
 * [AndroidPlatformTextInputSession]'s input method session. Instances of this class correspond to
 * calls to [AndroidPlatformTextInputSession.startInputMethod]. This class ensures that old
 * connections are disposed before new ones are created.
 *
 * It is also the request passed to [ComposeUiHost.startInputMethod], so hosts create connections
 * through the same coordination.
 *
 * @param fallbackView the host's [ComposeUiHost.view], used to build a do-nothing
 *   [BaseInputConnection] if a host asks for a connection after [dispose] (needed because
 *   [PlatformTextInputMethodRequest.createInputConnection] cannot return null)
 * @param onConnectionCreated Called when [createInputConnection] creates a non-disposed session
 *   connection.
 * @param onAllConnectionsClosed Called when all created [InputConnection]s receive
 *   [InputConnection.closeConnection] call.
 */
private class InputMethodSession(
    private val request: PlatformTextInputMethodRequest,
    private val fallbackView: View,
    private val onConnectionCreated: () -> Unit,
    private val onAllConnectionsClosed: () -> Unit,
) : PlatformTextInputMethodRequest {
    private val lock = Any()
    private var connections = mutableObjectListOf<WeakReference<NullableInputConnectionWrapper>>()
    private var disposed = false

    /**
     * Creates a new [InputConnection] and initializes [outAttrs] by calling this session's
     * [PlatformTextInputMethodRequest.createInputConnection]. If a previous connection is active,
     * it will be disposed (closed and reference cleared) before creating the new one.
     *
     * Returns null if [dispose] has been called.
     */
    fun createInputConnectionOrNull(outAttrs: EditorInfo): InputConnection? {
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
                    },
                )
                .also { connections.add(WeakReference(it)) }
        }
    }

    /**
     * Host entry point. [PlatformTextInputMethodRequest.createInputConnection] can't return null,
     * so after [dispose] this returns an inert [BaseInputConnection]. Hosts stop using the request
     * synchronously when the host call is cancelled, which happens in the same cancellation pass as
     * [dispose].
     */
    override fun createInputConnection(outAttributes: EditorInfo): InputConnection =
        createInputConnectionOrNull(outAttributes)?.also { onConnectionCreated() }
            ?: BaseInputConnection(fallbackView, false)

    /**
     * Disposes the current [InputConnection]. After calling this method, all future calls to
     * [createInputConnectionOrNull] will return null.
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
