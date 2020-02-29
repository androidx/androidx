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

import androidx.ui.core.LayoutNode
import androidx.ui.core.PointerEventPass
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed

/**
 * The core element that receives [PointerInputEvent]s and process them in Compose UI.
 */
internal class PointerInputEventProcessor2(val root: LayoutNode) {

    private val hitPathTracker = HitPathTracker2()
    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()

    /**
     * Receives [PointerInputEvent]s and process them through the tree rooted on [root].
     *
     * @param pointerEvent The [PointerInputEvent] to process.
     */
    fun process(pointerEvent: PointerInputEvent) {

        // Gets a new PointerInputChangeEvent with the PointerInputEvent.
        val pointerInputChangeEvent =
            pointerInputChangeEventProducer.produce(pointerEvent)

        // Add new hit paths to the tracker due to down events.
        pointerInputChangeEvent.changes.filter { it.changedToDownIgnoreConsumed() }.forEach {
            val hitResult: MutableList<PointerInputFilter> = mutableListOf()
            root.hitTest(
                it.current.position!!,
                hitResult
            )
            hitPathTracker.addHitPath(it.id, hitResult)
        }

        // Remove [PointerInputFilter]s that are no longer valid and refresh the offset information
        // for those that are.
        hitPathTracker.removeDetachedPointerInputFilters()

        // Dispatch the PointerInputChanges to the hit PointerInputFilters.
        var changes = pointerInputChangeEvent.changes
        hitPathTracker.apply {
            changes = dispatchChanges(changes, PointerEventPass.InitialDown, PointerEventPass.PreUp)
            changes = dispatchChanges(changes, PointerEventPass.PreDown, PointerEventPass.PostUp)
            dispatchChanges(changes, PointerEventPass.PostDown)
        }

        // Remove hit paths from the tracker due to up events.
        pointerInputChangeEvent.changes.filter { it.changedToUpIgnoreConsumed() }.forEach {
            hitPathTracker.removeHitPath(it.id)
        }
    }

    /**
     * Responds appropriately to Android ACTION_CANCEL events.
     *
     * Specifically, [PointerInputFilter.cancelHandler] is invoked on tracked [PointerInputFilter]s and
     * and this [PointerInputEventProcessor] is reset such that it is no longer tracking any
     * [PointerInputFilter]s and expects the next [PointerInputEvent] it processes to represent only
     * new pointers.
     */
    fun processCancel() {
        pointerInputChangeEventProducer.clear()
        hitPathTracker.processCancel()
    }
}