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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CursorAnimationStateTest {

    private val animationState = CursorAnimationState(true)

    @Test fun alphaNotAnimatingInitially() = runTest { assertNotAnimating() }

    @Test
    fun snapToVisibleAndAnimate_animatesAlpha() = runTest {
        val job = launch { animationState.snapToVisibleAndAnimate() }

        // Should start immediately.
        assertThat(animationState.cursorAlpha).isEqualTo(0f)
        runCurrent()

        // Then let's verify a few blinks…
        assertThat(animationState.cursorAlpha).isEqualTo(1f)
        testScheduler.advanceTimeBy(500)
        assertThat(animationState.cursorAlpha).isEqualTo(1f)
        testScheduler.advanceTimeBy(500)
        assertThat(animationState.cursorAlpha).isEqualTo(0f)
        testScheduler.advanceTimeBy(500)
        assertThat(animationState.cursorAlpha).isEqualTo(1f)

        job.cancel()
    }

    @Test
    fun snapToVisibleAndAnimate_suspendsWhileAnimating() = runTest {
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) { animationState.snapToVisibleAndAnimate() }

        // Advance a few blinks.
        repeat(10) {
            testScheduler.advanceTimeBy(500)
            assertThat(job.isActive).isTrue()
        }

        job.cancel()
    }

    @Test
    fun snapToVisibleAndAnimate_stopsAnimating_whenCancelledImmediately() = runTest {
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) { animationState.snapToVisibleAndAnimate() }
        job.cancel()

        assertNotAnimating()
        assertThat(job.isActive).isFalse()
    }

    @Test
    fun snapToVisibleAndAnimate_stopsAnimating_whenCancelledAsync() = runTest {
        val job = launch { animationState.snapToVisibleAndAnimate() }
        job.cancel()

        assertNotAnimating()
        assertThat(job.isActive).isFalse()
    }

    @Test
    fun snapToVisibleAndAnimate_stopsAnimating_whenCancelledAfterAWhile() = runTest {
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) { animationState.snapToVisibleAndAnimate() }

        // Advance a few blinks…
        repeat(10) { testScheduler.advanceTimeBy(500) }
        job.cancel()

        assertNotAnimating()
    }

    @Test
    fun cancelAndHide_stopsAnimating_immediately() = runTest {
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) { animationState.snapToVisibleAndAnimate() }
        animationState.cancelAndHide()

        assertNotAnimating()
        assertThat(job.isActive).isFalse()
    }

    @Test
    fun cancelAndHide_beforeStart_doesntBlockAnimation() = runTest {
        animationState.cancelAndHide()
        val job = launch { animationState.snapToVisibleAndAnimate() }

        runCurrent()
        assertThat(animationState.cursorAlpha).isEqualTo(1f)

        job.cancel()
    }

    @Test
    fun cancelAndHide_stopsAnimating_afterAWhile() = runTest {
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) { animationState.snapToVisibleAndAnimate() }

        // Advance a few blinks…
        repeat(10) { testScheduler.advanceTimeBy(500) }
        animationState.cancelAndHide()

        assertNotAnimating()
        assertThat(job.isActive).isFalse()
    }

    private fun TestScope.assertNotAnimating() {
        // Allow the cancellation to process.
        advanceUntilIdle()

        // Verify a few blinks.
        repeat(10) {
            assertThat(animationState.cursorAlpha).isEqualTo(0f)
            testScheduler.advanceTimeBy(490)
        }
    }
}
