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

package androidx.compose.animation.core

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class DeferredTargetAnimationTest {

    @Test
    fun testDeferredTargetAnimationValue() = runTest {
        val clock = TestFrameClock()
        val scheduler = testScheduler
        withContext(clock) {
            val animation = DeferredTargetAnimation(Offset.VectorConverter)

            // Verify initial value is null
            assertThat(animation.value).isNull()

            // Initialize to 10f, 20f
            val initialVal = animation.updateTarget(Offset(10f, 20f), this)
            assertThat(initialVal).isEqualTo(Offset(10f, 20f))
            assertThat(animation.value).isEqualTo(Offset(10f, 20f))
            scheduler.runCurrent()

            // Update to a new target
            animation.updateTarget(Offset(100f, 200f), this)
            scheduler.runCurrent()

            // Value shouldn't change yet because no frame has ticked
            assertThat(animation.value).isEqualTo(Offset(10f, 20f))

            // Tick a frame (first frame initializes startTimeNanos)
            var timeNanos = 0L
            clock.frame(timeNanos)
            scheduler.runCurrent()
            assertThat(animation.value).isEqualTo(Offset(10f, 20f))

            // Tick another frame (second frame computes new value based on time elapsed)
            timeNanos += 16_000_000L
            clock.frame(timeNanos)
            scheduler.runCurrent()
            // It should start animating, so it should be between 10f and 100f, etc.
            assertThat(animation.value).isNotEqualTo(Offset(10f, 20f))
            assertThat(animation.value).isNotEqualTo(Offset(100f, 200f))

            // Tick enough frames to finish animation
            repeat(100) {
                timeNanos += 16_000_000L
                clock.frame(timeNanos)
                scheduler.runCurrent()
            }
            assertThat(animation.value).isEqualTo(Offset(100f, 200f))
        }
    }

    private class TestFrameClock : MonotonicFrameClock {
        private val frameCh = Channel<Long>(Channel.UNLIMITED)

        suspend fun frame(frameTimeNanos: Long) {
            frameCh.send(frameTimeNanos)
        }

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frameCh.receive())
    }
}
