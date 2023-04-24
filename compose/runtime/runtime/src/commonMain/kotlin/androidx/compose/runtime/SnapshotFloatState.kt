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

@file:JvmName("PrimitiveSnapshotStateKt")
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
 * Return a new [MutableFloatState] initialized with the passed in [value]
 *
 * The MutableFloatState class is a single value holder whose reads and writes are observed by
 * Compose. Additionally, writes to it are transacted as part of the [Snapshot] system. On the JVM,
 * values are stored in memory as the primitive `float` type, avoiding the autoboxing that occurs
 * when using `MutableState<Float>`.
 *
 * @param value the initial value for the [MutableFloatState]
 *
 * @see FloatState
 * @see MutableFloatState
 * @see mutableStateOf
 * @see mutableIntStateOf
 * @see mutableLongStateOf
 * @see mutableDoubleStateOf
 */
fun mutableFloatStateOf(
    value: Float
): MutableFloatState = createSnapshotMutableFloatState(value)

/**
 * A value holder where reads to the [floatValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value.
 *
 * @see MutableFloatState
 * @see mutableDoubleStateOf
 */
@Stable
@JvmDefaultWithCompatibility
interface FloatState : State<Float> {
    @AutoboxingStateValueProperty("floatValue")
    override val value: Float
        @Suppress("AutoBoxing") get() = floatValue

    val floatValue: Float
}

/**
 * Permits property delegation of `val`s using `by` for [FloatState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun FloatState.getValue(thisObj: Any?, property: KProperty<*>): Float = floatValue

/**
 * A value holder where reads to the [floatValue] property during the execution of a [Composable]
 * function cause the current [RecomposeScope] to subscribe to changes of that value. When the
 * [floatValue] property is written to and changed, a recomposition of any subscribed [RecomposeScope]s
 * will be scheduled. If [floatValue] is written to with the same value, no recompositions will be
 * scheduled.
 *
 * @see [FloatState]
 * @see [mutableDoubleStateOf]
 */
@Stable
@JvmDefaultWithCompatibility
interface MutableFloatState : FloatState, MutableState<Float> {
    @AutoboxingStateValueProperty("floatValue")
    override var value: Float
        @Suppress("AutoBoxing") get() = floatValue
        set(value) { floatValue = value }

    override var floatValue: Float
}

/**
 * Permits property delegation of `var`s using `by` for [MutableFloatState].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableFloatState.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: Float
) {
    this.floatValue = value
}

internal expect fun createSnapshotMutableFloatState(
    value: Float
): MutableFloatState

/**
 * A single value holder whose reads and writes are observed by Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the wrapped value
 *
 * @see [mutableDoubleStateOf]
 */
internal open class SnapshotMutableFloatStateImpl(
    value: Float
) : StateObject, MutableFloatState, SnapshotMutableState<Float> {

    private var next = FloatStateStateRecord(value)

    override val firstStateRecord: StateRecord
        get() = next

    override var floatValue: Float
        get() = next.readable(this).value
        set(value) = next.withCurrent {
            if (it.value != value) {
                next.overwritable(this, it) { this.value = value }
            }
        }

    // Arbitrary policies are not allowed. The underlying `==` implementation
    // for primitive types corresponds to structural equality
    override val policy: SnapshotMutationPolicy<Float>
        get() = structuralEqualityPolicy()

    override fun component1(): Float = floatValue

    override fun component2(): (Float) -> Unit = { floatValue = it }

    override fun prependStateRecord(value: StateRecord) {
        next = value as FloatStateStateRecord
    }

    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord
    ): StateRecord? {
        val currentRecord = current as FloatStateStateRecord
        val appliedRecord = applied as FloatStateStateRecord
        return if (currentRecord.value == appliedRecord.value) {
            current
        } else {
            null
        }
    }

    override fun toString(): String = next.withCurrent {
        "MutableFloatState(value=${it.value})@${hashCode()}"
    }

    private class FloatStateStateRecord(
        var value: Float
    ) : StateRecord() {
        override fun assign(value: StateRecord) {
            this.value = (value as FloatStateStateRecord).value
        }

        override fun create(): StateRecord = FloatStateStateRecord(value)
    }
}
