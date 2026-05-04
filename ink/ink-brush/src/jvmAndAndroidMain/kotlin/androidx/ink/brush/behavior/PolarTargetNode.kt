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

/**
 * A [TerminalNode] that consumes two inputs, an angle and a magnitude, to affect a vector brush tip
 * property.
 */
public class PolarTargetNode
private constructor(
    nativePointer: Long,
    /**
     * The input node that produces the value used to affect the angle of the target vector
     * property.
     */
    public val angleInput: ValueNode,
    /**
     * The input node that produces the value used to affect the magnitude of the target vector
     * property.
     */
    public val magnitudeInput: ValueNode,
) : TerminalNode(nativePointer, listOf(angleInput, magnitudeInput)) {

    /**
     * Creates a [PolarTargetNode] that consumes two inputs, an angle and a magnitude, to affect a
     * vector brush tip property.
     *
     * @param target vector brush tip property to affect
     * @param angleRangeStart start of the angle range for the target property
     * @param angleRangeEnd end of the angle range for the target property
     * @param angleInput input node that produces the value used to affect the angle of the target
     *   vector property
     * @param magnitudeRangeStart start of the magnitude range for the target property
     * @param magnitudeRangeEnd end of the magnitude range for the target property
     * @param magnitudeInput input node that produces the value used to affect the magnitude of the
     *   target vector property
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
        PolarTargetNodeNative.createPolarTarget(
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

    /** The vector brush tip property to affect. */
    public val target: PolarTarget = PolarTargetNodeNative.getPolarTarget(nativePointer)

    /** The start of the angle range for the target property. */
    public val angleRangeStart: Float
        get() = PolarTargetNodeNative.getPolarTargetAngleRangeStart(nativePointer)

    /** The end of the angle range for the target property. */
    public val angleRangeEnd: Float
        get() = PolarTargetNodeNative.getPolarTargetAngleRangeEnd(nativePointer)

    /** The start of the magnitude range for the target property. */
    public val magnitudeRangeStart: Float
        get() = PolarTargetNodeNative.getPolarTargetMagnitudeRangeStart(nativePointer)

    /** The end of the magnitude range for the target property. */
    public val magnitudeRangeEnd: Float
        get() = PolarTargetNodeNative.getPolarTargetMagnitudeRangeEnd(nativePointer)

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

    /**
     * List of vector tip properties that can be modified by a [androidx.ink.brush.BrushBehavior].
     */
    public class PolarTarget
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate PolarTarget value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "PolarTarget." + name

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
}

/**
 * Singleton wrapper for `BrushBehavior::PolarTargetNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
@UsedByNative
private object PolarTargetNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun createPolarTarget(
        polarTarget: Int,
        angleRangeStart: Float,
        angleRangeEnd: Float,
        magnitudeRangeStart: Float,
        magnitudeRangeEnd: Float,
    ): Long

    fun getPolarTarget(nativePointer: Long): PolarTargetNode.PolarTarget =
        PolarTargetNode.PolarTarget.fromInt(getPolarTargetInt(nativePointer))

    @UsedByNative private external fun getPolarTargetInt(nativePointer: Long): Int

    @UsedByNative external fun getPolarTargetAngleRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getPolarTargetAngleRangeEnd(nativePointer: Long): Float

    @UsedByNative external fun getPolarTargetMagnitudeRangeStart(nativePointer: Long): Float

    @UsedByNative external fun getPolarTargetMagnitudeRangeEnd(nativePointer: Long): Float
}
