/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.input.pointer.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Velocity
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests that the [FrameworkVelocityTracker] works as expected, without depending on the underlying
 * implementation of the Framework VelocityTracker.
 */
@SmallTest
@RunWith(JUnit4::class)
class FrameworkVelocityTrackerTest {

    private val MaximumVelocity = Velocity(Float.MAX_VALUE, Float.MAX_VALUE)

    private fun addStroke(tracker: FrameworkVelocityTracker) {
        // Movement at 1000 units/second, for 10 steps.
        val timeStep = 10L
        val stepCount = 10
        // X will have positive velocity; Y will have negative velocity.
        val xStep = 10f
        val yStep = -10f
        for (i in 1..stepCount) {
            tracker.addPosition(i * timeStep, Offset(i * xStep, i * yStep))
        }
    }

    @Test
    fun addPosition_generatesVelocity() {
        val tracker = FrameworkVelocityTracker()
        addStroke(tracker)

        val velocity = tracker.calculateVelocity(MaximumVelocity)
        assertThat(velocity.x).isGreaterThan(0f)
        assertThat(velocity.y).isLessThan(0f)
    }

    @Test
    fun addPointerInputChange_generatesVelocity() {
        val tracker = FrameworkVelocityTracker()
        // Movement at 1000 units/second.
        val timeStep = 10L
        val stepCount = 10
        // X will have positive velocity; Y will have negative velocity.
        val xStep = 10f
        val yStep = -10f

        tracker.addPointerInputChange(createDownEvent(0, Offset.Zero), Offset.Zero)
        for (i in 1..stepCount) {
            tracker.addPointerInputChange(
                createMoveEvent(i * timeStep, Offset(i * xStep, i * yStep)),
                Offset.Zero,
            )
        }
        tracker.addPointerInputChange(createUpEvent(stepCount * timeStep), Offset.Zero)

        val velocity = tracker.calculateVelocity(MaximumVelocity)
        assertThat(velocity.x).isGreaterThan(0f)
        assertThat(velocity.y).isLessThan(0f)

        // A down event is a reset.
        tracker.addPointerInputChange(
            createDownEvent((stepCount + 1) * timeStep, Offset.Zero),
            Offset.Zero,
        )
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isEqualTo(Velocity.Zero)
    }

    @Test
    fun addPointerInputChangeWithHistory_generatesVelocity() {
        // This test is identical to addPointerInputChange_generatesVelocity, except that all of
        // the move events are sent as a single PointerInputChange with historical events.

        val tracker = FrameworkVelocityTracker()
        // Movement at 1000 units/second.
        val timeStep = 10L
        val stepCount = 10
        // X will have positive velocity; Y will have negative velocity.
        val xStep = 10f
        val yStep = -10f

        tracker.addPointerInputChange(createDownEvent(0, Offset.Zero), Offset.Zero)

        val history = mutableListOf<HistoricalChange>()
        for (i in 1..stepCount - 1) {
            history.add(HistoricalChange(i * timeStep, Offset(i * xStep, i * yStep)))
        }
        tracker.addPointerInputChange(
            createMoveEvent(
                stepCount * timeStep,
                Offset(stepCount * xStep, stepCount * yStep),
                history,
            ),
            Offset.Zero,
        )

        tracker.addPointerInputChange(createUpEvent(stepCount * timeStep), Offset.Zero)

        val velocity = tracker.calculateVelocity(MaximumVelocity)
        assertThat(velocity.x).isGreaterThan(0f)
        assertThat(velocity.y).isLessThan(0f)

        // A down event is a reset.
        tracker.addPointerInputChange(
            createDownEvent((stepCount + 1) * timeStep, Offset.Zero),
            Offset.Zero,
        )
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isEqualTo(Velocity.Zero)
    }

    @Test
    fun calculateVelocity_maximumApplied() {
        val tracker = FrameworkVelocityTracker()
        addStroke(tracker)

        val velocityLimit = Velocity(5f, 5f)
        val velocity = tracker.calculateVelocity(velocityLimit)
        assertThat(velocity.x).isAtMost(velocityLimit.x)
        assertThat(velocity.y).isAtLeast(-velocityLimit.y)
    }

    @Test
    fun resetTracking_worksAsExpected() {
        val tracker = FrameworkVelocityTracker()
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isEqualTo(Velocity.Zero)

        addStroke(tracker)
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isNotEqualTo(Velocity.Zero)

        tracker.resetTracking()
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isEqualTo(Velocity.Zero)

        addStroke(tracker)
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isNotEqualTo(Velocity.Zero)
    }

    @Test
    fun longGapBetweenEvents_resetsTracking() {
        val tracker = FrameworkVelocityTracker()
        addStroke(tracker)
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isNotEqualTo(Velocity.Zero)

        tracker.addPosition(1000L, Offset(1000f, 1000f))
        assertThat(tracker.calculateVelocity(MaximumVelocity)).isEqualTo(Velocity.Zero)
    }
}

private fun createDownEvent(timeMillis: Long, position: Offset) =
    PointerInputChange(
        id = PointerId(0),
        uptimeMillis = timeMillis,
        position = position,
        pressed = true,
        pressure = 1.0f,
        previousUptimeMillis = 0,
        previousPosition = Offset.Zero,
        previousPressed = false,
        isInitiallyConsumed = false,
        type = PointerType.Touch,
        historical = emptyList(),
        scrollDelta = Offset.Zero,
        scaleFactor = 1.0f,
        panOffset = Offset.Zero,
        originalEventPosition = position,
    )

private fun createMoveEvent(
    timeMillis: Long,
    position: Offset,
    history: List<HistoricalChange> = emptyList(),
) =
    PointerInputChange(
        id = PointerId(0),
        uptimeMillis = timeMillis,
        position = position,
        pressed = true,
        pressure = 1.0f,
        previousUptimeMillis = 0,
        previousPosition = Offset.Zero,
        previousPressed = true,
        isInitiallyConsumed = false,
        type = PointerType.Touch,
        historical = history,
        scrollDelta = Offset.Zero,
        scaleFactor = 1.0f,
        panOffset = Offset.Zero,
        originalEventPosition = position,
    )

private fun createUpEvent(timeMillis: Long) =
    PointerInputChange(
        id = PointerId(0),
        uptimeMillis = timeMillis,
        position = Offset.Zero,
        pressed = false,
        pressure = 1.0f,
        previousUptimeMillis = 0,
        previousPosition = Offset.Zero,
        previousPressed = true,
        isInitiallyConsumed = false,
    )
