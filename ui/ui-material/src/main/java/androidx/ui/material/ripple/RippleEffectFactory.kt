/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.ripple

import androidx.animation.AnimationClockObservable
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize

/**
 * An encapsulation of an [RippleEffect] constructor used by [ripple] and [RippleTheme].
 *
 * Will be used as a theme parameter in [RippleTheme.factory]
 */
interface RippleEffectFactory {

    /**
     * The factory method.
     *
     * Subclasses should override this method to return a new instance of an [RippleEffect].
     *
     * @param size The size of the target layout.
     * @param startPosition The position the animation will start from.
     * @param density The [Density] object to convert the dimensions.
     * @param radius Effects grow up to this size.
     * @param clipped If true the effect should be clipped by the target layout bounds.
     * @param clock The animation clock observable that will drive this ripple effect
     * @param onAnimationFinished Call when the effect animation has been finished.
     */
    fun create(
        size: PxSize,
        startPosition: PxPosition,
        density: Density,
        radius: Dp?,
        clipped: Boolean,
        clock: AnimationClockObservable,
        onAnimationFinished: ((RippleEffect) -> Unit)
    ): RippleEffect
}
