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

package androidx.compose.remote.document.intervals

import androidx.annotation.RestrictTo
import androidx.compose.remote.document.AnimationEasing
import androidx.compose.remote.document.isSpringAnimationSpec
import androidx.compose.remote.document.unpackAnimationSpec
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.sqrt
import kotlin.math.ulp
import kotlin.math.withSign

/**
 * Unpacked finite progress range and optional initial value extracted from a packed RemoteCompose
 * `FloatAnimation` description array.
 *
 * @property progressRange Sound range of the easing curve over normalized time `[0, 1]`.
 * @property initialValue Optional finite initial animation value, or `null` if unspecified.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ParsedAnimationSpec(
    public val progressRange: FloatInterval,
    public val initialValue: Float?,
) {
    override fun equals(other: Any?): Boolean =
        other is ParsedAnimationSpec &&
            progressRange == other.progressRange &&
            canonicalNullableFloatBits(initialValue) ==
                canonicalNullableFloatBits(other.initialValue)

    override fun hashCode(): Int {
        var result = progressRange.hashCode()
        result = 31 * result + (canonicalNullableFloatBits(initialValue) ?: 0)
        return result
    }

    override fun toString(): String =
        "ParsedAnimationSpec(progressRange=$progressRange, initialValue=$initialValue)"

    private companion object {
        private fun canonicalNullableFloatBits(value: Float?): Int? =
            when {
                value == null -> null
                value == 0f -> 0
                value.isNaN() -> Float.NaN.toBits()
                else -> value.toBits()
            }
    }
}

/**
 * Pure-Kotlin sound interval bounds for RemoteCompose float animations and CSS cubic-bezier easing
 * curves, matching `FloatAnimation` and `CubicEasing` in `remote-core`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object FloatAnimationBounds {
    private const val QUADRATIC_EPS = 1e-12
    private const val CUBIC_OVERSHOOT_ROUNDING_TOL = 1e-5
    private const val CUBIC_EASING_ERROR = 0.01f

    /**
     * Returns `true` when [spec] requires conservative fallback to [FloatInterval.UNBOUNDED] (such
     * as spring, wrap, elastic/spline, non-positive duration, or non-monotonic cubic `x1`/`x2`).
     */
    @JvmStatic
    public fun isConservativelyTaintedAnimation(spec: FloatArray): Boolean =
        parseAnimationSpec(spec) == null

    /**
     * Unpacks [spec] into its progress range and optional initial value, or returns `null` when
     * conservative fallback to [FloatInterval.UNBOUNDED] is required.
     */
    @JvmStatic
    public fun parseAnimationSpec(spec: FloatArray): ParsedAnimationSpec? {
        if (isSpringAnimationSpec(spec)) return null
        if (spec.isNotEmpty() && (!spec[0].isFinite() || spec[0] <= 0f)) return null
        if (spec.size <= 1) {
            return ParsedAnimationSpec(progressRange = FloatInterval.UNIT, initialValue = null)
        }

        val unpacked =
            try {
                unpackAnimationSpec(spec)
            } catch (_: IllegalArgumentException) {
                return null
            }
        if (!unpacked.wrap.isNaN()) return null
        val initialValue =
            if (!unpacked.initialValue.isNaN()) {
                if (unpacked.initialValue.isInfinite()) return null
                unpacked.initialValue
            } else {
                null
            }
        val pRange = progressRangeForEasingType(unpacked.type, unpacked.params) ?: return null
        return ParsedAnimationSpec(progressRange = pRange, initialValue = initialValue)
    }

    /**
     * Computes a sound interval bound for an animated float expression with packed [spec] and
     * statically bounded [targetInterval].
     */
    @JvmStatic
    public fun boundAnimatedFloat(
        spec: FloatArray,
        targetInterval: FloatInterval,
    ): FloatInterval {
        val parsed = parseAnimationSpec(spec) ?: return FloatInterval.UNBOUNDED
        if (targetInterval.isEmpty) return FloatInterval.EMPTY
        if (targetInterval.isUnbounded) return FloatInterval.UNBOUNDED

        val startInterval =
            parsed.initialValue?.let { targetInterval.union(FloatInterval.point(it)) }
                ?: targetInterval
        return interpolateBox(startInterval, targetInterval, parsed.progressRange)
    }

    /**
     * Evaluates `CubicEasing(x1, y1, x2, y2).get(x)` in pure Kotlin matching `remote-core`
     * bit-for-bit.
     */
    @JvmStatic
    public fun evaluateCubicEasing(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
        if (x <= 0.0f) return 0f
        if (x >= 1.0f) return 1.0f
        var t = 0.5f
        var range = 0.5f
        while (range > CUBIC_EASING_ERROR) {
            val tx = bezierCoord(x1, x2, t)
            range *= 0.5f
            if (tx < x) {
                t += range
            } else {
                t -= range
            }
        }
        val bx1 = bezierCoord(x1, x2, t - range)
        val bx2 = bezierCoord(x1, x2, t + range)
        val by1 = bezierCoord(y1, y2, t - range)
        val by2 = bezierCoord(y1, y2, t + range)
        return (by2 - by1) * (x - bx1) / (bx2 - bx1) + by1
    }

    /**
     * Soundly bounds `CubicEasing(x1, y1, x2, y2).get(pos)` matching
     * `AnimatedFloatExpression.CUBIC`.
     */
    @JvmStatic
    public fun evalCubicEasingInterval(
        x1: FloatInterval,
        y1: FloatInterval,
        x2: FloatInterval,
        y2: FloatInterval,
        pos: FloatInterval,
    ): FloatInterval {
        if (x1.isEmpty || y1.isEmpty || x2.isEmpty || y2.isEmpty || pos.isEmpty) {
            return FloatInterval.EMPTY
        }
        if (x1.isUnbounded || y1.isUnbounded || x2.isUnbounded || y2.isUnbounded) {
            return FloatInterval.UNBOUNDED
        }
        if (x1.min < 0f || x1.max > 1f || x2.min < 0f || x2.max > 1f) {
            return FloatInterval.UNBOUNDED
        }
        if (pos.max <= 0f) return FloatInterval.ZERO
        if (pos.min >= 1f) return FloatInterval.point(1f)

        if (x1.isPoint && y1.isPoint && x2.isPoint && y2.isPoint) {
            if (pos.isPoint) {
                val exact = evaluateCubicEasing(x1.min, y1.min, x2.min, y2.min, pos.min)
                return if (exact.isFinite()) FloatInterval.point(exact) else FloatInterval.UNBOUNDED
            } else if (y1.min in 0f..1f && y2.min in 0f..1f) {
                val lo = evaluateCubicEasing(x1.min, y1.min, x2.min, y2.min, pos.min)
                val hi = evaluateCubicEasing(x1.min, y1.min, x2.min, y2.min, pos.max)
                if (!lo.isFinite() || !hi.isFinite()) return FloatInterval.UNBOUNDED
                val minOut = if (lo <= 0f) 0f else max(0f, min(lo, hi).nextDown())
                val maxOut = if (hi >= 1f) 1f else min(1f, max(lo, hi).nextUp())
                return FloatInterval(minOut, maxOut)
            }
        }

        val loBounds = cubicBezierYBounds(y1.min, y2.min)
        val hiBounds = cubicBezierYBounds(y1.max, y2.max)
        return FloatInterval(min(loBounds.min, hiBounds.min), max(loBounds.max, hiBounds.max))
    }

    /**
     * Soundly bounds the y-coordinate of cubic Bezier `(0, 0) -> (x1, y1) -> (x2, y2) -> (1, 1)`
     * over `t in [0, 1]` using closed-form derivative roots.
     */
    @JvmStatic
    public fun cubicBezierYBounds(y1: Float, y2: Float): FloatInterval {
        if (!y1.isFinite() || !y2.isFinite()) return FloatInterval.UNBOUNDED
        if (y1 in 0f..1f && y2 in 0f..1f) return FloatInterval.UNIT

        val dy1 = y1.toDouble()
        val dy2 = y2.toDouble()
        var lo = 0.0
        var hi = 1.0

        val a = 3.0 * dy1 - 3.0 * dy2 + 1.0
        val b = 2.0 * (dy2 - 2.0 * dy1)
        val c = dy1

        fun checkRoot(t: Double) {
            if (t <= 0.0 || t >= 1.0) return
            val t1 = 1.0 - t
            val y = 3.0 * t1 * t1 * t * dy1 + 3.0 * t1 * t * t * dy2 + t * t * t
            if (y < lo) lo = y
            if (y > hi) hi = y
        }

        if (abs(a) < QUADRATIC_EPS) {
            if (abs(b) > QUADRATIC_EPS) checkRoot(-c / b)
        } else {
            val disc = b * b - 4.0 * a * c
            if (disc >= 0.0) {
                val root = sqrt(disc)
                // Citardauq stable root formula avoids catastrophic cancellation when b^2 >> 4ac.
                val q = -0.5 * (b + root.withSign(b))
                if (q != 0.0) {
                    checkRoot(q / a)
                    checkRoot(c / q)
                }
            }
        }

        val paddedLo = if (lo < 0.0) lo - CUBIC_OVERSHOOT_ROUNDING_TOL * max(1.0, abs(lo)) else lo
        val paddedHi =
            if (hi > 1.0 || dy1 < 0.0 || dy2 < 0.0) {
                hi + CUBIC_OVERSHOOT_ROUNDING_TOL * max(1.0, max(abs(hi), abs(lo)))
            } else {
                hi
            }
        val fLo = paddedLo.toFloat().let { if (it.toDouble() > paddedLo) it.nextDown() else it }
        val fHi = paddedHi.toFloat().let { if (it.toDouble() < paddedHi) it.nextUp() else it }
        if (!fLo.isFinite() || !fHi.isFinite()) return FloatInterval.UNBOUNDED
        return FloatInterval(fLo, fHi)
    }

    private fun bezierCoord(c1: Float, c2: Float, t: Float): Float {
        val t1 = 1f - t
        val f1 = 3f * t1 * t1 * t
        val f2 = 3f * t1 * t * t
        val f3 = t * t * t
        return c1 * f1 + c2 * f2 + f3
    }

    private fun progressRangeForEasingType(type: Int, params: FloatArray): FloatInterval? {
        return when (type) {
            AnimationEasing.CUBIC_STANDARD,
            AnimationEasing.CUBIC_ACCELERATE,
            AnimationEasing.CUBIC_DECELERATE,
            AnimationEasing.CUBIC_LINEAR,
            AnimationEasing.EASE_OUT_BOUNCE -> FloatInterval.UNIT
            AnimationEasing.CUBIC_ANTICIPATE,
            AnimationEasing.CUBIC_OVERSHOOT -> {
                val pts = AnimationEasing.cubicControlPoints(type) ?: return null
                cubicBezierYBounds(pts[1], pts[3])
            }
            AnimationEasing.CUBIC_CUSTOM -> {
                if (params.size < 4) return null
                val x1 = params[0]
                val y1 = params[1]
                val x2 = params[2]
                val y2 = params[3]
                if (x1 !in 0f..1f || x2 !in 0f..1f || !y1.isFinite() || !y2.isFinite()) return null
                if (y1 in 0f..1f && y2 in 0f..1f) FloatInterval.UNIT else cubicBezierYBounds(y1, y2)
            }
            else -> null
        }
    }

    private fun interpolateBox(
        start: FloatInterval,
        target: FloatInterval,
        progress: FloatInterval,
    ): FloatInterval {
        if (start.isPoint && target.isPoint && progress.isPoint) {
            val fVal = progress.min * (target.min - start.min) + start.min
            return if (fVal.isFinite()) FloatInterval(fVal, fVal) else FloatInterval.UNBOUNDED
        }
        if (
            start.isPoint &&
                target.isPoint &&
                start.min == target.min &&
                progress.min.isFinite() &&
                progress.max.isFinite()
        ) {
            return FloatInterval(start.min, start.min)
        }

        // Exact real-valued hull over the trilinear box s + p * (t - s) in Double.
        val sVals = floatArrayOf(start.min, start.max)
        val tVals = floatArrayOf(target.min, target.max)
        val pVals = floatArrayOf(progress.min, progress.max)
        var loD = Double.POSITIVE_INFINITY
        var hiD = Double.NEGATIVE_INFINITY
        for (s in sVals) {
            for (t in tVals) {
                for (p in pVals) {
                    val dVal = s.toDouble() + p.toDouble() * (t.toDouble() - s.toDouble())
                    if (!dVal.isFinite()) return FloatInterval.UNBOUNDED
                    if (dVal < loD) loD = dVal
                    if (dVal > hiD) hiD = dVal
                }
            }
        }

        // Sterbenz's Lemma: if all s, t share a strict sign and max(|s|, |t|) <= 2 * min(|s|, |t|),
        // then d = fl(t - s) is exact in IEEE-754 binary32. Combined with p in [0, 1], rounding
        // monotonicity guarantees fl(fl(p * d) + s) lies inside [min(s, t), max(s, t)].
        val sterbenz =
            (start.min > 0f &&
                target.min > 0f &&
                max(start.max.toDouble(), target.max.toDouble()) <=
                    2.0 * min(start.min.toDouble(), target.min.toDouble())) ||
                (start.max < 0f &&
                    target.max < 0f &&
                    max(-start.min.toDouble(), -target.min.toDouble()) <=
                        2.0 * min(-start.max.toDouble(), -target.max.toDouble()))
        val unitProgress = progress.min >= 0f && progress.max <= 1f
        if (sterbenz && unitProgress) {
            return FloatInterval(min(start.min, target.min), max(start.max, target.max))
        }

        // Bound worst-case IEEE-754 binary32 rounding error across the three operations:
        //   d = fl(t - s), m = fl(p * d), r = fl(m + s).
        val floatMaxD = Float.MAX_VALUE.toDouble()
        val dLo = target.min.toDouble() - start.max.toDouble()
        val dHi = target.max.toDouble() - start.min.toDouble()
        val maxAbsD = max(abs(dLo), abs(dHi))
        if (!maxAbsD.isFinite() || maxAbsD > floatMaxD) return FloatInterval.UNBOUNDED
        val e1 =
            if (sterbenz || maxAbsD == 0.0) {
                0.0
            } else {
                val maxDF =
                    maxAbsD.toFloat().let { if (it.toDouble() < maxAbsD) it.nextUp() else it }
                if (!maxDF.isFinite()) return FloatInterval.UNBOUNDED
                0.5 * maxDF.ulp.toDouble()
            }
        val dMax = (maxAbsD + e1).nextUp()
        if (!dMax.isFinite() || dMax > floatMaxD) return FloatInterval.UNBOUNDED

        val maxP = max(abs(progress.min.toDouble()), abs(progress.max.toDouble()))
        val prodMax = (maxP * dMax).nextUp()
        if (!prodMax.isFinite() || prodMax > floatMaxD) return FloatInterval.UNBOUNDED
        val prodMaxF = prodMax.toFloat().let { if (it.toDouble() < prodMax) it.nextUp() else it }
        if (!prodMaxF.isFinite()) return FloatInterval.UNBOUNDED
        val mulRoundErr = 0.5 * prodMaxF.ulp.toDouble()
        val e2 = (maxP * e1 + mulRoundErr).nextUp()
        val mMax = (prodMax + mulRoundErr).nextUp()
        if (!mMax.isFinite() || mMax > floatMaxD) return FloatInterval.UNBOUNDED

        val maxS = max(abs(start.min.toDouble()), abs(start.max.toDouble()))
        val sumMax = (mMax + maxS).nextUp()
        if (!sumMax.isFinite() || sumMax > floatMaxD) return FloatInterval.UNBOUNDED
        val sumMaxF = sumMax.toFloat().let { if (it.toDouble() < sumMax) it.nextUp() else it }
        if (!sumMaxF.isFinite()) return FloatInterval.UNBOUNDED
        // Include 4 * sumMax.ulp to soundly cover any Double rounding in loD/hiD.
        val e3 = (e2 + 0.5 * sumMaxF.ulp.toDouble() + 4.0 * sumMax.ulp).nextUp()

        val rawLo = (loD - e3).nextDown()
        val rawHi = (hiD + e3).nextUp()
        var outLo = rawLo.toFloat().let { if (it.toDouble() > rawLo) it.nextDown() else it }
        var outHi = rawHi.toFloat().let { if (it.toDouble() < rawHi) it.nextUp() else it }
        if (!outLo.isFinite() || !outHi.isFinite()) return FloatInterval.UNBOUNDED

        // When p in [0, 1] and s, t >= 0 (resp. <= 0) without overflow, rounding monotonicity
        // guarantees fl(fl(p * fl(t - s)) + s) >= 0f (resp. <= 0f).
        if (unitProgress) {
            if (start.min >= 0f && target.min >= 0f) outLo = max(outLo, 0f)
            if (start.max <= 0f && target.max <= 0f) outHi = min(outHi, 0f)
        }
        return FloatInterval(outLo, outHi)
    }
}
