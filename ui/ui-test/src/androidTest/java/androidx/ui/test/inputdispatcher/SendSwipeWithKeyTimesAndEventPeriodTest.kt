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

package androidx.ui.test.inputdispatcher

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import androidx.test.filters.SmallTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.MotionEventRecorder
import androidx.ui.test.util.assertHasValidEventTimes
import androidx.ui.test.util.between
import androidx.ui.test.util.moveEvents
import androidx.ui.test.util.relativeEventTimes
import androidx.ui.test.util.relativeTime
import androidx.ui.test.util.splitsDurationEquallyInto
import androidx.ui.test.util.verify
import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
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
 * given duration and a set of keyTimes have the expected timestamps, if the event period would
 * be different. This is not a situation that can occur in practice, but is necessary to test to
 * ensure the calculations made by the [AndroidInputDispatcher] are correct.
 *
 * The timestamps of the generated events should include all keyTimes, and divide the duration
 * between those keyTimes as equally as possible with as close to [eventPeriod] between each
 * successive event as possible.
 *
 * This uses a different verification mechanism as the previous tests, because here we need to
 * calculate the expected timestamps.
 */
@SmallTest
@RunWith(Parameterized::class)
class SendSwipeWithKeyTimesAndEventPeriodTest(private val config: TestConfig) {
    data class TestConfig(
        val duration: Duration,
        val keyTimes: List<Long>,
        val eventPeriod: Long
    )

    companion object {
        private val curve = { t: Long ->
            PxPosition(t.toFloat(), (-t).toFloat())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(10L, 4L, 7L).flatMap { eventPeriod ->
                // pick a prime number for duration
                val durationMs = 101L
                // testing all possible keyTimes between 0 and 100 takes way too long,
                // only test several combinations keyTimes close to the middle instead
                val firstKeyTime = (durationMs / 2) - eventPeriod
                val lastKeyTime = (durationMs / 2) + eventPeriod
                (firstKeyTime..lastKeyTime step eventPeriod).flatMap { keyTime1 ->
                    (keyTime1..lastKeyTime).map { keyTime2 ->
                        TestConfig(
                            Duration(milliseconds = durationMs),
                            listOf(keyTime1, keyTime2),
                            eventPeriod
                        )
                    }
                }
            }
        }
    }

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true,
        eventPeriodOverride = config.eventPeriod
    )

    private val duration get() = config.duration
    private val keyTimes get() = config.keyTimes
    private val eventPeriod = config.eventPeriod

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
    fun swipeWithKeyTimesAndEventPeriod() {
        // Given a specific eventPeriod and a swipe with a given duration and set of keyTimes
        subject.sendSwipe(curve = curve, duration = duration, keyTimes = keyTimes)

        // then
        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            // down + up + #keyTimes
            assertThat(size).isAtLeast(2 + keyTimes.size)

            // Check down and up events
            val durationMs = duration.inMilliseconds()
            first().verify(curve, ACTION_DOWN, 0)
            last().verify(curve, ACTION_UP, durationMs)

            // Check that coordinates are the function's value at the respective timestamps
            forEach {
                assertThat(it.x).isEqualTo(curve(it.relativeTime).x)
                assertThat(it.y).isEqualTo(curve(it.relativeTime).y)
            }

            // The given keyTimes must occur as event timestamps
            // Ordering is already required on the keyTimes parameter in setUp()
            assertThat(relativeEventTimes).containsAtLeastElementsIn(keyTimes.distinct())

            // The keyTimes divide the duration in a set of intervals. Each interval should
            // be represented by MotionEvents that divide that interval as equally as
            // possible with as close to [eventPeriod] between each successive event as
            // possible.
            keyTimes.plus(durationMs).distinct().zipWithNext().forEach { (t0, t1) ->
                val segment = moveEvents.between(t0, t1)
                segment.splitsDurationEquallyInto(t0, t1, eventPeriod)
            }
        }
    }
}
