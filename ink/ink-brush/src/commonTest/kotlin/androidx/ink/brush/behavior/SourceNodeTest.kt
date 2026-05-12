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

package androidx.ink.brush.behavior

import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.nativeloader.testing.awaitNativePointerCleanupAfter
import androidx.kruth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SourceNodeTest {

    @Test
    fun sourceNodeNativePointers_cleanedUpWhenOutOfScope() {
        awaitNativePointerCleanupAfter {
            val unused = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f)
        }
    }

    @Test
    fun sourceToString_returnsCorrectString() {
        assertThat(Source.NORMALIZED_PRESSURE.toString()).isEqualTo("Source.NORMALIZED_PRESSURE")
        assertThat(Source.TILT_IN_RADIANS.toString()).isEqualTo("Source.TILT_IN_RADIANS")
        assertThat(Source.TILT_X_IN_RADIANS.toString()).isEqualTo("Source.TILT_X_IN_RADIANS")
        assertThat(Source.TILT_Y_IN_RADIANS.toString()).isEqualTo("Source.TILT_Y_IN_RADIANS")
        assertThat(Source.ORIENTATION_IN_RADIANS.toString())
            .isEqualTo("Source.ORIENTATION_IN_RADIANS")
        assertThat(Source.ORIENTATION_ABOUT_ZERO_IN_RADIANS.toString())
            .isEqualTo("Source.ORIENTATION_ABOUT_ZERO_IN_RADIANS")
        assertThat(Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND.toString())
            .isEqualTo("Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
        assertThat(Source.VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND.toString())
            .isEqualTo("Source.VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
        assertThat(Source.VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND.toString())
            .isEqualTo("Source.VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
        assertThat(Source.DIRECTION_IN_RADIANS.toString()).isEqualTo("Source.DIRECTION_IN_RADIANS")
        assertThat(Source.DIRECTION_ABOUT_ZERO_IN_RADIANS.toString())
            .isEqualTo("Source.DIRECTION_ABOUT_ZERO_IN_RADIANS")
        assertThat(Source.NORMALIZED_DIRECTION_X.toString())
            .isEqualTo("Source.NORMALIZED_DIRECTION_X")
        assertThat(Source.NORMALIZED_DIRECTION_Y.toString())
            .isEqualTo("Source.NORMALIZED_DIRECTION_Y")
        assertThat(Source.DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("Source.DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(Source.TIME_OF_INPUT_IN_SECONDS.toString())
            .isEqualTo("Source.TIME_OF_INPUT_IN_SECONDS")
        assertThat(Source.TIME_FROM_INPUT_TO_STROKE_END_IN_SECONDS.toString())
            .isEqualTo("Source.TIME_FROM_INPUT_TO_STROKE_END_IN_SECONDS")
        assertThat(Source.PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("Source.PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(Source.PREDICTED_TIME_ELAPSED_IN_SECONDS.toString())
            .isEqualTo("Source.PREDICTED_TIME_ELAPSED_IN_SECONDS")
        assertThat(Source.DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("Source.DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(Source.TIME_SINCE_INPUT_IN_SECONDS.toString())
            .isEqualTo("Source.TIME_SINCE_INPUT_IN_SECONDS")
        assertThat(Source.TIME_SINCE_STROKE_END_IN_SECONDS.toString())
            .isEqualTo("Source.TIME_SINCE_STROKE_END_IN_SECONDS")
        assertThat(Source.ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
        assertThat(Source.ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
        assertThat(Source.ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
        assertThat(
                Source.ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED.toString()
            )
            .isEqualTo("Source.ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
        assertThat(
                Source.ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED.toString()
            )
            .isEqualTo("Source.ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
        assertThat(Source.SPEED_IN_CENTIMETERS_PER_SECOND.toString())
            .isEqualTo("Source.SPEED_IN_CENTIMETERS_PER_SECOND")
        assertThat(Source.VELOCITY_X_IN_CENTIMETERS_PER_SECOND.toString())
            .isEqualTo("Source.VELOCITY_X_IN_CENTIMETERS_PER_SECOND")
        assertThat(Source.VELOCITY_Y_IN_CENTIMETERS_PER_SECOND.toString())
            .isEqualTo("Source.VELOCITY_Y_IN_CENTIMETERS_PER_SECOND")
        assertThat(Source.DISTANCE_TRAVELED_IN_CENTIMETERS.toString())
            .isEqualTo("Source.DISTANCE_TRAVELED_IN_CENTIMETERS")
        assertThat(Source.PREDICTED_DISTANCE_TRAVELED_IN_CENTIMETERS.toString())
            .isEqualTo("Source.PREDICTED_DISTANCE_TRAVELED_IN_CENTIMETERS")
        assertThat(Source.ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED")
        assertThat(Source.ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED")
        assertThat(Source.ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED")
        assertThat(Source.ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED")
        assertThat(Source.ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED.toString())
            .isEqualTo("Source.ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED")
        assertThat(Source.DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH.toString())
            .isEqualTo("Source.DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH")
    }

    @Test
    fun sourceNodeConstructor_throwsForNonFiniteSourceValueRange() {
        assertFailsWith<IllegalArgumentException> {
            SourceNode(Source.NORMALIZED_PRESSURE, Float.NaN, 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            SourceNode(Source.NORMALIZED_PRESSURE, 0f, Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun sourceNodeConstructor_throwsForEmptySourceValueRange() {
        assertFailsWith<IllegalArgumentException> {
            SourceNode(Source.NORMALIZED_PRESSURE, 0.5f, 0.5f)
        }
    }

    @Test
    fun sourceNodeInputs_isEmpty() {
        val node = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f)
        assertThat(node.inputs).isEmpty()
    }

    @Test
    fun sourceNodeToString() {
        val node = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f)
        assertThat(node.toString()).isEqualTo("SourceNode(NORMALIZED_PRESSURE, 0.0, 1.0, CLAMP)")
    }

    @Test
    fun sourceNodeEquals_checksEqualityOfValues() {
        val node1 = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node2 = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node3 = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 2f)
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun sourceNodeHashCode_withIdenticalValues_match() {
        val node1 = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node2 = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f)
        val node3 = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 2f)
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun sourceNodeConstructor_withInvalidArguments_throws() {
        // sourceValueRangeStart not finite
        val sourceValueRangeStartError =
            assertFailsWith<IllegalArgumentException> {
                SourceNode(
                    source = Source.NORMALIZED_PRESSURE,
                    sourceValueRangeStart = Float.NaN, // Not finite.
                    sourceValueRangeEnd = 1.0f,
                )
            }
        assertThat(sourceValueRangeStartError.message).contains("source")
        assertThat(sourceValueRangeStartError.message).contains("finite")

        // sourceValueRangeEnd not finite
        val sourceValueRangeEndError =
            assertFailsWith<IllegalArgumentException> {
                SourceNode(
                    source = Source.NORMALIZED_PRESSURE,
                    sourceValueRangeStart = 1.0f,
                    sourceValueRangeEnd = Float.NaN, // Not finite.
                )
            }
        assertThat(sourceValueRangeStartError.message).contains("source")
        assertThat(sourceValueRangeStartError.message).contains("finite")

        // sourceValueRangeEnd == sourceValueRangeEnd
        val sourceValueRangeError =
            assertFailsWith<IllegalArgumentException> {
                SourceNode(
                    source = Source.NORMALIZED_PRESSURE,
                    sourceValueRangeStart = 0.5f, // same as upper bound.
                    sourceValueRangeEnd = 0.5f, // same as lower bound.
                )
            }
        assertThat(sourceValueRangeError.message).contains("source")
        assertThat(sourceValueRangeError.message).contains("distinct")

        // source and outOfRangeBehavior combination is invalid (TIME_SINCE_INPUT must use CLAMP)
        val sourceOutOfRangeBehaviorError =
            assertFailsWith<IllegalArgumentException> {
                SourceNode(
                    source = Source.TIME_SINCE_INPUT_IN_SECONDS,
                    sourceValueRangeStart = 0.2f,
                    sourceValueRangeEnd = .8f,
                    sourceOutOfRangeBehavior = OutOfRange.REPEAT,
                )
            }
        assertThat(sourceOutOfRangeBehaviorError.message).contains("TimeSince")
        assertThat(sourceOutOfRangeBehaviorError.message).contains("kClamp")
    }
}
