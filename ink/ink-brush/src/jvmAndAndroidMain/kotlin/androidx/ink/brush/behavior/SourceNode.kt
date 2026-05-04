/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.collection.MutableIntObjectMap
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/** A [ValueNode] that gets data from the stroke input batch. */
public class SourceNode private constructor(nativePointer: Long) :
    ValueNode(nativePointer, emptyList()) {

    /**
     * Creates a [SourceNode] that gets data from the stroke inputs.
     *
     * @param source the property of the data to get values from
     * @param sourceValueRangeStart the start of the range of values that the source can produce
     * @param sourceValueRangeEnd the end of the range of values that the source can produce
     * @param sourceOutOfRangeBehavior the behavior to use if the source produces a value outside
     *   the specified range
     */
    @JvmOverloads
    public constructor(
        source: Source,
        sourceValueRangeStart: Float,
        sourceValueRangeEnd: Float,
        sourceOutOfRangeBehavior: OutOfRange = OutOfRange.CLAMP,
    ) : this(
        SourceNodeNative.createSource(
            source.value,
            sourceValueRangeStart,
            sourceValueRangeEnd,
            sourceOutOfRangeBehavior.value,
        )
    )

    internal companion object {
        internal fun wrapNative(unownedNativePointer: Long): SourceNode =
            SourceNode(unownedNativePointer)
    }

    /** The property of the data to get values from. */
    public val source: Source = SourceNodeNative.getSource(nativePointer)

    /** The start of the range of values that the source can produce. */
    public val sourceValueRangeStart: Float
        get() = SourceNodeNative.getSourceValueRangeStart(nativePointer)

    /** The end of the range of values that the source can produce. */
    public val sourceValueRangeEnd: Float
        get() = SourceNodeNative.getSourceValueRangeEnd(nativePointer)

    /** The behavior to use if the source produces a value outside the specified range. */
    public val sourceOutOfRangeBehavior: OutOfRange =
        SourceNodeNative.getSourceOutOfRangeBehavior(nativePointer)

    override fun toString(): String =
        "SourceNode(${source.toSimpleString()}, $sourceValueRangeStart, $sourceValueRangeEnd, ${sourceOutOfRangeBehavior.toSimpleString()})"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SourceNode) return false
        return source == other.source &&
            sourceValueRangeStart == other.sourceValueRangeStart &&
            sourceValueRangeEnd == other.sourceValueRangeEnd &&
            sourceOutOfRangeBehavior == other.sourceOutOfRangeBehavior
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + sourceValueRangeStart.hashCode()
        result = 31 * result + sourceValueRangeEnd.hashCode()
        result = 31 * result + sourceOutOfRangeBehavior.hashCode()
        return result
    }

    /**
     * List of input properties along with their units that can act as sources for a
     * [androidx.ink.brush.BrushBehavior].
     *
     * Behaviors that consider properties of the stroke input do not consider alterations to the
     * visible position of that point in the stroke by brush behaviors that modify that position
     * (e.g. [TargetNode.Target.POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE]). That is, the
     * position, velocity, and acceleration of the stroke input may not match the visible position,
     * velocity, and acceleration of that point in the drawn stroke. The stroke inputs considered by
     * these behaviors are specifically the "modeled" inputs used to construct the stroke geometry,
     * which may be upsampled, denoised, or otherwise transformed from the raw stroke input.
     */
    public class Source
    internal constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate Source value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "Source.$name"

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<Source>()

            internal fun fromInt(value: Int): Source =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid Source value: $value" }

            /** Stylus or touch pressure with values reported in the range [0, 1]. */
            @JvmField public val NORMALIZED_PRESSURE: Source = Source(0, "NORMALIZED_PRESSURE")
            /** Stylus tilt with values reported in the range [0, π/2] radians. */
            @JvmField public val TILT_IN_RADIANS: Source = Source(1, "TILT_IN_RADIANS")
            /**
             * Stylus tilt along the x axis in the range [-π/2, π/2], with a positive value
             * corresponding to tilt toward the respective positive axis. In order for those values
             * to be reported, both tilt and orientation have to be populated on the StrokeInput.
             */
            @JvmField public val TILT_X_IN_RADIANS: Source = Source(2, "TILT_X_IN_RADIANS")
            /**
             * Stylus tilt along the y axis in the range [-π/2, π/2], with a positive value
             * corresponding to tilt toward the respective positive axis. In order for those values
             * to be reported, both tilt and orientation have to be populated on the StrokeInput.
             */
            @JvmField public val TILT_Y_IN_RADIANS: Source = Source(3, "TILT_Y_IN_RADIANS")
            /** Stylus orientation with values reported in the range [0, 2π). */
            @JvmField
            public val ORIENTATION_IN_RADIANS: Source = Source(4, "ORIENTATION_IN_RADIANS")
            /** Stylus orientation with values reported in the range (-π, π]. */
            @JvmField
            public val ORIENTATION_ABOUT_ZERO_IN_RADIANS: Source =
                Source(5, "ORIENTATION_ABOUT_ZERO_IN_RADIANS")
            /**
             * Absolute speed of the modeled stroke input in multiples of the brush size per second.
             * Note that this value doesn't take into account brush behaviors that offset the
             * position of the visual tip of the stroke.
             */
            @JvmField
            public val SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND: Source =
                Source(6, "SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
            /**
             * Signed x component of the velocity of the modeled stroke input in multiples of the
             * brush size per second. Note that this value doesn't take into account brush behaviors
             * that offset the visible position of that point in the stroke.
             */
            @JvmField
            public val VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND: Source =
                Source(7, "VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
            /**
             * Signed y component of the velocity of the modeled stroke input in multiples of the
             * brush size per second. Note that this value doesn't take into account brush behaviors
             * that offset the visible position of that point in the stroke.
             */
            @JvmField
            public val VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND: Source =
                Source(8, "VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND")
            /**
             * Angle of the modeled stroke input's current direction of travel in stroke coordinate
             * space, normalized to the range [0, 2π). A value of 0 indicates the direction of the
             * positive x-axis; a value of π/2 indicates the direction of the positive y-axis.
             */
            @JvmField public val DIRECTION_IN_RADIANS: Source = Source(9, "DIRECTION_IN_RADIANS")
            /**
             * Angle of the modeled stroke input's current direction of travel in stroke coordinate
             * space, normalized to the range (-π, π]. A value of 0 indicates the direction of the
             * positive x-axis; a value of π/2 indicates the direction of the positive y-axis.
             */
            @JvmField
            public val DIRECTION_ABOUT_ZERO_IN_RADIANS: Source =
                Source(10, "DIRECTION_ABOUT_ZERO_IN_RADIANS")
            /**
             * Signed x component of the modeled stroke input's current direction of travel in
             * stroke coordinate space, normalized to the range [-1, 1].
             */
            @JvmField
            public val NORMALIZED_DIRECTION_X: Source = Source(11, "NORMALIZED_DIRECTION_X")
            /**
             * Signed y component of the modeled stroke input's current direction of travel in
             * stroke coordinate space, normalized to the range [-1, 1].
             */
            @JvmField
            public val NORMALIZED_DIRECTION_Y: Source = Source(12, "NORMALIZED_DIRECTION_Y")
            /**
             * Distance traveled by the inputs of the current stroke, starting at 0 at the first
             * input, where one distance unit is equal to the brush size.
             */
            @JvmField
            public val DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE: Source =
                Source(13, "DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE")
            /**
             * Time elapsed in seconds from between the start of the stroke and the current modeled
             * stroke input. The value remains fixed for any given part of the stroke once drawn.
             */
            @JvmField
            public val TIME_OF_INPUT_IN_SECONDS: Source = Source(14, "TIME_OF_INPUT_IN_SECONDS")
            /**
             * Time elapsed from the current modeled stroke input until the last input in the
             * stroke.
             */
            @JvmField
            public val TIME_FROM_INPUT_TO_STROKE_END_IN_SECONDS: Source =
                Source(15, "TIME_FROM_INPUT_TO_STROKE_END_IN_SECONDS")
            /**
             * Distance traveled by the inputs of the current prediction, starting at 0 at the last
             * non-predicted input, in multiples of the brush size. Zero for inputs before the
             * predicted portion of the stroke.
             */
            @JvmField
            public val PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE: Source =
                Source(16, "PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE")
            /**
             * Elapsed time of the prediction in seconds, starting at 0 at the last non-predicted
             * input. Zero for inputs before the predicted portion of the stroke.
             */
            @JvmField
            public val PREDICTED_TIME_ELAPSED_IN_SECONDS: Source =
                Source(17, "PREDICTED_TIME_ELAPSED_IN_SECONDS")
            /**
             * The distance left to be traveled from a given modeled input to the current last
             * modeled input of the stroke in multiples of the brush size. This value changes for
             * each input as the stroke is drawn.
             */
            @JvmField
            public val DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE: Source =
                Source(18, "DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE")
            /**
             * Time elapsed in seconds since the modeled stroke input. This continues to increase
             * even after all stroke inputs have completed, and can be used to drive stroke
             * animations. This enumerators are only compatible with a
             * [SourceNode.sourceOutOfRangeBehavior] of [OutOfRange.CLAMP], to ensure that the
             * animation will eventually end.
             */
            @JvmField
            public val TIME_SINCE_INPUT_IN_SECONDS: Source =
                Source(19, "TIME_SINCE_INPUT_IN_SECONDS")
            /**
             * Time elapsed since the final input of the stroke, or zero if the final input hasn't
             * arrived yet. This can be used to drive wet-layer stroke animations that should occur
             * after the final input. This source is only compatible with a
             * [SourceNode.sourceOutOfRangeBehavior] of [OutOfRange.CLAMP], to ensure that the
             * animation will eventually end.
             */
            @JvmField
            public val TIME_SINCE_STROKE_END_IN_SECONDS: Source =
                Source(20, "TIME_SINCE_STROKE_END_IN_SECONDS")
            /**
             * Absolute acceleration of the modeled stroke input in multiples of the brush size per
             * second squared. Note that this value doesn't take into account brush behaviors that
             * offset the position of that visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(21, "ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed x component of the acceleration of the modeled stroke input in multiples of
             * the brush size per second squared. Note that this value doesn't take into account
             * brush behaviors that offset the position of that visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(22, "ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed y component of the acceleration of the modeled stroke input in multiples of
             * the brush size per second squared. Note that this value doesn't take into account
             * brush behaviors that offset the position of that visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(23, "ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed component of acceleration of the modeled stroke input in the direction of its
             * velocity in multiples of the brush size per second squared. Note that this value
             * doesn't take into account brush behaviors that offset the position of that visible
             * point in the stroke.
             */
            @JvmField
            public val ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(24, "ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed component of acceleration of the modeled stroke input perpendicular to its
             * velocity, rotated 90 degrees in the direction from the positive x-axis towards the
             * positive y-axis, in multiples of the brush size per second squared. Note that this
             * value doesn't take into account brush behaviors that offset the position of that
             * visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(25, "ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /** Absolute speed of the modeled stroke input pointer in centimeters per second. */
            @JvmField
            public val SPEED_IN_CENTIMETERS_PER_SECOND: Source =
                Source(26, "SPEED_IN_CENTIMETERS_PER_SECOND")
            /**
             * Signed x component of the modeled stroke input pointer's velocity in centimeters per
             * second.
             */
            @JvmField
            public val VELOCITY_X_IN_CENTIMETERS_PER_SECOND: Source =
                Source(27, "VELOCITY_X_IN_CENTIMETERS_PER_SECOND")
            /**
             * Signed y component of the modeled stroke input pointer's velocity in centimeters per
             * second.
             */
            @JvmField
            public val VELOCITY_Y_IN_CENTIMETERS_PER_SECOND: Source =
                Source(28, "VELOCITY_Y_IN_CENTIMETERS_PER_SECOND")
            /**
             * Distance in centimeters traveled by the modeled stroke input pointer along the input
             * path from the start of the stroke.
             */
            @JvmField
            public val DISTANCE_TRAVELED_IN_CENTIMETERS: Source =
                Source(29, "DISTANCE_TRAVELED_IN_CENTIMETERS")
            /**
             * Distance in centimeters alonge the input path from the real portion of the modeled
             * stroke to this input. Zero for inputs before the predicted portion of the stroke.
             */
            @JvmField
            public val PREDICTED_DISTANCE_TRAVELED_IN_CENTIMETERS: Source =
                Source(30, "PREDICTED_DISTANCE_TRAVELED_IN_CENTIMETERS")
            /**
             * Absolute acceleration of the modeled stroke input pointer in centimeters per second
             * squared.
             */
            @JvmField
            public val ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(31, "ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed x component of the acceleration of the modeled stroke input pointer in
             * centimeters per second squared.
             */
            @JvmField
            public val ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(32, "ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed y component of the acceleration of the modeled stroke input pointer in
             * centimeters per second squared.
             */
            @JvmField
            public val ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(33, "ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed component acceleration of the modeled stroke input pointer in the direction of
             * its velocity in centimeters per second squared.
             */
            @JvmField
            public val ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(34, "ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed component of acceleration of the modeled stroke input pointer perpendicular to
             * its velocity, rotated 90 degrees in the direction from the positive x-axis towards
             * the positive y-axis, in centimeters per second squared.
             */
            @JvmField
            public val ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(35, "ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Distance from the current modeled input to the end of the stroke along the input
             * path, as a fraction of the current total length of the stroke. This value changes for
             * each input as inputs are added.
             */
            @JvmField
            public val DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH: Source =
                Source(36, "DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH")
        }
    }
}

/**
 * Singleton wrapper for `BrushBehavior::SourceNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
@UsedByNative
private object SourceNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun createSource(
        source: Int,
        sourceValueRangeStart: Float,
        sourceValueRangeEnd: Float,
        sourceOutOfRangeBehavior: Int,
    ): Long

    fun getSource(nativePointer: Long): SourceNode.Source =
        SourceNode.Source.fromInt(getSourceInt(nativePointer))

    @UsedByNative private external fun getSourceInt(nativePointer: Long): Int

    @UsedByNative external fun getSourceValueRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getSourceValueRangeEnd(nativePointer: Long): Float

    fun getSourceOutOfRangeBehavior(nativePointer: Long): OutOfRange =
        OutOfRange.fromInt(getSourceOutOfRangeBehaviorInt(nativePointer))

    @UsedByNative private external fun getSourceOutOfRangeBehaviorInt(nativePointer: Long): Int
}
