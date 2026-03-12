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

package androidx.compose.ui.test.injectionscope.trackpad

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.TrackpadInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.panWithVelocity
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.DataPoint
import androidx.compose.ui.test.util.RecordingFilter
import androidx.compose.ui.unit.Velocity
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test for [TrackpadInjectionScope.panWithVelocity] to see if we can generate gestures that end
 * with a specific velocity.
 */
@MediumTest
@RunWith(Parameterized::class)
class PanWithVelocityTest(private val config: TestConfig) {
    data class TestConfig(val durationMillis: Long, val velocity: Float)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (duration in listOf(100, 500, 1000)) {
                    for (velocity in listOf(100f, 999f, 2000f)) {
                        add(TestConfig(duration.toLong(), velocity))
                    }
                }
            }
        }

        private const val tag = "widget"

        private const val boxSize = 800f
        private val boxCenter = Offset(boxSize, boxSize) / 2f

        private val panDelta = Offset(200f, 0f)
    }

    @get:Rule val rule = createComposeRule()

    private val recorder = TrackpadPanInputRecorder()

    @Test
    fun panWithVelocity() {
        rule.setContent {
            Box(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
                ClickableTestBox(recorder, boxSize, boxSize, tag = tag)
            }
        }

        rule.onNodeWithTag(tag).performTrackpadInput {
            moveTo(center)
            panWithVelocity(panDelta, config.velocity, config.durationMillis)
        }

        rule.runOnIdle {
            recorder.run {
                // At least the last 100ms should have velocity
                val minimumEventSize = max(2, (100 / eventPeriodMillis).toInt())
                assertThat(events.size).isAtLeast(minimumEventSize)

                assertSinglePointer()
                assertThat(events.first().eventType).isEqualTo(PointerEventType.Enter)

                // Check timestamps
                assertTimestampsAreIncreasing()
                assertThat(recordedDurationMillis).isEqualTo(config.durationMillis)

                val computedVelocity: Velocity

                @OptIn(ExperimentalComposeUiApi::class)
                if (
                    ComposeUiFlags.isTrackpadGestureHandlingEnabled && Build.VERSION.SDK_INT >= 34
                ) {
                    assertThat(events.map { it.position }.toSet()).containsExactly(boxCenter)
                    assertThat(events[1].eventType).isEqualTo(PointerEventType.PanStart)
                    assertThat(events.subList(2, events.size - 1).map { it.eventType }.toSet())
                        .containsExactly(PointerEventType.PanMove)
                    assertThat(events.last().eventType).isEqualTo(PointerEventType.PanEnd)

                    computedVelocity = -panVelocityTracker.calculateVelocity()
                } else {
                    assertThat(events[1].eventType).isEqualTo(PointerEventType.Press)
                    assertThat(events.subList(2, events.size - 2).map { it.eventType }.toSet())
                        .containsExactly(PointerEventType.Move)
                    assertThat(events[events.size - 2].eventType)
                        .isEqualTo(PointerEventType.Release)
                    assertThat(events.last().eventType).isEqualTo(PointerEventType.Move)

                    computedVelocity = fingerVelocityTracker.calculateVelocity()
                }

                assertThat(computedVelocity.x).isWithin(0.1f).of(config.velocity)
                assertThat(computedVelocity.y).isWithin(0.1f).of(0f)
            }
        }
    }
}

private class TrackpadPanInputRecorder : PointerInputModifier {
    private val _events = mutableListOf<DataPoint>()
    val events
        get() = _events as List<DataPoint>

    val fingerVelocityTracker = VelocityTracker()
    val panVelocityTracker = VelocityTracker()
    private var accumulatedPan = Offset.Zero

    override val pointerInputFilter = RecordingFilter { event ->
        event.changes.forEach {
            _events.add(DataPoint(it, event))
            if (it.pressed) {
                fingerVelocityTracker.addPosition(it.uptimeMillis, it.position)
            } else if (event.type == PointerEventType.PanMove) {
                accumulatedPan += it.panOffset
                panVelocityTracker.addPosition(it.uptimeMillis, accumulatedPan)
            }
        }
    }
}

private fun TrackpadPanInputRecorder.assertTimestampsAreIncreasing() {
    check(events.isNotEmpty()) { "No events recorded" }
    events.reduce { prev, curr ->
        assertThat(curr.timestamp).isAtLeast(prev.timestamp)
        curr
    }
}

private val TrackpadPanInputRecorder.recordedDurationMillis: Long
    get() {
        check(events.isNotEmpty()) { "No events recorded" }
        return events.last().timestamp - events.first().timestamp
    }

private fun TrackpadPanInputRecorder.assertSinglePointer() {
    assertThat(events.map { it.id }.distinct()).hasSize(1)
}
