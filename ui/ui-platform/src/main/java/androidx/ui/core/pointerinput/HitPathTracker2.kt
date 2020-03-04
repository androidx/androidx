/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.pointerinput

import androidx.annotation.VisibleForTesting
import androidx.ui.core.CustomEvent
import androidx.ui.core.CustomEventDispatcher
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize

/**
 * Organizes pointers and the [PointerInputFilter]s that they hit into a hierarchy such that
 * [PointerInputChange]s can be dispatched to the [PointerInputFilter]s in a hierarchical fashion.
 */
internal class HitPathTracker2 {

    @VisibleForTesting
    internal val root: NodeParent = NodeParent()

    /**
     * Associates a [pointerId] to a list of hit [pointerInputFilters] and keeps track of them.
     *
     * This enables future calls to [dispatchChanges] to dispatch the correct [PointerInputChange]s
     * to the right [PointerInputFilter]s at the right time.
     *
     * @param pointerId The id of the pointer that was hit tested against [PointerInputFilter]s
     * @param pointerInputFilters The [PointerInputFilter]s that were hit by [pointerId].  Must be
     * ordered from ancestor to descendant.
     */
    fun addHitPath(pointerId: PointerId, pointerInputFilters: List<PointerInputFilter>) {
        var parent: NodeParent = root
        var merging = true
        eachPin@ for (pointerInputFilter in pointerInputFilters) {
            if (merging) {
                val node = parent.children.find { it.pointerInputFilter == pointerInputFilter }
                if (node != null) {
                    node.pointerIds.add(pointerId)
                    parent = node
                    continue@eachPin
                } else {
                    merging = false
                }
            }
            val node = Node(pointerInputFilter).apply {
                pointerIds.add(pointerId)
            }
            parent.children.add(node)
            parent = node

            // TODO(shepshapard): Is CustomEventDispatcherImpl instantiated even if initHandler is
            //  null?
            pointerInputFilter.initHandler?.invoke(
                CustomEventDispatcherImpl(
                    node,
                    this
                )
            )
        }
    }

    /**
     * Stops tracking the [pointerId] and stops tracking any [PointerInputFilter]s that are
     * therefore no longer associated with any pointer ids.
     */
    fun removeHitPath(pointerId: PointerId) {
        root.removePointerId(pointerId)
    }

    /**
     * Dispatches [pointerInputChanges] through the hierarchy; first down the hierarchy, passing
     * [downPass] to each [PointerInputFilter], and then up the hierarchy with [upPass] if [upPass]
     * is not null.
     */
    fun dispatchChanges(
        pointerInputChanges: List<PointerInputChange>,
        downPass: PointerEventPass,
        upPass: PointerEventPass? = null
    ): List<PointerInputChange> {

        // TODO(b/124523868): It may be more efficient for PointerInputFilters to be able to opt in
        //  or out of passes.
        val idToChangesMap = pointerInputChanges.associateByTo(mutableMapOf()) { it.id }
        root.dispatchChanges(idToChangesMap, downPass, upPass)
        return idToChangesMap.values.toList()
    }

    /**
     * Dispatches the [event] through the hierarchy in all 5 passes of [PointerEventPass].
     *
     * @param event The [Any] to dispatch.
     * @param dispatchingNode The pointer input node responsible for the dispatch.
     *
     * @return The resulting [PointerInputChange]s.
     */
    @VisibleForTesting
    internal fun dispatchCustomEvent(
        event: CustomEvent,
        dispatchingNode: Node
    ) {
        val associatedPointers = dispatchingNode.pointerIds

        // TODO(b/124523868): It may be more efficient for PointerInputNodes to be able to opt in
        //  or out of passes.
        root.dispatchCustomEvent(
            event,
            associatedPointers,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            dispatchingNode
        )
        root.dispatchCustomEvent(
            event,
            associatedPointers,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp,
            dispatchingNode
        )
        root.dispatchCustomEvent(
            event,
            associatedPointers,
            PointerEventPass.PostDown,
            null,
            dispatchingNode
        )
    }

    private class CustomEventDispatcherImpl(
        val dispatchingNode: Node,
        val hitPathTracker: HitPathTracker2
    ) : CustomEventDispatcher {
        override fun dispatchCustomEvent(event: CustomEvent) {
            hitPathTracker.dispatchCustomEvent(event, dispatchingNode)
        }
    }

    /**
     * Dispatches cancel events to all tracked [PointerInputFilter]s to notify them that
     * [PointerInputFilter.pointerInputHandler] will not be called again until all pointers have been
     * removed from the application and then at least one is added again, and removes all tracked
     * data.
     */
    fun processCancel() {
        root.dispatchCancel()
        root.clear()
    }

    /**
     * Removes [PointerInputFilter]s that have been removed from the component tree.
     */
    // TODO(shepshapard): Ideally, we can process the detaching of PointerInputFilters at the time
    //  that either their associated LayoutNode is removed from the three, or their
    //  associated PointerInputModifier is removed from a LayoutNode.
    fun removeDetachedPointerInputFilters() {
        root.removeDetachedPointerInputFilters()
    }
}

/**
 * Represents a parent node in the [HitPathTracker2]'s tree.  This primarily exists because the tree
 * necessarily has a root that is very similar to all other nodes, except that it does not track any
 * pointer or [PointerInputFilter] information.
 */
@VisibleForTesting
internal open class NodeParent {
    val children: MutableSet<Node> = mutableSetOf()

    /**
     * Dispatches the [pointerInputChanges] to all child nodes.
     *
     * Note: [pointerInputChanges] is expected to be mutated during dispatch.
     */
    open fun dispatchChanges(
        pointerInputChanges: MutableMap<PointerId, PointerInputChange>,
        downPass: PointerEventPass,
        upPass: PointerEventPass?
    ) {
        children.forEach { it.dispatchChanges(pointerInputChanges, downPass, upPass) }
    }

    /**
     * Dispatches the [event] to all child [Node]s.
     */
    open fun dispatchCustomEvent(
        event: CustomEvent,
        relevantPointers: Set<PointerId>,
        downPass: PointerEventPass,
        upPass: PointerEventPass?,
        dispatchingNode: Node
    ) {
        children.forEach {
            it.dispatchCustomEvent(
                event,
                relevantPointers,
                downPass,
                upPass,
                dispatchingNode
            )
        }
    }

    /**
     * Dispatches the cancel event to all child [Node]s.
     */
    open fun dispatchCancel() {
        children.forEach { it.dispatchCancel() }
    }

    /**
     * Removes all child nodes.
     */
    fun clear() {
        children.clear()
    }

    /**
     * Removes all child [Node]s that are no longer attached to the compose tree.
     */
    fun removeDetachedPointerInputFilters() {
        children.removeAndProcess(
            removeIf = {
                !it.pointerInputFilter.isAttached
            },
            ifRemoved = {
                it.dispatchCancel()
            },
            ifKept = {
                it.removeDetachedPointerInputFilters()
            })
    }

    /**
     * Removes the tracking of [pointerId] and removes all child [Node]s that are no longer
     * tracking
     * any [PointerId]s.
     */
    fun removePointerId(pointerId: PointerId) {
        children.forEach {
            it.pointerIds.remove(pointerId)
        }
        children.removeAll {
            it.pointerIds.isEmpty()
        }
        children.forEach {
            it.removePointerId(pointerId)
        }
    }

    /**
     * With each item, if calling [removeIf] with it is true, removes the item from [this] and calls
     * [ifRemoved] with it, otherwise calls [ifKept] with it.
     */
    private fun <T> MutableIterable<T>.removeAndProcess(
        removeIf: (T) -> Boolean,
        ifRemoved: (T) -> Unit,
        ifKept: (T) -> Unit
    ) {
        with(iterator()) {
            while (hasNext()) {
                val next = next()
                if (removeIf(next)) {
                    remove()
                    ifRemoved(next)
                } else {
                    ifKept(next)
                }
            }
        }
    }
}

/**
 * Represents a single Node in the tree that also tracks a [PointerInputFilter] and which pointers
 * hit it (tracked as [PointerId]s).
 */
@VisibleForTesting
internal class Node(val pointerInputFilter: PointerInputFilter) : NodeParent() {

    val pointerIds: MutableSet<PointerId> = mutableSetOf()

    override fun dispatchChanges(
        pointerInputChanges: MutableMap<PointerId, PointerInputChange>,
        downPass: PointerEventPass,
        upPass: PointerEventPass?
    ) {
        // Filter for changes that are associated with pointer ids that are relevant to this node.
        val relevantChanges =
            pointerInputChanges.filterTo(mutableMapOf()) { entry ->
                pointerIds.contains(entry.key)
            }

        if (relevantChanges.isEmpty()) {
            throw IllegalStateException(
                "Currently, HitPathTracker is operating under the assumption that there should " +
                        "never be a circumstance in which it is tracking a PointerInputFilter " +
                        "where when it receives pointerInputChanges, none are relevant to that " +
                        "PointerInputFilter.  This assumption may not hold true in the future, " +
                        "but currently it assumes it can abide by this contract."
            )
        }

        // For each relevant change:
        //  1. subtract the offset
        //  2. dispatch the change on the down pass,
        //  3. update it in relevantChanges.
        relevantChanges.let {
            // TODO(shepshapard): would be nice if we didn't have to subtract and then add
            //  offsets.  This is currently done because the calculated offsets are currently
            //  global, not relative to eachother.
            it.subtractOffset(pointerInputFilter.position)
            it.dispatchToPointerInputFilter(pointerInputFilter, downPass, pointerInputFilter.size)
            it.addOffset(pointerInputFilter.position)
        }

        // Call children recursively with the relevant changes.
        children.forEach { it.dispatchChanges(relevantChanges, downPass, upPass) }

        // For each relevant change:
        //  1. dispatch the change on the up pass,
        //  2. add the offset,
        //  3. update it in  relevant changes.
        if (upPass != null) {
            relevantChanges.let {
                it.subtractOffset(pointerInputFilter.position)
                it.dispatchToPointerInputFilter(pointerInputFilter, upPass, pointerInputFilter.size)
                it.addOffset(pointerInputFilter.position)
            }
        }

        // Mutate the pointerInputChanges with the ones we modified.
        pointerInputChanges.putAll(relevantChanges)
    }

    /**
     * Dispatches the [event] to the pointer input node this [Node] is tracking and to all child
     * [Node]s.
     *
     * If this [Node] is tracking any [PointerId]s in [relevantPointers],
     * <ol>
     * <li> Dispatches the [event] to the pointer input node it is tracking with [downPass].
     * <li> Dispatches the [event] to all child [Node]s.
     * <li> Dispatches the [event] to the pointer input node it is tracking with [upPass] (if not
     * null).
     * </ol>
     */
    override fun dispatchCustomEvent(
        event: CustomEvent,
        relevantPointers: Set<PointerId>,
        downPass: PointerEventPass,
        upPass: PointerEventPass?,
        dispatchingNode: Node
    ) {
        // If we have a PointerInputNode and we don't contain any of the relevant pointers, stop
        // and back track up the tree.
        if (!relevantPointers.any { pointerIds.contains(it) }) {
            return
        }

        if (this != dispatchingNode) {
            pointerInputFilter.customEventHandler?.invoke(event, downPass)
        }

        // Call children recursively with the relevant changes.
        children.forEach {
            it.dispatchCustomEvent(event, relevantPointers, downPass, upPass, dispatchingNode)
        }

        if (upPass != null && this != dispatchingNode) {
            pointerInputFilter.customEventHandler?.invoke(event, upPass)
        }
    }

    // TODO(shepshapard): Should some order of cancel dispatch be guaranteed? I think the answer is
    //  essentially "no", but given that an order can be consistent... maybe we might as well
    //  set an arbitrary standard and stick to it so user expectations are maintained.
    /**
     * Does a depth first traversal and invokes [PointerInputFilter.cancelHandler] during
     * backtracking.
     */
    override fun dispatchCancel() {
        children.forEach { it.dispatchCancel() }
        pointerInputFilter.cancelHandler.invoke()
    }

    override fun toString(): String {
        return "Node(pointerInputFilter=$pointerInputFilter, children=$children, " +
                "pointerIds=$pointerIds)"
    }

    private fun MutableMap<PointerId, PointerInputChange>.dispatchToPointerInputFilter(
        filter: PointerInputFilter,
        pass: PointerEventPass,
        size: IntPxSize
    ) {
        filter.pointerInputHandler(values.toList(), pass, size).forEach {
            this[it.id] = it
        }
    }

    private fun MutableMap<out Any, PointerInputChange>.addOffset(position: IntPxPosition) {
        if (position != IntPxPosition.Origin) {
            replaceEverything {
                it.copy(
                    current = it.current.copy(position = it.current.position?.plus(position)),
                    previous = it.previous.copy(position = it.previous.position?.plus(position))
                )
            }
        }
    }

    private fun MutableMap<out Any, PointerInputChange>.subtractOffset(position: IntPxPosition) {
        addOffset(-position)
    }

    private inline fun <K, V> MutableMap<K, V>.replaceEverything(f: (V) -> V) {
        for (entry in this) {
            entry.setValue(f(entry.value))
        }
    }
}
