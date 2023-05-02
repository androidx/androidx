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

@file:JvmName("SnapshotDoubleStateKt")
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
 * Return a new [MutableDoubleState] initialized with the passed in [value]
 *
 * The MutableDoubleState class is a single value holder whose reads and writes are observed by
 * Compose. Additionally, writes to it are transacted as part of the [Snapshot] system. On the JVM,
 * values are stored in memory as the primitive `double` type, avoiding the autoboxing that occurs
 * when using `MutableState<Double>`.
 *
 * @param value the initial value for the [MutableDoubleState]
 *
 * @see DoubleState
 * @see MutableDoubleState
 * @see mutableStateOf
 * @see mutableIntStateOf
 * @see mutableLongStateOf
 * @see mutableFloatStateOf
 */
fun mutableDoubleStateOf(
    value: Double
): MutableDoubleState = createSnapshotMutableDoubleState(value)

/**
 * A value holder where reads to the [doubleValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value.
 *
 * @see MutableDoubleState
 * @see mutableDoubleStateOf
 */
@Stable
@JvmDefaultWithCompatibility
interface DoubleState : State<Double> {
    @AutoboxingStateValueProperty("doubleValue")
    override val value: Double
        @Suppress("AutoBoxing") get() = doubleValue

    val doubleValue: Double
}

/**
 * Permits property delegation of `val`s using `by` for [DoubleState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun DoubleState.getValue(
    thisObj: Any?,
    property: KProperty<*>
): Double = doubleValue

/**
 * A value holder where reads to the [doubleValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value. When the
 * [doubleValue] property is written to and changed, a recomposition of any subscribed [RecomposeScope]s
 * will be scheduled. If [doubleValue] is written to with the same value, no recompositions will be
 * scheduled.
 *
 * @see [DoubleState]
 * @see [mutableDoubleStateOf]
 */
@Stable
@JvmDefaultWithCompatibility
interface MutableDoubleState : DoubleState, MutableState<Double> {
    @AutoboxingStateValueProperty("doubleValue")
    override var value: Double
        @Suppress("AutoBoxing") get() = doubleValue
        set(value) { doubleValue = value }

    override var doubleValue: Double
}

/**
 * Permits property delegation of `var`s using `by` for [MutableDoubleState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableDoubleState.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: Double
) {
    this.doubleValue = value
}

internal expect fun createSnapshotMutableDoubleState(
    value: Double
): MutableDoubleState

/**
 * A single value holder whose reads and writes are observed by Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the wrapped value
 *
 * @see [mutableDoubleStateOf]
 */
internal open class SnapshotMutableDoubleStateImpl(
    value: Double
) : StateObject, MutableDoubleState, SnapshotMutableState<Double> {

    private var next = DoubleStateStateRecord(value)

    override val firstStateRecord: StateRecord
        get() = next

    override var doubleValue: Double
        get() = next.readable(this).value
        set(value) = next.withCurrent {
            if (it.value != value) {
                next.overwritable(this, it) { this.value = value }
            }
        }

    // Arbitrary policies are not allowed. The underlying `==` implementation
    // for primitive types corresponds to structural equality
    override val policy: SnapshotMutationPolicy<Double>
        get() = structuralEqualityPolicy()

    override fun component1(): Double = doubleValue

    override fun component2(): (Double) -> Unit = { doubleValue = it }

    override fun prependStateRecord(value: StateRecord) {
        next = value as DoubleStateStateRecord
    }

    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord
    ): StateRecord? {
        val currentRecord = current as DoubleStateStateRecord
        val appliedRecord = applied as DoubleStateStateRecord
        return if (currentRecord.value == appliedRecord.value) {
            current
        } else {
            null
        }
    }

    override fun toString(): String = next.withCurrent {
        "MutableDoubleState(value=${it.value})@${hashCode()}"
    }

    private class DoubleStateStateRecord(
        var value: Double
    ) : StateRecord() {
        override fun assign(value: StateRecord) {
            this.value = (value as DoubleStateStateRecord).value
        }

        override fun create(): StateRecord = DoubleStateStateRecord(value)
    }
}
