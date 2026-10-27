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
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * global snapshot and notifies every active [androidx.compose.runtime.Recomposer], each of which
 * filters the changed set against its own recorded read set.
 *
 * Therefore, concurrent sessions share one observer registration and one coalescing loop per
 * [CoroutineContext] the loop is asked to run on:
 * - When the session count for a context transitions `0 -> 1`, one global write observer and
 *   consumer loop are started on that context.
 * - While K >= 1 sessions share a context, they share that single observer and single coalesced
 *   notification dispatch.
 * - When the count drops `1 -> 0` (in `finally`), the [ObserverHandle] is disposed and the consumer
 *   coroutine is cancelled so zero global observers or background coroutines remain when idle.
 *
 * Production sessions use the default context, a single-threaded slice of [Dispatchers.Default].
 * Tests that drive a composition with a virtual-time dispatcher can pass that dispatcher so that
 * apply notifications are delivered deterministically under it; several loops on different contexts
 * are harmless, as a [Snapshot.sendApplyNotifications] with nothing pending is a no-op.
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
    private val monitors = HashMap<CoroutineContext, Monitor>()

    /** The context sessions coalesce on unless they ask for another. */
    private val DefaultContext: CoroutineContext =
        Dispatchers.Default.limitedParallelism(
            parallelism = 1,
            name = "RemoteComposeSnapshotWriteMonitor",
        )

    private class Monitor(val observerHandle: ObserverHandle, val scope: CoroutineScope) {
        var refCount = 0
    }

    @get:VisibleForTesting
    internal val activeSessionCount: Int
        get() = synchronized(lock) { monitors.values.sumOf { it.refCount } }

    @get:VisibleForTesting
    internal val isRunning: Boolean
        get() = synchronized(lock) { monitors.isNotEmpty() }

    /**
     * Acquires a monitor for a capture session running in [sessionContext]: uses [DefaultContext]
     * when [sessionContext] has no [ContinuationInterceptor] or uses [Dispatchers.Default] so
     * default sessions share one monitor, and otherwise runs on [sessionContext]'s interceptor so
     * caller-supplied dispatchers (such as a `TestDispatcher` or main dispatcher) receive apply
     * notifications on that same dispatcher.
     */
    fun acquireForSession(sessionContext: CoroutineContext): AutoCloseable {
        val interceptor = sessionContext[ContinuationInterceptor]
        return if (interceptor == null || interceptor == Dispatchers.Default) {
            acquire()
        } else {
            acquire(interceptor)
        }
    }

    /**
     * Starts a monitor on [context] if no other session holds one there. Close the returned handle
     * (once) when the session ends; the monitor stops when the last handle for its context is
     * closed.
     *
     * @param context where [Snapshot.sendApplyNotifications] is called from. Defaults to a
     *   single-threaded slice of [Dispatchers.Default].
     */
    fun acquire(context: CoroutineContext = DefaultContext): AutoCloseable {
        val monitorContext = context.minusKey(Job)
        synchronized(lock) {
            val monitor = monitors.getOrPut(monitorContext) { startMonitor(monitorContext) }
            monitor.refCount++
        }
        return Handle(monitorContext)
    }

    private fun startMonitor(context: CoroutineContext): Monitor {
        val channel = Channel<Unit>(1)
        val sent = AtomicBoolean(false)
        val scope = CoroutineScope(context + SupervisorJob())
        // consumeEach cancels the channel when the consumer completes or is cancelled, so
        // cancelling the scope in release() also tears down the channel.
        scope.launch {
            channel.consumeEach {
                sent.set(false)
                Snapshot.sendApplyNotifications()
            }
        }
        val observerHandle = Snapshot.registerGlobalWriteObserver {
            if (sent.compareAndSet(false, true)) {
                if (channel.trySend(Unit).isFailure) {
                    sent.set(false)
                }
            }
        }
        return Monitor(observerHandle, scope)
    }

    private fun release(context: CoroutineContext) {
        synchronized(lock) {
            val monitor =
                checkNotNull(monitors[context]) {
                    "SnapshotWriteMonitor.release() called without matching acquire()"
                }
            if (--monitor.refCount == 0) {
                monitors.remove(context)
                monitor.observerHandle.dispose()
                monitor.scope.cancel()
            }
        }
    }

    private class Handle(private val context: CoroutineContext) : AutoCloseable {
        private val isClosed = AtomicBoolean(false)

        override fun close() {
            if (isClosed.compareAndSet(false, true)) release(context)
        }
    }
}
