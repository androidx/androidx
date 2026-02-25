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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Abstract base class for all remote float representations.
 *
 * `RemoteFloat` represents a floating-point value that can be a constant, a possibly named
 * variable, or a dynamic expression (e.g., an arithmetic operation).
 */
@Stable
public abstract class RemoteFloat internal constructor() : BaseRemoteState<Float>() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal abstract val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun arrayForCreationState(creationState: RemoteComposeCreationState): FloatArray {
        val cachedArray = creationState.floatArrayCache.get(this)
        if (cachedArray != null) {
            return cachedArray
        }
        val array = arrayProvider(creationState)
        creationState.floatArrayCache.put(this, array)
        return array
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toRemoteInt(): RemoteInt {
        constantValueOrNull?.let {
            return RemoteInt(it.toInt())
        }
        return RemoteIntExpression(constantValueOrNull = null) { creationState ->
            longArrayOf(getLongIdForCreationState(creationState))
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun unaryMinus(): RemoteFloat {
        constantValueOrNull?.let {
            return RemoteFloat(-it)
        }

        return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
            combineToFloatArray(creationState, arrayOf(this), -1f, AnimatedFloatExpression.MUL)
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun rem(v: Float): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MOD) { a, b -> a % b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun rem(v: RemoteFloat): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MOD) { a, b -> a % b }

    /** Returns a new [RemoteFloat] that evaluates to minimum of this [RemoteFloat] and [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun min(v: Float): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MIN, { a, b -> kotlin.math.min(a, b) }) {
            array,
            opId ->
            null
        }

    /** Returns a new [RemoteFloat] that evaluates to minimum of this [RemoteFloat] and [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun min(v: RemoteFloat): RemoteFloat =
        binaryOp(this, v, AnimatedFloatExpression.MIN) { a, b -> kotlin.math.min(a, b) }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: Float): RemoteFloat {
        if (v == 0f) {
            return this
        }
        return binaryOp(this, v, AnimatedFloatExpression.ADD, { a, b -> a + b }) { array, opId ->
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

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: RemoteFloat): RemoteFloat {
        v.constantValueOrNull?.let {
            return plus(it)
        }
        constantValueOrNull?.let {
            return v.plus(it)
        }
        return binaryOp(this, v, AnimatedFloatExpression.ADD) { a, b -> a + b }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun minus(v: Float): RemoteFloat {
        if (v == 0f) {
            return this
        }
        return binaryOp(this, v, SUB, { a, b -> a - b }) { array, opId ->
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

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun minus(v: RemoteFloat): RemoteFloat {
        v.constantValueOrNull?.let {
            return minus(it)
        }
        constantValueOrNull?.let {
            return (-v).plus(it)
        }
        return binaryOp(this, v, SUB) { a, b -> a - b }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun times(v: Float): RemoteFloat {
        if (v == 0f) {
            return RemoteFloat(0f)
        }
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

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun times(v: RemoteFloat): RemoteFloat {
        v.constantValueOrNull?.let {
            return times(it)
        }
        constantValueOrNull?.let {
            return v.times(it)
        }
        return binaryOp(this, v, AnimatedFloatExpression.MUL) { a, b -> a * b }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] div [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun div(v: Float): RemoteFloat {
        if (constantValueOrNull != null && constantValueOrNull == 0f) {
            return RemoteFloat(0f)
        }
        if (v == 1f) {
            return this
        }
        return binaryOp(this, v, AnimatedFloatExpression.DIV, { a, b -> a / b }) { array, opId ->
            when (opId) {
                OP_MUL -> {
                    val arrayCopy = array.clone()
                    arrayCopy[arrayCopy.size - 2] /= v
                    maybeTrimIfOne(arrayCopy)
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

    private fun maybeTrimIfZero(array: FloatArray) =
        if (array[array.size - 2] == 0f) {
            array.copyOfRange(0, array.size - 2)
        } else {
            array
        }

    private fun maybeTrimIfOne(array: FloatArray) =
        if (array[array.size - 2] == 1f) {
            array.copyOfRange(0, array.size - 2)
        } else {
            array
        }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] div [v]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun div(v: RemoteFloat): RemoteFloat {
        if (constantValueOrNull != null && constantValueOrNull == 0f) {
            return RemoteFloat(0f)
        }
        v.constantValueOrNull?.let {
            return div(it)
        }
        return binaryOp(this, v, AnimatedFloatExpression.DIV) { a, b -> a / b }
    }

    /** Converts this [RemoteFloat] to a [RemoteDp] */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun eq(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *b, *a, SUB, ABS, IFELSE) }) { a, b ->
            if (a == b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is not equal to the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun ne(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *b, *a, SUB, ABS, IFELSE) }) { a, b ->
            if (a != b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun lt(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *b, *a, SUB, IFELSE) }) { a, b ->
            if (a < b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than or equal to the value
     * of this [RemoteFloat] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun le(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *a, *b, SUB, IFELSE) }) { a, b ->
            if (a <= b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public infix fun gt(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *a, *b, SUB, IFELSE) }) { a, b ->
            if (a > b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than or equal to the
     * value of this [RemoteFloat] or `false` otherwise.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public operator fun invoke(float: Float): RemoteFloat {
            return RemoteFloatExpression(if (isConstant(float)) float else null) { _ ->
                floatArrayOf(float)
            }
        }

        /**
         * Creates a [RemoteFloat] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteFloat] referencing the ID.
         */
        internal fun createForId(id: Float): RemoteFloat = RemoteFloat(id)

        /**
         * Creates a named [RemoteFloat] with an initial value. This allows referring to a float by
         * a symbolic name in the remote document. Named remote ints can be set via
         * AndroidRemoteContext.setNamedFloat.
         *
         * @param name The name of the remote float.
         * @param defaultValue The initial value of the remote float.
         * @return A [RemoteFloat] representing the named float.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun createNamedRemoteFloat(
            name: String,
            defaultValue: Float,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteFloat {
            return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
                floatArrayOf(creationState.document.addNamedFloat("$domain:$name", defaultValue))
            }
        }

        @JvmStatic
        public fun createNamedRemoteFloatExpression(
            name: String,
            domain: RemoteState.Domain = RemoteState.Domain.User,
            expression: RemoteFloatContext.() -> RemoteFloat,
        ): RemoteFloat {
            return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
                val context = RemoteFloatContext(creationState)
                val result = expression(context)
                val initialValueId = result.getFloatIdForCreationState(creationState)
                val floatId = creationState.document.addNamedFloat("$domain:$name", initialValueId)
                floatArrayOf(floatId)
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
        },
        when (flags and (3 shl 4)) {
            TextFromFloat.GROUPING_BY3 -> StringUtils.GROUPING_BY3
            TextFromFloat.GROUPING_BY4 -> StringUtils.GROUPING_BY4
            TextFromFloat.GROUPING_BY32 -> StringUtils.GROUPING_BY32
            else -> StringUtils.GROUPING_NONE
        },
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
        return RemoteBoolean(RemoteInt.createForId(directEval(aConst, bConst)))
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLt(
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLe(
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGt(
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGe(
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

/** A mutable implementation of [RemoteFloat]. It also implements [MutableRemoteState<Float>]. */
public class MutableRemoteFloat
internal constructor(private var idProvider: (creationState: RemoteComposeCreationState) -> Float) :
    RemoteFloat(), MutableRemoteState<Float> {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        id: Float? = null
    ) : this({ creationState -> id ?: creationState.document.reserveFloatVariable() })

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Float?
        get() = null

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState -> floatArrayOf(idProvider(creationState)) }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        Utils.idFromNan(idProvider(creationState))

    public companion object {
        /**
         * Creates a new mutable state (allocates an ID).
         *
         * @param initialValue The initial value for the state.
         * @return A new [MutableRemoteFloat] instance.
         */
        public fun createMutable(initialValue: Float): MutableRemoteFloat {
            return MutableRemoteFloat { creationState ->
                creationState.document.addFloatConstant(initialValue)
            }
        }

        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteFloat] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Float): MutableRemoteFloat = MutableRemoteFloat(id)
    }
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
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Float?,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
) : RemoteFloat() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState -> floatArrayOf(asNan(getIdForCreationState(creationState))) }

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Float?
        get() = null

    public val easing: FloatAnimation = FloatAnimation(*anim)
    public val exp: AnimatedFloatExpression = AnimatedFloatExpression()

    public var lastValue: Float = Float.NaN
    public var lastChanged: Float = Float.NaN
    public val start: Long = System.nanoTime()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_STANDARD: Int = 1
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_ACCELERATE: Int = 2
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_DECELERATE: Int = 3
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_LINEAR: Int = 4
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_ANTICIPATE: Int = 5
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_OVERSHOOT: Int = 6
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val CUBIC_CUSTOM: Int = 11
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val SPLINE_CUSTOM: Int = 12
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val EASE_OUT_BOUNCE: Int = 13
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val EASE_OUT_ELASTIC: Int = 14

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun toArray(a: RemoteFloat, creationState: RemoteComposeCreationState): FloatArray =
    a.arrayForCreationState(creationState)

/**
 * Composable function to remember and provide a [RemoteFloat] from a [FloatArray]. This is intended
 * for use within a `@Composable` context.
 *
 * @param content A lambda that provides the [FloatArray] to be remembered.
 * @return A [RemoteFloat] representing the remembered float array.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun rememberRemoteFloatArray(content: () -> FloatArray): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    return rememberRemoteFloatExpression {
        val floatArrayId = state.document.addFloatArray(content())
        floatArrayId.rf
    }
}

/**
 * Factory composable for mutable remote float state.
 *
 * @param initialValue The initial [Float] value.
 * @return A [MutableRemoteFloat] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
public fun rememberMutableRemoteFloat(initialValue: Float): MutableRemoteFloat {
    return remember { MutableRemoteFloat.createMutable(initialValue) }
}

/**
 * Remembers a remote float expression based on [RemoteFloatContext].
 *
 * @param content A lambda that provides the [RemoteFloat] expression.
 * @return A [RemoteFloat] instance representing the provided expression.
 */
@Composable
@RemoteComposable
public fun rememberMutableRemoteFloat(
    content: RemoteFloatContext.() -> RemoteFloat
): MutableRemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val context = RemoteFloatContext(state)
        // Currently evaluated eagerly to grab the right component
        val value = content(context)
        MutableRemoteFloat { state ->
            // Force creation of an id
            asNan(value.getIdForCreationState(state))
        }
    }
}

/**
 * Factory composable for state.
 *
 * @param content A lambda that provides the [RemoteFloat] expression.
 * @return A [RemoteFloat] instance representing the provided expression.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun rememberRemoteFloatExpression(
    content: RemoteFloatContext.() -> RemoteFloat
): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val context = RemoteFloatContext(state)
        // Currently evaluated eagerly to grab the right component
        val remoteFloat = content(context)
        remoteFloat
    }
}

/**
 * Remembers a named remote float expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @param content A lambda that provides the [RemoteFloat] expression.
 * @return A [RemoteFloat] instance representing the named expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteFloat(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    content: RemoteFloatContext.() -> RemoteFloat,
): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    return rememberNamedState(name, domain) {
        val context = RemoteFloatContext(state)
        // Currently evaluated eagerly to grab the right component
        val remoteFloat = content(context)
        RemoteFloatExpression(constantValueOrNull = null) { creationState ->
            // Create an additional expression to name, in case the input value is meaningful
            // and just a default. So override is of this named value, not the expression.
            val floatId =
                state.document.floatExpression(*remoteFloat.arrayForCreationState(creationState))
            state.document.addNamedFloat("$domain:$name", floatId)
            floatArrayOf(floatId)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
@Deprecated("Use rememberNamedRemoteFloat(name, domain, content = { content() })")
public fun rememberRemoteFloat(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    content: RemoteFloatContext.() -> RemoteFloat,
): RemoteFloat {
    return rememberNamedRemoteFloat(name, domain, content)
}

/**
 * Creates a [RemoteFloat] using a [RemoteFloatContext] and a specified
 * [RemoteComposeCreationState].
 *
 * @param state The [RemoteComposeCreationState] to use.
 * @param content A lambda that takes a [RemoteFloatContext] and returns a [RemoteFloat].
 * @return The created [RemoteFloat].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("Use rememberRemoteFloatExpression(content = { content() })")
public fun remoteFloat(
    state: RemoteStateScope,
    content: RemoteFloatContext.() -> RemoteFloat,
): RemoteFloat {
    val context = RemoteFloatContext(state)
    val value = context.content()
    return value
}

/**
 * Checks if a given float represents a time variable.
 *
 * @param fl The float to check.
 * @return `true` if the float is a time variable, `false` otherwise.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    get() = RemoteFloat(this.toFloat())

/** Extension property to convert a [Float] to a [RemoteFloat]. */
public val Float.rf: RemoteFloat
    get() = RemoteFloat(this)

/** Extension function to get either a Float ID or a Float literal from a [Number]. */
internal fun Number.getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float =
    when (this) {
        is Float -> this
        else -> toFloat()
    }
