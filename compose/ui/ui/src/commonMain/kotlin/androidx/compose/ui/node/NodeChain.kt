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

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.areObjectsOfSameType
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.internal.throwIllegalStateException
import androidx.compose.ui.layout.ModifierInfo

private val SentinelHead =
    object : Modifier.Node() {
            override fun toString() = "<Head>"
        }
        .apply { aggregateChildKindSet = 0.inv() }

internal class NodeChain(val layoutNode: LayoutNode) {
    internal val innerCoordinator = InnerNodeCoordinator(layoutNode)
    internal var outerCoordinator: NodeCoordinator = innerCoordinator
        private set

    internal val tail: Modifier.Node = innerCoordinator.tail
    internal var head: Modifier.Node = tail
        private set

    private val isUpdating: Boolean
        get() = head === SentinelHead

    private val aggregateChildKindSet: Int
        get() = head.aggregateChildKindSet

    private var current: MutableVector<Modifier.Element>? = null
    private var buffer: MutableVector<Modifier.Element>? = null
    private var cachedDiffer: Differ? = null
    private var logger: Logger? = null

    internal fun useLogger(logger: Logger?) {
        this.logger = logger
    }

    /**
     * Takes the current "head" of the chain and adds a sentinel node as its parent in order to make
     * the diffing process a bit easier to work with. This function will return the new "sentinel
     * head", and keep [head] pointing to the actual head.
     *
     * TODO: Now that we run the diff from head to tail, this may not be as helpful as it once was.
     *   Consider removing this and trimChain entirely. If we don't, we should at least refactor to
     *   make SentinelHead not a shared global mutable object, and instead just allocate one per
     *   owner or one per chain.
     */
    private fun padChain(): Modifier.Node {
        checkPrecondition(head !== SentinelHead) { "padChain called on already padded chain" }
        val currentHead = head
        currentHead.parent = SentinelHead
        SentinelHead.child = currentHead
        return SentinelHead
    }

    private fun trimChain(paddedHead: Modifier.Node): Modifier.Node {
        checkPrecondition(paddedHead === SentinelHead) {
            "trimChain called on already trimmed chain"
        }
        val result = SentinelHead.child ?: tail
        result.parent = null
        SentinelHead.child = null
        SentinelHead.aggregateChildKindSet = 0.inv()
        SentinelHead.updateCoordinator(null)
        checkPrecondition(result !== SentinelHead) { "trimChain did not update the head" }
        return result
    }

    /**
     * This method will update the node chain based on the provided modifier chain. This method is
     * responsible for calling all appropriate lifecycles for nodes that are
     * created/disposed/updated during this call.
     *
     * This method will attempt to optimize for the common scenario of the modifier chain being of
     * equal size and each element being able to be reused from the prior one. In most cases this is
     * what recomposition will result in, provided modifiers weren't conditionally provided. In the
     * cases where the modifier is not of equal length to the prior value, or modifiers of different
     * reuse types ended up in the same position, this method will deopt into a slower path which
     * will perform a diff on the modifier chain and execute a minimal number of insertions and
     * deletions.
     */
    internal fun updateFrom(m: Modifier) {
        // If we run the diff and there are no structural changes, we can avoid looping through the
        // list and updating the coordinators. We simply keep track of this during the diff to avoid
        // this overhead at the end if we can, since it should be fairly common. Note that this is
        // slightly different from [attachNeeded] since a node can be updated and return null or a
        // new instance which is perfectly valid and would require a new attach cycle, however the
        // coordinator would be identical and so [attachNeeded] would be true but this false
        var coordinatorSyncNeeded = false
        // Use the node chain itself as a head/tail temporarily to prevent pruning the linkedlist
        // to the point where we don't have reference to it. We need to undo this at the end of
        // this method.
        val paddedHead = padChain()
        // to avoid allocating vectors every time modifier is set, we have two vectors that we
        // reuse over time. Since the common case is the modifier chains will be of equal length,
        // these vectors should be sized appropriately. The "before" list is nullable, since many
        // layout nodes will never have modifier set more than once, so we avoid allocating the
        // vector in those cases.
        var before = current
        val beforeSize = before?.size ?: 0
        val after = m.fillVector(buffer ?: mutableVectorOf())
        var i = 0
        if (after.size == beforeSize) {
            // assume if the sizes are the same, that we are in a common case of no structural
            // changes we will attempt an O(n) fast-path diff and exit if a diff is detected, and
            // do the O(N^2) diff beyond that point
            // NOTE: we do not need to sync coordinators here since if any modifiers are added or
            // removed we will break into a structural update.
            var node: Modifier.Node? = paddedHead.child
            while (node != null && i < beforeSize) {
                checkPreconditionNotNull(before) { "expected prior modifier list to be non-empty" }
                val prev = before[i]
                val next = after[i]
                when (actionForModifiers(prev, next)) {
                    ActionReplace -> {
                        // structural change!
                        // back up one for the structural diff algorithm. This should be safe since
                        // our chain is padded with the EmptyHead/EmptyTail nodes
                        logger?.linearDiffAborted(i, prev, next, node)
                        node = node.parent
                        break
                    }
                    ActionUpdate -> {
                        // this is "the same" modifier, but some things have changed so we want to
                        // reuse the node but also update it
                        updateNode(prev, next, node)
                        logger?.nodeUpdated(i, i, prev, next, node)
                    }
                    ActionReuse -> {
                        logger?.nodeReused(i, i, prev, next, node)
                        // no need to do anything, this is "the same" modifier
                    }
                }
                // NOTE: We do not need to check if the node is attached since these are all updated
                // or reused modifiers only
                node = node.child
                i++
            }
            if (i < beforeSize) {
                coordinatorSyncNeeded = true
                checkPreconditionNotNull(before) { "expected prior modifier list to be non-empty" }
                checkPreconditionNotNull(node) { "structuralUpdate requires a non-null tail" }
                // there must have been a structural change
                // we only need to diff what is left of the list, so we use `i` to determine how
                // much of the list is left.
                structuralUpdate(
                    i,
                    before,
                    after,
                    node,
                    !layoutNode.applyingModifierOnAttach,
                )
            }
        } else if (layoutNode.applyingModifierOnAttach && beforeSize == 0) {
            // common case where we are initializing the chain and the previous size is zero. In
            // this case we just do all inserts. Since this is so common, we add a fast path here
            // for this condition. Since the layout node is currently attaching, the inserted nodes
            // will not get eagerly attached, which means we can avoid dealing with the coordinator
            // sync until the end, which keeps this code path much simpler.
            coordinatorSyncNeeded = true
            var node = paddedHead
            while (i < after.size) {
                val next = after[i]
                val parent = node
                node = createAndInsertNodeAsChild(next, parent)
                logger?.nodeInserted(0, i, next, parent, node)
                i++
            }
            syncAggregateChildKindSet()
        } else if (after.size == 0) {
            checkPreconditionNotNull(before) { "expected prior modifier list to be non-empty" }
            // common case where we we are removing all the modifiers.
            var node = paddedHead.child
            while (node != null && i < before.size) {
                logger?.nodeRemoved(i, before[i], node)
                node = detachAndRemoveNode(node).child
                i++
            }
            innerCoordinator.wrappedBy = layoutNode.parent?.innerCoordinator
            outerCoordinator = innerCoordinator
        } else {
            coordinatorSyncNeeded = true
            before = before ?: MutableVector()
            structuralUpdate(
                0,
                before,
                after,
                paddedHead,
                !layoutNode.applyingModifierOnAttach,
            )
        }
        current = after
        // clear the before vector to allow old modifiers to be Garbage Collected
        buffer = before?.also { it.clear() }
        head = trimChain(paddedHead)
        if (coordinatorSyncNeeded) {
            syncCoordinators()
        }
    }

    /**
     * This will "reset" all of the nodes in the chain. This includes both calling the reset
     * lifecycles, calling the detach lifecycles, and calling [markAsDetached].
     */
    internal fun resetState() {
        tailToHead { if (it.isAttached) it.reset() }
        runDetachLifecycle()
        markAsDetached()
    }

    fun syncCoordinators() {
        var coordinator: NodeCoordinator = innerCoordinator
        var node: Modifier.Node? = tail.parent
        while (node != null) {
            val layoutmod = node.asLayoutModifierNode()
            if (layoutmod != null) {
                val next =
                    if (node.coordinator != null) {
                        val c = node.coordinator as LayoutModifierNodeCoordinator
                        val prevNode = c.layoutModifierNode
                        c.layoutModifierNode = layoutmod
                        if (prevNode !== node) c.onLayoutModifierNodeChanged()
                        c
                    } else {
                        val c = LayoutModifierNodeCoordinator(layoutNode, layoutmod)
                        node.updateCoordinator(c)
                        c
                    }
                coordinator.wrappedBy = next
                next.wrapped = coordinator
                coordinator = next
            } else {
                node.updateCoordinator(coordinator)
            }
            node = node.parent
        }
        coordinator.wrappedBy = layoutNode.parent?.innerCoordinator
        outerCoordinator = coordinator
    }

    /**
     * Ensures that the current []aggregateChildKindSet] value for each of the modifier nodes are
     * set correctly. We must do this after every diff where a node was inserted or deleted. We can
     * safely avoid it in the (relatively common) case of the chain getting updated but not changing
     * structurally.
     */
    private fun syncAggregateChildKindSet() {
        var node: Modifier.Node? = tail.parent
        var aggregateChildKindSet = 0
        while (node != null && node !== SentinelHead) {
            aggregateChildKindSet = aggregateChildKindSet or node.kindSet
            node.aggregateChildKindSet = aggregateChildKindSet
            node = node.parent
        }
    }

    /**
     * Runs through all nodes in the chain and calls [markAsAttached]. This will ensure that the
     * node has a coordinator and is marked as attached, but the node will not yet be notified that
     * this happened. As a result, [runAttachLifecycle] is expected to be called afterwards to
     * notify the node that it is attached. These are separated to ensure that when the attach
     * lifecycle is called, all nodes in the hierarchy are marked as attached.
     */
    fun markAsAttached() {
        headToTail { it.markAsAttached() }
    }

    /**
     * Runs through all nodes in the chain and calls the attach lifecycle. It also runs
     * invalidations as a result of the attach, if needed.
     */
    fun runAttachLifecycle() {
        headToTail {
            it.runAttachLifecycle()
            if (it.insertedNodeAwaitingAttachForInvalidation) {
                autoInvalidateInsertedNode(it)
            }
            if (it.updatedNodeAwaitingAttachForInvalidation) {
                autoInvalidateUpdatedNode(it)
            }
            // when we attach with performInvalidations == false no separate
            // invalidations needed as the whole LayoutNode is attached to the tree.
            // it will cause all the needed invalidations.
            // TODO: can we get rid of these now?
            it.insertedNodeAwaitingAttachForInvalidation = false
            it.updatedNodeAwaitingAttachForInvalidation = false
        }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information that may
     * be useful. This is used for tooling to retrieve layout modifier and layer information.
     */
    fun getModifierInfo(): List<ModifierInfo> {
        val current = current ?: return emptyList()
        val infoList = MutableVector<ModifierInfo>(current.size)
        var i = 0
        headToTailExclusive { node ->
            val coordinator =
                requireNotNull(node.coordinator) {
                    "getModifierInfo called on node with no coordinator"
                }
            // placeWithLayer puts the layer on the _next_ coordinator
            //
            // - If the last node does placeWithLayer, the layer is on the innerCoordinator
            // - the first LayoutNode in the tree gets a layer from the root
            //
            // This logic prefers to use this.layer, but will use innerCoordinator on the last node
            // -- this exists for ui-inspector and must remain stable due to a non-same-version
            // release dependency on tree structure.
            val currentNodeLayer = coordinator.layer
            val innerNodeLayer =
                innerCoordinator.layer.takeIf {
                    // emit the innerCoordinator only if it's different than current coordinator and
                    // this is the last node

                    // note: this logic will correctly handle the case where a Modifier.Node as the
                    // last
                    // element in the chain calls placeWithLayer. However, it does also cause an
                    // emit
                    // when .graphicsLayer is the last element in the chain as well - was previously
                    // depended upon by ui-tooling to avoid seeing the Crossfade layer.

                    // Going forward, as a contract, all layers will be emitted. And UI-tooling
                    // should
                    // not gain a new dependency on omitted layers.
                    val localChild = node.child
                    localChild === tail && node.coordinator !== localChild.coordinator
                }
            val layer = currentNodeLayer ?: innerNodeLayer
            infoList += ModifierInfo(current[i++], coordinator, layer)
        }
        return infoList.asMutableList()
    }

    internal fun markAsDetached() {
        tailToHead { if (it.isAttached) it.markAsDetached() }
    }

    internal fun runDetachLifecycle() {
        tailToHead { if (it.isAttached) it.runDetachLifecycle() }
    }

    private fun getDiffer(
        head: Modifier.Node,
        offset: Int,
        before: MutableVector<Modifier.Element>,
        after: MutableVector<Modifier.Element>,
        shouldAttachOnInsert: Boolean,
    ): Differ {
        val current = cachedDiffer
        @Suppress("IfThenToElvis")
        return if (current == null) {
            Differ(
                    head,
                    offset,
                    before,
                    after,
                    // TODO: is this always true?
                    shouldAttachOnInsert,
                )
                .also { cachedDiffer = it }
        } else {
            current.also {
                it.node = head
                it.offset = offset
                it.before = before
                it.after = after
                it.shouldAttachOnInsert = shouldAttachOnInsert
            }
        }
    }

    private fun propagateCoordinator(start: Modifier.Node, coordinator: NodeCoordinator) {
        var node = start.parent
        while (node != null) {
            if (node === SentinelHead) {
                coordinator.wrappedBy = layoutNode.parent?.innerCoordinator
                outerCoordinator = coordinator
                break
            }
            if (node.isKind(Nodes.Layout)) break
            node.updateCoordinator(coordinator)
            node = node.parent
        }
    }

    private inner class Differ(
        var node: Modifier.Node,
        var offset: Int,
        var before: MutableVector<Modifier.Element>,
        var after: MutableVector<Modifier.Element>,
        var shouldAttachOnInsert: Boolean,
    ) : DiffCallback {
        override fun areItemsTheSame(oldIndex: Int, newIndex: Int): Boolean {
            return actionForModifiers(before[offset + oldIndex], after[offset + newIndex]) !=
                ActionReplace
        }

        override fun insert(newIndex: Int) {
            val index = offset + newIndex
            val parent = node
            node = createAndInsertNodeAsChild(after[index], parent)
            logger?.nodeInserted(index, index, after[index], parent, node)

            if (shouldAttachOnInsert) {
                val childCoordinator = node.child!!.coordinator!!
                val layoutmod = node.asLayoutModifierNode()
                if (layoutmod != null) {
                    val thisCoordinator = LayoutModifierNodeCoordinator(layoutNode, layoutmod)
                    node.updateCoordinator(thisCoordinator)
                    propagateCoordinator(node, thisCoordinator)
                    thisCoordinator.wrappedBy = childCoordinator.wrappedBy
                    thisCoordinator.wrapped = childCoordinator
                    childCoordinator.wrappedBy = thisCoordinator
                } else {
                    node.updateCoordinator(childCoordinator)
                }
                node.markAsAttached()
                node.runAttachLifecycle()
                autoInvalidateInsertedNode(node)
            } else {
                node.insertedNodeAwaitingAttachForInvalidation = true
            }
        }

        override fun remove(atIndex: Int, oldIndex: Int) {
            val toRemove = node.child!!
            logger?.nodeRemoved(oldIndex, before[offset + oldIndex], toRemove)
            if (toRemove.isKind(Nodes.Layout)) {
                val removedCoordinator = toRemove.coordinator!!
                // parent might be null
                val parentCoordinator = removedCoordinator.wrappedBy
                // child should never be null because of innerCoordinator
                val childCoordinator = removedCoordinator.wrapped!!
                parentCoordinator?.wrapped = childCoordinator
                childCoordinator.wrappedBy = parentCoordinator
                propagateCoordinator(node, childCoordinator)
            }
            node = detachAndRemoveNode(toRemove)
        }

        override fun same(oldIndex: Int, newIndex: Int) {
            node = node.child!!
            val prev = before[offset + oldIndex]
            val next = after[offset + newIndex]
            if (prev != next) {
                updateNode(prev, next, node)
                logger?.nodeUpdated(offset + oldIndex, offset + newIndex, prev, next, node)
            } else {
                logger?.nodeReused(offset + oldIndex, offset + newIndex, prev, next, node)
            }
        }
    }

    internal interface Logger {
        fun linearDiffAborted(
            index: Int,
            prev: Modifier.Element,
            next: Modifier.Element,
            node: Modifier.Node
        )

        fun nodeUpdated(
            oldIndex: Int,
            newIndex: Int,
            prev: Modifier.Element,
            next: Modifier.Element,
            node: Modifier.Node,
        )

        fun nodeReused(
            oldIndex: Int,
            newIndex: Int,
            prev: Modifier.Element,
            next: Modifier.Element,
            node: Modifier.Node
        )

        fun nodeInserted(
            atIndex: Int,
            newIndex: Int,
            element: Modifier.Element,
            child: Modifier.Node,
            inserted: Modifier.Node
        )

        fun nodeRemoved(oldIndex: Int, element: Modifier.Element, node: Modifier.Node)
    }

    /**
     * This method utilizes a modified Myers Diff Algorithm which will diff the two modifier chains
     * and execute a minimal number of insertions/deletions. We make no attempt to execute "moves"
     * as part of this diff. If a modifier moves that is no different than it being inserted in the
     * new location and removed in the old location.
     *
     * @param tail - The Node that corresponds to the _end_ of the [before] list. This Node is
     *   expected to have an up to date [aggregateChildKindSet].
     */
    private fun structuralUpdate(
        offset: Int,
        before: MutableVector<Modifier.Element>,
        after: MutableVector<Modifier.Element>,
        tail: Modifier.Node,
        shouldAttachOnInsert: Boolean,
    ) {
        val differ = getDiffer(tail, offset, before, after, shouldAttachOnInsert)
        executeDiff(before.size - offset, after.size - offset, differ)
        syncAggregateChildKindSet()
    }

    private fun detachAndRemoveNode(node: Modifier.Node): Modifier.Node {
        if (node.isAttached) {
            // for removing nodes, we always do the autoInvalidateNode call,
            // regardless of whether or not it was a ModifierNodeElement with autoInvalidate
            // true, or a BackwardsCompatNode, etc.
            autoInvalidateRemovedNode(node)
            node.runDetachLifecycle()
            node.markAsDetached()
        }
        return removeNode(node)
    }

    /**
     * This removes [node] from the current linked list. For example:
     *
     *      Head... -> parent -> node -> child -> ...Tail
     *
     * gets transformed into a list of the following shape:
     *
     *      Head... -> parent -> child -> ...Tail
     *
     * @return The parent of the removed [node]
     */
    private fun removeNode(node: Modifier.Node): Modifier.Node {
        val child = node.child
        val parent = node.parent
        if (child != null) {
            child.parent = parent
            node.child = null
        }
        if (parent != null) {
            parent.child = child
            node.parent = null
        }
        return parent!!
    }

    private fun createAndInsertNodeAsChild(
        element: Modifier.Element,
        parent: Modifier.Node,
    ): Modifier.Node {
        val node =
            when (element) {
                is ModifierNodeElement<*> ->
                    element.create().also {
                        it.kindSet = calculateNodeKindSetFromIncludingDelegates(it)
                    }
                else -> BackwardsCompatNode(element)
            }
        checkPrecondition(!node.isAttached) {
            "A ModifierNodeElement cannot return an already attached node from create() "
        }
        node.insertedNodeAwaitingAttachForInvalidation = true
        return insertChild(node, parent)
    }

    /**
     * This inserts [node] as the child of [parent] in the current linked list. For example:
     *
     *      Head... -> parent -> ...Tail
     *
     * gets transformed into a list of the following shape:
     *
     *      Head... -> parent -> node -> ...Tail
     *
     * @return The inserted [node]
     */
    private fun insertChild(node: Modifier.Node, parent: Modifier.Node): Modifier.Node {
        val theChild = parent.child
        if (theChild != null) {
            theChild.parent = node
            node.child = theChild
        }
        parent.child = node
        node.parent = parent
        return node
    }

    private fun updateNode(prev: Modifier.Element, next: Modifier.Element, node: Modifier.Node) {
        when {
            prev is ModifierNodeElement<*> && next is ModifierNodeElement<*> -> {
                next.updateUnsafe(node)
                if (node.isAttached) {
                    // the modifier element is labeled as "auto invalidate", which means
                    // that since the node was updated, we need to invalidate everything
                    // relevant to it.
                    autoInvalidateUpdatedNode(node)
                } else {
                    node.updatedNodeAwaitingAttachForInvalidation = true
                }
            }
            node is BackwardsCompatNode -> {
                node.element = next
                // We always autoInvalidate BackwardsCompatNode.
                if (node.isAttached) {
                    autoInvalidateUpdatedNode(node)
                } else {
                    node.updatedNodeAwaitingAttachForInvalidation = true
                }
            }
            else -> throwIllegalStateException("Unknown Modifier.Node type")
        }
    }

    // TRAVERSAL

    internal inline fun <reified T> firstFromHead(type: NodeKind<T>, block: (T) -> Boolean): T? {
        headToTail(type) { if (block(it)) return it }
        return null
    }

    internal inline fun <reified T> headToTail(type: NodeKind<T>, block: (T) -> Unit) {
        headToTail(type.mask) { it.dispatchForKind(type, block) }
    }

    internal inline fun headToTail(mask: Int, block: (Modifier.Node) -> Unit) {
        if (aggregateChildKindSet and mask == 0) return
        headToTail {
            if (it.kindSet and mask != 0) {
                block(it)
            }
            if (it.aggregateChildKindSet and mask == 0) return
        }
    }

    /**
     * Traverses the linked list from head to tail, running [block] on each Node as it goes. If
     * [block] returns true, it will stop traversing and return true. If [block] returns false, it
     * will continue.
     *
     * @return Returns true if [block] ever returned true, false otherwise.
     */
    internal inline fun headToTail(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = head
        while (node != null) {
            block(node)
            node = node.child
        }
    }

    internal inline fun headToTailExclusive(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = head
        while (node != null && node !== tail) {
            block(node)
            node = node.child
        }
    }

    internal inline fun <reified T> tailToHead(type: NodeKind<T>, block: (T) -> Unit) {
        tailToHead(type.mask) { it.dispatchForKind(type, block) }
    }

    internal inline fun tailToHead(mask: Int, block: (Modifier.Node) -> Unit) {
        if (aggregateChildKindSet and mask == 0) return
        tailToHead {
            if (it.kindSet and mask != 0) {
                block(it)
            }
        }
    }

    internal inline fun tailToHead(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = tail
        while (node != null) {
            block(node)
            node = node.parent
        }
    }

    internal inline fun <reified T> tail(type: NodeKind<T>): T? {
        tailToHead(type) {
            return it
        }
        return null
    }

    internal inline fun <reified T> head(type: NodeKind<T>): T? {
        headToTail(type) {
            return it
        }
        return null
    }

    internal fun has(type: NodeKind<*>): Boolean = aggregateChildKindSet and type.mask != 0

    internal fun has(mask: Int): Boolean = aggregateChildKindSet and mask != 0

    override fun toString(): String = buildString {
        append("[")
        if (head === tail) {
            append("]")
            return@buildString
        }
        headToTailExclusive {
            append("$it")
            if (it.child === tail) {
                append("]")
                return@buildString
            }
            append(",")
        }
    }
}

private const val ActionReplace = 0
private const val ActionUpdate = 1
private const val ActionReuse = 2

/**
 * Here's the rules for reusing nodes for different modifiers:
 * 1. if modifiers are equals, we REUSE but NOT UPDATE
 * 2. if modifiers are same class, we REUSE and UPDATE
 * 3. else REPLACE (NO REUSE, NO UPDATE)
 */
internal fun actionForModifiers(prev: Modifier.Element, next: Modifier.Element): Int {
    return if (prev == next) {
        ActionReuse
    } else if (areObjectsOfSameType(prev, next)) {
        ActionUpdate
    } else {
        ActionReplace
    }
}

private fun <T : Modifier.Node> ModifierNodeElement<T>.updateUnsafe(node: Modifier.Node) {
    @Suppress("UNCHECKED_CAST") update(node as T)
}

private fun Modifier.fillVector(
    result: MutableVector<Modifier.Element>
): MutableVector<Modifier.Element> {
    val capacity = result.size.coerceAtLeast(16)
    val stack = MutableVector<Modifier>(capacity).also { it.add(this) }
    var predicate: ((Modifier.Element) -> Boolean)? = null
    while (stack.isNotEmpty()) {
        when (val next = stack.removeAt(stack.size - 1)) {
            is CombinedModifier -> {
                stack.add(next.inner)
                stack.add(next.outer)
            }
            is Modifier.Element -> result.add(next)
            // some other androidx.compose.ui.node.Modifier implementation that we don't know
            // about...
            // late-allocate the predicate only once for the entire stack
            else ->
                next.all(
                    predicate
                        ?: { element: Modifier.Element ->
                                result.add(element)
                                true
                            }
                            .also { predicate = it }
                )
        }
    }
    return result
}
