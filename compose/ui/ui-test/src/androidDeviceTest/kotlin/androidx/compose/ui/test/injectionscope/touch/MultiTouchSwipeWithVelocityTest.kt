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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SwipeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.multiTouchSwipeWithVelocity
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MultiTouchSwipeWithVelocityTest {

    @get:Rule
    val rule =
        createComposeRule(
            androidx.compose.ui.test.ComposeUiTestConfig(effectContext = StandardTestDispatcher())
        )
    private val recorder = MultiPointerInputRecorder()

    @Test
    fun testMultiTouchSwipeWithVelocity() {
        val boxSize = 500f
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(recorder, boxSize, boxSize, tag = "widget")
            }
        }

        val startA = Offset(10f, 10f)
        val endA = Offset(100f, 10f)
        val velA = 150f

        val startB = Offset(10f, 100f)
        val endB = Offset(200f, 100f)
        val velB = 300f

        val duration = 200L

        rule.onNodeWithTag("widget").performTouchInput {
            multiTouchSwipeWithVelocity(
                swipes = listOf(SwipeInfo(startA, endA, velA), SwipeInfo(startB, endB, velB)),
                durationMillis = duration,
            )
        }

        rule.runOnIdle {
            // Verify velocities
            val trackerA = VelocityTracker()
            val trackerB = VelocityTracker()

            recorder.events.forEach { event ->
                event.pointers.forEach { pointer ->
                    if (pointer.id.value == 0L) {
                        trackerA.addPosition(pointer.timestamp, pointer.position)
                    } else if (pointer.id.value == 1L) {
                        trackerB.addPosition(pointer.timestamp, pointer.position)
                    }
                }
            }

            val velocityA = trackerA.calculateVelocity()
            val velocityB = trackerB.calculateVelocity()

            assertThat(velocityA.x).isWithin(0.1f).of(velA)
            assertThat(velocityA.y).isWithin(0.1f).of(0f)
            assertThat(velocityB.x).isWithin(0.1f).of(velB)
            assertThat(velocityB.y).isWithin(0.1f).of(0f)
        }
    }
}
