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

package androidx.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.NodeKind
import androidx.compose.ui.node.ObserverNodeOwnerScope
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

private val EmptyStackTraceElements = emptyArray<StackTraceElement>()

/**
 * Used in place of the standard Job cancellation pathway to avoid reflective
 * javaClass.simpleName lookups to build the exception message and stack trace collection.
 * Remove if these are changed in kotlinx.coroutines.
 */
private class ModifierNodeDetachedCancellationException : CancellationException(
    "The Modifier.Node was detached"
) {
    override fun fillInStackTrace(): Throwable {
        // Avoid null.clone() on Android <= 6.0 when accessing stackTrace
        stackTrace = EmptyStackTraceElements
        return this
    }
}

/**
 * An ordered, immutable collection of [modifier elements][Modifier.Element] that decorate or add
 * behavior to Compose UI elements. For example, backgrounds, padding and click event listeners
 * decorate or add behavior to rows, text or buttons.
 *
 * @sample androidx.compose.ui.samples.ModifierUsageSample
 *
 * Modifier implementations should offer a fluent factory extension function on [Modifier] for
 * creating combined modifiers by starting from existing modifiers:
 *
 * @sample androidx.compose.ui.samples.ModifierFactorySample
 *
 * Modifier elements may be combined using [then]. Order is significant; modifier elements that
 * appear first will be applied first.
 *
 * Composables that accept a [Modifier] as a parameter to be applied to the whole component
 * represented by the composable function should name the parameter `modifier` and
 * assign the parameter a default value of [Modifier]. It should appear as the first
 * optional parameter in the parameter list; after all required parameters (except for trailing
 * lambda parameters) but before any other parameters with default values. Any default modifiers
 * desired by a composable function should come after the `modifier` parameter's value in the
 * composable function's implementation, keeping [Modifier] as the default parameter value.
 * For example:
 *
 * @sample androidx.compose.ui.samples.ModifierParameterSample
 *
 * The pattern above allows default modifiers to still be applied as part of the chain
 * if a caller also supplies unrelated modifiers.
 *
 * Composables that accept modifiers to be applied to a specific subcomponent `foo`
 * should name the parameter `fooModifier` and follow the same guidelines above for default values
 * and behavior. Subcomponent modifiers should be grouped together and follow the parent
 * composable's modifier. For example:
 *
 * @sample androidx.compose.ui.samples.SubcomponentModifierSample
 */
@Suppress("ModifierFactoryExtensionFunction")
@Stable
@JvmDefaultWithCompatibility
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
     * Returns `true` if [predicate] returns true for any [Element] in this [Modifier].
     */
    fun any(predicate: (Element) -> Boolean): Boolean

    /**
     * Returns `true` if [predicate] returns true for all [Element]s in this [Modifier] or if
     * this [Modifier] contains no [Element]s.
     */
    fun all(predicate: (Element) -> Boolean): Boolean

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [Modifier] representing this modifier followed by [other] in sequence.
     */
    infix fun then(other: Modifier): Modifier =
        if (other === Modifier) this else CombinedModifier(this, other)

    /**
     * A single element contained within a [Modifier] chain.
     */
    @JvmDefaultWithCompatibility
    interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)

        override fun any(predicate: (Element) -> Boolean): Boolean = predicate(this)

        override fun all(predicate: (Element) -> Boolean): Boolean = predicate(this)
    }

    /**
     * The longer-lived object that is created for each [Modifier.Element] applied to a
     * [androidx.compose.ui.layout.Layout]. Most [Modifier.Node] implementations will have a
     * corresponding "Modifier Factory" extension method on Modifier that will allow them to be used
     * indirectly, without ever implementing a [Modifier.Node] subclass directly. In some cases it
     * may be useful to define a custom [Modifier.Node] subclass in order to efficiently implement
     * some collection of behaviors that requires maintaining state over time and over many
     * recompositions where the various provided Modifier factories are not sufficient.
     *
     * When a [Modifier] is set on a [androidx.compose.ui.layout.Layout], each [Modifier.Element]
     * contained in that linked list will result in a corresponding [Modifier.Node] instance in a
     * matching linked list of [Modifier.Node]s that the [androidx.compose.ui.layout.Layout] will
     * hold on to. As subsequent [Modifier] chains get set on the
     * [androidx.compose.ui.layout.Layout], the linked list of [Modifier.Node]s will be diffed and
     * updated as appropriate, even though the [Modifier] instance might be completely new. As a
     * result, the lifetime of a [Modifier.Node] is the intersection of the lifetime of the
     * [androidx.compose.ui.layout.Layout] that it lives on and a corresponding [Modifier.Element]
     * being present in the [androidx.compose.ui.layout.Layout]'s [Modifier].
     *
     * If one creates a subclass of [Modifier.Node], it is expected that it will implement one or
     * more interfaces that interact with the various Compose UI subsystems. To use the
     * [Modifier.Node] subclass, it is expected that it will be instantiated by adding a
     * [androidx.compose.ui.node.ModifierNodeElement] to a [Modifier] chain.
     *
     * @see androidx.compose.ui.node.ModifierNodeElement
     * @see androidx.compose.ui.node.CompositionLocalConsumerModifierNode
     * @see androidx.compose.ui.node.DelegatableNode
     * @see androidx.compose.ui.node.DelegatingNode
     * @see androidx.compose.ui.node.LayoutModifierNode
     * @see androidx.compose.ui.node.DrawModifierNode
     * @see androidx.compose.ui.node.SemanticsModifierNode
     * @see androidx.compose.ui.node.PointerInputModifierNode
     * @see androidx.compose.ui.modifier.ModifierLocalModifierNode
     * @see androidx.compose.ui.node.ParentDataModifierNode
     * @see androidx.compose.ui.node.LayoutAwareModifierNode
     * @see androidx.compose.ui.node.GlobalPositionAwareModifierNode
     * @see androidx.compose.ui.node.IntermediateLayoutModifierNode
     */
    abstract class Node : DelegatableNode {
        @Suppress("LeakingThis")
        final override var node: Node = this
            private set

        private var scope: CoroutineScope? = null

        /**
         * A [CoroutineScope] that can be used to launch tasks that should run while the node is
         * attached.
         *
         * The scope is accessible between [onAttach] and [onDetach] calls, and will be cancelled
         * after the node is detached (after [onDetach] returns).
         *
         * @sample androidx.compose.ui.samples.ModifierNodeCoroutineScopeSample
         *
         * @throws IllegalStateException If called while the node is not attached.
         */
        val coroutineScope: CoroutineScope
            get() = scope ?: CoroutineScope(
                requireOwner().coroutineContext +
                    Job(parent = requireOwner().coroutineContext[Job])
            ).also {
                scope = it
            }

        internal var kindSet: Int = 0

        // NOTE: We use an aggregate mask that or's all of the type masks of the children of the
        // chain so that we can quickly prune a subtree. This INCLUDES the kindSet of this node
        // as well. Initialize this to "every node" so that before it is set it doesn't
        // accidentally cause a truncated traversal.
        internal var aggregateChildKindSet: Int = 0.inv()
        internal var parent: Node? = null
        internal var child: Node? = null
        internal var ownerScope: ObserverNodeOwnerScope? = null
        internal var coordinator: NodeCoordinator? = null
            private set
        internal var insertedNodeAwaitingAttachForInvalidation = false
        internal var updatedNodeAwaitingAttachForInvalidation = false
        private var onAttachRunExpected = false
        private var onDetachRunExpected = false
        /**
         * Indicates that the node is attached to a [androidx.compose.ui.layout.Layout] which is
         * part of the UI tree.
         * This will get set to true right before [onAttach] is called, and set to false right
         * after [onDetach] is called.
         *
         * @see onAttach
         * @see onDetach
         */
        var isAttached: Boolean = false
            private set

        /**
         * If this property returns `true`, then nodes will be automatically invalidated after the
         * modifier update completes (For example, if the returned Node is a [DrawModifierNode], its
         * [DrawModifierNode.invalidateDraw] function will be invoked automatically as part of
         * auto invalidation).
         *
         * This is enabled by default, and provides a convenient mechanism to schedule invalidation
         * and apply changes made to the modifier. You may choose to set this to `false` if your
         * modifier has auto-invalidatable properties that do not frequently require invalidation to
         * improve performance by skipping unnecessary invalidation. If `autoInvalidate` is set to
         * `false`, you must call the appropriate invalidate functions manually when the modifier
         * is updated or else the updates may not be reflected in the UI appropriately.
         */
        @Suppress("GetterSetterNames")
        @get:Suppress("GetterSetterNames")
        open val shouldAutoInvalidate: Boolean
            get() = true

        internal open fun updateCoordinator(coordinator: NodeCoordinator?) {
            this.coordinator = coordinator
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun isKind(kind: NodeKind<*>) = kindSet and kind.mask != 0

        internal open fun markAsAttached() {
            checkPrecondition(!isAttached) { "node attached multiple times" }
            checkPrecondition(coordinator != null) {
                "attach invoked on a node without a coordinator"
            }
            isAttached = true
            onAttachRunExpected = true
        }

        internal open fun runAttachLifecycle() {
            checkPrecondition(isAttached) {
                "Must run markAsAttached() prior to runAttachLifecycle"
            }
            checkPrecondition(onAttachRunExpected) { "Must run runAttachLifecycle() only once " +
                "after markAsAttached()"
            }
            onAttachRunExpected = false
            onAttach()
            onDetachRunExpected = true
        }

        internal open fun runDetachLifecycle() {
            checkPrecondition(isAttached) { "node detached multiple times" }
            checkPrecondition(coordinator != null) {
                "detach invoked on a node without a coordinator"
            }
            checkPrecondition(onDetachRunExpected) {
                "Must run runDetachLifecycle() once after runAttachLifecycle() and before " +
                    "markAsDetached()"
            }
            onDetachRunExpected = false
            onDetach()
        }

        internal open fun markAsDetached() {
            checkPrecondition(isAttached) { "Cannot detach a node that is not attached" }
            checkPrecondition(!onAttachRunExpected) {
                "Must run runAttachLifecycle() before markAsDetached()"
            }
            checkPrecondition(!onDetachRunExpected) {
                "Must run runDetachLifecycle() before markAsDetached()"
            }
            isAttached = false

            scope?.let {
                it.cancel(ModifierNodeDetachedCancellationException())
                scope = null
            }
        }

        internal open fun reset() {
            checkPrecondition(isAttached) { "reset() called on an unattached node" }
            onReset()
        }

        /**
         * Called when the node is attached to a [androidx.compose.ui.layout.Layout] which is
         * part of the UI tree.
         * When called, `node` is guaranteed to be non-null. You can call sideEffect,
         * coroutineScope, etc.
         * This is not guaranteed to get called at a time where the rest of the Modifier.Nodes in
         * the hierarchy are "up to date". For instance, at the time of calling onAttach for this
         * node, another node may be in the tree that will be detached by the time Compose has
         * finished applying changes. As a result, if you need to guarantee that the state of the
         * tree is "final" for this round of changes, you should use the [sideEffect] API to
         * schedule the calculation to be done at that time.
         */
        open fun onAttach() {}

        /**
         * Called when the node is not attached to a [androidx.compose.ui.layout.Layout] which is
         * not a part of the UI tree anymore. Note that the node can be reattached again.
         *
         * This should be called right before the node gets removed from the list, so you should
         * still be able to traverse inside of this method. Ideally we would not allow you to
         * trigger side effects here.
         */
        open fun onDetach() {}

        /**
         * Called when the node is about to be moved to a pool of layouts ready to be reused.
         * For example it happens when the node is part of the item of LazyColumn after this item
         * is scrolled out of the viewport. This means this node could be in future reused for a
         * [androidx.compose.ui.layout.Layout] displaying a semantically different content when
         * the list will be populating a new item.
         *
         * Use this callback to reset some local item specific state, like "is my component focused".
         *
         * This callback is called while the node is attached. Right after this callback the node
         * will be detached and later reattached when reused.
         *
         * @sample androidx.compose.ui.samples.ModifierNodeResetSample
         */
        open fun onReset() {}

        /**
         * This can be called to register [effect] as a function to be executed after all of the
         * changes to the tree are applied.
         *
         * This API can only be called if the node [isAttached].
         */
        @ExperimentalComposeUiApi
        fun sideEffect(effect: () -> Unit) {
            requireOwner().registerOnEndApplyChangesListener(effect)
        }

        internal open fun setAsDelegateTo(owner: Node) {
            node = owner
        }
    }

    /**
     * The companion object `Modifier` is the empty, default, or starter [Modifier]
     * that contains no [elements][Element]. Use it to create a new [Modifier] using
     * modifier extension factory functions:
     *
     * @sample androidx.compose.ui.samples.ModifierUsageSample
     *
     * or as the default value for [Modifier] parameters:
     *
     * @sample androidx.compose.ui.samples.ModifierParameterSample
     */
    // The companion object implements `Modifier` so that it may be used as the start of a
    // modifier extension factory expression.
    companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override fun any(predicate: (Element) -> Boolean): Boolean = false
        override fun all(predicate: (Element) -> Boolean): Boolean = true
        override infix fun then(other: Modifier): Modifier = other
        override fun toString() = "Modifier"
    }
}

/**
 * A node in a [Modifier] chain. A CombinedModifier always contains at least two elements;
 * a Modifier [outer] that wraps around the Modifier [inner].
 */
class CombinedModifier(
    internal val outer: Modifier,
    internal val inner: Modifier
) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(predicate: (Modifier.Element) -> Boolean): Boolean =
        outer.any(predicate) || inner.any(predicate)

    override fun all(predicate: (Modifier.Element) -> Boolean): Boolean =
        outer.all(predicate) && inner.all(predicate)

    override fun equals(other: Any?): Boolean =
        other is CombinedModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = outer.hashCode() + 31 * inner.hashCode()

    override fun toString() = "[" + foldIn("") { acc, element ->
        if (acc.isEmpty()) element.toString() else "$acc, $element"
    } + "]"
}
