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

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.Display.DEFAULT_DISPLAY
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_BUTTON_PRESS
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.MotionEvent.TOOL_TYPE_UNKNOWN
import android.view.ViewConfiguration
import androidx.collection.intSetOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectPointerEventType
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.platform.makeSynchronizedObject
import androidx.compose.ui.test.platform.synchronized
import androidx.core.view.InputDeviceCompat.SOURCE_MOUSE
import androidx.core.view.InputDeviceCompat.SOURCE_ROTARY_ENCODER
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCHSCREEN
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.core.view.MotionEventCompat.AXIS_SCROLL
import androidx.core.view.ViewConfigurationCompat.getScaledHorizontalScrollFactor
import androidx.core.view.ViewConfigurationCompat.getScaledVerticalScrollFactor

private val MouseAsTouchEvents =
    intSetOf(
        ACTION_DOWN,
        ACTION_MOVE,
        ACTION_UP,
        ACTION_POINTER_DOWN,
        ACTION_POINTER_UP,
        ACTION_CANCEL,
        ACTION_OUTSIDE,
    )

private fun createIndirectPointerInputChangesFromMotionEvents(
    motionEvent: MotionEvent,
    previousMotionEvent: MotionEvent?,
): List<IndirectPointerInputChange> {
    val action = motionEvent.actionMasked
    val upIndex =
        when (action) {
            ACTION_UP -> 0
            ACTION_POINTER_UP -> motionEvent.actionIndex
            else -> -1
        }

    val previousAction = previousMotionEvent?.actionMasked
    val previousMotionEventWasPressed =
        when (previousAction) {
            ACTION_DOWN,
            ACTION_POINTER_DOWN,
            ACTION_MOVE -> true
            else -> false
        }

    val uptimeMillis = motionEvent.eventTime
    return List(motionEvent.pointerCount) { index ->
        // For tests, we directly use the motion event's pointer ID vs. the production approach
        // of translate MotionEvent ids to separate Compose PointerIds.
        val motionEventPointerId = motionEvent.getPointerId(index)
        val pointerId = PointerId(motionEventPointerId.toLong())
        val position = Offset(motionEvent.getX(index), motionEvent.getY(index))

        val pressed = index != upIndex

        val matchedPointerIdInPreviousMotionEventIndex =
            previousMotionEvent?.findPointerIndex(motionEventPointerId) ?: -1

        val previousUptimeMillis: Long
        val previousPosition: Offset
        val previousPressed: Boolean

        if (matchedPointerIdInPreviousMotionEventIndex >= 0) {
            // Found existing id in previous event
            previousUptimeMillis = previousMotionEvent!!.eventTime
            previousPosition =
                Offset(
                    previousMotionEvent.getX(matchedPointerIdInPreviousMotionEventIndex),
                    previousMotionEvent.getY(matchedPointerIdInPreviousMotionEventIndex),
                )
            previousPressed = previousMotionEventWasPressed
        } else {
            // Existing id NOT in previous event, so we match the current event values minus
            // pressed, that should always be false.
            previousUptimeMillis = uptimeMillis
            previousPosition = position
            previousPressed = false
        }

        IndirectPointerInputChange(
            id = pointerId,
            uptimeMillis = uptimeMillis,
            position = position,
            pressed = pressed,
            pressure = motionEvent.getPressure(index),
            previousUptimeMillis = previousUptimeMillis,
            previousPosition = previousPosition,
            previousPressed = previousPressed,
        )
    }
}

internal fun convertActionToIndirectPointerEventType(actionMasked: Int): IndirectPointerEventType {
    return when (actionMasked) {
        ACTION_UP,
        ACTION_POINTER_UP -> IndirectPointerEventType.Release
        ACTION_DOWN,
        ACTION_POINTER_DOWN -> IndirectPointerEventType.Press
        ACTION_MOVE -> IndirectPointerEventType.Move
        else -> IndirectPointerEventType.Unknown
    }
}

internal actual fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest,
): InputDispatcher {
    require(root is ViewRootForTest) {
        "InputDispatcher only supports dispatching to ViewRootForTest, not to " +
            root::class.java.simpleName
    }
    val view = root.view
    return AndroidInputDispatcher(testContext, root) {
        when (val inputEvent = it.inputEvent) {
            is KeyEvent -> view.dispatchKeyEvent(inputEvent)
            is MotionEvent -> {
                when (inputEvent.source) {
                    SOURCE_TOUCHSCREEN -> view.dispatchTouchEvent(inputEvent)
                    SOURCE_ROTARY_ENCODER -> view.dispatchGenericMotionEvent(inputEvent)
                    SOURCE_MOUSE ->
                        when (inputEvent.actionMasked) {
                            in MouseAsTouchEvents -> view.dispatchTouchEvent(inputEvent)
                            else -> view.dispatchGenericMotionEvent(inputEvent)
                        }
                    SOURCE_TOUCH_NAVIGATION -> {
                        val indirectPointerEventAdditionalInformation =
                            it.additionalEventInformation!!
                                .indirectPointerEventAdditionalInformation

                        val primaryDirectionalMotionAxis =
                            indirectPointerEventAdditionalInformation.primaryDirectionalMotionAxis

                        val previousMotionEvent =
                            indirectPointerEventAdditionalInformation.previousMotionEvent

                        val indirectPointerEvent =
                            IndirectPointerEvent(
                                type =
                                    convertActionToIndirectPointerEventType(
                                        inputEvent.actionMasked
                                    ),
                                changes =
                                    createIndirectPointerInputChangesFromMotionEvents(
                                        inputEvent,
                                        previousMotionEvent,
                                    ),
                                primaryDirectionalMotionAxis = primaryDirectionalMotionAxis,
                                motionEvent = inputEvent,
                            )

                        root.sendIndirectPointerEvent(indirectPointerEvent)
                    }
                    else ->
                        throw IllegalArgumentException(
                            "Can't dispatch MotionEvents with source ${inputEvent.source}"
                        )
                }
            }
        }
    }
}

/*
 * Bundles the Android input event with additional information. In some cases, you can not set all
 * the information sent along in a [InputEvent] when creating it manually. Only the system can do
 * that, for example, you can't set device information [InputDevice] tied to a [MotionEvent].
 *
 * For certain cases, like Compose's Indirect Pointer events, you need to be able to do that.
 *
 * This allows you to do that and pass along to the information to the test system to function the
 * same as if the event was created by the system.
 */
internal class InputEventWithAdditionalInformation(
    val inputEvent: InputEvent,
    val additionalEventInformation: AdditionalEventInformation? = null,
)

internal class AdditionalEventInformation(
    val indirectPointerEventAdditionalInformation: IndirectPointerEventAdditionalInformation
)

internal class IndirectPointerEventAdditionalInformation(
    val primaryDirectionalMotionAxis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    val previousMotionEvent: MotionEvent? = null,
)

internal class AndroidInputDispatcher(
    private val testContext: TestContext,
    private val root: ViewRootForTest,
    private val sendEvent: (InputEventWithAdditionalInformation) -> Unit,
) : InputDispatcher(testContext, root) {

    // For saving information between dispatcher lifecycles. Specifically, saving the previous
    // [MotionEvent] which is needed to create [IndirectPointerEvent]s.
    private val androidPlatformContext: PlatformTestContext
        get() = testContext.platform

    private val batchLock = makeSynchronizedObject()
    private var batchedEvents = mutableListOf<InputEventWithAdditionalInformation>()
    private var disposed = false

    // The current time of the Main Clock relative to the event stream. We need this to find the
    // difference between a new event coming in (with a new current time) and the last dispatched
    // event, so we can move the clock that much as we flush the events.
    private var lastDispatchedEventTime = currentTime

    // TODO(b/214439478): Find out if we should add these values to Compose's ViewConfiguration.
    // Scroll factors for Rotary Input.
    private val verticalScrollFactor: Float by lazy {
        val context = root.view.context
        val config = ViewConfiguration.get(context)
        getScaledVerticalScrollFactor(config, context)
    }
    private val horizontalScrollFactor: Float by lazy {
        val context = root.view.context
        val config = ViewConfiguration.get(context)
        getScaledHorizontalScrollFactor(config, context)
    }

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        enqueueTouchEvent(
            if (lastPositions.size == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            lastPositions.keys.sorted().indexOf(pointerId),
        )
    }

    override fun PartialGesture.enqueueMove() {
        enqueueTouchEvent(ACTION_MOVE, 0)
    }

    override fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
    ) {
        val entries = lastPositions.entries.sortedBy { it.key }
        val absoluteHistoricalTimes = relativeHistoricalTimes.map { currentTime + it }
        enqueueTouchEvent(
            downTime = downTime,
            action = ACTION_MOVE,
            actionIndex = 0,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = absoluteHistoricalTimes + listOf(currentTime),
            coordinates =
                List(entries.size) { historicalCoordinates[it] + listOf(entries[it].value) },
        )
    }

    override fun PartialGesture.enqueueUp(pointerId: Int) {
        enqueueTouchEvent(
            if (lastPositions.size == 1) ACTION_UP else ACTION_POINTER_UP,
            lastPositions.keys.sorted().indexOf(pointerId),
        )
    }

    override fun PartialGesture.enqueueCancel() {
        enqueueTouchEvent(ACTION_CANCEL, 0)
    }

    override fun enqueueIndirectPointerDown(
        pointerId: Int,
        position: Offset,
        indirectPointerEventPrimaryDirectionalMotionAxis:
            IndirectPointerEventPrimaryDirectionalMotionAxis,
    ) {
        if (deviceSystemTime == 0L) {
            // System expects a system time for indirect touch events.
            deviceSystemTime = System.currentTimeMillis()
        }
        super.enqueueIndirectPointerDown(
            pointerId,
            position,
            indirectPointerEventPrimaryDirectionalMotionAxis,
        )
    }

    override fun PartialIndirectGesture.enqueueIndirectDown(pointerId: Int) {
        enqueueIndirectPointerEvent(
            if (lastPositions.size == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            lastPositions.keys.sorted().indexOf(pointerId),
        )
    }

    override fun PartialIndirectGesture.enqueueIndirectMove() {
        enqueueIndirectPointerEvent(ACTION_MOVE, 0)
    }

    @Suppress("PrimitiveInCollection")
    override fun PartialIndirectGesture.enqueueIndirectMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
    ) {
        val entries = lastPositions.entries.sortedBy { it.key }
        val absoluteHistoricalTimes = relativeHistoricalTimes.map { currentTime + it }
        enqueueIndirectPointerEvent(
            downTime = downTime,
            action = ACTION_MOVE,
            actionIndex = 0,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = absoluteHistoricalTimes + listOf(currentTime),
            coordinates =
                List(entries.size) { historicalCoordinates[it] + listOf(entries[it].value) },
            additionalEventInformation = createAdditionalEventInformation(),
        )
    }

    override fun PartialIndirectGesture.enqueueIndirectUp(pointerId: Int) {
        enqueueIndirectPointerEvent(
            if (lastPositions.size == 1) ACTION_UP else ACTION_POINTER_UP,
            lastPositions.keys.sorted().indexOf(pointerId),
        )
    }

    override fun PartialIndirectGesture.enqueueIndirectCancel() {
        enqueueIndirectPointerEvent(ACTION_CANCEL, 0)
    }

    override fun CursorInputState.enqueueMousePress(buttonId: Int) {
        enqueueMouseEvent(if (hasOneButtonPressed) ACTION_DOWN else ACTION_MOVE)
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueMouseEvent(ACTION_BUTTON_PRESS)
        }
    }

    override fun CursorInputState.enqueueMouseMove() {
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueMouseEvent(if (isEntered) ACTION_HOVER_MOVE else ACTION_MOVE)
        } else if (hasAnyButtonPressed) {
            enqueueMouseEvent(ACTION_MOVE)
        }
    }

    override fun CursorInputState.enqueueMouseRelease(buttonId: Int) {
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueMouseEvent(ACTION_BUTTON_RELEASE)
        }
        enqueueMouseEvent(if (hasNoButtonsPressed) ACTION_UP else ACTION_MOVE)
    }

    override fun CursorInputState.enqueueMouseEnter() {
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueMouseEvent(ACTION_HOVER_ENTER)
        }
    }

    override fun CursorInputState.enqueueMouseExit() {
        enqueueMouseEvent(ACTION_HOVER_EXIT)
    }

    override fun CursorInputState.enqueueMouseCancel() {
        enqueueMouseEvent(ACTION_CANCEL)
    }

    override fun CursorInputState.enqueueMouseScroll(delta: Float, scrollWheel: ScrollWheel) {
        enqueueMouseEvent(
            ACTION_SCROLL,
            // We invert vertical scrolling to align with another platforms.
            // Vertical scrolling on desktop/web have opposite sign.
            if (scrollWheel == ScrollWheel.Vertical) -delta else delta,
            when (scrollWheel) {
                ScrollWheel.Horizontal -> MotionEvent.AXIS_HSCROLL
                ScrollWheel.Vertical -> MotionEvent.AXIS_VSCROLL
                else -> -1
            },
        )
    }

    override fun CursorInputState.enqueueMouseScroll(offset: Offset) {
        enqueueMouseEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = ACTION_SCROLL,
            coordinate = lastPosition,
            metaState = keyInputState.constructMetaState(),
            buttonState = pressedButtons.fold(0) { state, buttonId -> state or buttonId },
            // We invert vertical scrolling to align with another platforms.
            // Vertical scrolling on desktop/web have opposite sign.
            delta = offset.copy(y = -offset.y),
        )
    }

    override fun CursorInputState.enqueueTrackpadPress(buttonId: Int) {
        enqueueTrackpadEvent(if (hasOneButtonPressed) ACTION_DOWN else ACTION_MOVE)
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueTrackpadEvent(ACTION_BUTTON_PRESS)
        }
    }

    override fun CursorInputState.enqueueTrackpadMove() {
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueTrackpadEvent(if (isEntered) ACTION_HOVER_MOVE else ACTION_MOVE)
        } else if (hasAnyButtonPressed) {
            enqueueTrackpadEvent(ACTION_MOVE)
        }
    }

    override fun CursorInputState.enqueueTrackpadRelease(buttonId: Int) {
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueTrackpadEvent(ACTION_BUTTON_RELEASE)
        }
        enqueueTrackpadEvent(if (hasNoButtonsPressed) ACTION_UP else ACTION_MOVE)
    }

    override fun CursorInputState.enqueueTrackpadEnter() {
        if (isWithinRootBounds(currentCursorPosition)) {
            enqueueTrackpadEvent(ACTION_HOVER_ENTER)
        }
    }

    override fun CursorInputState.enqueueTrackpadExit() {
        enqueueTrackpadEvent(ACTION_HOVER_EXIT)
    }

    override fun CursorInputState.enqueueTrackpadCancel() {
        enqueueTrackpadEvent(ACTION_CANCEL)
    }

    override fun CursorInputState.enqueueTrackpadPanStart() {
        // A two-finger trackpad scroll on Android is represented by a fake single finger,
        // moving like a single finger would on the touchscreen to generate a scroll.
        // To accomplish a full scroll for a specific offset this we need to:
        // - release all buttons (if any), with an up and enter
        // - exit hover
        // - press
        // - move the correct distance
        // - release
        // - enter hover
        if (hasAnyButtonPressed) {
            pressedButtons.forEach { buttonId ->
                unsetButtonBit(buttonId)
                if (isWithinRootBounds(currentCursorPosition)) {
                    enqueueTrackpadEvent(ACTION_BUTTON_RELEASE)
                }
            }
            enqueueTrackpadEvent(ACTION_UP)
            enqueueTrackpadEnter()
        }
        enqueueTrackpadExit()
        val fakeFingerDownTime = currentTime
        downTime = fakeFingerDownTime
        enqueueTwoFingerSwipeTrackpadEvent(
            downTime = fakeFingerDownTime,
            eventTime = currentTime,
            action = ACTION_DOWN,
            coordinate = lastPosition,
            delta = Offset.Zero,
            accumulatedDelta = cursorInputState.panAccumulatedOffset!!,
            metaState = keyInputState.constructMetaState(),
        )
    }

    override fun CursorInputState.enqueueTrackpadPanMove(delta: Offset) {
        enqueueTwoFingerSwipeTrackpadEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = ACTION_MOVE,
            coordinate = lastPosition,
            delta = delta,
            accumulatedDelta = cursorInputState.panAccumulatedOffset!!,
            metaState = keyInputState.constructMetaState(),
        )
    }

    override fun CursorInputState.enqueueTrackpadPanEnd() {
        enqueueTwoFingerSwipeTrackpadEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = ACTION_UP,
            coordinate = lastPosition,
            delta = Offset.Zero,
            accumulatedDelta = cursorInputState.panAccumulatedOffset!!,
            metaState = keyInputState.constructMetaState(),
        )
        enqueueTrackpadEnter()
    }

    override fun CursorInputState.enqueueTrackpadScaleStart() {
        // A trackpad pinch on Android is represented by two fake fingers, moving like two fingers
        // would on a touchscreen to generate a pinch
        // To accomplish a full pinch for a specific scale factor we need to
        // - release all buttons (if any), with an up and enter
        // - exit hover
        // - press finger 1 and finger 2 at an initial separation
        // - move both fingers together or apart by the correct amount
        // - release finger and finger 2
        // - enter hover
        if (hasAnyButtonPressed) {
            pressedButtons.forEach { buttonId ->
                unsetButtonBit(buttonId)
                if (isWithinRootBounds(currentCursorPosition)) {
                    enqueueTrackpadEvent(ACTION_BUTTON_RELEASE)
                }
            }
            enqueueTrackpadEvent(ACTION_UP)
            enqueueTrackpadEnter()
        }
        enqueueTrackpadExit()
        val fakeFingersDownTime = currentTime
        downTime = fakeFingersDownTime
        enqueuePinchTrackpadEvent(
            downTime = fakeFingersDownTime,
            eventTime = currentTime,
            action = ACTION_DOWN,
            actionIndex = 0,
            coordinate = lastPosition,
            delta = 1f,
            accumulatedDelta = 1f,
            metaState = keyInputState.constructMetaState(),
        )
        enqueuePinchTrackpadEvent(
            downTime = fakeFingersDownTime,
            eventTime = currentTime,
            action = ACTION_POINTER_DOWN,
            actionIndex = 1,
            coordinate = lastPosition,
            delta = 1f,
            accumulatedDelta = 1f,
            metaState = keyInputState.constructMetaState(),
        )
    }

    override fun CursorInputState.enqueueTrackpadScaleChange(delta: Float) {
        enqueuePinchTrackpadEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = ACTION_MOVE,
            actionIndex = 0,
            coordinate = lastPosition,
            delta = delta,
            accumulatedDelta = cursorInputState.scaleAccumulatedFactor!!,
            metaState = keyInputState.constructMetaState(),
        )
    }

    override fun CursorInputState.enqueueTrackpadScaleEnd() {
        enqueuePinchTrackpadEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = ACTION_POINTER_UP,
            actionIndex = 1,
            coordinate = lastPosition,
            delta = 1f,
            accumulatedDelta = cursorInputState.scaleAccumulatedFactor!!,
            metaState = keyInputState.constructMetaState(),
        )
        enqueuePinchTrackpadEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = ACTION_UP,
            actionIndex = 0,
            coordinate = lastPosition,
            delta = 1f,
            cursorInputState.scaleAccumulatedFactor!!,
            metaState = keyInputState.constructMetaState(),
        )
        enqueueTrackpadEnter()
    }

    fun KeyInputState.constructMetaState(): Int {

        fun genState(key: Key, mask: Int) = if (isKeyDown(key)) mask else 0

        return (if (capsLockOn) KeyEvent.META_CAPS_LOCK_ON else 0) or
            (if (numLockOn) KeyEvent.META_NUM_LOCK_ON else 0) or
            (if (scrollLockOn) KeyEvent.META_SCROLL_LOCK_ON else 0) or
            genState(Key.Function, KeyEvent.META_FUNCTION_ON) or
            genState(Key.CtrlLeft, KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_CTRL_ON) or
            genState(Key.CtrlRight, KeyEvent.META_CTRL_RIGHT_ON or KeyEvent.META_CTRL_ON) or
            genState(Key.AltLeft, KeyEvent.META_ALT_LEFT_ON or KeyEvent.META_ALT_ON) or
            genState(Key.AltRight, KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_ALT_ON) or
            genState(Key.MetaLeft, KeyEvent.META_META_LEFT_ON or KeyEvent.META_META_ON) or
            genState(Key.MetaRight, KeyEvent.META_META_RIGHT_ON or KeyEvent.META_META_ON) or
            genState(Key.ShiftLeft, KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON) or
            genState(Key.ShiftRight, KeyEvent.META_SHIFT_RIGHT_ON or KeyEvent.META_SHIFT_ON)
    }

    override fun KeyInputState.enqueueDown(key: Key) =
        enqueueKeyEvent(KeyEvent.ACTION_DOWN, key.nativeKeyCode, constructMetaState())

    override fun KeyInputState.enqueueUp(key: Key) =
        enqueueKeyEvent(KeyEvent.ACTION_UP, key.nativeKeyCode, constructMetaState())

    /**
     * Generates a MotionEvent with the given [action] and [actionIndex], adding all pointers that
     * are currently in the gesture, and adds the MotionEvent to the batch.
     *
     * @see MotionEvent.getAction
     * @see MotionEvent.getActionIndex
     */
    private fun PartialGesture.enqueueTouchEvent(action: Int, actionIndex: Int) {
        val entries = lastPositions.entries.sortedBy { it.key }
        enqueueTouchEvent(
            downTime = downTime,
            action = action,
            actionIndex = actionIndex,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = listOf(currentTime),
            coordinates = List(entries.size) { listOf(entries[it].value) },
        )
    }

    /** Generates an event with the given parameters. */
    private fun enqueueTouchEvent(
        downTime: Long,
        action: Int,
        actionIndex: Int,
        pointerIds: List<Int>,
        eventTimes: List<Long>,
        coordinates: List<List<Offset>>,
    ) {
        check(coordinates.size == pointerIds.size) {
            "Coordinates size should equal pointerIds size " +
                "(was: ${coordinates.size}, ${pointerIds.size})"
        }
        repeat(pointerIds.size) { pointerIndex ->
            check(eventTimes.size == coordinates[pointerIndex].size) {
                "Historical eventTimes size should equal coordinates[$pointerIndex] size " +
                    "(was: ${eventTimes.size}, ${coordinates[pointerIndex].size})"
            }
        }

        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue touch event (" +
                    "downTime=$downTime, " +
                    "action=$action, " +
                    "actionIndex=$actionIndex, " +
                    "pointerIds=$pointerIds, " +
                    "eventTimes=$eventTimes, " +
                    "coordinates=$coordinates)"
            }

            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }

            val motionEvent =
                MotionEvent.obtain(
                        /* downTime = */ downTime,
                        /* eventTime = */ eventTimes[0],
                        /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                        /* pointerCount = */ coordinates.size,
                        /* pointerProperties = */ Array(coordinates.size) { pointerIndex ->
                            PointerProperties().apply {
                                id = pointerIds[pointerIndex]
                                toolType = MotionEvent.TOOL_TYPE_FINGER
                            }
                        },
                        /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                            PointerCoords().apply {
                                val startOffset = coordinates[pointerIndex][0]

                                // Allows for non-valid numbers/Offsets to be passed along to
                                // Compose to
                                // test if it handles them properly (versus breaking here and not
                                // knowing if Compose properly handles these values).
                                x =
                                    if (startOffset.isValid()) {
                                        positionInScreen.x + startOffset.x
                                    } else {
                                        Float.NaN
                                    }

                                y =
                                    if (startOffset.isValid()) {
                                        positionInScreen.y + startOffset.y
                                    } else {
                                        Float.NaN
                                    }
                            }
                        },
                        /* metaState = */ 0,
                        /* buttonState = */ 0,
                        /* xPrecision = */ 1f,
                        /* yPrecision = */ 1f,
                        /* deviceId = */ 0,
                        /* edgeFlags = */ 0,
                        /* source = */ SOURCE_TOUCHSCREEN,
                        /* flags = */ 0,
                    )
                    .apply {
                        // The current time & coordinates are the last element in the lists, and
                        // need to
                        // be passed into the final addBatch call. If there are no historical
                        // events,
                        // the list sizes are 1 and we don't need to call addBatch at all.
                        for (timeIndex in 1 until eventTimes.size) {
                            addBatch(
                                /* eventTime = */ eventTimes[timeIndex],
                                /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                                    PointerCoords().apply {
                                        val currentOffset = coordinates[pointerIndex][timeIndex]

                                        // Allows for non-valid numbers/Offsets to be passed along
                                        // to Compose to test if it handles them properly (versus
                                        // breaking here and not knowing if Compose properly
                                        // handles these values).
                                        x =
                                            if (currentOffset.isValid()) {
                                                positionInScreen.x + currentOffset.x
                                            } else {
                                                Float.NaN
                                            }

                                        y =
                                            if (currentOffset.isValid()) {
                                                positionInScreen.y + currentOffset.y
                                            } else {
                                                Float.NaN
                                            }
                                    }
                                },
                                /* metaState = */ 0,
                            )
                        }
                        offsetLocation(-positionInScreen.x, -positionInScreen.y)
                    }
            batchedEvents.add(InputEventWithAdditionalInformation(motionEvent))
        }
    }

    /**
     * Generates a MotionEvent with the given [action] and [actionIndex], adding all pointers that
     * are currently in the gesture, and adds the MotionEvent to the batch.
     *
     * @see MotionEvent.getAction
     * @see MotionEvent.getActionIndex
     */
    private fun PartialIndirectGesture.enqueueIndirectPointerEvent(action: Int, actionIndex: Int) {
        val entries = lastPositions.entries.sortedBy { it.key }

        enqueueIndirectPointerEvent(
            downTime = downTime,
            action = action,
            actionIndex = actionIndex,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = listOf(currentTime),
            coordinates = List(entries.size) { listOf(entries[it].value) },
            additionalEventInformation = createAdditionalEventInformation(),
        )
    }

    private fun PartialIndirectGesture.createAdditionalEventInformation():
        AdditionalEventInformation {
        val previousMotionEvent =
            androidPlatformContext.previousMotionEventForIndirectPointerEventCreation?.let {
                when (it.actionMasked) {
                    // Reset previous event since new incoming event represents a new indirect
                    // event stream.
                    ACTION_CANCEL,
                    ACTION_UP -> {
                        androidPlatformContext.previousMotionEventForIndirectPointerEventCreation =
                            null
                        null
                    }
                    else -> it
                }
            }

        return AdditionalEventInformation(
            IndirectPointerEventAdditionalInformation(
                primaryDirectionalMotionAxis = indirectPointerEventPrimaryDirectionalMotionAxis,
                previousMotionEvent = previousMotionEvent?.let { MotionEvent.obtain(it) },
            )
        )
    }

    /** Generates a motion event with the given parameters. */
    @Suppress("PrimitiveInCollection")
    private fun enqueueIndirectPointerEvent(
        downTime: Long,
        action: Int,
        actionIndex: Int,
        pointerIds: List<Int>,
        eventTimes: List<Long>,
        coordinates: List<List<Offset>>,
        additionalEventInformation: AdditionalEventInformation,
    ) {
        check(coordinates.size == pointerIds.size) {
            "Coordinates size should equal pointerIds size " +
                "(was: ${coordinates.size}, ${pointerIds.size})"
        }
        repeat(pointerIds.size) { pointerIndex ->
            check(eventTimes.size == coordinates[pointerIndex].size) {
                "Historical eventTimes size should equal coordinates[$pointerIndex] size " +
                    "(was: ${eventTimes.size}, ${coordinates[pointerIndex].size})"
            }
        }

        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue touch event (" +
                    "downTime=$downTime, " +
                    "action=$action, " +
                    "actionIndex=$actionIndex, " +
                    "pointerIds=$pointerIds, " +
                    "eventTimes=$eventTimes, " +
                    "coordinates=$coordinates)"
            }

            val motionEvent =
                MotionEvent.obtain(
                        /* downTime = */ downTime,
                        /* eventTime = */ eventTimes[0],
                        /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                        /* pointerCount = */ coordinates.size,
                        /* pointerProperties = */ Array(coordinates.size) { pointerIndex ->
                            PointerProperties().apply {
                                id = pointerIds[pointerIndex]
                                toolType = MotionEvent.TOOL_TYPE_FINGER
                            }
                        },
                        /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                            PointerCoords().apply {
                                val startOffset = coordinates[pointerIndex][0]

                                // Allows for non-valid numbers/Offsets to be passed along to
                                // Compose to
                                // test if it handles them properly (versus breaking here and not
                                // knowing if Compose properly handles these values).
                                // Also, Indirect coordinates are not related to the screen, so we
                                // use
                                // the start offsets directly (that is, we do not localize the
                                // coordinates for indirect pointer events).
                                x =
                                    if (startOffset.isValid()) {
                                        startOffset.x
                                    } else {
                                        Float.NaN
                                    }

                                y =
                                    if (startOffset.isValid()) {
                                        startOffset.y
                                    } else {
                                        Float.NaN
                                    }
                            }
                        },
                        /* metaState = */ 0,
                        /* buttonState = */ 0,
                        /* xPrecision = */ 1f,
                        /* yPrecision = */ 1f,
                        /* deviceId = */ 0,
                        /* edgeFlags = */ 0,
                        /* source = */ SOURCE_TOUCH_NAVIGATION,
                        /* flags = */ 0,
                    )
                    .apply {
                        // The current time & coordinates are the last element in the lists, and
                        // need to be passed into the final addBatch call. If there are no
                        // historical events, the list sizes is one, and we don't need to call
                        // addBatch at all.
                        for (timeIndex in 1 until eventTimes.size) {
                            addBatch(
                                /* eventTime = */ eventTimes[timeIndex],
                                /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                                    PointerCoords().apply {
                                        val currentOffset = coordinates[pointerIndex][timeIndex]

                                        // Allows for non-valid numbers/Offsets to be passed along
                                        // to Compose to test if it handles them properly (versus
                                        // breaking here and not knowing if Compose properly
                                        // handles these values).
                                        // Also, Indirect coordinates are not related to the screen,
                                        // so we use the start offsets directly (that is, we do not
                                        // localize the coordinates for indirect pointer events).
                                        x =
                                            if (currentOffset.isValid()) {
                                                currentOffset.x
                                            } else {
                                                Float.NaN
                                            }

                                        y =
                                            if (currentOffset.isValid()) {
                                                currentOffset.y
                                            } else {
                                                Float.NaN
                                            }
                                    }
                                },
                                /* metaState = */ 0,
                            )
                        }
                    }

            batchedEvents.add(
                InputEventWithAdditionalInformation(motionEvent, additionalEventInformation)
            )

            androidPlatformContext.previousMotionEventForIndirectPointerEventCreation = motionEvent
        }
    }

    private fun CursorInputState.enqueueMouseEvent(action: Int, delta: Float = 0f, axis: Int = -1) {
        enqueueMouseEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = action,
            coordinate = lastPosition,
            metaState = keyInputState.constructMetaState(),
            buttonState = pressedButtons.fold(0) { state, buttonId -> state or buttonId },
            axis = axis,
            axisDelta = delta,
        )
    }

    private fun enqueueMouseEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        coordinate: Offset,
        metaState: Int,
        buttonState: Int,
        axis: Int = -1,
        axisDelta: Float = 0f,
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue mouse event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "coordinate=$coordinate, " +
                    "metaState=$metaState, " +
                    "buttonState=$buttonState, " +
                    "axis=$axis, " +
                    "axisDelta=$axisDelta)"
            }
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }

            val motionEvent =
                MotionEvent.obtain(
                        /* downTime = */ downTime,
                        /* eventTime = */ eventTime,
                        /* action = */ action,
                        /* pointerCount = */ 1,
                        /* pointerProperties = */ arrayOf(
                            PointerProperties().apply {
                                id = 0
                                toolType = MotionEvent.TOOL_TYPE_MOUSE
                            }
                        ),
                        /* pointerCoords = */ arrayOf(
                            PointerCoords().apply {
                                x = positionInScreen.x + coordinate.x
                                y = positionInScreen.y + coordinate.y
                                if (axis != -1) {
                                    setAxisValue(axis, axisDelta)
                                }
                            }
                        ),
                        /* metaState = */ metaState,
                        /* buttonState = */ buttonState,
                        /* xPrecision = */ 1f,
                        /* yPrecision = */ 1f,
                        /* deviceId = */ 0,
                        /* edgeFlags = */ 0,
                        /* source = */ SOURCE_MOUSE,
                        /* flags = */ 0,
                    )
                    .apply { offsetLocation(-positionInScreen.x, -positionInScreen.y) }

            batchedEvents.add(InputEventWithAdditionalInformation(motionEvent))
        }
    }

    private fun enqueueMouseEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        coordinate: Offset,
        metaState: Int,
        buttonState: Int,
        axis: Int = -1,
        delta: Offset,
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue mouse event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "coordinate=$coordinate, " +
                    "metaState=$metaState, " +
                    "buttonState=$buttonState, " +
                    "axis=$axis, " +
                    "delta=$delta)"
            }
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }

            val motionEvent =
                MotionEvent.obtain(
                        /* downTime = */ downTime,
                        /* eventTime = */ eventTime,
                        /* action = */ action,
                        /* pointerCount = */ 1,
                        /* pointerProperties = */ arrayOf(
                            PointerProperties().apply {
                                id = 0
                                toolType = MotionEvent.TOOL_TYPE_MOUSE
                            }
                        ),
                        /* pointerCoords = */ arrayOf(
                            PointerCoords().apply {
                                x = positionInScreen.x + coordinate.x
                                y = positionInScreen.y + coordinate.y
                                setAxisValue(MotionEvent.AXIS_HSCROLL, delta.x)
                                setAxisValue(MotionEvent.AXIS_VSCROLL, delta.y)
                            }
                        ),
                        /* metaState = */ metaState,
                        /* buttonState = */ buttonState,
                        /* xPrecision = */ 1f,
                        /* yPrecision = */ 1f,
                        /* deviceId = */ 0,
                        /* edgeFlags = */ 0,
                        /* source = */ SOURCE_MOUSE,
                        /* flags = */ 0,
                    )
                    .apply { offsetLocation(-positionInScreen.x, -positionInScreen.y) }

            batchedEvents.add(InputEventWithAdditionalInformation(motionEvent))
        }
    }

    private fun CursorInputState.enqueueTrackpadEvent(action: Int) {
        enqueueTrackpadEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = action,
            coordinate = lastPosition,
            metaState = keyInputState.constructMetaState(),
            buttonState = pressedButtons.fold(0) { state, buttonId -> state or buttonId },
        )
    }

    private fun enqueueTrackpadEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        coordinate: Offset,
        metaState: Int,
        buttonState: Int,
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue trackpad event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "coordinate=$coordinate, " +
                    "metaState=$metaState, " +
                    "buttonState=$buttonState)"
            }
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }

            val motionEvent =
                MotionEvent.obtain(
                        /* downTime = */ downTime,
                        /* eventTime = */ eventTime,
                        /* action = */ action,
                        /* pointerCount = */ 1,
                        /* pointerProperties = */ arrayOf(
                            PointerProperties().apply {
                                id = 0
                                toolType = MotionEvent.TOOL_TYPE_FINGER
                            }
                        ),
                        /* pointerCoords = */ arrayOf(
                            PointerCoords().apply {
                                x = positionInScreen.x + coordinate.x
                                y = positionInScreen.y + coordinate.y
                            }
                        ),
                        /* metaState = */ metaState,
                        /* buttonState = */ buttonState,
                        /* xPrecision = */ 1f,
                        /* yPrecision = */ 1f,
                        /* deviceId = */ 0,
                        /* edgeFlags = */ 0,
                        /* source = */ SOURCE_MOUSE,
                        /* flags = */ 0,
                    )
                    .apply { offsetLocation(-positionInScreen.x, -positionInScreen.y) }

            batchedEvents.add(InputEventWithAdditionalInformation(motionEvent))
        }
    }

    private fun enqueueTwoFingerSwipeTrackpadEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        coordinate: Offset,
        delta: Offset,
        accumulatedDelta: Offset,
        metaState: Int,
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue trackpad event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "coordinate=$coordinate, " +
                    "delta=$delta, " +
                    "accumulatedDelta=$accumulatedDelta, " +
                    "metaState=$metaState)"
            }
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }

            val motionEvent =
                if (Build.VERSION.SDK_INT >= 34) {
                        MotionEvent.obtain(
                            /* downTime = */ downTime,
                            /* eventTime = */ eventTime,
                            /* action = */ action,
                            /* pointerCount = */ 1,
                            /* pointerProperties = */ arrayOf(
                                PointerProperties().apply {
                                    id = 0
                                    toolType = MotionEvent.TOOL_TYPE_FINGER
                                }
                            ),
                            /* pointerCoords = */ arrayOf(
                                PointerCoords().apply {
                                    x = positionInScreen.x + coordinate.x + accumulatedDelta.x
                                    y = positionInScreen.y + coordinate.y + accumulatedDelta.y
                                    setAxisValue(
                                        MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE,
                                        -delta.x,
                                    )
                                    setAxisValue(
                                        MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE,
                                        -delta.y,
                                    )
                                }
                            ),
                            /* metaState = */ metaState,
                            /* buttonState = */ 0,
                            /* xPrecision = */ 1f,
                            /* yPrecision = */ 1f,
                            /* deviceId = */ 0,
                            /* edgeFlags = */ 0,
                            /* source = */ SOURCE_MOUSE,
                            /* displayId = */ DEFAULT_DISPLAY,
                            /* flags = */ 0,
                            /* classification = */ MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
                        )!!
                    } else {
                        MotionEvent.obtain(
                            /* downTime = */ downTime,
                            /* eventTime = */ eventTime,
                            /* action = */ action,
                            /* pointerCount = */ 1,
                            /* pointerProperties = */ arrayOf(
                                PointerProperties().apply {
                                    id = 0
                                    toolType = MotionEvent.TOOL_TYPE_FINGER
                                }
                            ),
                            /* pointerCoords = */ arrayOf(
                                PointerCoords().apply {
                                    x = positionInScreen.x + coordinate.x + accumulatedDelta.x
                                    y = positionInScreen.y + coordinate.y + accumulatedDelta.y
                                    setAxisValue(
                                        MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE,
                                        -delta.x,
                                    )
                                    setAxisValue(
                                        MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE,
                                        -delta.y,
                                    )
                                }
                            ),
                            /* metaState = */ metaState,
                            /* buttonState = */ 0,
                            /* xPrecision = */ 1f,
                            /* yPrecision = */ 1f,
                            /* deviceId = */ 0,
                            /* edgeFlags = */ 0,
                            /* source = */ SOURCE_MOUSE,
                            /* flags = */ 0,
                        )
                    }
                    .apply { offsetLocation(-positionInScreen.x, -positionInScreen.y) }

            batchedEvents.add(InputEventWithAdditionalInformation(motionEvent))
        }
    }

    private fun enqueuePinchTrackpadEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        actionIndex: Int,
        coordinate: Offset,
        delta: Float,
        accumulatedDelta: Float,
        metaState: Int,
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue trackpad event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "coordinate=$coordinate, " +
                    "delta=$delta, " +
                    "accumulatedDelta=$accumulatedDelta, " +
                    "metaState=$metaState)"
            }
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }
            val pointerCount: Int
            val pointerProperties: Array<PointerProperties>
            val pointerCoords: Array<PointerCoords>

            /**
             * The initial pinch separation for the fingers as per
             * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/native/services/inputflinger/reader/mapper/gestures/GestureConverter.h;l=133;drc=af66cee7e92a59e81d1ea4b8872fa2e418a97599
             */
            val initialPinchSeparation = 200
            val scaledSeparationFromCursorPosition = (initialPinchSeparation * accumulatedDelta) / 2

            val firstPointerCoords =
                PointerCoords().apply {
                    x = positionInScreen.x + coordinate.x - scaledSeparationFromCursorPosition
                    y = positionInScreen.y + coordinate.y
                    setAxisValue(MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR, delta)
                }
            val secondPointerCoords =
                PointerCoords().apply {
                    x = positionInScreen.x + coordinate.x + scaledSeparationFromCursorPosition
                    y = positionInScreen.y + coordinate.y
                }

            if (action == ACTION_DOWN || action == ACTION_UP) {
                pointerCount = 1
                pointerProperties =
                    arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    )
                pointerCoords = arrayOf(firstPointerCoords)
            } else {
                pointerCount = 2
                pointerProperties =
                    arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        },
                        PointerProperties().apply {
                            id = 1
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        },
                    )
                pointerCoords = arrayOf(firstPointerCoords, secondPointerCoords)
            }

            val motionEvent =
                if (Build.VERSION.SDK_INT >= 34) {
                        MotionEvent.obtain(
                            /* downTime = */ downTime,
                            /* eventTime = */ eventTime,
                            /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                            /* pointerCount = */ pointerCount,
                            /* pointerProperties = */ pointerProperties,
                            /* pointerCoords = */ pointerCoords,
                            /* metaState = */ metaState,
                            /* buttonState = */ 0,
                            /* xPrecision = */ 1f,
                            /* yPrecision = */ 1f,
                            /* deviceId = */ 0,
                            /* edgeFlags = */ 0,
                            /* source = */ SOURCE_MOUSE,
                            /* displayId = */ DEFAULT_DISPLAY,
                            /* flags = */ 0,
                            /* classification = */ MotionEvent.CLASSIFICATION_PINCH,
                        )!!
                    } else {
                        MotionEvent.obtain(
                            /* downTime = */ downTime,
                            /* eventTime = */ eventTime,
                            /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                            /* pointerCount = */ pointerCount,
                            /* pointerProperties = */ pointerProperties,
                            /* pointerCoords = */ pointerCoords,
                            /* metaState = */ metaState,
                            /* buttonState = */ 0,
                            /* xPrecision = */ 1f,
                            /* yPrecision = */ 1f,
                            /* deviceId = */ 0,
                            /* edgeFlags = */ 0,
                            /* source = */ SOURCE_MOUSE,
                            /* flags = */ 0,
                        )!!
                    }
                    .apply { offsetLocation(-positionInScreen.x, -positionInScreen.y) }

            batchedEvents.add(InputEventWithAdditionalInformation(motionEvent))
        }
    }

    override fun RotaryInputState.enqueueRotaryScrollHorizontally(horizontalScrollPixels: Float) {
        enqueueRotaryScrollEvent(
            eventTime = currentTime,
            scrollPixels = -horizontalScrollPixels / horizontalScrollFactor,
        )
    }

    override fun RotaryInputState.enqueueRotaryScrollVertically(verticalScrollPixels: Float) {
        enqueueRotaryScrollEvent(
            eventTime = currentTime,
            scrollPixels = -verticalScrollPixels / verticalScrollFactor,
        )
    }

    private fun enqueueRotaryScrollEvent(eventTime: Long, scrollPixels: Float) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue rotary scroll event (" +
                    "eventTime=$eventTime, " +
                    "scrollDelta=$scrollPixels)"
            }

            val motionEvent =
                MotionEvent.obtain(
                    /* downTime = */ 0,
                    /* eventTime = */ eventTime,
                    /* action = */ ACTION_SCROLL,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = TOOL_TYPE_UNKNOWN
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply { setAxisValue(AXIS_SCROLL, scrollPixels) }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ findInputDevice(root.view.context, SOURCE_ROTARY_ENCODER),
                    /* edgeFlags = */ 0,
                    /* source = */ SOURCE_ROTARY_ENCODER,
                    /* flags = */ 0,
                )

            batchedEvents.add(InputEventWithAdditionalInformation(motionEvent))
        }
    }

    /**
     * Generates a KeyEvent with the given [action] and [keyCode] and adds the KeyEvent to the
     * batch.
     *
     * @see KeyEvent.getAction
     * @see KeyEvent.getKeyCode
     */
    private fun KeyInputState.enqueueKeyEvent(action: Int, keyCode: Int, metaState: Int) {
        enqueueKeyEvent(
            downTime = downTime,
            eventTime = currentTime,
            action = action,
            code = keyCode,
            repeat = repeatCount,
            metaState = metaState,
        )
    }

    /** Generates a key event with the given parameters. */
    private fun enqueueKeyEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        code: Int,
        repeat: Int,
        metaState: Int,
    ) {
        synchronized(batchLock) {
            ensureNotDisposed {
                "Can't enqueue key event (" +
                    "downTime=$downTime, " +
                    "eventTime=$eventTime, " +
                    "action=$action, " +
                    "code=$code, " +
                    "repeat=$repeat, " +
                    "metaState=$metaState)"
            }

            val keyEvent =
                KeyEvent(
                    /* downTime = */ downTime,
                    /* eventTime = */ eventTime,
                    /* action = */ action,
                    /* code = */ code,
                    /* repeat = */ repeat,
                    /* metaState = */ metaState,
                    /* deviceId = */ KeyCharacterMap.VIRTUAL_KEYBOARD,
                    /* scancode = */ 0,
                )

            batchedEvents.add(InputEventWithAdditionalInformation(keyEvent))
        }
    }

    override fun flush() {
        // Must inject on the main thread, because it might modify View properties
        testContext.testOwner.runOnUiThread {
            val events =
                synchronized(batchLock) {
                    ensureNotDisposed { "Can't flush events" }
                    mutableListOf<InputEventWithAdditionalInformation>().apply {
                        addAll(batchedEvents)
                        batchedEvents.clear()
                    }
                }

            events.forEach { event ->
                // Before injecting the next event, pump the clock
                // by the difference between this and the last event
                advanceClockTime(event.inputEvent.eventTime - lastDispatchedEventTime)
                lastDispatchedEventTime = event.inputEvent.eventTime
                sendAndRecycleEvent(event)
            }
            // Run all due tasks to make sure the last event is being handled.
            // This is necessary in case we're on a confined scheduler.
            testContext.testOwner.runCurrent()
        }
    }

    private fun advanceClockTime(millis: Long) {
        // Don't bother advancing the clock if there's nothing to advance
        if (millis > 0) {
            testContext.testOwner.mainClock.advanceTimeBy(millis, ignoreFrameDuration = true)
        }
    }

    private fun ensureNotDisposed(lazyMessage: () -> String) {
        check(!disposed) { "${lazyMessage()}, AndroidInputDispatcher has already been disposed" }
    }

    override fun onDispose() {
        synchronized(batchLock) {
            if (!disposed) {
                disposed = true
                batchedEvents.forEach { recycleEventIfPossible(it) }
            }
        }
    }

    /** Sends and recycles the given [InputEventWithAdditionalInformation]. */
    private fun sendAndRecycleEvent(
        inputEventWithAdditionalInformation: InputEventWithAdditionalInformation
    ) {
        try {
            sendEvent(inputEventWithAdditionalInformation)
        } finally {
            recycleEventIfPossible(inputEventWithAdditionalInformation)
        }
    }

    /**
     * Recycles the [InputEventWithAdditionalInformation] if it is a [MotionEvent]. There is no
     * notion of recycling a [KeyEvent].
     */
    private fun recycleEventIfPossible(
        inputEventWithAdditionalInformation: InputEventWithAdditionalInformation
    ) {
        (inputEventWithAdditionalInformation.inputEvent as? MotionEvent)?.recycle()
    }

    private fun findInputDevice(context: Context, source: Int): Int {
        with(context.getSystemService(Context.INPUT_SERVICE) as InputManager) {
            inputDeviceIds.forEach { deviceId ->
                getInputDevice(deviceId)?.apply {
                    motionRanges
                        .find { it.source == source }
                        ?.let {
                            return deviceId
                        }
                }
            }
        }
        return 0
    }
}
