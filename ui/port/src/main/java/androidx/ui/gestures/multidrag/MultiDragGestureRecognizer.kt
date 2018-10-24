/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.multidrag

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.binding.GestureBinding
import androidx.ui.gestures.drag.Drag
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.recognizer.GestureRecognizer

/**
 * Recognizes movement on a per-pointer basis.
 *
 * In contrast to [DragGestureRecognizer], [MultiDragGestureRecognizer] watches
 * each pointer separately, which means multiple drags can be recognized
 * concurrently if multiple pointers are in contact with the screen.
 *
 * [MultiDragGestureRecognizer] is not intended to be used directly. Instead,
 * consider using one of its subclasses to recognize specific types for drag
 * gestures.
 *
 * See also:
 *
 *  * [ImmediateMultiDragGestureRecognizer], the most straight-forward variant
 *    of multi-pointer drag gesture recognizer.
 *  * [HorizontalMultiDragGestureRecognizer], which only recognizes drags that
 *    start horizontally.
 *  * [VerticalMultiDragGestureRecognizer], which only recognizes drags that
 *    start vertically.
 *  * [DelayedMultiDragGestureRecognizer], which only recognizes drags that
 *    start after a long-press gesture.
 */
abstract class MultiDragGestureRecognizer<T : MultiDragPointerState>(debugOwner: Any?) :
    GestureRecognizer(debugOwner) {

    /**
     * Called when this class recognizes the start of a drag gesture.
     *
     * The remaining notifications for this drag gesture are delivered to the
     * [Drag] object returned by this callback.
     */
    var onStart: GestureMultiDragStartCallback? = null

    private val pointers: MutableMap<Int, T> = mutableMapOf()

    override fun addPointer(event: PointerDownEvent) {
        assert(!pointers.containsKey(event.pointer))
        val state: T = createNewPointerState(event)
        pointers[event.pointer] = state
        GestureBinding.instance!!.pointerRouter.addRoute(event.pointer, ::_handleEvent)
        state._setArenaEntry(GestureBinding.instance!!.gestureArena.add(event.pointer, this))
    }

    /**
     * Subclasses should override this method to create per-pointer state
     * objects to track the pointer associated with the given event.
     */
    internal abstract fun createNewPointerState(event: PointerDownEvent): T

    private fun _handleEvent(event: PointerEvent) {
        assert(pointers.containsKey(event.pointer))
        val state: T = pointers[event.pointer]!!
        if (event is PointerMoveEvent) {
            state.move(event)
            // We might be disposed here.
        } else if (event is PointerUpEvent) {
            assert(event.delta == Offset.zero)
            state.up()
            // We might be disposed here.
            _removeState(event.pointer)
        } else if (event is PointerCancelEvent) {
            assert(event.delta == Offset.zero)
            state.cancel()
            // We might be disposed here.
            _removeState(event.pointer)
        } else if (event !is PointerDownEvent) {
            // we get the PointerDownEvent that resulted in our addPointer getting called since we
            // add ourselves to the pointer router then (before the pointer router has heard of
            // the event).
            assert(false)
        }
    }

    override fun acceptGesture(pointer: Int) {
        // We might already have canceled this drag if the up comes before the accept.
        val state = pointers[pointer] ?: return
        state.accepted { initialPosition -> _startDrag(initialPosition, pointer) }
    }

    internal fun _startDrag(initialPosition: Offset, pointer: Int): Drag? {
        val state = pointers[pointer]
        assert(state != null)
        assert(state!!.pendingDelta != null)
        val drag: Drag? = onStart?.let {
            invokeCallback<Drag>("onStart", { it(initialPosition) })
        }
        if (drag != null) {
            state.startDrag(drag)
        } else {
            _removeState(pointer)
        }
        return drag
    }

    override fun rejectGesture(pointer: Int) {
        if (pointers.containsKey(pointer)) {
            val state = pointers[pointer]
            assert(state != null)
            state!!.rejected()
            _removeState(pointer)
        } // else we already preemptively forgot about it (e.g. we got an up event)
    }

    private fun _removeState(pointer: Int) {
        pointers.let {
            assert(it.containsKey(pointer))
            GestureBinding.instance!!.pointerRouter.removeRoute(pointer, ::_handleEvent)
            it.remove(pointer)!!.dispose()
        }
        // Else, we've already been disposed. It's harmless to skip removing the state
        // for the given pointer because dispose() has already removed it.
    }

    override fun dispose() {
        pointers.keys.toList().forEach(::_removeState)
        assert(pointers.isEmpty())
        super.dispose()
    }
}