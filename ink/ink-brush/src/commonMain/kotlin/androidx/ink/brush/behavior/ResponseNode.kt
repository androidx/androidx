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

/** A [ValueNode] that maps an input value through a response curve. */
public class ResponseNode
private constructor(
    nativeAlloc: () -> Long,
    /** The input node that produces the value used to map through the response curve. */
    public val input: ValueNode,
    /** The response curve to apply to the input value. */
    responseCurve: EasingFunction? = null,
) : ValueNode(nativeAlloc, listOf(input)) {

    public val responseCurve: EasingFunction =
        // If this was constructed in Kotlin, use the passed in EasingFunction.
        responseCurve
            // Otherwise, construct it from the native ResponseNode.
            ?: EasingFunction.copyAndWrapNative(
                ResponseNodeNative.getResponseCurvePointer(nativePointer)
            )

    /**
     * Creates a [ResponseNode] that maps an input value through a response curve.
     *
     * @param responseCurve the response curve to apply to the input value
     * @param input input node that produces the value used to map through the response curve
     */
    public constructor(
        responseCurve: EasingFunction,
        input: ValueNode,
    ) : this({ ResponseNodeNative.create(responseCurve.nativePointer) }, input, responseCurve)

    internal companion object {
        internal fun wrapNative(nativeAlloc: () -> Long, inputStack: ArrayDeque<ValueNode>) =
            ResponseNode(nativeAlloc, inputStack.removeLast())
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

/**
 * Singleton wrapper for `BrushBehavior::ResponseNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
expect internal object ResponseNodeNative {
    fun create(easingFunctionNativePointer: Long): Long

    fun getResponseCurvePointer(nativePointer: Long): Long
}
