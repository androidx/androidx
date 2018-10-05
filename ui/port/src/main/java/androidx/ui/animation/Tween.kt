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

package androidx.ui.animation

import androidx.ui.runtimeType

/**
 * A linear interpolation between a beginning and ending value.
 *
 * [Tween] is useful if you want to interpolate across a range.
 *
 * To use a [Tween] object with an animation, call the [Tween] object's
 * [animate] method and pass it the [Animation] object that you want to
 * modify.
 *
 * You can chain [Tween] objects together using the [chain] method, so that a
 * single [Animation] object is configured by multiple [Tween] objects called
 * in succession. This is different than calling the [animate] method twice,
 * which results in two [Animation] separate objects, each configured with a
 * single [Tween].
 *
 * ## Sample code
 *
 * Suppose `_controller` is an [AnimationController], and we want to create an
 * [Animation<Offset>] that is controlled by that controller, and save it in
 * `_animation`:
 *
 * ```dart
 * Animation<Offset> _animation = new Tween<Offset>(
 *   begin: const Offset(100.0, 50.0),
 *   end: const Offset(200.0, 300.0),
 * ).animate(_controller);
 * ```
 *
 * That would provide an `_animation` that, over the lifetime of the
 * `_controller`'s animation, returns a value that depicts a point along the
 * line between the two offsets above. If we used a [MaterialPointArcTween]
 * instead of a [Tween<Offset>] in the code above, the points would follow a
 * pleasing curve instead of a straight line, with no other changes necessary.
 *
 * The [begin] and [end] properties must be non-null before the tween is
 * first used, but the arguments can be null if the values are going to be
 * filled in later.
 */
data class Tween<T>(
    /**
     * The value this variable has at the beginning of the animation.
     *
     * See the constructor for details about whether this property may be null
     * (it varies from subclass to subclass).
     */
    private var begin: T? = null,
    /**
     * The value this variable has at the end of the animation.
     *
     * See the constructor for details about whether this property may be null
     * (it varies from subclass to subclass).
     */
    private var end: T? = null,
    private val evaluator: TweenEvaluator<T>

) : Animatable<T>() {

    /**
     * Returns the value this variable has at the given animation clock value.
     *
     * The default implementation of this method uses the [+], [-], and [*]
     * operators on `T`. The [begin] and [end] properties must therefore be
     * non-null by the time this method is called.
     */
    fun lerp(t: Double): T {
        assert(begin != null)
        assert(end != null)
        return evaluator.invoke(begin!!, end!!, t)
    }

    /**
     * Returns the interpolated value for the current value of the given animation.
     *
     * This method returns `begin` and `end` when the animation values are 0.0 or
     * 1.0, respectively.
     *
     * This function is implemented by deferring to [lerp]. Subclasses that want to
     * provide custom behavior should override [lerp], not [evaluate].
     *
     * See the constructor for details about whether the [begin] and [end]
     * properties may be null when this is called. It varies from subclass to
     * subclass.
     */
    override fun evaluate(animation: Animation<Double>): T {
        val t = animation.value
        if (t == 0.0)
            return begin!!
        if (t == 1.0)
            return end!!
        return lerp(t)
    }

    override fun toString() = "${runtimeType()}($begin \\u2192 $end)"
}
