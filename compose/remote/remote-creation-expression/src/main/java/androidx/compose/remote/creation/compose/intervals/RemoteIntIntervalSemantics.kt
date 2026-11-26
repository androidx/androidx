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

import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteIntOperationKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.document.TextFromFloatFlags
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.StringAndColorIntervalOps
import androidx.compose.remote.document.intervals.StringSummary
import androidx.compose.remote.document.intervals.TriState
import java.util.HashMap
import kotlin.math.min as mathMin

/** External interval semantics for [RemoteIntOperationKey]. */
internal object RemoteIntIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun comparisonCondition(op: Any): IntervalComparison? =
        when (op as RemoteIntOperationKey) {
            RemoteIntOperationKey.CompareEQ -> IntervalComparison.EQ
            RemoteIntOperationKey.CompareNE -> IntervalComparison.NEQ
            RemoteIntOperationKey.CompareLT -> IntervalComparison.LT
            RemoteIntOperationKey.CompareLE -> IntervalComparison.LTE
            RemoteIntOperationKey.CompareGT -> IntervalComparison.GT
            RemoteIntOperationKey.CompareGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectCondition(op: Any): IntervalComparison? =
        when (op as RemoteIntOperationKey) {
            RemoteIntOperationKey.SelectIfLT -> IntervalComparison.LT
            RemoteIntOperationKey.SelectIfLE -> IntervalComparison.LTE
            RemoteIntOperationKey.SelectIfGT -> IntervalComparison.GT
            RemoteIntOperationKey.SelectIfGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        if (selectCondition(op) != null) RemoteValueKind.INT else RemoteValueKind.OTHER

    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval =
        if (key.op == RemoteIntOperationKey.ToFloat && key.args.isNotEmpty()) {
            evaluator.evalIntKey(key.args[0]).toFloatInterval()
        } else {
            evaluator.evalIntKey(key).toFloatInterval()
        }

    override fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval {
        val op = key.op as RemoteIntOperationKey
        val args = key.args
        selectCondition(op)?.let { cond ->
            if (args.size < 4) return IntInterval.UNBOUNDED
            return when (evaluator.compareIntKeys(cond, args[0], args[1])) {
                TriState.TRUE -> evaluator.evalIntKey(args[2])
                TriState.FALSE -> evaluator.evalIntKey(args[3])
                TriState.UNKNOWN ->
                    evaluator
                        .evalComparisonBranch(
                            isInt = true,
                            comparison = cond,
                            keyA = args[0],
                            keyB = args[1],
                            holds = true,
                        ) {
                            it.evalIntKey(args[2])
                        }
                        .union(
                            evaluator.evalComparisonBranch(
                                isInt = true,
                                comparison = cond,
                                keyA = args[0],
                                keyB = args[1],
                                holds = false,
                            ) {
                                it.evalIntKey(args[3])
                            }
                        )
            }
        }
        comparisonCondition(op)?.let { cond ->
            return if (args.size >= 2) {
                evaluator.compareIntKeys(cond, args[0], args[1]).toIntInterval()
            } else {
                IntInterval(0, 1)
            }
        }
        return when (op) {
            RemoteIntOperationKey.Reference,
            RemoteIntOperationKey.Id,
            RemoteIntOperationKey.ToFloat ->
                if (args.isNotEmpty()) evaluator.evalIntKey(args[0]) else IntInterval.UNBOUNDED
            RemoteIntOperationKey.Add ->
                if (args.size >= 2) {
                    evaluator.evalIntKey(args[0]) + evaluator.evalIntKey(args[1])
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Sub ->
                if (args.size >= 2) {
                    evaluator.evalIntKey(args[0]) - evaluator.evalIntKey(args[1])
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Mul ->
                if (args.size >= 2) {
                    evaluator.evalIntKey(args[0]) * evaluator.evalIntKey(args[1])
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Div ->
                if (args.size >= 2) {
                    evaluator.evalIntKey(args[0]) / evaluator.evalIntKey(args[1])
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Mod ->
                if (args.size >= 2) {
                    evaluator.evalIntKey(args[0]) % evaluator.evalIntKey(args[1])
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Abs ->
                if (args.isNotEmpty()) {
                    evaluator.evalIntKey(args[0]).abs()
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Neg ->
                if (args.isNotEmpty()) -evaluator.evalIntKey(args[0]) else IntInterval.UNBOUNDED
            RemoteIntOperationKey.Min ->
                if (args.size >= 2) {
                    evaluator.evalIntKey(args[0]).min(evaluator.evalIntKey(args[1]))
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Max ->
                if (args.size >= 2) {
                    evaluator.evalIntKey(args[0]).max(evaluator.evalIntKey(args[1]))
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Clamp ->
                if (args.size >= 3) {
                    evaluator
                        .evalIntKey(args[2])
                        .clamp(evaluator.evalIntKey(args[0]), evaluator.evalIntKey(args[1]))
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Not ->
                if (args.isNotEmpty()) {
                    evaluator.evalPointUnaryIntOp(
                        evaluator.evalIntKey(args[0]),
                        IntegerExpressionEvaluator.I_NOT,
                    )
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.And ->
                if (args.size < 2) {
                    IntInterval.UNBOUNDED
                } else {
                    val a = evaluator.evalIntKey(args[0])
                    val b = evaluator.evalIntKey(args[1])
                    when {
                        a.isEmpty || b.isEmpty -> IntInterval.EMPTY
                        a.isPoint && b.isPoint ->
                            evaluator.evalPointBinaryIntOp(a, b, IntegerExpressionEvaluator.I_AND)
                        a.min >= 0 && b.min >= 0 && a.hasFiniteMax && b.hasFiniteMax ->
                            IntInterval(0, mathMin(a.max, b.max))
                        b.min >= 0 && b.hasFiniteMax -> IntInterval(0, b.max)
                        a.min >= 0 && a.hasFiniteMax -> IntInterval(0, a.max)
                        else -> IntInterval.UNBOUNDED
                    }
                }
            RemoteIntOperationKey.Or ->
                evalBoolDomainOrPointBinaryInt(args, evaluator, IntegerExpressionEvaluator.I_OR)
            RemoteIntOperationKey.Xor ->
                evalBoolDomainOrPointBinaryInt(args, evaluator, IntegerExpressionEvaluator.I_XOR)
            RemoteIntOperationKey.Shl ->
                if (args.size >= 2) {
                    evaluator.evalPointBinaryIntOp(
                        evaluator.evalIntKey(args[0]),
                        evaluator.evalIntKey(args[1]),
                        IntegerExpressionEvaluator.I_SHL,
                    )
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.Shr ->
                if (args.size >= 2) {
                    evaluator.evalPointBinaryIntOp(
                        evaluator.evalIntKey(args[0]),
                        evaluator.evalIntKey(args[1]),
                        IntegerExpressionEvaluator.I_SHR,
                    )
                } else {
                    IntInterval.UNBOUNDED
                }
            RemoteIntOperationKey.CopySign ->
                if (args.size < 2) {
                    IntInterval.UNBOUNDED
                } else {
                    val a = evaluator.evalIntKey(args[0])
                    val b = evaluator.evalIntKey(args[1])
                    if (a.isEmpty || b.isEmpty) {
                        IntInterval.EMPTY
                    } else if (a.isPoint && b.isPoint) {
                        evaluator.evalPointBinaryIntOp(a, b, IntegerExpressionEvaluator.I_COPY_SIGN)
                    } else {
                        // IntegerExpressionEvaluator.OP_COPY_SIGN returns a when b >= 0 and -a when
                        // b < 0 (it toggles sign rather than taking |a| first).
                        when {
                            b.min >= 0 -> a
                            b.max < 0 -> -a
                            else -> a.union(-a)
                        }
                    }
                }
            RemoteIntOperationKey.ToRemoteString,
            RemoteIntOperationKey.CompareEQ,
            RemoteIntOperationKey.CompareNE,
            RemoteIntOperationKey.CompareLT,
            RemoteIntOperationKey.CompareLE,
            RemoteIntOperationKey.CompareGT,
            RemoteIntOperationKey.CompareGE,
            RemoteIntOperationKey.SelectIfLT,
            RemoteIntOperationKey.SelectIfLE,
            RemoteIntOperationKey.SelectIfGT,
            RemoteIntOperationKey.SelectIfGE -> IntInterval.UNBOUNDED
        }
    }

    override fun evaluateBooleanInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState? {
        val op = key.op as RemoteIntOperationKey
        val args = key.args
        comparisonCondition(op)?.let { cond ->
            return if (args.size >= 2) {
                evaluator.compareIntKeys(cond, args[0], args[1])
            } else {
                TriState.UNKNOWN
            }
        }
        if (
            args.size < 2 ||
                (op != RemoteIntOperationKey.Xor &&
                    op != RemoteIntOperationKey.And &&
                    op != RemoteIntOperationKey.Or)
        ) {
            return null
        }
        val leftInt = evaluator.evalIntKey(args[0])
        val rightInt = evaluator.evalIntKey(args[1])
        if (leftInt.isEmpty || rightInt.isEmpty) return TriState.UNKNOWN
        val leftIsBoolDomain = leftInt.min >= 0 && leftInt.max <= 1
        val rightIsBoolDomain = rightInt.min >= 0 && rightInt.max <= 1
        return when (op) {
            RemoteIntOperationKey.Xor -> {
                if (!leftIsBoolDomain || !rightIsBoolDomain) {
                    null
                } else {
                    val c0 = leftInt.takeIf { it.isPoint }?.min
                    val c1 = rightInt.takeIf { it.isPoint }?.min
                    when {
                        c1 == 1 -> !evaluator.evalBoolKey(args[0])
                        c0 == 1 -> !evaluator.evalBoolKey(args[1])
                        else -> {
                            val b0 = evaluator.evalBoolKey(args[0])
                            val b1 = evaluator.evalBoolKey(args[1])
                            if (b0 != TriState.UNKNOWN && b1 != TriState.UNKNOWN) {
                                TriState.of(b0 != b1)
                            } else {
                                null
                            }
                        }
                    }
                }
            }
            RemoteIntOperationKey.And -> {
                val b0 = evaluator.evalBoolKey(args[0])
                val b1 = evaluator.evalBoolKey(args[1])
                when {
                    b0 == TriState.FALSE || b1 == TriState.FALSE -> TriState.FALSE
                    b0 == TriState.TRUE &&
                        b1 == TriState.TRUE &&
                        leftIsBoolDomain &&
                        rightIsBoolDomain -> TriState.TRUE
                    else -> null
                }
            }
            RemoteIntOperationKey.Or -> {
                val b0 = evaluator.evalBoolKey(args[0])
                val b1 = evaluator.evalBoolKey(args[1])
                when {
                    b0 == TriState.FALSE && b1 == TriState.FALSE -> TriState.FALSE
                    (b0 == TriState.TRUE || b1 == TriState.TRUE) &&
                        leftInt.min >= 0 &&
                        rightInt.min >= 0 -> TriState.TRUE
                    else -> null
                }
            }
            else -> null
        }
    }

    override fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary {
        if (key.op != RemoteIntOperationKey.ToRemoteString || key.args.isEmpty()) {
            return StringSummary.UNKNOWN
        }
        return StringAndColorIntervalOps.summarizeTextFromFloat(
            evaluator.evalIntKey(key.args[0]).toFloatInterval(),
            digitsBefore = 40,
            digitsAfter = 0,
            flags =
                TextFromFloatFlags.SEPARATOR_COMMA_PERIOD or
                    TextFromFloatFlags.GROUPING_BY3 or
                    TextFromFloatFlags.PAD_AFTER_NONE or
                    TextFromFloatFlags.PAD_PRE_NONE,
        )
    }

    private fun evalBoolDomainOrPointBinaryInt(
        args: List<RemoteStateCacheKey>,
        evaluator: RemoteValueIntervalEvaluator,
        opCode: Int,
    ): IntInterval {
        if (args.size < 2) return IntInterval.UNBOUNDED
        val a = evaluator.evalIntKey(args[0])
        val b = evaluator.evalIntKey(args[1])
        if (a.isEmpty || b.isEmpty) return IntInterval.EMPTY
        if (a.isPoint && b.isPoint) {
            return evaluator.evalPointBinaryIntOp(a, b, opCode)
        }
        if (!a.isEmpty && !b.isEmpty && a.min >= 0 && a.max <= 1 && b.min >= 0 && b.max <= 1) {
            if (opCode == IntegerExpressionEvaluator.I_OR && (a.min == 1 || b.min == 1)) {
                return IntInterval.point(1)
            }
            return IntInterval(0, 1)
        }
        return IntInterval.UNBOUNDED
    }

    override fun extractFloatAffine(
        key: RemoteOperationCacheKey,
        range: FloatInterval,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): FloatAffineForm? {
        if (key.op != RemoteIntOperationKey.ToFloat || key.args.isEmpty()) return null
        val intKey = key.args[0]
        val intRange = evaluator.evalIntKey(intKey)
        val intForm = evaluator.extractIntAffineForm(intKey, depth + 1) ?: return null
        if (!intRange.hasFiniteMin || !intRange.hasFiniteMax) return null
        val ulp =
            if (
                intRange.min >= -EXACT_FLOAT32_INT_LIMIT && intRange.max <= EXACT_FLOAT32_INT_LIMIT
            ) {
                0.0
            } else {
                maxUlp(range)
            }
        if (intForm.constant < -(1L shl 53) || intForm.constant > (1L shl 53)) return null
        val floatTerms = HashMap<AffineTerm, Double>(intForm.terms.size)
        for ((k, w) in intForm.terms) {
            if (w < -MAX_EXACT_INT_AFFINE_WEIGHT || w > MAX_EXACT_INT_AFFINE_WEIGHT) return null
            floatTerms[AffineTerm(k, isInt = true)] = w.toDouble()
        }
        return FloatAffineForm(floatTerms, intForm.constant.toDouble(), ulp)
    }

    override fun extractIntAffine(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): IntAffineForm? {
        val args = key.args
        return when (key.op as RemoteIntOperationKey) {
            RemoteIntOperationKey.Reference,
            RemoteIntOperationKey.Id ->
                args.firstOrNull()?.let { evaluator.extractIntAffineForm(it, depth + 1) }
            RemoteIntOperationKey.Neg -> {
                val arg = args.firstOrNull() ?: return null
                val r0 = evaluator.evalIntKey(arg)
                if (!r0.hasFiniteMin || !r0.hasFiniteMax) return null
                evaluator.extractIntAffineForm(arg, depth + 1)?.negate()
            }
            RemoteIntOperationKey.Add -> addIntAffineArgs(args, negateRhs = false, evaluator, depth)
            RemoteIntOperationKey.Sub -> addIntAffineArgs(args, negateRhs = true, evaluator, depth)
            RemoteIntOperationKey.Mul -> mulIntAffineArgs(args, evaluator, depth)
            else -> null
        }
    }

    override fun refineBoolean(
        key: RemoteOperationCacheKey,
        holds: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? {
        val op = key.op as RemoteIntOperationKey
        val args = key.args
        if (args.size < 2) return null
        comparisonCondition(op)?.let { rawCond ->
            val effCond = if (holds) rawCond else rawCond.negate()
            return environment.narrowComparison(effCond, args[0], args[1], asInt = true)
        }
        return when (op) {
            RemoteIntOperationKey.Xor -> {
                val c0 = ((args[0] as? RemoteConstantCacheKey)?.value as? Number)?.toInt()
                val c1 = ((args[1] as? RemoteConstantCacheKey)?.value as? Number)?.toInt()
                val target =
                    when {
                        c1 == 1 -> args[0]
                        c0 == 1 -> args[1]
                        else -> null
                    } ?: return null
                val r = evaluator.evalIntKey(target)
                when {
                    !r.isEmpty && r.min >= 0 && r.max <= 1 -> environment.refine(target, !holds)
                    !holds && !r.isEmpty && r.min >= 0 ->
                        applyIntKeyNarrowing(environment, target, IntInterval.point(1))
                    else -> null
                }
            }
            RemoteIntOperationKey.And ->
                if (holds) {
                    val r0 = evaluator.evalIntKey(args[0])
                    val r1 = evaluator.evalIntKey(args[1])
                    if (!r0.isEmpty && r0.min >= 0 && !r1.isEmpty && r1.min >= 0) {
                        environment.refine(args[0], true).refine(args[1], true)
                    } else {
                        null
                    }
                } else {
                    null
                }
            RemoteIntOperationKey.Or ->
                if (!holds) {
                    val r0 = evaluator.evalIntKey(args[0])
                    val r1 = evaluator.evalIntKey(args[1])
                    if (!r0.isEmpty && r0.min >= 0 && !r1.isEmpty && r1.min >= 0) {
                        environment.refine(args[0], false).refine(args[1], false)
                    } else {
                        null
                    }
                } else {
                    null
                }
            else -> null
        }
    }

    override fun refineComparison(
        key: RemoteOperationCacheKey,
        comparison: IntervalComparison,
        otherKey: RemoteStateCacheKey,
        asInt: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? {
        if (!asInt || key.op != RemoteIntOperationKey.Sub || key.args.size < 2) return null
        val otherR = evaluator.evalIntKey(otherKey)
        if (!otherR.isPoint || otherR.min != 0) return null
        val lhsR = evaluator.evalIntKey(key.args[0])
        val rhsR = evaluator.evalIntKey(key.args[1])
        if (!lhsR.hasFiniteMin || !lhsR.hasFiniteMax || !rhsR.hasFiniteMin || !rhsR.hasFiniteMax) {
            return null
        }
        val minDiff = lhsR.min.toLong() - rhsR.max.toLong()
        val maxDiff = lhsR.max.toLong() - rhsR.min.toLong()
        if (minDiff < Int.MIN_VALUE.toLong() || maxDiff > Int.MAX_VALUE.toLong()) return null
        return environment.narrowComparison(comparison, key.args[0], key.args[1], asInt = true)
    }

    override fun propagateFloatNarrowing(
        key: RemoteOperationCacheKey,
        interval: FloatInterval,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment {
        if (key.op != RemoteIntOperationKey.ToFloat || key.args.isEmpty()) return environment
        val intKey = key.args[0]
        val existingInt = evaluator.evalIntKey(intKey)
        val narrowedInt = existingInt.intersect(exactIntBoundsWithinFloatInterval(interval))
        return if (!narrowedInt.isEmpty && narrowedInt != existingInt) {
            applyIntKeyNarrowing(environment, intKey, narrowedInt)
        } else {
            environment
        }
    }

    private fun addIntAffineArgs(
        args: List<RemoteStateCacheKey>,
        negateRhs: Boolean,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): IntAffineForm? {
        if (args.size < 2) return null
        val r0 = evaluator.evalIntKey(args[0])
        val r1 = evaluator.evalIntKey(args[1])
        if (!r0.hasFiniteMin || !r0.hasFiniteMax || !r1.hasFiniteMin || !r1.hasFiniteMax) {
            return null
        }
        val lo =
            if (negateRhs) r0.min.toLong() - r1.max.toLong() else r0.min.toLong() + r1.min.toLong()
        val hi =
            if (negateRhs) r0.max.toLong() - r1.min.toLong() else r0.max.toLong() + r1.max.toLong()
        if (lo < Int.MIN_VALUE.toLong() || hi > Int.MAX_VALUE.toLong()) return null
        val a = evaluator.extractIntAffineForm(args[0], depth + 1) ?: return null
        val b = evaluator.extractIntAffineForm(args[1], depth + 1) ?: return null
        val rhs = if (negateRhs) b.negate() ?: return null else b
        return a.add(rhs)
    }

    private fun mulIntAffineArgs(
        args: List<RemoteStateCacheKey>,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): IntAffineForm? {
        if (args.size < 2) return null
        val r0 = evaluator.evalIntKey(args[0])
        val r1 = evaluator.evalIntKey(args[1])
        if (!r0.hasFiniteMin || !r0.hasFiniteMax || !r1.hasFiniteMin || !r1.hasFiniteMax) {
            return null
        }
        val p1 = r0.min.toLong() * r1.min.toLong()
        val p2 = r0.min.toLong() * r1.max.toLong()
        val p3 = r0.max.toLong() * r1.min.toLong()
        val p4 = r0.max.toLong() * r1.max.toLong()
        if (
            minOf(p1, p2, p3, p4) < Int.MIN_VALUE.toLong() ||
                maxOf(p1, p2, p3, p4) > Int.MAX_VALUE.toLong()
        ) {
            return null
        }
        return when {
            r0.isPoint -> evaluator.extractIntAffineForm(args[1], depth + 1)?.scale(r0.min.toLong())
            r1.isPoint -> evaluator.extractIntAffineForm(args[0], depth + 1)?.scale(r1.min.toLong())
            else -> null
        }
    }
}
