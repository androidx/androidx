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
import android.view.MotionEvent.ACTION_MOVE
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
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val curve = { t: Long ->
    PxPosition(
        t.toFloat(),
        (-t).toFloat()
    )
}

/**
 * Tests if the [AndroidInputDispatcher.sendSwipe] gesture works when specifying the gesture as a
 * function between two positions. Verifies if the generated MotionEvents for a gesture with a
 * given duration have the expected timestamps. The timestamps should divide the duration as
 * equally as possible with as close to [AndroidInputDispatcher.eventPeriod] between each
 * successive event as possible.
 */
@SmallTest
@RunWith(Parameterized::class)
class SwipeWithDurationTest(private val config: TestConfig) {
    data class TestConfig(
        val duration: Duration,
        val expectedTimestamps: List<Long>
    )

    companion object {
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

/**
 * Tests if the [AndroidInputDispatcher.sendSwipe] gesture works when specifying the gesture as a
 * function between two positions. Verifies if the generated MotionEvents for a gesture with a
 * given duration and a set of keyTimes have the expected timestamps. The timestamps should
 * include all keyTimes, and divide the duration between those keyTimes as equally as possible
 * with as close to [AndroidInputDispatcher.eventPeriod] between each successive event as possible.
 */
@SmallTest
@RunWith(Parameterized::class)
class SwipeWithKeyTimesTest(private val config: TestConfig) {
    data class TestConfig(
        val duration: Duration,
        val keyTimes: List<Long>,
        val expectedTimestamps: List<Long>
    )

    companion object {
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
