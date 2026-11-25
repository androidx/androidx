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

package androidx.compose.remote.creation.compose.state

/** Algebraic category of a scalar unary or binary operation for [ScalarPeepholeOptimizer]. */
internal enum class AlgebraicOpKind {
    Add,
    Sub,
    Mul,
    Div,
    Neg,
    Idempotent,
    SelfZero,
    Other,
}

/** Polarity of an additive term (`+term` or `-term`). */
internal enum class TermSign {
    Positive,
    Negative;

    /** Flips `+` to `-` and `-` to `+`. */
    fun flip(): TermSign = if (this == Positive) Negative else Positive
}

/** Pairs an expression or constant [term] with its additive [sign] (`+term` or `-term`). */
internal data class SignedTerm<out T>(val term: T, val sign: TermSign = TermSign.Positive)

/** Result of folding two trailing additive constants `(x +/- c1) +/- c2`. */
internal sealed interface AdditiveConstFold<out T> {
    data object Zero : AdditiveConstFold<Nothing>

    data class Add<T>(val constant: T) : AdditiveConstFold<T>

    data class Sub<T>(val constant: T) : AdditiveConstFold<T>
}

/** Result of folding trailing multiplicative/divisive constants `(x *|/ c1) *|/ c2`. */
internal sealed interface MulDivConstFold<out T> {
    data object Zero : MulDivConstFold<Nothing>

    data object Identity : MulDivConstFold<Nothing>

    data class Mul<T>(val constant: T) : MulDivConstFold<T>

    data class Div<T>(val constant: T) : MulDivConstFold<T>
}

/** Matched binary AST node exposed to [ScalarPeepholeOptimizer]. */
internal data class BinaryMatch<E>(val kind: AlgebraicOpKind, val left: E, val right: E)

/** Matched constant-branch conditional select AST node exposed to [ScalarPeepholeOptimizer]. */
internal data class SelectMatch<T : Any, Cond>(
    val condition: Cond,
    val ifTrue: T,
    val ifFalse: T,
)

/**
 * Domain policy isolating numeric differences (IEEE-754 [Float] vs wrapping 32-bit [Int]) from the
 * shared algebraic rewrite rules in [ScalarPeepholeOptimizer].
 */
internal interface ScalarAlgebra<T : Any, E, Op : RemoteOperation, Cond> {
    val zero: T
    val one: T

    fun isZero(value: T): Boolean

    fun isOne(value: T): Boolean

    fun constantsEqualExact(a: T, b: T): Boolean

    /** Checks whether evaluating [expr] twice in the same frame yields identical values. */
    fun isDeterministic(expr: E): Boolean

    /**
     * Checks whether `expr - expr` is guaranteed to evaluate to [zero].
     *
     * Safe to cancel:
     * - Deterministic named state or pure integer math (e.g., `(x + y) - x` simplifies to `y`).
     *
     * Not safe to cancel:
     * - Non-deterministic calls such as [rand], where `rand() - rand()` samples two independent
     *   values.
     * - [RemoteFloat] expressions that may evaluate to `Infinity` or `NaN` (e.g., `(1f / y) - (1f /
     *   y)` when `y == 0f`, where `Infinity - Infinity` must remain `NaN` rather than `0f`).
     */
    fun isSafeToCancel(expr: E): Boolean

    /** Checks whether [condition] evaluates deterministically without side effects or [rand]. */
    fun isDeterministicCondition(condition: Cond): Boolean

    /** Extracts the known constant value of [expr], or returns `null` if [expr] is dynamic. */
    fun getConstantFrom(expr: E): T?

    fun wrapConstant(value: T): E

    /** Returns the structural [RemoteStateCacheKey] identifying [expr]. */
    fun getCacheKey(expr: E): RemoteStateCacheKey

    /** Classifies [op] into its [AlgebraicOpKind] category for unary rewrites. */
    fun classifyUnaryOp(op: Op): AlgebraicOpKind

    /** Classifies [op] into its [AlgebraicOpKind] category for binary rewrites. */
    fun classifyBinaryOp(op: Op): AlgebraicOpKind

    /** Unwraps and returns the inner operand if [expr] is unary negation, or `null`. */
    fun tryMatchUnaryNeg(expr: E): E?

    /** Unpacks [expr] into a [BinaryMatch] if it is a binary node, or returns `null`. */
    fun tryMatchBinary(expr: E): BinaryMatch<E>?

    /**
     * Unpacks [expr] into a [BinaryMatch] if it is an unpackable [AlgebraicOpKind.Add] or
     * [AlgebraicOpKind.Sub] node.
     */
    fun tryMatchAdditive(expr: E): BinaryMatch<E>?

    /**
     * Unpacks [expr] into a [SelectMatch] if it selects between two constants, or returns `null`.
     */
    fun tryMatchConstantSelect(expr: E): SelectMatch<T, Cond>?

    fun tryEvalConstantUnary(op: Op, value: T, directEval: (T) -> T): T?

    fun tryEvalConstantBinary(op: Op, a: T, b: T, directEval: (T, T) -> T): T?

    fun tryEvalUnaryOnSelectBranch(op: Op, value: T, directEval: (T) -> T): T?

    fun tryEvalBinaryOnSelectBranch(op: Op, a: T, b: T, directEval: (T, T) -> T): T?

    fun canCancelOppositeConstants(a: SignedTerm<T>, b: SignedTerm<T>): Boolean

    fun tryCombineAdditiveConstants(
        prev: T,
        prevIsSub: Boolean,
        next: T,
        nextIsSub: Boolean,
    ): AdditiveConstFold<T>?

    fun tryCombineMulDivConstants(
        innerKind: AlgebraicOpKind,
        prev: T,
        outerKind: AlgebraicOpKind,
        next: T,
    ): MulDivConstFold<T>?

    fun add(a: E, b: E): E

    fun sub(a: E, b: E): E

    fun mul(a: E, b: E): E

    fun div(a: E, b: E): E

    fun addConst(a: E, b: T): E = add(a, wrapConstant(b))

    fun subConst(a: E, b: T): E = sub(a, wrapConstant(b))

    fun mulConst(a: E, b: T): E = mul(a, wrapConstant(b))

    fun divConst(a: E, b: T): E = div(a, wrapConstant(b))

    fun neg(a: E): E

    fun buildSelect(condition: Cond, ifTrue: T, ifFalse: T): E

    /** Constructs an unsimplified unary AST node applying [op] to [operand]. */
    fun buildRawUnaryOp(op: Op, operand: E): E

    /** Constructs an unsimplified binary AST node applying [op] to [left] and [right]. */
    fun buildRawBinaryOp(op: Op, left: E, right: E): E
}

/**
 * Shared creation-time algebraic simplifier for scalar remote expressions ([RemoteFloat] and
 * [RemoteInt]).
 *
 * Implements smart-constructor rewrites over typed AST nodes:
 * - Constant folding
 * - Algebraic identities (`x + 0`, `x - 0`, `0 - x`, `x * 0`, `x * 1`, `0 / x`, `x / 1`) and
 *   constant-to-RHS canonicalization (`c + x -> x + c`, `c * x -> x * c`, `c - x -> (-x) + c`)
 * - Double-negation elimination (`-(-x) -> x`)
 * - Creation-time trailing constant reassociation (`(x +/- c1) +/- c2`, `(x *|/ c1) *|/ c2`)
 * - Opposite-sign additive cancellation (`y + (c - y) -> c`, `(a + b) - a -> b`, etc.)
 * - Idempotent / self-cancelling collapse (`min`, `max`, `and`, `or`, `xor`)
 * - Conditional `Select` lifting (`op(select)`, `c op select`, `select op c`, `select op select`)
 *   with constant-branch collapse when both branches evaluate to the exact same value
 */
internal class ScalarPeepholeOptimizer<T : Any, E, Op : RemoteOperation, Cond>(
    internal val algebra: ScalarAlgebra<T, E, Op, Cond>
) {
    /** Simplifies a unary operation `op(operand)` at AST creation time. */
    fun simplifyUnaryOp(op: Op, operand: E, directEval: (T) -> T): E {
        algebra.getConstantFrom(operand)?.let { c ->
            val folded = algebra.tryEvalConstantUnary(op, c, directEval)
            return if (folded != null) {
                algebra.wrapConstant(folded)
            } else {
                algebra.buildRawUnaryOp(op, operand)
            }
        }

        // 1. Simplify double negation (-(-x) -> x).
        if (algebra.classifyUnaryOp(op) == AlgebraicOpKind.Neg) {
            algebra.tryMatchUnaryNeg(operand)?.let {
                return it
            }
        }

        // 2. Lift unary op into constant-branch Select (op(cond.select(t, f)) -> cond.select(...)).
        algebra.tryMatchConstantSelect(operand)?.let { sel ->
            val newTrue = algebra.tryEvalUnaryOnSelectBranch(op, sel.ifTrue, directEval)
            val newFalse = algebra.tryEvalUnaryOnSelectBranch(op, sel.ifFalse, directEval)
            if (newTrue != null && newFalse != null) {
                if (algebra.constantsEqualExact(newTrue, newFalse)) {
                    return algebra.wrapConstant(newTrue)
                }
                return algebra.buildSelect(sel.condition, newTrue, newFalse)
            }
        }

        return algebra.buildRawUnaryOp(op, operand)
    }

    /** Simplifies a binary operation `left op right` at AST creation time. */
    fun simplifyBinaryOp(op: Op, left: E, right: E, directEval: (T, T) -> T): E =
        simplifyBinaryOpInternal(
            op = op,
            left = left,
            lConst = algebra.getConstantFrom(left),
            right = right,
            rConst = algebra.getConstantFrom(right),
            directEval = directEval,
        )

    /**
     * Simplifies `lConst op right` with an unboxed left constant, avoiding eager wrapper
     * allocations on fast paths.
     */
    fun simplifyBinaryOpLeftConst(op: Op, lConst: T, right: E, directEval: (T, T) -> T): E =
        simplifyBinaryOpInternal(
            op = op,
            left = null,
            lConst = lConst,
            right = right,
            rConst = algebra.getConstantFrom(right),
            directEval = directEval,
        )

    /**
     * Simplifies `left op rConst` with an unboxed right constant, avoiding eager wrapper
     * allocations on fast paths.
     */
    fun simplifyBinaryOpRightConst(op: Op, left: E, rConst: T, directEval: (T, T) -> T): E =
        simplifyBinaryOpInternal(
            op = op,
            left = left,
            lConst = algebra.getConstantFrom(left),
            right = null,
            rConst = rConst,
            directEval = directEval,
        )

    private fun simplifyBinaryOpInternal(
        op: Op,
        left: E?,
        lConst: T?,
        right: E?,
        rConst: T?,
        directEval: (T, T) -> T,
    ): E {
        val kind = algebra.classifyBinaryOp(op)

        // 1. Annihilating-zero short-circuits precede directEval so `0 / 0` and `Inf * 0f` match
        //    runtime/baseline semantics without dividing by zero or producing NaN.
        if (
            kind == AlgebraicOpKind.Mul &&
                ((rConst != null && algebra.isZero(rConst)) ||
                    (lConst != null && algebra.isZero(lConst)))
        ) {
            return algebra.wrapConstant(algebra.zero)
        }
        if (kind == AlgebraicOpKind.Div && lConst != null && algebra.isZero(lConst)) {
            return algebra.wrapConstant(algebra.zero)
        }

        // 2. Constant folding when both operands are known constants.
        if (lConst != null && rConst != null) {
            val folded = algebra.tryEvalConstantBinary(op, lConst, rConst, directEval)
            return if (folded != null) {
                algebra.wrapConstant(folded)
            } else {
                algebra.buildRawBinaryOp(
                    op,
                    left ?: algebra.wrapConstant(lConst),
                    right ?: algebra.wrapConstant(rConst),
                )
            }
        }

        // At most one operand is a constant below: when rConst != null, left != null; when
        // lConst != null, right != null.

        // 3. Algebraic identities and canonical constant placement (constants on RHS).
        when (kind) {
            AlgebraicOpKind.Add -> {
                if (rConst != null && algebra.isZero(rConst)) return left!!
                if (lConst != null) {
                    return if (algebra.isZero(lConst)) {
                        right!!
                    } else {
                        algebra.addConst(right!!, lConst)
                    }
                }
            }
            AlgebraicOpKind.Sub -> {
                if (rConst != null && algebra.isZero(rConst)) return left!!
                if (lConst != null) {
                    return if (algebra.isZero(lConst)) {
                        algebra.neg(right!!)
                    } else {
                        algebra.addConst(algebra.neg(right!!), lConst)
                    }
                }
            }
            AlgebraicOpKind.Mul -> {
                if (rConst != null && algebra.isOne(rConst)) return left!!
                if (lConst != null) {
                    return if (algebra.isOne(lConst)) {
                        right!!
                    } else {
                        algebra.mulConst(right!!, lConst)
                    }
                }
            }
            AlgebraicOpKind.Div -> {
                if (rConst != null && algebra.isOne(rConst)) return left!!
            }
            else -> {}
        }

        // 4. Reassociate trailing constants at creation time ((x op1 c1) op2 c2).
        if (rConst != null) {
            tryFoldTrailingConstant(kind, left!!, rConst)?.let {
                return it
            }
        }

        // 5. Lift binary op across constant-branch Select nodes before materializing constant
        //    wrappers.
        if (lConst != null) {
            algebra.tryMatchConstantSelect(right!!)?.let { rSel ->
                foldSelectWithConstant(op, lConst, rSel, isLeftConstant = true, directEval)?.let {
                    return it
                }
            }
        }
        if (rConst != null) {
            algebra.tryMatchConstantSelect(left!!)?.let { lSel ->
                foldSelectWithConstant(op, rConst, lSel, isLeftConstant = false, directEval)?.let {
                    return it
                }
            }
        }
        if (left != null && right != null) {
            val lSel = algebra.tryMatchConstantSelect(left)
            val rSel = algebra.tryMatchConstantSelect(right)
            if (
                lSel != null &&
                    rSel != null &&
                    lSel.condition == rSel.condition &&
                    algebra.isDeterministicCondition(lSel.condition)
            ) {
                foldSelectWithSelect(op, lSel, rSel, directEval)?.let {
                    return it
                }
            }
        }

        val leftExpr = left ?: algebra.wrapConstant(lConst!!)
        val rightExpr = right ?: algebra.wrapConstant(rConst!!)

        // 6. Operation-specific DAG simplifications (additive cancellation, idempotent ops).
        val simplified =
            when (kind) {
                AlgebraicOpKind.Add ->
                    trySimplifyMatchingOppositeSignTerms(leftExpr, rightExpr, subtractRight = false)
                AlgebraicOpKind.Sub ->
                    trySimplifyMatchingOppositeSignTerms(leftExpr, rightExpr, subtractRight = true)
                AlgebraicOpKind.Idempotent ->
                    if (
                        algebra.getCacheKey(leftExpr) == algebra.getCacheKey(rightExpr) &&
                            algebra.isDeterministic(leftExpr) &&
                            algebra.isDeterministic(rightExpr)
                    ) {
                        if (
                            leftExpr is MutableRemoteValue<*> && rightExpr !is MutableRemoteValue<*>
                        ) {
                            rightExpr
                        } else {
                            leftExpr
                        }
                    } else {
                        null
                    }
                AlgebraicOpKind.SelfZero ->
                    if (
                        algebra.getCacheKey(leftExpr) == algebra.getCacheKey(rightExpr) &&
                            algebra.isDeterministic(leftExpr) &&
                            algebra.isDeterministic(rightExpr)
                    ) {
                        algebra.wrapConstant(algebra.zero)
                    } else {
                        null
                    }
                else -> null
            }
        if (simplified != null) {
            return simplified
        }

        return algebra.buildRawBinaryOp(op, leftExpr, rightExpr)
    }

    /**
     * Folds a trailing constant [rConst] into [left] if [left] is a compatible `Add`/`Sub` or
     * `Mul`/`Div` node with a constant right-hand operand.
     */
    internal fun tryFoldTrailingConstant(kind: AlgebraicOpKind, left: E, rConst: T): E? {
        val inner = algebra.tryMatchBinary(left) ?: return null
        val prevConst = algebra.getConstantFrom(inner.right) ?: return null

        if (
            (kind == AlgebraicOpKind.Add || kind == AlgebraicOpKind.Sub) &&
                (inner.kind == AlgebraicOpKind.Add || inner.kind == AlgebraicOpKind.Sub)
        ) {
            val folded =
                algebra.tryCombineAdditiveConstants(
                    prev = prevConst,
                    prevIsSub = inner.kind == AlgebraicOpKind.Sub,
                    next = rConst,
                    nextIsSub = kind == AlgebraicOpKind.Sub,
                ) ?: return null
            return when (folded) {
                AdditiveConstFold.Zero -> inner.left
                is AdditiveConstFold.Add -> algebra.addConst(inner.left, folded.constant)
                is AdditiveConstFold.Sub -> algebra.subConst(inner.left, folded.constant)
            }
        }

        if (
            (kind == AlgebraicOpKind.Mul || kind == AlgebraicOpKind.Div) &&
                (inner.kind == AlgebraicOpKind.Mul || inner.kind == AlgebraicOpKind.Div)
        ) {
            val folded =
                algebra.tryCombineMulDivConstants(
                    innerKind = inner.kind,
                    prev = prevConst,
                    outerKind = kind,
                    next = rConst,
                ) ?: return null
            return applyMulDivFold(inner.left, folded)
        }

        return null
    }

    /** Rebuilds an expression from [base] and the combined constant result [folded]. */
    private fun applyMulDivFold(base: E, folded: MulDivConstFold<T>): E =
        when (folded) {
            MulDivConstFold.Zero -> algebra.wrapConstant(algebra.zero)
            MulDivConstFold.Identity -> base
            is MulDivConstFold.Mul -> algebra.mulConst(base, folded.constant)
            is MulDivConstFold.Div -> algebra.divConst(base, folded.constant)
        }

    /**
     * Absorbs one outer unary negation from [t] by unwrapping [SignedTerm.term] and flipping
     * [SignedTerm.sign].
     *
     * Turns `SignedTerm(-x, Positive)` into `SignedTerm(x, Negative)` and `SignedTerm(-x,
     * Negative)` into `SignedTerm(x, Positive)`, leaving non-negated terms unchanged.
     */
    private fun normalizeSign(t: SignedTerm<E>): SignedTerm<E> {
        val unwrapped = algebra.tryMatchUnaryNeg(t.term)
        return if (unwrapped != null) SignedTerm(unwrapped, t.sign.flip()) else t
    }

    /**
     * Splits an additive [SignedTerm] into its signed left and right child terms, or `null`.
     *
     * Propagates [SignedTerm.sign] across `+` and `-`: `-(a - b)` unpacks to `(-a, +b)`, while
     * `+(a - b)` unpacks to `(+a, -b)`.
     */
    private fun unpackAdditive(t: SignedTerm<E>): Pair<SignedTerm<E>, SignedTerm<E>>? {
        val bin = algebra.tryMatchAdditive(t.term) ?: return null
        val rightSign = if (bin.kind == AlgebraicOpKind.Sub) t.sign.flip() else t.sign
        return SignedTerm(bin.left, t.sign) to SignedTerm(bin.right, rightSign)
    }

    /**
     * Materializes [t] back into an expression, negating [SignedTerm.term] when [SignedTerm.sign]
     * is [TermSign.Negative].
     */
    private fun toExpr(t: SignedTerm<E>): E =
        if (t.sign == TermSign.Positive) t.term else algebra.neg(t.term)

    /**
     * Checks whether signed terms [a] and [b] sum to zero safely.
     *
     * Unwraps at most one unary negation on each operand and delegates determinism/finiteness
     * safety to [ScalarAlgebra.isSafeToCancel].
     */
    internal fun sumsToZero(a: SignedTerm<E>, b: SignedTerm<E>): Boolean {
        val normA = normalizeSign(a)
        val normB = normalizeSign(b)

        val aConst = algebra.getConstantFrom(normA.term)
        val bConst = algebra.getConstantFrom(normB.term)
        if (aConst != null && bConst != null) {
            return algebra.canCancelOppositeConstants(
                SignedTerm(aConst, normA.sign),
                SignedTerm(bConst, normB.sign),
            )
        }
        if (normA.sign == normB.sign) return false
        return algebra.getCacheKey(normA.term) == algebra.getCacheKey(normB.term) &&
            algebra.isSafeToCancel(normA.term) &&
            algebra.isSafeToCancel(normB.term)
    }

    /** Sums two signed terms, placing a positive term first when available. */
    internal fun sumSigned(a: SignedTerm<E>, b: SignedTerm<E>): E =
        when {
            a.sign == TermSign.Positive && b.sign == TermSign.Positive ->
                algebra.add(a.term, b.term)
            a.sign == TermSign.Positive -> algebra.sub(a.term, b.term)
            b.sign == TermSign.Positive -> algebra.sub(b.term, a.term)
            else -> algebra.sub(algebra.neg(a.term), b.term)
        }

    /**
     * Cancels matching opposite-sign terms across `left + right` or `left - right` up to one level
     * deep.
     *
     * Handles direct cancellation (`a - a`), one-sided nested cancellation (`y + (c - y)`), and
     * immediate child cross-cancellation (`(a + c) + (b - c)`).
     */
    internal fun trySimplifyMatchingOppositeSignTerms(
        left: E,
        right: E,
        subtractRight: Boolean,
    ): E? {
        val a = SignedTerm(left, TermSign.Positive)
        val b = SignedTerm(right, if (subtractRight) TermSign.Negative else TermSign.Positive)
        if (sumsToZero(a, b)) {
            return algebra.wrapConstant(algebra.zero)
        }

        val aParts = unpackAdditive(a)
        val bParts = unpackAdditive(b)

        // a + (b0 + b1)
        if (bParts != null) {
            val (b0, b1) = bParts
            if (sumsToZero(a, b0)) return toExpr(b1)
            if (sumsToZero(a, b1)) return toExpr(b0)
        }
        // (a0 + a1) + b
        if (aParts != null) {
            val (a0, a1) = aParts
            if (sumsToZero(a0, b)) return toExpr(a1)
            if (sumsToZero(a1, b)) return toExpr(a0)
        }
        // (a0 + a1) + (b0 + b1)
        if (aParts != null && bParts != null) {
            val (a0, a1) = aParts
            val (b0, b1) = bParts
            if (sumsToZero(a0, b0)) return sumSigned(a1, b1)
            if (sumsToZero(a0, b1)) return sumSigned(a1, b0)
            if (sumsToZero(a1, b0)) return sumSigned(a0, b1)
            if (sumsToZero(a1, b1)) return sumSigned(a0, b0)
        }
        return null
    }

    /**
     * Distributes [op] with [constant] into the constant branches of [sel], collapsing equal
     * branches.
     */
    private fun foldSelectWithConstant(
        op: Op,
        constant: T,
        sel: SelectMatch<T, Cond>,
        isLeftConstant: Boolean,
        directEval: (T, T) -> T,
    ): E? {
        val newTrue =
            if (isLeftConstant) {
                algebra.tryEvalBinaryOnSelectBranch(op, constant, sel.ifTrue, directEval)
            } else {
                algebra.tryEvalBinaryOnSelectBranch(op, sel.ifTrue, constant, directEval)
            } ?: return null
        val newFalse =
            if (isLeftConstant) {
                algebra.tryEvalBinaryOnSelectBranch(op, constant, sel.ifFalse, directEval)
            } else {
                algebra.tryEvalBinaryOnSelectBranch(op, sel.ifFalse, constant, directEval)
            } ?: return null
        if (algebra.constantsEqualExact(newTrue, newFalse)) {
            return algebra.wrapConstant(newTrue)
        }
        return algebra.buildSelect(sel.condition, newTrue, newFalse)
    }

    /**
     * Merges [leftSel] and [rightSel] sharing a deterministic condition under [op], collapsing
     * equal branches.
     */
    private fun foldSelectWithSelect(
        op: Op,
        leftSel: SelectMatch<T, Cond>,
        rightSel: SelectMatch<T, Cond>,
        directEval: (T, T) -> T,
    ): E? {
        val newTrue =
            algebra.tryEvalBinaryOnSelectBranch(op, leftSel.ifTrue, rightSel.ifTrue, directEval)
                ?: return null
        val newFalse =
            algebra.tryEvalBinaryOnSelectBranch(op, leftSel.ifFalse, rightSel.ifFalse, directEval)
                ?: return null
        if (algebra.constantsEqualExact(newTrue, newFalse)) {
            return algebra.wrapConstant(newTrue)
        }
        return algebra.buildSelect(leftSel.condition, newTrue, newFalse)
    }
}

/** IEEE-754 [Float] algebra policy for [FloatPeepholeOptimizer]. */
internal object FloatAlgebra :
    ScalarAlgebra<Float, RemoteFloat, RemoteFloatOperationKey, SelectFloatCondition> {
    override val zero: Float = 0f
    override val one: Float = 1f

    override fun isZero(value: Float): Boolean = value == 0f

    override fun isOne(value: Float): Boolean = value == 1f

    override fun constantsEqualExact(a: Float, b: Float): Boolean = a.toRawBits() == b.toRawBits()

    override fun isDeterministic(expr: RemoteFloat): Boolean = expr.cacheKey.isDeterministic()

    override fun isSafeToCancel(expr: RemoteFloat): Boolean =
        (expr !is RemoteFloatSelect || !expr.hasNonFiniteConstantBranch) &&
            expr.cacheKey.isDeterministic(requireFiniteFloats = true)

    override fun isDeterministicCondition(condition: SelectFloatCondition): Boolean =
        condition.isDeterministic()

    override fun getConstantFrom(expr: RemoteFloat): Float? = expr.constantValueOrNull

    override fun wrapConstant(value: Float): RemoteFloat = RemoteFloat(value)

    override fun getCacheKey(expr: RemoteFloat): RemoteStateCacheKey = expr.cacheKey

    override fun classifyUnaryOp(op: RemoteFloatOperationKey): AlgebraicOpKind =
        if (op == RemoteFloatOperationKey.UnaryMinus) AlgebraicOpKind.Neg else AlgebraicOpKind.Other

    override fun classifyBinaryOp(op: RemoteFloatOperationKey): AlgebraicOpKind =
        when (op) {
            RemoteFloatOperationKey.Plus -> AlgebraicOpKind.Add
            RemoteFloatOperationKey.Minus -> AlgebraicOpKind.Sub
            RemoteFloatOperationKey.Times -> AlgebraicOpKind.Mul
            RemoteFloatOperationKey.Div -> AlgebraicOpKind.Div
            RemoteFloatOperationKey.Min,
            RemoteFloatOperationKey.Max -> AlgebraicOpKind.Idempotent
            else -> AlgebraicOpKind.Other
        }

    override fun tryMatchUnaryNeg(expr: RemoteFloat): RemoteFloat? =
        if (expr is UnaryRemoteFloatOp && expr.op == RemoteFloatOperationKey.UnaryMinus) {
            expr.operand
        } else {
            null
        }

    override fun tryMatchBinary(expr: RemoteFloat): BinaryMatch<RemoteFloat>? {
        if (expr !is BinaryRemoteFloatOp) return null
        return BinaryMatch(classifyBinaryOp(expr.op), expr.left, expr.right)
    }

    override fun tryMatchAdditive(expr: RemoteFloat): BinaryMatch<RemoteFloat>? {
        if (expr !is BinaryRemoteFloatOp) return null
        val kind =
            when (expr.op) {
                RemoteFloatOperationKey.Plus -> AlgebraicOpKind.Add
                RemoteFloatOperationKey.Minus -> AlgebraicOpKind.Sub
                else -> return null
            }
        if (
            expr.left.constantValueOrNull?.isFinite() == false ||
                expr.right.constantValueOrNull?.isFinite() == false
        ) {
            return null
        }
        return BinaryMatch(kind, expr.left, expr.right)
    }

    override fun tryMatchConstantSelect(
        expr: RemoteFloat
    ): SelectMatch<Float, SelectFloatCondition>? {
        if (expr !is RemoteFloatSelect || !expr.hasConstantBranches) return null
        return SelectMatch(
            expr.condition,
            expr.ifTrue.constantValueOrNull!!,
            expr.ifFalse.constantValueOrNull!!,
        )
    }

    override fun tryEvalConstantUnary(
        op: RemoteFloatOperationKey,
        value: Float,
        directEval: (Float) -> Float,
    ): Float? {
        // Host arithmetic produces IEEE-754 quiet NaN (0x7FC00000), which NanEncoding.isNan
        // intentionally excludes because NanEncoding only matches non-zero variable/op IDs.
        val result = directEval(value)
        return if (result.isNaN()) null else result
    }

    override fun tryEvalConstantBinary(
        op: RemoteFloatOperationKey,
        a: Float,
        b: Float,
        directEval: (Float, Float) -> Float,
    ): Float? {
        val result = directEval(a, b)
        return if (result.isNaN()) null else result
    }

    override fun tryEvalUnaryOnSelectBranch(
        op: RemoteFloatOperationKey,
        value: Float,
        directEval: (Float) -> Float,
    ): Float? {
        if (op != RemoteFloatOperationKey.UnaryMinus && op != RemoteFloatOperationKey.Abs) {
            return null
        }
        val result = directEval(value)
        return if (result.isNaN()) null else result
    }

    override fun tryEvalBinaryOnSelectBranch(
        op: RemoteFloatOperationKey,
        a: Float,
        b: Float,
        directEval: (Float, Float) -> Float,
    ): Float? {
        val result = directEval(a, b)
        return if (result.isNaN()) null else result
    }

    override fun canCancelOppositeConstants(
        a: SignedTerm<Float>,
        b: SignedTerm<Float>,
    ): Boolean {
        if (!a.term.isFinite() || !b.term.isFinite()) return false
        val signedA = if (a.sign == TermSign.Positive) a.term else -a.term
        val signedB = if (b.sign == TermSign.Positive) b.term else -b.term
        return signedA + signedB == 0f
    }

    override fun tryCombineAdditiveConstants(
        prev: Float,
        prevIsSub: Boolean,
        next: Float,
        nextIsSub: Boolean,
    ): AdditiveConstFold<Float>? {
        if (!prev.isFinite() || !next.isFinite()) return null
        val prevSigned = if (prevIsSub) -prev else prev
        val delta = if (nextIsSub) -next else next
        val combined = prevSigned + delta
        if (!combined.isFinite()) return null
        return when {
            combined == 0f -> AdditiveConstFold.Zero
            combined < 0f -> AdditiveConstFold.Sub(-combined)
            else -> AdditiveConstFold.Add(combined)
        }
    }

    override fun tryCombineMulDivConstants(
        innerKind: AlgebraicOpKind,
        prev: Float,
        outerKind: AlgebraicOpKind,
        next: Float,
    ): MulDivConstFold<Float>? {
        if (!prev.isFinite() || !next.isFinite()) return null
        val updated =
            when (outerKind) {
                AlgebraicOpKind.Mul ->
                    if (innerKind == AlgebraicOpKind.Mul) prev * next else prev / next
                AlgebraicOpKind.Div ->
                    if (innerKind == AlgebraicOpKind.Mul) prev / next else prev * next
                else -> return null
            }
        if (!updated.isFinite()) return null
        // Reject subnormal/zero underflow where mantissa precision would be lost on dynamic `x`.
        if (
            kotlin.math.abs(updated) < java.lang.Float.MIN_NORMAL &&
                (innerKind == AlgebraicOpKind.Div || (prev != 0f && next != 0f))
        ) {
            return null
        }
        return when {
            innerKind == AlgebraicOpKind.Mul && updated == 0f -> MulDivConstFold.Zero
            updated == 1f -> MulDivConstFold.Identity
            innerKind == AlgebraicOpKind.Mul -> MulDivConstFold.Mul(updated)
            else -> MulDivConstFold.Div(updated)
        }
    }

    override fun add(a: RemoteFloat, b: RemoteFloat): RemoteFloat = a + b

    override fun sub(a: RemoteFloat, b: RemoteFloat): RemoteFloat = a - b

    override fun mul(a: RemoteFloat, b: RemoteFloat): RemoteFloat = a * b

    override fun div(a: RemoteFloat, b: RemoteFloat): RemoteFloat = a / b

    override fun addConst(a: RemoteFloat, b: Float): RemoteFloat = a + b

    override fun subConst(a: RemoteFloat, b: Float): RemoteFloat = a - b

    override fun mulConst(a: RemoteFloat, b: Float): RemoteFloat = a * b

    override fun divConst(a: RemoteFloat, b: Float): RemoteFloat = a / b

    override fun neg(a: RemoteFloat): RemoteFloat = -a

    override fun buildSelect(
        condition: SelectFloatCondition,
        ifTrue: Float,
        ifFalse: Float,
    ): RemoteFloat = condition.createSelect(RemoteFloat(ifTrue), RemoteFloat(ifFalse))

    override fun buildRawUnaryOp(
        op: RemoteFloatOperationKey,
        operand: RemoteFloat,
    ): RemoteFloat = UnaryRemoteFloatOp(op, operand)

    override fun buildRawBinaryOp(
        op: RemoteFloatOperationKey,
        left: RemoteFloat,
        right: RemoteFloat,
    ): RemoteFloat = BinaryRemoteFloatOp(op, left, right)
}

/** Wrapping 32-bit [Int] algebra policy for [IntPeepholeOptimizer]. */
internal object IntAlgebra :
    ScalarAlgebra<Int, RemoteInt, RemoteIntOperationKey, SelectIntCondition> {
    override val zero: Int = 0
    override val one: Int = 1

    override fun isZero(value: Int): Boolean = value == 0

    override fun isOne(value: Int): Boolean = value == 1

    override fun constantsEqualExact(a: Int, b: Int): Boolean = a == b

    override fun isDeterministic(expr: RemoteInt): Boolean = expr.cacheKey.isDeterministic()

    override fun isSafeToCancel(expr: RemoteInt): Boolean = expr.cacheKey.isDeterministic()

    override fun isDeterministicCondition(condition: SelectIntCondition): Boolean =
        condition.isDeterministic()

    override fun getConstantFrom(expr: RemoteInt): Int? = expr.constantValueOrNull

    override fun wrapConstant(value: Int): RemoteInt = RemoteInt(value)

    override fun getCacheKey(expr: RemoteInt): RemoteStateCacheKey = expr.cacheKey

    override fun classifyUnaryOp(op: RemoteIntOperationKey): AlgebraicOpKind =
        if (op == RemoteIntOperationKey.Neg) AlgebraicOpKind.Neg else AlgebraicOpKind.Other

    override fun classifyBinaryOp(op: RemoteIntOperationKey): AlgebraicOpKind =
        when (op) {
            RemoteIntOperationKey.Add -> AlgebraicOpKind.Add
            RemoteIntOperationKey.Sub -> AlgebraicOpKind.Sub
            RemoteIntOperationKey.Mul -> AlgebraicOpKind.Mul
            RemoteIntOperationKey.Div -> AlgebraicOpKind.Div
            RemoteIntOperationKey.Min,
            RemoteIntOperationKey.Max,
            RemoteIntOperationKey.And,
            RemoteIntOperationKey.Or -> AlgebraicOpKind.Idempotent
            RemoteIntOperationKey.Xor -> AlgebraicOpKind.SelfZero
            else -> AlgebraicOpKind.Other
        }

    override fun tryMatchUnaryNeg(expr: RemoteInt): RemoteInt? =
        if (expr is UnaryRemoteIntOp && expr.op == RemoteIntOperationKey.Neg) expr.operand else null

    override fun tryMatchBinary(expr: RemoteInt): BinaryMatch<RemoteInt>? {
        if (expr !is BinaryRemoteIntOp) return null
        return BinaryMatch(classifyBinaryOp(expr.op), expr.left, expr.right)
    }

    override fun tryMatchAdditive(expr: RemoteInt): BinaryMatch<RemoteInt>? {
        if (expr !is BinaryRemoteIntOp) return null
        val kind =
            when (expr.op) {
                RemoteIntOperationKey.Add -> AlgebraicOpKind.Add
                RemoteIntOperationKey.Sub -> AlgebraicOpKind.Sub
                else -> return null
            }
        return BinaryMatch(kind, expr.left, expr.right)
    }

    override fun tryMatchConstantSelect(expr: RemoteInt): SelectMatch<Int, SelectIntCondition>? {
        if (expr !is RemoteIntSelect || !expr.hasConstantBranches) return null
        return SelectMatch(
            expr.condition,
            expr.ifTrue.constantValueOrNull!!,
            expr.ifFalse.constantValueOrNull!!,
        )
    }

    override fun tryEvalConstantUnary(
        op: RemoteIntOperationKey,
        value: Int,
        directEval: (Int) -> Int,
    ): Int = directEval(value)

    override fun tryEvalConstantBinary(
        op: RemoteIntOperationKey,
        a: Int,
        b: Int,
        directEval: (Int, Int) -> Int,
    ): Int? {
        if ((op == RemoteIntOperationKey.Div || op == RemoteIntOperationKey.Mod) && b == 0) {
            return null
        }
        return directEval(a, b)
    }

    override fun tryEvalUnaryOnSelectBranch(
        op: RemoteIntOperationKey,
        value: Int,
        directEval: (Int) -> Int,
    ): Int = directEval(value)

    override fun tryEvalBinaryOnSelectBranch(
        op: RemoteIntOperationKey,
        a: Int,
        b: Int,
        directEval: (Int, Int) -> Int,
    ): Int? {
        if ((op == RemoteIntOperationKey.Div || op == RemoteIntOperationKey.Mod) && b == 0) {
            return null
        }
        // IntegerExpressionEvaluator.OP_COPY_SIGN computes `(a ^ (b >> 31)) - (b >> 31)` (flipping
        // `a`'s sign when `b < 0` without taking `|a|`), diverging from host `withSign` when
        // `a < 0`. Skip Select lifting so runtime evaluation is preserved.
        if (op == RemoteIntOperationKey.CopySign) {
            return null
        }
        return directEval(a, b)
    }

    override fun canCancelOppositeConstants(a: SignedTerm<Int>, b: SignedTerm<Int>): Boolean {
        val signedA = if (a.sign == TermSign.Positive) a.term else -a.term
        val signedB = if (b.sign == TermSign.Positive) b.term else -b.term
        return signedA + signedB == 0
    }

    override fun tryCombineAdditiveConstants(
        prev: Int,
        prevIsSub: Boolean,
        next: Int,
        nextIsSub: Boolean,
    ): AdditiveConstFold<Int> {
        val prevSigned = if (prevIsSub) -prev else prev
        val delta = if (nextIsSub) -next else next
        val combined = prevSigned + delta
        return when {
            combined == 0 -> AdditiveConstFold.Zero
            combined < 0 && combined != Int.MIN_VALUE -> AdditiveConstFold.Sub(-combined)
            else -> AdditiveConstFold.Add(combined)
        }
    }

    override fun tryCombineMulDivConstants(
        innerKind: AlgebraicOpKind,
        prev: Int,
        outerKind: AlgebraicOpKind,
        next: Int,
    ): MulDivConstFold<Int>? =
        when (outerKind) {
            AlgebraicOpKind.Mul ->
                if (innerKind == AlgebraicOpKind.Mul) {
                    val prod = prev * next
                    when (prod) {
                        0 -> MulDivConstFold.Zero
                        1 -> MulDivConstFold.Identity
                        else -> MulDivConstFold.Mul(prod)
                    }
                } else {
                    // Integer division is not associative with multiplication: (x / 10) * 2 != x /
                    // 5.
                    null
                }
            AlgebraicOpKind.Div ->
                when (innerKind) {
                    AlgebraicOpKind.Mul ->
                        // Intentionally reassociates `(x * prev) / next -> x * (prev / next)` when
                        // `next` divides `prev` evenly, assuming the intermediate `x * prev`
                        // product does not overflow 32-bit signed `Int` (unlike `(x / prev) / next`
                        // below, where `prev * next` overflows the constants themselves even for
                        // small `x`).
                        if (
                            next != 0 && !(prev == Int.MIN_VALUE && next == -1) && prev % next == 0
                        ) {
                            when (val quot = prev / next) {
                                0 -> MulDivConstFold.Zero
                                1 -> MulDivConstFold.Identity
                                else -> MulDivConstFold.Mul(quot)
                            }
                        } else {
                            null
                        }
                    AlgebraicOpKind.Div -> {
                        val prodLong = prev.toLong() * next.toLong()
                        if (
                            next != 0 &&
                                !(prev == -1 && next != 1 && next != -1) &&
                                prodLong >= Int.MIN_VALUE &&
                                prodLong <= Int.MAX_VALUE
                        ) {
                            val prod = prodLong.toInt()
                            if (prod == 1) MulDivConstFold.Identity else MulDivConstFold.Div(prod)
                        } else {
                            null
                        }
                    }
                    else -> null
                }
            else -> null
        }

    override fun add(a: RemoteInt, b: RemoteInt): RemoteInt = a + b

    override fun sub(a: RemoteInt, b: RemoteInt): RemoteInt = a - b

    override fun mul(a: RemoteInt, b: RemoteInt): RemoteInt = a * b

    override fun div(a: RemoteInt, b: RemoteInt): RemoteInt = a / b

    override fun addConst(a: RemoteInt, b: Int): RemoteInt = a + b

    override fun subConst(a: RemoteInt, b: Int): RemoteInt = a - b

    override fun mulConst(a: RemoteInt, b: Int): RemoteInt = a * b

    override fun divConst(a: RemoteInt, b: Int): RemoteInt = a / b

    override fun neg(a: RemoteInt): RemoteInt = -a

    override fun buildSelect(
        condition: SelectIntCondition,
        ifTrue: Int,
        ifFalse: Int,
    ): RemoteInt = condition.createSelect(RemoteInt(ifTrue), RemoteInt(ifFalse))

    override fun buildRawUnaryOp(op: RemoteIntOperationKey, operand: RemoteInt): RemoteInt =
        UnaryRemoteIntOp(op, operand)

    override fun buildRawBinaryOp(
        op: RemoteIntOperationKey,
        left: RemoteInt,
        right: RemoteInt,
    ): RemoteInt = BinaryRemoteIntOp(op, left, right)
}

/** Shared peephole optimizer instance for [RemoteFloat] expressions. */
internal val FloatPeepholeOptimizer = ScalarPeepholeOptimizer(FloatAlgebra)

/** Shared peephole optimizer instance for [RemoteInt] expressions. */
internal val IntPeepholeOptimizer = ScalarPeepholeOptimizer(IntAlgebra)
