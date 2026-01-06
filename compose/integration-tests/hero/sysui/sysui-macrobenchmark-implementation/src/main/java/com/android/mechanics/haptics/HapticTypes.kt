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

/**
 * Describes haptics triggered when crossing a breakpoint.
 *
 * Important: This is a complete enumeration of all effects supported.
 */
sealed class BreakpointHaptics {

    /** No Haptics. */
    data object None : BreakpointHaptics()

    /** Haptics force determined by the discontinuity delta and the breakpoint's spring. */
    @HapticsExperimentalApi
    data class SpringForce(val stiffness: Float, val dampingRatio: Float) : BreakpointHaptics()

    /** Play a generic threshold effect. */
    @HapticsExperimentalApi data object GenericThreshold : BreakpointHaptics()
}

/**
 * Describes haptics continuously played within a segment.
 *
 * Important: This is a complete enumeration of all effects supported.
 */
sealed class SegmentHaptics {

    data object None : SegmentHaptics()

    /**
     * Haptics effect describing tension texture.
     *
     * On breakpoints, tension released is played back with an effect similar to
     * [BreakpointHaptics.SpringForce] .
     */
    @HapticsExperimentalApi
    data class SpringTension(
        val anchorPointPx: Float,
        val attachedMassKg: Float = 1f, // In Kg
        val stiffness: Float = 900f, // in Newtons / meter
        val dampingRatio: Float = 0.95f, // unitless,
    ) : SegmentHaptics()
}
