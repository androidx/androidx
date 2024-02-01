/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.input.rotary

import android.view.MotionEvent.ACTION_SCROLL
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat.SOURCE_ROTARY_ENCODER
import androidx.core.view.ViewConfigurationCompat.getScaledHorizontalScrollFactor
import androidx.core.view.ViewConfigurationCompat.getScaledVerticalScrollFactor
import androidx.test.core.view.MotionEventBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RotaryScrollEventTest {
    @get:Rule
    val rule = createComposeRule()

    private val initialFocus = FocusRequester()
    private lateinit var rootView: View
    private var receivedEvent: RotaryScrollEvent? = null
    private val tolerance: Float = 0.000001f

    @Before
    fun before() {
        receivedEvent = null
    }

    @Test
    fun androidWearCrownRotation_triggersRotaryEvent() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_SCROLL)
                    .setSource(SOURCE_ROTARY_ENCODER)
                    .build()
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(receivedEvent).isNotNull()
        }
    }

    // tests bad data
    @Test
    fun androidWearCrownRotation_triggersRotaryEventWithBadData() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_SCROLL)
                    .setSource(SOURCE_ROTARY_ENCODER)
                    .setPointer(Float.NaN, Float.NaN)
                    .build()
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(receivedEvent).isNull()
        }
    }

    @Test
    fun delegated_androidWearCrownRotation_triggersRotaryEvent() {
        val node = object : DelegatingNode() {
            val rse = delegate(object : RotaryInputModifierNode, Modifier.Node() {
                override fun onRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
                    receivedEvent = event
                    return true
                }
                override fun onPreRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
                    return false
                }
            })
        }
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .elementFor(node)
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_SCROLL)
                    .setSource(SOURCE_ROTARY_ENCODER)
                    .build()
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(receivedEvent).isNotNull()
        }
    }

    @Test
    fun delegated_multiple_androidWearCrownRotation_triggersRotaryEvent() {
        var event1: RotaryScrollEvent? = null
        var event2: RotaryScrollEvent? = null
        val node = object : DelegatingNode() {
            val a = delegate(object : RotaryInputModifierNode, Modifier.Node() {
                override fun onRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
                    event1 = event
                    return false
                }
                override fun onPreRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
                    return false
                }
            })
            val b = delegate(object : RotaryInputModifierNode, Modifier.Node() {
                override fun onRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
                    event2 = event
                    return false
                }
                override fun onPreRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
                    return false
                }
            })
        }
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .elementFor(node)
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_SCROLL)
                    .setSource(SOURCE_ROTARY_ENCODER)
                    .build()
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(event1).isNotNull()
            assertThat(event2).isNotNull()
        }
    }

    @Test
    fun focusedItemReceivesHorizontalRotaryEvent() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        @OptIn(ExperimentalTestApi::class)
        rule.onRoot().performRotaryScrollInput {
            rotateToScrollHorizontally(3.0f)
        }

        // Assert.
        rule.runOnIdle {
            with(checkNotNull(receivedEvent)) {
                assertThat(verticalScrollPixels)
                    .isWithin(tolerance).of(3.0f * verticalScrollFactor / horizontalScrollFactor)
                assertThat(horizontalScrollPixels).isWithin(tolerance).of(3.0f)
            }
        }
    }

    @Test
    fun focusedItemReceivesVerticalRotaryEvent() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        @OptIn(ExperimentalTestApi::class)
        rule.onRoot().performRotaryScrollInput {
            rotateToScrollVertically(3.0f)
        }

        // Assert.
        rule.runOnIdle {
            with(checkNotNull(receivedEvent)) {
                assertThat(verticalScrollPixels).isWithin(tolerance).of(3.0f)
                assertThat(horizontalScrollPixels)
                    .isWithin(tolerance).of(3.0f * horizontalScrollFactor / verticalScrollFactor)
            }
        }
    }

    @Test
    fun rotaryEventHasTime() {
        val TIME = 1234567890L

        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_SCROLL)
                    .setSource(SOURCE_ROTARY_ENCODER)
                    .setEventTime(TIME)
                    .build()
            )
        }

        // Assert.
        rule.runOnIdle {
            with(checkNotNull(receivedEvent)) {
                assertThat(uptimeMillis).isEqualTo(TIME)
            }
        }
    }

    @Test
    fun rotaryEventHasDeviceId() {
        val DEVICE_ID = 1234

        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.runOnIdle {
            rootView.dispatchGenericMotionEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(ACTION_SCROLL)
                    .setSource(SOURCE_ROTARY_ENCODER)
                    .setDeviceId(DEVICE_ID)
                    .build()
            )
        }

        // Assert.
        rule.runOnIdle {
            with(checkNotNull(receivedEvent)) {
                assertThat(inputDeviceId).isEqualTo(DEVICE_ID)
            }
        }
    }

    @Test
    fun rotaryEventUsesTestTime() {
        val TIME_DELTA = 1234L

        val receivedEvents = mutableListOf<RotaryScrollEvent>()
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        receivedEvents.add(it)
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        @OptIn(ExperimentalTestApi::class)
        rule.onRoot().performRotaryScrollInput {
            rotateToScrollVertically(3.0f)
            advanceEventTime(TIME_DELTA)
            rotateToScrollVertically(3.0f)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            assertThat(receivedEvents[1].uptimeMillis - receivedEvents[0].uptimeMillis)
                .isEqualTo(TIME_DELTA)
        }
    }

    @Test
    fun onRotaryKeyEvent_afterUpdate() {
        // Arrange.
        val focusRequester = FocusRequester()
        var keyEventFromOnKeyEvent1: RotaryScrollEvent? = null
        var keyEventFromOnKeyEvent2: RotaryScrollEvent? = null
        var onRotaryScrollEvent: (event: RotaryScrollEvent) -> Boolean by mutableStateOf(
            value = {
                keyEventFromOnKeyEvent1 = it
                true
            }
        )
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onRotaryScrollEvent(onRotaryScrollEvent)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle {
            onRotaryScrollEvent = {
                keyEventFromOnKeyEvent2 = it
                true
            }
        }
        @OptIn(ExperimentalTestApi::class)
        rule.onRoot().performRotaryScrollInput { rotateToScrollVertically(3.0f) }

        // Assert.
        rule.runOnIdle {
            assertThat(keyEventFromOnKeyEvent1).isNull()
            assertThat(keyEventFromOnKeyEvent2).isNotNull()
        }
    }

    @Test
    fun onRotaryPreviewKeyEvent_afterUpdate() {
        // Arrange.
        val focusRequester = FocusRequester()
        var keyEventFromOnPreRotaryScrollEvent1: RotaryScrollEvent? = null
        var keyEventFromOnPreRotaryScrollEvent2: RotaryScrollEvent? = null
        var onPreRotaryScrollEvent: (event: RotaryScrollEvent) -> Boolean by mutableStateOf(
            value = {
                keyEventFromOnPreRotaryScrollEvent1 = it
                true
            }
        )
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onPreRotaryScrollEvent(onPreRotaryScrollEvent)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle {
            onPreRotaryScrollEvent = {
                keyEventFromOnPreRotaryScrollEvent2 = it
                true
            }
        }
        @OptIn(ExperimentalTestApi::class)
        rule.onRoot().performRotaryScrollInput { rotateToScrollVertically(3.0f) }

        // Assert.
        rule.runOnIdle {
            assertThat(keyEventFromOnPreRotaryScrollEvent1).isNull()
            assertThat(keyEventFromOnPreRotaryScrollEvent2).isNotNull()
        }
    }

    private fun Modifier.focusable(initiallyFocused: Boolean = false) = this
        .then(if (initiallyFocused) Modifier.focusRequester(initialFocus) else Modifier)
        .focusTarget()

    private fun ContentWithInitialFocus(content: @Composable () -> Unit) {
        rule.setContent {
            rootView = LocalView.current
            Box(modifier = Modifier.requiredSize(10.dp, 10.dp)) { content() }
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }

    private val horizontalScrollFactor: Float
        get() = getScaledHorizontalScrollFactor(
            ViewConfiguration.get(rootView.context),
            rootView.context
        )

    private val verticalScrollFactor: Float
        get() = getScaledVerticalScrollFactor(
            ViewConfiguration.get(rootView.context),
            rootView.context
        )
}
