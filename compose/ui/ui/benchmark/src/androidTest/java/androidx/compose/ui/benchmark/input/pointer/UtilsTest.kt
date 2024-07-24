/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class UtilsTest {
    // Tests for Down Motion Event Creation <---------------
    @Test
    fun createDownMotionEvents_noEvent() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 0

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val downs =
            createDowns(
                initialX = xMoveInitial,
                initialTime = initialTime,
                y = y,
                rootView = view,
                numberOfEvents = numberOfEvents,
            )

        // Should just return an empty array
        assertThat(downs.size).isEqualTo(numberOfEvents)
    }

    @Test
    fun createDownMotionEvents_oneEvent() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 1

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val downs =
            createDowns(
                initialX = xMoveInitial,
                initialTime = initialTime,
                y = y,
                rootView = view,
                numberOfEvents = numberOfEvents,
            )

        assertThat(downs.size).isEqualTo(numberOfEvents)

        for ((index, down) in downs.withIndex()) {
            if (index == 0) {
                assertThat(down.actionMasked).isEqualTo(MotionEvent.ACTION_DOWN)
            } else {
                assertThat(down.actionMasked).isEqualTo(MotionEvent.ACTION_POINTER_DOWN)
            }

            val expectedTime = initialTime + (index * DefaultPointerInputTimeDelta)
            assertThat(down.eventTime).isEqualTo(expectedTime)

            val expectedX = xMoveInitial + (index * DefaultPointerInputMoveAmountPx)
            assertThat(down.x).isEqualTo(expectedX)

            assertThat(down.y).isEqualTo(y)
            assertThat(down.historySize).isEqualTo(0)
        }
    }

    @Test
    fun createDownMotionEvents_sixEventsWithSixPointers() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val downs =
            createDowns(
                initialX = xMoveInitial,
                initialTime = initialTime,
                y = y,
                rootView = view,
                numberOfEvents = numberOfEvents,
            )

        assertThat(downs.size).isEqualTo(numberOfEvents)

        for ((index, down) in downs.withIndex()) {
            if (index == 0) {
                assertThat(down.actionMasked).isEqualTo(MotionEvent.ACTION_DOWN)
            } else {
                assertThat(down.actionMasked).isEqualTo(MotionEvent.ACTION_POINTER_DOWN)
            }

            val expectedTime = initialTime + (index * DefaultPointerInputTimeDelta)
            assertThat(down.eventTime).isEqualTo(expectedTime)

            val expectedX = xMoveInitial + (index * DefaultPointerInputMoveAmountPx)

            val pointerId: Int = down.getPointerId(index)
            val localPointerCoords = MotionEvent.PointerCoords()
            down.getPointerCoords(pointerId, localPointerCoords)

            assertThat(localPointerCoords.x).isEqualTo(expectedX)
            assertThat(localPointerCoords.y).isEqualTo(y)

            assertThat(down.historySize).isEqualTo(0)
        }
    }

    // Tests for Up Motion Event Creation <---------------
    @Test
    fun createUpMotionEvents_noEvent() {
        val initialTime = 100

        val simplifiedUps = arrayOf<BenchmarkSimplifiedPointerInputPointer>()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val ups =
            createUps(
                initialTime = initialTime,
                initialPointers = simplifiedUps,
                rootView = view,
            )

        // Should just return an empty array
        assertThat(ups.size).isEqualTo(simplifiedUps.size)
    }

    @Test
    fun createUpMotionEvents_oneEvent() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val simplifiedUps =
            arrayOf(BenchmarkSimplifiedPointerInputPointer(id = 0, x = xMoveInitial, y = y))

        val ups =
            createUps(
                initialTime = initialTime,
                initialPointers = simplifiedUps,
                rootView = view,
            )

        assertThat(ups.size).isEqualTo(simplifiedUps.size)

        for ((index, move) in ups.withIndex()) {
            if (index == (ups.size - 1)) { // last event should be ACTION_UP
                assertThat(move.actionMasked).isEqualTo(MotionEvent.ACTION_UP)
            } else {
                assertThat(move.actionMasked).isEqualTo(MotionEvent.ACTION_POINTER_UP)
            }

            val expectedTime = initialTime + (index * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            val expectedX = simplifiedUps[index].x
            assertThat(move.x).isEqualTo(expectedX)

            val expectedY = simplifiedUps[index].y
            assertThat(move.y).isEqualTo(expectedY)
            assertThat(move.historySize).isEqualTo(0)
        }
    }

    @Test
    fun createUpMotionEvents_sixEventsWithSixPointers() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6

        val simplifiedUps =
            Array(numberOfEvents) { index ->
                BenchmarkSimplifiedPointerInputPointer(
                    id = index,
                    x = xMoveInitial + (index * DefaultPointerInputMoveAmountPx),
                    y = y
                )
            }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val ups =
            createUps(
                initialTime = initialTime,
                initialPointers = simplifiedUps,
                rootView = view,
            )

        assertThat(ups.size).isEqualTo(simplifiedUps.size)

        // The main x and y will always be the first pointer, but we want to verify that for all
        // events.
        val expectedMainX = simplifiedUps[0].x
        val expectedMainY = simplifiedUps[0].y

        for ((index, move) in ups.withIndex()) {
            if (index == (ups.size - 1)) { // last event should be ACTION_UP
                assertThat(move.actionMasked).isEqualTo(MotionEvent.ACTION_UP)
            } else {
                assertThat(move.actionMasked).isEqualTo(MotionEvent.ACTION_POINTER_UP)
            }

            val expectedTime = initialTime + (index * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            assertThat(move.x).isEqualTo(expectedMainX)
            assertThat(move.y).isEqualTo(expectedMainY)

            // Check all pointers are there and valid
            val expectedPointerCount = simplifiedUps.size - index
            assertThat(move.pointerCount).isEqualTo(expectedPointerCount)

            for (pointerIndex in 0 until move.pointerCount) {
                val pointerId: Int = move.getPointerId(pointerIndex)
                val localPointerCoords = MotionEvent.PointerCoords()
                move.getPointerCoords(pointerId, localPointerCoords)

                val expectedPointerCoords = simplifiedUps[pointerIndex]
                assertThat(localPointerCoords.x).isEqualTo(expectedPointerCoords.x)
                assertThat(localPointerCoords.y).isEqualTo(expectedPointerCoords.y)
            }

            assertThat(move.historySize).isEqualTo(0)
        }
    }

    // Tests for Move Motion Event Creation <---------------
    // Note: For tests with history, I am only checking the history count, not each history's x/y.
    // One pointer/finger
    @Test
    fun createMoveMotionEvents_sixEventsOnePointerNegativeMoveDeltaWithoutHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = false

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers =
                    arrayOf(
                        BenchmarkSimplifiedPointerInputPointer(id = 0, x = xMoveInitial, y = y)
                    ),
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory,
                timeDelta = 100,
                moveDelta = -DefaultPointerInputMoveAmountPx
            )

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            val expectedX = xMoveInitial - (abs(moveIndex * DefaultPointerInputMoveAmountPx))
            assertThat(move.x).isEqualTo(expectedX)

            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize).isEqualTo(0)
        }
    }

    @Test
    fun createMoveMotionEvents_sixEventsOnePointerPositiveMoveDeltaWithoutHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = false

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers =
                    arrayOf(
                        BenchmarkSimplifiedPointerInputPointer(id = 0, x = xMoveInitial, y = y)
                    ),
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory
            )

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            val expectedX = xMoveInitial + (moveIndex * DefaultPointerInputMoveAmountPx)
            assertThat(move.x).isEqualTo(expectedX)

            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize).isEqualTo(0)
        }
    }

    @Test
    fun createMoveMotionEvents_sixEventsOnePointerPositiveMoveDeltaWithHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = true

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers =
                    arrayOf(
                        BenchmarkSimplifiedPointerInputPointer(id = 0, x = xMoveInitial, y = y)
                    ),
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory
            )

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputTimeDelta)

            val expectedX = xMoveInitial + (moveIndex * DefaultPointerInputMoveAmountPx)

            assertThat(move.eventTime).isEqualTo(expectedTime)
            assertThat(move.x).isEqualTo(expectedX)
            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize)
                .isEqualTo(numberOfHistoricalEventsBasedOnArrayLocation(moveIndex))
        }
    }

    @Test
    fun createMoveMotionEvents_sixEventsOnePointerNegativeMoveDeltaWithHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = true

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers =
                    arrayOf(
                        BenchmarkSimplifiedPointerInputPointer(id = 0, x = xMoveInitial, y = y)
                    ),
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory,
                timeDelta = 100,
                moveDelta = -DefaultPointerInputMoveAmountPx
            )
        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputTimeDelta)

            val expectedX = xMoveInitial - (abs(moveIndex * DefaultPointerInputMoveAmountPx))

            assertThat(move.eventTime).isEqualTo(expectedTime)
            assertThat(move.x).isEqualTo(expectedX)
            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize)
                .isEqualTo(numberOfHistoricalEventsBasedOnArrayLocation(moveIndex))
        }
    }

    // Multiple pointers/fingers
    @Test
    fun createMoveMotionEvents_sixEventsThreePointerNegativeMoveDeltaWithoutHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val numberOfPointers = 3 // fingers
        val enableFlingStyleHistory = false

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val initialPointers =
            Array(numberOfPointers) { simpleIndex ->
                BenchmarkSimplifiedPointerInputPointer(
                    id = simpleIndex,
                    x = xMoveInitial + (simpleIndex * DefaultPointerInputMoveAmountPx),
                    y = y
                )
            }

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers = initialPointers,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory,
                timeDelta = 100,
                moveDelta = -DefaultPointerInputMoveAmountPx
            )

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((index, move) in moves.withIndex()) {
            val expectedTime = initialTime + (index * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)
            assertThat(move.historySize).isEqualTo(0)

            for (pointerIndex in 0 until move.pointerCount) {
                val pointerId: Int = move.getPointerId(pointerIndex)
                val localPointerCoords = MotionEvent.PointerCoords()
                move.getPointerCoords(pointerId, localPointerCoords)

                val expectedX =
                    (xMoveInitial - (abs(index * DefaultPointerInputMoveAmountPx))) +
                        (pointerIndex * DefaultPointerInputMoveAmountPx)
                assertThat(localPointerCoords.x).isEqualTo(expectedX)

                assertThat(localPointerCoords.y).isEqualTo(y)
            }
        }
    }

    @Test
    fun createMoveMotionEvents_sixEventsThreePointerPositiveMoveDeltaWithoutHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val numberOfPointers = 3 // fingers
        val enableFlingStyleHistory = false

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val initialPointers =
            Array(numberOfPointers) { simpleIndex ->
                BenchmarkSimplifiedPointerInputPointer(
                    id = simpleIndex,
                    x = xMoveInitial + (simpleIndex * DefaultPointerInputMoveAmountPx),
                    y = y
                )
            }

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers = initialPointers,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory
            )

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((index, move) in moves.withIndex()) {
            val expectedTime = initialTime + (index * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)
            assertThat(move.historySize).isEqualTo(0)

            for (pointerIndex in 0 until move.pointerCount) {
                val pointerId: Int = move.getPointerId(pointerIndex)
                val localPointerCoords = MotionEvent.PointerCoords()
                move.getPointerCoords(pointerId, localPointerCoords)

                val expectedX =
                    (xMoveInitial + (index * DefaultPointerInputMoveAmountPx)) +
                        (pointerIndex * DefaultPointerInputMoveAmountPx)
                assertThat(localPointerCoords.x).isEqualTo(expectedX)

                assertThat(localPointerCoords.y).isEqualTo(y)
            }
        }
    }

    @Test
    fun createMoveMotionEvents_sixEventsThreePointerNegativeMoveDeltaWithHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val numberOfPointers = 3 // fingers
        val enableFlingStyleHistory = true

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val initialPointers =
            Array(numberOfPointers) { simpleIndex ->
                BenchmarkSimplifiedPointerInputPointer(
                    id = simpleIndex,
                    x = xMoveInitial + (simpleIndex * DefaultPointerInputMoveAmountPx),
                    y = y
                )
            }

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers = initialPointers,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory,
                timeDelta = 100,
                moveDelta = -DefaultPointerInputMoveAmountPx
            )

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            assertThat(move.historySize)
                .isEqualTo(numberOfHistoricalEventsBasedOnArrayLocation(moveIndex))

            for (pointerIndex in 0 until move.pointerCount) {
                val pointerId: Int = move.getPointerId(pointerIndex)
                val localPointerCoords = MotionEvent.PointerCoords()
                move.getPointerCoords(pointerId, localPointerCoords)

                val expectedX =
                    (xMoveInitial - (abs(moveIndex * DefaultPointerInputMoveAmountPx))) +
                        (pointerIndex * DefaultPointerInputMoveAmountPx)
                assertThat(localPointerCoords.x).isEqualTo(expectedX)
                assertThat(localPointerCoords.y).isEqualTo(y)
            }
        }
    }

    @Test
    fun createMoveMotionEvents_sixEventsThreePointerPositiveMoveDeltaWithHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val numberOfPointers = 3 // fingers
        val enableFlingStyleHistory = true

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val initialPointers =
            Array(numberOfPointers) { simpleIndex ->
                BenchmarkSimplifiedPointerInputPointer(
                    id = simpleIndex,
                    x = xMoveInitial + (simpleIndex * DefaultPointerInputMoveAmountPx),
                    y = y
                )
            }

        val moves =
            createMoveMotionEvents(
                initialTime = initialTime,
                initialPointers = initialPointers,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory
            )

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            assertThat(move.historySize)
                .isEqualTo(numberOfHistoricalEventsBasedOnArrayLocation(moveIndex))

            for (pointerIndex in 0 until move.pointerCount) {
                val pointerId: Int = move.getPointerId(pointerIndex)
                val localPointerCoords = MotionEvent.PointerCoords()
                move.getPointerCoords(pointerId, localPointerCoords)

                val expectedX =
                    (xMoveInitial + (moveIndex * DefaultPointerInputMoveAmountPx)) +
                        (pointerIndex * DefaultPointerInputMoveAmountPx)
                assertThat(localPointerCoords.x).isEqualTo(expectedX)
                assertThat(localPointerCoords.y).isEqualTo(y)
            }
        }
    }

    @Test
    fun testNumberOfHistoricalEventsBasedOnArrayLocation() {
        var numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(0)
        assertThat(numberOfEvents).isEqualTo(12)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(1)
        assertThat(numberOfEvents).isEqualTo(9)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(2)
        assertThat(numberOfEvents).isEqualTo(4)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(3)
        assertThat(numberOfEvents).isEqualTo(4)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(4)
        assertThat(numberOfEvents).isEqualTo(2)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(5)
        assertThat(numberOfEvents).isEqualTo(2)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(20)
        assertThat(numberOfEvents).isEqualTo(2)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(2000)
        assertThat(numberOfEvents).isEqualTo(2)

        numberOfEvents = numberOfHistoricalEventsBasedOnArrayLocation(-1)
        assertThat(numberOfEvents).isEqualTo(2)
    }
}
