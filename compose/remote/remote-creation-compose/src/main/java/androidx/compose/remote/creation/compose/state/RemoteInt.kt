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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteInt.OperationKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val OP_ABS = 0x100000000L + IntegerExpressionEvaluator.I_ABS
private const val OP_ADD = 0x100000000L + IntegerExpressionEvaluator.I_ADD
private const val OP_AND = 0x100000000L + IntegerExpressionEvaluator.I_AND
private const val OP_CLAMP = 0x100000000L + IntegerExpressionEvaluator.I_CLAMP
private const val OP_COPY_SIGN = 0x100000000L + IntegerExpressionEvaluator.I_COPY_SIGN
private const val OP_DIV = 0x100000000L + IntegerExpressionEvaluator.I_DIV
private const val OP_IFELSE = 0x100000000L + IntegerExpressionEvaluator.I_IFELSE
private const val OP_MAX = 0x100000000L + IntegerExpressionEvaluator.I_MAX
private const val OP_MIN = 0x100000000L + IntegerExpressionEvaluator.I_MIN
private const val OP_MOD = 0x100000000L + IntegerExpressionEvaluator.I_MOD
private const val OP_MUL = 0x100000000L + IntegerExpressionEvaluator.I_MUL
private const val OP_NEG = 0x100000000L + IntegerExpressionEvaluator.I_NEG
private const val OP_NOT = 0x100000000L + IntegerExpressionEvaluator.I_NOT
private const val OP_OR = 0x100000000L + IntegerExpressionEvaluator.I_OR
private const val OP_SUB = 0x100000000L + IntegerExpressionEvaluator.I_SUB
private const val OP_SHL = 0x100000000L + IntegerExpressionEvaluator.I_SHL
private const val OP_SHR = 0x100000000L + IntegerExpressionEvaluator.I_SHR
private const val OP_XOR = 0x100000000L + IntegerExpressionEvaluator.I_XOR

private const val MAX_SAFE_LONG_ARRAY = 30

/**
 * Abstract base class for all remote integer representations.
 *
 * `RemoteInt` represents an integer value that can be a constant, a named variable, or a dynamic
 * expression (e.g., a bitwise OR).
 */
@Stable
public abstract class RemoteInt
internal constructor(
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Int?,
    cacheKey: RemoteStateCacheKey,
    internal val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
) : BaseRemoteState<Int>(cacheKey) {
    internal enum class OperationKey(
        override val precedence: Int = 100,
        public val symbol: String? = null,
    ) : DebuggableOperation {
        ToRemoteString,
        Add(3, "+"),
        Sub(3, "-"),
        Mul(4, "*"),
        Div(4, "/"),
        Mod(4, "%"),
        And(1),
        Or(1),
        Xor(1),
        Shl(2, "shl"),
        Shr(2, "shr"),
        Abs,
        Neg(5),
        Not(5),
        CopySign,
        Min,
        Max,
        Id,
        ToFloat,
        CompareEQ(1, "=="),
        CompareNE(1, "!="),
        CompareLT(1, "<"),
        CompareLE(1, "<="),
        CompareGT(1, ">"),
        CompareGE(1, ">="),
        Reference,
        Clamp,
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
    }

    /**
     * Retrieves the [LongArray] representing this [RemoteInt]\'s expression using the provided
     * [creationState]. It utilizes a cache within the [creationState] to avoid redundant
     * computations, improving performance.
     *
     * @param stateScope The current [RemoteStateScope].
     * @return The [LongArray] representing this remote integer\'s expression.
     */
    internal fun arrayForCreationState(stateScope: RemoteStateScope): LongArray {
        return stateScope.creationState.getOrPutLongArray(cacheKey) {
            arrayProvider(stateScope.creationState)
        }
    }

    internal fun hasBeenWrittenToDoc(creationState: RemoteComposeCreationState) =
        creationState.remoteVariableToId.contains(cacheKey)

    /**
     * Converts this [RemoteInt] to a [RemoteFloat]. If the [RemoteInt] is a literal, it\'s directly
     * converted to a float. Otherwise, a [RemoteFloatExpression] is created that references the
     * remote float ID of this integer.
     *
     * @return A [RemoteFloatExpression] representing this integer as a float.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteFloat(): RemoteFloat {
        constantValueOrNull?.let {
            return RemoteFloat(it.toFloat())
        }
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToFloat, this),
        ) { creationState ->
            val key = cacheKey // Needed because smart cast with cacheKey is impossible.
            if (key is RemoteOperationCacheKey && key.op == RemoteFloat.OperationKey.ToInt) {
                // Force conversion from float to int with a no-op expression so that truncation
                // occurs as expected for a float->int->float round trip. Note calling binaryOp like
                // this skips the peephole optimizer.
                val temp =
                    binaryOp(this, 0, OperationKey.Add, OP_ADD, { a, _ -> a }) { _, _ -> null }
                floatArrayOf(temp.getFloatIdForCreationState(creationState))
            } else {
                floatArrayOf(getFloatIdForCreationState(creationState))
            }
        }
    }

    /**
     * Converts this [RemoteInt] to a [RemoteLong].
     *
     * @return A [RemoteLong] representing this integer as a long.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteLong(): RemoteLong {
        return RemoteLong.fromLowHigh(this, selectIfLt(this, 0.ri, (-1).ri, 0.ri))
    }

    /**
     * Converts this [RemoteInt] to a [RemoteString] using the specified [format].
     *
     * This method maps the localized ICU [android.icu.text.DecimalFormat] configuration (including
     * padding, rounding, and digit constraints) to a remote-compatible string representation. It
     * specifically handles complex padding logic and threshold-based selections to ensure the
     * formatted output remains consistent when evaluated on the remote target.
     *
     * @param format The [android.icu.text.DecimalFormat] used to format the integer value. Defaults
     *   to [DefaultIntegerFormat].
     * @return A [RemoteString] representing the formatted integer value.
     */
    public fun toRemoteString(
        format: android.icu.text.DecimalFormat = DefaultIntegerFormat
    ): RemoteString {
        return toRemoteFloat().toRemoteString(format)
    }

    /**
     * Converts this RemoteInt to a RemoteString.
     *
     * This method maps the localized [DecimalFormat] symbols (such as separators and grouping
     * sizes) and configuration (such as padding and rounding) to a remote-compatible string
     * representation.
     *
     * @param format The [DecimalFormat] to use for determining separators, grouping, and padding.
     * @return A [RemoteString] representing the formatted float.
     */
    public fun toRemoteString(format: DecimalFormat): RemoteString {
        return toRemoteFloat().toRemoteString(format)
    }

    /**
     * Returns a [RemoteInt] that is a reference of this RemoteInt.
     *
     * This is temporarily useful because the floatArray has a maximum size.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun createReference(): RemoteInt {
        return RemoteIntExpression(
            constantValueOrNull = constantValueOrNull,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.Reference, this),
            arrayProvider = { creationState ->
                longArrayOf(getLongIdForCreationState(creationState))
            },
        )
    }

    /**
     * Boilerplate for implementing an unary operation.
     *
     * @param opCode The opcode to insert in the generated [LongArray] if the source isn\'t a const
     *   int.
     * @param directEval When the source is a const int, this lambda will be called to evaluate the
     *   result directly.
     */
    private fun unaryOp(op: OperationKey, opCode: Long, directEval: (Int) -> Int): RemoteInt {
        constantValueOrNull?.let {
            return RemoteInt(directEval(it))
        }
        return RemoteIntExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(op, this),
        ) { creationState ->
            combineToLongArray(creationState, arrayOf(this), opCode)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: Int): RemoteInt {
        if (v == 0) {
            return this
        }
        return binaryOp(this, v, OperationKey.Add, OP_ADD, { a, b -> a + b }) { array, opId ->
            when (opId) {
                OP_ADD -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] += v
                    maybeTrimIfZero(arrayCopy)
                }
                OP_SUB -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] -= v
                    maybeTrimIfZero(arrayCopy)
                }
                else -> null
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun minus(v: Int): RemoteInt {
        if (v == 0) {
            return this
        }
        return binaryOp(this, v, OperationKey.Sub, OP_SUB, { a, b -> a - b }) { array, opId ->
            when (opId) {
                OP_ADD -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] -= v
                    maybeTrimIfZero(arrayCopy)
                }
                OP_SUB -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] += v
                    maybeTrimIfZero(arrayCopy)
                }
                else -> null
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun times(v: Int): RemoteInt {
        if (v == 0) {
            return RemoteInt(0)
        }
        if (v == 1) {
            return this
        }
        if (constantValueOrNull != null && constantValueOrNull == 1) {
            return RemoteInt(v)
        }
        return binaryOp(this, v, OperationKey.Mul, OP_MUL, { a, b -> a * b }) { array, opId ->
            when (opId) {
                OP_MUL -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] *= v
                    maybeTrimIfOne(arrayCopy)
                }
                OP_DIV -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] /= v
                    maybeTrimIfOne(arrayCopy)
                }
                else -> null
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun div(v: Int): RemoteInt {
        if (constantValueOrNull != null && constantValueOrNull == 0) {
            return RemoteInt(0)
        }
        if (v == 1) {
            return this
        }
        return binaryOp(this, v, OperationKey.Div, OP_DIV, { a, b -> a / b }) { array, opId ->
            when (opId) {
                OP_MUL -> {
                    val arrayCopy = array.clone()
                    if (arrayCopy[arrayCopy.size - 2] % v == 0L) {
                        arrayCopy[arrayCopy.size - 2] /= v
                        maybeTrimIfOne(arrayCopy)
                    } else {
                        null
                    }
                }
                OP_DIV -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] *= v
                    maybeTrimIfOne(arrayCopy)
                }
                else -> null
            }
        }
    }

    private fun maybeTrimIfZero(array: LongArray) =
        if (array.size >= 2 && array[array.size - 2] == 0L) {
            array.copyOfRange(0, array.size - 2)
        } else {
            array
        }

    private fun maybeTrimIfOne(array: LongArray) =
        if (array.size >= 2 && array[array.size - 2] == 1L) {
            array.copyOfRange(0, array.size - 2)
        } else {
            array
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun rem(v: Int): RemoteInt =
        binaryOp(this, v, OperationKey.Mod, OP_MOD) { a, b -> a % b }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: RemoteInt): RemoteInt {
        v.constantValueOrNull?.let {
            return plus(it)
        }
        constantValueOrNull?.let {
            return v.plus(it)
        }
        return binaryOp(this, v, OperationKey.Add, OP_ADD) { a, b -> a + b }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun minus(v: RemoteInt): RemoteInt {
        v.constantValueOrNull?.let {
            return minus(it)
        }
        constantValueOrNull?.let {
            return (-v).plus(it)
        }
        return binaryOp(this, v, OperationKey.Sub, OP_SUB) { a, b -> a - b }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun times(v: RemoteInt): RemoteInt {
        if (
            (constantValueOrNull != null && constantValueOrNull == 0) ||
                (v.constantValueOrNull != null && v.constantValueOrNull == 0)
        ) {
            return RemoteInt(0)
        }
        v.constantValueOrNull?.let {
            return times(it)
        }
        constantValueOrNull?.let {
            return v.times(it)
        }
        return binaryOp(this, v, OperationKey.Mul, OP_MUL) { a, b -> a * b }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun div(v: RemoteInt): RemoteInt {
        if (constantValueOrNull != null && constantValueOrNull == 0) {
            return RemoteInt(0)
        }
        v.constantValueOrNull?.let {
            return div(it)
        }
        return binaryOp(this, v, OperationKey.Div, OP_DIV) { a, b -> a / b }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun rem(v: RemoteInt): RemoteInt =
        binaryOp(this, v, OperationKey.Mod, OP_MOD) { a, b -> a % b }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun unaryMinus(): RemoteInt = unaryOp(OperationKey.Neg, OP_NEG) { v -> -v }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun inv(): RemoteInt = unaryOp(OperationKey.Not, OP_NOT) { v -> v.inv() }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val absoluteValue: RemoteInt
        get() = unaryOp(OperationKey.Abs, OP_ABS) { v -> abs(v) }

    public companion object {
        internal val DefaultIntegerFormat =
            android.icu.text.DecimalFormat().apply { maximumFractionDigits = 0 }

        public operator fun invoke(value: Int): RemoteInt {
            return RemoteIntExpression(
                value,
                cacheKey = RemoteConstantCacheKey(value),
                { longArrayOf(value.toLong()) },
            )
        }

        /**
         * Creates a [RemoteInt] referencing a remote ID.
         *
         * @param v The remote ID.
         * @return A [RemoteInt] referencing the ID.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(v: Long): RemoteInt {
            return RemoteIntExpression(
                constantValueOrNull = null,
                cacheKey = RemoteStateIdKey(v.toInt()),
                arrayProvider = { _ -> longArrayOf(v) },
            )
        }

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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun isConstant(v: Long): Boolean {
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
         * Creates a [RemoteInt] instance from a [Long] value, which could be a literal or an ID.
         * The `hasConstantValue` is determined by calling [isConstant].
         *
         * @param value The constant [Long] value.
         * @return A [RemoteIntExpression] representing the constant integer.
         */

        /**
         * Creates a named [RemoteInt] with an initial value. Named remote ints can be set via
         * AndroidRemoteContext.setNamedInt.
         *
         * @param name The unique name for this remote long.
         * @param defaultValue The initial [Int] value for the named remote int.
         * @param domain The domain of the named integer (defaults to [RemoteState.Domain.User]).
         * @return A [RemoteInt] representing the named int.
         */
        @JvmStatic
        public fun createNamedRemoteInt(
            name: String,
            defaultValue: Int,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteInt {
            return RemoteIntExpression(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                longArrayOf(creationState.document.addNamedInt(domain.prefixed(name), defaultValue))
            }
        }
    }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [other] is equal to the value of this
     * [RemoteInt] or `false` otherwise.
     */
    public fun isEqualTo(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareEQ,
            { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) },
        ) { a, b ->
            if (a == b) 1 else 0
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated("Use isEqualTo instead", ReplaceWith("isEqualTo(other)"))
    public infix fun eq(other: RemoteInt): RemoteBoolean = isEqualTo(other)

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [other] is not equal to the value of
     * this [RemoteInt] or `false` otherwise.
     */
    public fun isNotEqualTo(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareNE,
            { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) },
        ) { a, b ->
            if (a != b) 1 else 0
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated("Use isNotEqualTo instead", ReplaceWith("isNotEqualTo(other)"))
    public infix fun ne(other: RemoteInt): RemoteBoolean = isNotEqualTo(other)

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [other] is less than the value of this
     * [RemoteInt] or `false` otherwise.
     */
    public fun isLessThan(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareLT,
            { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_IFELSE) },
        ) { a, b ->
            if (a < b) 1 else 0
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated("Use isLessThan instead", ReplaceWith("isLessThan(other)"))
    public infix fun lt(other: RemoteInt): RemoteBoolean = isLessThan(other)

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [other] is less than or equal to the
     * value of this [RemoteInt] or `false` otherwise.
     */
    public fun isLessThanOrEqual(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareLE,
            { a, b -> longArrayOf(1, 0, *a, *b, OP_SUB, OP_IFELSE) },
        ) { a, b ->
            if (a <= b) 1 else 0
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated("Use isLessThanOrEqual instead", ReplaceWith("isLessThanOrEqual(other)"))
    public infix fun le(other: RemoteInt): RemoteBoolean = isLessThanOrEqual(other)

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [other] is greater than the value of
     * this [RemoteInt] or `false` otherwise.
     */
    public fun isGreaterThan(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareGT,
            { a, b -> longArrayOf(0, 1, *a, *b, OP_SUB, OP_IFELSE) },
        ) { a, b ->
            if (a > b) 1 else 0
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated("Use isGreaterThan instead", ReplaceWith("isGreaterThan(other)"))
    public infix fun gt(other: RemoteInt): RemoteBoolean = isGreaterThan(other)

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [other] is greater than or equal to the
     * value of this [RemoteInt] or `false` otherwise.
     */
    public fun isGreaterThanOrEqual(other: RemoteInt): RemoteBoolean =
        comparisonOp(
            this,
            other,
            OperationKey.CompareGE,
            { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_IFELSE) },
        ) { a, b ->
            if (a >= b) 1 else 0
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated("Use isGreaterThanOrEqual instead", ReplaceWith("isGreaterThanOrEqual(other)"))
    public infix fun ge(other: RemoteInt): RemoteBoolean = isGreaterThanOrEqual(other)

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted left by the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.shl] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun shl(other: RemoteInt): RemoteInt =
        binaryOp(this, other, OperationKey.Shl, OP_SHL) { a, b -> a shl b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted right by the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.shr] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun shr(other: RemoteInt): RemoteInt =
        binaryOp(this, other, OperationKey.Shr, OP_SHR) { a, b -> a shr b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic or with the value
     * of [other].
     *
     * This is designed to align with the standard Kotlin [Int.or] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun or(other: RemoteInt): RemoteInt =
        binaryOp(this, other, OperationKey.Or, OP_OR) { a, b -> a or b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic and with the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.and] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun and(other: RemoteInt): RemoteInt =
        binaryOp(this, other, OperationKey.And, OP_AND) { a, b -> a and b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic xor with the
     * value of [other].
     *
     * This is designed to align with the standard Kotlin [Int.xor] infix function.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun xor(other: RemoteInt): RemoteInt =
        binaryOp(this, other, OperationKey.Xor, OP_XOR) { a, b -> a xor b }
}

/**
 * Constructs a longArray that either inlines or references the contents of [remoteInts] followed by
 * [extras]. Inlining is preferred as long as the resulting array length is less than
 * [MAX_SAFE_LONG_ARRAY].
 */
internal fun combineToLongArray(
    creationState: RemoteComposeCreationState,
    remoteInts: Array<RemoteInt>,
    vararg extras: Long,
): LongArray {
    var totalSizeInline = extras.size
    var totalSizeReference = extras.size + remoteInts.size
    var arrays =
        Array<LongArray>(remoteInts.size) { i ->
            val remoteInt = remoteInts[i]
            // If remoteInt has already been written to the document then use a reference
            // rather than inlining the expression. This results in smaller documents.
            if (remoteInt.hasBeenWrittenToDoc(creationState)) {
                totalSizeInline += 1
                longArrayOf(remoteInt.getLongIdForCreationState(creationState))
            } else {
                var array = remoteInt.arrayForCreationState(creationState)
                totalSizeInline += array.size
                array
            }
        }

    val combinedArray: LongArray
    var idx = 0

    if (totalSizeInline > MAX_SAFE_LONG_ARRAY) {
        // Add references for the RemoteInt values.
        combinedArray = LongArray(totalSizeReference)
        for (i in 0 until remoteInts.size) {
            combinedArray[i] = remoteInts[i].getLongIdForCreationState(creationState)
        }
        idx = remoteInts.size
    } else {
        // Inline the RemoteInt arrays.
        combinedArray = LongArray(totalSizeInline)
        for (array in arrays) {
            System.arraycopy(array, 0, combinedArray, idx, array.size)
            idx += array.size
        }
    }

    for (extra in extras) {
        combinedArray[idx++] = extra
    }

    return combinedArray
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun LongArray.isLiteral(): Boolean = size == 1 && RemoteInt.isLiteral(get(0))

/**
 * Boilerplate for implementing a binary operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [LongArray] if both sources aren\'t a const
 *   int.
 * @param directEval When the source is a const int, this lambda will be called to evaluate the
 *   result directly.
 * @param peepHoleEval This allows the caller the option to apply a peephole optimization to a
 *   previous operation. E.g. (x * 3) * 4 could be written as x * 12. If no optimization is possible
 *   peepHoleEval should return null.
 */
private fun binaryOp(
    a: RemoteInt,
    b: Int,
    op: OperationKey,
    opCode: Long,
    directEval: (Int, Int) -> Int,
    peepHoleEval: (LongArray, Long) -> LongArray?,
): RemoteInt {
    val aConst = a.constantValueOrNull
    if (aConst != null) {
        return RemoteInt(directEval(aConst, b))
    }
    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(op, a, b),
    ) { creationState ->
        val aArray = a.arrayForCreationState(creationState)
        val last = aArray.last()
        if (aArray.size > 2 && last >= 0x100000000L && aArray[aArray.size - 2] < 0x100000000L) {
            // If the last two elements of the array are a regular number and an operation, run
            // peepHoleEval with combineToLongArray if that returned null.
            peepHoleEval(aArray, last)
                ?: combineToLongArray(creationState, arrayOf(a), b.toLong(), opCode)
        } else {
            combineToLongArray(creationState, arrayOf(a), b.toLong(), opCode)
        }
    }
}

/**
 * Boilerplate for implementing a binary operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [LongArray] if both sources aren\'t a const
 *   int.
 * @param directEval When the source is a const int, this lambda will be called to evaluate the
 *   result directly.
 */
private fun binaryOp(
    a: RemoteInt,
    b: Int,
    op: OperationKey,
    opCode: Long,
    directEval: (Int, Int) -> Int,
): RemoteInt {
    val aConst = a.constantValueOrNull
    if (aConst != null) {
        return RemoteInt(directEval(aConst, b))
    }
    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(op, a, b),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(a), b.toLong(), opCode)
    }
}

/**
 * Boilerplate for implementing a binary operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [LongArray] if both sources aren\'t a const
 *   int.
 * @param directEval When the source is a const int, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun binaryOp(
    a: RemoteInt,
    b: RemoteInt,
    op: OperationKey,
    opCode: Long,
    directEval: (Int, Int) -> Int,
): RemoteInt {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteInt(directEval(aConst, bConst))
    }

    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(op, a, b),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(a, b), opCode)
    }
}

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
    op: OperationKey,
    expressionGenerator: (LongArray, LongArray) -> LongArray,
    directEval: (Int, Int) -> Int,
): RemoteBoolean {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteBoolean(RemoteInt(directEval(aConst, bConst)))
    }

    return RemoteBoolean(
        RemoteIntExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(op, a, b),
        ) { creationState ->
            val aArray = a.arrayForCreationState(creationState)
            val bArray = b.arrayForCreationState(creationState)
            // A comparisonOp adds five op codes
            val combinedSize = aArray.size + bArray.size + 5
            if (combinedSize > MAX_SAFE_LONG_ARRAY) { // Check if new array would exceed limit
                expressionGenerator(
                    longArrayOf(a.getLongIdForCreationState(creationState)),
                    longArrayOf(b.getLongIdForCreationState(creationState)),
                )
            } else {
                expressionGenerator(aArray, bArray)
            }
        }
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
    binaryOp(v, sign, OperationKey.CopySign, OP_COPY_SIGN) { a, b ->
        Math.copySign(a.toDouble(), b.toDouble()).toInt()
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
    binaryOp(a, b, OperationKey.Min, OP_MIN) { a, b -> min(a, b) }

/**
 * Returns a [RemoteInt] that evaluates to the maximum of [a] and [b].
 *
 * @param a The first [RemoteInt].
 * @param b The second [RemoteInt].
 * @return A [RemoteInt] representing the maximum of `a` and `b`.\
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RemoteInt, b: RemoteInt): RemoteInt =
    binaryOp(a, b, OperationKey.Max, OP_MAX) { a, b -> max(a, b) }

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

    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.Clamp, min, max, value),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(min, max, value), OP_CLAMP)
    }
}

/** A mutable implementation of [RemoteInt]. */
public class MutableRemoteInt
internal constructor(
    constantValueOrNull: Int? = null,
    cacheKey: RemoteStateCacheKey,
    internal val idProvider: (creationState: RemoteComposeCreationState) -> Long,
) :
    RemoteInt(
        constantValueOrNull = constantValueOrNull,
        cacheKey = cacheKey,
        arrayProvider = { creationState ->
            val id =
                creationState.getOrPutVariableId(cacheKey) {
                    Utils.idFromLong(idProvider(creationState)).toInt()
                }
            longArrayOf(id.toLong() + 0x100000000L)
        },
    ),
    MutableRemoteState<Int> {

    /**
     * Constructor for [MutableRemoteInt] that allows specifying an initial ID.
     *
     * @param id An explicit ID for this mutable integer.
     */
    internal constructor(
        id: Long
    ) : this(
        constantValueOrNull = null,
        cacheKey = RemoteStateIdKey(id.toInt()),
        idProvider = { _ -> id },
    )

    public companion object {
        /**
         * Creates a new mutable state (allocates an ID).
         *
         * @param initialValue The initial value for the state.
         * @return A new [MutableRemoteInt] instance.
         */
        public operator fun invoke(initialValue: Int): MutableRemoteInt {
            return MutableRemoteInt(
                constantValueOrNull = null,
                cacheKey = RemoteStateInstanceKey(),
            ) { creationState ->
                creationState.document.addInteger(initialValue)
            }
        }

        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteInt] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Long): MutableRemoteInt =
            MutableRemoteInt(
                constantValueOrNull = null,
                cacheKey = RemoteStateIdKey(id.toInt()),
                idProvider = { creationState -> id },
            )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        Utils.idFromLong(idProvider(creationState)).toInt()
}

private fun calcHashID(array: LongArray): Int {
    var sum = 0L
    for (i in array) {
        sum = sum * 31L + i
    }
    return sum.hashCode()
}

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
        return if (constA < constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfLT, a, b, ifTrue, ifFalse),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifFalse, ifTrue, b, a), OP_SUB, OP_IFELSE)
    }
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
        return if (constA <= constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfLE, a, b, ifTrue, ifFalse),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifTrue, ifFalse, a, b), OP_SUB, OP_IFELSE)
    }
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
        return if (constA > constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfGT, a, b, ifTrue, ifFalse),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifFalse, ifTrue, a, b), OP_SUB, OP_IFELSE)
    }
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
        return if (constA >= constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteIntExpression(
        constantValueOrNull = null,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.SelectIfGE, a, b, ifTrue, ifFalse),
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifTrue, ifFalse, b, a), OP_SUB, OP_IFELSE)
    }
}

/**
 * An implementation of [RemoteInt] that represents an integer expression.
 *
 * @param arrayProvider A lambda that provides the [LongArray] representing the expression.
 * @property hasConstantValue Indicates if this expression will always yield the same value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteIntExpression
internal constructor(
    public override val constantValueOrNull: Int?,
    cacheKey: RemoteStateCacheKey,
    arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
) : RemoteInt(constantValueOrNull, cacheKey, arrayProvider) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val array = arrayForCreationState(creationState)

        // in case we have a single element array, check if the element is an id or not;
        // if it is an existing id, just return this one, no need to create a new one...
        if (array.size == 1 && array[0] > 0x100000000L) {
            return Utils.idFromLong(array[0]).toInt()
        }
        val hash = calcHashID(array)
        val ie = creationState.intExpressionCache[hash]
        if (ie != null) {
            if (
                ie != this &&
                    ie is RemoteIntExpression &&
                    ie.arrayForCreationState(creationState) contentEquals array
            ) {
                return ie.getIdForCreationState(creationState)
            }

            creationState.intExpressionCache.put(hash, this)
            return Utils.idFromLong(creationState.document.integerExpression(*array)).toInt()
        } else {
            creationState.intExpressionCache.put(hash, this)
            return Utils.idFromLong(creationState.document.integerExpression(*array)).toInt()
        }
    }
}

/**
 * Factory composable for mutable remote integer state.
 *
 * @param initialValue The initial [Int] value.
 * @return A [MutableRemoteInt] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
public fun rememberMutableRemoteInt(initialValue: Int): MutableRemoteInt {
    return remember { MutableRemoteInt(initialValue) }
}

/**
 * A Composable function to remember and provide a [RemoteInt] expression.
 *
 * @param value A lambda that provides the [RemoteInt] expression.
 * @return A [RemoteIntExpression] representing the remembered remote integer.
 */

/**
 * Remembers a named remote integer expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @param defaultValue The initial [Int] value.
 * @return A [RemoteInt] instance representing the named expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteInt(
    name: String,
    defaultValue: Int,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteInt {
    return rememberNamedState(name, domain) {
        RemoteInt.createNamedRemoteInt(name, defaultValue, domain)
    }
}

/** Extension property to convert an [Int] to a [RemoteInt]. */
public val Int.ri: RemoteInt
    get() {
        return RemoteInt(this)
    }
