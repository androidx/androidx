/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CursorAnimationDurationTest {

    @Test
    fun whenNeverBlink_cursorAlwaysOn() = runTest {
        val subject = CursorAnimationState(false)
        val job = launch { subject.snapToVisibleAndAnimate() }
        assertNotAnimating(subject, 1f)
        job.cancel()
    }

    @Test
    fun default_blinksForever() = runTest {
        val subject = CursorAnimationState(true)
        val job = launch { subject.snapToVisibleAndAnimate() }
        // note, don't approach large portions of Long.MAX_VALUE advance here as it has to "blink""
        // every 500 and if it runs longer than 10s the coroutine framework faults
        advanceTimeBy(500 * 10_000) // 10 thousand blinks
        val cur = subject.cursorAlpha
        advanceTimeBy(500)
        assertThat(cur).isNotEqualTo(subject.cursorAlpha)
        job.cancel()
    }

    private fun TestScope.assertNotAnimating(animationState: CursorAnimationState, alpha: Float) {
        // Allow the cancellation to process.
        advanceUntilIdle()

        // Verify a few blinks.
        repeat(10) {
            assertThat(animationState.cursorAlpha).isEqualTo(alpha)
            testScheduler.advanceTimeBy(490)
        }
    }
}
