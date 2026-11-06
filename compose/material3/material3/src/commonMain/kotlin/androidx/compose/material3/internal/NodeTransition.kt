/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.internal

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A [Modifier.Node] counterpart of [androidx.compose.animation.core.Transition]: animates a group
 * of values towards targets derived from the state of [node], without composition.
 *
 * Create it in [node], define its values with [animateColor], [animateDp] or [animateValue], read
 * [AnimatedValue.value] while drawing or measuring, call [update] whenever the state of [node]
 * changes, and call [reset] from [Modifier.Node.onDetach].
 *
 * This is a plain object rather than a delegated node, so it doesn't add a node nor a coroutine
 * scope: animations run in the [Modifier.Node.coroutineScope] of [node]. Values only allocate an
 * [Animatable] once their target changes after being read, so nodes whose state never changes don't
 * pay for any animations. Immediate snaps (a [SnapSpec] without delay) don't need one either.
 */
internal class NodeTransition(private val node: Modifier.Node) {
    // The values form a linked list, to avoid allocating a collection.
    private var lastValue: AnimatedValue<*, *>? = null

    fun animateColor(animationSpec: () -> AnimationSpec<Color>, targetValue: () -> Color) =
        animateValue({ Animatable(it) }, animationSpec, targetValue)

    fun animateDp(animationSpec: () -> AnimationSpec<Dp>, targetValue: () -> Dp) =
        animateValue({ Animatable(it, Dp.VectorConverter) }, animationSpec, targetValue)

    /**
     * Adds a value animating towards [targetValue] with [animationSpec]. Both lambdas are invoked
     * on [update], so they can read the state of the node.
     */
    fun <T : Any, V : AnimationVector> animateValue(
        createAnimatable: (initialValue: T) -> Animatable<T, V>,
        animationSpec: () -> AnimationSpec<T>,
        targetValue: () -> T,
    ): AnimatedValue<T, V> =
        AnimatedValue(createAnimatable, animationSpec, targetValue, lastValue).also {
            lastValue = it
        }

    /**
     * Animates the values whose target changed. Returns whether the caller must invalidate draw or
     * layout to read the new values, which is the case when a value allocated its [Animatable] or
     * snapped to its target. Existing [Animatable]s invalidate their readers on their own as they
     * animate.
     */
    fun update(): Boolean {
        if (!node.isAttached) return false
        var invalidate = false
        var value = lastValue
        while (value != null) {
            if (value.update()) invalidate = true
            value = value.previous
        }
        return invalidate
    }

    /** Drops all animations. Must be called when [node] is detached. */
    fun reset() {
        var value = lastValue
        while (value != null) {
            value.reset()
            value = value.previous
        }
    }

    inner class AnimatedValue<T : Any, V : AnimationVector>
    internal constructor(
        private val createAnimatable: (initialValue: T) -> Animatable<T, V>,
        private val animationSpec: () -> AnimationSpec<T>,
        private val targetValue: () -> T,
        internal val previous: AnimatedValue<*, *>?,
    ) {
        private var animatable: Animatable<T, V>? = null
        private var job: Job? = null
        // The last target that was read, or null if the value wasn't read since being reset.
        private var target: T? = null

        val value: T
            get() = animatable?.value ?: target ?: targetValue().also { target = it }

        /** Animates towards the new target, if any. Returns whether readers must be invalidated. */
        internal fun update(): Boolean {
            val oldTarget = target ?: return false
            val newTarget = targetValue()
            if (oldTarget == newTarget) return false
            target = newTarget
            val spec = animationSpec()
            if (spec is SnapSpec<*> && spec.delay == 0) {
                // An Animatable would only snap once its coroutine runs, after this frame: drop it,
                // so that the value is the new target right away.
                job?.cancel()
                animatable = null
                return true
            }
            val existing = animatable
            val anim = existing ?: createAnimatable(oldTarget).also { animatable = it }
            job = node.coroutineScope.launch { anim.animateTo(newTarget, spec) }
            return existing == null
        }

        internal fun reset() {
            animatable = null
            job = null
            target = null
        }
    }
}
