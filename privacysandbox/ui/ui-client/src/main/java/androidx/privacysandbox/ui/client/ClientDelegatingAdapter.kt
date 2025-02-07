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

package androidx.privacysandbox.ui.client

import android.content.Context
import android.os.Bundle
import androidx.annotation.GuardedBy
import androidx.core.util.Consumer
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory.createFromCoreLibInfo
import androidx.privacysandbox.ui.client.view.RefreshableSessionClient
import androidx.privacysandbox.ui.core.IDelegateChangeListener
import androidx.privacysandbox.ui.core.IDelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.IDelegatorCallback
import androidx.privacysandbox.ui.core.ISessionRefreshCallback
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionConstants
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Client side class for [IDelegatingSandboxedUiAdapter] which helps open sessions with the
 * delegate, and refresh sessions on the client when the delegate is updated by the provider.
 *
 * This class:
 * 1. Takes opensession calls from the SandboxedSdkView and helps open session with the delegate
 * 2. Takes onDelegateChanged requests from the provider and a. order refreshes on all active
 *    clients for this adapter b. communicates back success/failure if all clients refreshed
 *    successfully or some failed.
 */
internal class ClientDelegatingAdapter(
    private val delegatingAdapterInterface: IDelegatingSandboxedUiAdapter,
    /**
     * Has the latest delegate that has been set on the delegating adapter. There may be race
     * conditions on opening a session with the delegate and changing the delegate so, a lock must
     * be acquired when dealing with this variable.
     */
    @GuardedBy("lock") var latestDelegate: SandboxedUiAdapter
) : SandboxedUiAdapter {
    private val lock = Any()

    // Using Dispatcher.Unconfined implies the coroutine may resume on any available thread.
    // The thread doesn't matter to us for this case and so between the two options we have - Main
    // or Unconfined we prefer Unconfined.
    private val scope = CoroutineScope(Dispatchers.Unconfined + NonCancellable)

    /**
     * List of SandboxedSdkView owned active session clients that are served using this adapter.
     * Calls for refreshing a client and adding a client after an onSessionOpened is received can
     * race. We may end up taking clients on old delegates which miss the refresh if locks aren't
     * used.
     */
    @GuardedBy("lock")
    private val sessionClients: MutableList<RefreshableSessionClient> = mutableListOf()

    /**
     * An observer receives change requests from the ui provider delegator. The calls to attach and
     * detach an observer may race and so this has been guarded by locks.
     */
    @GuardedBy("lock") private var observer: IDelegateChangeListener? = null

    /**
     * Wrapper of the SandboxedSdkView's clients so that all calls are intercepted here and the
     * client calls to open a session and refreshing the delegates can be handled well.
     */
    private inner class ClientWrapper(
        private var client: RefreshableSessionClient?,
        /** the delegate that was used when this [ClientWrapper] was created to open a session. */
        private val delegateUsed: SandboxedUiAdapter
    ) : SandboxedUiAdapter.SessionClient {

        // It is possible that onSessionError and onSessionOpened race in certain conditions so,
        // taking locks to ensure we process one completely before the other.
        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            synchronized(lock) {
                if (latestDelegate != delegateUsed) {
                    // TODO(b/351341237): Handle client retries
                    client?.onSessionError(
                        Throwable(
                            "The client may retry. The delegate changed while opening a new session." +
                                " This happens only when the adapter is being reused for a new SandboxedSdkView. " +
                                " Will not happen when switching sessions on existing SandboxedSdkViews"
                        )
                    )
                    return
                }
                client?.onSessionOpened(session)
                if (observer == null) {
                    createNewDelegateChangeObserver()
                    tryToCallRemoteObject(delegatingAdapterInterface) {
                        addDelegateChangeListener(observer)
                    }
                }
                client?.let { sessionClients.add(it) }
            }
        }

        /**
         * Forwards the error to the underlying client. Since this client is no longer active, we
         * remove it from the list of clients If the list is empty, this adapter has no clients, we
         * detach the observer for delegate change as it may be purposeless.
         */
        override fun onSessionError(throwable: Throwable) {
            synchronized(lock) {
                sessionClients.remove(client)
                if (sessionClients.isEmpty() && observer != null) {
                    tryToCallRemoteObject(delegatingAdapterInterface) {
                        removeDelegateChangeListener(observer)
                    }
                    observer = null
                }
                client?.onSessionError(throwable)
                client = null
            }
        }

        override fun onResizeRequested(width: Int, height: Int) {
            client?.onResizeRequested(width, height)
        }
    }

    /**
     * Wraps the client received for opening a session with its own wrapper and then opens a session
     * with the delegate If the observer is missing, it also attaches a delegate change observer to
     * the adapter.
     */
    override fun openSession(
        context: Context,
        sessionConstants: SessionConstants,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient
    ) {
        val delegateUsed: SandboxedUiAdapter = synchronized(lock) { latestDelegate }
        delegateUsed.openSession(
            context,
            sessionConstants,
            initialWidth,
            initialHeight,
            isZOrderOnTop,
            clientExecutor,
            ClientWrapper(client as RefreshableSessionClient, delegateUsed)
        )
    }

    /**
     * The observer accepts [IDelegateChangeListener.onDelegateChanged] calls from the adapter and
     * processes the change on its active clients. We have added reentrancy protection so other
     * refresh requests will be queued until all responses are received. We forward
     * success/failure(if any client fails) to the adapter.
     */
    @GuardedBy("lock")
    private fun createNewDelegateChangeObserver() {
        observer =
            object : IDelegateChangeListener.Stub() {
                private val toDispatch: MutableList<RefreshableSessionClient> = mutableListOf()
                private val mutex = Mutex()

                override fun onDelegateChanged(delegate: Bundle, callback: IDelegatorCallback) {
                    scope.launch { // Launch the refresh operation
                        try {
                            mutex.withLock { // Use mutex for reentrancy protection
                                synchronized(lock) {
                                    latestDelegate = createFromCoreLibInfo(delegate)
                                    toDispatch.addAll(sessionClients)
                                }
                                val results =
                                    toDispatch
                                        .map { dispatchClient ->
                                            async {
                                                requestRefresh(
                                                    dispatchClient
                                                ) // suspend and return its result
                                            }
                                        }
                                        .awaitAll()
                                // Determine overall success
                                val overallSuccess = results.all { it }
                                callback.onDelegateChangeResult(overallSuccess)
                            }
                        } catch (e: Exception) {
                            callback.onDelegateChangeResult(false)
                        } finally {
                            toDispatch.clear()
                        }
                    }
                }

                private fun createConsumer(binder: ISessionRefreshCallback): Consumer<Boolean> {
                    return Consumer { result -> binder.onRefreshResult(result) }
                }

                private suspend fun requestRefresh(
                    dispatchClient: RefreshableSessionClient
                ): Boolean {
                    val isRefreshSuccessful = suspendCancellableCoroutine { continuation ->
                        dispatchClient.onSessionRefreshRequested(
                            createConsumer(
                                object : ISessionRefreshCallback.Stub() {
                                    override fun onRefreshResult(success: Boolean) {
                                        if (success) {
                                            synchronized(lock) {
                                                sessionClients.remove(dispatchClient)
                                            }
                                        }
                                        continuation.resume(success)
                                    }
                                }
                            )
                        )
                    }
                    return isRefreshSuccessful
                }
            }
    }
}
