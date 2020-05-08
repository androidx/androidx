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

package androidx.ui.test

import androidx.animation.FloatPropKey
import androidx.animation.LinearEasing
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.State
import androidx.compose.mutableStateOf
import androidx.test.espresso.Espresso.onIdle
import androidx.test.filters.MediumTest
import androidx.ui.animation.Transition
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.drawBackground
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize
import androidx.ui.test.android.ComposeIdlingResource
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
class TestAnimationClockTest {

    private var animationRunning = false
    private val recordedAnimatedValues = mutableListOf<Float>()
    private var hasRecomposed = false

    @get:Rule
    val composeTestRule = createComposeRule()
    private val clockTestRule = composeTestRule.clockTestRule

    /**
     * Tests if advancing the clock manually works when the clock is paused, and that idleness is
     * reported correctly when doing that.
     */
    @Test
    fun testAnimation_manuallyAdvanceClock_paused() {
        clockTestRule.pauseClock()

        val animationState = mutableStateOf(AnimationStates.From)
        composeTestRule.setContent { Ui(animationState) }

        runOnIdleCompose {
            recordedAnimatedValues.clear()

            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To

            // Changes need to trickle down the animation system, so compose should be non-idle
            assertThat(ComposeIdlingResource.isIdle()).isFalse()
        }

        // Await recomposition
        onIdle()

        // Did one recomposition, but no animation frames
        recordedAnimatedValues.forEach {
            assertThat(it).isIn(listOf(0f))
        }

        // Advance first half of the animation (.5 sec)
        runOnIdleCompose {
            clockTestRule.advanceClock(500)
            assertThat(ComposeIdlingResource.isIdle()).isFalse()
        }

        // Await next animation frame
        onIdle()

        // Did one animation frame
        recordedAnimatedValues.forEach {
            assertThat(it).isIn(listOf(0f, 25f))
        }

        // Advance second half of the animation (.5 sec)
        runOnIdleCompose {
            clockTestRule.advanceClock(500)
            assertThat(ComposeIdlingResource.isIdle()).isFalse()
        }

        // Await next animation frame
        onIdle()

        // Did last animation frame
        assertThat(animationRunning).isFalse()
        recordedAnimatedValues.forEach {
            assertThat(it).isIn(listOf(0f, 25f, 50f))
        }
    }

    /**
     * Tests if advancing the clock manually works when the clock is resumed, and that idleness
     * is reported correctly when doing that.
     */
    @Test
    fun testAnimation_manuallyAdvanceClock_resumed() {
        val animationState = mutableStateOf(AnimationStates.From)
        composeTestRule.setContent { Ui(animationState) }

        // Before we kick off the animation, the test clock should be idle
        assertThat(clockTestRule.clock.isIdle).isTrue()

        runOnIdleCompose {
            recordedAnimatedValues.clear()

            // Kick off the animation
            animationRunning = true
            animationState.value = AnimationStates.To

            // Changes need to trickle down the animation system, so compose should be non-idle
            assertThat(ComposeIdlingResource.isIdle()).isFalse()

            // Force model changes down the pipeline
            FrameManager.nextFrame()
            Recomposer.current().recomposeSync()

            // After we kicked off the animation, the test clock should be non-idle
            assertThat(clockTestRule.clock.isIdle).isFalse()
        }

        // Animation is running, fast-forward it because we don't want to wait on it
        clockTestRule.advanceClock(1000)

        // Force the clock forwarding through the pipeline
        // Avoid synchronization steps when doing this: if we would synchronize, we would never
        // know it if advanceClock didn't work.
        runOnUiThread {
            FrameManager.nextFrame()
            Recomposer.current().recomposeSync()
        }

        // After the animation is finished, ...
        assertThat(animationRunning).isFalse()
        // ... the test clock should be idle again
        assertThat(clockTestRule.clock.isIdle).isTrue()

        // Animation values are recorded in draw, so wait until we've drawn
        onIdle()

        // So the clock has now been pumped both by the test and the Choreographer, so there is
        // really no way to tell in which states the animation has been rendered. Only that we
        // rendered the final state.
        assertThat(recordedAnimatedValues).contains(50f)
    }

    @Composable
    private fun Ui(animationState: State<AnimationStates>) {
        val size = Size(50.0f, 50.0f)
        hasRecomposed = true
        Box(modifier = Modifier.drawBackground(Color.Yellow).fillMaxSize()) {
            hasRecomposed = true
            Transition(
                definition = animationDefinition,
                toState = animationState.value,
                onStateChangeFinished = { animationRunning = false }
            ) { state ->
                hasRecomposed = true
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val xValue = state[x]
                    recordedAnimatedValues.add(xValue)
                    drawRect(Color.Cyan, Offset(xValue, 0.0f), size)
                }
            }
        }
    }

    private val x = FloatPropKey()

    private enum class AnimationStates {
        From,
        To
    }

    private val animationDefinition = transitionDefinition {
        state(AnimationStates.From) {
            this[x] = 0f
        }
        state(AnimationStates.To) {
            this[x] = 50f
        }
        transition(AnimationStates.From to AnimationStates.To) {
            x using tween {
                easing = LinearEasing
                duration = 1000L.toInt()
            }
        }
        transition(AnimationStates.To to AnimationStates.From) {
            x using snap()
        }
    }
}
