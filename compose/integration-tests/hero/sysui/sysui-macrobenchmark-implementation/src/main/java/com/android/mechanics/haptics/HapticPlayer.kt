/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.haptics

import androidx.compose.runtime.staticCompositionLocalOf

/** Composition-local to provide a [HapticPlayer]. */
val LocalHapticPlayer = staticCompositionLocalOf { HapticPlayer.NoPlayer }

interface HapticPlayer {

    fun playSegmentHaptics(
        segmentHaptics: SegmentHaptics,
        spatialInput: Float,
        spatialVelocity: Float,
    )

    fun playBreakpointHaptics(
        breakpointHaptics: BreakpointHaptics,
        spatialInput: Float,
        spatialVelocity: Float,
    )

    /** Get the minimum interval required for haptics to play */
    fun getPlaybackIntervalNanos(): Long = 0L

    companion object {
        val NoPlayer =
            object : HapticPlayer {
                override fun playSegmentHaptics(
                    segmentHaptics: SegmentHaptics,
                    spatialInput: Float,
                    spatialVelocity: Float,
                ) {}

                override fun playBreakpointHaptics(
                    breakpointHaptics: BreakpointHaptics,
                    spatialInput: Float,
                    spatialVelocity: Float,
                ) {}
            }
    }
}
