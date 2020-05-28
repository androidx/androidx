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
import androidx.ui.test.InputDispatcher
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.MotionEventRecorder
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
 * Tests if [AndroidInputDispatcher.delay] works by performing three gestures with a delay in
 * between them. By varying the gestures and the delay, we test for lingering state problems.
 */
@SmallTest
@RunWith(Parameterized::class)
class DelayTest(private val config: TestConfig) {
    data class TestConfig(
        val firstDelay: Duration,
        val secondDelay: Duration,
        val firstGesture: Gesture,
        val secondGesture: Gesture,
        val thirdGesture: Gesture
    )

    enum class Gesture(internal val function: (InputDispatcher) -> Unit) {
        Click({ it.sendClick(anyPosition) }),
        Swipe({ it.sendSwipe(anyPosition, anyPosition, 107.milliseconds) }),
        Partial({
            it.sendDown(anyPosition)
            it.sendMove(anyPosition)
            it.sendUp(anyPosition)
        })
    }

    companion object {
        private val anyPosition = PxPosition.Origin

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (delay1 in listOf(0, 23)) {
                    for (delay2 in listOf(0, 47)) {
                        for (gesture1 in Gesture.values()) {
                            for (gesture2 in Gesture.values()) {
                                for (gesture3 in Gesture.values()) {
                                    add(
                                        TestConfig(
                                            firstDelay = delay1.milliseconds,
                                            secondDelay = delay2.milliseconds,
                                            firstGesture = gesture1,
                                            secondGesture = gesture2,
                                            thirdGesture = gesture3
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
    fun testDelay() {
        // Perform two gestures with a delay in between
        config.firstGesture.function(subject)
        subject.delay(config.firstDelay)
        config.secondGesture.function(subject)
        subject.delay(config.secondDelay)
        config.thirdGesture.function(subject)

        // Check if the time between the gestures was exactly the delay
        val expectedFirstDelay = config.firstDelay.inMilliseconds()
        val expectedSecondDelay = config.secondDelay.inMilliseconds()
        recorder.events.filter { it.action in listOf(ACTION_DOWN, ACTION_UP) }.apply {
            assertThat(this).hasSize(6) // 3x (DOWN + UP)
            assertThat(this[0].action).isEqualTo(ACTION_DOWN)
            assertThat(this[1].action).isEqualTo(ACTION_UP)
            assertThat(this[2].action).isEqualTo(ACTION_DOWN)
            assertThat(this[3].action).isEqualTo(ACTION_UP)
            assertThat(this[4].action).isEqualTo(ACTION_DOWN)
            assertThat(this[5].action).isEqualTo(ACTION_UP)
            // Check time between first up and second down, that should be the first delay:
            assertThat(this[2].eventTime - this[1].eventTime).isEqualTo(expectedFirstDelay)
            // Check time between first up and second down, that should be the second delay:
            assertThat(this[4].eventTime - this[3].eventTime).isEqualTo(expectedSecondDelay)
        }
    }
}
