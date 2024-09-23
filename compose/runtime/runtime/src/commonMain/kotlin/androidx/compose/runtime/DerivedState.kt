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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass

package androidx.compose.runtime

import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.SnapshotThreadLocal
import androidx.compose.runtime.internal.identityHashCode
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateObjectImpl
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.current
import androidx.compose.runtime.snapshots.currentSnapshot
import androidx.compose.runtime.snapshots.newWritableRecord
import androidx.compose.runtime.snapshots.sync
import androidx.compose.runtime.snapshots.withCurrent
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.math.min

/**
 * A [State] that is derived from one or more other states.
 *
 * @see derivedStateOf
 */
internal interface DerivedState<T> : State<T> {
    /** Provides a current [Record]. */
    val currentRecord: Record<T>

    /**
     * Mutation policy that controls how changes are handled after state dependencies update. If the
     * policy is `null`, the derived state update is triggered regardless of the value produced and
     * it is up to observer to invalidate it correctly.
     */
    val policy: SnapshotMutationPolicy<T>?

    interface Record<T> {
        /**
         * The value of the derived state retrieved without triggering a notification to read
         * observers.
         */
        val currentValue: T

        /**
         * Map of the dependencies used to produce [value] or [currentValue] to nested read level.
         *
         * This map can be used to determine if the state could affect value of this derived state,
         * when a [StateObject] appears in the apply observer set.
         */
        val dependencies: ObjectIntMap<StateObject>
    }
}

private val calculationBlockNestedLevel = SnapshotThreadLocal<IntRef>()

private inline fun <T> withCalculationNestedLevel(block: (IntRef) -> T): T {
    val ref =
        calculationBlockNestedLevel.get() ?: IntRef(0).also { calculationBlockNestedLevel.set(it) }
    return block(ref)
}

private class DerivedSnapshotState<T>(
    private val calculation: () -> T,
    override val policy: SnapshotMutationPolicy<T>?
) : StateObjectImpl(), DerivedState<T> {
    private var first: ResultRecord<T> = ResultRecord(currentSnapshot().id)

    class ResultRecord<T>(snapshotId: Int) : StateRecord(snapshotId), DerivedState.Record<T> {
        companion object {
            val Unset = Any()
        }

        var validSnapshotId: Int = 0
        var validSnapshotWriteCount: Int = 0

        override var dependencies: ObjectIntMap<StateObject> = emptyObjectIntMap()
        var result: Any? = Unset
        var resultHash: Int = 0

        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST") val other = value as ResultRecord<T>
            dependencies = other.dependencies
            result = other.result
            resultHash = other.resultHash
        }

        override fun create(): StateRecord = create(currentSnapshot().id)

        override fun create(snapshotId: Int): StateRecord = ResultRecord<T>(snapshotId)

        fun isValid(derivedState: DerivedState<*>, snapshot: Snapshot): Boolean {
            val snapshotChanged = sync {
                validSnapshotId != snapshot.id || validSnapshotWriteCount != snapshot.writeCount
            }
            val isValid =
                result !== Unset &&
                    (!snapshotChanged || resultHash == readableHash(derivedState, snapshot))

            if (isValid && snapshotChanged) {
                sync {
                    validSnapshotId = snapshot.id
                    validSnapshotWriteCount = snapshot.writeCount
                }
            }

            return isValid
        }

        fun readableHash(derivedState: DerivedState<*>, snapshot: Snapshot): Int {
            var hash = 7
            val dependencies = sync { dependencies }
            if (dependencies.isNotEmpty()) {
                notifyObservers(derivedState) {
                    dependencies.forEach { stateObject, readLevel ->
                        if (readLevel != 1) {
                            return@forEach
                        }

                        // Find the first record without triggering an observer read.
                        val record =
                            if (stateObject is DerivedSnapshotState<*>) {
                                // eagerly access the parent derived states without recording the
                                // read
                                // that way we can be sure derived states in deps were recalculated,
                                // and are updated to the last values
                                stateObject.current(snapshot)
                            } else {
                                current(stateObject.firstStateRecord, snapshot)
                            }

                        hash = 31 * hash + identityHashCode(record)
                        hash = 31 * hash + record.snapshotId
                    }
                }
            }
            return hash
        }

        override val currentValue: T
            @Suppress("UNCHECKED_CAST") get() = result as T
    }

    /**
     * Get current record in snapshot. Forces recalculation if record is invalid to refresh state
     * value.
     *
     * @return latest state record for the derived state.
     */
    fun current(snapshot: Snapshot): StateRecord =
        currentRecord(current(first, snapshot), snapshot, false, calculation)

    private fun currentRecord(
        readable: ResultRecord<T>,
        snapshot: Snapshot,
        forceDependencyReads: Boolean,
        calculation: () -> T
    ): ResultRecord<T> {
        if (readable.isValid(this, snapshot)) {
            // If the dependency is not recalculated, emulate nested state reads
            // for correct invalidation later
            if (forceDependencyReads) {
                notifyObservers(this) {
                    val dependencies = readable.dependencies
                    withCalculationNestedLevel { calculationLevelRef ->
                        val invalidationNestedLevel = calculationLevelRef.element
                        dependencies.forEach { dependency, nestedLevel ->
                            calculationLevelRef.element = invalidationNestedLevel + nestedLevel
                            snapshot.readObserver?.invoke(dependency)
                        }
                        calculationLevelRef.element = invalidationNestedLevel
                    }
                }
            }
            return readable
        }

        val newDependencies = MutableObjectIntMap<StateObject>()
        val result = withCalculationNestedLevel { calculationLevelRef ->
            val nestedCalculationLevel = calculationLevelRef.element
            notifyObservers(this) {
                calculationLevelRef.element = nestedCalculationLevel + 1

                val result =
                    Snapshot.observe(
                        {
                            if (it === this) error("A derived state calculation cannot read itself")
                            if (it is StateObject) {
                                val readNestedLevel = calculationLevelRef.element
                                newDependencies[it] =
                                    min(
                                        readNestedLevel - nestedCalculationLevel,
                                        newDependencies.getOrDefault(it, Int.MAX_VALUE)
                                    )
                            }
                        },
                        null,
                        calculation
                    )

                calculationLevelRef.element = nestedCalculationLevel
                result
            }
        }

        val record = sync {
            val currentSnapshot = Snapshot.current

            if (
                readable.result !== ResultRecord.Unset &&
                    @Suppress("UNCHECKED_CAST") policy?.equivalent(result, readable.result as T) ==
                        true
            ) {
                readable.dependencies = newDependencies
                readable.resultHash = readable.readableHash(this, currentSnapshot)
                readable
            } else {
                val writable = first.newWritableRecord(this, currentSnapshot)
                writable.dependencies = newDependencies
                writable.resultHash = writable.readableHash(this, currentSnapshot)
                writable.result = result
                writable
            }
        }

        if (calculationBlockNestedLevel.get()?.element == 0) {
            Snapshot.notifyObjectsInitialized()

            sync {
                val currentSnapshot = Snapshot.current
                record.validSnapshotId = currentSnapshot.id
                record.validSnapshotWriteCount = currentSnapshot.writeCount
            }
        }

        return record
    }

    override val firstStateRecord: StateRecord
        get() = first

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        first = value as ResultRecord<T>
    }

    override val value: T
        get() {
            // Unlike most state objects, the record list of a derived state can change during a
            // read
            // because reading updates the cache. To account for this, instead of calling readable,
            // which sends the read notification, the read observer is notified directly and current
            // value is used instead which doesn't notify. This allow the read observer to read the
            // value and only update the cache once.
            Snapshot.current.readObserver?.invoke(this)
            // Read observer could advance the snapshot, so get current snapshot again
            val snapshot = Snapshot.current
            val record = current(first, snapshot)
            @Suppress("UNCHECKED_CAST")
            return currentRecord(record, snapshot, true, calculation).result as T
        }

    override val currentRecord: DerivedState.Record<T>
        get() {
            val snapshot = Snapshot.current
            val record = current(first, snapshot)
            return currentRecord(record, snapshot, false, calculation)
        }

    override fun toString(): String =
        first.withCurrent { "DerivedState(value=${displayValue()})@${hashCode()}" }

    /**
     * A function used by the debugger to display the value of the current value of the mutable
     * state object without triggering read observers.
     */
    @Suppress("unused")
    val debuggerDisplayValue: T?
        @JvmName("getDebuggerDisplayValue")
        get() =
            first.withCurrent {
                @Suppress("UNCHECKED_CAST")
                if (it.isValid(this, Snapshot.current)) it.result as T else null
            }

    private fun displayValue(): String {
        first.withCurrent {
            if (it.isValid(this, Snapshot.current)) {
                return it.result.toString()
            }
            return "<Not calculated>"
        }
    }
}

/**
 * Creates a [State] object whose [State.value] is the result of [calculation]. The result of
 * calculation will be cached in such a way that calling [State.value] repeatedly will not cause
 * [calculation] to be executed multiple times, but reading [State.value] will cause all [State]
 * objects that got read during the [calculation] to be read in the current [Snapshot], meaning that
 * this will correctly subscribe to the derived state objects if the value is being read in an
 * observed context such as a [Composable] function. Derived states without mutation policy trigger
 * updates on each dependency change. To avoid invalidation on update, provide suitable
 * [SnapshotMutationPolicy] through [derivedStateOf] overload.
 *
 * @sample androidx.compose.runtime.samples.DerivedStateSample
 * @param calculation the calculation to create the value this state object represents.
 */
@StateFactoryMarker
fun <T> derivedStateOf(
    calculation: () -> T,
): State<T> = DerivedSnapshotState(calculation, null)

/**
 * Creates a [State] object whose [State.value] is the result of [calculation]. The result of
 * calculation will be cached in such a way that calling [State.value] repeatedly will not cause
 * [calculation] to be executed multiple times, but reading [State.value] will cause all [State]
 * objects that got read during the [calculation] to be read in the current [Snapshot], meaning that
 * this will correctly subscribe to the derived state objects if the value is being read in an
 * observed context such as a [Composable] function.
 *
 * @sample androidx.compose.runtime.samples.DerivedStateSample
 * @param policy mutation policy to control when changes to the [calculation] result trigger update.
 * @param calculation the calculation to create the value this state object represents.
 */
@StateFactoryMarker
fun <T> derivedStateOf(
    policy: SnapshotMutationPolicy<T>,
    calculation: () -> T,
): State<T> = DerivedSnapshotState(calculation, policy)

/** Observe the recalculations performed by derived states. */
internal interface DerivedStateObserver {
    /** Called before a calculation starts. */
    fun start(derivedState: DerivedState<*>)

    /** Called after the started calculation is complete. */
    fun done(derivedState: DerivedState<*>)
}

private val derivedStateObservers = SnapshotThreadLocal<MutableVector<DerivedStateObserver>>()

internal fun derivedStateObservers(): MutableVector<DerivedStateObserver> =
    derivedStateObservers.get()
        ?: MutableVector<DerivedStateObserver>(0).also { derivedStateObservers.set(it) }

private inline fun <R> notifyObservers(derivedState: DerivedState<*>, block: () -> R): R {
    val observers = derivedStateObservers()
    observers.forEach { it.start(derivedState) }
    return try {
        block()
    } finally {
        observers.forEach { it.done(derivedState) }
    }
}

/**
 * Observe the recalculations performed by any derived state that is recalculated during the
 * execution of [block].
 *
 * @param observer called for every calculation of a derived state in the [block].
 * @param block the block of code to observe.
 */
internal inline fun <R> observeDerivedStateRecalculations(
    observer: DerivedStateObserver,
    block: () -> R
) {
    val observers = derivedStateObservers()
    try {
        observers.add(observer)
        block()
    } finally {
        observers.removeAt(observers.lastIndex)
    }
}
