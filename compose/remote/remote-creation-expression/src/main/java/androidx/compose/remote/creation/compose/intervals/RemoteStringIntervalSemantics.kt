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

package androidx.compose.remote.creation.compose.intervals

import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.creation.compose.state.StringOperationKey
import androidx.compose.remote.document.TextTransformOperation
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.StringAndColorIntervalOps
import androidx.compose.remote.document.intervals.StringSummary
import androidx.compose.remote.document.intervals.TriState

/** External interval semantics for [StringOperationKey]. */
internal object RemoteStringIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun selectCondition(op: Any): IntervalComparison? =
        when (op as StringOperationKey) {
            StringOperationKey.SelectIfLT -> IntervalComparison.LT
            StringOperationKey.SelectIfLE -> IntervalComparison.LTE
            StringOperationKey.SelectIfGT -> IntervalComparison.GT
            StringOperationKey.SelectIfGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind {
        val stringOp = op as StringOperationKey
        if (selectCondition(stringOp) == null || args.size < 2) return RemoteValueKind.OTHER
        val k0 = args[0].getValueKind()
        val k1 = args[1].getValueKind()
        return when {
            (k0.isIntEncoded || k1.isIntEncoded) &&
                k0 != RemoteValueKind.FLOAT &&
                k1 != RemoteValueKind.FLOAT -> RemoteValueKind.INT
            k0 == RemoteValueKind.FLOAT || k1 == RemoteValueKind.FLOAT -> RemoteValueKind.FLOAT
            else -> RemoteValueKind.OTHER
        }
    }

    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval = evaluator.evalIntKey(key).toFloatInterval()

    override fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval =
        when (key.op as StringOperationKey) {
            StringOperationKey.Length ->
                if (key.args.isNotEmpty()) {
                    evaluator.evalStringKey(key.args[0]).length
                } else {
                    IntInterval.NON_NEGATIVE
                }
            StringOperationKey.IsEmpty -> evalIsEmpty(key.args, evaluator).toIntInterval()
            StringOperationKey.IsNotEmpty -> (!evalIsEmpty(key.args, evaluator)).toIntInterval()
            else -> IntInterval.UNBOUNDED
        }

    override fun evaluateBooleanInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState? =
        when (key.op as StringOperationKey) {
            StringOperationKey.IsEmpty -> evalIsEmpty(key.args, evaluator)
            StringOperationKey.IsNotEmpty -> !evalIsEmpty(key.args, evaluator)
            else -> null
        }

    override fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary {
        val op = key.op as StringOperationKey
        val args = key.args
        selectCondition(op)?.let { cond ->
            if (args.size < 4) return StringSummary.UNKNOWN
            val cmpKind = selectComparisonKind(op, args)
            val isIntCmp =
                when {
                    cmpKind.isIntEncoded -> true
                    cmpKind == RemoteValueKind.FLOAT -> false
                    else ->
                        return evaluator
                            .evalStringKey(args[2])
                            .union(evaluator.evalStringKey(args[3]))
                }
            val cmp =
                if (isIntCmp) {
                    evaluator.compareIntKeys(cond, args[0], args[1])
                } else {
                    evaluator.compareFloatKeys(cond, args[0], args[1])
                }
            return when (cmp) {
                TriState.TRUE -> evaluator.evalStringKey(args[2])
                TriState.FALSE -> evaluator.evalStringKey(args[3])
                TriState.UNKNOWN ->
                    evaluator
                        .evalComparisonBranch(
                            isInt = isIntCmp,
                            comparison = cond,
                            keyA = args[0],
                            keyB = args[1],
                            holds = true,
                        ) {
                            it.evalStringKey(args[2])
                        }
                        .union(
                            evaluator.evalComparisonBranch(
                                isInt = isIntCmp,
                                comparison = cond,
                                keyA = args[0],
                                keyB = args[1],
                                holds = false,
                            ) {
                                it.evalStringKey(args[3])
                            }
                        )
            }
        }
        return when (op) {
            StringOperationKey.Concat ->
                if (args.size >= 2) {
                    StringAndColorIntervalOps.summarizeTextMerge(
                        evaluator.evalStringKey(args[0]),
                        evaluator.evalStringKey(args[1]),
                    )
                } else {
                    StringSummary.UNKNOWN
                }
            StringOperationKey.Substring ->
                when (args.size) {
                    2 ->
                        StringAndColorIntervalOps.summarizeSubstring(
                            src = evaluator.evalStringKey(args[0]),
                            start = evaluator.evalIntKey(args[1]).toFloatInterval(),
                            len = FloatInterval.point(-1f),
                        )
                    3 -> {
                        val startInt = evaluator.evalIntKey(args[1])
                        val lenInt = evaluator.evalIntKey(args[2]) - startInt
                        StringAndColorIntervalOps.summarizeSubstring(
                            src = evaluator.evalStringKey(args[0]),
                            start = startInt.toFloatInterval(),
                            len = lenInt.toFloatInterval(),
                        )
                    }
                    else -> StringSummary.UNKNOWN
                }
            StringOperationKey.Uppercase ->
                evalCaseOrTrim(args, evaluator, TextTransformOperation.TO_UPPERCASE)
            StringOperationKey.Lowercase ->
                evalCaseOrTrim(args, evaluator, TextTransformOperation.TO_LOWERCASE)
            StringOperationKey.Trim -> evalCaseOrTrim(args, evaluator, TextTransformOperation.TRIM)
            else -> StringSummary.UNKNOWN
        }
    }

    private fun evalIsEmpty(
        args: List<RemoteStateCacheKey>,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState {
        if (args.isEmpty()) return TriState.UNKNOWN
        val len = evaluator.evalStringKey(args[0]).length
        return when {
            len.isEmpty -> TriState.UNKNOWN
            len.max == 0 -> TriState.TRUE
            len.min > 0 -> TriState.FALSE
            else -> TriState.UNKNOWN
        }
    }

    private fun evalCaseOrTrim(
        args: List<RemoteStateCacheKey>,
        evaluator: RemoteValueIntervalEvaluator,
        operation: Int,
    ): StringSummary {
        if (args.isEmpty()) return StringSummary.UNKNOWN
        return StringAndColorIntervalOps.summarizeTextTransform(
            src = evaluator.evalStringKey(args[0]),
            start = FloatInterval.ZERO,
            len = FloatInterval.point(-1f),
            operation = operation,
        )
    }
}
