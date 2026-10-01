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

import androidx.ink.brush.ExperimentalInkAnimationApi
import androidx.kruth.assertThat
import kotlin.test.Test

@OptIn(ExperimentalInkAnimationApi::class)
class StrokePaintAnimationClockTest {
    @Test
    fun calculateBasePaintAnimationPhaseForNewStroke_returnsZeroForNonAnimatedStroke() {
        assertThat(
                StrokePaintAnimationClock.calculateBasePaintAnimationPhaseForNewStroke(
                    clockStateMillis = 12345,
                    brushFamilyLoopDurationMillis = 0,
                )
            )
            .isEqualTo(0.0f)
    }

    @Test
    fun calculateBasePaintAnimationPhaseForNewStroke_returnsExpectedValue() {
        assertThat(
                StrokePaintAnimationClock.calculateBasePaintAnimationPhaseForNewStroke(
                    clockStateMillis = 1250,
                    brushFamilyLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(0.75f)
        assertThat(
                StrokePaintAnimationClock.calculateBasePaintAnimationPhaseForNewStroke(
                    clockStateMillis = 1000,
                    brushFamilyLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(0.0f)
        assertThat(
                StrokePaintAnimationClock.calculateBasePaintAnimationPhaseForNewStroke(
                    clockStateMillis = 2000,
                    brushFamilyLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(0.0f)
        assertThat(
                StrokePaintAnimationClock.calculateBasePaintAnimationPhaseForNewStroke(
                    clockStateMillis = 2500,
                    brushFamilyLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(0.5f)
    }

    @Test
    fun calculateCurrentAnimationPhaseForBrushPaint_returnsZeroForNonAnimatedPaint() {
        assertThat(
                StrokePaintAnimationClock.calculateCurrentAnimationPhaseForBrushPaint(
                    clockStateMillis = 12345,
                    brushFamilyLoopDurationMillis = 1000,
                    brushPaintLoopDurationMillis = 0,
                    strokeBasePaintAnimationPhase = 0.25f,
                )
            )
            .isEqualTo(0.0f)
    }

    @Test
    fun calculateCurrentAnimationPhaseForBrushPaint_returnsExpectedValue() {
        assertThat(
                StrokePaintAnimationClock.calculateCurrentAnimationPhaseForBrushPaint(
                    clockStateMillis = 1500,
                    brushFamilyLoopDurationMillis = 1000,
                    brushPaintLoopDurationMillis = 500,
                    strokeBasePaintAnimationPhase = 0.25f,
                )
            )
            .isEqualTo(0.5f)
        assertThat(
                StrokePaintAnimationClock.calculateCurrentAnimationPhaseForBrushPaint(
                    clockStateMillis = 1500,
                    brushFamilyLoopDurationMillis = 1000,
                    brushPaintLoopDurationMillis = 500,
                    strokeBasePaintAnimationPhase = 0.5f,
                )
            )
            .isEqualTo(0.0f)
        assertThat(
                StrokePaintAnimationClock.calculateCurrentAnimationPhaseForBrushPaint(
                    clockStateMillis = -125,
                    brushFamilyLoopDurationMillis = 1000,
                    brushPaintLoopDurationMillis = 500,
                    strokeBasePaintAnimationPhase = 0.75f,
                )
            )
            .isEqualTo(0.25f)
    }
}
