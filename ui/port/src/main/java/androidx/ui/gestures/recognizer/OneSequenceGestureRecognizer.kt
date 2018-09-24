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

package androidx.ui.gestures.recognizer

import androidx.annotation.CallSuper
import androidx.ui.gestures.arena.GestureArenaEntry
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.binding.GestureBinding
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.team.GestureArenaTeam

/**
 * Base class for gesture recognizers that can only recognize one
 * gesture at a time. For example, a single [TapGestureRecognizer]
 * can never recognize two taps happening simultaneously, even if
 * multiple pointers are placed on the same widget.
 *
 * This is in contrast to, for instance, [MultiTapGestureRecognizer],
 * which manages each pointer independently and can consider multiple
 * simultaneous touches to each result in a separate tap.
 */
abstract class OneSequenceGestureRecognizer(
    debugOwner: Any? = null
) : GestureRecognizer(debugOwner) {

    private val _entries: MutableMap<Int, GestureArenaEntry> = mutableMapOf()
    private val _trackedPointers: MutableSet<Int> = mutableSetOf()

    /** Called when a pointer event is routed to this recognizer. */
    protected abstract fun handleEvent(event: PointerEvent)

    override fun acceptGesture(pointer: Int) {}

    override fun rejectGesture(pointer: Int) {}

    /**
     * Called when the number of pointers this recognizer is tracking changes from one to zero.
     *
     * The given pointer ID is the ID of the last pointer this recognizer was
     * tracking.
     */
    protected abstract fun didStopTrackingLastPointer(pointer: Int)

    /**
     * Resolves this recognizer's participation in each gesture arena with the
     * given disposition.
     */
    @CallSuper
    protected open fun resolve(disposition: GestureDisposition) {
        val localEntries: List<GestureArenaEntry> = _entries.values.toList()
        _entries.clear()
        localEntries.forEach {
            it.resolve(disposition)
        }
    }

    override fun dispose() {
        resolve(GestureDisposition.rejected)
        _trackedPointers.forEach {
            GestureBinding.instance!!.pointerRouter.removeRoute(it, ::handleEvent)
        }
        _trackedPointers.clear()
        assert(_entries.isEmpty())
        super.dispose()
    }

    /**
     * The team that this recognizer belongs to, if any.
     *
     * If [team] is null, this recognizer competes directly in the
     * [GestureArenaManager] to recognize a sequence of pointer events as a
     * gesture. If [team] is non-null, this recognizer competes in the arena in
     * a group with other recognizers on the same team.
     *
     * A recognizer can be assigned to a team only when it is not participating
     * in the arena. For example, a common time to assign a recognizer to a team
     * is shortly after creating the recognizer.
     */
    var team: GestureArenaTeam? = null
        /** The [team] can only be set once. */
        set(value) {
            assert(value != null)
            assert(_entries.isEmpty())
            assert(_trackedPointers.isEmpty())
            assert(field == null)
            field = value
        }

    private fun _addPointerToArena(pointer: Int): GestureArenaEntry {
        team?.let {
            return it.add(pointer, this)
        }
        return GestureBinding.instance!!.gestureArena.add(pointer, this)
    }

    /**
     * Causes events related to the given pointer ID to be routed to this recognizer.
     *
     * The pointer events are delivered to [handleEvent].
     *
     * Use [stopTrackingPointer] to remove the route added by this function.
     */
    protected fun startTrackingPointer(pointer: Int) {
        GestureBinding.instance!!.pointerRouter.addRoute(pointer, ::handleEvent)
        _trackedPointers.add(pointer)
        assert(!_entries.containsKey(pointer))
        _entries[pointer] = _addPointerToArena(pointer)
    }

    /**
     * Stops events related to the given pointer ID from being routed to this recognizer.
     *
     * If this function reduces the number of tracked pointers to zero, it will
     * call [didStopTrackingLastPointer] synchronously.
     *
     * Use [startTrackingPointer] to add the routes in the first place.
     */
    protected fun stopTrackingPointer(pointer: Int) {
        if (_trackedPointers.contains(pointer)) {
            GestureBinding.instance!!.pointerRouter.removeRoute(pointer, ::handleEvent)
            _trackedPointers.remove(pointer)
            if (_trackedPointers.isEmpty())
                didStopTrackingLastPointer(pointer)
        }
    }

    /**
     * Stops tracking the pointer associated with the given event if the event is
     * a [PointerUpEvent] or a [PointerCancelEvent] event.
     */
    protected fun stopTrackingIfPointerNoLongerDown(event: PointerEvent) {
        if (event is PointerUpEvent || event is PointerCancelEvent)
            stopTrackingPointer(event.pointer)
    }
}