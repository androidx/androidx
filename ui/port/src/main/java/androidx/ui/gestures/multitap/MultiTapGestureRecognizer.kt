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

package androidx.ui.gestures.multitap

import androidx.ui.async.Timer
import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.binding.GestureBinding
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.kTouchSlop
import androidx.ui.gestures.pointer_router.PointerRoute
import androidx.ui.gestures.recognizer.GestureRecognizer
import androidx.ui.gestures.tap.TapDownDetails
import androidx.ui.gestures.tap.TapUpDetails

/**
 * Signature used by [MultiTapGestureRecognizer] for when a pointer that might
 * cause a tap has contacted the screen at a particular location.
 */
typealias GestureMultiTapDownCallback = (pointer: Int, details: TapDownDetails) -> Unit

/**
 * Signature used by [MultiTapGestureRecognizer] for when a pointer that will
 * trigger a tap has stopped contacting the screen at a particular location.
 */
typealias GestureMultiTapUpCallback = (pointer: Int, details: TapUpDetails) -> Unit

/** Signature used by [MultiTapGestureRecognizer] for when a tap has occurred. */
typealias GestureMultiTapCallback = (pointer: Int) -> Unit

/**
 * Signature for when the pointer that previously triggered a
 * [GestureMultiTapDownCallback] will not end up causing a tap.
 */
typealias GestureMultiTapCancelCallback = (pointer: Int) -> Unit

/**
 * Recognizes taps on a per-pointer basis.
 *
 * [MultiTapGestureRecognizer] considers each sequence of pointer events that
 * could constitute a tap independently of other pointers: For example, down-1,
 * down-2, up-1, up-2 produces two taps, on up-1 and up-2.
 *
 * See also:
 *
 *  * [TapGestureRecognizer]
 */
class MultiTapGestureRecognizer(
    /**
     * The amount of time between [onTapDown] and [onLongTapDown].
     *
     * The [longTapDelay] defaults to [Duration.zero], which means
     * [onLongTapDown] is called immediately after [onTapDown].
     */
    val longTapDelay: Duration = Duration.zero,
    debugOwner: Any? = null
) : GestureRecognizer(debugOwner = debugOwner) {

    /**
     * A pointer that might cause a tap has contacted the screen at a particular
     * location.
     */
    var onTapDown: GestureMultiTapDownCallback? = null

    /**
     * A pointer that will trigger a tap has stopped contacting the screen at a
     * particular location.
     */
    var onTapUp: GestureMultiTapUpCallback? = null

    /** A tap has occurred. */
    var onTap: GestureMultiTapCallback? = null

    /**
     * The pointer that previously triggered [onTapDown] will not end up causing
     * a tap.
     */
    var onTapCancel: GestureMultiTapCancelCallback? = null

    /**
     * A pointer that might cause a tap is still in contact with the screen at a
     * particular location after [longTapDelay].
     */
    var onLongTapDown: GestureMultiTapDownCallback? = null

    private val gestureMap: MutableMap<Int, TapGesture> = mutableMapOf()

    override fun addPointer(event: PointerDownEvent) {
        assert(!gestureMap.containsKey(event.pointer))
        gestureMap[event.pointer] = TapGesture(
            gestureRecognizer = this,
            event = event,
            longTapDelay = longTapDelay
        )
        onTapDown?.let {
            invokeCallback("onTapDown", {
                it(
                    event.pointer, TapDownDetails(
                        globalPosition = event.position
                    )
                )
            })
        }
    }

    override fun acceptGesture(pointer: Int) {
        assert(gestureMap.containsKey(pointer))
        gestureMap[pointer]!!.accept()
    }

    override fun rejectGesture(pointer: Int) {
        assert(gestureMap.containsKey(pointer))
        gestureMap[pointer]!!.reject()
        assert(!gestureMap.containsKey(pointer))
    }

    internal fun dispatchCancel(pointer: Int) {
        assert(gestureMap.containsKey(pointer))
        gestureMap.remove(pointer)

        onTapCancel?.let {
            invokeCallback("onTapCancel", {
                it(pointer)
            })
        }
    }

    internal fun dispatchTap(pointer: Int, globalPosition: Offset) {
        assert(gestureMap.containsKey(pointer))
        gestureMap.remove(pointer)

        onTapUp?.let {
            invokeCallback("onTapUp", {
                it(pointer, TapUpDetails(globalPosition = globalPosition))
            })
        }

        onTap?.let {
            invokeCallback("onTap", {
                it(pointer)
            })
        }
    }

    internal fun dispatchLongTap(pointer: Int, lastPosition: Offset) {
        assert(gestureMap.containsKey(pointer))
        onLongTapDown?.let {
            invokeCallback("onLongTapDown", {
                it(pointer, TapDownDetails(globalPosition = lastPosition))
            })
        }
    }

    override fun dispose() {
        val localGestures: MutableList<TapGesture> = gestureMap.values.toMutableList()
        localGestures.forEach {
            it.cancel()
        }
        // Rejection of each gesture should cause it to be removed from our map
        assert(gestureMap.isEmpty())
        super.dispose()
    }

    override val debugDescription = "multitap"
}

/**
 * TapGesture represents a full gesture resulting from a single tap sequence,
 * as part of a [MultiTapGestureRecognizer]. Tap gestures are passive, meaning
 * that they will not preempt any other arena member in play.
 */
private class TapGesture(
    val gestureRecognizer: MultiTapGestureRecognizer,
    event: PointerDownEvent,
    longTapDelay: Duration
) : TapTracker(
    event = event,
    entry = GestureBinding.instance!!.gestureArena.add(event.pointer, gestureRecognizer)
) {
    private var timer: Timer? = null
    private var wonArena = false
    private var lastPosition = event.position
    private var finalPosition: Offset? = null

    init {
        startTrackingPointer(::handleEvent)
        if (longTapDelay > Duration.zero) {
            timer = Timer.create(longTapDelay) {
                timer = null
                gestureRecognizer.dispatchLongTap(event.pointer, lastPosition)
            }
        }
    }

    fun handleEvent(event: PointerEvent) {
        assert(event.pointer == pointer)
        if (event is PointerMoveEvent) {
            if (!isWithinTolerance(event, kTouchSlop))
                cancel()
            else
                lastPosition = event.position
        } else if (event is PointerCancelEvent) {
            cancel()
        } else if (event is PointerUpEvent) {
            stopTrackingPointer(::handleEvent)
            finalPosition = event.position
            check()
        }
    }

    override fun stopTrackingPointer(route: PointerRoute) {
        timer?.cancel()
        timer = null
        super.stopTrackingPointer(route)
    }

    fun accept() {
        wonArena = true
        check()
    }

    fun reject() {
        stopTrackingPointer(::handleEvent)
        gestureRecognizer.dispatchCancel(pointer)
    }

    fun cancel() {
        // If we won the arena already, then entry is resolved, so resolving
        // again is a no-op. But we still need to clean up our own state.
        if (wonArena)
            reject()
        else
            entry.resolve(GestureDisposition.rejected) // eventually calls reject()
    }

    private fun check() {
        if (wonArena) {
            finalPosition?.let {
                gestureRecognizer.dispatchTap(pointer, it)
            }
        }
    }
}
