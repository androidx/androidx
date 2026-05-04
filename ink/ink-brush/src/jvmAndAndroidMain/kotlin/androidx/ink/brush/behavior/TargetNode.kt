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

/** A [TerminalNode] that consumes a single input to affect a scalar brush tip property. */
public class TargetNode
private constructor(
    nativePointer: Long,
    /** The input node that produces the value used to affect the target. */
    public val input: ValueNode,
) : TerminalNode(nativePointer, listOf(input)) {

    /**
     * Creates a [TargetNode] that consumes a single input to affect a scalar brush tip property.
     *
     * @param target the brush tip property to affect
     * @param targetModifierRangeStart start of the range of the input value that should affect the
     *   target property
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
        TargetNodeNative.createTarget(
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

    /** The brush tip property to affect. */
    public val target: Target = TargetNodeNative.getTarget(nativePointer)

    /** The start of the range of the input value that should affect the target property. */
    public val targetModifierRangeStart: Float
        get() = TargetNodeNative.getTargetModifierRangeStart(nativePointer)

    /** The end of the range of the input value that should affect the target property. */
    public val targetModifierRangeEnd: Float
        get() = TargetNodeNative.getTargetModifierRangeEnd(nativePointer)

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

    /**
     * List of scalar tip properties that can be modified by a [androidx.ink.brush.BrushBehavior].
     */
    public class Target
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate Target value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "Target." + name

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<Target>()

            internal fun fromInt(value: Int): Target =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid Target value: $value" }

            /**
             * Scales the brush-tip width, starting from the value calculated using
             * [androidx.ink.brush.BrushTip.scaleX]. If multiple behaviors have this target, they
             * stack multiplicatively.
             */
            @JvmField public val WIDTH_MULTIPLIER: Target = Target(0, "WIDTH_MULTIPLIER")
            /**
             * Scales the brush-tip height, starting from the value calculated using
             * [androidx.ink.brush.BrushTip.scaleY]. If multiple behaviors have this target, they
             * stack multiplicatively.
             */
            @JvmField public val HEIGHT_MULTIPLIER: Target = Target(1, "HEIGHT_MULTIPLIER")
            /** Convenience enumerator to target both [WIDTH_MULTIPLIER] and [HEIGHT_MULTIPLIER]. */
            @JvmField public val SIZE_MULTIPLIER: Target = Target(2, "SIZE_MULTIPLIER")
            /**
             * Adds the target modifier to [androidx.ink.brush.BrushTip.slantDegrees]. The final
             * brush slant value is clamped to [-π/2, π/2]. If multiple behaviors have this target,
             * they stack additively.
             */
            @JvmField
            public val SLANT_OFFSET_IN_RADIANS: Target = Target(3, "SLANT_OFFSET_IN_RADIANS")
            /**
             * Adds the target modifier to [androidx.ink.brush.BrushTip.pinch]. The final brush
             * pinch value is clamped to [0, 1]. If multiple behaviors have this target, they stack
             * additively.
             */
            @JvmField public val PINCH_OFFSET: Target = Target(4, "PINCH_OFFSET")
            /**
             * Adds the target modifier to [androidx.ink.brush.BrushTip.rotationDegrees]. The final
             * brush rotation angle is effectively normalized (mod 2π). If multiple behaviors have
             * this target, they stack additively.
             */
            @JvmField
            public val ROTATION_OFFSET_IN_RADIANS: Target = Target(5, "ROTATION_OFFSET_IN_RADIANS")
            /**
             * Adds the target modifier to [androidx.ink.brush.BrushTip.cornerRounding]. The final
             * brush corner rounding value is clamped to [0, 1]. If multiple behaviors have this
             * target, they stack additively.
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
             * Shifts the luminosity of the base brush color. An offset of ±1.0 corresponds to
             * changing the luminosity by up to ±100%. If multiple behaviors have this target, they
             * stack additively. The final luminosity offset is clamped to [-1, 1].
             */
            @JvmField public val LUMINOSITY_OFFSET: Target = Target(14, "LUMINOSITY_OFFSET")
            /**
             * Scales the opacity of the base brush color. If multiple behaviors have one of these
             * targets, they stack multiplicatively. The final opacity multiplier is clamped to
             * [0, 2].
             */
            @JvmField public val OPACITY_MULTIPLIER: Target = Target(15, "OPACITY_MULTIPLIER")
        }
    }
}

/**
 * Singleton wrapper for `BrushBehavior::TargetNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
@UsedByNative
private object TargetNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun createTarget(
        target: Int,
        targetModifierRangeStart: Float,
        targetModifierRangeEnd: Float,
    ): Long

    fun getTarget(nativePointer: Long): TargetNode.Target =
        TargetNode.Target.fromInt(getTargetInt(nativePointer))

    @UsedByNative private external fun getTargetInt(nativePointer: Long): Int

    @UsedByNative external fun getTargetModifierRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getTargetModifierRangeEnd(nativePointer: Long): Float
}
