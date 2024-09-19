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

package androidx.ink.brush

import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushBehaviorTest {

    @Test
    fun sourceConstants_areDistinct() {
        val list =
            listOf<BrushBehavior.Source>(
                BrushBehavior.Source.CONSTANT_ZERO,
                BrushBehavior.Source.NORMALIZED_PRESSURE,
                BrushBehavior.Source.TILT_IN_RADIANS,
                BrushBehavior.Source.TILT_X_IN_RADIANS,
                BrushBehavior.Source.TILT_Y_IN_RADIANS,
                BrushBehavior.Source.ORIENTATION_IN_RADIANS,
                BrushBehavior.Source.ORIENTATION_ABOUT_ZERO_IN_RADIANS,
                BrushBehavior.Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
                BrushBehavior.Source.VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
                BrushBehavior.Source.VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
                BrushBehavior.Source.DIRECTION_IN_RADIANS,
                BrushBehavior.Source.DIRECTION_ABOUT_ZERO_IN_RADIANS,
                BrushBehavior.Source.NORMALIZED_DIRECTION_X,
                BrushBehavior.Source.NORMALIZED_DIRECTION_Y,
                BrushBehavior.Source.DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.Source.TIME_OF_INPUT_IN_SECONDS,
                BrushBehavior.Source.TIME_OF_INPUT_IN_MILLIS,
                BrushBehavior.Source.PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_SECONDS,
                BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_MILLIS,
                BrushBehavior.Source.DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.Source.TIME_SINCE_INPUT_IN_SECONDS,
                BrushBehavior.Source.TIME_SINCE_INPUT_IN_MILLIS,
                BrushBehavior.Source.ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
                BrushBehavior.Source.ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
                BrushBehavior.Source.ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
                BrushBehavior.Source
                    .ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
                BrushBehavior.Source
                    .ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
                BrushBehavior.Source.INPUT_SPEED_IN_CENTIMETERS_PER_SECOND,
                BrushBehavior.Source.INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND,
                BrushBehavior.Source.INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND,
                BrushBehavior.Source.INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS,
                BrushBehavior.Source.PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS,
                BrushBehavior.Source.INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED,
                BrushBehavior.Source.INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED,
                BrushBehavior.Source.INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED,
                BrushBehavior.Source.INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED,
                BrushBehavior.Source.INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED,
            )
        assertThat(list.toSet()).hasSize(list.size)
    }

    @Test
    fun sourceHashCode_withIdenticalValues_match() {
        assertThat(BrushBehavior.Source.NORMALIZED_PRESSURE.hashCode())
            .isEqualTo(BrushBehavior.Source.NORMALIZED_PRESSURE.hashCode())

        assertThat(BrushBehavior.Source.TILT_IN_RADIANS.hashCode())
            .isNotEqualTo(BrushBehavior.Source.NORMALIZED_PRESSURE.hashCode())
    }

    @Test
    fun sourceEquals_checksEqualityOfValues() {
        assertThat(BrushBehavior.Source.NORMALIZED_PRESSURE)
            .isEqualTo(BrushBehavior.Source.NORMALIZED_PRESSURE)

        assertThat(BrushBehavior.Source.TILT_IN_RADIANS)
            .isNotEqualTo(BrushBehavior.Source.NORMALIZED_PRESSURE)
        assertThat(BrushBehavior.Source.TILT_IN_RADIANS).isNotEqualTo(null)
    }

    @Test
    fun sourceToString_returnsCorrectString() {
        assertThat(BrushBehavior.Source.CONSTANT_ZERO.toString())
            .isEqualTo("BrushBehavior.Source.CONSTANT_ZERO")
        assertThat(BrushBehavior.Source.NORMALIZED_PRESSURE.toString())
            .isEqualTo("BrushBehavior.Source.NORMALIZED_PRESSURE")
        assertThat(BrushBehavior.Source.TILT_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Source.TILT_IN_RADIANS")
        assertThat(BrushBehavior.Source.TILT_X_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Source.TILT_X_IN_RADIANS")
        assertThat(BrushBehavior.Source.TILT_Y_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Source.TILT_Y_IN_RADIANS")
        assertThat(BrushBehavior.Source.ORIENTATION_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Source.ORIENTATION_IN_RADIANS")
        assertThat(BrushBehavior.Source.ORIENTATION_ABOUT_ZERO_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Source.ORIENTATION_ABOUT_ZERO_IN_RADIANS")
        assertThat(BrushBehavior.Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND.toString())
            .isEqualTo("BrushBehavior.Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
        assertThat(BrushBehavior.Source.VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND.toString())
            .isEqualTo("BrushBehavior.Source.VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
        assertThat(BrushBehavior.Source.VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND.toString())
            .isEqualTo("BrushBehavior.Source.VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
        assertThat(BrushBehavior.Source.DIRECTION_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Source.DIRECTION_IN_RADIANS")
        assertThat(BrushBehavior.Source.DIRECTION_ABOUT_ZERO_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Source.DIRECTION_ABOUT_ZERO_IN_RADIANS")
        assertThat(BrushBehavior.Source.NORMALIZED_DIRECTION_X.toString())
            .isEqualTo("BrushBehavior.Source.NORMALIZED_DIRECTION_X")
        assertThat(BrushBehavior.Source.NORMALIZED_DIRECTION_Y.toString())
            .isEqualTo("BrushBehavior.Source.NORMALIZED_DIRECTION_Y")
        assertThat(BrushBehavior.Source.DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("BrushBehavior.Source.DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(BrushBehavior.Source.TIME_OF_INPUT_IN_SECONDS.toString())
            .isEqualTo("BrushBehavior.Source.TIME_OF_INPUT_IN_SECONDS")
        assertThat(BrushBehavior.Source.TIME_OF_INPUT_IN_MILLIS.toString())
            .isEqualTo("BrushBehavior.Source.TIME_OF_INPUT_IN_MILLIS")
        assertThat(
                BrushBehavior.Source.PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE"
            )
        assertThat(BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_SECONDS.toString())
            .isEqualTo("BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_SECONDS")
        assertThat(BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_MILLIS.toString())
            .isEqualTo("BrushBehavior.Source.PREDICTED_TIME_ELAPSED_IN_MILLIS")
        assertThat(BrushBehavior.Source.DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("BrushBehavior.Source.DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(BrushBehavior.Source.TIME_SINCE_INPUT_IN_SECONDS.toString())
            .isEqualTo("BrushBehavior.Source.TIME_SINCE_INPUT_IN_SECONDS")
        assertThat(BrushBehavior.Source.TIME_SINCE_INPUT_IN_MILLIS.toString())
            .isEqualTo("BrushBehavior.Source.TIME_SINCE_INPUT_IN_MILLIS")
        assertThat(
                BrushBehavior.Source.ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
            )
        assertThat(
                BrushBehavior.Source.ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
            )
        assertThat(
                BrushBehavior.Source.ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
            )
        assertThat(
                BrushBehavior.Source
                    .ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
            )
        assertThat(
                BrushBehavior.Source
                    .ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
            )
        assertThat(BrushBehavior.Source.INPUT_SPEED_IN_CENTIMETERS_PER_SECOND.toString())
            .isEqualTo("BrushBehavior.Source.INPUT_SPEED_IN_CENTIMETERS_PER_SECOND")
        assertThat(BrushBehavior.Source.INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND.toString())
            .isEqualTo("BrushBehavior.Source.INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND")
        assertThat(BrushBehavior.Source.INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND.toString())
            .isEqualTo("BrushBehavior.Source.INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND")
        assertThat(BrushBehavior.Source.INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS.toString())
            .isEqualTo("BrushBehavior.Source.INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS")
        assertThat(BrushBehavior.Source.PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS.toString())
            .isEqualTo("BrushBehavior.Source.PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS")
        assertThat(
                BrushBehavior.Source.INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED.toString()
            )
            .isEqualTo("BrushBehavior.Source.INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED")
        assertThat(
                BrushBehavior.Source.INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED"
            )
        assertThat(
                BrushBehavior.Source.INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED"
            )
        assertThat(
                BrushBehavior.Source.INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED"
            )
        assertThat(
                BrushBehavior.Source.INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.Source.INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED"
            )
    }

    @Test
    fun targetConstants_areDistinct() {
        val list =
            listOf<BrushBehavior.Target>(
                BrushBehavior.Target.WIDTH_MULTIPLIER,
                BrushBehavior.Target.HEIGHT_MULTIPLIER,
                BrushBehavior.Target.SIZE_MULTIPLIER,
                BrushBehavior.Target.SLANT_OFFSET_IN_RADIANS,
                BrushBehavior.Target.PINCH_OFFSET,
                BrushBehavior.Target.ROTATION_OFFSET_IN_RADIANS,
                BrushBehavior.Target.CORNER_ROUNDING_OFFSET,
                BrushBehavior.Target.POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.Target.POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.Target.POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.Target.POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.Target.HUE_OFFSET_IN_RADIANS,
                BrushBehavior.Target.SATURATION_MULTIPLIER,
                BrushBehavior.Target.LUMINOSITY,
                BrushBehavior.Target.OPACITY_MULTIPLIER,
            )
        assertThat(list.toSet()).hasSize(list.size)
    }

    @Test
    fun targetHashCode_withIdenticalValues_match() {
        assertThat(BrushBehavior.Target.WIDTH_MULTIPLIER.hashCode())
            .isEqualTo(BrushBehavior.Target.WIDTH_MULTIPLIER.hashCode())

        assertThat(BrushBehavior.Target.WIDTH_MULTIPLIER.hashCode())
            .isNotEqualTo(BrushBehavior.Target.HEIGHT_MULTIPLIER.hashCode())
    }

    @Test
    fun targetEquals_checksEqualityOfValues() {
        assertThat(BrushBehavior.Target.WIDTH_MULTIPLIER)
            .isEqualTo(BrushBehavior.Target.WIDTH_MULTIPLIER)

        assertThat(BrushBehavior.Target.WIDTH_MULTIPLIER)
            .isNotEqualTo(BrushBehavior.Target.HEIGHT_MULTIPLIER)
        assertThat(BrushBehavior.Target.WIDTH_MULTIPLIER).isNotEqualTo(null)
    }

    @Test
    fun targetToString_returnsCorrectString() {
        assertThat(BrushBehavior.Target.WIDTH_MULTIPLIER.toString())
            .isEqualTo("BrushBehavior.Target.WIDTH_MULTIPLIER")
        assertThat(BrushBehavior.Target.HEIGHT_MULTIPLIER.toString())
            .isEqualTo("BrushBehavior.Target.HEIGHT_MULTIPLIER")
        assertThat(BrushBehavior.Target.SIZE_MULTIPLIER.toString())
            .isEqualTo("BrushBehavior.Target.SIZE_MULTIPLIER")
        assertThat(BrushBehavior.Target.SLANT_OFFSET_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Target.SLANT_OFFSET_IN_RADIANS")
        assertThat(BrushBehavior.Target.PINCH_OFFSET.toString())
            .isEqualTo("BrushBehavior.Target.PINCH_OFFSET")
        assertThat(BrushBehavior.Target.ROTATION_OFFSET_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Target.ROTATION_OFFSET_IN_RADIANS")
        assertThat(BrushBehavior.Target.CORNER_ROUNDING_OFFSET.toString())
            .isEqualTo("BrushBehavior.Target.CORNER_ROUNDING_OFFSET")
        assertThat(BrushBehavior.Target.POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("BrushBehavior.Target.POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(BrushBehavior.Target.POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("BrushBehavior.Target.POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(
                BrushBehavior.Target.POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE.toString()
            )
            .isEqualTo("BrushBehavior.Target.POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(
                BrushBehavior.Target.POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE.toString()
            )
            .isEqualTo("BrushBehavior.Target.POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(BrushBehavior.Target.HUE_OFFSET_IN_RADIANS.toString())
            .isEqualTo("BrushBehavior.Target.HUE_OFFSET_IN_RADIANS")
        assertThat(BrushBehavior.Target.SATURATION_MULTIPLIER.toString())
            .isEqualTo("BrushBehavior.Target.SATURATION_MULTIPLIER")
        assertThat(BrushBehavior.Target.LUMINOSITY.toString())
            .isEqualTo("BrushBehavior.Target.LUMINOSITY")
        assertThat(BrushBehavior.Target.OPACITY_MULTIPLIER.toString())
            .isEqualTo("BrushBehavior.Target.OPACITY_MULTIPLIER")
    }

    @Test
    fun outOfRangeConstants_areDistinct() {
        val list =
            listOf<BrushBehavior.OutOfRange>(
                BrushBehavior.OutOfRange.CLAMP,
                BrushBehavior.OutOfRange.REPEAT,
                BrushBehavior.OutOfRange.MIRROR,
            )
        assertThat(list.toSet()).hasSize(list.size)
    }

    @Test
    fun outOfRangeHashCode_withIdenticalValues_match() {
        assertThat(BrushBehavior.OutOfRange.CLAMP.hashCode())
            .isEqualTo(BrushBehavior.OutOfRange.CLAMP.hashCode())

        assertThat(BrushBehavior.OutOfRange.CLAMP.hashCode())
            .isNotEqualTo(BrushBehavior.OutOfRange.REPEAT.hashCode())
    }

    @Test
    fun outOfRangeEquals_checksEqualityOfValues() {
        assertThat(BrushBehavior.OutOfRange.CLAMP).isEqualTo(BrushBehavior.OutOfRange.CLAMP)

        assertThat(BrushBehavior.OutOfRange.CLAMP).isNotEqualTo(BrushBehavior.OutOfRange.REPEAT)
        assertThat(BrushBehavior.OutOfRange.CLAMP).isNotEqualTo(null)
    }

    @Test
    fun outOfRangeToString_returnsCorrectString() {
        assertThat(BrushBehavior.OutOfRange.CLAMP.toString())
            .isEqualTo("BrushBehavior.OutOfRange.CLAMP")
        assertThat(BrushBehavior.OutOfRange.REPEAT.toString())
            .isEqualTo("BrushBehavior.OutOfRange.REPEAT")
        assertThat(BrushBehavior.OutOfRange.MIRROR.toString())
            .isEqualTo("BrushBehavior.OutOfRange.MIRROR")
    }

    @Test
    fun optionalInputPropertyConstants_areDistinct() {
        val list =
            listOf<BrushBehavior.OptionalInputProperty>(
                BrushBehavior.OptionalInputProperty.PRESSURE,
                BrushBehavior.OptionalInputProperty.TILT,
                BrushBehavior.OptionalInputProperty.ORIENTATION,
                BrushBehavior.OptionalInputProperty.TILT_X_AND_Y,
            )
        assertThat(list.toSet()).hasSize(list.size)
    }

    @Test
    fun optionalInputPropertyHashCode_withIdenticalValues_match() {
        assertThat(BrushBehavior.OptionalInputProperty.PRESSURE.hashCode())
            .isEqualTo(BrushBehavior.OptionalInputProperty.PRESSURE.hashCode())

        assertThat(BrushBehavior.OptionalInputProperty.PRESSURE.hashCode())
            .isNotEqualTo(BrushBehavior.OptionalInputProperty.TILT.hashCode())
    }

    @Test
    fun optionalInputPropertyEquals_checksEqualityOfValues() {
        assertThat(BrushBehavior.OptionalInputProperty.PRESSURE)
            .isEqualTo(BrushBehavior.OptionalInputProperty.PRESSURE)

        assertThat(BrushBehavior.OptionalInputProperty.PRESSURE)
            .isNotEqualTo(BrushBehavior.OptionalInputProperty.TILT)
        assertThat(BrushBehavior.OptionalInputProperty.PRESSURE).isNotEqualTo(null)
    }

    @Test
    fun optionalInputPropertyToString_returnsCorrectString() {
        assertThat(BrushBehavior.OptionalInputProperty.PRESSURE.toString())
            .isEqualTo("BrushBehavior.OptionalInputProperty.PRESSURE")
        assertThat(BrushBehavior.OptionalInputProperty.TILT.toString())
            .isEqualTo("BrushBehavior.OptionalInputProperty.TILT")
        assertThat(BrushBehavior.OptionalInputProperty.ORIENTATION.toString())
            .isEqualTo("BrushBehavior.OptionalInputProperty.ORIENTATION")
        assertThat(BrushBehavior.OptionalInputProperty.TILT_X_AND_Y.toString())
            .isEqualTo("BrushBehavior.OptionalInputProperty.TILT_X_AND_Y")
    }

    @Test
    fun binaryOpConstants_areDistinct() {
        val list =
            listOf<BrushBehavior.BinaryOp>(
                BrushBehavior.BinaryOp.PRODUCT,
                BrushBehavior.BinaryOp.SUM
            )
        assertThat(list.toSet()).hasSize(list.size)
    }

    @Test
    fun binaryOpHashCode_withIdenticalValues_match() {
        assertThat(BrushBehavior.BinaryOp.PRODUCT.hashCode())
            .isEqualTo(BrushBehavior.BinaryOp.PRODUCT.hashCode())

        assertThat(BrushBehavior.BinaryOp.PRODUCT.hashCode())
            .isNotEqualTo(BrushBehavior.BinaryOp.SUM.hashCode())
    }

    @Test
    fun binaryOpEquals_checksEqualityOfValues() {
        assertThat(BrushBehavior.BinaryOp.PRODUCT).isEqualTo(BrushBehavior.BinaryOp.PRODUCT)

        assertThat(BrushBehavior.BinaryOp.PRODUCT).isNotEqualTo(BrushBehavior.BinaryOp.SUM)
        assertThat(BrushBehavior.BinaryOp.PRODUCT).isNotEqualTo(null)
    }

    @Test
    fun binaryOpToString_returnsCorrectString() {
        assertThat(BrushBehavior.BinaryOp.PRODUCT.toString())
            .isEqualTo("BrushBehavior.BinaryOp.PRODUCT")
        assertThat(BrushBehavior.BinaryOp.SUM.toString()).isEqualTo("BrushBehavior.BinaryOp.SUM")
    }

    @Test
    fun dampingSourceConstants_areDistinct() {
        val list =
            listOf<BrushBehavior.DampingSource>(
                BrushBehavior.DampingSource.DISTANCE_IN_CENTIMETERS,
                BrushBehavior.DampingSource.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE,
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
            )
        assertThat(list.toSet()).hasSize(list.size)
    }

    @Test
    fun dampingSourceHashCode_withIdenticalValues_match() {
        assertThat(BrushBehavior.DampingSource.TIME_IN_SECONDS.hashCode())
            .isEqualTo(BrushBehavior.DampingSource.TIME_IN_SECONDS.hashCode())

        assertThat(BrushBehavior.DampingSource.TIME_IN_SECONDS.hashCode())
            .isNotEqualTo(
                BrushBehavior.DampingSource.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE.hashCode()
            )
    }

    @Test
    fun dampingSourceEquals_checksEqualityOfValues() {
        assertThat(BrushBehavior.DampingSource.TIME_IN_SECONDS)
            .isEqualTo(BrushBehavior.DampingSource.TIME_IN_SECONDS)

        assertThat(BrushBehavior.DampingSource.TIME_IN_SECONDS)
            .isNotEqualTo(BrushBehavior.DampingSource.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
        assertThat(BrushBehavior.DampingSource.TIME_IN_SECONDS).isNotEqualTo(null)
    }

    @Test
    fun dampingSourceToString_returnsCorrectString() {
        assertThat(BrushBehavior.DampingSource.DISTANCE_IN_CENTIMETERS.toString())
            .isEqualTo("BrushBehavior.DampingSource.DISTANCE_IN_CENTIMETERS")
        assertThat(BrushBehavior.DampingSource.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("BrushBehavior.DampingSource.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(BrushBehavior.DampingSource.TIME_IN_SECONDS.toString())
            .isEqualTo("BrushBehavior.DampingSource.TIME_IN_SECONDS")
    }

    @Test
    fun interpolationConstants_areDistinct() {
        val list =
            listOf<BrushBehavior.Interpolation>(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.Interpolation.INVERSE_LERP,
            )
        assertThat(list.toSet()).hasSize(list.size)
    }

    @Test
    fun interpolationHashCode_withIdenticalValues_match() {
        assertThat(BrushBehavior.Interpolation.LERP.hashCode())
            .isEqualTo(BrushBehavior.Interpolation.LERP.hashCode())

        assertThat(BrushBehavior.Interpolation.LERP.hashCode())
            .isNotEqualTo(BrushBehavior.Interpolation.INVERSE_LERP.hashCode())
    }

    @Test
    fun interpolationEquals_checksEqualityOfValues() {
        assertThat(BrushBehavior.Interpolation.LERP).isEqualTo(BrushBehavior.Interpolation.LERP)

        assertThat(BrushBehavior.Interpolation.LERP)
            .isNotEqualTo(BrushBehavior.Interpolation.INVERSE_LERP)
        assertThat(BrushBehavior.Interpolation.LERP).isNotEqualTo(null)
    }

    @Test
    fun interpolationToString_returnsCorrectString() {
        assertThat(BrushBehavior.Interpolation.LERP.toString())
            .isEqualTo("BrushBehavior.Interpolation.LERP")
        assertThat(BrushBehavior.Interpolation.INVERSE_LERP.toString())
            .isEqualTo("BrushBehavior.Interpolation.INVERSE_LERP")
    }

    @Test
    fun sourceNodeConstructor_throwsForNonFiniteSourceValueRange() {
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, Float.NaN, 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.SourceNode(
                BrushBehavior.Source.NORMALIZED_PRESSURE,
                0f,
                Float.POSITIVE_INFINITY,
            )
        }
    }

    @Test
    fun sourceNodeConstructor_throwsForEmptySourceValueRange() {
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0.5f, 0.5f)
        }
    }

    @Test
    fun sourceNodeInputs_isEmpty() {
        val node = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 1f)
        assertThat(node.inputs).isEmpty()
    }

    @Test
    fun sourceNodeToString() {
        val node = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 1f)
        assertThat(node.toString()).isEqualTo("SourceNode(NORMALIZED_PRESSURE, 0.0, 1.0, CLAMP)")
    }

    @Test
    fun sourceNodeEquals_checksEqualityOfValues() {
        val node1 = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node2 = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node3 = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 2f)
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun sourceNodeHashCode_withIdenticalValues_match() {
        val node1 = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node2 = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node3 = BrushBehavior.SourceNode(BrushBehavior.Source.NORMALIZED_PRESSURE, 0f, 2f)
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun constantNodeConstructor_throwsForNonFiniteValue() {
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.ConstantNode(Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> { BrushBehavior.ConstantNode(Float.NaN) }
    }

    @Test
    fun constantNodeInputs_isEmpty() {
        assertThat(BrushBehavior.ConstantNode(42f).inputs).isEmpty()
    }

    @Test
    fun constantNodeToString() {
        assertThat(BrushBehavior.ConstantNode(42f).toString()).isEqualTo("ConstantNode(42.0)")
    }

    @Test
    fun constantNodeEquals_checksEqualityOfValues() {
        val node1 = BrushBehavior.ConstantNode(1f)
        val node2 = BrushBehavior.ConstantNode(1f)
        val node3 = BrushBehavior.ConstantNode(2f)
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun constantNodeHashCode_withIdenticalValues_match() {
        val node1 = BrushBehavior.ConstantNode(1f)
        val node2 = BrushBehavior.ConstantNode(1f)
        val node3 = BrushBehavior.ConstantNode(2f)
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun fallbackFilterNodeInputs_containsInput() {
        val input = BrushBehavior.ConstantNode(0f)
        val node =
            BrushBehavior.FallbackFilterNode(BrushBehavior.OptionalInputProperty.PRESSURE, input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun fallbackFilterNodeToString() {
        val input = BrushBehavior.ConstantNode(0f)
        val node =
            BrushBehavior.FallbackFilterNode(BrushBehavior.OptionalInputProperty.PRESSURE, input)
        assertThat(node.toString()).isEqualTo("FallbackFilterNode(PRESSURE, ConstantNode(0.0))")
    }

    @Test
    fun fallbackFilterNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.FallbackFilterNode(
                BrushBehavior.OptionalInputProperty.PRESSURE,
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.FallbackFilterNode(
                BrushBehavior.OptionalInputProperty.PRESSURE,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.FallbackFilterNode(
                BrushBehavior.OptionalInputProperty.PRESSURE,
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun fallbackFilterNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.FallbackFilterNode(
                BrushBehavior.OptionalInputProperty.PRESSURE,
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.FallbackFilterNode(
                BrushBehavior.OptionalInputProperty.PRESSURE,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.FallbackFilterNode(
                BrushBehavior.OptionalInputProperty.PRESSURE,
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun toolTypeFilterNodeConstructor_throwsForEmptyEnabledToolTypes() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.ToolTypeFilterNode(emptySet(), input)
        }
    }

    @Test
    fun toolTypeFilterNodeInputs_containsInput() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.ToolTypeFilterNode(setOf(InputToolType.STYLUS), input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun toolTypeFilterNodeToString() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.ToolTypeFilterNode(setOf(InputToolType.STYLUS), input)
        assertThat(node.toString())
            .isEqualTo("ToolTypeFilterNode([InputToolType.STYLUS], ConstantNode(0.0))")
    }

    @Test
    fun toolTypeFilterNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(1f)
            )
        val node2 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(1f)
            )
        val node3 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(2f)
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun toolTypeFilterNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(1f)
            )
        val node2 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(1f)
            )
        val node3 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(2f)
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun dampingNodeConstructor_throwsForNonFiniteDampingGap() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.DampingNode(
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                Float.POSITIVE_INFINITY,
                input,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.DampingNode(BrushBehavior.DampingSource.TIME_IN_SECONDS, Float.NaN, input)
        }
    }

    @Test
    fun dampingNodeConstructor_throwsForNegativeDampingGap() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.DampingNode(BrushBehavior.DampingSource.TIME_IN_SECONDS, -1f, input)
        }
    }

    @Test
    fun dampingNodeInputs_containsInput() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.DampingNode(BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f, input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun dampingNodeToString() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.DampingNode(BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f, input)
        assertThat(node.toString())
            .isEqualTo("DampingNode(TIME_IN_SECONDS, 1.0, ConstantNode(0.0))")
    }

    @Test
    fun dampingNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.DampingNode(
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.DampingNode(
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.DampingNode(
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                1f,
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun dampingNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.DampingNode(
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.DampingNode(
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.DampingNode(
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                1f,
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun responseNodeInputs_containsInput() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.ResponseNode(EasingFunction.Predefined.EASE, input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun responseNodeToString() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.ResponseNode(EasingFunction.Predefined.EASE, input)
        assertThat(node.toString())
            .isEqualTo("ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(0.0))")
    }

    @Test
    fun responseNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(1f)
            )
        val node2 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(1f)
            )
        val node3 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(2f)
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun responseNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(1f)
            )
        val node2 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(1f)
            )
        val node3 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(2f)
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun binaryOpNodeInputs_containsInputsInOrder() {
        val firstInput = BrushBehavior.ConstantNode(0f)
        val secondInput = BrushBehavior.ConstantNode(1f)
        val node = BrushBehavior.BinaryOpNode(BrushBehavior.BinaryOp.SUM, firstInput, secondInput)
        assertThat(node.inputs).containsExactly(firstInput, secondInput).inOrder()
    }

    @Test
    fun binaryOpNodeToString() {
        val firstInput = BrushBehavior.ConstantNode(0f)
        val secondInput = BrushBehavior.ConstantNode(1f)
        val node = BrushBehavior.BinaryOpNode(BrushBehavior.BinaryOp.SUM, firstInput, secondInput)
        assertThat(node.toString())
            .isEqualTo("BinaryOpNode(SUM, ConstantNode(0.0), ConstantNode(1.0))")
    }

    @Test
    fun binaryOpNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.BinaryOpNode(
                BrushBehavior.BinaryOp.SUM,
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.BinaryOpNode(
                BrushBehavior.BinaryOp.SUM,
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.BinaryOpNode(
                BrushBehavior.BinaryOp.SUM,
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun binaryOpNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.BinaryOpNode(
                BrushBehavior.BinaryOp.SUM,
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.BinaryOpNode(
                BrushBehavior.BinaryOp.SUM,
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.BinaryOpNode(
                BrushBehavior.BinaryOp.SUM,
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun interpolationNodeInputs_containsInputsInOrder() {
        val paramInput = BrushBehavior.ConstantNode(0.5f)
        val startInput = BrushBehavior.ConstantNode(0f)
        val endInput = BrushBehavior.ConstantNode(1f)
        val node =
            BrushBehavior.InterpolationNode(
                interpolation = BrushBehavior.Interpolation.LERP,
                paramInput = paramInput,
                startInput = startInput,
                endInput = endInput,
            )
        assertThat(node.inputs).containsExactly(paramInput, startInput, endInput).inOrder()
    }

    @Test
    fun interpolationNodeToString() {
        val node =
            BrushBehavior.InterpolationNode(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.ConstantNode(0.5f),
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        assertThat(node.toString())
            .isEqualTo(
                "InterpolationNode(LERP, ConstantNode(0.5), ConstantNode(0.0), ConstantNode(1.0))"
            )
    }

    @Test
    fun interpolationNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.InterpolationNode(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.ConstantNode(0.5f),
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.InterpolationNode(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.ConstantNode(0.5f),
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.InterpolationNode(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.ConstantNode(0.5f),
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun interpolationNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.InterpolationNode(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.ConstantNode(0.5f),
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.InterpolationNode(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.ConstantNode(0.5f),
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.InterpolationNode(
                BrushBehavior.Interpolation.LERP,
                BrushBehavior.ConstantNode(0.5f),
                BrushBehavior.ConstantNode(0f),
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun targetNodeConstructor_throwsForNonFiniteTargetModifierRange() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.TargetNode(BrushBehavior.Target.SIZE_MULTIPLIER, Float.NaN, 1f, input)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.TargetNode(
                BrushBehavior.Target.SIZE_MULTIPLIER,
                0f,
                Float.POSITIVE_INFINITY,
                input,
            )
        }
    }

    @Test
    fun targetNodeConstructor_throwsForEmptyTargetModifierRange() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.TargetNode(BrushBehavior.Target.SIZE_MULTIPLIER, 0.5f, 0.5f, input)
        }
    }

    @Test
    fun targetNodeInputs_containsInput() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.TargetNode(BrushBehavior.Target.SIZE_MULTIPLIER, 0f, 1f, input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun targetNodeToString() {
        val input = BrushBehavior.ConstantNode(0f)
        val node = BrushBehavior.TargetNode(BrushBehavior.Target.SIZE_MULTIPLIER, 0f, 1f, input)
        assertThat(node.toString())
            .isEqualTo("TargetNode(SIZE_MULTIPLIER, 0.0, 1.0, ConstantNode(0.0))")
    }

    @Test
    fun targetNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.TargetNode(
                BrushBehavior.Target.SIZE_MULTIPLIER,
                0f,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.TargetNode(
                BrushBehavior.Target.SIZE_MULTIPLIER,
                0f,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.TargetNode(
                BrushBehavior.Target.SIZE_MULTIPLIER,
                0f,
                1f,
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun targetNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.TargetNode(
                BrushBehavior.Target.SIZE_MULTIPLIER,
                0f,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.TargetNode(
                BrushBehavior.Target.SIZE_MULTIPLIER,
                0f,
                1f,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.TargetNode(
                BrushBehavior.Target.SIZE_MULTIPLIER,
                0f,
                1f,
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun brushBehaviorConstructor_withInvalidArguments_throws() {
        // sourceValueRangeLowerBound not finite
        val sourceValueRangeLowerBoundError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = Float.NaN, // Not finite.
                    sourceValueRangeUpperBound = 1.0f,
                    targetModifierRangeLowerBound = 1.0f,
                    targetModifierRangeUpperBound = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(sourceValueRangeLowerBoundError.message).contains("source")
        assertThat(sourceValueRangeLowerBoundError.message).contains("finite")

        // sourceValueRangeUpperBound not finite
        val sourceValueRangeUpperBoundError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 1.0f,
                    sourceValueRangeUpperBound = Float.NaN, // Not finite.
                    targetModifierRangeLowerBound = 1.0f,
                    targetModifierRangeUpperBound = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(sourceValueRangeUpperBoundError.message).contains("source")
        assertThat(sourceValueRangeUpperBoundError.message).contains("finite")

        // sourceValueRangeUpperBound == sourceValueRangeUpperBound
        val sourceValueRangeError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 0.5f, // same as upper bound.
                    sourceValueRangeUpperBound = 0.5f, // same as lower bound.
                    targetModifierRangeLowerBound = 1.0f,
                    targetModifierRangeUpperBound = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(sourceValueRangeError.message).contains("source")
        assertThat(sourceValueRangeError.message).contains("distinct")

        // targetModifierRangeLowerBound not finite
        val targetModifierRangeLowerBoundError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 0.2f,
                    sourceValueRangeUpperBound = .8f,
                    targetModifierRangeLowerBound = Float.NaN, // Not finite.
                    targetModifierRangeUpperBound = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(targetModifierRangeLowerBoundError.message).contains("target")
        assertThat(targetModifierRangeLowerBoundError.message).contains("finite")

        // targetModifierRangeUpperBound not finite
        val targetModifierRangeUpperBoundError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 0.2f,
                    sourceValueRangeUpperBound = .8f,
                    targetModifierRangeLowerBound = 1.0f,
                    targetModifierRangeUpperBound = Float.NaN, // Not finite.
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(targetModifierRangeUpperBoundError.message).contains("target")
        assertThat(targetModifierRangeUpperBoundError.message).contains("finite")

        // responseTimeMillis less than 0L
        val responseTimeMillisError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 0.2f,
                    sourceValueRangeUpperBound = .8f,
                    targetModifierRangeLowerBound = 1.0f,
                    targetModifierRangeUpperBound = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = -1L, // Less than 0.
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(responseTimeMillisError.message).contains("dampingGap")
        assertThat(responseTimeMillisError.message).contains("non-negative")

        // enabledToolType contains empty set.
        val enabledToolTypeError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 0.2f,
                    sourceValueRangeUpperBound = .8f,
                    targetModifierRangeLowerBound = 1.0f,
                    targetModifierRangeUpperBound = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(),
                )
            }
        assertThat(enabledToolTypeError.message).contains("enabledToolTypes")
        assertThat(enabledToolTypeError.message).contains("non-empty")

        // source and outOfRangeBehavior combination is invalid (TIME_SINCE_INPUT must use CLAMP)
        val sourceOutOfRangeBehaviorError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.TIME_SINCE_INPUT_IN_SECONDS,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 0.2f,
                    sourceValueRangeUpperBound = .8f,
                    targetModifierRangeLowerBound = 1.0f,
                    targetModifierRangeUpperBound = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.REPEAT,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(sourceOutOfRangeBehaviorError.message).contains("TimeSince")
        assertThat(sourceOutOfRangeBehaviorError.message).contains("kClamp")
    }

    @Test
    fun brushBehaviorToString_returnsReasonableString() {
        assertThat(
                BrushBehavior(
                        listOf(
                            BrushBehavior.TargetNode(
                                target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                                targetModifierRangeLowerBound = 1.0f,
                                targetModifierRangeUpperBound = 1.75f,
                                input =
                                    BrushBehavior.SourceNode(
                                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                                        sourceValueRangeLowerBound = 0.0f,
                                        sourceValueRangeUpperBound = 1.0f,
                                    ),
                            )
                        )
                    )
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior([TargetNode(WIDTH_MULTIPLIER, 1.0, 1.75, " +
                    "SourceNode(NORMALIZED_PRESSURE, 0.0, 1.0, CLAMP))])"
            )
    }

    @Test
    fun brushBehaviorEquals_withIdenticalValues_returnsTrue() {
        val original =
            BrushBehavior(
                source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                sourceValueRangeLowerBound = 0.0f,
                sourceValueRangeUpperBound = 1.0f,
                targetModifierRangeLowerBound = 1.0f,
                targetModifierRangeUpperBound = 1.75f,
                sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                responseTimeMillis = 1L,
                enabledToolTypes = setOf(InputToolType.STYLUS),
            )

        val exact =
            BrushBehavior(
                source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                sourceValueRangeLowerBound = 0.0f,
                sourceValueRangeUpperBound = 1.0f,
                targetModifierRangeLowerBound = 1.0f,
                targetModifierRangeUpperBound = 1.75f,
                sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                responseTimeMillis = 1L,
                enabledToolTypes = setOf(InputToolType.STYLUS),
            )

        assertThat(original.equals(exact)).isTrue()
    }

    @Test
    fun brushBehaviorEquals_withDifferentValues_returnsFalse() {
        val original =
            BrushBehavior(
                source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                sourceValueRangeLowerBound = 0.0f,
                sourceValueRangeUpperBound = 1.0f,
                targetModifierRangeLowerBound = 1.0f,
                targetModifierRangeUpperBound = 1.75f,
                sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                responseTimeMillis = 1L,
                enabledToolTypes = setOf(InputToolType.STYLUS),
            )

        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.TILT_IN_RADIANS, // different
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.HEIGHT_MULTIPLIER, // different
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()

        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.REPEAT, // different
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.3f, // different
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 0.8f, // different
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.56f, // different
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.99f, // different
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.LINEAR, // different
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 35L, // different
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    BrushBehavior(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                        sourceValueRangeLowerBound = 0.0f,
                        sourceValueRangeUpperBound = 1.0f,
                        targetModifierRangeLowerBound = 1.0f,
                        targetModifierRangeUpperBound = 1.75f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        responseTimeMillis = 1L,
                        enabledToolTypes = setOf(InputToolType.TOUCH), // different
                    )
                )
            )
            .isFalse()
    }

    /**
     * Creates an expected C++ StepFunction BrushBehavior and returns true if every property of the
     * Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun matchesNativeStepBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ PredefinedFunction BrushBehavior and returns true if every property
     * of the Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun matchesNativePredefinedBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ CubicBezier BrushBehavior and returns true if every property of the
     * Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun matchesNativeCubicBezierBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ Linear BrushBehavior and returns true if every property of the Kotlin
     * BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++ BrushBehavior.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun matchesNativeLinearBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean
}
