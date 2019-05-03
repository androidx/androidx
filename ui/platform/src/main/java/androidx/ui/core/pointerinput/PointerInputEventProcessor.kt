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
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed

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

/**
 * The core element that receives [PointerInputEvent]s and process them through Crane.
 */
internal class PointerInputEventProcessor(val root: LayoutNode) {

    private val hitPathTracker = HitPathTracker()
    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()

    /**
     * Receives [PointerInputEvent]s and process them through Crane.
     */
    fun process(pointerEvent: PointerInputEvent) {
        val pointerInputChangeEvent = pointerInputChangeEventProducer.produce(pointerEvent)
        addReceiversDueToDownEvents(pointerInputChangeEvent)
        removeDetachedReceivers()
        resolveLayoutOffsetInformation()
        dispatchToReceivers(pointerInputChangeEvent)
        removeReceiversDueToUpEvents(pointerInputChangeEvent)
    }

    private fun addReceiversDueToDownEvents(pointerInputChangeEvent: PointerInputChangeEvent) {
        pointerInputChangeEvent.changes.filter { it.changedToDownIgnoreConsumed() }.forEach {
            val hitResult: MutableList<PointerInputNode> = mutableListOf()
            hitTestOnDescendants(
                root,
                it.current.position!!,
                hitResult
            )
            hitPathTracker.addHitPath(it.id, hitResult)
        }
    }

    private fun removeDetachedReceivers() {
        hitPathTracker.removeDetachedPointerInputNodes()
    }

    private fun resolveLayoutOffsetInformation() {
        hitPathTracker.refreshOffsets()
    }

    private fun dispatchToReceivers(pointerInputChangeEvent: PointerInputChangeEvent) {
        var changes = pointerInputChangeEvent.changes
        hitPathTracker.apply {
            changes = dispatchChanges(changes, PointerEventPass.InitialDown, PointerEventPass.PreUp)
            changes = dispatchChanges(changes, PointerEventPass.PreDown, PointerEventPass.PostUp)
            dispatchChanges(changes, PointerEventPass.PostDown)
        }
    }

    private fun removeReceiversDueToUpEvents(pointerInputChangeEvent: PointerInputChangeEvent) {
        pointerInputChangeEvent.changes.filter { it.changedToUpIgnoreConsumed() }.forEach {
            hitPathTracker.removePointerId(it.id)
        }
    }

    /**
     * Searches for [PointerInputNode]s among the descendants of [parent], determines if the
     * [offset] is within their virtual bounds, and adds them to [hitPointerInputNodes] if they are.
     *
     * This method actually just recursively searches for [PointerInputNode]s among its decedents
     * in a DFS in reverse child order (so children that will be drawn on top of it's siblings will
     * be checked first) and calls [hitTest], passing them in when found.  If that method returns
     * true, it stops looking so [PointerInputNode]s that are under other [PointerInputNode]s won't
     * receive [PointerInputChange]s.
     */
    private fun hitTestOnDescendants(
        parent: ComponentNode,
        offset: PxPosition,
        hitPointerInputNodes: MutableList<PointerInputNode>
    ) {
        var done = false
        parent.visitChildren(true) { child ->
            if (!done) {
                when (child) {
                    is PointerInputNode -> {
                        if (hitTest(child, offset, hitPointerInputNodes)) {
                            done = true
                        }
                    }
                    is LayoutNode -> {
                        val newOffset =
                            PxPosition(offset.x - child.x, offset.y - child.y)
                        hitTestOnDescendants(child, newOffset, hitPointerInputNodes)
                    }
                    else ->
                        hitTestOnDescendants(child, offset, hitPointerInputNodes)
                }
            }
        }
    }

    /**
     * Looks for the first descendant [LayoutNode] of [pointerInputNode], tracking other
     * descendant [PointerInputNode]s as it looks, and adds all of the [PointerInputNode]s to
     * [hitPointerInputNodes] if [offset] is in bounds of the eventually discovered [LayoutNode].
     * Then continues hit testing on descendants of the discovered [LayoutNode].
     *
     * @return True if a [PointerInputNode] was added to [hitPointerInputNodes].
     */
    private fun hitTest(
        pointerInputNode: PointerInputNode,
        offset: PxPosition,
        hitPointerInputNodes: MutableList<PointerInputNode>
    ): Boolean {
        val pointerInputNodes = mutableSetOf(pointerInputNode)
        var child: ComponentNode? = pointerInputNode.child
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
                    hitTestOnDescendants(child, newOffset, hitPointerInputNodes)
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