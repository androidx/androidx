/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.unit.Velocity

internal class DifferentialVelocityTracker {
    private val xVelocityTracker = VelocityTracker1D(isDataDifferential = true)
    private val yVelocityTracker = VelocityTracker1D(isDataDifferential = true)

    fun addDelta(timeMillis: Long, delta: Offset) {
        xVelocityTracker.addDataPoint(timeMillis, delta.x)
        yVelocityTracker.addDataPoint(timeMillis, delta.y)
    }

    fun calculateVelocity(): Velocity {
        val velocityX = xVelocityTracker.calculateVelocity(Float.MAX_VALUE)
        val velocityY = yVelocityTracker.calculateVelocity(Float.MAX_VALUE)
        return Velocity(velocityX, velocityY)
    }
}
