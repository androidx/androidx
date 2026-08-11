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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.IndirectState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.runtime.snapshots.current
import androidx.compose.runtime.snapshots.notifyObservers
import kotlin.jvm.JvmName

/**
 * A [ComputedState] is a [State] object whose [value] is the result of a supplied calculation. The
 * calculation is executed each time that [value] is read, and is not cached.
 */
internal interface ComputedState<T> : IndirectState<T>

private class ComputedSnapshotState<T>(
    private val calculation: () -> T,
    override val policy: SnapshotMutationPolicy<T>,
) : ComputedState<T> {

    override val value: T
        get() {
            Snapshot.current.readObserver?.invoke(this)
            return notifyObservers(this, calculation)
        }

    override fun isInvalidFor(previousValue: T): Boolean {
        return !policy.equivalent(Snapshot.withoutReadObservation(calculation), previousValue)
    }

    override fun toString(): String =
        "ComputedState(calculation=${calculation}: ${displayValue()})@${hashCode()}"

    /**
     * A function used by the debugger to display the value of the current value of the mutable
     * state object without triggering read observers.
     */
    @Suppress("unused")
    val debuggerDisplayValue: T
        @JvmName("getDebuggerDisplayValue") get() = displayValue()

    private fun displayValue() = Snapshot.withoutReadObservation(calculation)
}

/**
 * Creates a [State] object whose [State.value] is the result of [calculation]. The calculation is
 * executed each time the value is read and is not cached when re-reading the value. Reading the
 * value of the state multiple times in the same snapshot will execute [calculation] each time, and
 * the current snapshot will subscribe to the state objects referenced in the calculation.
 *
 * Note that the calculation lambda may be called more times than [State.value] is read as part of
 * managing the state and checking its validity.
 *
 * If any of the referenced states are modified, reads of the state are only considered to be
 * invalid if the [calculation] lambda returns a value that is not equal to the last value read by
 * the observer, with equality being defined by the given [policy]. In composition, this notably
 * means that invalidations triggered by reads in a [computedStateOf] will conditionally invalidate
 * the body of the composable based on whether the new result of [calculation] is different from the
 * last value read at that point. It's considered an error to use [neverEqualPolicy] with
 * [computedStateOf], as this defeats all skipping behavior provided by using a ComputedState.
 *
 * A [ComputedState][computedStateOf] should be preferred over a [DerivedState][derivedStateOf] when
 * you don't need the caching behavior that `DerivedState` provides.
 *
 * @param policy mutation policy to control when changes to the [calculation] result trigger update.
 * @param calculation the calculation to create the value this state object represents.
 */
@StateFactoryMarker
public fun <T> computedStateOf(
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
    calculation: () -> T,
): State<T> = ComputedSnapshotState(calculation, policy)
