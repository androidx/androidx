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

@file:JvmName("SnapshotLongStateKt")
@file:JvmMultifileClass
package androidx.compose.runtime

import androidx.compose.runtime.internal.JvmDefaultWithCompatibility
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
 * Return a new [MutableLongState] initialized with the passed in [value]
 *
 * The MutableLongState class is a single value holder whose reads and writes are observed by
 * Compose. Additionally, writes to it are transacted as part of the [Snapshot] system. On the JVM,
 * values are stored in memory as the primitive `long` type, avoiding the autoboxing that occurs
 * when using `MutableState<Long>`.
 *
 * @param value the initial value for the [MutableLongState]
 *
 * @see LongState
 * @see MutableLongState
 */
fun mutableStateOf(
    value: Long
): MutableLongState = createSnapshotMutableLongState(value)

/**
 * A value holder where reads to the [longValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value.
 *
 * @see MutableLongState
 * @see mutableStateOf
 */
@Stable
@JvmDefaultWithCompatibility
interface LongState : State<Long> {
    @AutoboxingStateValueProperty("longValue")
    override val value: Long
        @Suppress("AutoBoxing") get() = longValue

    val longValue: Long
}

/**
 * Permits property delegation of `val`s using `by` for [LongState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun LongState.getValue(thisObj: Any?, property: KProperty<*>): Long = longValue

/**
 * A value holder where reads to the [longValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value. When the
 * [longValue] property is written to and changed, a recomposition of any subscribed [RecomposeScope]s
 * will be scheduled. If [longValue] is written to with the same value, no recompositions will be
 * scheduled.
 *
 * @see [LongState]
 * @see [mutableStateOf]
 */
@Stable
@JvmDefaultWithCompatibility
interface MutableLongState : LongState, MutableState<Long> {
    @AutoboxingStateValueProperty("longValue")
    override var value: Long
        @Suppress("AutoBoxing") get() = longValue
        set(value) { longValue = value }

    override var longValue: Long
}

/**
 * Permits property delegation of `var`s using `by` for [MutableLongState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableLongState.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: Long
) {
    longValue = value
}

internal expect fun createSnapshotMutableLongState(
    value: Long
): MutableLongState

/**
 * A single value holder whose reads and writes are observed by Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the wrapped value
 *
 * @see [mutableStateOf]
 */
internal open class SnapshotMutableLongStateImpl(
    value: Long
) : StateObject, MutableLongState, SnapshotMutableState<Long> {

    private var next = LongStateStateRecord(value)

    override val firstStateRecord: StateRecord
        get() = next

    override var longValue: Long
        get() = next.readable(this).value
        set(value) = next.withCurrent {
            if (it.value != value) {
                next.overwritable(this, it) { this.value = value }
            }
        }

    // Arbitrary policies are not allowed. The underlying `==` implementation
    // for primitive types corresponds to structural equality
    override val policy: SnapshotMutationPolicy<Long>
        get() = structuralEqualityPolicy()

    override fun component1(): Long = longValue

    override fun component2(): (Long) -> Unit = { longValue = it }

    override fun prependStateRecord(value: StateRecord) {
        next = value as LongStateStateRecord
    }

    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord
    ): StateRecord? {
        val currentRecord = current as LongStateStateRecord
        val appliedRecord = applied as LongStateStateRecord
        return if (currentRecord.value == appliedRecord.value) {
            current
        } else {
            null
        }
    }

    override fun toString(): String = next.withCurrent {
        "MutableLongState(value=${it.value})@${hashCode()}"
    }

    private class LongStateStateRecord(
        var value: Long
    ) : StateRecord() {
        override fun assign(value: StateRecord) {
            this.value = (value as LongStateStateRecord).value
        }

        override fun create(): StateRecord = LongStateStateRecord(value)
    }
}
