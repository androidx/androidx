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

package androidx.compose.remote.document.intervals

import androidx.compose.remote.core.operations.utilities.easing.CubicEasing
import androidx.compose.remote.document.AnimationEasing
import androidx.compose.remote.document.packAnimationSpec
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.nextDown
import kotlin.test.Test

class FloatAnimationBoundsTest {

    @Test
    fun evaluateCubicEasing_bitForBitParityWithRemoteCore() {
        val presets =
            intArrayOf(
                AnimationEasing.CUBIC_STANDARD,
                AnimationEasing.CUBIC_ACCELERATE,
                AnimationEasing.CUBIC_DECELERATE,
                AnimationEasing.CUBIC_LINEAR,
                AnimationEasing.CUBIC_ANTICIPATE,
                AnimationEasing.CUBIC_OVERSHOOT,
            )
        val xs = floatArrayOf(-0.2f, 0f, 0.1f, 0.25f, 0.5f, 0.73f, 0.99f, 1f, 1.5f)
        for (preset in presets) {
            val pts = AnimationEasing.cubicControlPoints(preset)!!
            val core = CubicEasing().apply { config(preset) }
            for (x in xs) {
                assertWithMessage("Mismatch for preset=$preset x=$x")
                    .that(
                        FloatAnimationBounds.evaluateCubicEasing(pts[0], pts[1], pts[2], pts[3], x)
                    )
                    .isEqualTo(core.get(x))
            }
        }
    }

    @Test
    fun boundAnimatedFloat_handlesStandardOvershootInitialValueAndTaint() {
        val standardSpec = packAnimationSpec(duration = 0.5f, type = AnimationEasing.CUBIC_STANDARD)
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(standardSpec)).isFalse()
        assertThat(FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval(10f, 20f)))
            .isEqualTo(FloatInterval(10f, 20f))

        val withInit =
            packAnimationSpec(
                duration = 0.5f,
                type = AnimationEasing.CUBIC_STANDARD,
                initialValue = 0f,
            )
        val withInitBound =
            FloatAnimationBounds.boundAnimatedFloat(withInit, FloatInterval(10f, 20f))
        assertThat(withInitBound.min).isEqualTo(0f)
        assertThat(withInitBound.max).isAtLeast(20f)
        assertThat(withInitBound.max).isLessThan(20.001f)

        val overshootSpec =
            packAnimationSpec(duration = 0.5f, type = AnimationEasing.CUBIC_OVERSHOOT)
        val overshootBound =
            FloatAnimationBounds.boundAnimatedFloat(overshootSpec, FloatInterval(0f, 100f))
        assertThat(overshootBound.min).isLessThan(0f)
        assertThat(overshootBound.max).isGreaterThan(100f)

        val wrapSpec =
            packAnimationSpec(duration = 0.5f, type = AnimationEasing.CUBIC_STANDARD, wrap = 360f)
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(wrapSpec)).isTrue()
        assertThat(FloatAnimationBounds.boundAnimatedFloat(wrapSpec, FloatInterval(0f, 100f)))
            .isEqualTo(FloatInterval.UNBOUNDED)

        val springSpec = floatArrayOf(0f, 1f, 2f, 3f, 4f)
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(springSpec)).isTrue()

        // Soundness regressions for [0, 1] easing under float32 overflow, cancellation, and
        // non-monotonic ULP boundary rounding in p * (t - s) + s
        assertThat(
                FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval(-3e38f, 3e38f))
            )
            .isEqualTo(FloatInterval.UNBOUNDED)
        val largeSpan =
            FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval(1f, 1e8f))
        assertWithMessage("Float32 cancellation (1*(1-1e8)+1e8 == 0f) must be included")
            .that(0f in largeSpan)
            .isTrue()
        assertThat(1e8f in largeSpan).isTrue()

        val posUlpSpan =
            FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval(3f, 33554432f))
        val posInterior = 1f * (3f - 33554430f) + 33554430f // evaluates to 2f < 3f
        assertThat(posInterior).isEqualTo(2f)
        assertWithMessage("Interior binary32 rounding result 2f must be in $posUlpSpan")
            .that(posInterior in posUlpSpan)
            .isTrue()

        val negUlpSpan =
            FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval(-33554432f, -3f))
        val negInterior = 1f * (-3f - -33554430f) + -33554430f // evaluates to -2f > -3f
        assertThat(negInterior).isEqualTo(-2f)
        assertWithMessage("Interior binary32 rounding result -2f must be in $negUlpSpan")
            .that(negInterior in negUlpSpan)
            .isTrue()

        val oddUpper = 20f.nextDown()
        val oddUpperSpan =
            FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval(0f, oddUpper))
        val oddInterior = 1f * (oddUpper - 9.23872e-7f) + 9.23872e-7f
        assertWithMessage("Interior upward rounding $oddInterior must be in $oddUpperSpan")
            .that(oddInterior in oddUpperSpan)
            .isTrue()

        // Constant target stays exact
        assertThat(FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval.ZERO))
            .isEqualTo(FloatInterval.ZERO)

        // CUBIC_ANTICIPATE, EASE_OUT_BOUNCE, CUBIC_CUSTOM, EASE_OUT_ELASTIC, SPLINE_CUSTOM,
        // malformed
        val anticipateSpec =
            packAnimationSpec(duration = 0.5f, type = AnimationEasing.CUBIC_ANTICIPATE)
        val anticipateBound =
            FloatAnimationBounds.boundAnimatedFloat(anticipateSpec, FloatInterval(0f, 100f))
        assertThat(anticipateBound.min).isLessThan(0f)
        assertThat(anticipateBound.max).isGreaterThan(100f)

        val bounceSpec = packAnimationSpec(duration = 0.5f, type = AnimationEasing.EASE_OUT_BOUNCE)
        assertThat(FloatAnimationBounds.boundAnimatedFloat(bounceSpec, FloatInterval(10f, 20f)))
            .isEqualTo(FloatInterval(10f, 20f))

        val customSpec =
            packAnimationSpec(
                duration = 0.5f,
                type = AnimationEasing.CUBIC_CUSTOM,
                spec = floatArrayOf(0.2f, -0.5f, 0.8f, 1.5f),
            )
        val customBound =
            FloatAnimationBounds.boundAnimatedFloat(customSpec, FloatInterval(0f, 100f))
        assertThat(customBound.min).isLessThan(0f)
        assertThat(customBound.max).isGreaterThan(100f)

        val badCustomX =
            packAnimationSpec(
                duration = 0.5f,
                type = AnimationEasing.CUBIC_CUSTOM,
                spec = floatArrayOf(-0.2f, 0f, 0.8f, 1f),
            )
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(badCustomX)).isTrue()

        val elasticSpec =
            packAnimationSpec(duration = 0.5f, type = AnimationEasing.EASE_OUT_ELASTIC)
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(elasticSpec)).isTrue()

        val splineSpec =
            packAnimationSpec(
                duration = 0.5f,
                type = AnimationEasing.SPLINE_CUSTOM,
                spec = floatArrayOf(0f, 0.5f, 1f),
            )
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(splineSpec)).isTrue()

        // Empty / 1-element / non-positive duration / truncated / infinite initialValue specs
        assertThat(FloatAnimationBounds.parseAnimationSpec(floatArrayOf()))
            .isEqualTo(ParsedAnimationSpec(FloatInterval.UNIT, null))
        assertThat(FloatAnimationBounds.parseAnimationSpec(floatArrayOf(0.5f)))
            .isEqualTo(ParsedAnimationSpec(FloatInterval.UNIT, null))
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(floatArrayOf(0f))).isTrue()
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(floatArrayOf(-1f)))
            .isTrue()
        assertThat(FloatAnimationBounds.isConservativelyTaintedAnimation(floatArrayOf(Float.NaN)))
            .isTrue()
        assertThat(
                FloatAnimationBounds.isConservativelyTaintedAnimation(
                    floatArrayOf(1f, Float.fromBits((4 shl 16) or AnimationEasing.CUBIC_CUSTOM))
                )
            )
            .isTrue()
        assertThat(
                FloatAnimationBounds.isConservativelyTaintedAnimation(
                    packAnimationSpec(duration = 1f, initialValue = Float.POSITIVE_INFINITY)
                )
            )
            .isTrue()
        assertThat(FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval.EMPTY))
            .isEqualTo(FloatInterval.EMPTY)
        assertThat(FloatAnimationBounds.boundAnimatedFloat(standardSpec, FloatInterval.UNBOUNDED))
            .isEqualTo(FloatInterval.UNBOUNDED)
    }

    @Test
    fun evalCubicEasingInterval_boundsMonotoneAndOvershootingCurves() {
        val mono =
            FloatAnimationBounds.evalCubicEasingInterval(
                x1 = FloatInterval.point(0.4f),
                y1 = FloatInterval.point(0f),
                x2 = FloatInterval.point(0.2f),
                y2 = FloatInterval.point(1f),
                pos = FloatInterval(0.25f, 0.75f),
            )
        assertThat(mono.min).isGreaterThan(0f)
        assertThat(mono.max).isLessThan(1f)

        val overshooting =
            FloatAnimationBounds.evalCubicEasingInterval(
                x1 = FloatInterval.point(0.34f),
                y1 = FloatInterval.point(1.56f),
                x2 = FloatInterval.point(0.64f),
                y2 = FloatInterval.point(1f),
                pos = FloatInterval.UNIT,
            )
        assertThat(overshooting.max).isGreaterThan(1.0f)
        assertThat(overshooting.min).isEqualTo(0f)

        // Exact point evaluation even when y1/y2 overshoot [0, 1]
        val overshootAtHalf =
            FloatAnimationBounds.evalCubicEasingInterval(
                x1 = FloatInterval.point(0.34f),
                y1 = FloatInterval.point(1.56f),
                x2 = FloatInterval.point(0.64f),
                y2 = FloatInterval.point(1f),
                pos = FloatInterval.point(0.5f),
            )
        assertThat(overshootAtHalf.isPoint).isTrue()
        assertThat(overshootAtHalf.min)
            .isEqualTo(FloatAnimationBounds.evaluateCubicEasing(0.34f, 1.56f, 0.64f, 1f, 0.5f))

        // Clamped endpoints, non-point control points, invalid x, empty/unbounded
        assertThat(
                FloatAnimationBounds.evalCubicEasingInterval(
                    FloatInterval.point(0.4f),
                    FloatInterval.point(0f),
                    FloatInterval.point(0.2f),
                    FloatInterval.point(1f),
                    FloatInterval(-2f, 0f),
                )
            )
            .isEqualTo(FloatInterval.ZERO)
        assertThat(
                FloatAnimationBounds.evalCubicEasingInterval(
                    FloatInterval.point(0.4f),
                    FloatInterval.point(0f),
                    FloatInterval.point(0.2f),
                    FloatInterval.point(1f),
                    FloatInterval(1f, 3f),
                )
            )
            .isEqualTo(FloatInterval.point(1f))
        val rangedCtrl =
            FloatAnimationBounds.evalCubicEasingInterval(
                FloatInterval(0.2f, 0.5f),
                FloatInterval(-0.5f, 1.5f),
                FloatInterval(0.5f, 0.8f),
                FloatInterval(0f, 1.5f),
                FloatInterval(0.2f, 0.8f),
            )
        assertThat(rangedCtrl.min).isLessThan(0f)
        assertThat(rangedCtrl.max).isGreaterThan(1f)
        assertThat(
                FloatAnimationBounds.evalCubicEasingInterval(
                    FloatInterval(-0.1f, 0.5f),
                    FloatInterval.ZERO,
                    FloatInterval.point(0.5f),
                    FloatInterval.point(1f),
                    FloatInterval.UNIT,
                )
            )
            .isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(
                FloatAnimationBounds.evalCubicEasingInterval(
                    FloatInterval.point(0.4f),
                    FloatInterval.point(0f),
                    FloatInterval.point(0.2f),
                    FloatInterval.point(1f),
                    FloatInterval.EMPTY,
                )
            )
            .isEqualTo(FloatInterval.EMPTY)

        // Citardauq quadratic root stability regression for large equal y1 == y2
        val hugePeak = FloatAnimationBounds.cubicBezierYBounds(1e16f, 1e16f)
        assertWithMessage(
                "Expected peak 7.5e15f in cubicBezierYBounds(1e16f, 1e16f), got $hugePeak"
            )
            .that(7.5e15f in hugePeak)
            .isTrue()
        val hugeNeg = FloatAnimationBounds.cubicBezierYBounds(-1e6f, -1e6f)
        assertThat(-7.5e5f in hugeNeg).isTrue()
        assertThat(hugeNeg.max).isGreaterThan(1f)

        // Unbounded control points widen soundly to UNBOUNDED
        assertThat(
                FloatAnimationBounds.evalCubicEasingInterval(
                    x1 = FloatInterval.point(0.4f),
                    y1 = FloatInterval.UNBOUNDED,
                    x2 = FloatInterval.point(0.2f),
                    y2 = FloatInterval.point(1f),
                    pos = FloatInterval.point(0.5f),
                )
            )
            .isEqualTo(FloatInterval.UNBOUNDED)
    }

    @Test
    fun parsedAnimationSpec_canonicalizesZeroAndNanInEqualsAndHashCode() {
        val posZero = ParsedAnimationSpec(FloatInterval.UNIT, 0.0f)
        val negZero = ParsedAnimationSpec(FloatInterval.UNIT, -0.0f)
        assertThat(posZero).isEqualTo(negZero)
        assertThat(posZero.hashCode()).isEqualTo(negZero.hashCode())

        val nanA = ParsedAnimationSpec(FloatInterval.UNIT, Float.NaN)
        val nanB = ParsedAnimationSpec(FloatInterval.UNIT, Float.fromBits(0x7FC01234))
        assertThat(nanA).isEqualTo(nanA)
        assertThat(nanA).isEqualTo(nanB)
        assertThat(nanA.hashCode()).isEqualTo(nanB.hashCode())

        val nullInit = ParsedAnimationSpec(FloatInterval.UNIT, null)
        assertThat(nullInit).isNotEqualTo(posZero)
    }
}
