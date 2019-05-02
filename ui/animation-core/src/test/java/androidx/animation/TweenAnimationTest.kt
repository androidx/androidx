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

import androidx.ui.lerp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TweenAnimationTest {

    @Test
    fun delayCorrectness() {
        val testDelay = 100L
        val testDuration = 200
        val start = 0f
        val end = 1000f

        val animation = TweenBuilder<Float>().run {
            delay = 100
            duration = testDuration
            easing = LinearEasing
            build()
        }

        fun atPlaytime(playTime: Long) =
            animation.getValue(playTime, start, end, 0f, ::lerp)

        assertThat(atPlaytime(0L)).isZero()
        assertThat(atPlaytime(testDelay / 2)).isZero()
        assertThat(atPlaytime(testDelay)).isZero()
        assertThat(atPlaytime(testDelay + 1)).isNonZero()
    }

    @Test
    fun easingIsApplied() {
        val totalDuration = 300
        val accelerateEasing: Easing = { fraction -> fraction * 2f }
        val animation = TweenBuilder<Float>().run {
            duration = totalDuration
            easing = accelerateEasing
            build()
        }

        val fraction = 0.3f
        val value = animation.at((totalDuration * fraction).toInt())
        val expectedValue = accelerateEasing(fraction)
        assertThat(value).isEqualTo(expectedValue)
    }

    @Test
    fun endValueCalculatedForPlaytimeOverDuration() {
        val testDuration = 200

        val animation = TweenBuilder<Float>().run {
            duration = testDuration
            build()
        }

        assertThat(animation.at(testDuration + 10)).isEqualTo(1f)
    }
}
