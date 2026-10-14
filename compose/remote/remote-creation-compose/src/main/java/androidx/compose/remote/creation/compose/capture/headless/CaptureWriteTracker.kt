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
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asContextElement

/**
 * Called on the capture's thread when [CaptureWriteTracker] finds writes made by the capture that
 * have not been delivered to apply observers yet, before it waits for them. Lets tests hold back a
 * delivery until the capture is known to be waiting for it.
 */
@VisibleForTesting @Volatile internal var onCaptureAwaitingWriteDelivery: (() -> Unit)? = null

/**
 * Tracks global snapshot writes made by one capture's own coroutines (effects and their frame
 * callbacks) until an apply notification delivers them.
 *
 * A `MutableSnapshot.apply()` on another thread (e.g. a concurrent capture's recomposer, or a
 * render snapshot) claims every pending global write and delivers it to apply observers only after
 * releasing the global lock, outside [Snapshot.isApplyObserverNotificationPending]. If that claims
 * a write made by this capture's effect, [Snapshot.sendApplyNotifications] here finds nothing to
 * send and [Recomposer.hasPendingWork] stays `false` until the other thread delivers it, so the
 * capture could sample itself as quiescent and render stale state (b/571028326).
 *
 * TODO(b/573872754): Remove once [Snapshot.isApplyObserverNotificationPending] also covers global
 *   writes delivered by `MutableSnapshot.apply()`; [awaitRecomposerQuiescence] then only needs its
 *   existing wait on that flag.
 *
 * Writes are attributed to the capture through [contextElement], a thread-local marker installed
 * while the capture's coroutines run (the Recomposer's effect context inherits it), so writes from
 * elsewhere in the process are ignored. Its apply observer must run after the Recomposer's, so that
 * once a write is delivered here the Recomposer has already seen it: [start] it after the
 * Recomposer registers its apply observer, and before effects can resume from a frame.
 *
 * Thread-safe: observers run on whichever thread writes or delivers.
 */
internal class CaptureWriteTracker {
    private val activeTracker = ThreadLocal<CaptureWriteTracker?>()
    private val lock = Any()
    private val undelivered = newIdentitySet()
    private var writeObserver: ObserverHandle? = null
    private var applyObserver: ObserverHandle? = null
    private var isStopped = false

    /** Marks coroutines running in a context containing this element as this capture's. */
    val contextElement: CoroutineContext.Element = activeTracker.asContextElement(this)

    /**
     * Waits until every write made by this capture has been delivered to apply observers, sending
     * apply notifications between polls so writes made while the dispatcher is yielded are
     * delivered too.
     *
     * The wait is bounded by [pollUntil] and never fails the capture: the runtime calls write
     * observers after releasing the snapshot lock, so another thread can claim and deliver a write
     * before it is recorded here. Such an entry is indistinguishable from one still in flight, so
     * entries left at the deadline are dropped.
     */
    suspend fun awaitDelivery(canYield: Boolean) {
        if (synchronized(lock) { undelivered.isEmpty() }) return
        onCaptureAwaitingWriteDelivery?.invoke()
        val isDelivered =
            pollUntil(canYield) {
                Snapshot.sendApplyNotifications()
                synchronized(lock) { undelivered.isEmpty() }
            }
        if (!isDelivered) synchronized(lock) { undelivered.clear() }
    }

    /** Starts tracking, unless already started or [stop]ped. */
    fun start() {
        synchronized(lock) { if (isStopped || applyObserver != null) return }
        // Registered outside lock: registration takes the snapshot lock, which a writer may hold
        // while calling the write observer below.
        val apply = Snapshot.registerApplyObserver { changed, _ ->
            synchronized(lock) { if (undelivered.isNotEmpty()) undelivered.removeAll(changed) }
        }
        val write = Snapshot.registerGlobalWriteObserver { state ->
            if (activeTracker.get() === this) synchronized(lock) { undelivered.add(state) }
        }
        val isStarted =
            synchronized(lock) {
                if (isStopped) {
                    false
                } else {
                    applyObserver = apply
                    writeObserver = write
                    true
                }
            }
        if (!isStarted) {
            write.dispose()
            apply.dispose()
        }
    }

    /** Stops tracking and releases the observers. A later [start] does nothing. */
    fun stop() {
        val (write, apply) =
            synchronized(lock) {
                isStopped = true
                undelivered.clear()
                (writeObserver to applyObserver).also {
                    writeObserver = null
                    applyObserver = null
                }
            }
        write?.dispose()
        apply?.dispose()
    }
}

private fun newIdentitySet(): MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())
