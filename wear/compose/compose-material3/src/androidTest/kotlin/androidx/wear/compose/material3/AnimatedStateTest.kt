/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimatedStateTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun testAnimatedState_tweeningHalfwayThrough() {
        rule.mainClock.autoAdvance = false
        lateinit var progress: MutableState<Float>
        lateinit var smoothProgress: Animatable<Float, AnimationVector1D>
        rule.setContentWithTheme {
            progress = remember { mutableFloatStateOf(0f) }
            smoothProgress =
                rememberAnimatedStateOf(
                    { progress.value },
                    tween(
                        1024, // Use a multiple of 16 as animations snap to 16ms
                        easing = LinearEasing
                    )
                )
        }
        progress.value = 1f
        // Set the time to 1ms past halfway through the animation
        rule.mainClock.advanceTimeBy(513)
        assertThat(smoothProgress.value).isEqualTo(0.5f)
    }

    @Test
    fun testAnimatedState_changesImmediatelyWithSnapSpec() {
        rule.mainClock.autoAdvance = false
        lateinit var progress: MutableState<Float>
        lateinit var smoothProgress: Animatable<Float, AnimationVector1D>
        rule.setContentWithTheme {
            progress = remember { mutableFloatStateOf(0f) }
            smoothProgress = rememberAnimatedStateOf({ progress.value }, snap())
            LaunchedEffect(Unit) { progress.value = 1f }
        }
        rule.mainClock.advanceTimeBy(1)
        assertThat(smoothProgress.value).isEqualTo(1f)
    }

    @Test
    fun testAnimatedState_interruptsAndRestartsWhenStateChanges() {
        rule.mainClock.autoAdvance = false
        lateinit var progress: MutableState<Float>
        lateinit var smoothProgress: Animatable<Float, AnimationVector1D>
        rule.setContentWithTheme {
            progress = remember { mutableFloatStateOf(0f) }
            smoothProgress =
                rememberAnimatedStateOf(
                    { progress.value },
                    tween(
                        1024, // Use a multiple of 16 as animations snap to 16ms
                        easing = LinearEasing
                    )
                )
        }
        progress.value = 1f
        // Set the time to 1ms past halfway through the animation
        rule.mainClock.advanceTimeBy(513)
        assertThat(smoothProgress.value).isEqualTo(0.5f)
        // Revert back to 0, this should interrupt the animation and restart it
        progress.value = 0f
        // Set the time to 1ms past halfway through the animation, again
        rule.mainClock.advanceTimeBy(513)
        assertThat(smoothProgress.value).isEqualTo(0.25f)
    }
}
