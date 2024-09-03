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

package androidx.compose.ui.input.pointer

import androidx.collection.LongSparseArray
import androidx.collection.MutableLongObjectMap
import androidx.collection.MutableObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.util.PointerIdArray
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.dispatchForKind
import androidx.compose.ui.node.layoutCoordinates
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach

/**
 * Organizes pointers and the [PointerInputFilter]s that they hit into a hierarchy such that
 * [PointerInputChange]s can be dispatched to the [PointerInputFilter]s in a hierarchical fashion.
 *
 * @property rootCoordinates the root [LayoutCoordinates] that [PointerInputChange]s will be
 *   relative to.
 */
internal class HitPathTracker(private val rootCoordinates: LayoutCoordinates) {

    /*@VisibleForTesting*/
    internal val root: NodeParent = NodeParent()

    private val hitPointerIdsAndNodes = MutableLongObjectMap<MutableObjectList<Node>>(10)

    /**
     * Associates a [pointerId] to a list of hit [pointerInputNodes] and keeps track of them.
     *
     * This enables future calls to [dispatchChanges] to dispatch the correct [PointerInputChange]s
     * to the right [PointerInputFilter]s at the right time.
     *
     * If [pointerInputNodes] is empty, nothing will be added.
     *
     * @param pointerId The id of the pointer that was hit tested against [PointerInputFilter]s
     * @param pointerInputNodes The [PointerInputFilter]s that were hit by [pointerId]. Must be
     *   ordered from ancestor to descendant.
     * @param prunePointerIdsAndChangesNotInNodesList Prune [PointerId]s (and associated changes)
     *   that are NOT in the pointerInputNodes parameter from the cached tree of ParentNode/Node.
     */
    fun addHitPath(
        pointerId: PointerId,
        pointerInputNodes: List<Modifier.Node>,
        prunePointerIdsAndChangesNotInNodesList: Boolean = false
    ) {
        var parent: NodeParent = root
        hitPointerIdsAndNodes.clear()
        var merging = true

        eachPin@ for (i in pointerInputNodes.indices) {
            val pointerInputNode = pointerInputNodes[i]

            // Doesn't add nodes that aren't attached
            if (pointerInputNode.isAttached) {
                pointerInputNode.detachedListener = {
                    removePointerInputModifierNode(pointerInputNode)
                }

                if (merging) {
                    val node = parent.children.firstOrNull { it.modifierNode == pointerInputNode }

                    if (node != null) {
                        node.markIsIn()
                        node.pointerIds.add(pointerId)

                        val mutableObjectList =
                            hitPointerIdsAndNodes.getOrPut(pointerId.value) {
                                mutableObjectListOf()
                            }

                        mutableObjectList.add(node)
                        parent = node
                        continue@eachPin
                    } else {
                        merging = false
                    }
                }
                // TODO(lmr): i wonder if Node here and PointerInputNode ought to be the same thing?
                val node = Node(pointerInputNode).apply { pointerIds.add(pointerId) }

                val mutableObjectList =
                    hitPointerIdsAndNodes.getOrPut(pointerId.value) { mutableObjectListOf() }

                mutableObjectList.add(node)

                parent.children.add(node)
                parent = node
            }
        }

        if (prunePointerIdsAndChangesNotInNodesList) {
            hitPointerIdsAndNodes.forEach { key, value ->
                removeInvalidPointerIdsAndChanges(key, value)
            }
        }
    }

    private fun removePointerInputModifierNode(pointerInputNode: Modifier.Node) {
        root.removePointerInputModifierNode(pointerInputNode)
    }

    // Removes pointers/changes that are not in the latest hit test
    private fun removeInvalidPointerIdsAndChanges(
        pointerId: Long,
        hitNodes: MutableObjectList<Node>
    ) {
        root.removeInvalidPointerIdsAndChanges(pointerId, hitNodes)
    }

    /**
     * Dispatches [internalPointerEvent] through the hierarchy.
     *
     * @param internalPointerEvent The change to dispatch.
     * @return whether this event was dispatched to a [PointerInputFilter]
     */
    fun dispatchChanges(
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean = true
    ): Boolean {
        val changed =
            root.buildCache(
                internalPointerEvent.changes,
                rootCoordinates,
                internalPointerEvent,
                isInBounds
            )
        if (!changed) {
            return false
        }
        var dispatchHit =
            root.dispatchMainEventPass(
                internalPointerEvent.changes,
                rootCoordinates,
                internalPointerEvent,
                isInBounds
            )
        dispatchHit = root.dispatchFinalEventPass(internalPointerEvent) || dispatchHit

        return dispatchHit
    }

    fun clearPreviouslyHitModifierNodeCache() {
        root.clear()
    }

    /**
     * Dispatches cancel events to all tracked [PointerInputFilter]s to notify them that
     * [PointerInputFilter.onPointerEvent] will not be called again until all pointers have been
     * removed from the application and then at least one is added again, and removes all tracked
     * data.
     */
    fun processCancel() {
        root.dispatchCancel()
        clearPreviouslyHitModifierNodeCache()
    }
}

/**
 * Represents a parent node in the [HitPathTracker]'s tree. This primarily exists because the tree
 * necessarily has a root that is very similar to all other nodes, except that it does not track any
 * pointer or [PointerInputFilter] information.
 */
/*@VisibleForTesting*/
@OptIn(InternalCoreApi::class)
internal open class NodeParent {
    val children: MutableVector<Node> = mutableVectorOf()

    // Supports removePointerInputModifierNode() function
    private val removeMatchingPointerInputModifierNodeList = MutableObjectList<NodeParent>(10)

    open fun buildCache(
        changes: LongSparseArray<PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        var changed = false
        children.forEach {
            changed =
                it.buildCache(changes, parentCoordinates, internalPointerEvent, isInBounds) ||
                    changed
        }
        return changed
    }

    /**
     * Dispatches [changes] down the tree, for the initial and main pass.
     *
     * [changes] and other properties needed in all passes should be cached inside this method so
     * they can be reused in [dispatchFinalEventPass], since the passes happen consecutively.
     *
     * @param changes the map containing [PointerInputChange]s that will be dispatched to relevant
     *   [PointerInputFilter]s
     * @param parentCoordinates the [LayoutCoordinates] the positional information in [changes] is
     *   relative to
     * @param internalPointerEvent the [InternalPointerEvent] needed to construct [PointerEvent]s
     */
    open fun dispatchMainEventPass(
        changes: LongSparseArray<PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        var dispatched = false
        children.forEach {
            dispatched =
                it.dispatchMainEventPass(
                    changes,
                    parentCoordinates,
                    internalPointerEvent,
                    isInBounds
                ) || dispatched
        }
        return dispatched
    }

    /**
     * Dispatches the final event pass down the tree.
     *
     * Properties cached in [dispatchMainEventPass] should be reset after this method, to ensure
     * clean state for a future pass where pointer IDs / positions might be different.
     */
    open fun dispatchFinalEventPass(internalPointerEvent: InternalPointerEvent): Boolean {
        var dispatched = false
        children.forEach {
            dispatched = it.dispatchFinalEventPass(internalPointerEvent) || dispatched
        }
        cleanUpHits(internalPointerEvent)
        return dispatched
    }

    /** Dispatches the cancel event to all child [Node]s. */
    open fun dispatchCancel() {
        children.forEach { it.dispatchCancel() }
    }

    open fun removePointerInputModifierNode(pointerInputModifierNode: Modifier.Node) {
        removeMatchingPointerInputModifierNodeList.clear()

        // adds root first
        removeMatchingPointerInputModifierNodeList.add(this)

        while (removeMatchingPointerInputModifierNodeList.isNotEmpty()) {
            val parent =
                removeMatchingPointerInputModifierNodeList.removeAt(
                    removeMatchingPointerInputModifierNodeList.size - 1
                )

            var index = 0
            while (index < parent.children.size) {
                val child = parent.children[index]

                if (child.modifierNode == pointerInputModifierNode) {
                    parent.children.remove(child)
                    child.dispatchCancel()
                    // TODO(JJW): Break here if we change tree structure so same node can't be in
                    //  multiple locations (they can be now).
                } else {
                    removeMatchingPointerInputModifierNodeList.add(child)
                    index++
                }
            }
        }
    }

    /** Removes all child nodes. */
    fun clear() {
        children.clear()
    }

    open fun removeInvalidPointerIdsAndChanges(
        pointerIdValue: Long,
        hitNodes: MutableObjectList<Node>
    ) {
        children.forEach { it.removeInvalidPointerIdsAndChanges(pointerIdValue, hitNodes) }
    }

    open fun cleanUpHits(internalPointerEvent: InternalPointerEvent) {
        for (i in children.lastIndex downTo 0) {
            val child = children[i]
            if (child.pointerIds.isEmpty()) {
                children.removeAt(i)
            }
        }
    }
}

/**
 * Represents a single Node in the tree that also tracks a [PointerInputFilter] and which pointers
 * hit it (tracked as [PointerId]s).
 */
/*@VisibleForTesting*/
@OptIn(InternalCoreApi::class)
internal class Node(val modifierNode: Modifier.Node) : NodeParent() {

    // Note: pointerIds are stored in a structure specific to their value type (PointerId).
    // This structure uses a LongArray internally, which avoids auto-boxing caused by
    // a more generic collection such as HashMap or MutableVector.
    val pointerIds = PointerIdArray()

    /**
     * Cached properties that will be set before the main event pass, and reset after the final
     * pass. Since we know that these won't change within the entire pass, we don't need to
     * calculate / create these for each pass / multiple times during a pass.
     *
     * @see buildCache
     * @see clearCache
     */
    private val relevantChanges: LongSparseArray<PointerInputChange> = LongSparseArray(2)
    private var coordinates: LayoutCoordinates? = null
    private var pointerEvent: PointerEvent? = null
    private var wasIn = false
    private var isIn = true
    private var hasExited = true

    override fun removeInvalidPointerIdsAndChanges(
        pointerIdValue: Long,
        hitNodes: MutableObjectList<Node>
    ) {
        if (this.pointerIds.contains(pointerIdValue)) {
            if (!hitNodes.contains(this)) {
                this.pointerIds.remove(pointerIdValue)
                this.relevantChanges.remove(pointerIdValue)
            }
        }

        children.forEach { it.removeInvalidPointerIdsAndChanges(pointerIdValue, hitNodes) }
    }

    override fun dispatchMainEventPass(
        changes: LongSparseArray<PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        // TODO(b/158243568): The below dispatching operations may cause the pointerInputFilter to
        //  become detached. Currently, they just no-op if it becomes detached and the detached
        //  pointerInputFilters are removed from being tracked with the next event. I currently
        //  believe they should be detached immediately. Though, it is possible they should be
        //  detached after the conclusion of dispatch (so onCancel isn't called during calls
        //  to onPointerEvent). As a result we guard each successive dispatch with the same check.
        return dispatchIfNeeded {
            val event = pointerEvent!!
            val size = coordinates!!.size

            // Dispatch on the tunneling pass.
            modifierNode.dispatchForKind(Nodes.PointerInput) {
                it.onPointerEvent(event, PointerEventPass.Initial, size)
            }

            // Dispatch to children.
            if (modifierNode.isAttached) {
                children.forEach {
                    it.dispatchMainEventPass(
                        // Pass only the already-filtered and position-translated changes down to
                        // children
                        relevantChanges,
                        coordinates!!,
                        internalPointerEvent,
                        isInBounds
                    )
                }
            }

            if (modifierNode.isAttached) {
                // Dispatch on the bubbling pass.
                modifierNode.dispatchForKind(Nodes.PointerInput) {
                    it.onPointerEvent(event, PointerEventPass.Main, size)
                }
            }
        }
    }

    override fun dispatchFinalEventPass(internalPointerEvent: InternalPointerEvent): Boolean {
        // TODO(b/158243568): The below dispatching operations may cause the pointerInputFilter to
        //  become detached. Currently, they just no-op if it becomes detached and the detached
        //  pointerInputFilters are removed from being tracked with the next event. I currently
        //  believe they should be detached immediately. Though, it is possible they should be
        //  detached after the conclusion of dispatch (so onCancel isn't called during calls
        //  to onPointerEvent). As a result we guard each successive dispatch with the same check.
        val result = dispatchIfNeeded {
            val event = pointerEvent!!
            val size = coordinates!!.size
            // Dispatch on the tunneling pass.
            modifierNode.dispatchForKind(Nodes.PointerInput) {
                it.onPointerEvent(event, PointerEventPass.Final, size)
            }

            // Dispatch to children.
            if (modifierNode.isAttached) {
                children.forEach { it.dispatchFinalEventPass(internalPointerEvent) }
            }
        }
        cleanUpHits(internalPointerEvent)
        clearCache()
        return result
    }

    /**
     * Calculates cached properties that will be stored in this [Node] for the duration of both
     * [dispatchMainEventPass] and [dispatchFinalEventPass]. This allows us to avoid repeated work
     * between passes, and within passes, as these properties won't change during the overall
     * dispatch.
     *
     * @see clearCache
     */
    override fun buildCache(
        changes: LongSparseArray<PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        val childChanged =
            super.buildCache(changes, parentCoordinates, internalPointerEvent, isInBounds)

        // Avoid future work if we know this node will no-op
        if (!modifierNode.isAttached) return true

        modifierNode.dispatchForKind(Nodes.PointerInput) { coordinates = it.layoutCoordinates }

        @OptIn(ExperimentalComposeUiApi::class)
        for (j in 0 until changes.size()) {
            val keyValue = changes.keyAt(j)
            val change = changes.valueAt(j)

            if (pointerIds.contains(keyValue)) {
                val prevPosition = change.previousPosition
                val currentPosition = change.position

                if (prevPosition.isValid() && currentPosition.isValid()) {
                    // And translate their position relative to the parent coordinates, to give us a
                    // change local to the PointerInputFilter's coordinates
                    val historical = ArrayList<HistoricalChange>(change.historical.size)

                    change.historical.fastForEach {
                        val historicalPosition = it.position
                        // In some rare cases, historic data may have an invalid position, that is,
                        // Offset.Unspecified. In those cases, we don't want to include it in the
                        // data returned to the developer because the values are invalid.
                        if (historicalPosition.isValid()) {
                            historical.add(
                                HistoricalChange(
                                    it.uptimeMillis,
                                    coordinates!!.localPositionOf(
                                        parentCoordinates,
                                        historicalPosition
                                    ),
                                    it.originalEventPosition
                                )
                            )
                        }
                    }

                    relevantChanges.put(
                        keyValue,
                        change.copy(
                            previousPosition =
                                coordinates!!.localPositionOf(parentCoordinates, prevPosition),
                            currentPosition =
                                coordinates!!.localPositionOf(parentCoordinates, currentPosition),
                            historical = historical
                        )
                    )
                }
            }
        }

        if (relevantChanges.isEmpty()) {
            pointerIds.clear()
            children.clear()
            return true // not hit
        }

        // Clean up any pointerIds that weren't dispatched
        for (i in pointerIds.lastIndex downTo 0) {
            val pointerId = pointerIds[i]
            if (!changes.containsKey(pointerId.value)) {
                pointerIds.removeAt(i)
            }
        }

        val changesList = ArrayList<PointerInputChange>(relevantChanges.size())
        for (i in 0 until relevantChanges.size()) {
            changesList.add(relevantChanges.valueAt(i))
        }
        val event = PointerEvent(changesList, internalPointerEvent)

        val activeHoverChange =
            event.changes.fastFirstOrNull { internalPointerEvent.activeHoverEvent(it.id) }

        if (activeHoverChange != null) {
            if (!isInBounds) {
                isIn = false
            } else if (!isIn && (activeHoverChange.pressed || activeHoverChange.previousPressed)) {
                // We have to recalculate isIn because we didn't redo hit testing
                val size = coordinates!!.size
                @Suppress("DEPRECATION")
                isIn = !activeHoverChange.isOutOfBounds(size)
            }
            if (
                isIn != wasIn &&
                    (event.type == PointerEventType.Move ||
                        event.type == PointerEventType.Enter ||
                        event.type == PointerEventType.Exit)
            ) {
                event.type =
                    if (isIn) {
                        PointerEventType.Enter
                    } else {
                        PointerEventType.Exit
                    }
            } else if (event.type == PointerEventType.Enter && wasIn && !hasExited) {
                event.type = PointerEventType.Move // We already knew that it was in.
            } else if (event.type == PointerEventType.Exit && isIn && activeHoverChange.pressed) {
                event.type = PointerEventType.Move // We are still in.
            }
        }

        val changed =
            childChanged ||
                event.type != PointerEventType.Move ||
                hasPositionChanged(pointerEvent, event)
        pointerEvent = event
        return changed
    }

    private fun hasPositionChanged(oldEvent: PointerEvent?, newEvent: PointerEvent): Boolean {
        if (oldEvent == null || oldEvent.changes.size != newEvent.changes.size) {
            return true
        }
        for (i in 0 until newEvent.changes.size) {
            val old = oldEvent.changes[i]
            val current = newEvent.changes[i]
            if (old.position != current.position) {
                return true
            }
        }
        return false
    }

    /**
     * Resets cached properties in case this node will continue to track different [pointerIds] than
     * the ones we built the cache for, instead of being removed.
     *
     * @see buildCache
     */
    private fun clearCache() {
        relevantChanges.clear()
        coordinates = null
    }

    /**
     * Calls [block] if there are relevant changes, and if [modifierNode] is attached
     *
     * @return whether [block] was called
     */
    private inline fun dispatchIfNeeded(block: () -> Unit): Boolean {
        // If there are no relevant changes, there is nothing to process so return false.
        if (relevantChanges.isEmpty()) return false
        // If the input filter is not attached, avoid dispatching
        if (!modifierNode.isAttached) return false

        block()

        // We dispatched to at least one pointer input filter so return true.
        return true
    }

    // TODO(shepshapard): Should some order of cancel dispatch be guaranteed? I think the answer is
    //  essentially "no", but given that an order can be consistent... maybe we might as well
    //  set an arbitrary standard and stick to it so user expectations are maintained.
    /**
     * Does a depth first traversal and invokes [PointerInputFilter.onCancel] during backtracking.
     */
    override fun dispatchCancel() {
        children.forEach { it.dispatchCancel() }
        modifierNode.dispatchForKind(Nodes.PointerInput) { it.onCancelPointerInput() }
    }

    fun markIsIn() {
        isIn = true
    }

    override fun cleanUpHits(internalPointerEvent: InternalPointerEvent) {
        super.cleanUpHits(internalPointerEvent)

        val event = pointerEvent ?: return

        wasIn = isIn

        event.changes.fastForEach { change ->
            // There are two scenarios where we need to remove the pointerIds:
            //   1. Pointer is released AND event stream doesn't have an active hover.
            //   2. Pointer is released AND is released outside the area.
            val released = !change.pressed
            val nonHoverEventStream = !internalPointerEvent.activeHoverEvent(change.id)
            val outsideArea = !isIn

            val removePointerId = (released && nonHoverEventStream) || (released && outsideArea)

            if (removePointerId) {
                pointerIds.remove(change.id)
            }
        }

        isIn = false
        hasExited = event.type == PointerEventType.Exit
    }

    override fun toString(): String {
        return "Node(modifierNode=$modifierNode, children=$children, " + "pointerIds=$pointerIds)"
    }
}
