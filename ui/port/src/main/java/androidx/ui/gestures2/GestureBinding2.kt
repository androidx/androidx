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

package androidx.ui.gestures2

import androidx.ui.engine.window.Window
import androidx.ui.foundation.binding.BindingBase
import androidx.ui.foundation.debugPrint
import androidx.ui.gestures.debugPrintHitTestResults
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.gestures.hit_test.HitTestable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.LinkedList

enum class PointerEventPass {
    INITIAL_DOWN, PRE_UP, PRE_DOWN, POST_UP, POST_DOWN
}

open class GestureMixinsWrapper(
    base: BindingBase
) : BindingBase by base

class GestureBinding2 internal constructor(
    val window: Window,
    val base: BindingBase,
    private val hitTestDelegate: HitTestable,
    var scope: CoroutineScope
) : GestureMixinsWrapper(base) {

    init {
        scope.launch {
            window.onPointerDataPacket.consumeEach {
                pendingPointerEvents.addAll(
                    PointerEventConverter2.expand(it.data)
                )
                if (!locked)
                    flushPointerEventQueue()
            }
        }
    }

    private val pendingPointerEvents = LinkedList<PointerEvent>()
    private val hitTests: MutableMap<Int, HitTestResult> = mutableMapOf()
    private val pointerEvents: MutableMap<Int, PointerEvent2> = mutableMapOf()

    override fun unlocked() {
        super.unlocked()
        flushPointerEventQueue()
    }

    /**
     * Dispatch a [PointerCancelEvent] for the given pointer soon.
     *
     * The pointer event will be dispatch before the next pointer event and
     * before the end of the microtask but not within this function call.
     */
    fun cancelPointer(pointer: Int) {
        if (pendingPointerEvents.isEmpty() && !locked)

        // TODO(shepshapard): ScheduleMicroTask
        // scheduleMicrotask(
            flushPointerEventQueue()
        // );
        pendingPointerEvents.addFirst(PointerCancelEvent(pointer = pointer))
    }

    private fun flushPointerEventQueue() {
        assert(!locked)
        while (pendingPointerEvents.isNotEmpty()) {

            val event = pendingPointerEvents.removeFirst()

            val pointerId = event.pointer
            val pointerEvent2: PointerEvent2?
            val result: HitTestResult?

            if (event is PointerDownEvent) {
                // Create the pointer event and add it to the tracking set
                pointerEvent2 = PointerEvent2(event)
                pointerEvents[pointerId] = pointerEvent2

                // Carry out a hit test to determine which render objects are being touched
                assert(!hitTests.containsKey(pointerId))
                result = HitTestResult()
                hitTestDelegate.hitTest(result, event.position)
                hitTests[pointerId] = result
                androidx.ui.assert {
                    if (debugPrintHitTestResults)
                        debugPrint("$event: $result")
                    true
                }
            } else if (event is PointerUpEvent || event is PointerCancelEvent) {
                // Remove hit test and pointer event, and update pointer event.
                result = hitTests.remove(pointerId)
                pointerEvent2 = pointerEvents.remove(pointerId)?.update(event)
            } else if (event.down) {
                result = hitTests[pointerId]
                pointerEvent2 = pointerEvents[pointerId]?.update(event)?.also {
                    if (pointerId == 1) println("gb pos change: " + it.positionChange().dy)
                    pointerEvents[pointerId] = it
                }
            } else {
                return // We currently ignore add, remove, and hover move events.
            }
            // TODO(shepshapard): Error in response to pointer id not existing
            if (result != null && pointerEvent2 != null)
                dispatchEvent(pointerEvent2, result)
        }
    }

    private fun dispatchEvent(event: PointerEvent2, result: HitTestResult) {
        assert(!locked)
        // Forwards is from child to parent
        val childtoParent = result.path
        val parentToChild = childtoParent.reversed()
        var event = event
        // Down from parent to child
        parentToChild.forEach {
            event = it.target.handleEvent(event, it, PointerEventPass.INITIAL_DOWN)
        }
        // PrePass up (hacky up path of onNestedPreScroll)
        childtoParent.forEach {
            event = it.target.handleEvent(event, it, PointerEventPass.PRE_UP)
        }
        // Pre-pass down (onNestedPreScroll)
        parentToChild.forEach {
            event = it.target.handleEvent(event, it, PointerEventPass.PRE_DOWN)
        }
        // Post-pass up (onNestedScroll)
        childtoParent.forEach {
            event = it.target.handleEvent(event, it, PointerEventPass.POST_UP)
        }
        // Post-pass down (hacky down path of onNestedScroll)
        parentToChild.forEach {
            event = it.target.handleEvent(event, it, PointerEventPass.POST_DOWN)
        }
    }

    companion object {
        var instance: GestureBinding2? = null
            private set

        fun initInstance(
            window: Window,
            base: BindingBase,
            hitTestDelegate: HitTestable,
            scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
        ): GestureBinding2 {
            instance = instance ?: GestureBinding2(window, base, hitTestDelegate, scope)
            return instance!!
        }
    }
}