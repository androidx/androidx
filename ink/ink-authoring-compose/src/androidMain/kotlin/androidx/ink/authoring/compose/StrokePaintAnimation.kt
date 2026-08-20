/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.authoring.compose

import androidx.annotation.RestrictTo
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.ink.brush.ExperimentalInkAnimationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Returns a [State] representing the current state of a stroke paint animation clock. Normally,
 * this clock state will advance each frame, and can be used to drive animated stroke paint
 * textures. A single clock state should be use to drive animations for both wet and dry strokes, to
 * ensure the animation does not stutter during wet-to-dry handoff.
 *
 * The returned [State] will [remember] its clock state across recompositions, so that animations
 * can continue uninterrupted.
 *
 * @param isPaused Whether or not the clock is paused. When paused, the clock state will not change,
 *   and animations will not advance.
 * @param speedMultiplier How quickly the clock state should advance relative to real time. Values
 *   greater than one will make animations advance faster; values between zero and one will make
 *   animations advance slower; values less than zero will make animations go in reverse. A value of
 *   zero pauses the clock.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkAnimationApi
public fun strokePaintAnimatorClockStateMillisAsState(
    isPaused: Boolean = false,
    speedMultiplier: Float = 1.0f,
): State<Long> {
    require(speedMultiplier.isFinite()) { "`speedMultiplier` must be finite; got $speedMultiplier" }
    val animationState = remember { StrokePaintAnimationStateImpl() }
    LaunchedEffect(isPaused, speedMultiplier) {
        if (isPaused || speedMultiplier == 0.0f) {
            return@LaunchedEffect
        }
        animationState.animate(speedMultiplier)
    }
    return animationState
}

private class StrokePaintAnimationStateImpl() : State<Long> {
    private val mutex = MutatorMutex()

    private var prevFrameTimeNanos: Long by mutableLongStateOf(0L)
    private var clockStateNanos: Long by mutableLongStateOf(0L)

    override val value: Long
        get() = clockStateNanos / NANOS_PER_MILLI

    internal suspend fun animate(speedMultiplier: Float) {
        mutex.mutate {
            withContext(Dispatchers.Main.immediate) {
                // withFrameNanos suspends until the next frame is being prepared, providing the
                // timestamp
                // of that frame.
                prevFrameTimeNanos = withFrameNanos { it }
                while (true) {
                    val nextFrameTimeNanos = withFrameNanos { it }
                    val nanosSinceLastUpdate = nextFrameTimeNanos - prevFrameTimeNanos
                    prevFrameTimeNanos = nextFrameTimeNanos
                    clockStateNanos += (nanosSinceLastUpdate.toDouble() * speedMultiplier).toLong()
                }
            }
        }
    }
}

// The number of nanoseconds in one millisecond.
private const val NANOS_PER_MILLI: Long = 1_000_000L
