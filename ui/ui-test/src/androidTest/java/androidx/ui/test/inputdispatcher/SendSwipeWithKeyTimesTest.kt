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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests if the [AndroidInputDispatcher.sendSwipe] gesture works when specifying the gesture as a
 * function between two positions. Verifies if the generated MotionEvents for a gesture with a
 * given duration and a set of keyTimes have the expected timestamps. The timestamps should
 * include all keyTimes, and divide the duration between those keyTimes as equally as possible
 * with as close to [AndroidInputDispatcher.eventPeriod] between each successive event as possible.
 */
@SmallTest
@RunWith(Parameterized::class)
class SendSwipeWithKeyTimesTest(private val config: TestConfig) {
    data class TestConfig(
        val duration: Duration,
        val keyTimes: List<Long>,
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
                // 10.ms normally splits into 1 event, but here we add a keyTime that must yield
                // an event at that time
                TestConfig(10.milliseconds, listOf(1), listOf(1, 10)),
                TestConfig(10.milliseconds, listOf(2), listOf(2, 10)),
                TestConfig(10.milliseconds, listOf(3), listOf(3, 10)),
                TestConfig(10.milliseconds, listOf(4), listOf(4, 10)),
                TestConfig(10.milliseconds, listOf(5), listOf(5, 10)),
                TestConfig(10.milliseconds, listOf(6), listOf(6, 10)),
                TestConfig(10.milliseconds, listOf(7), listOf(7, 10)),
                TestConfig(10.milliseconds, listOf(8), listOf(8, 10)),
                TestConfig(10.milliseconds, listOf(9), listOf(9, 10)),
                // With 2 keyTimes we expect to see both those keyTimes in the generated events
                TestConfig(10.milliseconds, listOf(1, 9), listOf(1, 9, 10)),
                // Same for 3 keyTimes
                TestConfig(10.milliseconds, listOf(1, 5, 9), listOf(1, 5, 9, 10)),
                // If two keyTimes are longer than eventPeriod apart from each other, that period
                // must be split as usual (here: between 10 and 28)
                TestConfig(30.milliseconds, listOf(5, 10, 28), listOf(5, 10, 19, 28, 30))
            )
        }
    }

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true
    )

    private val recorder = MotionEventRecorder()
    private val subject = AndroidInputDispatcher(recorder::recordEvent)

    @Before
    fun setUp() {
        require(config.keyTimes.distinct() == config.keyTimes.distinct().sorted()) {
            "keyTimes needs to be sorted, not ${config.keyTimes}"
        }
    }

    @After
    fun tearDown() {
        recorder.disposeEvents()
    }

    @Test
    fun swipeWithKeyTimes() {
        // Given a swipe with a given duration and set of keyTimes
        subject.sendSwipe(curve = curve, duration = config.duration, keyTimes = config.keyTimes)

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
