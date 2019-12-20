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

package androidx.ui.test

import android.view.MotionEvent
import androidx.test.filters.SmallTest
import androidx.ui.core.Duration
import androidx.ui.core.inMilliseconds
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.MotionEventRecorder
import androidx.ui.test.util.assertHasValidEventTimes
import androidx.ui.test.util.isMonotonousBetween
import androidx.ui.test.util.moveEvents
import androidx.ui.test.util.splitsDurationEquallyInto
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.max

private const val x0 = 5f
private const val x1 = 23f
private const val y0 = 7f
private const val y1 = 29f

/**
 * Tests if the [AndroidInputDispatcher.sendSwipe] gesture works when specifying the gesture as a
 * line between two positions
 */
@SmallTest
@RunWith(Parameterized::class)
class AndroidInputDispatcherSendSwipeLineTest(private val config: TestConfig) {
    data class TestConfig(
        val duration: Duration,
        val eventPeriod: Long
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(10L, 9L, 11L).flatMap { period ->
                (1L..100L).map { durationMs ->
                    TestConfig(Duration(milliseconds = durationMs), period)
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
    private val eventPeriod = config.eventPeriod

    private lateinit var recorder: MotionEventRecorder
    private lateinit var subject: AndroidInputDispatcher

    @Before
    fun setUp() {
        recorder = MotionEventRecorder()
        subject = AndroidInputDispatcher(recorder.asCollectedProviders())
    }

    @After
    fun tearDown() {
        recorder.clear()
    }

    @Test
    fun swipeByLine() {
        subject.sendSwipe(x0, y0, x1, y1, duration)
        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val expectedMoveEvents = max(1, duration.inMilliseconds() / eventPeriod).toInt()
            assertThat(size).isAtLeast(2 + expectedMoveEvents) // down move+ up
            assertThat(first().action).isEqualTo(MotionEvent.ACTION_DOWN)
            assertThat(last().action).isEqualTo(MotionEvent.ACTION_UP)

            // Check coordinates
            assertThat(first().x).isEqualTo(x0)
            assertThat(first().y).isEqualTo(y0)
            moveEvents.isMonotonousBetween(x0, y0, x1, y1)
            assertThat(last().x).isEqualTo(x1)
            assertThat(last().y).isEqualTo(y1)

            // Check timestamps
            val durationMs = duration.inMilliseconds()
            assertThat(last().eventTime - first().eventTime).isEqualTo(durationMs)
            moveEvents.splitsDurationEquallyInto(0L, durationMs, eventPeriod)
        }
    }
}
