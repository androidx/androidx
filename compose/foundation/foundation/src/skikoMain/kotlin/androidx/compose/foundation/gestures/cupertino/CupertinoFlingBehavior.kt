/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.gestures.cupertino

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.gestures.DefaultScrollMotionDurationScale
import androidx.compose.foundation.gestures.ScrollableDefaultFlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.MotionDurationScale
import kotlin.math.abs
import kotlinx.coroutines.withContext

internal class CupertinoFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,

    /*
     * Post-drag inertia with velocity below [velocityThreshold] value will be consumed entirely
     * and not trigger any fling at all, value is approx and reverse-engineered from iOS 16 UIScrollView
     * blackbox
     */
    private val velocityThreshold: Float = 500f
) : ScrollableDefaultFlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < velocityThreshold) {
            return 0f
        }

        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity,
                ).animateDecay(flingDecay) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    velocityLeft = this.velocity
                    // avoid rounding errors and stop if anything is unconsumed
                    if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                }
                velocityLeft
            } else {
                initialVelocity
            }
        }
    }
}