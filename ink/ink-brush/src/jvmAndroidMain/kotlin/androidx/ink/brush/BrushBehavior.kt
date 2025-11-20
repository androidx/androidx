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
import androidx.collection.MutableIntObjectMap
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
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
 * 4. Terminal nodes apply one or more input values to chosen properties of the brush tip.
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@ExperimentalInkCustomBrushApi
// NotCloseable: Finalize is only used to free the native peer.
@Suppress("NotCloseable")
public class BrushBehavior
private constructor(
    /** A handle to the underlying native [BrushBehavior] object. */
    internal val nativePointer: Long,
    // The [terminalNodes] val below is a defensive copy of this parameter.
    terminalNodes: List<TerminalNode>,
) {
    public val terminalNodes: List<TerminalNode> = unmodifiableList(terminalNodes.toList())

    /** Constructs a [BrushBehavior] from a list of [TerminalNode]s. */
    public constructor(
        // The [terminalNodes] val above is a defensive copy of this parameter.
        terminalNodes: List<TerminalNode>
    ) : this(BrushBehaviorNative.createFromTerminalNodes(terminalNodes), terminalNodes)

    /**
     * Constructs a simple [BrushBehavior] using whatever [Node]s are necessary for the specified
     * fields.
     */
    public constructor(
        source: Source,
        target: Target,
        sourceValueRangeStart: Float,
        sourceValueRangeEnd: Float,
        targetModifierRangeStart: Float,
        targetModifierRangeEnd: Float,
        sourceOutOfRangeBehavior: OutOfRange = OutOfRange.CLAMP,
        responseCurve: EasingFunction = EasingFunction.Predefined.LINEAR,
        responseTimeMillis: Long = 0L,
        enabledToolTypes: Set<InputToolType> = ALL_TOOL_TYPES,
        isFallbackFor: OptionalInputProperty? = null,
    ) : this(
        run<List<TerminalNode>> {
            var node: ValueNode =
                SourceNode(
                    source,
                    sourceValueRangeStart,
                    sourceValueRangeEnd,
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
                        node,
                    )
            }
            listOf(TargetNode(target, targetModifierRangeStart, targetModifierRangeEnd, node))
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
     *   .setSourceValueRangeStart(...)
     *   .build();
     * ```
     */
    @Suppress("ScopeReceiverThis")
    public class Builder {
        private var source: Source = Source.NORMALIZED_PRESSURE
        private var target: Target = Target.SIZE_MULTIPLIER
        private var sourceOutOfRangeBehavior: OutOfRange = OutOfRange.CLAMP
        private var sourceValueRangeStart: Float = 0f
        private var sourceValueRangeEnd: Float = 1f
        private var targetModifierRangeStart: Float = 0f
        private var targetModifierRangeEnd: Float = 1f
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

        public fun setSourceValueRangeStart(sourceValueRangeStart: Float): Builder = apply {
            this.sourceValueRangeStart = sourceValueRangeStart
        }

        public fun setSourceValueRangeEnd(sourceValueRangeEnd: Float): Builder = apply {
            this.sourceValueRangeEnd = sourceValueRangeEnd
        }

        public fun setTargetModifierRangeStart(targetModifierRangeStart: Float): Builder = apply {
            this.targetModifierRangeStart = targetModifierRangeStart
        }

        public fun setTargetModifierRangeEnd(targetModifierRangeEnd: Float): Builder = apply {
            this.targetModifierRangeEnd = targetModifierRangeEnd
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
                sourceValueRangeStart,
                sourceValueRangeEnd,
                targetModifierRangeStart,
                targetModifierRangeEnd,
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
        return terminalNodes == other.terminalNodes
    }

    override fun hashCode(): Int {
        return terminalNodes.hashCode()
    }

    override fun toString(): String = "BrushBehavior($terminalNodes)"

    /** Delete native BrushBehavior memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        BrushBehaviorNative.free(nativePointer)
    }

    public companion object {
        /** Returns a new [BrushBehavior.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()

        @JvmField
        public val ALL_TOOL_TYPES: Set<InputToolType> =
            setOf(
                InputToolType.STYLUS,
                InputToolType.UNKNOWN,
                InputToolType.MOUSE,
                InputToolType.TOUCH,
            )

        /**
         * Construct a [BrushBehavior] from an unowned heap-allocated native pointer to a C++
         * `BrushBehavior`. Kotlin wrapper objects nested under the [BrushBehavior] are initialized
         * similarly using their own [wrapNative] methods, passing those pointers to newly
         * copy-constructed heap-allocated objects. That avoids the need to call Kotlin constructors
         * for those objects from C++.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): BrushBehavior {
            val terminalNodes = mutableListOf<TerminalNode>()
            val inputStack = ArrayDeque<ValueNode>()
            for (i in 0 until BrushBehaviorNative.getNodeCount(unownedNativePointer)) {
                when (
                    val node =
                        Node.wrapNative(
                            BrushBehaviorNative.newCopyOfNode(unownedNativePointer, i),
                            inputStack,
                        )
                ) {
                    is TerminalNode -> terminalNodes.add(node)
                    is ValueNode -> inputStack.addLast(node)
                    else ->
                        throw IllegalArgumentException(
                            "Node must either be a TerminalNode or ValueNode: $node"
                        )
                }
            }
            return BrushBehavior(unownedNativePointer, terminalNodes)
        }
    }

    /**
     * List of input properties along with their units that can act as sources for a
     * [BrushBehavior].
     *
     * Behaviors that consider properties of the stroke input do not consider alterations to the
     * visible position of that point in the stroke by brush behaviors that modify that position
     * (e.g. [Target.POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE]). That is, the position,
     * velocity, and acceleration of the stroke input may not match the visible position, velocity,
     * and acceleration of that point in the drawn stroke. The stroke inputs considered by these
     * behaviors are specifically the "modeled" inputs used to construct the stroke geometry, which
     * may be upsampled, denoised, or otherwise transformed from the raw stroke input.
     */
    public class Source
    internal constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate Source value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.Source.$name"

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
             * Time elapsed in millis from when the stroke started to when this part of the stroke
             * was drawn. The value remains fixed for any given part of the stroke once drawn.
             */
            @JvmField
            public val TIME_OF_INPUT_IN_MILLIS: Source = Source(15, "TIME_OF_INPUT_IN_MILLIS")
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
             * Elapsed time of the prediction in milliseconds, starting at 0 at the last
             * non-predicted input. Zero for inputs before the predicted portion of the stroke.
             */
            @JvmField
            public val PREDICTED_TIME_ELAPSED_IN_MILLIS: Source =
                Source(18, "PREDICTED_TIME_ELAPSED_IN_MILLIS")
            /**
             * The distance left to be traveled from a given modeled input to the current last
             * modeled input of the stroke in multiples of the brush size. This value changes for
             * each input as the stroke is drawn.
             */
            @JvmField
            public val DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE: Source =
                Source(19, "DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE")
            /**
             * Time elapsed in seconds since the modeled stroke input. This continues to increase
             * even after all stroke inputs have completed, and can be used to drive stroke
             * animations. This enumerators are only compatible with a [sourceOutOfRangeBehavior] of
             * [OutOfRange.CLAMP], to ensure that the animation will eventually end.
             */
            @JvmField
            public val TIME_SINCE_INPUT_IN_SECONDS: Source =
                Source(20, "TIME_SINCE_INPUT_IN_SECONDS")
            /**
             * Time elapsed in milliseconds since the modeled stroke input. This continues to
             * increase even after all stroke inputs have completed, and can be used to drive stroke
             * animations. This enumerators are only compatible with a [sourceOutOfRangeBehavior] of
             * [OutOfRange.CLAMP], to ensure that the animation will eventually end.
             */
            @JvmField
            public val TIME_SINCE_INPUT_IN_MILLIS: Source = Source(21, "TIME_SINCE_INPUT_IN_MILLIS")
            /**
             * Absolute acceleration of the modeled stroke input in multiples of the brush size per
             * second squared. Note that this value doesn't take into account brush behaviors that
             * offset the position of that visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(22, "ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed x component of the acceleration of the modeled stroke input in multiples of
             * the brush size per second squared. Note that this value doesn't take into account
             * brush behaviors that offset the position of that visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(23, "ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed y component of the acceleration of the modeled stroke input in multiples of
             * the brush size per second squared. Note that this value doesn't take into account
             * brush behaviors that offset the position of that visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(24, "ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed component of acceleration of the modeled stroke input in the direction of its
             * velocity in multiples of the brush size per second squared. Note that this value
             * doesn't take into account brush behaviors that offset the position of that visible
             * point in the stroke.
             */
            @JvmField
            public val ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(25, "ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /**
             * Signed component of acceleration of the modeled stroke input perpendicular to its
             * velocity, rotated 90 degrees in the direction from the positive x-axis towards the
             * positive y-axis, in multiples of the brush size per second squared. Note that this
             * value doesn't take into account brush behaviors that offset the position of that
             * visible point in the stroke.
             */
            @JvmField
            public val ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED: Source =
                Source(26, "ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED")
            /** Absolute speed of the modeled stroke input pointer in centimeters per second. */
            @JvmField
            public val INPUT_SPEED_IN_CENTIMETERS_PER_SECOND: Source =
                Source(27, "INPUT_SPEED_IN_CENTIMETERS_PER_SECOND")
            /**
             * Signed x component of the modeled stroke input pointer's velocity in centimeters per
             * second.
             */
            @JvmField
            public val INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND: Source =
                Source(28, "INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND")
            /**
             * Signed y component of the modeled stroke input pointer's velocity in centimeters per
             * second.
             */
            @JvmField
            public val INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND: Source =
                Source(29, "INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND")
            /**
             * Distance in centimeters traveled by the modeled stroke input pointer along the input
             * path from the start of the stroke.
             */
            @JvmField
            public val INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS: Source =
                Source(30, "INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS")
            /**
             * Distance in centimeters alonge the input path from the real portion of the modeled
             * stroke to this input. Zero for inputs before the predicted portion of the stroke.
             */
            @JvmField
            public val PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS: Source =
                Source(31, "PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS")
            /**
             * Absolute acceleration of the modeled stroke input pointer in centimeters per second
             * squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(32, "INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed x component of the acceleration of the modeled stroke input pointer in
             * centimeters per second squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(33, "INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed y component of the acceleration of the modeled stroke input pointer in
             * centimeters per second squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(34, "INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed component acceleration of the modeled stroke input pointer in the direction of
             * its velocity in centimeters per second squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(35, "INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Signed component of acceleration of the modeled stroke input pointer perpendicular to
             * its velocity, rotated 90 degrees in the direction from the positive x-axis towards
             * the positive y-axis, in centimeters per second squared.
             */
            @JvmField
            public val INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED: Source =
                Source(36, "INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED")
            /**
             * Distance from the current modeled input to the end of the stroke along the input
             * path, as a fraction of the current total length of the stroke. This value changes for
             * each input as inputs are added.
             */
            @JvmField
            public val DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH: Source =
                Source(37, "DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH")
        }
    }

    /** List of scalar tip properties that can be modified by a [BrushBehavior]. */
    public class Target
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate Target value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.Target." + name

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<Target>()

            internal fun fromInt(value: Int): Target =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid Target value: $value" }

            /**
             * Scales the brush-tip width, starting from the value calculated using
             * [BrushTip.scaleX] and [BrushTip.scaleY]. The final brush width is clamped to a
             * maximum of twice the base width. If multiple behaviors have one of these targets,
             * they stack multiplicatively.
             */
            @JvmField public val WIDTH_MULTIPLIER: Target = Target(0, "WIDTH_MULTIPLIER")
            /**
             * Scales the brush-tip height, starting from the value calculated using
             * [BrushTip.scaleX] and [BrushTip.scaleY]. The final brush height is clamped to a
             * maximum of twice the base height. If multiple behaviors have one of these targets,
             * they stack multiplicatively.
             */
            @JvmField public val HEIGHT_MULTIPLIER: Target = Target(1, "HEIGHT_MULTIPLIER")
            /** Convenience enumerator to target both [WIDTH_MULTIPLIER] and [HEIGHT_MULTIPLIER]. */
            @JvmField public val SIZE_MULTIPLIER: Target = Target(2, "SIZE_MULTIPLIER")
            /**
             * Adds the target modifier to [BrushTip.slant]. The final brush slant value is clamped
             * to [-π/2, π/2]. If multiple behaviors have this target, they stack additively.
             */
            @JvmField
            public val SLANT_OFFSET_IN_RADIANS: Target = Target(3, "SLANT_OFFSET_IN_RADIANS")
            /**
             * Adds the target modifier to [BrushTip.pinch]. The final brush pinch value is clamped
             * to [0, 1]. If multiple behaviors have this target, they stack additively.
             */
            @JvmField public val PINCH_OFFSET: Target = Target(4, "PINCH_OFFSET")
            /**
             * Adds the target modifier to [BrushTip.rotation]. The final brush rotation angle is
             * effectively normalized (mod 2π). If multiple behaviors have this target, they stack
             * additively.
             */
            @JvmField
            public val ROTATION_OFFSET_IN_RADIANS: Target = Target(5, "ROTATION_OFFSET_IN_RADIANS")
            /**
             * Adds the target modifier to [BrushTip.cornerRounding]. The final brush corner
             * rounding value is clamped to [0, 1]. If multiple behaviors have this target, they
             * stack additively.
             */
            @JvmField
            public val CORNER_ROUNDING_OFFSET: Target = Target(6, "CORNER_ROUNDING_OFFSET")
            /** Adds the target modifier times the brush size to the brush tip x position. */
            @JvmField
            public val POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE: Target =
                Target(7, "POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE")
            /** Adds the target modifier times the brush size to the brush tip y position . */
            @JvmField
            public val POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE: Target =
                Target(8, "POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE")
            /**
             * Moves the brush tip by the target modifier times the brush size in the direction of
             * the modeled stroke input's velocity (the opposite direction if the value is
             * negative).
             */
            @JvmField
            public val POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE: Target =
                Target(9, "POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE")
            /**
             * Moves the brush tip by the target modifier times the brush size perpendicular to the
             * modeled stroke input's velocity, rotated 90 degrees in the direction from the
             * positive x-axis to the positive y-axis.
             */
            @JvmField
            public val POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE: Target =
                Target(10, "POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE")
            /**
             * Adds the target modifier to the initial texture animation progress value of the
             * current particle (which is relevant only for strokes with an animated texture). The
             * final progress offset is not clamped, but is effectively normalized (mod 1). If
             * multiple behaviors have this target, they stack additively.
             */
            @JvmField
            public val TEXTURE_ANIMATION_PROGRESS_OFFSET: Target =
                Target(11, "TEXTURE_ANIMATION_PROGRESS_OFFSET")

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
            @JvmField public val HUE_OFFSET_IN_RADIANS: Target = Target(12, "HUE_OFFSET_IN_RADIANS")
            /**
             * Scales the saturation of the base brush color. If multiple behaviors have one of
             * these targets, they stack multiplicatively. The final saturation multiplier is
             * clamped to [0, 2].
             */
            @JvmField public val SATURATION_MULTIPLIER: Target = Target(13, "SATURATION_MULTIPLIER")
            /**
             * Target the luminosity of the color. An offset of +/-100% corresponds to changing the
             * luminosity by up to +/-100%.
             */
            @JvmField public val LUMINOSITY: Target = Target(14, "LUMINOSITY")
            /**
             * Scales the opacity of the base brush color. If multiple behaviors have one of these
             * targets, they stack multiplicatively. The final opacity multiplier is clamped to
             * [0, 2].
             */
            @JvmField public val OPACITY_MULTIPLIER: Target = Target(15, "OPACITY_MULTIPLIER")
        }
    }

    /** List of vector tip properties that can be modified by a [BrushBehavior]. */
    public class PolarTarget
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate PolarTarget value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.PolarTarget." + name

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is PolarTarget) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<PolarTarget>()

            internal fun fromInt(value: Int): PolarTarget =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid PolarTarget value: $value" }

            /**
             * Adds the vector to the brush tip's absolute x/y position in stroke space, where the
             * angle input is measured in radians and the magnitude input is measured in units equal
             * to the brush size. An angle of zero indicates an offset in the direction of the
             * positive X-axis in stroke space; an angle of π/2 indicates the direction of the
             * positive Y-axis in stroke space.
             */
            @JvmField
            public val POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE:
                PolarTarget =
                PolarTarget(0, "POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE")

            /**
             * Adds the vector to the brush tip's forward/lateral position relative to the current
             * direction of input travel, where the angle input is measured in radians and the
             * magnitude input is measured in units equal to the brush size. An angle of zero
             * indicates a forward offset in the current direction of input travel, while an angle
             * of π indicates a backwards offset. Meanwhile, if the X- and Y-axes of stroke space
             * were rotated so that the positive X-axis points in the direction of stroke travel,
             * then an angle of π/2 would indicate a lateral offset towards the positive Y-axis, and
             * an angle of -π/2 would indicate a lateral offset towards the negative Y-axis.
             */
            @JvmField
            public val POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE:
                PolarTarget =
                PolarTarget(1, "POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE")
        }
    }

    /**
     * The desired behavior when an input value is outside the range defined by
     * [sourceValueRangeStart] and [sourceValueRangeEnd].
     */
    public class OutOfRange
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate OutOfRange value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.OutOfRange." + name

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<OutOfRange>()

            internal fun fromInt(value: Int): OutOfRange =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid OutOfRange value: $value" }

            /** Values outside the range will be clamped to not exceed the bounds. */
            @JvmField public val CLAMP: OutOfRange = OutOfRange(0, "CLAMP")

            /**
             * Values will be shifted by an integer multiple of the range size so that they fall
             * within the bounds.
             *
             * In this case, the range will be treated as a half-open interval, with a value exactly
             * at [sourceValueRangeEnd] being treated as though it was [sourceValueRangeStart].
             */
            @JvmField public val REPEAT: OutOfRange = OutOfRange(1, "REPEAT")
            /**
             * Similar to [Repeat], but every other repetition of the bounds will be mirrored, as
             * though the two elements [sourceValueRangeStart] and [sourceValueRangeEnd] were
             * swapped. This means the range does not need to be treated as a half-open interval
             * like in the case of [Repeat].
             */
            @JvmField public val MIRROR: OutOfRange = OutOfRange(2, "MIRROR")
        }
    }

    /** List of input properties that might not be reported by inputs. */
    public class OptionalInputProperty
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate OptionalInputProperty value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.OptionalInputProperty.$name"

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<OptionalInputProperty>()

            internal fun fromInt(value: Int): OptionalInputProperty =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) {
                    "Invalid OptionalInputProperty value: $value"
                }

            @JvmField
            public val PRESSURE: OptionalInputProperty = OptionalInputProperty(0, "PRESSURE")
            @JvmField public val TILT: OptionalInputProperty = OptionalInputProperty(1, "TILT")
            @JvmField
            public val ORIENTATION: OptionalInputProperty = OptionalInputProperty(2, "ORIENTATION")
            /** Tilt-x and tilt-y require both tilt and orientation to be reported. */
            @JvmField
            public val TILT_X_AND_Y: OptionalInputProperty =
                OptionalInputProperty(3, "TILT_X_AND_Y")
        }
    }

    /** A binary operation for combining two values in a [BinaryOpNode]. */
    public class BinaryOp
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate BinaryOp value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.BinaryOp." + name

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<BinaryOp>()

            internal fun fromInt(value: Int): BinaryOp =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid BinaryOp value: $value" }

            /** Evaluates to the product of the two input values, or null if either is null. */
            @JvmField public val PRODUCT: BinaryOp = BinaryOp(0, "PRODUCT")
            /** Evaluates to the sum of the two input values, or null if either is null. */
            @JvmField public val SUM: BinaryOp = BinaryOp(1, "SUM")
        }
    }

    /** Dimensions/units for measuring the [dampingGap] field of a [DampingNode] */
    public class DampingSource
    internal constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate DampingSource value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.DampingSource.$name"

        public companion object {

            private val VALUE_TO_INSTANCE = MutableIntObjectMap<DampingSource>()

            internal fun fromInt(value: Int): DampingSource =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid DampingSource value: $value" }

            /**
             * Value damping occurs over distance traveled by the input pointer, and the
             * [dampingGap] is measured in centimeters. If the input data does not indicate the
             * relationship between stroke units and physical units (e.g. as may be the case for
             * programmatically-generated inputs), then no damping will be performed (i.e. the
             * [dampingGap] will be treated as zero).
             */
            @JvmField
            public val DISTANCE_IN_CENTIMETERS: DampingSource =
                DampingSource(0, "DISTANCE_IN_CENTIMETERS")
            /**
             * Value damping occurs over distance traveled by the input pointer, and the
             * [dampingGap] is measured in multiples of the brush size.
             */
            @JvmField
            public val DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE: DampingSource =
                DampingSource(1, "DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE")
            /** Value damping occurs over time, and the [dampingGap] is measured in seconds. */
            @JvmField
            public val TIME_IN_SECONDS: DampingSource = DampingSource(2, "TIME_IN_SECONDS")
        }
    }

    /** Interpolation functions for use in an [InterpolationNode]. */
    public class Interpolation
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate Interpolation value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BrushBehavior.Interpolation.$name"

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<Interpolation>()

            internal fun fromInt(value: Int): Interpolation =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid Interpolation value: $value" }

            /**
             * Linear interpolation. Evaluates to the [InterpolationNode.startInput] value when the
             * [InterpolationNode.paramInput] value is 0, and to the [InterpolationNode.endInput]
             * value when the [InterpolationNode.paramInput] value is 1.
             */
            @JvmField public val LERP: Interpolation = Interpolation(0, "LERP")
            /**
             * Inverse linear interpolation. Evaluates to 0 when the [InterpolationNode.paramInput]
             * value is equal to the [InterpolationNode.startInput] value, and to 1 when the
             * parameter is equal to the [InterpolationNode.endInput] value. Evaluates to null when
             * the [InterpolationNode.startInput] and [InterpolationNode.endInput] values are equal.
             */
            @JvmField public val INVERSE_LERP: Interpolation = Interpolation(1, "INVERSE_LERP")
        }
    }

    /**
     * Represents one node in a [BrushBehavior]'s expression graph. [Node] objects are immutable and
     * their inputs must be chosen at construction time; therefore, they can only ever be assembled
     * into an acyclic graph.
     */
    public abstract class Node
    internal constructor(
        internal val nativePointer: Long,
        /** The ordered list of inputs that this node directly depends on. */
        public val inputs: List<ValueNode>,
    ) {

        // NOMUTANTS -- Not tested post garbage collection.
        protected fun finalize() {
            // Note that the instance becomes finalizable at the conclusion of the Object
            // constructor,
            // which in Kotlin is always before any non-default field initialization has been done
            // by a
            // derived class constructor.
            if (nativePointer == 0L) return
            BrushBehaviorNodeNative.free(nativePointer)
        }

        public companion object {
            public fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): Node =
                when (BrushBehaviorNodeNative.getNodeType(unownedNativePointer)) {
                    0 -> SourceNode.wrapNative(unownedNativePointer)
                    1 -> ConstantNode.wrapNative(unownedNativePointer)
                    2 -> NoiseNode.wrapNative(unownedNativePointer)
                    3 -> FallbackFilterNode.wrapNative(unownedNativePointer, inputStack)
                    4 -> ToolTypeFilterNode.wrapNative(unownedNativePointer, inputStack)
                    5 -> DampingNode.wrapNative(unownedNativePointer, inputStack)
                    6 -> ResponseNode.wrapNative(unownedNativePointer, inputStack)
                    7 -> BinaryOpNode.wrapNative(unownedNativePointer, inputStack)
                    8 -> InterpolationNode.wrapNative(unownedNativePointer, inputStack)
                    9 -> TargetNode.wrapNative(unownedNativePointer, inputStack)
                    10 -> PolarTargetNode.wrapNative(unownedNativePointer, inputStack)
                    else ->
                        throw IllegalArgumentException(
                            "Unknown node type: ${BrushBehaviorNodeNative.getNodeType(unownedNativePointer)}"
                        )
                }
        }
    }

    /**
     * A [ValueNode] is a non-terminal node in the graph; it produces a value to be consumed as an
     * input by other [Node]s, and may itself depend on zero or more inputs.
     */
    public abstract class ValueNode
    internal constructor(nativePointer: Long, inputs: List<ValueNode>) :
        Node(nativePointer, inputs)

    /** A [ValueNode] that gets data from the stroke input batch. */
    public class SourceNode private constructor(nativePointer: Long) :
        ValueNode(nativePointer, emptyList()) {

        /**
         * Creates a [SourceNode] that gets data from the stroke inputs.
         *
         * @param source the property of the data to get values from
         * @param sourceValueRangeStart the start of the range of values that the source can produce
         * @param sourceValueRangeEnd the end of the range of values that the source can produce
         * @param sourceOutOfRangeBehavior the behavior to use if the source produces a value
         *   outside the specified range
         */
        @JvmOverloads
        public constructor(
            source: Source,
            sourceValueRangeStart: Float,
            sourceValueRangeEnd: Float,
            sourceOutOfRangeBehavior: OutOfRange = OutOfRange.CLAMP,
        ) : this(
            BrushBehaviorNodeNative.createSource(
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

        public val source: Source = BrushBehaviorNodeNative.getSource(nativePointer)

        public val sourceValueRangeStart: Float
            get() = BrushBehaviorNodeNative.getSourceValueRangeStart(nativePointer)

        public val sourceValueRangeEnd: Float
            get() = BrushBehaviorNodeNative.getSourceValueRangeEnd(nativePointer)

        public val sourceOutOfRangeBehavior: OutOfRange =
            BrushBehaviorNodeNative.getSourceOutOfRangeBehavior(nativePointer)

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
    }

    /** A [ValueNode] that produces a constant output value. */
    public class ConstantNode private constructor(nativePointer: Long) :
        ValueNode(nativePointer, emptyList()) {

        /** Creates a [ConstantNode] that produces a constant output value. */
        public constructor(value: Float) : this(BrushBehaviorNodeNative.createConstant(value))

        internal companion object {
            internal fun wrapNative(unownedNativePointer: Long): ConstantNode =
                ConstantNode(unownedNativePointer)
        }

        public val value: Float
            get() = BrushBehaviorNodeNative.getConstantValue(nativePointer)

        override fun toString(): String = "ConstantNode($value)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is ConstantNode) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()
    }

    /** A [ValueNode] that produces a smooth random function. */
    public class NoiseNode private constructor(nativePointer: Long) :
        ValueNode(nativePointer, emptyList()) {

        /**
         * Creates a [NoiseNode] that produces a random variation.
         *
         * @param seed the seed for the random number generator
         * @param varyOver the source of the varying over which the random function is evaluated
         * @param basePeriod the base period of the random function
         */
        public constructor(
            seed: Int,
            varyOver: DampingSource,
            basePeriod: Float,
        ) : this(BrushBehaviorNodeNative.createNoise(seed, varyOver.value, basePeriod))

        internal companion object {
            internal fun wrapNative(unownedNativePointer: Long): NoiseNode =
                NoiseNode(unownedNativePointer)
        }

        public val seed: Int
            get() = BrushBehaviorNodeNative.getNoiseSeed(nativePointer)

        public val varyOver: DampingSource = BrushBehaviorNodeNative.getNoiseVaryOver(nativePointer)

        public val basePeriod: Float
            get() = BrushBehaviorNodeNative.getNoiseBasePeriod(nativePointer)

        override fun toString(): String =
            "NoiseNode($seed, ${varyOver.toSimpleString()}, $basePeriod)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is NoiseNode) return false
            return seed == other.seed &&
                varyOver == other.varyOver &&
                basePeriod == other.basePeriod
        }

        override fun hashCode(): Int {
            var result = seed.hashCode()
            result = 31 * result + varyOver.hashCode()
            result = 31 * result + basePeriod.hashCode()
            return result
        }
    }

    /**
     * A [ValueNode] for filtering out a branch of a behavior graph unless a particular stroke input
     * property is missing.
     */
    public class FallbackFilterNode
    private constructor(nativePointer: Long, public val input: ValueNode) :
        ValueNode(nativePointer, listOf(input)) {

        /**
         * Creates a [FallbackFilterNode] that filters out a branch of a behavior graph unless a
         * particular stroke input property is missing.
         *
         * @param isFallbackFor the input property that must be missing for this node to not be
         *   filtered
         * @param input input node whose value is filtered if the input proerty is present
         */
        public constructor(
            isFallbackFor: OptionalInputProperty,
            input: ValueNode,
        ) : this(BrushBehaviorNodeNative.createFallbackFilter(isFallbackFor.value), input)

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): FallbackFilterNode =
                FallbackFilterNode(unownedNativePointer, inputStack.removeLast())
        }

        public val isFallbackFor: OptionalInputProperty =
            BrushBehaviorNodeNative.getFallbackFilterIsFallbackFor(nativePointer)

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
    }

    /**
     * A [ValueNode] for filtering out a branch of a behavior graph unless this stroke's tool type
     * is in the specified set.
     */
    public class ToolTypeFilterNode
    private constructor(nativePointer: Long, public val input: ValueNode) :
        ValueNode(nativePointer, listOf(input)) {

        /**
         * Creates a [ToolTypeFilterNode] that filters out a branch of a behavior graph unless this
         * stroke's tool type is in the specified set.
         *
         * @param enabledToolTypes the set of tool types that should be enabled
         * @param input input node that produces the value filtered if the tool type is not enabled
         */
        public constructor(
            // The [enabledToolTypes] val below is a defensive copy of this parameter.
            enabledToolTypes: Set<InputToolType>,
            input: ValueNode,
        ) : this(
            BrushBehaviorNodeNative.createToolTypeFilter(
                mouseEnabled = enabledToolTypes.contains(InputToolType.MOUSE),
                touchEnabled = enabledToolTypes.contains(InputToolType.TOUCH),
                stylusEnabled = enabledToolTypes.contains(InputToolType.STYLUS),
                unknownEnabled = enabledToolTypes.contains(InputToolType.UNKNOWN),
            ),
            input,
        )

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): ToolTypeFilterNode =
                ToolTypeFilterNode(unownedNativePointer, input = inputStack.removeLast())
        }

        public val enabledToolTypes: Set<InputToolType> =
            unmodifiableSet(
                mutableSetOf<InputToolType>().apply {
                    if (BrushBehaviorNodeNative.getToolTypeFilterMouseEnabled(nativePointer)) {
                        add(InputToolType.MOUSE)
                    }
                    if (BrushBehaviorNodeNative.getToolTypeFilterTouchEnabled(nativePointer)) {
                        add(InputToolType.TOUCH)
                    }
                    if (BrushBehaviorNodeNative.getToolTypeFilterStylusEnabled(nativePointer)) {
                        add(InputToolType.STYLUS)
                    }
                    if (BrushBehaviorNodeNative.getToolTypeFilterUnknownEnabled(nativePointer)) {
                        add(InputToolType.UNKNOWN)
                    }
                }
            )

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
    }

    /**
     * A [ValueNode] that damps changes in an input value, causing the output value to slowly follow
     * changes in the input value over a specified time or distance.
     */
    public class DampingNode private constructor(nativePointer: Long, public val input: ValueNode) :
        ValueNode(nativePointer, listOf(input)) {

        /**
         * Creates a [DampingNode] that damps changes in an input value, causing the output value to
         * slowly follow changes in the input value over a specified time or distance.
         *
         * @param dampingSource the source of the damping
         * @param dampingGap the amount of damping to apply
         * @param input input node that produces the value to be modified by the damping
         */
        public constructor(
            dampingSource: DampingSource,
            dampingGap: Float,
            input: ValueNode,
        ) : this(BrushBehaviorNodeNative.createDamping(dampingSource.value, dampingGap), input)

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): DampingNode = DampingNode(unownedNativePointer, input = inputStack.removeLast())
        }

        public val dampingSource: DampingSource =
            BrushBehaviorNodeNative.getDampingSource(nativePointer)

        public val dampingGap: Float
            get() = BrushBehaviorNodeNative.getDampingGap(nativePointer)

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
    }

    /** A [ValueNode] that maps an input value through a response curve. */
    public class ResponseNode
    private constructor(
        nativePointer: Long,
        public val responseCurve: EasingFunction,
        public val input: ValueNode,
    ) : ValueNode(nativePointer, listOf(input)) {

        /**
         * Creates a [ResponseNode] that maps an input value through a response curve.
         *
         * @param responseCurve the response curve to apply to the input value
         * @param input input node that produces the value used to map through the response curve
         */
        public constructor(
            responseCurve: EasingFunction,
            input: ValueNode,
        ) : this(
            BrushBehaviorNodeNative.createResponse(responseCurve.nativePointer),
            responseCurve,
            input,
        )

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): ResponseNode =
                ResponseNode(
                    unownedNativePointer,
                    EasingFunction.wrapNative(
                        BrushBehaviorNodeNative.newCopyOfResponseEasingFunction(
                            unownedNativePointer
                        )
                    ),
                    inputStack.removeLast(),
                )
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
    }

    /** A [ValueNode] that combines two other values with a binary operation. */
    public class BinaryOpNode
    private constructor(
        nativePointer: Long,
        public val firstInput: ValueNode,
        public val secondInput: ValueNode,
    ) : ValueNode(nativePointer, listOf(firstInput, secondInput)) {

        /**
         * Creates a [BinaryOpNode] that combines two other values with a binary operation.
         *
         * @param operation the binary operation to perform
         * @param firstInput input node that produces the first value used in the binary operation
         * @param secondInput input node that produces the second value used in the binary operation
         */
        public constructor(
            operation: BinaryOp,
            firstInput: ValueNode,
            secondInput: ValueNode,
        ) : this(BrushBehaviorNodeNative.createBinaryOp(operation.value), firstInput, secondInput)

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): BinaryOpNode {
                // Inputs are in reverse order at the end of the stack.
                val secondInput = inputStack.removeLast()
                val firstInput = inputStack.removeLast()
                return BinaryOpNode(unownedNativePointer, firstInput, secondInput)
            }
        }

        public val operation: BinaryOp = BrushBehaviorNodeNative.getBinaryOperation(nativePointer)

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
    }

    /**
     * A [ValueNode] that interpolates between two inputs based on a parameter input. The specific
     * kind of interpolation performed depends on the [Interpolation] parameter.
     */
    public class InterpolationNode
    private constructor(
        nativePointer: Long,
        public val paramInput: ValueNode,
        public val startInput: ValueNode,
        public val endInput: ValueNode,
    ) : ValueNode(nativePointer, listOf(paramInput, startInput, endInput)) {

        /**
         * Creates an [InterpolationNode] that interpolates between two inputs based on a parameter
         * input. The specific kind of interpolation performed depends on the [Interpolation]
         * parameter.
         *
         * @param interpolation the kind of interpolation to perform
         * @param paramInput input node that produces the parameter value used to interpolate
         *   between the start and end inputs
         * @param startInput input node that produces the starting value for the interpolation
         * @param endInput input node that produces the ending value for the interpolation
         */
        public constructor(
            interpolation: Interpolation,
            paramInput: ValueNode,
            startInput: ValueNode,
            endInput: ValueNode,
        ) : this(
            BrushBehaviorNodeNative.createInterpolation(interpolation.value),
            paramInput,
            startInput,
            endInput,
        )

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): InterpolationNode {
                // Inputs are in reverse order at the end of the stack.
                val endInput = inputStack.removeLast()
                val startInput = inputStack.removeLast()
                val paramInput = inputStack.removeLast()
                return InterpolationNode(
                    unownedNativePointer,
                    paramInput = paramInput,
                    startInput = startInput,
                    endInput = endInput,
                )
            }
        }

        public val interpolation: Interpolation =
            BrushBehaviorNodeNative.getInterpolation(nativePointer)

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
    }

    /**
     * A [TerminalNode] is a terminal node in the graph; it does not produce a value and cannot be
     * used as an input to other [Node]s, but instead applies a modification to the brush tip state.
     * A [BrushBehavior] consists of a list of [TerminalNode]s and the various [ValueNode]s that
     * they transitively depend on.
     */
    public abstract class TerminalNode
    internal constructor(nativePointer: Long, inputs: List<ValueNode>) :
        Node(nativePointer, inputs) {}

    /** A [TerminalNode] that consumes a single input to affect a scalar brush tip property. */
    public class TargetNode private constructor(nativePointer: Long, public val input: ValueNode) :
        TerminalNode(nativePointer, listOf(input)) {

        /**
         * Creates a [TargetNode] that consumes a single input to affect a scalar brush tip
         * property.
         *
         * @param target the brush tip property to affect
         * @param targetModifierRangeStart start of the range of the input value that should affect
         *   the target property
         * @param targetModifierRangeEnd end of the range of the input value that should affect the
         *   target property
         * @param input input node that produces the value used to affect the target
         */
        public constructor(
            target: Target,
            targetModifierRangeStart: Float,
            targetModifierRangeEnd: Float,
            input: ValueNode,
        ) : this(
            BrushBehaviorNodeNative.createTarget(
                target.value,
                targetModifierRangeStart,
                targetModifierRangeEnd,
            ),
            input,
        )

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): TargetNode = TargetNode(unownedNativePointer, input = inputStack.removeLast())
        }

        public val target: Target = BrushBehaviorNodeNative.getTarget(nativePointer)

        public val targetModifierRangeStart: Float
            get() = BrushBehaviorNodeNative.getTargetModifierRangeStart(nativePointer)

        public val targetModifierRangeEnd: Float
            get() = BrushBehaviorNodeNative.getTargetModifierRangeEnd(nativePointer)

        override fun toString(): String =
            "TargetNode(${target.toSimpleString()}, $targetModifierRangeStart, " +
                "$targetModifierRangeEnd, $input)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is TargetNode) return false
            if (other === this) return true
            return target == other.target &&
                targetModifierRangeStart == other.targetModifierRangeStart &&
                targetModifierRangeEnd == other.targetModifierRangeEnd &&
                input == other.input
        }

        override fun hashCode(): Int {
            var result = target.hashCode()
            result = 31 * result + targetModifierRangeStart.hashCode()
            result = 31 * result + targetModifierRangeEnd.hashCode()
            result = 31 * result + input.hashCode()
            return result
        }
    }

    /**
     * A [TerminalNode] that consumes two inputs, an angle and a magnitude, to affect a vector brush
     * tip property.
     */
    public class PolarTargetNode
    private constructor(
        nativePointer: Long,
        public val angleInput: ValueNode,
        public val magnitudeInput: ValueNode,
    ) : TerminalNode(nativePointer, listOf(angleInput, magnitudeInput)) {

        /**
         * Creates a [PolarTargetNode] that consumes two inputs, an angle and a magnitude, to affect
         * a vector brush tip property.
         *
         * @param target vector brush tip property to affect
         * @param angleRangeStart start of the angle range for the target property
         * @param angleRangeEnd end of the angle range for the target property
         * @param angleInput input node that produces the value used to affect the angle of the
         *   target vector property
         * @param magnitudeRangeStart start of the magnitude range for the target property
         * @param magnitudeRangeEnd end of the magnitude range for the target property
         * @param magnitudeInput input node that produces the value used to affect the magnitude of
         *   the target vector property
         */
        public constructor(
            target: PolarTarget,
            angleRangeStart: Float,
            angleRangeEnd: Float,
            angleInput: ValueNode,
            magnitudeRangeStart: Float,
            magnitudeRangeEnd: Float,
            magnitudeInput: ValueNode,
        ) : this(
            BrushBehaviorNodeNative.createPolarTarget(
                target.value,
                angleRangeStart,
                angleRangeEnd,
                magnitudeRangeStart,
                magnitudeRangeEnd,
            ),
            angleInput,
            magnitudeInput,
        )

        internal companion object {
            internal fun wrapNative(
                unownedNativePointer: Long,
                inputStack: ArrayDeque<ValueNode>,
            ): PolarTargetNode {
                // Inputs are in reverse order at the end of the stack.
                val magnitudeInput = inputStack.removeLast()
                val angleInput = inputStack.removeLast()
                return PolarTargetNode(unownedNativePointer, angleInput, magnitudeInput)
            }
        }

        public val target: PolarTarget = BrushBehaviorNodeNative.getPolarTarget(nativePointer)

        public val angleRangeStart: Float
            get() = BrushBehaviorNodeNative.getPolarTargetAngleRangeStart(nativePointer)

        public val angleRangeEnd: Float
            get() = BrushBehaviorNodeNative.getPolarTargetAngleRangeEnd(nativePointer)

        public val magnitudeRangeStart: Float
            get() = BrushBehaviorNodeNative.getPolarTargetMagnitudeRangeStart(nativePointer)

        public val magnitudeRangeEnd: Float
            get() = BrushBehaviorNodeNative.getPolarTargetMagnitudeRangeEnd(nativePointer)

        override fun toString(): String =
            "PolarTargetNode(${target.toSimpleString()}, $angleRangeStart, $angleRangeEnd, " +
                "$angleInput, $magnitudeRangeStart, $magnitudeRangeEnd, $magnitudeInput)"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is PolarTargetNode) return false
            if (other === this) return true
            return target == other.target &&
                angleRangeStart == other.angleRangeStart &&
                angleRangeEnd == other.angleRangeEnd &&
                angleInput == other.angleInput &&
                magnitudeRangeStart == other.magnitudeRangeStart &&
                magnitudeRangeEnd == other.magnitudeRangeEnd &&
                magnitudeInput == other.magnitudeInput
        }

        override fun hashCode(): Int {
            var result = target.hashCode()
            result = 31 * result + angleRangeStart.hashCode()
            result = 31 * result + angleRangeEnd.hashCode()
            result = 31 * result + angleInput.hashCode()
            result = 31 * result + magnitudeRangeStart.hashCode()
            result = 31 * result + magnitudeRangeEnd.hashCode()
            result = 31 * result + magnitudeInput.hashCode()
            return result
        }
    }
}

/** Singleton wrapper for `BrushBehavior` native methods. */
@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object BrushBehaviorNative {
    init {
        NativeLoader.load()
    }

    fun createFromTerminalNodes(terminalNodes: List<BrushBehavior.TerminalNode>): Long {
        val orderedNodes = ArrayDeque<BrushBehavior.Node>()
        val stack = ArrayDeque<BrushBehavior.Node>(terminalNodes)
        while (!stack.isEmpty()) {
            stack.removeLast().let { node ->
                orderedNodes.addFirst(node)
                stack.addAll(node.inputs)
            }
        }
        return createFromOrderedNodes(orderedNodes.map { it.nativePointer }.toLongArray())
    }

    /** Creates a new native `BrushBehavior` with the given ordered nodes. */
    @UsedByNative external fun createFromOrderedNodes(orderdNodeNativePointers: LongArray): Long

    /** Release the underlying memory allocated in [createFromOrderedNodes]. */
    @UsedByNative external fun free(nativePointer: Long)

    /** Returns the number of `BrushBehavior::Node`s in the native `BrushBehavior`. */
    @UsedByNative external fun getNodeCount(nativePointer: Long): Int

    /**
     * Returns an unowned native pointer to a new, stack-allocated copy of the native
     * `BrushBehavior::Node` at the given index in the pointed-at `BrushBehavior`.
     */
    @UsedByNative external fun newCopyOfNode(nativePointer: Long, index: Int): Long
}

/**
 * Singleton wrapper for `BrushBehavior::Node` native methods.
 *
 * Note that even though Kotlin [BrushBehavior.Node] is an abstract class with several subtypes,
 * [BrushBehavior.Node.nativePointer] all wrap the _same_ native type (a specialization of
 * `std::variant`).
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object BrushBehaviorNodeNative {
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

    @UsedByNative external fun createConstant(value: Float): Long

    @UsedByNative external fun createNoise(seed: Int, varyOver: Int, basePeriod: Float): Long

    @UsedByNative external fun createFallbackFilter(isFallbackFor: Int): Long

    @UsedByNative
    external fun createToolTypeFilter(
        mouseEnabled: Boolean,
        touchEnabled: Boolean,
        stylusEnabled: Boolean,
        unknownEnabled: Boolean,
    ): Long

    @UsedByNative external fun createDamping(dampingSource: Int, dampingGap: Float): Long

    @UsedByNative external fun createResponse(easingFunctionNativePointer: Long): Long

    @UsedByNative external fun createBinaryOp(operation: Int): Long

    @UsedByNative external fun createInterpolation(interpolation: Int): Long

    @UsedByNative
    external fun createTarget(
        target: Int,
        targetModifierRangeStart: Float,
        targetModifierRangeEnd: Float,
    ): Long

    @UsedByNative
    external fun createPolarTarget(
        polarTarget: Int,
        angleRangeStart: Float,
        angleRangeEnd: Float,
        magnitudeRangeStart: Float,
        magnitudeRangeEnd: Float,
    ): Long

    @UsedByNative external fun free(nodeNativePointer: Long)

    @UsedByNative external fun getNodeType(nodeNativePointer: Long): Int

    // SourceNode accessors:

    fun getSource(nativePointer: Long): BrushBehavior.Source =
        BrushBehavior.Source.fromInt(getSourceInt(nativePointer))

    @UsedByNative private external fun getSourceInt(nativePointer: Long): Int

    @UsedByNative external fun getSourceValueRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getSourceValueRangeEnd(nativePointer: Long): Float

    fun getSourceOutOfRangeBehavior(nativePointer: Long): BrushBehavior.OutOfRange =
        BrushBehavior.OutOfRange.fromInt(getSourceOutOfRangeBehaviorInt(nativePointer))

    @UsedByNative private external fun getSourceOutOfRangeBehaviorInt(nativePointer: Long): Int

    // ConstantNode accessors:

    @UsedByNative external fun getConstantValue(nativePointer: Long): Float

    // NoiseNode accessors:

    @UsedByNative external fun getNoiseSeed(nativePointer: Long): Int

    fun getNoiseVaryOver(nativePointer: Long): BrushBehavior.DampingSource =
        BrushBehavior.DampingSource.fromInt(getNoiseVaryOverInt(nativePointer))

    @UsedByNative private external fun getNoiseVaryOverInt(nativePointer: Long): Int

    @UsedByNative external fun getNoiseBasePeriod(nativePointer: Long): Float

    // FallbackFilterNode accessors:

    fun getFallbackFilterIsFallbackFor(nativePointer: Long): BrushBehavior.OptionalInputProperty =
        BrushBehavior.OptionalInputProperty.fromInt(
            getFallbackFilterIsFallbackForInt(nativePointer)
        )

    @UsedByNative private external fun getFallbackFilterIsFallbackForInt(nativePointer: Long): Int

    // ToolTypeFilterNode accessors:

    @UsedByNative external fun getToolTypeFilterMouseEnabled(nativePointer: Long): Boolean

    @UsedByNative external fun getToolTypeFilterTouchEnabled(nativePointer: Long): Boolean

    @UsedByNative external fun getToolTypeFilterStylusEnabled(nativePointer: Long): Boolean

    @UsedByNative external fun getToolTypeFilterUnknownEnabled(nativePointer: Long): Boolean

    // DampingNode accessors:

    fun getDampingSource(nativePointer: Long): BrushBehavior.DampingSource =
        BrushBehavior.DampingSource.fromInt(getDampingSourceInt(nativePointer))

    @UsedByNative private external fun getDampingSourceInt(nativePointer: Long): Int

    @UsedByNative external fun getDampingGap(nativePointer: Long): Float

    // Getters for ResponseNode:

    @UsedByNative external fun newCopyOfResponseEasingFunction(nativePointer: Long): Long

    // BinaryOpNode accessors:

    fun getBinaryOperation(nativePointer: Long): BrushBehavior.BinaryOp =
        BrushBehavior.BinaryOp.fromInt(getBinaryOperationInt(nativePointer))

    @UsedByNative private external fun getBinaryOperationInt(nativePointer: Long): Int

    // InterpolationNode accessors:

    fun getInterpolation(nativePointer: Long): BrushBehavior.Interpolation =
        BrushBehavior.Interpolation.fromInt(getInterpolationInt(nativePointer))

    @UsedByNative private external fun getInterpolationInt(nativePointer: Long): Int

    // TargetNode accessors:

    fun getTarget(nativePointer: Long): BrushBehavior.Target =
        BrushBehavior.Target.fromInt(getTargetInt(nativePointer))

    @UsedByNative private external fun getTargetInt(nativePointer: Long): Int

    @UsedByNative external fun getTargetModifierRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getTargetModifierRangeEnd(nativePointer: Long): Float

    // PolarTargetNode accessors:

    fun getPolarTarget(nativePointer: Long): BrushBehavior.PolarTarget =
        BrushBehavior.PolarTarget.fromInt(getPolarTargetInt(nativePointer))

    @UsedByNative private external fun getPolarTargetInt(nativePointer: Long): Int

    @UsedByNative external fun getPolarTargetAngleRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getPolarTargetAngleRangeEnd(nativePointer: Long): Float

    @UsedByNative external fun getPolarTargetMagnitudeRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getPolarTargetMagnitudeRangeEnd(nativePointer: Long): Float
}
