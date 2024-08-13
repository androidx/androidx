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
        assertThat(responseTimeMillisError.message).contains("response_time")
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
        assertThat(enabledToolTypeError.message).contains("enabled_tool_types")
        assertThat(enabledToolTypeError.message).contains("at least one")

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
                    enabledToolTypes = setOf(),
                )
            }
        assertThat(sourceOutOfRangeBehaviorError.message).contains("TimeSince")
        assertThat(sourceOutOfRangeBehaviorError.message).contains("kClamp")
    }

    @Test
    fun brushBehaviorCopy_withArguments_createsCopyWithChanges() {
        val behavior1 =
            BrushBehavior(
                source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                sourceValueRangeLowerBound = 0.2f,
                sourceValueRangeUpperBound = .8f,
                targetModifierRangeLowerBound = 1.1f,
                targetModifierRangeUpperBound = 1.7f,
                sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                responseTimeMillis = 1L,
                enabledToolTypes = setOf(InputToolType.STYLUS),
                isFallbackFor = BrushBehavior.OptionalInputProperty.TILT_X_AND_Y,
            )
        assertThat(behavior1.copy(responseTimeMillis = 3L))
            .isEqualTo(
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeLowerBound = 0.2f,
                    sourceValueRangeUpperBound = .8f,
                    targetModifierRangeLowerBound = 1.1f,
                    targetModifierRangeUpperBound = 1.7f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 3L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                    isFallbackFor = BrushBehavior.OptionalInputProperty.TILT_X_AND_Y,
                )
            )
    }

    @Test
    fun brushBehaviorCopy_createsCopy() {
        val behavior1 =
            BrushBehavior(
                source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                sourceValueRangeLowerBound = 0.2f,
                sourceValueRangeUpperBound = .8f,
                targetModifierRangeLowerBound = 1.1f,
                targetModifierRangeUpperBound = 1.7f,
                sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                responseTimeMillis = 1L,
                enabledToolTypes = setOf(InputToolType.STYLUS),
                isFallbackFor = BrushBehavior.OptionalInputProperty.TILT_X_AND_Y,
            )
        val behavior2 = behavior1.copy()
        assertThat(behavior2).isEqualTo(behavior1)
        assertThat(behavior2).isNotSameInstanceAs(behavior1)
    }

    @Test
    fun brushBehaviorToString_returnsReasonableString() {
        assertThat(
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
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior(source=BrushBehavior.Source.NORMALIZED_PRESSURE, " +
                    "target=BrushBehavior.Target.WIDTH_MULTIPLIER, " +
                    "sourceOutOfRangeBehavior=BrushBehavior.OutOfRange.CLAMP, " +
                    "sourceValueRangeLowerBound=0.0, sourceValueRangeUpperBound=1.0, " +
                    "targetModifierRangeLowerBound=1.0, targetModifierRangeUpperBound=1.75, " +
                    "responseCurve=EasingFunction.Predefined.EASE_IN_OUT, responseTimeMillis=1, " +
                    "enabledToolTypes=[InputToolType.STYLUS], isFallbackFor=null)"
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
