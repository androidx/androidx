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

package androidx.ink.rendering.android.canvas

import androidx.annotation.AnyThread
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.ink.brush.ExperimentalInkAnimationApi
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.roundToLong

/**
 * Controls animated paint textures for rendered strokes. Typically a single
 * [StrokePaintAnimationClock] object is used for all strokes in a document.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkAnimationApi
public fun interface StrokePaintAnimationClock {

    /**
     * Returns the number of subjective milliseconds that have elapsed since this clock's zero
     * state. Depending on the implementation, this value may update only once per frame, and it may
     * change at a different speed than real time.
     *
     * This method must be safe to call from any thread.
     */
    @AnyThread public fun getClockStateMillis(): Long

    @ExperimentalInkAnimationApi
    public companion object {
        /** An animation clock whose clock state never changes. */
        @JvmField
        public val STOPPED_CLOCK: StrokePaintAnimationClock = StrokePaintAnimationClock { 0L }

        /**
         * Given a whole-stroke animation duration, calculates the 0-2 base phase value for the
         * stroke. This is the animation progress value that the stroke should appear at for the
         * animator's zero clock state, such that the stroke would be at the start of its animation
         * at the animator's current clock state.
         *
         * If `animationLoopDurationMillis` is zero, indicating that the stroke is not animated,
         * then this method returns zero.
         */
        @JvmStatic
        @FloatRange(from = 0.0, to = 2.0, toInclusive = false)
        public fun calculateBasePhaseForNewStroke(
            clockStateMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) animationLoopDurationMillis: Long,
        ): Float =
            if (animationLoopDurationMillis == 0L) {
                0.0f
            } else {
                (-clockStateMillis).mod(2 * animationLoopDurationMillis).toFloat() /
                    animationLoopDurationMillis.toFloat()
            }

        /**
         * Given a stroke's base phase and its whole-stroke animation duration, and the animation
         * duration for a particular brush paint in that stroke, returns the 0-2 phase value the
         * paint should have at the animator's current clock state.
         *
         * If `paintAnimationLoopDurationMillis` is zero, indicating that the paint is not animated,
         * then this method returns zero. Otherwise, `strokeAnimationLoopDurationMillis` must be a
         * multiple of `paintAnimationLoopDurationMillis`.
         */
        @JvmStatic
        @FloatRange(from = 0.0, to = 2.0, toInclusive = false)
        public fun calculateCurrentPhaseForPaint(
            clockStateMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) strokeAnimationLoopDurationMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) paintAnimationLoopDurationMillis: Long,
            @FloatRange(from = 0.0, to = 2.0, toInclusive = false) strokeBasePhase: Float,
        ): Float {
            require(strokeAnimationLoopDurationMillis >= paintAnimationLoopDurationMillis)
            if (paintAnimationLoopDurationMillis == 0L) {
                return 0.0f
            }
            require(strokeAnimationLoopDurationMillis.mod(paintAnimationLoopDurationMillis) == 0L)
            return (clockStateMillis +
                    (strokeBasePhase * strokeAnimationLoopDurationMillis.toDouble()).roundToLong())
                .mod(2 * paintAnimationLoopDurationMillis)
                .toFloat() / paintAnimationLoopDurationMillis.toFloat()
        }
    }
}
