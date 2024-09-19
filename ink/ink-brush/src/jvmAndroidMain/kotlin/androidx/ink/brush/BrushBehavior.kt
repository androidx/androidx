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

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativeLoader
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableSet
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A behavior describing how stroke input properties should affect the shape and color of the brush
 * tip.
 *
 * The behavior is conceptually a graph made from the various node types defined below. Each edge of
 * the graph represents passing a nullable floating point value between nodes, and each node in the
 * graph fits into one of the following categories:
 * 1. Leaf nodes generate an output value without graph inputs. For example, they can create a value
 *    from properties of stroke input.
 * 2. Filter nodes can conditionally toggle branches of the graph "on" by outputting their input
 *    value, or "off" by outputting a null value.
 * 3. Operator nodes take in one or more input values and generate an output. For example, by
 *    mapping input to output with an easing function.
 * 4. Target nodes apply an input value to chosen properties of the brush tip.
 *
 * For each input in a stroke, [BrushTip.behaviors] are applied as follows:
 * 1. The actual target modifier (as calculated above) for each tip property is accumulated from
 *    every [BrushBehavior] present on the current [BrushTip]. Multiple behaviors can affect the
 *    same [Target]. Depending on the [Target], modifiers from multiple behaviors will stack either
 *    additively or multiplicatively, according to the documentation for that [Target]. Regardless,
 *    the order of specified behaviors does not affect the result.
 * 2. The modifiers are applied to the shape and color shift values of the tip's state according to
 *    the documentation for each [Target]. The resulting tip property values are then clamped or
 *    normalized to within their valid range of values. E.g. the final value of
 *    [BrushTip.cornerRounding] will be clamped within [0, 1]. Generally: The affected shape values
 *    are those found in [BrushTip] members. The color shift values remain in the range -100% to
 *    +100%. Note that when stored on a vertex, the color shift is encoded such that each channel is
 *    in the range [0, 1], where 0.5 represents a 0% shift.
 *
 * Note that the accumulated tip shape property modifiers may be adjusted by the implementation
 * before being applied: The rates of change of shape properties may be constrained to keep them
 * from changing too rapidly with respect to distance traveled from one input to the next.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@ExperimentalInkCustomBrushApi
// NotCloseable: Finalize is only used to free the native peer.
@Suppress("NotCloseable")
public class BrushBehavior(
    // The [targetNodes] val below is a defensive copy of this parameter.
    targetNodes: List<TargetNode>
) {
    public val targetNodes: List<TargetNode> = unmodifiableList(targetNodes.toList())

    /** A handle to the underlying native [BrushBehavior] object. */
    internal val nativePointer: Long = createNativeBrushBehavior(targetNodes)

    /**
     * Constructs a simple [BrushBehavior] using whatever [Node]s are necessary for the specified
     * fields.
     */
    public constructor(
        source: Source,
        target: Target,
        sourceValueRangeLowerBound: Float,
        sourceValueRangeUpperBound: Float,
        targetModifierRangeLowerBound: Float,
        targetModifierRangeUpperBound: Float,
        sourceOutOfRangeBehavior: OutOfRange = OutOfRange.CLAMP,
        responseCurve: EasingFunction = EasingFunction.Predefined.LINEAR,
        responseTimeMillis: Long = 0L,
        enabledToolTypes: Set<InputToolType> = ALL_TOOL_TYPES,
        isFallbackFor: OptionalInputProperty? = null,
    ) : this(
        run<List<TargetNode>> {
            var node: ValueNode =
                SourceNode(
                    source,
                    sourceValueRangeLowerBound,
                    sourceValueRangeUpperBound,
                    sourceOutOfRangeBehavior,
                )
            if (enabledToolTypes != ALL_TOOL_TYPES) {
                node = ToolTypeFilterNode(enabledToolTypes, node)
            }
            if (isFallbackFor != null) {
                node = FallbackFilterNode(isFallbackFor, node)
            }
            // [EasingFunction.Predefined.LINEAR] is the identity function, so no need to add a
            // [ResponseNode] with that function.
            if (responseCurve != EasingFunction.Predefined.LINEAR) {
                node = ResponseNode(responseCurve, node)
            }
            if (responseTimeMillis != 0L) {
                node =
                    DampingNode(
                        DampingSource.TIME_IN_SECONDS,
                        responseTimeMillis.toFloat() / 1000.0f,
                        node
                    )
            }
            listOf(
                TargetNode(
                    target,
                    targetModifierRangeLowerBound,
                    targetModifierRangeUpperBound,
                    node
                )
            )
        }
    )

    /**
     * Builder for [BrushBehavior].
     *
     * For Java developers, use BrushBehavior.Builder to construct a [BrushBehavior] with default
     * values, overriding only as needed. For example:
     * ```
     * BrushBehavior behavior = new BrushBehavior.Builder()
     *   .setSource(...)
     *   .setTarget(...)
     *   .setSourceOutOfRangeBehavior(...)
     *   .setSourceValueRangeLowerBound(...)
     *   .build();
     * ```
     */
    @Suppress("ScopeReceiverThis")
    public class Builder {
        private var source: Source = Source.NORMALIZED_PRESSURE
        private var target: Target = Target.SIZE_MULTIPLIER
        private var sourceOutOfRangeBehavior: OutOfRange = OutOfRange.CLAMP
        private var sourceValueRangeLowerBound: Float = 0f
        private var sourceValueRangeUpperBound: Float = 1f
        private var targetModifierRangeLowerBound: Float = 0f
        private var targetModifierRangeUpperBound: Float = 1f
        private var responseCurve: EasingFunction = EasingFunction.Predefined.LINEAR
        private var responseTimeMillis: Long = 0L
        private var enabledToolTypes: Set<InputToolType> = ALL_TOOL_TYPES
        private var isFallbackFor: OptionalInputProperty? = null

        public fun setSource(source: Source): Builder = apply { this.source = source }

        public fun setTarget(target: Target): Builder = apply { this.target = target }

        public fun setSourceOutOfRangeBehavior(sourceOutOfRangeBehavior: OutOfRange): Builder =
            apply {
                this.sourceOutOfRangeBehavior = sourceOutOfRangeBehavior
            }

        public fun setSourceValueRangeLowerBound(sourceValueRangeLowerBound: Float): Builder =
            apply {
                this.sourceValueRangeLowerBound = sourceValueRangeLowerBound
            }

        public fun setSourceValueRangeUpperBound(sourceValueRangeUpperBound: Float): Builder =
            apply {
                this.sourceValueRangeUpperBound = sourceValueRangeUpperBound
            }

        public fun setTargetModifierRangeLowerBound(targetModifierRangeLowerBound: Float): Builder =
            apply {
                this.targetModifierRangeLowerBound = targetModifierRangeLowerBound
            }

        public fun setTargetModifierRangeUpperBound(targetModifierRangeUpperBound: Float): Builder =
            apply {
                this.targetModifierRangeUpperBound = targetModifierRangeUpperBound
            }

        public fun setResponseCurve(responseCurve: EasingFunction): Builder = apply {
            this.responseCurve = responseCurve
        }

        public fun setResponseTimeMillis(responseTimeMillis: Long): Builder = apply {
            this.responseTimeMillis = responseTimeMillis
        }

        public fun setEnabledToolTypes(enabledToolTypes: Set<InputToolType>): Builder = apply {
            this.enabledToolTypes = enabledToolTypes.toSet()
        }

        public fun setIsFallbackFor(isFallbackFor: OptionalInputProperty?): Builder = apply {
            this.isFallbackFor = isFallbackFor
        }

        public fun build(): BrushBehavior =
            BrushBehavior(
                source,
                target,
                sourceValueRangeLowerBound,
                sourceValueRangeUpperBound,
                targetModifierRangeLowerBound,
                targetModifierRangeUpperBound,
                sourceOutOfRangeBehavior,
                responseCurve,
                responseTimeMillis,
                enabledToolTypes,
                isFallbackFor,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushBehavior) return false
        if (other === this) return true
        return targetNodes == other.targetNodes
    }

    override fun hashCode(): Int {
        return targetNodes.hashCode()
    }

    override fun toString(): String = "BrushBehavior($targetNodes)"

    /** Delete native BrushBehavior memory. */
    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        nativeFreeBrushBehavior(nativePointer)
    }

    private fun createNativeBrushBehavior(targetNodes: List<TargetNode>): Long {
        val orderedNodes = ArrayDeque<Node>()
        val stack = ArrayDeque<Node>(targetNodes)
        while (!stack.isEmpty()) {
            stack.removeLast().let { node ->
                orderedNodes.addFirst(node)
                stack.addAll(node.inputs)
            }
        }

        val nativePointer = nativeCreateEmptyBrushBehavior()
        for (node in orderedNodes) {
            node.appendToNativeBrushBehavior(nativePointer)
        }
        return nativeValidateOrDeleteAndThrow(nativePointer)
    }

    /** Creates an underlying native brush behavior with no nodes and returns its memory address. */
    private external fun nativeCreateEmptyBrushBehavior():
        Long // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    /**
     * Validates a native `BrushBehavior` and returns the pointer back, or deletes the native
     * `BrushBehavior` and throws an exception if it's not valid.
     */
    private external fun nativeValidateOrDeleteAndThrow(
        nativePointer: Long
    ): Long // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    /**
     * Release the underlying memory allocated in [nativeCreateBrushBehaviorLinear],
     * [nativeCreateBrushBehaviorPredefined], [nativeCreateBrushBehaviorSteps], or
     * [nativeCreateBrushBehaviorCubicBezier].
     */
    private external fun nativeFreeBrushBehavior(
        nativePointer: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    public companion object {
        init {
            NativeLoader.load()
        }

        /** Returns a new [BrushBehavior.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()

        @JvmField
        public val ALL_TOOL_TYPES: Set<InputToolType> =
            setOf(
                InputToolType.STYLUS,
                InputToolType.UNKNOWN,
                InputToolType.MOUSE,
                InputToolType.TOUCH
            )
    }

    /**
     * List of input properties along with their units that can act as sources for a
     * [BrushBehavior].
     */
    public class Source private constructor(@JvmField internal val value: Int) {
        internal fun toSimpleString(): String =
            when (this) {
                CONSTANT_ZERO -> "CONSTANT_ZERO"
                NORMALIZED_PRESSURE -> "NORMALIZED_PRESSURE"
                TILT_IN_RADIANS -> "TILT_IN_RADIANS"
                TILT_X_IN_RADIANS -> "TILT_X_IN_RADIANS"
                TILT_Y_IN_RADIANS -> "TILT_Y_IN_RADIANS"
                ORIENTATION_IN_RADIANS -> "ORIENTATION_IN_RADIANS"
                ORIENTATION_ABOUT_ZERO_IN_RADIANS -> "ORIENTATION_ABOUT_ZERO_IN_RADIANS"
                SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND ->
                    "SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND"
                VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND ->
                    "VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND"
                VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND ->
                    "VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND"
                DIRECTION_IN_RADIANS -> "DIRECTION_IN_RADIANS"
                DIRECTION_ABOUT_ZERO_IN_RADIANS -> "DIRECTION_ABOUT_ZERO_IN_RADIANS"
                NORMALIZED_DIRECTION_X -> "NORMALIZED_DIRECTION_X"
                NORMALIZED_DIRECTION_Y -> "NORMALIZED_DIRECTION_Y"
                DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE ->
                    "DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE"
                TIME_OF_INPUT_IN_SECONDS -> "TIME_OF_INPUT_IN_SECONDS"
                TIME_OF_INPUT_IN_MILLIS -> "TIME_OF_INPUT_IN_MILLIS"
                PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE ->
                    "PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE"
                PREDICTED_TIME_ELAPSED_IN_SECONDS -> "PREDICTED_TIME_ELAPSED_IN_SECONDS"
                PREDICTED_TIME_ELAPSED_IN_MILLIS -> "PREDICTED_TIME_ELAPSED_IN_MILLIS"
                DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE ->
                    "DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE"
                TIME_SINCE_INPUT_IN_SECONDS -> "TIME_SINCE_INPUT_IN_SECONDS"
                TIME_SINCE_INPUT_IN_MILLIS -> "TIME_SINCE_INPUT_IN_MILLIS"
                ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
                    "ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
                ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
                    "ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
                ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
                    "ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
                ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
                    "ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
                ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
                    "ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED"
                INPUT_SPEED_IN_CENTIMETERS_PER_SECOND -> "INPUT_SPEED_IN_CENTIMETERS_PER_SECOND"
                INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND ->
                    "INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND"
                INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND ->
                    "INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND"
                INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS -> "INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS"
                PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS ->
                    "PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS"
                INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED ->
                    "INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED"
                INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED ->
                    "INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED"
                INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED ->
                    "INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED"
                INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED ->
                    "INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED"
                INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED ->
                    "INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + this.toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Source) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {

            /**
             * A source whose value is always zero. This can be used to provide a constant modifier
             * to a target value. Normally this is not needed, because you can just set those
             * modifiers directly on the [BrushTip], but it can become useful when combined with the
             * [enabledToolTypes] and/or [isFallbackFor] fields to only conditionally enable it.
             */
            @JvmField public val CONSTANT_ZERO: Source = Source(0)
            /** Stylus or touch pressure with values reported in the range [0, 1]. */
            @JvmField public val NORMALIZED_PRESSURE: Source = Source(1)
            /** Stylus tilt with values reported in the range [0, π/2] radians. */
            @JvmField public val TILT_IN_RADIANS: Source = Source(2)
            /**
             * Stylus tilt along the x axis in the range [-π/2, π/2], with a positive value
             * corresponding to tilt toward the respective positive axis. In order for those values
             * to be reported, both tilt and orientation have to be populated on the StrokeInput.
             */
            @JvmField public val TILT_X_IN_RADIANS: Source = Source(3)
            /**
             * Stylus tilt along the y axis in the range [-π/2, π/2], with a positive value
             * corresponding to tilt toward the respective positive axis. In order for those values
             * to be reported, both tilt and orientation have to be populated on the StrokeInput.
             */
            @JvmField public val TILT_Y_IN_RADIANS: Source = Source(4)
            /** Stylus orientation with values reported in the range [0, 2π). */
            @JvmField public val ORIENTATION_IN_RADIANS: Source = Source(5)
            /** Stylus orientation with values reported in the range (-π, π]. */
            @JvmField public val ORIENTATION_ABOUT_ZERO_IN_RADIANS: Source = Source(6)
            /**
             * Pointer speed with values >= 0 in distance units per second, where one distance unit
             * is equal to the brush size.
             */
            @JvmField public val SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND: Source = Source(7)
            /**
             * Signed x component of pointer velocity in distance units per second, where one
             * distance unit is equal to the brush size.
             */
            @JvmField
            public val VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND: Source = Source(8)
            /**
             * Signed y component of pointer velocity in distance units per second, where one
             * distance unit is equal to the brush size.
             */
            @JvmField
            public val VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND: Source = Source(9)
            /**
             * The angle of the stroke's current direction of travel in stroke space, normalized to
             * the range [0, 2π). A value of 0 indicates the direction of the positive X-axis in
             * stroke space; a value of π/2 indicates the direction of the positive Y-axis in stroke
             * space.
             */
            @JvmField public val DIRECTION_IN_RADIANS: Source = Source(10)
            /**
             * The angle of the stroke's current direction of travel in stroke space, normalized to
             * the range (-π, π]. A value of 0 indicates the direction of the positive X-axis in
             * stroke space; a value of π/2 indicates the direction of the positive Y-axis in stroke
             * space.
             */
            @JvmField public val DIRECTION_ABOUT_ZERO_IN_RADIANS: Source = Source(11)
            /**
             * Signed x component of the normalized travel direction, with values in the range
             * [-1, 1].
             */
            @JvmField public val NORMALIZED_DIRECTION_X: Source = Source(12)
            /**
             * Signed y component of the normalized travel direction, with values in the range
             * [-1, 1].
             */
            @JvmField public val NORMALIZED_DIRECTION_Y: Source = Source(13)
            /**
             * Distance traveled by the inputs of the current stroke, starting at 0 at the first
             * input, where one distance unit is equal to the brush size.
             */
            @JvmField public val DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE: Source = Source(14)
            /**
             * The time elapsed, in seconds, from when the stroke started to when this part of the
             * stroke was drawn. The value remains fixed for any given part of the stroke once
             * drawn.
             */
            @JvmField public val TIME_OF_INPUT_IN_SECONDS: Source = Source(15)
            /**
             * The time elapsed, in millis, from when the stroke started to when this part of the
             * stroke was drawn. The value remains fixed for any given part of the stroke once
             * drawn.
             */
            @JvmField public val TIME_OF_INPUT_IN_MILLIS: Source = Source(16)
            /**
             * Distance traveled by the inputs of the current prediction, starting at 0 at the last
             * non-predicted input, where one distance unit is equal to the brush size. For cases
             * where prediction hasn't started yet, we don't return a negative value, but clamp to a
             * min of 0.
             */
            @JvmField
            public val PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE: Source = Source(17)
            /**
             * Elapsed time of the prediction, starting at 0 at the last non-predicted input. For
             * cases where prediction hasn't started yet, we don't return a negative value, but
             * clamp to a min of 0.
             */
            @JvmField public val PREDICTED_TIME_ELAPSED_IN_SECONDS: Source = Source(18)
            /**
             * Elapsed time of the prediction, starting at 0 at the last non-predicted input. For
             * cases where prediction hasn't started yet, we don't return a negative value, but
             * clamp to a min of 0.
             */
            @JvmField public val PREDICTED_TIME_ELAPSED_IN_MILLIS: Source = Source(19)
            /**
             * The distance left to be traveled from a given input to the current last input of the
             * stroke, where one distance unit is equal to the brush size. This value changes for
             * each input as the stroke is drawn.
             */
            @JvmField public val DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE: Source = Source(20)
            /**
             * The amount of time that has elapsed, in seconds, since this part of the stroke was
             * drawn. This continues to increase even after all stroke inputs have completed, and
             * can be used to drive stroke animations. This enumerators are only compatible with a
             * [sourceOutOfRangeBehavior] of [OutOfRange.CLAMP], to ensure that the animation will
             * eventually end.
             */
            @JvmField public val TIME_SINCE_INPUT_IN_SECONDS: Source = Source(21)
            /**
             * The amount of time that has elapsed, in millis, since this part of the stroke was
             * drawn. This continues to increase even after all stroke inputs have completed, and
             * can be used to drive stroke animations. This enumerators are only compatible with a
             * [sourceOutOfRangeBehavior] of [OutOfRange.CLAMP], to ensure that the animation will
             * eventually end.
             */
            @JvmField public val TIME_SINCE_INPUT_IN_MILLIS: Source = Source(22)
            /**
             * Directionless pointer acceleration with values >= 0 in distance units per second
             * squared, where one distance unit is equal to the brush size.
             */
            @JvmField
            public val ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(23)
            /**
             * Signed x component of pointer acceleration in distance units per second squared,
             * where one distance unit is equal to the brush size.
             */
            @JvmField
            public val ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(24)
            /**
             * Signed y component of pointer acceleration in distance units per second squared,
             * where one distance unit is equal to the brush size.
             */
            @JvmField
            public val ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(25)
            /**
             * Pointer acceleration along the current direction of travel in distance units per
             * second squared, where one distance unit is equal to the brush size. A positive value
             * indicates that the pointer is accelerating along the current direction of travel,
             * while a negative value indicates that the pointer is decelerating.
             */
            @JvmField
            public val ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(26)
            /**
             * Pointer acceleration perpendicular to the current direction of travel in distance
             * units per second squared, where one distance unit is equal to the brush size. If the
             * X- and Y-axes of stroke space were rotated so that the positive X-axis points in the
             * direction of stroke travel, then a positive value for this source indicates
             * acceleration along the positive Y-axis (and a negative value indicates acceleration
             * along the negative Y-axis).
             */
            @JvmField
            public val ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(27)
            /**
             * The physical speed of the input pointer at the point in question, in centimeters per
             * second.
             */
            @JvmField public val INPUT_SPEED_IN_CENTIMETERS_PER_SECOND: Source = Source(28)
            /**
             * Signed x component of the physical velocity of the input pointer at the point in
             * question, in centimeters per second.
             */
            @JvmField public val INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND: Source = Source(29)
            /**
             * Signed y component of the physical velocity of the input pointer at the point in
             * question, in centimeters per second.
             */
            @JvmField public val INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND: Source = Source(30)
            /**
             * The physical distance traveled by the input pointer from the start of the stroke
             * along the input path to the point in question, in centimeters.
             */
            @JvmField public val INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS: Source = Source(31)
            /**
             * The physical distance that the input pointer would have to travel from its actual
             * last real position along its predicted path to reach the predicted point in question,
             * in centimeters. For points on the stroke before the predicted portion, this has a
             * value of zero.
             */
            @JvmField
            public val PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS: Source = Source(32)
            /**
             * The directionless physical acceleration of the input pointer at the point in
             * question, with values >= 0, in centimeters per second squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED: Source = Source(33)
            /**
             * Signed x component of the physical acceleration of the input pointer, in centimeters
             * per second squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED: Source = Source(34)
            /**
             * Signed y component of the physical acceleration of the input pointer, in centimeters
             * per second squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED: Source = Source(35)
            /**
             * The physical acceleration of the input pointer along its current direction of travel
             * at the point in question, in centimeters per second squared. A positive value
             * indicates that the pointer is accelerating along the current direction of travel,
             * while a negative value indicates that the pointer is decelerating.
             */
            @JvmField
            public val INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(36)
            /**
             * The physical acceleration of the input pointer perpendicular to its current direction
             * of travel at the point in question, in centimeters per second squared. If the X- and
             * Y-axes of stroke space were rotated so that the positive X-axis points in the
             * direction of stroke travel, then a positive value for this source indicates
             * acceleration along the positive Y-axis (and a negative value indicates acceleration
             * along the negative Y-axis).
             */
            @JvmField
            public val INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(37)
            private const val PREFIX = "BrushBehavior.Source."
        }
    }

    /** List of tip properties that can be modified by a [BrushBehavior]. */
    public class Target private constructor(@JvmField internal val value: Int) {

        internal fun toSimpleString(): String =
            when (this) {
                WIDTH_MULTIPLIER -> "WIDTH_MULTIPLIER"
                HEIGHT_MULTIPLIER -> "HEIGHT_MULTIPLIER"
                SIZE_MULTIPLIER -> "SIZE_MULTIPLIER"
                SLANT_OFFSET_IN_RADIANS -> "SLANT_OFFSET_IN_RADIANS"
                PINCH_OFFSET -> "PINCH_OFFSET"
                ROTATION_OFFSET_IN_RADIANS -> "ROTATION_OFFSET_IN_RADIANS"
                CORNER_ROUNDING_OFFSET -> "CORNER_ROUNDING_OFFSET"
                POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE ->
                    "POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE"
                POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE ->
                    "POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE"
                POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE ->
                    "POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE"
                POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE ->
                    "POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE"
                HUE_OFFSET_IN_RADIANS -> "HUE_OFFSET_IN_RADIANS"
                SATURATION_MULTIPLIER -> "SATURATION_MULTIPLIER"
                LUMINOSITY -> "LUMINOSITY"
                OPACITY_MULTIPLIER -> "OPACITY_MULTIPLIER"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + this.toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Target) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {

            /**
             * Scales the brush-tip width, starting from the value calculated using
             * [BrushTip.scaleX] and [BrushTip.scaleY]. The final brush width is clamped to a
             * maximum of twice the base width. If multiple behaviors have one of these targets,
             * they stack multiplicatively.
             */
            @JvmField public val WIDTH_MULTIPLIER: Target = Target(0)
            /**
             * Scales the brush-tip height, starting from the value calculated using
             * [BrushTip.scaleX] and [BrushTip.scaleY]. The final brush height is clamped to a
             * maximum of twice the base height. If multiple behaviors have one of these targets,
             * they stack multiplicatively.
             */
            @JvmField public val HEIGHT_MULTIPLIER: Target = Target(1)
            /** Convenience enumerator to target both [WIDTH_MULTIPLIER] and [HEIGHT_MULTIPLIER]. */
            @JvmField public val SIZE_MULTIPLIER: Target = Target(2)
            /**
             * Adds the target modifier to [BrushTip.slant]. The final brush slant value is clamped
             * to [-π/2, π/2]. If multiple behaviors have this target, they stack additively.
             */
            @JvmField public val SLANT_OFFSET_IN_RADIANS: Target = Target(3)
            /**
             * Adds the target modifier to [BrushTip.pinch]. The final brush pinch value is clamped
             * to [0, 1]. If multiple behaviors have this target, they stack additively.
             */
            @JvmField public val PINCH_OFFSET: Target = Target(4)
            /**
             * Adds the target modifier to [BrushTip.rotation]. The final brush rotation angle is
             * effectively normalized (mod 2π). If multiple behaviors have this target, they stack
             * additively.
             */
            @JvmField public val ROTATION_OFFSET_IN_RADIANS: Target = Target(5)
            /**
             * Adds the target modifier to [BrushTip.cornerRounding]. The final brush corner
             * rounding value is clamped to [0, 1]. If multiple behaviors have this target, they
             * stack additively.
             */
            @JvmField public val CORNER_ROUNDING_OFFSET: Target = Target(6)
            /**
             * Adds the target modifier to the brush tip x position, where one distance unit is
             * equal to the brush size.
             */
            @JvmField public val POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE: Target = Target(7)
            /**
             * Adds the target modifier to the brush tip y position, where one distance unit is
             * equal to the brush size.
             */
            @JvmField public val POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE: Target = Target(8)
            /**
             * Moves the brush tip center forward (or backward, for negative values) from the input
             * position, in the current direction of stroke travel, where one distance unit is equal
             * to the brush size.
             */
            @JvmField
            public val POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE: Target = Target(9)
            /**
             * Moves the brush tip center sideways from the input position, relative to the
             * direction of stroke travel, where one distance unit is equal to the brush size. If
             * the X- and Y-axes of stroke space were rotated so that the positive X-axis points in
             * the direction of stroke travel, then a positive value for this offset moves the brush
             * tip center towards the positive Y-axis (and a negative value moves the brush tip
             * center towards the negative Y-axis).
             */
            @JvmField
            public val POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE: Target = Target(10)

            // The following are targets for tip color adjustments, including opacity. Renderers can
            // apply
            // them to the brush color when a stroke is drawn to contribute to the local color of
            // each
            // part of the stroke.
            /**
             * Shifts the hue of the base brush color. A positive offset shifts around the hue wheel
             * from red towards orange, while a negative offset shifts the other way, from red
             * towards violet. The final hue offset is not clamped, but is effectively normalized
             * (mod 2π). If multiple behaviors have this target, they stack additively.
             */
            @JvmField public val HUE_OFFSET_IN_RADIANS: Target = Target(11)
            /**
             * Scales the saturation of the base brush color. If multiple behaviors have one of
             * these targets, they stack multiplicatively. The final saturation multiplier is
             * clamped to [0, 2].
             */
            @JvmField public val SATURATION_MULTIPLIER: Target = Target(12)
            /**
             * Target the luminosity of the color. An offset of +/-100% corresponds to changing the
             * luminosity by up to +/-100%.
             */
            @JvmField public val LUMINOSITY: Target = Target(13)
            /**
             * Scales the opacity of the base brush color. If multiple behaviors have one of these
             * targets, they stack multiplicatively. The final opacity multiplier is clamped to
             * [0, 2].
             */
            @JvmField public val OPACITY_MULTIPLIER: Target = Target(14)

            private const val PREFIX = "BrushBehavior.Target."
        }
    }

    /**
     * The desired behavior when an input value is outside the range defined by
     * [sourceValueRangeLowerBound, sourceValueRangeUpperBound].
     */
    public class OutOfRange private constructor(@JvmField internal val value: Int) {
        internal fun toSimpleString(): String =
            when (this) {
                CLAMP -> "CLAMP"
                REPEAT -> "REPEAT"
                MIRROR -> "MIRROR"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + this.toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is OutOfRange) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {

            // Values outside the range will be clamped to not exceed the bounds.
            @JvmField public val CLAMP: OutOfRange = OutOfRange(0)
            // Values will be shifted by an integer multiple of the range size so that they fall
            // within
            // the bounds.
            //
            // In this case, the range will be treated as a half-open interval, with a value exactly
            // at
            // [sourceValueRangeUpperBound] being treated as though it was
            // [sourceValueRangeLowerBound].
            @JvmField public val REPEAT: OutOfRange = OutOfRange(1)
            // Similar to [Repeat], but every other repetition of the bounds will be mirrored, as
            // though
            // the
            // two elements [sourceValueRangeLowerBound] and [sourceValueRangeUpperBound] were
            // swapped.
            // This means the range does not need to be treated as a half-open interval like in the
            // case
            // of [Repeat].
            @JvmField public val MIRROR: OutOfRange = OutOfRange(2)
            private const val PREFIX = "BrushBehavior.OutOfRange."
        }
    }

    /** List of input properties that might not be reported by inputs. */
    public class OptionalInputProperty private constructor(@JvmField internal val value: Int) {

        internal fun toSimpleString(): String =
            when (this) {
                PRESSURE -> "PRESSURE"
                TILT -> "TILT"
                ORIENTATION -> "ORIENTATION"
                TILT_X_AND_Y -> "TILT_X_AND_Y"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + this.toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is OptionalInputProperty) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {

            @JvmField public val PRESSURE: OptionalInputProperty = OptionalInputProperty(0)
            @JvmField public val TILT: OptionalInputProperty = OptionalInputProperty(1)
            @JvmField public val ORIENTATION: OptionalInputProperty = OptionalInputProperty(2)
            /** Tilt-x and tilt-y require both tilt and orientation to be reported. */
            @JvmField public val TILT_X_AND_Y: OptionalInputProperty = OptionalInputProperty(3)
            private const val PREFIX = "BrushBehavior.OptionalInputProperty."
        }
    }

    /** A binary operation for combining two values in a [BinaryOpNode]. */
    public class BinaryOp private constructor(@JvmField internal val value: Int) {

        internal fun toSimpleString(): String =
            when (this) {
                PRODUCT -> "PRODUCT"
                SUM -> "SUM"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + this.toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is BinaryOp) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /** Evaluates to the product of the two input values, or null if either is null. */
            @JvmField public val PRODUCT: BinaryOp = BinaryOp(0)
            /** Evaluates to the sum of the two input values, or null if either is null. */
            @JvmField public val SUM: BinaryOp = BinaryOp(1)

            private const val PREFIX = "BrushBehavior.BinaryOp."
        }
    }

    /** Dimensions/units for measuring the [dampingGap] field of a [DampingNode] */
    public class DampingSource private constructor(@JvmField internal val value: Int) {

        internal fun toSimpleString(): String =
            when (this) {
                DISTANCE_IN_CENTIMETERS -> "DISTANCE_IN_CENTIMETERS"
                DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> "DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE"
                TIME_IN_SECONDS -> "TIME_IN_SECONDS"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + this.toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is DampingSource) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /**
             * Value damping occurs over distance traveled by the input pointer, and the
             * [dampingGap] is measured in centimeters. If the input data does not indicate the
             * relationship between stroke units and physical units (e.g. as may be the case for
             * programmatically-generated inputs), then no damping will be performed (i.e. the
             * [dampingGap] will be treated as zero).
             */
            @JvmField public val DISTANCE_IN_CENTIMETERS: DampingSource = DampingSource(0)
            /**
             * Value damping occurs over distance traveled by the input pointer, and the
             * [dampingGap] is measured in multiples of the brush size.
             */
            @JvmField
            public val DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE: DampingSource = DampingSource(1)
            /** Value damping occurs over time, and the [dampingGap] is measured in seconds. */
            @JvmField public val TIME_IN_SECONDS: DampingSource = DampingSource(2)

            private const val PREFIX = "BrushBehavior.DampingSource."
        }
    }

    /** Interpolation functions for use in an [InterpolationNode]. */
    public class Interpolation private constructor(@JvmField internal val value: Int) {

        internal fun toSimpleString(): String =
            when (this) {
                LERP -> "LERP"
                INVERSE_LERP -> "INVERSE_LERP"
                else -> "INVALID"
            }

        override fun toString(): String = PREFIX + this.toSimpleString()

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Interpolation) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /**
             * Linear interpolation. Evaluates to the [InterpolationNode.startInput] value when the
             * [InterpolationNode.paramInput] value is 0, and to the [InterpolationNode.endInput]
             * value when the [InterpolationNode.paramInput] value is 1.
             */
            @JvmField public val LERP: Interpolation = Interpolation(0)
            /**
             * Inverse linear interpolation. Evaluates to 0 when the [InterpolationNode.paramInput]
             * value is equal to the [InterpolationNode.startInput] value, and to 1 when the
             * parameter is equal to the [InterpolationNode.endInput] value. Evaluates to null when
             * the [InterpolationNode.startInput] and [InterpolationNode.endInput] values are equal.
             */
            @JvmField public val INVERSE_LERP: Interpolation = Interpolation(1)

            private const val PREFIX = "BrushBehavior.Interpolation."
        }
    }

    /**
     * Represents one node in a [BrushBehavior]'s expression graph. [Node] objects are immutable and
     * their inputs must be chosen at construction time; therefore, they can only ever be assembled
     * into an acyclic graph.
     */
    public abstract class Node
    internal constructor(
        /** The ordered list of inputs that this node directly depends on. */
        public val inputs: List<ValueNode>
    ) {
        /** Appends a native version of this [Node] to a native [BrushBehavior]. */
        internal abstract fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long)
    }

    /**
     * A [ValueNode] is a non-terminal node in the graph; it produces a value to be consumed as an
     * input by other [Node]s, and may itself depend on zero or more inputs.
     */
    public abstract class ValueNode internal constructor(inputs: List<ValueNode>) : Node(inputs) {}

    /** A [ValueNode] that gets data from the stroke input batch. */
    public class SourceNode
    @JvmOverloads
    constructor(
        public val source: Source,
        public val sourceValueRangeLowerBound: Float,
        public val sourceValueRangeUpperBound: Float,
        public val sourceOutOfRangeBehavior: OutOfRange = OutOfRange.CLAMP,
    ) : ValueNode(emptyList()) {
        init {
            require(sourceValueRangeLowerBound.isFinite()) {
                "sourceValueRangeLowerBound must be finite, was $sourceValueRangeLowerBound"
            }
            require(sourceValueRangeUpperBound.isFinite()) {
                "sourceValueRangeUpperBound must be finite, was $sourceValueRangeUpperBound"
            }
            require(sourceValueRangeLowerBound != sourceValueRangeUpperBound) {
                "sourceValueRangeLowerBound and sourceValueRangeUpperBound must be distinct, both were $sourceValueRangeLowerBound"
            }
        }

        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendSourceNode(
                nativeBehaviorPointer,
                source.value,
                sourceValueRangeLowerBound,
                sourceValueRangeUpperBound,
                sourceOutOfRangeBehavior.value,
            )
        }

        override fun toString(): String =
            "SourceNode(${source.toSimpleString()}, $sourceValueRangeLowerBound, $sourceValueRangeUpperBound, ${sourceOutOfRangeBehavior.toSimpleString()})"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is SourceNode) return false
            return source == other.source &&
                sourceValueRangeLowerBound == other.sourceValueRangeLowerBound &&
                sourceValueRangeUpperBound == other.sourceValueRangeUpperBound &&
                sourceOutOfRangeBehavior == other.sourceOutOfRangeBehavior
        }

        override fun hashCode(): Int {
            var result = source.hashCode()
            result = 31 * result + sourceValueRangeLowerBound.hashCode()
            result = 31 * result + sourceValueRangeUpperBound.hashCode()
            result = 31 * result + sourceOutOfRangeBehavior.hashCode()
            return result
        }

        /** Appends a native `BrushBehavior::SourceNode` to a native brush behavior struct. */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendSourceNode(
            nativeBehaviorPointer: Long,
            source: Int,
            sourceValueRangeLowerBound: Float,
            sourceValueRangeUpperBound: Float,
            sourceOutOfRangeBehavior: Int,
        )
    }

    /** A [ValueNode] that produces a constant output value. */
    public class ConstantNode constructor(public val value: Float) : ValueNode(emptyList()) {
        init {
            require(value.isFinite()) { "value must be finite, was $value" }
        }

        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendConstantNode(nativeBehaviorPointer, value)
        }

        override fun toString(): String = "ConstantNode($value)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is ConstantNode) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        /** Appends a native `BrushBehavior::ConstantNode` to a native brush behavior struct. */
        private external fun nativeAppendConstantNode(
            nativeBehaviorPointer: Long,
            value: Float
        ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    }

    /**
     * A [ValueNode] for filtering out a branch of a behavior graph unless a particular stroke input
     * property is missing.
     */
    public class FallbackFilterNode
    constructor(public val isFallbackFor: OptionalInputProperty, public val input: ValueNode) :
        ValueNode(listOf(input)) {
        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendFallbackFilterNode(nativeBehaviorPointer, isFallbackFor.value)
        }

        override fun toString(): String =
            "FallbackFilterNode(${isFallbackFor.toSimpleString()}, $input)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is FallbackFilterNode) return false
            if (other === this) return true
            return isFallbackFor == other.isFallbackFor && input == other.input
        }

        override fun hashCode(): Int {
            var result = isFallbackFor.hashCode()
            result = 31 * result + input.hashCode()
            return result
        }

        /**
         * Appends a native `BrushBehavior::FallbackFilterNode` to a native brush behavior struct.
         */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendFallbackFilterNode(
            nativeBehaviorPointer: Long,
            isFallbackFor: Int,
        )
    }

    /**
     * A [ValueNode] for filtering out a branch of a behavior graph unless this stroke's tool type
     * is in the specified set.
     */
    public class ToolTypeFilterNode
    constructor(
        // The [enabledToolTypes] val below is a defensive copy of this parameter.
        enabledToolTypes: Set<InputToolType>,
        public val input: ValueNode,
    ) : ValueNode(listOf(input)) {
        public val enabledToolTypes: Set<InputToolType> = unmodifiableSet(enabledToolTypes.toSet())

        init {
            require(!enabledToolTypes.isEmpty()) { "enabledToolTypes must be non-empty" }
        }

        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendToolTypeFilterNode(
                nativeBehaviorPointer = nativeBehaviorPointer,
                mouseEnabled = enabledToolTypes.contains(InputToolType.MOUSE),
                touchEnabled = enabledToolTypes.contains(InputToolType.TOUCH),
                stylusEnabled = enabledToolTypes.contains(InputToolType.STYLUS),
                unknownEnabled = enabledToolTypes.contains(InputToolType.UNKNOWN),
            )
        }

        override fun toString(): String = "ToolTypeFilterNode($enabledToolTypes, $input)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is ToolTypeFilterNode) return false
            if (other === this) return true
            return enabledToolTypes == other.enabledToolTypes && input == other.input
        }

        override fun hashCode(): Int {
            var result = enabledToolTypes.hashCode()
            result = 31 * result + input.hashCode()
            return result
        }

        /**
         * Appends a native `BrushBehavior::ToolTypeFilterNode` to a native brush behavior struct.
         */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendToolTypeFilterNode(
            nativeBehaviorPointer: Long,
            mouseEnabled: Boolean,
            touchEnabled: Boolean,
            stylusEnabled: Boolean,
            unknownEnabled: Boolean,
        )
    }

    /**
     * A [ValueNode] that damps changes in an input value, causing the output value to slowly follow
     * changes in the input value over a specified time or distance.
     */
    public class DampingNode
    constructor(
        public val dampingSource: DampingSource,
        public val dampingGap: Float,
        public val input: ValueNode,
    ) : ValueNode(listOf(input)) {
        init {
            require(dampingGap.isFinite() && dampingGap >= 0.0f) {
                "dampingGap must be finite and non-negative, was $dampingGap"
            }
        }

        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendDampingNode(nativeBehaviorPointer, dampingSource.value, dampingGap)
        }

        override fun toString(): String =
            "DampingNode(${dampingSource.toSimpleString()}, $dampingGap, $input)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is DampingNode) return false
            if (other === this) return true
            return dampingSource == other.dampingSource &&
                dampingGap == other.dampingGap &&
                input == other.input
        }

        override fun hashCode(): Int {
            var result = dampingSource.hashCode()
            result = 31 * result + dampingGap.hashCode()
            result = 31 * result + input.hashCode()
            return result
        }

        /** Appends a native `BrushBehavior::DampingNode` to a native brush behavior struct. */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendDampingNode(
            nativeBehaviorPointer: Long,
            dampingSource: Int,
            dampingGap: Float,
        )
    }

    /** A [ValueNode] that maps an input value through a response curve. */
    public class ResponseNode
    constructor(public val responseCurve: EasingFunction, public val input: ValueNode) :
        ValueNode(listOf(input)) {
        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            when (responseCurve) {
                is EasingFunction.Predefined ->
                    nativeAppendResponseNodePredefined(nativeBehaviorPointer, responseCurve.value)
                is EasingFunction.CubicBezier ->
                    nativeAppendResponseNodeCubicBezier(
                        nativeBehaviorPointer,
                        responseCurve.x1,
                        responseCurve.y1,
                        responseCurve.x2,
                        responseCurve.y2,
                    )
                is EasingFunction.Steps ->
                    nativeAppendResponseNodeSteps(
                        nativeBehaviorPointer,
                        responseCurve.stepCount,
                        responseCurve.stepPosition.value,
                    )
                is EasingFunction.Linear ->
                    nativeAppendResponseNodeLinear(
                        nativeBehaviorPointer,
                        FloatArray(responseCurve.points.size * 2).apply {
                            var index = 0
                            for (point in responseCurve.points) {
                                set(index, point.x)
                                ++index
                                set(index, point.y)
                                ++index
                            }
                        },
                    )
            }
        }

        override fun toString(): String = "ResponseNode($responseCurve, $input)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is ResponseNode) return false
            if (other === this) return true
            return responseCurve == other.responseCurve && input == other.input
        }

        override fun hashCode(): Int {
            var result = responseCurve.hashCode()
            result = 31 * result + input.hashCode()
            return result
        }

        /**
         * Appends a native `BrushBehavior::ResponseNode` with response curve of type
         * [EasingFunction.Predefined] to a native brush behavior struct.
         */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendResponseNodePredefined(
            nativeBehaviorPointer: Long,
            predefinedResponseCurve: Int,
        )

        /**
         * Appends a native `BrushBehavior::ResponseNode` with response curve of type
         * [EasingFunction.CubicBezier] to a native brush behavior struct.
         */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendResponseNodeCubicBezier(
            nativeBehaviorPointer: Long,
            cubicBezierX1: Float,
            cubicBezierX2: Float,
            cubicBezierY1: Float,
            cubicBezierY2: Float,
        )

        /**
         * Appends a native `BrushBehavior::ResponseNode` with response curve of type
         * [EasingFunction.Steps] to a native brush behavior struct.
         */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendResponseNodeSteps(
            nativeBehaviorPointer: Long,
            stepsCount: Int,
            stepsPosition: Int,
        )

        /**
         * Appends a native `BrushBehavior::ResponseNode` with response curve of type
         * [EasingFunction.Linear] to a native brush behavior struct.
         */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendResponseNodeLinear(
            nativeBehaviorPointer: Long,
            points: FloatArray,
        )
    }

    /** A [ValueNode] that combines two other values with a binary operation. */
    public class BinaryOpNode
    constructor(
        public val operation: BinaryOp,
        public val firstInput: ValueNode,
        public val secondInput: ValueNode,
    ) : ValueNode(listOf(firstInput, secondInput)) {
        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendBinaryOpNode(nativeBehaviorPointer, operation.value)
        }

        override fun toString(): String =
            "BinaryOpNode(${operation.toSimpleString()}, $firstInput, $secondInput)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is BinaryOpNode) return false
            if (other === this) return true
            return operation == other.operation &&
                firstInput == other.firstInput &&
                secondInput == other.secondInput
        }

        override fun hashCode(): Int {
            var result = operation.hashCode()
            result = 31 * result + firstInput.hashCode()
            result = 31 * result + secondInput.hashCode()
            return result
        }

        /** Appends a native `BrushBehavior::BinaryOpNode` to a native brush behavior struct. */
        private external fun nativeAppendBinaryOpNode(
            nativeBehaviorPointer: Long,
            operation: Int
        ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    }

    /**
     * A [ValueNode] that interpolates between two inputs based on a parameter input. The specific
     * kind of interpolation performed depends on the [Interpolation] parameter.
     */
    public class InterpolationNode
    constructor(
        /** What kind of interpolation to perform. */
        public val interpolation: Interpolation,
        /** The input whose value is used as the parameter within the interpolation range. */
        public val paramInput: ValueNode,
        /** The input whose value forms the start of the interpolation range. */
        public val startInput: ValueNode,
        /** The input whose value forms the end of the interpolation range. */
        public val endInput: ValueNode,
    ) : ValueNode(listOf(paramInput, startInput, endInput)) {
        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendInterpolationNode(nativeBehaviorPointer, interpolation.value)
        }

        override fun toString(): String =
            "InterpolationNode(${interpolation.toSimpleString()}, $paramInput, $startInput, $endInput)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is InterpolationNode) return false
            if (other === this) return true
            return interpolation == other.interpolation &&
                paramInput == other.paramInput &&
                startInput == other.startInput &&
                endInput == other.endInput
        }

        override fun hashCode(): Int {
            var result = interpolation.hashCode()
            result = 31 * result + paramInput.hashCode()
            result = 31 * result + startInput.hashCode()
            result = 31 * result + endInput.hashCode()
            return result
        }

        /**
         * Appends a native `BrushBehavior::InterpolationNode` to a native brush behavior struct.
         */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendInterpolationNode(
            nativeBehaviorPointer: Long,
            interpolation: Int,
        )
    }

    /**
     * A [TargetNode] is a terminal node in the graph; it does not produce a value and cannot be
     * used as an input to other [Node]s, but instead applies a modification to the brush tip state.
     * A [BrushBehavior] consists of a list of [TargetNode]s and the various [ValueNode]s that they
     * transitively depend on.
     */
    public class TargetNode
    constructor(
        public val target: Target,
        public val targetModifierRangeLowerBound: Float,
        public val targetModifierRangeUpperBound: Float,
        public val input: ValueNode,
    ) : Node(listOf(input)) {
        init {
            require(targetModifierRangeLowerBound.isFinite()) {
                "targetModifierRangeLowerBound must be finite, was $targetModifierRangeLowerBound"
            }
            require(targetModifierRangeUpperBound.isFinite()) {
                "targetModifierRangeUpperBound must be finite, was $targetModifierRangeUpperBound"
            }
            require(targetModifierRangeLowerBound != targetModifierRangeUpperBound) {
                "targetModifierRangeLowerBound and targetModifierRangeUpperBound must be distinct, both were $targetModifierRangeLowerBound"
            }
        }

        override fun appendToNativeBrushBehavior(nativeBehaviorPointer: Long) {
            nativeAppendTargetNode(
                nativeBehaviorPointer,
                target.value,
                targetModifierRangeLowerBound,
                targetModifierRangeUpperBound,
            )
        }

        override fun toString(): String =
            "TargetNode(${target.toSimpleString()}, $targetModifierRangeLowerBound, $targetModifierRangeUpperBound, $input)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is TargetNode) return false
            if (other === this) return true
            return target == other.target &&
                targetModifierRangeLowerBound == other.targetModifierRangeLowerBound &&
                targetModifierRangeUpperBound == other.targetModifierRangeUpperBound &&
                input == other.input
        }

        override fun hashCode(): Int {
            var result = target.hashCode()
            result = 31 * result + targetModifierRangeLowerBound.hashCode()
            result = 31 * result + targetModifierRangeUpperBound.hashCode()
            result = 31 * result + input.hashCode()
            return result
        }

        /** Appends a native `BrushBehavior::TargetNode` to a native brush behavior struct. */
        // TODO: b/355248266 - @Keep must go in Proguard config file instead.
        private external fun nativeAppendTargetNode(
            nativeBehaviorPointer: Long,
            target: Int,
            targetModifierRangeLowerBound: Float,
            targetModifierRangeUpperBound: Float,
        )
    }
}
