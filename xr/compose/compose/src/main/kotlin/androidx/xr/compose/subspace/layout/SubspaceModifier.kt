/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.xr.compose.subspace.node.SubspaceLayoutNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * An ordered, immutable collection of [subspace modifier elements][SubspaceModifierNodeElement]
 * that decorate or add behavior to Subspace Compose elements.
 *
 * Based on [androidx.compose.ui.Modifier]
 */
public interface SubspaceModifier {

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each [SubspaceModifierNodeElement] from outside in.
     *
     * @param initial initial value for the accumulation.
     * @param operation function to apply to the current accumulated value and the next element.
     */
    public fun <R> foldIn(initial: R, operation: (R, SubspaceModifierNodeElement<Node>) -> R): R =
        initial

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each [SubspaceModifierNodeElement] from inside out.
     *
     * @param initial initial value for the accumulation.
     * @param operation function to apply to the next element and the current accumulated value.
     */
    public fun <R> foldOut(initial: R, operation: (SubspaceModifierNodeElement<Node>, R) -> R): R =
        initial

    /**
     * Returns `true` if [predicate] returns true for any [SubspaceModifierNodeElement] in this
     * [SubspaceModifier].
     *
     * @param predicate condition to evaluate for each element.
     */
    public fun any(predicate: (SubspaceModifierNodeElement<Node>) -> Boolean): Boolean = false

    /**
     * Returns `true` if [predicate] returns true for all [SubspaceModifierNodeElement]s in this
     * [SubspaceModifier] or if this [SubspaceModifier] contains no Elements.
     *
     * @param predicate condition to evaluate for each element.
     */
    public fun all(predicate: (SubspaceModifierNodeElement<Node>) -> Boolean): Boolean = true

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [SubspaceModifier] representing this modifier followed by [other] in sequence.
     *
     * @param other [SubspaceModifier] to concatenate to this one.
     */
    public infix fun then(other: SubspaceModifier): SubspaceModifier =
        if (other === SubspaceModifier) this else CombinedSubspaceModifier(this, other)

    /**
     * The longer-lived object that is created for each [SubspaceModifierNodeElement] applied to a
     * [SubspaceLayout]
     */
    public abstract class Node : DelegatableSubspaceNode {
        override val node: Node = this

        internal var parent: Node? = null
        internal var child: Node? = null
        internal var kindSet: Int = 0
        internal var aggregateChildKindSet: Int = 0.inv()
        internal var layoutNode: SubspaceLayoutNode? = null
        internal val coordinator: SubspaceLayoutModifierNodeCoordinator? =
            if (this is SubspaceLayoutModifierNode) {
                SubspaceLayoutModifierNodeCoordinator(this)
            } else {
                null
            }

        private var scope: CoroutineScope? = null

        /**
         * A [CoroutineScope] that can be used to launch tasks that should run while the node is
         * attached.
         *
         * The scope is accessible between [onAttach] and [onDetach] calls, and will be cancelled
         * after the node is detached (after [onDetach] returns).
         */
        public val coroutineScope: CoroutineScope
            get() =
                scope
                    ?: CoroutineScope(
                            requireOwner().coroutineContext +
                                Job(parent = requireOwner().coroutineContext[Job])
                        )
                        .also { scope = it }

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

            scope?.let {
                it.cancel("SubspaceModifier.Node was detached")
                scope = null
            }
        }

        /** Called when the node is attached to a [SubspaceLayout] which is part of the UI tree. */
        public open fun onAttach() {}

        /**
         * Called when the node is not attached to a [SubspaceLayout] anymore. Note that the node
         * can be reattached again.
         */
        public open fun onDetach() {}

        /**
         * If this property returns `true`, then nodes will be automatically invalidated after the
         * modifier update completes.
         *
         * This is enabled by default, and provides a convenient mechanism to schedule invalidation
         * and apply changes made to the modifier. You may choose to set this to `false` if your
         * modifier has auto-invalidatable properties that do not frequently require invalidation to
         * improve performance by skipping unnecessary invalidation. If `shouldAutoInvalidate` is
         * set to `false`, you must call the appropriate invalidate functions manually when the
         * modifier is updated or else the updates may not be reflected in the UI appropriately.
         */
        @Suppress("GetterSetterNames")
        @get:Suppress("GetterSetterNames")
        public open val shouldAutoInvalidate: Boolean = true
    }

    /**
     * The companion object `SubspaceModifier` is the empty, default, or starter [SubspaceModifier]
     * that contains no [SubspaceModifierNodeElements][SubspaceModifierNodeElement].
     */
    public companion object : SubspaceModifier {

        public infix fun then(other: SubspaceModifierNodeElement<Node>): SubspaceModifier = other

        override fun toString(): String = "SubspaceModifier"
    }
}

/**
 * A node in a [SubspaceModifier] chain. A CombinedSubspaceModifier always contains at least two
 * elements; a SubspaceModifier [outer] that wraps around the SubspaceModifier [inner].
 */
internal class CombinedSubspaceModifier(
    internal val outer: SubspaceModifier,
    internal val inner: SubspaceModifier,
) : SubspaceModifier {
    override fun <R> foldIn(
        initial: R,
        operation: (R, SubspaceModifierNodeElement<SubspaceModifier.Node>) -> R,
    ): R = inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(
        initial: R,
        operation: (SubspaceModifierNodeElement<SubspaceModifier.Node>, R) -> R,
    ): R = outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(
        predicate: (SubspaceModifierNodeElement<SubspaceModifier.Node>) -> Boolean
    ): Boolean = outer.any(predicate) || inner.any(predicate)

    override fun all(
        predicate: (SubspaceModifierNodeElement<SubspaceModifier.Node>) -> Boolean
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
