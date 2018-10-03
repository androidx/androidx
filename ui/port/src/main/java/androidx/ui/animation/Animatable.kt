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

import androidx.ui.animation.animations.AlwaysStoppedAnimation
import androidx.ui.animation.animations.AnimationWithParentMixin

/**
 * An object that can produce a value of type `T` given an [Animation<double>]
 * as input.
 *
 * Typically, the values of the input animation are nominally in the range 0.0
 * to 1.0. In principle, however, any value could be provided.
 */
abstract class Animatable<T> {

    /** The current value of this object for the given animation. */
    abstract fun evaluate(animation: Animation<Double>): T

    /**
     * Returns a new Animation that is driven by the given animation but that
     * takes on values determined by this object.
     */
    fun animate(parent: Animation<Double>): Animation<T> {
        return AnimatedEvaluation(parent, this)
    }

    /**
     * Returns a new Animatable whose value is determined by first evaluating
     * the given parent and then evaluating this object.
     */
    fun chain(parent: Animatable<Double>): Animatable<T> {
        return ChainedEvaluation(parent, this)
    }
}

private class AnimatedEvaluation<T>(
    parent: Animation<Double>,
    private val evaluatable: Animatable<T>
) : AnimationWithParentMixin<T>(parent) {

    override val value get() = evaluatable.evaluate(parent)

    override fun toString() = "$parent\u27A9$evaluatable\u27A9$value"

    override fun toStringDetails() = "${super.toStringDetails()} $evaluatable"
}

private class ChainedEvaluation<T>(
    private val parent: Animatable<Double>,
    private val evaluatable: Animatable<T>
) : Animatable<T>() {

    override fun evaluate(animation: Animation<Double>): T {
        val value = parent.evaluate(animation)
        return evaluatable.evaluate(AlwaysStoppedAnimation(value))
    }

    override fun toString() = "$parent\u27A9$evaluatable"
}