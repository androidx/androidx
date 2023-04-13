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

package androidx.compose.runtime

/**
 * Converts a `State<Int>` (as in, a [State] of boxed `Int`s) into a primitive-backed [IntState].
 * The state will be automatically unboxed to the required primitive type. The returned state is
 * read-only. The returned state will mirror the values of the base state and apply updates in the
 * same way as the receiver defines.
 *
 * On the JVM, this conversion does not avoid the autoboxing that [IntState] attempts to escape, but
 * instead is intended to allow interoperability between components that use either representation
 * of a state of type `Int`.
 */
@Stable
fun State<Int>.asIntState(): IntState =
    if (this is IntState) this else UnboxedIntState(this)

internal class UnboxedIntState(
    private val baseState: State<Int>
) : IntState {
    override val intValue: Int
        get() = baseState.value

    override val value: Int
        get() = baseState.value

    override fun toString(): String = "UnboxedIntState(baseState=$baseState)@${hashCode()}"
}

/**
 * Converts a `State<Long>` (as in, a [State] of boxed `Long`s) into a primitive-backed [LongState].
 * The state will be automatically unboxed to the required primitive type. The returned state is
 * read-only. The returned state will mirror the values of the base state and apply updates in the
 * same way as the receiver defines.
 *
 * On the JVM, this conversion does not avoid the autoboxing that [LongState] attempts to escape,
 * but instead is intended to allow interoperability between components that use either
 * representation of a state of type `Long`.
 */
@Stable
fun State<Long>.asLongState(): LongState =
    if (this is LongState) this else UnboxedLongState(this)

internal class UnboxedLongState(
    private val baseState: State<Long>
) : LongState {
    override val longValue: Long
        get() = baseState.value

    override val value: Long
        get() = baseState.value

    override fun toString(): String = "UnboxedLongState(baseState=$baseState)@${hashCode()}"
}

/**
 * Converts a `State<Float>` (as in, a [State] of boxed `Float`s) into a primitive-backed [Float].
 * The state will be automatically unboxed to the required primitive type. The returned state is
 * read-only. The returned state will mirror the values of the base state and apply updates in the
 * same way as the receiver defines.
 *
 * On the JVM, this conversion does not avoid the autoboxing that [Float] attempts to escape,
 * but instead is intended to allow interoperability between components that use either
 * representation of a state of type `Float`.
 */
@Stable
fun State<Float>.asFloatState(): FloatState =
    if (this is FloatState) this else UnboxedFloatState(this)

internal class UnboxedFloatState(
    private val baseState: State<Float>
) : FloatState {
    override val floatValue: Float
        get() = baseState.value

    override val value: Float
        get() = baseState.value

    override fun toString(): String = "UnboxedFloatState(baseState=$baseState)@${hashCode()}"
}

/**
 * Converts a `State<Double>` (as in, a [State] of boxed `Double`s) into a primitive-backed
 * [Double]. The state will be automatically unboxed to the required primitive type. The returned
 * state is read-only. The returned state will mirror the values of the base state and apply updates
 * in the same way as the receiver defines.
 *
 * On the JVM, this conversion does not avoid the autoboxing that [Double] attempts to escape,
 * but instead is intended to allow interoperability between components that use either
 * representation of a state of type `Double`.
 */
@Stable
fun State<Double>.asDoubleState(): DoubleState =
    if (this is DoubleState) this else UnboxedDoubleState(this)

internal class UnboxedDoubleState(
    private val baseState: State<Double>
) : DoubleState {
    override val doubleValue: Double
        get() = baseState.value

    override val value: Double
        get() = baseState.value

    override fun toString(): String = "UnboxedDoubleState(baseState=$baseState)@${hashCode()}"
}