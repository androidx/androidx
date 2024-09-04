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

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyframeAnimationTest {

    @Test
    fun equalsStartAndEndValues() {
        val start = 0f
        val end = start // the same
        val fullTime = 400
        val animation =
            keyframes {
                    durationMillis = fullTime
                    start at 100
                    0.5f at 200
                    0.8f at 300
                    end at fullTime
                }
                .vectorize(Float.VectorConverter)

        assertThat(animation.at(0)).isEqualTo(start)
        assertThat(animation.at(250)).isEqualTo(0.65f)
        assertThat(animation.at(fullTime.toLong())).isEqualTo(end)
    }

    @Test
    fun possibleToOverrideStartAndEndValues() {
        val fullTime = 100
        val animation =
            keyframes {
                    durationMillis = fullTime
                    1f at 0
                    0f at fullTime
                }
                .vectorize(Float.VectorConverter)

        assertThat(animation.at(0)).isEqualTo(1f)
        assertThat(animation.at(fullTime.toLong())).isEqualTo(0f)
    }

    @Test
    fun withEasingOnFullDuration() {
        val easing = FastOutSlowInEasing
        val animation =
            keyframes {
                    durationMillis = 100
                    0f at 0 using easing
                    1f at durationMillis
                }
                .vectorize(Float.VectorConverter)

        assertThat(animation.at(31)).isEqualTo(easing.transform(0.31f))
    }

    @Test
    fun easingOnTheSecondPart() {
        val easing = FastOutSlowInEasing
        val animation =
            keyframes {
                    durationMillis = 200
                    1f at 100 using easing
                    2f at durationMillis
                }
                .vectorize(Float.VectorConverter)

        assertThat(animation.at(140)).isEqualTo(1f + easing.transform(0.4f))
    }

    @Test
    fun firstPartIsLinearWithEasingOnTheSecondPart() {
        val animation =
            keyframes {
                    durationMillis = 100
                    0.5f at 50 using FastOutSlowInEasing
                    1f at durationMillis
                }
                .vectorize(Float.VectorConverter)

        assertThat(animation.at(25)).isEqualTo(0.25f)
    }

    @Test
    fun testMultiDimensKeyframesWithEasing() {
        val easing = FastOutLinearInEasing
        val animation =
            keyframes {
                    durationMillis = 400
                    AnimationVector(200f, 300f) at 200 using easing
                }
                .vectorize(TwoWayConverter({ it }, { it }))

        val start = AnimationVector(0f, 0f)
        val end = AnimationVector(200f, 400f)

        for (time in 0..400 step 50) {
            val v1: Float
            val v2: Float
            if (time < 200) {
                v1 = lerp(0f, 200f, time / 200f)
                v2 = lerp(0f, 300f, time / 200f)
            } else {
                v1 = 200f
                v2 = lerp(300f, 400f, easing.transform((time - 200) / 200f))
            }
            assertEquals(
                AnimationVector(v1, v2),
                animation.getValueFromMillis(time.toLong(), start, end, AnimationVector(0f, 0f))
            )
        }
    }

    @Test
    fun testNotEquals0() {
        val config: KeyframesSpec.KeyframesSpecConfig<Float>.() -> Unit = {
            durationMillis = 500
            0f at 100
            0.5f at 200 using FastOutLinearInEasing
            0.8f at 300
            1f at durationMillis
        }

        val animation = keyframes(config)

        val animationReuseConfig = keyframes(config)

        val animationRedeclareConfig = keyframes {
            durationMillis = 500
            0f at 100
            0.5f at 200 using FastOutLinearInEasing
            0.8f at 300
            1f at durationMillis
        }

        assertTrue(animation != animationReuseConfig)
        assertTrue(animation != animationRedeclareConfig)
        assertTrue(animationReuseConfig != animationRedeclareConfig)
    }

    @Test
    fun testNotEquals1() {
        val animation = keyframes {
            durationMillis = 500
            0f at 100
            0.5f at 200 using FastOutLinearInEasing
            0.8f at 300
            1f at durationMillis
        }

        val animationAlteredDuration = keyframes {
            durationMillis = 700
            0f at 100
            0.5f at 200 using FastOutLinearInEasing
            0.8f at 300
            1f at durationMillis
        }

        val animationAlteredEasing = keyframes {
            durationMillis = 500
            0f at 100 using FastOutSlowInEasing
            0.5f at 200
            0.8f at 300
            1f at durationMillis
        }

        val animationAlteredKeyframes = keyframes {
            durationMillis = 500
            0f at 100
            0.3f at 200 using FastOutLinearInEasing
            0.8f at 400
            1f at durationMillis
        }

        assertTrue(animation != animationAlteredDuration)
        assertTrue(animation != animationAlteredEasing)
        assertTrue(animation != animationAlteredKeyframes)
    }

    @Test
    fun percentageBasedKeyFrames() {
        val start = 0f
        val end = start // the same
        val fullTime = 400
        val animation =
            keyframes {
                    durationMillis = fullTime
                    start atFraction 0.25f
                    0.5f atFraction 0.5f
                    0.8f atFraction 0.75f
                    end atFraction 1f
                }
                .vectorize(Float.VectorConverter)

        assertThat(animation.at(0)).isEqualTo(start)
        assertThat(animation.at(250)).isEqualTo(0.65f)
        assertThat(animation.at(fullTime.toLong())).isEqualTo(end)
    }

    @Test
    fun percentageBasedKeyframesWithEasing() {
        val animation =
            keyframes {
                    durationMillis = 100
                    0.5f atFraction 0.5f using FastOutSlowInEasing
                    1f atFraction 1f
                }
                .vectorize(Float.VectorConverter)

        assertThat(animation.at(25)).isEqualTo(0.25f)
    }

    @Test
    fun outOfRangeValuesOnly() {
        val duration = 100
        val delay = 200

        // Out of range values should be effectively ignored.
        // It should interpolate within the expected time range without issues
        val animation =
            keyframes {
                    durationMillis = duration
                    delayMillis = delay

                    (-1f) at -delay using LinearEasing
                    (-2f) at -duration using LinearEasing
                    (-3f) at (duration + 50) using LinearEasing
                }
                .vectorize(Float.VectorConverter)

        // Within delay, should always return initial value unless it was overwritten
        assertThat(animation.at(0)).isEqualTo(0f)
        assertThat(animation.at(100)).isEqualTo(0f)
        assertThat(animation.at(200)).isEqualTo(0f)

        // Within time range
        assertThat(animation.at(delay)).isEqualTo(0f)
        assertThat(animation.at((duration / 2) + delay)).isEqualTo(0.5f)
        assertThat(animation.at(duration + delay)).isEqualTo(1f)

        // Out of range - past animation duration
        // Should always be the target value unless it was overwritten
        assertThat(animation.at(delay + duration + 1)).isEqualTo(1f)
        assertThat(animation.at(delay + duration + 50)).isEqualTo(1f)
    }

    @Test
    fun outOfRangeValues_withForcedInitialAndTarget() {
        val duration = 100
        val delay = 200

        // Out of range values should be effectively ignored.
        // It should interpolate within the expected time range without issues
        val animation =
            keyframes {
                    durationMillis = duration
                    delayMillis = delay

                    (-1f) at -delay using LinearEasing
                    (-2f) at -duration using LinearEasing
                    (-3f) at (duration + 50) using LinearEasing

                    // Force initial and target
                    4f at 0 using LinearEasing
                    5f at duration using LinearEasing
                }
                .vectorize(Float.VectorConverter)

        // Within delay, should always return initial value unless it was overwritten
        assertThat(animation.at(0)).isEqualTo(4f)
        assertThat(animation.at(100)).isEqualTo(4f)
        assertThat(animation.at(200)).isEqualTo(4f)

        // Within time range
        assertThat(animation.at(delay)).isEqualTo(4f)
        assertThat(animation.at((duration / 2) + delay)).isEqualTo(4.5f)
        assertThat(animation.at(duration + delay)).isEqualTo(5f)

        // Out of range - past animation duration
        // Should always be the target value unless it was overwritten
        assertThat(animation.at(delay + duration + 1)).isEqualTo(5f)
        assertThat(animation.at(delay + duration + 50)).isEqualTo(5f)
    }
}
