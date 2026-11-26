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

import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.document.NanEncoding
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AdditivePeepholeTest {
    private val xVal = 7f
    private val yVal = 19f
    private val zVal = 43f

    private val xiVal = 7
    private val yiVal = 19
    private val ziVal = 43

    private fun evalFloat(expr: RemoteFloat): Float {
        expr.constantValueOrNull?.let {
            return it
        }
        val creationState = testCreationState()
        val context = TestRemoteContext()
        val id = creationState.getId(expr)
        context.loadDocument(creationState)
        return context.getFloat(id)
    }

    private fun evalInt(expr: RemoteInt): Int {
        expr.constantValueOrNull?.let {
            return it
        }
        val creationState = testCreationState()
        val context = TestRemoteContext()
        val id = creationState.getId(expr)
        context.loadDocument(creationState)
        return context.getInteger(id)
    }

    private fun trySimplifyMatchingOppositeSignTerms(
        a: RemoteFloat,
        b: RemoteFloat,
        subtractB: Boolean,
    ): RemoteFloat? = FloatPeepholeOptimizer.trySimplifyMatchingOppositeSignTerms(a, b, subtractB)

    private fun trySimplifyMatchingOppositeSignTerms(
        a: RemoteInt,
        b: RemoteInt,
        subtractB: Boolean,
    ): RemoteInt? = IntPeepholeOptimizer.trySimplifyMatchingOppositeSignTerms(a, b, subtractB)

    private fun foldAdditiveConstFloat(
        a: RemoteFloat,
        b: Float,
        op: RemoteFloatOperationKey,
    ): RemoteFloat? {
        if (!b.isFinite()) return null
        val kind = FloatAlgebra.classifyBinaryOp(op)
        if (kind != AlgebraicOpKind.Add && kind != AlgebraicOpKind.Sub) return null
        return FloatPeepholeOptimizer.tryFoldTrailingConstant(kind, a, b)
    }

    private fun foldAdditiveConstInt(
        a: RemoteInt,
        b: Int,
        op: RemoteIntOperationKey,
    ): RemoteInt? {
        val kind = IntAlgebra.classifyBinaryOp(op)
        if (kind != AlgebraicOpKind.Add && kind != AlgebraicOpKind.Sub) return null
        return IntPeepholeOptimizer.tryFoldTrailingConstant(kind, a, b)
    }

    private fun toTermSign(sign: Int): TermSign =
        if (sign > 0) TermSign.Positive else TermSign.Negative

    private fun sumSignedFloat(
        a: RemoteFloat,
        aSign: Int,
        b: RemoteFloat,
        bSign: Int,
    ): RemoteFloat =
        FloatPeepholeOptimizer.sumSigned(
            SignedTerm(a, toTermSign(aSign)),
            SignedTerm(b, toTermSign(bSign)),
        )

    private fun sumSignedInt(a: RemoteInt, aSign: Int, b: RemoteInt, bSign: Int): RemoteInt =
        IntPeepholeOptimizer.sumSigned(
            SignedTerm(a, toTermSign(aSign)),
            SignedTerm(b, toTermSign(bSign)),
        )

    private fun sumsToZero(a: RemoteFloat, aSign: Int, b: RemoteFloat, bSign: Int): Boolean =
        FloatPeepholeOptimizer.sumsToZero(
            SignedTerm(a, toTermSign(aSign)),
            SignedTerm(b, toTermSign(bSign)),
        )

    private fun sumsToZero(a: RemoteInt, aSign: Int, b: RemoteInt, bSign: Int): Boolean =
        IntPeepholeOptimizer.sumsToZero(
            SignedTerm(a, toTermSign(aSign)),
            SignedTerm(b, toTermSign(bSign)),
        )

    // region trySimplifyMatchingOppositeSignTerms (Float)

    @Test
    fun trySimplifyMatchingOppositeSignTerms_float_direct_allSignCombinations() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)

        // Opposite effective signs cancel to 0f.
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x, x, subtractB = true)?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x, -x, subtractB = false)?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(-x, x, subtractB = false)?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(-x, -x, subtractB = true)?.constantValueOrNull
            )
            .isEqualTo(0f)

        // Same effective signs do not cancel.
        assertThat(trySimplifyMatchingOppositeSignTerms(x, x, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(-x, -x, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x, -x, subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(-x, x, subtractB = true)).isNull()

        // Distinct variables do not cancel.
        assertThat(trySimplifyMatchingOppositeSignTerms(x, y, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x, y, subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x, -y, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(-x, y, subtractB = true)).isNull()

        // Compound deterministic expressions cancel directly.
        val scaled = x * 2f
        assertThat(
                trySimplifyMatchingOppositeSignTerms(scaled, scaled, subtractB = true)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(scaled, -scaled, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
    }

    @Test
    fun trySimplifyMatchingOppositeSignTerms_float_direct_constantsAndNonFinite() {
        assertThat(
                trySimplifyMatchingOppositeSignTerms(5f.rf, 5f.rf, subtractB = true)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(5f.rf, (-5f).rf, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms((-5f).rf, 5f.rf, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms((-5f).rf, (-5f).rf, subtractB = true)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(0f.rf, 0f.rf, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(0f.rf, 0f.rf, subtractB = true)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)

        assertThat(trySimplifyMatchingOppositeSignTerms(5f.rf, 5f.rf, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(5f.rf, 6f.rf, subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(5f.rf, (-6f).rf, subtractB = false))
            .isNull()

        // Non-finite constants must never cancel into 0f.
        val posInf = Float.POSITIVE_INFINITY.rf
        val negInf = Float.NEGATIVE_INFINITY.rf
        assertThat(trySimplifyMatchingOppositeSignTerms(posInf, posInf, subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(posInf, negInf, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(negInf, posInf, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(negInf, negInf, subtractB = true)).isNull()
    }

    @Test
    fun trySimplifyMatchingOppositeSignTerms_float_rightNested_allCombinations() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)
        val signs = listOf(1, -1)

        // Exhaustively test a +/- (b0 +/- b1) where a matches b0 or b1 (variable or constant peer).
        for (aSign in signs) {
            val a = if (aSign > 0) x else -x
            val aEval = aSign * xVal

            for (subtractB in listOf(false, true)) {
                val bSign = if (subtractB) -1 else 1

                for (bMinus in listOf(false, true)) {
                    val b1OpSign = if (bMinus) -1 else 1

                    for (b0Sign in signs) {
                        for (b1Sign in signs) {
                            // Case 1: b0 is +/-x, b1 is +/-y
                            val b0x = if (b0Sign > 0) x else -x
                            val b1y = if (b1Sign > 0) y else -y
                            val bXY = if (bMinus) b0x - b1y else b0x + b1y
                            val resultXY = trySimplifyMatchingOppositeSignTerms(a, bXY, subtractB)
                            val cancelsB0 = aSign == -(bSign * b0Sign)
                            if (cancelsB0) {
                                assertThat(resultXY).isNotNull()
                                val expected =
                                    aEval + bSign * (b0Sign * xVal + b1OpSign * b1Sign * yVal)
                                assertThat(evalFloat(resultXY!!)).isEqualTo(expected)
                            } else {
                                assertThat(resultXY).isNull()
                            }

                            // Case 2: b0 is +/-y, b1 is +/-x
                            val b0y = if (b0Sign > 0) y else -y
                            val b1x = if (b1Sign > 0) x else -x
                            val bYX = if (bMinus) b0y - b1x else b0y + b1x
                            val resultYX = trySimplifyMatchingOppositeSignTerms(a, bYX, subtractB)
                            val cancelsB1 = aSign == -(bSign * b1OpSign * b1Sign)
                            if (cancelsB1) {
                                assertThat(resultYX).isNotNull()
                                val expected =
                                    aEval + bSign * (b0Sign * yVal + b1OpSign * b1Sign * xVal)
                                assertThat(evalFloat(resultYX!!)).isEqualTo(expected)
                            } else {
                                assertThat(resultYX).isNull()
                            }

                            // Case 3: b0 is +/-x, b1 is constant +/-44f (covers 44f - x shape)
                            val b1c = (b1Sign * 44f).rf
                            val bXC = if (bMinus) b0x - b1c else b0x + b1c
                            val resultXC = trySimplifyMatchingOppositeSignTerms(a, bXC, subtractB)
                            if (cancelsB0) {
                                val expectedConst = bSign * b1OpSign * b1Sign * 44f
                                assertThat(resultXC?.constantValueOrNull).isEqualTo(expectedConst)
                            } else {
                                assertThat(resultXC).isNull()
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun trySimplifyMatchingOppositeSignTerms_float_leftNested_allCombinations() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)
        val signs = listOf(1, -1)

        // Exhaustively test (a0 +/- a1) +/- b where b matches a0 or a1 (variable or constant peer).
        for (bTermSign in signs) {
            val b = if (bTermSign > 0) x else -x

            for (subtractB in listOf(false, true)) {
                val bSign = if (subtractB) -1 else 1
                val bEffSign = bSign * bTermSign
                val bEval = bEffSign * xVal

                for (aMinus in listOf(false, true)) {
                    val a1OpSign = if (aMinus) -1 else 1

                    for (a0Sign in signs) {
                        for (a1Sign in signs) {
                            // Case 1: a0 is +/-x, a1 is +/-y
                            val a0x = if (a0Sign > 0) x else -x
                            val a1y = if (a1Sign > 0) y else -y
                            val aXY = if (aMinus) a0x - a1y else a0x + a1y
                            val resultXY = trySimplifyMatchingOppositeSignTerms(aXY, b, subtractB)
                            val cancelsA0 = a0Sign == -bEffSign
                            if (cancelsA0) {
                                assertThat(resultXY).isNotNull()
                                val expected = a0Sign * xVal + a1OpSign * a1Sign * yVal + bEval
                                assertThat(evalFloat(resultXY!!)).isEqualTo(expected)
                            } else {
                                assertThat(resultXY).isNull()
                            }

                            // Case 2: a0 is +/-y, a1 is +/-x
                            val a0y = if (a0Sign > 0) y else -y
                            val a1x = if (a1Sign > 0) x else -x
                            val aYX = if (aMinus) a0y - a1x else a0y + a1x
                            val resultYX = trySimplifyMatchingOppositeSignTerms(aYX, b, subtractB)
                            val cancelsA1 = (a1OpSign * a1Sign) == -bEffSign
                            if (cancelsA1) {
                                assertThat(resultYX).isNotNull()
                                val expected = a0Sign * yVal + a1OpSign * a1Sign * xVal + bEval
                                assertThat(evalFloat(resultYX!!)).isEqualTo(expected)
                            } else {
                                assertThat(resultYX).isNull()
                            }

                            // Case 3: a0 is +/-x, a1 is constant +/-44f (covers (44f - x) + x)
                            val a1c = (a1Sign * 44f).rf
                            val aXC = if (aMinus) a0x - a1c else a0x + a1c
                            val resultXC = trySimplifyMatchingOppositeSignTerms(aXC, b, subtractB)
                            if (cancelsA0) {
                                val expectedConst = a1OpSign * a1Sign * 44f
                                assertThat(resultXC?.constantValueOrNull).isEqualTo(expectedConst)
                            } else {
                                assertThat(resultXC).isNull()
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun trySimplifyMatchingOppositeSignTerms_float_crossNested_allSlotsAndSumSignedBranches() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)
        val z = createNamedRemoteFloat("z", zVal)
        val signs = listOf(1, -1)

        // Exhaustively test all 4 cross slots (a0/b0, a0/b1, a1/b0, a1/b1) x all +/- ops x
        // subtractB.
        for (aMinus in listOf(false, true)) {
            val a1OpSign = if (aMinus) -1 else 1
            for (bMinus in listOf(false, true)) {
                val b1OpSign = if (bMinus) -1 else 1
                for (subtractB in listOf(false, true)) {
                    val bSign = if (subtractB) -1 else 1

                    for (a0Sign in signs) {
                        for (b0Sign in signs) {
                            // Slot (a0, b0): a = (+/-x +/- y), b = (+/-x +/- z)
                            val a =
                                (if (a0Sign > 0) x else -x).let { l ->
                                    if (aMinus) l - y else l + y
                                }
                            val b =
                                (if (b0Sign > 0) x else -x).let { l ->
                                    if (bMinus) l - z else l + z
                                }
                            val res = trySimplifyMatchingOppositeSignTerms(a, b, subtractB)
                            if (a0Sign == -(bSign * b0Sign)) {
                                assertThat(res).isNotNull()
                                val expected = a1OpSign * yVal + bSign * b1OpSign * zVal
                                assertThat(evalFloat(res!!)).isEqualTo(expected)
                            } else {
                                assertThat(res).isNull()
                            }

                            // Slot (a0, b1): a = (+/-x +/- y), b = (z +/- (+/-x))
                            val bSwap =
                                (if (b0Sign > 0) x else -x).let { r ->
                                    if (bMinus) z - r else z + r
                                }
                            val resSwap = trySimplifyMatchingOppositeSignTerms(a, bSwap, subtractB)
                            if (a0Sign == -(bSign * b1OpSign * b0Sign)) {
                                assertThat(resSwap).isNotNull()
                                val expected = a1OpSign * yVal + bSign * zVal
                                assertThat(evalFloat(resSwap!!)).isEqualTo(expected)
                            } else {
                                assertThat(resSwap).isNull()
                            }
                        }
                    }

                    // Slot (a1, b0): a = (y +/- x), b = (x +/- z)
                    val a1b0A = if (aMinus) y - x else y + x
                    val a1b0B = if (bMinus) x - z else x + z
                    val a1b0Res = trySimplifyMatchingOppositeSignTerms(a1b0A, a1b0B, subtractB)
                    if (a1OpSign == -bSign) {
                        assertThat(a1b0Res).isNotNull()
                        assertThat(evalFloat(a1b0Res!!)).isEqualTo(yVal + bSign * b1OpSign * zVal)
                    } else {
                        assertThat(a1b0Res).isNull()
                    }

                    // Slot (a1, b1): a = (y +/- x), b = (z +/- x)
                    val a1b1B = if (bMinus) z - x else z + x
                    val a1b1Res = trySimplifyMatchingOppositeSignTerms(a1b0A, a1b1B, subtractB)
                    if (a1OpSign == -(bSign * b1OpSign)) {
                        assertThat(a1b1Res).isNotNull()
                        assertThat(evalFloat(a1b1Res!!)).isEqualTo(yVal + bSign * zVal)
                    } else {
                        assertThat(a1b1Res).isNull()
                    }
                }
            }
        }

        // Explicitly verify all 4 sign branches of sumSignedFloat (++, +-, -+, --) and double
        // cancel.
        assertThat(trySimplifyMatchingOppositeSignTerms(y + x, z - x, subtractB = false))
            .isEqualTo(y + z)
        assertThat(trySimplifyMatchingOppositeSignTerms(y + x, x - z, subtractB = true))
            .isEqualTo(y + z)
        assertThat(trySimplifyMatchingOppositeSignTerms(y + x, z + x, subtractB = true))
            .isEqualTo(y - z)
        assertThat(trySimplifyMatchingOppositeSignTerms(x - y, z - x, subtractB = false))
            .isEqualTo(z - y)
        assertThat(trySimplifyMatchingOppositeSignTerms((-x) - y, (-x) + z, subtractB = true))
            .isEqualTo((-y) - z)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x + y, y + x, subtractB = true)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms((-x) - y, y + x, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
    }

    @Test
    fun trySimplifyMatchingOppositeSignTerms_float_crossNested_signedConstants() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)

        assertThat(trySimplifyMatchingOppositeSignTerms(x + 5f, y - 5f, subtractB = false))
            .isEqualTo(x + y)
        assertThat(trySimplifyMatchingOppositeSignTerms(x + (-5f), y + 5f, subtractB = false))
            .isEqualTo(x + y)
        assertThat(trySimplifyMatchingOppositeSignTerms(x - (-5f), y - 5f, subtractB = false))
            .isEqualTo(x + y)
        assertThat(trySimplifyMatchingOppositeSignTerms(x + 5f, y + 5f, subtractB = true))
            .isEqualTo(x - y)
        assertThat(trySimplifyMatchingOppositeSignTerms(x - 5f, y - 5f, subtractB = true))
            .isEqualTo(x - y)
        assertThat(trySimplifyMatchingOppositeSignTerms(x + 5f, y - (-5f), subtractB = true))
            .isEqualTo(x - y)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x + 8f, 44f.rf - x, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(52f)

        // Same-sign constants do not cancel.
        assertThat(trySimplifyMatchingOppositeSignTerms(x + 5f, y + 5f, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x - 5f, y - 5f, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x + 5f, y - 5f, subtractB = true)).isNull()
    }

    @Test
    fun trySimplifyMatchingOppositeSignTerms_float_guardsAndEdgeCases() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)

        // Non-deterministic rand / randRange must not cancel at any nesting level.
        assertThat(trySimplifyMatchingOppositeSignTerms(rand(), rand(), subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(rand(), 10f.rf - rand(), subtractB = false))
            .isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(10f.rf - rand(), rand(), subtractB = false))
            .isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x + rand(), y - rand(), subtractB = false))
            .isNull()
        assertThat(
                trySimplifyMatchingOppositeSignTerms(
                    randRange(0f.rf, 1f.rf),
                    randRange(0f.rf, 1f.rf),
                    subtractB = true,
                )
            )
            .isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(sin(rand()), sin(rand()), subtractB = true))
            .isNull()

        // Non-finite constants inside operands must not cancel.
        val inf = Float.POSITIVE_INFINITY
        assertThat(trySimplifyMatchingOppositeSignTerms(x + inf, x + inf, subtractB = true))
            .isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x, inf.rf - x, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x + inf, x, subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x + inf, y - inf, subtractB = false))
            .isNull()

        // Non-finite select branches (literal or overflowed) must not cancel.
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)
        val infSel = flag.select(inf.rf, 10f.rf)
        assertThat(trySimplifyMatchingOppositeSignTerms(infSel, infSel, subtractB = true)).isNull()
        val baseMaxSel = flag.select(Float.MAX_VALUE.rf, 10f.rf)
        val overflowSel = baseMaxSel + Float.MAX_VALUE
        assertThat(trySimplifyMatchingOppositeSignTerms(overflowSel, overflowSel, subtractB = true))
            .isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(overflowSel, baseMaxSel, subtractB = true))
            .isNull()

        // Overflowed select wrapped in another operation must still block cancellation of the
        // non-finite subexpression (though cancelling an outer finite peer like `+ x - x` may
        // still simplify `(overflowSel + x) - (overflowSel + x)` to `overflowSel - overflowSel`,
        // which preserves `NaN` on the overflowed branch rather than collapsing to `0f`).
        val wrappedOverflowAdd = overflowSel + x
        val wrappedOverflowMul = overflowSel * x
        val wrappedOverflowSin = sin(overflowSel)
        assertThat(wrappedOverflowAdd.cacheKey.isDeterministic(requireFiniteFloats = true))
            .isFalse()
        assertThat(wrappedOverflowMul.cacheKey.isDeterministic(requireFiniteFloats = true))
            .isFalse()
        assertThat(wrappedOverflowSin.cacheKey.isDeterministic(requireFiniteFloats = true))
            .isFalse()
        val simplifiedWrappedAdd =
            trySimplifyMatchingOppositeSignTerms(
                wrappedOverflowAdd,
                wrappedOverflowAdd,
                subtractB = true,
            )
        assertThat(simplifiedWrappedAdd).isEqualTo(overflowSel - overflowSel)
        assertThat(evalFloat(simplifiedWrappedAdd!!)).isNaN()
        assertThat(
                trySimplifyMatchingOppositeSignTerms(
                    wrappedOverflowMul,
                    wrappedOverflowMul,
                    subtractB = true,
                )
            )
            .isNull()
        assertThat(
                trySimplifyMatchingOppositeSignTerms(
                    wrappedOverflowSin,
                    wrappedOverflowSin,
                    subtractB = true,
                )
            )
            .isNull()
        assertThat(
                trySimplifyMatchingOppositeSignTerms(
                    y + wrappedOverflowAdd,
                    wrappedOverflowAdd,
                    subtractB = true,
                )
            )
            .isNull()

        // MutableRemoteFloat cancels only with itself, never with a distinct instance.
        val m1 = MutableRemoteFloat(5f)
        val m2 = MutableRemoteFloat(5f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(m1, m1, subtractB = true)?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(m1, 44f.rf - m1, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(44f)
        assertThat(trySimplifyMatchingOppositeSignTerms(m1, m2, subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(m1, 44f.rf - m2, subtractB = false))
            .isNull()

        // Malformed / unextractable RemoteOperationCacheKeys safely return null.
        val malformedPlus =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteOperationCacheKey.create(RemoteFloatOperationKey.Plus, x),
            ) {
                floatArrayOf(0f)
            }
        assertThat(trySimplifyMatchingOppositeSignTerms(x, malformedPlus, subtractB = true))
            .isNull()

        val unbackedArg =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        RemoteFloatOperationKey.Plus,
                        RemoteNamedCacheKey(RemoteValue.Domain.User, "detached"),
                        x,
                    ),
            ) {
                floatArrayOf(0f)
            }
        assertThat(trySimplifyMatchingOppositeSignTerms(x, unbackedArg, subtractB = true)).isNull()

        // Precomputed O(1) expressionFlags support arbitrary depth without traversal limits, while
        // still rejecting deep non-deterministic or non-finite leaves in O(1).
        var deep: RemoteFloat = x
        var deepRand: RemoteFloat = rand()
        var deepInf: RemoteFloat = x + Float.POSITIVE_INFINITY
        repeat(200) {
            deep = sin(deep)
            deepRand = sin(deepRand)
            deepInf = sin(deepInf)
        }
        assertThat(
                trySimplifyMatchingOppositeSignTerms(deep, deep, subtractB = true)
                    ?.constantValueOrNull
            )
            .isEqualTo(0f)
        assertThat(trySimplifyMatchingOppositeSignTerms(deepRand, deepRand, subtractB = true))
            .isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(deepInf, deepInf, subtractB = true))
            .isNull()
    }

    // endregion

    // region foldAdditiveConstFloat

    @Test
    fun foldAdditiveConstFloat_allOpAndSignCombinations() {
        val x = createNamedRemoteFloat("x", xVal)
        val prevValues = listOf(10f, 5f, -5f, -10f)
        val nextValues = listOf(10f, 5f, -5f, -10f)
        val ops = listOf(RemoteFloatOperationKey.Plus, RemoteFloatOperationKey.Minus)

        for (prevOp in ops) {
            for (prevConst in prevValues) {
                val base =
                    if (prevOp == RemoteFloatOperationKey.Plus) x + prevConst else x - prevConst
                val prevSigned =
                    if (prevOp == RemoteFloatOperationKey.Minus) -prevConst else prevConst

                for (nextOp in ops) {
                    for (nextConst in nextValues) {
                        val delta =
                            if (nextOp == RemoteFloatOperationKey.Minus) -nextConst else nextConst
                        val expectedCombined = prevSigned + delta
                        val folded = foldAdditiveConstFloat(base, nextConst, nextOp)

                        assertThat(folded).isNotNull()
                        when {
                            expectedCombined == 0f -> assertThat(folded).isEqualTo(x)
                            expectedCombined < 0f ->
                                assertThat(folded).isEqualTo(x - (-expectedCombined))
                            else -> assertThat(folded).isEqualTo(x + expectedCombined)
                        }
                        assertThat(evalFloat(folded!!)).isEqualTo(xVal + expectedCombined)
                    }
                }
            }
        }

        // Chained constant folding on negated inner expression (e.g. (44f - x) - c).
        val baseNeg = 44f.rf - x
        assertThat(foldAdditiveConstFloat(baseNeg, 8f, RemoteFloatOperationKey.Minus))
            .isEqualTo((-x) + 36f)
        assertThat(foldAdditiveConstFloat(baseNeg, 44f, RemoteFloatOperationKey.Minus))
            .isEqualTo(-x)
        assertThat(foldAdditiveConstFloat(baseNeg, 50f, RemoteFloatOperationKey.Minus))
            .isEqualTo((-x) - 6f)
        assertThat(foldAdditiveConstFloat(baseNeg, 6f, RemoteFloatOperationKey.Plus))
            .isEqualTo((-x) + 50f)
    }

    @Test
    fun foldAdditiveConstFloat_guardsAndNonFoldableInputs() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)
        val xPlus5 = x + 5f

        // Unsupported operation keys return null.
        assertThat(foldAdditiveConstFloat(xPlus5, 2f, RemoteFloatOperationKey.Times)).isNull()
        assertThat(foldAdditiveConstFloat(xPlus5, 2f, RemoteFloatOperationKey.Div)).isNull()
        assertThat(foldAdditiveConstFloat(xPlus5, 2f, RemoteFloatOperationKey.Min)).isNull()

        // Non-finite b returns null.
        assertThat(foldAdditiveConstFloat(xPlus5, Float.NaN, RemoteFloatOperationKey.Plus)).isNull()
        assertThat(
                foldAdditiveConstFloat(
                    xPlus5,
                    Float.POSITIVE_INFINITY,
                    RemoteFloatOperationKey.Plus,
                )
            )
            .isNull()
        assertThat(
                foldAdditiveConstFloat(
                    xPlus5,
                    Float.NEGATIVE_INFINITY,
                    RemoteFloatOperationKey.Minus,
                )
            )
            .isNull()

        // Non-finite previous constant or overflowing sum returns null.
        assertThat(
                foldAdditiveConstFloat(
                    x + Float.POSITIVE_INFINITY,
                    Float.POSITIVE_INFINITY,
                    RemoteFloatOperationKey.Minus,
                )
            )
            .isNull()
        assertThat(
                foldAdditiveConstFloat(
                    x + Float.POSITIVE_INFINITY,
                    5f,
                    RemoteFloatOperationKey.Plus,
                )
            )
            .isNull()
        assertThat(
                foldAdditiveConstFloat(
                    x + Float.MAX_VALUE,
                    Float.MAX_VALUE,
                    RemoteFloatOperationKey.Plus,
                )
            )
            .isNull()
        assertThat(
                foldAdditiveConstFloat(
                    x - Float.MAX_VALUE,
                    Float.MAX_VALUE,
                    RemoteFloatOperationKey.Minus,
                )
            )
            .isNull()

        // Select with finite branches folds canonically via Select lifting; overflowed select
        // retains its non-finite branch rather than cancelling back to a finite select.
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)
        val sel = flag.select(10f.rf, 20f.rf)
        assertThat((sel + 5f) - 5f).isEqualTo(sel)
        val foldedSel = ((sel + 5f) + 3f) as RemoteFloatSelect
        assertThat(foldedSel).isEqualTo(flag.select(18f.rf, 28f.rf))
        assertThat(foldedSel.ifTrue.constantValueOrNull).isEqualTo(18f)
        assertThat(foldedSel.ifFalse.constantValueOrNull).isEqualTo(28f)

        val overflowSel = flag.select(Float.MAX_VALUE.rf, 10f.rf) + Float.MAX_VALUE
        assertThat(
                foldAdditiveConstFloat(
                    overflowSel,
                    Float.MAX_VALUE,
                    RemoteFloatOperationKey.Minus,
                )
            )
            .isNull()

        // Non-additive or variable-RHS operands return null.
        assertThat(foldAdditiveConstFloat(x, 5f, RemoteFloatOperationKey.Plus)).isNull()
        assertThat(foldAdditiveConstFloat(-x, 5f, RemoteFloatOperationKey.Plus)).isNull()
        assertThat(foldAdditiveConstFloat(x * 2f, 5f, RemoteFloatOperationKey.Plus)).isNull()
        assertThat(foldAdditiveConstFloat(x + y, 5f, RemoteFloatOperationKey.Plus)).isNull()
        assertThat(foldAdditiveConstFloat(x - y, 5f, RemoteFloatOperationKey.Minus)).isNull()
        assertThat(foldAdditiveConstFloat(5f.rf, 3f, RemoteFloatOperationKey.Plus)).isNull()

        // Malformed / unbacked cache keys safely return null.
        val detachedPlus =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        RemoteFloatOperationKey.Plus,
                        RemoteNamedCacheKey(RemoteValue.Domain.User, "detached"),
                        5f,
                    ),
            ) {
                floatArrayOf(0f)
            }
        assertThat(foldAdditiveConstFloat(detachedPlus, 3f, RemoteFloatOperationKey.Plus)).isNull()

        val wrongArgCount =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteOperationCacheKey.create(RemoteFloatOperationKey.Plus, x),
            ) {
                floatArrayOf(0f)
            }
        assertThat(foldAdditiveConstFloat(wrongArgCount, 3f, RemoteFloatOperationKey.Plus)).isNull()
    }

    // endregion

    // region trySimplifyMatchingOppositeSignTerms & foldAdditiveConstInt (Int)

    @Test
    fun trySimplifyMatchingOppositeSignTerms_int_allNestedAndCrossCombinations() {
        val x = createNamedRemoteInt("x", xiVal)
        val y = createNamedRemoteInt("y", yiVal)
        val z = createNamedRemoteInt("z", ziVal)
        val signs = listOf(1, -1)

        // Direct cancellation.
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x, x, subtractB = true)?.constantValueOrNull
            )
            .isEqualTo(0)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x, -x, subtractB = false)?.constantValueOrNull
            )
            .isEqualTo(0)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(-x, x, subtractB = false)?.constantValueOrNull
            )
            .isEqualTo(0)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(-x, -x, subtractB = true)?.constantValueOrNull
            )
            .isEqualTo(0)
        assertThat(trySimplifyMatchingOppositeSignTerms(x, x, subtractB = false)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x, -x, subtractB = true)).isNull()

        // Right-nested a +/- (b0 +/- b1) and left-nested (a0 +/- a1) +/- b.
        for (aSign in signs) {
            val a = if (aSign > 0) x else -x
            val aEval = aSign * xiVal
            for (subtractB in listOf(false, true)) {
                val bSign = if (subtractB) -1 else 1
                for (opMinus in listOf(false, true)) {
                    val opSign = if (opMinus) -1 else 1
                    for (tSign in signs) {
                        val tx = if (tSign > 0) x else -x
                        val right = if (opMinus) tx - y else tx + y
                        val resRight = trySimplifyMatchingOppositeSignTerms(a, right, subtractB)
                        if (aSign == -(bSign * tSign)) {
                            assertThat(resRight).isNotNull()
                            assertThat(evalInt(resRight!!))
                                .isEqualTo(aEval + bSign * (tSign * xiVal + opSign * yiVal))
                        } else {
                            assertThat(resRight).isNull()
                        }

                        val resLeft = trySimplifyMatchingOppositeSignTerms(right, a, subtractB)
                        if (tSign == -(bSign * aSign)) {
                            assertThat(resLeft).isNotNull()
                            assertThat(evalInt(resLeft!!))
                                .isEqualTo(tSign * xiVal + opSign * yiVal + bSign * aEval)
                        } else {
                            assertThat(resLeft).isNull()
                        }
                    }
                }
            }
        }

        // Cross-nested, signed constants (including Int.MIN_VALUE), and all 4 sumSignedInt
        // branches.
        assertThat(trySimplifyMatchingOppositeSignTerms(y + x, z - x, subtractB = false))
            .isEqualTo(y + z)
        assertThat(trySimplifyMatchingOppositeSignTerms(y + x, z + x, subtractB = true))
            .isEqualTo(y - z)
        assertThat(trySimplifyMatchingOppositeSignTerms(x - y, z - x, subtractB = false))
            .isEqualTo(z - y)
        assertThat(trySimplifyMatchingOppositeSignTerms((-x) - y, (-x) + z, subtractB = true))
            .isEqualTo((-y) - z)
        assertThat(trySimplifyMatchingOppositeSignTerms(x + 5, y - 5, subtractB = false))
            .isEqualTo(x + y)
        assertThat(trySimplifyMatchingOppositeSignTerms(x + (-5), y + 5, subtractB = false))
            .isEqualTo(x + y)
        assertThat(trySimplifyMatchingOppositeSignTerms(x - (-5), y - 5, subtractB = false))
            .isEqualTo(x + y)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(
                    x + Int.MIN_VALUE,
                    y + Int.MIN_VALUE,
                    subtractB = false,
                )
            )
            .isEqualTo(x + y)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x + 8, 44.ri - x, subtractB = false)
                    ?.constantValueOrNull
            )
            .isEqualTo(52)
        assertThat(
                trySimplifyMatchingOppositeSignTerms(x + y, y + x, subtractB = true)
                    ?.constantValueOrNull
            )
            .isEqualTo(0)

        // Non-deterministic rand().toRemoteInt() must not cancel.
        val r1 = (rand() * 100f).toRemoteInt()
        val r2 = (rand() * 100f).toRemoteInt()
        assertThat(trySimplifyMatchingOppositeSignTerms(r1, r2, subtractB = true)).isNull()
        assertThat(trySimplifyMatchingOppositeSignTerms(x + r1, y - r2, subtractB = false)).isNull()
    }

    @Test
    fun foldAdditiveConstInt_allOpSignAndWraparoundCombinations() {
        val x = createNamedRemoteInt("x", xiVal)
        val y = createNamedRemoteInt("y", yiVal)
        val prevValues = listOf(10, 5, -5, -10)
        val nextValues = listOf(10, 5, -5, -10)
        val ops = listOf(RemoteIntOperationKey.Add, RemoteIntOperationKey.Sub)

        for (prevOp in ops) {
            for (prevConst in prevValues) {
                val base = if (prevOp == RemoteIntOperationKey.Add) x + prevConst else x - prevConst
                val prevSigned = if (prevOp == RemoteIntOperationKey.Sub) -prevConst else prevConst

                for (nextOp in ops) {
                    for (nextConst in nextValues) {
                        val delta =
                            if (nextOp == RemoteIntOperationKey.Sub) -nextConst else nextConst
                        val expectedCombined = prevSigned + delta
                        val folded = foldAdditiveConstInt(base, nextConst, nextOp)

                        assertThat(folded).isNotNull()
                        when {
                            expectedCombined == 0 -> assertThat(folded).isEqualTo(x)
                            expectedCombined < 0 && expectedCombined != Int.MIN_VALUE ->
                                assertThat(folded).isEqualTo(x - (-expectedCombined))
                            else -> assertThat(folded).isEqualTo(x + expectedCombined)
                        }
                        assertThat(evalInt(folded!!)).isEqualTo(xiVal + expectedCombined)
                    }
                }
            }
        }

        // Exact 32-bit wraparound including Int.MAX_VALUE / Int.MIN_VALUE.
        assertThat(foldAdditiveConstInt(x + Int.MAX_VALUE, 1, RemoteIntOperationKey.Add))
            .isEqualTo(x + Int.MIN_VALUE)
        assertThat(
                foldAdditiveConstInt(x + Int.MIN_VALUE, Int.MIN_VALUE, RemoteIntOperationKey.Add)
            )
            .isEqualTo(x)
        assertThat(
                foldAdditiveConstInt(x - Int.MIN_VALUE, Int.MIN_VALUE, RemoteIntOperationKey.Sub)
            )
            .isEqualTo(x)
        val wrapped =
            foldAdditiveConstInt(x + 2_000_000_000, 2_000_000_000, RemoteIntOperationKey.Add)
        assertThat(wrapped).isEqualTo(x - 294_967_296)
        assertThat(evalInt(wrapped!!)).isEqualTo(xiVal + 2_000_000_000 + 2_000_000_000)

        // Unsupported ops and non-foldable operands return null.
        assertThat(foldAdditiveConstInt(x + 5, 2, RemoteIntOperationKey.Mul)).isNull()
        assertThat(foldAdditiveConstInt(x + 5, 2, RemoteIntOperationKey.Div)).isNull()
        assertThat(foldAdditiveConstInt(x, 5, RemoteIntOperationKey.Add)).isNull()
        assertThat(foldAdditiveConstInt(-x, 5, RemoteIntOperationKey.Add)).isNull()
        assertThat(foldAdditiveConstInt(x * 2, 5, RemoteIntOperationKey.Add)).isNull()
        assertThat(foldAdditiveConstInt(x + y, 5, RemoteIntOperationKey.Add)).isNull()
        assertThat(foldAdditiveConstInt(5.ri, 3, RemoteIntOperationKey.Add)).isNull()
    }

    // endregion

    // region sumSignedFloat & sumSignedInt

    @Test
    fun sumSignedFloat_allSignCombinationsAndEdgeCases() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)
        val signs = listOf(1, -1)

        // All 4 sign pairs on distinct variables (verifying positive-term-first preference & eval).
        assertThat(sumSignedFloat(x, 1, y, 1)).isEqualTo(x + y)
        assertThat(sumSignedFloat(x, 1, y, -1)).isEqualTo(x - y)
        assertThat(sumSignedFloat(x, -1, y, 1)).isEqualTo(y - x)
        assertThat(sumSignedFloat(x, -1, y, -1)).isEqualTo((-x) - y)

        for (aSign in signs) {
            for (bSign in signs) {
                val sum = sumSignedFloat(x, aSign, y, bSign)
                assertThat(evalFloat(sum)).isEqualTo(aSign * xVal + bSign * yVal)
            }
        }

        // Same variable cancels when signs oppose, adds when signs match.
        assertThat(sumSignedFloat(x, 1, x, -1).constantValueOrNull).isEqualTo(0f)
        assertThat(sumSignedFloat(x, -1, x, 1).constantValueOrNull).isEqualTo(0f)
        assertThat(evalFloat(sumSignedFloat(x, 1, x, 1))).isEqualTo(2f * xVal)
        assertThat(evalFloat(sumSignedFloat(x, -1, x, -1))).isEqualTo(-2f * xVal)

        // Negated operands and constants evaluate accurately across all 4 sign pairs.
        val negX = -x
        val c = 12.5f.rf
        for (aSign in signs) {
            for (bSign in signs) {
                assertThat(evalFloat(sumSignedFloat(negX, aSign, y, bSign)))
                    .isEqualTo(aSign * (-xVal) + bSign * yVal)
                assertThat(evalFloat(sumSignedFloat(x, aSign, c, bSign)))
                    .isEqualTo(aSign * xVal + bSign * 12.5f)
                assertThat(evalFloat(sumSignedFloat(c, aSign, y, bSign)))
                    .isEqualTo(aSign * 12.5f + bSign * yVal)
                assertThat(sumSignedFloat(c, aSign, 4f.rf, bSign).constantValueOrNull)
                    .isEqualTo(aSign * 12.5f + bSign * 4f)
            }
        }

        // Zero constant collapses cleanly.
        assertThat(sumSignedFloat(x, 1, 0f.rf, 1)).isEqualTo(x)
        assertThat(sumSignedFloat(x, 1, 0f.rf, -1)).isEqualTo(x)
        assertThat(sumSignedFloat(0f.rf, 1, y, 1)).isEqualTo(y)
        assertThat(sumSignedFloat(0f.rf, -1, y, 1)).isEqualTo(y)
        assertThat(sumSignedFloat(x, -1, 0f.rf, -1)).isEqualTo(-x)
    }

    @Test
    fun sumSignedInt_allSignCombinationsAndEdgeCases() {
        val x = createNamedRemoteInt("x", xiVal)
        val y = createNamedRemoteInt("y", yiVal)
        val signs = listOf(1, -1)

        assertThat(sumSignedInt(x, 1, y, 1)).isEqualTo(x + y)
        assertThat(sumSignedInt(x, 1, y, -1)).isEqualTo(x - y)
        assertThat(sumSignedInt(x, -1, y, 1)).isEqualTo(y - x)
        assertThat(sumSignedInt(x, -1, y, -1)).isEqualTo((-x) - y)

        for (aSign in signs) {
            for (bSign in signs) {
                val sum = sumSignedInt(x, aSign, y, bSign)
                assertThat(evalInt(sum)).isEqualTo(aSign * xiVal + bSign * yiVal)
                assertThat(sumSignedInt(12.ri, aSign, 4.ri, bSign).constantValueOrNull)
                    .isEqualTo(aSign * 12 + bSign * 4)
            }
        }

        assertThat(sumSignedInt(x, 1, x, -1).constantValueOrNull).isEqualTo(0)
        assertThat(sumSignedInt(x, -1, x, 1).constantValueOrNull).isEqualTo(0)
        assertThat(sumSignedInt(x, 1, 0.ri, -1)).isEqualTo(x)
        assertThat(sumSignedInt(0.ri, -1, y, 1)).isEqualTo(y)
        assertThat(sumSignedInt(Int.MIN_VALUE.ri, -1, Int.MIN_VALUE.ri, -1).constantValueOrNull)
            .isEqualTo(0)
    }

    // endregion

    // region sumsToZero

    @Test
    fun sumsToZero_float_exhaustive() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)
        val signs = listOf(1, -1)

        // All combinations of (x vs -x) x aSign x (x vs -x) x bSign.
        for (aNeg in listOf(false, true)) {
            val a = if (aNeg) -x else x
            val aTermSign = if (aNeg) -1 else 1
            for (aSign in signs) {
                for (bNeg in listOf(false, true)) {
                    val b = if (bNeg) -x else x
                    val bTermSign = if (bNeg) -1 else 1
                    for (bSign in signs) {
                        val expected = (aSign * aTermSign) == -(bSign * bTermSign)
                        assertThat(sumsToZero(a, aSign, b, bSign)).isEqualTo(expected)
                    }
                }
            }
        }

        // Distinct variables are never additive inverses.
        for (aSign in signs) {
            for (bSign in signs) {
                assertThat(sumsToZero(x, aSign, y, bSign)).isFalse()
                assertThat(sumsToZero(x, aSign, -y, bSign)).isFalse()
            }
        }

        // Constants across all sign/value combinations including 0f, -0f, and non-finite.
        val constValues = listOf(5f, -5f, 0f, -0f, 7f)
        for (av in constValues) {
            for (aSign in signs) {
                for (bv in constValues) {
                    for (bSign in signs) {
                        val expected = (aSign * av) + (bSign * bv) == 0f
                        assertThat(sumsToZero(av.rf, aSign, bv.rf, bSign)).isEqualTo(expected)
                    }
                }
            }
        }
        val posInf = Float.POSITIVE_INFINITY.rf
        val negInf = Float.NEGATIVE_INFINITY.rf
        for (aSign in signs) {
            for (bSign in signs) {
                assertThat(sumsToZero(posInf, aSign, posInf, bSign)).isFalse()
                assertThat(sumsToZero(posInf, aSign, negInf, bSign)).isFalse()
                assertThat(sumsToZero(negInf, aSign, negInf, bSign)).isFalse()
                assertThat(sumsToZero(posInf, aSign, 5f.rf, bSign)).isFalse()
            }
        }

        // Variable vs constant is never an additive inverse.
        assertThat(sumsToZero(x, 1, xVal.rf, -1)).isFalse()

        // Deterministic compound expression vs non-deterministic / non-finite guards.
        val det = sin(x) * 2f
        assertThat(sumsToZero(det, 1, det, -1)).isTrue()
        assertThat(sumsToZero(det, 1, -det, 1)).isTrue()
        assertThat(sumsToZero(det, 1, det, 1)).isFalse()

        assertThat(sumsToZero(rand(), 1, rand(), -1)).isFalse()
        assertThat(sumsToZero(randRange(0f.rf, 1f.rf), 1, randRange(0f.rf, 1f.rf), -1)).isFalse()
        assertThat(sumsToZero(x + Float.POSITIVE_INFINITY, 1, x + Float.POSITIVE_INFINITY, -1))
            .isFalse()

        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)
        val finiteSel = flag.select(1f.rf, 2f.rf)
        val infSel = flag.select(Float.POSITIVE_INFINITY.rf, 2f.rf)
        val baseMaxSel = flag.select(Float.MAX_VALUE.rf, 2f.rf)
        val overflowSel = baseMaxSel + Float.MAX_VALUE
        assertThat(sumsToZero(finiteSel, 1, finiteSel, -1)).isTrue()
        assertThat(sumsToZero(infSel, 1, infSel, -1)).isFalse()
        assertThat(sumsToZero(overflowSel, 1, overflowSel, -1)).isFalse()

        // Overflowed select nested inside another op must not report sumsToZero.
        val wrappedOverflow = overflowSel + x
        assertThat(sumsToZero(wrappedOverflow, 1, wrappedOverflow, -1)).isFalse()
        assertThat(sumsToZero(sin(overflowSel), 1, sin(overflowSel), -1)).isFalse()

        val m1 = MutableRemoteFloat(3f)
        val m2 = MutableRemoteFloat(3f)
        assertThat(sumsToZero(m1, 1, m1, -1)).isTrue()
        assertThat(sumsToZero(m1, 1, m2, -1)).isFalse()
    }

    @Test
    fun sumsToZero_float_rhsNonFiniteSelect_doesNotCancel() {
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)
        val syntheticKey =
            RemoteOperationCacheKey.create(
                RemoteFloatOperationKey.Plus,
                flag,
                1f,
            )
        val syntheticNonFiniteSel =
            RemoteFloatSelect(
                condition = SelectFloatCondition.BooleanCondition(flag),
                ifTrue = Float.POSITIVE_INFINITY.rf,
                ifFalse = 2f.rf,
                cacheKey = syntheticKey,
            )
        val sameKeyNonSelect =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        RemoteFloatOperationKey.Plus,
                        flag,
                        1f,
                    ),
            ) {
                floatArrayOf(0f)
            }
        assertThat(sumsToZero(sameKeyNonSelect, 1, syntheticNonFiniteSel, -1)).isFalse()
        assertThat(sumsToZero(syntheticNonFiniteSel, 1, sameKeyNonSelect, -1)).isFalse()
    }

    @Test
    fun sumsToZero_int_exhaustive() {
        val x = createNamedRemoteInt("x", xiVal)
        val y = createNamedRemoteInt("y", yiVal)
        val signs = listOf(1, -1)

        for (aNeg in listOf(false, true)) {
            val a = if (aNeg) -x else x
            val aTermSign = if (aNeg) -1 else 1
            for (aSign in signs) {
                for (bNeg in listOf(false, true)) {
                    val b = if (bNeg) -x else x
                    val bTermSign = if (bNeg) -1 else 1
                    for (bSign in signs) {
                        val expected = (aSign * aTermSign) == -(bSign * bTermSign)
                        assertThat(sumsToZero(a, aSign, b, bSign)).isEqualTo(expected)
                    }
                }
            }
        }

        assertThat(sumsToZero(x, 1, y, -1)).isFalse()

        val constValues = listOf(5, -5, 0, 7, Int.MIN_VALUE, Int.MAX_VALUE)
        for (av in constValues) {
            for (aSign in signs) {
                for (bv in constValues) {
                    for (bSign in signs) {
                        val sa = if (aSign > 0) av else -av
                        val sb = if (bSign > 0) bv else -bv
                        assertThat(sumsToZero(av.ri, aSign, bv.ri, bSign)).isEqualTo(sa + sb == 0)
                    }
                }
            }
        }

        val randInt = (rand() * 10f).toRemoteInt()
        assertThat(sumsToZero(randInt, 1, randInt, -1)).isFalse()

        val m1 = MutableRemoteInt(3)
        val m2 = MutableRemoteInt(3)
        assertThat(sumsToZero(m1, 1, m1, -1)).isTrue()
        assertThat(sumsToZero(m1, 1, m2, -1)).isFalse()
    }

    // endregion

    // region RemoteStateCacheKey expressionFlags & canonical Select keys

    @Test
    fun cacheKey_expressionFlags_andCanonicalSelectKeys() {
        val f = createNamedRemoteFloat("f", 3.5f)
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)

        assertThat(f.cacheKey.isDeterministic(requireFiniteFloats = true)).isTrue()
        assertThat(RemoteConstantCacheKey(4.5f).isDeterministic(requireFiniteFloats = true))
            .isTrue()
        assertThat(
                RemoteConstantCacheKey(Float.POSITIVE_INFINITY)
                    .isDeterministic(requireFiniteFloats = false)
            )
            .isTrue()
        assertThat(
                RemoteConstantCacheKey(Float.POSITIVE_INFINITY)
                    .isDeterministic(requireFiniteFloats = true)
            )
            .isFalse()
        assertThat(
                RemoteConstantCacheKey(Float.NEGATIVE_INFINITY)
                    .isDeterministic(requireFiniteFloats = true)
            )
            .isFalse()

        // Canonical Select cache keys match equivalent directly constructed Select nodes.
        val sel = flag.select(10f.rf, 20f.rf)
        assertThat(sel + 5f).isEqualTo(flag.select(15f.rf, 25f.rf))
        assertThat((sel + 5f).cacheKey).isEqualTo(flag.select(15f.rf, 25f.rf).cacheKey)

        val selInt = flag.select(10.ri, 20.ri)
        assertThat(selInt + 5).isEqualTo(flag.select(15.ri, 25.ri))
        assertThat((selInt + 5).cacheKey).isEqualTo(flag.select(15.ri, 25.ri).cacheKey)

        // Overflowed select directly exposes non-finite float flag on its canonical cacheKey.
        val overflowSel = flag.select(Float.MAX_VALUE.rf, 10f.rf) + Float.MAX_VALUE
        assertThat(overflowSel.cacheKey.isDeterministic(requireFiniteFloats = false)).isTrue()
        assertThat(overflowSel.cacheKey.isDeterministic(requireFiniteFloats = true)).isFalse()

        // Non-deterministic flag propagates in O(1) across any depth.
        var deepRand: RemoteFloat = rand()
        repeat(200) { deepRand = cos(deepRand) }
        assertThat(deepRand.cacheKey.isDeterministic(requireFiniteFloats = false)).isFalse()
    }

    @Test
    fun nanAndDomainNonFiniteOps_doNotCancelToZero() {
        val x = createNamedRemoteFloat("x", xVal)
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)

        val nan = sqrt((-1f).rf)
        assertThat((nan - nan).constantValueOrNull).isNull()
        assertThat(evalFloat(nan - nan)).isNaN()

        val distinctNans = (x + sqrt((-1f).rf)) - acos(2f.rf)
        assertThat(distinctNans.constantValueOrNull).isNull()
        assertThat(distinctNans).isNotEqualTo(x)
        assertThat(evalFloat(distinctNans)).isNaN()

        val rawNan = RemoteFloat(Float.NaN)
        assertThat((rawNan - rawNan).constantValueOrNull).isNull()

        val divZero = x / 0f
        assertThat((divZero - divZero).constantValueOrNull).isNull()
        assertThat(evalFloat(divZero - divZero)).isNaN()

        val remZero = x % 0f
        assertThat((remZero + (44f.rf - remZero)).constantValueOrNull).isNull()
        assertThat(evalFloat(remZero + (44f.rf - remZero))).isNaN()

        val lnZeroBranch = ln(flag.select(0f.rf, 1f.rf))
        assertThat((lnZeroBranch - lnZeroBranch).constantValueOrNull).isNull()
        assertThat(evalFloat(lnZeroBranch - lnZeroBranch)).isNaN()
    }

    @Test
    fun dynamicBranchSelect_cancelsWhenFinite_rejectsWhenNonFinite() {
        val x = createNamedRemoteFloat("x", xVal)
        val y = createNamedRemoteFloat("y", yVal)
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)

        val dynSel = flag.select(x, y)
        assertThat((dynSel - dynSel).constantValueOrNull).isEqualTo(0f)
        assertThat((dynSel + (44f.rf - dynSel)).constantValueOrNull).isEqualTo(44f)

        val nonFiniteDynSel = flag.select(x + Float.POSITIVE_INFINITY, y)
        assertThat((nonFiniteDynSel - nonFiniteDynSel).constantValueOrNull).isNull()
        assertThat(evalFloat(nonFiniteDynSel - nonFiniteDynSel)).isNaN()
    }

    @Test
    fun lambdaRemoteFloat_divByZeroAfterCancel_andMixedMutableIdempotent() {
        val xi = createNamedRemoteInt("xi", xiVal)
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)

        // Lambda-backed RemoteFloat with default key must not falsely cancel rand() or Infinity.
        val lambdaRand = RemoteFloat { rand() }
        assertThat((lambdaRand - lambdaRand).constantValueOrNull).isNull()

        val lambdaInf = RemoteFloat { Float.POSITIVE_INFINITY.rf }
        assertThat((lambdaInf - lambdaInf).constantValueOrNull).isNull()
        assertThat(evalFloat(lambdaInf - lambdaInf)).isNaN()

        // Integer divide-by-zero after additive/xor/select simplification must not throw.
        assertThat(((xi - xi) / 0).constantValueOrNull).isEqualTo(0)
        assertThat(((xi xor xi) / 0).constantValueOrNull).isEqualTo(0)
        val collapsedSelectDivZero = (flag.select(4.ri, 8.ri) + flag.select(6.ri, 2.ri)) / 0
        assertThat(evalInt(collapsedSelectDivZero)).isEqualTo(0)

        // Mixed MutableRemote* and read-only Remote* sharing an ID must collapse symmetrically.
        val idFloat = NanEncoding.asNan(42)
        val mFloat = MutableRemoteFloat.createMutableForId(idFloat)
        val rFloat = RemoteFloat(idFloat)
        assertThat(min(mFloat, rFloat)).isEqualTo(rFloat)
        assertThat(min(rFloat, mFloat)).isEqualTo(rFloat)
        assertThat(min(mFloat, rFloat)).isNotInstanceOf(MutableRemoteFloat::class.java)

        val mInt = MutableRemoteInt.createMutableForId(42L)
        val rInt = RemoteInt.createForId(NanEncoding.encodeLongId(42))
        assertThat(min(mInt, rInt)).isEqualTo(rInt)
        assertThat(min(rInt, mInt)).isEqualTo(rInt)
        assertThat(min(mInt, rInt)).isNotInstanceOf(MutableRemoteInt::class.java)
    }

    @Test
    fun cbrtNegative_customNonFiniteCacheKey_andCanonicalSelectCancellation() {
        val negX = createNamedRemoteFloat("negX", -8f)
        val y = createNamedRemoteFloat("y", yVal)

        // Player OP_CBRT evaluates `pow(x, 1/3)` which produces NaN for x < 0; must NOT cancel.
        val cbrtExpr = cbrt(negX)
        assertThat((cbrtExpr - cbrtExpr).constantValueOrNull).isNull()
        assertThat(evalFloat(cbrtExpr - cbrtExpr)).isNaN()
        val nestedCbrt = y + (10f.rf - cbrtExpr) + cbrtExpr
        assertThat(nestedCbrt.constantValueOrNull).isNull()
        assertThat(evalFloat(nestedCbrt)).isNaN()

        // Custom RemoteStateCacheKey (not extending BaseRemoteStateCacheKey) with
        // isDeterministic(requireFiniteFloats = true) == false must propagate non-finite flag into
        // parent operations so `(customInf + 1f) - (customInf + 1f)` does not cancel to 0f.
        val customNonFiniteKey =
            object : RemoteStateCacheKey {
                override fun toDebugString(): String = "customNonFinite"

                override fun isDeterministic(requireFiniteFloats: Boolean): Boolean =
                    !requireFiniteFloats
            }
        val customInfFloat =
            RemoteFloat(constantValueOrNull = null, cacheKey = customNonFiniteKey) {
                floatArrayOf(Float.POSITIVE_INFINITY)
            }
        val shiftedCustomInf = customInfFloat + 1f
        assertThat((shiftedCustomInf - shiftedCustomInf).constantValueOrNull).isNull()
        assertThat(evalFloat(shiftedCustomInf - shiftedCustomInf)).isNaN()

        // Canonical Select cache keys allow `(a < b).select(...)` and `selectIfLt(a, b, ...)` to
        // cancel as matching opposite-sign terms.
        val selViaBool = y.isLessThan(100f.rf).select(5f.rf, 9f.rf)
        val selViaHelper = selectIfLt(y, 100f.rf, 5f.rf, 9f.rf)
        assertThat((selViaBool - selViaHelper).constantValueOrNull).isEqualTo(0f)
    }

    // endregion
}
