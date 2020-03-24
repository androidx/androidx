/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.animation

import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RepeatableAnimationTest {

    private val Animation = TweenBuilder<Float>().apply {
        duration = Duration
    }

    private val DelayedAnimation = TweenBuilder<Float>().apply {
        delay = DelayDuration
        duration = Duration
    }

    @Test
    fun twoRepeatsValuesCalculation() {
        val repeat = RepeatableBuilder<Float>().run {
            iterations = 2
            animation = Animation
            build()
        }
        val animationWrapper = TargetBasedAnimationWrapper(
            0f,
            AnimationVector(0f),
            0f,
            repeat,
            FloatToVectorConverter
        )

        assertThat(repeat.at(0)).isEqualTo(0f)
        assertThat(repeat.at(Duration - 1)).isGreaterThan(0.9f)
        assertThat(repeat.at(Duration + 1)).isLessThan(0.1f)
        assertThat(repeat.at(Duration * 2 - 1)).isGreaterThan(0.9f)
        assertThat(repeat.at(Duration * 2)).isEqualTo(1f)
        assertThat(animationWrapper.isFinished(Duration * 2L - 1L)).isFalse()
        assertThat(animationWrapper.isFinished(Duration * 2L)).isTrue()
    }

    @Test
    fun testRepeatedAnimationDuration() {
        val iters = 5
        val repeat = RepeatableBuilder<Float>().apply {
            iterations = iters
            animation = DelayedAnimation
        }.build()

        val duration = repeat.getDurationMillis(
            AnimationVector1D(0f),
            AnimationVector1D(0f),
            AnimationVector1D(0f)
        )

        assertEquals((DelayDuration + Duration) * iters.toLong(), duration)
    }

    private companion object {
        private val DelayDuration = 13
        private val Duration = 50
    }
}