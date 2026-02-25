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
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
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
 * An inline value class representing a reference to a remote integer.
 *
 * @param v The integer value of the reference.
 */
@Stable
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public value class RemoteIntReference(private val v: Int) {
    public fun toInt(): Int {
        return v
    }
}

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
    internal val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
) : BaseRemoteState<Int>() {

    /**
     * Retrieves the [LongArray] representing this [RemoteInt]\'s expression using the provided
     * [creationState]. It utilizes a cache within the [creationState] to avoid redundant
     * computations, improving performance.
     *
     * @param creationState The current [RemoteComposeCreationState].
     * @return The [LongArray] representing this remote integer\'s expression.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun arrayForCreationState(creationState: RemoteComposeCreationState): LongArray {
        val cachedArray = creationState.longArrayCache.get(this)
        if (cachedArray != null) {
            return cachedArray
        }
        val array = arrayProvider(creationState)
        creationState.longArrayCache.put(this, array)
        return array
    }

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
        return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
            floatArrayOf(getFloatIdForCreationState(creationState))
        }
    }

    /**
     * Converts this RemoteInt to a RemoteString. The conversion includes formatting options such as
     * the number of digits to display and padding flags.
     *
     * @param before The number of digits to display.
     * @param flags The flags that control how the number is formatted. See [TextFromFloat].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteString(before: Int, flags: Int = TextFromFloat.PAD_PRE_SPACE): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(floatToString(it.toFloat(), before, 0, flags))
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    return creationState.createTextFromFloat(
                        RemoteComposeCreationState.TextFromFloatParams(
                            getIdForCreationState(creationState),
                            before,
                            0,
                            flags,
                        )
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? {
                    val preFlags = flags and 12
                    if (before == 1 || preFlags != TextFromFloat.PAD_PRE_SPACE) {
                        return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                    } else {
                        return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
                    }
                }
            },
        )
    }

    /**
     * Returns a [RemoteInt] that is a reference of this RemoteInt.
     *
     * This is temporarily useful because the floatArray has a maximum size.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun createReference(): RemoteInt {
        return RemoteIntExpression(
            constantValueOrNull,
            { creationState -> longArrayOf(getLongIdForCreationState(creationState)) },
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
    private fun unaryOp(opCode: Long, directEval: (Int) -> Int): RemoteInt {
        constantValueOrNull?.let {
            return RemoteInt(directEval(it))
        }
        return RemoteIntExpression(constantValueOrNull = null) { creationState ->
            combineToLongArray(creationState, arrayOf(this), opCode)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: Int): RemoteInt {
        if (v == 0) {
            return this
        }
        return binaryOp(this, v, OP_ADD, { a, b -> a + b }) { array, opId ->
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
        return binaryOp(this, v, OP_SUB, { a, b -> a - b }) { array, opId ->
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
        return binaryOp(this, v, OP_MUL, { a, b -> a * b }) { array, opId ->
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
        return binaryOp(this, v, OP_DIV, { a, b -> a / b }) { array, opId ->
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
    public operator fun rem(v: Int): RemoteInt = binaryOp(this, v, OP_MOD) { a, b -> a % b }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: RemoteInt): RemoteInt {
        v.constantValueOrNull?.let {
            return plus(it)
        }
        constantValueOrNull?.let {
            return v.plus(it)
        }
        return binaryOp(this, v, OP_ADD) { a, b -> a + b }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun minus(v: RemoteInt): RemoteInt {
        v.constantValueOrNull?.let {
            return minus(it)
        }
        constantValueOrNull?.let {
            return (-v).plus(it)
        }
        return binaryOp(this, v, OP_SUB) { a, b -> a - b }
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
        return binaryOp(this, v, OP_MUL) { a, b -> a * b }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun div(v: RemoteInt): RemoteInt {
        if (constantValueOrNull != null && constantValueOrNull == 0) {
            return RemoteInt(0)
        }
        v.constantValueOrNull?.let {
            return div(it)
        }
        return binaryOp(this, v, OP_DIV) { a, b -> a / b }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun rem(v: RemoteInt): RemoteInt = binaryOp(this, v, OP_MOD) { a, b -> a % b }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun unaryMinus(): RemoteInt = unaryOp(OP_NEG) { v -> -v }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun inv(): RemoteInt = unaryOp(OP_NOT) { v -> v.inv() }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val absoluteValue: RemoteInt
        get() = unaryOp(OP_ABS) { v -> abs(v) }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        public operator fun invoke(value: Int): RemoteInt {
            return RemoteIntExpression(value, { longArrayOf(value.toLong()) })
        }

        /**
         * Creates a [RemoteInt] referencing a remote ID.
         *
         * @param v The remote ID.
         * @return A [RemoteInt] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        internal fun createForId(v: Long): RemoteInt {
            if (isConstant(v)) {
                return RemoteIntExpression(v.toInt(), { longArrayOf(v) })
            }
            return RemoteIntExpression(
                constantValueOrNull = null,
                { creationState -> longArrayOf(v) },
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
         * @param v The constant [Long] value.
         * @return A [RemoteIntExpression] representing the constant integer.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated("Use createForId")
        public operator fun invoke(v: Long): RemoteInt {
            return createForId(v)
        }

        /**
         * Creates a named [RemoteInt] with an initial value. Named remote ints can be set via
         * AndroidRemoteContext.setNamedInt.
         *
         * @param name The unique name for this remote long.
         * @param defaultValue The initial [Int] value for the named remote int.
         * @return A [RemoteInt] representing the named int.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun createNamedRemoteInt(
            name: String,
            defaultValue: Int,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteInt {
            return RemoteIntExpression(constantValueOrNull = null) { creationState ->
                longArrayOf(creationState.document.addNamedInt("$domain:$name", defaultValue))
            }
        }
    }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is equal to the value of this
     * [RemoteInt] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun eq(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) }) {
            a,
            b ->
            if (a == b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is not equal to the value of this
     * [RemoteInt] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun ne(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) }) {
            a,
            b ->
            if (a != b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than the value of this
     * [RemoteInt] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun lt(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a < b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than or equal to the value
     * of this [RemoteInt] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun le(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(1, 0, *a, *b, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a <= b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than the value of this
     * [RemoteInt] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun gt(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(0, 1, *a, *b, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a > b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than or equal to the
     * value of this [RemoteInt] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun ge(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a >= b) 1 else 0
        }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted left by the
     * value of [v].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun shl(v: RemoteInt): RemoteInt = binaryOp(this, v, OP_SHL) { a, b -> a shl b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted right by the
     * value of [v].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun shr(v: RemoteInt): RemoteInt = binaryOp(this, v, OP_SHR) { a, b -> a shr b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic or with the value
     * of [v].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun or(v: RemoteInt): RemoteInt = binaryOp(this, v, OP_OR) { a, b -> a or b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic and with the
     * value of [v].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun and(v: RemoteInt): RemoteInt = binaryOp(this, v, OP_AND) { a, b -> a and b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic xor with the
     * value of [v].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun xor(v: RemoteInt): RemoteInt = binaryOp(this, v, OP_XOR) { a, b -> a xor b }
}

/**
 * Constructs a longArray that either inlines or references the contents of [remoteInts] followed by
 * [extras]. Inlining is preferred as long as the resulting array length is less than
 * [MAX_SAFE_LONG_ARRAY].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun combineToLongArray(
    creationState: RemoteComposeCreationState,
    remoteInts: Array<RemoteInt>,
    vararg extras: Long,
): LongArray {
    var totalSizeInline = extras.size
    var totalSizeReference = extras.size + remoteInts.size
    var arrays =
        Array<LongArray>(remoteInts.size) { i ->
            var array = remoteInts[i].arrayForCreationState(creationState)
            totalSizeInline += array.size
            array
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
    opCode: Long,
    directEval: (Int, Int) -> Int,
    peepHoleEval: (LongArray, Long) -> LongArray?,
): RemoteInt {
    val aConst = a.constantValueOrNull
    if (aConst != null) {
        return RemoteInt(directEval(aConst, b))
    }
    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
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
private fun binaryOp(a: RemoteInt, b: Int, opCode: Long, directEval: (Int, Int) -> Int): RemoteInt {
    val aConst = a.constantValueOrNull
    if (aConst != null) {
        return RemoteInt(directEval(aConst, b))
    }
    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
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
private fun binaryOp(
    a: RemoteInt,
    b: RemoteInt,
    opCode: Long,
    directEval: (Int, Int) -> Int,
): RemoteInt {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteInt(directEval(aConst, bConst))
    }
    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun comparisonOp(
    a: RemoteInt,
    b: RemoteInt,
    expressionGenerator: (LongArray, LongArray) -> LongArray,
    directEval: (Int, Int) -> Int,
): RemoteBoolean {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteBoolean(RemoteInt(directEval(aConst, bConst)))
    }

    return RemoteBoolean(
        RemoteIntExpression(constantValueOrNull = null) { creationState ->
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
    binaryOp(v, sign, OP_COPY_SIGN) { a, b -> Math.copySign(a.toDouble(), b.toDouble()).toInt() }

/**
 * Returns a [RemoteInt] that evaluates to the minimum of [a] and [b].
 *
 * @param a The first [RemoteInt].
 * @param b The second [RemoteInt].
 * @return A [RemoteInt] representing the minimum of `a` and `b`.\
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun min(a: RemoteInt, b: RemoteInt): RemoteInt = binaryOp(a, b, OP_MIN) { a, b -> min(a, b) }

/**
 * Returns a [RemoteInt] that evaluates to the maximum of [a] and [b].
 *
 * @param a The first [RemoteInt].
 * @param b The second [RemoteInt].
 * @return A [RemoteInt] representing the maximum of `a` and `b`.\
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun max(a: RemoteInt, b: RemoteInt): RemoteInt = binaryOp(a, b, OP_MAX) { a, b -> max(a, b) }

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

    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
        combineToLongArray(creationState, arrayOf(min, max, value), OP_CLAMP)
    }
}

/** A mutable implementation of [RemoteInt]. */
public class MutableRemoteInt
internal constructor(
    constantValueOrNull: Int? = null,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val idProvider: (creationState: RemoteComposeCreationState) -> Long,
) :
    RemoteInt(
        constantValueOrNull = constantValueOrNull,
        arrayProvider = { creationState -> longArrayOf(idProvider(creationState)) },
    ),
    MutableRemoteState<Int> {

    public companion object {
        /**
         * Creates a new mutable state (allocates an ID).
         *
         * @param initialValue The initial value for the state.
         * @return A new [MutableRemoteInt] instance.
         */
        public fun createMutable(initialValue: Int): MutableRemoteInt {
            return MutableRemoteInt(constantValueOrNull = null) { creationState ->
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
            MutableRemoteInt(constantValueOrNull = null, idProvider = { creationState -> id })
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

    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
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

    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
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

    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
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

    return RemoteIntExpression(constantValueOrNull = null) { creationState ->
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
    constantValueOrNull: Int?,
    arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
) : RemoteInt(constantValueOrNull, arrayProvider) {

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
    return remember { MutableRemoteInt.createMutable(initialValue) }
}

/**
 * Factory composable for state.
 *
 * @param value A lambda that provides the initial [Int] value for this remote integer.
 * @return A [MutableRemoteInt] instance that will be remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
@Deprecated("Use rememberMutableRemoteInt", ReplaceWith("rememberMutableRemoteInt(value())"))
public fun rememberRemoteIntValue(value: () -> Int): MutableRemoteInt =
    rememberMutableRemoteInt(value())

/**
 * A Composable function to remember and provide a **named** mutable remote integer value.
 *
 * @param name The unique name for this remote integer.
 * @param domain The domain of the named integer (defaults to [RemoteState.Domain.User]). This helps
 *   organize named values in the remote document.
 * @param value A lambda that provides the initial [Int] value for this remote integer.
 * @return A [RemoteInt] instance that will be remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
@Deprecated(
    "Use rememberNamedRemoteInt",
    ReplaceWith("rememberNamedRemoteInt(name, domain) { value().ri }"),
)
public fun rememberRemoteIntValue(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> Int,
): RemoteInt {
    return rememberNamedState(name, domain) {
        MutableRemoteInt(
            constantValueOrNull = null,
            idProvider = { creationState ->
                val initial = value()
                creationState.document.addNamedInt("$domain:$name", initial)
            },
        )
    }
}

/**
 * A Composable function to remember and provide a [RemoteInt] expression.
 *
 * @param content A lambda that provides the [RemoteInt] expression.
 * @return A [RemoteIntExpression] representing the remembered remote integer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
@Deprecated("Use rememberMutableRemoteInt", ReplaceWith("rememberMutableRemoteInt(value())"))
public fun rememberRemoteInt(content: () -> RemoteInt): RemoteInt {
    return remember {
        val remoteInt = content()
        RemoteIntExpression(remoteInt.constantValueOrNull, remoteInt.arrayProvider)
    }
}

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
