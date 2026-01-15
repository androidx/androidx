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

package androidx.compose.ui.tooling.animation.clock

import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.tooling.animation.Utils.assertEquals
import androidx.compose.ui.tooling.animation.Utils.createTestAnimatedVisibility
import androidx.compose.ui.tooling.animation.parseAnimatedVisibility
import androidx.compose.ui.tooling.animation.states.AnimatedVisibilityState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnimatedVisibilityClockTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun checkClockAfterStateChanged() {
        val clock = setupClock()
        rule.runOnIdle {
            assertEquals(AnimatedVisibilityState.Enter, clock.state)
            assertEquals(380, clock.getMaxDuration(), 30)
            assertEquals(380, clock.getMaxDurationPerIteration(), 30)
            val transitions = clock.getTransitions(200L)
            assertEquals(3, transitions.size)
            transitions[0].let {
                assertEquals("Built-in InterruptionHandlingOffset", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(0, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
            }
            transitions[1].let {
                assertEquals("Built-in alpha", it.label)
                assertEquals(0, it.startTimeMillis)
                assertTrue(it.endTimeMillis > 300)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
                assertEquals(3, it.values.size)
            }
            transitions[2].let {
                assertEquals("Built-in shrink/expand", it.label)
                assertEquals(0, it.startTimeMillis)
                assertTrue(it.endTimeMillis > 300)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
                assertEquals(3, it.values.size)
            }
            clock.state = AnimatedVisibilityState.Exit
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(AnimatedVisibilityState.Exit, clock.state)
            assertEquals(380, clock.getMaxDuration(), 30)
            assertEquals(380, clock.getMaxDurationPerIteration(), 30)
            val transitions = clock.getTransitions(200L)
            assertEquals(3, transitions.size)
            transitions[0].let {
                assertEquals("Built-in InterruptionHandlingOffset", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(0, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
            }
            transitions[1].let {
                assertEquals("Built-in alpha", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(330, it.endTimeMillis, 30)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
                assertEquals(3, it.values.size)
            }
            transitions[2].let {
                assertEquals("Built-in shrink/expand", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(380, it.endTimeMillis, 30)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
                assertEquals(3, it.values.size)
            }
        }
    }

    @Test
    fun changeTime() {
        val clock = setupClock()
        rule.runOnIdle {
            val propertiesAt0 = clock.getAnimatedProperties()
            propertiesAt0[0].let {
                assertEquals("Built-in InterruptionHandlingOffset", it.label)
                assertEquals(IntOffset(0, 0), it.value)
            }
            propertiesAt0[1].let {
                assertEquals("Built-in alpha", it.label)
                assertEquals(0f, it.value)
            }
            propertiesAt0[2].let {
                assertEquals("Built-in shrink/expand", it.label)
                assertEquals(IntSize(0, 0), it.value)
            }
            clock.setClockTime(millisToNanos(400L))
            val propertiesAt400 = clock.getAnimatedProperties()
            propertiesAt400[0].let {
                assertEquals("Built-in InterruptionHandlingOffset", it.label)
                assertEquals(IntOffset(0, 0), it.value)
            }
            propertiesAt400[1].let {
                assertEquals("Built-in alpha", it.label)
                assertEquals(1f, it.value)
            }
            propertiesAt400[2].let {
                assertEquals("Built-in shrink/expand", it.label)
                assertNotEquals(IntSize(0, 0), it.value)
            }
            // Change start and end state and reset time.
            clock.state = AnimatedVisibilityState.Exit
            clock.setClockTime(0L)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            val propertiesAt0 = clock.getAnimatedProperties()
            propertiesAt0[0].let {
                assertEquals("Built-in InterruptionHandlingOffset", it.label)
                assertEquals(IntOffset(0, 0), it.value)
            }
            propertiesAt0[1].let {
                assertEquals("Built-in alpha", it.label)
                assertEquals(1f, it.value)
            }
            propertiesAt0[2].let {
                assertEquals("Built-in shrink/expand", it.label)
                assertNotEquals(IntSize(0, 0), it.value)
            }
            clock.setClockTime(millisToNanos(100L))
            val propertiesAt400 = clock.getAnimatedProperties()
            propertiesAt400[0].let {
                assertEquals("Built-in InterruptionHandlingOffset", it.label)
                assertEquals(IntOffset(0, 0), it.value)
            }
            propertiesAt400[1].let {
                assertEquals("Built-in alpha", it.label)
                assertNotEquals(1f, it.value)
            }
            propertiesAt400[2].let {
                assertEquals("Built-in shrink/expand", it.label)
                assertNotEquals(IntSize(0, 0), it.value)
            }
            // Change start and end state.
            clock.state = AnimatedVisibilityState.Exit
        }
    }

    @Test
    fun clockKeepsSetTime() {
        val clock = setupClock()
        rule.runOnIdle {
            // Set clock to the end of the animation.
            clock.setStateParameters(AnimatedVisibilityState.Enter)
            clock.setClockTime(millisToNanos(380))
        }
        rule.waitForIdle()
        lateinit var previousProperty: List<ComposeAnimatedProperty>
        rule.runOnIdle {
            previousProperty = clock.getAnimatedProperties()
            assertTrue(previousProperty.isNotEmpty())
            // Change state but keep the clock time.
            clock.setStateParameters(AnimatedVisibilityState.Exit)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            val newProperties = clock.getAnimatedProperties()
            // After changing state - same properties but with different values
            assertNotEquals(previousProperty.map { it.value }, newProperties.map { it.value })
            assertEquals(previousProperty.map { it.label }, newProperties.map { it.label })
        }
    }

    private fun setupClock(): AnimatedVisibilityClock {
        lateinit var clock: AnimatedVisibilityClock
        rule.setContent {
            val transition = createTestAnimatedVisibility()
            clock = AnimatedVisibilityClock(transition.parseAnimatedVisibility())
        }
        rule.waitForIdle()
        rule.waitForIdle()
        rule.runOnIdle {
            clock.state = AnimatedVisibilityState.Enter
            clock.setClockTime(0)
        }
        rule.waitForIdle()
        return clock
    }
}
