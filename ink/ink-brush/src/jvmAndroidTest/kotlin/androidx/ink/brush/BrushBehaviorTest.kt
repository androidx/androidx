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

import androidx.ink.nativeloader.UsedByNative
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
    fun sourceToString_returnsCorrectString() {
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
        assertThat(BrushBehavior.Source.DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH.toString())
            .isEqualTo("BrushBehavior.Source.DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH")
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
        assertThat(BrushBehavior.Target.TEXTURE_ANIMATION_PROGRESS_OFFSET.toString())
            .isEqualTo("BrushBehavior.Target.TEXTURE_ANIMATION_PROGRESS_OFFSET")
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
    fun polarTargetToString_returnsCorrectString() {
        assertThat(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE"
            )
        assertThat(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior.PolarTarget.POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE"
            )
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
    fun binaryOpToString_returnsCorrectString() {
        assertThat(BrushBehavior.BinaryOp.PRODUCT.toString())
            .isEqualTo("BrushBehavior.BinaryOp.PRODUCT")
        assertThat(BrushBehavior.BinaryOp.SUM.toString()).isEqualTo("BrushBehavior.BinaryOp.SUM")
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
    fun noiseNodeConstructor_throwsForNonFiniteBasePeriod() {
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.NoiseNode(
                12345,
                BrushBehavior.DampingSource.TIME_IN_SECONDS,
                Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, Float.NaN)
        }
    }

    @Test
    fun noiseNodeConstructor_throwsForNegativeBasePeriod() {
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, -1f)
        }
    }

    @Test
    fun noiseNodeToString() {
        val node = BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f)
        assertThat(node.toString()).isEqualTo("NoiseNode(12345, TIME_IN_SECONDS, 1.0)")
    }

    @Test
    fun noiseNodeEquals_checksEqualityOfValues() {
        val node = BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f)
        assertThat(node)
            .isEqualTo(
                BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f)
            )
        assertThat(node)
            .isNotEqualTo(
                BrushBehavior.NoiseNode(12346, BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f)
            )
        assertThat(node)
            .isNotEqualTo(
                BrushBehavior.NoiseNode(
                    12345,
                    BrushBehavior.DampingSource.DISTANCE_IN_CENTIMETERS,
                    1f,
                )
            )
        assertThat(node)
            .isNotEqualTo(
                BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, 2f)
            )
    }

    @Test
    fun noiseNodeHashCode_withIdenticalValues_match() {
        val node1 = BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f)
        val node2 = BrushBehavior.NoiseNode(12345, BrushBehavior.DampingSource.TIME_IN_SECONDS, 1f)
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
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
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun toolTypeFilterNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.ToolTypeFilterNode(
                setOf(InputToolType.STYLUS),
                BrushBehavior.ConstantNode(2f),
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
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun responseNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(1f),
            )
        val node2 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(1f),
            )
        val node3 =
            BrushBehavior.ResponseNode(
                EasingFunction.Predefined.EASE,
                BrushBehavior.ConstantNode(2f),
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
    fun polarTargetNodeConstructor_throwsForNonFiniteAngleRange() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                Float.NaN,
                1f,
                input,
                0f,
                1f,
                input,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                Float.POSITIVE_INFINITY,
                input,
                0f,
                1f,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeConstructor_throwsForNonFiniteMagnitudeRange() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                input,
                Float.NaN,
                1f,
                input,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                input,
                0f,
                Float.POSITIVE_INFINITY,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeConstructor_throwsForEmptyAngleRange() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0.5f,
                0.5f,
                input,
                0f,
                1f,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeConstructor_throwsForEmptyMagnitudeRange() {
        val input = BrushBehavior.ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                input,
                0.5f,
                0.5f,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeInputs_containsInputs() {
        val angleInput = BrushBehavior.ConstantNode(0f)
        val magnitudeInput = BrushBehavior.ConstantNode(1f)
        val node =
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                angleInput,
                0f,
                1f,
                magnitudeInput,
            )
        assertThat(node.inputs).containsExactly(angleInput, magnitudeInput).inOrder()
    }

    @Test
    fun polarTargetNodeToString() {
        val angleInput = BrushBehavior.ConstantNode(2f)
        val magnitudeInput = BrushBehavior.ConstantNode(5f)
        val node =
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                angleInput,
                3f,
                4f,
                magnitudeInput,
            )
        assertThat(node.toString())
            .isEqualTo(
                "PolarTargetNode(POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE, 0.0, 1.0, ConstantNode(2.0), 3.0, 4.0, ConstantNode(5.0))"
            )
    }

    @Test
    fun polarTargetNodeEquals_checksEqualityOfValues() {
        val node1 =
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                BrushBehavior.ConstantNode(2f),
                3f,
                4f,
                BrushBehavior.ConstantNode(5f),
            )
        val node2 =
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                BrushBehavior.ConstantNode(2f),
                3f,
                4f,
                BrushBehavior.ConstantNode(5f),
            )
        val node3 =
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                BrushBehavior.ConstantNode(2f),
                3f,
                4f,
                BrushBehavior.ConstantNode(67f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun polarTargetNodeHashCode_withIdenticalValues_match() {
        val node1 =
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                BrushBehavior.ConstantNode(2f),
                3f,
                4f,
                BrushBehavior.ConstantNode(5f),
            )
        val node2 =
            BrushBehavior.PolarTargetNode(
                BrushBehavior.PolarTarget
                    .POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                BrushBehavior.ConstantNode(2f),
                3f,
                4f,
                BrushBehavior.ConstantNode(5f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
    }

    @Test
    fun brushBehaviorConstructor_withInvalidArguments_throws() {
        // sourceValueRangeStart not finite
        val sourceValueRangeStartError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = Float.NaN, // Not finite.
                    sourceValueRangeEnd = 1.0f,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(sourceValueRangeStartError.message).contains("source")
        assertThat(sourceValueRangeStartError.message).contains("finite")

        // sourceValueRangeEnd not finite
        val sourceValueRangeEndError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = 1.0f,
                    sourceValueRangeEnd = Float.NaN, // Not finite.
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(sourceValueRangeEndError.message).contains("source")
        assertThat(sourceValueRangeEndError.message).contains("finite")

        // sourceValueRangeEnd == sourceValueRangeEnd
        val sourceValueRangeError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = 0.5f, // same as upper bound.
                    sourceValueRangeEnd = 0.5f, // same as lower bound.
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(sourceValueRangeError.message).contains("source")
        assertThat(sourceValueRangeError.message).contains("distinct")

        // targetModifierRangeStart not finite
        val targetModifierRangeStartError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = 0.2f,
                    sourceValueRangeEnd = .8f,
                    targetModifierRangeStart = Float.NaN, // Not finite.
                    targetModifierRangeEnd = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(targetModifierRangeStartError.message).contains("target")
        assertThat(targetModifierRangeStartError.message).contains("finite")

        // targetModifierRangeEnd not finite
        val targetModifierRangeEndError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = 0.2f,
                    sourceValueRangeEnd = .8f,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = Float.NaN, // Not finite.
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(targetModifierRangeEndError.message).contains("target")
        assertThat(targetModifierRangeEndError.message).contains("finite")

        // responseTimeMillis less than 0L
        val responseTimeMillisError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = 0.2f,
                    sourceValueRangeEnd = .8f,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = -1L, // Less than 0.
                    enabledToolTypes = setOf(InputToolType.STYLUS),
                )
            }
        assertThat(responseTimeMillisError.message).contains("damping_gap")
        assertThat(responseTimeMillisError.message).contains("non-negative")

        // enabledToolType contains empty set.
        val enabledToolTypeError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = 0.2f,
                    sourceValueRangeEnd = .8f,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
                    sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                    responseTimeMillis = 1L,
                    enabledToolTypes = setOf(),
                )
            }
        assertThat(enabledToolTypeError.message).contains("enabled_tool_types")
        assertThat(enabledToolTypeError.message).contains("must enable at least one")

        // source and outOfRangeBehavior combination is invalid (TIME_SINCE_INPUT must use CLAMP)
        val sourceOutOfRangeBehaviorError =
            assertFailsWith<IllegalArgumentException> {
                BrushBehavior(
                    source = BrushBehavior.Source.TIME_SINCE_INPUT_IN_SECONDS,
                    target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                    sourceValueRangeStart = 0.2f,
                    sourceValueRangeEnd = .8f,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
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
                                targetModifierRangeStart = 1.0f,
                                targetModifierRangeEnd = 1.75f,
                                input =
                                    BrushBehavior.SourceNode(
                                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                                        sourceValueRangeStart = 0.0f,
                                        sourceValueRangeEnd = 1.0f,
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
                sourceValueRangeStart = 0.0f,
                sourceValueRangeEnd = 1.0f,
                targetModifierRangeStart = 1.0f,
                targetModifierRangeEnd = 1.75f,
                sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                responseTimeMillis = 1L,
                enabledToolTypes = setOf(InputToolType.STYLUS),
            )

        val exact =
            BrushBehavior(
                source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                target = BrushBehavior.Target.WIDTH_MULTIPLIER,
                sourceValueRangeStart = 0.0f,
                sourceValueRangeEnd = 1.0f,
                targetModifierRangeStart = 1.0f,
                targetModifierRangeEnd = 1.75f,
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
                sourceValueRangeStart = 0.0f,
                sourceValueRangeEnd = 1.0f,
                targetModifierRangeStart = 1.0f,
                targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.3f, // different
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 0.8f, // different
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.56f, // different
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.99f, // different
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
                        sourceValueRangeStart = 0.0f,
                        sourceValueRangeEnd = 1.0f,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
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
    @UsedByNative
    private external fun matchesNativeStepBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ PredefinedFunction BrushBehavior and returns true if every property
     * of the Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    @UsedByNative
    private external fun matchesNativePredefinedBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ CubicBezier BrushBehavior and returns true if every property of the
     * Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    @UsedByNative
    private external fun matchesNativeCubicBezierBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ Linear BrushBehavior and returns true if every property of the Kotlin
     * BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++ BrushBehavior.
     */
    @UsedByNative
    private external fun matchesNativeLinearBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean
}
