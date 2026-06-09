/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import androidx.collection.LongLongMap
import androidx.collection.buildLongLongMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.scene.merging
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilteredMap
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * Compose or user code can't work well if we miss some events.
 *
 * This class generates new synthetic events based on the previous event, if something is missing.
 *
 * Synthetic events:
 * 1. Synthetic Move, if we miss Move before Press/Release events with a different position
 * for Mouse.
 *
 * Reason: Compose can receive a native Move and send it as Enter/Exit to the nodes.
 * If we don't have some Move's before Press/Release, we can miss Enter/Exit.
 *
 * The alternative of sending synthetic moves is to send a native press/release as
 * Enter/Exit separately from Press/Release.
 * But this approach requires more changes - we need a separate HitPathTracker for Enter/Exit.
 * The user code won't see anything new with this approach
 * (besides that Enter/Exit event will have nativeEvent.type == Release/Press)
 *
 * We don't send synthetic events for touch, as it doesn't have Enter/Exit, and it will be
 * useless to send them.
 * Besides, a Release of touch is different from a Release of mouse.
 * Touch can be released in a different position
 * (the finger is lifted, but we can still detect its position),
 * Mouse can't be released in different position - we should move the cursor to this position.
 *
 * 2. Synthetic Press/Release if we send one event with 2 pressed touches without sending 1 pressed
 * touch first. For example, iOS simulator can send 2 touches simultaneously.
 */
internal class SyntheticEventSender(
    send: (PointerInputEvent) -> PointerEventResult
) {
    private val _send: (PointerInputEvent) -> PointerEventResult = send
    private var previousEvent: PointerInputEvent? = null
    private var isMousePointerInside: Boolean = false
    private var isScaleGestureInProgress: Boolean = false
    private var isPanGestureInProgress: Boolean = false

    /**
     * If something happened with Compose content (it relayouted), we need to send an
     * event to it with the latest pointer position. Otherwise, the content won't be updated
     * by the actual relative position of the pointer.
     *
     * For example, it can be needed when we scroll content without moving the pointer, and we need
     * to highlight the items under the pointer.
     */
    var needUpdatePointerPosition: Boolean = false

    fun reset() {
        needUpdatePointerPosition = false
        previousEvent = null
        isScaleGestureInProgress = false
        isPanGestureInProgress = false
    }

    /**
     * Send [event] and synthetic events before it if needed. On each sent event we just call [send]
     */
    fun send(event: PointerInputEvent): PointerEventResult {
        trackMousePointerState(event)

        val syntheticMoveForHoverResult = sendMissingMoveForHover(event)
        val syntheticReleasesResult = sendMissingReleases(event)
        val syntheticPressesResult = sendMissingPresses(event)
        val syntheticScaleEndResult = sendMissingScaleEnd(event)
        val syntheticScaleStartResult = sendMissingScaleStart(event)
        val syntheticPanEndResult = sendMissingPanEnd(event)
        val syntheticPanStartResult = sendMissingPanStart(event)
        val eventResult = if (shouldSend(event)) sendInternal(event) else sendNativeEventOnly(event)
        return syntheticMoveForHoverResult.merging(
            syntheticReleasesResult,
            syntheticPressesResult,
            syntheticScaleEndResult,
            syntheticScaleStartResult,
            syntheticPanEndResult,
            syntheticPanStartResult,
            eventResult
        )
    }

    private fun trackMousePointerState(event: PointerInputEvent) {
        if (!event.pointers.fastAny { it.type == PointerType.Mouse }) return

        when (event.eventType) {
            // Mark as true not just on Enter, just in case the system didn't send
            // us an Enter event (also, some tests don't bother sending it)
            PointerEventType.Enter,
            PointerEventType.Move,
            PointerEventType.Press,
            PointerEventType.Scroll,
            PointerEventType.ScaleStart,
            PointerEventType.ScaleChange,
            PointerEventType.ScaleEnd,
            PointerEventType.PanStart,
            PointerEventType.PanMove,
            PointerEventType.PanEnd,
                -> isMousePointerInside = true
            PointerEventType.Exit
                -> isMousePointerInside = false
        }
    }

    /**
     * Returns whether the given event should be sent.
     *
     * An event could be filtered out if, for example, it is a duplicate of the previous event.
     */
    private fun shouldSend(event: PointerInputEvent): Boolean {
        // Filter out press/release events with the same pressed pointers, buttons and keyboard
        // modifiers as the previous event.
        // Note that missing move events for this event should have already been sent
        fun areSameParams(e1: PointerInputEvent, e2: PointerInputEvent): Boolean {
            if (e1.pressedIds().toSet() != e2.pressedIds().toSet()) return false
            if (e1.buttons != e2.buttons) return false
            if (e1.keyboardModifiers != e2.keyboardModifiers) return false
            return true
        }
        if (event.eventType == PointerEventType.Press) {
            val prevEvent = previousEvent
            if ((prevEvent != null) && areSameParams(event, prevEvent)) return false
        }
        if (event.eventType == PointerEventType.Release) {
            val prevEvent = previousEvent ?: return false
            if (areSameParams(event, prevEvent)) return false
        }

        return true
    }

    /**
     * When an event generated as a result of a native event is filtered out (see [shouldSend]),
     * we nevertheless want listeners to native events to receive them.
     * This method sends an event with a type of [PointerEventType.Unknown] so that listeners can
     * still receive it.
     */
    private fun sendNativeEventOnly(event: PointerInputEvent): PointerEventResult {
        if (event.nativeEvent == null) return UnconsumedEventResult
        return _send(event.copy(eventType = PointerEventType.Unknown))
    }

    fun updatePointerPosition(): PointerEventResult {
        val nothingConsumed = UnconsumedEventResult

        if (!needUpdatePointerPosition) return nothingConsumed
        needUpdatePointerPosition = false

        // Re-send pointer position update only for mouse events.
        // Aligned with [AndroidComposeView.resendMotionEventOnLayout], but fixing b/397352507.
        val previousEvent = previousEvent ?: return nothingConsumed
        val mousePointer = previousEvent.pointers.fastFirstOrNull { it.type == PointerType.Mouse }
            ?: return nothingConsumed

        // Send synthetic move only if the scene is the current "target" of mouse events
        return if (isMousePointerInside || mousePointer.down) {
            sendSyntheticMove(previousEvent)
        } else {
            nothingConsumed
        }
    }

    /**
     * @param pointersSourceEvent the event which we treat as a source of the pointers
     */
    private fun sendSyntheticMove(
        pointersSourceEvent: PointerInputEvent
    ): PointerEventResult {
        val previousEvent = previousEvent ?: return UnconsumedEventResult
        val idToPosition = pointersSourceEvent.pointers.mapPointersToPosition()
        return sendInternal(
            previousEvent.copySynthetic(
                type = PointerEventType.Move,
                copyPointer = {
                    it.copySynthetic(
                        position = idToPosition.getPositionOrDefault(it.id, it.position)
                    )
                },
            )
        )
    }

    private fun sendMissingMoveForHover(
        currentEvent: PointerInputEvent
    ): PointerEventResult {
        // issuesEnterExit means that the pointer can issues hover events (enter/exit), and so we
        // should generate a synthetic Move (see why we need to do that in the class description)
        return if (currentEvent.pointers.fastAny { it.activeHover } &&
            isMoveEventMissing(previousEvent, currentEvent)) {
            sendSyntheticMove(currentEvent)
        } else {
            UnconsumedEventResult
        }
    }

    private fun sendMissingReleases(currentEvent: PointerInputEvent): PointerEventResult {
        val previousEvent = previousEvent ?: return UnconsumedEventResult
        val previousPressed = previousEvent.pressedIds()
        val currentPressed = currentEvent.pressedIds()
        val newReleased = (previousPressed - currentPressed.toSet()).toList()
        val sendingAsUp = HashSet<PointerId>(newReleased.size)

        var result = UnconsumedEventResult
        val lastIndex = when (currentEvent.eventType) {
            // The "real" event itself will be the last one
            PointerEventType.Release -> newReleased.lastIndex - 1
            else -> newReleased.lastIndex
        }
        for (i in lastIndex downTo 0) {
            sendingAsUp.add(newReleased[i])

            sendInternal(
                previousEvent.copySynthetic(
                    type = PointerEventType.Release,
                    copyPointer = {
                        it.copySynthetic(
                            down = it.down && !sendingAsUp.contains(it.id)
                        )
                    }
                )
            ).also {
                result = result.merging(it)
            }
        }
        return result
    }

    private fun sendMissingPresses(currentEvent: PointerInputEvent): PointerEventResult {
        val previousPressed = previousEvent?.pressedIds().orEmpty().toSet()
        val currentPressed = currentEvent.pressedIds()
        val newPressed = (currentPressed - previousPressed).toList()
        val sendingAsDown = HashSet<PointerId>(newPressed.size)

        var result = UnconsumedEventResult
        val lastIndex = when (currentEvent.eventType) {
            // The "real" event itself will be the last one
            PointerEventType.Press -> newPressed.lastIndex - 1
            else -> newPressed.lastIndex
        }
        for (i in 0..lastIndex) {
            sendingAsDown.add(newPressed[i])

            sendInternal(
                currentEvent.copySynthetic(
                    type = PointerEventType.Press,
                    copyPointer = {
                        it.copySynthetic(
                            down = previousPressed.contains(it.id) || sendingAsDown.contains(it.id)
                        )
                    }
                )
            ).also {
                result = result.merging(it)
            }
        }
        return result
    }

    private fun PointerInputEvent.pressedIds(): List<PointerId> =
        pointers.fastFilteredMap(PointerInputEventData::down, PointerInputEventData::id)


    private fun sendInternal(event: PointerInputEvent): PointerEventResult {
        when (event.eventType) {
            PointerEventType.ScaleStart -> isScaleGestureInProgress = true
            PointerEventType.ScaleEnd -> isScaleGestureInProgress = false
            PointerEventType.PanStart -> isPanGestureInProgress = true
            PointerEventType.PanEnd -> isPanGestureInProgress = false
            else -> {}
        }
        val result = _send(event)
        if (!result.dispatchedToAPointerInputModifier) {
            // What we want to do here is to make sure to dispatch the native event if the Compose
            // event has been filtered out by HitPathTracker. Unfortunately, there's no
            // `PointerEventResult.eventIgnored` flag, but `dispatchedToAPointerInputModifier`
            // seems adequate for now. It will be false also in the case of no `PointerInput` nodes,
            // but that's ok because then our native event will also not be delivered to anyone.
            sendNativeEventOnly(event)
        }
        // We don't send nativeEvent for synthetic events.
        // Nullify to avoid memory leaks (native events can point to native views).
        // Copy the pointers list because the original list may be reused
        previousEvent = event.copy(
            nativeEvent = null,
            pointers = event.pointers.toList()
        )
        return result
    }

    private fun sendMissingScaleStart(event: PointerInputEvent): PointerEventResult {
        return if (!isScaleGestureInProgress &&
            (event.eventType == PointerEventType.ScaleChange || event.eventType == PointerEventType.ScaleEnd)) {
            sendInternal(event.copySynthetic(PointerEventType.ScaleStart) { it.copySynthetic() })
        } else {
            UnconsumedEventResult
        }
    }

    private fun sendMissingScaleEnd(event: PointerInputEvent): PointerEventResult {
        return if (isScaleGestureInProgress && event.eventType == PointerEventType.ScaleStart) {
            sendInternal(event.copySynthetic(PointerEventType.ScaleEnd) { it.copySynthetic() })
        } else {
            UnconsumedEventResult
        }
    }

    private fun sendMissingPanStart(event: PointerInputEvent): PointerEventResult {
        return if (!isPanGestureInProgress &&
            (event.eventType == PointerEventType.PanMove || event.eventType == PointerEventType.PanEnd)) {
            sendInternal(event.copySynthetic(PointerEventType.PanStart) { it.copySynthetic() })
        } else {
            UnconsumedEventResult
        }
    }

    private fun sendMissingPanEnd(event: PointerInputEvent): PointerEventResult {
        return if (isPanGestureInProgress && event.eventType == PointerEventType.PanStart) {
            sendInternal(event.copySynthetic(PointerEventType.PanEnd) { it.copySynthetic() })
        } else {
            UnconsumedEventResult
        }
    }

    private fun isMoveEventMissing(
        previousEvent: PointerInputEvent?,
        currentEvent: PointerInputEvent,
    ) = !currentEvent.isMove() && currentEvent.anyPointerPositionChangedSince(previousEvent)

    private fun PointerInputEvent.isMove() =
        eventType == PointerEventType.Move ||
            eventType == PointerEventType.Enter ||
            eventType == PointerEventType.Exit

    private fun PointerInputEvent.anyPointerPositionChangedSince(
        previousEvent: PointerInputEvent?
    ): Boolean {
        val previousPointers = previousEvent?.pointers ?: return false

        // Typical case: only one pointer
        if ((pointers.size == 1) && (previousPointers.size == 1)) {
            val current = pointers[0]
            val previous = previousPointers[0]
            return (current.id == previous.id) && (current.position != previous.position)
        }

        val previousIdToPosition = previousEvent.pointers.mapPointersToPosition()
        return pointers.fastAny {
            val previousPosition = previousIdToPosition.getPositionOrDefault(it.id, Offset.Unspecified)
            previousPosition.isSpecified && (it.position != previousPosition)
        }
    }

    // we don't use copy here to not forget to nullify properties that shouldn't be in
    // a synthetic event
    private fun PointerInputEvent.copySynthetic(
        type: PointerEventType,
        copyPointer: (PointerInputEventData) -> PointerInputEventData,
    ) = PointerInputEvent(
        eventType = type,
        pointers = pointers.fastMap(copyPointer),
        uptime = uptime,
        nativeEvent = null,
        buttons = buttons,
        keyboardModifiers = keyboardModifiers,
        button = null
    )

    private fun PointerInputEventData.copySynthetic(
        position: Offset = this.position,
        down: Boolean = this.down
    ) = PointerInputEventData(
        id = id,
        uptime = uptime,
        positionOnScreen = position,
        position = position,
        down = down,
        pressure = pressure,
        type = type,
        activeHover = activeHover,
        scrollDelta = Offset.Zero,
        historical = emptyList(), // we don't copy historical for synthetic
        scaleGestureFactor = 1.0f,
        panGestureOffset = Offset.Zero,
        originalEventPosition = position,
    )
}

private typealias PointerToPositionMap = LongLongMap

private fun List<PointerInputEventData>.mapPointersToPosition(): PointerToPositionMap =
    buildLongLongMap(size) {
        this@mapPointersToPosition.fastForEach { ptr ->
            put(ptr.id.value, ptr.position.packedValue)
        }
    }

private fun PointerToPositionMap.getPositionOrDefault(key: PointerId, default: Offset): Offset =
    Offset(getOrDefault(key.value, default.packedValue))

private val UnconsumedEventResult = PointerEventResult(anyMovementConsumed = false)