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
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.View

internal const val DefaultPointerInputTimeDelta = 100
internal const val DefaultPointerInputMoveAmountPx = 10f

internal data class BenchmarkSimplifiedPointerInputPointer(val id: Int, val x: Float, val y: Float)

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
 * Creates an array of down [MotionEvent]s meaning the first event is a [MotionEvent.ACTION_DOWN]
 * and all following events (if there are any) are [MotionEvent.ACTION_POINTER_DOWN]s. These will
 * usually be paired with a [MotionEvent] up events.
 *
 * @param initialX Starting x coordinate for the first [MotionEvent]
 * @param initialTime Starting time for the first [MotionEvent]
 * @param y - Y used for all [MotionEvent]s (only x is updated for each moves).
 * @param rootView - [View] that the [MotionEvent] is dispatched to.
 * @param numberOfEvents Number of [MotionEvent]s to create.
 * @param timeDelta - Time between each [MotionEvent] in milliseconds.
 * @param moveDelta - Amount to move in pixels for each [MotionEvent]
 */
internal fun createDowns(
    initialX: Float,
    initialTime: Int,
    y: Float, // Same Y used for all moves
    rootView: View,
    numberOfEvents: Int = 1,
    timeDelta: Int = DefaultPointerInputTimeDelta,
    moveDelta: Float = DefaultPointerInputMoveAmountPx
): Array<MotionEvent> {
    if (numberOfEvents < 1) {
        return emptyArray()
    }

    var time = initialTime
    var x = initialX

    val pointerProperties = mutableListOf<PointerProperties>()
    val pointerCoords = mutableListOf<PointerCoords>()

    val downMotionEvents =
        Array(numberOfEvents) { index ->
            // Add pointers as we create down events
            pointerProperties.add(index, PointerProperties(index))
            pointerCoords.add(index, PointerCoords(x, y))

            val down =
                MotionEvent(
                    time,
                    if (index == 0) {
                        MotionEvent.ACTION_DOWN
                    } else {
                        MotionEvent.ACTION_POINTER_DOWN
                    },
                    index + 1,
                    index, // Used in conjunction with ACTION_POINTER_DOWN/UP
                    pointerProperties.toTypedArray(),
                    pointerCoords.toTypedArray(),
                    rootView
                )

            time += timeDelta
            x += moveDelta

            down // return down event
        }
    return downMotionEvents
}

/**
 * Creates an array of up [MotionEvent]s meaning alls up events are [MotionEvent.ACTION_POINTER_UP]s
 * minus the last one (which is a [MotionEvent.ACTION_UP]). These will usually be paired with a
 * [MotionEvent] down events.
 *
 * @param initialTime Starting time for the first [MotionEvent]
 * @param initialPointers All pointers to create set of up events
 * @param rootView - [View] that the [MotionEvent] is dispatched to.
 * @param timeDelta - Time between each [MotionEvent] in milliseconds.
 */
internal fun createUps(
    initialTime: Int,
    initialPointers: Array<BenchmarkSimplifiedPointerInputPointer>,
    rootView: View,
    timeDelta: Int = DefaultPointerInputTimeDelta
): Array<MotionEvent> {
    if (initialPointers.isEmpty()) {
        return emptyArray()
    }

    var time = initialTime

    val pointerProperties = mutableListOf<PointerProperties>()
    val pointerCoords = mutableListOf<PointerCoords>()

    // Convert simplified pointers to actual PointerProperties and PointerCoords.
    for ((index, initialPointer) in initialPointers.withIndex()) {
        pointerProperties.add(index, PointerProperties(initialPointer.id))
        pointerCoords.add(index, PointerCoords(initialPointer.x, initialPointer.y))
    }

    val upMotionEvents =
        Array(initialPointers.size) { index ->
            // Only the last element should be an ACTION_UP
            val action =
                if (index == initialPointers.size - 1) {
                    MotionEvent.ACTION_UP
                } else {
                    MotionEvent.ACTION_POINTER_UP
                }

            val numberOfPointers = initialPointers.size - index

            val up =
                MotionEvent(
                    time,
                    action,
                    numberOfPointers,
                    numberOfPointers - 1, // Used with ACTION_POINTER_DOWN/UP
                    pointerProperties.toTypedArray(),
                    pointerCoords.toTypedArray(),
                    rootView
                )

            // Update time for next ACTION_UP/ACTION_POINTER_UP
            time += timeDelta

            // The next ACTION_UP/ACTION_POINTER_UP will have one less pointer, so we remove the
            // last element from both lists.
            if (pointerProperties.isNotEmpty()) {
                pointerProperties.removeAt(pointerProperties.size - 1)
            }

            if (pointerCoords.isNotEmpty()) {
                pointerCoords.removeAt(pointerCoords.size - 1)
            }
            up // return up event
        }
    return upMotionEvents
}

/**
 * Creates an array of subsequent [MotionEvent.ACTION_MOVE]s to pair with a
 * [MotionEvent.ACTION_DOWN] and a [MotionEvent.ACTION_UP] to recreate a user input sequence. Note:
 * We offset pointers/events by time and x only (y stays the same).
 *
 * @param initialTime Starting time for the first [MotionEvent.ACTION_MOVE]
 * @param initialPointers Starting coordinates for all [MotionEvent.ACTION_MOVE] pointers
 * @param rootView - [View] that the [MotionEvent] is dispatched to.
 * @param numberOfMoveEvents Number of [MotionEvent.ACTION_MOVE]s to create.
 * @param enableFlingStyleHistory - Adds a history of [MotionEvent.ACTION_MOVE]s to each
 *   [MotionEvent.ACTION_MOVE] to mirror a fling event (where you will get more
 *   [MotionEvent.ACTION_MOVE]s than the refresh rate of the phone).
 * @param timeDelta - Time between each [MotionEvent.ACTION_MOVE] in milliseconds.
 * @param moveDelta - Amount to move in pixels for each [MotionEvent.ACTION_MOVE]
 */
internal fun createMoveMotionEvents(
    initialTime: Int,
    initialPointers: Array<BenchmarkSimplifiedPointerInputPointer>,
    rootView: View,
    numberOfMoveEvents: Int,
    enableFlingStyleHistory: Boolean = false,
    timeDelta: Int = DefaultPointerInputTimeDelta,
    moveDelta: Float = DefaultPointerInputMoveAmountPx
): Array<MotionEvent> {

    var time = initialTime

    // Creates a list of pointer properties and coordinates from initialPointers to represent
    // all pointers/fingers in the [MotionEvent] we create.
    val pointerProperties = mutableListOf<PointerProperties>()
    val pointerCoords = mutableListOf<PointerCoords>()

    for ((index, initialPointer) in initialPointers.withIndex()) {
        pointerProperties.add(index, PointerProperties(initialPointer.id))
        pointerCoords.add(index, PointerCoords(initialPointer.x, initialPointer.y))
    }

    val moveMotionEvents =
        Array(numberOfMoveEvents) { index ->
            val move =
                if (enableFlingStyleHistory) {
                    val historicalEventCount = numberOfHistoricalEventsBasedOnArrayLocation(index)

                    // Set the time to the previous event time (either a down or move event) and
                    // offset it, so it doesn't conflict with that previous event.
                    var historicalTime: Int = time - timeDelta + 10

                    var accountForMoveOffset = -1

                    // Creates starting x values for all historical pointers (takes ending x of
                    // all pointers and subtracts a delta and offset).
                    val historicalPointerCoords = mutableListOf<PointerCoords>()

                    for ((historicalIndex, historicalPointer) in pointerCoords.withIndex()) {
                        if (moveDelta > 0) {
                            // accountForMoveOffset stays -1 (to account for +1 offset [below])
                            historicalPointerCoords.add(
                                historicalIndex,
                                PointerCoords(
                                    historicalPointer.x - moveDelta + 1,
                                    historicalPointer.y
                                )
                            )
                        } else {
                            // accountForMoveOffset changes to 1 (to account for -1 offset [below])
                            accountForMoveOffset = 1
                            historicalPointerCoords.add(
                                historicalIndex,
                                PointerCoords(
                                    historicalPointer.x - moveDelta - 1,
                                    historicalPointer.y
                                )
                            )
                        }
                    }

                    val historicalTimeDelta: Int = (timeDelta - 10) / historicalEventCount
                    val historicalXDelta: Float =
                        (moveDelta + accountForMoveOffset) / historicalEventCount

                    // Next section of code creates "historical" events by
                    // 1. Creating [MotionEvent] with oldest historical event.
                    // 2. Adding each subsequent historical event one at a time via `addBatch()`.
                    // 3. Finishes by adding the main/end pointers via `addBatch()`.

                    // Executes step 1 -> Creates [MotionEvent] with oldest historical event.
                    val moveWithHistory =
                        MotionEvent(
                            historicalTime,
                            MotionEvent.ACTION_MOVE,
                            pointerProperties.size,
                            0,
                            pointerProperties.toTypedArray(), // ids always the same
                            historicalPointerCoords.toTypedArray(),
                            rootView
                        )

                    // Executes step 2 -> Adds each subsequent historical event one at a time via
                    // `addBatch()`.
                    // Starts with the second historical event (1), since we've already added the
                    // first when we created the [MotionEvent] above.
                    for (historyIndex in 1 until historicalEventCount) {
                        // Update historical time
                        historicalTime += historicalTimeDelta

                        // Update historical x
                        for (historicalPointerCoord in historicalPointerCoords) {
                            historicalPointerCoord.x += historicalXDelta
                        }

                        // Add to [MotionEvent] history
                        moveWithHistory.addBatch(
                            historicalTime.toLong(),
                            historicalPointerCoords.toTypedArray(),
                            0
                        )
                    }

                    // Executes step 3 -> Finishes by adding the main/end pointers via `addBatch()`,
                    // so it will show up as the current event for the [MotionEvent].
                    moveWithHistory.addBatch(time.toLong(), pointerCoords.toTypedArray(), 0)
                    // return move event with history added
                    moveWithHistory
                } else {
                    MotionEvent(
                        time,
                        MotionEvent.ACTION_MOVE,
                        pointerProperties.size,
                        0,
                        pointerProperties.toTypedArray(),
                        pointerCoords.toTypedArray(),
                        rootView
                    )
                }

            time += timeDelta
            // Update all pointer's x by move delta for next iteration
            for (pointerCoord in pointerCoords) {
                pointerCoord.x += moveDelta
            }
            move
        }
    return moveMotionEvents
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
