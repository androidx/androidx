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

import androidx.ink.brush.ExperimentalInkAnimationApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalInkAnimationApi::class)
class StrokePaintAnimationClockTest {
    @Test
    fun calculateBasePhaseForNewStroke_returnsZeroForNonAnimatedStroke() {
        assertThat(
                StrokePaintAnimationClock.calculateBasePhaseForNewStroke(
                    clockStateMillis = 12345,
                    animationLoopDurationMillis = 0,
                )
            )
            .isEqualTo(0.0f)
    }

    @Test
    fun calculateBasePhaseForNewStroke_returnsExpectedValue() {
        assertThat(
                StrokePaintAnimationClock.calculateBasePhaseForNewStroke(
                    clockStateMillis = 1250,
                    animationLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(0.75f)
        assertThat(
                StrokePaintAnimationClock.calculateBasePhaseForNewStroke(
                    clockStateMillis = 1000,
                    animationLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(1.0f)
        assertThat(
                StrokePaintAnimationClock.calculateBasePhaseForNewStroke(
                    clockStateMillis = 2000,
                    animationLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(0.0f)
        assertThat(
                StrokePaintAnimationClock.calculateBasePhaseForNewStroke(
                    clockStateMillis = 2500,
                    animationLoopDurationMillis = 1000,
                )
            )
            .isEqualTo(1.5f)
    }

    @Test
    fun calculateCurrentPhaseForPaint_returnsZeroForNonAnimatedPaint() {
        assertThat(
                StrokePaintAnimationClock.calculateCurrentPhaseForPaint(
                    clockStateMillis = 12345,
                    strokeAnimationLoopDurationMillis = 1000,
                    paintAnimationLoopDurationMillis = 0,
                    strokeBasePhase = 0.25f,
                )
            )
            .isEqualTo(0.0f)
    }

    @Test
    fun calculateCurrentPhaseForPaint_returnsExpectedValue() {
        assertThat(
                StrokePaintAnimationClock.calculateCurrentPhaseForPaint(
                    clockStateMillis = 1500,
                    strokeAnimationLoopDurationMillis = 1000,
                    paintAnimationLoopDurationMillis = 500,
                    strokeBasePhase = 0.25f,
                )
            )
            .isEqualTo(1.5f)
        assertThat(
                StrokePaintAnimationClock.calculateCurrentPhaseForPaint(
                    clockStateMillis = 1500,
                    strokeAnimationLoopDurationMillis = 1000,
                    paintAnimationLoopDurationMillis = 500,
                    strokeBasePhase = 0.5f,
                )
            )
            .isEqualTo(0.0f)
        assertThat(
                StrokePaintAnimationClock.calculateCurrentPhaseForPaint(
                    clockStateMillis = -125,
                    strokeAnimationLoopDurationMillis = 1000,
                    paintAnimationLoopDurationMillis = 500,
                    strokeBasePhase = 0.75f,
                )
            )
            .isEqualTo(1.25f)
    }
}
