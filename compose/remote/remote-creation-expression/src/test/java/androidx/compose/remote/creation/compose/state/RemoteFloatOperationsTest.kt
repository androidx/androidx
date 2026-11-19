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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import com.google.common.truth.Truth.assertThat
import java.lang.Float.floatToRawIntBits
import java.util.Random
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteFloatOperationsTest {

    @Test
    fun toString_max() {
        val a = createNamedRemoteFloat("a", 10f)
        val b = createNamedRemoteFloat("b", 20f)
        assertThat(max(a, b).toString()).isEqualTo("max(user:a, user:b)")
    }

    @Test
    fun toString_min() {
        val a = createNamedRemoteFloat("a", 10f)
        val b = createNamedRemoteFloat("b", 20f)
        assertThat(min(a, b).toString()).isEqualTo("min(user:a, user:b)")
    }

    @Test
    fun toString_pow() {
        val a = createNamedRemoteFloat("a", 2f)
        val b = createNamedRemoteFloat("b", 3f)
        assertThat(pow(a, b).toString()).isEqualTo("pow(user:a, user:b)")
    }

    @Test
    fun toString_sqrt() {
        val a = createNamedRemoteFloat("a", 16f)
        assertThat(sqrt(a).toString()).isEqualTo("sqrt(user:a)")
    }

    @Test
    fun toString_abs() {
        val a = createNamedRemoteFloat("a", -10f)
        assertThat(abs(a).toString()).isEqualTo("abs(user:a)")
    }

    @Test
    fun toString_sign() {
        val a = createNamedRemoteFloat("a", -10f)
        assertThat(sign(a).toString()).isEqualTo("sign(user:a)")
    }

    @Test
    fun toString_copySign() {
        val a = createNamedRemoteFloat("a", 10f)
        val b = createNamedRemoteFloat("b", -1f)
        assertThat(copySign(a, b).toString()).isEqualTo("copySign(user:a, user:b)")
    }

    @Test
    fun toString_exp() {
        val a = createNamedRemoteFloat("a", 2f)
        assertThat(exp(a).toString()).isEqualTo("exp(user:a)")
    }

    @Test
    fun toString_ceil() {
        val a = createNamedRemoteFloat("a", 2.3f)
        assertThat(ceil(a).toString()).isEqualTo("ceil(user:a)")
    }

    @Test
    fun toString_floor() {
        val a = createNamedRemoteFloat("a", 2.7f)
        assertThat(floor(a).toString()).isEqualTo("floor(user:a)")
    }

    @Test
    fun toString_log() {
        val a = createNamedRemoteFloat("a", 100f)
        assertThat(log(a).toString()).isEqualTo("log(user:a)")
    }

    @Test
    fun toString_ln() {
        val a = createNamedRemoteFloat("a", 10f)
        assertThat(ln(a).toString()).isEqualTo("ln(user:a)")
    }

    @Test
    fun toString_round() {
        val a = createNamedRemoteFloat("a", 2.5f)
        assertThat(round(a).toString()).isEqualTo("round(user:a)")
    }

    @Test
    fun toString_sin() {
        val a = createNamedRemoteFloat("a", 1f)
        assertThat(sin(a).toString()).isEqualTo("sin(user:a)")
    }

    @Test
    fun toString_cos() {
        val a = createNamedRemoteFloat("a", 1f)
        assertThat(cos(a).toString()).isEqualTo("cos(user:a)")
    }

    @Test
    fun toString_tan() {
        val a = createNamedRemoteFloat("a", 1f)
        assertThat(tan(a).toString()).isEqualTo("tan(user:a)")
    }

    @Test
    fun toString_asin() {
        val a = createNamedRemoteFloat("a", 0.5f)
        assertThat(asin(a).toString()).isEqualTo("asin(user:a)")
    }

    @Test
    fun toString_acos() {
        val a = createNamedRemoteFloat("a", 0.5f)
        assertThat(acos(a).toString()).isEqualTo("acos(user:a)")
    }

    @Test
    fun toString_atan() {
        val a = createNamedRemoteFloat("a", 0.5f)
        assertThat(atan(a).toString()).isEqualTo("atan(user:a)")
    }

    @Test
    fun toString_atan2() {
        val a = createNamedRemoteFloat("a", 1f)
        val b = createNamedRemoteFloat("b", 2f)
        assertThat(atan2(a, b).toString()).isEqualTo("atan2(user:a, user:b)")
    }

    @Test
    fun toString_cbrt() {
        val a = createNamedRemoteFloat("a", 8f)
        assertThat(cbrt(a).toString()).isEqualTo("cbrt(user:a)")
    }

    @Test
    fun toString_toDeg() {
        val a = createNamedRemoteFloat("a", 1f)
        assertThat(toDeg(a).toString()).isEqualTo("toDeg(user:a)")
    }

    @Test
    fun toString_toRad() {
        val a = createNamedRemoteFloat("a", 90f)
        assertThat(toRad(a).toString()).isEqualTo("toRad(user:a)")
    }

    @Test
    fun toString_lerp() {
        val from = createNamedRemoteFloat("from", 0f)
        val to = createNamedRemoteFloat("to", 100f)
        val tween = createNamedRemoteFloat("tween", 0.5f)
        assertThat(lerp(from, to, tween).toString())
            .isEqualTo("lerp(user:from, user:to, user:tween)")
    }

    @Test
    fun toString_mad() {
        val a = createNamedRemoteFloat("a", 2f)
        val b = createNamedRemoteFloat("b", 3f)
        val c = createNamedRemoteFloat("c", 4f)
        assertThat(mad(a, b, c).toString()).isEqualTo("mad(user:a, user:b, user:c)")
    }

    @Test
    fun toString_clamp() {
        val value = createNamedRemoteFloat("val", 15f)
        val min = createNamedRemoteFloat("min", 10f)
        val max = createNamedRemoteFloat("max", 20f)
        assertThat(clamp(value, min, max).toString())
            .isEqualTo("clamp(user:min, user:max, user:val)")
    }

    @Test
    fun toString_interpolate() {
        val p = createNamedRemoteFloat("p", 0.5f)
        val start = createNamedRemoteFloat("start", 100f)
        val end = createNamedRemoteFloat("end", 200f)
        val interpLinear = interpolateRemoteFloat(p, start, end, CUBIC_LINEAR)
        assertThat(interpLinear.toString())
            .isEqualTo("user:start + (user:end - user:start) * user:p")
    }

    @Test
    fun interpolate_constantFolding() {
        val p = RemoteFloat(0.5f)
        val start = RemoteFloat(100f)
        val end = RemoteFloat(200f)
        val interpConstant = interpolateRemoteFloat(p, start, end, type = CUBIC_LINEAR)
        assertThat(interpConstant.hasConstantValue).isTrue()
        assertThat(interpConstant.constantValue).isEqualTo(150f)
    }

    @Test
    fun toString_animation() {
        val rf = createNamedRemoteFloat("val", 10f)
        val animated = animateRemoteFloat(rf, duration = 2f, type = CUBIC_DECELERATE)
        assertThat(animated.toString()).isEqualTo("animate(user:val)")
    }

    @Test
    fun toString_cubicEasing() {
        val x1 = createNamedRemoteFloat("x1", 0.1f)
        val y1 = createNamedRemoteFloat("y1", 0.2f)
        val x2 = createNamedRemoteFloat("x2", 0.8f)
        val y2 = createNamedRemoteFloat("y2", 0.9f)
        val p = createNamedRemoteFloat("p", 0.5f)
        val cubic = cubicEasing(x1, y1, x2, y2, p)
        assertThat(cubic.toString())
            .isEqualTo("cubicEasing(user:x1, user:y1, user:x2, user:y2, user:p)")
    }

    @Test
    fun toString_evalSpline_nonLooping() {
        val pts = RemoteFloatArray(listOf(0f.rf, 0.5f.rf, 1f.rf))
        val p = createNamedRemoteFloat("p", 0.5f)
        val spline = evalSpline(pts, loop = false, p)
        assertThat(spline.toString()).isEqualTo("evalSpline(arrayOf(0.0, 0.5, 1.0), user:p)")
    }

    @Test
    fun toString_evalSpline_looping() {
        val pts = RemoteFloatArray(listOf(0f.rf, 0.5f.rf, 1f.rf))
        val p = createNamedRemoteFloat("p", 0.5f)
        val splineLoop = evalSpline(pts, loop = true, p)
        assertThat(splineLoop.toString())
            .isEqualTo("evalSpline(arrayOf(0.0, 0.5, 1.0), user:p, loop=true)")
    }

    @Test
    fun toString_rand() {
        assertThat(rand().toString()).isEqualTo("rand()")
    }

    @Test
    fun toString_randRange() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 10f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 20f)
        assertThat(randRange(a, b).toString()).isEqualTo("randInRange(user:a, user:b)")
    }

    @Test
    fun rand_evalWithSeed() {
        val context = TestRemoteContext()
        val creationState = testCreationState()
        val seed = 12345f

        // Seed PRNG via lower-level floatExpression API with RAND_SEED
        creationState.document.floatExpression(seed, AnimatedFloatExpression.RAND_SEED, 0f)

        val r = rand()
        val resultId = creationState.getId(r)

        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

        val expected = Random(floatToRawIntBits(seed).toLong()).nextFloat()
        assertThat(context.getFloat(resultId)).isWithin(0.0001f).of(expected)
    }

    @Test
    fun randRange_evalWithSeed() {
        val context = TestRemoteContext()
        val creationState = testCreationState()
        val seed = 67890f
        val from = 10f
        val to = 50f

        // Seed PRNG via lower-level floatExpression API with RAND_SEED
        creationState.document.floatExpression(seed, AnimatedFloatExpression.RAND_SEED, 0f)

        val r = randRange(from.rf, to.rf)
        val resultId = creationState.getId(r)

        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

        val expectedRand = Random(floatToRawIntBits(seed).toLong()).nextFloat()
        val expected = expectedRand * (to - from) + from
        assertThat(context.getFloat(resultId)).isWithin(0.0001f).of(expected)
    }
}
