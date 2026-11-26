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

import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScalarPeepholeOptimizerTest {

    private fun evalFloat(
        expr: RemoteFloat,
        overrides: Map<String, Float> = emptyMap(),
        boolOverrides: Map<String, Boolean> = emptyMap(),
    ): Float {
        expr.constantValueOrNull?.let {
            if (overrides.isEmpty() && boolOverrides.isEmpty()) return it
        }
        val creationState = testCreationState()
        val context = TestRemoteContext()
        val id = creationState.getId(expr)
        context.loadDocument(creationState) {
            for ((name, v) in overrides) {
                context.setNamedFloatOverride("USER:$name", v)
            }
            for ((name, v) in boolOverrides) {
                context.setNamedIntegerOverride("USER:$name", if (v) 1 else 0)
            }
        }
        return context.getFloat(id)
    }

    private fun evalInt(
        expr: RemoteInt,
        overrides: Map<String, Int> = emptyMap(),
        boolOverrides: Map<String, Boolean> = emptyMap(),
    ): Int {
        expr.constantValueOrNull?.let {
            if (overrides.isEmpty() && boolOverrides.isEmpty()) return it
        }
        val creationState = testCreationState()
        val context = TestRemoteContext()
        val id = creationState.getId(expr)
        context.loadDocument(creationState) {
            for ((name, v) in overrides) {
                context.setNamedIntegerOverride("USER:$name", v)
            }
            for ((name, v) in boolOverrides) {
                context.setNamedIntegerOverride("USER:$name", if (v) 1 else 0)
            }
        }
        return context.getInteger(id)
    }

    // region Generic ScalarPeepholeOptimizer Engine (Isolated Fake Algebra)

    private enum class FakeOp : RemoteOperation {
        Add,
        Sub,
        Mul,
        Div,
        Neg,
        Idem,
        SelfZero,
        CustomUnary,
        CustomBinary;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String = name

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> =
            throw UnsupportedOperationException()
    }

    private sealed interface FakeNode {
        data class Const(val v: Int) : FakeNode

        data class Var(val name: String, val deterministic: Boolean = true) : FakeNode

        data class Unary(val op: FakeOp, val arg: FakeNode) : FakeNode

        data class Binary(val op: FakeOp, val left: FakeNode, val right: FakeNode) : FakeNode

        data class Select(
            val cond: String,
            val ifTrue: FakeNode,
            val ifFalse: FakeNode,
            val deterministicCond: Boolean = true,
        ) : FakeNode
    }

    private data class FakeCacheKey(val node: FakeNode) : RemoteStateCacheKey {
        override fun toDebugString(): String = node.toString()
    }

    private class FakeIntAlgebra(
        var rejectConstantEvalOn999: Boolean = false,
        var rejectSelectBranchOn999: Boolean = false,
    ) : ScalarAlgebra<Int, FakeNode, FakeOp, Pair<String, Boolean>> {
        override val zero: Int = 0
        override val one: Int = 1

        override fun isZero(value: Int): Boolean = value == 0

        override fun isOne(value: Int): Boolean = value == 1

        override fun constantsEqualExact(a: Int, b: Int): Boolean = a == b

        override fun isDeterministic(expr: FakeNode): Boolean =
            when (expr) {
                is FakeNode.Const -> true
                is FakeNode.Var -> expr.deterministic
                is FakeNode.Unary -> isDeterministic(expr.arg)
                is FakeNode.Binary -> isDeterministic(expr.left) && isDeterministic(expr.right)
                is FakeNode.Select ->
                    expr.deterministicCond &&
                        isDeterministic(expr.ifTrue) &&
                        isDeterministic(expr.ifFalse)
            }

        override fun isSafeToCancel(expr: FakeNode): Boolean = isDeterministic(expr)

        override fun isDeterministicCondition(condition: Pair<String, Boolean>): Boolean =
            condition.second

        override fun getConstantFrom(expr: FakeNode): Int? = (expr as? FakeNode.Const)?.v

        var wrapConstantCalls: Int = 0

        override fun wrapConstant(value: Int): FakeNode {
            wrapConstantCalls++
            return FakeNode.Const(value)
        }

        override fun getCacheKey(expr: FakeNode): RemoteStateCacheKey = FakeCacheKey(expr)

        override fun classifyUnaryOp(op: FakeOp): AlgebraicOpKind =
            if (op == FakeOp.Neg) AlgebraicOpKind.Neg else AlgebraicOpKind.Other

        override fun classifyBinaryOp(op: FakeOp): AlgebraicOpKind =
            when (op) {
                FakeOp.Add -> AlgebraicOpKind.Add
                FakeOp.Sub -> AlgebraicOpKind.Sub
                FakeOp.Mul -> AlgebraicOpKind.Mul
                FakeOp.Div -> AlgebraicOpKind.Div
                FakeOp.Idem -> AlgebraicOpKind.Idempotent
                FakeOp.SelfZero -> AlgebraicOpKind.SelfZero
                else -> AlgebraicOpKind.Other
            }

        override fun tryMatchUnaryNeg(expr: FakeNode): FakeNode? =
            if (expr is FakeNode.Unary && expr.op == FakeOp.Neg) expr.arg else null

        override fun tryMatchBinary(expr: FakeNode): BinaryMatch<FakeNode>? =
            if (expr is FakeNode.Binary) {
                BinaryMatch(classifyBinaryOp(expr.op), expr.left, expr.right)
            } else {
                null
            }

        override fun tryMatchAdditive(expr: FakeNode): BinaryMatch<FakeNode>? =
            if (expr is FakeNode.Binary && (expr.op == FakeOp.Add || expr.op == FakeOp.Sub)) {
                BinaryMatch(classifyBinaryOp(expr.op), expr.left, expr.right)
            } else {
                null
            }

        override fun tryMatchConstantSelect(
            expr: FakeNode
        ): SelectMatch<Int, Pair<String, Boolean>>? {
            if (expr !is FakeNode.Select) return null
            val t = (expr.ifTrue as? FakeNode.Const)?.v ?: return null
            val f = (expr.ifFalse as? FakeNode.Const)?.v ?: return null
            return SelectMatch(expr.cond to expr.deterministicCond, t, f)
        }

        override fun tryEvalConstantUnary(op: FakeOp, value: Int, directEval: (Int) -> Int): Int? {
            val res = directEval(value)
            return if (rejectConstantEvalOn999 && res == 999) null else res
        }

        override fun tryEvalConstantBinary(
            op: FakeOp,
            a: Int,
            b: Int,
            directEval: (Int, Int) -> Int,
        ): Int? {
            val res = directEval(a, b)
            return if (rejectConstantEvalOn999 && res == 999) null else res
        }

        override fun tryEvalUnaryOnSelectBranch(
            op: FakeOp,
            value: Int,
            directEval: (Int) -> Int,
        ): Int? {
            val res = directEval(value)
            return if (rejectSelectBranchOn999 && res == 999) null else res
        }

        override fun tryEvalBinaryOnSelectBranch(
            op: FakeOp,
            a: Int,
            b: Int,
            directEval: (Int, Int) -> Int,
        ): Int? {
            val res = directEval(a, b)
            return if (rejectSelectBranchOn999 && res == 999) null else res
        }

        override fun canCancelOppositeConstants(a: SignedTerm<Int>, b: SignedTerm<Int>): Boolean {
            val sa = if (a.sign == TermSign.Positive) a.term else -a.term
            val sb = if (b.sign == TermSign.Positive) b.term else -b.term
            return sa + sb == 0
        }

        override fun tryCombineAdditiveConstants(
            prev: Int,
            prevIsSub: Boolean,
            next: Int,
            nextIsSub: Boolean,
        ): AdditiveConstFold<Int>? {
            val p = if (prevIsSub) -prev else prev
            val n = if (nextIsSub) -next else next
            val sum = p + n
            if (sum == 999) return null
            return when {
                sum == 0 -> AdditiveConstFold.Zero
                sum < 0 -> AdditiveConstFold.Sub(-sum)
                else -> AdditiveConstFold.Add(sum)
            }
        }

        override fun tryCombineMulDivConstants(
            innerKind: AlgebraicOpKind,
            prev: Int,
            outerKind: AlgebraicOpKind,
            next: Int,
        ): MulDivConstFold<Int>? {
            if (prev == 999 || next == 999) return null
            return when (outerKind) {
                AlgebraicOpKind.Mul ->
                    if (innerKind == AlgebraicOpKind.Mul) {
                        val prod = prev * next
                        when (prod) {
                            0 -> MulDivConstFold.Zero
                            1 -> MulDivConstFold.Identity
                            else -> MulDivConstFold.Mul(prod)
                        }
                    } else {
                        null
                    }
                AlgebraicOpKind.Div ->
                    if (innerKind == AlgebraicOpKind.Div && next != 0) {
                        val prod = prev * next
                        if (prod == 1) MulDivConstFold.Identity else MulDivConstFold.Div(prod)
                    } else {
                        null
                    }
                else -> null
            }
        }

        override fun add(a: FakeNode, b: FakeNode): FakeNode =
            optimizer.simplifyBinaryOp(FakeOp.Add, a, b) { x, y -> x + y }

        override fun sub(a: FakeNode, b: FakeNode): FakeNode =
            optimizer.simplifyBinaryOp(FakeOp.Sub, a, b) { x, y -> x - y }

        override fun mul(a: FakeNode, b: FakeNode): FakeNode =
            optimizer.simplifyBinaryOp(FakeOp.Mul, a, b) { x, y -> x * y }

        override fun div(a: FakeNode, b: FakeNode): FakeNode =
            optimizer.simplifyBinaryOp(FakeOp.Div, a, b) { x, y -> x / y }

        override fun addConst(a: FakeNode, b: Int): FakeNode =
            optimizer.simplifyBinaryOpRightConst(FakeOp.Add, a, b) { x, y -> x + y }

        override fun subConst(a: FakeNode, b: Int): FakeNode =
            optimizer.simplifyBinaryOpRightConst(FakeOp.Sub, a, b) { x, y -> x - y }

        override fun mulConst(a: FakeNode, b: Int): FakeNode =
            optimizer.simplifyBinaryOpRightConst(FakeOp.Mul, a, b) { x, y -> x * y }

        override fun divConst(a: FakeNode, b: Int): FakeNode =
            optimizer.simplifyBinaryOpRightConst(FakeOp.Div, a, b) { x, y -> x / y }

        override fun neg(a: FakeNode): FakeNode = optimizer.simplifyUnaryOp(FakeOp.Neg, a) { -it }

        override fun buildSelect(
            condition: Pair<String, Boolean>,
            ifTrue: Int,
            ifFalse: Int,
        ): FakeNode =
            FakeNode.Select(
                condition.first,
                FakeNode.Const(ifTrue),
                FakeNode.Const(ifFalse),
                condition.second,
            )

        override fun buildRawUnaryOp(op: FakeOp, operand: FakeNode): FakeNode =
            FakeNode.Unary(op, operand)

        override fun buildRawBinaryOp(op: FakeOp, left: FakeNode, right: FakeNode): FakeNode =
            FakeNode.Binary(op, left, right)

        val optimizer = ScalarPeepholeOptimizer(this)
    }

    @Test
    fun genericEngine_shortCircuitsZeroBeforeDirectEval_andRespectsAlgebraRejections() {
        val algebra = FakeIntAlgebra(rejectConstantEvalOn999 = true, rejectSelectBranchOn999 = true)
        val opt = algebra.optimizer
        val x = FakeNode.Var("x")

        var evalCalls = 0
        // 0 / 0 must short-circuit to 0 without invoking directEval.
        val zeroDivZero =
            opt.simplifyBinaryOp(FakeOp.Div, FakeNode.Const(0), FakeNode.Const(0)) { a, b ->
                evalCalls++
                a / b
            }
        assertThat(zeroDivZero).isEqualTo(FakeNode.Const(0))
        assertThat(evalCalls).isEqualTo(0)

        // c * 0 and 0 * c short-circuit to 0 without invoking directEval.
        assertThat(
                opt.simplifyBinaryOp(FakeOp.Mul, FakeNode.Const(42), FakeNode.Const(0)) { _, _ ->
                    evalCalls++
                    error("unreachable")
                }
            )
            .isEqualTo(FakeNode.Const(0))
        assertThat(
                opt.simplifyBinaryOp(FakeOp.Mul, FakeNode.Const(0), FakeNode.Const(42)) { _, _ ->
                    evalCalls++
                    error("unreachable")
                }
            )
            .isEqualTo(FakeNode.Const(0))
        assertThat(evalCalls).isEqualTo(0)

        // Rejected constant eval returns raw AST node without infinite recursion on Add/Sub.
        assertThat(opt.simplifyUnaryOp(FakeOp.CustomUnary, FakeNode.Const(1)) { 999 })
            .isEqualTo(FakeNode.Unary(FakeOp.CustomUnary, FakeNode.Const(1)))
        assertThat(
                opt.simplifyBinaryOp(FakeOp.Sub, FakeNode.Const(1000), FakeNode.Const(1)) { a, b ->
                    a - b
                }
            )
            .isEqualTo(FakeNode.Binary(FakeOp.Sub, FakeNode.Const(1000), FakeNode.Const(1)))

        // Rejected select branch eval falls back cleanly to raw AST node.
        val sel = FakeNode.Select("c", FakeNode.Const(-999), FakeNode.Const(5))
        assertThat(opt.simplifyUnaryOp(FakeOp.Neg, sel) { -it })
            .isEqualTo(FakeNode.Unary(FakeOp.Neg, sel))
        assertThat(opt.simplifyBinaryOp(FakeOp.Add, sel, FakeNode.Const(1998)) { a, b -> a + b })
            .isEqualTo(FakeNode.Binary(FakeOp.Add, sel, FakeNode.Const(1998)))

        // Non-constant select does not lift.
        val dynSel = FakeNode.Select("c", x, FakeNode.Const(5))
        assertThat(opt.simplifyUnaryOp(FakeOp.Neg, dynSel) { -it })
            .isEqualTo(FakeNode.Unary(FakeOp.Neg, dynSel))
        assertThat(opt.simplifyBinaryOp(FakeOp.Add, dynSel, FakeNode.Const(1)) { a, b -> a + b })
            .isEqualTo(FakeNode.Binary(FakeOp.Add, dynSel, FakeNode.Const(1)))
    }

    @Test
    fun genericEngine_identitiesCanonicalizationAndNonCommutingOps() {
        val opt = FakeIntAlgebra().optimizer
        val x = FakeNode.Var("x")

        assertThat(opt.simplifyBinaryOp(FakeOp.Add, x, FakeNode.Const(0)) { a, b -> a + b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOp(FakeOp.Add, FakeNode.Const(0), x) { a, b -> a + b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOp(FakeOp.Add, FakeNode.Const(7), x) { a, b -> a + b })
            .isEqualTo(FakeNode.Binary(FakeOp.Add, x, FakeNode.Const(7)))

        assertThat(opt.simplifyBinaryOp(FakeOp.Sub, x, FakeNode.Const(0)) { a, b -> a - b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOp(FakeOp.Sub, FakeNode.Const(0), x) { a, b -> a - b })
            .isEqualTo(FakeNode.Unary(FakeOp.Neg, x))
        assertThat(opt.simplifyBinaryOp(FakeOp.Sub, FakeNode.Const(7), x) { a, b -> a - b })
            .isEqualTo(
                FakeNode.Binary(FakeOp.Add, FakeNode.Unary(FakeOp.Neg, x), FakeNode.Const(7))
            )

        assertThat(opt.simplifyBinaryOp(FakeOp.Mul, x, FakeNode.Const(1)) { a, b -> a * b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOp(FakeOp.Mul, FakeNode.Const(1), x) { a, b -> a * b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOp(FakeOp.Mul, FakeNode.Const(7), x) { a, b -> a * b })
            .isEqualTo(FakeNode.Binary(FakeOp.Mul, x, FakeNode.Const(7)))

        assertThat(opt.simplifyBinaryOp(FakeOp.Div, x, FakeNode.Const(1)) { a, b -> a / b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOp(FakeOp.Div, FakeNode.Const(1), x) { a, b -> a / b })
            .isEqualTo(FakeNode.Binary(FakeOp.Div, FakeNode.Const(1), x))
        assertThat(opt.simplifyBinaryOp(FakeOp.Div, FakeNode.Const(7), x) { a, b -> a / b })
            .isEqualTo(FakeNode.Binary(FakeOp.Div, FakeNode.Const(7), x))

        assertThat(
                opt.simplifyBinaryOp(FakeOp.CustomBinary, FakeNode.Const(0), x) { a, b -> a + b }
            )
            .isEqualTo(FakeNode.Binary(FakeOp.CustomBinary, FakeNode.Const(0), x))
        assertThat(
                opt.simplifyBinaryOp(FakeOp.CustomBinary, x, FakeNode.Const(0)) { a, b -> a + b }
            )
            .isEqualTo(FakeNode.Binary(FakeOp.CustomBinary, x, FakeNode.Const(0)))
    }

    @Test
    fun genericEngine_doubleNegIdempotentSelfZeroAdditiveCancelAndSelectCollapse() {
        val algebra = FakeIntAlgebra()
        val opt = algebra.optimizer
        val x = FakeNode.Var("x")
        val y = FakeNode.Var("y")
        val rand = FakeNode.Var("rand", deterministic = false)

        // Double negation
        assertThat(algebra.neg(algebra.neg(x))).isSameInstanceAs(x)

        // Idempotent & SelfZero (deterministic vs non-deterministic)
        assertThat(opt.simplifyBinaryOp(FakeOp.Idem, x, x) { a, _ -> a }).isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOp(FakeOp.Idem, rand, rand) { a, _ -> a })
            .isEqualTo(FakeNode.Binary(FakeOp.Idem, rand, rand))
        assertThat(opt.simplifyBinaryOp(FakeOp.SelfZero, x, x) { _, _ -> 0 })
            .isEqualTo(FakeNode.Const(0))
        assertThat(opt.simplifyBinaryOp(FakeOp.SelfZero, rand, rand) { _, _ -> 0 })
            .isEqualTo(FakeNode.Binary(FakeOp.SelfZero, rand, rand))

        // SignedTerm / TermSign & additive cancellation
        assertThat(TermSign.Positive.flip()).isEqualTo(TermSign.Negative)
        assertThat(TermSign.Negative.flip()).isEqualTo(TermSign.Positive)
        assertThat(algebra.add(x, algebra.sub(y, x))).isSameInstanceAs(y)
        assertThat(algebra.sub(rand, rand)).isEqualTo(FakeNode.Binary(FakeOp.Sub, rand, rand))

        // Equal-branch collapse for unary, LHS constant, RHS constant, and Select op Select
        val symSel = FakeNode.Select("c", FakeNode.Const(-5), FakeNode.Const(5))
        assertThat(opt.simplifyUnaryOp(FakeOp.CustomUnary, symSel) { kotlin.math.abs(it) })
            .isEqualTo(FakeNode.Const(5))

        val sel1 = FakeNode.Select("c", FakeNode.Const(3), FakeNode.Const(7))
        val sel2 = FakeNode.Select("c", FakeNode.Const(7), FakeNode.Const(3))
        assertThat(
                opt.simplifyBinaryOp(FakeOp.CustomBinary, FakeNode.Const(20), sel1) { a, b ->
                    a - b
                }
            )
            .isEqualTo(FakeNode.Select("c", FakeNode.Const(17), FakeNode.Const(13)))
        assertThat(algebra.add(sel1, sel2)).isEqualTo(FakeNode.Const(10))

        // Different or non-deterministic select conditions do NOT merge
        val otherCondSel = FakeNode.Select("d", FakeNode.Const(7), FakeNode.Const(3))
        assertThat(algebra.add(sel1, otherCondSel))
            .isEqualTo(FakeNode.Binary(FakeOp.Add, sel1, otherCondSel))

        val nonDetSel1 =
            FakeNode.Select(
                "randCond",
                FakeNode.Const(3),
                FakeNode.Const(7),
                deterministicCond = false,
            )
        val nonDetSel2 =
            FakeNode.Select(
                "randCond",
                FakeNode.Const(7),
                FakeNode.Const(3),
                deterministicCond = false,
            )
        assertThat(algebra.add(nonDetSel1, nonDetSel2))
            .isEqualTo(FakeNode.Binary(FakeOp.Add, nonDetSel1, nonDetSel2))
    }

    // endregion

    // region FloatPeepholeOptimizer & FloatRpnLowering

    @Test
    fun floatOptimizer_identitiesCanonicalizationAndAstOpNodes() {
        val x = createNamedRemoteFloat("x", 8f)

        assertThat(x + 0f).isSameInstanceAs(x)
        assertThat(x + (-0f)).isSameInstanceAs(x)
        assertThat(0f.rf + x).isSameInstanceAs(x)
        assertThat((-0f).rf + x).isSameInstanceAs(x)
        assertThat(x - 0f).isSameInstanceAs(x)
        assertThat(x - (-0f)).isSameInstanceAs(x)
        assertThat(x * 1f).isSameInstanceAs(x)
        assertThat(1f.rf * x).isSameInstanceAs(x)
        assertThat(x / 1f).isSameInstanceAs(x)
        assertThat((x * 0f).constantValueOrNull).isEqualTo(0f)
        assertThat((0f.rf * x).constantValueOrNull).isEqualTo(0f)
        assertThat((0f.rf / x).constantValueOrNull).isEqualTo(0f)

        val cPlusX = 5f.rf + x
        assertThat(cPlusX).isInstanceOf(BinaryRemoteFloatOp::class.java)
        assertThat(cPlusX).isEqualTo(x + 5f)

        val zeroMinusX = 0f.rf - x
        assertThat(zeroMinusX).isInstanceOf(UnaryRemoteFloatOp::class.java)
        assertThat(zeroMinusX).isEqualTo(-x)

        val cMinusX = 20f.rf - x
        assertThat(cMinusX).isEqualTo((-x) + 20f)
        assertThat(evalFloat(cMinusX)).isEqualTo(12f)

        val cTimesX = 3f.rf * x
        assertThat(cTimesX).isEqualTo(x * 3f)

        val cDivX = 24f.rf / x
        assertThat(cDivX).isInstanceOf(BinaryRemoteFloatOp::class.java)
        assertThat((cDivX as BinaryRemoteFloatOp).left.constantValueOrNull).isEqualTo(24f)
        assertThat(evalFloat(cDivX)).isEqualTo(3f)
    }

    @Test
    fun floatOptimizer_nanConstantEval_fallsBackToRuntimeAstInsteadOfCorruptingArray() {
        val sqrtNeg = sqrt((-1f).rf)
        assertThat(sqrtNeg).isInstanceOf(UnaryRemoteFloatOp::class.java)
        assertThat(sqrtNeg.constantValueOrNull).isNull()
        assertThat(evalFloat(sqrtNeg)).isNaN()

        val remZero = 5f.rf % 0f
        assertThat(remZero).isInstanceOf(BinaryRemoteFloatOp::class.java)
        assertThat(remZero.constantValueOrNull).isNull()
        assertThat(evalFloat(remZero)).isNaN()

        val infMinusInf = Float.POSITIVE_INFINITY.rf - Float.POSITIVE_INFINITY.rf
        assertThat(infMinusInf).isInstanceOf(BinaryRemoteFloatOp::class.java)
        assertThat(infMinusInf.constantValueOrNull).isNull()
        assertThat(evalFloat(infMinusInf)).isNaN()

        val infPlusNegInf = Float.POSITIVE_INFINITY.rf + Float.NEGATIVE_INFINITY.rf
        assertThat(infPlusNegInf).isInstanceOf(BinaryRemoteFloatOp::class.java)
        assertThat(infPlusNegInf.constantValueOrNull).isNull()
        assertThat(evalFloat(infPlusNegInf)).isNaN()
    }

    @Test
    fun floatOptimizer_trailingConstants_allAdditiveAndMulDivMatrixAndGuards() {
        val x = createNamedRemoteFloat("x", 12f)

        assertThat((x + 5f) + 3f).isEqualTo(x + 8f)
        assertThat((x + 5f) - 8f).isEqualTo(x - 3f)
        assertThat((x - 5f) + 8f).isEqualTo(x + 3f)
        assertThat((x - 5f) - 3f).isEqualTo(x - 8f)
        assertThat((x + 5f) - 5f).isSameInstanceAs(x)
        assertThat((x - 5f) + 5f).isSameInstanceAs(x)

        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Add,
                    x + Float.MAX_VALUE,
                    Float.MAX_VALUE,
                )
            )
            .isNull()
        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Sub,
                    x + Float.POSITIVE_INFINITY,
                    Float.POSITIVE_INFINITY,
                )
            )
            .isNull()

        assertThat((x * 3f) * 4f).isEqualTo(x * 12f)
        assertThat((x * 12f) / 4f).isEqualTo(x * 3f)
        assertThat((x / 12f) * 4f).isEqualTo(x / 3f)
        assertThat((x / 3f) / 4f).isEqualTo(x / 12f)
        assertThat((x * 0.5f) * 2f).isSameInstanceAs(x)
        assertThat((x / 0.5f) / 2f).isSameInstanceAs(x)

        val rawZeroMul = BinaryRemoteFloatOp(RemoteFloatOperationKey.Times, x, 0f.rf)
        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                        AlgebraicOpKind.Mul,
                        rawZeroMul,
                        5f,
                    )
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)

        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Div,
                    x * 1e-25f,
                    1e25f,
                )
            )
            .isNull()
        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Mul,
                    x / 1e25f,
                    1e-25f,
                )
            )
            .isNull()
        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Div,
                    x * 2f,
                    0f,
                )
            )
            .isNull()
        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Add,
                    x * 2f,
                    3f,
                )
            )
            .isNull()
    }

    @Test
    fun lowerFloatNegation_absorbsUnsharedMulDivConstant_andPreservesSharedOrGeneralExprs() {
        val x = createNamedRemoteFloat("x", 6f)
        val y = createNamedRemoteFloat("y", 2f)

        val negMul = -(x * 3f)
        assertThat(negMul).isInstanceOf(UnaryRemoteFloatOp::class.java)
        val cs1 = testCreationState()
        val arrMul = negMul.arrayForCreationState(cs1)
        assertThat(arrMul).hasLength(3)
        assertThat(arrMul[1]).isEqualTo(-3f)
        assertThat(arrMul[2].toRawBits()).isEqualTo(AnimatedFloatExpression.MUL.toRawBits())
        assertThat(evalFloat(negMul)).isEqualTo(-18f)

        val negDiv = -(x / 2f)
        val cs2 = testCreationState()
        val arrDiv = negDiv.arrayForCreationState(cs2)
        assertThat(arrDiv).hasLength(3)
        assertThat(arrDiv[1]).isEqualTo(-2f)
        assertThat(arrDiv[2].toRawBits()).isEqualTo(AnimatedFloatExpression.DIV.toRawBits())
        assertThat(evalFloat(negDiv)).isEqualTo(-3f)

        val cs3 = testCreationState()
        val xArr = x.arrayForCreationState(cs3)
        assertThat(lowerFloatNegation(cs3, x * (-1f)).map { it.toRawBits() })
            .isEqualTo(xArr.map { it.toRawBits() })
        assertThat(lowerFloatNegation(cs3, x / (-1f)).map { it.toRawBits() })
            .isEqualTo(xArr.map { it.toRawBits() })

        val shared = x * 3f
        val cs4 = testCreationState()
        cs4.getId(shared)
        val sharedNegArr = lowerFloatNegation(cs4, shared)
        assertThat(sharedNegArr).hasLength(3)
        assertThat(sharedNegArr[1]).isEqualTo(-1f)
        assertThat(sharedNegArr[2].toRawBits()).isEqualTo(AnimatedFloatExpression.MUL.toRawBits())

        val cs5 = testCreationState()
        val dynNegArr = (-(x * y)).arrayForCreationState(cs5)
        assertThat(dynNegArr).hasLength(5)
        assertThat(dynNegArr[3]).isEqualTo(-1f)
        assertThat(evalFloat(-(x * y))).isEqualTo(-12f)
    }

    @Test
    fun floatOptimizer_selectLifting_unaryLhsRhsSameCondAndNonDeterministicGuard() {
        val x = createNamedRemoteFloat("x", 5f)
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)

        val boolSel = ((flag.select(10f.rf, 20f.rf) + 4f) * 0.5f) - 2f
        assertThat(boolSel).isEqualTo(flag.select(5f.rf, 10f.rf))
        assertThat(boolSel.cacheKey).isEqualTo(flag.select(5f.rf, 10f.rf).cacheKey)
        assertThat(evalFloat(boolSel, boolOverrides = mapOf("flag" to true))).isEqualTo(5f)
        assertThat(evalFloat(boolSel, boolOverrides = mapOf("flag" to false))).isEqualTo(10f)

        val cmpSel = -(selectIfLt(x, 10f.rf, 4f.rf, 8f.rf) * 3f)
        assertThat(cmpSel).isEqualTo(selectIfLt(x, 10f.rf, (-12f).rf, (-24f).rf))
        assertThat(evalFloat(cmpSel, overrides = mapOf("x" to 5f))).isEqualTo(-12f)
        assertThat(evalFloat(cmpSel, overrides = mapOf("x" to 15f))).isEqualTo(-24f)

        // Unary abs collapses symmetric branches to constant, but preserves -0.0f vs +0.0f raw bits
        assertThat(abs(flag.select((-7f).rf, 7f.rf)).constantValueOrNull).isEqualTo(7f)
        assertThat((-flag.select(0f.rf, (-0f).rf)).constantValueOrNull).isNull()

        // Non-commutative LHS constant lifting: `100f / sel` and `pow(2f, sel)`
        val lhsDiv = 100f.rf / flag.select(4f.rf, 20f.rf)
        assertThat(lhsDiv).isEqualTo(flag.select(25f.rf, 5f.rf))
        assertThat(pow(2f.rf, flag.select(3f.rf, 4f.rf))).isEqualTo(flag.select(8f.rf, 16f.rf))

        // Non-deterministic condition (`rand() < 0.5f`) MUST NOT merge two independent selects
        val randSel1 = selectIfLt(rand(), 0.5f.rf, 1f.rf, 2f.rf)
        val randSel2 = selectIfLt(rand(), 0.5f.rf, 2f.rf, 1f.rf)
        val randSum = randSel1 + randSel2
        assertThat(randSum.constantValueOrNull).isNull()
        assertThat(randSum).isInstanceOf(BinaryRemoteFloatOp::class.java)
    }

    // endregion

    // region IntPeepholeOptimizer & IntRpnLowering

    @Test
    fun intOptimizer_identitiesCanonicalizationAndAstOpNodes() {
        val x = createNamedRemoteInt("x", 8)

        assertThat(x + 0).isSameInstanceAs(x)
        assertThat(0.ri + x).isSameInstanceAs(x)
        assertThat(x - 0).isSameInstanceAs(x)
        assertThat(x * 1).isSameInstanceAs(x)
        assertThat(1.ri * x).isSameInstanceAs(x)
        assertThat(x / 1).isSameInstanceAs(x)
        assertThat((x * 0).constantValueOrNull).isEqualTo(0)
        assertThat((0.ri * x).constantValueOrNull).isEqualTo(0)
        assertThat((0.ri / x).constantValueOrNull).isEqualTo(0)

        val cPlusX = 5.ri + x
        assertThat(cPlusX).isInstanceOf(BinaryRemoteIntOp::class.java)
        assertThat(cPlusX).isEqualTo(x + 5)

        val zeroMinusX = 0.ri - x
        assertThat(zeroMinusX).isInstanceOf(UnaryRemoteIntOp::class.java)
        assertThat(zeroMinusX).isEqualTo(-x)

        val minMinusX = Int.MIN_VALUE.ri - x
        assertThat(minMinusX).isEqualTo((-x) + Int.MIN_VALUE)
        assertThat(evalInt(minMinusX)).isEqualTo(Int.MIN_VALUE - 8)

        val andZero = x and 0.ri
        assertThat(andZero.constantValueOrNull).isNull()
        assertThat(andZero.toString()).isEqualTo("user:x and 0")
    }

    @Test
    fun intOptimizer_trailingConstants_allAdditiveAndMulDivMatrixAndWraparound() {
        val x = createNamedRemoteInt("x", 36)

        assertThat((x + 10) + 5).isEqualTo(x + 15)
        assertThat((x + 10) - 15).isEqualTo(x - 5)
        assertThat((x - 10) + 15).isEqualTo(x + 5)
        assertThat((x - 10) - 5).isEqualTo(x - 15)
        assertThat((x + 10) - 10).isSameInstanceAs(x)
        assertThat((x - 10) + 10).isSameInstanceAs(x)
        assertThat((x + Int.MAX_VALUE) + 1).isEqualTo(x + Int.MIN_VALUE)
        assertThat((x + Int.MIN_VALUE) + Int.MIN_VALUE).isSameInstanceAs(x)

        assertThat((x * 3) * 4).isEqualTo(x * 12)
        assertThat((x * -1) * -1).isSameInstanceAs(x)
        assertThat((x * 12) / 4).isEqualTo(x * 3)
        assertThat((x * 4) / 4).isSameInstanceAs(x)
        assertThat((x / 3) / 4).isEqualTo(x / 12)
        assertThat((x / -1) / -1).isSameInstanceAs(x)

        val exactMinDiv = (x / -65536) / 32768
        assertThat(exactMinDiv).isEqualTo(x / Int.MIN_VALUE)
        assertThat(evalInt(exactMinDiv, overrides = mapOf("x" to Int.MIN_VALUE))).isEqualTo(1)
        assertThat(evalInt(exactMinDiv, overrides = mapOf("x" to Int.MAX_VALUE))).isEqualTo(0)

        assertThat(IntPeepholeOptimizer.tryFoldTrailingConstant(AlgebraicOpKind.Div, x * 10, 3))
            .isNull()
        assertThat(
                IntPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Div,
                    x * Int.MIN_VALUE,
                    -1,
                )
            )
            .isNull()
        assertThat(IntPeepholeOptimizer.tryFoldTrailingConstant(AlgebraicOpKind.Mul, x / 2, 2))
            .isNull()
        assertThat(IntPeepholeOptimizer.tryFoldTrailingConstant(AlgebraicOpKind.Div, x * 4, 0))
            .isNull()
        assertThat(IntPeepholeOptimizer.tryFoldTrailingConstant(AlgebraicOpKind.Div, x / 4, 0))
            .isNull()
    }

    @Test
    fun intOptimizer_selectLifting_unaryLhsRhsSameCondAndNonDeterministicGuard() {
        val x = createNamedRemoteInt("x", 5)
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)

        val sel = ((flag.select(10.ri, 20.ri) + 6) * 2) / 4
        assertThat(sel).isEqualTo(flag.select(8.ri, 13.ri))
        assertThat(sel.cacheKey).isEqualTo(flag.select(8.ri, 13.ri).cacheKey)
        assertThat(evalInt(sel, boolOverrides = mapOf("flag" to true))).isEqualTo(8)
        assertThat(evalInt(sel, boolOverrides = mapOf("flag" to false))).isEqualTo(13)

        val cmpSel = selectIfGe(x, 10.ri, 100.ri, 200.ri) - 25
        assertThat(cmpSel).isEqualTo(selectIfGe(x, 10.ri, 75.ri, 175.ri))
        assertThat(evalInt(cmpSel, overrides = mapOf("x" to 5))).isEqualTo(175)
        assertThat(evalInt(cmpSel, overrides = mapOf("x" to 12))).isEqualTo(75)

        // Unary `inv()`, `absoluteValue`, and non-commutative `100 / sel`, `100 % sel`, `1 shl sel`
        assertThat(flag.select(0.ri, (-1).ri).inv()).isEqualTo(flag.select((-1).ri, 0.ri))
        assertThat(flag.select((-9).ri, 9.ri).absoluteValue.constantValueOrNull).isEqualTo(9)
        assertThat(100.ri / flag.select(4.ri, 25.ri)).isEqualTo(flag.select(25.ri, 4.ri))
        assertThat(100.ri % flag.select(7.ri, 9.ri)).isEqualTo(flag.select(2.ri, 1.ri))
        assertThat(1.ri shl flag.select(3.ri, 5.ri)).isEqualTo(flag.select(8.ri, 32.ri))

        // Divide/modulo by zero on either LHS or RHS select branch leaves binary op intact
        val selDivZero = flag.select(10.ri, 20.ri) / 0
        val lhsDivZeroBranch = 100.ri / flag.select(0.ri, 5.ri)
        assertThat(selDivZero).isInstanceOf(BinaryRemoteIntOp::class.java)
        assertThat(lhsDivZeroBranch).isInstanceOf(BinaryRemoteIntOp::class.java)
        assertThat(evalInt(selDivZero)).isEqualTo(0)
        assertThat(evalInt(lhsDivZeroBranch, boolOverrides = mapOf("flag" to true))).isEqualTo(0)
        assertThat(evalInt(lhsDivZeroBranch, boolOverrides = mapOf("flag" to false))).isEqualTo(20)

        // Non-deterministic int select condition (`(rand() * 10f).toRemoteInt() < 5`) does NOT
        // merge
        val randIntCond1 = selectIfLt((rand() * 10f).toRemoteInt(), 5.ri, 1.ri, 2.ri)
        val randIntCond2 = selectIfLt((rand() * 10f).toRemoteInt(), 5.ri, 2.ri, 1.ri)
        val randIntSum = randIntCond1 + randIntCond2
        assertThat(randIntSum.constantValueOrNull).isNull()
        assertThat(randIntSum).isInstanceOf(BinaryRemoteIntOp::class.java)

        // CopySign with negative operand/branches must NOT lift across `c op sel`, `sel op c`, or
        // `sel op sel` via host `withSign`, preserving player OP_COPY_SIGN bit-flip semantics.
        val copySignLhsConst = copySign((-5).ri, flag.select(0.ri, 1.ri))
        assertThat(copySignLhsConst.constantValueOrNull).isNull()
        assertThat(copySignLhsConst).isInstanceOf(BinaryRemoteIntOp::class.java)
        assertThat(evalInt(copySignLhsConst, boolOverrides = mapOf("flag" to true))).isEqualTo(-5)
        assertThat(evalInt(copySignLhsConst, boolOverrides = mapOf("flag" to false))).isEqualTo(-5)

        val copySignRhsConst = copySign(flag.select((-5).ri, (-3).ri), 1.ri)
        assertThat(copySignRhsConst).isInstanceOf(BinaryRemoteIntOp::class.java)
        assertThat(evalInt(copySignRhsConst, boolOverrides = mapOf("flag" to true))).isEqualTo(-5)
        assertThat(evalInt(copySignRhsConst, boolOverrides = mapOf("flag" to false))).isEqualTo(-3)

        val copySignBothSel = copySign(flag.select((-5).ri, 5.ri), flag.select(1.ri, (-1).ri))
        assertThat(copySignBothSel).isInstanceOf(BinaryRemoteIntOp::class.java)
        assertThat(evalInt(copySignBothSel, boolOverrides = mapOf("flag" to true))).isEqualTo(-5)
        assertThat(evalInt(copySignBothSel, boolOverrides = mapOf("flag" to false))).isEqualTo(-5)
    }

    @Test
    fun genericEngine_primitiveConstOverloads_avoidEagerWrapperAllocations() {
        val algebra = FakeIntAlgebra()
        val opt = algebra.optimizer
        val x = FakeNode.Var("x")
        val sel = FakeNode.Select("c", FakeNode.Const(10), FakeNode.Const(20))

        algebra.wrapConstantCalls = 0
        assertThat(opt.simplifyBinaryOpRightConst(FakeOp.Add, x, 0) { a, b -> a + b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOpLeftConst(FakeOp.Add, 0, x) { a, b -> a + b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOpRightConst(FakeOp.Sub, x, 0) { a, b -> a - b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOpLeftConst(FakeOp.Sub, 0, x) { a, b -> a - b })
            .isEqualTo(FakeNode.Unary(FakeOp.Neg, x))
        assertThat(opt.simplifyBinaryOpRightConst(FakeOp.Mul, x, 1) { a, b -> a * b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOpLeftConst(FakeOp.Mul, 1, x) { a, b -> a * b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOpRightConst(FakeOp.Div, x, 1) { a, b -> a / b })
            .isSameInstanceAs(x)
        assertThat(opt.simplifyBinaryOpRightConst(FakeOp.Add, sel, 5) { a, b -> a + b })
            .isEqualTo(FakeNode.Select("c", FakeNode.Const(15), FakeNode.Const(25)))
        assertThat(opt.simplifyBinaryOpLeftConst(FakeOp.Sub, 30, sel) { a, b -> a - b })
            .isEqualTo(FakeNode.Select("c", FakeNode.Const(20), FakeNode.Const(10)))
        assertThat(algebra.wrapConstantCalls).isEqualTo(0)

        val xPlus5 = opt.simplifyBinaryOpRightConst(FakeOp.Add, x, 5) { a, b -> a + b }
        assertThat(algebra.wrapConstantCalls).isEqualTo(1)
        algebra.wrapConstantCalls = 0
        assertThat(opt.simplifyBinaryOpRightConst(FakeOp.Sub, xPlus5, 5) { a, b -> a - b })
            .isSameInstanceAs(x)
        assertThat(algebra.wrapConstantCalls).isEqualTo(0)

        algebra.wrapConstantCalls = 0
        assertThat(opt.simplifyBinaryOpRightConst(FakeOp.Add, xPlus5, 3) { a, b -> a + b })
            .isEqualTo(FakeNode.Binary(FakeOp.Add, x, FakeNode.Const(8)))
        assertThat(algebra.wrapConstantCalls).isEqualTo(1)

        algebra.wrapConstantCalls = 0
        assertThat(opt.simplifyBinaryOpLeftConst(FakeOp.Add, 2, xPlus5) { a, b -> a + b })
            .isEqualTo(FakeNode.Binary(FakeOp.Add, x, FakeNode.Const(7)))
        assertThat(algebra.wrapConstantCalls).isEqualTo(1)
    }

    @Test
    fun floatOptimizer_subnormalConstantUnderflow_doesNotReassociate_andNaNPrimitivesWork() {
        val x = createNamedRemoteFloat("x", 1e38f)

        val subnormalMul = (x * 1e-22f) * 1e-22f
        assertThat(subnormalMul.toString()).isEqualTo("user:x * 1.0E-22 * 1.0E-22")
        assertThat(evalFloat(subnormalMul)).isEqualTo((1e38f * 1e-22f) * 1e-22f)

        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Div,
                    x * 1e-22f,
                    1e22f,
                )
            )
            .isNull()
        assertThat(
                FloatPeepholeOptimizer.tryFoldTrailingConstant(
                    AlgebraicOpKind.Mul,
                    x / 1e22f,
                    1e-22f,
                )
            )
            .isNull()

        val nanLeft = max(Float.NaN, x)
        val nanRight = x + Float.NaN
        assertThat(nanLeft).isInstanceOf(BinaryRemoteFloatOp::class.java)
        assertThat(nanRight).isInstanceOf(BinaryRemoteFloatOp::class.java)
        assertThat(nanLeft.constantValueOrNull).isNull()
        assertThat(nanRight.constantValueOrNull).isNull()

        val winWidthNan =
            androidx.compose.remote.document.NanEncoding.asNan(
                androidx.compose.remote.core.RemoteContext.ID_WINDOW_WIDTH
            )
        assertThat((x + winWidthNan).toString()).isEqualTo("user:x + context:window_width")
        assertThat(max(winWidthNan, x).toString()).isEqualTo("max(context:window_width, user:x)")
    }

    // endregion
}
