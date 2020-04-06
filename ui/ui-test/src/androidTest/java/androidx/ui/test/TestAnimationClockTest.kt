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
import androidx.compose.State
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.test.espresso.Espresso.onIdle
import androidx.test.filters.MediumTest
import androidx.ui.animation.Transition
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.drawBackground
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.fillMaxSize
import androidx.ui.test.android.ComposeIdlingResource
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

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
    @MediumTest
    fun testAnimation_manuallyAdvanceClock_paused_singleStep() {
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
        assertThat(recordedAnimatedValues).isEqualTo(listOf(0f))

        // Animation doesn't actually start until the next frame.
        // Advance by 0ms to force dispatching of a frame time.
        runOnIdleCompose {
            // Advance clock on main thread so we can assert Compose is not idle afterwards
            clockTestRule.advanceClock(0)
            assertThat(ComposeIdlingResource.isIdle()).isFalse()
        }

        // Await start animation frame
        onIdle()

        // Did start animation frame
        assertThat(recordedAnimatedValues).isEqualTo(listOf(0f, 0f))

        // Advance first half of the animation (.5 sec)
        runOnIdleCompose {
            clockTestRule.advanceClock(500)
            assertThat(ComposeIdlingResource.isIdle()).isFalse()
        }

        // Await next animation frame
        onIdle()

        // Did one animation frame
        assertThat(recordedAnimatedValues).isEqualTo(listOf(0f, 0f, 25f))

        // Advance second half of the animation (.5 sec)
        runOnIdleCompose {
            clockTestRule.advanceClock(500)
            assertThat(ComposeIdlingResource.isIdle()).isFalse()
        }

        // Await next animation frame
        onIdle()

        // Did last animation frame
        assertThat(recordedAnimatedValues).isEqualTo(listOf(0f, 0f, 25f, 50f))
    }

    /**
     * Tests if advancing the clock manually works when the clock is resumed, and that idleness
     * is reported correctly when doing that.
     */
    @Test
    @MediumTest
    @Ignore("b/150357516: not yet implemented")
    fun testAnimation_manuallyAdvanceClock_resumed_singleStep() {
        // TODO(b/150357516): Test advancing the clock while it is resumed
    }

    @Composable
    private fun Ui(animationState: State<AnimationStates>) {
        val paint = remember { Paint().also { it.color = Color.Cyan } }
        val rect = remember { Rect.fromLTWH(0f, 0f, 50f, 50f) }

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
                    drawRect(rect.translate(xValue, 0f), paint)
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
