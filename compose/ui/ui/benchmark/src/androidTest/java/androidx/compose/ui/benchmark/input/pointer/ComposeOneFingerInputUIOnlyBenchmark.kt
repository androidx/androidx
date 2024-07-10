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

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose benchmarks for a single finger input (down/up and down/move/up) on an item using ONLY UI
 * module APIs (no Foundation calls which is what differentiates it from
 * [ComposeTapIntegrationBenchmark]). The benchmark uses pointerInput (ui) + awaitPointerEventScope
 * (ui) + awaitPointerEvent (ui) to track simple down/move/up inputs.
 *
 * The intent is to measure the speed of all parts necessary for a normal down, (from some
 * benchmarks, move,) and up starting from [MotionEvent]s getting dispatched to a particular view.
 * The test therefore includes hit testing and dispatch.
 *
 * The hierarchy is set up to look like: rootView -> Column -> Text (with click listener) -> Text
 * (with click listener) -> Text (with click listener) -> ...
 *
 * MotionEvents are dispatched to rootView as ACTION_DOWN and ACTION_UP (and for some benchmarks
 * ACTION_MOVE(s) are added). The validity of the test is verified inside awaitPointerEventScope { }
 * with com.google.common.truth.Truth.assertThat and by counting the events and later verifying that
 * they count is sufficiently high.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposeOneFingerInputUIOnlyBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun clickOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(0, "0", 0)
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @Test
    fun clickOnEarlyItemFyi() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(lastItem, "$lastItem", 0)
    }

    @Test
    fun clickWithMoveOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(0, "0", 6)
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @Test
    fun clickWithMoveOnEarlyItemFyi() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(lastItem, "$lastItem", 6)
    }

    @Test
    fun clickWithMoveAndFlingHistoryOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(0, "0", 6, true)
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @Test
    fun clickWithMoveAndFlingHistoryOnEarlyItemFyi() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(lastItem, "$lastItem", 6, true)
    }

    private fun clickOnItem(
        item: Int,
        expectedLabel: String,
        numberOfMoves: Int,
        enableHistory: Boolean = false
    ) {
        // half height of an item + top of the chosen item = middle of the chosen item
        val y = (ItemHeightPx / 2) + (item * ItemHeightPx)
        val xDown = 0f
        val xMoveInitial = xDown + MOVE_AMOUNT_PX

        benchmarkRule.runBenchmarkFor({ ComposeTapTestCase() }) {
            lateinit var case: ComposeTapTestCase
            lateinit var rootView: View

            benchmarkRule.runOnUiThread {
                doFramesUntilNoChangesPending()

                case = getTestCase()
                case.expectedLabel = expectedLabel

                rootView = getHostView()
            }

            // Simple Events
            val down =
                MotionEvent(
                    0,
                    MotionEvent.ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(xDown, y)),
                    rootView
                )

            val (time, x, moves) =
                createMoves(
                    initialX = xMoveInitial,
                    initialTime = 100,
                    y = y,
                    rootView = rootView,
                    numberOfMoveEvents = numberOfMoves,
                    enableFlingStyleHistory = enableHistory
                )

            val up =
                MotionEvent(
                    time,
                    MotionEvent.ACTION_UP,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(x, y)),
                    rootView
                )

            benchmarkRule.measureRepeatedOnUiThread {
                rootView.dispatchTouchEvent(down)
                case.expectedPressCount++
                assertThat(case.actualPressCount).isEqualTo(case.expectedPressCount)

                for (move in moves) {
                    rootView.dispatchTouchEvent(move)
                    case.expectedMoveCount++
                    assertThat(case.actualMoveCount).isEqualTo(case.expectedMoveCount)
                }

                // Double checks move count again (in case there weren't any moves).
                assertThat(case.actualMoveCount).isEqualTo(case.expectedMoveCount)

                rootView.dispatchTouchEvent(up)
                case.expectedReleaseCount++
                assertThat(case.actualReleaseCount).isEqualTo(case.expectedReleaseCount)

                assertThat(case.actualOtherEventCount).isEqualTo(case.expectedOtherEventCount)
            }
        }
    }

    private fun createMoves(
        initialX: Float,
        initialTime: Int,
        y: Float, // Same Y used for all moves
        rootView: View,
        numberOfMoveEvents: Int,
        enableFlingStyleHistory: Boolean = false,
        timeDelta: Int = 100,
        moveDelta: Float = MOVE_AMOUNT_PX
    ): Triple<Int, Float, Array<MotionEvent>> {
        var time = initialTime
        var x = initialX

        val moveMotionEvents =
            Array(numberOfMoveEvents) { index ->
                val move =
                    if (enableFlingStyleHistory) {
                        val historicalEventCount =
                            numberOfHistoricalEventsBasedOnArrayLocation(index)

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
    private fun numberOfHistoricalEventsBasedOnArrayLocation(index: Int): Int {
        return when (index) {
            0 -> 12
            1 -> 9
            2,
            3 -> 4
            else -> 2
        }
    }

    private class ComposeTapTestCase : ComposeTestCase {
        private var itemHeightDp: Dp? = null // Is set to correct value during composition.
        var actualPressCount = 0
        var expectedPressCount = 0

        var actualMoveCount = 0
        var expectedMoveCount = 0

        var actualReleaseCount = 0
        var expectedReleaseCount = 0

        var actualOtherEventCount = 0
        var expectedOtherEventCount = 0

        lateinit var expectedLabel: String

        @Composable
        override fun Content() {
            with(LocalDensity.current) { itemHeightDp = ItemHeightPx.toDp() }

            EmailList(NumItems)
        }

        @Composable
        fun EmailList(count: Int) {
            Column { repeat(count) { i -> Email("$i") } }
        }

        @Composable
        fun Email(label: String) {
            BasicText(
                text = label,
                modifier =
                    Modifier.pointerInput(label) {
                            awaitPointerEventScope {
                                while (true) {
                                    assertThat(label).isEqualTo(expectedLabel)
                                    val event = awaitPointerEvent()

                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            actualPressCount++
                                        }
                                        PointerEventType.Move -> {
                                            actualMoveCount++
                                        }
                                        PointerEventType.Release -> {
                                            actualReleaseCount++
                                        }
                                        else -> {
                                            actualOtherEventCount++
                                        }
                                    }
                                }
                            }
                        }
                        .fillMaxWidth()
                        .requiredHeight(itemHeightDp!!)
            )
        }
    }

    companion object {
        private const val MOVE_AMOUNT_PX = 10f
    }
}
