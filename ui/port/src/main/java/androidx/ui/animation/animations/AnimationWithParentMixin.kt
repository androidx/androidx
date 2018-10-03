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
 * Implements most of the [Animation] interface by deferring its behavior to a
 * given [parent] Animation.
 *
 * To implement an [Animation] that is driven by a parent, it is only necessary
 * to mix in this class, implement [parent], and implement `T get value`.
 *
 * To define a mapping from values in the range 0..1, consider subclassing
 * [Tween] instead.
 */
abstract class AnimationWithParentMixin<T>(
    /**
     * The animation whose value this animation will proxy.
     *
     * This animation must remain the same for the lifetime of this object. If
     * you wish to proxy a different animation at different times, consider using
     * [ProxyAnimation].
     */
    protected val parent: Animation<Double>
) : Animation<T>() {

    // keep these next five dartdocs in sync with the dartdocs in Animation<T>

    /**
     * Calls the listener every time the value of the animation changes.
     *
     * Listeners can be removed with [removeListener].
     */
    override fun addListener(listener: VoidCallback) = parent.addListener(listener)

    /**
     * Stop calling the listener every time the value of the animation changes.
     *
     * Listeners can be added with [addListener].
     */
    override fun removeListener(listener: VoidCallback) = parent.removeListener(listener)

    /**
     * Calls listener every time the status of the animation changes.
     *
     * Listeners can be removed with [removeStatusListener].
     */
    override fun addStatusListener(listener: AnimationStatusListener) =
        parent.addStatusListener(listener)

    /**
     * Stops calling the listener every time the status of the animation changes.
     *
     * Listeners can be added with [addStatusListener].
     */
    override fun removeStatusListener(listener: AnimationStatusListener) =
        parent.removeStatusListener(listener)

    /** The current status of this animation. */
    override val status: AnimationStatus get() = parent.status
}