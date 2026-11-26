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
import androidx.compose.remote.creation.compose.state.MutableRemoteValue
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteIntArray
import androidx.compose.remote.creation.compose.state.RemoteNamedCacheKey
import androidx.compose.remote.creation.compose.state.RemoteOperation
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteStringArray
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.creation.compose.state.adaptToType
import androidx.compose.remote.creation.compose.state.cacheKey
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.valueKind
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.TriState
import java.util.HashMap
import java.util.HashSet

/**
 * Feature switches controlling which built-in optimizations [IntervalOptimizer] performs.
 *
 * @property pruneTernaries eliminates statically dead select/ternary branches and collapses
 *   identical arms
 * @property refineTernaryBranches refines operand intervals inside surviving select/ternary
 *   branches using branch conditions
 * @property foldComparisons folds statically provable relational comparisons and boolean predicates
 *   into constants
 * @property simplifyRangeMath simplifies redundant range-clamping/rounding operations (`abs`,
 *   `min`, `max`, `clamp`, integer-valued `floor`/`ceil`/`round`, endpoint color `lerp`)
 * @property foldPointConstants folds pure deterministic expressions whose interval collapses to a
 *   single point into constants
 * @property foldArrayGets folds literal array lookups with statically constant/point indices into
 *   the selected element
 * @property lowerTwoElementArraysToSelect lowers 2-element literal array lookups with indices
 *   bounded to `[0, 1]` into conditional selects
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IntervalOptimizerConfig
@JvmOverloads
constructor(
    public val pruneTernaries: Boolean = true,
    public val refineTernaryBranches: Boolean = true,
    public val foldComparisons: Boolean = true,
    public val simplifyRangeMath: Boolean = true,
    public val foldPointConstants: Boolean = true,
    public val foldArrayGets: Boolean = true,
    public val lowerTwoElementArraysToSelect: Boolean = true,
) {
    /** Returns a copy of this configuration with the specified overrides. */
    public fun copy(
        pruneTernaries: Boolean = this.pruneTernaries,
        refineTernaryBranches: Boolean = this.refineTernaryBranches,
        foldComparisons: Boolean = this.foldComparisons,
        simplifyRangeMath: Boolean = this.simplifyRangeMath,
        foldPointConstants: Boolean = this.foldPointConstants,
        foldArrayGets: Boolean = this.foldArrayGets,
        lowerTwoElementArraysToSelect: Boolean = this.lowerTwoElementArraysToSelect,
    ): IntervalOptimizerConfig =
        IntervalOptimizerConfig(
            pruneTernaries = pruneTernaries,
            refineTernaryBranches = refineTernaryBranches,
            foldComparisons = foldComparisons,
            simplifyRangeMath = simplifyRangeMath,
            foldPointConstants = foldPointConstants,
            foldArrayGets = foldArrayGets,
            lowerTwoElementArraysToSelect = lowerTwoElementArraysToSelect,
        )

    override fun equals(other: Any?): Boolean =
        other is IntervalOptimizerConfig &&
            pruneTernaries == other.pruneTernaries &&
            refineTernaryBranches == other.refineTernaryBranches &&
            foldComparisons == other.foldComparisons &&
            simplifyRangeMath == other.simplifyRangeMath &&
            foldPointConstants == other.foldPointConstants &&
            foldArrayGets == other.foldArrayGets &&
            lowerTwoElementArraysToSelect == other.lowerTwoElementArraysToSelect

    override fun hashCode(): Int {
        var result = pruneTernaries.hashCode()
        result = 31 * result + refineTernaryBranches.hashCode()
        result = 31 * result + foldComparisons.hashCode()
        result = 31 * result + simplifyRangeMath.hashCode()
        result = 31 * result + foldPointConstants.hashCode()
        result = 31 * result + foldArrayGets.hashCode()
        result = 31 * result + lowerTwoElementArraysToSelect.hashCode()
        return result
    }

    override fun toString(): String =
        "IntervalOptimizerConfig(pruneTernaries=$pruneTernaries, " +
            "refineTernaryBranches=$refineTernaryBranches, foldComparisons=$foldComparisons, " +
            "simplifyRangeMath=$simplifyRangeMath, foldPointConstants=$foldPointConstants, " +
            "foldArrayGets=$foldArrayGets, " +
            "lowerTwoElementArraysToSelect=$lowerTwoElementArraysToSelect)"

    public companion object {
        /** Default configuration with all built-in interval optimizations enabled. */
        public val FULL: IntervalOptimizerConfig = IntervalOptimizerConfig()
    }
}

/**
 * Execution context provided to [RemoteIntervalOperation.simplifyWithIntervals] and
 * [IntervalOptimizationRule] callbacks during [IntervalOptimizer] traversal.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IntervalOptimizationContext {
    /** The active (possibly branch-refined) [IntervalEnvironment]. */
    public val environment: IntervalEnvironment

    /** The outer unrefined [IntervalEnvironment] (for preserving guards on trapping operands). */
    public val unrefinedEnvironment: IntervalEnvironment

    /** Memoized [RemoteValueIntervalEvaluator] backed by [environment]. */
    public val evaluator: RemoteValueIntervalEvaluator

    /** Active optimization feature switches. */
    public val config: IntervalOptimizerConfig

    /** Whether this context is currently optimizing inside a branch-refined environment. */
    public val isInRefinedBranch: Boolean
        get() = environment !== unrefinedEnvironment

    /** Recursively optimizes [value] under [environment]. */
    public fun optimize(value: RemoteValue<*>): RemoteValue<*>

    /** Recursively optimizes [value] under [unrefinedEnvironment]. */
    public fun optimizeUnrefined(value: RemoteValue<*>): RemoteValue<*>

    /**
     * Returns `true` if every operation in [key] whose interval semantics require bounded float
     * operands (such as `*`, `mad`, `clamp`, `lerp`, `min`, `max`) has bounded float operands under
     * [evaluator], guarding against IEEE-754 `NaN`/`Infinity` values being hidden by a bounded
     * interval.
     */
    public fun hasBoundedFloatOperands(key: RemoteStateCacheKey): Boolean =
        IntervalOptimizer.hasBoundedFloatOperands(key, evaluator)
}

/**
 * Pluggable rewrite hook for extending [IntervalOptimizer] with domain-specific expression
 * optimizations (such as wall-clock precision lowering or quantizer pushdown) without duplicating
 * DAG memoization, cycle detection, type adaptation, or path-sensitive branch refinement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IntervalOptimizationRule {
    /**
     * Optional pre-child rewrite invoked before children of [value] are visited. Return a
     * replacement [RemoteValue] to short-circuit default child traversal, or `null` to continue
     * normal optimization.
     */
    public fun rewriteBeforeChildren(
        value: RemoteValue<*>,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? = null

    /**
     * Optional post-child rewrite invoked after children of [value] have been optimized and
     * built-in operation simplifications have run. Return a replacement [RemoteValue], or `null` to
     * keep [value].
     */
    public fun rewriteAfterChildren(
        value: RemoteValue<*>,
        context: IntervalOptimizationContext,
    ): RemoteValue<*>? = null
}

/**
 * Memoized, extensible bottom-up DAG optimizer for [RemoteValue] expressions guided by an
 * [IntervalEnvironment].
 *
 * Performs:
 * 1. Pre-child custom [rules] and dead select/ternary pruning (avoiding traversal of dead
 *    subtrees).
 * 2. Path-sensitive branch refinement on surviving selects/ternaries with isolated memoization and
 *    unrefined fallback on operands where the operation can fail
 *    ([RemoteOperation.canFailOnOperandIndex]).
 * 3. Bottom-up child optimization and safe AST reconstruction ([RemoteOperation.reconstruct]).
 * 4. Post-child comparison/select folding, polymorphic operation simplifications
 *    ([RemoteIntervalOperation.simplifyWithIntervals]), post-child custom [rules], and
 *    purity/IEEE-754 guarded point-constant folding.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IntervalOptimizer
private constructor(
    public val environment: IntervalEnvironment,
    public val config: IntervalOptimizerConfig,
    public val rules: List<IntervalOptimizationRule>,
    private val refineDepth: Int,
    private val unrefinedEnvironment: IntervalEnvironment,
) {
    /**
     * Creates an [IntervalOptimizer] configured with [environment], [config], and optional custom
     * [rules].
     */
    @JvmOverloads
    public constructor(
        environment: IntervalEnvironment = MapIntervalEnvironment.EMPTY,
        config: IntervalOptimizerConfig = IntervalOptimizerConfig.FULL,
        rules: List<IntervalOptimizationRule> = emptyList(),
    ) : this(
        environment = environment,
        config = config,
        rules = rules,
        refineDepth = 0,
        unrefinedEnvironment = environment,
    )

    /** Memoized interval evaluator bound to [environment]. */
    public val evaluator: RemoteValueIntervalEvaluator = RemoteValueIntervalEvaluator(environment)

    // Keyed by valueKind, mutability, enumEntries identity, and cacheKey so RemoteFloat/RemoteInt/
    // RemoteBoolean, distinct RemoteEnum types, and MutableRemote* vs immutable Remote* sharing a
    // cache key never collide in the memo table.
    private val optimizeMemo = HashMap<OptimizeMemoKey, RemoteValue<*>>()
    private val visiting = HashSet<OptimizeMemoKey>()

    private val unrefinedOptimizer: IntervalOptimizer by
        lazy(LazyThreadSafetyMode.NONE) {
            if (unrefinedEnvironment === environment) {
                this
            } else {
                IntervalOptimizer(
                    environment = unrefinedEnvironment,
                    config = config,
                    rules = rules,
                    refineDepth = refineDepth,
                    unrefinedEnvironment = unrefinedEnvironment,
                )
            }
        }

    private val context: IntervalOptimizationContext =
        object : IntervalOptimizationContext {
            override val environment: IntervalEnvironment
                get() = this@IntervalOptimizer.environment

            override val unrefinedEnvironment: IntervalEnvironment
                get() = this@IntervalOptimizer.unrefinedEnvironment

            override val evaluator: RemoteValueIntervalEvaluator
                get() = this@IntervalOptimizer.evaluator

            override val config: IntervalOptimizerConfig
                get() = this@IntervalOptimizer.config

            override fun optimize(value: RemoteValue<*>): RemoteValue<*> =
                this@IntervalOptimizer.optimize(value)

            override fun optimizeUnrefined(value: RemoteValue<*>): RemoteValue<*> =
                this@IntervalOptimizer.unrefinedOptimizer.optimize(value)
        }

    /** Optimizes [value] under [environment], [config], and [rules]. */
    public fun optimize(value: RemoteValue<*>): RemoteValue<*> {
        if (value.hasConstantValue && !isCompositeLiteralArray(value)) return value
        val memoKey =
            OptimizeMemoKey(
                valueKind = value.valueKind,
                isMutable = value is MutableRemoteValue<*>,
                enumEntries = (value as? RemoteEnum<*>)?.enumEntries,
                cacheKey = value.cacheKey,
            )
        optimizeMemo[memoKey]?.let { cached ->
            return value.adaptToType(cached)
        }
        if (!visiting.add(memoKey)) return value
        val result =
            try {
                if (value is RemoteBoolean && value.cacheKey is RemoteNamedCacheKey) {
                    optimizeNamedBoolean(value, value.cacheKey as RemoteNamedCacheKey)
                } else {
                    optimizeImpl(value)
                }
            } finally {
                visiting.remove(memoKey)
            }
        val adapted = value.adaptToType(result)
        optimizeMemo[memoKey] = adapted
        return adapted
    }

    private fun optimizeNamedBoolean(
        value: RemoteBoolean,
        namedKey: RemoteNamedCacheKey,
    ): RemoteValue<*> {
        // Callers inspecting named variables may probe with a synthetic RemoteBoolean(name) on any
        // NamedVariable (including float/int vars). Check path-sensitive int overrides first, then
        // require an exact float 0f/1f (or an exact int 0/1 when no float bound exists) so
        // non-boolean values (e.g. 5f or 1.5f) are never truncated into true/1 by a boolean probe.
        for (i in 0 until rules.size) {
            rules[i].rewriteBeforeChildren(value, context)?.let { rewritten ->
                return rewritten
            }
        }
        var current: RemoteValue<*> = applyAfterChildRules(value)
        if (current === value && config.foldPointConstants) {
            val intOverride = environment.intOverride(namedKey)
            current =
                when {
                    intOverride != null && intOverride.isPoint && intOverride.min == 0 ->
                        RemoteBoolean(false)
                    intOverride != null && intOverride.isPoint && intOverride.min == 1 ->
                        RemoteBoolean(true)
                    else -> {
                        val range = evaluator.evalFloatKey(namedKey)
                        when {
                            range.isPoint && range.min == 0f -> RemoteBoolean(false)
                            range.isPoint && range.min == 1f -> RemoteBoolean(true)
                            range.isUnbounded -> {
                                val namedInt = environment.intForNamed(namedKey)
                                when {
                                    namedInt.isPoint && namedInt.min == 0 -> RemoteBoolean(false)
                                    namedInt.isPoint && namedInt.min == 1 -> RemoteBoolean(true)
                                    else -> value
                                }
                            }
                            else -> value
                        }
                    }
                }
        }
        return current
    }

    private fun optimizeImpl(value: RemoteValue<*>): RemoteValue<*> {
        // 0. Pre-child custom rewrite rules.
        for (i in 0 until rules.size) {
            rules[i].rewriteBeforeChildren(value, context)?.let { rewritten ->
                return rewritten
            }
        }

        val key = value.cacheKey as? RemoteOperationCacheKey
        if (key == null) {
            val afterRules = applyAfterChildRules(value)
            return if (config.foldPointConstants) tryFoldPointConstant(afterRules) else afterRules
        }
        val op = key.op as? RemoteOperation
        val semantics = intervalSemanticsFor(key.op)

        // 1. Pre-child dead ternary/select pruning (avoids traversing dead subtrees).
        if (config.pruneTernaries && semantics != null) {
            tryPruneTernaryBeforeChildren(evaluator, key.op, semantics, key.args)?.let { pruned ->
                return optimize(pruned)
            }
        }

        // 2. Surviving ternary/select with optional path-sensitive branch refinement.
        if (semantics != null && isTernarySelectOp(key.op, semantics)) {
            return optimizeTernarySelect(value, key, semantics)
        }

        // 3. Generic bottom-up child optimization.
        val withChildren = optimizeChildren(value, key, op) ?: value
        return finalizeOptimized(withChildren)
    }

    private fun finalizeOptimized(value: RemoteValue<*>): RemoteValue<*> {
        var current = value
        if (config.pruneTernaries || config.foldComparisons) {
            current =
                pruneComparisonOrSelectAfterChildren(
                    evaluator = evaluator,
                    value = current,
                    foldComparisons = config.foldComparisons,
                    pruneSelects = config.pruneTernaries,
                )
        }
        val curKey = current.cacheKey as? RemoteOperationCacheKey
        if (curKey != null) {
            intervalSemanticsFor(curKey.op)?.simplifyWithIntervals(current, curKey, context)?.let {
                current = it
            }
        }
        current = applyAfterChildRules(current)
        if (config.foldPointConstants) {
            current = tryFoldPointConstant(current)
        }
        return current
    }

    private fun applyAfterChildRules(value: RemoteValue<*>): RemoteValue<*> {
        var current = value
        for (i in 0 until rules.size) {
            rules[i].rewriteAfterChildren(current, context)?.let { current = it }
        }
        return current
    }

    private fun optimizeChildren(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        op: RemoteOperation?,
    ): RemoteValue<*>? {
        val args = key.args
        val inEagerBranch = environment !== unrefinedEnvironment
        val newArgs = ArrayList<RemoteValue<*>>(args.size)
        var argsChanged = false
        for (i in 0 until args.size) {
            val argKey = args[i]
            val argValue = argKey.getOrCreateValueOrNull() ?: return null
            // RPN ifElse/select evaluates both arms eagerly, so operands on which the operation can
            // fail at runtime (array indices and integer divisors) inside a branch must not drop
            // bounds/zero guards using branch-only interval refinements.
            val optimizedArg =
                if (inEagerBranch && op?.canFailOnOperandIndex(i) == true) {
                    unrefinedOptimizer.optimize(argValue)
                } else {
                    optimize(argValue)
                }
            if (optimizedArg.cacheKey != argKey) argsChanged = true
            newArgs.add(optimizedArg)
        }
        if (!argsChanged) return value
        // Preserve non-canonical constant ints (values outside {0, 1}) rather than reconstructing
        // RemoteBoolean(constInt), which folds via != 0 into RemoteBoolean(true), corrupting
        // toRemoteInt() and inverting negative conditions relative to wire I_IFELSE (> 0).
        if (key.op == BooleanOperationKey.RemoteBoolean && newArgs.size == 1) {
            val constInt = (newArgs[0] as? RemoteInt)?.constantValueOrNull
            if (constInt != null && constInt != 0 && constInt != 1) {
                return value
            }
        }
        return key.reconstructOrNull(newArgs) ?: value
    }

    private fun optimizeTernarySelect(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        semantics: RemoteOperationIntervalSemantics,
    ): RemoteValue<*> {
        val args = key.args
        val op = key.op as? RemoteOperation
        if (semantics.isBooleanSelect(key.op) && args.size >= 3) {
            return optimizeBooleanSelect(value, key, args)
        }
        val cond =
            semantics.selectCondition(key.op)
                ?: return finalizeOptimized(optimizeChildren(value, key, op) ?: value)
        if (args.size < 4) return finalizeOptimized(optimizeChildren(value, key, op) ?: value)
        return optimizeSelectIf(value, key, semantics, cond, args)
    }

    private fun optimizeBooleanSelect(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        args: List<RemoteStateCacheKey>,
    ): RemoteValue<*> {
        val condOrig = args[0].getOrCreateValueOrNull() ?: return finalizeOptimized(value)
        val trueOrig = args[1].getOrCreateValueOrNull() ?: return finalizeOptimized(value)
        val falseOrig = args[2].getOrCreateValueOrNull() ?: return finalizeOptimized(value)
        val condOptimized = optimize(condOrig)

        if (config.pruneTernaries && hasBoundedFloatOperands(condOptimized.cacheKey, evaluator)) {
            val condTri =
                extractConstCondition(condOptimized.cacheKey)?.let { TriState.of(it) }
                    ?: evaluator.evalBoolKey(condOptimized.cacheKey)
            when (condTri) {
                TriState.TRUE -> return optimize(trueOrig)
                TriState.FALSE -> return optimize(falseOrig)
                TriState.UNKNOWN -> {}
            }
        }

        val trueOptimized =
            optimizeUnderBooleanCondition(trueOrig, condOptimized.cacheKey, holds = true)
        val falseOptimized =
            optimizeUnderBooleanCondition(falseOrig, condOptimized.cacheKey, holds = false)

        if (config.pruneTernaries && trueOptimized.cacheKey == falseOptimized.cacheKey) {
            return trueOptimized
        }
        val changed =
            condOptimized.cacheKey != args[0] ||
                trueOptimized.cacheKey != args[1] ||
                falseOptimized.cacheKey != args[2]
        val surviving =
            if (changed) {
                key.reconstructOrNull(listOf(condOptimized, trueOptimized, falseOptimized)) ?: value
            } else {
                value
            }
        return finalizeOptimized(surviving)
    }

    private fun optimizeSelectIf(
        value: RemoteValue<*>,
        key: RemoteOperationCacheKey,
        semantics: RemoteOperationIntervalSemantics,
        comparison: IntervalComparison,
        args: List<RemoteStateCacheKey>,
    ): RemoteValue<*> {
        val aOrig = args[0].getOrCreateValueOrNull() ?: return finalizeOptimized(value)
        val bOrig = args[1].getOrCreateValueOrNull() ?: return finalizeOptimized(value)
        val trueOrig = args[2].getOrCreateValueOrNull() ?: return finalizeOptimized(value)
        val falseOrig = args[3].getOrCreateValueOrNull() ?: return finalizeOptimized(value)
        val cmpKind = semantics.selectComparisonKind(key.op, args)
        val isInt: Boolean? =
            when {
                cmpKind.isIntEncoded -> true
                cmpKind == RemoteValueKind.FLOAT || cmpKind == RemoteValueKind.DP -> false
                else -> null
            }

        val aOptimized = optimize(aOrig)
        val bOptimized = optimize(bOrig)

        if (
            isInt != null &&
                config.pruneTernaries &&
                hasBoundedFloatOperands(aOptimized.cacheKey, evaluator) &&
                hasBoundedFloatOperands(bOptimized.cacheKey, evaluator)
        ) {
            val cmp =
                if (isInt) {
                    evaluator.compareIntKeys(comparison, aOptimized.cacheKey, bOptimized.cacheKey)
                } else {
                    evaluator.compareFloatKeys(comparison, aOptimized.cacheKey, bOptimized.cacheKey)
                }
            when (cmp) {
                TriState.TRUE -> return optimize(trueOrig)
                TriState.FALSE -> return optimize(falseOrig)
                TriState.UNKNOWN -> {}
            }
        }

        val trueOptimized =
            if (isInt != null) {
                optimizeUnderComparison(
                    trueOrig,
                    isInt,
                    comparison,
                    aOptimized.cacheKey,
                    bOptimized.cacheKey,
                )
            } else {
                optimize(trueOrig)
            }
        val falseOptimized =
            if (isInt != null) {
                optimizeUnderComparison(
                    falseOrig,
                    isInt,
                    comparison.negate(),
                    aOptimized.cacheKey,
                    bOptimized.cacheKey,
                )
            } else {
                optimize(falseOrig)
            }

        if (config.pruneTernaries && trueOptimized.cacheKey == falseOptimized.cacheKey) {
            return trueOptimized
        }
        val changed =
            aOptimized.cacheKey != args[0] ||
                bOptimized.cacheKey != args[1] ||
                trueOptimized.cacheKey != args[2] ||
                falseOptimized.cacheKey != args[3]
        val surviving =
            if (changed) {
                key.reconstructOrNull(listOf(aOptimized, bOptimized, trueOptimized, falseOptimized))
                    ?: value
            } else {
                value
            }
        return finalizeOptimized(surviving)
    }

    private fun optimizeUnderBooleanCondition(
        branch: RemoteValue<*>,
        condKey: RemoteStateCacheKey,
        holds: Boolean,
    ): RemoteValue<*> {
        if (
            !config.refineTernaryBranches ||
                refineDepth >= MAX_REFINE_DEPTH ||
                !hasBoundedFloatOperands(condKey, evaluator)
        ) {
            return optimize(branch)
        }
        val refined = environment.refine(condKey, holds)
        return if (refined !== environment) {
            IntervalOptimizer(
                    environment = refined,
                    config = config,
                    rules = rules,
                    refineDepth = refineDepth + 1,
                    unrefinedEnvironment = unrefinedEnvironment,
                )
                .optimize(branch)
        } else {
            optimize(branch)
        }
    }

    private fun optimizeUnderComparison(
        branch: RemoteValue<*>,
        isInt: Boolean,
        comparison: IntervalComparison,
        aKey: RemoteStateCacheKey,
        bKey: RemoteStateCacheKey,
    ): RemoteValue<*> {
        if (
            !config.refineTernaryBranches ||
                refineDepth >= MAX_REFINE_DEPTH ||
                !hasBoundedFloatOperands(aKey, evaluator) ||
                !hasBoundedFloatOperands(bKey, evaluator)
        ) {
            return optimize(branch)
        }
        val refined = environment.narrowComparison(comparison, aKey, bKey, asInt = isInt)
        return if (refined !== environment) {
            IntervalOptimizer(
                    environment = refined,
                    config = config,
                    rules = rules,
                    refineDepth = refineDepth + 1,
                    unrefinedEnvironment = unrefinedEnvironment,
                )
                .optimize(branch)
        } else {
            optimize(branch)
        }
    }

    private fun tryFoldPointConstant(value: RemoteValue<*>): RemoteValue<*> {
        if (value.hasConstantValue) return value
        val key = value.cacheKey
        if (!key.canConstantFold(environment::canFoldStateId)) return value
        if (!hasBoundedFloatOperands(key, evaluator)) return value
        return when (value) {
            is RemoteFloat -> evaluator.evalFloatKey(key).takeIf { it.isPoint }?.min?.rf ?: value
            is RemoteDp ->
                evaluator.evalFloatKey(key).takeIf { it.isPoint }?.min?.rf?.let { RemoteDp(it) }
                    ?: value
            is RemoteInt -> evaluator.evalIntKey(key).takeIf { it.isPoint }?.min?.ri ?: value
            is RemoteString ->
                evaluator.evalStringKey(key).constantValue?.let { RemoteString(it) } ?: value
            is RemoteColor ->
                evaluator.evalColorKey(key).toArgbOrNull()?.let { RemoteColor(it) } ?: value
            is RemoteBoolean -> {
                val intRange = evaluator.evalIntKey(key)
                if (intRange.isEmpty || intRange.min < 0 || intRange.max > 1) {
                    value
                } else {
                    evaluator.evalBoolKey(key).booleanValueOrNull?.let { RemoteBoolean(it) }
                        ?: value
                }
            }
            is RemoteEnum<*> -> {
                val ordinal = evaluator.evalIntKey(key).takeIf { it.isPoint }?.min
                if (ordinal != null && ordinal in value.enumEntries.indices) {
                    RemoteEnum(ordinal.ri, value.enumEntries)
                } else {
                    value
                }
            }
            else -> value
        }
    }

    private class OptimizeMemoKey(
        val valueKind: RemoteValueKind,
        val isMutable: Boolean,
        val enumEntries: List<Enum<*>>?,
        val cacheKey: RemoteStateCacheKey,
    ) {
        override fun equals(other: Any?): Boolean =
            other is OptimizeMemoKey &&
                valueKind == other.valueKind &&
                isMutable == other.isMutable &&
                enumEntries === other.enumEntries &&
                cacheKey == other.cacheKey

        override fun hashCode(): Int {
            var h = valueKind.hashCode()
            h = 31 * h + isMutable.hashCode()
            h = 31 * h + System.identityHashCode(enumEntries)
            h = 31 * h + cacheKey.hashCode()
            return h
        }
    }

    public companion object {
        private const val MAX_REFINE_DEPTH = 6

        private fun isCompositeLiteralArray(value: RemoteValue<*>): Boolean =
            value is RemoteFloatArray || value is RemoteIntArray || value is RemoteStringArray

        internal fun isTernarySelectOp(
            op: Any,
            semantics: RemoteOperationIntervalSemantics,
        ): Boolean = semantics.isBooleanSelect(op) || semantics.selectCondition(op) != null

        /**
         * Verifies that every operation in [key] requiring bounded float operands (for example,
         * `FloatInterval.times` maps `UNBOUNDED * 0f` to `[0, 0]` even though `Infinity * 0f` and
         * `NaN * 0f` produce `NaN` at runtime) has bounded float inputs under [evaluator].
         *
         * Uses iterative post-order traversal memoized on [evaluator] so deep expression DAGs are
         * verified without recursion limits or redundant work.
         */
        internal fun hasBoundedFloatOperands(
            key: RemoteStateCacheKey,
            evaluator: RemoteValueIntervalEvaluator,
        ): Boolean = evaluator.hasBoundedFloatOperands(key)

        /**
         * Attempts to resolve a ternary/select operation directly to its surviving branch before
         * visiting child expressions, using [evaluator] (including self-comparison and affine
         * difference checks) guarded by [hasBoundedFloatOperands].
         *
         * @param evaluator Interval evaluator backing the current environment.
         * @param op Operation descriptor of the select/ternary node.
         * @param semantics Interval semantics handler for [op].
         * @param args Operand cache keys of the select/ternary node.
         * @return The surviving branch [RemoteValue], or `null` if neither branch is dead.
         */
        internal fun tryPruneTernaryBeforeChildren(
            evaluator: RemoteValueIntervalEvaluator,
            op: Any,
            semantics: RemoteOperationIntervalSemantics,
            args: List<RemoteStateCacheKey>,
        ): RemoteValue<*>? {
            if (semantics.isBooleanSelect(op) && args.size >= 3) {
                val tri =
                    if (hasBoundedFloatOperands(args[0], evaluator)) {
                        evaluator.evalBoolKey(args[0])
                    } else {
                        TriState.UNKNOWN
                    }
                return selectBranchForTriState(tri, args[1], args[2])
            }
            val cond = semantics.selectCondition(op) ?: return null
            if (args.size < 4) return null
            val cmpKind = semantics.selectComparisonKind(op, args)
            val cmp =
                if (
                    hasBoundedFloatOperands(args[0], evaluator) &&
                        hasBoundedFloatOperands(args[1], evaluator)
                ) {
                    when {
                        cmpKind.isIntEncoded -> evaluator.compareIntKeys(cond, args[0], args[1])
                        cmpKind == RemoteValueKind.FLOAT || cmpKind == RemoteValueKind.DP ->
                            evaluator.compareFloatKeys(cond, args[0], args[1])
                        else -> TriState.UNKNOWN
                    }
                } else {
                    TriState.UNKNOWN
                }
            return selectBranchForTriState(cmp, args[2], args[3])
        }

        /**
         * Simplifies foldable boolean comparisons/predicates and constant-condition or
         * identical-arm selects after their children have been optimized.
         *
         * @param evaluator Interval evaluator backing the current environment.
         * @param value The post-child [RemoteValue] to simplify.
         * @param foldComparisons Whether provable boolean predicates may fold to constants.
         * @param pruneSelects Whether constant-condition or identical-arm selects may be pruned.
         * @return The simplified [RemoteValue], or [value] if unchanged.
         */
        internal fun pruneComparisonOrSelectAfterChildren(
            evaluator: RemoteValueIntervalEvaluator,
            value: RemoteValue<*>,
            foldComparisons: Boolean = true,
            pruneSelects: Boolean = true,
        ): RemoteValue<*> {
            val key = value.cacheKey as? RemoteOperationCacheKey ?: return value
            val op = key.op as? RemoteOperation ?: return value
            val semantics = intervalSemanticsFor(key.op)
            val args = key.args

            if (
                foldComparisons &&
                    op.resultKind == RemoteValueKind.BOOLEAN &&
                    op != BooleanOperationKey.RemoteBoolean &&
                    semantics?.let { isTernarySelectOp(key.op, it) } != true &&
                    key.isDeterministic() &&
                    hasBoundedFloatOperands(key, evaluator)
            ) {
                val canFoldBoolean =
                    if (value is RemoteBoolean) {
                        val intRange = evaluator.evalIntKey(key)
                        !intRange.isEmpty && intRange.min >= 0 && intRange.max <= 1
                    } else {
                        true
                    }
                if (canFoldBoolean) {
                    when (evaluator.evalBoolKey(key)) {
                        TriState.TRUE ->
                            return when (value) {
                                is RemoteBoolean -> RemoteBoolean(true)
                                is RemoteFloat -> 1f.rf
                                else -> 1.ri
                            }
                        TriState.FALSE ->
                            return when (value) {
                                is RemoteBoolean -> RemoteBoolean(false)
                                is RemoteFloat -> 0f.rf
                                else -> 0.ri
                            }
                        TriState.UNKNOWN -> {}
                    }
                }
            }

            if (pruneSelects && semantics != null) {
                if (semantics.isBooleanSelect(key.op) && args.size >= 3) {
                    if (args[1] == args[2]) {
                        args[1].getOrCreateValueOrNull()?.let {
                            return it
                        }
                    }
                    extractConstCondition(args[0])?.let { constCond ->
                        (if (constCond) args[1] else args[2]).getOrCreateValueOrNull()?.let {
                            return it
                        }
                    }
                }
                if (
                    semantics.selectCondition(key.op) != null &&
                        args.size >= 4 &&
                        args[2] == args[3]
                ) {
                    args[2].getOrCreateValueOrNull()?.let {
                        return it
                    }
                }
            }
            return value
        }

        private fun selectBranchForTriState(
            tri: TriState,
            ifTrueKey: RemoteStateCacheKey,
            ifFalseKey: RemoteStateCacheKey,
        ): RemoteValue<*>? =
            when (tri) {
                TriState.TRUE -> ifTrueKey.getOrCreateValueOrNull()
                TriState.FALSE -> ifFalseKey.getOrCreateValueOrNull()
                TriState.UNKNOWN ->
                    if (ifTrueKey == ifFalseKey) ifTrueKey.getOrCreateValueOrNull() else null
            }

        private fun extractConstCondition(condKey: RemoteStateCacheKey): Boolean? =
            when (val condValue = condKey.getOrCreateValueOrNull()) {
                is RemoteInt ->
                    condValue.constantValueOrNull?.let {
                        when {
                            it == 0 -> false
                            it > 0 -> true
                            else -> null
                        }
                    }
                is RemoteBoolean -> condValue.constantValueOrNull
                else -> null
            }
    }
}

/**
 * Optimizes this [RemoteValue] expression DAG using [IntervalOptimizer] under [environment],
 * [config], and optional custom [rules].
 */
@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <S : RemoteValue<*>> S.optimizeWithIntervals(
    environment: IntervalEnvironment = MapIntervalEnvironment.EMPTY,
    config: IntervalOptimizerConfig = IntervalOptimizerConfig.FULL,
    rules: List<IntervalOptimizationRule> = emptyList(),
): S = IntervalOptimizer(environment, config, rules).optimize(this) as S
