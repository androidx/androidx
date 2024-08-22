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

package androidx.compose.runtime.snapshots.tooling

import androidx.collection.ScatterSet
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.collection.wrapIntoSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.sync

/**
 * An observer for the snapshot system that notifies an observer when a snapshot is created,
 * applied, and/or disposed.
 *
 * All methods are called in the thread of the snapshot so all observers must be thread safe as they
 * may be called from any thread.
 *
 * Calling any of the Snapshot API (including, reading or writing mutable state objects) is not
 * supported and may produce inconsistent result or throw an exception.
 */
@ExperimentalComposeRuntimeApi
@Suppress("CallbackName")
interface SnapshotObserver {
    /**
     * Called before a snapshot is created allowing reads and writes to the snapshot to be observed.
     *
     * This method is called in the same thread that creates the snapshot.
     *
     * @param parent the parent snapshot for the new snapshot if it is a nested snapshot or null
     *   otherwise.
     * @param readonly whether the snapshot being created will be read-only.
     * @return optional read and write observers that will be added to the snapshot created.
     */
    fun onCreating(parent: Snapshot?, readonly: Boolean): SnapshotInstanceObservers? = null

    /**
     * Called after snapshot is created.
     *
     * This is called prior to the instance being returned by [Snapshot.takeSnapshot] or
     * [Snapshot.takeMutableSnapshot].
     *
     * This method is called in the same thread that creates the snapshot.
     *
     * @param snapshot the snapshot that was created.
     * @param parent the parent snapshot for the new snapshot if it is a nested snapshot or null if
     *   it is a root snapshot.
     * @param observers the read and write observers that were installed by the value returned by
     *   [onCreated]. This allows correlating which snapshot observers returned by [onCreating] to
     *   the [snapshot] that was created.
     */
    fun onCreated(snapshot: Snapshot, parent: Snapshot?, observers: SnapshotInstanceObservers?) {}

    /**
     * Called while a snapshot is being disposed.
     *
     * This method is called in the same thread that disposes the snapshot.
     *
     * @param snapshot information about the snapshot that was created.
     */
    fun onDisposing(snapshot: Snapshot) {}

    /**
     * Called after a snapshot is applied.
     *
     * For nested snapshots, the changes will only be visible to the parent snapshot, not globally.
     * Snapshots do not have a parent will have changes that are visible globally and such
     * notification are equivalent the notification sent to [Snapshot.registerApplyObserver] and
     * will include all objects modified by any nested snapshots that have been applied to the
     * parent snapshot.
     *
     * This method is called in the same thread that applies the snapshot.
     *
     * @param snapshot the snapshot that was applied.
     * @param changed the set of objects that were modified during the snapshot.
     */
    fun onApplied(snapshot: Snapshot, changed: Set<Any>) {}
}

/**
 * The return result of [SnapshotObserver.onCreating] allowing the reads and writes performed in the
 * newly created snapshot to be observed
 */
@ExperimentalComposeRuntimeApi
class SnapshotInstanceObservers(
    /**
     * Called whenever a state is read in the snapshot. This is called before the read observer
     * passed to [Snapshot.takeSnapshot] or [Snapshot.takeMutableSnapshot].
     *
     * This method is called in the same thread that reads snapshot state.
     */
    val readObserver: ((Any) -> Unit)? = null,

    /**
     * Called just before a state object is written to the first time in the snapshot or a nested
     * mutable snapshot. This might be called several times for the same object if nested mutable
     * snapshots are created as the unmodified value may be needed by the nested snapshot so a new
     * copy is created. This is not called for each write, only when the write results in the object
     * be recorded as being modified requiring a copy to be made before the write completes. This is
     * called before the write has been applied to the instance.
     *
     * This is called before the write observer passed to [Snapshot.takeMutableSnapshot].
     *
     * This method is called in the same thread that writes to the snapshot state.
     */
    val writeObserver: ((Any) -> Unit)? = null,
)

/**
 * This is a tooling API and is not intended to be used in a production application as it will
 * introduce global overhead to creating, applying and disposing all snapshots and, potentially, to
 * reading and writing all state objects.
 *
 * Observe when snapshots are created, applied, and/or disposed. The observer can also install read
 * and write observers on the snapshot being created.
 *
 * This method is thread-safe and calling [ObserverHandle.dispose] on the [ObserverHandle] returned
 * is also thread-safe.
 *
 * @param snapshotObserver the snapshot observer to install.
 * @return [ObserverHandle] an instance to unregister the [snapshotObserver].
 */
@ExperimentalComposeRuntimeApi
fun Snapshot.Companion.observeSnapshots(snapshotObserver: SnapshotObserver): ObserverHandle {
    sync { observers = (observers ?: persistentListOf()).add(snapshotObserver) }
    return ObserverHandle {
        sync {
            val newObservers = observers?.remove(snapshotObserver)
            observers = newObservers?.takeIf { it.isNotEmpty() }
        }
    }
}

@ExperimentalComposeRuntimeApi private var observers: PersistentList<SnapshotObserver>? = null

@ExperimentalComposeRuntimeApi
internal inline fun <R : Snapshot> creatingSnapshot(
    parent: Snapshot?,
    noinline readObserver: ((Any) -> Unit)?,
    noinline writeObserver: ((Any) -> Unit)?,
    readonly: Boolean,
    crossinline block: (readObserver: ((Any) -> Unit)?, writeObserver: ((Any) -> Unit)?) -> R
): R {
    var observerMap: Map<SnapshotObserver, SnapshotInstanceObservers>? = null
    val observers = observers
    var actualReadObserver = readObserver
    var actualWriteObserver = writeObserver
    if (observers != null) {
        val result = observers.mergeObservers(parent, readonly, readObserver, writeObserver)
        val mappedObservers = result.first
        actualReadObserver = mappedObservers.readObserver
        actualWriteObserver = mappedObservers.writeObserver
        observerMap = result.second
    }
    val result = block(actualReadObserver, actualWriteObserver)
    observers?.dispatchCreatedObservers(parent, result, observerMap)
    return result
}

@ExperimentalComposeRuntimeApi
internal fun PersistentList<SnapshotObserver>.mergeObservers(
    parent: Snapshot?,
    readonly: Boolean,
    readObserver: ((Any) -> Unit)?,
    writeObserver: ((Any) -> Unit)?,
): Pair<SnapshotInstanceObservers, Map<SnapshotObserver, SnapshotInstanceObservers>?> {
    var currentReadObserver = readObserver
    var currentWriteObserver = writeObserver
    var observerMap: MutableMap<SnapshotObserver, SnapshotInstanceObservers>? = null
    fastForEach { observer ->
        val instance = observer.onCreating(parent, readonly)
        if (instance != null) {
            currentReadObserver = mergeObservers(instance.readObserver, currentReadObserver)
            currentWriteObserver = mergeObservers(instance.writeObserver, currentWriteObserver)
            (observerMap
                ?: run {
                    val newMap = mutableMapOf<SnapshotObserver, SnapshotInstanceObservers>()
                    observerMap = newMap
                    newMap
                })[observer] = instance
        }
    }
    return SnapshotInstanceObservers(currentReadObserver, currentWriteObserver) to observerMap
}

private fun mergeObservers(a: ((Any) -> Unit)?, b: ((Any) -> Unit)?): ((Any) -> Unit)? {
    return if (a != null && b != null) {
        {
            a(it)
            b(it)
        }
    } else a ?: b
}

@ExperimentalComposeRuntimeApi
internal fun PersistentList<SnapshotObserver>.dispatchCreatedObservers(
    parent: Snapshot?,
    result: Snapshot,
    observerMap: Map<SnapshotObserver, SnapshotInstanceObservers>?
) {
    fastForEach { observer ->
        val instance = observerMap?.get(observer)
        observer.onCreated(result, parent, instance)
    }
}

@OptIn(ExperimentalComposeRuntimeApi::class)
internal fun dispatchObserverOnDispose(snapshot: Snapshot) {
    observers?.fastForEach { observer -> observer.onDisposing(snapshot) }
}

@OptIn(ExperimentalComposeRuntimeApi::class)
internal fun dispatchObserverOnApplied(snapshot: Snapshot, changes: ScatterSet<StateObject>?) {
    val observers = observers
    if (!observers.isNullOrEmpty()) {
        val wrappedChanges = changes?.wrapIntoSet() ?: emptySet()
        observers.fastForEach { observer -> observer.onApplied(snapshot, wrappedChanges) }
    }
}
