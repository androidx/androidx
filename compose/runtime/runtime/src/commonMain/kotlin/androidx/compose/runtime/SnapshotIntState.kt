/*
 * Copyright 2023 The Android Open Source Project
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

@file:JvmName("SnapshotIntStateKt")
@file:JvmMultifileClass
package androidx.compose.runtime

import androidx.compose.runtime.snapshots.AutoboxingStateValueProperty
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.overwritable
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.withCurrent
import kotlin.reflect.KProperty

/**
 * Return a new [MutableIntState] initialized with the passed in [value]
 *
 * The MutableIntState class is a single value holder whose reads and writes are observed by
 * Compose. Additionally, writes to it are transacted as part of the [Snapshot] system. On the JVM,
 * values are stored in memory as the primitive `int` type, avoiding the autoboxing that occurs when
 * using `MutableState<Int>`.
 *
 * @param value the initial value for the [MutableIntState]
 *
 * @see IntState
 * @see MutableIntState
 */
fun mutableStateOf(
    value: Int
): MutableIntState = createSnapshotMutableIntState(value)

/**
 * A value holder where reads to the [intValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value.
 *
 * @see MutableIntState
 * @see mutableStateOf
 */
@Stable
@JvmDefaultWithCompatibility
interface IntState : State<Int> {
    @AutoboxingStateValueProperty("intValue")
    override val value: Int
        @Suppress("AutoBoxing") get() = intValue

    val intValue: Int
}

/**
 * Permits property delegation of `val`s using `by` for [IntState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun IntState.getValue(thisObj: Any?, property: KProperty<*>): Int = intValue

/**
 * A value holder where reads to the [intValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value. When the
 * [intValue] property is written to and changed, a recomposition of any subscribed [RecomposeScope]s
 * will be scheduled. If [intValue] is written to with the same value, no recompositions will be
 * scheduled.
 *
 * @see [IntState]
 * @see [mutableStateOf]
 */
@Stable
@JvmDefaultWithCompatibility
interface MutableIntState : IntState, MutableState<Int> {
    @AutoboxingStateValueProperty("intValue")
    override var value: Int
        @Suppress("AutoBoxing") get() = intValue
        set(value) { intValue = value }

    override var intValue: Int
}

/**
 * Permits property delegation of `var`s using `by` for [MutableIntState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableIntState.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: Int
) {
    intValue = value
}

/**
 * Returns a platform-specific implementation of [MutableIntState] based on
 * [SnapshotMutableStateImpl].
 */
internal expect fun createSnapshotMutableIntState(
    value: Int
): MutableIntState

/**
 * A single value holder whose reads and writes are observed by Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the wrapped value
 *
 * @see [mutableStateOf]
 */
internal open class SnapshotMutableIntStateImpl(
    value: Int
) : StateObject, MutableIntState, SnapshotMutableState<Int> {

    private var next = IntStateStateRecord(value)

    override val firstStateRecord: StateRecord
        get() = next

    override var intValue: Int
        get() = next.readable(this).value
        set(value) = next.withCurrent {
            if (it.value != value) {
                next.overwritable(this, it) { this.value = value }
            }
        }

    // Arbitrary policies are not allowed. The underlying `==` implementation
    // for primitive types corresponds to structural equality
    override val policy: SnapshotMutationPolicy<Int>
        get() = structuralEqualityPolicy()

    override fun component1(): Int = intValue

    override fun component2(): (Int) -> Unit = { intValue = it }

    override fun prependStateRecord(value: StateRecord) {
        next = value as IntStateStateRecord
    }

    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord
    ): StateRecord? {
        val currentRecord = current as IntStateStateRecord
        val appliedRecord = applied as IntStateStateRecord
        return if (currentRecord.value == appliedRecord.value) {
            current
        } else {
            null
        }
    }

    override fun toString(): String = next.withCurrent {
        "MutableIntState(value=${it.value})@${hashCode()}"
    }

    @InternalComposeApi
    val debuggerDisplayValue: Int
        @JvmName("getDebuggerDisplayValue")
        get() = next.withCurrent { it.value }

    private class IntStateStateRecord(
        var value: Int
    ) : StateRecord() {
        override fun assign(value: StateRecord) {
            this.value = (value as IntStateStateRecord).value
        }

        override fun create(): StateRecord = IntStateStateRecord(value)
    }
}
