/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.animation

import android.animation.Animator
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

/**
 * Add an action which will be invoked when the animation has ended.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 * @see Animator.end
 */
public inline fun Animator.doOnEnd(
    crossinline action: (animator: Animator) -> Unit
): Animator.AnimatorListener =
    addListener(onEnd = action)

/**
 * Add an action which will be invoked when the animation has started.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 * @see Animator.start
 */
public inline fun Animator.doOnStart(
    crossinline action: (animator: Animator) -> Unit
): Animator.AnimatorListener =
    addListener(onStart = action)

/**
 * Add an action which will be invoked when the animation has been cancelled.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 * @see Animator.cancel
 */
public inline fun Animator.doOnCancel(
    crossinline action: (animator: Animator) -> Unit
): Animator.AnimatorListener =
    addListener(onCancel = action)

/**
 * Add an action which will be invoked when the animation has repeated.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 */
public inline fun Animator.doOnRepeat(
    crossinline action: (animator: Animator) -> Unit
): Animator.AnimatorListener =
    addListener(onRepeat = action)

/**
 * Add an action which will be invoked when the animation has resumed after a pause.
 *
 * @return the [Animator.AnimatorPauseListener] added to the Animator
 * @see Animator.resume
 */
@RequiresApi(19)
public fun Animator.doOnResume(
    action: (animator: Animator) -> Unit
): Animator.AnimatorPauseListener =
    addPauseListener(onResume = action)

/**
 * Add an action which will be invoked when the animation has been paused.
 *
 * @return the [Animator.AnimatorPauseListener] added to the Animator
 * @see Animator.pause
 */
@RequiresApi(19)
public fun Animator.doOnPause(
    action: (animator: Animator) -> Unit
): Animator.AnimatorPauseListener =
    addPauseListener(onPause = action)

/**
 * Add a listener to this Animator using the provided actions.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 */
public inline fun Animator.addListener(
    crossinline onEnd: (animator: Animator) -> Unit = {},
    crossinline onStart: (animator: Animator) -> Unit = {},
    crossinline onCancel: (animator: Animator) -> Unit = {},
    crossinline onRepeat: (animator: Animator) -> Unit = {}
): Animator.AnimatorListener {
    val listener = object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animator: Animator) = onRepeat(animator)
        override fun onAnimationEnd(animator: Animator) = onEnd(animator)
        override fun onAnimationCancel(animator: Animator) = onCancel(animator)
        override fun onAnimationStart(animator: Animator) = onStart(animator)
    }
    addListener(listener)
    return listener
}

/**
 * Add a pause and resume listener to this Animator using the provided actions.
 *
 * @return the [Animator.AnimatorPauseListener] added to the Animator
 */
@RequiresApi(19)
public fun Animator.addPauseListener(
    onResume: (animator: Animator) -> Unit = {},
    onPause: (animator: Animator) -> Unit = {}
): Animator.AnimatorPauseListener {
    val listener = object : Animator.AnimatorPauseListener {
        override fun onAnimationPause(animator: Animator) = onPause(animator)
        override fun onAnimationResume(animator: Animator) = onResume(animator)
    }
    Api19Impl.addPauseListener(this, listener)
    return listener
}

@RequiresApi(19)
private object Api19Impl {
    @JvmStatic
    @DoNotInline
    fun addPauseListener(animator: Animator, listener: Animator.AnimatorPauseListener) {
        animator.addPauseListener(listener)
    }
}
