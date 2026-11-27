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
import androidx.compose.remote.creation.compose.state.BooleanOperationKey
import androidx.compose.remote.creation.compose.state.ColorOperationKey
import androidx.compose.remote.creation.compose.state.EnumOperationKey
import androidx.compose.remote.creation.compose.state.FloatArrayOperationKey
import androidx.compose.remote.creation.compose.state.IntArrayOperationKey
import androidx.compose.remote.creation.compose.state.RemoteFloatOperationKey
import androidx.compose.remote.creation.compose.state.RemoteIntOperationKey
import androidx.compose.remote.creation.compose.state.RemoteOperation
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.creation.compose.state.StringArrayOperationKey
import androidx.compose.remote.creation.compose.state.StringOperationKey
import androidx.compose.remote.document.intervals.ColorInterval
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.StringSummary
import androidx.compose.remote.document.intervals.TriState

/**
 * Optional extension of [RemoteOperation] for custom external operations that participate in
 * interval analysis, condition refinement, and interval-guided expression simplification.
 *
 * Built-in `Remote*` operations stay decoupled from interval analysis and have their semantics
 * supplied externally by isolated `Remote*IntervalSemantics` handlers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteIntervalOperation : RemoteOperation {
    /**
     * Reports whether interval arithmetic on this operation can turn an unbounded float operand
     * (which may represent `NaN` or `Infinity` at runtime) into a bounded or half-bounded interval
     * even though the runtime operation may still yield `NaN` or non-finite values (for example,
     * `UNBOUNDED * 0f == [0, 0]`, `clamp`, `lerp`, `min`, `max`, `sin`, or `exp`).
     */
    public val requiresBoundedFloatOperands: Boolean
        get() = false

    /**
     * Relational [IntervalComparison] evaluated by this two-operand comparison operation, or `null`
     * if this operation is not a relational comparison.
     */
    public val comparisonCondition: IntervalComparison?
        get() = null

    /**
     * Relational [IntervalComparison] guarded by this 4-arg `selectIf*` operation, or `null` if
     * this operation is not a relational select.
     */
    public val selectCondition: IntervalComparison?
        get() = null

    /**
     * Domain ([RemoteValueKind]) in which this 4-arg `selectIf*` operation compares its first two
     * arguments ([args]). Defaults to [RemoteValueKind.OTHER] so cross-type selects do not silently
     * conflate their branch [resultKind] with their comparison domain.
     */
    public fun selectComparisonKind(args: List<RemoteStateCacheKey>): RemoteValueKind =
        RemoteValueKind.OTHER

    /** Reports whether this operation is a 3-arg boolean-condition `select(ifTrue, ifFalse)`. */
    public val isBooleanSelect: Boolean
        get() = false

    /**
     * Evaluates a conservative [FloatInterval] for an invocation of this operation ([key]) under
     * [evaluator]. Defaults to [FloatInterval.UNBOUNDED].
     */
    public fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval = FloatInterval.UNBOUNDED

    /**
     * Evaluates a conservative [IntInterval] for an invocation of this operation ([key]) under
     * [evaluator]. Defaults to [IntInterval.UNBOUNDED].
     */
    public fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval = IntInterval.UNBOUNDED

    /**
     * Evaluates a conservative [TriState] truth value for an invocation of this operation ([key])
     * under [evaluator], or `null` to fall back to integer truthiness (`0` → [TriState.FALSE],
     * strictly `> 0` → [TriState.TRUE], otherwise [TriState.UNKNOWN]).
     */
    public fun evaluateBooleanInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState? = null

    /**
     * Evaluates a conservative [StringSummary] for an invocation of this operation ([key]) under
     * [evaluator]. Defaults to [StringSummary.UNKNOWN].
     */
    public fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary = StringSummary.UNKNOWN

    /**
     * Evaluates a conservative [ColorInterval] for an invocation of this operation ([key]) under
     * [evaluator]. Defaults to [ColorInterval.UNKNOWN].
     */
    public fun evaluateColorInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): ColorInterval = ColorInterval.UNKNOWN

    /**
     * Extracts a rounding-error-aware [FloatAffineForm] for [key] given its standard interval
     * [range], [evaluator], and recursion [depth], or returns `null` if this operation does not
     * have a linear float affine representation.
     */
    public fun extractFloatAffine(
        key: RemoteOperationCacheKey,
        range: FloatInterval,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): FloatAffineForm? = null

    /**
     * Extracts an overflow-verified [IntAffineForm] for [key] under [evaluator] and recursion
     * [depth], or returns `null` if this operation does not have a linear integer affine
     * representation.
     */
    public fun extractIntAffine(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): IntAffineForm? = null

    /**
     * Refines [environment] assuming boolean expression [key] evaluates to [holds], or returns
     * `null` to fall back to generic variable refinement.
     */
    public fun refineBoolean(
        key: RemoteOperationCacheKey,
        holds: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? = null

    /**
     * Refines [environment] assuming `key <comparison> otherKey` holds (in the integer domain when
     * [asInt] is `true`, or the float domain otherwise), or returns `null` if this operation has no
     * additional relational refinement.
     */
    public fun refineComparison(
        key: RemoteOperationCacheKey,
        comparison: IntervalComparison,
        otherKey: RemoteStateCacheKey,
        asInt: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? = null

    /**
     * Unwraps transparent wrapper operations (such as references or resolved selects) to the
     * underlying operand key during condition refinement, or returns `null` if not transparent.
     */
    public fun unwrapAliasKey(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): RemoteStateCacheKey? = null

    /**
     * Propagates a float narrowing [interval] applied to [key] back to child operand keys in
     * [environment] (e.g., exact integer back-propagation through `RemoteInt.toRemoteFloat()`).
     */
    public fun propagateFloatNarrowing(
        key: RemoteOperationCacheKey,
        interval: FloatInterval,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment = environment

    /**
     * Attempts to simplify an invocation of this operation ([value] with cache key [key]) after its
     * children have been optimized, using the interval bounds and settings in [context].
     *
     * Returns a simplified replacement [RemoteValue], or `null` to keep [value] unchanged.
     */
    public fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? = null
}

/**
 * External interval-semantics adapter for a family of [RemoteOperation]s, allowing interval
 * analysis code to stay isolated from `Remote*` implementation files.
 */
internal interface RemoteOperationIntervalSemantics {
    fun requiresBoundedFloatOperands(op: Any): Boolean = false

    fun comparisonCondition(op: Any): IntervalComparison? = null

    fun selectCondition(op: Any): IntervalComparison? = null

    fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        RemoteValueKind.OTHER

    fun isBooleanSelect(op: Any): Boolean = false

    fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval = FloatInterval.UNBOUNDED

    fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval = IntInterval.UNBOUNDED

    fun evaluateBooleanInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState? = null

    fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary = StringSummary.UNKNOWN

    fun evaluateColorInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): ColorInterval = ColorInterval.UNKNOWN

    fun extractFloatAffine(
        key: RemoteOperationCacheKey,
        range: FloatInterval,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): FloatAffineForm? = null

    fun extractIntAffine(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): IntAffineForm? = null

    fun refineBoolean(
        key: RemoteOperationCacheKey,
        holds: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? = null

    fun refineComparison(
        key: RemoteOperationCacheKey,
        comparison: IntervalComparison,
        otherKey: RemoteStateCacheKey,
        asInt: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? = null

    fun unwrapAliasKey(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): RemoteStateCacheKey? = null

    fun propagateFloatNarrowing(
        key: RemoteOperationCacheKey,
        interval: FloatInterval,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment = environment

    fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? = null
}

internal fun intervalSemanticsFor(op: Any): RemoteOperationIntervalSemantics? =
    when (op) {
        is RemoteFloatOperationKey -> RemoteFloatIntervalSemantics
        is RemoteIntOperationKey -> RemoteIntIntervalSemantics
        is BooleanOperationKey -> RemoteBooleanIntervalSemantics
        is StringOperationKey -> RemoteStringIntervalSemantics
        is ColorOperationKey -> RemoteColorIntervalSemantics
        is EnumOperationKey -> RemoteEnumIntervalSemantics
        is FloatArrayOperationKey -> RemoteFloatArrayIntervalSemantics
        is IntArrayOperationKey -> RemoteIntArrayIntervalSemantics
        is StringArrayOperationKey -> RemoteStringArrayIntervalSemantics
        is RemoteIntervalOperation -> CustomRemoteIntervalOperationSemantics
        else -> null
    }

private object CustomRemoteIntervalOperationSemantics : RemoteOperationIntervalSemantics {
    override fun requiresBoundedFloatOperands(op: Any): Boolean =
        (op as RemoteIntervalOperation).requiresBoundedFloatOperands

    override fun comparisonCondition(op: Any): IntervalComparison? =
        (op as RemoteIntervalOperation).comparisonCondition

    override fun selectCondition(op: Any): IntervalComparison? =
        (op as RemoteIntervalOperation).selectCondition

    override fun selectComparisonKind(op: Any, args: List<RemoteStateCacheKey>): RemoteValueKind =
        (op as RemoteIntervalOperation).selectComparisonKind(args)

    override fun isBooleanSelect(op: Any): Boolean = (op as RemoteIntervalOperation).isBooleanSelect

    override fun evaluateFloatInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): FloatInterval = (key.op as RemoteIntervalOperation).evaluateFloatInterval(key, evaluator)

    override fun evaluateIntInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntInterval = (key.op as RemoteIntervalOperation).evaluateIntInterval(key, evaluator)

    override fun evaluateBooleanInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): TriState? = (key.op as RemoteIntervalOperation).evaluateBooleanInterval(key, evaluator)

    override fun evaluateStringSummary(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): StringSummary = (key.op as RemoteIntervalOperation).evaluateStringSummary(key, evaluator)

    override fun evaluateColorInterval(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): ColorInterval = (key.op as RemoteIntervalOperation).evaluateColorInterval(key, evaluator)

    override fun extractFloatAffine(
        key: RemoteOperationCacheKey,
        range: FloatInterval,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): FloatAffineForm? =
        (key.op as RemoteIntervalOperation).extractFloatAffine(key, range, evaluator, depth)

    override fun extractIntAffine(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
        depth: Int,
    ): IntAffineForm? = (key.op as RemoteIntervalOperation).extractIntAffine(key, evaluator, depth)

    override fun refineBoolean(
        key: RemoteOperationCacheKey,
        holds: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? =
        (key.op as RemoteIntervalOperation).refineBoolean(key, holds, environment, evaluator)

    override fun refineComparison(
        key: RemoteOperationCacheKey,
        comparison: IntervalComparison,
        otherKey: RemoteStateCacheKey,
        asInt: Boolean,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment? =
        (key.op as RemoteIntervalOperation).refineComparison(
            key,
            comparison,
            otherKey,
            asInt,
            environment,
            evaluator,
        )

    override fun unwrapAliasKey(
        key: RemoteOperationCacheKey,
        evaluator: RemoteValueIntervalEvaluator,
    ): RemoteStateCacheKey? = (key.op as RemoteIntervalOperation).unwrapAliasKey(key, evaluator)

    override fun propagateFloatNarrowing(
        key: RemoteOperationCacheKey,
        interval: FloatInterval,
        environment: IntervalEnvironment,
        evaluator: RemoteValueIntervalEvaluator,
    ): IntervalEnvironment =
        (key.op as RemoteIntervalOperation).propagateFloatNarrowing(
            key,
            interval,
            environment,
            evaluator,
        )

    override fun simplifyWithIntervals(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? =
        (key.op as RemoteIntervalOperation).simplifyWithIntervals(value, key, context)
}
