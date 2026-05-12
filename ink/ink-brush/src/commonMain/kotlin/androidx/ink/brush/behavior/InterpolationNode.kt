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
import kotlin.jvm.JvmField

/**
 * A [ValueNode] that interpolates between two inputs based on a parameter input. The specific kind
 * of interpolation performed depends on the [Interpolation] parameter.
 */
public class InterpolationNode
private constructor(
    nativeAlloc: () -> Long,
    /**
     * The input node that produces the parameter value used to interpolate between the start and
     * end inputs.
     */
    public val paramInput: ValueNode,
    /** The input node that produces the starting value for the interpolation. */
    public val startInput: ValueNode,
    /** The input node that produces the ending value for the interpolation. */
    public val endInput: ValueNode,
) : ValueNode(nativeAlloc, listOf(paramInput, startInput, endInput)) {

    /**
     * Creates an [InterpolationNode] that interpolates between two inputs based on a parameter
     * input. The specific kind of interpolation performed depends on the [Interpolation] parameter.
     *
     * @param interpolation the kind of interpolation to perform
     * @param paramInput input node that produces the parameter value used to interpolate between
     *   the start and end inputs
     * @param startInput input node that produces the starting value for the interpolation
     * @param endInput input node that produces the ending value for the interpolation
     */
    public constructor(
        interpolation: Interpolation,
        paramInput: ValueNode,
        startInput: ValueNode,
        endInput: ValueNode,
    ) : this(
        { InterpolationNodeNative.create(interpolation.value) },
        paramInput,
        startInput,
        endInput,
    )

    internal companion object {
        internal fun wrapNative(
            nativeAlloc: () -> Long,
            inputStack: ArrayDeque<ValueNode>,
        ): InterpolationNode {
            // Inputs are in reverse order at the end of the stack.
            val endInput = inputStack.removeLast()
            val startInput = inputStack.removeLast()
            val paramInput = inputStack.removeLast()
            return InterpolationNode(
                nativeAlloc,
                paramInput = paramInput,
                startInput = startInput,
                endInput = endInput,
            )
        }
    }

    /** The kind of interpolation to perform. */
    public val interpolation: Interpolation =
        InterpolationNode.Interpolation.fromInt(
            InterpolationNodeNative.getInterpolationInt(nativePointer)
        )

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

    /** Interpolation functions for use in an [InterpolationNode]. */
    public class Interpolation
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate Interpolation value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "Interpolation.$name"

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
}

/**
 * Singleton wrapper for `BrushBehavior::InterpolationNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
expect internal object InterpolationNodeNative {
    fun create(interpolation: Int): Long

    fun getInterpolationInt(nativePointer: Long): Int
}
