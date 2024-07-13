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

package androidx.compose.ui.benchmark.input.pointer

import android.view.InputDevice.SOURCE_TOUCHSCREEN
import android.view.MotionEvent
import android.view.View

internal const val DefaultPointerInputMoveTimeDelta = 100
internal const val DefaultPointerInputMoveAmountPx = 10f

/**
 * Creates a simple [MotionEvent].
 *
 * @param dispatchTarget The [View] that the [MotionEvent] is going to be dispatched to. This
 *   guarantees that the MotionEvent is created correctly for both Compose (which relies on raw
 *   coordinates being correct) and Android (which requires that local coordinates are correct).
 */
internal fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    dispatchTarget: View
): MotionEvent {

    // It's important we get the absolute coordinates first for the construction of the MotionEvent,
    // and after it is created, adjust it back to the local coordinates. This way there is a history
    // of the absolute coordinates for developers who rely on that (ViewGroup does this as well).
    val locationOnScreen = IntArray(2) { 0 }
    dispatchTarget.getLocationOnScreen(locationOnScreen)

    pointerCoords.forEach {
        it.x += locationOnScreen[0]
        it.y += locationOnScreen[1]
    }

    val motionEvent =
        MotionEvent.obtain(
                0,
                eventTime.toLong(),
                action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                numPointers,
                pointerProperties,
                pointerCoords,
                0,
                0,
                0f,
                0f,
                0,
                0,
                SOURCE_TOUCHSCREEN, // Required for offsetLocation() to work correctly
                0
            )
            .apply {
                offsetLocation(-locationOnScreen[0].toFloat(), -locationOnScreen[1].toFloat())
            }

    pointerCoords.forEach {
        it.x -= locationOnScreen[0]
        it.y -= locationOnScreen[1]
    }

    return motionEvent
}

@Suppress("RemoveRedundantQualifierName")
internal fun PointerProperties(id: Int) = MotionEvent.PointerProperties().apply { this.id = id }

@Suppress("RemoveRedundantQualifierName")
internal fun PointerCoords(x: Float, y: Float) =
    MotionEvent.PointerCoords().apply {
        this.x = x
        this.y = y
    }

/**
 * Creates an array of subsequent [MotionEvent.ACTION_MOVE]s to pair with a
 * [MotionEvent.ACTION_DOWN] and a [MotionEvent.ACTION_UP] to recreate a user input sequence.
 *
 * @param initialX Starting x coordinate for the first [MotionEvent.ACTION_MOVE]
 * @param initialTime Starting time for the first [MotionEvent.ACTION_MOVE]
 * @param y - Y used for all [MotionEvent.ACTION_MOVE]s (only x is updated for each moves).
 * @param rootView - [View] that the [MotionEvent] is dispatched to.
 * @param numberOfMoveEvents Number of [MotionEvent.ACTION_MOVE]s to create.
 * @param enableFlingStyleHistory - Adds a history of [MotionEvent.ACTION_MOVE]s to each
 *   [MotionEvent.ACTION_MOVE] to mirror a fling event (where you will get more
 *   [MotionEvent.ACTION_MOVE]s than the refresh rate of the phone).
 * @param timeDelta - Time between each [MotionEvent.ACTION_MOVE] in milliseconds.
 * @param moveDelta - Amount to move in pixels for each [MotionEvent.ACTION_MOVE]
 */
internal fun createMoveMotionEvents(
    initialX: Float,
    initialTime: Int,
    y: Float, // Same Y used for all moves
    rootView: View,
    numberOfMoveEvents: Int,
    enableFlingStyleHistory: Boolean = false,
    timeDelta: Int = 100,
    moveDelta: Float = DefaultPointerInputMoveAmountPx
): Triple<Int, Float, Array<MotionEvent>> {
    var time = initialTime
    var x = initialX

    val moveMotionEvents =
        Array(numberOfMoveEvents) { index ->
            val move =
                if (enableFlingStyleHistory) {
                    val historicalEventCount = numberOfHistoricalEventsBasedOnArrayLocation(index)

                    // Move the time and x to the last known time (to start the historical time)
                    // and offset them so they do not conflict with the last Down or Move event.
                    var historicalTime: Int = time - timeDelta + 10

                    var accountForMoveOffset = -1
                    var historicalX =
                        if (moveDelta > 0) {
                            // accountForMoveOffset stays -1 (to account for +1 offset [below])
                            x - moveDelta + 1
                        } else {
                            // accountForMoveOffset changes to 1 (to account for -1 offset
                            // [below])
                            accountForMoveOffset = 1
                            x - moveDelta - 1
                        }

                    val historicalTimeDelta: Int = (timeDelta - 10) / historicalEventCount
                    val historicalXDelta: Float =
                        (moveDelta + accountForMoveOffset) / historicalEventCount

                    // First "historical" event (it will be pushed into history once another is
                    // added via `addBatch()`).
                    val moveWithHistory =
                        MotionEvent(
                            historicalTime,
                            MotionEvent.ACTION_MOVE,
                            1,
                            0,
                            arrayOf(PointerProperties(0)),
                            arrayOf(PointerCoords(historicalX, y)),
                            rootView
                        )

                    // Start on the second historical event (1), since the event added when we
                    // created the [MotionEvent] above will be pushed into the history and will
                    // then become the first historical event.
                    for (historyIndex in 1 until historicalEventCount) {
                        historicalTime += historicalTimeDelta
                        historicalX += historicalXDelta

                        moveWithHistory.addBatch(
                            historicalTime.toLong(),
                            arrayOf(PointerCoords(historicalX, y)),
                            0
                        )
                    }

                    // Since The event's current location, position and size are updated to
                    // the last values added via `addBatch()`, we need to add the main
                    // [MotionEvent] time, x, and y values last.
                    moveWithHistory.addBatch(time.toLong(), arrayOf(PointerCoords(x, y)), 0)
                    // return move event with history added
                    moveWithHistory
                } else {
                    MotionEvent(
                        time,
                        MotionEvent.ACTION_MOVE,
                        1,
                        0,
                        arrayOf(PointerProperties(0)),
                        arrayOf(PointerCoords(x, y)),
                        rootView
                    )
                }

            time += timeDelta
            x += moveDelta
            move
        }
    return Triple(time, x, moveMotionEvents)
}

/*
 * Based on traces of fling events, the first events in a series of "MOVES" have more
 * historical [MotionEvent]s than the subsequent events.
 *
 * Remember, historical events within a [MotionEvent] represent extra [MotionEvent]s
 * that occurred faster than the refresh rate of the phone. A fling will have many more events
 * in the beginning (and between the refresh rate since they are happening so quick) than in
 * the end.
 */
internal fun numberOfHistoricalEventsBasedOnArrayLocation(index: Int): Int {
    return when (index) {
        0 -> 12
        1 -> 9
        2,
        3 -> 4
        else -> 2
    }
}
