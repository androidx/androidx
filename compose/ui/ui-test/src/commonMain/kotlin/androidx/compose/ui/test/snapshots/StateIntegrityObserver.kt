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
import kotlin.concurrent.Volatile

/**
 * A block registered with runOnEachSnapshotApply, which runs every time a snapshot is applied to
 * global state, until [dispose] is called.
 *
 * [StateIntegrityChecker] calls [check] on the thread that applied the snapshot, with a read-only
 * snapshot, so [assertion] sees one consistent state.
 *
 * @param assertion the block, which throws if the state it reads is invalid
 */
internal class StateIntegrityObserver(private val assertion: () -> Unit) : ObserverHandle {
    /** A [Throwable] created where the block was registered, so its stack trace shows where. */
    val registeredAt = Throwable()

    // The checker can run this observer from a copy of its list after dispose() is called
    @Volatile private var isDisposed = false

    override fun dispose() {
        isDisposed = true
    }

    /**
     * Runs [assertion] in [snapshot], unless disposed, and returns the error it threw, if any. The
     * frame of this function is right below the frames of [assertion] in the error's stack trace.
     */
    fun check(snapshot: Snapshot): Throwable? {
        if (isDisposed) return null
        return try {
            snapshot.enter(assertion)
            null
        } catch (e: Throwable) {
            e
        }
    }
}
