/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.VelocityPathFinder
import androidx.compose.ui.test.util.isAlmostBetween
import androidx.compose.ui.test.util.isAlmostEqualTo
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.lerp
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests of [VelocityPathFinder] creates paths that will lead to the desired velocity on Platform.
 */
@RunWith(Parameterized::class)
class PlatformVelocityPathFinderTest(private val config: TestConfig) {
    data class TestConfig(
        val end: Offset,
        val requestedVelocity: Float,
        val durationMillis: Long,
        val expectedError: Boolean,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() =
            mutableListOf<TestConfig>().apply {
                for (direction in listOf(Direction.N)) {
                    // Test cases tailored for PlatformVelocityTracker

                    // 1. Zero velocity scenarios across various durations
                    add(TestConfig(direction.offset, 0f, 100L, true)) // v == 0, short duration
                    add(TestConfig(direction.offset, 0f, 500L, false)) // v == 0, medium duration
                    add(TestConfig(direction.offset, 0f, 1500L, false)) // v == 0, long duration

                    // 2. Slow / Low velocity flings (300 px/s - 800 px/s)
                    add(TestConfig(direction.offset, 300f, 500L, false))
                    add(TestConfig(direction.offset, 500f, 500L, false))
                    add(TestConfig(direction.offset, 800f, 1000L, false))

                    // 3. Medium / Faster velocity flings (1500 px/s - 4000 px/s)
                    add(TestConfig(direction.offset, 1500f, 500L, false))
                    add(TestConfig(direction.offset, 2500f, 300L, false))
                    add(TestConfig(direction.offset, 4000f, 500L, false))

                    // 4. Fast / High velocity flings (> 6000 px/s)
                    add(TestConfig(direction.offset, 6000f, 200L, false))
                    add(TestConfig(direction.offset, 6000f, 66L, false))
                    add(TestConfig(direction.offset, 10000f, 100L, true))
                }
            }
    }

    @Before
    @OptIn(ExperimentalComposeUiApi::class)
    fun setUp() {
        assumeTrue(AndroidComposeUiFlags.isFrameworkVelocityTrackerEnabled)
    }

    @Test
    fun test() {
        if (config.expectedError) {
            testWithExpectedError(config)
        } else {
            testWithoutExpectedError(config)
        }
    }

    private fun testWithoutExpectedError(config: TestConfig) {
        val pathFinder =
            VelocityPathFinder(
                startPosition = Offset.Zero,
                endPosition = config.end,
                endVelocity = config.requestedVelocity,
                durationMillis = config.durationMillis,
            )

        val f: (Long) -> Offset = { pathFinder.calculateOffsetForTime(it) }
        val velocityTracker = simulateSwipe(config, f)
        val velocity = velocityTracker.calculateVelocity()
        val tolerance = max(1f, config.requestedVelocity * 0.10f)
        assertThat(velocity.sum()).isWithin(tolerance).of(config.requestedVelocity)
        if (config.requestedVelocity > 0) {
            // Direction of velocity of 0 is undefined, so any direction is correct
            velocity.toOffset().normalize().isAlmostEqualTo(config.end.normalize())
        }

        // At t = 0, the function should return the start position (which is Offset.Zero here)
        f(0).isAlmostEqualTo(Offset.Zero)
        // At any time, the function should be between the start and end
        for (t in 0..config.durationMillis) {
            assertThat(f(t).x).isAlmostBetween(0f, config.end.x)
            assertThat(f(t).y).isAlmostBetween(0f, config.end.y)
        }
        // At t = durationMillis, the function should return the end position
        f(config.durationMillis).isAlmostEqualTo(config.end)
    }

    private fun testWithExpectedError(config: TestConfig) {
        try {
            VelocityPathFinder(
                    startPosition = Offset.Zero,
                    endPosition = config.end,
                    endVelocity = config.requestedVelocity,
                    durationMillis = config.durationMillis,
                )
                .calculateOffsetForTime(0L)
            fail("Expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message)
                .startsWith(
                    "Unable to generate a swipe gesture between ${Offset.Zero} and ${config.end} " +
                        "with duration ${config.durationMillis} that ends with velocity of " +
                        "${config.requestedVelocity} px/s, without going outside of the range " +
                        "[start..end]. Suggested fixes: "
                )
        }
    }

    private fun simulateSwipe(config: TestConfig, f: (Long) -> Offset): VelocityTracker {
        val velocityTracker = VelocityTracker()
        val steps = max(1, (config.durationMillis / eventPeriodMillis.toFloat()).roundToInt())
        for (step in 0..steps) {
            val progress = step / steps.toFloat()
            val t = lerp(0, config.durationMillis, progress)
            velocityTracker.addPosition(t, f(t))
        }
        return velocityTracker
    }

    private fun Offset.normalize(): Offset =
        if (isFinite && this != Offset.Zero) this / getDistance() else this

    private fun Velocity.toOffset(): Offset = Offset(x, y)

    private fun Velocity.sum(): Float = sqrt(x * x + y * y)

    /**
     * Direction of the swipe, when starting from [Offset.Zero]. N/W/S/E are straight lines,
     * NW/SW/SE/NE are at a 60º angle.
     */
    enum class Direction(val offset: Offset) {
        N(Offset(0f, -200f)),
        NW(Offset(-100f, -173.2f)),
        W(Offset(-200f, 0f)),
        SW(Offset(-173.2f, 100f)),
        S(Offset(0f, 200f)),
        SE(Offset(100f, 173.2f)),
        E(Offset(200f, 0f)),
        NE(Offset(173.2f, -100f)),
    }
}
