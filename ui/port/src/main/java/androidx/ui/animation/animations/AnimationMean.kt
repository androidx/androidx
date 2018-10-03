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

package androidx.ui.animation.animations

import androidx.ui.animation.Animation

/**
 * An animation of [double]s that tracks the mean of two other animations.
 *
 * The [status] of this animation is the status of the `right` animation if it is
 * moving, and the `left` animation otherwise.
 *
 * The [value] of this animation is the [double] that represents the mean value
 * of the values of the `left` and `right` animations.
 */
class AnimationMean(
    left: Animation<Double>,
    right: Animation<Double>
) : CompoundAnimation<Double>(left, right) {

    override val value = (left.value + right.value) / 2.0
}