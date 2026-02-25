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

package androidx.compose.ui.tooling.animation.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.tooling.animation.AnimationSearch
import androidx.compose.ui.tooling.animation.ClockInfo
import androidx.compose.ui.tooling.animation.NoopClockInfo
import androidx.compose.ui.tooling.animation.Utils.addAnimations
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class InfiniteTransitionSearchInfoTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun searchInfoFound() {
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) { rememberInfiniteTransition() }
        assertEquals(1, search.animations.size)

        search.animations.first().let { searchInfo ->
            val animation = searchInfo.createAnimation()
            assertNotNull(animation)
            animation!!
            val clock = searchInfo.createClock(animation, NoopClockInfo)
            assertNotNull(clock)
        }
    }

    @Test
    fun maxDurationIsUsedInClockCreation() {
        // Check what rememberInfiniteTransition's maxDuration matches maxDuration for all existing
        // clocks - that information is provided by ClockInfo.
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) {
            // Iteration duration is 300.
            rememberInfiniteTransition().apply {
                this.animateFloat(1f, 0f, infiniteRepeatable(tween(300), RepeatMode.Restart))
            }
        }

        val searchInfo = search.animations.first()
        val animation = searchInfo.createAnimation()!!
        var duration = 200L
        val clockInfo =
            object : ClockInfo {
                override fun getMaxDurationPerIterationMillis(): Long {
                    return duration
                }

                override fun requestLayout() {}
            }
        val clock = searchInfo.createClock(animation, clockInfo)
        // Both max duration and max duration per iteration are actual duration of the animation.
        assertEquals(300, clock.getMaxDuration())
        assertEquals(300, clock.getMaxDurationPerIteration())

        duration = 450
        // ClockInfo duration is bigger than animation duration - max duration per iteration is
        // still the same, but max duration is updated.
        assertEquals(450, clock.getMaxDuration())
        assertEquals(300, clock.getMaxDurationPerIteration())
    }

    @Test
    fun customLabel() {
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) { rememberInfiniteTransition(label = "customLabel") }

        assertEquals("customLabel", search.animations.first().label)
    }

    @Test
    fun defaultLabel() {
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) { rememberInfiniteTransition() }

        assertEquals("InfiniteTransition", search.animations.first().label)
    }

    @Test
    fun attachAndDetachOverride() {
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) { rememberInfiniteTransition() }
        assertEquals(1, search.animations.size)

        search.animations.first().let { searchInfo ->
            // Default attached values
            assertNotNull(searchInfo.toolingOverride.override.value)
            assertEquals(0L, searchInfo.toolingOverride.state.value)
            assertEquals(0L, searchInfo.toolingOverride.override.value?.value)

            // Detach
            searchInfo.detach()
            assertNull(searchInfo.toolingOverride.override.value)

            // Attach and jump to the end of the animation
            searchInfo.attach()
            searchInfo.toolingOverride.state.value = 300L
            assertNotNull(searchInfo.toolingOverride.override.value)
            assertEquals(300L, searchInfo.toolingOverride.override.value?.value)
        }
    }

    @Test
    fun findInitialAndTargetStates() {
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) { rememberInfiniteTransition() }
        val searchInfo = search.animations.first()
        // rememberInfiniteTransition doesn't have states.
        searchInfo.setInitialStateToCurrentAnimationValue()
        assertNull(searchInfo.initialState)
        searchInfo.setTargetStateToCurrentAnimationValue()
        assertNull(searchInfo.targetState)
    }
}
