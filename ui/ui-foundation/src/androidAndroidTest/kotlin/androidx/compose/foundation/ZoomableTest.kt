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

package androidx.compose.foundation

import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.compose.foundation.gestures.ZoomableState
import androidx.compose.foundation.gestures.zoomable
import androidx.ui.geometry.Offset
import androidx.compose.foundation.layout.preferredSize
import androidx.ui.test.AnimationClockTestRule
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.ui.test.pinch
import androidx.ui.test.size
import androidx.ui.unit.dp
import androidx.ui.unit.toSize
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val TEST_TAG = "zoomableTestTag"

private const val EDGE_FUZZ_FACTOR = 0.2f

@SmallTest
@RunWith(JUnit4::class)
class ZoomableTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val clockRule = AnimationClockTestRule()

    @Test
    fun zoomable_zoomIn() {
        var cumulativeScale = 1.0f
        val state = ZoomableState(
            onZoomDelta = { cumulativeScale *= it },
            animationClock = clockRule.clock
        )

        setZoomableContent { Modifier.zoomable(state) }

        onNodeWithTag(TEST_TAG).performGesture {
            val leftStartX = center.x - 10
            val leftEndX = size.toSize().width * EDGE_FUZZ_FACTOR
            val rightStartX = center.x + 10
            val rightEndX = size.toSize().width * (1 - EDGE_FUZZ_FACTOR)

            pinch(
                Offset(leftStartX, center.y),
                Offset(leftEndX, center.y),
                Offset(rightStartX, center.y),
                Offset(rightEndX, center.y)
            )
        }

        clockRule.advanceClock(milliseconds = 1000)

        runOnIdle {
            assertWithMessage("Should have scaled at least 4x").that(cumulativeScale).isAtLeast(4f)
        }
    }

    @Test
    fun zoomable_zoomOut() {
        var cumulativeScale = 1.0f
        val state = ZoomableState(
            onZoomDelta = { cumulativeScale *= it },
            animationClock = clockRule.clock
        )

        setZoomableContent { Modifier.zoomable(state) }

        onNodeWithTag(TEST_TAG).performGesture {
            val leftStartX = size.toSize().width * EDGE_FUZZ_FACTOR
            val leftEndX = center.x - 10
            val rightStartX = size.toSize().width * (1 - EDGE_FUZZ_FACTOR)
            val rightEndX = center.x + 10

            pinch(
                Offset(leftStartX, center.y),
                Offset(leftEndX, center.y),
                Offset(rightStartX, center.y),
                Offset(rightEndX, center.y)
            )
        }

        clockRule.advanceClock(milliseconds = 1000)

        runOnIdle {
            assertWithMessage("Should have scaled down at least 4x")
                .that(cumulativeScale)
                .isAtMost(0.25f)
        }
    }

    @Test
    fun zoomable_animateTo() {
        var cumulativeScale = 1.0f
        var callbackCount = 0
        val state = ZoomableState(
            onZoomDelta = {
                cumulativeScale *= it
                callbackCount += 1
            },
            animationClock = clockRule.clock
        )

        setZoomableContent { Modifier.zoomable(state) }

        runOnUiThread { state.smoothScaleBy(4f) }

        clockRule.advanceClock(milliseconds = 10)

        runOnIdle {
            assertWithMessage("Scrolling should have been smooth").that(callbackCount).isAtLeast(1)
        }

        clockRule.advanceClock(milliseconds = 10)

        runOnIdle {
            assertWithMessage("Scrolling should have been smooth").that(callbackCount).isAtLeast(2)
        }

        clockRule.advanceClock(milliseconds = 1000)

        runOnIdle {
            assertWithMessage("Scrolling should have been smooth").that(callbackCount).isAtLeast(3)
            assertWithMessage("Should have scaled at least 4x").that(cumulativeScale).isAtLeast(4f)
        }
    }

    private fun setZoomableContent(getModifier: @Composable () -> Modifier) {
        composeTestRule.setContent {
            Box(Modifier.preferredSize(600.dp).testTag(TEST_TAG) + getModifier())
        }
    }
}
