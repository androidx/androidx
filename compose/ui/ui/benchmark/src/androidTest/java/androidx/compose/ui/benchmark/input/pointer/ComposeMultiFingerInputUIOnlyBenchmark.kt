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
 * Compose benchmarks for multiple finger input (down/up and down/move/up) on an item using ONLY UI
 * module APIs (no Foundation calls which is what differentiates it from
 * [ComposeTapIntegrationBenchmark]). The benchmark uses pointerInput (ui) + awaitPointerEventScope
 * (ui) + awaitPointerEvent (ui) to track simple down/move/up inputs.
 *
 * The intent is to measure the speed of all parts necessary for a normal down, (move for some
 * benchmarks) and up starting from [MotionEvent]s getting dispatched to a particular view. The test
 * therefore includes hit testing and dispatch.
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
class ComposeMultiFingerInputUIOnlyBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun clickOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(item = 0, expectedLabel = "0", numberOfMoves = 0, numberOfFingers = 3)
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @Test
    fun clickOnEarlyItemFyi() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(
            item = lastItem,
            expectedLabel = "$lastItem",
            numberOfMoves = 0,
            numberOfFingers = 3
        )
    }

    @Test
    fun clickWithMoveOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(item = 0, expectedLabel = "0", numberOfMoves = 6, numberOfFingers = 3)
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @Test
    fun clickWithMoveOnEarlyItemFyi() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(
            item = lastItem,
            expectedLabel = "$lastItem",
            numberOfMoves = 6,
            numberOfFingers = 3
        )
    }

    @Test
    fun clickWithMoveAndFlingHistoryOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(
            item = 0,
            expectedLabel = "0",
            numberOfMoves = 6,
            numberOfFingers = 3,
            enableHistory = true
        )
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @Test
    fun clickWithMoveAndFlingHistoryOnEarlyItemFyi() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(
            item = lastItem,
            expectedLabel = "$lastItem",
            numberOfMoves = 6,
            numberOfFingers = 3,
            enableHistory = true
        )
    }

    private fun clickOnItem(
        item: Int,
        expectedLabel: String,
        numberOfMoves: Int,
        numberOfFingers: Int,
        enableHistory: Boolean = false
    ) {
        val initialTimeForFirstEvent = 0
        val initialXForFirstEvent = 0f
        // half height of an item + top of the chosen item = middle of the chosen item
        val y = (ItemHeightPx / 2) + (item * ItemHeightPx)

        benchmarkRule.runBenchmarkFor({ ComposeTapTestCase() }) {
            lateinit var case: ComposeTapTestCase
            lateinit var rootView: View

            benchmarkRule.runOnUiThread {
                doFramesUntilNoChangesPending()

                case = getTestCase()
                case.expectedLabel = expectedLabel

                rootView = getHostView()
            }

            // Create all MotionEvents
            // 1. Create downs
            val downs =
                createDowns(
                    initialX = initialXForFirstEvent,
                    initialTime = initialTimeForFirstEvent,
                    y = y,
                    rootView = rootView,
                    numberOfEvents = numberOfFingers,
                )

            assertThat(downs.size).isEqualTo(numberOfFingers)

            // 2. Create moves
            // Get start time for moves
            val lastDown = downs.last()
            val initialMoveTime = lastDown.eventTime.toInt() + DefaultPointerInputTimeDelta

            // Get start x, y, and pointer id for moves
            val initialMoveDataSimplified =
                Array(lastDown.pointerCount) { index ->
                    BenchmarkSimplifiedPointerInputPointer(
                        id = lastDown.getPointerId(index),
                        x = lastDown.getX(index) + DefaultPointerInputMoveAmountPx,
                        y = lastDown.getY(index)
                    )
                }

            val moves =
                createMoveMotionEvents(
                    initialTime = initialMoveTime,
                    initialPointers = initialMoveDataSimplified,
                    rootView = rootView,
                    numberOfMoveEvents = numberOfMoves,
                    enableFlingStyleHistory = enableHistory
                )

            // 2. Create ups
            // Get start time for ups
            val lastMove =
                if (moves.isNotEmpty()) {
                    moves.last()
                } else {
                    lastDown
                }
            val initialUpTime = lastMove.eventTime.toInt() + DefaultPointerInputTimeDelta

            // Get start x, y, and pointer id for ups
            val initialUpDataSimplified =
                Array(lastMove.pointerCount) { index ->
                    BenchmarkSimplifiedPointerInputPointer(
                        id = lastMove.getPointerId(index),
                        x = lastMove.getX(index),
                        y = lastMove.getY(index)
                    )
                }

            val ups =
                createUps(
                    initialTime = initialUpTime,
                    initialPointers = initialUpDataSimplified,
                    rootView = rootView,
                )

            assertThat(ups.size).isEqualTo(numberOfFingers)

            benchmarkRule.measureRepeatedOnUiThread {
                // Trigger and verify all up events.
                for (down in downs) {
                    rootView.dispatchTouchEvent(down)
                    case.expectedPressCount++
                    assertThat(case.actualPressCount).isEqualTo(case.expectedPressCount)
                }

                // Trigger and verify all move events.
                for (move in moves) {
                    rootView.dispatchTouchEvent(move)
                    case.expectedMoveCount++
                    assertThat(case.actualMoveCount).isEqualTo(case.expectedMoveCount)
                }

                // Double checks move count again (in case there weren't any moves).
                assertThat(case.actualMoveCount).isEqualTo(case.expectedMoveCount)

                // Trigger and verify all down events.
                for (up in ups) {
                    rootView.dispatchTouchEvent(up)
                    case.expectedReleaseCount++
                    assertThat(case.actualReleaseCount).isEqualTo(case.expectedReleaseCount)
                }

                assertThat(case.actualOtherEventCount).isEqualTo(case.expectedOtherEventCount)
            }
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
}
