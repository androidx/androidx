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

import androidx.animation.OnAnimationEnd
import androidx.animation.TargetAnimation
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.DensityAmbient

@Composable
internal actual fun ActualFlingConfig(
    onAnimationEnd: OnAnimationEnd?,
    adjustTarget: (Float) -> TargetAnimation?
): FlingConfig {
    // This function will internally update the calculation of fling decay when the density changes,
    // but the reference to the returned FlingConfig will not change across calls.
    val density = DensityAmbient.current
    val calculator = remember(density.density) { AndroidFlingCalculator(density) }
    val decayAnimation = remember { AndroidFlingDecaySpec(calculator) }
        .also { it.flingCalculator = calculator }
    return remember {
        FlingConfig(
            decayAnimation = decayAnimation,
            onAnimationEnd = onAnimationEnd,
            adjustTarget = adjustTarget
        )
    }
}
