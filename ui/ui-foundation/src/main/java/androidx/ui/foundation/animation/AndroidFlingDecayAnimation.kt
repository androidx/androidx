/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation.animation

import androidx.animation.DecayAnimation
import kotlin.math.sign

/**
 * A native Android fling curve decay.
 */
class AndroidFlingDecayAnimation(
    flingCalculator: AndroidFlingCalculator
) : DecayAnimation {

    var flingCalculator = flingCalculator
        // Allow the @Composable factory to update this for the animation it creates
        // when the density ambient changes.
        internal set

    override val absVelocityThreshold: Float get() = 0f

    private fun flingDistance(startVelocity: Float): Float =
        flingCalculator.flingDistance(startVelocity) * sign(startVelocity)

    override fun getTarget(start: Float, startVelocity: Float): Float =
        start + flingDistance(startVelocity)

    override fun getValue(playTime: Long, start: Float, startVelocity: Float): Float =
        start + flingCalculator.flingInfo(startVelocity).position(playTime)

    override fun getDurationMillis(start: Float, startVelocity: Float): Long =
        flingCalculator.flingDuration(startVelocity)

    override fun getVelocity(playTime: Long, start: Float, startVelocity: Float): Float =
        flingCalculator.flingInfo(startVelocity).velocity(playTime)
}