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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteFloatOperationKey
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteIntArray
import androidx.compose.remote.creation.compose.state.RemoteNamedCacheKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateIdKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteStringArray
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.animatedRemoteFloat
import androidx.compose.remote.creation.compose.state.atan
import androidx.compose.remote.creation.compose.state.atan2
import androidx.compose.remote.creation.compose.state.cacheKey
import androidx.compose.remote.creation.compose.state.ceil
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.exp
import androidx.compose.remote.creation.compose.state.floor
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rand
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.round
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.selectIfGe
import androidx.compose.remote.creation.compose.state.selectIfGt
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.creation.compose.state.sign
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.document.AnimationEasing
import androidx.compose.remote.document.NanEncoding
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.packAnimationSpec
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntervalOptimizerTest {

    @Test
    fun prunesDeadSelectsAndComparisonsAcrossAllTypes() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val c1 = RemoteColor.createNamedRemoteColor("c1", 0xFFFF0000.toInt())
        val c2 = RemoteColor.createNamedRemoteColor("c2", 0xFF0000FF.toInt())
        val env =
            MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(10f, 20f))
                .withNamedInt("n", IntInterval(5, 10))

        // Boolean.select across Float, Int, String, Boolean, Color
        assertThat(x.isGreaterThan(5f.rf).select(x + 1f.rf, 999f.rf).optimizeWithIntervals(env))
            .isEqualTo(x + 1f.rf)
        assertThat(n.isLessThan(0.ri).select(1.ri, n + 2.ri).optimizeWithIntervals(env))
            .isEqualTo(n + 2.ri)
        assertThat(n.isGreaterThan(4.ri).select("yes".rs, "no".rs).optimizeWithIntervals(env))
            .isEqualTo("yes".rs)
        assertThat(
                n.isGreaterThan(4.ri)
                    .select(RemoteBoolean(true), x.isLessThan(15f.rf))
                    .optimizeWithIntervals(env)
            )
            .isEqualTo(RemoteBoolean(true))
        assertThat(n.isGreaterThan(4.ri).select(c1, c2).optimizeWithIntervals(env)).isEqualTo(c1)
        assertThat(n.isLessThan(0.ri).select(c1, c2).optimizeWithIntervals(env)).isEqualTo(c2)

        // selectIf* with affine self-differences
        assertThat(selectIfLt(x, x + 1f.rf, x * 2f.rf, 0f.rf).optimizeWithIntervals(env))
            .isEqualTo(x * 2f.rf)
        assertThat(selectIfGe(n + 2.ri, n, n + 1.ri, 0.ri).optimizeWithIntervals(env))
            .isEqualTo(n + 1.ri)

        // String selectIf* for both float and int comparisons
        assertThat(selectIfLt(x, 100f.rf, "small".rs, "big".rs).optimizeWithIntervals(env))
            .isEqualTo("small".rs)
        assertThat(selectIfGt(n, 100.ri, "huge".rs, "ok".rs).optimizeWithIntervals(env))
            .isEqualTo("ok".rs)

        // Provable comparison folding
        assertThat(x.isGreaterThan(0f.rf).optimizeWithIntervals(env)).isEqualTo(RemoteBoolean(true))
        assertThat(n.isLessThan(0.ri).optimizeWithIntervals(env)).isEqualTo(RemoteBoolean(false))
    }

    @Test
    fun pathSensitiveBranchRefinementIsolatesMemoAcrossBranchesAndRoot() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val env =
            MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(0f, 100f))
                .withNamedInt("n", IntInterval(0, 10))

        // Nested ladder prunes dead inner arms in both branches
        val ladder =
            x.isLessThan(10f.rf)
                .select(
                    ifTrue = x.isLessThan(20f.rf).select(1f.rf, 999f.rf),
                    ifFalse = x.isLessThan(5f.rf).select(888f.rf, 4f.rf),
                )
        val optimizedLadder = ladder.optimizeWithIntervals(env)
        assertThat(optimizedLadder).isEqualTo(x.isLessThan(10f.rf).select(1f.rf, 4f.rf))

        // Equality branch folds n inside true branch without affecting false branch
        val eqSelect = n.isEqualTo(5.ri).select(n + 10.ri, n + 20.ri)
        assertThat(eqSelect.optimizeWithIntervals(env))
            .isEqualTo(n.isEqualTo(5.ri).select(15.ri, n + 20.ri))

        // Shared DAG node must NOT leak true-branch refinement into sibling branch or outer root
        val shared = x.isLessThan(20f.rf).select(1f.rf, 2f.rf)
        val combined = x.isLessThan(10f.rf).select(shared + 10f.rf, shared + 20f.rf) + shared
        val optimizedCombined = combined.optimizeWithIntervals(env)
        assertThat(optimizedCombined)
            .isEqualTo(x.isLessThan(10f.rf).select(11f.rf, shared + 20f.rf) + shared)
    }

    @Test
    fun purityAndIeee754AndNamedBooleanGuards() {
        val unboundedX = RemoteFloat.createNamedRemoteFloat("u", 0f)
        val boundedX = RemoteFloat.createNamedRemoteFloat("b", 0f)
        val nonFoldableId = 42
        val nonFoldableZeroId = 43
        val env =
            MapIntervalEnvironment.EMPTY.withNamedFloat("b", FloatInterval(0f, 10f))
                .withNamedFloat("five", FloatInterval.point(5f))
                .withNamedFloat("one", FloatInterval.point(1f))
                .withNamedFloat("zero", FloatInterval.point(0f))
                .withStateIdFloat(nonFoldableId, FloatInterval.point(7f))
                .withNonFoldableStateId(nonFoldableId)
                .withStateIdFloat(nonFoldableZeroId, FloatInterval.point(0f))
                .withNonFoldableStateId(nonFoldableZeroId)

        // Unbounded IEEE-754 absorption must NOT constant-fold OR trigger comparison/select/math
        // simplifications (e.g., NaN/Infinity absorbed by FloatInterval.clamp or
        // FloatInterval.times)
        val unboundedClamp = clamp(value = unboundedX, min = 5f.rf, max = 5f.rf)
        assertThat(unboundedClamp.optimizeWithIntervals(env).hasConstantValue).isFalse()
        val nonFoldableZero = RemoteFloat(NanEncoding.asNan(nonFoldableZeroId))
        val unboundedZeroProd = unboundedX * nonFoldableZero
        assertThat(unboundedZeroProd.optimizeWithIntervals(env).hasConstantValue).isFalse()
        assertThat(unboundedZeroProd.isEqualTo(0f.rf).optimizeWithIntervals(env).hasConstantValue)
            .isFalse()
        assertThat(
                unboundedZeroProd
                    .isEqualTo(0f.rf)
                    .select(1f.rf, 2f.rf)
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()
        assertThat(
                selectIfLt(unboundedZeroProd, 1f.rf, 1f.rf, 2f.rf)
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()
        assertThat(min(unboundedZeroProd, 10f.rf).optimizeWithIntervals(env))
            .isEqualTo(min(unboundedZeroProd, 10f.rf))
        assertThat(clamp(0f.rf, 10f.rf, unboundedZeroProd).optimizeWithIntervals(env))
            .isEqualTo(clamp(0f.rf, 10f.rf, unboundedZeroProd))
        val c1 = RemoteColor.createNamedRemoteColor("c1", 0xFFFF0000.toInt())
        val c2 = RemoteColor.createNamedRemoteColor("c2", 0xFF0000FF.toInt())
        assertThat(lerp(c1, c2, unboundedZeroProd).optimizeWithIntervals(env))
            .isEqualTo(lerp(c1, c2, unboundedZeroProd))
        assertThat(lerp(c1, c1, unboundedX).optimizeWithIntervals(env))
            .isEqualTo(lerp(c1, c1, unboundedX))
        // Trig, rem, div, sign, exp, and abs on unbounded inputs can return NaN at runtime and
        // must NOT fold comparisons or enum lookups
        assertThat(sin(unboundedX).isLessThan(2f.rf).optimizeWithIntervals(env).hasConstantValue)
            .isFalse()
        assertThat(cos(unboundedX).isLessThan(2f.rf).optimizeWithIntervals(env).hasConstantValue)
            .isFalse()
        assertThat(
                (unboundedX % 5f.rf).isLessThan(10f.rf).optimizeWithIntervals(env).hasConstantValue
            )
            .isFalse()
        assertThat(atan(unboundedX).isLessThan(2f.rf).optimizeWithIntervals(env).hasConstantValue)
            .isFalse()
        assertThat(
                atan2(unboundedX, 1f.rf)
                    .isLessThan(4f.rf)
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()
        assertThat(
                sign(unboundedX)
                    .isLessThanOrEqualTo(1f.rf)
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()
        assertThat(
                exp(-abs(unboundedX))
                    .isLessThanOrEqualTo(1f.rf)
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()
        assertThat(
                (2f.rf / max(unboundedX, 1f.rf))
                    .isLessThanOrEqualTo(2f.rf)
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()
        assertThat(
                abs(unboundedX)
                    .isGreaterThanOrEqualTo(0f.rf)
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()
        assertThat(
                RemoteEnum(clamp(unboundedX, 1f.rf, 1f.rf).toRemoteInt(), TestEnum.entries)
                    .toRemoteString()
                    .optimizeWithIntervals(env)
                    .hasConstantValue
            )
            .isFalse()

        // Bounded operands DO fold cleanly
        assertThat(clamp(value = boundedX, min = 5f.rf, max = 5f.rf).optimizeWithIntervals(env))
            .isEqualTo(5f.rf)

        // Non-deterministic & stateful expressions must never fold
        assertThat(rand().optimizeWithIntervals(env).hasConstantValue).isFalse()
        assertThat((rand() - rand()).optimizeWithIntervals(env).hasConstantValue).isFalse()

        val spec = packAnimationSpec(duration = 0.5f, type = AnimationEasing.CUBIC_STANDARD)
        assertThat(animatedRemoteFloat(5f.rf, spec).optimizeWithIntervals(env).hasConstantValue)
            .isFalse()

        // forceRemote = true reference must not fold
        val forced = (boundedX - boundedX).createReference(forceRemote = true)
        assertThat(forced.optimizeWithIntervals(env).hasConstantValue).isFalse()

        // MutableRemoteFloat (RemoteStateInstanceKey) must not fold
        val mutableFloat = MutableRemoteFloat(5f)
        assertThat(mutableFloat.optimizeWithIntervals(env)).isSameInstanceAs(mutableFloat)

        // nonFoldableStateIds must not fold
        val nonFoldableState = RemoteFloat(NanEncoding.asNan(nonFoldableId))
        assertThat(nonFoldableState.optimizeWithIntervals(env).hasConstantValue).isFalse()

        // Named RemoteBoolean probe: only fold exact 0f / 1f, never non-boolean float 5f
        val namedFive = RemoteBoolean.createNamedRemoteBoolean("five", false)
        assertThat(namedFive.optimizeWithIntervals(env).hasConstantValue).isFalse()
        assertThat(RemoteBoolean.createNamedRemoteBoolean("one", false).optimizeWithIntervals(env))
            .isEqualTo(RemoteBoolean(true))
        assertThat(RemoteBoolean.createNamedRemoteBoolean("zero", true).optimizeWithIntervals(env))
            .isEqualTo(RemoteBoolean(false))
    }

    @Test
    fun trappingOperandsInsideEagerBranchesUseUnrefinedEnvironment() {
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val x = RemoteInt.createNamedRemoteInt("x", 0)
        val env =
            MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(-5, 5))
                .withNamedInt("x", IntInterval(0, 100))

        // Inside n > 0 branch, max(n, 1) is the divisor of x / max(n, 1).
        // Because select evaluates both arms eagerly, max(n, 1) must NOT be simplified to n
        // (which would divide by 0 when n == 0)!
        val guardedDiv = n.isGreaterThan(0.ri).select(x / max(n, 1.ri), 0.ri)
        val optimizedDiv = guardedDiv.optimizeWithIntervals(env)
        assertThat(optimizedDiv).isEqualTo(guardedDiv)

        // Same for array index inside an eager branch: clamp(n, 0, 1) must NOT drop its lower
        // clamp bound using the branch-only assumption n >= 0!
        val arr = RemoteFloatArray(10f, 20f, 30f)
        val guardedLookup = n.isGreaterThanOrEqualTo(0.ri).select(arr[clamp(0.ri, 1.ri, n)], 0f.rf)
        val optimizedLookup = guardedLookup.optimizeWithIntervals(env)
        assertThat(optimizedLookup).isEqualTo(guardedLookup)

        // Same for Substring indices inside an eager branch: max(n, 0) must NOT simplify to n!
        val s = RemoteString.createNamedRemoteString("s", "abcdef")
        val guardedSub =
            n.isGreaterThanOrEqualTo(0.ri).select(s.substring(max(n, 0.ri), 2.ri), "".rs)
        assertThat(guardedSub.optimizeWithIntervals(env)).isEqualTo(guardedSub)
    }

    @Test
    fun simplifiesRangeMathAndLiteralArrayOperations() {
        val pos = RemoteFloat.createNamedRemoteFloat("pos", 0f)
        val neg = RemoteFloat.createNamedRemoteFloat("neg", 0f)
        val posInt = RemoteInt.createNamedRemoteInt("posInt", 0)
        val negInt = RemoteInt.createNamedRemoteInt("negInt", 0)
        val idx01 = RemoteInt.createNamedRemoteInt("idx01", 0)
        val idxPoint = RemoteInt.createNamedRemoteInt("idxPoint", 0)
        val floatIdx01 = RemoteFloat.createNamedRemoteFloat("fIdx01", 0f)
        val env =
            MapIntervalEnvironment.EMPTY.withNamedFloat("pos", FloatInterval(2f, 8f))
                .withNamedFloat("neg", FloatInterval(-8f, -2f))
                .withNamedInt("posInt", IntInterval(2, 8))
                .withNamedInt("negInt", IntInterval(-8, -2))
                .withNamedInt("idx01", IntInterval(0, 1))
                .withNamedInt("idxPoint", IntInterval.point(1))
                .withNamedFloat("fIdx01", FloatInterval(0f, 1.5f))

        // Float abs, min, max, clamp, integer-valued rounding
        assertThat(abs(pos).optimizeWithIntervals(env)).isEqualTo(pos)
        assertThat(abs(neg).optimizeWithIntervals(env)).isEqualTo(-neg)
        assertThat(min(pos, pos + 5f.rf).optimizeWithIntervals(env)).isEqualTo(pos)
        assertThat(max(pos, 1f.rf).optimizeWithIntervals(env)).isEqualTo(pos)
        assertThat(clamp(value = pos, min = 0f.rf, max = 10f.rf).optimizeWithIntervals(env))
            .isEqualTo(pos)
        assertThat(clamp(value = pos, min = 10f.rf, max = 20f.rf).optimizeWithIntervals(env))
            .isEqualTo(10f.rf)
        assertThat(clamp(value = pos, min = 0f.rf, max = 1f.rf).optimizeWithIntervals(env))
            .isEqualTo(1f.rf)
        assertThat(clamp(value = pos, min = 0f.rf, max = 5f.rf).optimizeWithIntervals(env))
            .isEqualTo(min(pos, 5f.rf))
        assertThat(clamp(value = pos, min = 5f.rf, max = 10f.rf).optimizeWithIntervals(env))
            .isEqualTo(max(pos, 5f.rf))
        assertThat(floor(posInt.toRemoteFloat()).optimizeWithIntervals(env))
            .isEqualTo(posInt.toRemoteFloat())
        assertThat(ceil(posInt.toRemoteFloat()).optimizeWithIntervals(env))
            .isEqualTo(posInt.toRemoteFloat())
        assertThat(round(posInt.toRemoteFloat()).optimizeWithIntervals(env))
            .isEqualTo(posInt.toRemoteFloat())

        // OP_ROUND clamps floats >= 2^31 to Int.MAX_VALUE:
        // - For [3e9f, 4e9f], round(floor(huge)) must NOT strip to floor(huge); it folds to the
        //   clamped constant Int.MAX_VALUE.toFloat().
        // - For [0f, 4e9f] (non-constant after clamping), round(floor(straddle)) stays intact.
        val hugeFloat = RemoteFloat.createNamedRemoteFloat("huge", 0f)
        val straddleHuge = RemoteFloat.createNamedRemoteFloat("straddleHuge", 0f)
        val hugeEnv =
            MapIntervalEnvironment.EMPTY.withNamedFloat("huge", FloatInterval(3e9f, 4e9f))
                .withNamedFloat("straddleHuge", FloatInterval(0f, 4e9f))
        val floorHuge = floor(hugeFloat)
        assertThat(round(floorHuge).optimizeWithIntervals(hugeEnv))
            .isEqualTo(Int.MAX_VALUE.toFloat().rf)
        val floorStraddle = floor(straddleHuge)
        assertThat(round(floorStraddle).optimizeWithIntervals(hugeEnv))
            .isEqualTo(round(floorStraddle))
        assertThat(floor(floorHuge).optimizeWithIntervals(hugeEnv)).isEqualTo(floorHuge)
        assertThat(ceil(floorHuge).optimizeWithIntervals(hugeEnv)).isEqualTo(floorHuge)

        // Color Tween and TweenInt endpoint & identical-color simplification
        val c1 = RemoteColor.createNamedRemoteColor("c1", 0xFFFF0000.toInt())
        val c2 = RemoteColor.createNamedRemoteColor("c2", 0xFF0000FF.toInt())
        val tZero = RemoteFloat.createNamedRemoteFloat("tZero", 0f)
        val tOne = RemoteFloat.createNamedRemoteFloat("tOne", 1f)
        val tMid = RemoteFloat.createNamedRemoteFloat("tMid", 0.5f)
        val colorEnv =
            MapIntervalEnvironment.EMPTY.withNamedFloat("tZero", FloatInterval.point(0f))
                .withNamedFloat("tOne", FloatInterval.point(1f))
                .withNamedFloat("tMid", FloatInterval(0f, 1f))
        assertThat(lerp(c1, c2, tZero).optimizeWithIntervals(colorEnv)).isEqualTo(c1)
        assertThat(lerp(c1, c2, tOne).optimizeWithIntervals(colorEnv)).isEqualTo(c2)
        assertThat(lerp(c1, c1, tMid).optimizeWithIntervals(colorEnv)).isEqualTo(c1)
        assertThat(
                lerp(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), tZero).optimizeWithIntervals(colorEnv)
            )
            .isEqualTo(RemoteColor(0xFFFF0000.toInt()))
        assertThat(
                lerp(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), tOne).optimizeWithIntervals(colorEnv)
            )
            .isEqualTo(RemoteColor(0xFF0000FF.toInt()))

        // Int abs, min, max, clamp
        assertThat(posInt.absoluteValue.optimizeWithIntervals(env)).isEqualTo(posInt)
        assertThat(negInt.absoluteValue.optimizeWithIntervals(env)).isEqualTo(-negInt)
        assertThat(min(posInt, 100.ri).optimizeWithIntervals(env)).isEqualTo(posInt)
        assertThat(max(posInt, 0.ri).optimizeWithIntervals(env)).isEqualTo(posInt)
        assertThat(clamp(0.ri, 10.ri, posInt).optimizeWithIntervals(env)).isEqualTo(posInt)
        assertThat(clamp(0.ri, 5.ri, posInt).optimizeWithIntervals(env))
            .isEqualTo(min(posInt, 5.ri))

        // Literal array point-index folding (with dynamic elements optimized bottom-up)
        val dynArr = RemoteFloatArray(listOf(pos, abs(pos)))
        assertThat(dynArr[idxPoint].optimizeWithIntervals(env)).isEqualTo(pos)

        val intArr = RemoteIntArray(listOf(posInt, posInt + 1.ri))
        assertThat(intArr[idxPoint].optimizeWithIntervals(env)).isEqualTo(posInt + 1.ri)

        val strArr = RemoteStringArray(listOf("AM".rs, "PM".rs))
        assertThat(strArr[idxPoint].optimizeWithIntervals(env)).isEqualTo("PM".rs)

        // 2-element literal array lowering to select on [0, 1]
        assertThat(dynArr[idx01].optimizeWithIntervals(env))
            .isEqualTo(pos) // identical after abs(pos) -> pos!
        val distinctFloatArr = RemoteFloatArray(listOf(pos, neg))
        assertThat(distinctFloatArr[idx01].optimizeWithIntervals(env))
            .isEqualTo(RemoteBoolean.fromRemoteInt(idx01).select(neg, pos))
        assertThat(distinctFloatArr[floatIdx01].optimizeWithIntervals(env))
            .isEqualTo(selectIfLt(floatIdx01, 1f.rf, pos, neg))
        assertThat(intArr[idx01].optimizeWithIntervals(env))
            .isEqualTo(RemoteBoolean.fromRemoteInt(idx01).select(posInt + 1.ri, posInt))
        assertThat(intArr[floatIdx01].optimizeWithIntervals(env))
            .isEqualTo(floatIdx01.isLessThan(1f.rf).select(posInt, posInt + 1.ri))
        assertThat(strArr[idx01].optimizeWithIntervals(env))
            .isEqualTo(RemoteBoolean.fromRemoteInt(idx01).select("PM".rs, "AM".rs))

        // Branch-narrowed RemoteInt index into RemoteFloatArray picks up intOverride
        val nBranch = RemoteInt.createNamedRemoteInt("nBranch", 0)
        val branchArrEnv = MapIntervalEnvironment.EMPTY.withNamedInt("nBranch", IntInterval(0, 1))
        val branchArrExpr = nBranch.isEqualTo(1.ri).select(distinctFloatArr[nBranch], 0f.rf)
        assertThat(branchArrExpr.optimizeWithIntervals(branchArrEnv))
            .isEqualTo(nBranch.isEqualTo(1.ri).select(neg, 0f.rf))

        // Config switches independently disable each optimization phase
        val noLower = IntervalOptimizerConfig(lowerTwoElementArraysToSelect = false)
        assertThat(strArr[idx01].optimizeWithIntervals(env, noLower)).isEqualTo(strArr[idx01])
        assertThat(strArr[idxPoint].optimizeWithIntervals(env, noLower)).isEqualTo("PM".rs)

        val noFoldArr =
            IntervalOptimizerConfig(
                foldArrayGets = false,
                lowerTwoElementArraysToSelect = false,
                foldPointConstants = false,
            )
        assertThat(distinctFloatArr[idxPoint].optimizeWithIntervals(env, noFoldArr))
            .isEqualTo(distinctFloatArr[idxPoint])
        val onlyFoldArr =
            IntervalOptimizerConfig(
                foldArrayGets = true,
                lowerTwoElementArraysToSelect = false,
                foldPointConstants = false,
            )
        assertThat(distinctFloatArr[idxPoint].optimizeWithIntervals(env, onlyFoldArr))
            .isEqualTo(neg)

        val noPrune =
            IntervalOptimizerConfig(
                pruneTernaries = false,
                foldComparisons = false,
                foldPointConstants = false,
            )
        val deadSel = posInt.isGreaterThan(0.ri).select(posInt, negInt)
        assertThat(deadSel.optimizeWithIntervals(env, noPrune)).isEqualTo(deadSel)

        val noRefine = IntervalOptimizerConfig(refineTernaryBranches = false)
        val refSel = idx01.isEqualTo(1.ri).select(idx01 + 10.ri, 0.ri)
        assertThat(refSel.optimizeWithIntervals(env, noRefine)).isEqualTo(refSel)

        val noFoldCmp = IntervalOptimizerConfig(foldComparisons = false, foldPointConstants = false)
        assertThat(posInt.isGreaterThan(0.ri).optimizeWithIntervals(env, noFoldCmp))
            .isEqualTo(posInt.isGreaterThan(0.ri))

        val noRangeMath = IntervalOptimizerConfig(simplifyRangeMath = false)
        assertThat(abs(pos).optimizeWithIntervals(env, noRangeMath)).isEqualTo(abs(pos))

        val noPointFold = IntervalOptimizerConfig(foldPointConstants = false)
        assertThat((pos - pos).optimizeWithIntervals(env, noPointFold)).isEqualTo(pos - pos)
    }

    @Test
    fun namedBooleanRefinementAndCustomRuleHooks() {
        // Path-sensitive refinement of a named RemoteBoolean folds inner uses via intOverride
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", false)
        val flagEnv = MapIntervalEnvironment.EMPTY.withNamedInt("flag", IntInterval(0, 1))
        val selfSelect = flag.select(flag, RemoteBoolean(false))
        assertThat(selfSelect.optimizeWithIntervals(flagEnv))
            .isEqualTo(flag.select(RemoteBoolean(true), RemoteBoolean(false)))
        val nestedFlagSelect = flag.select(flag.select(10.ri, 20.ri), 30.ri)
        assertThat(nestedFlagSelect.optimizeWithIntervals(flagEnv))
            .isEqualTo(flag.select(10.ri, 30.ri))

        // First-class boolean connective & identity simplification
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val nEnv = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(0, 10))
        assertThat((n.isGreaterThan(5.ri) and n.isLessThan(3.ri)).optimizeWithIntervals(nEnv))
            .isEqualTo(RemoteBoolean(false))
        assertThat(
                (n.isGreaterThan(5.ri) or n.isLessThanOrEqualTo(5.ri)).optimizeWithIntervals(nEnv)
            )
            .isEqualTo(RemoteBoolean(true))
        assertThat((flag and true.rb).optimizeWithIntervals(flagEnv)).isEqualTo(flag)
        assertThat((flag or false.rb).optimizeWithIntervals(flagEnv)).isEqualTo(flag)
        assertThat((flag xor true.rb).optimizeWithIntervals(flagEnv)).isEqualTo(!flag)

        // Non-canonical RemoteBoolean(pos) must NOT fold to RemoteBoolean(true) (which would
        // corrupt toRemoteInt()), while select still prunes dead false branch.
        val pos = RemoteInt.createNamedRemoteInt("pos", 1)
        val negPoint = RemoteInt.createNamedRemoteInt("negPoint", -1)
        val twoPoint = RemoteInt.createNamedRemoteInt("twoPoint", 2)
        val posEnv =
            MapIntervalEnvironment.EMPTY.withNamedInt("pos", IntInterval(1, 5))
                .withNamedInt("negPoint", IntInterval.point(-1))
                .withNamedInt("twoPoint", IntInterval.point(2))
        val nonCanonical = RemoteBoolean.fromRemoteInt(pos)
        assertThat(nonCanonical.toRemoteInt().optimizeWithIntervals(posEnv)).isEqualTo(pos)
        assertThat(nonCanonical.select(10f.rf, 20f.rf).optimizeWithIntervals(posEnv))
            .isEqualTo(10f.rf)
        // Point negative or > 1 RemoteInt wrapped in RemoteBoolean must NOT fold via reconstruct
        // into RemoteBoolean(true) or prune negative conditions to the true branch!
        val negBool = RemoteBoolean.fromRemoteInt(negPoint)
        assertThat(negBool.optimizeWithIntervals(posEnv).hasConstantValue).isFalse()
        assertThat(negBool.select(10f.rf, 20f.rf).optimizeWithIntervals(posEnv).hasConstantValue)
            .isFalse()
        assertThat(negBool.toRemoteInt().optimizeWithIntervals(posEnv)).isEqualTo((-1).ri)
        val twoBool = RemoteBoolean.fromRemoteInt(twoPoint)
        assertThat(twoBool.optimizeWithIntervals(posEnv).hasConstantValue).isFalse()
        assertThat(twoBool.toRemoteInt().optimizeWithIntervals(posEnv)).isEqualTo(2.ri)
        assertThat(twoBool.select(10f.rf, 20f.rf).optimizeWithIntervals(posEnv)).isEqualTo(10f.rf)

        // First-class RemoteEnum folding and ordinal/toRemoteString simplification
        val idxPoint = RemoteInt.createNamedRemoteInt("idxPoint", 0)
        val enumEnv =
            MapIntervalEnvironment.EMPTY.withNamedInt("idxPoint", IntInterval.point(1))
                .withNamedInt("n", IntInterval(0, 2))
        assertThat(RemoteEnum(idxPoint, TestEnum.entries).optimizeWithIntervals(enumEnv))
            .isEqualTo(RemoteEnum(TestEnum.B))
        assertThat(RemoteEnum(n + 1.ri, TestEnum.entries).ordinal.optimizeWithIntervals(enumEnv))
            .isEqualTo(n + 1.ri)
        assertThat(
                RemoteEnum(idxPoint, TestEnum.entries)
                    .toRemoteString()
                    .optimizeWithIntervals(enumEnv)
            )
            .isEqualTo("B".rs)

        // Custom rule triggers on named RemoteBoolean
        val forceFlagTrue =
            object : IntervalOptimizationRule {
                override fun rewriteAfterChildren(
                    value: RemoteValue<*>,
                    context: IntervalOptimizationContext,
                ): RemoteValue<*>? =
                    if (
                        value is RemoteBoolean &&
                            (value.cacheKey as? RemoteNamedCacheKey)?.name == "flag"
                    ) {
                        RemoteBoolean(true)
                    } else {
                        null
                    }
            }
        assertThat(flag.optimizeWithIntervals(flagEnv, rules = listOf(forceFlagTrue)))
            .isEqualTo(RemoteBoolean(true))

        // rewriteBeforeChildren & optimizeUnrefined hooks + MutableRemoteFloat subtype preservation
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val env = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(-5f, 5f))
        var sawRefinedBranch = false
        val beforeRule =
            object : IntervalOptimizationRule {
                override fun rewriteBeforeChildren(
                    value: RemoteValue<*>,
                    context: IntervalOptimizationContext,
                ): RemoteValue<*>? {
                    if (context.isInRefinedBranch && value == abs(x)) {
                        sawRefinedBranch = true
                        return context.optimizeUnrefined(value)
                    }
                    return null
                }
            }
        val branchWithAbs = x.isGreaterThan(0f.rf).select(abs(x), 0f.rf)
        assertThat(branchWithAbs.optimizeWithIntervals(env, rules = listOf(beforeRule)))
            .isEqualTo(branchWithAbs)
        assertThat(sawRefinedBranch).isTrue()

        val mutable = MutableRemoteFloat(1f)
        val badRuleReturningImmutable =
            object : IntervalOptimizationRule {
                override fun rewriteAfterChildren(
                    value: RemoteValue<*>,
                    context: IntervalOptimizationContext,
                ): RemoteValue<*>? = if (value === mutable) 2f.rf else null
            }
        assertThat(mutable.optimizeWithIntervals(env, rules = listOf(badRuleReturningImmutable)))
            .isSameInstanceAs(mutable)
    }

    @Test
    fun supportsCustomRulesAndCustomRemoteOperations() {
        // 1. Custom IntervalOptimizationRule lowering continuous seconds to discrete seconds
        val clockDowngradeRule =
            object : IntervalOptimizationRule {
                override fun rewriteAfterChildren(
                    value: RemoteValue<*>,
                    context: IntervalOptimizationContext,
                ): RemoteValue<*>? {
                    val idKey = value.cacheKey as? RemoteStateIdKey ?: return null
                    if (idKey.id == RemoteContext.ID_CONTINUOUS_SEC && value is RemoteFloat) {
                        return RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC)
                    }
                    return null
                }
            }

        val continuous = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)
        val expr = floor(continuous + 1f.rf)
        val optimized = expr.optimizeWithIntervals(rules = listOf(clockDowngradeRule))
        // After lowering continuous -> FLOAT_TIME_IN_SEC (which is integer-valued and finite!),
        // floor(time_in_sec + 1f) also simplifies away the redundant floor()!
        assertThat(optimized).isEqualTo(RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC) + 1f.rf)

        // 2. Custom RemoteIntervalOperation plugs directly into evaluator & optimizer with zero
        // changes
        // to IntervalOptimizer
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val env = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(2f, 4f))
        val customExpr =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteOperationCacheKey.create(TestCustomOp.IdentityIfPositive, x),
                arrayProvider = { FloatArray(0) },
            )
        assertThat(customExpr.optimizeWithIntervals(env)).isEqualTo(x)

        // When x can be negative, custom op stays intact
        val mixedEnv = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(-4f, 4f))
        assertThat(customExpr.optimizeWithIntervals(mixedEnv)).isEqualTo(customExpr)
    }

    @Test
    fun hasBoundedFloatOperands_handlesDeepSubtreesAndMemoizesWithoutOverflow() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val u = RemoteFloat.createNamedRemoteFloat("u", 0f)
        val env = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(1f, 2f))
        val evaluator = RemoteValueIntervalEvaluator(env)

        // Build a 30-level deep addition chain with no ops requiring bounded float operands.
        var deepPositive: RemoteFloat = x
        repeat(30) { deepPositive = deepPositive + 1f.rf }
        assertThat(IntervalOptimizer.hasBoundedFloatOperands(deepPositive.cacheKey, evaluator))
            .isTrue()
        // abs(deepPositive) simplifies away abs() cleanly.
        assertThat(abs(deepPositive).optimizeWithIntervals(env)).isEqualTo(deepPositive)

        // Nested op requiring bounded float operands must still block folding on unbounded input.
        val nestedUnboundedClamp = clamp(value = u, min = 5f.rf, max = 5f.rf) + 1f.rf
        assertThat(
                IntervalOptimizer.hasBoundedFloatOperands(nestedUnboundedClamp.cacheKey, evaluator)
            )
            .isFalse()
        assertThat(nestedUnboundedClamp.optimizeWithIntervals(env).hasConstantValue).isFalse()

        // Combining a shallow bounded op with a deep sibling succeeds cleanly.
        val boundedOpWithDeepSibling = (x * 2f.rf) + deepPositive
        assertThat(
                IntervalOptimizer.hasBoundedFloatOperands(
                    boundedOpWithDeepSibling.cacheKey,
                    evaluator,
                )
            )
            .isTrue()

        // Iterative traversal handles very deep chains (2000 levels) without stack overflow.
        var veryDeepBounded: RemoteStateCacheKey = x.cacheKey
        repeat(2000) {
            veryDeepBounded =
                RemoteOperationCacheKey(
                    RemoteFloatOperationKey.Plus,
                    listOf(veryDeepBounded, RemoteConstantCacheKey(1f)),
                )
        }
        assertThat(IntervalOptimizer.hasBoundedFloatOperands(veryDeepBounded, evaluator)).isTrue()

        var veryDeepUnbounded: RemoteStateCacheKey = (u * 2f.rf).cacheKey
        repeat(2000) {
            veryDeepUnbounded =
                RemoteOperationCacheKey(
                    RemoteFloatOperationKey.Plus,
                    listOf(veryDeepUnbounded, RemoteConstantCacheKey(1f)),
                )
        }
        assertThat(IntervalOptimizer.hasBoundedFloatOperands(veryDeepUnbounded, evaluator))
            .isFalse()
    }

    @Test
    fun soundnessRegressions_nonCanonicalBooleanUnboundedBranchRefinementAndMemoIsolation() {
        // 1. Non-canonical RemoteBoolean operations (Not/And/Or/Xor) must NOT fold to 1 (true),
        //    which would corrupt .toRemoteInt() (e.g. (!2) = 3, not 1) and isEqualTo(true.rb).
        val two = RemoteInt.createNamedRemoteInt("two", 2)
        val three = RemoteInt.createNamedRemoteInt("three", 3)
        val nonCanonEnv =
            MapIntervalEnvironment.EMPTY.withNamedInt("two", IntInterval.point(2))
                .withNamedInt("three", IntInterval.point(3))
        val twoBool = RemoteBoolean.fromRemoteInt(two)
        val threeBool = RemoteBoolean.fromRemoteInt(three)
        assertThat((!twoBool).optimizeWithIntervals(nonCanonEnv).hasConstantValue).isFalse()
        assertThat((!twoBool).toRemoteInt().optimizeWithIntervals(nonCanonEnv)).isEqualTo(3.ri)
        assertThat((threeBool and twoBool).optimizeWithIntervals(nonCanonEnv).hasConstantValue)
            .isFalse()
        assertThat((threeBool and twoBool).toRemoteInt().optimizeWithIntervals(nonCanonEnv))
            .isEqualTo(2.ri)

        // 2. Branch refinement must not narrow sibling x under sin(unboundedU) < x
        val u = RemoteFloat.createNamedRemoteFloat("u", 0f)
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val branchEnv = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(0f, 10f))
        val selectWithUnboundedSin =
            selectIfLt(
                sin(u),
                x,
                1f.rf,
                x.isLessThanOrEqualTo(1f.rf).select(1f.rf, 999f.rf),
            )
        assertThat(selectWithUnboundedSin.optimizeWithIntervals(branchEnv).hasConstantValue)
            .isFalse()

        // 3. Distinct RemoteEnum types wrapping the same RemoteInt do not collide in optimizeMemo
        val idx = RemoteInt.createNamedRemoteInt("idx", 1)
        val enumEnv = MapIntervalEnvironment.EMPTY.withNamedInt("idx", IntInterval.point(1))
        val opt = IntervalOptimizer(enumEnv)
        val e1 = RemoteEnum(idx, TestEnum.entries)
        val e2 = RemoteEnum(idx, OtherTestEnum.entries)
        assertThat(opt.optimize(e1).constantValueOrNull).isEqualTo(TestEnum.B)
        assertThat(opt.optimize(e2).constantValueOrNull).isEqualTo(OtherTestEnum.Y)

        // 4. Custom IntervalEnvironment implementing only intForNamed folds named RemoteBoolean
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", false)
        val customIntEnv =
            object : IntervalEnvironment {
                override fun intForNamed(key: RemoteNamedCacheKey): IntInterval =
                    if (key.name == "flag") IntInterval.point(1) else IntInterval.UNBOUNDED
            }
        assertThat(flag.optimizeWithIntervals(customIntEnv)).isEqualTo(RemoteBoolean(true))

        // 5. Pruned ternary branch invokes rewriteAfterChildren at most once per node
        var afterCount = 0
        val countRule =
            object : IntervalOptimizationRule {
                override fun rewriteAfterChildren(
                    value: RemoteValue<*>,
                    context: IntervalOptimizationContext,
                ): RemoteValue<*>? {
                    if (value == x) afterCount++
                    return null
                }
            }
        val cond = RemoteBoolean.createNamedRemoteBoolean("cond", true)
        val prunedSel = cond.select(x, x)
        prunedSel.optimizeWithIntervals(branchEnv, rules = listOf(countRule))
        assertThat(afterCount).isEqualTo(1)
    }

    private enum class TestEnum {
        A,
        B,
        C,
    }

    private enum class OtherTestEnum {
        X,
        Y,
    }

    private enum class TestCustomOp : RemoteIntervalOperation {
        IdentityIfPositive {
            override fun toDebugString(args: List<RemoteStateCacheKey>): String =
                "idIfPos(${args[0].toDebugString()})"

            override fun evaluateFloatInterval(
                key: RemoteOperationCacheKey,
                evaluator: RemoteValueIntervalEvaluator,
            ): FloatInterval = evaluator.evalFloatKey(key.args[0])

            override fun simplifyWithIntervals(
                value: RemoteValue<*>,
                key: RemoteOperationCacheKey,
                context: IntervalOptimizationContext,
            ): RemoteValue<*>? {
                val r = context.evaluator.evalFloatKey(key.args[0])
                return if (!r.isEmpty && !r.isUnbounded && r.min > 0f) {
                    key.args[0].getOrCreateValueOrNull()
                } else {
                    null
                }
            }
        }
    }
}
