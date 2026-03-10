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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.copy
import androidx.compose.ui.input.key.internal
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SyntheticEventSender
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.copy
import androidx.compose.ui.node.RootNodeOwner
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.trace

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
    processPointerInputEvent: (PointerInputEvent) -> PointerEventResult,
    private val cancelPointerInput: () -> Unit,
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
     * Indicates whether [updatePointerPosition] needs to be called.
     */
    val needUpdatePointerPosition: Boolean
        get() = syntheticEventSender.needUpdatePointerPosition

    fun onPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset,
        timeMillis: Long,
        type: PointerType,
        buttons: PointerButtons?,
        keyboardModifiers: PointerKeyboardModifiers?,
        nativeEvent: Any?,
        button: PointerButton?,
        scaleGestureFactor: Float,
        panGestureOffset: Offset,
    ): PointerEventResult {
        defaultPointerStateTracker.onPointerEvent(button, eventType)

        val actualButtons = buttons ?: defaultPointerStateTracker.buttons
        val actualKeyboardModifiers =
            keyboardModifiers ?: defaultPointerStateTracker.keyboardModifiers

        return onPointerEvent(
            eventType = eventType,
            pointers = listOf(ComposeScenePointer(PointerId(0), position, actualButtons.areAnyPressed, type)),
            buttons = actualButtons,
            keyboardModifiers = actualKeyboardModifiers,
            scrollDelta = scrollDelta,
            timeMillis = timeMillis,
            nativeEvent = nativeEvent,
            button = button,
            scaleGestureFactor = scaleGestureFactor,
            panGestureOffset = panGestureOffset,
        )
    }

    fun cancelPointerInput() {
        syntheticEventSender.reset()
        this.cancelPointerInput.invoke()
    }

    fun onPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        buttons: PointerButtons,
        keyboardModifiers: PointerKeyboardModifiers,
        scrollDelta: Offset,
        timeMillis: Long,
        nativeEvent: Any?,
        button: PointerButton?,
        scaleGestureFactor: Float,
        panGestureOffset: Offset,
    ): PointerEventResult {
        val event = PointerInputEvent(
            eventType = eventType,
            pointers = pointers,
            timeMillis = timeMillis,
            nativeEvent = nativeEvent,
            scrollDelta = scrollDelta,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            changedButton = button,
            scaleGestureFactor = scaleGestureFactor,
            panGestureOffset = panGestureOffset,
        )
        prepareForPointerInputEvent()
        val updatePointerPositionResult = updatePointerPosition()
        val eventResult = syntheticEventSender.send(event)
        updatePointerPositions(event)
        return updatePointerPositionResult.merging(eventResult)
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        defaultPointerStateTracker.onKeyEvent(keyEvent)
        return processKeyEvent(keyEvent.withTrackedModifiers())
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
        event.pointers.fastForEach { pointer ->
            if ((pointer.type == PointerType.Mouse && event.eventType != PointerEventType.Exit) ||
                pointer.down
            ) {
                pointerPositions[pointer.id] = pointer.position
            }
        }

        // touches/styluses positions should be removed from [pointerPositions] if they are not down
        // anymore also, mouse exited ComposeScene should be removed
        val iterator = pointerPositions.iterator()
        while (iterator.hasNext()) {
            val pointerId = iterator.next().key
            val pointer = event.pointers.fastFirstOrNull { it.id == pointerId } ?: continue
            if ((pointer.type != PointerType.Mouse && !pointer.down) ||
                (pointer.type == PointerType.Mouse && event.eventType == PointerEventType.Exit)
            ) {
                iterator.remove()
            }
        }
    }

    /**
     * Make sure that the current [KeyEvent] contains tracked modifiers.
     */
    private fun KeyEvent.withTrackedModifiers() = if (
        internal.nativeEvent == null && // It's not system initiated event and
        internal.modifiers.packedValue == 0 // modifiers aren't explicitly specified
    ) {
        this.copy(modifiers = defaultPointerStateTracker.keyboardModifiers)
    } else {
        this
    }
}

private class DefaultPointerStateTracker {
    fun onPointerEvent(button: PointerButton?, eventType: PointerEventType) {
        buttons = buttons.update(
            button = button ?: PointerButton.Primary,
            eventType = eventType
        )
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        keyboardModifiers = keyboardModifiers.update(
            key = keyEvent.key,
            eventType = keyEvent.type
        )
    }

    var buttons = PointerButtons()
        private set

    var keyboardModifiers = PointerKeyboardModifiers()
        private set
}

private fun PointerKeyboardModifiers.update(
    key: Key,
    eventType: KeyEventType,
): PointerKeyboardModifiers {
    val pressed = when (eventType) {
        KeyEventType.KeyDown -> true
        KeyEventType.KeyUp -> false
        else -> return this
    }
    return when (key) {
        Key.CtrlLeft, Key.CtrlRight -> copy(isCtrlPressed = pressed)
        Key.MetaLeft, Key.MetaRight -> copy(isMetaPressed = pressed)
        Key.AltLeft, Key.AltRight -> copy(isAltPressed = pressed)
        Key.ShiftLeft, Key.ShiftRight -> copy(isShiftPressed = pressed)
        // There is no binding in common for AltGraph
        Key.Symbol -> copy(isSymPressed = pressed)
        Key.Function -> copy(isFunctionPressed = pressed)
        Key.CapsLock -> copy(isCapsLockOn = pressed)
        Key.ScrollLock -> copy(isScrollLockOn = pressed)
        Key.NumLock -> copy(isNumLockOn = pressed)
        else -> this
    }
}

private fun PointerButtons.update(
    button: PointerButton,
    eventType: PointerEventType,
): PointerButtons {
    val pressed = when (eventType) {
        PointerEventType.Press -> true
        PointerEventType.Release -> false
        else -> return this
    }
    return when (button) {
        PointerButton.Primary -> copy(isPrimaryPressed = pressed)
        PointerButton.Secondary -> copy(isSecondaryPressed = pressed)
        PointerButton.Tertiary -> copy(isTertiaryPressed = pressed)
        PointerButton.Forward -> copy(isForwardPressed = pressed)
        PointerButton.Back -> copy(isBackPressed = pressed)
        else -> this
    }
}
