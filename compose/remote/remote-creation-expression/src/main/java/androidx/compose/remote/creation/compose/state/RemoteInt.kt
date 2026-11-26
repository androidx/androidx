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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY3
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_NONE
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_COMMA_PERIOD
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.withSign
import kotlin.reflect.KClass

/**
 * Read-only supertype for all remote integer representations.
 *
 * [RemoteInt] represents a 32-bit signed integer value that may be a constant, a named variable, or
 * a dynamic expression (such as an arithmetic or bitwise operation). For writable integer
 * variables, see [MutableRemoteInt].
 *
 * Operations on [RemoteInt] are evaluated dynamically on the remote player at runtime unless all
 * inputs are known constants, in which case constant folding evaluates the result immediately at
 * creation time (see [RemoteValue]).
 *
 * Comparing [RemoteInt] values with `==` checks structural equality (matching constant values or
 * expression DAGs), whereas [MutableRemoteInt] instances compare by their underlying mutable
 * variable; see [RemoteValue]. For runtime equality on the remote player, use [isEqualTo].
 */
@Stable
public sealed interface RemoteInt : RemoteValue<Int> {
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Int?

    /**
     * Converts this [RemoteInt] to a [RemoteFloat]. If the [RemoteInt] is a literal, it's directly
     * converted to a float. Otherwise, a [RemoteFloat] is created that references the remote float
     * ID of this integer.
     *
     * @return A [RemoteFloat] representing this integer as a float.
     */
    public fun toRemoteFloat(): RemoteFloat

    /**
     * Converts this [RemoteInt] to a [RemoteLong].
     *
     * @return A [RemoteLong] representing this integer as a long.
     */
    public fun toRemoteLong(): RemoteLong

    /**
     * Converts this [RemoteInt] to a [RemoteString] using the default localized integer format.
     *
     * @return A [RemoteString] representing the formatted integer value.
     */
    public fun toRemoteString(): RemoteString

    /**
     * Returns a [RemoteInt] that is a reference of this RemoteInt.
     *
     * This is temporarily useful because the floatArray has a maximum size.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun createReference(): RemoteInt

    /**
     * Adds [other] to this [RemoteInt].
     *
     * @param other integer value to add
     * @return a [RemoteInt] evaluating to `this + other`
     */
    public operator fun plus(other: Int): RemoteInt

    /**
     * Subtracts [other] from this [RemoteInt].
     *
     * @param other integer value to subtract
     * @return a [RemoteInt] evaluating to `this - other`
     */
    public operator fun minus(other: Int): RemoteInt

    /**
     * Multiplies this [RemoteInt] by [other].
     *
     * @param other integer multiplier
     * @return a [RemoteInt] evaluating to `this * other`
     */
    public operator fun times(other: Int): RemoteInt

    /**
     * Divides this [RemoteInt] by [other] using truncating integer division.
     *
     * @param other integer divisor
     * @return a [RemoteInt] evaluating to `this / other`
     */
    public operator fun div(other: Int): RemoteInt

    /**
     * Computes the remainder of truncating division of this [RemoteInt] by [other].
     *
     * @param other integer divisor
     * @return a [RemoteInt] evaluating to `this % other`
     */
    public operator fun rem(other: Int): RemoteInt

    /**
     * Adds [other] to this [RemoteInt].
     *
     * @param other [RemoteInt] to add
     * @return a [RemoteInt] evaluating to `this + other`
     */
    public operator fun plus(other: RemoteInt): RemoteInt

    /**
     * Subtracts [other] from this [RemoteInt].
     *
     * @param other [RemoteInt] to subtract
     * @return a [RemoteInt] evaluating to `this - other`
     */
    public operator fun minus(other: RemoteInt): RemoteInt

    /**
     * Multiplies this [RemoteInt] by [other].
     *
     * @param other [RemoteInt] multiplier
     * @return a [RemoteInt] evaluating to `this * other`
     */
    public operator fun times(other: RemoteInt): RemoteInt

    /**
     * Divides this [RemoteInt] by [other] using truncating integer division.
     *
     * @param other [RemoteInt] divisor
     * @return a [RemoteInt] evaluating to `this / other`
     */
    public operator fun div(other: RemoteInt): RemoteInt

    /**
     * Computes the remainder of truncating division of this [RemoteInt] by [other].
     *
     * @param other [RemoteInt] divisor
     * @return a [RemoteInt] evaluating to `this % other`
     */
    public operator fun rem(other: RemoteInt): RemoteInt

    /**
     * Negates this [RemoteInt].
     *
     * @return a [RemoteInt] evaluating to `-this`
     */
    public operator fun unaryMinus(): RemoteInt

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun inv(): RemoteInt

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val absoluteValue: RemoteInt

    public companion object {
        /**
         * Creates a [RemoteInt] referencing a remote ID.
         *
         * @param v The remote ID.
         * @return A [RemoteInt] referencing the ID.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(v: Long): RemoteInt {
            return RemoteInt(
                constantValueOrNull = null,
                cacheKey = RemoteStateIdKey(v.toInt()),
                arrayProvider = { _ -> longArrayOf(v) },
            )
        }

        /**
         * Creates a [RemoteInt] referencing a host system variable, such as
         * `RemoteContext.ID_YEAR`.
         *
         * @param id The system variable ID (an `ID_*` constant, not a long-encoded `INT_*` one).
         * @return A [RemoteInt] referencing the system variable.
         */
        internal fun createForContextId(id: Int): RemoteInt =
            createForId(id.toLong() + 0x100000000L)

        /**
         * Checks if a given [Long] value is considered a literal (i.e., not an ID or an OP code).
         *
         * @param v The [Long] value to check.
         * @return `true` if the value is a literal, `false` otherwise.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun isLiteral(v: Long): Boolean = v < 0x100000000L

        /**
         * Checks if a given [Long] value representing a remote integer is considered constant. This
         * performs a conservative check, assuming that variables are not constant unless explicitly
         * determined otherwise.
         *
         * @param v The [Long] value representing a remote integer (could be a literal or an ID).
         * @return `true` if the value is constant, `false` otherwise.
         */
        internal fun isConstant(v: Long): Boolean {
            if (isLiteral(v)) {
                return true
            }

            val id = Utils.idFromLong(v)
            if (id > IntegerExpressionEvaluator.OFFSET) {
                // Currently all integer operations have constant deterministic results.
                return true
            }

            // It's a variable which may or may not be constant. Unfortunately determining this
            // is currently expensive (would have to trawl through the ops serialized in the
            // document) so we conservatively assume it isn't constant.
            return false
        }

        /**
         * Creates a named [RemoteInt] with an initial value. Named remote ints can be set via
         * AndroidRemoteContext.setNamedInt.
         *
         * @param name The unique name for this remote int.
         * @param defaultValue The initial [Int] value for the named remote int.
         * @param domain The domain of the named integer (defaults to [RemoteValue.Domain.User]).
         * @return A [RemoteInt] representing the named int.
         */
        @JvmStatic
        public fun createNamedRemoteInt(
            name: String,
            defaultValue: Int,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
        ): RemoteInt {
            return RemoteInt(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                longArrayOf(
                    creationState.document
                        .addNamedInt(domain.prefixed(name), defaultValue)
                        .toLongEncodedId()
                )
            }
        }
    }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteInt]
     * is equal to the evaluated value of [other].
     *
     * @param other The [RemoteInt] to compare with.
     * @return A [RemoteBoolean] representing `this == other`.
     */
    public fun isEqualTo(other: RemoteInt): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteInt]
     * is not equal to the evaluated value of [other].
     *
     * @param other The [RemoteInt] to compare with.
     * @return A [RemoteBoolean] representing `this != other`.
     */
    public fun isNotEqualTo(other: RemoteInt): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteInt]
     * is less than the evaluated value of [other].
     *
     * @param other The [RemoteInt] to compare with.
     * @return A [RemoteBoolean] representing `this < other`.
     */
    public fun isLessThan(other: RemoteInt): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteInt]
     * is less than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteInt] to compare with.
     * @return A [RemoteBoolean] representing `this <= other`.
     */
    public fun isLessThanOrEqualTo(other: RemoteInt): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteInt]
     * is greater than the evaluated value of [other].
     *
     * @param other The [RemoteInt] to compare with.
     * @return A [RemoteBoolean] representing `this > other`.
     */
    public fun isGreaterThan(other: RemoteInt): RemoteBoolean

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if the evaluated value of this [RemoteInt]
     * is greater than or equal to the evaluated value of [other].
     *
     * @param other The [RemoteInt] to compare with.
     * @return A [RemoteBoolean] representing `this >= other`.
     */
    public fun isGreaterThanOrEqualTo(other: RemoteInt): RemoteBoolean

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted left by the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.shl] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public infix fun shl(other: RemoteInt): RemoteInt

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted right by the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.shr] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public infix fun shr(other: RemoteInt): RemoteInt

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic or with the value
     * of [other].
     *
     * This is designed to align with the standard Kotlin [Int.or] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public infix fun or(other: RemoteInt): RemoteInt

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic and with the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.and] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public infix fun and(other: RemoteInt): RemoteInt

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic xor with the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.xor] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public infix fun xor(other: RemoteInt): RemoteInt
}

/**
 * Operation keys identifying [RemoteInt] AST nodes in [RemoteOperationCacheKey].
 *
 * @property precedence operator precedence controlling parentheses in [toDebugString]
 * @property symbol infix operator symbol used by [toDebugString], or `null` for non-infix ops
 * @property opCode [IntegerExpressionEvaluator] RPN opcode, or `-1L` for operations without a
 *   single integer expression opcode
 */
internal enum class RemoteIntOperationKey(
    override val precedence: Int = 100,
    internal val symbol: String? = null,
    internal val opCode: Long = -1L,
) : RemoteOperation {
    ToRemoteString,
    Add(3, "+", OP_ADD),
    Sub(3, "-", OP_SUB),
    Mul(4, "*", OP_MUL),
    Div(4, "/", OP_DIV),
    Mod(4, "%", OP_MOD),
    And(1, opCode = OP_AND),
    Or(1, opCode = OP_OR),
    Xor(1, opCode = OP_XOR),
    Shl(2, "shl", OP_SHL),
    Shr(2, "shr", OP_SHR),
    Abs(opCode = OP_ABS),
    Neg(5, opCode = OP_NEG),
    Not(5, opCode = OP_NOT),
    CopySign(opCode = OP_COPY_SIGN),
    Min(opCode = OP_MIN),
    Max(opCode = OP_MAX),
    Id,
    ToFloat,
    CompareEQ(1, "=="),
    CompareNE(1, "!="),
    CompareLT(1, "<"),
    CompareLE(1, "<="),
    CompareGT(1, ">"),
    CompareGE(1, ">="),
    Reference,
    Clamp(opCode = OP_CLAMP),
    SelectIfLT(0),
    SelectIfLE(0),
    SelectIfGT(0),
    SelectIfGE(0);

    override fun toDebugString(args: List<RemoteStateCacheKey>): String {
        if (symbol != null && args.size == 2) {
            return args.formatOp(symbol, precedence)
        }
        return when (this) {
            Neg -> "-${args[0].toOperandString(precedence)}"
            Not -> "${args[0].toOperandString(precedence)}.inv()"
            And -> args.formatOp("and", precedence)
            Or -> args.formatOp("or", precedence)
            Xor -> args.formatOp("xor", precedence)
            ToFloat -> "${args[0].toOperandString(precedence)}.toRemoteFloat()"
            ToRemoteString -> "${args[0].toOperandString(precedence)}.toRemoteString()"
            SelectIfLT -> args.formatSelect("<")
            SelectIfLE -> args.formatSelect("<=")
            SelectIfGT -> args.formatSelect(">")
            SelectIfGE -> args.formatSelect(">=")
            else -> formatCamelCaseFunction(args)
        }
    }

    override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> {
        return when (this) {
            Add -> (args[0] as RemoteInt) + (args[1] as RemoteInt)
            Sub -> (args[0] as RemoteInt) - (args[1] as RemoteInt)
            Mul -> (args[0] as RemoteInt) * (args[1] as RemoteInt)
            Div -> (args[0] as RemoteInt) / (args[1] as RemoteInt)
            Mod -> (args[0] as RemoteInt) % (args[1] as RemoteInt)
            And -> (args[0] as RemoteInt) and (args[1] as RemoteInt)
            Or -> (args[0] as RemoteInt) or (args[1] as RemoteInt)
            Xor -> (args[0] as RemoteInt) xor (args[1] as RemoteInt)
            Shl -> (args[0] as RemoteInt) shl (args[1] as RemoteInt)
            Shr -> (args[0] as RemoteInt) shr (args[1] as RemoteInt)
            Abs -> (args[0] as RemoteInt).absoluteValue
            Neg -> -(args[0] as RemoteInt)
            Not -> (args[0] as RemoteInt).inv()
            CopySign -> copySign(args[0] as RemoteInt, args[1] as RemoteInt)
            Min -> min(args[0] as RemoteInt, args[1] as RemoteInt)
            Max -> max(args[0] as RemoteInt, args[1] as RemoteInt)
            Clamp -> clamp(args[0] as RemoteInt, args[1] as RemoteInt, args[2] as RemoteInt)
            ToFloat -> (args[0] as RemoteInt).toRemoteFloat()
            ToRemoteString -> (args[0] as RemoteInt).toRemoteString()
            CompareEQ -> (args[0] as RemoteInt).isEqualTo(args[1] as RemoteInt)
            CompareNE -> (args[0] as RemoteInt).isNotEqualTo(args[1] as RemoteInt)
            CompareLT -> (args[0] as RemoteInt).isLessThan(args[1] as RemoteInt)
            CompareLE -> (args[0] as RemoteInt).isLessThanOrEqualTo(args[1] as RemoteInt)
            CompareGT -> (args[0] as RemoteInt).isGreaterThan(args[1] as RemoteInt)
            CompareGE -> (args[0] as RemoteInt).isGreaterThanOrEqualTo(args[1] as RemoteInt)
            Reference -> (args[0] as RemoteInt).createReference()
            SelectIfLT ->
                selectIfLt(
                    args[0] as RemoteInt,
                    args[1] as RemoteInt,
                    args[2] as RemoteInt,
                    args[3] as RemoteInt,
                )
            SelectIfLE ->
                selectIfLe(
                    args[0] as RemoteInt,
                    args[1] as RemoteInt,
                    args[2] as RemoteInt,
                    args[3] as RemoteInt,
                )
            SelectIfGT ->
                selectIfGt(
                    args[0] as RemoteInt,
                    args[1] as RemoteInt,
                    args[2] as RemoteInt,
                    args[3] as RemoteInt,
                )
            SelectIfGE ->
                selectIfGe(
                    args[0] as RemoteInt,
                    args[1] as RemoteInt,
                    args[2] as RemoteInt,
                    args[3] as RemoteInt,
                )
            Id -> args[0] as RemoteInt
        }
    }

    override fun isResultWholeNumber(args: List<RemoteStateCacheKey>): Boolean =
        this != ToRemoteString

    override fun canFailOnOperandIndex(operandIndex: Int): Boolean =
        operandIndex == 1 && (this == Div || this == Mod)

    override val resultKind: RemoteValueKind
        get() =
            when (this) {
                ToFloat -> RemoteValueKind.FLOAT
                ToRemoteString -> RemoteValueKind.STRING
                CompareEQ,
                CompareNE,
                CompareLT,
                CompareLE,
                CompareGT,
                CompareGE -> RemoteValueKind.BOOLEAN
                else -> RemoteValueKind.INT
            }
}

internal fun RemoteInt(
    constantValueOrNull: Int?,
    cacheKey: RemoteStateCacheKey,
    arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
): RemoteInt = RemoteIntImpl(constantValueOrNull, cacheKey, arrayProvider)

internal val RemoteInt.arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray
    get() = (this as BaseRemoteInt).arrayProvider

/**
 * Retrieves the [LongArray] representing this [RemoteInt]'s expression using the provided
 * [creationState]. It utilizes a cache within the [creationState] to avoid redundant computations,
 * improving performance.
 *
 * @param creationState The current [RemoteComposeCreationState].
 * @return The [LongArray] representing this remote integer's expression.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteInt.arrayForCreationState(creationState: RemoteComposeCreationState): LongArray =
    creationState.getOrPutLongArray(cacheKey) { arrayProvider(creationState) }

/**
 * Returns `true` if this [RemoteInt] already has a variable ID in [creationState].
 *
 * @param creationState state tracking written variable IDs
 */
internal fun RemoteInt.hasBeenWrittenToDoc(creationState: RemoteComposeCreationState): Boolean =
    creationState.remoteVariableToId.contains(cacheKey)

/** Simplifies and constructs a unary [RemoteInt] operation via [IntPeepholeOptimizer]. */
internal fun unaryOp(
    a: RemoteInt,
    op: RemoteIntOperationKey,
    directEval: (Int) -> Int,
): RemoteInt = IntPeepholeOptimizer.simplifyUnaryOp(op, a, directEval)

internal abstract class BaseRemoteInt(cacheKey: RemoteStateCacheKey) :
    BaseRemoteValue<Int>(cacheKey), RemoteInt {
    override val equalityType: KClass<*>
        get() = RemoteInt::class

    override val valueKind: RemoteValueKind
        get() = RemoteValueKind.INT

    override fun createForIdOrNull(id: Int): RemoteValue<*> =
        RemoteInt.createForId(0x100000000L + id)

    override fun adaptToType(candidate: RemoteValue<*>): RemoteValue<*> =
        when (candidate) {
            is RemoteInt -> candidate
            is RemoteFloat -> candidate.toRemoteInt()
            is RemoteBoolean -> candidate.toRemoteInt()
            else -> this
        }

    abstract val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray

    override fun toRemoteFloat(): RemoteFloat {
        constantValueOrNull?.let {
            return RemoteFloat(it.toFloat())
        }
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(RemoteIntOperationKey.ToFloat, this),
        ) { creationState ->
            val key = cacheKey // Needed because smart cast with cacheKey is impossible.
            if (key is RemoteOperationCacheKey && key.op == RemoteFloatOperationKey.ToInt) {
                // Force conversion from float to int with a no-op expression so that truncation
                // occurs as expected for a float->int->float round trip. Directly instantiating
                // [RemoteInt] skips the peephole optimizer.
                val temp =
                    RemoteInt(
                        constantValueOrNull = null,
                        cacheKey =
                            RemoteOperationCacheKey.create(RemoteIntOperationKey.Add, this, 0),
                    ) { cs ->
                        combineToLongArray(cs, arrayOf(this), 0L, OP_ADD)
                    }
                floatArrayOf(creationState.getFloatId(temp))
            } else {
                floatArrayOf(getFloatId(creationState))
            }
        }
    }

    override fun toRemoteLong(): RemoteLong {
        return RemoteLong.fromLowHigh(this, selectIfLt(this, 0.ri, (-1).ri, 0.ri))
    }

    override fun toRemoteString(): RemoteString =
        toRemoteFloat()
            .toRemoteStringWithPadding(
                before = 40,
                after = 0,
                flags = SEPARATOR_COMMA_PERIOD or GROUPING_BY3 or PAD_AFTER_NONE or PAD_PRE_NONE,
            )

    override fun createReference(): RemoteInt {
        return RemoteInt(
            constantValueOrNull = constantValueOrNull,
            cacheKey = RemoteOperationCacheKey.create(RemoteIntOperationKey.Reference, this),
            arrayProvider = { creationState ->
                longArrayOf(getLongId(creationState))
            },
        )
    }

    override fun plus(other: Int): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Add) { a, b -> a + b }

    override fun minus(other: Int): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Sub) { a, b -> a - b }

    override fun times(other: Int): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Mul) { a, b -> a * b }

    override fun div(other: Int): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Div) { a, b -> a / b }

    override fun rem(other: Int): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Mod) { a, b -> a % b }

    override fun plus(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Add) { a, b -> a + b }

    override fun minus(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Sub) { a, b -> a - b }

    override fun times(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Mul) { a, b -> a * b }

    override fun div(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Div) { a, b -> a / b }

    override fun rem(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Mod) { a, b -> a % b }

    override fun unaryMinus(): RemoteInt = unaryOp(this, RemoteIntOperationKey.Neg) { v -> -v }

    override fun inv(): RemoteInt = unaryOp(this, RemoteIntOperationKey.Not) { v -> v.inv() }

    override val absoluteValue: RemoteInt
        get() = unaryOp(this, RemoteIntOperationKey.Abs) { v -> abs(v) }

    override fun isEqualTo(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteIntOperationKey.CompareEQ,
            { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) },
            IntComparisonOp.EQ,
        ) { a, b ->
            if (a == b) 1 else 0
        }

    override fun isNotEqualTo(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteIntOperationKey.CompareNE,
            { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) },
            IntComparisonOp.NE,
        ) { a, b ->
            if (a != b) 1 else 0
        }

    override fun isLessThan(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteIntOperationKey.CompareLT,
            { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_IFELSE) },
            IntComparisonOp.LT,
        ) { a, b ->
            if (a < b) 1 else 0
        }

    override fun isLessThanOrEqualTo(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteIntOperationKey.CompareLE,
            { a, b -> longArrayOf(1, 0, *a, *b, OP_SUB, OP_IFELSE) },
            IntComparisonOp.LE,
        ) { a, b ->
            if (a <= b) 1 else 0
        }

    override fun isGreaterThan(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteIntOperationKey.CompareGT,
            { a, b -> longArrayOf(0, 1, *a, *b, OP_SUB, OP_IFELSE) },
            IntComparisonOp.GT,
        ) { a, b ->
            if (a > b) 1 else 0
        }

    override fun isGreaterThanOrEqualTo(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            RemoteIntOperationKey.CompareGE,
            { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_IFELSE) },
            IntComparisonOp.GE,
        ) { a, b ->
            if (a >= b) 1 else 0
        }

    override fun shl(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Shl) { a, b -> a shl b }

    override fun shr(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Shr) { a, b -> a shr b }

    override fun or(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Or) { a, b -> a or b }

    override fun and(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.And) { a, b -> a and b }

    override fun xor(other: RemoteInt): RemoteInt =
        binaryOp(this, other, RemoteIntOperationKey.Xor) { a, b -> a xor b }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        writeIntToDocument(creationState)
}

private class RemoteIntImpl(
    override val constantValueOrNull: Int?,
    cacheKey: RemoteStateCacheKey,
    override val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
) : BaseRemoteInt(cacheKey)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun LongArray.isLiteral(): Boolean = size == 1 && RemoteInt.isLiteral(get(0))

/** Simplifies and constructs a binary [RemoteInt] operation with a literal right-hand operand. */
internal fun binaryOp(
    a: RemoteInt,
    b: Int,
    op: RemoteIntOperationKey,
    directEval: (Int, Int) -> Int,
): RemoteInt = IntPeepholeOptimizer.simplifyBinaryOpRightConst(op, a, b, directEval)

/** Simplifies and constructs a binary [RemoteInt] operation via [IntPeepholeOptimizer]. */
internal fun binaryOp(
    a: RemoteInt,
    b: RemoteInt,
    op: RemoteIntOperationKey,
    directEval: (Int, Int) -> Int,
): RemoteInt = IntPeepholeOptimizer.simplifyBinaryOp(op, a, b, directEval)

/**
 * Boilerplate for implementing a binary comparison operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param expressionGenerator Generator for the comparison expression [LongArray] used when both
 *   sources aren\'t a const float.
 * @param directEval When the sources are const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun comparisonOp(
    a: RemoteInt,
    b: RemoteInt,
    op: RemoteIntOperationKey,
    expressionGenerator: (LongArray, LongArray) -> LongArray,
    intComparisonOp: IntComparisonOp? = null,
    directEval: (Int, Int) -> Int,
): RemoteBoolean {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteBoolean(directEval(aConst, bConst) != 0)
    }

    val boolKey = RemoteOperationCacheKey.create(op, a, b)
    return RemoteBoolean(
        intValue =
            RemoteInt(
                constantValueOrNull = null,
                cacheKey =
                    RemoteOperationCacheKey(BooleanOperationKey.ToRemoteInt, listOf(boolKey)),
            ) { creationState ->
                val aArray = a.arrayForCreationState(creationState)
                val bArray = b.arrayForCreationState(creationState)
                // A comparisonOp adds five op codes
                val combinedSize = aArray.size + bArray.size + 5
                if (combinedSize > MAX_SAFE_LONG_ARRAY) { // Check if new array would exceed limit
                    expressionGenerator(
                        longArrayOf(creationState.getLongId(a)),
                        longArrayOf(creationState.getLongId(b)),
                    )
                } else {
                    expressionGenerator(aArray, bArray)
                }
            },
        cacheKey = boolKey,
        intComparison = intComparisonOp?.let { SelectIntCondition.IntComparison(a, b, it) },
    )
}

/**
 * Returns a [RemoteInt] that evaluates to the value of [v] with the sign of [sign]. This is a
 * remote equivalent of `Math.copySign`.
 *
 * @param v The [RemoteInt] whose magnitude is used.
 * @param sign The [RemoteInt] whose sign is used.
 * @return A [RemoteInt] with the magnitude of `v` and the sign of `sign`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun copySign(v: RemoteInt, sign: RemoteInt): RemoteInt =
    binaryOp(v, sign, RemoteIntOperationKey.CopySign) { a, b ->
        a.toDouble().withSign(b.toDouble()).toInt()
    }

/**
 * Returns a [RemoteInt] that evaluates to the minimum of [a] and [b].
 *
 * @param a The first [RemoteInt].
 * @param b The second [RemoteInt].
 * @return A [RemoteInt] representing the minimum of `a` and `b`.\
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RemoteInt, b: RemoteInt): RemoteInt =
    binaryOp(a, b, RemoteIntOperationKey.Min) { a, b -> min(a, b) }

/**
 * Returns a [RemoteInt] that evaluates to the maximum of [a] and [b].
 *
 * @param a The first [RemoteInt].
 * @param b The second [RemoteInt].
 * @return A [RemoteInt] representing the maximum of `a` and `b`.\
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RemoteInt, b: RemoteInt): RemoteInt =
    binaryOp(a, b, RemoteIntOperationKey.Max) { a, b -> max(a, b) }

/**
 * Returns a [RemoteInt] that evaluates to [value] clamped between [min] and [max].
 *
 * @param min The lower bound [RemoteInt].
 * @param max The upper bound [RemoteInt].
 * @param value The [RemoteInt] to clamp.
 * @return A [RemoteInt] representing the clamped value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun clamp(min: RemoteInt, max: RemoteInt, value: RemoteInt): RemoteInt {
    val minConst = min.constantValueOrNull
    val maxConst = max.constantValueOrNull
    val valueConst = value.constantValueOrNull
    if (minConst != null && maxConst != null && valueConst != null) {
        return if (valueConst < minConst) {
            min
        } else if (valueConst > maxConst) {
            max
        } else {
            value
        }
    }

    return RemoteInt(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(RemoteIntOperationKey.Clamp, min, max, value),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(min, max, value), OP_CLAMP)
    }
}

/**
 * A mutable implementation of [RemoteInt].
 *
 * Two [MutableRemoteInt] instances are `==` only if they reference the same underlying mutable
 * variable; separately created instances (e.g., via `MutableRemoteInt(initialValue)`) represent
 * distinct mutable variables and are never `==`.
 */
@Stable
public sealed interface MutableRemoteInt : RemoteInt, MutableRemoteValue<Int> {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteInt] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Long): MutableRemoteInt =
            MutableRemoteIntImpl(
                constantValueOrNull = null,
                cacheKey = RemoteStateIdKey(id.toInt()),
                idProvider = { _ -> id },
            )
    }
}

private class MutableRemoteIntImpl(
    override val constantValueOrNull: Int? = null,
    cacheKey: RemoteStateCacheKey,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Long,
) : BaseRemoteInt(cacheKey), MutableRemoteInt {
    constructor(
        id: Long
    ) : this(
        constantValueOrNull = null,
        cacheKey = RemoteStateIdKey(id.toInt()),
        idProvider = { _ -> id },
    )

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray =
        { creationState ->
            val id =
                creationState.getOrPutVariableId(cacheKey) {
                    Utils.idFromLong(idProvider(creationState)).toInt()
                }
            longArrayOf(id.toLong() + 0x100000000L)
        }

    override fun adaptToType(candidate: RemoteValue<*>): RemoteValue<*> =
        if (candidate is MutableRemoteInt) candidate else this

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        Utils.idFromLong(idProvider(creationState)).toInt()
}

/**
 * Creates a [RemoteInt] from a literal constant [Int] [value].
 *
 * @param value The constant [Int] value.
 * @return A [RemoteInt] representing the constant integer.
 */
public fun RemoteInt(value: Int): RemoteInt =
    RemoteInt(
        constantValueOrNull = value,
        cacheKey = RemoteConstantCacheKey(value),
        arrayProvider = { longArrayOf(value.toLong()) },
    )

/**
 * Creates a [MutableRemoteInt] initialized with [initialValue].
 *
 * @param initialValue The initial [Int] value.
 * @return A [MutableRemoteInt] initialized with [initialValue].
 */
@RememberInComposition
public fun MutableRemoteInt(initialValue: Int): MutableRemoteInt =
    MutableRemoteIntImpl(
        constantValueOrNull = null,
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState ->
            creationState.document.addMutableInt(initialValue).toLongEncodedId()
        },
    )

/**
 * Returns [ifTrue] if [a] < [b], otherwise returns [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the comparison.
 * @param b The right-hand side [RemoteInt] for the comparison.
 * @param ifTrue The [RemoteInt] to return if `a < b`.
 * @param ifFalse The [RemoteInt] to return if `a >= b`.
 * @return A [RemoteInt] representing the selected value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLt(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteInt,
    ifFalse: RemoteInt,
): RemoteInt {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA < constB) ifTrue else ifFalse
    }
    return SelectIntCondition.IntComparison(a, b, IntComparisonOp.LT).createSelect(ifTrue, ifFalse)
}

/**
 * Returns a [RemoteInt] that evaluates to [ifTrue] if [a] <= [b], otherwise returns [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the comparison.
 * @param b The right-hand side [RemoteInt] for the comparison.
 * @param ifTrue The [RemoteInt] to return if `a <= b`.
 * @param ifFalse The [RemoteInt] to return if `a > b`.
 * @return A [RemoteInt] representing the selected value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLe(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteInt,
    ifFalse: RemoteInt,
): RemoteInt {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA <= constB) ifTrue else ifFalse
    }
    return SelectIntCondition.IntComparison(a, b, IntComparisonOp.LE).createSelect(ifTrue, ifFalse)
}

/**
 * Returns a [RemoteInt] that evaluates to [ifTrue] if [a] > [b], otherwise returns [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the comparison.
 * @param b The right-hand side [RemoteInt] for the comparison.
 * @param ifTrue The [RemoteInt] to return if `a > b`.
 * @param ifFalse The [RemoteInt] to return if `a <= b`.
 * @return A [RemoteInt] representing the selected value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGt(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteInt,
    ifFalse: RemoteInt,
): RemoteInt {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA > constB) ifTrue else ifFalse
    }
    return SelectIntCondition.IntComparison(a, b, IntComparisonOp.GT).createSelect(ifTrue, ifFalse)
}

/**
 * Returns a [RemoteInt] that evaluates to [ifTrue] if [a] >= [b], otherwise returns [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the comparison.
 * @param b The right-hand side [RemoteInt] for the comparison.
 * @param ifTrue The [RemoteInt] to return if `a >= b`.
 * @param ifFalse The [RemoteInt] to return if `a < b`.
 * @return A [RemoteInt] representing the selected value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGe(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteInt,
    ifFalse: RemoteInt,
): RemoteInt {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA >= constB) ifTrue else ifFalse
    }
    return SelectIntCondition.IntComparison(a, b, IntComparisonOp.GE).createSelect(ifTrue, ifFalse)
}

/** Extension property to convert an [Int] to a [RemoteInt]. */
public val Int.ri: RemoteInt
    get() {
        return RemoteInt(this)
    }
