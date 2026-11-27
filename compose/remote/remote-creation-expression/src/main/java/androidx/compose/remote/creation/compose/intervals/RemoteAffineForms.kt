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

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.TriState
import java.util.HashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.ulp

internal const val MAX_AFFINE_DEPTH: Int = 16
internal const val EXACT_FLOAT32_INT_LIMIT: Int = FloatInterval.MAX_EXACT_INT
internal const val MAX_EXACT_INT_AFFINE_WEIGHT: Long = 1L shl 20

/** A symbolic affine basis variable identified by its [key] and integer/float view. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AffineTerm(public val key: RemoteStateCacheKey, public val isInt: Boolean) {
    override fun equals(other: Any?): Boolean =
        other is AffineTerm && key == other.key && isInt == other.isInt

    override fun hashCode(): Int = 31 * key.hashCode() + isInt.hashCode()

    override fun toString(): String = "AffineTerm(key=$key, isInt=$isInt)"
}

/**
 * Linear affine form `constant + sum(w_i * x_i) +/- errorBound` over symbolic [AffineTerm]s, used
 * to cancel correlated variables in float expressions and comparisons.
 *
 * [errorBound] accumulates a conservative upper bound on IEEE-754 binary32 floating-point rounding
 * error across arithmetic operations, measured in units in the last place (ULPs — the spacing
 * between adjacent representable `Float` values, computed via `Float.ulp`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FloatAffineForm(
    public val terms: Map<AffineTerm, Double>,
    public val constant: Double,
    public val errorBound: Double,
) {
    /** Returns `-this`. */
    public fun negate(): FloatAffineForm {
        val negTerms = HashMap<AffineTerm, Double>(terms.size)
        for ((k, w) in terms) negTerms[k] = -w
        return FloatAffineForm(negTerms, -constant, errorBound)
    }

    /** Returns `this * factor` with additional floating-point rounding error [roundingUlp]. */
    public fun scale(factor: Double, roundingUlp: Double = 0.0): FloatAffineForm {
        val scaledTerms = HashMap<AffineTerm, Double>(terms.size)
        for ((k, w) in terms) {
            val nw = w * factor
            if (nw != 0.0) scaledTerms[k] = nw
        }
        return FloatAffineForm(
            scaledTerms,
            constant * factor,
            errorBound * abs(factor) + roundingUlp,
        )
    }

    /** Returns `this + other` with additional floating-point rounding error [roundingUlp]. */
    public fun add(other: FloatAffineForm, roundingUlp: Double = 0.0): FloatAffineForm {
        val merged = HashMap<AffineTerm, Double>(terms.size + other.terms.size)
        for ((k, w) in terms) merged[k] = w
        for ((k, w) in other.terms) {
            val sum = (merged[k] ?: 0.0) + w
            if (sum == 0.0) merged.remove(k) else merged[k] = sum
        }
        return FloatAffineForm(
            merged,
            constant + other.constant,
            errorBound + other.errorBound + roundingUlp,
        )
    }

    /**
     * Returns the exact symbolic difference `this - other` (`roundingUlp = 0.0`, suitable for exact
     * relational comparison against zero; callers modeling runtime float subtraction should add
     * floating-point rounding error via [withExtraError]).
     */
    public fun subtract(other: FloatAffineForm): FloatAffineForm = add(other.negate(), 0.0)

    /** Returns a copy with additional rounding error [extraUlp] added to [errorBound]. */
    public fun withExtraError(extraUlp: Double): FloatAffineForm =
        FloatAffineForm(terms, constant, errorBound + extraUlp)

    /** Concretizes this affine form into a sound [FloatInterval] under [evaluator], or `null`. */
    public fun evaluate(evaluator: RemoteValueIntervalEvaluator): FloatInterval? {
        if (!constant.isFinite() || !errorBound.isFinite() || errorBound < 0.0) return null
        var lo = constant - errorBound
        var hi = constant + errorBound
        var minInc = true
        var maxInc = true
        for ((term, w) in terms) {
            if (!w.isFinite()) return null
            val termLo: Double
            val termHi: Double
            val termMinInc: Boolean
            val termMaxInc: Boolean
            if (term.isInt) {
                val iv = evaluator.evalIntKey(term.key)
                if (iv.isEmpty || !iv.hasFiniteMin || !iv.hasFiniteMax) return null
                termLo = iv.min.toDouble()
                termHi = iv.max.toDouble()
                termMinInc = true
                termMaxInc = true
            } else {
                val r = evaluator.evalFloatKey(term.key)
                if (r.isEmpty || r.isUnbounded) return null
                termLo = r.min.toDouble()
                termHi = r.max.toDouble()
                termMinInc = r.minInclusive
                termMaxInc = r.maxInclusive
            }
            when {
                w > 0.0 -> {
                    lo += w * termLo
                    hi += w * termHi
                    minInc = minInc && termMinInc
                    maxInc = maxInc && termMaxInc
                }
                w < 0.0 -> {
                    lo += w * termHi
                    hi += w * termLo
                    minInc = minInc && termMaxInc
                    maxInc = maxInc && termMinInc
                }
            }
        }
        if (!lo.isFinite() || !hi.isFinite()) return null
        val fLo = lo.toFloat().let { if (it.toDouble() > lo) it.nextDown() else it }
        val fHi = hi.toFloat().let { if (it.toDouble() < hi) it.nextUp() else it }
        if (!fLo.isFinite() || !fHi.isFinite()) return null
        val collapsed = fLo == fHi
        return FloatInterval(
            min = fLo,
            max = fHi,
            minInclusive = if (collapsed || errorBound > 0.0) true else minInc,
            maxInclusive = if (collapsed || errorBound > 0.0) true else maxInc,
        )
    }
}

/**
 * Overflow-verified 64-bit exact linear affine form `constant + sum(w_i * x_i)` over symbolic
 * integer [RemoteStateCacheKey]s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IntAffineForm(
    public val terms: Map<RemoteStateCacheKey, Long>,
    public val constant: Long,
) {
    /** Returns `-this`, or `null` if negating would overflow `Long`. */
    public fun negate(): IntAffineForm? {
        if (constant == Long.MIN_VALUE) return null
        val negTerms = HashMap<RemoteStateCacheKey, Long>(terms.size)
        for ((k, w) in terms) {
            if (w == Long.MIN_VALUE) return null
            negTerms[k] = -w
        }
        return IntAffineForm(negTerms, -constant)
    }

    /** Returns `this * factor`, or `null` on `Long` overflow. */
    public fun scale(factor: Long): IntAffineForm? {
        val newConst = safeMulLong(constant, factor) ?: return null
        val scaledTerms = HashMap<RemoteStateCacheKey, Long>(terms.size)
        for ((k, w) in terms) {
            val nw = safeMulLong(w, factor) ?: return null
            if (nw != 0L) scaledTerms[k] = nw
        }
        return IntAffineForm(scaledTerms, newConst)
    }

    /** Returns `this + other`, or `null` on `Long` overflow. */
    public fun add(other: IntAffineForm): IntAffineForm? {
        val newConst = safeAddLong(constant, other.constant) ?: return null
        val merged = HashMap<RemoteStateCacheKey, Long>(terms.size + other.terms.size)
        for ((k, w) in terms) merged[k] = w
        for ((k, w) in other.terms) {
            val sum = safeAddLong(merged[k] ?: 0L, w) ?: return null
            if (sum == 0L) merged.remove(k) else merged[k] = sum
        }
        return IntAffineForm(merged, newConst)
    }

    /** Evaluates 64-bit bounds of `this - other` under [evaluator], or `null` on overflow. */
    public fun subtractAndEvaluateLongBounds(
        other: IntAffineForm,
        evaluator: RemoteValueIntervalEvaluator,
    ): Pair<Long, Long>? = other.negate()?.let { add(it) }?.evaluateLongBounds(evaluator)

    /** Concretizes this affine form into an [IntInterval] if verified within `Int` bounds. */
    public fun evaluateIntInterval(evaluator: RemoteValueIntervalEvaluator): IntInterval? {
        val (lo, hi) = evaluateLongBounds(evaluator) ?: return null
        if (lo < Int.MIN_VALUE.toLong() || hi > Int.MAX_VALUE.toLong()) return null
        return IntInterval.fromLongBounds(lo, hi)
    }

    private fun evaluateLongBounds(evaluator: RemoteValueIntervalEvaluator): Pair<Long, Long>? {
        var lo = constant
        var hi = constant
        for ((k, w) in terms) {
            val r = evaluator.evalIntKey(k)
            if (r.isEmpty || !r.hasFiniteMin || !r.hasFiniteMax) return null
            val tLo = safeMulLong(w, if (w > 0L) r.min.toLong() else r.max.toLong()) ?: return null
            val tHi = safeMulLong(w, if (w > 0L) r.max.toLong() else r.min.toLong()) ?: return null
            lo = safeAddLong(lo, tLo) ?: return null
            hi = safeAddLong(hi, tHi) ?: return null
        }
        return Pair(lo, hi)
    }
}

internal fun RemoteValueIntervalEvaluator.extractFloatAffineForm(
    key: RemoteStateCacheKey,
    depth: Int,
): FloatAffineForm? {
    if (depth > MAX_AFFINE_DEPTH || !key.isDeterministic()) return null
    floatAffineMemo[key]?.let { (cachedDepth, cachedForm) ->
        if (cachedDepth <= depth) return cachedForm
    }
    val range = evalFloatKey(key)
    val result =
        when {
            range.isEmpty || range.isUnbounded -> null
            range.isPoint -> FloatAffineForm(emptyMap(), range.min.toDouble(), errorBound = 0.0)
            key is RemoteOperationCacheKey ->
                intervalSemanticsFor(key.op)?.extractFloatAffine(key, range, this, depth)
                    ?: FloatAffineForm(
                        mapOf(AffineTerm(key, isInt = false) to 1.0),
                        constant = 0.0,
                        errorBound = 0.0,
                    )
            else ->
                FloatAffineForm(
                    mapOf(AffineTerm(key, isInt = false) to 1.0),
                    constant = 0.0,
                    errorBound = 0.0,
                )
        }
    floatAffineMemo[key] = depth to result
    return result
}

internal fun RemoteValueIntervalEvaluator.extractIntAffineForm(
    key: RemoteStateCacheKey,
    depth: Int,
): IntAffineForm? {
    if (depth > MAX_AFFINE_DEPTH || !key.isDeterministic()) return null
    intAffineMemo[key]?.let { (cachedDepth, cachedForm) ->
        if (cachedDepth <= depth) return cachedForm
    }
    val range = evalIntKey(key)
    val result =
        when {
            range.isEmpty -> null
            range.isPoint -> IntAffineForm(emptyMap(), range.min.toLong())
            key is RemoteOperationCacheKey ->
                intervalSemanticsFor(key.op)?.extractIntAffine(key, this, depth)
                    ?: IntAffineForm(mapOf(key to 1L), constant = 0L)
            else -> IntAffineForm(mapOf(key to 1L), constant = 0L)
        }
    intAffineMemo[key] = depth to result
    return result
}

internal fun RemoteValueIntervalEvaluator.compareFloatAffineDiff(
    comparison: IntervalComparison,
    keyA: RemoteStateCacheKey,
    keyB: RemoteStateCacheKey,
): TriState {
    val formA = extractFloatAffineForm(keyA, depth = 0) ?: return TriState.UNKNOWN
    val formB = extractFloatAffineForm(keyB, depth = 0) ?: return TriState.UNKNOWN
    val diffInterval = formA.subtract(formB).evaluate(this) ?: return TriState.UNKNOWN
    return diffInterval.compare(comparison, FloatInterval.ZERO)
}

internal fun RemoteValueIntervalEvaluator.compareIntAffineDiff(
    comparison: IntervalComparison,
    keyA: RemoteStateCacheKey,
    keyB: RemoteStateCacheKey,
): TriState {
    val formA = extractIntAffineForm(keyA, depth = 0) ?: return TriState.UNKNOWN
    val formB = extractIntAffineForm(keyB, depth = 0) ?: return TriState.UNKNOWN
    val (diffMin, diffMax) =
        formA.subtractAndEvaluateLongBounds(formB, this) ?: return TriState.UNKNOWN
    return comparison.compareLongRangeAgainstZero(diffMin, diffMax)
}

internal fun RemoteValueIntervalEvaluator.combineFloatAffineAdd(
    a: FloatAffineForm,
    b: FloatAffineForm,
    range: FloatInterval,
    negateRhs: Boolean,
): FloatAffineForm {
    if (
        negateRhs &&
            a.errorBound == 0.0 &&
            b.errorBound == 0.0 &&
            a.constant == b.constant &&
            a.terms == b.terms
    ) {
        return FloatAffineForm(emptyMap(), constant = 0.0, errorBound = 0.0)
    }
    val raw = a.add(if (negateRhs) b.negate() else b, roundingUlp = 0.0)
    val tightRange = raw.evaluate(this) ?: range
    return raw.withExtraError(maxUlp(tightRange))
}

internal fun maxUlp(range: FloatInterval): Double =
    max(abs(range.min), abs(range.max)).ulp.toDouble()

private fun safeAddLong(a: Long, b: Long): Long? {
    val r = a + b
    return if (((a xor r) and (b xor r)) < 0L) null else r
}

private fun safeMulLong(a: Long, b: Long): Long? {
    if (a == 0L || b == 0L) return 0L
    if ((a == -1L && b == Long.MIN_VALUE) || (b == -1L && a == Long.MIN_VALUE)) return null
    val r = a * b
    return if (r / a != b) null else r
}
