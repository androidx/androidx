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

import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.document.NanEncoding

internal const val OP_ABS = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_ABS
internal const val OP_ADD = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_ADD
internal const val OP_AND = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_AND
internal const val OP_CLAMP = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_CLAMP
internal const val OP_COPY_SIGN =
    NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_COPY_SIGN
internal const val OP_DIV = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_DIV
internal const val OP_IFELSE = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_IFELSE
internal const val OP_MAX = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_MAX
internal const val OP_MIN = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_MIN
internal const val OP_MOD = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_MOD
internal const val OP_MUL = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_MUL
internal const val OP_NEG = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_NEG
internal const val OP_NOT = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_NOT
internal const val OP_OR = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_OR
internal const val OP_SUB = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_SUB
internal const val OP_SHL = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_SHL
internal const val OP_SHR = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_SHR
internal const val OP_XOR = NanEncoding.LONG_ID_OFFSET + IntegerExpressionEvaluator.I_XOR

internal const val MAX_SAFE_LONG_ARRAY = 30

/**
 * Unary [RemoteInt] AST operation node retaining its typed [operand] for optimization and RPN
 * lowering.
 */
internal class UnaryRemoteIntOp(
    val op: RemoteIntOperationKey,
    val operand: RemoteInt,
    val opCode: Long = op.opCode,
) : BaseRemoteInt(RemoteOperationCacheKey.create(op, operand)) {
    override val constantValueOrNull: Int?
        get() = null

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray =
        { creationState ->
            combineToLongArray(creationState, arrayOf(operand), opCode)
        }
}

/**
 * Binary [RemoteInt] AST operation node retaining typed [left] and [right] operands for
 * optimization and RPN lowering.
 */
internal class BinaryRemoteIntOp(
    val op: RemoteIntOperationKey,
    val left: RemoteInt,
    val right: RemoteInt,
    val opCode: Long = op.opCode,
) : BaseRemoteInt(RemoteOperationCacheKey.create(op, left, right)) {
    override val constantValueOrNull: Int?
        get() = null

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray =
        { creationState ->
            val rConst = right.constantValueOrNull
            if (rConst != null) {
                combineToLongArray(creationState, arrayOf(left), rConst.toLong(), opCode)
            } else {
                combineToLongArray(creationState, arrayOf(left, right), opCode)
            }
        }
}

/** Comparison operators for evaluating [RemoteInt] select conditions. */
internal enum class IntComparisonOp {
    LT,
    LE,
    GT,
    GE,
    EQ,
    NE,
}

/** Condition determining branch selection in a [RemoteIntSelect]. */
internal sealed class SelectIntCondition {
    /**
     * Builds the [LongArray] bytecode for selecting between [ifFalse] and [ifTrue].
     *
     * @param creationState creation state used to resolve variable IDs
     * @param ifFalse value selected when the condition evaluates to false
     * @param ifTrue value selected when the condition evaluates to true
     * @return long array encoding the selection expression in RPN
     */
    abstract fun buildLongArray(
        creationState: RemoteComposeCreationState,
        ifFalse: RemoteInt,
        ifTrue: RemoteInt,
    ): LongArray

    /**
     * Checks whether this condition always selects the same branch within a frame.
     *
     * Returns `false` when an operand derives from a non-deterministic expression—for example,
     * `selectIfLt((rand() * 10f).toRemoteInt(), 5.ri, ...)` or `((rand() *
     * 10f).toRemoteInt().isLessThan(5.ri)).select(...)`, where merging two selects would collapse
     * independent random draws into one.
     *
     * @return `true` if all operand cache keys are deterministic
     */
    abstract fun isDeterministic(): Boolean

    /**
     * Constructs a canonical [RemoteInt] selecting between [ifTrue] and [ifFalse] under this
     * condition.
     */
    abstract fun createSelect(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt

    /**
     * Direct comparison between two [RemoteInt] instances.
     *
     * @property a left-hand operand
     * @property b right-hand operand
     * @property op comparison operator
     */
    data class IntComparison(
        val a: RemoteInt,
        val b: RemoteInt,
        val op: IntComparisonOp,
    ) : SelectIntCondition() {
        override fun isDeterministic(): Boolean =
            a.cacheKey.isDeterministic() && b.cacheKey.isDeterministic()

        override fun createSelect(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt {
            if (ifTrue.cacheKey == ifFalse.cacheKey) {
                return ifTrue
            }
            val key =
                when (op) {
                    IntComparisonOp.LT ->
                        RemoteOperationCacheKey.create(
                            RemoteIntOperationKey.SelectIfLT,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    IntComparisonOp.LE ->
                        RemoteOperationCacheKey.create(
                            RemoteIntOperationKey.SelectIfLE,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    IntComparisonOp.GT ->
                        RemoteOperationCacheKey.create(
                            RemoteIntOperationKey.SelectIfGT,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    IntComparisonOp.GE ->
                        RemoteOperationCacheKey.create(
                            RemoteIntOperationKey.SelectIfGE,
                            a,
                            b,
                            ifTrue,
                            ifFalse,
                        )
                    IntComparisonOp.EQ ->
                        RemoteOperationCacheKey.create(
                            BooleanOperationKey.SelectInt,
                            a.isEqualTo(b),
                            ifTrue,
                            ifFalse,
                        )
                    IntComparisonOp.NE ->
                        RemoteOperationCacheKey.create(
                            BooleanOperationKey.SelectInt,
                            a.isNotEqualTo(b),
                            ifTrue,
                            ifFalse,
                        )
                }
            return RemoteIntSelect(
                condition = this,
                ifTrue = ifTrue,
                ifFalse = ifFalse,
                cacheKey = key,
            )
        }

        override fun buildLongArray(
            creationState: RemoteComposeCreationState,
            ifFalse: RemoteInt,
            ifTrue: RemoteInt,
        ): LongArray =
            when (op) {
                IntComparisonOp.LT ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, b, a),
                        OP_SUB,
                        OP_IFELSE,
                    )
                IntComparisonOp.GT ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, a, b),
                        OP_SUB,
                        OP_IFELSE,
                    )
                IntComparisonOp.LE ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, a, b),
                        OP_SUB,
                        OP_IFELSE,
                    )
                IntComparisonOp.GE ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, b, a),
                        OP_SUB,
                        OP_IFELSE,
                    )
                IntComparisonOp.EQ ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifTrue, ifFalse, b, a),
                        OP_SUB,
                        OP_ABS,
                        OP_IFELSE,
                    )
                IntComparisonOp.NE ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifFalse, ifTrue, b, a),
                        OP_SUB,
                        OP_ABS,
                        OP_IFELSE,
                    )
            }
    }

    /**
     * Boolean condition wrapping a [RemoteBoolean].
     *
     * @property bool boolean expression driving the branch selection
     */
    data class BooleanCondition(val bool: RemoteBoolean) : SelectIntCondition() {
        override fun isDeterministic(): Boolean = bool.cacheKey.isDeterministic()

        override fun createSelect(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt {
            if (ifTrue.cacheKey == ifFalse.cacheKey) {
                return ifTrue
            }
            return RemoteIntSelect(
                condition = this,
                ifTrue = ifTrue,
                ifFalse = ifFalse,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        BooleanOperationKey.SelectInt,
                        bool,
                        ifTrue,
                        ifFalse,
                    ),
            )
        }

        override fun buildLongArray(
            creationState: RemoteComposeCreationState,
            ifFalse: RemoteInt,
            ifTrue: RemoteInt,
        ): LongArray =
            combineToLongArray(creationState, arrayOf(ifFalse, ifTrue, bool.intValue), OP_IFELSE)
    }
}

internal typealias SealedIntCondition = SelectIntCondition

/**
 * Select expression choosing between [ifTrue] and [ifFalse] based on [condition].
 *
 * @property condition selection condition, either direct integer comparison or a boolean
 * @property ifTrue value returned when [condition] evaluates to true
 * @property ifFalse value returned when [condition] evaluates to false
 */
internal class RemoteIntSelect(
    val condition: SelectIntCondition,
    val ifTrue: RemoteInt,
    val ifFalse: RemoteInt,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteInt(cacheKey) {
    override val constantValueOrNull: Int?
        get() = null

    override val arrayProvider: (creationState: RemoteComposeCreationState) -> LongArray =
        { creationState ->
            condition.buildLongArray(creationState, ifFalse, ifTrue)
        }

    val hasConstantBranches: Boolean
        get() = ifTrue.constantValueOrNull != null && ifFalse.constantValueOrNull != null
}

internal fun RemoteInt.writeIntToDocument(creationState: RemoteComposeCreationState): Int {
    val array = arrayForCreationState(creationState)

    // In case we have a single element array, check if the element is an id or not;
    // if it is an existing id, just return this one, no need to create a new one.
    if (
        array.size == 1 && NanEncoding.isLongId(array[0]) && array[0] > NanEncoding.LONG_ID_OFFSET
    ) {
        return NanEncoding.decodeLongId(array[0])
    }
    return creationState.getOrPutIntExpressionId(array) {
        creationState.document.addIntegerExpression(*array)
    }
}

/**
 * Constructs a [LongArray] that either inlines or references the contents of [remoteInts] followed
 * by [extras]. Inlining is preferred as long as the resulting array length is at most
 * [MAX_SAFE_LONG_ARRAY].
 */
internal fun combineToLongArray(
    creationState: RemoteComposeCreationState,
    remoteInts: Array<RemoteInt>,
    vararg extras: Long,
): LongArray {
    var totalSizeInline = extras.size
    val totalSizeReference = extras.size + remoteInts.size
    val arrays =
        Array<LongArray>(remoteInts.size) { i ->
            val remoteInt = remoteInts[i]
            // If remoteInt has already been written to the document then use a reference
            // rather than inlining the expression. This results in smaller documents.
            if (remoteInt.hasBeenWrittenToDoc(creationState)) {
                totalSizeInline += 1
                longArrayOf(creationState.getLongId(remoteInt))
            } else {
                val array = remoteInt.arrayForCreationState(creationState)
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
            combinedArray[i] = creationState.getLongId(remoteInts[i])
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
