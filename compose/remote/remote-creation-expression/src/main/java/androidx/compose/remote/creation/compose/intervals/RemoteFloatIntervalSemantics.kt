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

import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloatOperationKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.SlotInterval
import androidx.compose.remote.document.intervals.StringAndColorIntervalOps
import androidx.compose.remote.document.intervals.StringSummary
import androidx.compose.remote.document.intervals.TriState
import kotlin.math.PI
import kotlin.math.abs as mathAbs
import kotlin.math.cbrt as mathCbrt
import kotlin.math.exp as mathExp
import kotlin.math.ln as mathLn
import kotlin.math.log10 as mathLog10
import kotlin.math.max as mathMax
import kotlin.math.min as mathMin
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.ulp

/** External interval semantics for [RemoteFloatOperationKey]. */
internal object RemoteFloatIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun requiresBoundedFloatOperands(op: Any): Boolean =
        when (op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.Times,
            RemoteFloatOperationKey.Div,
            RemoteFloatOperationKey.Rem,
            RemoteFloatOperationKey.Mad,
            RemoteFloatOperationKey.Clamp,
            RemoteFloatOperationKey.Lerp,
            RemoteFloatOperationKey.Min,
            RemoteFloatOperationKey.Max,
            RemoteFloatOperationKey.Abs,
            RemoteFloatOperationKey.Round,
            RemoteFloatOperationKey.Sin,
            RemoteFloatOperationKey.Cos,
            RemoteFloatOperationKey.Atan,
            RemoteFloatOperationKey.Atan2,
            RemoteFloatOperationKey.Sign,
            RemoteFloatOperationKey.CopySign,
            RemoteFloatOperationKey.Exp,
            RemoteFloatOperationKey.Cubic,
            RemoteFloatOperationKey.RandInRange -> true
            else -> false
        }

    override fun comparisonCondition(op: Any): IntervalComparison? =
        when (op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.CompareEQ -> IntervalComparison.EQ
            RemoteFloatOperationKey.CompareNE -> IntervalComparison.NEQ
            RemoteFloatOperationKey.CompareLT -> IntervalComparison.LT
            RemoteFloatOperationKey.CompareLE -> IntervalComparison.LTE
            RemoteFloatOperationKey.CompareGT -> IntervalComparison.GT
            RemoteFloatOperationKey.CompareGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectCondition(op: Any): IntervalComparison? =
        when (op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.SelectIfLT -> IntervalComparison.LT
            RemoteFloatOperationKey.SelectIfLE -> IntervalComparison.LTE
            RemoteFloatOperationKey.SelectIfGT -> IntervalComparison.GT
            RemoteFloatOperationKey.SelectIfGE -> IntervalComparison.GTE
            else -> null
        }

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        if (selectCondition(op) != null) RemoteValueKind.FLOAT else RemoteValueKind.OTHER

    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval {
        val op = key.op as RemoteFloatOperationKey
        val args = key.args
        selectCondition(op)?.let { cond ->
            if (args.size < 4) return FloatInterval.UNBOUNDED
            return when (evaluator.compareFloatKeys(cond, args[0], args[1])) {
                TriState.TRUE -> evaluator.evalFloatKey(args[2])
                TriState.FALSE -> evaluator.evalFloatKey(args[3])
                TriState.UNKNOWN ->
                    evaluator
                        .evalComparisonBranch(
                            isInt = false,
                            comparison = cond,
                            keyA = args[0],
                            keyB = args[1],
                            holds = true,
                        ) {
                            it.evalFloatKey(args[2])
                        }
                        .union(
                            evaluator.evalComparisonBranch(
                                isInt = false,
                                comparison = cond,
                                keyA = args[0],
                                keyB = args[1],
                                holds = false,
                            ) {
                                it.evalFloatKey(args[3])
                            }
                        )
            }
        }
        comparisonCondition(op)?.let { cond ->
            return if (args.size >= 2) {
                evaluator.compareFloatKeys(cond, args[0], args[1]).toFloatInterval()
            } else {
                FloatInterval.UNIT
            }
        }
        if (requiresBoundedFloatOperands(op)) {
            var hasEmpty = false
            var hasUnbounded = false
            for (i in 0 until args.size) {
                val operand = evaluator.evalFloatKey(args[i])
                if (operand.isEmpty) {
                    hasEmpty = true
                } else if (operand.isUnbounded) {
                    hasUnbounded = true
                }
            }
            if (hasEmpty) return FloatInterval.EMPTY
            if (hasUnbounded) return FloatInterval.UNBOUNDED
        }
        return when (op) {
            RemoteFloatOperationKey.Reference ->
                if (args.isNotEmpty()) evaluator.evalFloatKey(args[0]) else FloatInterval.UNBOUNDED
            RemoteFloatOperationKey.UnaryMinus ->
                if (args.isNotEmpty()) -evaluator.evalFloatKey(args[0]) else FloatInterval.UNBOUNDED
            RemoteFloatOperationKey.Plus ->
                if (args.size >= 2) {
                    evaluator.evalFloatKey(args[0]) + evaluator.evalFloatKey(args[1])
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Minus ->
                if (args.size >= 2) {
                    evaluator.evalFloatKey(args[0]) - evaluator.evalFloatKey(args[1])
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Times ->
                if (args.size >= 2) {
                    evaluator.evalFloatKey(args[0]) * evaluator.evalFloatKey(args[1])
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Div ->
                if (args.size >= 2) {
                    evaluator.evalFloatKey(args[0]) / evaluator.evalFloatKey(args[1])
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Rem ->
                if (args.size >= 2) {
                    evaluator.evalFloatKey(args[0]) % evaluator.evalFloatKey(args[1])
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Min ->
                if (args.size >= 2) {
                    evaluator.evalFloatKey(args[0]).min(evaluator.evalFloatKey(args[1]))
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Max ->
                if (args.size >= 2) {
                    evaluator.evalFloatKey(args[0]).max(evaluator.evalFloatKey(args[1]))
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Abs ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]).abs()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Clamp ->
                if (args.size >= 3) {
                    evaluator
                        .evalFloatKey(args[2])
                        .clamp(evaluator.evalFloatKey(args[0]), evaluator.evalFloatKey(args[1]))
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Mad ->
                if (args.size >= 3) {
                    evaluator.evalFloatKey(args[0]) * evaluator.evalFloatKey(args[1]) +
                        evaluator.evalFloatKey(args[2])
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Lerp -> {
                if (args.size < 3) {
                    FloatInterval.UNBOUNDED
                } else {
                    val from = evaluator.evalFloatKey(args[0])
                    val to = evaluator.evalFloatKey(args[1])
                    val t = evaluator.evalFloatKey(args[2])
                    when {
                        from.isEmpty || to.isEmpty || t.isEmpty -> FloatInterval.EMPTY
                        !from.isUnbounded &&
                            !to.isUnbounded &&
                            !t.isUnbounded &&
                            t.min >= 0f &&
                            t.max <= 1f -> evalLerpBox(from, to, t)
                        else -> from + (to - from) * t
                    }
                }
            }
            RemoteFloatOperationKey.Floor ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]).floor()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Ceil ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]).ceil()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Round ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]).round()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Sqrt ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]).sqrt()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Cbrt -> {
                val a = args.firstOrNull()?.let { evaluator.evalFloatKey(it) }
                when {
                    a == null -> FloatInterval.UNBOUNDED
                    a.isEmpty -> FloatInterval.EMPTY
                    // AnimatedFloatExpression.OP_CBRT uses Math.pow(x, 1/3.), returning NaN for x <
                    // 0.
                    a.min < 0f -> FloatInterval.UNBOUNDED
                    else ->
                        FloatInterval(
                            if (a.min.isInfinite()) a.min else mathCbrt(a.min),
                            if (a.max.isInfinite()) a.max else mathCbrt(a.max),
                        )
                }
            }
            RemoteFloatOperationKey.Exp -> {
                val a = args.firstOrNull()?.let { evaluator.evalFloatKey(it) }
                when {
                    a == null -> FloatInterval.UNBOUNDED
                    a.isEmpty -> FloatInterval.EMPTY
                    else -> FloatInterval(expFloatBound(a.min), expFloatBound(a.max))
                }
            }
            RemoteFloatOperationKey.Ln -> evalPositiveLogInterval(args, evaluator, ::mathLn)
            RemoteFloatOperationKey.Log -> evalPositiveLogInterval(args, evaluator, ::mathLog10)
            RemoteFloatOperationKey.Pow ->
                if (args.size >= 2) {
                    evaluator.evalBinaryFloatExactOrRange(
                        evaluator.evalFloatKey(args[0]),
                        evaluator.evalFloatKey(args[1]),
                        AnimatedFloatExpression.POW,
                        FloatInterval.UNBOUNDED,
                    )
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Rand ->
                FloatInterval(0f, 1f, minInclusive = true, maxInclusive = false)
            RemoteFloatOperationKey.RandInRange ->
                if (args.size < 2) {
                    FloatInterval.UNBOUNDED
                } else {
                    val from = evaluator.evalFloatKey(args[0])
                    val to = evaluator.evalFloatKey(args[1])
                    when {
                        from.isEmpty || to.isEmpty -> FloatInterval.EMPTY
                        from.isUnbounded || to.isUnbounded -> FloatInterval.UNBOUNDED
                        else -> evalLerpBox(from, to, FloatInterval.UNIT)
                    }
                }
            RemoteFloatOperationKey.Sin ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]).sin()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Cos ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]).cos()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Tan ->
                if (args.isNotEmpty()) {
                    evaluator.evalUnaryFloatExactOrUnbounded(
                        evaluator.evalFloatKey(args[0]),
                        AnimatedFloatExpression.TAN,
                    )
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Asin ->
                evalInverseTrigUnitInterval(
                    args,
                    evaluator,
                    AnimatedFloatExpression.ASIN,
                    (-PI / 2.0).toFloat(),
                    (PI / 2.0).toFloat(),
                )
            RemoteFloatOperationKey.Acos ->
                evalInverseTrigUnitInterval(
                    args,
                    evaluator,
                    AnimatedFloatExpression.ACOS,
                    0f,
                    PI.toFloat(),
                )
            RemoteFloatOperationKey.Atan ->
                if (args.isNotEmpty()) {
                    val a = evaluator.evalFloatKey(args[0])
                    when {
                        a.isEmpty -> FloatInterval.EMPTY
                        a.isPoint ->
                            evaluator.evalUnaryFloatExactOrUnbounded(
                                a,
                                AnimatedFloatExpression.ATAN,
                            )
                        else -> FloatInterval((-PI / 2.0).toFloat(), (PI / 2.0).toFloat())
                    }
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Atan2 ->
                if (args.size >= 2) {
                    val a = evaluator.evalFloatKey(args[0])
                    val b = evaluator.evalFloatKey(args[1])
                    if (a.isEmpty || b.isEmpty) {
                        FloatInterval.EMPTY
                    } else {
                        evaluator.evalBinaryFloatExactOrRange(
                            a,
                            b,
                            AnimatedFloatExpression.ATAN2,
                            FloatInterval(-PI.toFloat(), PI.toFloat()),
                        )
                    }
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.ToDeg ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]) * FloatInterval.point((180.0 / PI).toFloat())
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.ToRad ->
                if (args.isNotEmpty()) {
                    evaluator.evalFloatKey(args[0]) * FloatInterval.point((PI / 180.0).toFloat())
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Sign -> {
                val a = args.firstOrNull()?.let { evaluator.evalFloatKey(it) }
                when {
                    a == null -> FloatInterval.UNBOUNDED
                    a.isEmpty -> FloatInterval.EMPTY
                    a.isAlwaysGreaterThan(FloatInterval.ZERO) -> FloatInterval.point(1f)
                    a.isAlwaysLessThan(FloatInterval.ZERO) -> FloatInterval.point(-1f)
                    a.isPoint && a.min == 0f -> FloatInterval.ZERO
                    else -> FloatInterval(-1f, 1f)
                }
            }
            RemoteFloatOperationKey.CopySign ->
                if (args.size < 2) {
                    FloatInterval.UNBOUNDED
                } else {
                    val rawMag = evaluator.evalFloatKey(args[0])
                    val signSrc = evaluator.evalFloatKey(args[1])
                    if (rawMag.isEmpty || signSrc.isEmpty) {
                        FloatInterval.EMPTY
                    } else {
                        val absMag = rawMag.abs()
                        when {
                            signSrc.isAlwaysGreaterThan(FloatInterval.ZERO) -> absMag
                            signSrc.isAlwaysLessThan(FloatInterval.ZERO) -> -absMag
                            else -> (-absMag).union(absMag)
                        }
                    }
                }
            RemoteFloatOperationKey.TimeOfReferenceInSeconds -> FloatInterval(0f, 59f)
            RemoteFloatOperationKey.TimeOfReferenceInMinutes -> FloatInterval(0f, 59f)
            RemoteFloatOperationKey.TimeOfReferenceInHours -> FloatInterval(0f, 23f)
            RemoteFloatOperationKey.DayOfMonthForReference -> FloatInterval(1f, 31f)
            RemoteFloatOperationKey.MonthOfYearForReference -> FloatInterval(0f, 11f)
            RemoteFloatOperationKey.DayOfWeekForReference -> FloatInterval(0f, 6f)
            RemoteFloatOperationKey.ToInt,
            RemoteFloatOperationKey.ToRemoteInt ->
                if (args.isNotEmpty()) {
                    SlotInterval.fromFloat(evaluator.evalFloatKey(args[0]))
                        .toIntInterval()
                        .toFloatInterval()
                } else {
                    FloatInterval.UNBOUNDED
                }
            RemoteFloatOperationKey.Anim,
            RemoteFloatOperationKey.Cubic,
            RemoteFloatOperationKey.ToRemoteString,
            RemoteFloatOperationKey.SelectIfLT,
            RemoteFloatOperationKey.SelectIfLE,
            RemoteFloatOperationKey.SelectIfGT,
            RemoteFloatOperationKey.SelectIfGE,
            RemoteFloatOperationKey.ComparisonOp,
            RemoteFloatOperationKey.CompareEQ,
            RemoteFloatOperationKey.CompareNE,
            RemoteFloatOperationKey.CompareLT,
            RemoteFloatOperationKey.CompareLE,
            RemoteFloatOperationKey.CompareGT,
            RemoteFloatOperationKey.CompareGE,
            RemoteFloatOperationKey.DeltaFromReferenceInSeconds,
            RemoteFloatOperationKey.DeltaFromReferenceInMinutes,
            RemoteFloatOperationKey.DeltaFromReferenceInHours,
            RemoteFloatOperationKey.YearForReference,
            RemoteFloatOperationKey.Spline,
            RemoteFloatOperationKey.SplineLoop -> FloatInterval.UNBOUNDED
        }
    }

    override fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval {
        val op = key.op as RemoteFloatOperationKey
        val args = key.args
        comparisonCondition(op)?.let { cond ->
            return if (args.size >= 2) {
                evaluator.compareFloatKeys(cond, args[0], args[1]).toIntInterval()
            } else {
                IntInterval(0, 1)
            }
        }
        return when (op) {
            RemoteFloatOperationKey.ToInt,
            RemoteFloatOperationKey.ToRemoteInt ->
                if (args.isNotEmpty()) {
                    SlotInterval.fromFloat(evaluator.evalFloatKey(args[0])).toIntInterval()
                } else {
                    IntInterval.UNBOUNDED
                }
            else -> SlotInterval.fromFloat(evaluator.evalFloatKey(key)).toIntInterval()
        }
    }

    override fun evaluateBooleanInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState? {
        val cond = comparisonCondition(key.op) ?: return null
        return if (key.args.size >= 2) {
            evaluator.compareFloatKeys(cond, key.args[0], key.args[1])
        } else {
            TriState.UNKNOWN
        }
    }

    override fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary {
        if (key.op != RemoteFloatOperationKey.ToRemoteString || key.args.size < 4) {
            return StringSummary.UNKNOWN
        }
        val before =
            ((key.args[1] as? RemoteConstantCacheKey)?.value as? Number)?.toInt()
                ?: return StringSummary.UNKNOWN
        val after =
            ((key.args[2] as? RemoteConstantCacheKey)?.value as? Number)?.toInt()
                ?: return StringSummary.UNKNOWN
        val flags =
            ((key.args[3] as? RemoteConstantCacheKey)?.value as? Number)?.toInt()
                ?: return StringSummary.UNKNOWN
        return StringAndColorIntervalOps.summarizeTextFromFloat(
            evaluator.evalFloatKey(key.args[0]),
            before,
            after,
            flags,
        )
    }

    private fun evalPositiveLogInterval(
        args: List<RemoteStateCacheKey>,
        evaluator: RemoteValueIntervalEvaluator,
        logFn: (Float) -> Float,
    ): FloatInterval {
        if (args.isEmpty()) return FloatInterval.UNBOUNDED
        val a = evaluator.evalFloatKey(args[0])
        if (a.isEmpty) return FloatInterval.EMPTY
        if (a.min <= 0f) return FloatInterval.UNBOUNDED
        return FloatInterval(
            if (a.min.isInfinite()) Float.POSITIVE_INFINITY else logFn(a.min),
            if (a.max.isInfinite()) Float.POSITIVE_INFINITY else logFn(a.max),
        )
    }

    private fun evalInverseTrigUnitInterval(
        args: List<RemoteStateCacheKey>,
        evaluator: RemoteValueIntervalEvaluator,
        op: Float,
        minOut: Float,
        maxOut: Float,
    ): FloatInterval {
        if (args.isEmpty()) return FloatInterval.UNBOUNDED
        val a = evaluator.evalFloatKey(args[0])
        return when {
            a.isEmpty -> FloatInterval.EMPTY
            a.min < -1f || a.max > 1f -> FloatInterval.UNBOUNDED
            a.isPoint -> evaluator.evalUnaryFloatExactOrUnbounded(a, op)
            else -> FloatInterval(minOut, maxOut)
        }
    }

    private fun evalLerpBox(
        from: FloatInterval,
        to: FloatInterval,
        t: FloatInterval,
    ): FloatInterval {
        if (from.isPoint && to.isPoint) {
            // Fixed float32 endpoints: d = fl(to - from) is constant, and fl(from + fl(d * p))
            // is monotone in p in IEEE-754 float32, so the exact extrema occur at t.min / t.max.
            val diff = to.min - from.min
            if (!diff.isFinite()) return FloatInterval.UNBOUNDED
            val a = from.min + diff * t.min
            val b = from.min + diff * t.max
            if (!a.isFinite() || !b.isFinite()) return FloatInterval.UNBOUNDED
            return FloatInterval(mathMin(a, b), mathMax(a, b))
        }
        val fVals = floatArrayOf(from.min, from.max)
        val uVals = floatArrayOf(to.min, to.max)
        val tVals = floatArrayOf(t.min, t.max)
        var loD = Double.POSITIVE_INFINITY
        var hiD = Double.NEGATIVE_INFINITY
        var maxDiffAbs = 0.0
        for (f in fVals) {
            for (u in uVals) {
                val diffAbs = mathAbs(u.toDouble() - f.toDouble())
                if (diffAbs > maxDiffAbs) maxDiffAbs = diffAbs
                for (p in tVals) {
                    val dVal = f.toDouble() + (u.toDouble() - f.toDouble()) * p.toDouble()
                    if (!dVal.isFinite()) return FloatInterval.UNBOUNDED
                    if (dVal < loD) loD = dVal
                    if (dVal > hiD) hiD = dVal
                }
            }
        }
        val maxDiffFloat = ceilAbsToFloat(maxDiffAbs) ?: return FloatInterval.UNBOUNDED
        val diffErr = 0.5 * maxDiffFloat.ulp.toDouble()
        val maxTAbs = mathMax(mathAbs(t.min.toDouble()), mathAbs(t.max.toDouble()))
        val maxProdAbs = (maxDiffAbs + diffErr) * maxTAbs
        val maxProdFloat = ceilAbsToFloat(maxProdAbs) ?: return FloatInterval.UNBOUNDED
        val prodErr = diffErr * maxTAbs + 0.5 * maxProdFloat.ulp.toDouble()
        val maxValAbs = mathMax(mathAbs(loD), mathAbs(hiD))
        val maxSumAbs = maxValAbs + prodErr
        val maxSumFloat = ceilAbsToFloat(maxSumAbs) ?: return FloatInterval.UNBOUNDED
        val totalErr =
            prodErr + 0.5 * maxSumFloat.ulp.toDouble() + 4.0 * mathMax(1.0, maxValAbs).ulp
        val wideLo = loD - totalErr
        val wideHi = hiD + totalErr
        if (!wideLo.isFinite() || !wideHi.isFinite()) return FloatInterval.UNBOUNDED
        val outLo = wideLo.toFloat().let { if (it.toDouble() > wideLo) it.nextDown() else it }
        val outHi = wideHi.toFloat().let { if (it.toDouble() < wideHi) it.nextUp() else it }
        if (!outLo.isFinite() || !outHi.isFinite()) return FloatInterval.UNBOUNDED
        val boxHull = FloatInterval(outLo, outHi)
        val intervalHull = from + (to - from) * t
        return if (intervalHull.isUnbounded) boxHull else boxHull.intersect(intervalHull)
    }

    private fun ceilAbsToFloat(value: Double): Float? {
        if (!value.isFinite() || value > Float.MAX_VALUE.toDouble()) return null
        val f = value.toFloat()
        val roundedUp = if (f.toDouble() < value) f.nextUp() else f
        return if (roundedUp.isFinite()) roundedUp else null
    }

    private fun expFloatBound(v: Float): Float =
        when (v) {
            Float.NEGATIVE_INFINITY -> 0f
            Float.POSITIVE_INFINITY -> Float.POSITIVE_INFINITY
            else -> mathExp(v)
        }

    override fun extractFloatAffine(
        key: RemoteOperationCacheKey,
        range: FloatInterval,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): FloatAffineForm? {
        val args = key.args
        return when (key.op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.Reference ->
                args.firstOrNull()?.let { evaluator.extractFloatAffineForm(it, depth + 1) }
            RemoteFloatOperationKey.UnaryMinus ->
                args.firstOrNull()?.let {
                    evaluator.extractFloatAffineForm(it, depth + 1)?.negate()
                }
            RemoteFloatOperationKey.Plus ->
                addFloatAffineArgs(args, range, negateRhs = false, evaluator, depth)
            RemoteFloatOperationKey.Minus ->
                addFloatAffineArgs(args, range, negateRhs = true, evaluator, depth)
            RemoteFloatOperationKey.Times -> scaleFloatAffineTimes(args, range, evaluator, depth)
            RemoteFloatOperationKey.Div -> {
                if (args.size < 2) return null
                val r1 = evaluator.evalFloatKey(args[1])
                if (!r1.isPoint || r1.min == 0f) return null
                val raw =
                    evaluator
                        .extractFloatAffineForm(args[0], depth + 1)
                        ?.scale(1.0 / r1.min.toDouble(), 0.0) ?: return null
                val tightRange = raw.evaluate(evaluator) ?: range
                raw.withExtraError(maxUlp(tightRange))
            }
            RemoteFloatOperationKey.Mad -> {
                if (args.size < 3) return null
                val prodRange = evaluator.evalFloatKey(args[0]) * evaluator.evalFloatKey(args[1])
                if (prodRange.isEmpty || prodRange.isUnbounded) return null
                val prod = scaleFloatAffineTimes(args, prodRange, evaluator, depth) ?: return null
                val addend = evaluator.extractFloatAffineForm(args[2], depth + 1) ?: return null
                evaluator.combineFloatAffineAdd(prod, addend, range, negateRhs = false)
            }
            RemoteFloatOperationKey.Lerp -> {
                if (args.size < 3) return null
                val tRange = evaluator.evalFloatKey(args[2])
                if (!tRange.isPoint || !tRange.min.isFinite()) return null
                val a = evaluator.extractFloatAffineForm(args[0], depth + 1) ?: return null
                val b = evaluator.extractFloatAffineForm(args[1], depth + 1) ?: return null
                val diffRange = evaluator.evalFloatKey(args[1]) - evaluator.evalFloatKey(args[0])
                if (diffRange.isEmpty || diffRange.isUnbounded) return null
                val diff = evaluator.combineFloatAffineAdd(b, a, diffRange, negateRhs = true)
                val scaledRaw = diff.scale(tRange.min.toDouble(), roundingUlp = 0.0)
                val scaledRange = scaledRaw.evaluate(evaluator) ?: (diffRange * tRange)
                val scaled = scaledRaw.withExtraError(maxUlp(scaledRange))
                evaluator.combineFloatAffineAdd(a, scaled, range, negateRhs = false)
            }
            else -> null
        }
    }

    override fun refineBoolean(
        key: RemoteOperationCacheKey,
        holds: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? {
        val rawCond = comparisonCondition(key.op) ?: return null
        if (key.args.size < 2) return null
        val effCond = if (holds) rawCond else rawCond.negate()
        return environment.narrowComparison(effCond, key.args[0], key.args[1])
    }

    override fun refineComparison(
        key: RemoteOperationCacheKey,
        comparison: IntervalComparison,
        otherKey: RemoteStateCacheKey,
        asInt: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? {
        if (asInt) return null
        val otherRange = evaluator.evalFloatKey(otherKey)
        if (!otherRange.isPoint || otherRange.min != 0f) return null
        val args = key.args
        return when (key.op as RemoteFloatOperationKey) {
            RemoteFloatOperationKey.Minus -> {
                if (args.size < 2) return null
                val lhsR = evaluator.evalFloatKey(args[0])
                val rhsR = evaluator.evalFloatKey(args[1])
                if (lhsR.isEmpty || rhsR.isEmpty || lhsR.isUnbounded || rhsR.isUnbounded) {
                    return null
                }
                environment.narrowComparison(comparison, args[0], args[1])
            }
            RemoteFloatOperationKey.Plus -> {
                if (args.size < 2) return null
                val lhsR = evaluator.evalFloatKey(args[0])
                val rhsR = evaluator.evalFloatKey(args[1])
                if (lhsR.isEmpty || rhsR.isEmpty || lhsR.isUnbounded || rhsR.isUnbounded) {
                    return null
                }
                val negLhs = unwrapNegatedFloatKey(args[0])
                val negRhs = unwrapNegatedFloatKey(args[1])
                when {
                    negLhs != null -> environment.narrowComparison(comparison, args[1], negLhs)
                    negRhs != null -> environment.narrowComparison(comparison, args[0], negRhs)
                    else -> null
                }
            }
            RemoteFloatOperationKey.Abs -> {
                if (args.isEmpty()) return null
                val innerR = evaluator.evalFloatKey(args[0])
                if (innerR.isEmpty || innerR.isUnbounded) return null
                when (comparison) {
                    IntervalComparison.EQ,
                    IntervalComparison.LTE ->
                        environment.narrowComparison(IntervalComparison.EQ, args[0], otherKey)
                    IntervalComparison.GT ->
                        environment.narrowComparison(IntervalComparison.NEQ, args[0], otherKey)
                    else -> null
                }
            }
            else -> null
        }
    }

    override fun unwrapAliasKey(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): RemoteStateCacheKey? =
        if (key.op == RemoteFloatOperationKey.Reference) key.args.firstOrNull() else null

    private fun addFloatAffineArgs(
        args: List<RemoteStateCacheKey>,
        range: FloatInterval,
        negateRhs: Boolean,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): FloatAffineForm? {
        if (args.size < 2) return null
        val a = evaluator.extractFloatAffineForm(args[0], depth + 1) ?: return null
        val b = evaluator.extractFloatAffineForm(args[1], depth + 1) ?: return null
        return evaluator.combineFloatAffineAdd(a, b, range, negateRhs)
    }

    private fun scaleFloatAffineTimes(
        args: List<RemoteStateCacheKey>,
        range: FloatInterval,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): FloatAffineForm? {
        if (args.size < 2) return null
        val r0 = evaluator.evalFloatKey(args[0])
        val r1 = evaluator.evalFloatKey(args[1])
        val raw =
            when {
                r0.isPoint ->
                    evaluator.extractFloatAffineForm(args[1], depth + 1)?.scale(r0.min.toDouble())
                r1.isPoint ->
                    evaluator.extractFloatAffineForm(args[0], depth + 1)?.scale(r1.min.toDouble())
                else -> null
            } ?: return null
        val tightRange = raw.evaluate(evaluator) ?: range
        return raw.withExtraError(maxUlp(tightRange))
    }

    private fun unwrapNegatedFloatKey(key: RemoteStateCacheKey): RemoteStateCacheKey? {
        val opKey = key as? RemoteOperationCacheKey ?: return null
        val args = opKey.args
        return when (opKey.op) {
            RemoteFloatOperationKey.UnaryMinus -> args.firstOrNull()
            RemoteFloatOperationKey.Times -> {
                if (args.size < 2) return null
                val c0 = ((args[0] as? RemoteConstantCacheKey)?.value as? Number)?.toFloat()
                val c1 = ((args[1] as? RemoteConstantCacheKey)?.value as? Number)?.toFloat()
                when {
                    c1 == -1f -> args[0]
                    c0 == -1f -> args[1]
                    else -> null
                }
            }
            else -> null
        }
    }
}
