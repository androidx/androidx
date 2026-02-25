/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.node.RootForTest

internal expect fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest,
): InputDispatcher

/**
 * Dispatcher to inject any kind of input. An [InputDispatcher] is created at the beginning of
 * [performMultiModalInput] or the single modality alternatives, and disposed at the end of that
 * method. The state of all input modalities is persisted and restored on the next invocation of
 * [performMultiModalInput] (or an alternative).
 *
 * Dispatching input happens in two stages. In the first stage, all events are generated (enqueued),
 * using the `enqueue*` methods, and in the second stage all events are injected. Clients of
 * [InputDispatcher] should only call methods for the first stage listed below, the second stage is
 * handled by [performMultiModalInput] and friends.
 *
 * Touch input:
 * * [getCurrentTouchPosition]
 * * [enqueueTouchDown]
 * * [enqueueTouchMove]
 * * [updateTouchPointer]
 * * [enqueueTouchUp]
 * * [enqueueTouchCancel]
 *
 * Cursor input:
 * * [currentCursorPosition]
 *
 * Mouse input:
 * * [enqueueMousePress]
 * * [enqueueMouseMove]
 * * [updateMousePosition]
 * * [enqueueMouseRelease]
 * * [enqueueMouseCancel]
 * * [enqueueMouseScroll]
 *
 * Rotary input:
 * * [enqueueRotaryScrollHorizontally]
 * * [enqueueRotaryScrollVertically]
 *
 * Key input:
 * * [enqueueKeyDown]
 * * [enqueueKeyUp]
 *
 * Trackpad input:
 * * [enqueueTrackpadPress]
 * * [enqueueTrackpadMove]
 * * [updateTrackpadPosition]
 * * [enqueueTrackpadRelease]
 * * [enqueueTrackpadCancel]
 * * [enqueueTrackpadScroll]
 * * [enqueueTrackpadPinch]
 *
 * Chaining methods:
 * * [advanceEventTime]
 *
 * [exitHoverOnPress] and [moveOnScroll] allow controlling Android-specific behaviors that may not
 * be appropriate on other platforms. While it is a quick and simple solution, if more significant
 * differences are discovered, this problem may need to be revisited for a more robust solution.
 *
 * Note that the extra events sent due to [exitHoverOnPress] and [moveOnScroll] are in fact filtered
 * out on Android before they reach any Compose elements. They nevertheless need to be sent for the
 * benefit of any interop Android views inside Compose, which expect an Android-native model of the
 * event stream.
 */
internal abstract class InputDispatcher(
    private val testContext: TestContext,
    private val root: RootForTest,
    private val exitHoverOnPress: Boolean = true,
    private val moveOnScroll: Boolean = true,
) {
    companion object {
        /**
         * The default time between two successively injected events, 16 milliseconds. Events are
         * normally sent on every frame and thus follow the frame rate. On a 60Hz screen this is
         * ~16ms per frame.
         */
        var eventPeriodMillis = 16L
            internal set

        /**
         * The delay between a down event on a particular [Key] and the first repeat event on that
         * same key.
         */
        const val InitialRepeatDelay = 500L

        /**
         * The interval between subsequent repeats (after the initial repeat) on a particular key.
         */
        const val SubsequentRepeatDelay = 50L
    }

    /** The eventTime of the next event. */
    protected var currentTime = testContext.currentTime

    /** The state of the current touch gesture. If `null`, no touch gesture is in progress. */
    protected var partialGesture: PartialGesture? = null

    /**
     * The state of the cursor. The cursor state is always available. It starts at [Offset.Zero] in
     * not-entered state.
     */
    protected var cursorInputState: CursorInputState = CursorInputState()

    /**
     * The state of the keyboard keys. The key input state is always available. It starts with no
     * keys pressed down and the [KeyInputState.downTime] set to zero.
     */
    protected var keyInputState: KeyInputState = KeyInputState()

    /** The state of the rotary button. */
    protected var rotaryInputState: RotaryInputState = RotaryInputState()

    /**
     * Indicates if a gesture is in progress or not. A gesture is in progress if at least one finger
     * is (still) touching the screen.
     */
    val isTouchInProgress: Boolean
        get() = partialGesture != null

    /** Indicates whether caps lock is on or not. */
    val isCapsLockOn: Boolean
        get() = keyInputState.capsLockOn

    /** Indicates whether num lock is on or not. */
    val isNumLockOn: Boolean
        get() = keyInputState.numLockOn

    /** Indicates whether scroll lock is on or not. */
    val isScrollLockOn: Boolean
        get() = keyInputState.scrollLockOn

    init {
        val rootHash = identityHashCode(root)
        val state = testContext.states.remove(rootHash)
        if (state != null) {
            partialGesture = state.partialGesture
            cursorInputState = state.cursorInputState
            keyInputState = state.keyInputState
        }
    }

    protected open fun saveState(root: RootForTest?) {
        if (root != null) {
            val rootHash = identityHashCode(root)
            testContext.states[rootHash] =
                InputDispatcherState(partialGesture, cursorInputState, keyInputState)
        }
    }

    private val TestContext.currentTime
        get() = testOwner.mainClock.currentTime

    private val RootForTest.bounds
        get() = semanticsOwner.rootSemanticsNode.boundsInRoot

    protected fun isWithinRootBounds(position: Offset): Boolean = root.bounds.contains(position)

    /**
     * Increases the current event time by [durationMillis].
     *
     * Depending on the [keyInputState], there may be repeat key events that need to be sent within
     * the given duration. If there are, the clock will be forwarded until it is time for the repeat
     * key event, the key event will be sent, and then the clock will be forwarded by the remaining
     * duration.
     *
     * @param durationMillis The duration of the delay. Must be positive
     */
    fun advanceEventTime(durationMillis: Long = eventPeriodMillis) {
        require(durationMillis >= 0) {
            "duration of a delay can only be positive, not $durationMillis"
        }

        val endTime = currentTime + durationMillis
        keyInputState.sendRepeatKeysIfNeeded(endTime)
        currentTime = endTime
    }

    /**
     * During a touch gesture, returns the position of the last touch event of the given
     * [pointerId]. Returns `null` if no touch gesture is in progress for that [pointerId].
     *
     * @param pointerId The id of the pointer for which to return the current position
     * @return The current position of the pointer with the given [pointerId], or `null` if the
     *   pointer is not currently in use
     */
    fun getCurrentTouchPosition(pointerId: Int): Offset? {
        return partialGesture?.lastPositions?.get(pointerId)
    }

    /**
     * The current position of the mouse. If no mouse event has been sent yet, will be
     * [Offset.Zero].
     */
    val currentCursorPosition: Offset
        get() = cursorInputState.lastPosition

    /**
     * Indicates if the given [key] is pressed down or not.
     *
     * @param key The key to be checked.
     * @return true if given [key] is pressed, otherwise false.
     */
    fun isKeyDown(key: Key): Boolean = keyInputState.isKeyDown(key)

    /**
     * Generates a down touch event at [position] for the pointer with the given [pointerId]. Starts
     * a new touch gesture if no other [pointerId]s are down. Only possible if the [pointerId] is
     * not currently being used, although pointer ids may be reused during a touch gesture.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The coordinate of the down event
     * @see enqueueTouchMove
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun enqueueTouchDown(pointerId: Int, position: Offset) {
        var gesture = partialGesture

        // Check if this pointer is not already down
        require(gesture == null || !gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send DOWN event, a gesture is already in progress for pointer $pointerId"
        }

        if (cursorInputState.hasAnyButtonPressed) {
            // If cursor buttons are down, a touch gesture cancels the cursor gesture
            when (
                requireNotNull(cursorInputState.currentCursorInputSource) {
                    "Cursor input had buttons down, but not associated with a specific input type"
                }
            ) {
                CursorInputSource.Mouse -> enqueueMouseCancel()
                CursorInputSource.Trackpad -> enqueueTrackpadCancel()
            }
        } else if (cursorInputState.isEntered) {
            // If no cursor buttons were down, we may have been in hovered state
            when (
                requireNotNull(cursorInputState.currentCursorInputSource) {
                    "Cursor is entered, but not associated with a specific input type"
                }
            ) {
                CursorInputSource.Mouse -> cursorInputState.exitMouseHover()
                CursorInputSource.Trackpad -> cursorInputState.exitTrackpadHover()
            }
            cursorInputState.currentCursorInputSource = null
        }

        // Send a MOVE event if pointers have changed since the last event
        gesture?.flushPointerUpdates()

        // Start a new gesture, or add the pointerId to the existing gesture
        if (gesture == null) {
            gesture = PartialGesture(currentTime, position, pointerId)
            partialGesture = gesture
        } else {
            gesture.lastPositions[pointerId] = position
        }

        // Send the DOWN event
        gesture.enqueueDown(pointerId)
    }

    /**
     * Generates a move touch event without moving any of the pointers. Use this to commit all
     * changes in pointer location made with [updateTouchPointer]. The generated event will contain
     * the current position of all pointers.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     * @see enqueueTouchMoves
     */
    fun enqueueTouchMove() {
        val gesture =
            checkNotNull(partialGesture) { "Cannot send MOVE event, no gesture is in progress" }
        gesture.enqueueMove()
        gesture.hasPointerUpdates = false
    }

    /**
     * Enqueue the current time+coordinates as a move event, with the historical parameters
     * preceding it (so that they are ultimately available from methods like
     * MotionEvent.getHistoricalX).
     *
     * @see enqueueTouchMove
     * @see TouchInjectionScope.moveWithHistory
     */
    fun enqueueTouchMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
    ) {
        val gesture =
            checkNotNull(partialGesture) { "Cannot send MOVE event, no gesture is in progress" }
        gesture.enqueueMoves(relativeHistoricalTimes, historicalCoordinates)
        gesture.hasPointerUpdates = false
    }

    /**
     * Updates the position of the touch pointer with the given [pointerId] to the given [position],
     * but does not generate a move touch event. Use this to move multiple pointers simultaneously.
     * To generate the next move touch event, which will contain the current position of _all_
     * pointers (not just the moved ones), call [enqueueTouchMove]. If you move one or more pointers
     * and then call [enqueueTouchDown], without calling [enqueueTouchMove] first, a move event will
     * be generated right before that down event.
     *
     * @param pointerId The id of the pointer to move, as supplied in [enqueueTouchDown]
     * @param position The position to move the pointer to
     * @see enqueueTouchDown
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun updateTouchPointer(pointerId: Int, position: Offset) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) { "Cannot move pointers, no gesture is in progress" }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot move pointer $pointerId, it is not active in the current gesture"
        }

        gesture.lastPositions[pointerId] = position
        gesture.hasPointerUpdates = true
    }

    /**
     * Generates an up touch event for the given [pointerId] at the current position of that
     * pointer.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [enqueueTouchDown]
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchCancel
     */
    fun enqueueTouchUp(pointerId: Int) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) { "Cannot send UP event, no gesture is in progress" }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send UP event for pointer $pointerId, it is not active in the current gesture"
        }

        // First send the UP event
        gesture.enqueueUp(pointerId)

        // Then remove the pointer, and end the gesture if no pointers are left
        gesture.lastPositions.remove(pointerId)
        if (gesture.lastPositions.isEmpty()) {
            partialGesture = null
        }
    }

    /**
     * Generates a cancel touch event for the current touch gesture. Sent automatically when mouse
     * events are sent while a touch gesture is in progress.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     */
    fun enqueueTouchCancel() {
        val gesture =
            checkNotNull(partialGesture) { "Cannot send CANCEL event, no gesture is in progress" }
        gesture.enqueueCancel()
        partialGesture = null
    }

    /**
     * Generates a move event with all pointer locations, if any of the pointers has been moved by
     * [updateTouchPointer] since the last move event.
     */
    private fun PartialGesture.flushPointerUpdates() {
        if (hasPointerUpdates) {
            enqueueTouchMove()
        }
    }

    /**
     * Generates a mouse button pressed event for the given [buttonId]. This will generate all
     * required associated events as well, such as a down event if it is the first button being
     * pressed and an optional hover exit event.
     *
     * @param buttonId The id of the mouse button. This is platform dependent, use the values
     *   defined by [MouseButton.buttonId].
     */
    fun enqueueMousePress(buttonId: Int) {
        val cursor = cursorInputState

        check(!cursor.isButtonPressed(buttonId)) {
            "Cannot send mouse button down event, button $buttonId is already pressed"
        }
        check(isWithinRootBounds(currentCursorPosition) || cursor.hasAnyButtonPressed) {
            "Cannot start a mouse gesture outside the Compose root bounds, mouse position is " +
                "$currentCursorPosition and bounds are ${root.bounds}"
        }
        if (cursor.currentCursorInputSource == CursorInputSource.Trackpad) {
            enqueueTrackpadCancel()
        }
        if (partialGesture != null) {
            enqueueTouchCancel()
        }
        cursor.currentCursorInputSource = CursorInputSource.Mouse

        // Down time is when the first button is pressed
        if (cursor.hasNoButtonsPressed) {
            cursor.downTime = currentTime
        }
        cursor.setButtonBit(buttonId)

        // Exit hovering if necessary (Android-specific behavior)
        if (exitHoverOnPress) {
            if (cursor.isEntered) {
                cursor.exitMouseHover()
            }
        }
        // down/move + press
        cursor.enqueueMousePress(buttonId)
    }

    /**
     * Generates a mouse move or hover event to the given [position]. If buttons are pressed, a move
     * event is generated, otherwise generates a hover event.
     *
     * @param position The new mouse position
     */
    fun enqueueMouseMove(position: Offset) {
        val cursor = cursorInputState
        if (cursor.currentCursorInputSource == CursorInputSource.Trackpad) {
            enqueueTrackpadCancel()
        }
        // Touch needs to be cancelled, even if mouse is out of bounds
        if (partialGesture != null) {
            enqueueTouchCancel()
        }
        cursor.currentCursorInputSource = CursorInputSource.Mouse

        updateMousePosition(position)
        val isWithinBounds = isWithinRootBounds(position)

        if (isWithinBounds && !cursor.isEntered && cursor.hasNoButtonsPressed) {
            // If not yet hovering and no buttons pressed, enter hover state
            cursor.enterMouseHover()
        } else if (!isWithinBounds && cursor.isEntered) {
            // If hovering, exit now
            cursor.exitMouseHover()
        }
        cursor.enqueueMouseMove()
    }

    /**
     * Updates the mouse position without sending an event. Useful if down, up or scroll events need
     * to be injected on a different location than the preceding move event.
     *
     * @param position The new mouse position
     */
    fun updateMousePosition(position: Offset) {
        cursorInputState.lastPosition = position
        // Contrary to touch input, we don't need to store that the position has changed, because
        // all events that are affected send the current position regardless.
    }

    /**
     * Generates a mouse button released event for the given [buttonId]. This will generate all
     * required associated events as well, such as an up and hover enter event if it is the last
     * button being released.
     *
     * @param buttonId The id of the mouse button. This is platform dependent, use the values
     *   defined by [MouseButton.buttonId].
     */
    fun enqueueMouseRelease(buttonId: Int) {
        val cursor = cursorInputState

        check(cursor.isButtonPressed(buttonId)) {
            "Cannot send mouse button up event, button $buttonId is not pressed"
        }
        check(partialGesture == null) {
            "Touch gesture can't be in progress, mouse buttons are down"
        }

        cursor.unsetButtonBit(buttonId)
        cursor.enqueueMouseRelease(buttonId)

        // When no buttons remaining, enter hover state immediately (Android-specific behavior)
        if (
            exitHoverOnPress &&
                cursor.hasNoButtonsPressed &&
                isWithinRootBounds(currentCursorPosition)
        ) {
            cursor.enterMouseHover()
            cursor.enqueueMouseMove()
        } else {
            // If we are not entering hover, clear out the current cursor input source
            cursor.currentCursorInputSource = null
        }
    }

    /**
     * Generates a mouse hover enter event on the given [position].
     *
     * @param position The new mouse position
     */
    fun enqueueMouseEnter(position: Offset) {
        val cursor = cursorInputState

        check(!cursor.isEntered) {
            "Cannot send mouse hover enter event, mouse is already hovering"
        }
        check(cursor.hasNoButtonsPressed) {
            "Cannot send mouse hover enter event, mouse buttons are down"
        }
        check(isWithinRootBounds(position)) {
            "Cannot send mouse hover enter event, $position is out of bounds"
        }
        cursor.currentCursorInputSource = CursorInputSource.Mouse

        updateMousePosition(position)
        cursor.enterMouseHover()
    }

    /**
     * Generates a mouse hover exit event on the given [position].
     *
     * @param position The new mouse position
     */
    fun enqueueMouseExit(position: Offset) {
        val cursor = cursorInputState

        check(cursor.isEntered) { "Cannot send mouse hover exit event, mouse is not hovering" }

        updateMousePosition(position)
        cursor.exitMouseHover()
    }

    /**
     * Generates a mouse cancel event. Can only be done if no mouse buttons are currently pressed.
     * Sent automatically if a touch event is sent while mouse buttons are down.
     */
    fun enqueueMouseCancel() {
        val cursor = cursorInputState
        check(cursor.hasAnyButtonPressed) {
            "Cannot send mouse cancel event, no mouse buttons are pressed"
        }
        check(cursor.currentCursorInputSource == CursorInputSource.Mouse) {
            "Cannot send mouse cancel event, since the current cursor input isn't a mouse"
        }

        cursor.clearButtonState()
        cursor.enqueueMouseCancel()

        cursor.currentCursorInputSource = null
    }

    /**
     * Generates a scroll event on [scrollWheel] by [delta].
     *
     * Positive [delta] values correspond to scrolling forward (new content appears at the bottom of
     * a column, or at the end of a row), negative values correspond to scrolling backward (new
     * content appears at the top of a column, or at the start of a row).
     */
    fun enqueueMouseScroll(delta: Float, scrollWheel: ScrollWheel) {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Mouse

        if (moveOnScroll) {
            // On Android a scroll is always preceded by a move(/hover) event
            enqueueMouseMove(currentCursorPosition)
        }
        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueMouseScroll(delta, scrollWheel)
        }
    }

    fun enqueueMouseScroll(offset: Offset) {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Mouse

        if (moveOnScroll) {
            // On Android a scroll is always preceded by a move(/hover) event
            enqueueMouseMove(currentCursorPosition)
        }
        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueMouseScroll(offset)
        }
    }

    /**
     * Generates a mouse button pressed event for the given [buttonId]. This will generate all
     * required associated events as well, such as a down event if it is the first button being
     * pressed and an optional hover exit event.
     *
     * @param buttonId The id of the mouse button. This is platform dependent, use the values
     *   defined by [MouseButton.buttonId].
     */
    fun enqueueTrackpadPress(buttonId: Int) {
        val cursor = cursorInputState

        check(!cursor.isButtonPressed(buttonId)) {
            "Cannot send mouse button down event, button $buttonId is already pressed"
        }
        check(isWithinRootBounds(currentCursorPosition) || cursor.hasAnyButtonPressed) {
            "Cannot start a trackpad gesture outside the Compose root bounds, trackpad position " +
                "is $currentCursorPosition and bounds are ${root.bounds}"
        }
        if (cursor.currentCursorInputSource == CursorInputSource.Mouse) {
            enqueueMouseCancel()
        }
        if (partialGesture != null) {
            enqueueTouchCancel()
        }
        cursor.currentCursorInputSource = CursorInputSource.Trackpad

        // Down time is when the first button is pressed
        if (cursor.hasNoButtonsPressed) {
            cursor.downTime = currentTime
        }
        cursor.setButtonBit(buttonId)

        // Exit hovering if necessary (Android-specific behavior)
        if (exitHoverOnPress) {
            if (cursor.isEntered) {
                cursor.exitTrackpadHover()
            }
        }
        // down/move + press
        cursor.enqueueTrackpadPress(buttonId)
    }

    /**
     * Generates a mouse move or hover event to the given [position]. If buttons are pressed, a move
     * event is generated, otherwise generates a hover event.
     *
     * @param position The new mouse position
     */
    fun enqueueTrackpadMove(position: Offset) {
        val cursor = cursorInputState

        if (cursor.currentCursorInputSource == CursorInputSource.Mouse) {
            enqueueMouseCancel()
        }
        // Touch needs to be cancelled, even if trackpad is out of bounds
        if (partialGesture != null) {
            enqueueTouchCancel()
        }
        cursor.currentCursorInputSource = CursorInputSource.Trackpad

        updateTrackpadPosition(position)
        val isWithinBounds = isWithinRootBounds(position)

        if (isWithinBounds && !cursor.isEntered && cursor.hasNoButtonsPressed) {
            // If not yet hovering and no buttons pressed, enter hover state
            cursor.enterTrackpadHover()
        } else if (!isWithinBounds && cursor.isEntered) {
            // If hovering, exit now
            cursor.exitTrackpadHover()
        }
        cursor.enqueueTrackpadMove()
    }

    /**
     * Updates the trackpad position without sending an event. Useful if down, up or scroll events
     * need to be injected on a different location than the preceding move event.
     *
     * @param position The new trackpad position
     */
    fun updateTrackpadPosition(position: Offset) {
        cursorInputState.lastPosition = position
        // Contrary to touch input, we don't need to store that the position has changed, because
        // all events that are affected send the current position regardless.
    }

    /**
     * Generates a mouse button released event for the given [buttonId]. This will generate all
     * required associated events as well, such as an up and hover enter event if it is the last
     * button being released.
     *
     * @param buttonId The id of the mouse button. This is platform dependent, use the values
     *   defined by [MouseButton.buttonId].
     */
    fun enqueueTrackpadRelease(buttonId: Int) {
        val cursor = cursorInputState

        check(cursor.isButtonPressed(buttonId)) {
            "Cannot send mouse button up event, button $buttonId is not pressed"
        }
        check(partialGesture == null) {
            "Touch gesture can't be in progress, mouse buttons are down"
        }

        cursor.unsetButtonBit(buttonId)
        cursor.enqueueTrackpadRelease(buttonId)

        // When no buttons remaining, enter hover state immediately (Android-specific behavior)
        if (
            exitHoverOnPress &&
                cursor.hasNoButtonsPressed &&
                isWithinRootBounds(currentCursorPosition)
        ) {
            cursor.enterTrackpadHover()
            cursor.enqueueTrackpadMove()
        } else {
            // If we are not entering hover, clear out the current cursor input source
            cursor.currentCursorInputSource = null
        }
    }

    /**
     * Generates a trackpad hover enter event on the given [position].
     *
     * @param position The new trackpad position
     */
    fun enqueueTrackpadEnter(position: Offset) {
        val cursor = cursorInputState

        check(!cursor.isEntered) {
            "Cannot send trackpad hover enter event, trackpad is already hovering"
        }
        check(cursor.hasNoButtonsPressed) {
            "Cannot send trackpad hover enter event, mouse buttons are down"
        }
        check(isWithinRootBounds(position)) {
            "Cannot send trackpad hover enter event, $position is out of bounds"
        }

        cursor.currentCursorInputSource = CursorInputSource.Trackpad

        updateTrackpadPosition(position)
        cursor.enterTrackpadHover()
    }

    /**
     * Generates a trackpad hover exit event on the given [position].
     *
     * @param position The new trackpad position
     */
    fun enqueueTrackpadExit(position: Offset) {
        val cursor = cursorInputState

        check(cursor.isEntered) {
            "Cannot send trackpad hover exit event, trackpad is not hovering"
        }

        updateTrackpadPosition(position)
        cursor.exitTrackpadHover()

        cursor.currentCursorInputSource = null
    }

    /**
     * Generates a trackpad cancel event. Can only be done if no mouse buttons are currently
     * pressed. Sent automatically if a touch event is sent while mouse buttons are down.
     */
    fun enqueueTrackpadCancel() {
        val cursor = cursorInputState
        check(cursor.hasAnyButtonPressed) {
            "Cannot send trackpad cancel event, no mouse buttons are pressed"
        }
        check(cursor.currentCursorInputSource == CursorInputSource.Trackpad) {
            "Cannot send trackpad cancel event, since the current cursor input isn't a trackpad"
        }

        cursor.clearButtonState()
        cursor.enqueueTrackpadCancel()

        cursor.currentCursorInputSource = null
    }

    fun enqueueTrackpadPanStart() {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Trackpad
        check(!cursor.isInPanGesture) {
            "Cannot send trackpad pan start event, a pan gesture is already in progress"
        }
        cursor.panAccumulatedOffset = Offset.Zero

        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueTrackpadPanStart()
        }
    }

    fun enqueueTrackpadPanMove(delta: Offset) {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Trackpad
        check(cursor.isInPanGesture) {
            "Cannot send trackpad pan move event, no pan gesture is in progress"
        }
        cursor.panAccumulatedOffset = cursor.panAccumulatedOffset!! + delta
        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueTrackpadPanMove(delta)
        }
    }

    fun enqueueTrackpadPanEnd() {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Trackpad
        check(cursor.isInPanGesture) {
            "Cannot send trackpad pan end event, no pan gesture is in progress"
        }
        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueTrackpadPanEnd()
        }
        cursor.panAccumulatedOffset = null
    }

    fun enqueueTrackpadScaleStart() {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Trackpad
        check(!cursor.isInScaleGesture) {
            "Cannot send trackpad scale start event, a scale gesture is already in progress"
        }
        cursor.scaleAccumulatedFactor = 1f
        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueTrackpadScaleStart()
        }
    }

    fun enqueueTrackpadScaleChange(scaleFactor: Float) {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Trackpad
        check(cursor.isInScaleGesture) {
            "Cannot send trackpad scale change event, no pan gesture is in progress"
        }
        cursor.scaleAccumulatedFactor = cursor.scaleAccumulatedFactor!! * scaleFactor
        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueTrackpadScaleChange(scaleFactor)
        }
    }

    fun enqueueTrackpadScaleEnd() {
        val cursor = cursorInputState
        cursor.currentCursorInputSource = CursorInputSource.Trackpad
        check(cursor.isInScaleGesture) {
            "Cannot send trackpad scale end event, no scale gesture is in progress"
        }
        if (isWithinRootBounds(currentCursorPosition)) {
            cursor.enqueueTrackpadScaleEnd()
        }
        cursor.scaleAccumulatedFactor = null
    }

    /**
     * Generates a key down event for the given [key].
     *
     * @param key The keyboard key to be pushed down. Platform specific.
     */
    fun enqueueKeyDown(key: Key) {
        val keyboard = keyInputState

        check(!keyboard.isKeyDown(key)) {
            "Cannot send key down event, Key($key) is already pressed down."
        }

        // TODO(Onadim): Figure out whether key input needs to enqueue a touch cancel.
        // Down time is the time of the most recent key down event, which is now.
        keyboard.downTime = currentTime

        // Add key to pressed keys.
        keyboard.setKeyDown(key)

        keyboard.enqueueDown(key)
    }

    /**
     * Generates a key up event for the given [key].
     *
     * @param key The keyboard key to be released. Platform specific.
     */
    fun enqueueKeyUp(key: Key) {
        val keyboard = keyInputState

        check(keyboard.isKeyDown(key)) {
            "Cannot send key up event, Key($key) is not pressed down."
        }

        // TODO(Onadim): Figure out whether key input needs to enqueue a touch cancel.
        // Remove key from pressed keys.
        keyboard.setKeyUp(key)

        // Send the up event
        keyboard.enqueueUp(key)
    }

    fun enqueueRotaryScrollHorizontally(horizontalScrollPixels: Float) {
        // TODO(b/214437966): figure out if ongoing scroll events need to be cancelled.
        rotaryInputState.enqueueRotaryScrollHorizontally(horizontalScrollPixels)
    }

    fun enqueueRotaryScrollVertically(verticalScrollPixels: Float) {
        // TODO(b/214437966): figure out if ongoing scroll events need to be cancelled.
        rotaryInputState.enqueueRotaryScrollVertically(verticalScrollPixels)
    }

    private fun CursorInputState.enterMouseHover() {
        enqueueMouseEnter()
        isEntered = true
    }

    private fun CursorInputState.exitMouseHover() {
        enqueueMouseExit()
        isEntered = false
    }

    private fun CursorInputState.enterTrackpadHover() {
        enqueueTrackpadEnter()
        isEntered = true
    }

    private fun CursorInputState.exitTrackpadHover() {
        enqueueTrackpadExit()
        isEntered = false
    }

    /**
     * Sends any and all repeat key events that are required between [currentTime] and [endTime].
     *
     * Mutates the value of [currentTime] in order to send each of the repeat events at exactly the
     * time it should be sent.
     *
     * @param endTime All repeats set to occur before this time will be sent.
     */
    // TODO(b/236623354): Extend repeat key event support to [MainTestClock.advanceTimeBy].
    private fun KeyInputState.sendRepeatKeysIfNeeded(endTime: Long) {

        // Return if there is no key to repeat or if it is not yet time to repeat it.
        if (repeatKey == null || endTime - downTime < InitialRepeatDelay) return

        // Initial repeat
        if (lastRepeatTime <= downTime) {
            // Not yet had a repeat on this key, but it needs at least the initial one.
            check(repeatCount == 0) { "repeatCount should be reset to 0 when downTime updates" }
            repeatCount = 1

            lastRepeatTime = downTime + InitialRepeatDelay
            currentTime = lastRepeatTime

            enqueueRepeat()
        }

        // Subsequent repeats
        val numRepeats: Int = ((endTime - lastRepeatTime) / SubsequentRepeatDelay).toInt()

        repeat(numRepeats) {
            repeatCount += 1
            lastRepeatTime += SubsequentRepeatDelay
            currentTime = lastRepeatTime
            enqueueRepeat()
        }
    }

    /**
     * Enqueues a key down event on the repeat key, if there is one. If the repeat key is null, an
     * [IllegalStateException] is thrown.
     */
    private fun KeyInputState.enqueueRepeat() {
        val repKey =
            checkNotNull(repeatKey) {
                "A repeat key event cannot be sent if the repeat key is null."
            }
        keyInputState.enqueueDown(repKey)
    }

    /**
     * Sends all enqueued events and blocks while they are dispatched. If an exception is thrown
     * during the process, all events that haven't yet been dispatched will be dropped.
     */
    abstract fun flush()

    protected abstract fun PartialGesture.enqueueDown(pointerId: Int)

    protected abstract fun PartialGesture.enqueueMove()

    protected abstract fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
    )

    protected abstract fun PartialGesture.enqueueUp(pointerId: Int)

    protected abstract fun PartialGesture.enqueueCancel()

    protected abstract fun CursorInputState.enqueueMousePress(buttonId: Int)

    protected abstract fun CursorInputState.enqueueMouseMove()

    protected abstract fun CursorInputState.enqueueMouseRelease(buttonId: Int)

    protected abstract fun CursorInputState.enqueueMouseEnter()

    protected abstract fun CursorInputState.enqueueMouseExit()

    protected abstract fun CursorInputState.enqueueMouseCancel()

    protected abstract fun CursorInputState.enqueueTrackpadPress(buttonId: Int)

    protected abstract fun CursorInputState.enqueueTrackpadMove()

    protected abstract fun CursorInputState.enqueueTrackpadRelease(buttonId: Int)

    protected abstract fun CursorInputState.enqueueTrackpadEnter()

    protected abstract fun CursorInputState.enqueueTrackpadExit()

    protected abstract fun CursorInputState.enqueueTrackpadCancel()

    protected abstract fun KeyInputState.enqueueDown(key: Key)

    protected abstract fun KeyInputState.enqueueUp(key: Key)

    /**
     * Used to control lock key toggling behaviour on different platforms. Defaults to Android-style
     * toggling. To change toggling behaviour, override this method and switch to using
     * [LockKeyState.isLockKeyOnExcludingOffPress], or implement a different toggling behaviour.
     */
    protected open val KeyInputState.capsLockOn: Boolean
        get() = capsLockState.isLockKeyOnIncludingOffPress

    /**
     * Used to control lock key toggling behaviour on different platforms. Defaults to Android-style
     * toggling. To change toggling behaviour, override this method and switch to using
     * [LockKeyState.isLockKeyOnExcludingOffPress], or implement a different toggling behaviour.
     */
    protected open val KeyInputState.numLockOn: Boolean
        get() = numLockState.isLockKeyOnIncludingOffPress

    /**
     * Used to control lock key toggling behaviour on different platforms. Defaults to Android-style
     * toggling. To change toggling behaviour, override this method and switch to using
     * [LockKeyState.isLockKeyOnExcludingOffPress], or implement a different toggling behaviour.
     */
    protected open val KeyInputState.scrollLockOn: Boolean
        get() = scrollLockState.isLockKeyOnIncludingOffPress

    protected abstract fun CursorInputState.enqueueMouseScroll(
        delta: Float,
        scrollWheel: ScrollWheel,
    )

    protected abstract fun CursorInputState.enqueueMouseScroll(offset: Offset)

    protected abstract fun CursorInputState.enqueueTrackpadPanStart()

    protected abstract fun CursorInputState.enqueueTrackpadPanMove(delta: Offset)

    protected abstract fun CursorInputState.enqueueTrackpadPanEnd()

    protected abstract fun CursorInputState.enqueueTrackpadScaleStart()

    protected abstract fun CursorInputState.enqueueTrackpadScaleChange(delta: Float)

    protected abstract fun CursorInputState.enqueueTrackpadScaleEnd()

    protected abstract fun RotaryInputState.enqueueRotaryScrollHorizontally(
        horizontalScrollPixels: Float
    )

    protected abstract fun RotaryInputState.enqueueRotaryScrollVertically(
        verticalScrollPixels: Float
    )

    /**
     * Called when this [InputDispatcher] is about to be discarded, from
     * [MultiModalInjectionScopeImpl.dispose].
     */
    fun dispose() {
        saveState(root)
        onDispose()
    }

    /**
     * Override this method to take platform specific action when this dispatcher is disposed. E.g.
     * to recycle event objects that the dispatcher still holds on to.
     */
    protected open fun onDispose() {}
}

/**
 * The state of the current gesture. Contains the current position of all pointers and the down time
 * (start time) of the gesture. For the current time, see [InputDispatcher.currentTime].
 *
 * @param downTime The time of the first down event of this gesture
 * @param startPosition The position of the first down event of this gesture
 * @param pointerId The pointer id of the first down event of this gesture
 */
internal class PartialGesture(val downTime: Long, startPosition: Offset, pointerId: Int) {
    val lastPositions = mutableMapOf(Pair(pointerId, startPosition))
    var hasPointerUpdates: Boolean = false
}

/**
 * The current cursor state. Contains the current cursor position, which buttons are pressed, the
 * type of device that is the current cursor input source, if the cursor is hovering over the
 * current node and the down time of the cursor (which is the time of the last pointer down event
 * for the cursor).
 */
internal class CursorInputState {
    var downTime: Long = 0
    val pressedButtons: MutableSet<Int> = mutableSetOf()
    var lastPosition: Offset = Offset.Zero
    var isEntered: Boolean = false
    var currentCursorInputSource: CursorInputSource? = null
    var panAccumulatedOffset: Offset? = null
    var scaleAccumulatedFactor: Float? = null

    val hasAnyButtonPressed
        get() = pressedButtons.isNotEmpty()

    val hasOneButtonPressed
        get() = pressedButtons.size == 1

    val hasNoButtonsPressed
        get() = pressedButtons.isEmpty()

    fun isButtonPressed(buttonId: Int): Boolean {
        return pressedButtons.contains(buttonId)
    }

    val isInPanGesture
        get() = panAccumulatedOffset != null

    val isInScaleGesture
        get() = scaleAccumulatedFactor != null

    fun setButtonBit(buttonId: Int) {
        pressedButtons.add(buttonId)
    }

    fun unsetButtonBit(buttonId: Int) {
        pressedButtons.remove(buttonId)
    }

    fun clearButtonState() {
        pressedButtons.clear()
    }
}

internal enum class CursorInputSource {
    Mouse,
    Trackpad,
}

/**
 * Toggling states for lock keys.
 *
 * Note that lock keys may not be toggled in the same way across all platforms.
 *
 * Take caps lock as an example; consistently, all platforms turn caps lock on upon the first key
 * down event, and it stays on after the subsequent key up. However, on some platforms caps lock
 * will turn off immediately upon the next key down event (MacOS for example), whereas other
 * platforms (e.g. Linux, Android) wait for the next key up event before turning caps lock off.
 *
 * This enum breaks the lock key state down into four possible options - depending upon the
 * interpretation of these four states, Android-like or MacOS-like behaviour can both be achieved.
 *
 * To get Android-like behaviour, use [isLockKeyOnIncludingOffPress], whereas for MacOS-style
 * behaviour, use [isLockKeyOnExcludingOffPress].
 */
internal enum class LockKeyState(val state: Int) {
    UP_AND_OFF(0),
    DOWN_AND_ON(1),
    UP_AND_ON(2),
    DOWN_AND_OPTIONAL(3);

    /**
     * Whether or not the lock key is on. The lock key is considered on from the start of the "on
     * press" until the end of the "off press", i.e. from the first key down event to the second key
     * up event of the corresponding lock key.
     */
    val isLockKeyOnIncludingOffPress
        get() = state > 0

    /**
     * Whether or not the lock key is on. The lock key is considered on from the start of the "on
     * press" until the start of the "off press", i.e. from the first key down event to the second
     * key down event of the corresponding lock key.
     */
    val isLockKeyOnExcludingOffPress
        get() = this == DOWN_AND_ON || this == UP_AND_ON

    /** Returns the next state in the cycle of lock key states. */
    fun next(): LockKeyState {
        return when (this) {
            UP_AND_OFF -> DOWN_AND_ON
            DOWN_AND_ON -> UP_AND_ON
            UP_AND_ON -> DOWN_AND_OPTIONAL
            DOWN_AND_OPTIONAL -> UP_AND_OFF
        }
    }
}

/**
 * The current key input state. Contains the keys that are pressed, the down time of the keyboard
 * (which is the time of the last key down event), the state of the lock keys and the device ID.
 */
internal class KeyInputState {
    private val downKeys: HashSet<Key> = hashSetOf()

    var downTime = 0L
    var repeatKey: Key? = null
    var repeatCount = 0
    var lastRepeatTime = downTime
    var capsLockState: LockKeyState = LockKeyState.UP_AND_OFF
    var numLockState: LockKeyState = LockKeyState.UP_AND_OFF
    var scrollLockState: LockKeyState = LockKeyState.UP_AND_OFF

    fun isKeyDown(key: Key): Boolean = downKeys.contains(key)

    fun setKeyUp(key: Key) {
        downKeys.remove(key)
        if (key == repeatKey) {
            repeatKey = null
            repeatCount = 0
        }
        updateLockKeys(key)
    }

    fun setKeyDown(key: Key) {
        downKeys.add(key)
        repeatKey = key
        repeatCount = 0
        updateLockKeys(key)
    }

    /** Updates lock key state values. */
    private fun updateLockKeys(key: Key) {
        when (key) {
            Key.CapsLock -> capsLockState = capsLockState.next()
            Key.NumLock -> numLockState = numLockState.next()
            Key.ScrollLock -> scrollLockState = scrollLockState.next()
        }
    }
}

/**
 * We don't have any state associated with RotaryInput, but we use a RotaryInputState class for
 * consistency with the other APIs.
 */
internal class RotaryInputState

/**
 * The state of an [InputDispatcher], saved when the [GestureScope] is disposed and restored when
 * the [GestureScope] is recreated.
 *
 * @param partialGesture The state of an incomplete gesture. If no gesture was in progress when the
 *   state of the [InputDispatcher] was saved, this will be `null`.
 * @param cursorInputState The state of the cursor.
 * @param keyInputState The state of the keyboard.
 */
internal data class InputDispatcherState(
    val partialGesture: PartialGesture?,
    val cursorInputState: CursorInputState,
    val keyInputState: KeyInputState,
)
