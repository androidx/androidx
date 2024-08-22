/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImmutableParallelogramTest {

    @Test
    fun fromCenterAndDimensions_constructsCorrectImmutableParallelogram() {
        val parallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(ImmutableVec(10f, 0f), 6f, 4f)

        assertThat(parallelogram.center).isEqualTo(ImmutableVec(10f, 0f))
        assertThat(parallelogram.width).isEqualTo(6f)
        assertThat(parallelogram.height).isEqualTo(4f)
        assertThat(parallelogram.rotation).isZero()
        assertThat(parallelogram.shearFactor).isZero()
    }

    @Test
    fun fromCenterDimensionsAndRotation_constructsCorrectImmutableParallelogram() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsAndRotation(
                ImmutableVec(10f, 0f),
                6f,
                4f,
                Angle.FULL_TURN_RADIANS,
            )

        assertThat(parallelogram.center).isEqualTo(ImmutableVec(10f, 0f))
        assertThat(parallelogram.width).isEqualTo(6f)
        assertThat(parallelogram.height).isEqualTo(4f)
        assertThat(parallelogram.rotation).isZero()
        assertThat(parallelogram.shearFactor).isZero()
    }

    @Test
    fun fromCenterDimensionsRotationAndShear_constructsCorrectImmutableParallelogram() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(10f, 0f),
                6f,
                4f,
                Angle.HALF_TURN_RADIANS,
                1f,
            )

        assertThat(parallelogram.center).isEqualTo(ImmutableVec(10f, 0f))
        assertThat(parallelogram.width).isEqualTo(6f)
        assertThat(parallelogram.height).isEqualTo(4f)
        assertThat(parallelogram.rotation).isWithin(1e-6f).of(Math.PI.toFloat())
        assertThat(parallelogram.shearFactor).isEqualTo(1f)
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(10f, 10f),
                12f,
                2f,
                Angle.HALF_TURN_RADIANS,
                0f,
            )
        assertThat(parallelogram).isEqualTo(parallelogram)
        assertThat(parallelogram.hashCode()).isEqualTo(parallelogram.hashCode())
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )
        val other =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )

        assertThat(parallelogram).isEqualTo(other)
        assertThat(parallelogram.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenDifferentTypes_returnsFalse() {
        // An axis-aligned rectangle with center at (0,0) and width and height equal to 2
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(0f, 0f),
                2f,
                2f,
                Angle.ZERO,
                0f,
            )
        val other = ImmutableBox.fromTwoPoints(ImmutableVec(-1f, -1f), ImmutableVec(1f, 1f))

        assertThat(parallelogram).isNotEqualTo(other)
    }

    @Test
    fun equals_whenDifferentCenter_returnsFalse() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )
        val other =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(10f, -10.5f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )

        assertThat(parallelogram).isNotEqualTo(other)
    }

    @Test
    fun equals_whenDifferentWidth_returnsFalse() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                11f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )
        val other =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )

        assertThat(parallelogram).isNotEqualTo(other)
    }

    @Test
    fun equals_whenDifferentHeight_returnsFalse() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )
        val other =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )

        assertThat(parallelogram).isNotEqualTo(other)
    }

    @Test
    fun equals_whenDifferentRotation_returnsFalse() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )
        val other =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.QUARTER_TURN_RADIANS,
                -3f,
            )

        assertThat(parallelogram).isNotEqualTo(other)
    }

    @Test
    fun equals_whenDifferentShearFactor_returnsFalse() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                -3f,
            )
        val other =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(-10f, 10f),
                12f,
                -7.5f,
                Angle.HALF_TURN_RADIANS,
                0f,
            )

        assertThat(parallelogram).isNotEqualTo(other)
    }

    @Test
    fun getters_returnCorrectValues() {
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(3f, -5f),
                8f,
                -1f,
                Angle.HALF_TURN_RADIANS,
                0f,
            )

        assertThat(parallelogram.center).isEqualTo(ImmutableVec(3f, -5f))
        assertThat(parallelogram.width).isEqualTo(8f)
        assertThat(parallelogram.height).isEqualTo(-1f)
        assertThat(parallelogram.rotation).isEqualTo(Angle.HALF_TURN_RADIANS)
        assertThat(parallelogram.shearFactor).isEqualTo(0f)
    }

    @Test
    fun signedArea_returnsCorrectValue() {
        val parallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(ImmutableVec(0f, 10f), 6f, 4f)
        val degenerateParallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(ImmutableVec(0f, 10f), 0f, 4f)
        val negativeAreaParallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(ImmutableVec(0f, 10f), 2f, -3f)

        assertThat(parallelogram.computeSignedArea()).isEqualTo(24f)
        assertThat(degenerateParallelogram.computeSignedArea()).isZero()
        assertThat(negativeAreaParallelogram.computeSignedArea()).isEqualTo(-6f)
    }

    @Test
    fun toString_returnsCorrectValue() {
        val parallelogramString =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                    ImmutableVec(3f, -5f),
                    8f,
                    -1f,
                    Angle.HALF_TURN_RADIANS,
                    0.25f,
                )
                .toString()

        assertThat(parallelogramString).contains("ImmutableParallelogram")
        assertThat(parallelogramString).contains("center")
        assertThat(parallelogramString).contains("width")
        assertThat(parallelogramString).contains("height")
        assertThat(parallelogramString).contains("rotation")
        assertThat(parallelogramString).contains("shearFactor")
    }
}
