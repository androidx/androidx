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

import androidx.ui.VoidCallback
import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.AnimationStatusListener

/**
 * An animation that is always dismissed.
 *
 * Using this constant involves less overhead than building an
 * [AnimationController] with an initial value of 0.0. This is useful when an
 * API expects an animation but you don't actually want to animate anything.
 */
object AlwaysDismissedAnimation : Animation<Double>() {

    override fun addListener(listener: VoidCallback) {}

    override fun removeListener(listener: VoidCallback) {}

    override fun addStatusListener(listener: AnimationStatusListener) {}

    override fun removeStatusListener(listener: AnimationStatusListener) {}

    override val status = AnimationStatus.DISMISSED

    override val value = 0.0

    override fun toString() = "AlwaysDismissedAnimation"
}
