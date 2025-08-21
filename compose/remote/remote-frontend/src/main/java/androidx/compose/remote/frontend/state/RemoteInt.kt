/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.actions.Action
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.player.view.state.RemoteDomains
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
@JvmInline
value class RemoteIntReference(private val v: Int) {
    fun toInt(): Int {
        return v
    }
}

/**
 * Abstract base class for all remote integer representations in Compose Remote, this extends.
 * [RemoteState<Int>].
 *
 * @property hasConstantValue Whether this [RemoteInt] will always evaluate to the same [value].
 *   This is a conservative check that may report false negatives for some expressions that
 *   reference other expressions since the tracking involved is expensive.
 * @property arrayProvider A lambda that provides the [LongArray] representing the expression for
 *   this [RemoteInt], given a [RemoteComposeCreationState].
 */
abstract class RemoteInt
internal constructor(
    override val hasConstantValue: Boolean,
    internal val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
) : RemoteState<Int> {

    // @Deprecated("Use getLongIdForCreationState instead")
    // TODO: re-enable asap
    val id: Long
        get() {
            // FallbackCreationState.state.platform.log(
            //     Platform.LogCategory.TODO,
            //     "Use RemoteInt.getLongIdForCreationState directly"
            // )
            return getLongIdForCreationState(FallbackCreationState.state)
        }

    /**
     * Retrieves the [LongArray] representing this [RemoteInt]'s expression using the provided
     * [creationState]. It utilizes a cache within the [creationState] to avoid redundant
     * computations, improving performance.
     *
     * @param creationState The current [RemoteComposeCreationState].
     * @return The [LongArray] representing this remote integer's expression.
     */
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
     * Retrieves the integer portion of the remote ID.
     *
     * @return The integer ID.
     */
    fun getIntId(): Int {
        return Utils.idFromLong(id).toInt()
    }

    /**
     * Converts this [RemoteInt] to a [RemoteFloat]. If the [RemoteInt] is a literal, it's directly
     * converted to a float. Otherwise, a [RemoteFloatExpression] is created that references the
     * remote float ID of this integer.
     *
     * @return A [RemoteFloatExpression] representing this integer as a float.
     */
    fun toRemoteFloat(): RemoteFloatExpression {
        return RemoteFloatExpression(hasConstantValue) { creationState ->
            val a = arrayForCreationState(creationState)
            if (a.isLiteral()) {
                floatArrayOf(a[0].toFloat())
            } else {
                floatArrayOf(getFloatIdForCreationState(creationState))
            }
        }
    }

    /**
     * Converts this RemoteInt to a RemoteString. The conversion includes formatting options such as
     * the number of digits to display and padding flags.
     *
     * @param before The number of digits to display.
     * @param flags The flags that control how the number is formatted. See [TextFromFloat].
     */
    fun toRemoteString(before: Int, flags: Int = TextFromFloat.PAD_PRE_SPACE): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""), // TODO compute the string?,
            hasConstantValue,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    val a = arrayForCreationState(creationState)
                    if (a.isLiteral()) {
                        return creationState.document.textCreateId(
                            floatToString(a[0].toFloat(), before, 0, flags)
                        )
                    }

                    return creationState.document.createTextFromFloat(
                        getFloatIdForCreationState(creationState),
                        before,
                        0,
                        flags,
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? {
                    val a = arrayForCreationState(creationState)
                    if (a.isLiteral()) {
                        return floatToString(a[0].toFloat(), before, 0, flags).toCodePointSet()
                    }

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
    // TODO: Remove the need for this.
    fun createReference(): RemoteInt {
        return RemoteIntExpression(
            hasConstantValue,
            { creationState -> longArrayOf(getLongIdForCreationState(creationState)) },
        )
    }

    /**
     * Boilerplate for implementing an unary operation.
     *
     * @param opCode The opcode to insert in the generated [LongArray] if the source isn't a const
     *   int.
     * @param directEval When the source is a const int, this lambda will be called to evaluate the
     *   result directly.
     */
    private fun unaryOp(opCode: Long, directEval: (Int) -> Int): RemoteInt {
        return RemoteIntExpression(hasConstantValue) { creationState ->
            val a = arrayForCreationState(creationState)
            if (a.isLiteral()) {
                longArrayOf(directEval(a[0].toInt()).toLong())
            } else {
                combineToLongArray(creationState, arrayOf(this), opCode)
            }
        }
    }

    operator fun plus(v: Int) = binaryOp(this, v, OP_ADD) { a, b -> a + b }

    operator fun minus(v: Int) = binaryOp(this, v, OP_SUB) { a, b -> a - b }

    operator fun times(v: Int) = binaryOp(this, v, OP_MUL) { a, b -> a * b }

    operator fun div(v: Int) = binaryOp(this, v, OP_DIV) { a, b -> a / b }

    operator fun rem(v: Int) = binaryOp(this, v, OP_MOD) { a, b -> a % b }

    operator fun plus(v: RemoteInt) = binaryOp(this, v, OP_ADD) { a, b -> a + b }

    operator fun minus(v: RemoteInt) = binaryOp(this, v, OP_SUB) { a, b -> a - b }

    operator fun times(v: RemoteInt) = binaryOp(this, v, OP_MUL) { a, b -> a * b }

    operator fun div(v: RemoteInt) = binaryOp(this, v, OP_DIV) { a, b -> a / b }

    operator fun rem(v: RemoteInt) = binaryOp(this, v, OP_MOD) { a, b -> a % b }

    operator fun unaryMinus() = unaryOp(OP_NEG) { v -> -v }

    fun inv() = unaryOp(OP_NOT) { v -> v.inv() }

    val absoluteValue: RemoteInt
        get() = unaryOp(OP_ABS) { v -> kotlin.math.abs(v) }

    companion object {
        /**
         * Creates a [RemoteInt] instance from a constant [Int] value.
         *
         * @param v The constant [Int] value.
         * @return A [RemoteIntExpression] representing the constant integer.
         */
        operator fun invoke(v: Int): RemoteInt {
            return RemoteIntExpression(true, { creationState -> longArrayOf(v.toLong()) })
        }

        /**
         * Checks if a given [Long] value is considered a literal (i.e., not an ID or an OP code).
         *
         * @param v The [Long] value to check.
         * @return `true` if the value is a literal, `false` otherwise.
         */
        fun isLiteral(v: Long): Boolean = v < 0x100000000L

        /**
         * Checks if a given [Long] value representing a remote integer is considered constant. This
         * performs a conservative check, assuming that variables are not constant unless explicitly
         * determined otherwise.
         *
         * @param v The [Long] value representing a remote integer (could be a literal or an ID).
         * @return `true` if the value is constant, `false` otherwise.
         */
        fun isConstant(v: Long): Boolean {
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
        operator fun invoke(v: Long): RemoteInt {
            return RemoteIntExpression(isConstant(v), { creationState -> longArrayOf(v) })
        }

        /**
         * Creates a named [RemoteInt] with an initial value. Named remote ints can be set via
         * AndroidRemoteContext.setNamedInt.
         *
         * @param name The unique name for this remote long.
         * @param initialValue The initial [Int] value for the named remote int.
         * @return A [RemoteInt] representing the named int.
         */
        @JvmStatic
        fun createNamedRemoteInt(name: String, initialValue: Int): RemoteInt {
            return RemoteIntExpression(false) { creationState ->
                // TODO: check what happens if the initial value for this is the same as a
                //  subsequent non-named variable.
                longArrayOf(creationState.document.addNamedInt(name, initialValue))
            }
        }
    }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is equal to the value of this
     * [RemoteInt] or `false` otherwise.
     */
    infix fun eq(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) }) {
            a,
            b ->
            if (a == b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is not equal to the value of this
     * [RemoteInt] or `false` otherwise.
     */
    infix fun ne(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_ABS, OP_IFELSE) }) {
            a,
            b ->
            if (a != b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than the value of this
     * [RemoteInt] or `false` otherwise.
     */
    infix fun lt(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(0, 1, *b, *a, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a < b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than or equal to the value
     * of this [RemoteInt] or `false` otherwise.
     */
    infix fun le(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(1, 0, *a, *b, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a <= b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than the value of this
     * [RemoteInt] or `false` otherwise.
     */
    infix fun gt(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(0, 1, *a, *b, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a > b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than or equal to the
     * value of this [RemoteInt] or `false` otherwise.
     */
    infix fun ge(b: RemoteInt): RemoteBoolean =
        comparisonOp(this, b, { a, b -> longArrayOf(1, 0, *b, *a, OP_SUB, OP_IFELSE) }) { a, b ->
            if (a >= b) 1 else 0
        }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted left by the
     * value of [v].
     */
    infix fun shl(v: RemoteInt) = binaryOp(this, v, OP_SHL) { a, b -> a shl b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] shifted right by the
     * value of [v].
     */
    infix fun shr(v: RemoteInt) = binaryOp(this, v, OP_SHR) { a, b -> a shr b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic or with the value
     * of [v].
     */
    infix fun or(v: RemoteInt) = binaryOp(this, v, OP_OR) { a, b -> a or b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic and with the
     * value of [v].
     */
    infix fun and(v: RemoteInt) = binaryOp(this, v, OP_AND) { a, b -> a and b }

    /**
     * Returns a [RemoteInt] that evaluates to the value of this [RemoteInt] logic xor with the
     * value of [v].
     */
    infix fun xor(v: RemoteInt) = binaryOp(this, v, OP_XOR) { a, b -> a xor b }
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
            for (v in array) {
                combinedArray[idx++] = v
            }
        }
    }

    for (extra in extras) {
        combinedArray[idx++] = extra
    }

    return combinedArray
}

// TODO: Restrict to LibraryGroup.
fun LongArray.isLiteral() = size == 1 && RemoteInt.isLiteral(get(0))

/**
 * Boilerplate for implementing a binary operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [LongArray] if both sources aren't a const
 *   int.
 * @param directEval When the source is a const int, this lambda will be called to evaluate the
 *   result directly.
 */
private fun binaryOp(a: RemoteInt, b: Int, opCode: Long, directEval: (Int, Int) -> Int): RemoteInt {
    return RemoteIntExpression(a.hasConstantValue) { creationState ->
        val aArray = a.arrayForCreationState(creationState)
        if (aArray.isLiteral()) {
            longArrayOf(directEval(aArray[0].toInt(), b).toLong())
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
 * @param opCode The opcode to insert in the generated [LongArray] if both sources aren't a const
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
    return RemoteIntExpression(a.hasConstantValue && b.hasConstantValue) { creationState ->
        val aArray = a.arrayForCreationState(creationState)
        val bArray = b.arrayForCreationState(creationState)
        if (aArray.isLiteral() && bArray.isLiteral()) {
            // A and b are both constants so we can evaluate directly.
            longArrayOf(directEval(aArray[0].toInt(), bArray[0].toInt()).toLong())
        } else {
            combineToLongArray(creationState, arrayOf(a, b), opCode)
        }
    }
}

/**
 * Boilerplate for implementing a binary comparison operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param expressionGenerator Generator for the comparison expression [LongArray] used when both
 *   sources aren't a const float.
 * @param directEval When the sources are const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun comparisonOp(
    a: RemoteInt,
    b: RemoteInt,
    expressionGenerator: (LongArray, LongArray) -> LongArray,
    directEval: (Long, Long) -> Long,
) =
    RemoteBoolean(
        RemoteIntExpression(a.hasConstantValue && b.hasConstantValue) { creationState ->
            val aArray = a.arrayForCreationState(creationState)
            val bArray = b.arrayForCreationState(creationState)
            if (aArray.isLiteral() && bArray.isLiteral()) {
                // A and b are both constants so we can evaluate directly.
                longArrayOf(directEval(aArray[0], bArray[0]))
            } else {
                // Either a or b isn't a constant so we need to evaluate dynamically.
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
        }
    )

/**
 * Returns a [RemoteInt] that evaluates to the value of [v] with the sign of [sign]. This is a
 * remote equivalent of `Math.copySign`.
 *
 * @param v The [RemoteInt] whose magnitude is used.
 * @param sign The [RemoteInt] whose sign is used.
 * @return A [RemoteInt] with the magnitude of `v` and the sign of `sign`.
 */
fun copySign(v: RemoteInt, sign: RemoteInt) =
    binaryOp(v, sign, OP_COPY_SIGN) { a, b -> Math.copySign(a.toDouble(), b.toDouble()).toInt() }

/**
 * Returns a [RemoteInt] that evaluates to the minimum of [a] and [b].
 *
 * @param a The first [RemoteInt].
 * @param b The second [RemoteInt].
 * @return A [RemoteInt] representing the minimum of `a` and `b`.
 */
fun min(a: RemoteInt, b: RemoteInt) = binaryOp(a, b, OP_MIN) { a, b -> min(a, b) }

/**
 * Returns a [RemoteInt] that evaluates to the maximum of [a] and [b].
 *
 * @param a The first [RemoteInt].
 * @param b The second [RemoteInt].
 * @return A [RemoteInt] representing the maximum of `a` and `b`.
 */
fun max(a: RemoteInt, b: RemoteInt) = binaryOp(a, b, OP_MAX) { a, b -> max(a, b) }

/**
 * Returns a [RemoteInt] that evaluates to [value] clamped between [min] and [max].
 *
 * @param min The lower bound [RemoteInt].
 * @param max The upper bound [RemoteInt].
 * @param value The [RemoteInt] to clamp.
 * @return A [RemoteInt] representing the clamped value.
 */
fun clamp(min: RemoteInt, max: RemoteInt, value: RemoteInt): RemoteInt {
    return RemoteIntExpression(
        min.hasConstantValue && max.hasConstantValue && value.hasConstantValue
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(min, max, value), OP_CLAMP)
    }
}

/**
 * A mutable implementation of [RemoteInt] that holds its value in a [MutableIntState].
 *
 * @property content The underlying [MutableIntState] that stores the actual integer value.
 * @property idProvider A lambda that provides the unique ID for this mutable integer within the
 *   [RemoteComposeCreationState]. This ID is used to identify the integer in the remote document.
 */
class MutableRemoteInt(
    private val content: MutableIntState,
    val idProvider: (creationState: RemoteComposeCreationState) -> Long,
) :
    RemoteInt(true, { creationState -> longArrayOf(idProvider(creationState)) }),
    MutableRemoteState<Int> {

    /**
     * Constructor for [MutableRemoteInt] that allows specifying an initial ID. If no ID is
     * provided, it defaults to the initial value's long representation.
     *
     * @param content The [MutableIntState] to hold the value.
     * @param id An optional explicit ID for this mutable integer. If `null`, a default is used.
     */
    constructor(
        content: MutableIntState,
        id: Long? = null,
    ) : this(content, { creationState -> id ?: content.value.toLong() })

    override var value: Int
        get() {
            return content.intValue
        }
        set(newValue) {
            content.intValue = newValue
        }

    override operator fun component1(): Int = value

    override operator fun component2(): (Int) -> Unit = { newValue -> content.intValue = newValue }

    override fun writeToDocument(creationState: RemoteComposeCreationState) =
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
fun selectIfLT(a: RemoteInt, b: RemoteInt, ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt =
    RemoteIntExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifFalse, ifTrue, b, a), OP_SUB, OP_IFELSE)
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
fun selectIfLE(a: RemoteInt, b: RemoteInt, ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt =
    RemoteIntExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifTrue, ifFalse, a, b), OP_SUB, OP_IFELSE)
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
fun selectIfGT(a: RemoteInt, b: RemoteInt, ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt =
    RemoteIntExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifFalse, ifTrue, a, b), OP_SUB, OP_IFELSE)
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
fun selectIfGE(a: RemoteInt, b: RemoteInt, ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt =
    RemoteIntExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        combineToLongArray(creationState, arrayOf(ifTrue, ifFalse, b, a), OP_SUB, OP_IFELSE)
    }

/**
 * An implementation of [RemoteInt] that represents an integer expression.
 *
 * @param arrayProvider A lambda that provides the [LongArray] representing the expression.
 * @property hasConstantValue Indicates if this expression will always yield the same value.
 */
class RemoteIntExpression
internal constructor(
    hasConstantValue: Boolean,
    arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray,
) : RemoteInt(hasConstantValue, arrayProvider) {

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val array = arrayForCreationState(creationState)

        // in case we have a single element array, check if the element is an id or not;
        // if it is an existing id, just return this one, no need to create a new one...
        if (array.size == 1 && array[0] > 0x100000000L) {
            return Utils.idFromLong(array[0]).toInt()
        }
        val hash = calcHashID(array)
        val ie = creationState.intExpressionCache.get(hash)
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

    override val value: Int
        get() = TODO("Implement expression evaluation")
}

/**
 * A Composable function to remember and provide a mutable remote integer value.
 *
 * @param value A lambda that provides the initial [Int] value for this remote integer.
 * @return A [MutableRemoteInt] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
fun rememberRemoteIntValue(value: () -> Int): MutableRemoteInt {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val initial = value()
        // TODO either store with an id and reference, or use directly
        val id = state.document.addInteger(initial)
        MutableRemoteInt(mutableIntStateOf(initial), id)
    }
}

/**
 * A Composable function to remember and provide a **named** mutable remote integer value.
 *
 * @param name The unique name for this remote integer.
 * @param domain The domain of the named integer (defaults to [RemoteDomains.USER]). This helps
 *   organize named values in the remote document.
 * @param value A lambda that provides the initial [Int] value for this remote integer.
 * @return A [MutableRemoteInt] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
fun rememberRemoteIntValue(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    value: () -> Int,
): MutableRemoteInt {
    val state = LocalRemoteComposeCreationState.current
    return remember(name) {
        val initial = value()
        // TODO either store with an id and reference, or use directly
        val id = state.document.addInteger(initial)
        state.document.setStringName(id.toInt(), "$domain:$name")
        MutableRemoteInt(mutableIntStateOf(initial), id)
    }
}

/**
 * A Composable function to remember and provide a [RemoteInt] expression.
 *
 * @param content A lambda that provides the [RemoteInt] expression.
 * @return A [RemoteIntExpression] representing the remembered remote integer.
 */
@Composable
@RemoteComposable
fun rememberRemoteInt(content: () -> RemoteInt): RemoteInt {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val remoteInt = content()
        remoteInt.getIdForCreationState(state)
        RemoteIntExpression(remoteInt.hasConstantValue, remoteInt.arrayProvider)
    }
}

/**
 * A Composable function to remember and provide a **named** [RemoteInt] expression.
 *
 * @param name The unique name for this remote integer.
 * @param domain The domain of the named integer (defaults to [RemoteDomains.USER]).
 * @param content A lambda that provides the [RemoteInt] expression.
 * @return A [RemoteIntExpression] representing the named remote integer.
 */
@Composable
fun rememberRemoteInt(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    content: () -> RemoteInt,
): RemoteIntExpression {
    val state = LocalRemoteComposeCreationState.current
    val remoteInt = content()
    state.document.setStringName(remoteInt.getIdForCreationState(state), "$domain:$name")
    return remember {
        // Since this is named, its value can be change, so it's not const.
        RemoteIntExpression(false) { creationState ->
            longArrayOf(remoteInt.getLongIdForCreationState(creationState))
        }
    }
}

fun ValueChange(valueId: MutableRemoteInt, value: Int): Action {
    return ValueIntegerChange(valueId.value, value)
}

fun ValueChange(valueId: MutableRemoteInt, value: RemoteInt): Action {
    val id1 = Utils.idFromLong(valueId.id)
    val id2 = Utils.idFromLong(value.id)
    return ValueIntegerExpressionChange(id1, id2)
}
