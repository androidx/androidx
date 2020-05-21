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

import android.view.MotionEvent
import androidx.test.filters.SmallTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.MotionEventRecorder
import androidx.ui.test.util.assertHasValidEventTimes
import androidx.ui.test.util.expectError
import androidx.ui.test.util.verify
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests if the [AndroidInputDispatcher.sendMove] gesture works.
 */
@SmallTest
@RunWith(Parameterized::class)
class SendMoveTest(config: TestConfig) {
    data class TestConfig(
        val x: Float,
        val y: Float
    )

    companion object {
        private val downPosition = PxPosition(5f, 5f)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(0f, 10f).flatMap { x ->
                listOf(0f, -10f).map { y ->
                    TestConfig(x, y)
                }
            }.plus(TestConfig(downPosition.x, downPosition.y))
        }
    }

    private val dispatcherRule = AndroidInputDispatcher.TestRule(disableDispatchInRealTime = true)
    private val eventPeriod get() = dispatcherRule.eventPeriod

    @get:Rule
    val inputDispatcherRule: TestRule = dispatcherRule

    private val position = PxPosition(config.x, config.y)

    private val recorder = MotionEventRecorder()
    private val subject = AndroidInputDispatcher(recorder::recordEvent)

    @After
    fun tearDown() {
        recorder.disposeEvents()
    }

    @Test
    fun testSendMove() {
        subject.sendDown(downPosition)
        subject.sendMove(position)
        assertThat(subject.currentPosition).isEqualTo(position)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            assertThat(size).isEqualTo(2)
            first().verify(downPosition, MotionEvent.ACTION_DOWN, 0)
            last().verify(position, MotionEvent.ACTION_MOVE, eventPeriod)
        }
    }
}

/**
 * Tests if the [AndroidInputDispatcher.sendMove] gesture throws after
 * [AndroidInputDispatcher.sendUp] or [AndroidInputDispatcher.sendCancel] has been called.
 */
@SmallTest
class SendMoveAfterFinishedTest {
    private val downPosition = PxPosition(5f, 5f)
    private val position = PxPosition(1f, 1f)

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true
    )

    private val subject = AndroidInputDispatcher {}

    @Test
    fun testMoveWithoutDown() {
        expectError<IllegalStateException> {
            subject.sendMove(position)
        }
    }

    @Test
    fun testMoveAfterUp() {
        subject.sendDown(downPosition)
        subject.sendUp(downPosition)
        expectError<IllegalStateException> {
            subject.sendMove(position)
        }
    }

    @Test
    fun testMoveAfterCancel() {
        subject.sendDown(downPosition)
        subject.sendCancel(downPosition)
        expectError<IllegalStateException> {
            subject.sendMove(position)
        }
    }
}
