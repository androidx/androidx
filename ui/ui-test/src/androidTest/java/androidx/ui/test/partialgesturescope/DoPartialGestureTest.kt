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

package androidx.ui.test.partialgesturescope

import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.ui.test.PartialGestureScope
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.createComposeRule
import androidx.ui.test.doPartialGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendCancel
import androidx.ui.test.sendDown
import androidx.ui.test.sendMoveBy
import androidx.ui.test.sendMoveTo
import androidx.ui.test.sendUp
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.PointerInputRecorder
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.expectError
import androidx.ui.test.util.timeDiffWith
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val tag = "widget"
private val anyPosition = PxPosition(1f, 1f)

private val (PartialGestureScope.(PxPosition) -> Unit).string: String
    get() = when (this) {
        PartialGestureScope::sendDown -> "sendDown()"
        PartialGestureScope::sendMoveTo -> "sendMoveTo()"
        PartialGestureScope::sendMoveBy -> "sendMoveBy()"
        PartialGestureScope::sendUp -> "sendUp()"
        PartialGestureScope::sendCancel -> "sendCancel()"
        else -> "custom"
    }

/**
 * Tests sending of partial gestures, spread out of one or more invocations of [doPartialGesture]
 */
@MediumTest
@RunWith(Parameterized::class)
class DoPartialGestureTest(private val config: TestConfig) {
    data class TestConfig(val partialGesture: PartialGestureScope.(PxPosition) -> Unit) {
        val isDown = partialGesture == PartialGestureScope::sendDown
        val isUp = partialGesture == PartialGestureScope::sendUp
        val isCancel = partialGesture == PartialGestureScope::sendCancel

        override fun toString(): String {
            return "TestConfig(partialGesture=${partialGesture.string})"
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                TestConfig(PartialGestureScope::sendDown),
                TestConfig(PartialGestureScope::sendMoveTo),
                TestConfig(PartialGestureScope::sendMoveBy),
                TestConfig(PartialGestureScope::sendUp),
                TestConfig(PartialGestureScope::sendCancel)
            )
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcherRule = AndroidInputDispatcher.TestRule(disableDispatchInRealTime = true)
    private val eventPeriod get() = dispatcherRule.eventPeriod

    @get:Rule
    val inputDispatcherRule: TestRule = dispatcherRule

    private val recorder = PointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox(recorder, tag = tag)
        }
    }

    @Test
    fun sameScope() {
        // When we inject a down event followed by another event in the same scope
        findByTag(tag).doPartialGesture {
            sendDown(anyPosition)
            expectError<IllegalStateException>(config.isDown) {
                config.partialGesture.invoke(this, anyPosition)
            }
        }

        runOnIdleCompose {
            recorder.verifyEvents()
        }
    }

    @Test
    fun differentScope() {
        // When we inject a down event followed by another event in a different scope
        findByTag(tag).doPartialGesture { sendDown(anyPosition) }
        expectError<java.lang.IllegalStateException>(config.isDown) {
            findByTag(tag).doPartialGesture { config.partialGesture.invoke(this, anyPosition) }
        }

        runOnIdleCompose {
            recorder.verifyEvents()
        }
    }

    @Test
    fun withoutDown() {
        expectError<IllegalStateException>(!config.isDown) {
            findByTag(tag).doPartialGesture { config.partialGesture.invoke(this, anyPosition) }
        }
    }

    private fun PointerInputRecorder.verifyEvents() {
        assertTimestampsAreIncreasing()
        if (config.isDown || config.isCancel) {
            assertThat(events).hasSize(1)
            assertThat(events[0].down).isTrue()
        } else {
            assertThat(events).hasSize(2)
            assertThat(events[0].down).isTrue()
            assertThat(events[1].down).isEqualTo(!config.isUp)
            assertThat(events[1].timeDiffWith(events[0])).isEqualTo(eventPeriod)
        }
    }
}

/**
 * Tests sending of partial gestures after a partial gesture has been completed, spread out of
 * one or more invocations of [doPartialGesture].
 */
@LargeTest
@RunWith(Parameterized::class)
class DoPartialGestureAfterGestureEndTest(private val config: TestConfig) {
    data class TestConfig(
        val endGestureWith: PartialGestureScope.(PxPosition) -> Unit,
        val nextGesture: PartialGestureScope.(PxPosition) -> Unit
    ) {
        val endsWithUp = endGestureWith == PartialGestureScope::sendUp
        val nextIsDown = nextGesture == PartialGestureScope::sendDown

        override fun toString(): String {
            return "TestConfig(endGestureWith=${endGestureWith.string}, " +
                    "partialGesture=${nextGesture.string})"
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // End gesture with UP
                TestConfig(PartialGestureScope::sendUp, PartialGestureScope::sendDown),
                TestConfig(PartialGestureScope::sendUp, PartialGestureScope::sendMoveTo),
                TestConfig(PartialGestureScope::sendUp, PartialGestureScope::sendMoveBy),
                TestConfig(PartialGestureScope::sendUp, PartialGestureScope::sendUp),
                TestConfig(PartialGestureScope::sendUp, PartialGestureScope::sendCancel),
                // End gesture with CANCEL
                TestConfig(PartialGestureScope::sendCancel, PartialGestureScope::sendDown),
                TestConfig(PartialGestureScope::sendCancel, PartialGestureScope::sendMoveTo),
                TestConfig(PartialGestureScope::sendCancel, PartialGestureScope::sendMoveBy),
                TestConfig(PartialGestureScope::sendCancel, PartialGestureScope::sendUp),
                TestConfig(PartialGestureScope::sendCancel, PartialGestureScope::sendCancel)
            )
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule =
        AndroidInputDispatcher.TestRule(disableDispatchInRealTime = true)

    private val recorder = PointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox(recorder, tag = tag)
        }
    }

    @Test
    fun allInSameScope() {
        findByTag(tag).doPartialGesture {
            sendDown(anyPosition)
            config.endGestureWith.invoke(this, anyPosition)
            expectError<IllegalStateException>(!config.nextIsDown) {
                config.nextGesture.invoke(this, anyPosition)
            }
        }

        runOnIdleCompose {
            recorder.verifyEvents()
        }
    }

    @Test
    fun downEndInSameScope() {
        findByTag(tag).doPartialGesture {
            sendDown(anyPosition)
            config.endGestureWith.invoke(this, anyPosition)
        }
        expectError<IllegalStateException>(!config.nextIsDown) {
            findByTag(tag).doPartialGesture { config.nextGesture.invoke(this, anyPosition) }
        }

        runOnIdleCompose {
            recorder.verifyEvents()
        }
    }

    @Test
    fun allInDifferentScope() {
        findByTag(tag).doPartialGesture { sendDown(anyPosition) }
        findByTag(tag).doPartialGesture { config.endGestureWith.invoke(this, anyPosition) }
        expectError<IllegalStateException>(!config.nextIsDown) {
            findByTag(tag).doPartialGesture { config.nextGesture.invoke(this, anyPosition) }
        }

        runOnIdleCompose {
            recorder.verifyEvents()
        }
    }

    private fun PointerInputRecorder.verifyEvents() {
        assertTimestampsAreIncreasing()

        // Recorded events given the TestConfig:
        //
        //             | !nextIsDown |  nextIsDown
        // ------------+-------------+-----------------
        // !endsWithUp | [down]      | [down, down]
        //  endsWithUp | [down, up]  | [down, up, down]

        // Check the number of events
        var expectedSize = 1
        if (config.endsWithUp) expectedSize++ // +1 for the up event
        if (config.nextIsDown) expectedSize++ // +1 for the down event
        assertThat(events).hasSize(expectedSize)

        // Check the down values
        assertThat(events[0].down).isTrue()
        if (config.endsWithUp) {
            assertThat(events[1].down).isFalse()
        }
        if (config.nextIsDown) {
            assertThat(events.last().down).isTrue()
        }
    }
}
