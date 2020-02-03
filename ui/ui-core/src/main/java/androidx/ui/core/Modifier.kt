/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.Stable

/**
 * An immutable chain of [modifier elements][Modifier.Element] for use with Composables.
 * A Composable that has a `Modifier` can be considered decorated or wrapped by that `Modifier`.
 *
 * Modifiers may be compared for equality provided that all of their [elements][Element] are
 * `object`s, `data class`es or otherwise implement [equals][Any.equals]. A correct implementation
 * of an [Element] must meet this requirement.
 *
 * Modifier elements may be combined using `+` Order is significant; modifier elements to the left
 * are applied before modifier elements to the right.
 *
 * Composables that accept a [Modifier] as a parameter to be applied to the whole component
 * represented by the composable function should name the parameter `modifier` and
 * assign the parameter a default value of [Modifier.None]. It should appear as the first
 * optional parameter in the parameter list; after all required parameters (except for trailing
 * lambda parameters) but before any other parameters with default values. Any default modifiers
 * desired by a composable function should be concatenated to the right or left of the `modifier`
 * parameter's value in the composable function's implementation, keeping [Modifier.None] as the
 * default parameter value. For example:
 *
 * @sample androidx.ui.core.samples.ModifierParameterSample
 *
 * The pattern above allows default modifiers to still be applied as part of the chain
 * if a caller also supplies unrelated modifiers.
 *
 * Composables that accept modifiers to be applied to a specific subcomponent `foo`
 * should name the parameter `fooModifier` and follow the same guidelines above for default values
 * and behavior. Subcomponent modifiers should be grouped together and follow the parent
 * composable's modifier. For example:
 *
 * @sample androidx.ui.core.samples.SubcomponentModifierSample
 */
@Stable
interface Modifier {

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value
     * and each element from outside in.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all
     * of the elements that appear after it. [foldIn] may be used to accumulate a value starting
     * from the parent or head of the modifier chain to the final wrapped child.
     */
    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value
     * and each element from inside out.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all
     * of the elements that appear after it. [foldOut] may be used to accumulate a value starting
     * from the child or tail of the modifier chain up to the parent or head of the chain.
     */
    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    /**
     * Wraps another [Modifier] with this one, returning the new chain.
     */
    @Deprecated(
        "use + instead",
        replaceWith = ReplaceWith("this + other")
    )
    infix fun wraps(other: Modifier): Modifier = this + other

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [Modifier] representing this modifier followed by [other] in sequence.
     */
    operator fun plus(other: Modifier): Modifier =
        if (other === None) this else foldOut(other, ::CombinedModifier)

    /**
     * An empty [Modifier] that contains no [elements][Element].
     * Suitable for use as a sentinel or default parameter.
     */
    object None : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override operator fun plus(other: Modifier): Modifier = other
        override fun toString() = "Modifier.None"
    }

    /**
     * A single element contained within a [Modifier] chain.
     */
    interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)
    }
}

/**
 * A node in a [Modifier] chain. A CombinedModifier always contains at least two elements;
 * a Modifier of one is always just the [Modifier.Element] itself, and a Modifier of zero is always
 * [Modifier.None].
 */
private class CombinedModifier(
    private val element: Modifier.Element,
    private val wrapped: Modifier
) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        wrapped.foldIn(operation(initial, element), operation)

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        operation(element, wrapped.foldOut(initial, operation))

    override fun equals(other: Any?): Boolean =
        other is CombinedModifier && element == other.element && wrapped == other.wrapped

    override fun hashCode(): Int = wrapped.hashCode() + 31 * element.hashCode()

    override fun toString() = "[" + foldIn("") { acc, element ->
        if (acc.isEmpty()) element.toString() else "$acc, $element"
    } + "]"
}
