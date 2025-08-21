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

import androidx.annotation.IntDef
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ABS
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.IFELSE
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.core.operations.utilities.StringUtils
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.frontend.layout.RemoteFloatContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/** An inline value class representing a reference to a remote float. */
@JvmInline value class RemoteFloatReference(private val v: Float)

/** Extension property to convert an [Int] to a [RemoteFloat]. */
val Int.rf: RemoteFloat
    get() {
        return RemoteFloatExpression(true) { _ -> floatArrayOf(this.toFloat()) }
    }

/** Extension property to convert a [Float] to a [RemoteFloat]. */
val Float.rf: RemoteFloat
    get() {
        return RemoteFloatExpression(true) { _ -> floatArrayOf(this) }
    }

/** Extension function to get either a Float ID or a Float literal from a [Number]. */
internal fun Number.getFloatIdForCreationState(creationState: RemoteComposeCreationState): Float =
    when (this) {
        is Float -> this
        is RemoteFloat -> getFloatIdForCreationState(creationState)
        else -> toFloat()
    }

/** Extension property that extracts whether or not a [Number] represents a constant value. */
internal val Number.hasConstantValue: Boolean
    get() =
        when (this) {
            is RemoteFloat -> this.hasConstantValue
            else -> true
        }

/**
 * Abstract base class for all remote float representations. It extends [Number] and implements
 * [RemoteState<Float>].
 *
 * @property hasConstantValue Whether this [RemoteFloat] will always evaluate to the same value.
 *   This is a conservative check and might return false for some expressions that are effectively
 *   constant.
 */
abstract class RemoteFloat internal constructor(override val hasConstantValue: Boolean) :
    Number(), RemoteState<Float> {
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

    /**
     * Deprecated property to get the ID of the remote float. It's recommended to use
     * [getFloatIdForCreationState] directly for clarity and to pass the correct
     * [RemoteComposeCreationState].
     */
    // TODO: re-enable asap
    // @Deprecated("Use getIdForCreationState directly")
    open val id: Float
        get() {
            // FallbackCreationState.state.platform.log(
            //     Platform.LogCategory.TODO,
            //     "Use RemoteFloat.getIdForCreationState directly"
            // )
            return getFloatIdForCreationState(FallbackCreationState.state)
        }

    fun internalAsFloat(): Float {
        return id
    }

    override fun toFloat(): Float {
        return id
    }

    /**
     * Returns a [RemoteInt] that evaluates to the result of this [RemoteFloat] converted to Int.
     */
    fun toRemoteInt(): RemoteInt {
        return RemoteIntExpression(hasConstantValue) { creationState ->
            val a = arrayForCreationState(creationState)
            if (a.isLiteral()) {
                longArrayOf(a[0].toInt().toLong())
            } else {
                longArrayOf(getLongIdForCreationState(creationState))
            }
        }
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
    fun toRemoteString(
        before: Int,
        after: Int = 2,
        flags: Int = TextFromFloat.PAD_AFTER_ZERO,
    ): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""), // TODO compute the string?
            hasConstantValue,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    val a = arrayForCreationState(creationState)
                    if (a.isLiteral()) {
                        return creationState.document.textCreateId(
                            floatToString(a[0], before, after, flags)
                        )
                    }

                    return creationState.document.createTextFromFloat(
                        getFloatIdForCreationState(creationState),
                        before,
                        after,
                        flags,
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? {
                    val a = arrayForCreationState(creationState)
                    if (a.isLiteral()) {
                        return floatToString(a[0], before, after, flags).toCodePointSet()
                    }

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

    override fun toByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun toDouble(): Double {
        TODO("Not yet implemented")
    }

    override fun toInt(): Int {
        TODO("Not yet implemented")
    }

    override fun toLong(): Long {
        TODO("Not yet implemented")
    }

    override fun toShort(): Short {
        TODO("Not yet implemented")
    }

    /**
     * Boilerplate for implementing an unary operation.
     *
     * @param opCode The opcode to insert in the generated [FloatArray] if the source isn't a const
     *   int.
     * @param directEval When the source is a const int, this lambda will be called to evaluate the
     *   result directly.
     */
    internal fun unaryOp(opCode: Float, directEval: (Float) -> Float): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue) { creationState ->
            val a = arrayForCreationState(creationState)
            if (a.size != 1 || a[0].isNaN()) {
                floatArrayOf(*a, opCode)
            } else {
                floatArrayOf(directEval(a[0]))
            }
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to the negative of this [RemoteFloat]. */
    operator fun unaryMinus(): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue) { creationState ->
            floatArrayOf(*arrayForCreationState(creationState), -1f, AnimatedFloatExpression.MUL)
        }
    }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [v]. */
    operator fun rem(v: Float) = binaryOp(this, v, AnimatedFloatExpression.MOD) { a, b -> a % b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] modulo [v]. */
    operator fun rem(v: RemoteFloat) =
        binaryOp(this, v, AnimatedFloatExpression.MOD) { a, b -> a % b }

    /** Returns a new [RemoteFloat] that evaluates to minimum of this [RemoteFloat] and [v]. */
    fun min(v: Float) =
        binaryOp(this, v, AnimatedFloatExpression.MIN) { a, b -> kotlin.math.min(a, b) }

    /** Returns a new [RemoteFloat] that evaluates to minimum of this [RemoteFloat] and [v]. */
    fun min(v: RemoteFloat) =
        binaryOp(this, v, AnimatedFloatExpression.MIN) { a, b -> kotlin.math.min(a, b) }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [v]. */
    operator fun plus(v: Float) = binaryOp(this, v, AnimatedFloatExpression.ADD) { a, b -> a + b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] plus [v]. */
    operator fun plus(v: RemoteFloat) =
        binaryOp(this, v, AnimatedFloatExpression.ADD) { a, b -> a + b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [v]. */
    operator fun minus(v: Float) = binaryOp(this, v, SUB) { a, b -> a - b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] minus [v]. */
    operator fun minus(v: RemoteFloat) = binaryOp(this, v, SUB) { a, b -> a - b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [v]. */
    operator fun times(v: Float) = binaryOp(this, v, AnimatedFloatExpression.MUL) { a, b -> a * b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] times [v]. */
    operator fun times(v: RemoteFloat) =
        binaryOp(this, v, AnimatedFloatExpression.MUL) { a, b -> a * b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] div [v]. */
    operator fun div(v: Float) = binaryOp(this, v, AnimatedFloatExpression.DIV) { a, b -> a / b }

    /** Returns a new [RemoteFloat] that evaluates to this [RemoteFloat] div [v]. */
    operator fun div(v: RemoteFloat) =
        binaryOp(this, v, AnimatedFloatExpression.DIV) { a, b -> a / b }

    /** Converts this [RemoteFloat] to a [RemoteDp] */
    fun asRemoteDp(): RemoteDp {
        return RemoteDp(this)
    }

    /**
     * Returns a [RemoteFloat] that is a reference of this RemoteFloat.
     *
     * This is temporarily useful because the floatArray has a maximum size.
     */
    // TODO: Remove the need for this.
    fun createReference(): RemoteFloat {
        return RemoteFloatExpression(
            hasConstantValue,
            { creationState -> floatArrayOf(getFloatIdForCreationState(creationState)) },
        )
    }

    /**
     * Property to convert this [RemoteFloat] to a density-independent pixel value. It multiplies
     * the current float value by the screen's density.
     */
    val dp: RemoteFloat
        get() {
            return RemoteFloatExpression(hasConstantValue) { creationState ->
                floatArrayOf(
                    *arrayForCreationState(creationState),
                    RemoteContext.FLOAT_DENSITY,
                    AnimatedFloatExpression.MUL,
                )
            }
        }

    /**
     * Array access operator for [RemoteFloat] with a [RemoteFloat] index. Performs a dereference
     * operation on a remote float array.
     */
    operator fun get(v: RemoteFloat): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue) { creationState ->
            floatArrayOf(
                *v.arrayForCreationState(creationState),
                *arrayForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    /**
     * Array access operator for [RemoteFloat] with an [Int] index. Performs a dereference operation
     * on a remote loat array.
     */
    operator fun get(v: Int): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue) { creationState ->
            floatArrayOf(
                v.toFloat(),
                *arrayForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    /**
     * Array access operator for [RemoteFloat] with a [RemoteInt] index. Performs a dereference
     * operation on a remote float array.
     */
    operator fun get(v: RemoteInt): RemoteFloat {
        return RemoteFloatExpression(hasConstantValue && v.hasConstantValue) { creationState ->
            floatArrayOf(
                v.getFloatIdForCreationState(creationState),
                *arrayForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is equal to the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    infix fun eq(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *b, *a, SUB, ABS, IFELSE) }) { a, b ->
            if (a == b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is not equal to the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    infix fun ne(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *b, *a, SUB, ABS, IFELSE) }) { a, b ->
            if (a != b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    infix fun lt(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *b, *a, SUB, IFELSE) }) { a, b ->
            if (a < b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is less than or equal to the value
     * of this [RemoteFloat] or `false` otherwise.
     */
    infix fun le(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *a, *b, SUB, IFELSE) }) { a, b ->
            if (a <= b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than the value of this
     * [RemoteFloat] or `false` otherwise.
     */
    infix fun gt(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(0f, 1f, *a, *b, SUB, IFELSE) }) { a, b ->
            if (a > b) 1 else 0
        }

    /**
     * Returns a [RemoteBoolean] that evaluates to `true` if [b] is greater than or equal to the
     * value of this [RemoteFloat] or `false` otherwise.
     */
    infix fun ge(b: RemoteFloat): RemoteBoolean =
        comparisonOp(this, b, { a, b -> floatArrayOf(1f, 0f, *b, *a, SUB, IFELSE) }) { a, b ->
            if (a >= b) 1 else 0
        }

    companion object {
        private fun isConstant(v: Float): Boolean {
            // Assume all NaNs are variables which are probably non-const.
            return !v.isNaN()
        }

        operator fun invoke(float: Float): RemoteFloat {
            return RemoteFloatExpression(isConstant(float)) { _ -> floatArrayOf(float) }
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
        fun createNamedRemoteFloat(name: String, initialValue: Float): RemoteFloat {
            return RemoteFloatExpression(false) { creationState ->
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
    )

internal fun FloatArray.isLiteral() = size == 1 && !get(0).isNaN()

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren't a const
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
    return RemoteFloatExpression(b.hasConstantValue) { creationState ->
        val bArray = b.arrayForCreationState(creationState)
        if (!a.isNaN() && bArray.isLiteral()) {
            // A and b are both constants so we can evaluate directly.
            floatArrayOf(directEval(a, bArray[0]))
        } else {
            // B isn't a constant so we need to evaluate dynamically.
            floatArrayOf(a, *bArray, opCode)
        }
    }
}

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren't a const
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
    return RemoteFloatExpression(a.hasConstantValue) { creationState ->
        val aArray = a.arrayForCreationState(creationState)
        if (aArray.isLiteral() && !b.isNaN()) {
            // A and b are both constants so we can evaluate directly.
            floatArrayOf(directEval(aArray[0], b))
        } else {
            // A isn't a constant so we need to evaluate dynamically.
            floatArrayOf(*aArray, b, opCode)
        }
    }
}

/**
 * Boilerplate for implementing a binary arithmetic operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param opCode The opcode to insert in the generated [FloatArray] if both sources aren't a const
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
    return RemoteFloatExpression(a.hasConstantValue && b.hasConstantValue) { creationState ->
        val aArray = a.arrayForCreationState(creationState)
        val bArray = b.arrayForCreationState(creationState)
        if (aArray.isLiteral() && bArray.isLiteral()) {
            // A and b are both constants so we can evaluate directly.
            floatArrayOf(directEval(aArray[0], bArray[0]))
        } else {
            // Either a or b isn't a constant so we need to evaluate dynamically.
            floatArrayOf(*aArray, *bArray, opCode)
        }
    }
}

/**
 * Boilerplate for implementing a binary comparison operation.
 *
 * @param a The left hand side value of the binary operation
 * @param b The right hand side value of the binary operation
 * @param expressionGenerator Generator for the comparison expression [FloatArray] used when both
 *   sources aren't a const float.
 * @param directEval When the sources are const float, this lambda will be called to evaluate the
 *   result directly.
 */
internal fun comparisonOp(
    a: RemoteFloat,
    b: RemoteFloat,
    expressionGenerator: (FloatArray, FloatArray) -> FloatArray,
    directEval: (Float, Float) -> Long,
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
                val id =
                    creationState.document.floatExpression(*expressionGenerator(aArray, bArray))
                longArrayOf(0x100000000 + Utils.idFromNan(id).toLong())
            }
        }
    )

/** Returns [ifTrue] if [a] < [b], otherwise returns [ifFalse]. */
fun selectIfLT(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat =
    RemoteFloatExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        floatArrayOf(
            *ifFalse.arrayForCreationState(creationState),
            *ifTrue.arrayForCreationState(creationState),
            *b.arrayForCreationState(creationState),
            *a.arrayForCreationState(creationState),
            SUB,
            IFELSE,
        )
    }

/** Returns [ifTrue] if [a] <= [b], otherwise returns [ifFalse]. */
fun selectIfLE(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat =
    RemoteFloatExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        floatArrayOf(
            *ifTrue.arrayForCreationState(creationState),
            *ifFalse.arrayForCreationState(creationState),
            *a.arrayForCreationState(creationState),
            *b.arrayForCreationState(creationState),
            SUB,
            IFELSE,
        )
    }

/** Returns [ifTrue] if [a] > [b], otherwise returns [ifFalse]. */
fun selectIfGT(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat =
    RemoteFloatExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        floatArrayOf(
            *ifFalse.arrayForCreationState(creationState),
            *ifTrue.arrayForCreationState(creationState),
            *a.arrayForCreationState(creationState),
            *b.arrayForCreationState(creationState),
            SUB,
            IFELSE,
        )
    }

/** Returns [ifTrue] if [a] >= [b], otherwise returns [ifFalse]. */
fun selectIfGE(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
): RemoteFloat =
    RemoteFloatExpression(
        a.hasConstantValue &&
            b.hasConstantValue &&
            ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue
    ) { creationState ->
        floatArrayOf(
            *ifTrue.arrayForCreationState(creationState),
            *ifFalse.arrayForCreationState(creationState),
            *b.arrayForCreationState(creationState),
            *a.arrayForCreationState(creationState),
            SUB,
            IFELSE,
        )
    }

/**
 * The difference between now and [referenceEpochMillis] in seconds.
 *
 * @param referenceEpochMillis The reference time in milliseconds since the epoch in the system
 *   default locale.
 * @return A [RemoteFloat] that evaluates to the difference between now and [referenceEpochMillis]
 *   in seconds.
 */
fun deltaFromReferenceInSeconds(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun deltaFromReferenceInMinutes(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun deltaFromReferenceInHours(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun timeOfReferenceInSeconds(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun timeOfReferenceInMinutes(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun timeOfReferenceInHours(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun dayOfMonthForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun monthOfYearForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun dayOfWeekForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
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
fun yearForReference(referenceEpochMillis: RemoteLong): RemoteFloat {
    return RemoteFloatExpression(false) { creationState ->
        floatArrayOf(
            creationState.document.timeAttribute(
                referenceEpochMillis.getIdForCreationState(creationState),
                TimeAttribute.TIME_YEAR,
            )
        )
    }
}

/**
 * A mutable implementation of [RemoteFloat] that holds a [MutableFloatState]. It also implements
 * [MutableRemoteState<Float>].
 *
 * @property content The underlying [MutableFloatState] that holds the actual float value.
 * @property idProvider A lambda that provides the ID for this mutable float within the
 *   [RemoteComposeCreationState]. Defaults to reserving a new float variable ID if not provided.
 */
class MutableRemoteFloat(
    private val content: MutableFloatState,
    private var idProvider: (creationState: RemoteComposeCreationState) -> Float,
) : RemoteFloat(true), MutableRemoteState<Float> {
    constructor(
        content: MutableFloatState,
        id: Float? = null,
    ) : this(content, { creationState -> id ?: creationState.document.reserveFloatVariable() })

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState -> floatArrayOf(idProvider(creationState)) }

    override var value: Float
        get() {
            return content.floatValue
        }
        set(newValue) {
            content.floatValue = newValue
        }

    override operator fun component1(): Float = value

    override operator fun component2(): (Float) -> Unit = { newValue ->
        content.floatValue = newValue
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState) =
        Utils.idFromNan(idProvider(creationState))
}

/**
 * An implementation of [RemoteFloat] that represents a float expression.
 *
 * @property hasConstantValue Indicates if this expression will always yield the same value.
 * @property arrayProvider A lambda that provides the [FloatArray] representing the expression.
 */
class RemoteFloatExpression
internal constructor(
    hasConstantValue: Boolean,
    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
) : RemoteFloat(hasConstantValue) {

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val array = arrayForCreationState(creationState)
        // In case we have a single element array, check if the element is an id or not;
        // if it is an existing id, just return this one, no need to create a new one...
        if (array.size == 1 && array[0].isNaN()) {
            return Utils.idFromNan(array[0])
        }

        val hash = calcHashID(array, null)
        val fe = creationState.expressionCache.get(hash)
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

    override val value: Float
        get() = TODO("Implement expression evaluation")

    override val id: Float
        get(): Float {
            // Some of the callers expect RemoteFloat(123) to return 123 from this method.
            val array = arrayForCreationState(FallbackCreationState.state)
            if (array.size == 1) {
                return array[0]
            }
            return getFloatIdForCreationState(FallbackCreationState.state)
        }
}

/**
 * Represents an animated remote float. It combines an input [RemoteFloat] with an animation
 * definition ([FloatArray]).
 *
 * @property input The base [RemoteFloat] that is being animated.
 * @property anim The [FloatArray] defining the animation (e.g., keyframes, easing parameters).
 */
class AnimatedRemoteFloat(val input: RemoteFloat, val anim: FloatArray) : RemoteFloat(false) {
    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
        get() = { creationState -> floatArrayOf(getFloatIdForCreationState(creationState)) }

    val easing = FloatAnimation(*anim)
    val exp = AnimatedFloatExpression()

    var lastValue: Float = Float.NaN
    var lastChanged: Float = Float.NaN
    val start = System.nanoTime()

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val array = input.arrayForCreationState(creationState)
        val hash = calcHashID(array, anim)
        val fe = creationState.expressionCache.get(hash)
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

    override val value: Float
        get() = TODO("Not yet implemented")

    fun getAnimationTime(): Float {
        return (System.nanoTime() - start) * 1E-9f
    }

    fun getEasedFloat(array: FloatArray): Float {
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

const val CUBIC_STANDARD = 1
const val CUBIC_ACCELERATE = 2
const val CUBIC_DECELERATE = 3
const val CUBIC_LINEAR = 4
const val CUBIC_ANTICIPATE = 5
const val CUBIC_OVERSHOOT = 6
const val CUBIC_CUSTOM = 11
const val SPLINE_CUSTOM = 12
const val EASE_OUT_BOUNCE = 13
const val EASE_OUT_ELASTIC = 14

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
public annotation class AnimationType

/**
 * Converts a [Number] to a [FloatArray]. If the number is a [RemoteFloat], its
 * `arrayForCreationState` is used. Otherwise, it's converted to a single-element [FloatArray].
 *
 * @param a The number to convert.
 * @return A [FloatArray] representation of the number.
 */
fun toArray(a: Number): FloatArray =
    when (a) {
        is RemoteFloat -> a.arrayForCreationState(FallbackCreationState.state)
        is Float -> floatArrayOf(a)
        else -> floatArrayOf(a.toFloat())
    }

/**
 * Converts a [Number] to a [FloatArray] using a specific [RemoteComposeCreationState]. If the
 * number is a [RemoteFloat], its `arrayForCreationState` is used. Otherwise, it's converted to a
 * single-element [FloatArray].
 *
 * @param a The number to convert.
 * @param creationState The [RemoteComposeCreationState] to use for conversion.
 * @return A [FloatArray] representation of the number.
 */
fun toArray(a: Number, creationState: RemoteComposeCreationState): FloatArray =
    when (a) {
        is RemoteFloat -> a.arrayForCreationState(creationState)
        is Float -> floatArrayOf(a)
        else -> floatArrayOf(a.toFloat())
    }

/**
 * Composable function to remember and provide a [RemoteFloat] from a [FloatArray]. This is intended
 * for use within a `@Composable` context.
 *
 * @param content A lambda that provides the [FloatArray] to be remembered.
 * @return A [RemoteFloat] representing the remembered float array.
 */
@Composable
@RemoteComposable
fun rememberRemoteFloatArray(content: () -> FloatArray): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    var floatArrayId = 0f
    if (state !is NoRemoteCompose) {
        floatArrayId = state.document.addFloatArray(content())
    }
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
fun rememberRemoteFloatValue(content: RemoteFloatContext.() -> Float): MutableRemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val context = RemoteFloatContext(state)
        val value = content(context)
        val id = state.document.addFloatConstant(value)
        MutableRemoteFloat(content = mutableFloatStateOf(value), id)
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
fun rememberRemoteFloat(content: () -> RemoteFloat): RemoteFloat {
    val state = LocalRemoteComposeCreationState.current
    val remoteFloat = content()
    remoteFloat.getIdForCreationState(state)
    return remember {
        RemoteFloatExpression(remoteFloat.hasConstantValue, remoteFloat.arrayProvider)
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
fun remoteFloat(
    state: RemoteComposeCreationState,
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
fun updateTime(array: FloatArray): FloatArray {
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
fun isTimeVar(fl: Float): Boolean {
    return RemoteContext.isTime(fl)
}

/**
 * Converts a [FloatArray] expression to a human-readable string representation. It includes labels
 * for NaN values, which typically represent IDs.
 *
 * @param array The [FloatArray] representing the expression.
 * @return A string representation of the expression.
 */
fun toString(array: FloatArray): String {
    val labels = arrayOfNulls<String>(array.size)
    for (i in array.indices) {
        if (java.lang.Float.isNaN(array[i])) {
            labels[i] = "[" + Utils.idStringFromNan(array[i]) + "]"
        }
    }
    return AnimatedFloatExpression.toString(array, labels)
}
