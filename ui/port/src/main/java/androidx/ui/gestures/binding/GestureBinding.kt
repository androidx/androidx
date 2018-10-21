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

package androidx.ui.gestures.binding

import androidx.ui.assert
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.window.Window
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.binding.BindingBase
import androidx.ui.foundation.debugPrint
import androidx.ui.gestures.debugPrintHitTestResults
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.hit_test.HitTestDispatcher
import androidx.ui.gestures.hit_test.HitTestEntry
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.gestures.hit_test.HitTestTarget
import androidx.ui.gestures.hit_test.HitTestable
import androidx.ui.ui.pointer.PointerDataPacket
import androidx.ui.gestures.events.PointerUpEvent
import kotlinx.coroutines.launch
import java.util.LinkedList
import androidx.ui.gestures.arena.GestureArenaManager
import androidx.ui.gestures.converter.PointerEventConverter
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.pointer_router.PointerRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach

open class GestureMixinsWrapper(
    base: BindingBase
) : BindingBase by base

// TODO(Migration/shepshapard): Need tests, which do some funny Mixin stuff that
// needs to be investigated.
class GestureBinding internal constructor(
    val window: Window,
    val base: BindingBase,
    val hitTestDelegate: HitTestable
) : GestureMixinsWrapper(base), HitTestable, HitTestDispatcher, HitTestTarget {

    // was initInstances
    init {
        GlobalScope.launch(Dispatchers.Unconfined) {
            window.onPointerDataPacket.openSubscription().consumeEach {
                _handlePointerDataPacket(it)
            }
        }
    }

    override fun unlocked() {
        super.unlocked()
        _flushPointerEventQueue()
    }

    val _pendingPointerEvents = LinkedList<PointerEvent>()

    fun _handlePointerDataPacket(packet: PointerDataPacket) {
        // We convert pointer data to logical pixels so that e.g. the touch slop can be
        // defined in a device-independent manner.
        _pendingPointerEvents.addAll(
            PointerEventConverter.expand(packet.data, window.devicePixelRatio)
        )
        if (!locked)
            _flushPointerEventQueue()
    }

    /**
     * Dispatch a [PointerCancelEvent] for the given pointer soon.
     *
     * The pointer event will be dispatch before the next pointer event and
     * before the end of the microtask but not within this function call.
     */
    fun cancelPointer(pointer: Int) {
        if (_pendingPointerEvents.isEmpty() && !locked)

        // TODO(shepshapard): ScheduleMicroTask
        // scheduleMicrotask(
            _flushPointerEventQueue()
        // );
        _pendingPointerEvents.addFirst(PointerCancelEvent(pointer = pointer))
    }

    fun _flushPointerEventQueue() {
        assert(!locked)
        while (_pendingPointerEvents.isNotEmpty())
            _handlePointerEvent(_pendingPointerEvents.removeFirst())
    }

    /** A router that routes all pointer events received from the engine. */
    val pointerRouter = PointerRouter()

    /**
     * The gesture arenas used for disambiguating the meaning of sequences of
     * pointer events.
     */
    val gestureArena = GestureArenaManager()

    /**
     * State for all pointers which are currently down.
     *
     * The state of hovering pointers is not tracked because that would require
     * hit-testing on every frame.
     */
    val _hitTests: MutableMap<Int, HitTestResult> = mutableMapOf()

    private fun _handlePointerEvent(event: PointerEvent) {
        assert(!locked)
        val result: HitTestResult?
        if (event is PointerDownEvent) {
            assert(!_hitTests.containsKey(event.pointer))
            result = HitTestResult()
            hitTest(result, event.position)
            _hitTests[event.pointer] = result
            assert {
                if (debugPrintHitTestResults)
                    debugPrint("$event: $result")
                true
            }
        } else if (event is PointerUpEvent || event is PointerCancelEvent) {
            result = _hitTests.remove(event.pointer)
        } else if (event.down) {
            result = _hitTests[event.pointer]
        } else {
            return // We currently ignore add, remove, and hover move events.
        }
        if (result != null)
            dispatchEvent(event, result)
    }

    /** Determine which [HitTestTarget] objects are located at a given position. */
    override fun hitTest(result: HitTestResult, position: Offset) {
        hitTestDelegate.hitTest(result, position)
        result.add(HitTestEntry(this))
    }

    /**
     * Dispatch an event to a hit test result's path.
     *
     * This sends the given event to every [HitTestTarget] in the entries
     * of the given [HitTestResult], and catches exceptions that any of
     * the handlers might throw. The `result` argument must not be null.
     */
    // from HitTestDispatcher
    override fun dispatchEvent(event: PointerEvent, result: HitTestResult) {
        assert(!locked)
        result.path.forEach {
            try {
                it.target.handleEvent(event, it)
            } catch (exception: Exception) {
                FlutterError.reportError(FlutterErrorDetailsForPointerEventDispatcher(
                    exception = exception,
                    stack = exception.stackTrace,
                    library = "gesture library",
                    context = "while dispatching a pointer event",
                    event = event,
                    hitTestEntry = it,
                    informationCollector = { information: StringBuffer ->
                        information.appendln("Event:")
                        information.appendln("  $event")
                        information.appendln("Target:")
                        information.append("  ${it.target}")
                    }
                ))
            }
        }
    }

    // from HitTestTarget
    override fun handleEvent(event: PointerEvent, entry: HitTestEntry) {
        pointerRouter.route(event)
        if (event is PointerDownEvent) {
            gestureArena.close(event.pointer)
        } else if (event is PointerUpEvent) {
            gestureArena.sweep(event.pointer)
        }
    }

    companion object {
        var instance: GestureBinding? = null
            private set

        fun initInstance(
            window: Window,
            base: BindingBase,
            hitTestDelegate: HitTestable
        ): GestureBinding {
            instance = instance ?: GestureBinding(window, base, hitTestDelegate)
            return instance!!
        }
    }
}