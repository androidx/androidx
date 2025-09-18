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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.scene.merging
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull

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
 * The user code  won't see anything new with this approach
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
    }

    /**
     * Send [event] and synthetic events before it if needed. On each sent event we just call [send]
     */
    fun send(event: PointerInputEvent): PointerEventResult {
        val syntheticMoveForHoverResult = sendMissingMoveForHover(event)
        val syntheticReleasesResult = sendMissingReleases(event)
        val syntheticPressesResult = sendMissingPresses(event)
        val eventResult = sendInternal(event)
        return syntheticMoveForHoverResult.merging(
            syntheticReleasesResult,
            syntheticPressesResult,
            eventResult
        )
    }

    fun updatePointerPosition(): PointerEventResult {
        if (needUpdatePointerPosition) {
            needUpdatePointerPosition = false

            previousEvent?.let { event ->
                // Re-send pointer position update only for hover mouse events.
                // Aligned with [AndroidComposeView.resendMotionEventOnLayout], but fixing b/397352507.
                if (event.pointers.fastAny { it.type == PointerType.Mouse }) {
                    return sendSyntheticMove(event)
                }
            }
        }
        return PointerEventResult(anyMovementConsumed = false)
    }

    /**
     * @param pointersSourceEvent the event which we treat as a source of the pointers
     */
    private fun sendSyntheticMove(
        pointersSourceEvent: PointerInputEvent
    ): PointerEventResult {
        val previousEvent = previousEvent ?: return PointerEventResult(anyMovementConsumed = false)
        val idToPosition = pointersSourceEvent.pointers.associate { it.id to it.position }
        return sendInternal(
            previousEvent.copySynthetic(
                type = PointerEventType.Move,
                copyPointer = { it.copySynthetic(position = idToPosition[it.id] ?: it.position) },
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
            PointerEventResult(anyMovementConsumed = false)
        }
    }

    private fun sendMissingReleases(currentEvent: PointerInputEvent): PointerEventResult {
        val previousEvent = previousEvent ?: return PointerEventResult(anyMovementConsumed = false)
        val previousPressed = previousEvent.pressedIds()
        val currentPressed = currentEvent.pressedIds()
        val newReleased = (previousPressed - currentPressed.toSet()).toList()
        val sendingAsUp = HashSet<PointerId>(newReleased.size)

        var result = PointerEventResult(anyMovementConsumed = false)
        // Don't send the first released pointer
        // It will be sent as a real event. Here we only need to send synthetic events
        // before a real one.
        for (i in newReleased.size - 2 downTo 0) {
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

        var result = PointerEventResult(anyMovementConsumed = false)
        // Don't send the last pressed pointer (newPressed.size - 1)
        // It will be sent as a real event. Here we only need to send synthetic events
        // before a real one.
        for (i in 0..newPressed.size - 2) {
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
        pointers.fastMapNotNull { if (it.down) it.id else null }


    private fun sendInternal(event: PointerInputEvent): PointerEventResult {
        val anyMovementConsumed = _send(event)
        // We don't send nativeEvent for synthetic events.
        // Nullify to avoid memory leaks (native events can point to native views).
        previousEvent = event.copy(nativeEvent = null)
        return anyMovementConsumed
    }

    private fun isMoveEventMissing(
        previousEvent: PointerInputEvent?,
        currentEvent: PointerInputEvent,
    ) = !currentEvent.isMove() && !currentEvent.isSamePosition(previousEvent)

    private fun PointerInputEvent.isMove() =
        eventType == PointerEventType.Move ||
            eventType == PointerEventType.Enter ||
            eventType == PointerEventType.Exit

    private fun PointerInputEvent.isSamePosition(previousEvent: PointerInputEvent?): Boolean {
        val previousIdToPosition = previousEvent?.pointers?.associate { it.id to it.position }
        return pointers.fastAll {
            val previousPosition = previousIdToPosition?.get(it.id)
            previousPosition == null || it.position == previousPosition
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
        id,
        uptime,
        position,
        position,
        down,
        pressure,
        type,
        activeHover,
        scrollDelta = Offset(0f, 0f),
        historical = emptyList(), // we don't copy historical for synthetic
        originalEventPosition = position
    )
}
