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
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.document.RemoteIntId
import androidx.compose.remote.document.addIdList
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import kotlin.reflect.KClass

/**
 * Read-only supertype for remote boolean values.
 *
 * [RemoteBoolean] internally stores its state as a [RemoteInt], typically using `1` for `true` and
 * `0` for `false`. For writable boolean variables, see [MutableRemoteBoolean].
 *
 * Operations on [RemoteBoolean] are evaluated dynamically on the remote player at runtime unless
 * all inputs are known constants, in which case constant folding evaluates the result immediately
 * at creation time (see [RemoteValue]).
 *
 * Comparing [RemoteBoolean] values with `==` checks structural equality (matching constant values
 * or expression DAGs and mutability), whereas [MutableRemoteBoolean] instances compare by their
 * underlying mutable variable; see [RemoteValue]. For runtime equality on the remote player, use
 * [isEqualTo].
 */
@Stable
public sealed interface RemoteBoolean : RemoteValue<Boolean> {
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Boolean?

    /**
     * Logical NOT operator for [RemoteBoolean].
     *
     * This creates a new [RemoteBoolean] whose value is the logical inverse of this boolean. It
     * achieves this by performing a bitwise XOR operation with `1` on the underlying [RemoteInt].
     *
     * @return A new [RemoteBoolean] representing the logical NOT of this boolean.
     */
    public operator fun not(): RemoteBoolean

    /**
     * Converts this [RemoteBoolean] to its underlying [RemoteInt] representation, which evaluates
     * to `1` for `true` and `0` for `false`.
     *
     * @return The [RemoteInt] that holds the boolean\'s value.
     */
    public fun toRemoteInt(): RemoteInt

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteString] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteString] to be selected if this boolean is `false`.
     * @return A new [RemoteString] representing the conditionally selected string.
     */
    public fun select(ifTrue: RemoteString, ifFalse: RemoteString): RemoteString

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteFloat] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteFloat] to be selected if this boolean is `false`.
     * @return A new [RemoteFloat] representing the conditionally selected float value.
     */
    public fun select(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteInt] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteInt] to be selected if this boolean is `false`.
     * @return A new [RemoteInt] representing the conditionally selected integer value.
     */
    public fun select(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteBoolean] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteBoolean] to be selected if this boolean is `false`.
     * @return A new [RemoteBoolean] representing the conditionally selected boolean value.
     */
    public fun select(ifTrue: RemoteBoolean, ifFalse: RemoteBoolean): RemoteBoolean

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The color to be selected if this boolean is `true` (as an [Int] representing an
     *   ARGB color).
     * @param ifFalse The color to be selected if this boolean is `false` (as an [Int] representing
     *   an ARGB color).
     * @return A new [RemoteColor] representing the conditionally selected color.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun select(@ColorInt ifTrue: Int, @ColorInt ifFalse: Int): RemoteColor

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteColor] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteColor] to be selected if this boolean is `false`.
     * @return A new [RemoteColor] representing the conditionally selected color.
     */
    public fun select(ifTrue: RemoteColor, ifFalse: RemoteColor): RemoteColor

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The [RemoteDp] to be selected if this boolean is `true`.
     * @param ifFalse The [RemoteDp] to be selected if this boolean is `false`.
     * @return A new [RemoteDp] representing the conditionally selected Dp value.
     */
    public fun select(ifTrue: RemoteDp, ifFalse: RemoteDp): RemoteDp

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteBoolean] is equal to the evaluated value of [other].
     *
     * @param other The other [RemoteBoolean] to compare with.
     * @return A new [RemoteBoolean] representing `this == other`.
     */
    public fun isEqualTo(other: RemoteBoolean): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this
     * [RemoteBoolean] is not equal to the evaluated value of [other].
     *
     * @param other The other [RemoteBoolean] to compare with.
     * @return A new [RemoteBoolean] representing `this != other`.
     */
    public fun isNotEqualTo(other: RemoteBoolean): RemoteBoolean

    /**
     * Logical OR operator for [RemoteBoolean]s.
     *
     * Performs a bitwise OR operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * This is designed to align with the standard Kotlin [Boolean.or] infix function.
     *
     * @param other The other [RemoteBoolean] to perform the OR operation with.
     * @return A new [RemoteBoolean] representing the result of the logical OR.
     */
    public infix fun or(other: RemoteBoolean): RemoteBoolean

    /**
     * Logical AND operator for [RemoteBoolean]s.
     *
     * Performs a bitwise AND operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * This is designed to align with the standard Kotlin [Boolean.and] infix function.
     *
     * @param other The other [RemoteBoolean] to perform the AND operation with.
     * @return A new [RemoteBoolean] representing the result of the logical AND.
     */
    public infix fun and(other: RemoteBoolean): RemoteBoolean

    /**
     * Logical XOR operator for [RemoteBoolean]s.
     *
     * Performs a bitwise XOR operation on the underlying [RemoteInt] values of this boolean and the
     * other [RemoteBoolean]. The result is a new [RemoteBoolean].
     *
     * This is designed to align with the standard Kotlin [Boolean.xor] infix function.
     *
     * @param other The other [RemoteBoolean] to perform the XOR operation with.
     * @return A new [RemoteBoolean] representing the result of the logical XOR.
     */
    public infix fun xor(other: RemoteBoolean): RemoteBoolean

    public companion object {
        /**
         * Creates a [RemoteBoolean] referencing a remote ID.
         *
         * @param id The remote ID (stored as a [RemoteInt]).
         * @return A [RemoteBoolean] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Int): RemoteBoolean =
            RemoteBoolean(RemoteInt.createForId(0x100000000L + id))

        /**
         * Creates a named [RemoteBoolean] with an initial value. Named remote booleans can be set
         * via AndroidRemoteContext.setNamedBoolean.
         *
         * @param name The unique name for this remote boolean.
         * @param domain The domain of the named boolean (defaults to [RemoteValue.Domain.User]).
         * @param defaultValue The initial [Boolean] value for the named remote boolean.
         * @return A [RemoteBoolean] representing the named boolean.
         */
        @JvmStatic
        public fun createNamedRemoteBoolean(
            name: String,
            defaultValue: Boolean,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
        ): RemoteBoolean {
            return RemoteBoolean(
                createNamedRemoteInt(
                    name = name,
                    defaultValue = if (defaultValue) 1 else 0,
                    domain = domain,
                )
            )
        }
    }
}

internal val RemoteBoolean.floatComparison: SelectFloatCondition.FloatComparison?
    get() = (this as? RemoteBooleanImpl)?.floatComparison

internal val RemoteBoolean.intComparison: SelectIntCondition.IntComparison?
    get() = (this as? RemoteBooleanImpl)?.intComparison

internal fun RemoteBoolean(
    intValue: RemoteInt,
    floatComparison: SelectFloatCondition.FloatComparison? = null,
    intComparison: SelectIntCondition.IntComparison? = null,
): RemoteBoolean = RemoteBooleanImpl(intValue, floatComparison, intComparison)

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteBoolean.intValue: RemoteInt
    get() = (this as BaseRemoteBoolean).intValue

private abstract class BaseRemoteBoolean :
    BaseRemoteValue<Boolean>(RemoteStateInstanceKey()), RemoteBoolean {
    override val equalityType: KClass<*>
        get() = RemoteBoolean::class

    abstract val intValue: RemoteInt

    override val cacheKey: RemoteStateCacheKey
        get() = intValue.cacheKey

    override val asEncoded: RemoteInt
        get() = intValue

    override val constantValueOrNull: Boolean?
        get() = intValue.constantValueOrNull?.let { it != 0 }

    override fun not(): RemoteBoolean = RemoteBoolean(intValue xor RemoteInt(1))

    override fun toRemoteInt(): RemoteInt = intValue

    override fun select(ifTrue: RemoteString, ifFalse: RemoteString): RemoteString {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        return RemoteString(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(
                    BooleanOperationKey.SelectString,
                    this,
                    ifTrue,
                    ifFalse,
                ),
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    val listId =
                        creationState.document.addIdList(
                            creationState.getId(ifFalse),
                            creationState.getId(ifTrue),
                        )
                    return creationState.document
                        .textLookup(
                            listId,
                            RemoteIntId(creationState.getId(intValue)),
                        )
                        .id
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? {
                    if (hasConstantValue) {
                        val selected = if (constantValue) ifTrue else ifFalse
                        return selected.computeRequiredCodePointSet(creationState)
                    }
                    return mergeSets(
                        ifTrue.computeRequiredCodePointSet(creationState),
                        ifFalse.computeRequiredCodePointSet(creationState),
                    )
                }
            },
        )
    }

    override fun select(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        val condition = floatComparison ?: SelectFloatCondition.BooleanCondition(this)
        return condition.createSelect(ifTrue, ifFalse)
    }

    override fun select(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        val condition = intComparison ?: SelectIntCondition.BooleanCondition(this)
        return condition.createSelect(ifTrue, ifFalse)
    }

    override fun select(ifTrue: RemoteBoolean, ifFalse: RemoteBoolean): RemoteBoolean {
        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        // Peephole: both branches are constants
        val ifTrueConst = ifTrue.constantValueOrNull
        val ifFalseConst = ifFalse.constantValueOrNull
        if (ifTrueConst != null && ifFalseConst != null) {
            if (ifTrueConst == ifFalseConst) {
                return ifTrue
            }
            // flag ? true : false  =>  flag
            // flag ? false : true  =>  !flag
            return if (ifTrueConst) this else !this
        }

        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        // Peephole: flag ? flag : ifFalse
        if (cacheKey == ifTrue.cacheKey) {
            // flag ? flag : false  =>  flag
            if (ifFalseConst == false) return this
            // flag ? flag : true   =>  true
            if (ifFalseConst == true) return ifFalse
        }

        // Peephole: flag ? ifTrue : flag
        if (cacheKey == ifFalse.cacheKey) {
            // flag ? true : flag   =>  flag
            if (ifTrueConst == true) return this
            // flag ? false : flag  =>  false
            if (ifTrueConst == false) return ifTrue
        }

        return RemoteBoolean(
            RemoteInt(
                constantValueOrNull = null,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        BooleanOperationKey.SelectBoolean,
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

    override fun select(ifTrue: Int, ifFalse: Int): RemoteColor {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                RemoteColor(ifTrue)
            } else {
                RemoteColor(ifFalse)
            }
        }

        if (ifTrue == ifFalse) {
            return RemoteColor(ifTrue)
        }

        return lerp(ifFalse, ifTrue, intValue.toRemoteFloat())
    }

    override fun select(ifTrue: RemoteColor, ifFalse: RemoteColor): RemoteColor {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        return lerp(ifFalse, ifTrue, intValue.toRemoteFloat())
    }

    override fun select(ifTrue: RemoteDp, ifFalse: RemoteDp): RemoteDp {
        return select(ifTrue.value, ifFalse.value).asRemoteDp()
    }

    override fun isEqualTo(other: RemoteBoolean): RemoteBoolean = intValue.isEqualTo(other.intValue)

    override fun isNotEqualTo(other: RemoteBoolean): RemoteBoolean =
        intValue.isNotEqualTo(other.intValue)

    override fun or(other: RemoteBoolean): RemoteBoolean = RemoteBoolean(intValue or other.intValue)

    override fun and(other: RemoteBoolean): RemoteBoolean =
        RemoteBoolean(intValue and other.intValue)

    override fun xor(other: RemoteBoolean): RemoteBoolean =
        RemoteBoolean(intValue xor other.intValue)

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

    override fun toString(): String {
        val str = cacheKey.toDebugString()
        return when (str) {
            "1" -> "true"
            "0" -> "false"
            else -> str
        }
    }
}

private class RemoteBooleanImpl(
    override val intValue: RemoteInt,
    val floatComparison: SelectFloatCondition.FloatComparison? = null,
    val intComparison: SelectIntCondition.IntComparison? = null,
) : BaseRemoteBoolean()

internal enum class BooleanOperationKey : RemoteOperation {
    SelectString,
    SelectFloat,
    SelectInt,
    SelectBoolean;

    override val precedence: Int
        get() = 0

    override fun toDebugString(args: List<RemoteStateCacheKey>): String {
        val condStr =
            when (val cond = args[0].toOperandString(1)) {
                "1" -> "true"
                "0" -> "false"
                else -> cond
            }
        return "$condStr ? ${args[1].toOperandString(0)} : ${args[2].toOperandString(0)}"
    }

    override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> {
        val cond = asRemoteBoolean(args[0])
        return when (this) {
            SelectString -> cond.select(args[1] as RemoteString, args[2] as RemoteString)
            SelectFloat -> cond.select(args[1] as RemoteFloat, args[2] as RemoteFloat)
            SelectInt -> cond.select(args[1] as RemoteInt, args[2] as RemoteInt)
            SelectBoolean ->
                cond.select(asRemoteBoolean(args[1]), asRemoteBoolean(args[2])).intValue
        }
    }

    private fun asRemoteBoolean(value: RemoteValue<*>): RemoteBoolean =
        value as? RemoteBoolean ?: RemoteBoolean(value as RemoteInt)

    override fun isResultWholeNumber(args: List<RemoteStateCacheKey>): Boolean =
        when (this) {
            SelectFloat ->
                args.size >= 3 && args[1].isResultWholeNumber && args[2].isResultWholeNumber
            SelectInt,
            SelectBoolean -> true
            SelectString -> false
        }
}

/**
 * A mutable implementation of [RemoteBoolean].
 *
 * Two [MutableRemoteBoolean] instances are `==` only if they reference the same underlying mutable
 * variable; separately created instances (e.g., via `MutableRemoteBoolean(initialValue)`) represent
 * distinct mutable variables and are never `==`.
 */
@Stable
public sealed interface MutableRemoteBoolean : RemoteBoolean, MutableRemoteValue<Boolean> {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [MutableRemoteBoolean] for a given ID.
         *
         * @param id The ID for this mutable boolean.
         * @return A [MutableRemoteBoolean] instance.
         */
        internal fun createMutableForId(id: Long): MutableRemoteBoolean =
            MutableRemoteBooleanImpl(MutableRemoteInt.createMutableForId(id))
    }
}

private class MutableRemoteBooleanImpl(override val intValue: MutableRemoteInt) :
    BaseRemoteBoolean(), MutableRemoteBoolean {
    override val asEncodedMutable: MutableRemoteInt
        get() = intValue
}

/**
 * Creates a [RemoteBoolean] from a literal constant [Boolean] [value].
 *
 * It converts the boolean value into a [RemoteInt]: `1` for `true` and `0` for `false`.
 *
 * @param value The constant [Boolean] value.
 * @return A [RemoteBoolean] representing the constant boolean.
 */
public fun RemoteBoolean(value: Boolean): RemoteBoolean =
    RemoteBoolean(
        if (value) {
            RemoteInt(1)
        } else {
            RemoteInt(0)
        }
    )

/**
 * Creates a [MutableRemoteBoolean] with an initial [Boolean] [initialValue].
 *
 * @param initialValue The initial value for this mutable boolean.
 * @return A [MutableRemoteBoolean] initialized with [initialValue].
 */
@RememberInComposition
public fun MutableRemoteBoolean(initialValue: Boolean): MutableRemoteBoolean =
    MutableRemoteBooleanImpl(MutableRemoteInt(if (initialValue) 1 else 0))

/** Extension property to convert a [Boolean] to a [RemoteBoolean]. */
public val Boolean.rb: RemoteBoolean
    get() {
        return RemoteBoolean(this)
    }
