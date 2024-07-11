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

    @Test
    fun testCreateMoveMotionEvents_sixEventsNegativeMoveDeltaWithoutHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = false

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val (time, x, moves) =
            createMoveMotionEvents(
                initialX = xMoveInitial,
                initialTime = initialTime,
                y = y,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory,
                timeDelta = 100,
                moveDelta = -DefaultPointerInputMoveAmountPx
            )

        val expectedOutputTime = initialTime + (numberOfEvents * DefaultPointerInputMoveTimeDelta)
        assertThat(time).isEqualTo(expectedOutputTime)

        val expectedOutputX = xMoveInitial - (abs(numberOfEvents * DefaultPointerInputMoveAmountPx))
        assertThat(x).isEqualTo(expectedOutputX)

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((index, move) in moves.withIndex()) {
            val expectedTime = initialTime + (index * DefaultPointerInputMoveTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            val expectedX = xMoveInitial - (abs(index * DefaultPointerInputMoveAmountPx))
            assertThat(move.x).isEqualTo(expectedX)

            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize).isEqualTo(0)
        }
    }

    @Test
    fun testCreateMoveMotionEvents_sixEventsPositiveMoveDeltaWithoutHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = false

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val (time, x, moves) =
            createMoveMotionEvents(
                initialX = xMoveInitial,
                initialTime = initialTime,
                y = y,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory
            )

        val expectedOutputTime = initialTime + (numberOfEvents * DefaultPointerInputMoveTimeDelta)
        assertThat(time).isEqualTo(expectedOutputTime)

        val expectedOutputX = xMoveInitial + (numberOfEvents * DefaultPointerInputMoveAmountPx)
        assertThat(x).isEqualTo(expectedOutputX)

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((index, move) in moves.withIndex()) {
            val expectedTime = initialTime + (index * DefaultPointerInputMoveTimeDelta)
            assertThat(move.eventTime).isEqualTo(expectedTime)

            val expectedX = xMoveInitial + (index * DefaultPointerInputMoveAmountPx)
            assertThat(move.x).isEqualTo(expectedX)

            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize).isEqualTo(0)
        }
    }

    @Test
    fun testCreateMoveMotionEvents_sixEventsPositiveMoveDeltaWithHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = true

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val (time, x, moves) =
            createMoveMotionEvents(
                initialX = xMoveInitial,
                initialTime = initialTime,
                y = y,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory
            )

        val expectedOutputTime = initialTime + (numberOfEvents * DefaultPointerInputMoveTimeDelta)
        assertThat(time).isEqualTo(expectedOutputTime)

        val expectedOutputX = xMoveInitial + (numberOfEvents * DefaultPointerInputMoveAmountPx)
        assertThat(x).isEqualTo(expectedOutputX)

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputMoveTimeDelta)

            val expectedX = xMoveInitial + (moveIndex * DefaultPointerInputMoveAmountPx)

            assertThat(move.eventTime).isEqualTo(expectedTime)
            assertThat(move.x).isEqualTo(expectedX)
            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize)
                .isEqualTo(numberOfHistoricalEventsBasedOnArrayLocation(moveIndex))
        }
    }

    @Test
    fun testCreateMoveMotionEvents_sixEventsNegativeMoveDeltaWithHistory() {
        val y = (ItemHeightPx / 2)
        val xMoveInitial = 0f
        val initialTime = 100
        val numberOfEvents = 6
        val enableFlingStyleHistory = true

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)

        val (time, x, moves) =
            createMoveMotionEvents(
                initialX = xMoveInitial,
                initialTime = initialTime,
                y = y,
                rootView = view,
                numberOfMoveEvents = numberOfEvents,
                enableFlingStyleHistory = enableFlingStyleHistory,
                timeDelta = 100,
                moveDelta = -DefaultPointerInputMoveAmountPx
            )

        val expectedOutputTime = initialTime + (numberOfEvents * DefaultPointerInputMoveTimeDelta)
        assertThat(time).isEqualTo(expectedOutputTime)

        val expectedOutputX = xMoveInitial - (abs(numberOfEvents * DefaultPointerInputMoveAmountPx))
        assertThat(x).isEqualTo(expectedOutputX)

        assertThat(moves.size).isEqualTo(numberOfEvents)

        for ((moveIndex, move) in moves.withIndex()) {
            val expectedTime = initialTime + (moveIndex * DefaultPointerInputMoveTimeDelta)

            val expectedX = xMoveInitial - (abs(moveIndex * DefaultPointerInputMoveAmountPx))

            assertThat(move.eventTime).isEqualTo(expectedTime)
            assertThat(move.x).isEqualTo(expectedX)
            assertThat(move.y).isEqualTo(y)
            assertThat(move.historySize)
                .isEqualTo(numberOfHistoricalEventsBasedOnArrayLocation(moveIndex))
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
