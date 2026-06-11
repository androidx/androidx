/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * Platform-specific mechanism for monitoring global snapshot state writes and scheduling the
 * dispatch of snapshot apply notifications.
 *
 * Unlike Android, which has a single UI thread, a multiplatform process can host several owner
 * contexts at once (e.g. several [FrameRecomposer]s, or an off-UI [ImageComposeScene]). Apply
 * notifications must therefore be delivered on each owner's dispatcher, not on a single global one.
 *
 * [register] observes global writes and, coalesced per batch, sends
 * [Snapshot.sendApplyNotifications] on a given [CoroutineDispatcher]. Registrations are **shared and
 * reference-counted** per dispatcher - all callers passing the same dispatcher share a single observer
 * and apply pump, and that registration is released only when the **last** returned [AutoCloseable] is
 * closed. Keying on the dispatcher keeps several owners that run on one dispatcher - e.g. several
 * [FrameRecomposer]s on the UI dispatcher - on a single pump instead of each spinning up its own
 * redundant observer.
 */
internal object GlobalSnapshotManager {
    private val lock = makeSynchronizedObject()

    /** Live registrations keyed by the dispatcher they pump on. Guarded by [lock]. */
    private val registrations = mutableMapOf<CoroutineDispatcher, Registration>()

    /**
     * Ensures global snapshot writes schedule coalesced [Snapshot.sendApplyNotifications] on the
     * [CoroutineDispatcher] carried by [coroutineContext], starting a shared registration on the first
     * call for that dispatcher. Nothing else from the context is used.
     *
     * @param coroutineContext the host context whose [CoroutineDispatcher] the apply pump runs on.
     * @return an [AutoCloseable] that releases this caller's share of the registration on close
     * (the underlying observer/pump is released only once every caller for that dispatcher has
     * closed its handle), or `null` if [coroutineContext] carries no dispatcher or an *immediate*
     * (non-dispatching) one - the cases where no pump is started.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun register(coroutineContext: CoroutineContext): AutoCloseable? {
        val dispatcher = coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
            ?: return null
        return register(dispatcher)
    }

    /**
     * Ensures global snapshot writes schedule coalesced [Snapshot.sendApplyNotifications] on
     * [dispatcher], starting a shared registration on the first call for that dispatcher.
     *
     * @param dispatcher the dispatcher the apply pump runs on.
     * @return an [AutoCloseable] that releases this caller's share of the registration on close
     * (the underlying observer/pump is released only once every caller for that dispatcher has
     * closed its handle), or `null` for an *immediate* (non-dispatching) [dispatcher] - the cases
     * where no pump is started.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun register(dispatcher: CoroutineDispatcher): AutoCloseable? {
        // isDispatchNeeded is a per-resume, thread-dependent property, so an immediate dispatcher
        // cannot be soundly classified by this one-time registration check.
        if (!dispatcher.isDispatchNeeded(dispatcher)) {
            return null
        }
        val registration = synchronized(lock) {
            registrations.getOrPut(dispatcher) { Registration(dispatcher) }
                .also { it.refCount++ }
            }
        if (registrations.size > 1) {
            // There are a couple of problems with it e.g. b/418800424
            println("GlobalSnapshotManager: concurrent registrations of apply dispatchers " +
                "might lead to races; prefer a single apply dispatcher."
            )
        }
        return AutoCloseable { release(registration) }
    }

    private fun release(registration: Registration) {
        synchronized(lock) {
            if (--registration.refCount > 0) return
            registrations.remove(registration.dispatcher)
        }
        registration.dispose()
    }

    @VisibleForTesting
    fun clear() {
        synchronized(lock) {
            registrations.values.forEach { it.dispose() }
            registrations.clear()
        }
    }

    /** A shared observer + coalescing apply pump for one host [dispatcher]. */
    private class Registration(val dispatcher: CoroutineDispatcher) {
        /** Number of live handles. Guarded by [GlobalSnapshotManager.lock]. */
        var refCount = 0

        private val scheduled = atomic(false)
        private val channel = Channel<Unit>(Channel.CONFLATED)
        private val scope = CoroutineScope(dispatcher + Job())
        private val writeObserverHandle: ObserverHandle

        init {
            scope.launch {
                channel.consumeEach {
                    scheduled.value = false
                    Snapshot.sendApplyNotifications()
                }
            }
            writeObserverHandle = Snapshot.registerGlobalWriteObserver {
                if (scheduled.compareAndSet(expect = false, update = true)) {
                    channel.trySend(Unit)
                }
            }
        }

        fun dispose() {
            if (!channel.close()) return
            writeObserverHandle.dispose()
            scope.cancel()
        }
    }
}
