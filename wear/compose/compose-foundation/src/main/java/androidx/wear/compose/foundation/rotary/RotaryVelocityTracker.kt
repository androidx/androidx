/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.rotary

import androidx.compose.ui.input.pointer.util.VelocityTracker1D

/** A wrapper around VelocityTracker1D to provide support for rotary input. */
internal class RotaryVelocityTracker {
    private var velocityTracker: VelocityTracker1D = VelocityTracker1D(true)

    /** Retrieve the last computed velocity. */
    val velocity: Float
        get() = velocityTracker.calculateVelocity()

    /** Start tracking motion. */
    fun start(currentTime: Long) {
        velocityTracker.resetTracking()
        velocityTracker.addDataPoint(currentTime, 0f)
    }

    /** Continue tracking motion as the input rotates. */
    fun move(currentTime: Long, delta: Float) {
        velocityTracker.addDataPoint(currentTime, delta)
    }

    /** Stop tracking motion. */
    fun end() {
        velocityTracker.resetTracking()
    }
}
