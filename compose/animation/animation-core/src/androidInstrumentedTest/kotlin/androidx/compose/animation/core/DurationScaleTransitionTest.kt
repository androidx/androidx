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
package androidx.compose.animation.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.junit.Test

@SmallTest
class DurationScaleTransitionTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalTransitionApi::class)
    @Test
    fun childTransitionWithDurationScale() {
        val motionDurationScale =
            object : MotionDurationScale {
                override val scaleFactor: Float
                    get() = 4f
            }
        runComposeUiTest(effectContext = motionDurationScale) {
            mainClock.autoAdvance = false
            val state = MutableTransitionState(0)
            var value1 = -1f
            var value2 = -1f
            var withChild by mutableStateOf(false)
            setContent {
                val transition = rememberTransition(transitionState = state)
                val animatedValue1 by
                    transition.animateFloat(
                        { tween(160, easing = LinearEasing) },
                    ) {
                        if (it == 0) 0f else 1000f
                    }
                value1 = animatedValue1
                if (withChild) {
                    val child = transition.createChildTransition { it }
                    val animatedValue2 by
                        child.animateFloat(
                            { tween(160, easing = LinearEasing) },
                        ) {
                            if (it == 0) 0f else 1000f
                        }
                    value2 = animatedValue2
                }
            }
            mainClock.advanceTimeByFrame() // let everything settle
            state.targetState = 1
            mainClock.advanceTimeByFrame() // recompose
            mainClock.advanceTimeByFrame() // lock in animation clock
            assertThat(value1).isEqualTo(0f)
            assertThat(value2).isEqualTo(-1f) // not set until withChild = true

            mainClock.advanceTimeBy(320) // half way through transition
            assertThat(value1).isWithin(0.1f).of(500f)
            assertThat(value2).isEqualTo(-1f) // not set until withChild = true

            withChild = true
            mainClock.advanceTimeByFrame() // recomposed after withChild changed
            assertThat(value2).isEqualTo(0f)

            // Now the transition will progress from here with value2 taking 1000ms more
            mainClock.advanceTimeBy(320)
            assertThat(value1).isEqualTo(1000f)
            assertThat(value2).isWithin(0.1f).of(500f)
            mainClock.advanceTimeBy(320)
            assertThat(value2).isEqualTo(1000f)
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalTransitionApi::class)
    @Test
    fun childTransitionWithDurationScaleSeekableTransition() {
        val motionDurationScale =
            object : MotionDurationScale {
                override val scaleFactor: Float
                    get() = 4f
            }
        runComposeUiTest(effectContext = motionDurationScale) {
            mainClock.autoAdvance = false
            val state = SeekableTransitionState(0)
            var value1 = -1f
            var value2 = -1f
            var withChild by mutableStateOf(false)
            lateinit var coroutineScope: CoroutineScope
            setContent {
                coroutineScope = rememberCoroutineScope()
                val transition = rememberTransition(transitionState = state)
                val animatedValue1 by
                    transition.animateFloat(
                        { tween(160, easing = LinearEasing) },
                    ) {
                        if (it == 0) 0f else 1000f
                    }
                value1 = animatedValue1
                if (withChild) {
                    val child = transition.createChildTransition { it }
                    val animatedValue2 by
                        child.animateFloat(
                            { tween(160, easing = LinearEasing) },
                        ) {
                            if (it == 0) 0f else 1000f
                        }
                    value2 = animatedValue2
                }
            }
            mainClock.advanceTimeByFrame() // let everything settle
            val seekTo = runOnUiThread {
                coroutineScope.async { state.seekTo(fraction = 0f, targetState = 1) }
            }
            mainClock.advanceTimeByFrame() // recompose
            assertThat(seekTo.isCompleted).isTrue()

            assertThat(value1).isEqualTo(0f)
            assertThat(value2).isEqualTo(-1f) // not set until withChild = true

            runOnUiThread { coroutineScope.launch { state.animateTo(targetState = 1) } }
            mainClock.advanceTimeByFrame() // lock in the animation clock
            mainClock.advanceTimeBy(320) // half way through transition
            assertThat(value1).isWithin(0.1f).of(500f)
            assertThat(value2).isEqualTo(-1f) // not set until withChild = true

            withChild = true
            mainClock.advanceTimeByFrame() // recomposed after withChild changed
            assertThat(value2).isEqualTo(0f)

            // Now the transition will progress from here with value2 taking 1000ms more
            mainClock.advanceTimeBy(320)
            assertThat(value1).isEqualTo(1000f)
            assertThat(value2).isWithin(0.1f).of(500f)
            mainClock.advanceTimeBy(320)
            assertThat(value2).isEqualTo(1000f)
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalTransitionApi::class)
    @Test
    fun childTransitionWithDurationScaleSeekTransition() {
        val motionDurationScale =
            object : MotionDurationScale {
                override val scaleFactor: Float
                    get() = 4f
            }
        runComposeUiTest(effectContext = motionDurationScale) {
            mainClock.autoAdvance = false
            val state = SeekableTransitionState(0)
            var value1 = -1f
            var value2 = -1f
            var withChild by mutableStateOf(false)
            lateinit var coroutineScope: CoroutineScope
            setContent {
                coroutineScope = rememberCoroutineScope()
                val transition = rememberTransition(transitionState = state)
                val animatedValue1 by
                    transition.animateFloat(
                        { tween(160, easing = LinearEasing) },
                    ) {
                        if (it == 0) 0f else 1000f
                    }
                value1 = animatedValue1
                if (withChild) {
                    val child = transition.createChildTransition { it }
                    val animatedValue2 by
                        child.animateFloat(
                            { tween(160, easing = LinearEasing) },
                        ) {
                            if (it == 0) 0f else 1000f
                        }
                    value2 = animatedValue2
                }
            }
            mainClock.advanceTimeByFrame() // let everything settle
            val seekTo = runOnUiThread {
                coroutineScope.async { state.seekTo(fraction = 0.5f, targetState = 1) }
            }
            mainClock.advanceTimeByFrame() // recompose
            assertThat(seekTo.isCompleted).isTrue()

            assertThat(value1).isEqualTo(500f)
            assertThat(value2).isEqualTo(-1f) // not set until withChild = true

            withChild = true
            mainClock.advanceTimeByFrame() // recomposed after withChild changed
            mainClock.advanceTimeByFrame() // allow seekToFrame() to run after total duration change

            // Now we're 50% of the way to 1500ms = 750ms
            assertThat(value1).isWithin(0.1f).of(750f)
            assertThat(value2).isWithin(0.1f).of(250f)

            runOnUiThread {
                coroutineScope.launch {
                    state.seekTo(fraction = 0.75f) // 1125ms
                }
            }
            mainClock.advanceTimeByFrame()

            assertThat(value1).isEqualTo(1000f)
            assertThat(value2).isWithin(0.1f).of(625f)

            runOnUiThread { coroutineScope.launch { state.seekTo(fraction = 1f) } }
            mainClock.advanceTimeByFrame()

            assertThat(value1).isEqualTo(1000f)
            assertThat(value2).isEqualTo(1000f)
        }
    }
}
