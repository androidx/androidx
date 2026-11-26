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

import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteNamedCacheKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.cacheKey
import androidx.compose.remote.creation.compose.state.intValue
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.mad
import androidx.compose.remote.creation.compose.state.rand
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.IntervalComparison
import androidx.compose.remote.document.intervals.TriState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteAffineAndRefinementTest {

    @Test
    fun floatAffineCancellationAndComparisons() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val env = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(0f, 100f))
        val eval = RemoteValueIntervalEvaluator(env)

        // Exact self-cancellation: x - x == 0f
        assertThat(eval.evalFloat(x - x)).isEqualTo(FloatInterval.ZERO)

        // Correlated affine comparisons
        assertThat(eval.evalBoolean(x.isLessThan(x + 1f.rf))).isEqualTo(TriState.TRUE)
        assertThat(eval.evalBoolean(x.isGreaterThan(x + 1f.rf))).isEqualTo(TriState.FALSE)
        assertThat(eval.evalBoolean((x * 2f.rf).isGreaterThanOrEqualTo(x + x - 1f.rf)))
            .isEqualTo(TriState.TRUE)

        // MAD cancellation: mad(x, 2f, -(x * 2f)) tightens near zero (within ULP error)
        val madCancel = mad(x, 2f.rf, -(x * 2f.rf))
        val madRange = eval.evalFloat(madCancel)
        assertThat(madRange.min).isWithin(1e-3f).of(0f)
        assertThat(madRange.max).isWithin(1e-3f).of(0f)

        // Lerp cancellation: lerp(x, x + 10f, 0.5f) - x is tight around 5f (standard interval would
        // be [-95f, 105f])
        val lerpDiff = lerp(x, x + 10f.rf, 0.5f.rf) - x
        val lerpRange = eval.evalFloat(lerpDiff)
        assertThat(lerpRange.min).isWithin(1e-3f).of(5f)
        assertThat(lerpRange.max).isWithin(1e-3f).of(5f)

        // Nondeterministic rand() - rand() must NOT cancel
        val randDiff = rand() - rand()
        assertThat(eval.evalFloat(randDiff).isPoint).isFalse()
    }

    @Test
    fun intAffineCancellationAndOverflowSafety() {
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val env = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(0, 1000))
        val eval = RemoteValueIntervalEvaluator(env)

        assertThat(eval.evalInt(n - n)).isEqualTo(IntInterval.ZERO)
        assertThat(eval.evalInt((n * 3.ri) - (n + n + n))).isEqualTo(IntInterval.ZERO)
        assertThat(eval.evalBoolean((n + 2.ri).isGreaterThan(n))).isEqualTo(TriState.TRUE)
        assertThat(eval.evalBoolean((n + 2.ri).isLessThan(n))).isEqualTo(TriState.FALSE)

        // Overflow near Int.MAX_VALUE must NOT prove n + 2 > n
        val overflowEnv =
            MapIntervalEnvironment.EMPTY.withNamedInt(
                "n",
                IntInterval(Int.MAX_VALUE - 1, Int.MAX_VALUE),
            )
        val overflowEval = RemoteValueIntervalEvaluator(overflowEnv)
        assertThat(overflowEval.evalBoolean((n + 2.ri).isGreaterThan(n)))
            .isEqualTo(TriState.UNKNOWN)
    }

    @Test
    fun intToFloatAffineBridgeAndBackPropagation() {
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val env = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(0, 100))
        val eval = RemoteValueIntervalEvaluator(env)

        // Affine cancellation across Int -> Float conversion
        val nf = n.toRemoteFloat()
        assertThat(eval.evalFloat(nf - nf)).isEqualTo(FloatInterval.ZERO)
        assertThat(eval.evalBoolean(nf.isLessThan((n + 1.ri).toRemoteFloat())))
            .isEqualTo(TriState.TRUE)

        // Back-propagation: nf < 5.5f narrows underlying int n to [0, 5]
        val refinedTrue = env.refine(nf.isLessThan(5.5f.rf).cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedTrue).evalInt(n))
            .isEqualTo(IntInterval(0, 5))

        // Negated branch: !(nf < 5.5f) => nf >= 5.5f narrows n to [6, 100]
        val refinedFalse = env.refine(nf.isLessThan(5.5f.rf).cacheKey, false)
        assertThat(RemoteValueIntervalEvaluator(refinedFalse).evalInt(n))
            .isEqualTo(IntInterval(6, 100))

        // Unwrapping via .toRemoteInt() / .intValue also propagates refinement
        val refinedViaIntValue = env.refine(nf.isLessThan(5.5f.rf).intValue.cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedViaIntValue).evalInt(n))
            .isEqualTo(IntInterval(0, 5))
    }

    @Test
    fun pathSensitiveNestedBranchRefinement() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val env = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(0f, 100f))
        val eval = RemoteValueIntervalEvaluator(env)

        // Inside x < 10f: x < 20f is provably TRUE (selects 1f, prunes 999f).
        // Inside !(x < 10f) (i.e. x >= 10f): x < 5f is provably FALSE (selects 4f, prunes 888f).
        val ladder =
            x.isLessThan(10f.rf)
                .select(
                    ifTrue = x.isLessThan(20f.rf).select(1f.rf, 999f.rf),
                    ifFalse = x.isLessThan(5f.rf).select(888f.rf, 4f.rf),
                )
        assertThat(eval.evalFloat(ladder)).isEqualTo(FloatInterval(1f, 4f))

        // Same for selectIfLt on floats
        val selectIfLadder =
            selectIfLt(
                x,
                10f.rf,
                selectIfLt(x, 20f.rf, 1f.rf, 999f.rf),
                selectIfLt(x, 5f.rf, 888f.rf, 4f.rf),
            )
        assertThat(eval.evalFloat(selectIfLadder)).isEqualTo(FloatInterval(1f, 4f))

        // Nested int selectIfLt
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val intEnv = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(0, 50))
        val intLadder =
            selectIfLt(
                n,
                10.ri,
                selectIfLt(n, 20.ri, 1.ri, 500.ri),
                selectIfLt(n, 5.ri, 600.ri, 4.ri),
            )
        assertThat(RemoteValueIntervalEvaluator(intEnv).evalInt(intLadder))
            .isEqualTo(IntInterval(1, 4))
    }

    @Test
    fun booleanConnectivesAndZeroComparisonUnwrapping() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 0f)
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val m = RemoteInt.createNamedRemoteInt("m", 0)
        val env =
            MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(-50f, 50f))
                .withNamedFloat("y", FloatInterval(10f, 20f))
                .withNamedInt("n", IntInterval(-50, 50))
                .withNamedInt("m", IntInterval(10, 20))

        // AND holds -> both sides refine
        val andCond = x.isGreaterThanOrEqualTo(0f.rf) and x.isLessThanOrEqualTo(10f.rf)
        val refinedAnd = env.refine(andCond.cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedAnd).evalFloat(x))
            .isEqualTo(FloatInterval(0f, 10f))

        // OR does not hold -> both sides are false
        val orCond = x.isLessThan(0f.rf) or x.isGreaterThan(10f.rf)
        val refinedOrFalse = env.refine(orCond.cacheKey, false)
        assertThat(RemoteValueIntervalEvaluator(refinedOrFalse).evalFloat(x))
            .isEqualTo(FloatInterval(0f, 10f))

        // NOT -> flips holds
        val notCond = !(x.isLessThan(5f.rf))
        val refinedNot = env.refine(notCond.cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedNot).evalFloat(x))
            .isEqualTo(FloatInterval(5f, 50f))

        // Path-sensitive AND/OR contradiction and tautology detection
        val baseEval = RemoteValueIntervalEvaluator(env)
        assertThat(baseEval.evalBoolean(n.isGreaterThan(5.ri) and n.isLessThan(3.ri)))
            .isEqualTo(TriState.FALSE)
        assertThat(baseEval.evalBoolean(n.isGreaterThan(5.ri) or n.isLessThanOrEqualTo(5.ri)))
            .isEqualTo(TriState.TRUE)

        // Boolean Equal/NotEqual against constant boolean refines underlying comparison
        val eqFalse = x.isLessThan(5f.rf).isEqualTo(false.rb)
        assertThat(RemoteValueIntervalEvaluator(env.refine(eqFalse.cacheKey, true)).evalFloat(x))
            .isEqualTo(FloatInterval(5f, 50f))

        // Float zero-comparison unwrapping: (x - y) <= 0f -> x <= 20f
        val diffLeZero = (x - y).isLessThanOrEqualTo(0f.rf)
        val refinedDiff = env.refine(diffLeZero.cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedDiff).evalFloat(x))
            .isEqualTo(FloatInterval(-50f, 20f))

        // Negated-plus zero-comparison unwrapping: (10f - x) <= 0f (i.e. (-x) + 10f <= 0f) -> x >=
        // 10f
        val constMinusX = (10f.rf - x).isLessThanOrEqualTo(0f.rf)
        val refinedConstMinusX = env.refine(constMinusX.cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedConstMinusX).evalFloat(x))
            .isEqualTo(FloatInterval(10f, 50f))

        // abs(x) <= 0f -> x == 0f
        val absLeZero = abs(x).isLessThanOrEqualTo(0f.rf)
        val refinedAbs = env.refine(absLeZero.cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedAbs).evalFloat(x))
            .isEqualTo(FloatInterval.ZERO)

        // Int zero-comparison unwrapping: (n - m) < 0 -> n <= 19
        val intDiffLtZero = (n - m).isLessThan(0.ri)
        val refinedIntDiff = env.refine(intDiffLtZero.cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedIntDiff).evalInt(n))
            .isEqualTo(IntInterval(-50, 19))

        // Enum ordinal affine cancellation and narrowing
        val mode = RemoteEnum(n, TestMode.entries)
        assertThat(baseEval.evalInt(mode.ordinal - n)).isEqualTo(IntInterval.ZERO)
        val refinedEnum = env.refine(mode.ordinal.isLessThan(2.ri).cacheKey, true)
        assertThat(RemoteValueIntervalEvaluator(refinedEnum).evalInt(n))
            .isEqualTo(IntInterval(-50, 1))
    }

    private enum class TestMode {
        A,
        B,
        C,
    }

    @Test
    fun customIntervalEnvironmentPreservesNestedOverlays() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 0f)
        val customBase =
            object : IntervalEnvironment {
                override fun floatForNamed(key: RemoteNamedCacheKey): FloatInterval =
                    FloatInterval(0f, 100f)
            }

        // Two consecutive default withFloatNarrowing calls must preserve BOTH overlays.
        val cond = x.isLessThan(10f.rf) and y.isGreaterThan(80f.rf)
        val refined = customBase.refine(cond.cacheKey, true)
        val eval = RemoteValueIntervalEvaluator(refined)

        assertThat(eval.evalFloat(x).max).isEqualTo(10f)
        assertThat(eval.evalFloat(x).maxInclusive).isFalse()
        assertThat(eval.evalFloat(y).min).isEqualTo(80f)
        assertThat(eval.evalFloat(y).minInclusive).isFalse()
    }

    @Test
    fun soundnessRegressions_xorNonBooleanUnboundedNanUlpAndOverflow() {
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)

        // 1. Xor with 1 on non-boolean [0, 5]: holds=true must NOT narrow n to 0 (2..5 also hold!)
        val nonBoolEnv = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(0, 5))
        val xor1 = n xor 1.ri
        val refinedXorTrue = nonBoolEnv.refine(xor1.cacheKey, holds = true)
        assertThat(RemoteValueIntervalEvaluator(refinedXorTrue).evalInt(n))
            .isEqualTo(IntInterval(0, 5))
        // holds=false on non-negative n forces (n xor 1) == 0 -> n == 1
        val refinedXorFalse = nonBoolEnv.refine(xor1.cacheKey, holds = false)
        assertThat(RemoteValueIntervalEvaluator(refinedXorFalse).evalInt(n))
            .isEqualTo(IntInterval.point(1))
        // holds=false on signed [-5, 5] must NOT force n == 1 because (-2 xor 1) == -1 <= 0
        val signedEnv = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(-5, 5))
        val refinedSignedXorFalse = signedEnv.refine(xor1.cacheKey, holds = false)
        assertThat(RemoteValueIntervalEvaluator(refinedSignedXorFalse).evalInt(n))
            .isEqualTo(IntInterval(-5, 5))

        // 2. Direct bool var refinement on signed [-5, 5] preserves negative ints under !holds
        val refinedSignedFalse = signedEnv.refine(n.cacheKey, holds = false)
        assertThat(RemoteValueIntervalEvaluator(refinedSignedFalse).evalInt(n))
            .isEqualTo(IntInterval(-5, 0))
        val boolEnv = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(0, 1))
        assertThat(
                RemoteValueIntervalEvaluator(boolEnv.refine(n.cacheKey, holds = false)).evalInt(n)
            )
            .isEqualTo(IntInterval.ZERO)
        assertThat(
                RemoteValueIntervalEvaluator(boolEnv.refine(n.cacheKey, holds = true)).evalInt(n)
            )
            .isEqualTo(IntInterval.point(1))

        // 3. UNBOUNDED float comparison must NOT narrow (guards against NaN entering negated arms)
        val unboundedRefined =
            MapIntervalEnvironment.EMPTY.refine(x.isLessThan(10f.rf).cacheKey, holds = false)
        assertThat(RemoteValueIntervalEvaluator(unboundedRefined).evalFloat(x))
            .isEqualTo(FloatInterval.UNBOUNDED)

        // 4. 32-bit int mul overflow inside (MIN_BOUND, MAX_BOUND): 50_000 * 50_000 wraps!
        val mulOverflowEnv = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(0, 50_000))
        val mulEval = RemoteValueIntervalEvaluator(mulOverflowEnv)
        assertThat(mulEval.evalBoolean((n * 50_000.ri + 1.ri).isGreaterThan(n * 50_000.ri)))
            .isEqualTo(TriState.UNKNOWN)

        // 5. Large-magnitude float ULP soundness: for x in [1e7f, 2e7f], ulp(2e7f) == 2f, so
        // x + 1f == x is reachable and x + 1f > x must be UNKNOWN (not TRUE!)
        val largeFloatEnv =
            MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(1e7f, 2e7f))
        val largeEval = RemoteValueIntervalEvaluator(largeFloatEnv)
        assertThat(largeEval.evalBoolean((x + 1f.rf).isGreaterThan(x))).isEqualTo(TriState.UNKNOWN)

        // 6. exactIntBoundsWithinFloatInterval boundary precision at +/- 2^24
        val exactLimit = 16_777_216
        assertThat(
                exactIntBoundsWithinFloatInterval(
                    FloatInterval(-exactLimit.toFloat(), exactLimit.toFloat())
                )
            )
            .isEqualTo(IntInterval(-exactLimit - 1, exactLimit + 1))
        assertThat(
                exactIntBoundsWithinFloatInterval(
                    FloatInterval(
                        -exactLimit.toFloat(),
                        exactLimit.toFloat(),
                        minInclusive = false,
                        maxInclusive = false,
                    )
                )
            )
            .isEqualTo(IntInterval(-exactLimit + 1, exactLimit - 1))

        // 7. IntAffineForm.scale overflow on (-1L * Long.MIN_VALUE) returns null
        assertThat(IntAffineForm(mapOf(n.cacheKey to -1L), 0L).scale(Long.MIN_VALUE)).isNull()
        assertThat(IntAffineForm(emptyMap(), -1L).scale(Long.MIN_VALUE)).isNull()
        assertThat(IntAffineForm(mapOf(n.cacheKey to Long.MIN_VALUE), 0L).scale(-1L)).isNull()

        // 8. Bitwise And/Or condition refinement requires non-negative operands (signed I_IFELSE)
        val p = RemoteInt.createNamedRemoteInt("p", 0)
        val two = RemoteInt.createNamedRemoteInt("two", 2)
        val signedPairEnv =
            MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(-5, 5))
                .withNamedInt("p", IntInterval(0, 1))
                .withNamedInt("two", IntInterval.point(2))
        val pBool = RemoteBoolean.fromRemoteInt(p)
        val nBool = RemoteBoolean.fromRemoteInt(n)
        // n = -2, p = 1 => (n | p) == -1 <= 0 (false under I_IFELSE), so p must NOT refine to 0!
        assertThat(
                RemoteValueIntervalEvaluator(
                        signedPairEnv.refine((nBool or pBool).cacheKey, holds = false)
                    )
                    .evalInt(p)
            )
            .isEqualTo(IntInterval(0, 1))
        assertThat(
                RemoteValueIntervalEvaluator(signedPairEnv.refine((n or p).cacheKey, holds = false))
                    .evalInt(p)
            )
            .isEqualTo(IntInterval(0, 1))
        // n = -1, p = 1 => (n & p) == 1 > 0 (true under I_IFELSE), so n must NOT refine to > 0!
        assertThat(
                RemoteValueIntervalEvaluator(
                        signedPairEnv.refine((nBool and pBool).cacheKey, holds = true)
                    )
                    .evalInt(n)
            )
            .isEqualTo(IntInterval(-5, 5))
        assertThat(
                RemoteValueIntervalEvaluator(signedPairEnv.refine((n and p).cacheKey, holds = true))
                    .evalInt(n)
            )
            .isEqualTo(IntInterval(-5, 5))

        // 9. Boolean Equal/NotEqual refinement requires BOTH operands in [0, 1]
        val nonCanonicalTwo = RemoteBoolean.fromRemoteInt(two)
        assertThat(
                RemoteValueIntervalEvaluator(
                        signedPairEnv.refine(
                            pBool.isNotEqualTo(nonCanonicalTwo).cacheKey,
                            holds = true,
                        )
                    )
                    .evalInt(p)
            )
            .isEqualTo(IntInterval(0, 1))
        assertThat(
                RemoteValueIntervalEvaluator(
                        signedPairEnv.refine(
                            nonCanonicalTwo.isNotEqualTo(pBool).cacheKey,
                            holds = true,
                        )
                    )
                    .evalInt(p)
            )
            .isEqualTo(IntInterval(0, 1))

        // 10. FloatAffineForm rejects NaN weights and negative errorBound
        val boundedEnv = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(0f, 10f))
        val boundedEval = RemoteValueIntervalEvaluator(boundedEnv)
        assertThat(
                FloatAffineForm(
                        mapOf(AffineTerm(x.cacheKey, isInt = false) to Double.NaN),
                        constant = 1.0,
                        errorBound = 0.0,
                    )
                    .evaluate(boundedEval)
            )
            .isNull()
        assertThat(
                FloatAffineForm(emptyMap(), constant = 1.0, errorBound = -1.0).evaluate(boundedEval)
            )
            .isNull()

        // 11. Custom bounded-range op over UNBOUNDED float must NOT narrow bounded sibling x
        val unboundedU = RemoteFloat.createNamedRemoteFloat("unboundedU", 0f)
        val customKey = RemoteOperationCacheKey.create(CustomBoundedOp.Bounded, unboundedU)
        val refinedFromCustom =
            boundedEnv.narrowComparison(
                IntervalComparison.GTE,
                customKey,
                x.cacheKey,
                asInt = false,
            )
        assertThat(RemoteValueIntervalEvaluator(refinedFromCustom).evalFloat(x))
            .isEqualTo(FloatInterval(0f, 10f))
    }

    private enum class CustomBoundedOp : RemoteIntervalOperation {
        Bounded;

        override val requiresBoundedFloatOperands: Boolean = true

        override fun toDebugString(args: List<RemoteStateCacheKey>): String = "bounded(u)"

        override fun evaluateFloatInterval(
            key: RemoteOperationCacheKey,
            evaluator: RemoteValueIntervalEvaluator,
        ): FloatInterval = FloatInterval(-1f, 1f)
    }
}
