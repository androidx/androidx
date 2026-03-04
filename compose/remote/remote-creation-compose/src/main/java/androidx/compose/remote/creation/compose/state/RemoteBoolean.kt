/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteBoolean.OperationKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A class representing a remote boolean value.
 *
 * `RemoteBoolean` internally stores its state as a [RemoteInt], typically using `1` for `true` and
 * `0` for `false`. This allows boolean logic to be evaluated efficiently on the remote rendering
 * engine.
 */
public open class RemoteBoolean internal constructor(internal val intValue: RemoteInt) :
    BaseRemoteState<Boolean>() {
    internal override val cacheKey: RemoteStateCacheKey
        get() = intValue.cacheKey

    internal enum class OperationKey {
        SelectString,
        SelectFloat,
        SelectInt,
        SelectBoolean,
    }

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Boolean?
        get() = intValue.constantValueOrNull?.let { it != 0 }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

    /**
     * Logical NOT operator for [RemoteBoolean].
     *
     * This creates a new [RemoteBoolean] whose value is the logical inverse of this boolean. It
     * achieves this by performing a bitwise XOR operation with `1` on the underlying [RemoteInt].
     *
     * @return A new [RemoteBoolean] representing the logical NOT of this boolean.
     */
    public operator fun not(): RemoteBoolean = RemoteBoolean(intValue xor RemoteInt(1))

    /**
     * Constructor for creating a [RemoteBoolean] instance from a standard [Boolean].
     *
     * It converts the standard boolean value into a [RemoteInt]: `1` for `true` and `0` for `
     * false`.
     *
     * @param value The standard boolean value to convert.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        value: Boolean
    ) : this(
        if (value) {
            RemoteInt(1)
        } else {
            RemoteInt(0)
        }
    )

    /**
     * Converts this [RemoteBoolean] to its underlying [RemoteInt] representation, which evaluates
     * to `1` for `true` and `0` for `false`.
     *
     * @return The [RemoteInt] that holds the boolean\'s value.
     */
    public fun toRemoteInt(): RemoteInt = intValue

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteString] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteString] to be selected if this boolean is `false`.
     * @return A new [RemoteString] representing the conditionally selected string.
     */
    public fun select(ifTrue: RemoteString, ifFalse: RemoteString): RemoteString {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(OperationKey.SelectString, this, ifTrue, ifFalse),
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    return creationState.document.textLookup(
                        creationState.document.addStringList(
                            ifFalse.getIdForCreationState(creationState),
                            ifTrue.getIdForCreationState(creationState),
                        ),
                        intValue.getIdForCreationState(creationState),
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
    }

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteFloat] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteFloat] to be selected if this boolean is `false`.
     * @return A new [RemoteFloat] representing the conditionally selected float value.
     */
    public fun select(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(OperationKey.SelectFloat, this, ifTrue, ifFalse),
            arrayProvider = { creationState ->
                combineToFloatArray(
                    creationState,
                    arrayOf(ifFalse, ifTrue, intValue.toRemoteFloat()),
                    AnimatedFloatExpression.IFELSE,
                )
            },
        )
    }

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteInt] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteInt] to be selected if this boolean is `false`.
     * @return A new [RemoteInt] representing the conditionally selected integer value.
     */
    public fun select(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }
        return RemoteIntExpression(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(OperationKey.SelectInt, this, ifTrue, ifFalse),
            arrayProvider = { creationState ->
                combineToLongArray(
                    creationState,
                    arrayOf(ifFalse, ifTrue, intValue),
                    0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                )
            },
        )
    }

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteBoolean] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteBoolean] to be selected if this boolean is `false`.
     * @return A new [RemoteBoolean] representing the conditionally selected integer value.
     */
    public fun select(ifTrue: RemoteBoolean, ifFalse: RemoteBoolean): RemoteBoolean {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        return RemoteBoolean(
            RemoteIntExpression(
                constantValueOrNull = null,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        OperationKey.SelectBoolean,
                        this,
                        ifTrue,
                        ifFalse,
                    ),
                arrayProvider = { creationState ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifFalse.intValue, ifTrue.intValue, intValue),
                        0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                    )
                },
            )
        )
    }

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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun select(@ColorInt ifTrue: Int, @ColorInt ifFalse: Int): RemoteColor {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                RemoteColor(ifTrue)
            } else {
                RemoteColor(ifFalse)
            }
        }

        return tween(ifFalse, ifTrue, intValue.toRemoteFloat())
    }

    /**
     * If this RemoteBoolean evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteColor] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteColor] to be selected if this boolean is `false`.
     * @return A new [RemoteColor] representing the conditionally selected color.
     */
    public fun select(ifTrue: RemoteColor, ifFalse: RemoteColor): RemoteColor {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        return tween(ifFalse, ifTrue, intValue.toRemoteFloat())
    }

    /**
     * Equality operator for [RemoteBoolean]s.
     *
     * Returns a new [RemoteBoolean] that evaluates to `true` if this boolean\'s underlying
     * [RemoteInt] is equal to another [RemoteBoolean]\'s underlying [RemoteInt].
     *
     * @param b The other [RemoteBoolean] to compare with.
     * @return A new [RemoteBoolean] representing the result of the equality comparison.
     */
    public infix fun eq(b: RemoteBoolean): RemoteBoolean = intValue eq b.intValue

    /**
     * Inequality operator for [RemoteBoolean]s.
     *
     * Returns a new [RemoteBoolean] that evaluates to `true` if this boolean\'s underlying
     * [RemoteInt] is *not* equal to another [RemoteBoolean]\'s underlying [RemoteInt].
     *
     * @param b The other [RemoteBoolean] to compare with.
     * @return A new [RemoteBoolean] representing the result of the inequality comparison.
     */
    public infix fun ne(b: RemoteBoolean): RemoteBoolean = intValue ne b.intValue

    /**
     * Logical OR operator for [RemoteBoolean]s.
     *
     * Performs a bitwise OR operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * @param b The other [RemoteBoolean] to perform the OR operation with.
     * @return A new [RemoteBoolean] representing the result of the logical OR.
     */
    public infix fun or(b: RemoteBoolean): RemoteBoolean = RemoteBoolean(intValue or b.intValue)

    /**
     * Logical AND operator for [RemoteBoolean]s.
     *
     * Performs a bitwise AND operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * @param b The other [RemoteBoolean] to perform the AND operation with.
     * @return A new [RemoteBoolean] representing the result of the logical AND.
     */
    public infix fun and(b: RemoteBoolean): RemoteBoolean = RemoteBoolean(intValue and b.intValue)

    /**
     * Logical XOR operator for [RemoteBoolean]s.
     *
     * Performs a bitwise XOR operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * @param b The other [RemoteBoolean] to perform the XOR operation with.
     * @return A new [RemoteBoolean] representing the result of the logical XOR.
     */
    public infix fun xor(b: RemoteBoolean): RemoteBoolean = RemoteBoolean(intValue xor b.intValue)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteBoolean] from a literal constant.
         *
         * @param value The constant [Boolean] value.
         * @return A [RemoteBoolean] representing the constant boolean.
         */
        public operator fun invoke(value: Boolean): RemoteBoolean = RemoteBoolean(value = value)

        /**
         * Creates a [RemoteBoolean] referencing a remote ID.
         *
         * @param id The remote ID (stored as a [RemoteInt]).
         * @return A [RemoteBoolean] referencing the ID.
         */
        internal fun createForId(id: Int): RemoteBoolean =
            RemoteBoolean(RemoteInt.createForId(0x100000000L + id))

        /**
         * Creates a named [RemoteBoolean] with an initial value. Named remote booleans can be set
         * via AndroidRemoteContext.setNamedBoolean.
         *
         * @param name The unique name for this remote boolean.
         * @param domain The domain of the named boolean (defaults to [RemoteState.Domain.User]).
         * @param defaultValue The initial [Boolean] value for the named remote boolean.
         * @return A [RemoteBoolean] representing the named boolean.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun createNamedRemoteBoolean(
            name: String,
            defaultValue: Boolean,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteBoolean {
            return RemoteBoolean(
                RemoteInt.createNamedRemoteInt(
                    name = name,
                    defaultValue = if (defaultValue) 1 else 0,
                    domain = domain,
                )
            )
        }
    }
}

/** A mutable implementation of [RemoteBoolean]. */
public class MutableRemoteBoolean internal constructor(remoteInt: MutableRemoteInt) :
    RemoteBoolean(remoteInt), MutableRemoteState<Boolean> {

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Boolean?
        get() =
            when (intValue.constantValueOrNull) {
                0 -> false
                is Int -> true
                null -> null
            }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

    public companion object {
        /**
         * Creates a [MutableRemoteBoolean] for a given ID.
         *
         * @param id The ID for this mutable boolean.
         * @return A [MutableRemoteBoolean] instance.
         */
        internal fun createMutableForId(id: Long): MutableRemoteBoolean =
            MutableRemoteBoolean(MutableRemoteInt.createMutableForId(id))

        /**
         * Creates a [MutableRemoteBoolean] with an initial value.
         *
         * @param initialValue The initial value for this mutable boolean.
         * @return A [MutableRemoteBoolean] instance.
         */
        public fun createMutable(initialValue: Boolean): MutableRemoteBoolean =
            MutableRemoteBoolean(MutableRemoteInt.createMutable(if (initialValue) 1 else 0))
    }
}

/** Extension property to convert a [Boolean] to a [RemoteBoolean]. */
public val Boolean.rb: RemoteBoolean
    get() {
        return RemoteBoolean(this)
    }

/**
 * Factory composable for mutable remote boolean state.
 *
 * @param initialValue The initial [Boolean] value.
 * @return A [MutableRemoteBoolean] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
public fun rememberMutableRemoteBoolean(initialValue: Boolean): MutableRemoteBoolean {
    return remember {
        MutableRemoteBoolean(MutableRemoteInt.createMutable(if (initialValue) 1 else 0))
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("Use rememberMutableRemoteBoolean(value())")
@Composable
@RemoteComposable
public fun rememberRemoteBooleanValue(value: () -> Boolean): RemoteBoolean =
    rememberMutableRemoteBoolean(value())

/**
 * Remembers a named remote boolean expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param initialValue The initial [Boolean] value.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteBoolean] instance representing the named expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteBoolean(
    name: String,
    initialValue: Boolean,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteBoolean {
    return rememberNamedState(name, domain) {
        RemoteBoolean(RemoteInt.createNamedRemoteInt(name, if (initialValue) 1 else 0, domain))
    }
}
