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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNodeCoordinator
import androidx.xr.compose.subspace.node.SubspaceModifierElement

/**
 * An ordered, immutable collection of [subspace modifier elements][SubspaceModifierElement] that
 * decorate or add behavior to Subspace Compose elements.
 *
 * Based on [androidx.compose.ui.Modifier]
 */
public interface SubspaceModifier {

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each SubspaceModifierElement from outside in.
     */
    public fun <R> foldIn(
        initial: R,
        operation: (R, SubspaceModifierElement<SubspaceModifier.Node>) -> R,
    ): R = initial

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each SubspaceModifierElement from inside out.
     */
    public fun <R> foldOut(
        initial: R,
        operation: (SubspaceModifierElement<SubspaceModifier.Node>, R) -> R,
    ): R = initial

    /**
     * Returns `true` if [predicate] returns true for any [SubspaceModifierElement] in this
     * [SubspaceModifier].
     */
    public fun any(
        predicate: (SubspaceModifierElement<SubspaceModifier.Node>) -> Boolean
    ): Boolean = false

    /**
     * Returns `true` if [predicate] returns true for all [SubspaceModifierElement]s in this
     * [SubspaceModifier] or if this [SubspaceModifier] contains no [Element]s.
     */
    public fun all(
        predicate: (SubspaceModifierElement<SubspaceModifier.Node>) -> Boolean
    ): Boolean = true

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [SubspaceModifier] representing this modifier followed by [other] in sequence.
     */
    public infix fun then(other: SubspaceModifier): SubspaceModifier =
        if (other === SubspaceModifier) this else CombinedSubspaceModifier(this, other)

    /**
     * The longer-lived object that is created for each [SubspaceModifierElement] applied to a
     * [SubspaceLayout]
     */
    public abstract class Node {
        internal var parent: Node? = null
        internal var child: Node? = null
        internal val coordinator: SubspaceLayoutModifierNodeCoordinator? = run {
            if (this is SubspaceLayoutModifierNode) {
                SubspaceLayoutModifierNodeCoordinator(this)
            } else {
                null
            }
        }

        /**
         * Indicates that the node is attached to a [SubspaceLayout] which is part of the UI tree.
         * This will get set to true right before [onAttach] is called, and set to false right after
         * [onDetach] is called.
         *
         * @see onAttach
         * @see onDetach
         */
        public var isAttached: Boolean = false
            private set

        internal open fun markAsAttached() {
            check(!isAttached) { "Cannot attach node that is already attached!" }
            isAttached = true
        }

        internal open fun markAsDetached() {
            check(isAttached) { "Cannot detach node that is not attached!" }
            isAttached = false
        }

        /** Called when the node is attached to a [SubspaceLayout] which is part of the UI tree. */
        public open fun onAttach() {}

        /**
         * Called when the node is not attached to a [SubspaceLayout] anymore. Note that the node
         * can be reattached again.
         */
        public open fun onDetach() {}
    }

    /**
     * The companion object `SubspaceModifier` is the empty, default, or starter [SubspaceModifier]
     * that contains no [SubspaceModifierElements][SubspaceModifierElement].
     */
    public companion object : SubspaceModifier {

        public infix fun then(
            other: SubspaceModifierElement<SubspaceModifier.Node>
        ): SubspaceModifier = other

        override fun toString(): String = "SubspaceModifier"
    }
}

/**
 * A node in a [SubspaceModifier] chain. A CombinedSubspaceModifier always contains at least two
 * elements; a SubspaceModifier [outer] that wraps around the SubspaceModifier [inner].
 */
public class CombinedSubspaceModifier(
    internal val outer: SubspaceModifier,
    internal val inner: SubspaceModifier,
) : SubspaceModifier {
    override fun <R> foldIn(
        initial: R,
        operation: (R, SubspaceModifierElement<SubspaceModifier.Node>) -> R,
    ): R = inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(
        initial: R,
        operation: (SubspaceModifierElement<SubspaceModifier.Node>, R) -> R,
    ): R = outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(
        predicate: (SubspaceModifierElement<SubspaceModifier.Node>) -> Boolean
    ): Boolean = outer.any(predicate) || inner.any(predicate)

    override fun all(
        predicate: (SubspaceModifierElement<SubspaceModifier.Node>) -> Boolean
    ): Boolean = outer.all(predicate) && inner.all(predicate)

    override fun equals(other: Any?): Boolean =
        other is CombinedSubspaceModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = outer.hashCode() + 31 * inner.hashCode()

    override fun toString(): String =
        "[" +
            foldIn("") { acc, element ->
                if (acc.isEmpty()) element.toString() else "$acc, $element"
            } +
            "]"
}

/**
 * Generates a lazy sequence that walks up the node tree to the root.
 *
 * If this node is the root, an empty sequence is returned.
 */
public fun SubspaceModifier.Node.traverseAncestors(): Sequence<SubspaceModifier.Node> {
    return generateSequence(seed = parent) { it.parent }
}

/** Generates a sequence with self and elements up the node tree to the root. */
public fun SubspaceModifier.Node.traverseSelfThenAncestors(): Sequence<SubspaceModifier.Node> =
    sequenceOf(this) + traverseAncestors()

/**
 * Generates a lazy sequence that walks down the node tree.
 *
 * If this node is a leaf node, an empty sequence is returned.
 */
public fun SubspaceModifier.Node.traverseDescendants(): Sequence<SubspaceModifier.Node> {
    return generateSequence(seed = child) { it.child }
}

/** Generates a sequence with self and elements down the node tree. */
public fun SubspaceModifier.Node.traverseSelfThenDescendants(): Sequence<SubspaceModifier.Node> =
    sequenceOf(this) + traverseDescendants()

/** Returns the first element of type [T] in the sequence, or `null` if none match. */
internal inline fun <reified T> Sequence<*>.findInstance(): T? = firstOrNull { it is T } as T?
