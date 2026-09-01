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

import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.internal.SnapshotThreadLocal

/**
 * An [IndirectState] is a [State] object that cannot be assigned to and generates its [value] from
 * a calculation of any number of other state objects.
 */
internal interface IndirectState<T> : State<T> {
    /**
     * Mutation policy that controls how changes are handled after state dependencies update. If the
     * policy is `null`, the state update is triggered regardless of the value produced, and it is
     * up to observer to invalidate it correctly.
     */
    val policy: SnapshotMutationPolicy<T>?

    /**
     * Compares [previousValue] against the current [value], and returns true if the objects are
     * equivalent and false otherwise. The equality of this comparison is defined by the associated
     * [policy] of this state. This function is called to check whether an invalidation to any of
     * the underlying states used to compute the value have led to a meaningfully new [value] of
     * this state. A false result should skip any work that would be done by an invalidation to this
     * state.
     *
     * Note that this function does not automatically track reads on states referenced by the
     * calculation. Dependencies must be explicitly re-read to track future invalidations of this
     * state.
     */
    fun isInvalidFor(previousValue: T): Boolean
}

internal interface IndirectStateObserver {
    fun start(state: IndirectState<*>)

    fun done(state: IndirectState<*>, calculatedValue: Any?)

    data object CalculationFailed
}

private val indirectStateObservers = SnapshotThreadLocal<MutableVector<IndirectStateObserver>>()

internal fun indirectStateObservers(): MutableVector<IndirectStateObserver> =
    indirectStateObservers.get()
        ?: MutableVector<IndirectStateObserver>(0).also { indirectStateObservers.set(it) }

internal inline fun <R> notifyObservers(state: IndirectState<*>, block: () -> R): R {
    val observers = indirectStateObservers()
    observers.forEach { it.start(state) }
    var result: Any? = IndirectStateObserver.CalculationFailed
    return try {
        block().also { result = it }
    } finally {
        observers.forEach { it.done(state, result) }
    }
}

/**
 * Observe the recalculations performed by any derived state that is recalculated during the
 * execution of [block].
 *
 * @param observer called for every calculation of a derived state in the [block].
 * @param block the block of code to observe.
 */
internal inline fun <R> observeIndirectStateRecalculations(
    observer: IndirectStateObserver,
    block: () -> R,
): R {
    val observers = indirectStateObservers()
    observers.add(observer)
    val result =
        try {
            block()
        } finally {
            observers.removeAt(observers.lastIndex)
        }
    return result
}
