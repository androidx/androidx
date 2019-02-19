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
import androidx.ui.core.Density
import androidx.ui.core.LayoutNode
import androidx.ui.core.PointerInputNode
import androidx.ui.core.Position
import androidx.ui.core.Size
import androidx.ui.core.childToLocal
import androidx.ui.core.dp
import androidx.ui.core.localToGlobal
import androidx.ui.core.toPx
import androidx.ui.engine.geometry.Offset

typealias PointerInputHandler = (PointerInputChange, PointerEventPass) -> PointerInputChange
private typealias PointerInputHandlerPath = List<PointerInputNode>

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
            previousPointerInputData[it.id] = it.pointerInputData
        }
        return PointerInputChangeEvent(pointerEvent.timeStamp, changes)
    }
}

/**
 * Applies offsets to [PointerInputChange]'s via the offset of the [LayoutNode] associated with the
 * [PointerInputNode] that is about to receive the [PointerInputChange].
 *
 * Call [offsetPointerInputChange] to apply the offset over time and call [reset] when the
 * incoming pointerInputChange is new and has not yet been processed by
 * [PointerInputChangeOffsetManager].
 */
private class PointerInputChangeOffsetManager(val density: Density) {
    private val positionZero = Position(0.dp, 0.dp)
    private val pointerInputNodeGlobalOffsets: MutableMap<PointerInputNode, Offset> = mutableMapOf()
    private val changeOffsets: MutableMap<Int, Offset> = mutableMapOf()

    fun reset(pointerInputNodePaths: Collection<PointerInputHandlerPath>) {
        changeOffsets.clear()
        pointerInputNodeGlobalOffsets.clear()

        // Discover the global positions of PointerInputNodes and cache them
        var previousParentLayoutNode: LayoutNode? = null
        var previousOffset = Offset(0f, 0f)
        pointerInputNodePaths.flatten().forEach {
            if (!pointerInputNodeGlobalOffsets.containsKey(it)) {
                val parentLayoutNode = it.parentLayoutNode
                val offset: Offset = previousOffset +
                        when (parentLayoutNode) {
                            null -> Offset.zero
                            previousParentLayoutNode -> Offset.zero
                            else -> {
                                val position = previousParentLayoutNode?.childToLocal(
                                    parentLayoutNode,
                                    positionZero
                                ) ?: parentLayoutNode.localToGlobal(positionZero)
                                Offset(position.x.toPx(density), position.y.toPx(density))
                            }
                        }
                pointerInputNodeGlobalOffsets[it] = offset
                previousOffset = offset
                previousParentLayoutNode = parentLayoutNode
            }
        }
    }

    fun offsetPointerInputChange(
        change: PointerInputChange,
        node: PointerInputNode
    ): PointerInputChange {
        val newOffset = pointerInputNodeGlobalOffsets[node]!!
        val oldOffset = changeOffsets.getOrPut(change.id) {
            Offset.zero
        }
        val offsetDiff = newOffset - oldOffset
        changeOffsets[change.id] = newOffset
        if (offsetDiff != Offset.zero) {
            return change.subtractOffset(offsetDiff)
        }
        return change
    }
}

/**
 * The core element that receives [PointerInputEvent]s and process them through Crane.
 */
internal class PointerInputEventProcessor(val density: Density, val root: LayoutNode) {

    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()
    private val offsetManager = PointerInputChangeOffsetManager(density)
    private val pointerInputHandlerPaths: MutableMap<Int, PointerInputHandlerPath> = mutableMapOf()

    /**
     * Receives [PointerInputEvent]s and process them through Crane.
     */
    fun process(pointerEvent: PointerInputEvent) {
        val pointerInputChangeEvent = pointerInputChangeEventProducer.produce(pointerEvent)
        addReceiversDueToDownEvents(pointerInputChangeEvent)
        dispatchToReceivers(pointerInputChangeEvent)
        removeReceiversDueToUpEvents(pointerInputChangeEvent)
    }

    private fun addReceiversDueToDownEvents(pointerInputChangeEvent: PointerInputChangeEvent) {
        pointerInputChangeEvent.changes.filter { it.changedToDownIgnoreConsumed() }.forEach {
            val hitResult: MutableList<PointerInputNode> = mutableListOf()
            hitTestOnChildren(root, it.current.position!!, root.size, hitResult)
            pointerInputHandlerPaths[it.id] = hitResult
        }
    }

    private fun dispatchToReceivers(pointerInputChangeEvent: PointerInputChangeEvent) {
        offsetManager.reset(pointerInputHandlerPaths.values)

        pointerInputChangeEvent.changes.forEach { pointerInputChange ->
            val pointerInputHandlerPath =
                pointerInputHandlerPaths[pointerInputChange.id] ?: return@forEach

            // Forwards is from child to parent
            val parentToChild = pointerInputHandlerPath
            val childtoParent = parentToChild.reversed()
            var change = pointerInputChange

            // TODO(b/124523868): PointerInputNodes should opt into passes prevent having to visit
            // each one for every PointerInputChange.

            // Down from parent to child
            change = parentToChild.dispatchChange(change, PointerEventPass.InitialDown)
            // PrePass up (hacky up path of onNestedPreScroll)
            change = childtoParent.dispatchChange(change, PointerEventPass.PreUp)
            // Pre-pass down (onNestedPreScroll)
            change = parentToChild.dispatchChange(change, PointerEventPass.PreDown)
            // Post-pass up (onNestedScroll)
            change = childtoParent.dispatchChange(change, PointerEventPass.PostUp)
            // Post-pass down (hacky down path of onNestedScroll)
            parentToChild.dispatchChange(change, PointerEventPass.PostDown)
        }
    }

    private fun PointerInputHandlerPath.dispatchChange(
        pointerInputChange: PointerInputChange,
        pass: PointerEventPass
    ): PointerInputChange {
        var change = pointerInputChange
        forEach {
            change = offsetManager.offsetPointerInputChange(change, it)
            change = it.pointerInputHandler(change, pass)
        }
        return change
    }

    private fun removeReceiversDueToUpEvents(pointerInputChangeEvent: PointerInputChangeEvent) {
        pointerInputChangeEvent.changes.filter { it.changedToUpIgnoreConsumed() }.forEach {
            pointerInputHandlerPaths.remove(it.id)
        }
    }

    // TODO(shepshapard): Should siblings both be able to receive input?  Right now, siblings
    // do not even block each other if they are overlapping.
    /**
     * Carries out hit testing among the [parent]'s children, and then recursively on the
     * children's children, and so on.  Hit tests are only carried out on [PointerInputNode]s.
     *
     * When a [PointerInputNode] is hit, it's [PointerInputNode.pointerInputHandler] is added to
     * [hitTestResult] where parents are added before children.
     */
    private fun hitTestOnChildren(
        parent: ComponentNode,
        offset: Offset,
        size: Size,
        hitPointerInputNodes: MutableList<PointerInputNode>
    ) {
        parent.visitChildren { child ->
            // If the child is a PointerInputNode, then hit test on that child.
            if (child is PointerInputNode) {
                val parentSize = size.toPx(density)
                if (offset.dx >= 0 &&
                    offset.dx < parentSize.width &&
                    offset.dy >= 0 &&
                    offset.dy < parentSize.height
                ) {
                    hitPointerInputNodes.add(child)
                }
            }

            // Keep going down the tree looking for PointerInputNodes to hit test.

            // If this child is a LayoutNode, do 2 things:
            // 1. Update the offset to be relative to that LayoutNode
            val newOffset =
                if (child is LayoutNode) {
                    Offset(offset.dx - child.x.toPx(density), offset.dy - child.y.toPx(density))
                } else offset
            // 2. Update the size to be the size of the LayoutNode
            val newSize =
                if (child is LayoutNode) {
                    child.size
                } else size

            hitTestOnChildren(child, newOffset, newSize, hitPointerInputNodes)
        }
    }
}