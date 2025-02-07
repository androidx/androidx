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

package androidx.privacysandbox.ui.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [SandboxedUiAdapter] that helps delegate calls to other uiAdapters.
 *
 * When this adapter is set to the client's container, the adapter can switch the delegate
 * uiAdapter, that serves the `Session`, without involving the client. For each new delegate, a
 * session would be requested with all clients of this adapter.
 *
 * One example use-case of these kind of UIAdapters is to support updating the provider of the UI
 * without the client's involvement.
 */
@ExperimentalFeatures.DelegatingAdapterApi
class DelegatingSandboxedUiAdapter(private var delegate: Bundle) : SandboxedUiAdapter {

    /** Listener that consumes events to process the delegate change for a client */
    interface DelegateChangeListener {
        /** When invoked triggers processing of the delegate change for a client */
        suspend fun onDelegateChanged(delegate: Bundle) {}
    }

    /**
     * List of listeners that consume events to refresh the currently active sessions of this
     * adapter.
     */
    private var delegateChangeListeners: CopyOnWriteArrayList<DelegateChangeListener> =
        CopyOnWriteArrayList()
    private val mutex = Mutex()

    /**
     * Updates the delegate and notifies all listeners to process the update. If any listener fails
     * process refresh we throw [IllegalStateException]. Cancellation of this API does not propagate
     * to the client side.
     */
    suspend fun updateDelegate(delegate: Bundle) {
        // TODO(b/374955412): Support cancellation across process
        mutex.withLock {
            this.delegate = delegate
            coroutineScope {
                delegateChangeListeners.forEach { l -> launch { l.onDelegateChanged(delegate) } }
            }
        }
    }

    override fun openSession(
        context: Context,
        sessionConstants: SessionConstants,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient,
    ) {}

    @SuppressLint("ExecutorRegistration")
    // Used by [updateDelegate] and therefore runs on updateDelegate's calling context
    fun addDelegateChangeListener(listener: DelegateChangeListener) {
        delegateChangeListeners.add(listener)
    }

    // TODO(b/350656753): Add tests to check functionality of DelegatingAdapters
    @SuppressLint("ExecutorRegistration")
    // Used by [updateDelegate] and therefore runs on updateDelegate's calling context
    fun removeDelegateChangeListener(listener: DelegateChangeListener) {
        delegateChangeListeners.remove(listener)
    }

    /** Fetches the current delegate which is a [SandboxedUiAdapter] Bundle. */
    // TODO(b/375388971): Check coreLibInfo is present
    fun getDelegate(): Bundle {
        return delegate
    }
}
