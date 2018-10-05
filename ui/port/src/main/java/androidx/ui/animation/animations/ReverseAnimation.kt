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
import androidx.ui.animation.AnimationLazyListenerMixin
import androidx.ui.animation.AnimationStatus
import androidx.ui.runtimeType

/**
 * An animation that is the reverse of another animation.
 *
 * If the parent animation is running forward from 0.0 to 1.0, this animation
 * is running in reverse from 1.0 to 0.0.
 *
 * Using a [ReverseAnimation] is different from simply using a [Tween] with a
 * begin of 1.0 and an end of 0.0 because the tween does not change the status
 * or direction of the animation.
 */
class ReverseAnimation(
    /** The animation whose value and direction this animation is reversing. */
    private val parent: Animation<Double>
) : AnimationLazyListenerMixin<Double>() {

    override val status: AnimationStatus
        get() = when (parent.status) {
            AnimationStatus.FORWARD -> AnimationStatus.REVERSE
            AnimationStatus.REVERSE -> AnimationStatus.FORWARD
            AnimationStatus.COMPLETED -> AnimationStatus.DISMISSED
            AnimationStatus.DISMISSED -> AnimationStatus.COMPLETED
        }

    override val value: Double
        get() = 1.0 - parent.value

    override fun didStartListening() {
        parent.addStatusListener(statusChangeHandler)
    }

    override fun didStopListening() {
        parent.removeStatusListener(statusChangeHandler)
    }

    override fun addListener(listener: VoidCallback) {
        didRegisterListener()
        parent.addListener(listener)
    }

    override fun removeListener(listener: VoidCallback) {
        parent.removeListener(listener)
        didUnregisterListener()
    }

    private val statusChangeHandler = { status: AnimationStatus ->
        notifyStatusListeners(status)
    }

    override fun toString() = "$parent\u27AA${runtimeType()}"
}