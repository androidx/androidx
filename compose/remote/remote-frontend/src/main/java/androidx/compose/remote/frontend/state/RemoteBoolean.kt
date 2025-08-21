/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.frontend.state

import androidx.annotation.ColorInt
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.runtime.mutableStateOf

/**
 * A class representing a remote boolean value.
 *
 * This `RemoteBoolean` internally stores its state as a [RemoteInt], where typically `0` represents
 * `false` and `1` represents `true`. This allows boolean logic to be expressed and evaluated
 * efficiently on a remote rendering engine.
 *
 * @property v The internal [RemoteInt] that holds the boolean value.
 */
class RemoteBoolean internal constructor(internal val v: RemoteInt) : RemoteState<Boolean> {
    override val value: Boolean
        get() = v.value == 0

    /**
     * Whether or not this RemoteBoolean will always evaluate to the same [value]. This is currently
     * a conservative check that may report false negatives for some expressions that reference
     * other expressions since the tracking involved is expensive.
     */
    override val hasConstantValue: Boolean
        get() = v.hasConstantValue

    override fun writeToDocument(creationState: RemoteComposeCreationState) =
        v.writeToDocument(creationState)

    /**
     * Logical NOT operator for [RemoteBoolean].
     *
     * This creates a new [RemoteBoolean] whose value is the logical inverse of this boolean. It
     * achieves this by performing a bitwise XOR operation with `1` on the underlying [RemoteInt].
     *
     * @return A new [RemoteBoolean] representing the logical NOT of this boolean.
     */
    operator fun not(): RemoteBoolean = RemoteBoolean(v xor RemoteInt(1))

    /**
     * Constructor for creating a [RemoteBoolean] instance from a standard [Boolean].
     *
     * It converts the standard boolean value into a [RemoteInt]: `1` for `true` and `0` for `
     * false`.
     *
     * @param b The standard boolean value to convert.
     */
    constructor(
        b: Boolean
    ) : this(
        if (b) {
            RemoteInt(1)
        } else {
            RemoteInt(0)
        }
    )

    /**
     * Converts this [RemoteBoolean] to its underlying [RemoteInt] representation, which evaluates
     * to `1` for `true` and `0` for `false`.
     *
     * @return The [RemoteInt] that holds the boolean's value.
     */
    fun toRemoteInt() = v

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteString] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteString] to be selected if this boolean is `false`.
     * @return A new [RemoteString] representing the conditionally selected string.
     */
    fun select(ifTrue: RemoteString, ifFalse: RemoteString): RemoteString =
        MutableRemoteString(
            mutableStateOf(""),
            hasConstantValue && ifTrue.hasConstantValue && ifFalse.hasConstantValue,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    return creationState.document.textLookup(
                        creationState.document.addStringList(
                            ifFalse.getIdForCreationState(creationState),
                            ifTrue.getIdForCreationState(creationState),
                        ),
                        v.getIdForCreationState(creationState),
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) =
                    mergeSets(
                        ifTrue.computeRequiredCodePointSet(creationState),
                        ifFalse.computeRequiredCodePointSet(creationState),
                    )
            },
        )

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteFloat] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteFloat] to be selected if this boolean is `false`.
     * @return A new [RemoteFloat] representing the conditionally selected float value.
     */
    fun select(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat =
        RemoteFloatExpression(
            hasConstantValue && ifTrue.hasConstantValue && ifFalse.hasConstantValue,
            { creationState ->
                val b = v.arrayForCreationState(creationState)
                val t = ifTrue.arrayForCreationState(creationState)
                val f = ifFalse.arrayForCreationState(creationState)

                if (b.isLiteral() && t.isLiteral() && f.isLiteral()) {
                    // All inputs are constant so evaluate directly.
                    floatArrayOf(if (b[0] == 1L) t[0] else f[0])
                } else {
                    // One of the inputs wasn't constant so evaluate dynamically.
                    floatArrayOf(
                        *ifFalse.arrayProvider(creationState),
                        *ifTrue.arrayProvider(creationState),
                        v.getFloatIdForCreationState(creationState),
                        AnimatedFloatExpression.IFELSE,
                    )
                }
            },
        )

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteInt] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteInt] to be selected if this boolean is `false`.
     * @return A new [RemoteInt] representing the conditionally selected integer value.
     */
    fun select(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt =
        RemoteIntExpression(
            hasConstantValue && ifTrue.hasConstantValue && ifFalse.hasConstantValue,
            { creationState ->
                val b = v.arrayForCreationState(creationState)
                val t = ifTrue.arrayForCreationState(creationState)
                val f = ifFalse.arrayForCreationState(creationState)

                if (b.isLiteral() && t.isLiteral() && f.isLiteral()) {
                    // All inputs are constant so evaluate directly.
                    longArrayOf(if (b[0] == 1L) t[0] else f[0])
                } else {
                    // One of the inputs wasn't constant so evaluate dynamically.
                    combineToLongArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, v),
                        0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                    )
                }
            },
        )

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteBoolean] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteBoolean] to be selected if this boolean is `false`.
     * @return A new [RemoteBoolean] representing the conditionally selected integer value.
     */
    fun select(ifTrue: RemoteBoolean, ifFalse: RemoteBoolean): RemoteBoolean =
        RemoteBoolean(
            RemoteIntExpression(
                hasConstantValue && ifTrue.hasConstantValue && ifFalse.hasConstantValue,
                { creationState ->
                    val b = v.arrayForCreationState(creationState)
                    val t = ifTrue.v.arrayForCreationState(creationState)
                    val f = ifFalse.v.arrayForCreationState(creationState)

                    if (b.isLiteral() && t.isLiteral() && f.isLiteral()) {
                        // All inputs are constant so evaluate directly.
                        longArrayOf(if (b[0] == 1L) t[0] else f[0])
                    } else {
                        // One of the inputs wasn't constant so evaluate dynamically.
                        combineToLongArray(
                            creationState,
                            arrayOf(ifFalse.v, ifTrue.v, v),
                            0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                        )
                    }
                },
            )
        )

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The color to be selected if this boolean is `true` (as an [Int] representing an
     *   ARGB color).
     * @param ifFalse The color to be selected if this boolean is `false` (as an [Int] representing
     *   an ARGB color).
     * @return A new [RemoteColor] representing the conditionally selected color.
     */
    fun select(@ColorInt ifTrue: Int, @ColorInt ifFalse: Int): RemoteColor =
        tween(ifFalse, ifTrue, v.toRemoteFloat())

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteColor] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteColor] to be selected if this boolean is `false`.
     * @return A new [RemoteColor] representing the conditionally selected color.
     */
    fun select(ifTrue: RemoteColor, ifFalse: RemoteColor): RemoteColor =
        tween(ifFalse, ifTrue, v.toRemoteFloat())

    /**
     * Equality operator for [RemoteBoolean]s.
     *
     * Returns a new [RemoteBoolean] that evaluates to `true` if this boolean's underlying
     * [RemoteInt] is equal to another [RemoteBoolean]'s underlying [RemoteInt].
     *
     * @param b The other [RemoteBoolean] to compare with.
     * @return A new [RemoteBoolean] representing the result of the equality comparison.
     */
    infix fun eq(b: RemoteBoolean) = v eq b.v

    /**
     * Inequality operator for [RemoteBoolean]s.
     *
     * Returns a new [RemoteBoolean] that evaluates to `true` if this boolean's underlying
     * [RemoteInt] is *not* equal to another [RemoteBoolean]'s underlying [RemoteInt].
     *
     * @param b The other [RemoteBoolean] to compare with.
     * @return A new [RemoteBoolean] representing the result of the inequality comparison.
     */
    infix fun ne(b: RemoteBoolean) = v ne b.v

    /**
     * Logical OR operator for [RemoteBoolean]s.
     *
     * Performs a bitwise OR operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * @param b The other [RemoteBoolean] to perform the OR operation with.
     * @return A new [RemoteBoolean] representing the result of the logical OR.
     */
    infix fun or(b: RemoteBoolean) = RemoteBoolean(v or b.v)

    /**
     * Logical AND operator for [RemoteBoolean]s.
     *
     * Performs a bitwise AND operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * @param b The other [RemoteBoolean] to perform the AND operation with.
     * @return A new [RemoteBoolean] representing the result of the logical AND.
     */
    infix fun and(b: RemoteBoolean) = RemoteBoolean(v and b.v)

    /**
     * Logical XOR operator for [RemoteBoolean]s.
     *
     * Performs a bitwise XOR operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * @param b The other [RemoteBoolean] to perform the XOR operation with.
     * @return A new [RemoteBoolean] representing the result of the logical XOR.
     */
    infix fun xor(b: RemoteBoolean) = RemoteBoolean(v xor b.v)
}
