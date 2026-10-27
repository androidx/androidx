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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.internal.AtomicReference

/** A queue of the sets of objects changed by applied snapshots. */
internal class SnapshotChangeQueue {
    private val pendingChanges = AtomicReference<Any?>(null)

    /**
     * Add changes to the changes queue. This uses an atomic reference as a queue to minimize the
     * number of allocations required in the normal case. If, for example, only one set is added to
     * the queue, the set itself is the atomic reference. If the queue is empty the reference is
     * null. Only if there are more than one set added to the queue is an allocation required, then
     * the atomic reference is a list containing all the sets in the queue. Given the size of the
     * queue, the type of object referenced is, 0 -> null 1 -> Set<Any?> 2 or more ->
     * List<Set<Any?>>
     */
    fun add(set: Set<Any>) {
        while (true) {
            val old = pendingChanges.get()
            val new =
                when (old) {
                    null -> set
                    is Set<*> -> listOf(old, set)
                    is List<*> -> old + listOf(set)
                    else -> report()
                }
            if (pendingChanges.compareAndSet(old, new)) break
        }
    }

    /**
     * Remove a set of changes from the change queue. See [add] for a description of how this queue
     * works.
     */
    @Suppress("UNCHECKED_CAST")
    fun remove(): Set<Any>? {
        while (true) {
            val old = pendingChanges.get()
            val result: Set<Any>?
            val new: Any?
            when (old) {
                null -> return null // The queue is empty
                is Set<*> -> {
                    result = old as Set<Any>?
                    new = null
                }
                is List<*> -> {
                    result = old[0] as Set<Any>?
                    new =
                        when {
                            old.size == 2 -> old[1]
                            old.size > 2 -> old.subList(1, old.size)
                            else -> null
                        }
                }
                else -> report()
            }
            if (pendingChanges.compareAndSet(old, new)) {
                return result
            }
        }
    }

    private fun report(): Nothing = composeRuntimeError("Unexpected notification")
}
