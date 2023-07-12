/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PointerInputDensityTest {

    @get:Rule
    val rule = createComposeRule()

    private val tag = "Tagged Layout"

    @Test
    fun sendNotANumberDensityInPointerEvents() {
        lateinit var view: View

        val motionEventsToTrigger = generateMultipleMotionEvents(
            lastPointerX = Float.NaN,
            lastPointerY = Float.NaN
        )
        val recordedEvents = mutableListOf<PointerEventType>()

        rule.setContent {
            view = LocalView.current

            Box(Modifier
                .fillMaxSize()
                .background(Color.Green)
                .testTag(tag)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event: PointerEvent = awaitPointerEvent()
                            recordedEvents += event.type
                        }
                    }
                }
            ) { }
        }

        rule.waitForIdle()

        rule.runOnUiThread {
            motionEventsToTrigger.forEach {
                view.dispatchTouchEvent(it)
            }
        }
        rule.waitForIdle()

        assertThat(recordedEvents).hasSize(2)
        assertThat(recordedEvents[0]).isEqualTo(PointerEventType.Press)
        assertThat(recordedEvents[1]).isEqualTo(PointerEventType.Press)
    }

    @Test
    fun sendPositiveInfinityDensityInPointerEvents() {
        lateinit var view: View

        val motionEventsToTrigger = generateMultipleMotionEvents(
            lastPointerX = Float.POSITIVE_INFINITY,
            lastPointerY = Float.POSITIVE_INFINITY
        )
        val recordedEvents = mutableListOf<PointerEventType>()

        rule.setContent {
            view = LocalView.current

            Box(Modifier
                .fillMaxSize()
                .background(Color.Red)
                .testTag(tag)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event: PointerEvent = awaitPointerEvent()
                            recordedEvents += event.type
                        }
                    }
                }
            ) { }
        }

        rule.waitForIdle()

        rule.runOnUiThread {
            motionEventsToTrigger.forEach {
                view.dispatchTouchEvent(it)
            }
        }
        rule.waitForIdle()

        assertThat(recordedEvents).hasSize(2)
        assertThat(recordedEvents[0]).isEqualTo(PointerEventType.Press)
        assertThat(recordedEvents[1]).isEqualTo(PointerEventType.Press)
    }

    @Test
    fun sendNegativeInfinityDensityInPointerEvents() {
        lateinit var view: View

        val motionEventsToTrigger = generateMultipleMotionEvents(
            lastPointerX = Float.NEGATIVE_INFINITY,
            lastPointerY = Float.NEGATIVE_INFINITY
        )
        val recordedEvents = mutableListOf<PointerEventType>()

        rule.setContent {
            view = LocalView.current

            Box(Modifier
                .fillMaxSize()
                .background(Color.Cyan)
                .testTag(tag)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event: PointerEvent = awaitPointerEvent()
                            recordedEvents += event.type
                        }
                    }
                }
            ) { }
        }

        rule.waitForIdle()

        rule.runOnUiThread {
            motionEventsToTrigger.forEach {
                view.dispatchTouchEvent(it)
            }
        }
        rule.waitForIdle()

        assertThat(recordedEvents).hasSize(2)
        assertThat(recordedEvents[0]).isEqualTo(PointerEventType.Press)
        assertThat(recordedEvents[1]).isEqualTo(PointerEventType.Press)
    }

    @Test
    fun compositionLocalDensityChangeRestartsPointerInputOverload1() {
        compositionLocalDensityChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, block = it)
        }
    }

    @Test
    fun compositionLocalDensityChangeRestartsPointerInputOverload2() {
        compositionLocalDensityChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, Unit, block = it)
        }
    }

    @Test
    fun compositionLocalDensityChangeRestartsPointerInputOverload3() {
        compositionLocalDensityChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, Unit, Unit, block = it)
        }
    }

    private fun compositionLocalDensityChangeRestartsPointerInput(
        pointerInput: (block: suspend PointerInputScope.() -> Unit) -> Modifier
    ) {
        var density by mutableStateOf(5f)

        val pointerInputDensities = mutableListOf<Float>()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density)) {
                Box(pointerInput {
                    pointerInputDensities.add(density)
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }.testTag(tag)
                )
            }
        }

        // Because the pointer input coroutine scope is created lazily, that is, it won't be
        // created/triggered until there is a event(tap), we must trigger a tap to instantiate the
        // pointer input block of code.
        rule.waitForIdle()
        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(Offset.Zero)
                moveBy(Offset(1f, 1f))
                up()
            }

        rule.runOnIdle {
            assertThat(pointerInputDensities.size).isEqualTo(1)
            assertThat(pointerInputDensities.last()).isEqualTo(5f)
            density = 9f
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(Offset.Zero)
                moveBy(Offset(1f, 1f))
                up()
            }

        rule.runOnIdle {
            assertThat(pointerInputDensities.size).isEqualTo(2)
            assertThat(pointerInputDensities.last()).isEqualTo(9f)
        }
    }

    private fun generateMultipleMotionEvents(
        lastPointerX: Float,
        lastPointerY: Float
    ): List<MotionEvent> {
        val firstPointerOffset = Offset(0f, 0f)
        val secondPointerOffset = Offset(10f, 0f)

        val firstFingerPointerPropertiesId = 0
        val secondFingerPointerPropertiesId = 1
        val thirdFingerPointerPropertiesId = 2

        val firstPointerProperties =
            PointerProperties(firstFingerPointerPropertiesId).also {
                it.toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        val secondPointerProperties =
            PointerProperties(secondFingerPointerPropertiesId).also {
                it.toolType = MotionEvent.TOOL_TYPE_FINGER
            }

        val thirdPointerProperties =
            PointerProperties(thirdFingerPointerPropertiesId).also {
                it.toolType = MotionEvent.TOOL_TYPE_FINGER
            }

        val eventDownTime = 1L
        var eventStartTime = 0L

        val firstPointerEvent = MotionEvent.obtain(
            eventDownTime,
            eventStartTime,
            MotionEvent.ACTION_DOWN,
            1,
            arrayOf(firstPointerProperties),
            arrayOf(PointerCoords(firstPointerOffset.x, firstPointerOffset.y)),
            0,
            0,
            0f,
            0f,
            0,
            0,
            0,
            0
        )

        eventStartTime += 500

        val secondPointerEvent = MotionEvent.obtain(
            eventDownTime,
            eventStartTime,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            arrayOf(
                firstPointerProperties,
                secondPointerProperties
            ),
            arrayOf(
                PointerCoords(firstPointerOffset.x, firstPointerOffset.y),
                PointerCoords(secondPointerOffset.x, secondPointerOffset.y)
            ),
            0,
            0,
            0f,
            0f,
            0,
            0,
            0,
            0
        )

        eventStartTime += 500

        val thirdPointerEvent = MotionEvent.obtain(
            eventDownTime,
            eventStartTime,
            MotionEvent.ACTION_POINTER_DOWN,
            3,
            arrayOf(
                firstPointerProperties,
                secondPointerProperties,
                thirdPointerProperties
            ),
            arrayOf(
                PointerCoords(firstPointerOffset.x, firstPointerOffset.y),
                PointerCoords(secondPointerOffset.x, secondPointerOffset.y),
                PointerCoords(lastPointerX, lastPointerY)
            ),
            0,
            0,
            0f,
            0f,
            0,
            0,
            0,
            0
        )

        return listOf(firstPointerEvent, secondPointerEvent, thirdPointerEvent)
    }
}
