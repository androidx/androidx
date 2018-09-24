/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.velocity_tracker

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.toStringAsFixed

/**
 * A two dimensional velocity estimate.
 *
 * VelocityEstimates are computed by [VelocityTracker.getVelocityEstimate]. An
 * estimate's [confidence] measures how well the velocity tracker's position
 * data fit a straight line, [duration] is the time that elapsed between the
 * first and last position sample used to compute the velocity, and [offset]
 * is similarly the difference between the first and last positions.
 *
 * See also:
 *
 *  * VelocityTracker, which computes [VelocityEstimate]s.
 *  * Velocity, which encapsulates (just) a velocity vector and provides some
 *    useful velocity operations.
 */
open class VelocityEstimate(
    /** The number of pixels per second of velocity in the x and y directions. */
    val pixelsPerSecond: Offset,
    /**
     * A value between 0.0 and 1.0 that indicates how well [VelocityTracker]
     * was able to fit a straight line to its position data.
     *
     * The value of this property is 1.0 for a perfect fit, 0.0 for a poor fit.
     */
    val confidence: Double,
    /**
     * The time that elapsed between the first and last position sample used
     * to compute [pixelsPerSecond].
     */
    val duration: Duration,
    /**
     * The difference between the first and last position sample used
     * to compute [pixelsPerSecond].
     */
    val offset: Offset
) {
    override fun toString() = "VelocityEstimate(" +
            "${pixelsPerSecond.dx.toStringAsFixed(1)}, ${pixelsPerSecond.dy.toStringAsFixed(1)}; " +
            "offset: $offset, " +
            "duration: $duration, " +
            "confidence: ${confidence.toStringAsFixed(1)})"
}