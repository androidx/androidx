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

import androidx.compose.foundation.AtomicReference
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Holds the state of the animation that blinks the cursor.
 *
 * We can't use the Compose Animation APIs because they busy-loop on delays, and this animation
 * spends most of its time delayed so that's a ton of wasted frames. Pure coroutine delays, however,
 * will not cause any work to be done until the delay is over.
 */
internal class CursorAnimationState {

    private var animationJob = AtomicReference<Job?>(null)

    /**
     * The alpha value that should be used to draw the cursor.
     * Will always be in the range [0, 1].
     */
    var cursorAlpha by mutableFloatStateOf(0f)
        private set

    /**
     * Immediately shows the cursor (sets [cursorAlpha] to 1f) and starts blinking it on and off
     * every 500ms. If a previous animation was running, it will be cancelled before the new one
     * starts.
     *
     * Won't return until the animation cancelled via [cancelAndHide] or this coroutine's [Job] is
     * cancelled. In both cases, the cursor will always end up hidden.
     */
    suspend fun snapToVisibleAndAnimate() = runCursorAnimation {
        coroutineScope {
            // Can't do a single atomic update because we need to get the old value before launching
            // the new coroutine. So we set to null first, and then launch only if still null (i.e.
            // no other caller beat us to starting a new animation).
            val oldJob = animationJob.getAndSet(null)

            // Even though we're launching a new coroutine, because of structured concurrency, the
            // restart function won't return until the animation is finished, and cancelling the
            // calling coroutine will cancel the animation.
            animationJob.compareAndSet(null, launch {
                // Join the old job after cancelling to ensure it finishes its finally block before
                // we start changing the cursor alpha, so we don't end up interleaving alpha
                // updates.
                oldJob?.cancelAndJoin()

                // Start the new animation and run until cancelled.
                try {
                    while (true) {
                        cursorAlpha = 1f
                        // Ignore MotionDurationScale – the cursor should blink even when animations
                        // are disabled by the system.
                        delay(500)
                        cursorAlpha = 0f
                        delay(500)
                    }
                } finally {
                    // Hide cursor when the animation is cancelled.
                    cursorAlpha = 0f
                }
            })
        }
    }

    /**
     * Immediately cancels the cursor animation and hides the cursor (sets [cursorAlpha] to 0f).
     */
    fun cancelAndHide() {
        val job = animationJob.getAndSet(null)
        job?.cancel()
    }
}

/**
 * Runs the infinite animation in [block], taking into account the current
 * [InfiniteAnimationPolicy].
 *
 * This is needed to allow the text field cursor blinking to be cancelled by
 * [InfiniteAnimationPolicy] as if it was an animation. Otherwise `waitForIdle` in tests with a
 * focused text field will never return. Note that on Android this isn't needed because there
 * `waitForIdle` appears to completely ignore delayed tasks (see
 * https://issuetracker.google.com/issues/324768454 for details).
 */
private suspend fun runCursorAnimation(block: suspend () -> Unit) =
    when (val policy = coroutineContext[InfiniteAnimationPolicy]) {
        null -> block()
        else -> policy.onInfiniteOperation { block() }
    }
