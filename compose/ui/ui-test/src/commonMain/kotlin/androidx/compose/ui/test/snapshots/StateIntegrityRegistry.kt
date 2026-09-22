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

/**
 * Manages the blocks that a test registers with
 * [runOnEachSnapshotApply][androidx.compose.ui.test.ComposeUiTest.runOnEachSnapshotApply], from
 * when they're registered until they're disposed or the test finishes.
 *
 * @param withSnapshotApplyGuard wraps each run of the blocks, so that the test environment can
 *   catch blocks that try to synchronize with the UI
 * @param sendApplyNotifications sends the apply notifications of pending writes to global state, so
 *   that the blocks check them before they stop. If another thread can be sending them too, e.g.
 *   the main thread, it must also wait until that thread is done, or the blocks can miss them.
 */
internal class StateIntegrityRegistry(
    withSnapshotApplyGuard: (() -> Unit) -> Unit = { it() },
    sendApplyNotifications: () -> Unit = { Snapshot.sendApplyNotifications() },
) {
    private val lock = makeSynchronizedObject()
    private var pendingFailure: Throwable? = null
    private val checker =
        StateIntegrityChecker(
            withSnapshotApplyGuard,
            sendApplyNotifications,
            onFailure = ::recordFailure,
        )

    /**
     * Starts running [block] every time a snapshot is applied, until the returned handle is
     * disposed or the test finishes.
     */
    fun runOnEachSnapshotApply(block: () -> Unit): ObserverHandle {
        val observer = StateIntegrityObserver(block)
        checker.add(observer)
        return ObserverHandle { checker.remove(observer) }
    }

    /** Throws the first error that a block threw since the last call, if any. */
    fun throwIfFailed() {
        takeFailure()?.let { throw it }
    }

    /**
     * Runs [block], the test, then stops the remaining blocks. Throws the first error that they
     * threw, or adds it to the error that the test threw.
     */
    inline fun <R> withRegistry(block: () -> R): R {
        var testFailure: Throwable? = null
        try {
            return block()
        } catch (t: Throwable) {
            testFailure = t
            throw t
        } finally {
            tearDown()
            reportPendingFailure(testFailure)
        }
    }

    /** Checks pending writes to global state once, then stops the remaining blocks. */
    fun tearDown() {
        checker.stop()
    }

    /** Throws the pending error if the test passed, or adds it to [testFailure] as suppressed. */
    private fun reportPendingFailure(testFailure: Throwable?) {
        val failure = takeFailure() ?: return
        if (testFailure == null) throw failure
        if (failure !== testFailure) testFailure.addSuppressed(failure)
    }

    /**
     * Keeps only the first error until it's thrown. Later ones are usually the same broken
     * invariant failing again on each snapshot.
     */
    private fun recordFailure(failure: Throwable) {
        synchronized(lock) { if (pendingFailure == null) pendingFailure = failure }
    }

    /** Returns the pending error, if any, and forgets it. */
    private fun takeFailure(): Throwable? =
        synchronized(lock) { pendingFailure.also { pendingFailure = null } }
}
