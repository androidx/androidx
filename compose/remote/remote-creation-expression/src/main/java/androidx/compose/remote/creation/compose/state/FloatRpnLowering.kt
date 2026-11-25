/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY3
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY32
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY4
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_COMMA_PERIOD
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_PERIOD_COMMA
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_SPACE_COMMA
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_UNDER_PERIOD
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.StringUtils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.document.NanEncoding

internal const val MAX_SAFE_FLOAT_ARRAY = 30

/**
 * Unary [RemoteFloat] AST operation node retaining its typed [operand] for optimization and RPN
 * lowering.
 */
internal class UnaryRemoteFloatOp(
    val op: RemoteFloatOperationKey,
    val operand: RemoteFloat,
    val opCode: Float = op.opCode,
) : BaseRemoteFloat(RemoteOperationCacheKey.create(op, operand)) {
    override val constantValueOrNull: Float?
        get() = null

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray =
        if (op == RemoteFloatOperationKey.UnaryMinus) {
            { creationState -> lowerFloatNegation(creationState, operand) }
        } else {
            { creationState -> combineToFloatArray(creationState, arrayOf(operand), opCode) }
        }
}

/**
 * Lowers unary negation of [operand] to RPN [FloatArray] bytecode.
 *
 * Since float RPN lacks a dedicated unary-negate opcode, negation normally emits `[operand, -1f,
 * MUL]`:
 * * If [operand] is an unshared [BinaryRemoteFloatOp] multiplying or dividing by a constant `c` (`x
 *     * c` or `x / c` where [hasBeenWrittenToDoc] is `false`), folds the sign directly into `-c`
 *       (or returns `x`'s array directly when `-c == 1f`).
 * * Otherwise, emits `[operand, -1f, AnimatedFloatExpression.MUL]` via [combineToFloatArray].
 *
 * @param creationState state resolving variable IDs and cached RPN arrays
 * @param operand expression to negate
 * @return RPN [FloatArray] evaluating to `-[operand]`
 */
internal fun lowerFloatNegation(
    creationState: RemoteComposeCreationState,
    operand: RemoteFloat,
): FloatArray {
    if (
        operand is BinaryRemoteFloatOp &&
            (operand.op == RemoteFloatOperationKey.Times ||
                operand.op == RemoteFloatOperationKey.Div) &&
            !operand.hasBeenWrittenToDoc(creationState)
    ) {
        val c = operand.right.constantValueOrNull
        if (c != null) {
            val negC = -c
            return if (negC == 1f) {
                operand.left.arrayForCreationState(creationState)
            } else {
                combineToFloatArray(creationState, arrayOf(operand.left), negC, operand.opCode)
            }
        }
    }
    return combineToFloatArray(
        creationState,
        arrayOf(operand),
        -1f,
        AnimatedFloatExpression.MUL,
    )
}

/**
 * Binary [RemoteFloat] AST operation node retaining typed [left] and [right] operands for
 * optimization and RPN lowering.
 */
internal class BinaryRemoteFloatOp(
    val op: RemoteFloatOperationKey,
    val left: RemoteFloat,
    val right: RemoteFloat,
    val opCode: Float = op.opCode,
) : BaseRemoteFloat(RemoteOperationCacheKey.create(op, left, right)) {
    override val constantValueOrNull: Float?
        get() = null

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray =
        { creationState ->
            val rConst = right.constantValueOrNull
            if (rConst != null) {
                combineToFloatArray(creationState, arrayOf(left), rConst, opCode)
            } else {
                combineToFloatArray(creationState, arrayOf(left, right), opCode)
            }
        }
}

/** Comparison operators for evaluating [RemoteFloat] select conditions. */
internal enum class FloatComparisonOp {
    LT,
    LE,
    GT,
    GE,
    EQ,
    NE,
}

/** Condition determining branch selection in a [RemoteFloatSelect]. */
internal sealed class SelectFloatCondition {
    /**
     * Builds the [FloatArray] bytecode for selecting between [ifFalse] and [ifTrue].
     *
     * @param creationState creation state used to resolve variable IDs
     * @param ifFalse value selected when the condition evaluates to false
     * @param ifTrue value selected when the condition evaluates to true
     * @return float array encoding the selection expression in RPN
     */
    abstract fun buildFloatArray(
        creationState: RemoteComposeCreationState,
        ifFalse: RemoteFloat,
        ifTrue: RemoteFloat,
    ): FloatArray

    /**
     * Constructs a canonical [RemoteFloat] selecting between [ifTrue] and [ifFalse] under this
     * condition.
     */
    abstract fun createSelect(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat

    /**
     * Direct comparison between two [RemoteFloat] instances.
     *
     * @property a left-hand operand
     * @property b right-hand operand
     * @property op comparison operator
     */
    data class FloatComparison(
        val a: RemoteFloat,
        val b: RemoteFloat,
        val op: FloatComparisonOp,
    ) : SelectFloatCondition() {
        override fun createSelect(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat {
            if (ifTrue.cacheKey == ifFalse.cacheKey) {
                return ifTrue
            }
            val key =
                when (op) {
                    FloatComparisonOp.LT ->
                        RemoteOperationCacheKey.create(
                            RemoteFloatOperationKey.SelectIfLT,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    FloatComparisonOp.LE ->
                        RemoteOperationCacheKey.create(
                            RemoteFloatOperationKey.SelectIfLE,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    FloatComparisonOp.GT ->
                        RemoteOperationCacheKey.create(
                            RemoteFloatOperationKey.SelectIfGT,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    FloatComparisonOp.GE ->
                        RemoteOperationCacheKey.create(
                            RemoteFloatOperationKey.SelectIfGE,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    FloatComparisonOp.EQ ->
                        RemoteOperationCacheKey.create(
                            BooleanOperationKey.SelectFloat,
                            a.isEqualTo(b),
                            ifTrue,
                            ifFalse,
                        )
                    FloatComparisonOp.NE ->
                        RemoteOperationCacheKey.create(
                            BooleanOperationKey.SelectFloat,
                            a.isNotEqualTo(b),
                            ifTrue,
                            ifFalse,
                        )
                }
            return RemoteFloatSelect(
                condition = this,
                ifTrue = ifTrue,
                ifFalse = ifFalse,
                cacheKey = key,
            )
        }

        override fun buildFloatArray(
            creationState: RemoteComposeCreationState,
            ifFalse: RemoteFloat,
            ifTrue: RemoteFloat,
        ): FloatArray =
            when (op) {
                FloatComparisonOp.LT ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.GT ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, a, b),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.LE ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, a, b),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.GE ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.EQ ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.ABS,
                        AnimatedFloatExpression.IFELSE,
                    )
                FloatComparisonOp.NE ->
                    combineToFloatArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, b, a),
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.ABS,
                        AnimatedFloatExpression.IFELSE,
                    )
            }
    }

    /**
     * Boolean condition wrapping a [RemoteBoolean].
     *
     * @property bool boolean expression driving the branch selection
     */
    data class BooleanCondition(val bool: RemoteBoolean) : SelectFloatCondition() {
        override fun createSelect(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat {
            if (ifTrue.cacheKey == ifFalse.cacheKey) {
                return ifTrue
            }
            return RemoteFloatSelect(
                condition = this,
                ifTrue = ifTrue,
                ifFalse = ifFalse,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        BooleanOperationKey.SelectFloat,
                        bool,
                        ifTrue,
                        ifFalse,
                    ),
            )
        }

        override fun buildFloatArray(
            creationState: RemoteComposeCreationState,
            ifFalse: RemoteFloat,
            ifTrue: RemoteFloat,
        ): FloatArray =
            combineToFloatArray(
                creationState,
                arrayOf(ifFalse, ifTrue, bool.intValue.toRemoteFloat()),
                AnimatedFloatExpression.IFELSE,
            )
    }
}

internal typealias SealedFloatCondition = SelectFloatCondition

/**
 * Select expression choosing between [ifTrue] and [ifFalse] based on [condition].
 *
 * @property condition selection condition, either direct float comparison or a boolean
 * @property ifTrue value returned when [condition] evaluates to true
 * @property ifFalse value returned when [condition] evaluates to false
 */
internal class RemoteFloatSelect(
    val condition: SelectFloatCondition,
    val ifTrue: RemoteFloat,
    val ifFalse: RemoteFloat,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteFloat(cacheKey) {
    override val constantValueOrNull: Float?
        get() = null

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray =
        { creationState ->
            condition.buildFloatArray(creationState, ifFalse, ifTrue)
        }

    val hasConstantBranches: Boolean
        get() = ifTrue.constantValueOrNull != null && ifFalse.constantValueOrNull != null
}

internal fun RemoteFloat.writeFloatToDocument(creationState: RemoteComposeCreationState): Int {
    val array = arrayForCreationState(creationState)
    // In case we have a single element array, check if the element is a variable id
    // (and not a 0-argument math operator like RAND); if it is an existing id, just
    // return this one, no need to create a new floatExpression.
    if (
        array.size == 1 &&
            NanEncoding.isNan(array[0]) &&
            !AnimatedFloatExpression.isMathOperator(array[0])
    ) {
        return NanEncoding.idFromNan(array[0])
    }

    return creationState.getOrPutFloatExpressionId(array, animation = null) {
        creationState.document.addFloatExpression(*array)
    }
}

internal fun RemoteFloat.floatIdForCreationState(creationState: RemoteComposeCreationState): Float {
    constantValueOrNull?.let {
        return it
    }
    val array = arrayForCreationState(creationState)
    if (array.size == 1) {
        return array[0]
    }
    return NanEncoding.asNan(creationState.getId(this))
}

internal fun floatToString(v: Float, before: Short, after: Short, flags: Int): String =
    StringUtils.floatToString(
        v,
        before.toInt(),
        after.toInt(),
        when (flags and 12) {
            PAD_PRE_SPACE -> ' '
            PAD_PRE_NONE -> 0.toChar()
            PAD_PRE_ZERO -> '0'
            else -> ' '
        },
        when (flags and 3) {
            PAD_AFTER_SPACE -> ' '
            PAD_AFTER_NONE -> 0.toChar()
            PAD_AFTER_ZERO -> '0'
            else -> ' '
        },
        when (flags and (3 shl 6)) {
            SEPARATOR_PERIOD_COMMA -> StringUtils.SEPARATOR_PERIOD_COMMA
            SEPARATOR_COMMA_PERIOD -> StringUtils.SEPARATOR_COMMA_PERIOD
            SEPARATOR_SPACE_COMMA -> StringUtils.SEPARATOR_SPACE_COMMA
            SEPARATOR_UNDER_PERIOD -> StringUtils.SEPARATOR_UNDER_PERIOD
            else -> StringUtils.SEPARATOR_PERIOD_COMMA
        },
        when (flags and (3 shl 4)) {
            GROUPING_BY3 -> StringUtils.GROUPING_BY3
            GROUPING_BY4 -> StringUtils.GROUPING_BY4
            GROUPING_BY32 -> StringUtils.GROUPING_BY32
            else -> StringUtils.GROUPING_NONE
        },
        flags shr 8,
    )

/**
 * Constructs a [FloatArray] that either inlines or references the contents of [remoteFloats]
 * followed by [extras]. Inlining is preferred as long as the resulting array length is at most
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
            val remoteFloat = remoteFloats[i]
            // If remoteFloat has already been written to the document then use a reference
            // rather than inlining the expression. This results in smaller documents.
            if (remoteFloat.hasBeenWrittenToDoc(creationState)) {
                totalSizeInline += 1
                floatArrayOf(creationState.getFloatId(remoteFloat))
            } else {
                val array = remoteFloat.arrayForCreationState(creationState)
                totalSizeInline += array.size
                array
            }
        }

    val combinedArray: FloatArray
    var idx = 0

    if (totalSizeInline > MAX_SAFE_FLOAT_ARRAY) {
        // Add references for the RemoteFloat values.
        combinedArray = FloatArray(totalSizeReference)
        for (i in remoteFloats.indices) {
            combinedArray[i] = creationState.getFloatId(remoteFloats[i])
        }
        idx = remoteFloats.size
    } else {
        // Inline the RemoteFloat arrays.
        combinedArray = FloatArray(totalSizeInline)
        for (array in arrays) {
            array.copyInto(
                combinedArray,
                destinationOffset = idx,
                startIndex = 0,
                endIndex = array.size,
            )
            idx += array.size
        }
    }

    for (extra in extras) {
        combinedArray[idx++] = extra
    }

    return combinedArray
}

internal fun toArray(a: Number): FloatArray =
    when (a) {
        is Float -> floatArrayOf(a)
        else -> floatArrayOf(a.toFloat())
    }

internal fun toArray(a: RemoteFloat, creationState: RemoteComposeCreationState): FloatArray =
    a.arrayForCreationState(creationState)

internal fun isTimeVar(fl: Float): Boolean = RemoteContext.isTime(fl)

internal fun toString(array: FloatArray): String {
    val labels = arrayOfNulls<String>(array.size)
    for (i in array.indices) {
        if (NanEncoding.isNan(array[i])) {
            labels[i] = "[" + Utils.idString(NanEncoding.idFromNan(array[i])) + "]"
        }
    }
    return AnimatedFloatExpression.toString(array, labels)
}

/** An inline value class representing a reference to a remote float. */
@JvmInline internal value class RemoteFloatReference(private val v: Float)

/** Extension function to get either a Float ID or a Float literal from a [Number]. */
internal fun Number.getFloatId(creationState: RemoteComposeCreationState): Float =
    when (this) {
        is Float -> this
        else -> toFloat()
    }
