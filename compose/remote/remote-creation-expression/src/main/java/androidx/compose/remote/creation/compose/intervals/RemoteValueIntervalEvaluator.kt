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
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.state.BaseRemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColorConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteNamedCacheKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateIdKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.cacheKey
import androidx.compose.remote.document.intervals.ColorInterval
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.StringSummary
import androidx.compose.remote.document.intervals.TriState
import java.util.HashMap
import java.util.HashSet
import kotlin.math.max
import kotlin.math.min

/**
 * Memoized, polymorphic interval evaluator over [RemoteValue] expression DAGs ([RemoteFloat],
 * [RemoteInt], [RemoteBoolean], [RemoteString], [RemoteColor]), backed by an [IntervalEnvironment].
 *
 * Built-in operation semantics are dispatched externally via isolated `Remote*IntervalSemantics`
 * handlers (`intervalSemanticsFor`), while custom external operations can opt in via
 * [RemoteIntervalOperation].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteValueIntervalEvaluator
internal constructor(
    public val environment: IntervalEnvironment,
    internal val selectDepth: Int,
) {
    /** Creates an interval evaluator backed by [environment]. */
    @JvmOverloads
    public constructor(
        environment: IntervalEnvironment = MapIntervalEnvironment.EMPTY
    ) : this(environment = environment, selectDepth = 0)

    private val floatMemo = HashMap<RemoteStateCacheKey, FloatInterval>()
    private val intMemo = HashMap<RemoteStateCacheKey, IntInterval>()
    private val boolMemo = HashMap<RemoteStateCacheKey, TriState>()
    private val stringMemo = HashMap<RemoteStateCacheKey, StringSummary>()
    private val colorMemo = HashMap<RemoteStateCacheKey, ColorInterval>()

    private val visitingFloat = HashSet<RemoteStateCacheKey>()
    private val visitingInt = HashSet<RemoteStateCacheKey>()
    private val visitingBool = HashSet<RemoteStateCacheKey>()
    private val visitingString = HashSet<RemoteStateCacheKey>()
    private val visitingColor = HashSet<RemoteStateCacheKey>()

    internal val floatEvaluator: AnimatedFloatExpression = AnimatedFloatExpression()
    internal val intEvaluator: IntegerExpressionEvaluator = IntegerExpressionEvaluator()

    /** Evaluates a sound [FloatInterval] bounding [value]. */
    public fun evalFloat(value: RemoteFloat): FloatInterval {
        value.constantValueOrNull?.let {
            return FloatInterval.point(it)
        }
        return evalFloatKey(value.cacheKey)
    }

    /** Evaluates a sound [IntInterval] bounding [value]. */
    public fun evalInt(value: RemoteInt): IntInterval {
        value.constantValueOrNull?.let {
            return IntInterval.point(it)
        }
        return evalIntKey(value.cacheKey)
    }

    /** Evaluates a sound [TriState] truth value for [value]. */
    public fun evalBoolean(value: RemoteBoolean): TriState {
        value.constantValueOrNull?.let {
            return TriState.of(it)
        }
        return evalBoolKey(value.cacheKey)
    }

    /** Evaluates a sound [StringSummary] bounding [value]. */
    public fun evalString(value: RemoteString): StringSummary {
        value.constantValueOrNull?.let {
            return StringSummary.constant(it)
        }
        return evalStringKey(value.cacheKey)
    }

    /** Evaluates a sound [ColorInterval] bounding [value]. */
    public fun evalColor(value: RemoteColor): ColorInterval {
        value.constantValueOrNull?.let {
            return ColorInterval.constant(it)
        }
        return evalColorKey(value.cacheKey)
    }

    /** Evaluates a sound [FloatInterval] for [key] with cycle-safe DAG memoization. */
    public fun evalFloatKey(key: RemoteStateCacheKey): FloatInterval {
        environment.floatOverride(key)?.let {
            return it
        }
        floatMemo[key]?.let {
            return it
        }
        if (!visitingFloat.add(key)) return FloatInterval.UNBOUNDED
        return try {
            computeFloatKey(key).also { floatMemo[key] = it }
        } finally {
            visitingFloat.remove(key)
        }
    }

    /** Evaluates a sound [IntInterval] for [key] with cycle-safe DAG memoization. */
    public fun evalIntKey(key: RemoteStateCacheKey): IntInterval {
        environment.intOverride(key)?.let {
            return it
        }
        intMemo[key]?.let {
            return it
        }
        if (!visitingInt.add(key)) return IntInterval.UNBOUNDED
        return try {
            computeIntKey(key).also { intMemo[key] = it }
        } finally {
            visitingInt.remove(key)
        }
    }

    /** Evaluates a sound [TriState] truth value for [key] with cycle-safe DAG memoization. */
    public fun evalBoolKey(key: RemoteStateCacheKey): TriState {
        boolMemo[key]?.let {
            return it
        }
        if (!visitingBool.add(key)) return TriState.UNKNOWN
        return try {
            computeBoolKey(key).also { boolMemo[key] = it }
        } finally {
            visitingBool.remove(key)
        }
    }

    /** Evaluates a sound [StringSummary] for [key] with cycle-safe DAG memoization. */
    public fun evalStringKey(key: RemoteStateCacheKey): StringSummary {
        stringMemo[key]?.let {
            return it
        }
        if (!visitingString.add(key)) return StringSummary.UNKNOWN
        return try {
            computeStringKey(key).also { stringMemo[key] = it }
        } finally {
            visitingString.remove(key)
        }
    }

    /** Evaluates a sound [ColorInterval] for [key] with cycle-safe DAG memoization. */
    public fun evalColorKey(key: RemoteStateCacheKey): ColorInterval {
        colorMemo[key]?.let {
            return it
        }
        if (!visitingColor.add(key)) return ColorInterval.UNKNOWN
        return try {
            computeColorKey(key).also { colorMemo[key] = it }
        } finally {
            visitingColor.remove(key)
        }
    }

    /**
     * Evaluates `keyA <comparison> keyB` as floats using interval bounds and deterministic
     * self-comparison checks.
     */
    public fun compareFloatKeys(
        comparison: IntervalComparison,
        keyA: RemoteStateCacheKey,
        keyB: RemoteStateCacheKey,
    ): TriState {
        val intervalA = evalFloatKey(keyA)
        val intervalB = evalFloatKey(keyB)
        val direct = intervalA.compare(comparison, intervalB)
        if (direct != TriState.UNKNOWN) return direct
        if (
            keyA == keyB && !intervalA.isEmpty && !intervalA.isUnbounded && keyA.isDeterministic()
        ) {
            return comparison.selfComparisonResult()
        }
        return TriState.UNKNOWN
    }

    /**
     * Evaluates `keyA <comparison> keyB` as integers using interval bounds and deterministic
     * self-comparison checks.
     */
    public fun compareIntKeys(
        comparison: IntervalComparison,
        keyA: RemoteStateCacheKey,
        keyB: RemoteStateCacheKey,
    ): TriState {
        val intervalA = evalIntKey(keyA)
        val intervalB = evalIntKey(keyB)
        val direct = intervalA.compare(comparison, intervalB)
        if (direct != TriState.UNKNOWN) return direct
        if (keyA == keyB && !intervalA.isEmpty && keyA.isDeterministic()) {
            return comparison.selfComparisonResult()
        }
        return TriState.UNKNOWN
    }

    internal fun evalUnaryFloatExactOrUnbounded(x: FloatInterval, op: Float): FloatInterval {
        if (x.isPoint) {
            val v = floatEvaluator.eval(floatArrayOf(x.min, op))
            if (v.isFinite()) return FloatInterval.point(v)
        }
        return FloatInterval.UNBOUNDED
    }

    internal fun evalBinaryFloatExactOrRange(
        a: FloatInterval,
        b: FloatInterval,
        op: Float,
        fallback: FloatInterval,
    ): FloatInterval {
        if (a.isPoint && b.isPoint) {
            val v = floatEvaluator.eval(floatArrayOf(a.min, b.min, op))
            if (v.isFinite()) return FloatInterval.point(v)
        }
        return fallback
    }

    internal fun evalPointUnaryIntOp(a: IntInterval, opCode: Int): IntInterval =
        when {
            a.isEmpty -> IntInterval.EMPTY
            a.isPoint -> IntInterval.point(intEvaluator.eval(1 shl 1, intArrayOf(a.min, opCode)))
            else -> IntInterval.UNBOUNDED
        }

    internal fun evalPointBinaryIntOp(a: IntInterval, b: IntInterval, opCode: Int): IntInterval =
        when {
            a.isEmpty || b.isEmpty -> IntInterval.EMPTY
            a.isPoint && b.isPoint ->
                IntInterval.point(intEvaluator.eval(1 shl 2, intArrayOf(a.min, b.min, opCode)))
            else -> IntInterval.UNBOUNDED
        }

    internal fun unionFloatArrayElements(
        elems: List<RemoteStateCacheKey>,
        idxInterval: IntInterval,
    ): FloatInterval {
        if (idxInterval.isEmpty) return FloatInterval.EMPTY
        if (elems.isEmpty()) return FloatInterval.ZERO
        val start = if (idxInterval.hasFiniteMin) max(0, idxInterval.min) else 0
        val end =
            if (idxInterval.hasFiniteMax) min(elems.size - 1, idxInterval.max) else elems.size - 1
        var acc: FloatInterval? = null
        for (i in start..end) {
            val elem = evalFloatKey(elems[i])
            acc = acc?.union(elem) ?: elem
        }
        if (
            !idxInterval.hasFiniteMin ||
                idxInterval.min < 0 ||
                !idxInterval.hasFiniteMax ||
                idxInterval.max >= elems.size
        ) {
            acc = acc?.union(FloatInterval.ZERO) ?: FloatInterval.ZERO
        }
        return acc ?: FloatInterval.ZERO
    }

    internal fun unionIntArrayElements(
        elems: List<RemoteStateCacheKey>,
        idxInterval: IntInterval,
    ): IntInterval {
        if (idxInterval.isEmpty) return IntInterval.EMPTY
        if (elems.isEmpty()) return IntInterval.ZERO
        val start = if (idxInterval.hasFiniteMin) max(0, idxInterval.min) else 0
        val end =
            if (idxInterval.hasFiniteMax) min(elems.size - 1, idxInterval.max) else elems.size - 1
        var acc: IntInterval? = null
        for (i in start..end) {
            val elem = evalIntKey(elems[i])
            acc = acc?.union(elem) ?: elem
        }
        if (
            !idxInterval.hasFiniteMin ||
                idxInterval.min < 0 ||
                !idxInterval.hasFiniteMax ||
                idxInterval.max >= elems.size
        ) {
            acc = acc?.union(IntInterval.ZERO) ?: IntInterval.ZERO
        }
        return acc ?: IntInterval.ZERO
    }

    internal fun unionStringArrayElements(
        elems: List<RemoteStateCacheKey>,
        idxInterval: IntInterval,
    ): StringSummary {
        if (
            elems.isEmpty() ||
                idxInterval.isEmpty ||
                !idxInterval.hasFiniteMin ||
                idxInterval.min < 0 ||
                !idxInterval.hasFiniteMax ||
                idxInterval.max >= elems.size
        ) {
            return StringSummary.UNKNOWN
        }
        var acc: StringSummary? = null
        for (i in idxInterval.min..idxInterval.max) {
            val elem = evalStringKey(elems[i])
            acc = acc?.union(elem) ?: elem
        }
        return acc ?: StringSummary.UNKNOWN
    }

    internal inline fun <T> evalBooleanBranch(
        condKey: RemoteStateCacheKey,
        holds: Boolean,
        evalFn: (RemoteValueIntervalEvaluator) -> T,
    ): T {
        val refined = refineEnvironmentForBooleanBranch(condKey, holds)
        return if (refined !== environment) {
            evalFn(RemoteValueIntervalEvaluator(refined, selectDepth + 1))
        } else {
            evalFn(this)
        }
    }

    internal inline fun <T> evalComparisonBranch(
        isInt: Boolean,
        comparison: IntervalComparison,
        keyA: RemoteStateCacheKey,
        keyB: RemoteStateCacheKey,
        holds: Boolean,
        evalFn: (RemoteValueIntervalEvaluator) -> T,
    ): T {
        val refined = refineEnvironmentForComparisonBranch(isInt, comparison, keyA, keyB, holds)
        return if (refined !== environment) {
            evalFn(RemoteValueIntervalEvaluator(refined, selectDepth + 1))
        } else {
            evalFn(this)
        }
    }

    internal fun refineEnvironmentForBooleanBranch(
        @Suppress("UNUSED_PARAMETER") condKey: RemoteStateCacheKey,
        @Suppress("UNUSED_PARAMETER") holds: Boolean,
    ): IntervalEnvironment = environment

    internal fun refineEnvironmentForComparisonBranch(
        @Suppress("UNUSED_PARAMETER") isInt: Boolean,
        @Suppress("UNUSED_PARAMETER") comparison: IntervalComparison,
        @Suppress("UNUSED_PARAMETER") keyA: RemoteStateCacheKey,
        @Suppress("UNUSED_PARAMETER") keyB: RemoteStateCacheKey,
        @Suppress("UNUSED_PARAMETER") holds: Boolean,
    ): IntervalEnvironment = environment

    private fun computeFloatKey(key: RemoteStateCacheKey): FloatInterval =
        when (key) {
            is RemoteConstantCacheKey ->
                when (val v = key.value) {
                    is Number -> FloatInterval.point(v.toFloat())
                    is Boolean -> FloatInterval.point(if (v) 1f else 0f)
                    else -> FloatInterval.UNBOUNDED
                }
            is RemoteStateIdKey -> environment.floatForStateId(key.id)
            is RemoteNamedCacheKey -> environment.floatForNamed(key)
            is RemoteOperationCacheKey ->
                intervalSemanticsFor(key.op)?.evaluateFloatInterval(key, this)
                    ?: FloatInterval.UNBOUNDED
            else -> {
                val constVal = (key as? BaseRemoteStateCacheKey)?.getState()?.constantValueOrNull
                if (constVal is Number) {
                    FloatInterval.point(constVal.toFloat())
                } else {
                    FloatInterval.UNBOUNDED
                }
            }
        }

    private fun computeIntKey(key: RemoteStateCacheKey): IntInterval =
        when (key) {
            is RemoteConstantCacheKey ->
                when (val v = key.value) {
                    is Number -> IntInterval.point(v.toInt())
                    is Boolean -> IntInterval.point(if (v) 1 else 0)
                    is Enum<*> -> IntInterval.point(v.ordinal)
                    else -> IntInterval.UNBOUNDED
                }
            is RemoteStateIdKey -> environment.intForStateId(key.id)
            is RemoteNamedCacheKey -> environment.intForNamed(key)
            is RemoteOperationCacheKey ->
                intervalSemanticsFor(key.op)?.evaluateIntInterval(key, this)
                    ?: IntInterval.UNBOUNDED
            else -> {
                when (
                    val constVal =
                        (key as? BaseRemoteStateCacheKey)?.getState()?.constantValueOrNull
                ) {
                    is Number -> IntInterval.point(constVal.toInt())
                    is Boolean -> IntInterval.point(if (constVal) 1 else 0)
                    is Enum<*> -> IntInterval.point(constVal.ordinal)
                    else -> IntInterval.UNBOUNDED
                }
            }
        }

    private fun computeBoolKey(key: RemoteStateCacheKey): TriState {
        if (key is RemoteConstantCacheKey) {
            (key.value as? Boolean)?.let {
                return TriState.of(it)
            }
        }
        if (key is RemoteOperationCacheKey) {
            intervalSemanticsFor(key.op)?.evaluateBooleanInterval(key, this)?.let {
                return it
            }
        }
        val intRange = evalIntKey(key)
        return when {
            intRange.isEmpty -> TriState.UNKNOWN
            intRange.isPoint && intRange.min == 0 -> TriState.FALSE
            // Require strictly positive so truthiness agrees with both != 0 and wire I_IFELSE (>
            // 0).
            intRange.min > 0 -> TriState.TRUE
            else -> TriState.UNKNOWN
        }
    }

    private fun computeStringKey(key: RemoteStateCacheKey): StringSummary =
        when (key) {
            is RemoteConstantCacheKey ->
                (key.value as? String)?.let { StringSummary.constant(it) } ?: StringSummary.UNKNOWN
            is RemoteStateIdKey -> environment.stringForStateId(key.id)
            is RemoteNamedCacheKey -> environment.stringForNamed(key)
            is RemoteOperationCacheKey ->
                intervalSemanticsFor(key.op)?.evaluateStringSummary(key, this)
                    ?: StringSummary.UNKNOWN
            else -> {
                val constVal = (key as? BaseRemoteStateCacheKey)?.getState()?.constantValueOrNull
                if (constVal is String) StringSummary.constant(constVal) else StringSummary.UNKNOWN
            }
        }

    private fun computeColorKey(key: RemoteStateCacheKey): ColorInterval =
        when (key) {
            is RemoteColorConstantCacheKey -> ColorInterval.constant(key.color)
            is RemoteConstantCacheKey ->
                (key.value as? Int)?.let { ColorInterval.constant(it) } ?: ColorInterval.UNKNOWN
            is RemoteStateIdKey -> environment.colorForStateId(key.id)
            is RemoteNamedCacheKey -> environment.colorForNamed(key)
            is RemoteOperationCacheKey ->
                intervalSemanticsFor(key.op)?.evaluateColorInterval(key, this)
                    ?: ColorInterval.UNKNOWN
            else -> {
                val constVal = (key as? BaseRemoteStateCacheKey)?.getState()?.constantValueOrNull
                if (constVal is Int) ColorInterval.constant(constVal) else ColorInterval.UNKNOWN
            }
        }
}
