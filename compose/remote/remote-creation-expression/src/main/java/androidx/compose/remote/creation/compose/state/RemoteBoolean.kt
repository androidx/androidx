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
import androidx.compose.remote.document.NanEncoding
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
            RemoteBoolean(
                intValue = RemoteInt.createForId(NanEncoding.encodeLongId(id)),
                cacheKey = RemoteStateIdKey(id),
            )

        /**
         * Wraps [intValue] (`0` = `false`, non-zero = `true`) as a [RemoteBoolean], folding
         * immediately when [intValue] has a constant value.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromRemoteInt(intValue: RemoteInt): RemoteBoolean =
            intValue.constantValueOrNull?.let { RemoteBoolean(it != 0) } ?: RemoteBoolean(intValue)

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
                intValue =
                    createNamedRemoteInt(
                        name = name,
                        defaultValue = if (defaultValue) 1 else 0,
                        domain = domain,
                    ),
                cacheKey = RemoteNamedCacheKey(domain, name),
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
    cacheKey: RemoteStateCacheKey =
        intValue.constantValueOrNull?.let { RemoteConstantCacheKey(it != 0) }
            ?: RemoteOperationCacheKey.create(BooleanOperationKey.RemoteBoolean, intValue),
    floatComparison: SelectFloatCondition.FloatComparison? = null,
    intComparison: SelectIntCondition.IntComparison? = null,
): RemoteBoolean = RemoteBooleanImpl(intValue, cacheKey, floatComparison, intComparison)

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteBoolean.intValue: RemoteInt
    get() = (this as BaseRemoteBoolean).intValue

private abstract class BaseRemoteBoolean(cacheKey: RemoteStateCacheKey) :
    BaseRemoteValue<Boolean>(cacheKey), RemoteBoolean {
    override val equalityType: KClass<*>
        get() = RemoteBoolean::class

    override val valueKind: RemoteValueKind
        get() = RemoteValueKind.BOOLEAN

    override fun createForIdOrNull(id: Int): RemoteValue<*> = RemoteBoolean.createForId(id)

    override fun adaptToType(candidate: RemoteValue<*>): RemoteValue<*> =
        when (candidate) {
            is RemoteBoolean -> candidate
            is RemoteInt -> RemoteBoolean.fromRemoteInt(candidate)
            is RemoteFloat ->
                candidate.constantValueOrNull?.let { RemoteBoolean(it.toInt() != 0) }
                    ?: RemoteBoolean.fromRemoteInt(candidate.toRemoteInt())
            else -> this
        }

    abstract val intValue: RemoteInt

    override val asEncoded: RemoteInt
        get() = intValue

    override val constantValueOrNull: Boolean?
        get() = intValue.constantValueOrNull?.let { it != 0 }

    override fun not(): RemoteBoolean {
        constantValueOrNull?.let {
            return RemoteBoolean(!it)
        }
        unwrapBooleanNegation(this)?.let {
            return it
        }
        return RemoteBoolean(
            intValue = intValue xor RemoteInt(1),
            cacheKey = RemoteOperationCacheKey.create(BooleanOperationKey.Not, this),
        )
    }

    override fun toRemoteInt(): RemoteInt {
        constantValueOrNull?.let {
            return RemoteInt(if (it) 1 else 0)
        }
        return RemoteInt(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(BooleanOperationKey.ToRemoteInt, this),
        ) { creationState ->
            intValue.arrayForCreationState(creationState)
        }
    }

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

        val boolKey =
            RemoteOperationCacheKey.create(
                BooleanOperationKey.SelectBoolean,
                this,
                ifTrue,
                ifFalse,
            )
        return RemoteBoolean(
            intValue =
                RemoteInt(
                    constantValueOrNull = null,
                    cacheKey =
                        RemoteOperationCacheKey(BooleanOperationKey.ToRemoteInt, listOf(boolKey)),
                    arrayProvider = { creationState ->
                        combineToLongArray(
                            creationState,
                            arrayOf(ifFalse.intValue, ifTrue.intValue, intValue),
                            0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                        )
                    },
                ),
            cacheKey = boolKey,
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

        return lerp(ifFalse, ifTrue, toRemoteInt().toRemoteFloat())
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

        return lerp(ifFalse, ifTrue, toRemoteInt().toRemoteFloat())
    }

    override fun select(ifTrue: RemoteDp, ifFalse: RemoteDp): RemoteDp {
        return select(ifTrue.value, ifFalse.value).asRemoteDp()
    }

    override fun isEqualTo(other: RemoteBoolean): RemoteBoolean {
        val aConst = constantValueOrNull
        val bConst = other.constantValueOrNull
        if (aConst != null && bConst != null) {
            return RemoteBoolean(aConst == bConst)
        }
        if (cacheKey.isDeterministic()) {
            if (cacheKey == other.cacheKey) return RemoteBoolean(true)
            if (isBooleanComplement(this, other)) return RemoteBoolean(false)
        }
        return RemoteBoolean(
            intValue = intValue.isEqualTo(other.intValue).intValue,
            cacheKey = RemoteOperationCacheKey.create(BooleanOperationKey.Equal, this, other),
        )
    }

    override fun isNotEqualTo(other: RemoteBoolean): RemoteBoolean {
        val aConst = constantValueOrNull
        val bConst = other.constantValueOrNull
        if (aConst != null && bConst != null) {
            return RemoteBoolean(aConst != bConst)
        }
        if (cacheKey.isDeterministic()) {
            if (cacheKey == other.cacheKey) return RemoteBoolean(false)
            if (isBooleanComplement(this, other)) return RemoteBoolean(true)
        }
        return RemoteBoolean(
            intValue = intValue.isNotEqualTo(other.intValue).intValue,
            cacheKey = RemoteOperationCacheKey.create(BooleanOperationKey.NotEqual, this, other),
        )
    }

    override fun or(other: RemoteBoolean): RemoteBoolean {
        val aConst = constantValueOrNull
        val bConst = other.constantValueOrNull
        if (aConst != null && bConst != null) {
            return RemoteBoolean(aConst || bConst)
        }
        if (cacheKey.isDeterministic()) {
            if (cacheKey == other.cacheKey) return this
            if (isBooleanComplement(this, other)) return RemoteBoolean(true)
        }
        return RemoteBoolean(
            intValue = intValue or other.intValue,
            cacheKey = RemoteOperationCacheKey.create(BooleanOperationKey.Or, this, other),
        )
    }

    override fun and(other: RemoteBoolean): RemoteBoolean {
        val aConst = constantValueOrNull
        val bConst = other.constantValueOrNull
        if (aConst != null && bConst != null) {
            return RemoteBoolean(aConst && bConst)
        }
        if (cacheKey.isDeterministic()) {
            if (cacheKey == other.cacheKey) return this
            if (isBooleanComplement(this, other)) return RemoteBoolean(false)
        }
        return RemoteBoolean(
            intValue = intValue and other.intValue,
            cacheKey = RemoteOperationCacheKey.create(BooleanOperationKey.And, this, other),
        )
    }

    override fun xor(other: RemoteBoolean): RemoteBoolean {
        val aConst = constantValueOrNull
        val bConst = other.constantValueOrNull
        if (aConst != null && bConst != null) {
            return RemoteBoolean(aConst xor bConst)
        }
        if (cacheKey.isDeterministic()) {
            if (cacheKey == other.cacheKey) return RemoteBoolean(false)
            if (isBooleanComplement(this, other)) return RemoteBoolean(true)
        }
        return RemoteBoolean(
            intValue = intValue xor other.intValue,
            cacheKey = RemoteOperationCacheKey.create(BooleanOperationKey.Xor, this, other),
        )
    }

    override fun getId(creationState: RemoteComposeCreationState): Int =
        creationState.getId(intValue)

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)
}

private fun unwrapBooleanNegation(value: RemoteBoolean): RemoteBoolean? {
    val key = value.cacheKey
    if (key is RemoteOperationCacheKey && key.op == BooleanOperationKey.Not && key.args.size == 1) {
        return (key.args[0] as? BaseRemoteStateCacheKey)?.getState() as? RemoteBoolean
    }
    return null
}

private fun isBooleanComplement(a: RemoteBoolean, b: RemoteBoolean): Boolean {
    val aKey = a.cacheKey
    val bKey = b.cacheKey
    return (aKey is RemoteOperationCacheKey &&
        aKey.op == BooleanOperationKey.Not &&
        aKey.args.size == 1 &&
        aKey.args[0] == bKey) ||
        (bKey is RemoteOperationCacheKey &&
            bKey.op == BooleanOperationKey.Not &&
            bKey.args.size == 1 &&
            bKey.args[0] == aKey)
}

private class RemoteBooleanImpl(
    override val intValue: RemoteInt,
    cacheKey: RemoteStateCacheKey,
    val floatComparison: SelectFloatCondition.FloatComparison? = null,
    val intComparison: SelectIntCondition.IntComparison? = null,
) : BaseRemoteBoolean(cacheKey)

internal enum class BooleanOperationKey(override val precedence: Int = 100) : RemoteOperation {
    Not(5),
    Equal(2),
    NotEqual(2),
    And(1),
    Or(1),
    Xor(1),
    ToRemoteInt,
    RemoteBoolean,
    SelectString(0),
    SelectFloat(0),
    SelectInt(0),
    SelectBoolean(0);

    override fun toDebugString(args: List<RemoteStateCacheKey>): String {
        return when (this) {
            Not -> "!${args[0].toOperandString(precedence)}"
            And -> args.formatOp("and", precedence)
            Or -> args.formatOp("or", precedence)
            Xor -> args.formatOp("xor", precedence)
            Equal -> args.formatOp("==", precedence)
            NotEqual -> args.formatOp("!=", precedence)
            ToRemoteInt -> "${args[0].toOperandString(precedence)}.toRemoteInt()"
            RemoteBoolean -> "RemoteBoolean(${args[0].toDebugString()})"
            SelectString,
            SelectFloat,
            SelectInt,
            SelectBoolean ->
                "${args[0].toOperandString(1)} ? ${args[1].toOperandString(0)} : ${args[2].toOperandString(0)}"
        }
    }

    override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> {
        return when (this) {
            Not -> !(args[0] as RemoteBoolean)
            And -> (args[0] as RemoteBoolean) and (args[1] as RemoteBoolean)
            Or -> (args[0] as RemoteBoolean) or (args[1] as RemoteBoolean)
            Xor -> (args[0] as RemoteBoolean) xor (args[1] as RemoteBoolean)
            Equal -> (args[0] as RemoteBoolean).isEqualTo(args[1] as RemoteBoolean)
            NotEqual -> (args[0] as RemoteBoolean).isNotEqualTo(args[1] as RemoteBoolean)
            ToRemoteInt -> (args[0] as RemoteBoolean).toRemoteInt()
            RemoteBoolean -> RemoteBoolean(args[0] as RemoteInt)
            SelectString ->
                (args[0] as RemoteBoolean).select(
                    args[1] as RemoteString,
                    args[2] as RemoteString,
                )
            SelectFloat ->
                (args[0] as RemoteBoolean).select(args[1] as RemoteFloat, args[2] as RemoteFloat)
            SelectInt ->
                (args[0] as RemoteBoolean).select(args[1] as RemoteInt, args[2] as RemoteInt)
            SelectBoolean ->
                (args[0] as RemoteBoolean).select(
                    args[1] as RemoteBoolean,
                    args[2] as RemoteBoolean,
                )
        }
    }

    override fun isResultWholeNumber(args: List<RemoteStateCacheKey>): Boolean =
        if (this == SelectFloat) {
            args.size >= 3 && args[1].isResultWholeNumber && args[2].isResultWholeNumber
        } else {
            resultKind.isIntegral
        }

    override val resultKind: RemoteValueKind
        get() =
            when (this) {
                Not,
                Equal,
                NotEqual,
                And,
                Or,
                Xor,
                RemoteBoolean,
                SelectBoolean -> RemoteValueKind.BOOLEAN
                ToRemoteInt,
                SelectInt -> RemoteValueKind.INT
                SelectFloat -> RemoteValueKind.FLOAT
                SelectString -> RemoteValueKind.STRING
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
            MutableRemoteBooleanImpl(
                intValue = MutableRemoteInt.createMutableForId(id),
                cacheKey = RemoteStateIdKey(id.toInt()),
            )
    }
}

private class MutableRemoteBooleanImpl(
    override val intValue: MutableRemoteInt,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteBoolean(cacheKey), MutableRemoteBoolean {
    override val asEncodedMutable: MutableRemoteInt
        get() = intValue

    override fun adaptToType(candidate: RemoteValue<*>): RemoteValue<*> =
        if (candidate is MutableRemoteBoolean) candidate else this
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
        intValue =
            if (value) {
                RemoteInt(1)
            } else {
                RemoteInt(0)
            },
        cacheKey = RemoteConstantCacheKey(value),
    )

/**
 * Creates a [MutableRemoteBoolean] with an initial [Boolean] [initialValue].
 *
 * @param initialValue The initial value for this mutable boolean.
 * @return A [MutableRemoteBoolean] initialized with [initialValue].
 */
@RememberInComposition
public fun MutableRemoteBoolean(initialValue: Boolean): MutableRemoteBoolean =
    MutableRemoteBooleanImpl(
        intValue = MutableRemoteInt(if (initialValue) 1 else 0),
        cacheKey = RemoteStateInstanceKey(),
    )

/** Extension property to convert a [Boolean] to a [RemoteBoolean]. */
public val Boolean.rb: RemoteBoolean
    get() {
        return RemoteBoolean(this)
    }
