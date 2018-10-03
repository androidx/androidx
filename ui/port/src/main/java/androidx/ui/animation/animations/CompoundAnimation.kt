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
import androidx.ui.animation.AnimationLazyListenerMixin
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.AnimationStatusListener
import androidx.ui.runtimeType

/**
 * An interface for combining multiple Animations. Subclasses need only
 * implement the `value` getter to control how the child animations are
 * combined. Can be chained to combine more than 2 animations.
 *
 * For example, to create an animation that is the sum of two others, subclass
 * this class and define `T get value = first.value + second.value;`
 *
 * By default, the [status] of a [CompoundAnimation] is the status of the
 * [next] animation if [next] is moving, and the status of the [first]
 * animation otherwise.
 */
abstract class CompoundAnimation<T>(
    /**
     * The first sub-animation. Its status takes precedence if neither are
     * animating.
     */
    private val first: Animation<T>,
    /** The second sub-animation. */
    private val next: Animation<T>
) : AnimationLazyListenerMixin<T>() {

    /**
     * Gets the status of this animation based on the [first] and [next] status.
     *
     * The default is that if the [next] animation is moving, use its status.
     * Otherwise, default to [first].
     */
    override val status: AnimationStatus
        get() {
            if (next.status == AnimationStatus.FORWARD || next.status == AnimationStatus.REVERSE)
                return next.status
            return first.status
        }

    override fun toString() = "${runtimeType()}($first, $next)"

    private var lastStatus: AnimationStatus? = null
    private val maybeNotifyStatusListeners: AnimationStatusListener = {
        if (status != lastStatus) {
            lastStatus = status
            notifyStatusListeners(status)
        }
    }

    private var lastValue: T? = null
    private val maybeNotifyListeners = {
        if (value != lastValue) {
            lastValue = value
            notifyListeners()
        }
    }

    override fun didStartListening() {
        first.addListener(maybeNotifyListeners)
        first.addStatusListener(maybeNotifyStatusListeners)
        next.addListener(maybeNotifyListeners)
        next.addStatusListener(maybeNotifyStatusListeners)
    }

    override fun didStopListening() {
        first.removeListener(maybeNotifyListeners)
        first.removeStatusListener(maybeNotifyStatusListeners)
        next.removeListener(maybeNotifyListeners)
        next.removeStatusListener(maybeNotifyStatusListeners)
    }
}