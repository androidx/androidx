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

/** A [ValueNode] that produces a constant output value. */
public class ConstantNode private constructor(nativeAlloc: () -> Long) :
    ValueNode(nativeAlloc, emptyList()) {

    /** Creates a [ConstantNode] that produces a constant output value. */
    public constructor(value: Float) : this({ ConstantNodeNative.create(value) })

    internal companion object {
        internal fun wrapNative(nativeAlloc: () -> Long) = ConstantNode(nativeAlloc)
    }

    /** The constant value produced by this node. */
    public val value: Float
        get() = ConstantNodeNative.getValue(nativePointer)

    override fun toString(): String = "ConstantNode($value)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ConstantNode) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

/**
 * Singleton wrapper for `BrushBehavior::ConstantNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
expect internal object ConstantNodeNative {
    fun create(value: Float): Long

    fun getValue(nativePointer: Long): Float
}
