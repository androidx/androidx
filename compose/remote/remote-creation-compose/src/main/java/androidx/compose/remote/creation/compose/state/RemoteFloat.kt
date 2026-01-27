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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.Utils.asNan
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ABS
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.IFELSE
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.core.operations.utilities.StringUtils
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteFloatContext
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.math.RoundingMode
import java.text.DecimalFormat

private const val MAX_SAFE_FLOAT_ARRAY = 30
private const val OP_ADD = AnimatedFloatExpression.OFFSET + 1
private const val OP_SUB = AnimatedFloatExpression.OFFSET + 2
private const val OP_MUL = AnimatedFloatExpression.OFFSET + 3
private const val OP_DIV = AnimatedFloatExpression.OFFSET + 4

/** An inline value class representing a reference to a remote float. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RemoteFloatReference(private val v: Float)

/** Extension property to convert an [Int] to a [RemoteFloat]. */
public val Int.rf: RemoteFloat
    get() {
        return RemoteFloatExpression(this.toFloat()) { _ -> floatArrayOf(this.toFloat()) }
    }

/** Extension property to convert a [Float] to a [RemoteFloat]. */
public val Float.rf: RemoteFloat
    get() {
        return RemoteFloat(this)
    }

/** Extension function to get either a Float ID or a Float literal from a [Number]. */
internal fun Number.getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float =
    when (this) {
        is Float -> this
        else -> toFloat()
    }

/**
 * Abstract base class for all remote float representations. It extends [Number] and implements
 * [RemoteState<Float>].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RemoteFloat : BaseRemoteState<Float>() {
    internal abstract val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray

    internal fun arrayForCreationState(creationState: RemoteComposeCreationState): FloatArray {
        val cachedArray = creationState.floatArrayCache.get(this)
        if (cachedArray != null) {
            return cachedArray
        }
        val array = arrayProvider(creationState)
        creationState.floatArrayCache.put(this, array)
        return array
    }

    override fun getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float {
        constantValueOrNull?.let {
            return it
        }
        val array = arrayForCreationState(creationState)
        if (array.size == 1) {
            return array[0]
        }
        return super.getFloatIdForCreationState(creationState)
    }

    /**
     * Returns a [RemoteInt] that evaluates to the result of this [RemoteFloat] converted to Int.
     */
    public fun toRemoteInt(): RemoteInt {
        constantValueOrNull?.let {
            return RemoteInt(it.toInt())
        }
        return RemoteIntExpression(constantValueOrNull = null) { creationState ->
            longArrayOf(getLongIdForCreationState(creationState))
        }
    }

    public fun toRemoteString(format: DecimalFormat): RemoteString {
        val decimalSeparator = format.decimalFormatSymbols.decimalSeparator
        val groupingSeparator = format.decimalFormatSymbols.groupingSeparator

        val grouping =
            if (format.groupingSize == 3) {
                val pattern = format.toPattern()

                if (pattern.matches(",[0#]{2},[0#]{3}".toRegex())) {
                    TextFromFloat.GROUPING_BY32
                } else {
                    TextFromFloat.GROUPING_BY3
                }
            } else if (format.groupingSize == 4) {
                TextFromFloat.GROUPING_BY4
            } else {
                TextFromFloat.GROUPING_NONE
            }

        val separator =
            if (groupingSeparator == ',' && decimalSeparator == '.') {
                TextFromFloat.SEPARATOR_COMMA_PERIOD
            } else if (groupingSeparator == '.' && decimalSeparator == ',') {
                TextFromFloat.SEPARATOR_PERIOD_COMMA
            } else if (groupingSeparator == ' ' && decimalSeparator == ',') {
                TextFromFloat.SEPARATOR_SPACE_COMMA
            } else if (groupingSeparator == '_' && decimalSeparator == '.') {
                TextFromFloat.SEPARATOR_UNDER_PERIOD
            } else {
                // default
                TextFromFloat.SEPARATOR_COMMA_PERIOD
            }

        var options = 0
        if (format.negativePrefix == "(") {
            options = options or TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES
        }

        if (format.roundingMode != RoundingMode.UNNECESSARY) {
            // Not clear we can represent rounding properly
            options = options or TextFromFloat.OPTIONS_ROUNDING
        }

        var flags = separator or grouping or options
        if (format.minimumFractionDigits > 1) {
            flags = flags or TextFromFloat.PAD_AFTER_ZERO
        } else {
            flags = flags or TextFromFloat.PAD_AFTER_NONE
        }

        if (format.minimumIntegerDigits > 1) {
            flags = flags or TextFromFloat.PAD_PRE_ZERO
        } else {
            flags = flags or TextFromFloat.PAD_PRE_NONE
        }

        return toRemoteString(
            before = format.maximumIntegerDigits.coerceAtMost(255),
            after = format.maximumFractionDigits.coerceAtMost(255),
            flags = flags,
        )
    }

    /**
     * Returns a [RemoteString] that converts the result of this [RemoteFloat] with specified
     * formatting.
     *
     * @param before The number of digits to show before the decimal point.
     * @param after The number of digits to show after the decimal point (defaults to 2).
     * @param flags Formatting flags for the string conversion (defaults to
     *   [TextFromFloat.PAD_AFTER_ZERO]).
     * @return A [RemoteString] representing the formatted float.
     */
    public fun toRemoteString(
        before: Int,
        after: Int = 2,
        flags: Int = TextFromFloat.PAD_AFTER_ZERO,
    ): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(floatToString(it, before, after, flags))
        }
        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    return creationState.createTextFromFloat(
                        RemoteComposeCreationState.TextFromFloatParams(
                            getIdForCreationState(creationState),
                            before,
                            after,
                            flags,
                        )
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? {
                    val preFlags = flags and 12
                    val afterFlags = flags and 3
                    if (after == 0) {
                        if (before == 1 || preFlags != TextFromFloat.PAD_PRE_SPACE) {
                            return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                        } else {
                            return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
                        }
                    }

                    // If flags is non-zero then we may pad with a space.
                    if (
                        (before == 1 && after == 1) ||
                            (preFlags != TextFromFloat.PAD_PRE_SPACE &&
                                afterFlags != TextFromFloat.PAD_AFTER_SPACE)
                    ) {
                        return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".")
                    } else {
                        return setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", " ")
                    }
                }
            },
        )
    }

    /**
     * Boilerplate for implementing an unary operation.
     *
     * @param opCode The opcode to insert in the generated [FloatArray] if the source isn\'t a const
     *   int.
     * @param directEval When the source is a const int, this lambda will be called to evaluate the
     *   result directly.
     */
    internal fun unaryOp(opCode: Float, directEval: (Float) -> Float): RemoteFloat {
        constantValueOrNull?.let {
            return RemoteFloat(directEval(it))
        }

        return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
            combineToFloatArray(creationState, arrayOf(this), opCode)
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to the negative of this [RemoteFloat]. */
    public operator fun unaryMinus(): RemoteFloat {
        constantValueOrNull?.let {
            return RemoteFloat(-it)
        }

        return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
            combineToFloatArray(creationState, arrayOf(this), -1f, AnimatedFloatExpression.MUL)
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [v]. */
    public operator fun rem(v: Float): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MOD) { a, b -> a % b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [v]. */
    public operator fun rem(v: RemoteFloat): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MOD) { a, b -> a % b }

    /** Returns a new [RemoteFloat] that evaluates to minimum of this [RemoteFloat] and [v]. */
    public fun min(v: Float): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MIN, { a, b -> kotlin.math.min(a, b) }) {
            array,
            opId ->
            null
        }

    /** Returns a new [RemoteFloat] that evaluates to minimum of this [RemoteFloat] and [v]. */
    public fun min(v: RemoteFloat): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MIN) { a, b -> kotlin.math.min(a, b) }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [v]. */
    public operator fun plus(v: Float): RemoteFloat {
        if (v == 0f) {
            return this
        }
        return binaryOp(this, v, AnimatedFloatExpression.ADD, { a, b -> a + b }) { array, opId ->
            when (opId) {
                OP_ADD -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] += v
                    arrayCopy
                }
                OP_SUB -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] -= v
                    arrayCopy
                }
                else -> null
            }
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [v]. */
    public operator fun plus(v: RemoteFloat): RemoteFloat {
        if (v.constantValueOrNull != null && v.constantValueOrNull == 0f) {
            return this
        }
        if (constantValueOrNull != null && constantValueOrNull == 0f) {
            return v
        }
        return binaryOp(this, v, AnimatedFloatExpression.ADD) { a, b -> a + b }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [v]. */
    public operator fun minus(v: Float): RemoteFloat {
        if (v == 0f) {
            return this
        }
        return binaryOp(this, v, SUB, { a, b -> a - b }) { array, opId ->
            when (opId) {
                OP_ADD -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] -= v
                    arrayCopy
                }
                OP_SUB -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] += v
                    arrayCopy
                }
                else -> null
            }
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [v]. */
    public operator fun minus(v: RemoteFloat): RemoteFloat {
        if (v.constantValueOrNull != null && v.constantValueOrNull == 0f) {
            return this
        }
        return binaryOp(this, v, SUB) { a, b -> a - b }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [v]. */
    public operator fun times(v: Float): RemoteFloat {
        if (v == 1f) {
            return this
        }
        if (constantValueOrNull != null && constantValueOrNull == 1f) {
            return RemoteFloat(v)
        }
        return binaryOp(this, v, AnimatedFloatExpression.MUL, { a, b -> a * b }) { array, opId ->
            when (opId) {
                OP_MUL -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] *= v
                    arrayCopy
                }
                OP_DIV -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] /= v
                    arrayCopy
                }
                else -> null
            }
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [v]. */
    public operator fun times(v: RemoteFloat): RemoteFloat {
        if (v.constantValueOrNull != null && v.constantValueOrNull == 1f) {
            return this
        }
        if (constantValueOrNull != null && constantValueOrNull == 1f) {
            return v
        }
        return binaryOp(this, v, AnimatedFloatExpression.MUL) { a, b -> a * b }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] div [v]. */
    public operator fun div(v: Float): RemoteFloat {
        if (v == 1f) {
            return this
        }
        return binaryOp(this, v, AnimatedFloatExpression.DIV, { a, b -> a / b }) { array, opId ->
            when (opId) {
                OP_MUL -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] /= v
                    arrayCopy
                }
                OP_DIV -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] *= v
                    arrayCopy
                }
                else -> null
            }
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] div [v]. */
    public operator fun div(v: RemoteFloat): RemoteFloat {
        if (v.constantValueOrNull != null && v.constantValueOrNull == 1f) {
            return this
        }
        return binaryOp(this, v, AnimatedFloatExpression.DIV) { a, b -> a / b }
    }

    /** Converts this [RemoteFloat] to a [RemoteDp] */
    public fun asRemoteDp(): RemoteDp {
        return RemoteDp(this)
    }

    /**
     * Returns a [RemoteFloat] that is a reference of this RemoteFloat.
     *
     * This is temporarily useful because the floatArray has a maximum size.
     *
     * @param forceRemote If true, forces the creation of a remote reference even if the value is
     *   constant.
     */
    @JvmOverloads
    public fun createReference(forceRemote: Boolean = false): RemoteFloat {
        return RemoteFloatExpression(
            constantValueOrNull = if (forceRemote) null else constantValueOrNull,
            { creationState -> floatArrayOf(asNan(getIdForCreationState(creationState))) },
        )
    }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is equal to the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    public infix fun eq(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *b, *a, SUB, ABS, IFELSE) }) { a, b ->
            if (a == b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is not equal to the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    public infix fun ne(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *b, *a, SUB, ABS, IFELSE) }) { a, b ->
            if (a != b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    public infix fun lt(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *b, *a, SUB, IFELSE) }) { a, b ->
            if (a < b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than or equal to the value
     * of this [RemoteFloat] or `false` otherwise.
     */
    public infix fun le(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *a, *b, SUB, IFELSE) }) { a, b ->
            if (a <= b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    public infix fun gt(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *a, *b, SUB, IFELSE) }) { a, b ->
            if (a > b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than or equal to the
     * value of this [RemoteFloat] or `false` otherwise.
     */
    public infix fun ge(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *b, *a, SUB, IFELSE) }) { a, b ->
            if (a >= b) 1 else 0
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        private fun isConstant(v: Float): Boolean {
            // Assume all NaNs are variables which are probably non-const.
            return !v.isNaN()
        }

        public operator fun invoke(float: Float): RemoteFloat {
            return RemoteFloatExpression(if (isConstant(float)) float else null) { _ ->
                floatArrayOf(float)
            }
        }

        /**
         * Creates a named [RemoteFloat] with an initial value. This allows referring to a float by
         * a symbolic name in the remote document. Named remote ints can be set via
         * AndroidRemoteContext.setNamedFloat.
         *
         * @param name The name of the remote float.
         * @param initialValue The initial value of the remote float.
         * @return A [RemoteFloat] representing the named float.
         */
        @JvmStatic
        public fun createNamedRemoteFloat(name: String, initialValue: Float): RemoteFloat {
            return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
                // TODO: check what happens if the initial value for this is the same as a
                // subsequent non-named variable.
                floatArrayOf(creationState.document.addNamedFloat(name, initialValue))
            }
        }
    }
}

internal fun floatToString(v: Float, before: Int, after: Int, flags: Int) =
    StringUtils.floatToString(
        v,
        before,
        after,
        when (flags and 12) {
            TextFromFloat.PAD_PRE_SPACE -> ' '
            TextFromFloat.PAD_PRE_NONE -> 0.toChar()
            TextFromFloat.PAD_PRE_ZERO -> '0'
            else -> ' '
        },
        when (flags and 3) {
            TextFromFloat.PAD_AFTER_SPACE -> ' '
            TextFromFloat.PAD_AFTER_NONE -> 0.toChar()
            TextFromFloat.PAD_AFTER_ZERO -> '0'
            else -> ' '
        },
        when (flags and (3 shl 6)) {
            TextFromFloat.SEPARATOR_PERIOD_COMMA -> StringUtils.SEPARATOR_PERIOD_COMMA
            TextFromFloat.SEPARATOR_COMMA_PERIOD -> StringUtils.SEPARATOR_COMMA_PERIOD
            TextFromFloat.SEPARATOR_SPACE_COMMA -> StringUtils.SEPARATOR_SPACE_COMMA
            TextFromFloat.SEPARATOR_UNDER_PERIOD -> StringUtils.SEPARATOR_UNDER_PERIOD
            else -> StringUtils.SEPARATOR_PERIOD_COMMA
        }.toByte(),
        when (flags and (3 shl 4)) {
            TextFromFloat.GROUPING_BY3 -> StringUtils.GROUPING_BY3
            TextFromFloat.GROUPING_BY4 -> StringUtils.GROUPING_BY4
            TextFromFloat.GROUPING_BY32 -> StringUtils.GROUPING_BY32
            else -> StringUtils.GROUPING_NONE
        }.toByte(),
        flags shr 8,
    )

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren\'t a const
 *   float.
 * @param directEval When the source is a const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun binaryOp(
    a: Float,
    b: RemoteFloat,
    opCode: Float,
    directEval: (Float, Float) -> Float,
): RemoteFloat {
    val bConst = b.constantValueOrNull
    if (!a.isNaN() && bConst != null) {
        return RemoteFloat(directEval(a, bConst))
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        combineToFloatArray(creationState, arrayOf(RemoteFloat(a), b), opCode)
    }
}

/**
 * Boilerplate for implementing a binary arithmetic operation, with [peepHoleEval] allowing the
 * possibility of folding this operation into the previous one (e.g. folding several additions into
 * one).
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren\'t a const
 *   float.
 * @param directEval When the source is a const float, this lambda will be called to evaluate the
 *   result directly.
 * @param peepHoleEval This allows the caller the option to apply a peephole optimization to a
 *   previous operation. E.g. (x * 3) * 4 could be written as x * 12. If no optimization is possible
 *   peepHoleEval should return null.
 */
internal fun binaryOp(
    a: RemoteFloat,
    b: Float,
    opCode: Float,
    directEval: (Float, Float) -> Float,
    peepHoleEval: (FloatArray, Int) -> FloatArray?,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    if (aConst != null && !b.isNaN()) {
        return RemoteFloat(directEval(aConst, b))
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        val aArray = a.arrayForCreationState(creationState)
        val last = aArray.last()
        if (aArray.size > 2 && last.isNaN() && !aArray[aArray.size - 2].isNaN()) {
            // If the last two elements of the array are a regular number and an operation, run
            // peepHoleEval with combineToFloatArray if that returned null.
            peepHoleEval(aArray, Utils.idFromNan(last))
                ?: combineToFloatArray(creationState, arrayOf(a), b, opCode)
        } else {
            combineToFloatArray(creationState, arrayOf(a), b, opCode)
        }
    }
}

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren\'t a const
 *   float.
 * @param directEval When the source is a const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun binaryOp(
    a: RemoteFloat,
    b: Float,
    opCode: Float,
    directEval: (Float, Float) -> Float,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    if (aConst != null && !b.isNaN()) {
        return RemoteFloat(directEval(aConst, b))
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        combineToFloatArray(creationState, arrayOf(a), b, opCode)
    }
}

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren\'t a const
 *   float.
 * @param directEval When the sources are const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun binaryOp(
    a: RemoteFloat,
    b: RemoteFloat,
    opCode: Float,
    directEval: (Float, Float) -> Float,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return RemoteFloat(directEval(aConst, bConst))
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        combineToFloatArray(creationState, arrayOf(a, b), opCode)
    }
}

/**
 * Boilerplate for implementing a binary comparison operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param expressionGenerator Generator for the comparison expression [FloatArray] used when both
 *   sources aren\'t a const float.
 * @param directEval When the sources are const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun comparisonOp(
    a: RemoteFloat,
    b: RemoteFloat,
    expressionGenerator: (FloatArray, FloatArray) -> FloatArray,
    directEval: (Float, Float) -> Long,
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
            val (finalAArray, finalBArray) =
                if (combinedSize > MAX_SAFE_FLOAT_ARRAY) { // Check if new array would exceed limit
                    Pair(
                        floatArrayOf(a.getFloatIdForCreationState(creationState)),
                        floatArrayOf(b.getFloatIdForCreationState(creationState)),
                    )
                } else {
                    Pair(aArray, bArray)
                }

            val id =
                creationState.document.floatExpression(
                    *expressionGenerator(finalAArray, finalBArray)
                )
            longArrayOf(0x100000000 + Utils.idFromNan(id).toLong())
        }
    )
}

/** Returns [ifTrue] if [a] < [b], otherwise returns [ifFalse]. */
public fun selectIfLT(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst < bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        combineToFloatArray(creationState, arrayOf(ifFalse, ifTrue, b, a), SUB, IFELSE)
    }
}

/** Returns [ifTrue] if [a] <= [b], otherwise returns [ifFalse]. */
public fun selectIfLE(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst <= bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        combineToFloatArray(creationState, arrayOf(ifTrue, ifFalse, a, b), SUB, IFELSE)
    }
}

/** Returns [ifTrue] if [a] > [b], otherwise returns [ifFalse]. */
public fun selectIfGT(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst > bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        combineToFloatArray(creationState, arrayOf(ifFalse, ifTrue, a, b), SUB, IFELSE)
    }
}

/** Returns [ifTrue] if [a] >= [b], otherwise returns [ifFalse]. */
public fun selectIfGE(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat {
    val aConst = a.constantValueOrNull
    val bConst = b.constantValueOrNull
    if (aConst != null && bConst != null) {
        return if (aConst >= bConst) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        combineToFloatArray(creationState, arrayOf(ifTrue, ifFalse, b, a), SUB, IFELSE)
    }
}

/**
 * The difference between now and [referenceEpochMillis] in seconds.
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the difference between now and [referenceEpochMillis]
 *   in seconds.
 */
public fun deltaFromReferenceInSeconds(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_FROM_NOW_SEC,
            )
        )
    }
}

/**
 * The difference between now and [referenceEpochMillis] in minutes.
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the difference between now and [referenceEpochMillis]
 *   in minutes.
 */
public fun deltaFromReferenceInMinutes(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_FROM_NOW_MIN,
            )
        )
    }
}

/**
 * The difference between now and [referenceEpochMillis] in hours.
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return @return A [RemoteFloat] that evaluates to the difference between now and
 *   [referenceEpochMillis] in hours.
 */
public fun deltaFromReferenceInHours(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_FROM_NOW_HR,
            )
        )
    }
}

/**
 * The time of day for [referenceEpochMillis] in seconds (0-59).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the time of day for [referenceEpochMillis] in seconds
 *   (0-59).
 */
public fun timeOfReferenceInSeconds(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_IN_SEC,
            )
        )
    }
}

/**
 * The time of day for [referenceEpochMillis] in minutes (0-59).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the time of day for [referenceEpochMillis] in minutes
 *   (0-59).
 */
public fun timeOfReferenceInMinutes(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_IN_MIN,
            )
        )
    }
}

/**
 * The time of day for [referenceEpochMillis] in hours (0-23).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the time of day for [referenceEpochMillis] in hours
 *   (0-23).
 */
public fun timeOfReferenceInHours(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_IN_HR,
            )
        )
    }
}

/**
 * The day of month for [referenceEpochMillis] (1-31).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the day of month for [referenceEpochMillis] (1-31).
 */
public fun dayOfMonthForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_DAY_OF_MONTH,
            )
        )
    }
}

/**
 * The month of year for [referenceEpochMillis] (0-11).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the month of year for [referenceEpochMillis] (0-11).
 */
public fun monthOfYearForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_MONTH_VALUE,
            )
        )
    }
}

/**
 * The day of week for [referenceEpochMillis] (0-16).
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteInt] that evaluates to the day of week for [referenceEpochMillis] (0-16).
 */
public fun dayOfWeekForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_DAY_OF_WEEK,
            )
        )
    }
}

/**
 * The year of [referenceEpochMillis].
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteInt] that evaluates to the year of [referenceEpochMillis].
 */
public fun yearForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_YEAR,
            )
        )
    }
}

/**
 * A mutable implementation of [RemoteFloat]. It also implements [MutableRemoteState<Float>].
 *
 * @property idProvider A lambda that provides the ID for this mutable float within the
 *   [RemoteComposeCreationState]. Defaults to reserving a new float variable ID if not provided.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableRemoteFloat(
    private var idProvider: (creationState: RemoteComposeCreationState) -> Float
) : RemoteFloat(), MutableRemoteState<Float> {
    public constructor(
        id: Float? = null
    ) : this({ creationState -> id ?: creationState.document.reserveFloatVariable() })

    public override val constantValueOrNull: Float?
        get() = null

    public override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState -> floatArrayOf(idProvider(creationState)) }

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        Utils.idFromNan(idProvider(creationState))
}

/**
 * An implementation of [RemoteFloat] that represents a float expression.
 *
 * @property hasConstantValue Indicates if this expression will always yield the same value.
 * @property arrayProvider A lambda that provides the [FloatArray] representing the expression.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteFloatExpression
internal constructor(
    public override val constantValueOrNull: Float?,
    public override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
) : RemoteFloat() {

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val array = arrayForCreationState(creationState)
        // In case we have a single element array, check if the element is an id or not;
        // if it is an existing id, just return this one, no need to create a new one...
        if (array.size == 1 && array[0].isNaN()) {
            return Utils.idFromNan(array[0])
        }

        val hash = calcHashID(array, null)
        val fe = creationState.expressionCache[hash]
        if (fe != null) {
            // TODO check if contentEquals is safe here with NaN?
            if (
                fe != this &&
                    fe is RemoteFloatExpression &&
                    fe.arrayForCreationState(creationState) contentEquals array
            ) {
                return fe.getIdForCreationState(creationState)
            }
            creationState.expressionCache.put(hash, this)
            return Utils.idFromNan(creationState.document.floatExpression(*array))
        } else {
            creationState.expressionCache.put(hash, this)
            return Utils.idFromNan(creationState.document.floatExpression(*array))
        }
    }
}

/**
 * Represents an animated remote float. It combines an input [RemoteFloat] with an animation
 * definition ([FloatArray]).
 *
 * @property input The base [RemoteFloat] that is being animated.
 * @property anim The [FloatArray] defining the animation (e.g., keyframes, easing parameters).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnimatedRemoteFloat(public val input: RemoteFloat, public val anim: FloatArray) :
    RemoteFloat() {
    public override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState -> floatArrayOf(asNan(getIdForCreationState(creationState))) }

    public override val constantValueOrNull: Float?
        get() = null

    public val easing: FloatAnimation = FloatAnimation(*anim)
    public val exp: AnimatedFloatExpression = AnimatedFloatExpression()

    public var lastValue: Float = Float.NaN
    public var lastChanged: Float = Float.NaN
    public val start: Long = System.nanoTime()

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val array = input.arrayForCreationState(creationState)
        val hash = calcHashID(array, anim)
        val fe = creationState.expressionCache[hash]
        if (fe != null) {
            // TODO check if contentEquals is safe here with NaN?
            if (
                fe != this &&
                    fe is RemoteFloatExpression &&
                    fe.arrayForCreationState(creationState) contentEquals array
            ) {
                return fe.getIdForCreationState(creationState)
            }
            creationState.expressionCache.put(hash, this)
            return Utils.idFromNan(creationState.document.floatExpression(array, anim))
        } else {
            creationState.expressionCache.put(hash, this)
            return Utils.idFromNan(creationState.document.floatExpression(array, anim))
        }
    }

    public fun getAnimationTime(): Float {
        return (System.nanoTime() - start) * 1E-9f
    }

    public fun getEasedFloat(array: FloatArray): Float {
        val value = exp.eval(updateTime(array))

        val time =
            if (lastChanged.isNaN()) {
                lastChanged = getAnimationTime()
                0f
            } else {
                getAnimationTime() - lastChanged
            }

        if (lastValue.isNaN()) {
            lastValue = value
        }
        var outPut = lastValue
        if (lastValue == value) {
            lastChanged = getAnimationTime()
            return value
        }

        if (time < easing.duration) {
            easing.initialValue = lastValue
            easing.targetValue = value
            outPut = easing.get(time)
        } else {
            lastChanged = getAnimationTime()
            lastValue = value
            outPut = value
            easing.initialValue = lastValue
        }
        return outPut
    }
}

private fun calcHashID(array: FloatArray, anim: FloatArray?): Int {
    var sum = 0
    for (fl in array) {
        sum = sum * 31 + fl.toRawBits()
    }
    var animLocal = anim
    if (animLocal != null) {
        for (fl in animLocal) {
            sum = sum * 31 + fl.toRawBits()
        }
    }
    return sum
}

public const val CUBIC_STANDARD: Int = 1
public const val CUBIC_ACCELERATE: Int = 2
public const val CUBIC_DECELERATE: Int = 3
public const val CUBIC_LINEAR: Int = 4
public const val CUBIC_ANTICIPATE: Int = 5
public const val CUBIC_OVERSHOOT: Int = 6
public const val CUBIC_CUSTOM: Int = 11
public const val SPLINE_CUSTOM: Int = 12
public const val EASE_OUT_BOUNCE: Int = 13
public const val EASE_OUT_ELASTIC: Int = 14

/** Describes the type of animation RemoteCompose applies to an animated value. */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            CUBIC_STANDARD,
            CUBIC_ACCELERATE,
            CUBIC_DECELERATE,
            CUBIC_LINEAR,
            CUBIC_ANTICIPATE,
            CUBIC_OVERSHOOT,
            CUBIC_CUSTOM,
            SPLINE_CUSTOM,
            EASE_OUT_BOUNCE,
            EASE_OUT_ELASTIC,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class AnimationType

/**
 * Converts a [Number] to a [FloatArray]. If the number is a [RemoteFloat], its
 * `arrayForCreationState` is used. Otherwise, it\'s converted to a single-element [FloatArray].
 *
 * @param a The number to convert.
 * @return A [FloatArray] representation of the number.
 */
public fun toArray(a: Number): FloatArray =
    when (a) {
        is Float -> floatArrayOf(a)
        else -> floatArrayOf(a.toFloat())
    }

/**
 * Converts a [Number] to a [FloatArray] using a specific [RemoteComposeCreationState]. If the
 * number is a [RemoteFloat], its `arrayForCreationState` is used. Otherwise, it\'s converted to a
 * single-element [FloatArray].
 *
 * @param a The number to convert.
 * @param creationState The [RemoteComposeCreationState] to use for conversion.
 * @return A [FloatArray] representation of the number.
 */
public fun toArray(a: RemoteFloat, creationState: RemoteComposeCreationState): FloatArray =
    a.arrayForCreationState(creationState)

/**
 * Composable function to remember and provide a [RemoteFloat] from a [FloatArray]. This is intended
 * for use within a `@Composable` context.
 *
 * @param content A lambda that provides the [FloatArray] to be remembered.
 * @return A [RemoteFloat] representing the remembered float array.
 */
@Composable
@RemoteComposable
public fun rememberRemoteFloatArray(content: () -> FloatArray): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    val floatArrayId = state.document.addFloatArray(content())
    return rememberRemoteFloat { floatArrayId.rf }
}

/**
 * Composable function to remember and provide a mutable remote float value. This is intended for
 * use within a `@Composable` context and allows defining the initial value using a
 * [RemoteFloatContext].
 *
 * @param content A lambda that takes a [RemoteFloatContext] and returns the initial float value.
 * @return A [MutableRemoteFloat] that can be observed and changed.
 */
@Composable
@RemoteComposable
public fun rememberMutableRemoteFloat(
    content: RemoteFloatContext.() -> RemoteFloat
): MutableRemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val context = RemoteFloatContext(state)
        val value = content(context)
        MutableRemoteFloat { state -> value.getFloatIdForCreationState(state) }
    }
}

/**
 * Composable function to remember and provide a [RemoteFloat]. This is intended for use within a
 * `@Composable` context.
 *
 * @param content A lambda that provides the [RemoteFloat] to be remembered.
 * @return A [RemoteFloat] representing the remembered remote float.
 */
@Composable
@RemoteComposable
public fun rememberRemoteFloat(content: RemoteFloatContext.() -> RemoteFloat): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    val context = RemoteFloatContext(state)
    val remoteFloat = content(context)
    return remember { remoteFloat }
}

/**
 * A Composable function to remember and provide a **named** remote float value.
 *
 * @param name The unique name for this remote float.
 * @param domain The domain of the named float (defaults to [RemoteDomains.USER]). This helps
 *   organize named values in the remote document.
 * @param content default [RemoteFloat] value for this remote float.
 * @return A [RemoteFloat] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
public fun rememberRemoteFloat(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    content: RemoteFloatContext.() -> RemoteFloat,
): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    val context = RemoteFloatContext(state)
    val remoteFloat = content(context)
    return rememberNamedState(name, domain) {
        RemoteFloatExpression(constantValueOrNull = null) { creationState ->
            // Create an additional expression to name, in case the input value is meaningful
            // and just a default. So override is of this named value, not the expression.
            val id =
                state.document.floatExpression(*remoteFloat.arrayForCreationState(creationState))
            state.document.setStringName(Utils.idFromNan(id), "$domain:$name")
            floatArrayOf(id)
        }
    }
}

/**
 * Creates a [RemoteFloat] using a [RemoteFloatContext] and a specified
 * [RemoteComposeCreationState].
 *
 * @param state The [RemoteComposeCreationState] to use.
 * @param content A lambda that takes a [RemoteFloatContext] and returns a [RemoteFloat].
 * @return The created [RemoteFloat].
 */
public fun remoteFloat(
    state: RemoteStateScope,
    content: RemoteFloatContext.() -> RemoteFloat,
): RemoteFloat {
    val context = RemoteFloatContext(state)
    val value = context.content()
    return value
}

/**
 * Updates an array of floats, replacing time-dependent variables with their current values and a
 * specific density ID.
 *
 * @param array The input [FloatArray].
 * @return A new [FloatArray] with time variables and density updated.
 */
public fun updateTime(array: FloatArray): FloatArray {
    val ret = array.copyOf()
    for ((i, fl) in array.withIndex()) {
        if (isTimeVar(fl)) {
            ret[i] = RemoteContext.getTime(fl)
        }
        // TODO: we should revisit the document variable lifecycle
        if (Utils.idFromNan(fl) == RemoteContext.ID_DENSITY) {
            ret[i] = 2.75f
        }
    }
    return ret
}

/**
 * Checks if a given float represents a time variable.
 *
 * @param fl The float to check.
 * @return `true` if the float is a time variable, `false` otherwise.
 */
public fun isTimeVar(fl: Float): Boolean {
    return RemoteContext.isTime(fl)
}

/**
 * Converts a [FloatArray] expression to a human-readable string representation. It includes labels
 * for NaN values, which typically represent IDs.
 *
 * @param array The [FloatArray] representing the expression.
 * @return A string representation of the expression.
 */
public fun toString(array: FloatArray): String {
    val labels = arrayOfNulls<String>(array.size)
    for (i in array.indices) {
        if (java.lang.Float.isNaN(array[i])) {
            labels[i] = "[" + Utils.idStringFromNan(array[i]) + "]"
        }
    }
    return AnimatedFloatExpression.toString(array, labels)
}

/**
 * Constructs a floatArray that either inlines or references the contents of [remoteFloats] followed
 * by [extras]. Inlining is preferred as long as the resulting array length is less than
 * [MAX_SAFE_FLOAT_ARRAY].
 */
internal fun combineToFloatArray(
    creationState: RemoteComposeCreationState,
    remoteFloats: Array<RemoteFloat>,
    vararg extras: Float,
): FloatArray {
    var totalSizeInline = extras.size
    val totalSizeReference = extras.size + remoteFloats.size
    val arrays =
        Array<FloatArray>(remoteFloats.size) { i ->
            val array = remoteFloats[i].arrayForCreationState(creationState)
            totalSizeInline += array.size
            array
        }

    val combinedArray: FloatArray
    var idx = 0

    if (totalSizeInline > MAX_SAFE_FLOAT_ARRAY) {
        // Add references for the RemoteFloat values.
        combinedArray = FloatArray(totalSizeReference)
        for (i in remoteFloats.indices) {
            combinedArray[i] = remoteFloats[i].getFloatIdForCreationState(creationState)
        }
        idx = remoteFloats.size
    } else {
        // Inline the RemoteFloat arrays.
        combinedArray = FloatArray(totalSizeInline)
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
