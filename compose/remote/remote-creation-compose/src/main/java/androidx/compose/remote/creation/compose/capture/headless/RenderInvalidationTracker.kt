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

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver

/**
 * Runs [block], a render, inside a new mutable snapshot so that it sees a consistent view of state
 * while other threads keep writing.
 *
 * A render must only read state. Writes made while rendering are rejected with an
 * [IllegalStateException] unless [applyWrites] is `true`, in which case they are applied when the
 * block completes (kept for single-shot captures that do not enforce clean recomposition; a
 * streaming capture would re-render forever if a render wrote a state it also read).
 *
 * Not applying is also what keeps this snapshot out of the way of concurrent sessions:
 * `MutableSnapshot.apply()` claims any pending global snapshot writes and delivers their apply
 * notifications without being tracked by [Snapshot.isApplyObserverNotificationPending]
 * (b/573872754), so an apply here could take writes made by another session's effects, which could
 * then sample itself as quiescent before its recomposer has seen them.
 */
internal fun <T> withRenderSnapshot(applyWrites: Boolean, block: () -> T): T {
    val snapshot = Snapshot.takeMutableSnapshot()
    try {
        val result = snapshot.enter(block)
        if (snapshot.hasPendingChanges()) {
            check(applyWrites) {
                "Snapshot state was written while rendering. Rendering must only read state; " +
                    "move the write into composition or an effect."
            }
            snapshot.apply().check()
        }
        return result
    } finally {
        snapshot.dispose()
    }
}

/**
 * Decides when a capture loop must actually re-render and re-encode the document. Every apply
 * notification wakes the loop so the recomposer is driven to quiescence, but a render is only
 * performed if:
 * - composition applied changes to the node tree since the previous render
 *   (`RemoteComposeApplier.changeCount` moved),
 * - a state read by the previous render changed (e.g. inside a `RemoteCanvas` draw lambda, which
 *   runs at render time), or
 * - any change landed while a render was in progress.
 *
 * Unrelated global snapshot writes elsewhere in the process therefore do not cost a full render and
 * encode.
 *
 * The states read by a render are tracked by a [SnapshotStateObserver], the same mechanism Compose
 * UI uses for reads during layout and draw (`OwnerSnapshotObserver`): the render runs inside
 * [SnapshotStateObserver.observeReads] with this tracker as the scope, and the observer calls
 * [onRenderReadChanged] when any of those states is later changed. Renders do not write state (see
 * [withRenderSnapshot]), so a notification can never be caused by the render itself.
 *
 * Limits: render inputs that are not snapshot state (e.g. plain fields mutated from an effect) are
 * not observed; they are only rendered alongside the next tree change or render-read state change.
 *
 * Thread-safe: [onApplied] and [onRenderReadChanged] run on whichever thread sends apply
 * notifications.
 */
internal class RenderInvalidationTracker {
    private val lock = Any()
    private var isRendering = false
    private var renderInputsChanged = true
    private var lastRenderChangeCount = 0L

    /**
     * Apply observer hook. A change that lands while a render is in progress may have been missed
     * by both that render's snapshot and the observer's (just cleared) read set, so render again.
     */
    fun onApplied() {
        synchronized(lock) { if (isRendering) renderInputsChanged = true }
    }

    /** [SnapshotStateObserver] hook: a state read by the previous render changed. */
    fun onRenderReadChanged() {
        synchronized(lock) { renderInputsChanged = true }
    }

    /**
     * Returns `true` and marks a render as in progress if the document may have changed since the
     * previous render; `false` if rendering can be skipped.
     */
    fun beginRender(changeCount: Long): Boolean =
        synchronized(lock) {
            val shouldRender = renderInputsChanged || changeCount != lastRenderChangeCount
            if (shouldRender) {
                renderInputsChanged = false
                lastRenderChangeCount = changeCount
                isRendering = true
            }
            shouldRender
        }

    /** Ends a render started by [beginRender]. */
    fun endRender() {
        synchronized(lock) { isRendering = false }
    }
}
