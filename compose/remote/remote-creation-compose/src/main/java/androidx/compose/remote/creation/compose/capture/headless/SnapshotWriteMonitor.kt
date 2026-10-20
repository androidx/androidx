/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.capture.headless

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * Reference-counted monitor that coalesces global snapshot writes into
 * [Snapshot.sendApplyNotifications] while at least one streaming `captureRemoteDocument` session is
 * active.
 *
 * Compose Runtime's [Snapshot.registerGlobalWriteObserver] is process-wide (written state objects
 * carry no per-session identity), and a single [Snapshot.sendApplyNotifications] call advances the
 * global snapshot and notifies every active [Recomposer], each of which filters the changed set
 * against its own recorded read set.
 *
 * Therefore, multiple concurrent sessions across threads share a single observer registration and a
 * single coalescing channel loop:
 * - When active session count transitions `0 -> 1`, one global write observer and consumer loop are
 *   started.
 * - While K >= 1 sessions are active across threads, all K sessions share that single observer and
 *   single coalesced notification dispatch.
 * - When active session count drops `1 -> 0` (in `finally`), the [ObserverHandle] is disposed and
 *   the consumer coroutine is cancelled so zero global observers or background coroutines remain
 *   when idle.
 *
 * See prior art in other Compose composition hosts across AndroidX:
 * - `androidx.glance.session.globalSnapshotMonitor` and
 *   `androidx.glance.session.GlobalSnapshotManager` (session-scoped suspend monitor and
 *   process-wide coalescing channel loop on `Dispatchers.Default`).
 * - `androidx.compose.ui.platform.GlobalSnapshotManager` (process-wide coalescing channel loop on
 *   `AndroidUiDispatcher.Main`).
 * - `androidx.glance.session.SessionWorker.runSession` (hosting `Recomposer`, frame clock, and
 *   snapshot monitor inside a coroutine scope).
 */
internal object SnapshotWriteMonitor {
    private val lock = Any()
    private var refCount = 0
    private var observerHandle: ObserverHandle? = null
    private var monitorScope: CoroutineScope? = null

    @get:VisibleForTesting
    internal val activeSessionCount: Int
        get() = synchronized(lock) { refCount }

    @get:VisibleForTesting
    internal val isRunning: Boolean
        get() = synchronized(lock) { observerHandle != null }

    /**
     * Starts the monitor if no other session holds it. Close the returned handle (once) when the
     * session ends; the monitor stops when the last handle is closed.
     */
    fun acquire(): AutoCloseable {
        synchronized(lock) {
            if (refCount++ == 0) {
                val channel = Channel<Unit>(1)
                val sent = AtomicBoolean(false)
                val scope =
                    CoroutineScope(
                        Dispatchers.Default.limitedParallelism(
                            parallelism = 1,
                            name = "RemoteComposeSnapshotWriteMonitor",
                        ) + SupervisorJob()
                    )
                // consumeEach cancels the channel when the consumer completes or is cancelled, so
                // cancelling the scope in release() also tears down the channel.
                scope.launch {
                    channel.consumeEach {
                        sent.set(false)
                        Snapshot.sendApplyNotifications()
                    }
                }
                monitorScope = scope
                observerHandle = Snapshot.registerGlobalWriteObserver {
                    if (sent.compareAndSet(false, true)) {
                        if (channel.trySend(Unit).isFailure) {
                            sent.set(false)
                        }
                    }
                }
            }
        }
        return Handle()
    }

    private fun release() {
        synchronized(lock) {
            check(refCount > 0) {
                "SnapshotWriteMonitor.release() called without matching acquire()"
            }
            if (--refCount == 0) {
                observerHandle?.dispose()
                observerHandle = null
                monitorScope?.cancel()
                monitorScope = null
            }
        }
    }

    private class Handle : AutoCloseable {
        private val isClosed = AtomicBoolean(false)

        override fun close() {
            if (isClosed.compareAndSet(false, true)) release()
        }
    }
}
