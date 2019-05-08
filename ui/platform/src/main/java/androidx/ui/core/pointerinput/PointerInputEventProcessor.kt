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

package androidx.ui.core.pointerinput

import androidx.ui.core.ComponentNode
import androidx.ui.core.ConsumedData
import androidx.ui.core.DrawNode
import androidx.ui.core.LayoutNode
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputNode
import androidx.ui.core.PxPosition
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.core.Timestamp
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed

/**
 * The core element that receives [PointerInputEvent]s and process them in Compose UI.
 */
internal class PointerInputEventProcessor(val root: LayoutNode) {

    private val hitPathTracker = HitPathTracker()
    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()

    /**
     * Receives [PointerInputEvent]s and process them through the tree rooted on [root].
     */
    fun process(pointerEvent: PointerInputEvent) {

        // Gets a new PointerInputChangeEvent with the PointerInputEvent.
        val pointerInputChangeEvent = pointerInputChangeEventProducer.produce(pointerEvent)

        // Add new hit paths to the tracker due to down events.
        pointerInputChangeEvent.changes.filter { it.changedToDownIgnoreConsumed() }.forEach {
            val hitResult: MutableList<PointerInputNode> = mutableListOf()
            root.hitTestOnDescendants(
                it.current.position!!,
                hitResult
            )
            hitPathTracker.addHitPath(it.id, hitResult)
        }

        // Remove PointerInputNodes that are detached.
        hitPathTracker.removeDetachedPointerInputNodes()

        // Refresh offset information so changes are dispatched to PointerInputNodes with the
        // correct offsets.
        hitPathTracker.refreshOffsets()

        // Dispatch the PointerInputChanges to the hit PointerInputNodes.
        var changes = pointerInputChangeEvent.changes
        hitPathTracker.apply {
            changes = dispatchChanges(changes, PointerEventPass.InitialDown, PointerEventPass.PreUp)
            changes = dispatchChanges(changes, PointerEventPass.PreDown, PointerEventPass.PostUp)
            dispatchChanges(changes, PointerEventPass.PostDown)
        }

        // Remove hit paths from the tracker due to up events.
        pointerInputChangeEvent.changes.filter { it.changedToUpIgnoreConsumed() }.forEach {
            hitPathTracker.removePointerId(it.id)
        }
    }

    // TODO(b/131780534): This method should return true if a hit was made to prevent
    // overlapping PointerInputNodes that are descendants of children of the ComponentNode from
    // both being successfully hit.
    /**
     * Searches for [PointerInputNode]s among the [ComponentNode]'s descendants, determines if the
     * [offset] is within their virtual bounds, and adds them to [hitPointerInputNodes] if they are.
     *
     * This method actually just recursively searches for PointerInputNodes among the descendants
     * of the ComponentNode in a DFS in a reverse child order (so children that will be drawn on top
     * of their siblings will be checked first) and calls [hitTest] on them when found. If that
     * method returns true, it stops searching so that other PointerInputNodes that are drawn under
     * the hit PointerInputNode can't also be hit.
     */
    private fun ComponentNode.hitTestOnDescendants(
        offset: PxPosition,
        hitPointerInputNodes: MutableList<PointerInputNode>
    ) {
        var hitChildPointerInputNode = false

        // TODO(shepshapard): This visitChildren use is ugly since once we successfully hit a child
        // we have to continue to loop through the rest of the children event though we don't
        // actually need to.  Figure out a better call here.
        visitChildren(true) { child ->
            if (!hitChildPointerInputNode) {
                when (child) {
                    is PointerInputNode -> {
                        hitChildPointerInputNode = child.hitTest(offset, hitPointerInputNodes)
                    }
                    is LayoutNode -> {
                        val newOffset =
                            PxPosition(offset.x - child.x, offset.y - child.y)
                        child.hitTestOnDescendants(newOffset, hitPointerInputNodes)
                    }
                    else ->
                        child.hitTestOnDescendants(offset, hitPointerInputNodes)
                }
            }
        }
    }

    /**
     * Looks for the first descendant [LayoutNode] of the [PointerInputNode], tracking other
     * descendant PointerInputNodes as it goes, and adds all of the PointerInputNodes to
     * [hitPointerInputNodes] if [offset] is in bounds of the eventually discovered LayoutNode.
     * Then continues hit testing on descendants of the discovered LayoutNode.
     *
     * @return True if a PointerInputNode was hit (and thus was added to hitPointerInputNodes).
     */
    private fun PointerInputNode.hitTest(
        offset: PxPosition,
        hitPointerInputNodes: MutableList<PointerInputNode>
    ): Boolean {
        val pointerInputNodes = mutableSetOf(this)
        var child: ComponentNode? = child
        var nodeHit = false
        while (child != null) {
            child = when (child) {
                is PointerInputNode -> {
                    pointerInputNodes.add(child)
                    child.child
                }
                is LayoutNode -> {
                    val offsetX = offset.x.value
                    val offsetY = offset.y.value
                    val childX = child.x.value
                    val childY = child.y.value
                    if (offsetX >= childX &&
                        offsetX < childX + child.width.value &&
                        offsetY >= childY &&
                        offsetY < childY + child.height.value
                    ) {
                        nodeHit = true
                        hitPointerInputNodes.addAll(pointerInputNodes)
                    }
                    val newOffset =
                        PxPosition(offset.x - child.x, offset.y - child.y)
                    child.hitTestOnDescendants(newOffset, hitPointerInputNodes)
                    null
                }
                is SemanticsComponentNode -> {
                    child.child
                }
                is DrawNode -> {
                    null
                }
            }
        }
        return nodeHit
    }
}

/**
 * Produces [PointerInputChangeEvent]s by tracking changes between [PointerInputEvent]s
 */
private class PointerInputChangeEventProducer {
    private val previousPointerInputData: MutableMap<Int, PointerInputData> = mutableMapOf()

    internal fun produce(pointerEvent: PointerInputEvent):
            PointerInputChangeEvent {
        val changes: MutableList<PointerInputChange> = mutableListOf()
        pointerEvent.pointers.forEach {
            changes.add(
                PointerInputChange(
                    it.id,
                    it.pointerInputData,
                    previousPointerInputData[it.id] ?: PointerInputData(),
                    ConsumedData()
                )
            )
            if (it.pointerInputData.down) {
                previousPointerInputData[it.id] = it.pointerInputData
            } else {
                previousPointerInputData.remove(it.id)
            }
        }
        return PointerInputChangeEvent(pointerEvent.timestamp, changes)
    }
}

// TODO(shepshapard): The timestamp property probably doesn't need to exist (and therefore, nor does
// this class, but going to wait to refactor it out till after things like API review to avoid
// thrashing.
private data class PointerInputChangeEvent(
    val timestamp: Timestamp,
    val changes: List<PointerInputChange>
)