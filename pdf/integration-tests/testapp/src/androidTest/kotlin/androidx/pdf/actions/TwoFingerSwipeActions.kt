/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.actions

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

internal abstract class TwoFingerSwipeAction(
    private val startYPercent: Float,
    private val endYPercent: Float,
    private val actionDescription: String,
) : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isEnabled()
    }

    override fun getDescription(): String {
        return actionDescription
    }

    override fun perform(uiController: UiController, view: View) {
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()

        val centerX = viewWidth * 0.5f
        val startY = viewHeight * startYPercent
        val endY = viewHeight * endYPercent

        // Coordinates for Pointer 0 (first finger)
        val p0X = centerX - FINGER_HORIZONTAL_OFFSET_PX

        // Coordinates for Pointer 1 (second finger)
        val p1X = centerX + FINGER_HORIZONTAL_OFFSET_PX

        val stepIntervalMs = DEFAULT_SWIPE_DURATION_MS / NUM_STEPS.toLong()
        val downTime = SystemClock.uptimeMillis()
        var currentEventTime = downTime

        // Pointer properties
        val p0Props = MotionEvent.PointerProperties().apply { id = POINTER_ID_0 }
        val p1Props = MotionEvent.PointerProperties().apply { id = POINTER_ID_1 }

        // Initial coordinates for ACTION_DOWN and ACTION_POINTER_DOWN
        val p0InitialCoords = createPointerCoords(p0X, startY)
        val p1InitialCoords = createPointerCoords(p1X, startY)

        with(uiController) {
            // 1. First finger (pointer 0) touches down
            dispatchSingleMotionEvent(
                downTime,
                currentEventTime,
                MotionEvent.ACTION_DOWN,
                listOf(p0Props to p0InitialCoords),
            )

            // 2. Second finger (pointer 1) touches down
            currentEventTime += POINTER_EVENT_DELAY_MS
            val actionPointer1Down =
                MotionEvent.ACTION_POINTER_DOWN or
                    (POINTER_INDEX_1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            dispatchSingleMotionEvent(
                downTime,
                currentEventTime,
                actionPointer1Down,
                listOf(p0Props to p0InitialCoords, p1Props to p1InitialCoords),
            )

            // 3. Both fingers move (swipe)
            val moveEvents = mutableListOf<MotionEvent>()
            val currentP0MoveCoords =
                MotionEvent.PointerCoords().apply { copyFrom(p0InitialCoords) }
            val currentP1MoveCoords =
                MotionEvent.PointerCoords().apply { copyFrom(p1InitialCoords) }
            val allPointerProps = arrayOf(p0Props, p1Props)
            val currentPointerCoordsArray = arrayOf(currentP0MoveCoords, currentP1MoveCoords)

            for (i in 1..NUM_STEPS) {
                currentEventTime += stepIntervalMs
                val progress = i.toFloat() / NUM_STEPS
                val interpolatedY = startY + (endY - startY) * progress

                currentP0MoveCoords.y = interpolatedY
                currentP1MoveCoords.y = interpolatedY

                val moveEvent =
                    createMotionEvent(
                        downTime,
                        currentEventTime,
                        MotionEvent.ACTION_MOVE,
                        allPointerProps,
                        currentPointerCoordsArray,
                    )
                moveEvents.add(moveEvent)
            }

            if (moveEvents.isNotEmpty()) {
                injectMotionEventSequence(moveEvents)
                moveEvents.forEach { it.recycle() }
            }

            // Final coordinates for UP events (Y has changed, X remains the same)
            val p0FinalCoords = createPointerCoords(p0X, endY)
            val p1FinalCoords = createPointerCoords(p1X, endY)

            // 4. Second finger (pointer 1) lifts up
            currentEventTime += POINTER_EVENT_DELAY_MS
            val actionPointer1Up =
                MotionEvent.ACTION_POINTER_UP or
                    (POINTER_INDEX_1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            dispatchSingleMotionEvent(
                downTime,
                currentEventTime,
                actionPointer1Up,
                listOf(p0Props to p0FinalCoords, p1Props to p1FinalCoords),
            )

            // 5. First finger (pointer 0) lifts up
            currentEventTime += POINTER_EVENT_DELAY_MS
            dispatchSingleMotionEvent(
                downTime,
                currentEventTime,
                MotionEvent.ACTION_UP,
                listOf(p0Props to p0FinalCoords),
            )
        }
    }

    private fun createPointerCoords(
        x: Float,
        y: Float,
        pressure: Float = 1f,
        size: Float = 1f,
    ): MotionEvent.PointerCoords {
        return MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            this.pressure = pressure
            this.size = size
        }
    }

    private fun createMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        pointerProps: Array<MotionEvent.PointerProperties>,
        pointerCoords: Array<MotionEvent.PointerCoords>,
    ): MotionEvent {
        return MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            pointerProps.size,
            pointerProps,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0,
        )
    }

    private fun UiController.dispatchSingleMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        activePointers: List<Pair<MotionEvent.PointerProperties, MotionEvent.PointerCoords>>,
    ) {
        val pointerPropsArray = activePointers.map { it.first }.toTypedArray()
        val pointerCoordsArray = activePointers.map { it.second }.toTypedArray()

        val event =
            createMotionEvent(downTime, eventTime, action, pointerPropsArray, pointerCoordsArray)
        this.injectMotionEventSequence(listOf(event))
        event.recycle()
    }

    companion object {
        private const val POINTER_ID_0 = 0
        private const val POINTER_ID_1 = 1
        private const val POINTER_INDEX_1 = 1

        // Default gesture parameters
        private const val DEFAULT_SWIPE_DURATION_MS = 300L
        private const val NUM_STEPS = 10 // Number of move steps in the swipe
        private const val FINGER_HORIZONTAL_OFFSET_PX = 10f
        private const val POINTER_EVENT_DELAY_MS = 10L
    }
}

internal class TwoFingerSwipeUpAction :
    TwoFingerSwipeAction(
        startYPercent = 0.8f, // Start from 80% from top
        endYPercent = 0.2f, // End at 20% from top
        actionDescription = "Perform a two-finger swipe up",
    )

internal class TwoFingerSwipeDownAction :
    TwoFingerSwipeAction(
        startYPercent = 0.2f, // Start from 20% from top
        endYPercent = 0.8f, // End at 80% from top
        actionDescription = "Perform a two-finger swipe down",
    )
