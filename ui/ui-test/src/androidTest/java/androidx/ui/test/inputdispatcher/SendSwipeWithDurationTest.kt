/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test.inputdispatcher

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import androidx.test.filters.SmallTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.MotionEventRecorder
import androidx.ui.test.util.assertHasValidEventTimes
import androidx.ui.test.util.verify
import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests if the [AndroidInputDispatcher.sendSwipe] gesture works when specifying the gesture as a
 * function between two positions. Verifies if the generated MotionEvents for a gesture with a
 * given duration have the expected timestamps. The timestamps should divide the duration as
 * equally as possible with as close to [AndroidInputDispatcher.eventPeriod] between each
 * successive event as possible.
 */
@SmallTest
@RunWith(Parameterized::class)
class SendSwipeWithDurationTest(private val config: TestConfig) {
    data class TestConfig(
        val duration: Duration,
        val expectedTimestamps: List<Long>
    )

    companion object {
        private val curve = { t: Long ->
            PxPosition(t.toFloat(), (-t).toFloat())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // With eventPeriod of 10.ms, 0 events is 0.ms and 1 event is 10.ms
                // Even though 1.ms is closer to 0.ms than to 10.ms, split into 1 event as we
                // must have at least 1 move event to have movement.
                TestConfig(1.milliseconds, listOf(1)),
                // With eventPeriod of 10.ms, 0 events is 0.ms and 1 event is 10.ms
                // Split 7.ms in 1 event as 7.ms is closer to 10.ms than to 0.ms
                TestConfig(7.milliseconds, listOf(7)),
                // With eventPeriod of 10.ms, a duration of 10.ms is exactly 1 event
                TestConfig(10.milliseconds, listOf(10)),
                // With eventPeriod of 10.ms, 1 event is 10.ms and 2 events is 20.ms
                // Split 14.ms in 1 event as 14.ms is closer to 10.ms than to 20.ms
                TestConfig(14.milliseconds, listOf(14)),
                // With eventPeriod of 10.ms, 1 event is 10.ms and 2 events is 20.ms
                // 15.ms is as close to 10.ms as it is to 20.ms, in which case the larger number
                // of events is preferred -> 2 events
                TestConfig(15.milliseconds, listOf(8, 15)),
                // With eventPeriod of 10.ms, 1 event is 10.ms and 2 events is 20.ms
                // Split 19.ms in 2 events as 19.ms is closer to 20.ms than to 10.ms
                TestConfig(19.milliseconds, listOf(10, 19)),
                // With eventPeriod of 10.ms, 2 events is 20.ms and 3 events is 30.ms
                // Split 24.ms in 2 events as 24.ms is closer to 20.ms than to 30.ms
                TestConfig(24.milliseconds, listOf(12, 24)),
                // With eventPeriod of 10.ms, 2 events is 20.ms and 3 events is 30.ms
                // 25.ms is as close to 20.ms as it is to 30.ms, in which case the larger number
                // of events is preferred -> 3 events
                TestConfig(25.milliseconds, listOf(8, 17, 25)),
                // With eventPeriod of 10.ms, 9 event is 90.ms and 10 events is 100.ms
                // Split 97.ms in 10 events as 97.ms is closer to 100.ms than to 90.ms
                TestConfig(97.milliseconds, listOf(10, 19, 29, 39, 49, 58, 68, 78, 87, 97))
            )
        }
    }

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true
    )

    private val recorder = MotionEventRecorder()
    private val subject = AndroidInputDispatcher(recorder::recordEvent)

    @After
    fun tearDown() {
        recorder.disposeEvents()
    }

    @Test
    fun swipeWithDuration() {
        // Given a swipe with a given duration
        subject.sendSwipe(curve = curve, duration = config.duration)

        // then
        val expectedNumberOfMoveEvents = config.expectedTimestamps.size
        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            // down + up + #move
            assertThat(size).isEqualTo(2 + expectedNumberOfMoveEvents)

            val durationMs = config.duration.inMilliseconds()
            // First is down, last is up
            first().verify(curve, ACTION_DOWN, expectedRelativeTime = 0)
            last().verify(curve, ACTION_UP, expectedRelativeTime = durationMs)
            // In between are all move events with the expected timestamps
            drop(1).zip(config.expectedTimestamps).forEach { (event, expectedTimestamp) ->
                event.verify(curve, ACTION_MOVE, expectedRelativeTime = expectedTimestamp)
            }
        }
    }
}
