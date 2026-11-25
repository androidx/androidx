/*
 * Copyright 2025 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComponentToken
import androidx.compose.runtime.Immutable
import java.lang.ref.WeakReference
import kotlin.math.floor as mathFloor
import kotlin.reflect.KClass

/**
 * Represents a key used for caching [RemoteValue] instances or expressions in
 * [androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState].
 *
 * Caching based on these keys prevents redundant operations and document writes when the same
 * expression or state is encountered multiple times during document creation.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteStateCacheKey {
    /** The child argument cache keys of this node in the AST DAG. Empty for leaf nodes. */
    public val args: List<RemoteStateCacheKey>
        get() = emptyList()

    /**
     * Reports whether this [RemoteStateCacheKey] always produces a whole number when finite.
     *
     * Synthesized bottom-up in `O(1)` via [BaseRemoteStateCacheKey.expressionFlags] on
     * [RemoteOperationCacheKey] and leaf cache keys (constants, typed variable keys, and discrete
     * integer context IDs), so queries are `O(1)` without traversing the DAG.
     *
     * ```kotlin
     * // True for integer-typed or integer-preserving expression keys:
     * (floor(x) + 1f.rf).cacheKey.isResultWholeNumber                       // true
     * n.toRemoteFloat().cacheKey.isResultWholeNumber                        // true
     * RemoteStateIdKey(RemoteContext.ID_TIME_IN_HR).isResultWholeNumber     // true
     *
     * // False when fractional or mutable values are possible:
     * (floor(x) / 2f.rf).cacheKey.isResultWholeNumber                       // false
     * RemoteStateIdKey(RemoteContext.ID_CONTINUOUS_SEC).isResultWholeNumber // false
     * MutableRemoteFloat(3f).cacheKey.isResultWholeNumber                   // false
     * ```
     *
     * - Returns `true` when every finite evaluation has no fractional part.
     * - Returns `false` for [MutableRemoteFloat], named floats, non-finite constants, and
     *   expressions that cannot be proven integer-valued statically.
     * - Note: Operations such as [floor], [ceil], [sign], and arithmetic (`+`, `-`, `*`, `%`,
     *   [mad], [lerp]) can still yield `NaN` or infinity at runtime; `true` guarantees no
     *   fractional part whenever the result is finite.
     */
    public val isResultWholeNumber: Boolean
        get() = false

    /**
     * Returns the static [RemoteValueKind] denoted by this [RemoteStateCacheKey].
     *
     * Checks [RemoteOperation.resultKind] for operation keys, an attached live [RemoteValue] when
     * present, and constant/container key types otherwise. For detached [RemoteStateIdKey] and
     * [RemoteNamedCacheKey] leaves, delegates to [stateIdKind] and [namedKeyKind] so callers backed
     * by a document index can supply declared variable types.
     *
     * @param stateIdKind fallback classifier for detached [RemoteStateIdKey] IDs
     * @param namedKeyKind fallback classifier for detached [RemoteNamedCacheKey] keys
     * @return static [RemoteValueKind] of this key, or [RemoteValueKind.OTHER] when unknown
     */
    public fun getValueKind(
        stateIdKind: (Int) -> RemoteValueKind = { RemoteValueKind.OTHER },
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind = { RemoteValueKind.OTHER },
    ): RemoteValueKind = RemoteValueKind.OTHER

    /**
     * Returns the live [RemoteValue] currently attached to this cache key, or constructs a new
     * [RemoteValue] wrapper if this key is self-contained.
     *
     * Because [RemoteOperationCacheKey.args] stores cache keys rather than strong references to
     * child values (and may wrap raw primitive constants with no attached [RemoteValue]), AST
     * passes use this method to obtain a [RemoteValue] for a child key:
     * 1. If a live [RemoteValue] is attached ([BaseRemoteStateCacheKey.getState]), returns it.
     * 2. Otherwise, creates a wrapper for self-contained keys: constant primitives, strings, and
     *    colors ([RemoteConstantCacheKey], [RemoteColorConstantCacheKey]), literal float arrays,
     *    and [RemoteStateIdKey] leaves via [createForStateId] (which defaults to a NaN-boxed
     *    [RemoteFloat]).
     * 3. Returns `null` if no live [RemoteValue] is attached and the key cannot be reconstructed
     *    from its contents alone (such as detached named, component, instance, array, or operation
     *    keys, or unsupported constant types).
     *
     * @param createForStateId factory invoked for unattached [RemoteStateIdKey] leaves
     * @return the attached or newly constructed [RemoteValue], or `null` if unavailable
     */
    public fun getOrCreateValueOrNull(
        createForStateId: (Int) -> RemoteValue<*>? = { RemoteFloat(Utils.asNan(it)) }
    ): RemoteValue<*>? = null

    /**
     * Reports whether this cache-key DAG can safely be replaced with a constant literal when its
     * runtime value is statically known.
     *
     * Structurally rejects instance-keyed values, mutable array keys, unknown operations, random
     * generators, stateful animations, and `forceRemote = true` references in `O(1)` via
     * precomputed flags on [BaseRemoteStateCacheKey].
     *
     * When [canFoldStateId] is `null`, or when the DAG contains no [RemoteStateIdKey] leaves, the
     * check finishes in `O(1)` without traversing children. When [canFoldStateId] is non-null and
     * the DAG contains [RemoteStateIdKey] nodes, only branches containing state IDs are traversed
     * and [canFoldStateId] is invoked with each reachable [RemoteStateIdKey.id]; this method
     * returns `true` only if [canFoldStateId] returns `true` for every reachable state ID
     * (short-circuiting to `false` on the first rejection). Callers use this predicate to block
     * folding of specific variable IDs such as mutable/output slots or animated variables. If a
     * branch containing a state ID exceeds the internal traversal depth limit, this method
     * conservatively returns `false`.
     *
     * @param canFoldStateId optional predicate tested against each reachable [RemoteStateIdKey.id];
     *   returning `false` for any ID prevents constant folding of the enclosing DAG (`null` permits
     *   all state IDs in `O(1)`)
     * @return `true` if replacing with a constant preserves runtime behavior
     */
    public fun canConstantFold(canFoldStateId: ((Int) -> Boolean)? = null): Boolean = true

    public fun toDebugString(): String

    /**
     * Checks whether this cache key's expression DAG evaluates deterministically.
     *
     * Returns `false` when any node in the DAG may produce a different value on repeated
     * evaluations within the same frame—for example, expressions calling [rand] or [randRange],
     * stateful animations, or unkeyed `RemoteFloat { ... }` lambdas.
     *
     * @param requireFiniteFloats when `true`, also rejects expressions that contain or may produce
     *   non-finite [Float] values (`Infinity` or `NaN`, such as `x / 0f` or `ln(x)`)
     * @return `true` if the expression is deterministic (and finite when requested)
     */
    public fun isDeterministic(requireFiniteFloats: Boolean = false): Boolean = true
}

/** Bit flag indicating every node in the expression DAG is deterministic. */
internal const val FLAG_DETERMINISTIC: Int = 1

/** Bit flag indicating at least one node in the expression DAG holds a non-finite [Float]. */
internal const val FLAG_HAS_NON_FINITE_FLOAT: Int = 1 shl 1

/** Bit flag indicating every finite evaluation of this expression has no fractional part. */
internal const val FLAG_RESULT_WHOLE_NUMBER: Int = 1 shl 2

/**
 * Bit flag indicating this expression DAG is structurally eligible for constant folding (ignoring
 * caller-supplied per-state-ID restrictions).
 */
internal const val FLAG_CAN_CONSTANT_FOLD: Int = 1 shl 3

/**
 * Bit flag indicating this expression DAG contains at least one [RemoteStateIdKey] (or custom
 * non-[BaseRemoteStateCacheKey] leaf) that requires consulting `canFoldStateId`.
 */
internal const val FLAG_HAS_STATE_ID: Int = 1 shl 4

/**
 * Traverses this [RemoteStateCacheKey] DAG using the provided [visitor].
 *
 * @param visitor The visitor that inspects each node.
 * @param memo A memoization map to ensure shared DAG nodes are visited once.
 * @return The result of visiting this node.
 */
internal fun <R> RemoteStateCacheKey.accept(
    visitor: RemoteStateVisitor<R>,
    memo: MutableMap<RemoteStateCacheKey, R> = mutableMapOf(),
): R {
    memo[this]?.let {
        return it
    }
    val visitedArgs = ArrayList<R>(args.size)
    for (i in 0 until args.size) {
        visitedArgs.add(args[i].accept(visitor, memo))
    }
    val state = (this as? BaseRemoteStateCacheKey)?.getState()
    val result = visitor.visit(this, state, visitedArgs)
    memo[this] = result
    return result
}

internal fun List<RemoteStateCacheKey>.joinToDebugString(
    separator: String = ", ",
    startIndex: Int = 0,
): String = buildString {
    for (i in startIndex until this@joinToDebugString.size) {
        if (i > startIndex) {
            append(separator)
        }
        append(this@joinToDebugString[i].toDebugString())
    }
}

internal fun List<RemoteStateCacheKey>.formatOp(opSymbol: String, precedence: Int): String =
    this[0].toOperandString(precedence) +
        " $opSymbol " +
        this[1].toOperandString(precedence, isRightOperand = true)

internal fun List<RemoteStateCacheKey>.formatArrayAccess(precedence: Int): String =
    this[0].toOperandString(precedence) + "[" + this[1].toDebugString() + "]"

internal fun List<RemoteStateCacheKey>.formatSelect(opSymbol: String): String {
    val cond =
        "${this[0].toOperandString(1)} $opSymbol " +
            this[1].toOperandString(1, isRightOperand = true)
    val trueBranch = this[2].toOperandString(0, isRightOperand = true)
    val falseBranch = this[3].toOperandString(0, isRightOperand = true)
    return "$cond ? $trueBranch : $falseBranch"
}

internal fun List<String>.fastJoinToString(separator: String = ", "): String = buildString {
    for (i in indices) {
        if (i > 0) {
            append(separator)
        }
        append(this@fastJoinToString[i])
    }
}

internal fun Enum<*>.formatCamelCaseFunction(args: List<RemoteStateCacheKey>): String =
    "${name.replaceFirstChar { it.lowercase() }}(${args.joinToDebugString()})"

/**
 * Interface implemented by remote operation keys (e.g. [RemoteFloatOperationKey]) to define
 * precedence, custom AST debug formatting (e.g. `user:a + user:b`), and AST node reconstruction.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteOperation {
    public val precedence: Int
        get() = 100

    /**
     * Whether evaluating this operation with identical inputs always yields the same result.
     *
     * Operations that sample fresh or animated state—such as [RemoteFloatOperationKey.Rand],
     * [RemoteFloatOperationKey.RandInRange], and [RemoteFloatOperationKey.Anim]—return `false` so
     * `rand() - rand()` is never simplified to `0f`.
     */
    public val isDeterministic: Boolean
        get() = true

    /**
     * Checks whether this operation may evaluate to a non-finite [Float] (`Infinity` or `NaN`) for
     * the given [args] even when every child in [args] is finite.
     */
    public fun mayProduceNonFiniteFloatForArgs(args: List<RemoteStateCacheKey>): Boolean = false

    /** Formats this operation AST using its evaluated operand [args]. */
    public fun toDebugString(args: List<RemoteStateCacheKey>): String

    /**
     * Reconstructs the [RemoteValue] operation using the new [args].
     *
     * @param args The transformed argument list of [RemoteValue] instances.
     * @return The reconstructed [RemoteValue] instance.
     */
    public fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> =
        throw UnsupportedOperationException("Reconstruction is not supported for $this")

    /**
     * Reports whether this operation yields a whole number whenever its result is finite.
     *
     * Operations that always truncate or produce discrete values (such as [floor], [ceil], [round],
     * comparisons, or integer ops) return `true`. Integer-preserving operations inspect [args]
     * lazily via [RemoteStateCacheKey.isResultWholeNumber] (for example, `+` and `*` require both
     * operands to be integer-valued, while `select` requires both result arms to be
     * integer-valued). Operations that may introduce fractions (such as `/` or [sin]) return
     * `false`.
     *
     * @param args operand cache keys in [RemoteStateCacheKey.args]
     * @return `true` if every finite result has no fractional part
     */
    public fun isResultWholeNumber(args: List<RemoteStateCacheKey>): Boolean = resultKind.isIntegral

    /**
     * Reports whether this operation can be replaced with a constant literal when its runtime value
     * is statically known.
     *
     * Defaults to [isDeterministic]; explicit `forceRemote = true` references return `false`
     * because they must remain runtime remote variables.
     *
     * @param args operation operand cache keys
     * @return `true` if replacing with a constant preserves runtime behavior
     */
    public fun canConstantFoldWithArgs(args: List<RemoteStateCacheKey>): Boolean = isDeterministic

    /**
     * Reports whether operand [operandIndex] must stay a compile-time constant during wire
     * encoding.
     *
     * AST rewrites (such as templating or subtree capture) keep constant-only operands as literals
     * rather than replacing them with dynamic variables.
     *
     * ```kotlin
     * // x.toRemoteStringOptions(before = 2, after = 1, flags = 0) has args [x, 2, 1, 0]:
     * op.requiresConstantForOperandIndex(0) // false — value `x` may be dynamic
     * op.requiresConstantForOperandIndex(1) // true  — `before` digit count is a wire literal
     *
     * // x.createReference(forceRemote = true) has args [x, true]:
     * op.requiresConstantForOperandIndex(0) // false — referenced float `x` may be dynamic
     * op.requiresConstantForOperandIndex(1) // true  — `forceRemote` stays a literal Boolean
     * ```
     *
     * * Other constant-only operands include animation specs ([RemoteFloatOperationKey.Anim] index
     *   `1`), color component tags, and `fromAHSV` alpha / `lerp` ARGB endpoints.
     *
     * @param operandIndex zero-based operand index in [RemoteStateCacheKey.args]
     * @return `true` if operand [operandIndex] cannot be replaced by a dynamic expression
     */
    public fun requiresConstantForOperandIndex(operandIndex: Int): Boolean = false

    /**
     * Reports whether this operation can fail at runtime on invalid values of operand
     * [operandIndex] (such as integer division by zero or out-of-bounds array/string indexing).
     *
     * @param operandIndex zero-based operand index in [RemoteStateCacheKey.args]
     * @return `true` if guards protecting operand [operandIndex] must not be dropped eagerly
     */
    public fun canFailOnOperandIndex(operandIndex: Int): Boolean = false

    /**
     * Static [RemoteValueKind] produced by this operation (defaults to [RemoteValueKind.OTHER]).
     */
    public val resultKind: RemoteValueKind
        get() = RemoteValueKind.OTHER
}

/**
 * Formats this cache key as an operand string within an outer operation.
 *
 * If this key represents a nested operation whose [RemoteOperation.precedence] is lower than
 * [parentPrecedence] (or equal, if [isRightOperand] is true), the resulting debug string is wrapped
 * in parentheses to accurately preserve evaluation order and associativity.
 */
internal fun RemoteStateCacheKey.toOperandString(
    parentPrecedence: Int,
    isRightOperand: Boolean = false,
): String {
    val str = toDebugString()
    if (this is RemoteOperationCacheKey) {
        val childOp = op as? RemoteOperation
        if (childOp != null) {
            val needsParentheses =
                childOp.precedence < parentPrecedence ||
                    (childOp.precedence == parentPrecedence && isRightOperand) ||
                    (childOp.precedence == 0 && parentPrecedence == 0)
            if (needsParentheses) {
                return "($str)"
            }
        }
    }
    return str
}

/** Base class for [RemoteStateCacheKey] implementations that provides a memoized [hashCode]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseRemoteStateCacheKey : RemoteStateCacheKey {
    private var _hashCode: Int = 0

    /**
     * Associated [RemoteValue] instance, used for common sub-expression elimination and DAG
     * traversal. Default implementation returns null. Note to save memory this deliberately doesn't
     * have a backing field because not all sub-classes need it.
     */
    public open fun getState(): RemoteValue<*>? = null

    /**
     * Associates this key with a [RemoteValue] instance. Default implementation is a no-op to avoid
     * allocating backing fields on keys that do not retain state.
     */
    public open fun setState(state: RemoteValue<*>?) {}

    /**
     * Precomputed bitmask of expression properties ([FLAG_DETERMINISTIC],
     * [FLAG_HAS_NON_FINITE_FLOAT], [FLAG_RESULT_WHOLE_NUMBER], [FLAG_CAN_CONSTANT_FOLD],
     * [FLAG_HAS_STATE_ID]) synthesized bottom-up at construction time in `O(1)`.
     */
    internal open val expressionFlags: Int
        get() = FLAG_DETERMINISTIC or FLAG_CAN_CONSTANT_FOLD

    override val isResultWholeNumber: Boolean
        get() = (expressionFlags and FLAG_RESULT_WHOLE_NUMBER) != 0

    override fun isDeterministic(requireFiniteFloats: Boolean): Boolean =
        (expressionFlags and FLAG_DETERMINISTIC) != 0 &&
            (!requireFiniteFloats || (expressionFlags and FLAG_HAS_NON_FINITE_FLOAT) == 0)

    final override fun canConstantFold(canFoldStateId: ((Int) -> Boolean)?): Boolean =
        canConstantFold(DEFAULT_MAX_FOLD_DEPTH, canFoldStateId)

    internal fun canConstantFold(
        maxDepth: Int,
        canFoldStateId: ((Int) -> Boolean)? = null,
        visited: MutableSet<RemoteStateCacheKey>? = null,
    ): Boolean {
        if (maxDepth < 0 || (expressionFlags and FLAG_CAN_CONSTANT_FOLD) == 0) return false
        if (canFoldStateId == null || (expressionFlags and FLAG_HAS_STATE_ID) == 0) return true
        if (visited != null && !visited.add(this)) return true
        return canConstantFoldStateIds(maxDepth, canFoldStateId, visited)
    }

    internal open fun canConstantFoldStateIds(
        maxDepth: Int,
        canFoldStateId: (Int) -> Boolean,
        visited: MutableSet<RemoteStateCacheKey>? = null,
    ): Boolean = true

    override fun getValueKind(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind = getState()?.valueKind ?: getValueKindUnattached(stateIdKind, namedKeyKind)

    protected open fun getValueKindUnattached(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind = RemoteValueKind.OTHER

    final override fun getOrCreateValueOrNull(
        createForStateId: (Int) -> RemoteValue<*>?
    ): RemoteValue<*>? = getState() ?: createUnattachedValueOrNull(createForStateId)

    protected open fun createUnattachedValueOrNull(
        createForStateId: (Int) -> RemoteValue<*>?
    ): RemoteValue<*>? = null

    final override fun hashCode(): Int {
        if (_hashCode == 0) {
            _hashCode = hashCodeImpl()
            if (_hashCode == 0) _hashCode = 1
        }
        return _hashCode
    }

    protected abstract fun hashCodeImpl(): Int

    abstract override fun equals(other: Any?): Boolean
}

/** A fallback cache key based on the identity of this key instance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteStateInstanceKey
internal constructor(private val isDeterministicInstance: Boolean) : BaseRemoteStateCacheKey() {
    public constructor() : this(isDeterministicInstance = true)

    private var state: RemoteValue<*>? = null

    override fun getState(): RemoteValue<*>? = state

    override fun setState(state: RemoteValue<*>?) {
        this.state = state
    }

    private val baseExpressionFlags: Int = if (isDeterministicInstance) FLAG_DETERMINISTIC else 0

    override val expressionFlags: Int
        get() =
            if (state.isIntegralValueType()) {
                baseExpressionFlags or FLAG_RESULT_WHOLE_NUMBER
            } else {
                baseExpressionFlags
            }

    override fun hashCodeImpl(): Int = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean = this === other

    override fun toDebugString(): String = "instance"
}

internal class RemoteStateArrayKey(internal val size: Int) : BaseRemoteStateCacheKey() {
    private var state: RemoteValue<*>? = null

    override fun getState(): RemoteValue<*>? = state

    override fun setState(state: RemoteValue<*>?) {
        this.state = state
    }

    override val expressionFlags: Int
        get() = FLAG_DETERMINISTIC

    override fun getValueKindUnattached(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind = RemoteValueKind.FLOAT_ARRAY

    override fun hashCodeImpl(): Int = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean = this === other

    override fun toDebugString(): String = "mutableFloatArray(size=$size)"
}

/** A cache key for constant values (primitive types, strings, etc.). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteConstantCacheKey(public val value: Any?) : BaseRemoteStateCacheKey() {
    private var stateRef: WeakReference<RemoteValue<*>>? = null

    override fun getState(): RemoteValue<*>? = stateRef?.get()

    override fun setState(state: RemoteValue<*>?) {
        stateRef = if (state != null) WeakReference(state) else null
    }

    override fun getValueKindUnattached(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind =
        when (value) {
            is Float,
            is Double -> RemoteValueKind.FLOAT
            is Int,
            is Short,
            is Byte -> RemoteValueKind.INT
            is Boolean -> RemoteValueKind.BOOLEAN
            is String -> RemoteValueKind.STRING
            is Long -> RemoteValueKind.LONG
            is Enum<*> -> RemoteValueKind.ENUM
            else -> RemoteValueKind.OTHER
        }

    override fun createUnattachedValueOrNull(
        createForStateId: (Int) -> RemoteValue<*>?
    ): RemoteValue<*>? =
        try {
            resolveConstantState(this)
        } catch (_: IllegalArgumentException) {
            null
        }

    init {
        if (value is Float) {
            check(!value.isNaN()) { "Float constant value cannot be NaN" }
        }
        if (value is Double) {
            check(!value.isNaN()) { "Double constant value cannot be NaN" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteConstantCacheKey) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCodeImpl(): Int =
        when (value) {
            // Enums inherit java.lang.Object.hashCode() (identity hash code), which is
            // non-deterministic across JVM executions. We hash the declaring class name and
            // constant name to ensure determinism.
            is Enum<*> -> value.declaringJavaClass.name.hashCode() + value.name.hashCode()
            else -> value?.hashCode() ?: 0
        }

    override fun toString(): String = "RemoteConstantCacheKey(value=$value)"

    override fun toDebugString(): String =
        when (value) {
            null -> "null"
            is String -> "\"$value\""
            is Enum<*> -> "${value::class.simpleName}.${value.name}"
            else -> value.toString()
        }

    private val baseExpressionFlags: Int =
        FLAG_DETERMINISTIC or
            FLAG_CAN_CONSTANT_FOLD or
            (if (value is Float && !value.isFinite()) FLAG_HAS_NON_FINITE_FLOAT else 0) or
            (if (value.isConstantWholeNumber()) FLAG_RESULT_WHOLE_NUMBER else 0)

    override val expressionFlags: Int
        get() =
            if (getState() is MutableRemoteFloat) {
                baseExpressionFlags and FLAG_RESULT_WHOLE_NUMBER.inv()
            } else {
                baseExpressionFlags
            }
}

/** A cache key for constant color values, identified by their packed ARGB integer value. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteColorConstantCacheKey(@ColorInt public val color: Int) :
    BaseRemoteStateCacheKey() {
    private var state: RemoteValue<*>? = null

    override fun getState(): RemoteValue<*>? = state

    override fun setState(state: RemoteValue<*>?) {
        this.state = state
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteColorConstantCacheKey) return false
        return color == other.color
    }

    override fun hashCodeImpl(): Int = color.hashCode()

    override fun getValueKindUnattached(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind = RemoteValueKind.COLOR

    override fun createUnattachedValueOrNull(
        createForStateId: (Int) -> RemoteValue<*>?
    ): RemoteValue<*> = RemoteColor(color)

    override fun toString(): String =
        "RemoteColorConstantCacheKey(color=0x${color.toUInt().toString(16)})"

    override fun toDebugString(): String = "Color(0x${color.toUInt().toString(16)})"
}

/** A cache key for named variables, identified by their [name] and [domain]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteNamedCacheKey(
    public val domain: RemoteValue.Domain,
    public val name: String,
) : BaseRemoteStateCacheKey() {
    /** Qualified wire name including [domain] prefix (e.g., `"USER:count"` or `"SYSTEM:theme"`). */
    public val wireName: String
        get() = domain.prefixed(name)

    private var state: RemoteValue<*>? = null

    override fun getState(): RemoteValue<*>? = state

    override fun setState(state: RemoteValue<*>?) {
        this.state = state
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteNamedCacheKey) return false
        if (domain != other.domain) return false
        if (name != other.name) return false
        return true
    }

    override val expressionFlags: Int
        get() =
            if (state.isIntegralValueType()) {
                FLAG_DETERMINISTIC or FLAG_CAN_CONSTANT_FOLD or FLAG_RESULT_WHOLE_NUMBER
            } else {
                FLAG_DETERMINISTIC or FLAG_CAN_CONSTANT_FOLD
            }

    override fun getValueKindUnattached(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind = namedKeyKind(this)

    override fun hashCodeImpl(): Int {
        var result = domain.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "RemoteNamedCacheKey(domain=$domain, name=$name)"

    override fun toDebugString(): String = "${domain.prefix.lowercase()}$name"
}

/** A cache key for variable by id. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteStateIdKey(public val id: Int) : BaseRemoteStateCacheKey() {
    private var state: RemoteValue<*>? = null

    override fun getState(): RemoteValue<*>? = state

    override fun setState(state: RemoteValue<*>?) {
        this.state = state
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteStateIdKey) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCodeImpl(): Int = id

    private val baseExpressionFlags: Int =
        FLAG_DETERMINISTIC or
            FLAG_CAN_CONSTANT_FOLD or
            FLAG_HAS_STATE_ID or
            (if (id <= 0) {
                // id == 0 (Utils.idFromNan) or id == -1 (NanEncoding.idFromNan) encodes literal
                // IEEE Float.NaN rather than a valid variable ID.
                FLAG_HAS_NON_FINITE_FLOAT
            } else {
                0
            }) or
            (if (isIntegerContextId(id)) FLAG_RESULT_WHOLE_NUMBER else 0)

    override val expressionFlags: Int
        get() {
            val s = state
            return when {
                s is MutableRemoteFloat -> baseExpressionFlags and FLAG_RESULT_WHOLE_NUMBER.inv()
                s.isIntegralValueType() -> baseExpressionFlags or FLAG_RESULT_WHOLE_NUMBER
                else -> baseExpressionFlags
            }
        }

    override fun getValueKindUnattached(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind = stateIdKind(id)

    override fun createUnattachedValueOrNull(
        createForStateId: (Int) -> RemoteValue<*>?
    ): RemoteValue<*>? = createForStateId(id)

    override fun canConstantFoldStateIds(
        maxDepth: Int,
        canFoldStateId: (Int) -> Boolean,
        visited: MutableSet<RemoteStateCacheKey>?,
    ): Boolean = canFoldStateId(id)

    override fun toString(): String = "RemoteStateIdKey(id=$id)"

    override fun toDebugString(): String {
        return when (id) {
            RemoteContext.ID_CONTINUOUS_SEC -> "context:continuous_sec"
            RemoteContext.ID_TIME_IN_SEC -> "context:time_in_sec"
            RemoteContext.ID_TIME_IN_MIN -> "context:time_in_min"
            RemoteContext.ID_TIME_IN_HR -> "context:time_in_hr"
            RemoteContext.ID_WINDOW_WIDTH -> "context:window_width"
            RemoteContext.ID_WINDOW_HEIGHT -> "context:window_height"
            RemoteContext.ID_COMPONENT_WIDTH -> "context:component_width"
            RemoteContext.ID_COMPONENT_HEIGHT -> "context:component_height"
            RemoteContext.ID_CALENDAR_MONTH -> "context:calendar_month"
            RemoteContext.ID_OFFSET_TO_UTC -> "context:offset_to_utc"
            RemoteContext.ID_WEEK_DAY -> "context:week_day"
            RemoteContext.ID_DAY_OF_MONTH -> "context:day_of_month"
            RemoteContext.ID_DAY_OF_YEAR -> "context:day_of_year"
            RemoteContext.ID_YEAR -> "context:year"
            RemoteContext.ID_TOUCH_POS_X -> "context:touch_pos_x"
            RemoteContext.ID_TOUCH_POS_Y -> "context:touch_pos_y"
            RemoteContext.ID_TOUCH_VEL_X -> "context:touch_vel_x"
            RemoteContext.ID_TOUCH_VEL_Y -> "context:touch_vel_y"
            RemoteContext.ID_ACCELERATION_X -> "context:acceleration_x"
            RemoteContext.ID_ACCELERATION_Y -> "context:acceleration_y"
            RemoteContext.ID_ACCELERATION_Z -> "context:acceleration_z"
            RemoteContext.ID_GYRO_ROT_X -> "context:gyro_rot_x"
            RemoteContext.ID_GYRO_ROT_Y -> "context:gyro_rot_y"
            RemoteContext.ID_GYRO_ROT_Z -> "context:gyro_rot_z"
            RemoteContext.ID_MAGNETIC_X -> "context:magnetic_x"
            RemoteContext.ID_MAGNETIC_Y -> "context:magnetic_y"
            RemoteContext.ID_MAGNETIC_Z -> "context:magnetic_z"
            RemoteContext.ID_LIGHT -> "context:light"
            RemoteContext.ID_DENSITY -> "context:density"
            RemoteContext.ID_API_LEVEL -> "context:api_level"
            RemoteContext.ID_TOUCH_EVENT_TIME -> "context:touch_event_time"
            RemoteContext.ID_ANIMATION_TIME -> "context:animation_time"
            RemoteContext.ID_ANIMATION_DELTA_TIME -> "context:animation_delta_time"
            RemoteContext.ID_EPOCH_SECOND -> "context:epoch_second"
            RemoteContext.ID_FONT_SIZE -> "context:font_size"
            else -> "context:#$id"
        }
    }
}

/** A synthetic state wrapper for literal [FloatArray] arguments in operations. */
internal class FloatArrayRemoteValue(internal val floatArray: FloatArray, key: FloatArrayCacheKey) :
    BaseRemoteValue<FloatArray>(key) {
    override val equalityType: KClass<*>
        get() = FloatArrayRemoteValue::class

    override val constantValueOrNull: FloatArray
        get() = floatArray

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        throw UnsupportedOperationException(
            "FloatArrayRemoteValue cannot be written directly to document"
        )
    }
}

/**
 * A cache key for literal [FloatArray] arguments.
 *
 * Elements may be NaN-encoded variable ids or operators, so equality uses [nanIdContentEquals]
 * rather than [FloatArray.contentEquals], which treats all NaNs as equal.
 */
internal class FloatArrayCacheKey(internal val floatArray: FloatArray) : BaseRemoteStateCacheKey() {
    override fun getValueKindUnattached(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind = RemoteValueKind.FLOAT_ARRAY

    override fun createUnattachedValueOrNull(
        createForStateId: (Int) -> RemoteValue<*>?
    ): RemoteValue<*> = FloatArrayRemoteValue(floatArray, this)

    override fun equals(other: Any?): Boolean {
        return other is FloatArrayCacheKey && floatArray.nanIdContentEquals(other.floatArray)
    }

    override fun hashCodeImpl(): Int = floatArray.nanIdContentHashCode()

    override fun toDebugString(): String = floatArray.contentToString()
}

/**
 * A cache key for component-specific values (like width/height/center), identified by the
 * [componentId] (or component [token]) and the [type] of value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComponentCacheKey
internal constructor(
    public val componentId: Int,
    public val type: String,
    internal val token: RemoteComponentToken?,
) : BaseRemoteStateCacheKey() {
    public constructor(componentId: Int, type: String) : this(componentId, type, null)

    internal constructor(token: RemoteComponentToken, type: String) : this(-1, type, token)

    private var state: RemoteValue<*>? = null

    override fun getState(): RemoteValue<*>? = state

    override fun setState(state: RemoteValue<*>?) {
        this.state = state
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteComponentCacheKey) return false
        if (token != other.token) return false
        if (token == null && componentId != other.componentId) return false
        if (type != other.type) return false
        return true
    }

    override fun hashCodeImpl(): Int {
        var result = token?.hashCode() ?: componentId
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String =
        if (token != null) {
            "RemoteComponentCacheKey(token=${token.hashCode()}, type=$type)"
        } else {
            "RemoteComponentCacheKey(componentId=$componentId, type=$type)"
        }

    override fun toDebugString(): String =
        if (token != null) {
            "component:@${token.hashCode()}.$type"
        } else {
            "component:$componentId.$type"
        }
}

/**
 * A cache key for operations performed on other remote states, identified by the operation [op]
 * (usually an [Enum]) and its [args].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteOperationCacheKey(
    public val op: Enum<*>,
    override val args: List<RemoteStateCacheKey>,
) : BaseRemoteStateCacheKey() {
    private var state: RemoteValue<*>? = null

    override fun getState(): RemoteValue<*>? = state

    override fun setState(state: RemoteValue<*>?) {
        this.state = state
    }

    override fun getValueKind(
        stateIdKind: (Int) -> RemoteValueKind,
        namedKeyKind: (RemoteNamedCacheKey) -> RemoteValueKind,
    ): RemoteValueKind {
        val opKind = (op as? RemoteOperation)?.resultKind
        if (opKind != null && opKind != RemoteValueKind.OTHER) return opKind
        return getState()?.valueKind ?: RemoteValueKind.OTHER
    }

    override fun canConstantFoldStateIds(
        maxDepth: Int,
        canFoldStateId: (Int) -> Boolean,
        visited: MutableSet<RemoteStateCacheKey>?,
    ): Boolean {
        val seen =
            visited ?: HashSet<RemoteStateCacheKey>().apply { add(this@RemoteOperationCacheKey) }
        val childMaxDepth = maxDepth - 1
        for (i in 0 until args.size) {
            val child = args[i]
            if (child is BaseRemoteStateCacheKey) {
                if (
                    (child.expressionFlags and FLAG_HAS_STATE_ID) != 0 &&
                        !child.canConstantFold(childMaxDepth, canFoldStateId, seen)
                ) {
                    return false
                }
            } else if (childMaxDepth < 0) {
                return false
            } else if (seen.add(child) && !child.canConstantFold(canFoldStateId)) {
                return false
            }
        }
        return true
    }

    /**
     * Reconstructs this [RemoteOperationCacheKey] with replacement [args], returning `null` on
     * failure.
     *
     * Callers needing a specific wrapper subtype can adapt the result via
     * [RemoteValue.adaptToType].
     *
     * @param args replacement operand values matching this operation's arity and types
     * @return reconstructed [RemoteValue], or `null` if reconstruction is unsupported or fails
     */
    public fun reconstructOrNull(args: List<RemoteValue<*>>): RemoteValue<*>? {
        val operation = op as? RemoteOperation ?: return null
        return try {
            if (op == EnumOperationKey.RemoteEnum) {
                val enumNode = getState() as? RemoteEnum<*> ?: return null
                return RemoteEnum(args[0] as RemoteInt, enumNode.enumEntries)
            }
            operation.reconstruct(args)
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteOperationCacheKey) return false
        if (op != other.op) return false
        if (args != other.args) return false
        return true
    }

    override fun hashCodeImpl(): Int {
        // Enums inherit java.lang.Object.hashCode() (identity hash code), which is
        // non-deterministic across JVM executions. We hash the declaring class name and
        // constant name to ensure determinism.
        var result = op.declaringJavaClass.name.hashCode() + op.name.hashCode()
        result = 31 * result + args.hashCode()
        return result
    }

    override fun toString(): String = "RemoteOperationCacheKey(op=$op, args=$args)"

    override fun toDebugString(): String {
        return if (op is RemoteOperation) {
            (op as RemoteOperation).toDebugString(args)
        } else {
            "Operation($op, args=[${args.joinToDebugString()}])"
        }
    }

    private val baseExpressionFlags: Int = computeExpressionFlags(op, args)

    override val expressionFlags: Int
        get() =
            if (state is MutableRemoteFloat) {
                baseExpressionFlags and FLAG_RESULT_WHOLE_NUMBER.inv()
            } else {
                baseExpressionFlags
            }

    public companion object {
        /**
         * Synthesizes the bottom-up [expressionFlags] bitmask (`O(1)`) for [op] applied to [args].
         *
         * * Clears [FLAG_DETERMINISTIC] if [op] is a [RemoteOperation] with `isDeterministic ==
         *   false` or any child in [args] is non-deterministic. (Plain [Enum] keys that do not
         *   implement [RemoteOperation] default to deterministic.)
         * * Sets [FLAG_HAS_NON_FINITE_FLOAT] if [op] reports
         *   [RemoteOperation.mayProduceNonFiniteFloatForArgs] for [args] or any child in [args]
         *   already carries [FLAG_HAS_NON_FINITE_FLOAT].
         * * Sets [FLAG_RESULT_WHOLE_NUMBER] if [op] is a [RemoteOperation] whose
         *   [RemoteOperation.isResultWholeNumber] returns `true` for [args].
         * * Sets [FLAG_CAN_CONSTANT_FOLD] if [op] is a [RemoteOperation] whose
         *   [RemoteOperation.canConstantFoldWithArgs] returns `true` for [args] and every child in
         *   [args] is foldable.
         * * Sets [FLAG_HAS_STATE_ID] if any child in [args] carries [FLAG_HAS_STATE_ID] (or is a
         *   custom non-[BaseRemoteStateCacheKey]).
         *
         * @param op operation key identifying the AST node
         * @param args child operand cache keys
         * @return combined bitmask of expression flags
         */
        internal fun computeExpressionFlags(op: Enum<*>, args: List<RemoteStateCacheKey>): Int {
            val remoteOp = op as? RemoteOperation
            var det = if (remoteOp?.isDeterministic == false) 0 else FLAG_DETERMINISTIC
            var nonFinite =
                if (remoteOp?.mayProduceNonFiniteFloatForArgs(args) == true) {
                    FLAG_HAS_NON_FINITE_FLOAT
                } else {
                    0
                }
            val wholeNumber =
                if (remoteOp?.isResultWholeNumber(args) == true) FLAG_RESULT_WHOLE_NUMBER else 0
            var canFold =
                if (remoteOp != null && remoteOp.canConstantFoldWithArgs(args)) {
                    FLAG_CAN_CONSTANT_FOLD
                } else {
                    0
                }
            var hasStateId = 0
            for (i in args.indices) {
                val child = args[i]
                val childFlags =
                    if (child is BaseRemoteStateCacheKey) {
                        child.expressionFlags
                    } else {
                        var flags =
                            if (!child.isDeterministic(requireFiniteFloats = false)) {
                                0
                            } else if (!child.isDeterministic(requireFiniteFloats = true)) {
                                FLAG_DETERMINISTIC or FLAG_HAS_NON_FINITE_FLOAT
                            } else {
                                FLAG_DETERMINISTIC
                            }
                        if ((flags and FLAG_DETERMINISTIC) != 0 && child.canConstantFold()) {
                            flags = flags or FLAG_CAN_CONSTANT_FOLD or FLAG_HAS_STATE_ID
                        }
                        flags
                    }
                det = det and (childFlags and FLAG_DETERMINISTIC)
                nonFinite = nonFinite or (childFlags and FLAG_HAS_NON_FINITE_FLOAT)
                canFold = canFold and (childFlags and FLAG_CAN_CONSTANT_FOLD)
                hasStateId = hasStateId or (childFlags and FLAG_HAS_STATE_ID)
            }
            return det or nonFinite or wholeNumber or canFold or hasStateId
        }

        /** Creates a [RemoteOperationCacheKey] by converting [args] to a list of cache keys. */
        public fun create(op: Enum<*>, vararg args: Any?): RemoteOperationCacheKey =
            RemoteOperationCacheKey(op, toCacheKeyList(args))
    }
}

/**
 * Converts a list of arguments into a list of [RemoteStateCacheKey]s. Any argument that is not
 * already a [RemoteStateCacheKey] is wrapped in a [RemoteConstantCacheKey].
 */
internal fun toCacheKeyList(args: Array<out Any?>): List<RemoteStateCacheKey> {
    return args.map {
        when (it) {
            null -> RemoteConstantCacheKey(null)
            is RemoteConstantCacheKey -> it
            is RemoteValue<*> -> it.cacheKey
            is RemoteStateCacheKey -> it
            is Byte,
            is Short,
            is Int,
            is Long,
            is Float,
            is Double,
            is Boolean,
            is Char,
            is String,
            is Enum<*> -> RemoteConstantCacheKey(it)
            else ->
                throw IllegalArgumentException(
                    "Unsupported cache key type: ${it::class}. " +
                        "Only primitives, Strings, Enums and RemoteValues are supported."
                )
        }
    }
}

/** The cache key for this remote state within the RemoteComposeCreationState. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteValue<*>.cacheKey: RemoteStateCacheKey
    get() = requireRemoteValueAccess().cacheKey

private fun RemoteValue<*>?.isIntegralValueType(): Boolean =
    this is RemoteInt || this is RemoteBoolean || this is RemoteLong

private fun Any?.isConstantWholeNumber(): Boolean =
    when (this) {
        is Int,
        is Long,
        is Short,
        is Byte,
        is Boolean -> true
        is Float -> isWholeNumberConstant()
        is Double -> isWholeNumberConstant()
        else -> false
    }

private fun Float.isWholeNumberConstant(): Boolean = isFinite() && this == mathFloor(this)

private fun Double.isWholeNumberConstant(): Boolean = isFinite() && this == mathFloor(this)

private fun isIntegerContextId(id: Int): Boolean =
    when (id) {
        RemoteContext.ID_TIME_IN_SEC,
        RemoteContext.ID_TIME_IN_MIN,
        RemoteContext.ID_TIME_IN_HR,
        RemoteContext.ID_CALENDAR_MONTH,
        RemoteContext.ID_OFFSET_TO_UTC,
        RemoteContext.ID_WEEK_DAY,
        RemoteContext.ID_DAY_OF_MONTH,
        RemoteContext.ID_DAY_OF_YEAR,
        RemoteContext.ID_YEAR,
        RemoteContext.ID_EPOCH_SECOND -> true
        else -> false
    }

/**
 * Default traversal depth limit when checking per-state-ID foldability in
 * [BaseRemoteStateCacheKey].
 */
internal const val DEFAULT_MAX_FOLD_DEPTH: Int = 16

/** The canonical domain identifier for user-defined state in the RemoteCompose wire protocol. */
internal const val USER_DOMAIN: String = "USER"

/** The canonical domain identifier for system-defined state in the RemoteCompose wire protocol. */
internal const val SYSTEM_DOMAIN: String = "SYSTEM"

/**
 * Value-domain classification of a [RemoteValue] for AST memoization and operand typing.
 *
 * Distinct [RemoteValue] wrappers (such as [RemoteFloat], [RemoteInt], and [RemoteBoolean]) can
 * share identical [RemoteStateCacheKey] instances; keying memo tables by `(value.valueKind,
 * value.cacheKey)` prevents type collisions across passes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RemoteValueKind {
    /** [RemoteFloat] value. */
    FLOAT,

    /** [RemoteInt] value. */
    INT,

    /** [RemoteBoolean] value. */
    BOOLEAN,

    /** [RemoteString] value. */
    STRING,

    /** [RemoteColor] value. */
    COLOR,

    /** [RemoteDp] value. */
    DP,

    /** [RemoteTextUnit] value. */
    TEXT_UNIT,

    /** [RemoteLong] value. */
    LONG,

    /** [RemoteImageBitmap] value. */
    IMAGE_BITMAP,

    /** [RemoteFloatArray] or [RemoteMutableFloatArray] value. */
    FLOAT_ARRAY,

    /** [RemoteIntArray] value. */
    INT_ARRAY,

    /** [RemoteStringArray] value. */
    STRING_ARRAY,

    /** [RemoteMatrix3x3] value. */
    MATRIX,

    /** [RemoteEnum] value. */
    ENUM,

    /** Any other [RemoteValue] subtype. */
    OTHER;

    /**
     * Reports whether values of this kind are wire-encoded as 32-bit integers ([INT] or [BOOLEAN]).
     */
    public val isIntEncoded: Boolean
        get() = this == INT || this == BOOLEAN

    /**
     * Reports whether every finite value of this kind is an exact whole number ([INT], [BOOLEAN],
     * or [LONG]).
     */
    public val isIntegral: Boolean
        get() = this == INT || this == BOOLEAN || this == LONG
}

/** Classifies this [RemoteValue] into its [RemoteValueKind]. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteValue<*>.valueKind: RemoteValueKind
    get() = (this as? RemoteValueAccess)?.valueKind ?: RemoteValueKind.OTHER

/**
 * Creates an ID-backed [RemoteValue] matching this value's concrete domain for wire [id].
 *
 * Useful when synthesizing formal parameters for patterns or expression templates whose type must
 * mirror an existing template value.
 *
 * @param id raw document variable or resource ID
 * @return ID-backed [RemoteValue] of the same domain, or `null` if this type cannot be bound by ID
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteValue<*>.createForIdOrNull(id: Int): RemoteValue<*>? =
    (this as? RemoteValueAccess)?.createForIdOrNull(id)

/**
 * Adapts [candidate] to match this value's concrete [RemoteValue] type, or returns `this`.
 *
 * Converts between [RemoteFloat], [RemoteInt], [RemoteBoolean], [RemoteDp], and [RemoteEnum] when
 * compatible, validates matching [RemoteEnum.enumEntries] for enums, and preserves public
 * [MutableRemoteValue] subtypes without downcasting to immutable supertypes.
 *
 * @param candidate replacement value to adapt
 * @return type-compatible [RemoteValue], or `this` if adaptation is not possible
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteValue<*>.adaptToType(candidate: RemoteValue<*>): RemoteValue<*> =
    (this as? RemoteValueAccess)?.adaptToType(candidate) ?: this
