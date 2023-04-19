/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.Modifier
import org.jetbrains.annotations.TestOnly

/**
 * A [Modifier.Node] which is able to delegate work to other [Modifier.Node] instances.
 *
 * This can be useful to compose multiple node implementations into one.
 *
 * @sample androidx.compose.ui.samples.DelegatedNodeSampleExplicit
 * @sample androidx.compose.ui.samples.DelegatedNodeSampleImplicit
 * @sample androidx.compose.ui.samples.LazyDelegationExample
 * @sample androidx.compose.ui.samples.ConditionalDelegationExample
 * @sample androidx.compose.ui.samples.DelegateInAttachSample
 *
 * @see DelegatingNode
 */
abstract class DelegatingNode : Modifier.Node() {

    /**
     * This is the kindSet of the node if it had no delegates. This will never change, but kindSet
     * might, so we cache this value to be able to more efficiently recalculate the kindSet
     */
    @Suppress("LeakingThis")
    internal val selfKindSet: Int = calculateNodeKindSetFrom(this)
    override fun updateCoordinator(coordinator: NodeCoordinator?) {
        super.updateCoordinator(coordinator)
        forEachImmediateDelegate {
            it.updateCoordinator(coordinator)
        }
    }

    internal var delegate: Modifier.Node? = null

    @TestOnly
    internal fun <T : DelegatableNode> delegateUnprotected(delegatableNode: T): T =
        delegate(delegatableNode)
    @TestOnly
    internal fun undelegateUnprotected(instance: DelegatableNode) = undelegate(instance)

    /**
     * In order to properly delegate work to another [Modifier.Node], the delegated instance must
     * be created and returned inside of a [delegate] call. Doing this will
     * ensure that the created node instance follows all of the right lifecycles and is properly
     * discoverable in this position of the node tree.
     *
     * By using [delegate], the [delegatableNode] parameter is returned from this function for
     * convenience.
     *
     * This method can be called from within an `init` block, however the returned delegated node
     * will not be attached until the delegating node is attached. If [delegate] is called after
     * the delegating node is already attached, the returned delegated node will be attached.
     */
    protected fun <T : DelegatableNode> delegate(delegatableNode: T): T {
        val delegateNode = delegatableNode.node
        val isAlreadyDelegated = delegateNode !== delegatableNode
        if (isAlreadyDelegated) {
            val delegator = (delegatableNode as? Modifier.Node)?.parent
            val isDelegatedToThisNode = delegateNode === node && delegator == this
            if (isDelegatedToThisNode) {
                // nothing left to do
                return delegatableNode
            } else {
                error("Cannot delegate to an already delegated node")
            }
        }
        check(!delegateNode.isAttached) {
            "Cannot delegate to an already attached node"
        }
        // this could be a delegate itself, so we make sure to setAsDelegateTo(node) instead of
        // setAsDelegateTo(this).
        delegateNode.setAsDelegateTo(node)
        val beforeKindSet = kindSet
        // need to include the delegate's delegates in the calculation
        val delegatedKindSet = calculateNodeKindSetFromIncludingDelegates(delegateNode)
        delegateNode.kindSet = delegatedKindSet
        validateDelegateKindSet(delegatedKindSet, delegateNode)

        // We store the delegates of a node as a singly-linked-list, with the "head" as `delegate`
        // and the next pointer as `child`.
        delegateNode.child = delegate
        delegate = delegateNode

        // for a delegate, parent always points to the node which delegated to it
        delegateNode.parent = this
        updateNodeKindSet(kindSet or delegatedKindSet, recalculateOwner = false)

        if (isAttached) {
            if (Nodes.Layout in delegatedKindSet && Nodes.Layout !in beforeKindSet) {
                // We delegated to a layout modifier. In this case, we need to ensure that a new
                // NodeCoordinator gets created for this node
                val chain = requireLayoutNode().nodes
                node.updateCoordinator(null)
                chain.syncCoordinators()
            } else {
                updateCoordinator(coordinator)
            }
            delegateNode.attach()
            autoInvalidateInsertedNode(delegateNode)
        }
        return delegatableNode
    }

    /**
     * This function expects a node which was passed in to [delegate] for this node, and is
     * currently being delegated to to be passed in as [instance]. After this function returns, the
     * node will no longer be attached, and will not be an active delegate of this node.
     *
     * If [instance] is not an active delegate of this node, this function will throw an
     * [IllegalStateException].
     */
    protected fun undelegate(instance: DelegatableNode) {
        var prev: Modifier.Node? = null
        var it: Modifier.Node? = delegate
        var found = false
        while (it != null) {
            if (it === instance) {
                // remove from delegate chain
                if (it.isAttached) {
                    autoInvalidateRemovedNode(it)
                    it.detach()
                }
                it.setAsDelegateTo(it) // sets "node" back to itself
                it.aggregateChildKindSet = 0
                if (prev == null) {
                    this.delegate = it.child
                } else {
                    prev.child = it.child
                }
                it.child = null
                it.parent = null
                found = true
                break
            }
            prev = it
            it = it.child
        }
        if (found) {
            val beforeKindSet = kindSet
            val afterKindSet = calculateNodeKindSetFromIncludingDelegates(this)
            updateNodeKindSet(afterKindSet, recalculateOwner = true)

            if (isAttached && Nodes.Layout in beforeKindSet && Nodes.Layout !in afterKindSet) {
                // the delegate getting removed was a layout delegate. As a result, we need
                // to sync coordinators
                val chain = requireLayoutNode().nodes
                node.updateCoordinator(null)
                chain.syncCoordinators()
            }
        } else {
            error("Could not find delegate: $instance")
        }
    }

    private fun validateDelegateKindSet(delegateKindSet: Int, delegateNode: Modifier.Node) {
        val current = kindSet
        if (Nodes.Layout in delegateKindSet && Nodes.Layout in current) {
            // at this point, we know that the node was _already_ a layout modifier, and we are
            // delegating to another layout modifier. In order to properly handle this, we need
            // to require that the delegating node is itself a LayoutModifierNode to ensure that
            // they are explicitly handling the combination. If not, we throw, since
            check(this is LayoutModifierNode) {
                "Delegating to multiple LayoutModifierNodes without the delegating node " +
                    "implementing LayoutModifierNode itself is not allowed." +
                    "\nDelegating Node: $this" +
                    "\nDelegate Node: $delegateNode"
            }
        }
    }

    private fun updateNodeKindSet(newKindSet: Int, recalculateOwner: Boolean) {
        val before = kindSet
        kindSet = newKindSet
        if (before != newKindSet) {
            var agg = newKindSet
            if (isDelegationRoot) {
                aggregateChildKindSet = agg
            }
            // if we changed, then we must update our aggregateChildKindSet of ourselves and
            // everything up the spine

            if (isAttached) {
                val owner = node
                var it: Modifier.Node? = this
                // first we traverse up the delegate tree until we hit the "owner" node, which is
                // the node which is actually part of the tree, ie the "root delegating node".
                // As we iterate here, we update the aggregateChildKindSet as well as the kindSet,
                // since a delegating node takes on the kinds of the nodes it delegates to.
                while (it != null) {
                    agg = it.kindSet or agg
                    it.kindSet = agg
                    if (it === owner) break
                    it = it.parent
                }

                if (recalculateOwner && it === owner) {
                    agg = calculateNodeKindSetFromIncludingDelegates(owner)
                    owner.kindSet = agg
                }

                agg = agg or (it?.child?.aggregateChildKindSet ?: 0)

                // Now we are traversing the spine of nodes in the actual tree, so we update the
                // aggregateChildKindSet here, but not the kindSets.
                while (it != null) {
                    agg = it.kindSet or agg
                    it.aggregateChildKindSet = agg
                    it = it.parent
                }
            }
        }
    }

    internal inline fun forEachImmediateDelegate(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = delegate
        while (node != null) {
            block(node)
            node = node.child
        }
    }

    override fun attach() {
        super.attach()
        forEachImmediateDelegate {
            it.updateCoordinator(coordinator)
            // NOTE: it might already be attached if the delegate was delegated to inside of
            // onAttach()
            if (!it.isAttached) {
                it.attach()
            }
        }
    }

    override fun detach() {
        forEachImmediateDelegate { it.detach() }
        super.detach()
    }

    override fun reset() {
        super.reset()
        forEachImmediateDelegate { it.reset() }
    }
}
