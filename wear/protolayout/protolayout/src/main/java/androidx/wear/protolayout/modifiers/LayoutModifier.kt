/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.modifiers

import androidx.wear.protolayout.modifiers.LayoutModifier.Element
import java.util.Objects

/**
 * An ordered, immutable collection of [modifier elements][LayoutModifier.Element] that decorate or
 * add behavior to ProtoLayout layout elements. For example, backgrounds, padding and click actions.
 * When a single modifier is applied multiple times, the last one wins.
 *
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
interface LayoutModifier {
    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from left to right.
     *
     * [foldRight] may be used to accumulate a value starting from the head of the modifier chain to
     * the final modifier element.
     */
    fun <R> foldRight(initial: R, operation: (R, Element) -> R): R

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [LayoutModifier] representing this modifier followed by [other] in sequence.
     */
    infix fun then(other: LayoutModifier): LayoutModifier =
        if (other === LayoutModifier) this else CombinedLayoutModifier(this, other)

    /** A single element contained within a [LayoutModifier] chain. */
    interface Element : LayoutModifier {
        override fun <R> foldRight(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)
    }

    /**
     * The companion object `LayoutModifier` is the empty, default, or starter [LayoutModifier] that
     * contains no [elements][Element]. Use it to create a new [LayoutModifier] using modifier
     * extension factory functions.
     */
    companion object : LayoutModifier {
        @Suppress("MissingJvmstatic")
        override fun <R> foldRight(initial: R, operation: (R, Element) -> R): R = initial

        @Suppress("MissingJvmstatic")
        override infix fun then(other: LayoutModifier): LayoutModifier = other

        @Suppress("MissingJvmstatic") override fun toString(): String = "LayoutModifier"
    }
}

/**
 * A node in a [LayoutModifier] chain. A [CombinedLayoutModifier] always contains at least two
 * elements.
 */
internal class CombinedLayoutModifier(
    private val left: LayoutModifier,
    private val right: LayoutModifier
) : LayoutModifier {
    override fun <R> foldRight(initial: R, operation: (R, Element) -> R): R =
        right.foldRight(left.foldRight(initial, operation), operation)

    override fun equals(other: Any?): Boolean =
        other is CombinedLayoutModifier && left == other.left && right == other.right

    override fun hashCode(): Int = Objects.hash(left, right)

    override fun toString(): String =
        "[" +
            foldRight("") { acc, element ->
                if (acc.isEmpty()) element.toString() else "$acc, $element"
            } +
            "]"
}
