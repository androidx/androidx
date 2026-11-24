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

package androidx.compose.ui.test.snapshots

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.platform.makeSynchronizedObject
import androidx.compose.ui.test.platform.synchronized
import androidx.compose.ui.util.fastForEach
import kotlin.concurrent.Volatile

/**
 * Runs [StateIntegrityObserver]s every time a snapshot is applied, and reports the errors that they
 * throw.
 *
 * While it has observers, it also traces where the state that they read is written to global state,
 * so that each error shows the test statements behind it. Writes in nested snapshots, e.g. in
 * [Snapshot.withMutableSnapshot], aren't traced, but their errors still point at the test
 * statement, as such snapshots are usually applied while the test runs it.
 *
 * @param withSnapshotApplyGuard wraps each run of the observers' blocks
 * @param sendApplyNotifications sends the apply notifications of pending writes to global state
 * @param onFailure called with each error that a block throws, on the thread that applied the
 *   snapshot
 * @see StateChangeTracer
 */
internal class StateIntegrityChecker(
    private val withSnapshotApplyGuard: (() -> Unit) -> Unit,
    private val sendApplyNotifications: () -> Unit,
    private val onFailure: (Throwable) -> Unit,
) {
    private val lock = makeSynchronizedObject()
    private val observers = mutableListOf<StateIntegrityObserver>()
    private var snapshotObserverHandles: List<ObserverHandle>? = null
    // Created on the test thread when the first observer is added
    @Volatile private var tracer: StateChangeTracer? = null
    @Volatile private var tracedStates: Set<Any>? = emptySet()

    /** Starts running [observer] every time a snapshot is applied. */
    fun add(observer: StateIntegrityObserver) {
        startObserving()
        synchronized(lock) {
            if (tracer == null) tracer = StateChangeTracer()
            observers += observer
            tracedStates = null
        }
    }

    /**
     * Checks pending writes to global state, then stops running [observer], unless it was already
     * stopped.
     */
    fun remove(observer: StateIntegrityObserver) {
        if (synchronized(lock) { observer !in observers }) return
        checkPendingWrites()
        synchronized(lock) {
            observers -= observer
            if (observers.isEmpty()) stopTracingLocked()
        }
        observer.dispose()
    }

    /** Checks pending writes to global state, then stops running all observers. */
    fun stop() {
        if (synchronized(lock) { observers.isNotEmpty() }) checkPendingWrites()
        val toDispose = synchronized(lock) { ArrayList(observers).also { observers.clear() } }
        toDispose.fastForEach { it.dispose() }
        val handles =
            synchronized(lock) {
                stopTracingLocked()
                tracer = null
                snapshotObserverHandles.also { snapshotObserverHandles = null }
            }
        handles?.fastForEach { it.dispose() }
    }

    /** Checks pending writes, reporting errors with [onFailure] so that stopping never throws. */
    private fun checkPendingWrites() {
        runCatching { sendApplyNotifications() }.onFailure(onFailure)
    }

    /** Starts observing applies, and writes to global state, unless it already does. */
    private fun startObserving() {
        if (synchronized(lock) { snapshotObserverHandles != null }) return
        // TODO: Also observe writes in nested snapshots, with Snapshot.observeSnapshots, once it's
        //  stable
        val handles =
            listOf(
                Snapshot.registerApplyObserver { changed, _ -> onApplied(changed) },
                Snapshot.registerGlobalWriteObserver(::onWrite),
            )
        val isFirst =
            synchronized(lock) {
                (snapshotObserverHandles == null).also { if (it) snapshotObserverHandles = handles }
            }
        if (!isFirst) handles.fastForEach { it.dispose() }
    }

    private fun onWrite(state: Any) {
        val traced = tracedStates
        if (traced == null || state in traced) tracer?.recordWrite(state)
    }

    private fun onApplied(changed: Set<Any>) {
        val toCheck = synchronized(lock) { ArrayList(observers) }
        if (toCheck.isNotEmpty()) check(toCheck, changed)
    }

    /**
     * Runs [observers] in one read-only snapshot, and reports their errors, explained with the
     * [changed] state that they read.
     */
    private fun check(observers: List<StateIntegrityObserver>, changed: Set<Any>) {
        val reads = mutableSetOf<Any>()
        val allReads = mutableSetOf<Any>()
        // Not nested in a snapshot that the applying thread may be in, e.g. one that observes the
        // reads of a layout pass, so the blocks see global state and their reads don't leak to it
        val snapshot = Snapshot.global { Snapshot.takeSnapshot { reads += it } }
        try {
            withSnapshotApplyGuard {
                observers.fastForEach { observer ->
                    reads.clear()
                    val failure = observer.check(snapshot)
                    allReads.addAll(reads)
                    if (failure != null) {
                        explain(failure, observer, reads.filter { it in changed }, snapshot)
                        onFailure(failure)
                    }
                }
            }
        } finally {
            snapshot.dispose()
        }
        synchronized(lock) {
            // This apply consumed the recorded writes of the state that it changed, so they can't
            // explain a later change of that state
            tracer?.forgetWrites(changed)
            // Trace the writes of what the observers read, unless observers were added or removed
            if (this.observers == observers) {
                tracedStates = allReads
                tracer?.retainWrites(allReads)
            }
        }
    }

    /**
     * Makes [failure], which [observer] threw, show the test statements behind it.
     *
     * @param changedReads the state that the observer read and that changed in [snapshot]
     * @param snapshot the snapshot that the observer ran in, to describe the state as it saw it
     */
    private fun explain(
        failure: Throwable,
        observer: StateIntegrityObserver,
        changedReads: List<Any>,
        snapshot: Snapshot,
    ) {
        val tracer = tracer ?: return
        val changed = mutableMapOf<Any, String>()
        snapshot.enter {
            changedReads.fastForEach { state ->
                changed[state] = runCatching { state.toString() }.getOrDefault("State")
            }
        }
        try {
            tracer.explain(failure, changed, observer.registeredAt)
        } catch (e: Throwable) {
            // Still report the failure, with why it couldn't be explained
            failure.addSuppressed(e)
        }
    }

    private fun stopTracingLocked() {
        tracedStates = emptySet()
        tracer?.retainWrites(emptySet())
    }
}
