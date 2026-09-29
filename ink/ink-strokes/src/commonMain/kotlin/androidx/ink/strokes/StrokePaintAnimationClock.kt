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

package androidx.ink.strokes

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
         * Given a whole-stroke animation duration, calculates the [0, 1) base paint animation phase
         * value for the stroke. This is the paint animation progress value that the stroke would
         * use if the paint animation clock state were zero, such that the stroke is at the start of
         * its animation at the current clock state.
         *
         * If `brushFamilyLoopDurationMillis` is zero, indicating that the stroke's `BrushFamily`
         * contains no animated `BrushPaint`s, then this method returns zero.
         */
        @JvmStatic
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
        public fun calculateBasePaintAnimationPhaseForNewStroke(
            clockStateMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) brushFamilyLoopDurationMillis: Long,
        ): Float =
            if (brushFamilyLoopDurationMillis == 0L) {
                0.0f
            } else {
                (-clockStateMillis).mod(brushFamilyLoopDurationMillis).toFloat() /
                    brushFamilyLoopDurationMillis.toFloat()
            }

        /**
         * Given a stroke's base paint animation phase, and its `BrushFamily`'s paint animation loop
         * duration, and the paint animation loop duration for a particular `BrushPaint` in that
         * family, returns the [0, 1) phase value that `BrushPaint` should have at the given clock
         * state.
         *
         * If `brushPaintLoopDurationMillis` is zero, indicating that the `BrushPaint` is not
         * animated, then this method returns zero. Otherwise, `brushFamilyLoopDurationMillis` must
         * be a multiple of `brushPaintLoopDurationMillis`.
         */
        @JvmStatic
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
        public fun calculateCurrentAnimationPhaseForBrushPaint(
            clockStateMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) brushFamilyLoopDurationMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) brushPaintLoopDurationMillis: Long,
            @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
            strokeBasePaintAnimationPhase: Float,
        ): Float {
            require(brushFamilyLoopDurationMillis >= brushPaintLoopDurationMillis)
            if (brushPaintLoopDurationMillis == 0L) {
                return 0.0f
            }
            require(brushFamilyLoopDurationMillis.mod(brushPaintLoopDurationMillis) == 0L)
            return (clockStateMillis +
                    (strokeBasePaintAnimationPhase * brushFamilyLoopDurationMillis.toDouble())
                        .roundToLong())
                .mod(brushPaintLoopDurationMillis)
                .toFloat() / brushPaintLoopDurationMillis.toFloat()
        }
    }
}
