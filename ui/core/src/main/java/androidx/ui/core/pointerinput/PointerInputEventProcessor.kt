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

import android.content.Context
import androidx.ui.core.ComponentNode
import androidx.ui.core.LayoutNode
import androidx.ui.core.PointerInputNode
import androidx.ui.core.Size
import androidx.ui.core.toPx
import androidx.ui.engine.geometry.Offset

typealias PointerInputHandler = (PointerInputChange, PointerEventPass) -> PointerInputChange
private typealias PointerInputHandlerPath = List<PointerInputHandler>

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
 * The core element that receives [PointerInputEvent]s and process them through Crane.
 */
internal class PointerInputEventProcessor(val context: Context, val root: LayoutNode) {

    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()
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
            val hitTestResult: MutableList<PointerInputHandler> = mutableListOf()
            hitTestOnChildren(root, it.current.position!!, root.size, hitTestResult)
            pointerInputHandlerPaths[it.id] = hitTestResult
        }
    }

    private fun dispatchToReceivers(pointerInputChangeEvent: PointerInputChangeEvent) {
        pointerInputChangeEvent.changes.forEach { pointerInputChange ->
            val pointerInputHandlerPath =
                pointerInputHandlerPaths[pointerInputChange.id] ?: return@forEach

            // Forwards is from child to parent
            val parentToChild = pointerInputHandlerPath
            val childtoParent = parentToChild.reversed()
            var change = pointerInputChange

            // Down from parent to child
            parentToChild.forEach {
                change = it(change, PointerEventPass.InitialDown)
            }
            // PrePass up (hacky up path of onNestedPreScroll)
            childtoParent.forEach {
                change = it(change, PointerEventPass.PreUp)
            }
            // Pre-pass down (onNestedPreScroll)
            parentToChild.forEach {
                change = it(change, PointerEventPass.PreDown)
            }
            // Post-pass up (onNestedScroll)
            childtoParent.forEach {
                change = it(change, PointerEventPass.PostUp)
            }
            // Post-pass down (hacky down path of onNestedScroll)
            parentToChild.forEach {
                change = it(change, PointerEventPass.PostDown)
            }
        }
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
        hitTestResult: MutableList<PointerInputHandler>
    ) {
        parent.visitChildren { child ->
            // If the child is a PointerInputNode, then hit test on that child.
            if (child is PointerInputNode) {
                val parentSize = size.toPx(context)
                if (offset.dx >= 0 &&
                    offset.dx < parentSize.width &&
                    offset.dy >= 0 &&
                    offset.dy < parentSize.height
                ) {
                    hitTestResult.add(child.pointerInputHandler)
                }
            }

            // Keep going down the tree looking for PointerInputNodes to hit test.

            // If this child is a LayoutNode, do 2 things:
            // 1. Update the offset to be relative to that LayoutNode
            val newOffset =
                if (child is LayoutNode) {
                    Offset(offset.dx - child.x.toPx(context), offset.dy - child.y.toPx(context))
                } else offset
            // 2. Update the size to be the size of the LayoutNode
            val newSize =
                if (child is LayoutNode) {
                    child.size
                } else size

            hitTestOnChildren(child, newOffset, newSize, hitTestResult)
        }
    }
}