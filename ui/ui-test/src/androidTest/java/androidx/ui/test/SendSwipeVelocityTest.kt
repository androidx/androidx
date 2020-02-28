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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.test.filters.MediumTest
import androidx.ui.core.DensityAmbient
import androidx.ui.core.PointerInput
import androidx.ui.foundation.Canvas
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.PointerInputRecorder
import androidx.ui.test.util.assertOnlyLastEventIsUp
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.downEvents
import androidx.ui.test.util.isAlmostEqualTo
import androidx.ui.test.util.isMonotonousBetween
import androidx.ui.test.util.recordedDuration
import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.getDistance
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.max

/**
 * Tests if we can generate gestures that end with a specific velocity
 */
@MediumTest
@RunWith(Parameterized::class)
class SendSwipeVelocityTest(private val config: TestConfig) {
    data class TestConfig(
        val direction: Direction,
        val duration: Duration,
        val velocity: Float,
        val eventPeriod: Long
    )

    enum class Direction(
        val x0: (Rect) -> Float,
        val y0: (Rect) -> Float,
        val x1: (Rect) -> Float,
        val y1: (Rect) -> Float
    ) {
        LeftToRight({ it.left + 1 }, ::hmiddle, { it.right - 1 }, ::hmiddle),
        RightToLeft({ it.right - 1 }, ::hmiddle, { it.left + 1 }, ::hmiddle),
        TopToBottom(::vmiddle, { it.top + 1 }, ::vmiddle, { it.bottom - 1 }),
        BottomToTop(::vmiddle, { it.bottom - 1 }, ::vmiddle, { it.top + 1 })
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (period in listOf(10L, 7L, 16L)) {
                    for (direction in Direction.values()) {
                        for (duration in listOf(100, 500, 1000)) {
                            for (velocity in listOf(79f, 200f, 1500f, 4691f)) {
                                add(
                                    TestConfig(
                                        direction,
                                        Duration(milliseconds = duration.toLong()),
                                        velocity,
                                        period
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun hmiddle(bounds: Rect): Float = (bounds.left + bounds.right) / 2
        private fun vmiddle(bounds: Rect): Float = (bounds.top + bounds.bottom) / 2

        private const val tag = "widget"
    }

    private val x0 get() = config.direction.x0(bounds)
    private val y0 get() = config.direction.y0(bounds)
    private val x1 get() = config.direction.x1(bounds)
    private val y1 get() = config.direction.y1(bounds)
    private val start get() = PxPosition(x0.px, y0.px)
    private val end get() = PxPosition(x1.px, y1.px)
    private val duration get() = config.duration
    private val velocity get() = config.velocity
    private val eventPeriod get() = config.eventPeriod

    private val expectedXVelocity = when (config.direction) {
        Direction.LeftToRight -> velocity
        Direction.RightToLeft -> -velocity
        else -> 0f
    }

    private val expectedYVelocity = when (config.direction) {
        Direction.TopToBottom -> velocity
        Direction.BottomToTop -> -velocity
        else -> 0f
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true,
        eventPeriodOverride = eventPeriod
    )

    private lateinit var recorder: PointerInputRecorder
    private lateinit var bounds: Rect

    @Before
    fun setUp() {
        recorder = PointerInputRecorder()
    }

    @Composable
    fun Ui() {
        val paint = remember { Paint().apply { color = Color.Yellow } }
        Stack(LayoutSize.Fill + LayoutAlign.BottomEnd) {
            Semantics(container = true, properties = { testTag = tag }) {
                PointerInput(
                    pointerInputHandler = recorder::onPointerInput,
                    cancelHandler = {}
                ) {
                    with(DensityAmbient.current) {
                        Canvas(LayoutSize(500.px.toDp())) {
                            bounds = Rect(0f, 0f, size.width.value, size.height.value)
                            drawRect(bounds, paint)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun swipeWithVelocity() {
        composeTestRule.setContent { Ui() }
        findByTag(tag).doGesture { sendSwipeWithVelocity(start, end, velocity, duration) }
        composeTestRule.runOnUiThread {
            recorder.run {
                val durationMs = duration.inMilliseconds()
                val minimumEventSize = max(2, (durationMs / eventPeriod).toInt())
                assertThat(events.size).isAtLeast(minimumEventSize)
                assertOnlyLastEventIsUp()

                // Check coordinates
                events.first().position.isAlmostEqualTo(start)
                downEvents.isMonotonousBetween(start, end)
                events.last().position.isAlmostEqualTo(end)

                // Check timestamps
                assertTimestampsAreIncreasing()
                assertThat(recordedDuration).isEqualTo(duration)

                // Check velocity
                val actualVelocity = recordedVelocity.pixelsPerSecond
                assertThat(actualVelocity.x.value).isWithin(.1f).of(expectedXVelocity)
                assertThat(actualVelocity.y.value).isWithin(.1f).of(expectedYVelocity)
                assertThat(actualVelocity.getDistance().value)
                    .isWithin(velocity * 0.001f).of(velocity)
            }
        }
    }
}
