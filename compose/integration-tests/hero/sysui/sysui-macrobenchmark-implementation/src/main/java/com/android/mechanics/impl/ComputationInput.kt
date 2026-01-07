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

package com.android.mechanics.impl

import com.android.mechanics.MotionValue
import com.android.mechanics.spec.Breakpoint
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.SegmentData
import com.android.mechanics.spring.SpringState

/** Static configuration that remains constant over a MotionValue's lifecycle. */
internal interface StaticConfig {
    /**
     * A threshold value (in output units) that determines when the [MotionValue]'s internal spring
     * animation is considered stable.
     */
    val stableThreshold: Float

    /** Optional label for identifying a MotionValue for debugging purposes. */
    val label: String?
}

/** The up-to-date [MotionValue] input, used by [Computations] to calculate the updated output. */
internal interface CurrentFrameInput {
    val spec: MotionSpec
    val currentInput: Float
    val currentAnimationTimeNanos: Long
    val currentDirection: InputDirection
    val currentGestureDragOffset: Float
}

/**
 * The [MotionValue] state of the last completed frame.
 *
 * The values must be published at the start of the frame, together with the
 * [CurrentFrameInput.currentAnimationTimeNanos].
 */
internal interface LastFrameState {
    /**
     * The segment in use, defined by the min/max [Breakpoint]s and the [Mapping] in between. This
     * implicitly also captures the [InputDirection] and [MotionSpec].
     */
    val lastSegment: SegmentData
    /**
     * State of the [Guarantee]. Its interpretation is defined by the [lastSegment]'s
     * [SegmentData.entryBreakpoint]'s [Breakpoint.guarantee]. If that breakpoint has no guarantee,
     * this value will be [GuaranteeState.Inactive].
     *
     * This is the maximal guarantee value seen so far, as well as the guarantee's start value, and
     * is used to compute the spring-tightening fraction.
     */
    val lastGuaranteeState: GuaranteeState
    /**
     * The state of an ongoing animation of a discontinuity.
     *
     * The spring animation is described by the [DiscontinuityAnimation.springStartState], which
     * tracks the oscillation of the spring until the displacement is guaranteed not to exceed
     * [stableThreshold] anymore. The spring animation started at
     * [DiscontinuityAnimation.springStartTimeNanos], and uses the
     * [DiscontinuityAnimation.springParameters]. The displacement's origin is at
     * [DiscontinuityAnimation.targetValue].
     *
     * This state does not have to be updated every frame, even as an animation is ongoing: the
     * spring animation can be computed with the same start parameters, and as time progresses, the
     * [SpringState.calculateUpdatedState] is passed an ever larger `elapsedNanos` on each frame.
     *
     * The [DiscontinuityAnimation.targetValue] is a delta to the direct mapped output value from
     * the [SegmentData.mapping]. It might accumulate the target value - it is not required to reset
     * when the animation ends.
     */
    val lastAnimation: DiscontinuityAnimation
    /**
     * Last frame's spring state, based on initial origin values in [lastAnimation], carried-forward
     * to [lastFrameTimeNanos].
     */
    val lastSpringState: SpringState
    /** The time of the last frame, in nanoseconds. */
    val lastFrameTimeNanos: Long
    /** The [currentInput] of the last frame */
    val lastInput: Float
    val lastGestureDragOffset: Float

    val directMappedVelocity: Float

    /** Last time that haptics played */
    var lastHapticsTimeNanos: Long
}
