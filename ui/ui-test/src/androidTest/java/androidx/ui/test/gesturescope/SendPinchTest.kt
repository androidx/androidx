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

package androidx.ui.test.gesturescope

import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendPinch
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.MultiPointerInputRecorder
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.isMonotonicBetween
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SendPinchTest {
    companion object {
        private const val TAG = "PINCH"
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val eventPeriod = AndroidInputDispatcher.TestRule().eventPeriod
    private val recorder = MultiPointerInputRecorder()

    @Test
    fun pinch() {
        composeTestRule.setContent {
            Stack(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, tag = TAG)
            }
        }

        val start0 = PxPosition(40f, 50f)
        val end0 = PxPosition(8f, 50f)
        val start1 = PxPosition(60f, 50f)
        val end1 = PxPosition(92f, 50f)
        val duration = 400.milliseconds

        findByTag(TAG).doGesture {
            sendPinch(start0, end0, start1, end1, duration)
        }

        runOnIdleCompose {
            recorder.run {
                assertTimestampsAreIncreasing()

                val expectedMoveEvents = duration.inMilliseconds() / eventPeriod
                // expect up and down events for each pointer as well as the move events
                assertThat(events.size).isEqualTo(4 + expectedMoveEvents)

                val pointerChanges = events.flatMap { it.pointers }

                val pointerIds = pointerChanges.map { it.id }.distinct()
                val pointerUpChanges = pointerChanges.filter { !it.down }

                assertThat(pointerIds).hasSize(2)

                // Assert each pointer went back up
                assertThat(pointerUpChanges.map { it.id }).containsExactlyElementsIn(pointerIds)

                // Assert the up events are at the end
                @Suppress("NestedLambdaShadowedImplicitParameter")
                assertThat(events.takeLastWhile { it.pointers.any { !it.down } }).hasSize(2)

                pointerChanges.filter { it.id.value == 0L }.isMonotonicBetween(start0, end0)
                pointerChanges.filter { it.id.value == 1L }.isMonotonicBetween(start1, end1)
            }
        }
    }
}
