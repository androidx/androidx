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

@file:SuppressLint("ClassVerificationFailure") // Entire file is RequiresApi(19)

package androidx.core.transition

import android.annotation.SuppressLint
import android.transition.Transition
import androidx.annotation.RequiresApi

/**
 * Add an action which will be invoked when this transition has ended.
 */
@RequiresApi(19)
public inline fun Transition.doOnEnd(
    crossinline action: (transition: Transition) -> Unit
): Transition.TransitionListener = addListener(onEnd = action)

/**
 * Add an action which will be invoked when this transition has started.
 */
@RequiresApi(19)
public inline fun Transition.doOnStart(
    crossinline action: (transition: Transition) -> Unit
): Transition.TransitionListener = addListener(onStart = action)

/**
 * Add an action which will be invoked when this transition has been cancelled.
 */
@RequiresApi(19)
public inline fun Transition.doOnCancel(
    crossinline action: (transition: Transition) -> Unit
): Transition.TransitionListener = addListener(onCancel = action)

/**
 * Add an action which will be invoked when this transition has resumed after a pause.
 */
@RequiresApi(19)
public inline fun Transition.doOnResume(
    crossinline action: (transition: Transition) -> Unit
): Transition.TransitionListener = addListener(onResume = action)

/**
 * Add an action which will be invoked when this transition has been paused.
 */
@RequiresApi(19)
public inline fun Transition.doOnPause(
    crossinline action: (transition: Transition) -> Unit
): Transition.TransitionListener = addListener(onPause = action)

/**
 * Add a listener to this Transition using the provided actions.
 */
@RequiresApi(19)
public inline fun Transition.addListener(
    crossinline onEnd: (transition: Transition) -> Unit = {},
    crossinline onStart: (transition: Transition) -> Unit = {},
    crossinline onCancel: (transition: Transition) -> Unit = {},
    crossinline onResume: (transition: Transition) -> Unit = {},
    crossinline onPause: (transition: Transition) -> Unit = {}
): Transition.TransitionListener {
    val listener = object : Transition.TransitionListener {
        override fun onTransitionEnd(transition: Transition) = onEnd(transition)
        override fun onTransitionResume(transition: Transition) = onResume(transition)
        override fun onTransitionPause(transition: Transition) = onPause(transition)
        override fun onTransitionCancel(transition: Transition) = onCancel(transition)
        override fun onTransitionStart(transition: Transition) = onStart(transition)
    }
    addListener(listener)
    return listener
}
