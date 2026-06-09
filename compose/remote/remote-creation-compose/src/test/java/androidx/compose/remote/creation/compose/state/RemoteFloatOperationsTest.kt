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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteFloatOperationsTest {

    @Test
    fun toDebugString_max() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 10f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 20f)
        assertThat(max(a, b).toDebugString()).isEqualTo("max(user:a, user:b)")
    }

    @Test
    fun toDebugString_min() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 10f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 20f)
        assertThat(min(a, b).toDebugString()).isEqualTo("min(user:a, user:b)")
    }

    @Test
    fun toDebugString_pow() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 2f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 3f)
        assertThat(pow(a, b).toDebugString()).isEqualTo("pow(user:a, user:b)")
    }

    @Test
    fun toDebugString_sqrt() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 16f)
        assertThat(sqrt(a).toDebugString()).isEqualTo("sqrt(user:a)")
    }

    @Test
    fun toDebugString_abs() {
        val a = RemoteFloat.createNamedRemoteFloat("a", -10f)
        assertThat(abs(a).toDebugString()).isEqualTo("abs(user:a)")
    }

    @Test
    fun toDebugString_sign() {
        val a = RemoteFloat.createNamedRemoteFloat("a", -10f)
        assertThat(sign(a).toDebugString()).isEqualTo("sign(user:a)")
    }

    @Test
    fun toDebugString_copySign() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 10f)
        val b = RemoteFloat.createNamedRemoteFloat("b", -1f)
        assertThat(copySign(a, b).toDebugString()).isEqualTo("copySign(user:a, user:b)")
    }

    @Test
    fun toDebugString_exp() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 2f)
        assertThat(exp(a).toDebugString()).isEqualTo("exp(user:a)")
    }

    @Test
    fun toDebugString_ceil() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 2.3f)
        assertThat(ceil(a).toDebugString()).isEqualTo("ceil(user:a)")
    }

    @Test
    fun toDebugString_floor() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 2.7f)
        assertThat(floor(a).toDebugString()).isEqualTo("floor(user:a)")
    }

    @Test
    fun toDebugString_log() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 100f)
        assertThat(log(a).toDebugString()).isEqualTo("log(user:a)")
    }

    @Test
    fun toDebugString_ln() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 10f)
        assertThat(ln(a).toDebugString()).isEqualTo("ln(user:a)")
    }

    @Test
    fun toDebugString_round() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 2.5f)
        assertThat(round(a).toDebugString()).isEqualTo("round(user:a)")
    }

    @Test
    fun toDebugString_sin() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        assertThat(sin(a).toDebugString()).isEqualTo("sin(user:a)")
    }

    @Test
    fun toDebugString_cos() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        assertThat(cos(a).toDebugString()).isEqualTo("cos(user:a)")
    }

    @Test
    fun toDebugString_tan() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        assertThat(tan(a).toDebugString()).isEqualTo("tan(user:a)")
    }

    @Test
    fun toDebugString_asin() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 0.5f)
        assertThat(asin(a).toDebugString()).isEqualTo("asin(user:a)")
    }

    @Test
    fun toDebugString_acos() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 0.5f)
        assertThat(acos(a).toDebugString()).isEqualTo("acos(user:a)")
    }

    @Test
    fun toDebugString_atan() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 0.5f)
        assertThat(atan(a).toDebugString()).isEqualTo("atan(user:a)")
    }

    @Test
    fun toDebugString_atan2() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 2f)
        assertThat(atan2(a, b).toDebugString()).isEqualTo("atan2(user:a, user:b)")
    }

    @Test
    fun toDebugString_cbrt() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 8f)
        assertThat(cbrt(a).toDebugString()).isEqualTo("cbrt(user:a)")
    }

    @Test
    fun toDebugString_toDeg() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        assertThat(toDeg(a).toDebugString()).isEqualTo("toDeg(user:a)")
    }

    @Test
    fun toDebugString_toRad() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 90f)
        assertThat(toRad(a).toDebugString()).isEqualTo("toRad(user:a)")
    }

    @Test
    fun toDebugString_lerp() {
        val from = RemoteFloat.createNamedRemoteFloat("from", 0f)
        val to = RemoteFloat.createNamedRemoteFloat("to", 100f)
        val tween = RemoteFloat.createNamedRemoteFloat("tween", 0.5f)
        assertThat(lerp(from, to, tween).toDebugString())
            .isEqualTo("lerp(user:from, user:to, user:tween)")
    }

    @Test
    fun toDebugString_mad() {
        val a = RemoteFloat.createNamedRemoteFloat("a", 2f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 3f)
        val c = RemoteFloat.createNamedRemoteFloat("c", 4f)
        assertThat(mad(a, b, c).toDebugString()).isEqualTo("mad(user:a, user:b, user:c)")
    }

    @Test
    fun toDebugString_clamp() {
        val value = RemoteFloat.createNamedRemoteFloat("val", 15f)
        val min = RemoteFloat.createNamedRemoteFloat("min", 10f)
        val max = RemoteFloat.createNamedRemoteFloat("max", 20f)
        assertThat(clamp(value, min, max).toDebugString())
            .isEqualTo("clamp(user:min, user:max, user:val)")
    }

    @Test
    fun toDebugString_interpolate() {
        val p = RemoteFloat.createNamedRemoteFloat("p", 0.5f)
        val start = RemoteFloat.createNamedRemoteFloat("start", 100f)
        val end = RemoteFloat.createNamedRemoteFloat("end", 200f)
        val interpLinear = interpolateRemoteFloat(p, start, end, CUBIC_LINEAR)
        assertThat(interpLinear.toDebugString())
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
    fun toDebugString_animation() {
        val rf = RemoteFloat.createNamedRemoteFloat("val", 10f)
        val animated = animateRemoteFloat(rf, duration = 2f, type = CUBIC_DECELERATE)
        assertThat(animated.toDebugString()).isEqualTo("animate(user:val)")
    }

    @Test
    fun toDebugString_cubicEasing() {
        val x1 = RemoteFloat.createNamedRemoteFloat("x1", 0.1f)
        val y1 = RemoteFloat.createNamedRemoteFloat("y1", 0.2f)
        val x2 = RemoteFloat.createNamedRemoteFloat("x2", 0.8f)
        val y2 = RemoteFloat.createNamedRemoteFloat("y2", 0.9f)
        val p = RemoteFloat.createNamedRemoteFloat("p", 0.5f)
        val cubic = cubicEasing(x1, y1, x2, y2, p)
        assertThat(cubic.toDebugString())
            .isEqualTo("cubicEasing(user:x1, user:y1, user:x2, user:y2, user:p)")
    }

    @Test
    fun toDebugString_evalSpline_nonLooping() {
        val pts = RemoteFloatArray(listOf(0f.rf, 0.5f.rf, 1f.rf))
        val p = RemoteFloat.createNamedRemoteFloat("p", 0.5f)
        val spline = evalSpline(pts, loop = false, p)
        assertThat(spline.toDebugString()).isEqualTo("evalSpline(arrayOf(0.0, 0.5, 1.0), user:p)")
    }

    @Test
    fun toDebugString_evalSpline_looping() {
        val pts = RemoteFloatArray(listOf(0f.rf, 0.5f.rf, 1f.rf))
        val p = RemoteFloat.createNamedRemoteFloat("p", 0.5f)
        val splineLoop = evalSpline(pts, loop = true, p)
        assertThat(splineLoop.toDebugString())
            .isEqualTo("evalSpline(arrayOf(0.0, 0.5, 1.0), user:p, loop=true)")
    }
}
