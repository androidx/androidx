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

import androidx.compose.remote.creation.compose.state.BooleanOperationKey
import androidx.compose.remote.creation.compose.state.RemoteNamedCacheKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.StringSummary
import androidx.compose.remote.document.intervals.TriState
import kotlin.math.min

/** External interval semantics for [BooleanOperationKey]. */
internal object RemoteBooleanIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun isBooleanSelect(op: Any): Boolean =
        when (op as BooleanOperationKey) {
            BooleanOperationKey.SelectString,
            BooleanOperationKey.SelectFloat,
            BooleanOperationKey.SelectInt,
            BooleanOperationKey.SelectBoolean -> true
            else -> false
        }

    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval {
        val op = key.op as BooleanOperationKey
        val args = key.args
        return when (op) {
            BooleanOperationKey.SelectFloat ->
                if (args.size < 3) {
                    FloatInterval.UNBOUNDED
                } else {
                    when (evaluator.evalBoolKey(args[0])) {
                        TriState.TRUE -> evaluator.evalFloatKey(args[1])
                        TriState.FALSE -> evaluator.evalFloatKey(args[2])
                        TriState.UNKNOWN ->
                            evaluator
                                .evalBooleanBranch(args[0], holds = true) {
                                    it.evalFloatKey(args[1])
                                }
                                .union(
                                    evaluator.evalBooleanBranch(args[0], holds = false) {
                                        it.evalFloatKey(args[2])
                                    }
                                )
                    }
                }
            BooleanOperationKey.Not,
            BooleanOperationKey.Equal,
            BooleanOperationKey.NotEqual,
            BooleanOperationKey.And,
            BooleanOperationKey.Or,
            BooleanOperationKey.Xor,
            BooleanOperationKey.ToRemoteInt,
            BooleanOperationKey.RemoteBoolean,
            BooleanOperationKey.SelectInt,
            BooleanOperationKey.SelectBoolean -> evaluator.evalIntKey(key).toFloatInterval()
            BooleanOperationKey.SelectString -> FloatInterval.UNBOUNDED
        }
    }

    override fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval {
        val op = key.op as BooleanOperationKey
        val args = key.args
        return when (op) {
            BooleanOperationKey.SelectInt,
            BooleanOperationKey.SelectBoolean ->
                if (args.size < 3) {
                    IntInterval.UNBOUNDED
                } else {
                    when (evaluator.evalBoolKey(args[0])) {
                        TriState.TRUE -> evaluator.evalIntKey(args[1])
                        TriState.FALSE -> evaluator.evalIntKey(args[2])
                        TriState.UNKNOWN ->
                            evaluator
                                .evalBooleanBranch(args[0], holds = true) { it.evalIntKey(args[1]) }
                                .union(
                                    evaluator.evalBooleanBranch(args[0], holds = false) {
                                        it.evalIntKey(args[2])
                                    }
                                )
                    }
                }
            BooleanOperationKey.ToRemoteInt,
            BooleanOperationKey.RemoteBoolean ->
                if (args.isNotEmpty()) evaluator.evalIntKey(args[0]) else IntInterval.UNBOUNDED
            BooleanOperationKey.Equal,
            BooleanOperationKey.NotEqual -> evaluator.evalBoolKey(key).toIntInterval()
            BooleanOperationKey.Not ->
                if (args.isEmpty()) {
                    IntInterval.UNBOUNDED
                } else {
                    val a = evaluator.evalIntKey(args[0])
                    when {
                        a.isEmpty -> IntInterval.EMPTY
                        a.isPoint -> IntInterval.point(a.min xor 1)
                        a.min >= 0 && a.max <= 1 -> IntInterval(0, 1)
                        else -> IntInterval.UNBOUNDED
                    }
                }
            BooleanOperationKey.And ->
                if (args.size < 2) {
                    IntInterval.UNBOUNDED
                } else {
                    val a = evaluator.evalIntKey(args[0])
                    val b = evaluator.evalIntKey(args[1])
                    when {
                        a.isEmpty || b.isEmpty -> IntInterval.EMPTY
                        a == IntInterval.ZERO || b == IntInterval.ZERO -> IntInterval.ZERO
                        a.isPoint && b.isPoint -> IntInterval.point(a.min and b.min)
                        a.min >= 0 && b.min >= 0 && a.hasFiniteMax && b.hasFiniteMax ->
                            IntInterval(0, min(a.max, b.max))
                        b.min >= 0 && b.hasFiniteMax -> IntInterval(0, b.max)
                        a.min >= 0 && a.hasFiniteMax -> IntInterval(0, a.max)
                        else -> IntInterval.UNBOUNDED
                    }
                }
            BooleanOperationKey.Or ->
                if (args.size < 2) {
                    IntInterval.UNBOUNDED
                } else {
                    val a = evaluator.evalIntKey(args[0])
                    val b = evaluator.evalIntKey(args[1])
                    when {
                        a.isEmpty || b.isEmpty -> IntInterval.EMPTY
                        a.isPoint && b.isPoint -> IntInterval.point(a.min or b.min)
                        a.min >= 0 && a.max <= 1 && b.min >= 0 && b.max <= 1 ->
                            if (a.min == 1 || b.min == 1) IntInterval.point(1)
                            else IntInterval(0, 1)
                        else -> IntInterval.UNBOUNDED
                    }
                }
            BooleanOperationKey.Xor ->
                if (args.size < 2) {
                    IntInterval.UNBOUNDED
                } else {
                    val a = evaluator.evalIntKey(args[0])
                    val b = evaluator.evalIntKey(args[1])
                    when {
                        a.isEmpty || b.isEmpty -> IntInterval.EMPTY
                        a.isPoint && b.isPoint -> IntInterval.point(a.min xor b.min)
                        a.min >= 0 && a.max <= 1 && b.min >= 0 && b.max <= 1 -> IntInterval(0, 1)
                        else -> IntInterval.UNBOUNDED
                    }
                }
            BooleanOperationKey.SelectFloat,
            BooleanOperationKey.SelectString -> IntInterval.UNBOUNDED
        }
    }

    override fun evaluateBooleanInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState? {
        val op = key.op as BooleanOperationKey
        val args = key.args
        return when (op) {
            BooleanOperationKey.Not ->
                if (args.isEmpty()) {
                    TriState.UNKNOWN
                } else {
                    val aInt = evaluator.evalIntKey(args[0])
                    if (!aInt.isEmpty && aInt.min >= 0 && aInt.max <= 1) {
                        !evaluator.evalBoolKey(args[0])
                    } else {
                        null
                    }
                }
            BooleanOperationKey.Equal ->
                if (args.size < 2) {
                    TriState.UNKNOWN
                } else {
                    val direct = evaluator.compareIntKeys(IntervalComparison.EQ, args[0], args[1])
                    when {
                        direct != TriState.UNKNOWN -> direct
                        args[0].isDeterministic() && isBooleanComplementKeys(args[0], args[1]) ->
                            TriState.FALSE
                        else -> TriState.UNKNOWN
                    }
                }
            BooleanOperationKey.NotEqual ->
                if (args.size < 2) {
                    TriState.UNKNOWN
                } else {
                    val direct = evaluator.compareIntKeys(IntervalComparison.NEQ, args[0], args[1])
                    when {
                        direct != TriState.UNKNOWN -> direct
                        args[0].isDeterministic() && isBooleanComplementKeys(args[0], args[1]) ->
                            TriState.TRUE
                        else -> TriState.UNKNOWN
                    }
                }
            BooleanOperationKey.And ->
                if (args.size < 2) {
                    TriState.UNKNOWN
                } else {
                    val aInt = evaluator.evalIntKey(args[0])
                    val bInt = evaluator.evalIntKey(args[1])
                    if (aInt.isEmpty || bInt.isEmpty) {
                        TriState.UNKNOWN
                    } else {
                        val b0 = evaluator.evalBoolKey(args[0])
                        val b1 = evaluator.evalBoolKey(args[1])
                        val bothCanonical =
                            aInt.min >= 0 && aInt.max <= 1 && bInt.min >= 0 && bInt.max <= 1
                        when {
                            b0 == TriState.FALSE || b1 == TriState.FALSE -> TriState.FALSE
                            b0 == TriState.TRUE && b1 == TriState.TRUE && bothCanonical ->
                                TriState.TRUE
                            bothCanonical &&
                                b0 == TriState.UNKNOWN &&
                                b1 == TriState.UNKNOWN &&
                                evaluator.evalBooleanBranch(args[0], holds = true) {
                                    it.evalBoolKey(args[1])
                                } == TriState.FALSE -> TriState.FALSE
                            else -> null
                        }
                    }
                }
            BooleanOperationKey.Or ->
                if (args.size < 2) {
                    TriState.UNKNOWN
                } else {
                    val aInt = evaluator.evalIntKey(args[0])
                    val bInt = evaluator.evalIntKey(args[1])
                    if (aInt.isEmpty || bInt.isEmpty) {
                        TriState.UNKNOWN
                    } else {
                        val b0 = evaluator.evalBoolKey(args[0])
                        val b1 = evaluator.evalBoolKey(args[1])
                        val bothNonNeg =
                            isKnownNonNegativeBooleanOperand(args[0], aInt) &&
                                isKnownNonNegativeBooleanOperand(args[1], bInt)
                        val bothCanonical =
                            aInt.min >= 0 && aInt.max <= 1 && bInt.min >= 0 && bInt.max <= 1
                        when {
                            b0 == TriState.FALSE && b1 == TriState.FALSE -> TriState.FALSE
                            (b0 == TriState.TRUE || b1 == TriState.TRUE) && bothNonNeg ->
                                TriState.TRUE
                            bothCanonical &&
                                b0 == TriState.UNKNOWN &&
                                b1 == TriState.UNKNOWN &&
                                evaluator.evalBooleanBranch(args[0], holds = false) {
                                    it.evalBoolKey(args[1])
                                } == TriState.TRUE -> TriState.TRUE
                            else -> null
                        }
                    }
                }
            BooleanOperationKey.Xor ->
                if (args.size < 2) {
                    TriState.UNKNOWN
                } else {
                    val aInt = evaluator.evalIntKey(args[0])
                    val bInt = evaluator.evalIntKey(args[1])
                    if (aInt.isEmpty || bInt.isEmpty) {
                        TriState.UNKNOWN
                    } else if (aInt.min >= 0 && aInt.max <= 1 && bInt.min >= 0 && bInt.max <= 1) {
                        val b0 = evaluator.evalBoolKey(args[0])
                        val b1 = evaluator.evalBoolKey(args[1])
                        when {
                            b0 != TriState.UNKNOWN && b1 != TriState.UNKNOWN ->
                                TriState.of(b0 != b1)
                            args[0] == args[1] && args[0].isDeterministic() -> TriState.FALSE
                            args[0].isDeterministic() &&
                                isBooleanComplementKeys(args[0], args[1]) -> TriState.TRUE
                            else -> TriState.UNKNOWN
                        }
                    } else {
                        null
                    }
                }
            BooleanOperationKey.SelectBoolean ->
                if (args.size < 3) {
                    null
                } else {
                    when (evaluator.evalBoolKey(args[0])) {
                        TriState.TRUE -> evaluator.evalBoolKey(args[1])
                        TriState.FALSE -> evaluator.evalBoolKey(args[2])
                        TriState.UNKNOWN -> {
                            val t =
                                evaluator.evalBooleanBranch(args[0], holds = true) {
                                    it.evalBoolKey(args[1])
                                }
                            val f =
                                evaluator.evalBooleanBranch(args[0], holds = false) {
                                    it.evalBoolKey(args[2])
                                }
                            if (t == f) t else TriState.UNKNOWN
                        }
                    }
                }
            BooleanOperationKey.ToRemoteInt,
            BooleanOperationKey.RemoteBoolean,
            BooleanOperationKey.SelectString,
            BooleanOperationKey.SelectFloat,
            BooleanOperationKey.SelectInt -> null
        }
    }

    override fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary {
        if (key.op != BooleanOperationKey.SelectString || key.args.size < 3) {
            return StringSummary.UNKNOWN
        }
        val args = key.args
        return when (evaluator.evalBoolKey(args[0])) {
            TriState.TRUE -> evaluator.evalStringKey(args[1])
            TriState.FALSE -> evaluator.evalStringKey(args[2])
            TriState.UNKNOWN ->
                evaluator
                    .evalBooleanBranch(args[0], holds = true) { it.evalStringKey(args[1]) }
                    .union(
                        evaluator.evalBooleanBranch(args[0], holds = false) {
                            it.evalStringKey(args[2])
                        }
                    )
        }
    }

    private fun isKnownNonNegativeBooleanOperand(
        key: RemoteStateCacheKey,
        interval: IntInterval,
    ): Boolean {
        if (!interval.isEmpty && interval.min >= 0) return true
        return key is RemoteNamedCacheKey &&
            interval == IntInterval.UNBOUNDED &&
            key.getValueKind() == RemoteValueKind.BOOLEAN
    }

    private fun isBooleanComplementKeys(
        aKey: RemoteStateCacheKey,
        bKey: RemoteStateCacheKey,
    ): Boolean =
        (aKey is RemoteOperationCacheKey &&
            aKey.op == BooleanOperationKey.Not &&
            aKey.args.size == 1 &&
            aKey.args[0] == bKey) ||
            (bKey is RemoteOperationCacheKey &&
                bKey.op == BooleanOperationKey.Not &&
                bKey.args.size == 1 &&
                bKey.args[0] == aKey)
}
