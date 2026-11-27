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
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteIntArray
import androidx.compose.remote.creation.compose.state.RemoteIntOperationKey
import androidx.compose.remote.creation.compose.state.RemoteNamedCacheKey
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteStringArray
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.acos
import androidx.compose.remote.creation.compose.state.animatedRemoteFloat
import androidx.compose.remote.creation.compose.state.asin
import androidx.compose.remote.creation.compose.state.atan
import androidx.compose.remote.creation.compose.state.atan2
import androidx.compose.remote.creation.compose.state.cacheKey
import androidx.compose.remote.creation.compose.state.cbrt
import androidx.compose.remote.creation.compose.state.ceil
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.copySign
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.cubicEasing
import androidx.compose.remote.creation.compose.state.exp
import androidx.compose.remote.creation.compose.state.floor
import androidx.compose.remote.creation.compose.state.joinToDebugString
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.ln
import androidx.compose.remote.creation.compose.state.log
import androidx.compose.remote.creation.compose.state.mad
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.pow
import androidx.compose.remote.creation.compose.state.rand
import androidx.compose.remote.creation.compose.state.randRange
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.round
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.creation.compose.state.sign
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.remote.document.AnimationEasing
import androidx.compose.remote.document.TextFromFloatFlags
import androidx.compose.remote.document.intervals.ColorInterval
import androidx.compose.remote.document.intervals.FloatInterval
import androidx.compose.remote.document.intervals.IntInterval
import androidx.compose.remote.document.intervals.StringSummary
import androidx.compose.remote.document.intervals.TriState
import androidx.compose.remote.document.packAnimationSpec
import com.google.common.truth.Truth.assertThat
import kotlin.math.PI
import kotlin.math.sqrt as mathSqrt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteValueIntervalEvaluatorTest {

    @Test
    fun floatOperationsAndSystemGlobals() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val env = MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(10f, 20f))
        val eval = RemoteValueIntervalEvaluator(env)

        assertThat(eval.evalFloat(x * 2f.rf + 5f.rf)).isEqualTo(FloatInterval(25f, 45f))
        assertThat(eval.evalFloat(clamp(value = x, min = 12f.rf, max = 18f.rf)))
            .isEqualTo(FloatInterval(12f, 18f))
        assertThat(eval.evalFloat(lerp(0f.rf, 100f.rf, (x - 10f.rf) / 10f.rf)))
            .isEqualTo(FloatInterval(0f, 100f))
        assertThat(eval.evalFloat(sin(x))).isEqualTo(FloatInterval(-1f, 1f))
        assertThat(eval.evalFloat(rand()))
            .isEqualTo(FloatInterval(0f, 1f, minInclusive = true, maxInclusive = false))

        // Animated float bounds & cubic easing
        val spec = packAnimationSpec(duration = 0.5f, type = AnimationEasing.CUBIC_STANDARD)
        assertThat(eval.evalFloat(animatedRemoteFloat(x, spec))).isEqualTo(FloatInterval(10f, 20f))

        val eased = cubicEasing(0.4f.rf, 0f.rf, 0.2f.rf, 1f.rf, (x - 10f.rf) / 10f.rf)
        assertThat(eval.evalFloat(eased)).isEqualTo(FloatInterval.UNIT)

        // Built-in system time global
        assertThat(eval.evalInt(RemoteTimeVariables.hourOfDay)).isEqualTo(IntInterval(0, 23))
    }

    @Test
    fun intBooleanAndSelectOperations() {
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val env = MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(5, 10))
        val eval = RemoteValueIntervalEvaluator(env)

        assertThat(eval.evalInt(n * 3.ri - 2.ri)).isEqualTo(IntInterval(13, 28))
        assertThat(eval.evalBoolean(n.isGreaterThan(4.ri))).isEqualTo(TriState.TRUE)
        assertThat(eval.evalBoolean(n.isLessThan(0.ri))).isEqualTo(TriState.FALSE)
        assertThat(eval.evalBoolean(n.isLessThan(8.ri))).isEqualTo(TriState.UNKNOWN)
        assertThat(eval.evalBoolean(!n.isGreaterThan(4.ri))).isEqualTo(TriState.FALSE)
        assertThat(eval.evalBoolean(n.isGreaterThan(4.ri) and n.isLessThan(20.ri)))
            .isEqualTo(TriState.TRUE)

        // Select prunes statically dead arm during interval evaluation
        assertThat(eval.evalFloat(n.isGreaterThan(4.ri).select(100f.rf, 999f.rf)))
            .isEqualTo(FloatInterval.point(100f))
        assertThat(eval.evalInt(selectIfLt(n, 0.ri, 1.ri, 2.ri))).isEqualTo(IntInterval.point(2))
    }

    private enum class TestMode {
        OFF,
        LOW,
        HIGH,
    }

    @Test
    fun firstClassBooleanAndEnumOperations() {
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", false)
        val unboundFlag = RemoteBoolean.createNamedRemoteBoolean("unboundFlag", false)
        val pos = RemoteInt.createNamedRemoteInt("pos", 1)
        val gt1 = RemoteInt.createNamedRemoteInt("gt1", 2)
        val env =
            MapIntervalEnvironment.EMPTY.withNamedInt("n", IntInterval(5, 10))
                .withNamedInt("flag", IntInterval.point(1))
                .withNamedInt("pos", IntInterval(1, 5))
                .withNamedInt("gt1", IntInterval(2, 5))
                .withNamedInt("mode", IntInterval(0, 1))
        val eval = RemoteValueIntervalEvaluator(env)

        // First-class RemoteBoolean ops
        val gt4 = n.isGreaterThan(4.ri)
        val lt0 = n.isLessThan(0.ri)
        assertThat(eval.evalBoolean(flag)).isEqualTo(TriState.TRUE)
        assertThat(eval.evalBoolean(!flag)).isEqualTo(TriState.FALSE)
        assertThat(eval.evalBoolean(gt4 or unboundFlag)).isEqualTo(TriState.TRUE)
        assertThat(eval.evalBoolean(lt0 and unboundFlag)).isEqualTo(TriState.FALSE)
        assertThat(eval.evalBoolean(gt4 xor lt0)).isEqualTo(TriState.TRUE)
        assertThat(eval.evalBoolean(gt4.isEqualTo(flag))).isEqualTo(TriState.TRUE)
        assertThat(eval.evalBoolean(gt4.isNotEqualTo(lt0))).isEqualTo(TriState.TRUE)
        assertThat(eval.evalInt(gt4.toRemoteInt())).isEqualTo(IntInterval.point(1))
        assertThat(eval.evalInt(lt0.toRemoteInt())).isEqualTo(IntInterval.ZERO)

        // Non-canonical RemoteBoolean(int) preserves soundness
        val nonCanonicalPos = RemoteBoolean.fromRemoteInt(pos)
        val nonCanonicalGt1 = RemoteBoolean.fromRemoteInt(gt1)
        assertThat(eval.evalBoolean(nonCanonicalPos)).isEqualTo(TriState.TRUE)
        assertThat(eval.evalInt(nonCanonicalPos.toRemoteInt())).isEqualTo(IntInterval(1, 5))
        assertThat(eval.evalBoolean(!nonCanonicalPos)).isEqualTo(TriState.UNKNOWN)
        assertThat(eval.evalBoolean(nonCanonicalGt1.isEqualTo(true.rb))).isEqualTo(TriState.FALSE)

        // First-class RemoteEnum ops
        val namedMode = RemoteEnum.createNamedRemoteEnum("mode", TestMode.OFF)
        assertThat(eval.evalInt(namedMode.ordinal)).isEqualTo(IntInterval(0, 1))
        assertThat(eval.evalString(namedMode.toRemoteString()).length).isEqualTo(IntInterval(3, 3))

        val wrappedMode = RemoteEnum(RemoteInt.createNamedRemoteInt("mode", 0), TestMode.entries)
        assertThat(eval.evalInt(wrappedMode.ordinal)).isEqualTo(IntInterval(0, 1))
        assertThat(eval.evalString(wrappedMode.toRemoteString { it.name.rs }).length)
            .isEqualTo(IntInterval(3, 3))
    }

    @Test
    fun stringAndColorOperationsIncludingFromArgbOrder() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val s = RemoteString.createNamedRemoteString("s", "hello")
        val env =
            MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(0f, 59f))
                .withNamedString("s", StringSummary(IntInterval(2, 5), isAsciiOnly = true))
        val eval = RemoteValueIntervalEvaluator(env)

        assertThat(eval.evalInt(s.length)).isEqualTo(IntInterval(2, 5))
        assertThat(eval.evalBoolean(s.isEmpty)).isEqualTo(TriState.FALSE)
        assertThat(eval.evalBoolean(s.isNotEmpty)).isEqualTo(TriState.TRUE)
        assertThat(eval.evalString(s + "!".rs).length).isEqualTo(IntInterval(3, 6))
        assertThat(
                eval
                    .evalString(
                        x.toRemoteStringOptions(
                            2,
                            0,
                            TextFromFloatFlags.PAD_PRE_ZERO or TextFromFloatFlags.PAD_AFTER_NONE,
                        )
                    )
                    .length
            )
            .isEqualTo(IntInterval(2, 2))

        // Verify FromArgb uses (red, green, blue, alpha) order accurately!
        val dynR = RemoteFloat.createNamedRemoteFloat("r", 0f)
        val colorEnv = MapIntervalEnvironment.EMPTY.withNamedFloat("r", FloatInterval(0.5f, 1f))
        val colorEval = RemoteValueIntervalEvaluator(colorEnv)
        val color = RemoteColor(red = dynR, green = 0f.rf, blue = 0f.rf, alpha = 0.25f.rf)
        val colorInterval = colorEval.evalColor(color)
        assertThat(colorInterval.alpha).isEqualTo(IntInterval.point(64))
        assertThat(colorInterval.red.min).isAtLeast(127)
        assertThat(colorInterval.red.max).isEqualTo(255)
        assertThat(colorInterval.green).isEqualTo(IntInterval.point(0))
        assertThat(colorInterval.blue).isEqualTo(IntInterval.point(0))
        assertThat(colorEval.evalFloat(color.alpha)).isEqualTo(FloatInterval.point(0.25f))

        // Named color exercises ColorOperationKey.Component extraction.
        val namedColor = RemoteColor.createNamedRemoteColor("c", 0)
        val namedColorEval =
            RemoteValueIntervalEvaluator(
                MapIntervalEnvironment.EMPTY.withNamedColor(
                    "c",
                    ColorInterval.constant(0x40FF0000),
                )
            )
        assertThat(namedColorEval.evalFloat(namedColor.alpha))
            .isEqualTo(FloatInterval.point(64f / 255f))
        assertThat(namedColorEval.evalFloat(namedColor.red)).isEqualTo(FloatInterval.point(1f))
    }

    @Test
    fun literalArrayIndexingAndCustomOperationExtensibility() {
        val idx = RemoteInt.createNamedRemoteInt("idx", 0)
        val env = MapIntervalEnvironment.EMPTY.withNamedInt("idx", IntInterval(1, 2))
        val eval = RemoteValueIntervalEvaluator(env)

        val floats = RemoteFloatArray(listOf(10f.rf, 20f.rf, 30f.rf, 40f.rf))
        assertThat(eval.evalFloat(floats[idx])).isEqualTo(FloatInterval(20f, 30f))

        val ints = RemoteIntArray(listOf(1.ri, 5.ri, 9.ri))
        assertThat(eval.evalInt(ints[idx])).isEqualTo(IntInterval(5, 9))

        val strings = RemoteStringArray(listOf("a".rs, "bb".rs, "ccc".rs))
        assertThat(eval.evalString(strings[idx]).length).isEqualTo(IntInterval(2, 3))

        // Custom RemoteOperation plugs directly into RemoteValueIntervalEvaluator without edits!
        val customKey = RemoteOperationCacheKey.create(CustomDoubleOp.DoubleValue, idx)
        assertThat(eval.evalIntKey(customKey)).isEqualTo(IntInterval(2, 4))

        // Array indexing picks up opposite-type path-sensitive narrowing overrides
        val nIdx = RemoteInt.createNamedRemoteInt("nIdx", 0)
        val envWithIntOverride =
            MapIntervalEnvironment.EMPTY.withNamedInt("nIdx", IntInterval(0, 3))
                .withIntNarrowing(nIdx.cacheKey, IntInterval.point(2))
        assertThat(RemoteValueIntervalEvaluator(envWithIntOverride).evalFloat(floats[nIdx]))
            .isEqualTo(FloatInterval.point(30f))

        val fIdx = RemoteFloat.createNamedRemoteFloat("fIdx", 0f)
        val envWithFloatOverride =
            MapIntervalEnvironment.EMPTY.withNamedFloat("fIdx", FloatInterval(0f, 2f))
                .withFloatNarrowing(fIdx.cacheKey, FloatInterval.point(1f))
        assertThat(RemoteValueIntervalEvaluator(envWithFloatOverride).evalInt(ints[fIdx]))
            .isEqualTo(IntInterval.point(5))

        // Out-of-bounds array indexing unions in 0
        val oobEnv = MapIntervalEnvironment.EMPTY.withNamedInt("idx", IntInterval(-1, 5))
        assertThat(RemoteValueIntervalEvaluator(oobEnv).evalFloat(floats[idx]))
            .isEqualTo(FloatInterval(0f, 40f))
        assertThat(RemoteValueIntervalEvaluator(oobEnv).evalInt(ints[idx]))
            .isEqualTo(IntInterval(0, 9))
    }

    @Test
    fun soundnessRegressions_copySignSqrtCbrtLerpNarrowingAndBoolSign() {
        // 1. RemoteInt CopySign returns a (b >= 0) or -a (b < 0), NOT |a|
        val a = RemoteInt.createNamedRemoteInt("a", 0)
        val bPos = RemoteInt.createNamedRemoteInt("bPos", 0)
        val bNeg = RemoteInt.createNamedRemoteInt("bNeg", 0)
        val bMix = RemoteInt.createNamedRemoteInt("bMix", 0)
        val intEnv =
            MapIntervalEnvironment.EMPTY.withNamedInt("a", IntInterval(-10, -2))
                .withNamedInt("bPos", IntInterval(0, 5))
                .withNamedInt("bNeg", IntInterval(-5, -1))
                .withNamedInt("bMix", IntInterval(-3, 3))
        val intEval = RemoteValueIntervalEvaluator(intEnv)
        assertThat(intEval.evalInt(copySign(a, bPos))).isEqualTo(IntInterval(-10, -2))
        assertThat(intEval.evalInt(copySign(a, bNeg))).isEqualTo(IntInterval(2, 10))
        assertThat(intEval.evalInt(copySign(a, bMix))).isEqualTo(IntInterval(-10, 10))

        // 2. Sqrt and Cbrt widen to UNBOUNDED when negative inputs are reachable (NaN at runtime)
        val negStraddle = RemoteFloat.createNamedRemoteFloat("negStraddle", 0f)
        val nonNeg = RemoteFloat.createNamedRemoteFloat("nonNeg", 0f)
        val floatEnv =
            MapIntervalEnvironment.EMPTY.withNamedFloat("negStraddle", FloatInterval(-4f, 9f))
                .withNamedFloat("nonNeg", FloatInterval(0f, 8f))
        val floatEval = RemoteValueIntervalEvaluator(floatEnv)
        assertThat(floatEval.evalFloat(sqrt(negStraddle))).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(floatEval.evalFloat(cbrt(negStraddle))).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(floatEval.evalFloat(sqrt(nonNeg))).isEqualTo(FloatInterval(0f, mathSqrt(8f)))
        assertThat(floatEval.evalFloat(cbrt(nonNeg))).isEqualTo(FloatInterval(0f, 2f))

        // 3. Lerp and RandInRange soundly capture float32 overflow & cancellation
        val huge = RemoteFloat.createNamedRemoteFloat("huge", 0f)
        val tUnit = RemoteFloat.createNamedRemoteFloat("tUnit", 0f)
        val lerpEnv =
            MapIntervalEnvironment.EMPTY.withNamedFloat("huge", FloatInterval(-3e38f, 3e38f))
                .withNamedFloat("tUnit", FloatInterval.UNIT)
        val lerpEval = RemoteValueIntervalEvaluator(lerpEnv)
        assertThat(lerpEval.evalFloat(lerp(huge, huge, tUnit))).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(lerpEval.evalFloat(randRange(huge, huge))).isEqualTo(FloatInterval.UNBOUNDED)
        val cancelRange = lerpEval.evalFloat(lerp(1e8f.rf, 1f.rf, tUnit))
        assertThat(0f in cancelRange).isTrue()
        assertThat(1e8f in cancelRange).isTrue()

        // 4. withFloatNarrowing / withIntNarrowing intersect with base bindings (never widen!)
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val baseEnv =
            MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(0f, 10f))
                .withNamedInt("n", IntInterval(0, 10))
        val narrowedMap =
            baseEnv
                .withFloatNarrowing(x.cacheKey, FloatInterval(-5f, 5f))
                .withIntNarrowing(n.cacheKey, IntInterval(-5, 5))
        assertThat(RemoteValueIntervalEvaluator(narrowedMap).evalFloat(x))
            .isEqualTo(FloatInterval(0f, 5f))
        assertThat(RemoteValueIntervalEvaluator(narrowedMap).evalInt(n))
            .isEqualTo(IntInterval(0, 5))

        // 5. Chained OverlaidIntervalEnvironment on a custom IntervalEnvironment preserves ALL
        // layers
        val customBase =
            object : IntervalEnvironment {
                override fun floatForNamed(key: RemoteNamedCacheKey): FloatInterval =
                    if (key.name == "x") FloatInterval(0f, 10f) else FloatInterval.UNBOUNDED

                override fun intForNamed(key: RemoteNamedCacheKey): IntInterval =
                    if (key.name == "n") IntInterval(0, 10) else IntInterval.UNBOUNDED
            }
        val chained =
            customBase
                .withFloatNarrowing(x.cacheKey, FloatInterval(-5f, 6f))
                .withIntNarrowing(n.cacheKey, IntInterval(2, 8))
        val chainedEval = RemoteValueIntervalEvaluator(chained)
        assertThat(chainedEval.evalFloat(x)).isEqualTo(FloatInterval(0f, 6f))
        assertThat(chainedEval.evalInt(n)).isEqualTo(IntInterval(2, 8))

        // 6. Negative RemoteInt evaluates to UNKNOWN (not TRUE!) as boolean for I_IFELSE parity
        assertThat(intEval.evalBoolKey(bNeg.cacheKey)).isEqualTo(TriState.UNKNOWN)
        assertThat(intEval.evalBoolKey(bPos.cacheKey)).isEqualTo(TriState.UNKNOWN)
        val strictlyPos = RemoteInt.createNamedRemoteInt("pos", 1)
        val posMixEval =
            RemoteValueIntervalEvaluator(
                MapIntervalEnvironment.EMPTY.withNamedInt("pos", IntInterval(1, 5))
                    .withNamedInt("bPos", IntInterval(0, 5))
                    .withNamedInt("bMix", IntInterval(-3, 3))
            )
        assertThat(posMixEval.evalBoolKey(strictlyPos.cacheKey)).isEqualTo(TriState.TRUE)
        // Bitwise OR with a possibly-negative RemoteInt must NOT evaluate to TRUE (I_IFELSE <= 0)
        assertThat(posMixEval.evalBoolKey((strictlyPos or bMix).cacheKey))
            .isEqualTo(TriState.UNKNOWN)
        assertThat(posMixEval.evalBoolKey((strictlyPos or bPos).cacheKey)).isEqualTo(TriState.TRUE)

        // 7. Derived unbounded non-canonical RemoteBoolean (!RemoteBoolean.fromRemoteInt(u))
        // must NOT be whitelisted as non-negative in OR
        val unboundedInt = RemoteInt.createNamedRemoteInt("unboundedInt", -2)
        val derivedNegatable = !RemoteBoolean.fromRemoteInt(unboundedInt)
        assertThat(posMixEval.evalBoolean(strictlyPos.isGreaterThan(0.ri) or derivedNegatable))
            .isEqualTo(TriState.UNKNOWN)

        // 8. RemoteColor.hsv with constant alpha != 1f uses FromAHSV with accurate alpha bounds
        val dynH = RemoteFloat.createNamedRemoteFloat("h", 0f)
        val hsvHalfAlpha = RemoteColor.hsv(dynH, 1f.rf, 1f.rf, alpha = 0.5f.rf)
        val hsvOpaque = RemoteColor.hsv(dynH, 1f.rf, 1f.rf, alpha = 1f.rf)
        assertThat(hsvHalfAlpha.cacheKey).isNotEqualTo(hsvOpaque.cacheKey)
        assertThat(RemoteValueIntervalEvaluator().evalColor(hsvHalfAlpha).alpha)
            .isEqualTo(IntInterval.point(128))
        assertThat(RemoteValueIntervalEvaluator().evalColor(hsvOpaque).alpha)
            .isEqualTo(IntInterval.point(255))

        // 9. RemoteInt.toRemoteString() includes GROUPING_BY3 ("1,000" -> length 5)
        val thousand = RemoteInt.createNamedRemoteInt("thousand", 1000)
        val thousandEval =
            RemoteValueIntervalEvaluator(
                MapIntervalEnvironment.EMPTY.withNamedInt("thousand", IntInterval.point(1000))
            )
        assertThat(thousandEval.evalString(thousand.toRemoteString()).length)
            .isEqualTo(IntInterval.point(5))
        assertThat(
                thousandEval
                    .evalStringKey(
                        RemoteOperationCacheKey.create(
                            RemoteIntOperationKey.ToRemoteString,
                            thousand,
                        )
                    )
                    .length
            )
            .isEqualTo(IntInterval.point(5))

        // 10. Float32 interior rounding in lerp must be soundly contained (tie-to-even u - f)
        val wideFrom = RemoteFloat.createNamedRemoteFloat("wideFrom", 67108872f)
        val lerpRoundEnv =
            MapIntervalEnvironment.EMPTY.withNamedFloat(
                "wideFrom",
                FloatInterval(67108864f, 67108880f),
            )
        val lerpRoundEval = RemoteValueIntervalEvaluator(lerpRoundEnv)
        val lerpRes = lerpRoundEval.evalFloat(lerp(wideFrom, 4f.rf, 1f.rf))
        val interiorRuntime = 67108872f + (4f - 67108872f) * 1f
        assertThat(lerpRes.contains(interiorRuntime)).isTrue()

        // 11. UNBOUNDED (reachable NaN) is never swallowed by bounded-range float ops
        val unboundedU = RemoteFloat.createNamedRemoteFloat("unboundedU", 0f)
        val emptyEval = RemoteValueIntervalEvaluator()
        assertThat(emptyEval.evalFloat(exp(unboundedU))).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(emptyEval.evalFloat(sin(unboundedU))).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(emptyEval.evalFloat(clamp(value = unboundedU, min = 0f.rf, max = 1f.rf)))
            .isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(emptyEval.evalBoolean(exp(unboundedU).isGreaterThan((-1f).rf)))
            .isEqualTo(TriState.UNKNOWN)
        assertThat(emptyEval.evalBoolean(sin(unboundedU).isLessThan(2f.rf)))
            .isEqualTo(TriState.UNKNOWN)

        // 12. FromArgb models ColorExpression 10-bit constant alpha quantization (0.1f -> 25)
        val dynR2 = RemoteFloat.createNamedRemoteFloat("r2", 0f)
        val argbQuantEval =
            RemoteValueIntervalEvaluator(
                MapIntervalEnvironment.EMPTY.withNamedFloat("r2", FloatInterval(0f, 1f))
            )
        val argbQuant = RemoteColor(red = dynR2, green = 0f.rf, blue = 0f.rf, alpha = 0.1f.rf)
        assertThat(argbQuantEval.evalColor(argbQuant).alpha).isEqualTo(IntInterval.point(25))

        // 13. Empty-interval propagation in evalIsEmpty, int And, compareIntKeys, array union
        val emptyEnv =
            MapIntervalEnvironment.EMPTY.withNamedString(
                    "es",
                    StringSummary(IntInterval.EMPTY, isAsciiOnly = true),
                )
                .withNamedInt("ei", IntInterval.EMPTY)
                .withNamedInt("pos5", IntInterval(0, 5))
        val emptyIntEval = RemoteValueIntervalEvaluator(emptyEnv)
        val es = RemoteString.createNamedRemoteString("es", "")
        val ei = RemoteInt.createNamedRemoteInt("ei", 0)
        val pos5 = RemoteInt.createNamedRemoteInt("pos5", 1)
        assertThat(emptyIntEval.evalBoolean(es.isEmpty)).isEqualTo(TriState.UNKNOWN)
        assertThat(emptyIntEval.evalInt(ei and pos5)).isEqualTo(IntInterval.EMPTY)
        assertThat(emptyIntEval.evalBoolean(ei.isEqualTo(ei))).isEqualTo(TriState.UNKNOWN)
        val arr = RemoteFloatArray(listOf(1f.rf, 2f.rf))
        assertThat(emptyIntEval.evalFloat(arr[ei])).isEqualTo(FloatInterval.EMPTY)
    }

    @Test
    fun broadFloatAndIntOperationCoverage() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 0f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 0f)
        val u = RemoteFloat.createNamedRemoteFloat("u", 0f)
        val n = RemoteInt.createNamedRemoteInt("n", 0)
        val m = RemoteInt.createNamedRemoteInt("m", 0)
        val env =
            MapIntervalEnvironment.EMPTY.withNamedFloat("x", FloatInterval(1f, 4f))
                .withNamedFloat("y", FloatInterval(-2f, 2f))
                .withNamedFloat("u", FloatInterval(-0.5f, 0.5f))
                .withNamedInt("n", IntInterval(2, 6))
                .withNamedInt("m", IntInterval(1, 3))
        val eval = RemoteValueIntervalEvaluator(env)

        assertThat(eval.evalFloat(x / 2f.rf)).isEqualTo(FloatInterval(0.5f, 2f))
        assertThat(eval.evalFloat(x % 2f.rf))
            .isEqualTo(FloatInterval(0f, 2f, minInclusive = true, maxInclusive = false))
        assertThat(eval.evalFloat(min(x, y))).isEqualTo(FloatInterval(-2f, 2f))
        assertThat(eval.evalFloat(max(x, y))).isEqualTo(FloatInterval(1f, 4f))
        assertThat(eval.evalFloat(abs(y))).isEqualTo(FloatInterval(0f, 2f))
        assertThat(eval.evalFloat(mad(x, 2f.rf, 1f.rf))).isEqualTo(FloatInterval(3f, 9f))
        assertThat(eval.evalFloat(floor(x))).isEqualTo(FloatInterval(1f, 4f))
        assertThat(eval.evalFloat(ceil(x))).isEqualTo(FloatInterval(1f, 4f))
        assertThat(eval.evalFloat(round(x))).isEqualTo(FloatInterval(1f, 4f))
        assertThat(eval.evalFloat(exp(x)).min).isGreaterThan(2.5f)
        assertThat(eval.evalFloat(ln(x)).min).isEqualTo(0f)
        assertThat(eval.evalFloat(log(x)).min).isEqualTo(0f)
        assertThat(eval.evalFloat(pow(x, 2f.rf))).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(eval.evalFloat(cos(x)).min).isAtLeast(-1f)
        assertThat(eval.evalFloat(asin(u)).max).isAtMost((PI / 2.0).toFloat())
        assertThat(eval.evalFloat(acos(u)).min).isAtLeast(0f)
        assertThat(eval.evalFloat(atan(y)).max).isAtMost((PI / 2.0).toFloat())
        assertThat(eval.evalFloat(atan2(y, x)).min).isAtLeast(-PI.toFloat())
        assertThat(eval.evalFloat(sign(x))).isEqualTo(FloatInterval.point(1f))
        assertThat(eval.evalFloat(sign(y))).isEqualTo(FloatInterval(-1f, 1f))
        assertThat(eval.evalFloat(copySign(y, x))).isEqualTo(FloatInterval(0f, 2f))

        // Integer bitwise, div, rem, min, max, abs
        assertThat(eval.evalInt(n / m)).isEqualTo(IntInterval(0, 6))
        assertThat(eval.evalInt(n % m)).isEqualTo(IntInterval(0, 2))
        assertThat(eval.evalInt(n and m)).isEqualTo(IntInterval(0, 3))
        assertThat(eval.evalInt(n or m)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(eval.evalInt(n xor m)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(eval.evalInt(n shl m)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(eval.evalInt(n shr m)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(eval.evalInt(n.inv())).isEqualTo(IntInterval.UNBOUNDED)
    }

    private enum class CustomDoubleOp : RemoteIntervalOperation {
        DoubleValue;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "double(${args.joinToDebugString()})"

        override fun evaluateIntInterval(
            key: RemoteOperationCacheKey,
            evaluator: RemoteValueIntervalEvaluator,
        ): IntInterval = evaluator.evalIntKey(key.args[0]) * IntInterval.point(2)
    }
}
