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

import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import kotlin.math.ceil
import kotlin.math.floor

private const val MAX_REFINE_DEPTH: Int = 16
private val activeRefineDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

private inline fun withRefineDepthBudget(
    fallback: IntervalEnvironment,
    block: () -> IntervalEnvironment,
): IntervalEnvironment {
    val depth = activeRefineDepth.get()
    if (depth >= MAX_REFINE_DEPTH) return fallback
    activeRefineDepth.set(depth + 1)
    return try {
        block()
    } finally {
        activeRefineDepth.set(depth)
    }
}

/**
 * Refines [environment] by assuming the boolean expression [key] evaluates to [holds].
 *
 * Dispatches polymorphically through [RemoteIntervalOperation.refineBoolean] for composite
 * operations and falls back to narrowing direct integer/boolean variables against `0`.
 */
internal fun refineEnvironmentByBooleanKey(
    environment: IntervalEnvironment,
    key: RemoteStateCacheKey,
    holds: Boolean,
): IntervalEnvironment =
    withRefineDepthBudget(environment) {
        val evaluator = RemoteValueIntervalEvaluator(environment)
        if (key is RemoteOperationCacheKey) {
            intervalSemanticsFor(key.op)?.refineBoolean(key, holds, environment, evaluator)?.let {
                return@withRefineDepthBudget it
            }
        }
        refineDirectBoolVar(environment, evaluator, key, holds)
    }

/**
 * Narrows float operand keys [rawKeyA] and [rawKeyB] in [environment] assuming `rawKeyA
 * <comparison> rawKeyB` holds.
 */
internal fun narrowFloatOperands(
    environment: IntervalEnvironment,
    comparison: IntervalComparison,
    rawKeyA: RemoteStateCacheKey,
    rawKeyB: RemoteStateCacheKey,
): IntervalEnvironment =
    withRefineDepthBudget(environment) {
        val baseEval = RemoteValueIntervalEvaluator(environment)
        if (
            !baseEval.hasBoundedFloatOperands(rawKeyA) || !baseEval.hasBoundedFloatOperands(rawKeyB)
        ) {
            return@withRefineDepthBudget environment
        }
        val keyA = unwrapAliasKey(baseEval, rawKeyA)
        val keyB = unwrapAliasKey(baseEval, rawKeyB)
        if (keyA == keyB) return@withRefineDepthBudget environment
        val rangeA = baseEval.evalFloatKey(keyA)
        val rangeB = baseEval.evalFloatKey(keyB)
        // Skip narrowing when either operand is unbounded because UNBOUNDED permits NaN, which
        // enters negated relational branches (e.g. !(NaN < c)) and would violate narrowed real
        // bounds.
        if (rangeA.isEmpty || rangeB.isEmpty || rangeA.isUnbounded || rangeB.isUnbounded) {
            return@withRefineDepthBudget environment
        }
        val (narrowA, narrowB) = comparison.narrowFloat(rangeA, rangeB)
        var current = environment
        if (!narrowA.isEmpty && narrowA != rangeA) {
            current = applyFloatKeyNarrowing(current, keyA, narrowA)
            if (rawKeyA != keyA) current = applyFloatKeyNarrowing(current, rawKeyA, narrowA)
        }
        if (!narrowB.isEmpty && narrowB != rangeB) {
            current = applyFloatKeyNarrowing(current, keyB, narrowB)
            if (rawKeyB != keyB) current = applyFloatKeyNarrowing(current, rawKeyB, narrowB)
        }
        if (keyA is RemoteOperationCacheKey) {
            intervalSemanticsFor(keyA.op)
                ?.refineComparison(
                    keyA,
                    comparison,
                    keyB,
                    asInt = false,
                    current,
                    RemoteValueIntervalEvaluator(current),
                )
                ?.let { current = it }
        }
        if (keyB is RemoteOperationCacheKey) {
            intervalSemanticsFor(keyB.op)
                ?.refineComparison(
                    keyB,
                    comparison.flip(),
                    keyA,
                    asInt = false,
                    current,
                    RemoteValueIntervalEvaluator(current),
                )
                ?.let { current = it }
        }
        current
    }

/**
 * Narrows integer operand keys [rawKeyA] and [rawKeyB] in [environment] assuming `rawKeyA
 * <comparison> rawKeyB` holds.
 */
internal fun narrowIntOperands(
    environment: IntervalEnvironment,
    comparison: IntervalComparison,
    rawKeyA: RemoteStateCacheKey,
    rawKeyB: RemoteStateCacheKey,
): IntervalEnvironment =
    withRefineDepthBudget(environment) {
        val baseEval = RemoteValueIntervalEvaluator(environment)
        val keyA = unwrapAliasKey(baseEval, rawKeyA)
        val keyB = unwrapAliasKey(baseEval, rawKeyB)
        if (keyA == keyB) return@withRefineDepthBudget environment
        val a = baseEval.evalIntKey(keyA)
        val b = baseEval.evalIntKey(keyB)
        val (narrowA, narrowB) = comparison.narrowInt(a, b)
        var current = environment
        if (!narrowA.isEmpty && narrowA != a) {
            current = applyIntKeyNarrowing(current, keyA, narrowA)
            if (rawKeyA != keyA) current = applyIntKeyNarrowing(current, rawKeyA, narrowA)
        }
        if (!narrowB.isEmpty && narrowB != b) {
            current = applyIntKeyNarrowing(current, keyB, narrowB)
            if (rawKeyB != keyB) current = applyIntKeyNarrowing(current, rawKeyB, narrowB)
        }
        if (keyA is RemoteOperationCacheKey) {
            intervalSemanticsFor(keyA.op)
                ?.refineComparison(
                    keyA,
                    comparison,
                    keyB,
                    asInt = true,
                    current,
                    RemoteValueIntervalEvaluator(current),
                )
                ?.let { current = it }
        }
        if (keyB is RemoteOperationCacheKey) {
            intervalSemanticsFor(keyB.op)
                ?.refineComparison(
                    keyB,
                    comparison.flip(),
                    keyA,
                    asInt = true,
                    current,
                    RemoteValueIntervalEvaluator(current),
                )
                ?.let { current = it }
        }
        current
    }

internal fun applyFloatKeyNarrowing(
    environment: IntervalEnvironment,
    key: RemoteStateCacheKey,
    interval: FloatInterval,
): IntervalEnvironment =
    withRefineDepthBudget(environment) {
        if (!key.isDeterministic() || key is RemoteConstantCacheKey) {
            return@withRefineDepthBudget environment
        }
        var current = environment.withFloatNarrowing(key, interval)
        if (key is RemoteOperationCacheKey) {
            val sem = intervalSemanticsFor(key.op)
            if (sem != null) {
                current =
                    sem.propagateFloatNarrowing(
                        key,
                        interval,
                        current,
                        RemoteValueIntervalEvaluator(current),
                    )
            }
        }
        current
    }

internal fun applyIntKeyNarrowing(
    environment: IntervalEnvironment,
    key: RemoteStateCacheKey,
    interval: IntInterval,
): IntervalEnvironment {
    if (!key.isDeterministic() || key is RemoteConstantCacheKey) return environment
    return environment.withIntNarrowing(key, interval)
}

internal fun exactIntBoundsWithinFloatInterval(interval: FloatInterval): IntInterval {
    if (interval.isEmpty) return IntInterval.EMPTY
    val limit = EXACT_FLOAT32_INT_LIMIT.toFloat()
    val lo =
        if (interval.min.isFinite() && interval.min >= -limit && interval.min <= limit) {
            when {
                interval.min == -limit && interval.minInclusive ->
                    -EXACT_FLOAT32_INT_LIMIT.toLong() - 1L
                interval.minInclusive -> ceil(interval.min).toLong()
                else -> floor(interval.min).toLong() + 1L
            }
        } else {
            IntInterval.MIN_BOUND.toLong()
        }
    val hi =
        if (interval.max.isFinite() && interval.max >= -limit && interval.max <= limit) {
            when {
                interval.max == limit && interval.maxInclusive ->
                    EXACT_FLOAT32_INT_LIMIT.toLong() + 1L
                interval.maxInclusive -> floor(interval.max).toLong()
                else -> ceil(interval.max).toLong() - 1L
            }
        } else {
            IntInterval.MAX_BOUND.toLong()
        }
    return IntInterval.fromLongBounds(lo, hi)
}

private fun refineDirectBoolVar(
    environment: IntervalEnvironment,
    evaluator: RemoteValueIntervalEvaluator,
    key: RemoteStateCacheKey,
    holds: Boolean,
): IntervalEnvironment {
    val currentInt = evaluator.evalIntKey(key)
    val refinedInt =
        when {
            !holds -> currentInt.intersect(IntInterval(IntInterval.MIN_BOUND, 0))
            currentInt.min == 0 -> currentInt.intersect(IntInterval(1, currentInt.max))
            else -> currentInt
        }
    return if (!refinedInt.isEmpty && refinedInt != currentInt) {
        applyIntKeyNarrowing(environment, key, refinedInt)
    } else {
        environment
    }
}

private fun unwrapAliasKey(
    evaluator: RemoteValueIntervalEvaluator,
    start: RemoteStateCacheKey,
): RemoteStateCacheKey {
    var cur = start
    repeat(4) {
        if (cur !is RemoteOperationCacheKey) return cur
        val next = intervalSemanticsFor(cur.op)?.unwrapAliasKey(cur, evaluator) ?: return cur
        cur = next
    }
    return cur
}
