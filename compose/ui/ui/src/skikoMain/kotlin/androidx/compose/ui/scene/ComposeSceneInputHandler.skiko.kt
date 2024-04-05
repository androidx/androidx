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

package androidx.compose.ui.scene

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SyntheticEventSender
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.copyFor
import androidx.compose.ui.node.RootNodeOwner
import androidx.compose.ui.util.trace
import org.jetbrains.skiko.currentNanoTime

/**
 * Handles input events for [ComposeScene].
 * It's used to encapsulate input handling and share between scene implementations.
 * Also, it's passed to [RootNodeOwner] to handle [onPointerUpdate] callback and provide
 * interface for initiating input from tests.
 *
 * @see SyntheticEventSender
 */
internal class ComposeSceneInputHandler(
    private val prepareForPointerInputEvent: () -> Unit,
    processPointerInputEvent: (PointerInputEvent) -> Unit,
    private val processKeyEvent: (KeyEvent) -> Boolean,
) {
    private val defaultPointerStateTracker = DefaultPointerStateTracker()
    private val pointerPositions = mutableStateMapOf<PointerId, Offset>()
    private val syntheticEventSender = SyntheticEventSender(processPointerInputEvent)

    /**
     * The mouse cursor (also works with touch pointer) position
     * or `null` if cursor is not inside a scene.
     */
    val lastKnownPointerPosition: Offset?
        get() = pointerPositions.values.firstOrNull()

    /**
     * Indicates if there were invalidations and triggering [BaseComposeScene.measureAndLayout]
     * is now required.
     */
    val hasInvalidations: Boolean
        get() = syntheticEventSender.needUpdatePointerPosition

    fun onPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = (currentNanoTime() / 1E6).toLong(),
        type: PointerType = PointerType.Mouse,
        buttons: PointerButtons? = null,
        keyboardModifiers: PointerKeyboardModifiers? = null,
        nativeEvent: Any? = null,
        button: PointerButton? = null
    ) {
        defaultPointerStateTracker.onPointerEvent(button, eventType)

        val actualButtons = buttons ?: defaultPointerStateTracker.buttons
        val actualKeyboardModifiers =
            keyboardModifiers ?: defaultPointerStateTracker.keyboardModifiers

        onPointerEvent(
            eventType,
            listOf(ComposeScenePointer(PointerId(0), position, actualButtons.areAnyPressed, type)),
            actualButtons,
            actualKeyboardModifiers,
            scrollDelta,
            timeMillis,
            nativeEvent,
            button
        )
    }

    fun onPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        buttons: PointerButtons = PointerButtons(),
        keyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = (currentNanoTime() / 1E6).toLong(),
        nativeEvent: Any? = null,
        button: PointerButton? = null,
    ) {
        val event = PointerInputEvent(
            eventType,
            pointers,
            timeMillis,
            nativeEvent,
            scrollDelta,
            buttons,
            keyboardModifiers,
            button,
        )
        prepareForPointerInputEvent()
        updatePointerPosition()
        syntheticEventSender.send(event)
        updatePointerPositions(event)
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        defaultPointerStateTracker.onKeyEvent(keyEvent)
        return processKeyEvent(keyEvent)
    }

    fun updatePointerPosition() = trace("ComposeSceneInputHandler:updatePointerPosition") {
        syntheticEventSender.updatePointerPosition()
    }

    fun onChangeContent() {
        syntheticEventSender.reset()
    }

    fun onPointerUpdate() {
        syntheticEventSender.needUpdatePointerPosition = true
    }

    private fun updatePointerPositions(event: PointerInputEvent) {
        // update positions for pointers that are down + mouse (if it is not Exit event)
        for (pointer in event.pointers) {
            if ((pointer.type == PointerType.Mouse && event.eventType != PointerEventType.Exit) ||
                pointer.down
            ) {
                pointerPositions[pointer.id] = pointer.position
            }
        }
        // touches/styluses positions should be removed from [pointerPositions] if they are not down anymore
        // also, mouse exited ComposeScene should be removed
        val iterator = pointerPositions.iterator()
        while (iterator.hasNext()) {
            val pointerId = iterator.next().key
            val pointer = event.pointers.find { it.id == pointerId } ?: continue
            if ((pointer.type != PointerType.Mouse && !pointer.down) ||
                (pointer.type == PointerType.Mouse && event.eventType == PointerEventType.Exit)
            ) {
                iterator.remove()
            }
        }
    }
}

private class DefaultPointerStateTracker {
    fun onPointerEvent(button: PointerButton?, eventType: PointerEventType) {
        when (eventType) {
            PointerEventType.Press -> buttons = buttons.copyFor(button ?: PointerButton.Primary, pressed = true)
            PointerEventType.Release -> buttons = buttons.copyFor(button ?: PointerButton.Primary, pressed = false)
        }
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        keyboardModifiers = keyEvent.nativeKeyEvent.toPointerKeyboardModifiers()
    }

    var buttons = PointerButtons()
        private set

    var keyboardModifiers = PointerKeyboardModifiers()
        private set
}

internal expect fun NativeKeyEvent.toPointerKeyboardModifiers(): PointerKeyboardModifiers
